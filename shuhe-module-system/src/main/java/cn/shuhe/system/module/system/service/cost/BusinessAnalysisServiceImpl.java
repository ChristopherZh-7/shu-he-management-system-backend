package cn.shuhe.system.module.system.service.cost;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import cn.shuhe.system.module.system.controller.admin.cost.vo.BusinessAnalysisReqVO;
import cn.shuhe.system.module.system.controller.admin.cost.vo.BusinessAnalysisRespVO;
import cn.shuhe.system.module.system.controller.admin.cost.vo.BusinessAnalysisRespVO.*;
import cn.shuhe.system.module.system.controller.admin.cost.vo.UserCostPageReqVO;
import cn.shuhe.system.module.system.controller.admin.cost.vo.UserCostRespVO;
import cn.shuhe.system.module.system.dal.dataobject.dept.DeptDO;
import cn.shuhe.system.module.system.dal.dataobject.user.AdminUserDO;
import cn.shuhe.system.module.system.dal.mysql.cost.OutsideCostRecordMapper;
import cn.shuhe.system.module.system.dal.mysql.cost.ProjectSiteMemberInfoMapper;
import cn.shuhe.system.module.system.dal.mysql.cost.SecurityOperationContractInfoMapper;
import cn.shuhe.system.module.system.dal.mysql.cost.ServiceItemInfoMapper;
import cn.shuhe.system.module.system.dal.mysql.dept.DeptMapper;
import cn.shuhe.system.module.system.service.dept.DeptService;
import cn.shuhe.system.module.system.service.user.AdminUserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 经营分析 Service 实现类
 */
@Slf4j
@Service
public class BusinessAnalysisServiceImpl implements BusinessAnalysisService {

    /**
     * 部门类型常量
     */
    public static final int DEPT_TYPE_SECURITY_SERVICE = 1;  // 安全服务
    public static final int DEPT_TYPE_SECURITY_OPERATION = 2; // 安全运营
    public static final int DEPT_TYPE_DATA_SECURITY = 3;      // 数据安全

    private static final Map<Integer, String> DEPT_TYPE_NAMES = Map.of(
            DEPT_TYPE_SECURITY_SERVICE, "安全服务",
            DEPT_TYPE_SECURITY_OPERATION, "安全运营",
            DEPT_TYPE_DATA_SECURITY, "数据安全"
    );

    /**
     * 分析上下文：用于批量预加载数据，避免 N+1 查询
     */
    @Data
    @Builder
    private static class AnalysisContext {
        private int year;
        private LocalDate cutoffDate;
        
        // 部门缓存：deptId -> DeptDO
        private Map<Long, DeptDO> deptCache;
        
        // 部门子部门缓存：parentId -> List<DeptDO>
        private Map<Long, List<DeptDO>> childDeptCache;
        
        // 部门类型缓存：deptId -> deptType（包含从层级推断的类型）
        private Map<Long, Integer> deptTypeCache;
        
        // 员工成本缓存：userId -> 年累计成本
        private Map<Long, BigDecimal> employeeCostCache;
        
        // 部门负责人集合：所有是部门负责人的用户ID
        private Set<Long> leaderUserIds;
        
        // 员工缓存：deptId -> List<AdminUserDO>（直属员工）
        private Map<Long, List<AdminUserDO>> deptEmployeeCache;
        
        // 安全运营收入缓存：userId -> 收入
        private Map<Long, BigDecimal> operationIncomeCache;
        
        // 安全服务收入缓存：userId -> 收入
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
    private HolidayService holidayService;

    @Resource
    private OutsideCostRecordMapper outsideCostRecordMapper;

    @Resource
    private SecurityOperationContractInfoMapper securityOperationContractInfoMapper;

    @Resource
    private ProjectSiteMemberInfoMapper projectSiteMemberInfoMapper;

    @Resource
    private ServiceItemInfoMapper serviceItemInfoMapper;

    @Override
    public BusinessAnalysisRespVO getBusinessAnalysis(BusinessAnalysisReqVO reqVO, Long currentUserId) {
        long startTime = System.currentTimeMillis();
        
        // 1. 设置默认值
        LocalDate cutoffDate = reqVO.getCutoffDate() != null ? reqVO.getCutoffDate() : LocalDate.now();
        int year = reqVO.getYear() != null ? reqVO.getYear() : cutoffDate.getYear();

        // 2. 根据权限获取可查看的部门
        List<DeptDO> visibleDepts = getVisibleDepts(reqVO.getDeptId(), currentUserId);
        if (CollUtil.isEmpty(visibleDepts)) {
            return BusinessAnalysisRespVO.builder()
                    .year(year)
                    .cutoffDate(cutoffDate)
                    .deptAnalysisList(Collections.emptyList())
                    .total(buildEmptyTotal())
                    .build();
        }

        // 3. 【性能优化】批量预加载所有数据到缓存
        log.info("开始预加载数据...");
        long preloadStart = System.currentTimeMillis();
        AnalysisContext context = buildAnalysisContext(year, cutoffDate, visibleDepts);
        log.info("预加载完成，耗时: {}ms", System.currentTimeMillis() - preloadStart);

        // 4. 计算每个部门的分析数据（使用缓存）
        List<DeptAnalysis> deptAnalysisList = new ArrayList<>();
        boolean includeSubDepts = reqVO.getLevel() != null && reqVO.getLevel() >= 2;
        boolean includeEmployees = Boolean.TRUE.equals(reqVO.getIncludeEmployees()) || 
                                   (reqVO.getLevel() != null && reqVO.getLevel() >= 3);

        for (DeptDO dept : visibleDepts) {
            DeptAnalysis analysis = buildDeptAnalysisWithCache(dept, context, includeSubDepts, includeEmployees);
            if (analysis != null) {
                deptAnalysisList.add(analysis);
            }
        }

        // 5. 计算总计
        TotalAnalysis total = calculateTotal(deptAnalysisList);

        log.info("经营分析计算完成，总耗时: {}ms", System.currentTimeMillis() - startTime);
        
        return BusinessAnalysisRespVO.builder()
                .year(year)
                .cutoffDate(cutoffDate)
                .deptAnalysisList(deptAnalysisList)
                .total(total)
                .build();
    }
    
    /**
     * 【性能优化】构建分析上下文，批量预加载所有需要的数据
     */
    private AnalysisContext buildAnalysisContext(int year, LocalDate cutoffDate, List<DeptDO> visibleDepts) {
        // 1. 加载所有部门
        List<DeptDO> allDepts = deptService.getDeptList(new cn.shuhe.system.module.system.controller.admin.dept.vo.dept.DeptListReqVO());
        Map<Long, DeptDO> deptCache = allDepts.stream()
                .collect(Collectors.toMap(DeptDO::getId, d -> d, (a, b) -> a));
        
        // 2. 构建子部门缓存
        Map<Long, List<DeptDO>> childDeptCache = allDepts.stream()
                .filter(d -> d.getParentId() != null && d.getParentId() > 0)
                .collect(Collectors.groupingBy(DeptDO::getParentId));
        
        // 3. 构建部门类型缓存（包含从层级推断的类型）
        Map<Long, Integer> deptTypeCache = new ConcurrentHashMap<>();
        for (DeptDO dept : allDepts) {
            Integer type = dept.getDeptType();
            if (type == null) {
                type = inferDeptTypeFromName(dept.getName());
            }
            if (type == null) {
                // 向上查找父部门类型
                type = getDeptTypeFromHierarchyWithCache(dept.getId(), deptCache, deptTypeCache);
            }
            if (type != null) {
                deptTypeCache.put(dept.getId(), type);
            }
        }
        
        // 4. 收集所有相关部门ID（包括子部门）
        Set<Long> allRelevantDeptIds = new HashSet<>();
        for (DeptDO dept : visibleDepts) {
            allRelevantDeptIds.add(dept.getId());
            collectChildDeptIdsWithCache(dept.getId(), allRelevantDeptIds, childDeptCache);
        }
        
        // 5. 批量加载所有员工
        List<AdminUserDO> allEmployees = adminUserService.getUserListByDeptIds(allRelevantDeptIds);
        Map<Long, List<AdminUserDO>> deptEmployeeCache = allEmployees.stream()
                .filter(u -> u.getDeptId() != null)
                .collect(Collectors.groupingBy(AdminUserDO::getDeptId));
        
        // 6. 收集所有部门负责人ID
        Set<Long> leaderUserIds = allDepts.stream()
                .filter(d -> d.getLeaderUserId() != null)
                .map(DeptDO::getLeaderUserId)
                .collect(Collectors.toSet());
        
        // 7. 批量计算所有员工成本
        Map<Long, BigDecimal> employeeCostCache = new ConcurrentHashMap<>();
        int month = cutoffDate.getMonthValue();
        for (AdminUserDO employee : allEmployees) {
            UserCostRespVO costVO = costCalculationService.getUserCost(employee.getId(), year, month);
            BigDecimal cost = (costVO != null && costVO.getYearToDateCost() != null) 
                    ? costVO.getYearToDateCost() : BigDecimal.ZERO;
            employeeCostCache.put(employee.getId(), cost);
        }
        
        // 8. 批量计算收入（驻场收入 + 轮次收入）
        Map<Long, BigDecimal> operationIncomeCache = new ConcurrentHashMap<>();  // 驻场收入缓存
        Map<Long, BigDecimal> serviceIncomeCache = new ConcurrentHashMap<>();    // 轮次收入缓存
        
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDateTime cutoffDateTime = LocalDateTime.of(cutoffDate, LocalTime.MAX);
        
        for (AdminUserDO employee : allEmployees) {
            Long userId = employee.getId();
            Long deptId = employee.getDeptId();
            Integer deptType = deptId != null ? deptTypeCache.get(deptId) : null;
            
            if (deptType != null) {
                // 所有部门类型都可能有驻场收入（从 project_site_member 表）
                BigDecimal onsiteIncome = calculateOnsiteIncome(userId, deptType, year, cutoffDate);
                operationIncomeCache.put(userId, onsiteIncome);
                
                // 安全服务和数据安全还可能有轮次收入
                if (deptType == DEPT_TYPE_SECURITY_SERVICE || deptType == DEPT_TYPE_DATA_SECURITY) {
                    BigDecimal roundIncome = calculateServiceRoundIncome(userId, year, cutoffDate);
                    serviceIncomeCache.put(userId, roundIncome);
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
    
    /**
     * 使用缓存递归收集子部门ID
     */
    private void collectChildDeptIdsWithCache(Long parentId, Set<Long> result, Map<Long, List<DeptDO>> childDeptCache) {
        List<DeptDO> children = childDeptCache.get(parentId);
        if (CollUtil.isNotEmpty(children)) {
            for (DeptDO child : children) {
                result.add(child.getId());
                collectChildDeptIdsWithCache(child.getId(), result, childDeptCache);
            }
        }
    }
    
    /**
     * 使用缓存从部门层级获取部门类型
     */
    private Integer getDeptTypeFromHierarchyWithCache(Long deptId, Map<Long, DeptDO> deptCache, 
                                                       Map<Long, Integer> deptTypeCache) {
        // 先检查缓存
        if (deptTypeCache.containsKey(deptId)) {
            return deptTypeCache.get(deptId);
        }
        
        DeptDO dept = deptCache.get(deptId);
        while (dept != null) {
            if (dept.getDeptType() != null) {
                return dept.getDeptType();
            }
            Integer inferred = inferDeptTypeFromName(dept.getName());
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

    /**
     * 根据权限获取可查看的部门列表
     * 只返回三个顶级业务部门，避免重复统计
     */
    private List<DeptDO> getVisibleDepts(Long requestedDeptId, Long currentUserId) {
        // 获取当前用户信息
        AdminUserDO currentUser = adminUserService.getUser(currentUserId);
        if (currentUser == null) {
            return Collections.emptyList();
        }

        // TODO: 判断是否是总经理或人事（可以看所有）
        // 暂时简化：如果请求了特定部门就只返回该部门，否则返回三个顶级业务部门
        
        if (requestedDeptId != null) {
            DeptDO dept = deptService.getDept(requestedDeptId);
            return dept != null ? List.of(dept) : Collections.emptyList();
        }

        // 获取三个顶级业务部门（parentId = 0 或父部门是公司根部门）
        // 顶级业务部门的特征：有 deptType 且是顶级部门（没有其他业务部门作为父部门）
        List<DeptDO> allDepts = deptService.getDeptList(new cn.shuhe.system.module.system.controller.admin.dept.vo.dept.DeptListReqVO());
        
        // 收集所有有业务类型的部门ID
        Set<Long> businessDeptIds = allDepts.stream()
                .filter(dept -> {
                    Integer type = dept.getDeptType();
                    if (type == null) {
                        type = inferDeptTypeFromName(dept.getName());
                    }
                    return type != null && (type == DEPT_TYPE_SECURITY_SERVICE
                            || type == DEPT_TYPE_SECURITY_OPERATION
                            || type == DEPT_TYPE_DATA_SECURITY);
                })
                .map(DeptDO::getId)
                .collect(Collectors.toSet());
        
        // 只返回顶级业务部门：其父部门不在业务部门集合中
        return allDepts.stream()
                .filter(dept -> {
                    Integer type = dept.getDeptType();
                    if (type == null) {
                        type = inferDeptTypeFromName(dept.getName());
                    }
                    if (type == null || (type != DEPT_TYPE_SECURITY_SERVICE
                            && type != DEPT_TYPE_SECURITY_OPERATION
                            && type != DEPT_TYPE_DATA_SECURITY)) {
                        return false;
                    }
                    // 父部门不是业务部门，说明这是顶级业务部门
                    Long parentId = dept.getParentId();
                    return parentId == null || parentId == 0L || !businessDeptIds.contains(parentId);
                })
                .collect(Collectors.toList());
    }

    // ========== 使用缓存的优化版本方法 ==========
    
    /**
     * 【优化版】构建部门分析数据（使用缓存）
     */
    private DeptAnalysis buildDeptAnalysisWithCache(DeptDO dept, AnalysisContext ctx,
                                                     boolean includeSubDepts, boolean includeEmployees) {
        Integer deptType = ctx.getDeptTypeCache().get(dept.getId());
        if (deptType == null) {
            deptType = inferDeptTypeFromName(dept.getName());
        }

        // 获取部门下所有员工（包括子部门）- 使用缓存
        List<AdminUserDO> employees = getEmployeesUnderDeptWithCache(dept.getId(), ctx);
        
        // 计算部门汇总（合同收入和员工成本）
        BigDecimal totalContractIncome = BigDecimal.ZERO;
        BigDecimal totalEmployeeCost = BigDecimal.ZERO;

        // 计算每个员工的数据并汇总
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

        // 计算部门的跨部门收入/支出（包括本部门和所有子部门）
        LocalDateTime cutoffDateTime = ctx.getCutoffDate().atTime(LocalTime.MAX);
        BigDecimal totalOutsideIncome = calculateDeptOutsideIncome(dept.getId(), ctx.getYear(), cutoffDateTime);
        BigDecimal totalOutsideExpense = calculateDeptOutsideExpense(dept.getId(), ctx.getYear(), cutoffDateTime);

        // 计算利润
        BigDecimal totalIncome = totalContractIncome.add(totalOutsideIncome);
        BigDecimal totalExpense = totalEmployeeCost.add(totalOutsideExpense);
        BigDecimal netProfit = totalIncome.subtract(totalExpense);
        BigDecimal profitRate = calculateProfitRate(netProfit, totalIncome);

        // 构建子部门列表
        List<SubDeptAnalysis> subDeptList = null;
        if (includeSubDepts) {
            subDeptList = buildSubDeptAnalysisListWithCache(dept.getId(), ctx, includeEmployees);
        }

        // 获取直属员工
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
                .deptTypeName(DEPT_TYPE_NAMES.get(deptType))
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
    
    /**
     * 【优化版】使用缓存获取部门下所有员工
     */
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
    
    /**
     * 【优化版】构建子部门分析列表（使用缓存）
     */
    private List<SubDeptAnalysis> buildSubDeptAnalysisListWithCache(Long parentDeptId, AnalysisContext ctx,
                                                                     boolean includeEmployees) {
        List<DeptDO> directChildren = ctx.getChildDeptCache().get(parentDeptId);
        if (CollUtil.isEmpty(directChildren)) {
            return Collections.emptyList();
        }
        
        // 排序
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
    
    /**
     * 【优化版】构建子部门分析（使用缓存）
     */
    private SubDeptAnalysis buildSubDeptAnalysisWithCache(DeptDO dept, AnalysisContext ctx, boolean includeEmployees) {
        List<AdminUserDO> directEmployees = ctx.getDeptEmployeeCache().get(dept.getId());
        if (directEmployees == null) {
            directEmployees = Collections.emptyList();
        }
        
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

        // 计算本部门直属的跨部门收支（不包括子部门）
        LocalDateTime cutoffDateTime = ctx.getCutoffDate().atTime(LocalTime.MAX);
        BigDecimal directOutsideIncome = outsideCostRecordMapper.sumIncomeByDeptIdAndYear(dept.getId(), ctx.getYear(), cutoffDateTime);
        BigDecimal directOutsideExpense = outsideCostRecordMapper.sumExpenseByDeptIdAndYear(dept.getId(), ctx.getYear(), cutoffDateTime);
        if (directOutsideIncome == null) directOutsideIncome = BigDecimal.ZERO;
        if (directOutsideExpense == null) directOutsideExpense = BigDecimal.ZERO;
        
        BigDecimal totalOutsideIncome = directOutsideIncome;
        BigDecimal totalOutsideExpense = directOutsideExpense;

        // 递归处理子部门
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
        BigDecimal profitRate = calculateProfitRate(netProfit, totalIncome);

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
    
    /**
     * 【优化版】构建员工分析（使用缓存）
     */
    private EmployeeAnalysis buildEmployeeAnalysisWithCache(AdminUserDO user, AnalysisContext ctx,
                                                             boolean includeParticipation) {
        // 从缓存获取员工成本
        BigDecimal employeeCost = ctx.getEmployeeCostCache().getOrDefault(user.getId(), BigDecimal.ZERO);

        // 从缓存获取员工收入
        // operationIncomeCache 现在存储驻场收入，serviceIncomeCache 存储轮次收入
        Integer deptType = user.getDeptId() != null ? ctx.getDeptTypeCache().get(user.getDeptId()) : null;
        BigDecimal contractIncome = BigDecimal.ZERO;
        if (deptType != null) {
            // 驻场收入（所有部门类型都可能有）
            BigDecimal onsiteIncome = ctx.getOperationIncomeCache().getOrDefault(user.getId(), BigDecimal.ZERO);
            contractIncome = contractIncome.add(onsiteIncome);
            
            // 轮次收入（仅安全服务和数据安全）
            if (deptType == DEPT_TYPE_SECURITY_SERVICE || deptType == DEPT_TYPE_DATA_SECURITY) {
                BigDecimal roundIncome = ctx.getServiceIncomeCache().getOrDefault(user.getId(), BigDecimal.ZERO);
                contractIncome = contractIncome.add(roundIncome);
            }
        }

        BigDecimal outsideIncome = BigDecimal.ZERO;
        BigDecimal outsideExpense = BigDecimal.ZERO;

        BigDecimal totalIncome = contractIncome.add(outsideIncome);
        BigDecimal totalExpense = employeeCost.add(outsideExpense);
        BigDecimal netProfit = totalIncome.subtract(totalExpense);
        BigDecimal profitRate = calculateProfitRate(netProfit, totalIncome);

        // 从缓存判断是否是管理人员
        boolean isLeader = ctx.getLeaderUserIds().contains(user.getId());

        // 从缓存获取部门信息
        DeptDO dept = user.getDeptId() != null ? ctx.getDeptCache().get(user.getDeptId()) : null;

        List<ProjectParticipation> participationList = null;
        if (includeParticipation) {
            participationList = getEmployeeParticipation(user, ctx.getYear(), ctx.getCutoffDate());
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
    
    // ========== 原有方法（保留用于单独查询） ==========

    /**
     * 构建部门分析数据
     */
    private DeptAnalysis buildDeptAnalysis(DeptDO dept, int year, LocalDate cutoffDate,
                                           boolean includeSubDepts, boolean includeEmployees) {
        Integer deptType = dept.getDeptType();
        if (deptType == null) {
            deptType = inferDeptTypeFromName(dept.getName());
        }

        // 获取部门下所有员工（包括子部门）
        List<AdminUserDO> employees = getEmployeesUnderDept(dept.getId());
        
        // 计算部门汇总
        BigDecimal totalContractIncome = BigDecimal.ZERO;
        BigDecimal totalEmployeeCost = BigDecimal.ZERO;

        // 计算每个员工的数据并汇总（合同收入和员工成本）
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

        // 计算部门的跨部门收入/支出（包括本部门和所有子部门）
        LocalDateTime cutoffDateTime = cutoffDate.atTime(LocalTime.MAX);
        BigDecimal totalOutsideIncome = calculateDeptOutsideIncome(dept.getId(), year, cutoffDateTime);
        BigDecimal totalOutsideExpense = calculateDeptOutsideExpense(dept.getId(), year, cutoffDateTime);

        // 计算利润
        BigDecimal totalIncome = totalContractIncome.add(totalOutsideIncome);
        BigDecimal totalExpense = totalEmployeeCost.add(totalOutsideExpense);
        BigDecimal netProfit = totalIncome.subtract(totalExpense);
        BigDecimal profitRate = calculateProfitRate(netProfit, totalIncome);

        // 构建子部门列表
        List<SubDeptAnalysis> subDeptList = null;
        if (includeSubDepts) {
            subDeptList = buildSubDeptAnalysisList(dept.getId(), year, cutoffDate, includeEmployees);
        }

        // 获取直属员工（只有该部门直接下属的员工，不包括子部门的）
        List<EmployeeAnalysis> directEmployeeList = null;
        if (includeEmployees) {
            // 获取直属于该部门的员工（deptId = dept.getId()）
            List<AdminUserDO> directEmployees = adminUserService.getUserListByDeptIds(Collections.singleton(dept.getId()));
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
                .deptTypeName(DEPT_TYPE_NAMES.get(deptType))
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

    /**
     * 构建子部门分析列表（只获取直接子部门，不递归获取所有后代）
     */
    private List<SubDeptAnalysis> buildSubDeptAnalysisList(Long parentDeptId, int year, LocalDate cutoffDate,
                                                           boolean includeEmployees) {
        // 只获取直接子部门（不递归获取所有后代）
        List<DeptDO> directChildren = deptMapper.selectListByParentId(Collections.singleton(parentDeptId));
        if (CollUtil.isEmpty(directChildren)) {
            return Collections.emptyList();
        }
        
        // 按 sort 字段和名称自然排序，确保一班、二班、三班等按正确顺序显示
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
    
    /**
     * 创建部门比较器，支持自然排序（按名称中的数字排序）
     * 优先按 sort 字段排序，sort 相同时按名称自然排序
     */
    private Comparator<DeptDO> createDeptComparator() {
        return (d1, d2) -> {
            // 优先按 sort 字段排序
            Integer sort1 = d1.getSort();
            Integer sort2 = d2.getSort();
            if (sort1 != null && sort2 != null && !sort1.equals(sort2)) {
                return sort1.compareTo(sort2);
            }
            if (sort1 != null && sort2 == null) {
                return -1;
            }
            if (sort1 == null && sort2 != null) {
                return 1;
            }
            // sort 相同或都为空时，按名称自然排序
            return naturalCompare(d1.getName(), d2.getName());
        };
    }
    
    /**
     * 自然排序比较器：按名称中的数字正确排序
     * 例如：1班 < 2班 < 3班 < 10班
     */
    private int naturalCompare(String s1, String s2) {
        if (s1 == null && s2 == null) return 0;
        if (s1 == null) return -1;
        if (s2 == null) return 1;
        
        int i1 = 0, i2 = 0;
        while (i1 < s1.length() && i2 < s2.length()) {
            char c1 = s1.charAt(i1);
            char c2 = s2.charAt(i2);
            
            if (Character.isDigit(c1) && Character.isDigit(c2)) {
                // 提取两个字符串中的数字
                int num1 = 0, num2 = 0;
                while (i1 < s1.length() && Character.isDigit(s1.charAt(i1))) {
                    num1 = num1 * 10 + (s1.charAt(i1) - '0');
                    i1++;
                }
                while (i2 < s2.length() && Character.isDigit(s2.charAt(i2))) {
                    num2 = num2 * 10 + (s2.charAt(i2) - '0');
                    i2++;
                }
                if (num1 != num2) {
                    return Integer.compare(num1, num2);
                }
            } else {
                if (c1 != c2) {
                    return Character.compare(c1, c2);
                }
                i1++;
                i2++;
            }
        }
        return Integer.compare(s1.length() - i1, s2.length() - i2);
    }

    /**
     * 构建子部门分析（递归支持多层嵌套）
     */
    private SubDeptAnalysis buildSubDeptAnalysis(DeptDO dept, int year, LocalDate cutoffDate, boolean includeEmployees) {
        // 获取该子部门的直属员工
        List<AdminUserDO> directEmployees = adminUserService.getUserListByDeptIds(Collections.singleton(dept.getId()));
        
        BigDecimal totalContractIncome = BigDecimal.ZERO;
        BigDecimal totalEmployeeCost = BigDecimal.ZERO;
        int totalEmployeeCount = directEmployees.size();

        // 计算直属员工的数据（合同收入和员工成本）
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

        // 计算本部门直属的跨部门收支（不包括子部门）
        LocalDateTime cutoffDateTime = cutoffDate.atTime(LocalTime.MAX);
        BigDecimal directOutsideIncome = outsideCostRecordMapper.sumIncomeByDeptIdAndYear(dept.getId(), year, cutoffDateTime);
        BigDecimal directOutsideExpense = outsideCostRecordMapper.sumExpenseByDeptIdAndYear(dept.getId(), year, cutoffDateTime);
        if (directOutsideIncome == null) directOutsideIncome = BigDecimal.ZERO;
        if (directOutsideExpense == null) directOutsideExpense = BigDecimal.ZERO;
        
        BigDecimal totalOutsideIncome = directOutsideIncome;
        BigDecimal totalOutsideExpense = directOutsideExpense;

        // 递归获取直接子部门（不获取所有后代）
        List<DeptDO> childDepts = deptMapper.selectListByParentId(Collections.singleton(dept.getId()));
        List<SubDeptAnalysis> childrenList = null;
        
        if (CollUtil.isNotEmpty(childDepts)) {
            // 按 sort 字段和名称自然排序，确保一班、二班、三班等按正确顺序显示
            childDepts.sort(createDeptComparator());
            
            childrenList = new ArrayList<>();
            for (DeptDO childDept : childDepts) {
                SubDeptAnalysis childAnalysis = buildSubDeptAnalysis(childDept, year, cutoffDate, includeEmployees);
                if (childAnalysis != null) {
                    childrenList.add(childAnalysis);
                    // 汇总子部门的数据到当前部门
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
        BigDecimal profitRate = calculateProfitRate(netProfit, totalIncome);

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

    /**
     * 构建员工分析
     */
    private EmployeeAnalysis buildEmployeeAnalysis(AdminUserDO user, int year, LocalDate cutoffDate,
                                                    boolean includeParticipation) {
        // 1. 获取员工成本
        BigDecimal employeeCost = getEmployeeCost(user.getId(), year, cutoffDate);

        // 2. 计算员工合同收入
        BigDecimal contractIncome = calculateEmployeeContractIncome(user, year, cutoffDate);

        // 3. 计算跨部门收入/支出（暂时设为0，后续可以细化）
        BigDecimal outsideIncome = BigDecimal.ZERO;
        BigDecimal outsideExpense = BigDecimal.ZERO;

        // 4. 计算利润
        BigDecimal totalIncome = contractIncome.add(outsideIncome);
        BigDecimal totalExpense = employeeCost.add(outsideExpense);
        BigDecimal netProfit = totalIncome.subtract(totalExpense);
        BigDecimal profitRate = calculateProfitRate(netProfit, totalIncome);

        // 5. 获取参与明细
        List<ProjectParticipation> participationList = null;
        if (includeParticipation) {
            participationList = getEmployeeParticipation(user, year, cutoffDate);
        }

        // 获取部门信息
        DeptDO dept = user.getDeptId() != null ? deptService.getDept(user.getDeptId()) : null;
        
        // 判断是否是管理人员（检查是否是任何部门的负责人）
        boolean isLeader = false;
        if (dept != null) {
            // 检查是否是当前部门的负责人
            if (dept.getLeaderUserId() != null && dept.getLeaderUserId().equals(user.getId())) {
                isLeader = true;
            }
        }
        // 也可以检查是否是其他部门的负责人
        if (!isLeader) {
            List<DeptDO> leaderDepts = deptService.getDeptListByLeaderUserId(user.getId());
            if (CollUtil.isNotEmpty(leaderDepts)) {
                isLeader = true;
            }
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

    /**
     * 计算员工合同收入
     * 
     * 收入来源：
     * 1. 驻场收入：从 project_site_member 表，按工作日分摊驻场费
     *    - 安全运营驻场 (dept_type=2)
     *    - 安全服务驻场 (dept_type=1)
     *    - 数据安全驻场 (dept_type=3)
     * 2. 二线服务收入：从 project_round 表，按轮次执行分摊
     *    - 安全服务二线
     *    - 数据安全（非驻场）
     */
    private BigDecimal calculateEmployeeContractIncome(AdminUserDO user, int year, LocalDate cutoffDate) {
        BigDecimal totalIncome = BigDecimal.ZERO;

        // 获取用户所属部门类型
        DeptDO dept = user.getDeptId() != null ? deptService.getDept(user.getDeptId()) : null;
        Integer deptType = null;
        if (dept != null) {
            deptType = dept.getDeptType();
            if (deptType == null) {
                // 向上查找父部门的类型
                deptType = getDeptTypeFromHierarchy(dept.getId());
            }
        }

        if (deptType == null) {
            return totalIncome;
        }

        // 1. 计算驻场收入（从新的 project_site_member 表）
        BigDecimal onsiteIncome = calculateOnsiteIncome(user.getId(), deptType, year, cutoffDate);
        totalIncome = totalIncome.add(onsiteIncome);

        // 2. 安全服务和数据安全还可能有二线服务收入（从轮次执行）
        if (deptType == DEPT_TYPE_SECURITY_SERVICE || deptType == DEPT_TYPE_DATA_SECURITY) {
            BigDecimal roundIncome = calculateServiceRoundIncome(user.getId(), year, cutoffDate);
            totalIncome = totalIncome.add(roundIncome);
        }

        return totalIncome;
    }

    /**
     * 计算驻场收入（通用方法，支持所有部门类型）
     * 
     * 从 project_site_member 表查询驻场参与记录，按工作日分摊驻场费
     */
    private BigDecimal calculateOnsiteIncome(Long userId, Integer deptType, int year, LocalDate cutoffDate) {
        // 查询该用户在该部门类型下的所有驻场记录
        List<Map<String, Object>> participations = projectSiteMemberInfoMapper.selectMemberParticipation(userId, deptType);
        if (CollUtil.isEmpty(participations)) {
            log.debug("[驻场收入] 用户 {} (deptType={}) 未参与任何驻场", userId, deptType);
            return BigDecimal.ZERO;
        }

        log.debug("[驻场收入] 用户 {} (deptType={}) 参与 {} 个驻场", userId, deptType, participations.size());
        
        BigDecimal totalIncome = BigDecimal.ZERO;
        LocalDate yearStart = LocalDate.of(year, 1, 1);

        for (Map<String, Object> participation : participations) {
            BigDecimal income = calculateSingleOnsiteParticipationIncome(participation, yearStart, cutoffDate);
            if (income.compareTo(BigDecimal.ZERO) > 0) {
                log.debug("[驻场收入] 用户 {} 项目 {} 驻场点 {} 收入: {}", 
                        userId, participation.get("projectName"), participation.get("siteName"), income);
            }
            totalIncome = totalIncome.add(income);
        }

        log.debug("[驻场收入] 用户 {} 总驻场收入: {}", userId, totalIncome);
        return totalIncome;
    }

    /**
     * 计算单个驻场参与的收入（通用方法）
     */
    private BigDecimal calculateSingleOnsiteParticipationIncome(Map<String, Object> participation,
                                                                 LocalDate yearStart, LocalDate cutoffDate) {
        // 获取成员类型：1-管理人员 2-驻场人员
        Integer memberType = getIntValue(participation.get("memberType"));
        Integer deptType = getIntValue(participation.get("deptType"));
        LocalDate memberStartDate = getLocalDate(participation.get("memberStartDate"));
        LocalDate memberEndDate = getLocalDate(participation.get("memberEndDate"));
        LocalDate contractStartDate = getLocalDateFromDateTime(participation.get("contractStartDate"));
        LocalDate contractEndDate = getLocalDateFromDateTime(participation.get("contractEndDate"));
        BigDecimal managementFee = getBigDecimal(participation.get("managementFee"));
        BigDecimal onsiteFee = getBigDecimal(participation.get("onsiteFee"));
        Integer sameMemberTypeCount = getIntValue(participation.get("sameMemberTypeCount"));

        String projectName = (String) participation.get("projectName");
        String siteName = (String) participation.get("siteName");
        String memberTypeName = memberType != null && memberType == 1 ? "管理" : "驻场";
        
        log.debug("[驻场收入计算] 项目={}, 驻场点={}, deptType={}, 成员类型={}, 管理费={}, 驻场费={}, 同类型成员数={}", 
                projectName, siteName, deptType, memberTypeName, managementFee, onsiteFee, sameMemberTypeCount);

        if (contractStartDate == null || contractEndDate == null) {
            log.debug("[驻场收入计算] 项目={} 合同开始或结束日期为空，跳过", projectName);
            return BigDecimal.ZERO;
        }

        // 根据成员类型选择对应的费用
        BigDecimal feePool;
        if (memberType != null && memberType == 1) {
            feePool = managementFee;
        } else {
            feePool = onsiteFee;
        }
        
        if (feePool.compareTo(BigDecimal.ZERO) <= 0) {
            log.debug("[驻场收入计算] 项目={} 费用池为空或<=0", projectName);
            return BigDecimal.ZERO;
        }

        // 计算合同总工作日
        int totalContractDays = calculateWorkingDaysBetween(contractStartDate, contractEndDate);
        if (totalContractDays <= 0) {
            log.debug("[驻场收入计算] 项目={} 总工作日<=0", projectName);
            return BigDecimal.ZERO;
        }

        // 计算该员工的有效工作日
        LocalDate effectiveStart = maxDate(memberStartDate != null ? memberStartDate : contractStartDate, 
                                           contractStartDate, yearStart);
        LocalDate effectiveEnd = minDate(memberEndDate != null ? memberEndDate : contractEndDate,
                                         contractEndDate, cutoffDate);

        if (effectiveStart == null || effectiveEnd == null || effectiveStart.isAfter(effectiveEnd)) {
            log.debug("[驻场收入计算] 项目={} 有效日期范围无效", projectName);
            return BigDecimal.ZERO;
        }

        int employeeDays = calculateWorkingDaysBetween(effectiveStart, effectiveEnd);

        // 按同类型成员人数平均分
        int memberCount = sameMemberTypeCount != null && sameMemberTypeCount > 0 ? sameMemberTypeCount : 1;
        
        BigDecimal income = feePool.multiply(new BigDecimal(employeeDays))
                .divide(new BigDecimal(totalContractDays), 4, RoundingMode.HALF_UP)
                .divide(new BigDecimal(memberCount), 2, RoundingMode.HALF_UP);
        
        log.debug("[驻场收入计算] 项目={}, 收入: {} × ({}/{}) / {} = {}", 
                projectName, feePool, employeeDays, totalContractDays, memberCount, income);
        
        return income;
    }

    /**
     * 计算安全运营员工收入
     */
    private BigDecimal calculateSecurityOperationIncome(Long userId, int year, LocalDate cutoffDate) {
        // 查询该用户参与的所有安全运营合同（作为管理人员或驻场人员）
        List<Map<String, Object>> participations = securityOperationContractInfoMapper.selectMemberParticipation(userId);
        if (CollUtil.isEmpty(participations)) {
            log.debug("[安全运营收入] 用户 {} 未参与任何安全运营合同", userId);
            return BigDecimal.ZERO;
        }

        log.debug("[安全运营收入] 用户 {} 参与 {} 个安全运营合同", userId, participations.size());
        
        BigDecimal totalIncome = BigDecimal.ZERO;
        LocalDate yearStart = LocalDate.of(year, 1, 1);

        for (Map<String, Object> participation : participations) {
            BigDecimal income = calculateSingleOperationParticipationIncome(participation, yearStart, cutoffDate);
            if (income.compareTo(BigDecimal.ZERO) > 0) {
                log.debug("[安全运营收入] 用户 {} 合同 {} 收入: {}", 
                        userId, participation.get("contractName"), income);
            }
            totalIncome = totalIncome.add(income);
        }

        log.debug("[安全运营收入] 用户 {} 总收入: {}", userId, totalIncome);
        return totalIncome;
    }

    /**
     * 计算单个安全运营参与的收入
     * 
     * 计算逻辑（已修正）：
     * 1. 以合同的开始/结束时间为准计算总工作日
     * 2. 计算截至今天（或指定截止日期）的工作日
     * 3. 根据 memberType 决定使用管理费还是驻场费：
     *    - memberType = 1（管理人员）：使用管理费
     *    - memberType = 2（驻场人员）：使用驻场费
     * 4. 按同类型成员人数平均分
     * 
     * 收入计算公式：
     * - 管理人员：管理费 × (截至今天工作日 / 总工作日) / 该合同管理人员数
     * - 驻场人员：驻场费 × (截至今天工作日 / 总工作日) / 该驻场点驻场人员数
     */
    private BigDecimal calculateSingleOperationParticipationIncome(Map<String, Object> participation,
                                                                    LocalDate yearStart, LocalDate cutoffDate) {
        // 获取成员类型：1-管理人员 2-驻场人员
        Integer memberType = getIntValue(participation.get("memberType"));
        // 获取成员日期
        LocalDate memberStartDate = getLocalDate(participation.get("memberStartDate"));
        LocalDate memberEndDate = getLocalDate(participation.get("memberEndDate"));
        // 使用合同的开始/结束时间
        LocalDate contractStartDate = getLocalDateFromDateTime(participation.get("contractStartDate"));
        LocalDate contractEndDate = getLocalDateFromDateTime(participation.get("contractEndDate"));
        BigDecimal managementFee = getBigDecimal(participation.get("managementFee"));
        BigDecimal onsiteFee = getBigDecimal(participation.get("onsiteFee"));
        // 同类型成员数量
        Integer sameMemberTypeCount = getIntValue(participation.get("sameMemberTypeCount"));

        String contractName = (String) participation.get("contractName");
        String siteName = (String) participation.get("siteName");
        String memberTypeName = memberType != null && memberType == 1 ? "管理" : "驻场";
        
        log.debug("[安全运营收入计算] 合同={}, 驻场点={}, 成员类型={}, 管理费={}, 驻场费={}, 同类型成员数={}, 合同日期={}-{}, 成员日期={}-{}", 
                contractName, siteName, memberTypeName, managementFee, onsiteFee, sameMemberTypeCount,
                contractStartDate, contractEndDate, memberStartDate, memberEndDate);

        if (contractStartDate == null || contractEndDate == null) {
            log.debug("[安全运营收入计算] 合同={} 开始或结束日期为空，跳过", contractName);
            return BigDecimal.ZERO;
        }

        // 根据成员类型选择对应的费用
        // memberType = 1（管理人员）使用管理费，memberType = 2（驻场人员）使用驻场费
        BigDecimal feePool;
        if (memberType != null && memberType == 1) {
            // 管理人员获得管理费
            feePool = managementFee;
            log.debug("[安全运营收入计算] 成员类型=管理人员，使用管理费: {}", feePool);
        } else {
            // 驻场人员获得驻场费
            feePool = onsiteFee;
            log.debug("[安全运营收入计算] 成员类型=驻场人员，使用驻场费: {}", feePool);
        }
        
        if (feePool.compareTo(BigDecimal.ZERO) <= 0) {
            log.debug("[安全运营收入计算] 合同={} 费用池为空或<=0 ({}费={})", 
                    contractName, memberTypeName, feePool);
            return BigDecimal.ZERO;
        }

        // 计算合同总工作日
        int totalContractDays = calculateWorkingDaysBetween(contractStartDate, contractEndDate);
        if (totalContractDays <= 0) {
            log.debug("[安全运营收入计算] 合同={} 总工作日<=0", contractName);
            return BigDecimal.ZERO;
        }

        // 计算该员工的有效工作日（在合同周期内、在该年度内、在截止日期前）
        LocalDate effectiveStart = maxDate(memberStartDate != null ? memberStartDate : contractStartDate, 
                                           contractStartDate, yearStart);
        LocalDate effectiveEnd = minDate(memberEndDate != null ? memberEndDate : contractEndDate,
                                         contractEndDate, cutoffDate);

        if (effectiveStart == null || effectiveEnd == null || effectiveStart.isAfter(effectiveEnd)) {
            log.debug("[安全运营收入计算] 合同={} 有效日期范围无效 ({} - {})", contractName, effectiveStart, effectiveEnd);
            return BigDecimal.ZERO;
        }

        int employeeDays = calculateWorkingDaysBetween(effectiveStart, effectiveEnd);

        // 按同类型成员人数平均分
        int memberCount = sameMemberTypeCount != null && sameMemberTypeCount > 0 ? sameMemberTypeCount : 1;
        
        BigDecimal income = feePool.multiply(new BigDecimal(employeeDays))
                .divide(new BigDecimal(totalContractDays), 4, RoundingMode.HALF_UP)
                .divide(new BigDecimal(memberCount), 2, RoundingMode.HALF_UP);
        
        log.debug("[安全运营收入计算] 合同={}, 成员类型={}, 收入计算: {} × ({}/{}) / {} = {}", 
                contractName, memberTypeName, feePool, employeeDays, totalContractDays, memberCount, income);
        
        return income;
    }
    
    /**
     * 从 LocalDateTime 或 Date 对象获取 LocalDate
     * 支持：LocalDate, LocalDateTime, Timestamp, java.sql.Date, java.util.Date, Long（毫秒时间戳）
     */
    private LocalDate getLocalDateFromDateTime(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDate) return (LocalDate) value;
        if (value instanceof java.time.LocalDateTime) return ((java.time.LocalDateTime) value).toLocalDate();
        if (value instanceof java.sql.Timestamp) return ((java.sql.Timestamp) value).toLocalDateTime().toLocalDate();
        if (value instanceof java.sql.Date) return ((java.sql.Date) value).toLocalDate();
        if (value instanceof java.util.Date) {
            return new java.sql.Date(((java.util.Date) value).getTime()).toLocalDate();
        }
        // 支持 Long 类型的毫秒时间戳
        if (value instanceof Long) {
            return java.time.Instant.ofEpochMilli((Long) value)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate();
        }
        if (value instanceof Number) {
            return java.time.Instant.ofEpochMilli(((Number) value).longValue())
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate();
        }
        return null;
    }

    /**
     * 计算安全服务/数据安全员工收入（基于轮次执行）
     */
    private BigDecimal calculateServiceRoundIncome(Long userId, int year, LocalDate cutoffDate) {
        // 查询该用户作为执行人完成的轮次
        LocalDateTime cutoffDateTime = LocalDateTime.of(cutoffDate, LocalTime.MAX);
        List<Map<String, Object>> completedRounds = serviceItemInfoMapper.selectCompletedRoundsByExecutor(
                userId, year, cutoffDateTime);
        
        if (CollUtil.isEmpty(completedRounds)) {
            log.debug("[服务轮次收入] 用户 {} 在 {} 年无已完成轮次", userId, year);
            return BigDecimal.ZERO;
        }

        log.debug("[服务轮次收入] 用户 {} 在 {} 年完成 {} 个轮次", userId, year, completedRounds.size());
        
        BigDecimal totalIncome = BigDecimal.ZERO;

        for (Map<String, Object> round : completedRounds) {
            BigDecimal roundIncome = calculateSingleRoundIncome(round, userId);
            if (roundIncome.compareTo(BigDecimal.ZERO) > 0) {
                log.debug("[服务轮次收入] 用户 {} 轮次 {} 收入: {}", 
                        userId, round.get("serviceItemName"), roundIncome);
            }
            totalIncome = totalIncome.add(roundIncome);
        }

        log.debug("[服务轮次收入] 用户 {} 总收入: {}", userId, totalIncome);
        return totalIncome;
    }

    /**
     * 计算单个轮次的收入
     */
    private BigDecimal calculateSingleRoundIncome(Map<String, Object> round, Long userId) {
        BigDecimal allocatedAmount = getBigDecimal(round.get("allocatedAmount"));
        Integer maxCount = getIntValue(round.get("maxCount"));
        String executorIds = (String) round.get("executorIds");
        String serviceItemName = (String) round.get("serviceItemName");
        Long roundId = getLongValue(round.get("roundId"));

        log.debug("[轮次收入计算] 服务项={}, 轮次ID={}, 分配金额={}, 最大轮次={}, 执行人IDs={}", 
                serviceItemName, roundId, allocatedAmount, maxCount, executorIds);

        if (allocatedAmount == null || allocatedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            log.debug("[轮次收入计算] 服务项={} 分配金额为空或<=0，跳过", serviceItemName);
            return BigDecimal.ZERO;
        }

        // 单次收入 = 分配金额 / 总轮次
        int totalRounds = maxCount != null && maxCount > 0 ? maxCount : 1;
        BigDecimal perRoundIncome = allocatedAmount.divide(new BigDecimal(totalRounds), 4, RoundingMode.HALF_UP);

        // 如果有多个执行人，平均分
        int executorCount = countExecutors(executorIds);
        if (executorCount > 1) {
            perRoundIncome = perRoundIncome.divide(new BigDecimal(executorCount), 2, RoundingMode.HALF_UP);
        }

        log.debug("[轮次收入计算] 服务项={}, 用户={} 收入: {} / {} / {} = {}", 
                serviceItemName, userId, allocatedAmount, totalRounds, executorCount, perRoundIncome);

        return perRoundIncome;
    }

    /**
     * 统计执行人数量
     * 
     * 支持多种格式：
     * - JSON 数组: [1,2,3] 或 ["1","2","3"]
     * - 逗号分隔: 1,2,3
     * - 单个ID: 123
     */
    private int countExecutors(String executorIds) {
        if (executorIds == null || executorIds.trim().isEmpty()) {
            return 1;
        }
        
        String trimmed = executorIds.trim();
        
        // 尝试使用 JSON 解析
        if (trimmed.startsWith("[")) {
            try {
                JSONArray jsonArray = JSONUtil.parseArray(trimmed);
                int count = jsonArray.size();
                return count > 0 ? count : 1;
            } catch (Exception e) {
                log.debug("[统计执行人] JSON解析失败，使用兜底逻辑，executorIds={}", executorIds);
            }
        }
        
        // 兜底：使用逗号分隔解析
        String cleaned = trimmed.replace("[", "").replace("]", "")
                .replace("\"", "").replace(" ", "");
        if (cleaned.isEmpty()) {
            return 1;
        }
        
        String[] parts = cleaned.split(",");
        // 过滤空字符串
        int count = 0;
        for (String part : parts) {
            if (part != null && !part.trim().isEmpty()) {
                count++;
            }
        }
        return count > 0 ? count : 1;
    }

    /**
     * 获取员工参与明细
     * 根据员工所属部门类型，查询其参与的项目/合同明细
     * 
     * 参与明细包括：
     * 1. 驻场参与（从 project_site_member 表）
     * 2. 轮次执行（从 project_round 表，仅安全服务和数据安全）
     */
    private List<ProjectParticipation> getEmployeeParticipation(AdminUserDO user, int year, LocalDate cutoffDate) {
        List<ProjectParticipation> result = new ArrayList<>();
        
        // 获取用户所属部门类型
        DeptDO dept = user.getDeptId() != null ? deptService.getDept(user.getDeptId()) : null;
        Integer deptType = null;
        if (dept != null) {
            deptType = dept.getDeptType();
            if (deptType == null) {
                deptType = getDeptTypeFromHierarchy(dept.getId());
            }
        }
        
        if (deptType == null) {
            log.debug("[参与明细] 用户 {} 无法确定部门类型", user.getId());
            return result;
        }
        
        // 1. 查询驻场参与（所有部门类型都可能有）
        result.addAll(getOnsiteParticipation(user.getId(), deptType, year, cutoffDate));
        
        // 2. 安全服务和数据安全还可能有轮次执行
        if (deptType == DEPT_TYPE_SECURITY_SERVICE || deptType == DEPT_TYPE_DATA_SECURITY) {
            result.addAll(getServiceRoundParticipation(user.getId(), year, cutoffDate));
        }
        
        log.debug("[参与明细] 用户 {} 共 {} 条记录", user.getId(), result.size());
        return result;
    }

    /**
     * 获取驻场参与明细（通用方法）
     * 从 project_site_member 表查询
     */
    private List<ProjectParticipation> getOnsiteParticipation(Long userId, Integer deptType, int year, LocalDate cutoffDate) {
        List<ProjectParticipation> result = new ArrayList<>();
        
        List<Map<String, Object>> participations = projectSiteMemberInfoMapper.selectMemberParticipation(userId, deptType);
        if (CollUtil.isEmpty(participations)) {
            return result;
        }
        
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        
        for (Map<String, Object> p : participations) {
            LocalDate contractStartDate = getLocalDateFromDateTime(p.get("contractStartDate"));
            LocalDate contractEndDate = getLocalDateFromDateTime(p.get("contractEndDate"));
            LocalDate memberStartDate = getLocalDate(p.get("memberStartDate"));
            LocalDate memberEndDate = getLocalDate(p.get("memberEndDate"));
            
            // 计算有效时间段
            LocalDate effectiveStart = maxDate(memberStartDate != null ? memberStartDate : contractStartDate,
                    contractStartDate, yearStart);
            LocalDate effectiveEnd = minDate(memberEndDate != null ? memberEndDate : contractEndDate,
                    contractEndDate, cutoffDate);
            
            if (effectiveStart == null || effectiveEnd == null || effectiveStart.isAfter(effectiveEnd)) {
                continue;
            }
            
            // 计算收入
            BigDecimal income = calculateSingleOnsiteParticipationIncome(p, yearStart, cutoffDate);
            
            // 计算工作日
            int workDays = calculateWorkingDaysBetween(effectiveStart, effectiveEnd);
            
            // 确定参与类型
            Integer memberType = getIntValue(p.get("memberType"));
            String participationType;
            String participationTypeName;
            if (memberType != null && memberType == 1) {
                participationType = "management";
                participationTypeName = "管理";
            } else {
                participationType = "onsite";
                participationTypeName = "驻场";
            }
            
            result.add(ProjectParticipation.builder()
                    .projectId(getLongValue(p.get("projectId")))
                    .projectName((String) p.get("projectName"))
                    .customerName((String) p.get("customerName"))
                    .participationType(participationType)
                    .participationTypeName(participationTypeName)
                    .startDate(effectiveStart)
                    .endDate(effectiveEnd)
                    .workDays(workDays)
                    .income(income)
                    .build());
        }
        
        return result;
    }
    
    /**
     * 获取安全运营员工的参与明细
     */
    private List<ProjectParticipation> getSecurityOperationParticipation(Long userId, int year, LocalDate cutoffDate) {
        List<ProjectParticipation> result = new ArrayList<>();
        
        List<Map<String, Object>> participations = securityOperationContractInfoMapper.selectMemberParticipation(userId);
        if (CollUtil.isEmpty(participations)) {
            return result;
        }
        
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        
        for (Map<String, Object> p : participations) {
            // 使用合同的开始/结束时间
            LocalDate contractStartDate = getLocalDateFromDateTime(p.get("contractStartDate"));
            LocalDate contractEndDate = getLocalDateFromDateTime(p.get("contractEndDate"));
            LocalDate memberStartDate = getLocalDate(p.get("memberStartDate"));
            LocalDate memberEndDate = getLocalDate(p.get("memberEndDate"));
            
            // 计算有效时间段
            LocalDate effectiveStart = maxDate(memberStartDate != null ? memberStartDate : contractStartDate,
                    contractStartDate, yearStart);
            LocalDate effectiveEnd = minDate(memberEndDate != null ? memberEndDate : contractEndDate,
                    contractEndDate, cutoffDate);
            
            if (effectiveStart == null || effectiveEnd == null || effectiveStart.isAfter(effectiveEnd)) {
                continue;
            }
            
            // 计算收入
            BigDecimal income = calculateSingleOperationParticipationIncome(p, yearStart, cutoffDate);
            
            // 计算工作日
            int workDays = calculateWorkingDaysBetween(effectiveStart, effectiveEnd);
            
            // 确定参与类型
            Integer memberType = getIntValue(p.get("memberType"));
            String participationType;
            String participationTypeName;
            if (memberType != null && memberType == 1) {
                participationType = "management";
                participationTypeName = "管理";
            } else {
                participationType = "onsite";
                participationTypeName = "驻场";
            }
            
            result.add(ProjectParticipation.builder()
                    .projectId(getLongValue(p.get("crmContractId")))
                    .projectName((String) p.get("contractName"))
                    .customerName((String) p.get("customerName"))
                    .participationType(participationType)
                    .participationTypeName(participationTypeName)
                    .startDate(effectiveStart)
                    .endDate(effectiveEnd)
                    .workDays(workDays)
                    .income(income)
                    .build());
        }
        
        return result;
    }
    
    /**
     * 获取安全服务/数据安全员工的轮次参与明细
     */
    private List<ProjectParticipation> getServiceRoundParticipation(Long userId, int year, LocalDate cutoffDate) {
        List<ProjectParticipation> result = new ArrayList<>();
        
        LocalDateTime cutoffDateTime = LocalDateTime.of(cutoffDate, LocalTime.MAX);
        List<Map<String, Object>> completedRounds = serviceItemInfoMapper.selectCompletedRoundsByExecutor(
                userId, year, cutoffDateTime);
        
        if (CollUtil.isEmpty(completedRounds)) {
            return result;
        }
        
        // 按服务项分组统计
        Map<Long, List<Map<String, Object>>> roundsByServiceItem = new LinkedHashMap<>();
        for (Map<String, Object> round : completedRounds) {
            Long serviceItemId = getLongValue(round.get("serviceItemId"));
            if (serviceItemId != null) {
                roundsByServiceItem.computeIfAbsent(serviceItemId, k -> new ArrayList<>()).add(round);
            }
        }
        
        for (Map.Entry<Long, List<Map<String, Object>>> entry : roundsByServiceItem.entrySet()) {
            List<Map<String, Object>> rounds = entry.getValue();
            if (rounds.isEmpty()) continue;
            
            Map<String, Object> firstRound = rounds.get(0);
            BigDecimal totalIncome = BigDecimal.ZERO;
            
            // 计算该服务项下所有轮次的收入
            for (Map<String, Object> round : rounds) {
                totalIncome = totalIncome.add(calculateSingleRoundIncome(round, userId));
            }
            
            result.add(ProjectParticipation.builder()
                    .projectId(entry.getKey())
                    .projectName((String) firstRound.get("serviceItemName"))
                    .customerName((String) firstRound.get("customerName"))
                    .participationType("executor")
                    .participationTypeName("执行")
                    .workDays(rounds.size()) // 完成的轮次数
                    .income(totalIncome)
                    .build());
        }
        
        return result;
    }
    
    private Long getLongValue(Object value) {
        if (value == null) return null;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 获取员工成本
     */
    private BigDecimal getEmployeeCost(Long userId, int year, LocalDate cutoffDate) {
        // 使用截止日期的月份获取成本
        int month = cutoffDate.getMonthValue();
        UserCostRespVO costVO = costCalculationService.getUserCost(userId, year, month);
        if (costVO == null) {
            return BigDecimal.ZERO;
        }
        return costVO.getYearToDateCost() != null ? costVO.getYearToDateCost() : BigDecimal.ZERO;
    }

    /**
     * 获取部门下所有员工（包括子部门）
     */
    private List<AdminUserDO> getEmployeesUnderDept(Long deptId) {
        // 获取部门及所有子部门的ID
        Set<Long> deptIds = new HashSet<>();
        deptIds.add(deptId);
        collectChildDeptIds(deptId, deptIds);

        // 获取所有这些部门的员工
        return adminUserService.getUserListByDeptIds(deptIds);
    }

    /**
     * 收集所有子部门ID（包括所有后代）
     * 
     * 注意：deptService.getChildDeptList 已经返回所有后代部门，不需要再递归
     */
    private void collectChildDeptIds(Long parentId, Set<Long> result) {
        // getChildDeptList 方法内部已经递归获取了所有后代部门，直接使用即可
        List<DeptDO> allDescendants = deptService.getChildDeptList(parentId);
        if (CollUtil.isNotEmpty(allDescendants)) {
            for (DeptDO child : allDescendants) {
                result.add(child.getId());
            }
        }
    }

    /**
     * 从部门层级中获取部门类型
     */
    private Integer getDeptTypeFromHierarchy(Long deptId) {
        DeptDO dept = deptService.getDept(deptId);
        while (dept != null) {
            if (dept.getDeptType() != null) {
                return dept.getDeptType();
            }
            Integer inferred = inferDeptTypeFromName(dept.getName());
            if (inferred != null) {
                return inferred;
            }
            if (dept.getParentId() != null && dept.getParentId() > 0) {
                dept = deptService.getDept(dept.getParentId());
            } else {
                break;
            }
        }
        return null;
    }

    /**
     * 计算总计
     */
    private TotalAnalysis calculateTotal(List<DeptAnalysis> deptAnalysisList) {
        if (CollUtil.isEmpty(deptAnalysisList)) {
            return buildEmptyTotal();
        }

        int employeeCount = 0;
        BigDecimal contractIncome = BigDecimal.ZERO;
        BigDecimal outsideIncome = BigDecimal.ZERO;
        BigDecimal employeeCost = BigDecimal.ZERO;
        BigDecimal outsideExpense = BigDecimal.ZERO;

        for (DeptAnalysis dept : deptAnalysisList) {
            employeeCount += dept.getEmployeeCount() != null ? dept.getEmployeeCount() : 0;
            contractIncome = contractIncome.add(dept.getContractIncome() != null ? dept.getContractIncome() : BigDecimal.ZERO);
            outsideIncome = outsideIncome.add(dept.getOutsideIncome() != null ? dept.getOutsideIncome() : BigDecimal.ZERO);
            employeeCost = employeeCost.add(dept.getEmployeeCost() != null ? dept.getEmployeeCost() : BigDecimal.ZERO);
            outsideExpense = outsideExpense.add(dept.getOutsideExpense() != null ? dept.getOutsideExpense() : BigDecimal.ZERO);
        }

        BigDecimal totalIncome = contractIncome.add(outsideIncome);
        BigDecimal totalExpense = employeeCost.add(outsideExpense);
        BigDecimal netProfit = totalIncome.subtract(totalExpense);
        BigDecimal profitRate = calculateProfitRate(netProfit, totalIncome);

        return TotalAnalysis.builder()
                .employeeCount(employeeCount)
                .contractIncome(contractIncome)
                .outsideIncome(outsideIncome)
                .totalIncome(totalIncome)
                .employeeCost(employeeCost)
                .outsideExpense(outsideExpense)
                .totalExpense(totalExpense)
                .netProfit(netProfit)
                .profitRate(profitRate)
                .build();
    }

    private TotalAnalysis buildEmptyTotal() {
        return TotalAnalysis.builder()
                .employeeCount(0)
                .contractIncome(BigDecimal.ZERO)
                .outsideIncome(BigDecimal.ZERO)
                .totalIncome(BigDecimal.ZERO)
                .employeeCost(BigDecimal.ZERO)
                .outsideExpense(BigDecimal.ZERO)
                .totalExpense(BigDecimal.ZERO)
                .netProfit(BigDecimal.ZERO)
                .profitRate(BigDecimal.ZERO)
                .build();
    }

    // ========== 工具方法 ==========

    private BigDecimal calculateProfitRate(BigDecimal netProfit, BigDecimal totalIncome) {
        if (totalIncome == null || totalIncome.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return netProfit.multiply(new BigDecimal("100"))
                .divide(totalIncome, 2, RoundingMode.HALF_UP);
    }

    private int calculateWorkingDaysBetween(LocalDate startDate, LocalDate endDate) {
        int workingDays = 0;
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            if (holidayService.isWorkday(current)) {
                workingDays++;
            }
            current = current.plusDays(1);
        }
        return workingDays;
    }

    private LocalDate maxDate(LocalDate... dates) {
        LocalDate max = null;
        for (LocalDate d : dates) {
            if (d != null && (max == null || d.isAfter(max))) {
                max = d;
            }
        }
        return max;
    }

    private LocalDate minDate(LocalDate... dates) {
        LocalDate min = null;
        for (LocalDate d : dates) {
            if (d != null && (min == null || d.isBefore(min))) {
                min = d;
            }
        }
        return min;
    }

    private Integer inferDeptTypeFromName(String deptName) {
        if (deptName == null || deptName.isEmpty()) {
            return null;
        }
        if (deptName.contains("运营")) {
            return DEPT_TYPE_SECURITY_OPERATION;
        }
        if (deptName.contains("数据安全") || deptName.contains("数据部")) {
            return DEPT_TYPE_DATA_SECURITY;
        }
        if (deptName.contains("技术服务") || deptName.contains("安服") || deptName.contains("安全服务")) {
            return DEPT_TYPE_SECURITY_SERVICE;
        }
        return null;
    }

    private Integer getIntValue(Object value) {
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal getBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return new BigDecimal(value.toString());
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private LocalDate getLocalDate(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDate) return (LocalDate) value;
        if (value instanceof java.sql.Date) return ((java.sql.Date) value).toLocalDate();
        if (value instanceof java.util.Date) {
            return new java.sql.Date(((java.util.Date) value).getTime()).toLocalDate();
        }
        return null;
    }

    // ========== 跨部门费用计算 ==========

    /**
     * 计算部门的跨部门收入（包括子部门）
     * 跨部门收入 = 作为目标方（target_dept_id）的费用总和
     */
    private BigDecimal calculateDeptOutsideIncome(Long deptId, int year, LocalDateTime cutoffDateTime) {
        BigDecimal total = BigDecimal.ZERO;
        
        // 获取本部门及所有子部门的ID
        List<Long> allDeptIds = getAllDescendantDeptIds(deptId);
        allDeptIds.add(deptId);
        
        // 批量查询这些部门的跨部门收入
        for (Long id : allDeptIds) {
            BigDecimal income = outsideCostRecordMapper.sumIncomeByDeptIdAndYear(id, year, cutoffDateTime);
            if (income != null) {
                total = total.add(income);
            }
        }
        
        return total;
    }

    /**
     * 计算部门的跨部门支出（包括子部门）
     * 跨部门支出 = 作为发起方（request_dept_id）的费用总和
     */
    private BigDecimal calculateDeptOutsideExpense(Long deptId, int year, LocalDateTime cutoffDateTime) {
        BigDecimal total = BigDecimal.ZERO;
        
        // 获取本部门及所有子部门的ID
        List<Long> allDeptIds = getAllDescendantDeptIds(deptId);
        allDeptIds.add(deptId);
        
        // 批量查询这些部门的跨部门支出
        for (Long id : allDeptIds) {
            BigDecimal expense = outsideCostRecordMapper.sumExpenseByDeptIdAndYear(id, year, cutoffDateTime);
            if (expense != null) {
                total = total.add(expense);
            }
        }
        
        return total;
    }

    /**
     * 获取部门的所有后代部门ID（子部门、孙子部门等）
     * 
     * 注意：deptService.getChildDeptList 已经返回所有后代部门，不需要再递归
     */
    private List<Long> getAllDescendantDeptIds(Long deptId) {
        // getChildDeptList 方法内部已经递归获取了所有后代部门，直接使用即可
        List<DeptDO> allDescendants = deptService.getChildDeptList(deptId);
        if (CollUtil.isEmpty(allDescendants)) {
            return new ArrayList<>();
        }
        return allDescendants.stream()
                .map(DeptDO::getId)
                .distinct()  // 确保去重，防止任何可能的重复
                .collect(Collectors.toList());
    }

}
