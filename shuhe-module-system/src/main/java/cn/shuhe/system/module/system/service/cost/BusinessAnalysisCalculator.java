package cn.shuhe.system.module.system.service.cost;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import cn.shuhe.system.module.system.controller.admin.cost.vo.BusinessAnalysisRespVO.DeptAnalysis;
import cn.shuhe.system.module.system.controller.admin.cost.vo.BusinessAnalysisRespVO.ProjectParticipation;
import cn.shuhe.system.module.system.controller.admin.cost.vo.BusinessAnalysisRespVO.TotalAnalysis;
import cn.shuhe.system.module.system.dal.dataobject.dept.DeptDO;
import cn.shuhe.system.module.system.dal.dataobject.user.AdminUserDO;
import cn.shuhe.system.module.system.dal.mysql.cost.OutsideCostRecordMapper;
import cn.shuhe.system.module.system.dal.mysql.cost.ProjectSiteMemberInfoMapper;
import cn.shuhe.system.module.system.dal.mysql.cost.SecurityOperationContractInfoMapper;
import cn.shuhe.system.module.system.dal.mysql.cost.ServiceItemInfoMapper;
import cn.shuhe.system.module.system.service.dept.DeptService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 经营分析计算器：封装所有收入 / 成本 / 利润计算规则。
 *
 * <p>调整任何业务计算逻辑时只需修改此类，Service 层只负责数据编排与树形结构构建。</p>
 */
@Slf4j
@Component
public class BusinessAnalysisCalculator {

    // ========== 部门类型常量（单一来源） ==========

    public static final int DEPT_TYPE_SECURITY_SERVICE = 1;
    public static final int DEPT_TYPE_SECURITY_OPERATION = 2;
    public static final int DEPT_TYPE_DATA_SECURITY = 3;

    public static final Map<Integer, String> DEPT_TYPE_NAMES = Map.of(
            DEPT_TYPE_SECURITY_SERVICE, "安全服务",
            DEPT_TYPE_SECURITY_OPERATION, "安全运营",
            DEPT_TYPE_DATA_SECURITY, "数据安全"
    );

    @Resource
    private HolidayService holidayService;

    @Resource
    private OutsideCostRecordMapper outsideCostRecordMapper;

    @Resource
    private ProjectSiteMemberInfoMapper projectSiteMemberInfoMapper;

    @Resource
    private SecurityOperationContractInfoMapper securityOperationContractInfoMapper;

    @Resource
    private ServiceItemInfoMapper serviceItemInfoMapper;

    @Resource
    private DeptService deptService;

    // ========== 部门类型推断 ==========

    public Integer inferDeptTypeFromName(String deptName) {
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

    // ========== 利润与汇总计算 ==========

    /**
     * 利润率 = 净利润 / 总收入 × 100（保留 2 位小数）
     */
    public BigDecimal calculateProfitRate(BigDecimal netProfit, BigDecimal totalIncome) {
        if (totalIncome == null || totalIncome.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return netProfit.multiply(new BigDecimal("100"))
                .divide(totalIncome, 2, RoundingMode.HALF_UP);
    }

    public TotalAnalysis buildEmptyTotal() {
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

    public TotalAnalysis calculateTotal(List<DeptAnalysis> deptAnalysisList) {
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

    // ========== 工作日计算 ==========

    public int calculateWorkingDaysBetween(LocalDate startDate, LocalDate endDate) {
        return holidayService.countWorkdaysBetween(startDate, endDate);
    }

    // ========== 驻场收入计算 ==========

    /**
     * 使用预加载数据计算驻场收入（批量路径，无 DB 查询）。
     */
    public BigDecimal calculateOnsiteIncomeFromData(List<Map<String, Object>> participations,
                                                    int year, LocalDate cutoffDate) {
        if (CollUtil.isEmpty(participations)) {
            return BigDecimal.ZERO;
        }
        BigDecimal totalIncome = BigDecimal.ZERO;
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        for (Map<String, Object> participation : participations) {
            totalIncome = totalIncome.add(calculateSingleOnsiteParticipationIncome(participation, yearStart, cutoffDate));
        }
        return totalIncome;
    }

    /**
     * 查询 DB 计算驻场收入（单用户非批量路径）。
     */
    public BigDecimal calculateOnsiteIncome(Long userId, Integer deptType, int year, LocalDate cutoffDate) {
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
     * 单条驻场参与记录的收入计算（通用核心公式）。
     *
     * <pre>
     * 收入 = 费用池 × (员工有效工作日 / 合同总工作日) / 同类型成员数
     * </pre>
     */
    public BigDecimal calculateSingleOnsiteParticipationIncome(Map<String, Object> participation,
                                                               LocalDate yearStart, LocalDate cutoffDate) {
        Integer memberType = getIntValue(participation.get("memberType"));
        LocalDate memberStartDate = getLocalDate(participation.get("memberStartDate"));
        LocalDate memberEndDate = getLocalDate(participation.get("memberEndDate"));
        LocalDate contractStartDate = getLocalDateFromDateTime(participation.get("contractStartDate"));
        LocalDate contractEndDate = getLocalDateFromDateTime(participation.get("contractEndDate"));
        BigDecimal managementFee = getBigDecimal(participation.get("managementFee"));
        BigDecimal onsiteFee = getBigDecimal(participation.get("onsiteFee"));
        Integer sameMemberTypeCount = getIntValue(participation.get("sameMemberTypeCount"));
        String projectName = (String) participation.get("projectName");

        log.debug("[驻场收入计算] 项目={}, 成员类型={}, 管理费={}, 驻场费={}, 同类型成员数={}",
                projectName, memberType, managementFee, onsiteFee, sameMemberTypeCount);

        if (contractStartDate == null || contractEndDate == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal feePool = (memberType != null && memberType == 1) ? managementFee : onsiteFee;
        if (feePool.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        int totalContractDays = calculateWorkingDaysBetween(contractStartDate, contractEndDate);
        if (totalContractDays <= 0) {
            return BigDecimal.ZERO;
        }

        LocalDate effectiveStart = maxDate(memberStartDate != null ? memberStartDate : contractStartDate,
                contractStartDate, yearStart);
        LocalDate effectiveEnd = minDate(memberEndDate != null ? memberEndDate : contractEndDate,
                contractEndDate, cutoffDate);

        if (effectiveStart == null || effectiveEnd == null || effectiveStart.isAfter(effectiveEnd)) {
            return BigDecimal.ZERO;
        }

        int employeeDays = calculateWorkingDaysBetween(effectiveStart, effectiveEnd);
        int memberCount = sameMemberTypeCount != null && sameMemberTypeCount > 0 ? sameMemberTypeCount : 1;

        BigDecimal income = feePool.multiply(new BigDecimal(employeeDays))
                .divide(new BigDecimal(totalContractDays), 4, RoundingMode.HALF_UP)
                .divide(new BigDecimal(memberCount), 2, RoundingMode.HALF_UP);

        log.debug("[驻场收入计算] 项目={}, 收入: {} × ({}/{}) / {} = {}",
                projectName, feePool, employeeDays, totalContractDays, memberCount, income);

        return income;
    }

    // ========== 安全运营收入计算 ==========

    public BigDecimal calculateSecurityOperationIncome(Long userId, int year, LocalDate cutoffDate) {
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
     * 单条安全运营参与记录的收入计算（核心公式同驻场，但数据来源不同）。
     */
    public BigDecimal calculateSingleOperationParticipationIncome(Map<String, Object> participation,
                                                                  LocalDate yearStart, LocalDate cutoffDate) {
        Integer memberType = getIntValue(participation.get("memberType"));
        LocalDate memberStartDate = getLocalDate(participation.get("memberStartDate"));
        LocalDate memberEndDate = getLocalDate(participation.get("memberEndDate"));
        LocalDate contractStartDate = getLocalDateFromDateTime(participation.get("contractStartDate"));
        LocalDate contractEndDate = getLocalDateFromDateTime(participation.get("contractEndDate"));
        BigDecimal managementFee = getBigDecimal(participation.get("managementFee"));
        BigDecimal onsiteFee = getBigDecimal(participation.get("onsiteFee"));
        Integer sameMemberTypeCount = getIntValue(participation.get("sameMemberTypeCount"));
        String contractName = (String) participation.get("contractName");
        String memberTypeName = memberType != null && memberType == 1 ? "管理" : "驻场";

        log.debug("[安全运营收入计算] 合同={}, 成员类型={}, 管理费={}, 驻场费={}, 同类型成员数={}, 合同日期={}-{}, 成员日期={}-{}",
                contractName, memberTypeName, managementFee, onsiteFee, sameMemberTypeCount,
                contractStartDate, contractEndDate, memberStartDate, memberEndDate);

        if (contractStartDate == null || contractEndDate == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal feePool;
        if (memberType != null && memberType == 1) {
            feePool = managementFee;
            log.debug("[安全运营收入计算] 成员类型=管理人员，使用管理费: {}", feePool);
        } else {
            feePool = onsiteFee;
            log.debug("[安全运营收入计算] 成员类型=驻场人员，使用驻场费: {}", feePool);
        }

        if (feePool.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        int totalContractDays = calculateWorkingDaysBetween(contractStartDate, contractEndDate);
        if (totalContractDays <= 0) {
            return BigDecimal.ZERO;
        }

        LocalDate effectiveStart = maxDate(memberStartDate != null ? memberStartDate : contractStartDate,
                contractStartDate, yearStart);
        LocalDate effectiveEnd = minDate(memberEndDate != null ? memberEndDate : contractEndDate,
                contractEndDate, cutoffDate);

        if (effectiveStart == null || effectiveEnd == null || effectiveStart.isAfter(effectiveEnd)) {
            return BigDecimal.ZERO;
        }

        int employeeDays = calculateWorkingDaysBetween(effectiveStart, effectiveEnd);
        int memberCount = sameMemberTypeCount != null && sameMemberTypeCount > 0 ? sameMemberTypeCount : 1;

        BigDecimal income = feePool.multiply(new BigDecimal(employeeDays))
                .divide(new BigDecimal(totalContractDays), 4, RoundingMode.HALF_UP)
                .divide(new BigDecimal(memberCount), 2, RoundingMode.HALF_UP);

        log.debug("[安全运营收入计算] 合同={}, 成员类型={}, 收入: {} × ({}/{}) / {} = {}",
                contractName, memberTypeName, feePool, employeeDays, totalContractDays, memberCount, income);

        return income;
    }

    // ========== 轮次（二线服务）收入计算 ==========

    /**
     * 使用预加载数据计算轮次收入（批量路径，无 DB 查询）。
     */
    public BigDecimal calculateServiceRoundIncomeFromData(List<Map<String, Object>> rounds, Long userId,
                                                          int year, LocalDate cutoffDate) {
        if (CollUtil.isEmpty(rounds)) {
            return BigDecimal.ZERO;
        }
        BigDecimal totalIncome = BigDecimal.ZERO;
        for (Map<String, Object> round : rounds) {
            String executorIdsStr = (String) round.get("executorIds");
            if (executorIdsStr != null && isExecutorInList(executorIdsStr, userId)) {
                totalIncome = totalIncome.add(calculateSingleRoundIncome(round, userId));
            }
        }
        return totalIncome;
    }

    /**
     * 查询 DB 计算二线/管理服务项收入（单用户非批量路径）。
     * 新逻辑：直接从 project_info.allocated_amount where executor_id = userId 汇总（不再按轮次计算）。
     */
    public BigDecimal calculateServiceRoundIncome(Long userId, int year, LocalDate cutoffDate) {
        List<Map<String, Object>> items = serviceItemInfoMapper.selectCompletedServiceItemsByExecutor(userId);
        if (CollUtil.isEmpty(items)) {
            log.debug("[二线收入] 用户 {} 无已完成的分配服务项", userId);
            return BigDecimal.ZERO;
        }
        BigDecimal totalIncome = BigDecimal.ZERO;
        for (Map<String, Object> item : items) {
            BigDecimal amount = getBigDecimal(item.get("allocatedAmount"));
            if (amount.compareTo(BigDecimal.ZERO) > 0) {
                log.debug("[二线收入] 用户 {} 服务项 {} 收入: {}", userId, item.get("serviceItemName"), amount);
            }
            totalIncome = totalIncome.add(amount);
        }
        log.debug("[二线收入] 用户 {} 总收入: {}", userId, totalIncome);
        return totalIncome;
    }

    /**
     * 单条轮次收入计算。
     *
     * <pre>
     * 单人单轮次收入 = 分配金额 / 总轮次数 / 执行人数
     * </pre>
     */
    public BigDecimal calculateSingleRoundIncome(Map<String, Object> round, Long userId) {
        BigDecimal allocatedAmount = getBigDecimal(round.get("allocatedAmount"));
        Integer maxCount = getIntValue(round.get("maxCount"));
        String executorIds = (String) round.get("executorIds");
        String serviceItemName = (String) round.get("serviceItemName");
        Long roundId = getLongValue(round.get("roundId"));

        log.debug("[轮次收入计算] 服务项={}, 轮次ID={}, 分配金额={}, 最大轮次={}, 执行人IDs={}",
                serviceItemName, roundId, allocatedAmount, maxCount, executorIds);

        if (allocatedAmount == null || allocatedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        int totalRounds = maxCount != null && maxCount > 0 ? maxCount : 1;
        BigDecimal perRoundIncome = allocatedAmount.divide(new BigDecimal(totalRounds), 4, RoundingMode.HALF_UP);

        int executorCount = countExecutors(executorIds);
        if (executorCount > 1) {
            perRoundIncome = perRoundIncome.divide(new BigDecimal(executorCount), 2, RoundingMode.HALF_UP);
        }

        log.debug("[轮次收入计算] 服务项={}, 用户={} 收入: {} / {} / {} = {}",
                serviceItemName, userId, allocatedAmount, totalRounds, executorCount, perRoundIncome);

        return perRoundIncome;
    }

    // ========== 员工合同收入汇总 ==========

    /**
     * 计算员工合同收入（非批量路径，用于单用户详情查询）。
     */
    public BigDecimal calculateEmployeeContractIncome(AdminUserDO user, int year, LocalDate cutoffDate) {
        BigDecimal totalIncome = BigDecimal.ZERO;
        DeptDO dept = user.getDeptId() != null ? deptService.getDept(user.getDeptId()) : null;
        Integer deptType = null;
        if (dept != null) {
            deptType = dept.getDeptType();
            if (deptType == null) {
                deptType = getDeptTypeFromHierarchy(dept.getId());
            }
        }
        if (deptType == null) {
            return totalIncome;
        }
        totalIncome = totalIncome.add(calculateOnsiteIncome(user.getId(), deptType, year, cutoffDate));
        if (deptType == DEPT_TYPE_SECURITY_SERVICE || deptType == DEPT_TYPE_DATA_SECURITY) {
            totalIncome = totalIncome.add(calculateServiceRoundIncome(user.getId(), year, cutoffDate));
        }
        return totalIncome;
    }

    // ========== 参与明细查询 ==========

    public List<ProjectParticipation> getEmployeeParticipation(AdminUserDO user, int year, LocalDate cutoffDate) {
        List<ProjectParticipation> result = new ArrayList<>();
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
        result.addAll(getOnsiteParticipation(user.getId(), deptType, year, cutoffDate));
        if (deptType == DEPT_TYPE_SECURITY_SERVICE || deptType == DEPT_TYPE_DATA_SECURITY) {
            result.addAll(getServiceRoundParticipation(user.getId(), year, cutoffDate));
        }
        log.debug("[参与明细] 用户 {} 共 {} 条记录", user.getId(), result.size());
        return result;
    }

    public List<ProjectParticipation> getOnsiteParticipation(Long userId, Integer deptType,
                                                              int year, LocalDate cutoffDate) {
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

            LocalDate effectiveStart = maxDate(memberStartDate != null ? memberStartDate : contractStartDate,
                    contractStartDate, yearStart);
            LocalDate effectiveEnd = minDate(memberEndDate != null ? memberEndDate : contractEndDate,
                    contractEndDate, cutoffDate);

            if (effectiveStart == null || effectiveEnd == null || effectiveStart.isAfter(effectiveEnd)) {
                continue;
            }

            BigDecimal income = calculateSingleOnsiteParticipationIncome(p, yearStart, cutoffDate);
            int workDays = calculateWorkingDaysBetween(effectiveStart, effectiveEnd);
            Integer memberType = getIntValue(p.get("memberType"));
            String participationType = (memberType != null && memberType == 1) ? "management" : "onsite";
            String participationTypeName = (memberType != null && memberType == 1) ? "管理" : "驻场";

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

    public List<ProjectParticipation> getSecurityOperationParticipation(Long userId, int year, LocalDate cutoffDate) {
        List<ProjectParticipation> result = new ArrayList<>();
        List<Map<String, Object>> participations = securityOperationContractInfoMapper.selectMemberParticipation(userId);
        if (CollUtil.isEmpty(participations)) {
            return result;
        }
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        for (Map<String, Object> p : participations) {
            LocalDate contractStartDate = getLocalDateFromDateTime(p.get("contractStartDate"));
            LocalDate contractEndDate = getLocalDateFromDateTime(p.get("contractEndDate"));
            LocalDate memberStartDate = getLocalDate(p.get("memberStartDate"));
            LocalDate memberEndDate = getLocalDate(p.get("memberEndDate"));

            LocalDate effectiveStart = maxDate(memberStartDate != null ? memberStartDate : contractStartDate,
                    contractStartDate, yearStart);
            LocalDate effectiveEnd = minDate(memberEndDate != null ? memberEndDate : contractEndDate,
                    contractEndDate, cutoffDate);

            if (effectiveStart == null || effectiveEnd == null || effectiveStart.isAfter(effectiveEnd)) {
                continue;
            }

            BigDecimal income = calculateSingleOperationParticipationIncome(p, yearStart, cutoffDate);
            int workDays = calculateWorkingDaysBetween(effectiveStart, effectiveEnd);
            Integer memberType = getIntValue(p.get("memberType"));
            String participationType = (memberType != null && memberType == 1) ? "management" : "onsite";
            String participationTypeName = (memberType != null && memberType == 1) ? "管理" : "驻场";

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

    public List<ProjectParticipation> getServiceRoundParticipation(Long userId, int year, LocalDate cutoffDate) {
        List<ProjectParticipation> result = new ArrayList<>();
        LocalDateTime cutoffDateTime = LocalDateTime.of(cutoffDate, LocalTime.MAX);
        List<Map<String, Object>> completedRounds = serviceItemInfoMapper.selectCompletedRoundsByExecutor(
                userId, year, cutoffDateTime);
        if (CollUtil.isEmpty(completedRounds)) {
            return result;
        }

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
            for (Map<String, Object> round : rounds) {
                totalIncome = totalIncome.add(calculateSingleRoundIncome(round, userId));
            }
            result.add(ProjectParticipation.builder()
                    .projectId(entry.getKey())
                    .projectName((String) firstRound.get("serviceItemName"))
                    .customerName((String) firstRound.get("customerName"))
                    .participationType("executor")
                    .participationTypeName("执行")
                    .workDays(rounds.size())
                    .income(totalIncome)
                    .build());
        }
        return result;
    }

    // ========== 跨部门费用计算 ==========

    /**
     * 查询单个部门的直属跨部门收入（不含子部门，用于子部门维度统计）。
     */
    public BigDecimal getDeptDirectOutsideIncome(Long deptId, int year, LocalDateTime cutoffDateTime) {
        BigDecimal result = outsideCostRecordMapper.sumIncomeByDeptIdAndYear(deptId, year, cutoffDateTime);
        return result != null ? result : BigDecimal.ZERO;
    }

    /**
     * 查询单个部门的直属跨部门支出（不含子部门，用于子部门维度统计）。
     */
    public BigDecimal getDeptDirectOutsideExpense(Long deptId, int year, LocalDateTime cutoffDateTime) {
        BigDecimal result = outsideCostRecordMapper.sumExpenseByDeptIdAndYear(deptId, year, cutoffDateTime);
        return result != null ? result : BigDecimal.ZERO;
    }

    /**
     * 计算部门的跨部门收入（含所有子部门），不使用缓存。
     */
    public BigDecimal calculateDeptOutsideIncome(Long deptId, int year, LocalDateTime cutoffDateTime) {
        List<Long> allDeptIds = getAllDescendantDeptIds(deptId);
        allDeptIds.add(deptId);
        return sumBatchResults(outsideCostRecordMapper.batchSumIncomeByDeptIds(allDeptIds, year, cutoffDateTime));
    }

    /**
     * 计算部门的跨部门支出（含所有子部门），不使用缓存。
     */
    public BigDecimal calculateDeptOutsideExpense(Long deptId, int year, LocalDateTime cutoffDateTime) {
        List<Long> allDeptIds = getAllDescendantDeptIds(deptId);
        allDeptIds.add(deptId);
        return sumBatchResults(outsideCostRecordMapper.batchSumExpenseByDeptIds(allDeptIds, year, cutoffDateTime));
    }

    /**
     * 计算部门的跨部门收入（含所有子部门），使用预加载子部门缓存。
     */
    public BigDecimal calculateDeptOutsideIncomeWithCache(Long deptId, int year, LocalDateTime cutoffDateTime,
                                                          Map<Long, List<DeptDO>> childDeptCache) {
        List<Long> allDeptIds = getAllDescendantDeptIdsWithCache(deptId, childDeptCache);
        allDeptIds.add(deptId);
        return sumBatchResults(outsideCostRecordMapper.batchSumIncomeByDeptIds(allDeptIds, year, cutoffDateTime));
    }

    /**
     * 计算部门的跨部门支出（含所有子部门），使用预加载子部门缓存。
     */
    public BigDecimal calculateDeptOutsideExpenseWithCache(Long deptId, int year, LocalDateTime cutoffDateTime,
                                                           Map<Long, List<DeptDO>> childDeptCache) {
        List<Long> allDeptIds = getAllDescendantDeptIdsWithCache(deptId, childDeptCache);
        allDeptIds.add(deptId);
        return sumBatchResults(outsideCostRecordMapper.batchSumExpenseByDeptIds(allDeptIds, year, cutoffDateTime));
    }

    public List<Long> getAllDescendantDeptIds(Long deptId) {
        List<DeptDO> allDescendants = deptService.getChildDeptList(deptId);
        if (CollUtil.isEmpty(allDescendants)) {
            return new ArrayList<>();
        }
        return allDescendants.stream().map(DeptDO::getId).distinct().collect(Collectors.toList());
    }

    public List<Long> getAllDescendantDeptIdsWithCache(Long deptId, Map<Long, List<DeptDO>> childDeptCache) {
        Set<Long> result = new HashSet<>();
        collectChildDeptIdsWithCache(deptId, result, childDeptCache);
        return new ArrayList<>(result);
    }

    // ========== 工具方法（公开，供 Service 复用） ==========

    public boolean isExecutorInList(String executorIdsStr, Long userId) {
        if (executorIdsStr == null || executorIdsStr.isEmpty()) {
            return false;
        }
        return executorIdsStr.contains(String.valueOf(userId));
    }

    /**
     * 统计执行人数量，支持 JSON 数组、逗号分隔、单 ID 三种格式。
     */
    public int countExecutors(String executorIds) {
        if (executorIds == null || executorIds.trim().isEmpty()) {
            return 1;
        }
        String trimmed = executorIds.trim();
        if (trimmed.startsWith("[")) {
            try {
                JSONArray jsonArray = JSONUtil.parseArray(trimmed);
                int count = jsonArray.size();
                return count > 0 ? count : 1;
            } catch (Exception e) {
                log.debug("[统计执行人] JSON解析失败，使用兜底逻辑，executorIds={}", executorIds);
            }
        }
        String cleaned = trimmed.replace("[", "").replace("]", "").replace("\"", "").replace(" ", "");
        if (cleaned.isEmpty()) {
            return 1;
        }
        int count = 0;
        for (String part : cleaned.split(",")) {
            if (part != null && !part.trim().isEmpty()) {
                count++;
            }
        }
        return count > 0 ? count : 1;
    }

    public LocalDate maxDate(LocalDate... dates) {
        LocalDate max = null;
        for (LocalDate d : dates) {
            if (d != null && (max == null || d.isAfter(max))) {
                max = d;
            }
        }
        return max;
    }

    public LocalDate minDate(LocalDate... dates) {
        LocalDate min = null;
        for (LocalDate d : dates) {
            if (d != null && (min == null || d.isBefore(min))) {
                min = d;
            }
        }
        return min;
    }

    /**
     * 从多种日期时间类型中提取 LocalDate（兼容 Timestamp、java.sql.Date、Long 毫秒戳等）。
     */
    public LocalDate getLocalDateFromDateTime(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDate) return (LocalDate) value;
        if (value instanceof java.time.LocalDateTime) return ((java.time.LocalDateTime) value).toLocalDate();
        if (value instanceof java.sql.Timestamp) return ((java.sql.Timestamp) value).toLocalDateTime().toLocalDate();
        if (value instanceof java.sql.Date) return ((java.sql.Date) value).toLocalDate();
        if (value instanceof java.util.Date) {
            return new java.sql.Date(((java.util.Date) value).getTime()).toLocalDate();
        }
        if (value instanceof Long) {
            return java.time.Instant.ofEpochMilli((Long) value)
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        }
        if (value instanceof Number) {
            return java.time.Instant.ofEpochMilli(((Number) value).longValue())
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        }
        return null;
    }

    public LocalDate getLocalDate(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDate) return (LocalDate) value;
        if (value instanceof java.sql.Date) return ((java.sql.Date) value).toLocalDate();
        if (value instanceof java.util.Date) {
            return new java.sql.Date(((java.util.Date) value).getTime()).toLocalDate();
        }
        return null;
    }

    public BigDecimal getBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return new BigDecimal(value.toString());
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    public Integer getIntValue(Object value) {
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Long getLongValue(Object value) {
        if (value == null) return null;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ========== 私有辅助方法 ==========

    /**
     * 向上遍历部门层级获取部门类型（非缓存版，仅供 Calculator 内部使用）。
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

    private BigDecimal sumBatchResults(List<Map<String, Object>> results) {
        BigDecimal total = BigDecimal.ZERO;
        if (results != null) {
            for (Map<String, Object> result : results) {
                Object amountObj = result.get("amount");
                if (amountObj != null) {
                    BigDecimal amount = amountObj instanceof BigDecimal
                            ? (BigDecimal) amountObj
                            : new BigDecimal(amountObj.toString());
                    total = total.add(amount);
                }
            }
        }
        return total;
    }
}
