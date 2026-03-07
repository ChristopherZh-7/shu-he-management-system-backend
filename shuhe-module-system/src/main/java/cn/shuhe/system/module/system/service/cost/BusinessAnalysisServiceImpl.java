package cn.shuhe.system.module.system.service.cost;

import cn.hutool.core.collection.CollUtil;
import cn.shuhe.system.module.system.controller.admin.cost.vo.BusinessAnalysisReqVO;
import cn.shuhe.system.module.system.controller.admin.cost.vo.BusinessAnalysisRespVO;
import cn.shuhe.system.module.system.controller.admin.cost.vo.BusinessAnalysisRespVO.*;
import cn.shuhe.system.module.system.controller.admin.cost.vo.UserCostRespVO;
import cn.shuhe.system.module.system.dal.dataobject.dept.DeptDO;
import cn.shuhe.system.module.system.dal.dataobject.user.AdminUserDO;
import cn.shuhe.system.module.system.dal.mysql.cost.OutsideCostRecordMapper;
import cn.shuhe.system.module.system.dal.mysql.cost.ProjectSiteMemberInfoMapper;
import cn.shuhe.system.module.system.dal.mysql.cost.ServiceItemInfoMapper;
import cn.shuhe.system.module.system.dal.mysql.dept.DeptMapper;
import cn.shuhe.system.module.system.service.dept.DeptService;
import cn.shuhe.system.module.system.service.user.AdminUserService;
import jakarta.annotation.Resource;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 经营分析 Service 实现类。
 *
 * <p>职责：数据拉取、批量预加载（AnalysisContext）、树形结构编排、VO 组装。
 * 所有计算规则统一委托给 {@link BusinessAnalysisCalculator}。</p>
 */
@Slf4j
@Service
public class BusinessAnalysisServiceImpl implements BusinessAnalysisService {

    /**
     * 分析上下文：批量预加载所有数据，避免 N+1 查询。
     */
    @Data
    @Builder
    private static class AnalysisContext {
        private int year;
        private LocalDate cutoffDate;

        /** deptId -> DeptDO */
        private Map<Long, DeptDO> deptCache;
        /** parentId -> List<DeptDO> */
        private Map<Long, List<DeptDO>> childDeptCache;
        /** deptId -> deptType（含从层级推断的类型） */
        private Map<Long, Integer> deptTypeCache;
        /** userId -> 年累计成本 */
        private Map<Long, BigDecimal> employeeCostCache;
        /** 所有部门负责人的 userId */
        private Set<Long> leaderUserIds;
        /** deptId -> 直属员工列表 */
        private Map<Long, List<AdminUserDO>> deptEmployeeCache;
        /** userId -> 驻场收入 */
        private Map<Long, BigDecimal> operationIncomeCache;
        /** userId -> 轮次收入 */
        private Map<Long, BigDecimal> serviceIncomeCache;
    }

    @Resource
    private DeptService deptService;

    @Resource
    private DeptMapper deptMapper;

    @Resource
    private AdminUserService adminUserService;

    @Resource
    private CostCalculationService costCalculationService;

    @Resource
    private OutsideCostRecordMapper outsideCostRecordMapper;

    @Resource
    private ProjectSiteMemberInfoMapper projectSiteMemberInfoMapper;

    @Resource
    private ServiceItemInfoMapper serviceItemInfoMapper;

    /** 所有计算规则的统一入口 */
    @Resource
    private BusinessAnalysisCalculator calculator;

    // ========== 公开接口实现 ==========

    @Override
    public BusinessAnalysisRespVO getBusinessAnalysis(BusinessAnalysisReqVO reqVO, Long currentUserId) {
        long startTime = System.currentTimeMillis();

        LocalDate cutoffDate = reqVO.getCutoffDate() != null ? reqVO.getCutoffDate() : LocalDate.now();
        int year = reqVO.getYear() != null ? reqVO.getYear() : cutoffDate.getYear();

        List<DeptDO> visibleDepts = getVisibleDepts(reqVO.getDeptId(), currentUserId);
        if (CollUtil.isEmpty(visibleDepts)) {
            return BusinessAnalysisRespVO.builder()
                    .year(year)
                    .cutoffDate(cutoffDate)
                    .deptAnalysisList(Collections.emptyList())
                    .total(calculator.buildEmptyTotal())
                    .build();
        }

        log.info("开始预加载数据...");
        long preloadStart = System.currentTimeMillis();
        AnalysisContext context = buildAnalysisContext(year, cutoffDate, visibleDepts);
        log.info("预加载完成，耗时: {}ms", System.currentTimeMillis() - preloadStart);

        boolean includeSubDepts = reqVO.getLevel() != null && reqVO.getLevel() >= 2;
        boolean includeEmployees = Boolean.TRUE.equals(reqVO.getIncludeEmployees()) ||
                (reqVO.getLevel() != null && reqVO.getLevel() >= 3);

        List<DeptAnalysis> deptAnalysisList = new ArrayList<>();
        for (DeptDO dept : visibleDepts) {
            DeptAnalysis analysis = buildDeptAnalysisWithCache(dept, context, includeSubDepts, includeEmployees);
            if (analysis != null) {
                deptAnalysisList.add(analysis);
            }
        }

        TotalAnalysis total = calculator.calculateTotal(deptAnalysisList);

        log.info("经营分析计算完成，总耗时: {}ms", System.currentTimeMillis() - startTime);

        return BusinessAnalysisRespVO.builder()
                .year(year)
                .cutoffDate(cutoffDate)
                .deptAnalysisList(deptAnalysisList)
                .total(total)
                .build();
    }

    @Override
    public DeptAnalysis getDeptDetail(Long deptId, int year, LocalDate cutoffDate) {
        DeptDO dept = deptService.getDept(deptId);
        if (dept == null) {
            return null;
        }
        return buildDeptAnalysis(dept, year, cutoffDate, true, true);
    }

    @Override
    public EmployeeAnalysis getEmployeeAnalysis(Long userId, int year, LocalDate cutoffDate) {
        AdminUserDO user = adminUserService.getUser(userId);
        if (user == null) {
            return null;
        }
        return buildEmployeeAnalysis(user, year, cutoffDate, true);
    }

    // ========== 数据预加载 ==========

    /**
     * 构建分析上下文，批量预加载所有需要的数据。
     */
    private AnalysisContext buildAnalysisContext(int year, LocalDate cutoffDate, List<DeptDO> visibleDepts) {
        List<DeptDO> allDepts = deptService.getAllDeptListFromCache();
        Map<Long, DeptDO> deptCache = allDepts.stream()
                .collect(Collectors.toMap(DeptDO::getId, d -> d, (a, b) -> a));

        Map<Long, List<DeptDO>> childDeptCache = allDepts.stream()
                .filter(d -> d.getParentId() != null && d.getParentId() > 0)
                .collect(Collectors.groupingBy(DeptDO::getParentId));

        Map<Long, Integer> deptTypeCache = new ConcurrentHashMap<>();
        for (DeptDO dept : allDepts) {
            Integer type = dept.getDeptType();
            if (type == null) {
                type = calculator.inferDeptTypeFromName(dept.getName());
            }
            if (type == null) {
                type = getDeptTypeFromHierarchyWithCache(dept.getId(), deptCache, deptTypeCache);
            }
            if (type != null) {
                deptTypeCache.put(dept.getId(), type);
            }
        }

        Set<Long> allRelevantDeptIds = new HashSet<>();
        for (DeptDO dept : visibleDepts) {
            allRelevantDeptIds.add(dept.getId());
            collectChildDeptIdsWithCache(dept.getId(), allRelevantDeptIds, childDeptCache);
        }

        List<AdminUserDO> allEmployees = adminUserService.getUserListByDeptIds(allRelevantDeptIds);
        Map<Long, List<AdminUserDO>> deptEmployeeCache = allEmployees.stream()
                .filter(u -> u.getDeptId() != null)
                .collect(Collectors.groupingBy(AdminUserDO::getDeptId));

        Set<Long> leaderUserIds = allDepts.stream()
                .filter(d -> d.getLeaderUserId() != null)
                .map(DeptDO::getLeaderUserId)
                .collect(Collectors.toSet());

        int month = cutoffDate.getMonthValue();
        Set<Long> allUserIds = allEmployees.stream().map(AdminUserDO::getId).collect(Collectors.toSet());
        Map<Long, BigDecimal> employeeCostCache = new ConcurrentHashMap<>(
                costCalculationService.batchGetUserYearToDateCost(allUserIds, year, month, cutoffDate));

        Map<Long, BigDecimal> operationIncomeCache = new ConcurrentHashMap<>();
        Map<Long, BigDecimal> serviceIncomeCache = new ConcurrentHashMap<>();

        LocalDateTime cutoffDateTime = LocalDateTime.of(cutoffDate, LocalTime.MAX);
        List<Long> allUserIdList = new ArrayList<>(allUserIds);

        // 批量查询所有员工的驻场参与记录
        Map<Long, List<Map<String, Object>>> userParticipationMap = new HashMap<>();
        if (!allUserIdList.isEmpty()) {
            List<Map<String, Object>> allParticipations =
                    projectSiteMemberInfoMapper.selectMemberParticipationBatch(allUserIdList, null);
            for (Map<String, Object> p : allParticipations) {
                Long userId = calculator.getLongValue(p.get("userId"));
                if (userId != null) {
                    userParticipationMap.computeIfAbsent(userId, k -> new ArrayList<>()).add(p);
                }
            }
        }

        // 批量查询所有员工的轮次收入记录
        Map<Long, List<Map<String, Object>>> userRoundMap = new HashMap<>();
        if (!allUserIdList.isEmpty()) {
            List<Map<String, Object>> allRounds =
                    serviceItemInfoMapper.selectCompletedRoundsByExecutorBatch(year, cutoffDateTime);
            for (Map<String, Object> r : allRounds) {
                String executorIdsStr = (String) r.get("executorIds");
                if (executorIdsStr != null) {
                    for (Long userId : allUserIdList) {
                        if (executorIdsStr.contains(String.valueOf(userId))) {
                            userRoundMap.computeIfAbsent(userId, k -> new ArrayList<>()).add(r);
                        }
                    }
                }
            }
        }

        // 计算每个员工的收入并写入缓存
        for (AdminUserDO employee : allEmployees) {
            Long userId = employee.getId();
            Long deptId = employee.getDeptId();
            Integer deptType = deptId != null ? deptTypeCache.get(deptId) : null;

            if (deptType != null) {
                List<Map<String, Object>> participations = userParticipationMap.getOrDefault(userId, Collections.emptyList());
                List<Map<String, Object>> filteredParticipations = participations.stream()
                        .filter(p -> {
                            Integer pDeptType = calculator.getIntValue(p.get("deptType"));
                            return pDeptType != null && pDeptType.equals(deptType);
                        })
                        .collect(Collectors.toList());
                operationIncomeCache.put(userId,
                        calculator.calculateOnsiteIncomeFromData(filteredParticipations, year, cutoffDate));

                if (deptType == BusinessAnalysisCalculator.DEPT_TYPE_SECURITY_SERVICE
                        || deptType == BusinessAnalysisCalculator.DEPT_TYPE_DATA_SECURITY) {
                    List<Map<String, Object>> rounds = userRoundMap.getOrDefault(userId, Collections.emptyList());
                    serviceIncomeCache.put(userId,
                            calculator.calculateServiceRoundIncomeFromData(rounds, userId, year, cutoffDate));
                }
            }
        }

        return AnalysisContext.builder()
                .year(year)
                .cutoffDate(cutoffDate)
                .deptCache(deptCache)
                .childDeptCache(childDeptCache)
                .deptTypeCache(deptTypeCache)
                .employeeCostCache(employeeCostCache)
                .leaderUserIds(leaderUserIds)
                .deptEmployeeCache(deptEmployeeCache)
                .operationIncomeCache(operationIncomeCache)
                .serviceIncomeCache(serviceIncomeCache)
                .build();
    }

    // ========== 权限与部门过滤 ==========

    /**
     * 根据权限获取可查看的顶级业务部门列表（避免重复统计）。
     */
    private List<DeptDO> getVisibleDepts(Long requestedDeptId, Long currentUserId) {
        AdminUserDO currentUser = adminUserService.getUser(currentUserId);
        if (currentUser == null) {
            log.warn("[getVisibleDepts] 用户 {} 不存在", currentUserId);
            return Collections.emptyList();
        }

        if (requestedDeptId != null) {
            DeptDO dept = deptService.getDept(requestedDeptId);
            log.info("[getVisibleDepts] 指定部门 deptId={}, found={}", requestedDeptId, dept != null);
            return dept != null ? List.of(dept) : Collections.emptyList();
        }

        List<DeptDO> allDepts = deptService.getAllDeptListFromCache();
        log.info("[getVisibleDepts] 总部门数: {}", allDepts.size());

        for (DeptDO dept : allDepts) {
            Integer type = dept.getDeptType();
            Integer inferred = calculator.inferDeptTypeFromName(dept.getName());
            log.info("[getVisibleDepts] 部门 id={}, name={}, deptType={}, inferredType={}, parentId={}",
                    dept.getId(), dept.getName(), type, inferred, dept.getParentId());
        }

        Set<Long> businessDeptIds = allDepts.stream()
                .filter(dept -> {
                    Integer type = dept.getDeptType();
                    if (type == null) type = calculator.inferDeptTypeFromName(dept.getName());
                    return type != null && isBusinessDeptType(type);
                })
                .map(DeptDO::getId)
                .collect(Collectors.toSet());
        log.info("[getVisibleDepts] 业务部门IDs: {}", businessDeptIds);

        List<DeptDO> result = allDepts.stream()
                .filter(dept -> {
                    Integer type = dept.getDeptType();
                    if (type == null) type = calculator.inferDeptTypeFromName(dept.getName());
                    if (type == null || !isBusinessDeptType(type)) return false;
                    Long parentId = dept.getParentId();
                    boolean isTopLevel = parentId == null || parentId == 0L || !businessDeptIds.contains(parentId);
                    if (!isTopLevel) {
                        log.info("[getVisibleDepts] 部门 {} ({}) 被过滤：父部门 {} 也是业务部门",
                                dept.getId(), dept.getName(), parentId);
                    }
                    return isTopLevel;
                })
                .collect(Collectors.toList());

        log.info("[getVisibleDepts] 最终返回 {} 个顶级业务部门: {}", result.size(),
                result.stream().map(d -> d.getId() + "(" + d.getName() + ")").collect(Collectors.joining(", ")));
        return result;
    }

    private boolean isBusinessDeptType(int type) {
        return type == BusinessAnalysisCalculator.DEPT_TYPE_SECURITY_SERVICE
                || type == BusinessAnalysisCalculator.DEPT_TYPE_SECURITY_OPERATION
                || type == BusinessAnalysisCalculator.DEPT_TYPE_DATA_SECURITY;
    }

    // ========== 优化版分析构建（使用缓存） ==========

    private DeptAnalysis buildDeptAnalysisWithCache(DeptDO dept, AnalysisContext ctx,
                                                    boolean includeSubDepts, boolean includeEmployees) {
        Integer deptType = ctx.getDeptTypeCache().get(dept.getId());
        if (deptType == null) {
            deptType = calculator.inferDeptTypeFromName(dept.getName());
        }

        List<AdminUserDO> employees = getEmployeesUnderDeptWithCache(dept.getId(), ctx);

        BigDecimal totalContractIncome = BigDecimal.ZERO;
        BigDecimal totalEmployeeCost = BigDecimal.ZERO;
        List<EmployeeAnalysis> employeeAnalysisList = new ArrayList<>();

        for (AdminUserDO employee : employees) {
            EmployeeAnalysis empAnalysis = buildEmployeeAnalysisWithCache(employee, ctx, includeEmployees);
            if (empAnalysis != null) {
                totalContractIncome = totalContractIncome.add(
                        empAnalysis.getContractIncome() != null ? empAnalysis.getContractIncome() : BigDecimal.ZERO);
                totalEmployeeCost = totalEmployeeCost.add(
                        empAnalysis.getEmployeeCost() != null ? empAnalysis.getEmployeeCost() : BigDecimal.ZERO);
                if (includeEmployees) {
                    employeeAnalysisList.add(empAnalysis);
                }
            }
        }

        LocalDateTime cutoffDateTime = ctx.getCutoffDate().atTime(LocalTime.MAX);
        BigDecimal totalOutsideIncome = calculator.calculateDeptOutsideIncomeWithCache(
                dept.getId(), ctx.getYear(), cutoffDateTime, ctx.getChildDeptCache());
        BigDecimal totalOutsideExpense = calculator.calculateDeptOutsideExpenseWithCache(
                dept.getId(), ctx.getYear(), cutoffDateTime, ctx.getChildDeptCache());

        BigDecimal totalIncome = totalContractIncome.add(totalOutsideIncome);
        BigDecimal totalExpense = totalEmployeeCost.add(totalOutsideExpense);
        BigDecimal netProfit = totalIncome.subtract(totalExpense);
        BigDecimal profitRate = calculator.calculateProfitRate(netProfit, totalIncome);

        List<SubDeptAnalysis> subDeptList = null;
        if (includeSubDepts) {
            subDeptList = buildSubDeptAnalysisListWithCache(dept.getId(), ctx, includeEmployees);
        }

        List<EmployeeAnalysis> directEmployeeList = null;
        if (includeEmployees) {
            List<AdminUserDO> directEmployees = ctx.getDeptEmployeeCache().get(dept.getId());
            if (CollUtil.isNotEmpty(directEmployees)) {
                directEmployeeList = new ArrayList<>();
                for (AdminUserDO employee : directEmployees) {
                    EmployeeAnalysis empAnalysis = buildEmployeeAnalysisWithCache(employee, ctx, true);
                    if (empAnalysis != null) {
                        directEmployeeList.add(empAnalysis);
                    }
                }
            }
        }

        return DeptAnalysis.builder()
                .deptId(dept.getId())
                .deptName(dept.getName())
                .deptType(deptType)
                .deptTypeName(BusinessAnalysisCalculator.DEPT_TYPE_NAMES.get(deptType))
                .employeeCount(employees.size())
                .contractIncome(totalContractIncome)
                .outsideIncome(totalOutsideIncome)
                .totalIncome(totalIncome)
                .employeeCost(totalEmployeeCost)
                .outsideExpense(totalOutsideExpense)
                .totalExpense(totalExpense)
                .netProfit(netProfit)
                .profitRate(profitRate)
                .subDeptList(subDeptList)
                .employeeList(CollUtil.isNotEmpty(directEmployeeList) ? directEmployeeList : null)
                .build();
    }

    private List<AdminUserDO> getEmployeesUnderDeptWithCache(Long deptId, AnalysisContext ctx) {
        Set<Long> deptIds = new HashSet<>();
        deptIds.add(deptId);
        collectChildDeptIdsWithCache(deptId, deptIds, ctx.getChildDeptCache());
        List<AdminUserDO> result = new ArrayList<>();
        for (Long id : deptIds) {
            List<AdminUserDO> employees = ctx.getDeptEmployeeCache().get(id);
            if (employees != null) {
                result.addAll(employees);
            }
        }
        return result;
    }

    private List<SubDeptAnalysis> buildSubDeptAnalysisListWithCache(Long parentDeptId, AnalysisContext ctx,
                                                                    boolean includeEmployees) {
        List<DeptDO> directChildren = ctx.getChildDeptCache().get(parentDeptId);
        if (CollUtil.isEmpty(directChildren)) {
            return Collections.emptyList();
        }
        List<DeptDO> sortedChildren = new ArrayList<>(directChildren);
        sortedChildren.sort(createDeptComparator());
        List<SubDeptAnalysis> result = new ArrayList<>();
        for (DeptDO subDept : sortedChildren) {
            SubDeptAnalysis subAnalysis = buildSubDeptAnalysisWithCache(subDept, ctx, includeEmployees);
            if (subAnalysis != null) {
                result.add(subAnalysis);
            }
        }
        return result;
    }

    private SubDeptAnalysis buildSubDeptAnalysisWithCache(DeptDO dept, AnalysisContext ctx, boolean includeEmployees) {
        List<AdminUserDO> directEmployees = ctx.getDeptEmployeeCache().getOrDefault(dept.getId(), Collections.emptyList());

        BigDecimal totalContractIncome = BigDecimal.ZERO;
        BigDecimal totalEmployeeCost = BigDecimal.ZERO;
        int totalEmployeeCount = directEmployees.size();
        List<EmployeeAnalysis> employeeAnalysisList = new ArrayList<>();

        for (AdminUserDO employee : directEmployees) {
            EmployeeAnalysis empAnalysis = buildEmployeeAnalysisWithCache(employee, ctx, includeEmployees);
            if (empAnalysis != null) {
                totalContractIncome = totalContractIncome.add(
                        empAnalysis.getContractIncome() != null ? empAnalysis.getContractIncome() : BigDecimal.ZERO);
                totalEmployeeCost = totalEmployeeCost.add(
                        empAnalysis.getEmployeeCost() != null ? empAnalysis.getEmployeeCost() : BigDecimal.ZERO);
                if (includeEmployees) {
                    employeeAnalysisList.add(empAnalysis);
                }
            }
        }

        LocalDateTime cutoffDateTime = ctx.getCutoffDate().atTime(LocalTime.MAX);
        BigDecimal totalOutsideIncome = calculator.getDeptDirectOutsideIncome(dept.getId(), ctx.getYear(), cutoffDateTime);
        BigDecimal totalOutsideExpense = calculator.getDeptDirectOutsideExpense(dept.getId(), ctx.getYear(), cutoffDateTime);

        List<DeptDO> childDepts = ctx.getChildDeptCache().get(dept.getId());
        List<SubDeptAnalysis> childrenList = null;

        if (CollUtil.isNotEmpty(childDepts)) {
            List<DeptDO> sortedChildDepts = new ArrayList<>(childDepts);
            sortedChildDepts.sort(createDeptComparator());
            childrenList = new ArrayList<>();
            for (DeptDO childDept : sortedChildDepts) {
                SubDeptAnalysis childAnalysis = buildSubDeptAnalysisWithCache(childDept, ctx, includeEmployees);
                if (childAnalysis != null) {
                    childrenList.add(childAnalysis);
                    totalContractIncome = totalContractIncome.add(
                            childAnalysis.getContractIncome() != null ? childAnalysis.getContractIncome() : BigDecimal.ZERO);
                    totalEmployeeCost = totalEmployeeCost.add(
                            childAnalysis.getEmployeeCost() != null ? childAnalysis.getEmployeeCost() : BigDecimal.ZERO);
                    totalOutsideIncome = totalOutsideIncome.add(
                            childAnalysis.getOutsideIncome() != null ? childAnalysis.getOutsideIncome() : BigDecimal.ZERO);
                    totalOutsideExpense = totalOutsideExpense.add(
                            childAnalysis.getOutsideExpense() != null ? childAnalysis.getOutsideExpense() : BigDecimal.ZERO);
                    totalEmployeeCount += childAnalysis.getEmployeeCount() != null ? childAnalysis.getEmployeeCount() : 0;
                }
            }
        }

        BigDecimal totalIncome = totalContractIncome.add(totalOutsideIncome);
        BigDecimal totalExpense = totalEmployeeCost.add(totalOutsideExpense);
        BigDecimal netProfit = totalIncome.subtract(totalExpense);
        BigDecimal profitRate = calculator.calculateProfitRate(netProfit, totalIncome);

        return SubDeptAnalysis.builder()
                .deptId(dept.getId())
                .deptName(dept.getName())
                .employeeCount(totalEmployeeCount)
                .contractIncome(totalContractIncome)
                .outsideIncome(totalOutsideIncome)
                .totalIncome(totalIncome)
                .employeeCost(totalEmployeeCost)
                .outsideExpense(totalOutsideExpense)
                .totalExpense(totalExpense)
                .netProfit(netProfit)
                .profitRate(profitRate)
                .children(CollUtil.isNotEmpty(childrenList) ? childrenList : null)
                .employeeList(includeEmployees && CollUtil.isNotEmpty(employeeAnalysisList) ? employeeAnalysisList : null)
                .build();
    }

    private EmployeeAnalysis buildEmployeeAnalysisWithCache(AdminUserDO user, AnalysisContext ctx,
                                                            boolean includeParticipation) {
        BigDecimal employeeCost = ctx.getEmployeeCostCache().getOrDefault(user.getId(), BigDecimal.ZERO);

        Integer deptType = user.getDeptId() != null ? ctx.getDeptTypeCache().get(user.getDeptId()) : null;
        BigDecimal contractIncome = BigDecimal.ZERO;
        if (deptType != null) {
            contractIncome = contractIncome.add(
                    ctx.getOperationIncomeCache().getOrDefault(user.getId(), BigDecimal.ZERO));
            if (deptType == BusinessAnalysisCalculator.DEPT_TYPE_SECURITY_SERVICE
                    || deptType == BusinessAnalysisCalculator.DEPT_TYPE_DATA_SECURITY) {
                contractIncome = contractIncome.add(
                        ctx.getServiceIncomeCache().getOrDefault(user.getId(), BigDecimal.ZERO));
            }
        }

        BigDecimal outsideIncome = BigDecimal.ZERO;
        BigDecimal outsideExpense = BigDecimal.ZERO;
        BigDecimal totalIncome = contractIncome.add(outsideIncome);
        BigDecimal totalExpense = employeeCost.add(outsideExpense);
        BigDecimal netProfit = totalIncome.subtract(totalExpense);
        BigDecimal profitRate = calculator.calculateProfitRate(netProfit, totalIncome);

        boolean isLeader = ctx.getLeaderUserIds().contains(user.getId());
        DeptDO dept = user.getDeptId() != null ? ctx.getDeptCache().get(user.getDeptId()) : null;

        List<ProjectParticipation> participationList = null;
        if (includeParticipation) {
            participationList = calculator.getEmployeeParticipation(user, ctx.getYear(), ctx.getCutoffDate());
        }

        return EmployeeAnalysis.builder()
                .userId(user.getId())
                .userName(user.getNickname())
                .deptId(user.getDeptId())
                .deptName(dept != null ? dept.getName() : null)
                .position(user.getPosition())
                .positionLevel(user.getPositionLevel())
                .isLeader(isLeader)
                .contractIncome(contractIncome)
                .outsideIncome(outsideIncome)
                .totalIncome(totalIncome)
                .employeeCost(employeeCost)
                .outsideExpense(outsideExpense)
                .totalExpense(totalExpense)
                .netProfit(netProfit)
                .profitRate(profitRate)
                .participationList(participationList)
                .build();
    }

    // ========== 原始版分析构建（保留用于单独查询，不依赖批量缓存） ==========

    private DeptAnalysis buildDeptAnalysis(DeptDO dept, int year, LocalDate cutoffDate,
                                           boolean includeSubDepts, boolean includeEmployees) {
        Integer deptType = dept.getDeptType();
        if (deptType == null) {
            deptType = calculator.inferDeptTypeFromName(dept.getName());
        }

        List<AdminUserDO> employees = getEmployeesUnderDept(dept.getId());
        BigDecimal totalContractIncome = BigDecimal.ZERO;
        BigDecimal totalEmployeeCost = BigDecimal.ZERO;
        List<EmployeeAnalysis> employeeAnalysisList = new ArrayList<>();

        for (AdminUserDO employee : employees) {
            EmployeeAnalysis empAnalysis = buildEmployeeAnalysis(employee, year, cutoffDate, includeEmployees);
            if (empAnalysis != null) {
                totalContractIncome = totalContractIncome.add(
                        empAnalysis.getContractIncome() != null ? empAnalysis.getContractIncome() : BigDecimal.ZERO);
                totalEmployeeCost = totalEmployeeCost.add(
                        empAnalysis.getEmployeeCost() != null ? empAnalysis.getEmployeeCost() : BigDecimal.ZERO);
                if (includeEmployees) {
                    employeeAnalysisList.add(empAnalysis);
                }
            }
        }

        LocalDateTime cutoffDateTime = cutoffDate.atTime(LocalTime.MAX);
        BigDecimal totalOutsideIncome = calculator.calculateDeptOutsideIncome(dept.getId(), year, cutoffDateTime);
        BigDecimal totalOutsideExpense = calculator.calculateDeptOutsideExpense(dept.getId(), year, cutoffDateTime);

        BigDecimal totalIncome = totalContractIncome.add(totalOutsideIncome);
        BigDecimal totalExpense = totalEmployeeCost.add(totalOutsideExpense);
        BigDecimal netProfit = totalIncome.subtract(totalExpense);
        BigDecimal profitRate = calculator.calculateProfitRate(netProfit, totalIncome);

        List<SubDeptAnalysis> subDeptList = null;
        if (includeSubDepts) {
            subDeptList = buildSubDeptAnalysisList(dept.getId(), year, cutoffDate, includeEmployees);
        }

        List<EmployeeAnalysis> directEmployeeList = null;
        if (includeEmployees) {
            List<AdminUserDO> directEmployees =
                    adminUserService.getUserListByDeptIds(Collections.singleton(dept.getId()));
            if (CollUtil.isNotEmpty(directEmployees)) {
                directEmployeeList = new ArrayList<>();
                for (AdminUserDO employee : directEmployees) {
                    EmployeeAnalysis empAnalysis = buildEmployeeAnalysis(employee, year, cutoffDate, true);
                    if (empAnalysis != null) {
                        directEmployeeList.add(empAnalysis);
                    }
                }
            }
        }

        return DeptAnalysis.builder()
                .deptId(dept.getId())
                .deptName(dept.getName())
                .deptType(deptType)
                .deptTypeName(BusinessAnalysisCalculator.DEPT_TYPE_NAMES.get(deptType))
                .employeeCount(employees.size())
                .contractIncome(totalContractIncome)
                .outsideIncome(totalOutsideIncome)
                .totalIncome(totalIncome)
                .employeeCost(totalEmployeeCost)
                .outsideExpense(totalOutsideExpense)
                .totalExpense(totalExpense)
                .netProfit(netProfit)
                .profitRate(profitRate)
                .subDeptList(subDeptList)
                .employeeList(CollUtil.isNotEmpty(directEmployeeList) ? directEmployeeList : null)
                .build();
    }

    private List<SubDeptAnalysis> buildSubDeptAnalysisList(Long parentDeptId, int year, LocalDate cutoffDate,
                                                           boolean includeEmployees) {
        List<DeptDO> directChildren = deptMapper.selectListByParentId(Collections.singleton(parentDeptId));
        if (CollUtil.isEmpty(directChildren)) {
            return Collections.emptyList();
        }
        directChildren.sort(createDeptComparator());
        List<SubDeptAnalysis> result = new ArrayList<>();
        for (DeptDO subDept : directChildren) {
            SubDeptAnalysis subAnalysis = buildSubDeptAnalysis(subDept, year, cutoffDate, includeEmployees);
            if (subAnalysis != null) {
                result.add(subAnalysis);
            }
        }
        return result;
    }

    private SubDeptAnalysis buildSubDeptAnalysis(DeptDO dept, int year, LocalDate cutoffDate, boolean includeEmployees) {
        List<AdminUserDO> directEmployees =
                adminUserService.getUserListByDeptIds(Collections.singleton(dept.getId()));

        BigDecimal totalContractIncome = BigDecimal.ZERO;
        BigDecimal totalEmployeeCost = BigDecimal.ZERO;
        int totalEmployeeCount = directEmployees.size();
        List<EmployeeAnalysis> employeeAnalysisList = new ArrayList<>();

        for (AdminUserDO employee : directEmployees) {
            EmployeeAnalysis empAnalysis = buildEmployeeAnalysis(employee, year, cutoffDate, includeEmployees);
            if (empAnalysis != null) {
                totalContractIncome = totalContractIncome.add(
                        empAnalysis.getContractIncome() != null ? empAnalysis.getContractIncome() : BigDecimal.ZERO);
                totalEmployeeCost = totalEmployeeCost.add(
                        empAnalysis.getEmployeeCost() != null ? empAnalysis.getEmployeeCost() : BigDecimal.ZERO);
                if (includeEmployees) {
                    employeeAnalysisList.add(empAnalysis);
                }
            }
        }

        LocalDateTime cutoffDateTime = cutoffDate.atTime(LocalTime.MAX);
        BigDecimal totalOutsideIncome = calculator.getDeptDirectOutsideIncome(dept.getId(), year, cutoffDateTime);
        BigDecimal totalOutsideExpense = calculator.getDeptDirectOutsideExpense(dept.getId(), year, cutoffDateTime);

        List<DeptDO> childDepts = deptMapper.selectListByParentId(Collections.singleton(dept.getId()));
        List<SubDeptAnalysis> childrenList = null;

        if (CollUtil.isNotEmpty(childDepts)) {
            childDepts.sort(createDeptComparator());
            childrenList = new ArrayList<>();
            for (DeptDO childDept : childDepts) {
                SubDeptAnalysis childAnalysis = buildSubDeptAnalysis(childDept, year, cutoffDate, includeEmployees);
                if (childAnalysis != null) {
                    childrenList.add(childAnalysis);
                    totalContractIncome = totalContractIncome.add(
                            childAnalysis.getContractIncome() != null ? childAnalysis.getContractIncome() : BigDecimal.ZERO);
                    totalEmployeeCost = totalEmployeeCost.add(
                            childAnalysis.getEmployeeCost() != null ? childAnalysis.getEmployeeCost() : BigDecimal.ZERO);
                    totalOutsideIncome = totalOutsideIncome.add(
                            childAnalysis.getOutsideIncome() != null ? childAnalysis.getOutsideIncome() : BigDecimal.ZERO);
                    totalOutsideExpense = totalOutsideExpense.add(
                            childAnalysis.getOutsideExpense() != null ? childAnalysis.getOutsideExpense() : BigDecimal.ZERO);
                    totalEmployeeCount += childAnalysis.getEmployeeCount() != null ? childAnalysis.getEmployeeCount() : 0;
                }
            }
        }

        BigDecimal totalIncome = totalContractIncome.add(totalOutsideIncome);
        BigDecimal totalExpense = totalEmployeeCost.add(totalOutsideExpense);
        BigDecimal netProfit = totalIncome.subtract(totalExpense);
        BigDecimal profitRate = calculator.calculateProfitRate(netProfit, totalIncome);

        return SubDeptAnalysis.builder()
                .deptId(dept.getId())
                .deptName(dept.getName())
                .employeeCount(totalEmployeeCount)
                .contractIncome(totalContractIncome)
                .outsideIncome(totalOutsideIncome)
                .totalIncome(totalIncome)
                .employeeCost(totalEmployeeCost)
                .outsideExpense(totalOutsideExpense)
                .totalExpense(totalExpense)
                .netProfit(netProfit)
                .profitRate(profitRate)
                .children(CollUtil.isNotEmpty(childrenList) ? childrenList : null)
                .employeeList(includeEmployees && CollUtil.isNotEmpty(employeeAnalysisList) ? employeeAnalysisList : null)
                .build();
    }

    private EmployeeAnalysis buildEmployeeAnalysis(AdminUserDO user, int year, LocalDate cutoffDate,
                                                   boolean includeParticipation) {
        BigDecimal employeeCost = getEmployeeCost(user.getId(), year, cutoffDate);
        BigDecimal contractIncome = calculator.calculateEmployeeContractIncome(user, year, cutoffDate);
        BigDecimal outsideIncome = BigDecimal.ZERO;
        BigDecimal outsideExpense = BigDecimal.ZERO;

        BigDecimal totalIncome = contractIncome.add(outsideIncome);
        BigDecimal totalExpense = employeeCost.add(outsideExpense);
        BigDecimal netProfit = totalIncome.subtract(totalExpense);
        BigDecimal profitRate = calculator.calculateProfitRate(netProfit, totalIncome);

        List<ProjectParticipation> participationList = null;
        if (includeParticipation) {
            participationList = calculator.getEmployeeParticipation(user, year, cutoffDate);
        }

        DeptDO dept = user.getDeptId() != null ? deptService.getDept(user.getDeptId()) : null;
        boolean isLeader = false;
        if (dept != null && dept.getLeaderUserId() != null && dept.getLeaderUserId().equals(user.getId())) {
            isLeader = true;
        }
        if (!isLeader) {
            isLeader = CollUtil.isNotEmpty(deptService.getDeptListByLeaderUserId(user.getId()));
        }

        return EmployeeAnalysis.builder()
                .userId(user.getId())
                .userName(user.getNickname())
                .deptId(user.getDeptId())
                .deptName(dept != null ? dept.getName() : null)
                .position(user.getPosition())
                .positionLevel(user.getPositionLevel())
                .isLeader(isLeader)
                .contractIncome(contractIncome)
                .outsideIncome(outsideIncome)
                .totalIncome(totalIncome)
                .employeeCost(employeeCost)
                .outsideExpense(outsideExpense)
                .totalExpense(totalExpense)
                .netProfit(netProfit)
                .profitRate(profitRate)
                .participationList(participationList)
                .build();
    }

    // ========== 辅助方法（编排层内部使用） ==========

    private BigDecimal getEmployeeCost(Long userId, int year, LocalDate cutoffDate) {
        UserCostRespVO costVO = costCalculationService.getUserCost(userId, year, cutoffDate.getMonthValue());
        if (costVO == null) {
            return BigDecimal.ZERO;
        }
        return costVO.getYearToDateCost() != null ? costVO.getYearToDateCost() : BigDecimal.ZERO;
    }

    private List<AdminUserDO> getEmployeesUnderDept(Long deptId) {
        Set<Long> deptIds = new HashSet<>();
        deptIds.add(deptId);
        List<DeptDO> allDescendants = deptService.getChildDeptList(deptId);
        if (CollUtil.isNotEmpty(allDescendants)) {
            allDescendants.forEach(d -> deptIds.add(d.getId()));
        }
        return adminUserService.getUserListByDeptIds(deptIds);
    }

    private void collectChildDeptIdsWithCache(Long parentId, Set<Long> result,
                                              Map<Long, List<DeptDO>> childDeptCache) {
        List<DeptDO> children = childDeptCache.get(parentId);
        if (CollUtil.isNotEmpty(children)) {
            for (DeptDO child : children) {
                result.add(child.getId());
                collectChildDeptIdsWithCache(child.getId(), result, childDeptCache);
            }
        }
    }

    /**
     * 向上遍历部门层级获取部门类型（使用缓存加速）。
     */
    private Integer getDeptTypeFromHierarchyWithCache(Long deptId, Map<Long, DeptDO> deptCache,
                                                      Map<Long, Integer> deptTypeCache) {
        if (deptTypeCache.containsKey(deptId)) {
            return deptTypeCache.get(deptId);
        }
        DeptDO dept = deptCache.get(deptId);
        while (dept != null) {
            if (dept.getDeptType() != null) {
                return dept.getDeptType();
            }
            Integer inferred = calculator.inferDeptTypeFromName(dept.getName());
            if (inferred != null) {
                return inferred;
            }
            if (dept.getParentId() != null && dept.getParentId() > 0) {
                dept = deptCache.get(dept.getParentId());
            } else {
                break;
            }
        }
        return null;
    }

    /**
     * 部门排序：先按 sort 字段，再按名称自然排序（使 1班 < 2班 < 10班）。
     */
    private Comparator<DeptDO> createDeptComparator() {
        return (d1, d2) -> {
            Integer sort1 = d1.getSort();
            Integer sort2 = d2.getSort();
            if (sort1 != null && sort2 != null && !sort1.equals(sort2)) return sort1.compareTo(sort2);
            if (sort1 != null && sort2 == null) return -1;
            if (sort1 == null && sort2 != null) return 1;
            return naturalCompare(d1.getName(), d2.getName());
        };
    }

    private int naturalCompare(String s1, String s2) {
        if (s1 == null && s2 == null) return 0;
        if (s1 == null) return -1;
        if (s2 == null) return 1;
        int i1 = 0, i2 = 0;
        while (i1 < s1.length() && i2 < s2.length()) {
            char c1 = s1.charAt(i1);
            char c2 = s2.charAt(i2);
            if (Character.isDigit(c1) && Character.isDigit(c2)) {
                int num1 = 0, num2 = 0;
                while (i1 < s1.length() && Character.isDigit(s1.charAt(i1))) {
                    num1 = num1 * 10 + (s1.charAt(i1++) - '0');
                }
                while (i2 < s2.length() && Character.isDigit(s2.charAt(i2))) {
                    num2 = num2 * 10 + (s2.charAt(i2++) - '0');
                }
                if (num1 != num2) return Integer.compare(num1, num2);
            } else {
                if (c1 != c2) return Character.compare(c1, c2);
                i1++;
                i2++;
            }
        }
        return Integer.compare(s1.length() - i1, s2.length() - i2);
    }
}
