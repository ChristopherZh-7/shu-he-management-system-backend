package cn.shuhe.system.module.system.service.cost;

import cn.hutool.core.collection.CollUtil;
import cn.shuhe.system.module.system.controller.admin.cost.vo.DeptCostSummaryReqVO;
import cn.shuhe.system.module.system.controller.admin.cost.vo.DeptCostSummaryRespVO;
import cn.shuhe.system.module.system.controller.admin.cost.vo.DeptCostSummaryRespVO.*;
import cn.shuhe.system.module.system.controller.admin.cost.vo.UserCostPageReqVO;
import cn.shuhe.system.module.system.controller.admin.cost.vo.UserCostRespVO;
import cn.shuhe.system.module.system.controller.admin.dept.vo.dept.DeptListReqVO;
import cn.shuhe.system.module.system.dal.dataobject.cost.ContractDeptAllocationDO;
import cn.shuhe.system.module.system.dal.dataobject.cost.ServiceItemAllocationDO;
import cn.shuhe.system.module.system.dal.dataobject.dept.DeptDO;
import cn.shuhe.system.module.system.dal.mysql.cost.ContractDeptAllocationMapper;
import cn.shuhe.system.module.system.dal.mysql.cost.OutsideCostRecordMapper;
import cn.shuhe.system.module.system.dal.mysql.cost.SecurityOperationContractInfoMapper;
import cn.shuhe.system.module.system.dal.mysql.cost.ServiceItemAllocationMapper;
import cn.shuhe.system.module.system.dal.mysql.cost.ServiceItemInfoMapper;
import cn.shuhe.system.module.system.service.dept.DeptService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 部门费用汇总 Service 实现类
 *
 * @author system
 */
@Slf4j
@Service
public class DeptCostSummaryServiceImpl implements DeptCostSummaryService {

    /**
     * 部门类型常量
     */
    public static final int DEPT_TYPE_SECURITY_SERVICE = 1;  // 安全服务
    public static final int DEPT_TYPE_SECURITY_OPERATION = 2; // 安全运营
    public static final int DEPT_TYPE_DATA_SECURITY = 3;      // 数据安全

    /**
     * 部门类型名称映射
     */
    private static final Map<Integer, String> DEPT_TYPE_NAMES = Map.of(
            DEPT_TYPE_SECURITY_SERVICE, "安全服务",
            DEPT_TYPE_SECURITY_OPERATION, "安全运营",
            DEPT_TYPE_DATA_SECURITY, "数据安全"
    );

    @Resource
    private DeptService deptService;

    @Resource
    private CostCalculationService costCalculationService;

    @Resource
    private HolidayService holidayService;

    @Resource
    private ContractDeptAllocationMapper contractDeptAllocationMapper;

    @Resource
    private OutsideCostRecordMapper outsideCostRecordMapper;

    @Resource
    private SecurityOperationContractInfoMapper securityOperationContractInfoMapper;

    @Resource
    private ServiceItemAllocationMapper serviceItemAllocationMapper;

    @Resource
    private ServiceItemInfoMapper serviceItemInfoMapper;

    @Override
    public DeptCostSummaryRespVO getDeptCostSummary(DeptCostSummaryReqVO reqVO) {
        // 1. 设置默认值
        LocalDate cutoffDate = reqVO.getCutoffDate() != null ? reqVO.getCutoffDate() : LocalDate.now();
        int year = reqVO.getYear() != null ? reqVO.getYear() : cutoffDate.getYear();

        // 2. 获取需要统计的部门列表
        List<DeptDO> depts = getTargetDepts(reqVO.getDeptId());
        if (CollUtil.isEmpty(depts)) {
            return DeptCostSummaryRespVO.builder()
                    .year(year)
                    .cutoffDate(cutoffDate)
                    .deptSummaries(Collections.emptyList())
                    .total(buildEmptyTotal())
                    .build();
        }

        // 3. 计算每个部门的汇总数据
        List<DeptSummary> deptSummaries = new ArrayList<>();
        for (DeptDO dept : depts) {
            DeptSummary summary = getDeptSummary(dept.getId(), year, cutoffDate);
            if (summary != null) {
                deptSummaries.add(summary);
            }
        }

        // 4. 计算总计
        TotalSummary total = calculateTotal(deptSummaries);

        // 5. 构建响应
        return DeptCostSummaryRespVO.builder()
                .year(year)
                .cutoffDate(cutoffDate)
                .deptSummaries(deptSummaries)
                .total(total)
                .build();
    }

    @Override
    public DeptSummary getDeptSummary(Long deptId, int year, LocalDate cutoffDate) {
        // 1. 获取部门信息
        DeptDO dept = deptService.getDept(deptId);
        if (dept == null) {
            return null;
        }

        Integer deptType = dept.getDeptType();
        if (deptType == null) {
            // 根据部门名称推断类型
            deptType = inferDeptTypeFromName(dept.getName());
        }

        // 2. 计算员工成本
        BigDecimal employeeCost = calculateEmployeeCost(deptId, year, cutoffDate);
        int employeeCount = getEmployeeCount(deptId);

        // 3. 计算合同收入
        ContractIncomeResult contractResult = calculateContractIncome(deptId, deptType, year, cutoffDate);

        // 4. 计算跨部门费用
        LocalDateTime cutoffDateTime = LocalDateTime.of(cutoffDate, LocalTime.MAX);
        BigDecimal outsideExpense = outsideCostRecordMapper.sumExpenseByDeptId(deptId, cutoffDateTime);
        Integer outsideExpenseCount = outsideCostRecordMapper.countExpenseByDeptId(deptId, cutoffDateTime);
        BigDecimal outsideIncome = outsideCostRecordMapper.sumIncomeByDeptId(deptId, cutoffDateTime);
        Integer outsideIncomeCount = outsideCostRecordMapper.countIncomeByDeptId(deptId, cutoffDateTime);

        // 确保非空
        outsideExpense = outsideExpense != null ? outsideExpense : BigDecimal.ZERO;
        outsideIncome = outsideIncome != null ? outsideIncome : BigDecimal.ZERO;
        outsideExpenseCount = outsideExpenseCount != null ? outsideExpenseCount : 0;
        outsideIncomeCount = outsideIncomeCount != null ? outsideIncomeCount : 0;

        // 5. 计算汇总
        BigDecimal totalIncome = contractResult.totalIncome.add(outsideIncome);
        BigDecimal totalExpense = employeeCost.add(outsideExpense);
        BigDecimal netProfit = totalIncome.subtract(totalExpense);
        BigDecimal profitRate = BigDecimal.ZERO;
        if (totalIncome.compareTo(BigDecimal.ZERO) > 0) {
            profitRate = netProfit.multiply(new BigDecimal("100"))
                    .divide(totalIncome, 2, RoundingMode.HALF_UP);
        }

        // 6. 构建响应
        return DeptSummary.builder()
                .deptId(deptId)
                .deptName(dept.getName())
                .deptType(deptType)
                .deptTypeName(DEPT_TYPE_NAMES.get(deptType))
                // 收入
                .contractIncome(contractResult.totalIncome)
                .contractCount(contractResult.contractCount)
                .outsideIncome(outsideIncome)
                .outsideIncomeCount(outsideIncomeCount)
                // 支出
                .employeeCost(employeeCost)
                .employeeCount(employeeCount)
                .outsideExpense(outsideExpense)
                .outsideExpenseCount(outsideExpenseCount)
                // 汇总
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .netProfit(netProfit)
                .profitRate(profitRate)
                // 明细
                .contractDetails(contractResult.details)
                .build();
    }

    /**
     * 获取目标部门列表
     */
    private List<DeptDO> getTargetDepts(Long deptId) {
        if (deptId != null) {
            // 指定了部门，只查询该部门
            DeptDO dept = deptService.getDept(deptId);
            return dept != null ? List.of(dept) : Collections.emptyList();
        }

        // 【性能优化】从缓存获取所有部门
        List<DeptDO> allDepts = deptService.getAllDeptListFromCache();
        return allDepts.stream()
                .filter(dept -> {
                    Integer type = dept.getDeptType();
                    if (type == null) {
                        type = inferDeptTypeFromName(dept.getName());
                    }
                    return type != null && (type == DEPT_TYPE_SECURITY_SERVICE 
                            || type == DEPT_TYPE_SECURITY_OPERATION 
                            || type == DEPT_TYPE_DATA_SECURITY);
                })
                .collect(Collectors.toList());
    }

    /**
     * 计算员工成本
     */
    private BigDecimal calculateEmployeeCost(Long deptId, int year, LocalDate cutoffDate) {
        // 使用现有的成本计算服务获取员工成本
        UserCostPageReqVO reqVO = new UserCostPageReqVO();
        reqVO.setDeptId(deptId);
        reqVO.setYear(year);
        reqVO.setPageNo(1);
        reqVO.setPageSize(1000); // 获取所有员工

        var pageResult = costCalculationService.getUserCostPage(reqVO);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return BigDecimal.ZERO;
        }

        // 累加所有员工的年度累计成本
        return pageResult.getList().stream()
                .map(UserCostRespVO::getYearToDateCost)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 获取员工人数
     */
    private int getEmployeeCount(Long deptId) {
        UserCostPageReqVO reqVO = new UserCostPageReqVO();
        reqVO.setDeptId(deptId);
        reqVO.setYear(LocalDate.now().getYear());
        reqVO.setPageNo(1);
        reqVO.setPageSize(1);

        var pageResult = costCalculationService.getUserCostPage(reqVO);
        return pageResult.getTotal() != null ? pageResult.getTotal().intValue() : 0;
    }

    /**
     * 合同收入计算结果
     */
    private static class ContractIncomeResult {
        BigDecimal totalIncome = BigDecimal.ZERO;
        int contractCount = 0;
        List<ContractIncomeDetail> details = new ArrayList<>();
    }

    /**
     * 计算合同收入
     */
    private ContractIncomeResult calculateContractIncome(Long deptId, Integer deptType, int year, LocalDate cutoffDate) {
        ContractIncomeResult result = new ContractIncomeResult();

        // 获取该部门的所有合同分配
        List<ContractDeptAllocationDO> allocations = contractDeptAllocationMapper.selectByDeptId(deptId);
        if (CollUtil.isEmpty(allocations)) {
            return result;
        }

        for (ContractDeptAllocationDO allocation : allocations) {
            ContractIncomeDetail detail = calculateSingleContractIncome(allocation, deptType, cutoffDate);
            if (detail != null) {
                result.details.add(detail);
                result.totalIncome = result.totalIncome.add(detail.getNetIncome());
                result.contractCount++;
            }
        }

        return result;
    }

    /**
     * 计算单个合同的收入
     */
    private ContractIncomeDetail calculateSingleContractIncome(ContractDeptAllocationDO allocation, 
                                                                Integer deptType, LocalDate cutoffDate) {
        BigDecimal allocatedAmount = allocation.getAllocatedAmount();
        if (allocatedAmount == null || allocatedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        ContractIncomeDetail detail = new ContractIncomeDetail();
        detail.setContractId(allocation.getContractId());
        detail.setContractNo(allocation.getContractNo());
        detail.setCustomerName(allocation.getCustomerName());
        detail.setAllocatedAmount(allocatedAmount);

        // 根据部门类型计算收入
        BigDecimal confirmedIncome;
        if (deptType != null && deptType == DEPT_TYPE_SECURITY_OPERATION) {
            // 安全运营：按工作日比例计算
            confirmedIncome = calculateOperationContractIncome(allocation, cutoffDate, detail);
        } else {
            // 安全服务/数据安全：按轮次计算（完成一次算一次钱）
            confirmedIncome = calculateServiceContractIncome(allocation, cutoffDate, detail);
        }

        detail.setConfirmedIncome(confirmedIncome);

        // 获取跨部门费用
        BigDecimal outsideExpense = outsideCostRecordMapper.sumExpenseByContractIdAndDeptId(
                allocation.getContractId(), allocation.getDeptId());
        BigDecimal outsideIncome = outsideCostRecordMapper.sumIncomeByContractIdAndDeptId(
                allocation.getContractId(), allocation.getDeptId());

        outsideExpense = outsideExpense != null ? outsideExpense : BigDecimal.ZERO;
        outsideIncome = outsideIncome != null ? outsideIncome : BigDecimal.ZERO;

        detail.setOutsideExpense(outsideExpense);
        detail.setOutsideIncome(outsideIncome);

        // 净收入 = 已确认收入 - 跨部门支出 + 跨部门收入
        BigDecimal netIncome = confirmedIncome.subtract(outsideExpense).add(outsideIncome);
        detail.setNetIncome(netIncome);

        return detail;
    }

    /**
     * 计算安全运营合同收入（按工作日比例）
     * 
     * 计算逻辑：
     * 1. 使用 crm_contract 的 start_time/end_time 计算合同总工作日
     * 2. 从 service_item_allocation 表获取管理费和驻场费的分配金额
     * 3. 已确认收入 = (管理费 + 驻场费) × (截至今天工作日 / 总工作日)
     */
    private BigDecimal calculateOperationContractIncome(ContractDeptAllocationDO allocation, 
                                                         LocalDate cutoffDate, ContractIncomeDetail detail) {
        // 查询安全运营收入计算所需信息（合同日期 + 分配金额）
        Map<String, Object> incomeInfo = securityOperationContractInfoMapper.selectOperationIncomeInfo(allocation.getId());
        if (incomeInfo == null) {
            log.warn("未找到安全运营收入信息，allocationId={}", allocation.getId());
            return BigDecimal.ZERO;
        }

        // 获取合同开始和结束时间（来自 crm_contract）
        LocalDate startDate = getLocalDateFromDateTime(incomeInfo.get("contractStartDate"));
        LocalDate endDate = getLocalDateFromDateTime(incomeInfo.get("contractEndDate"));

        if (startDate == null || endDate == null) {
            log.warn("合同日期为空，allocationId={}, contractId={}", allocation.getId(), allocation.getContractId());
            return BigDecimal.ZERO;
        }

        detail.setStartDate(startDate);
        detail.setEndDate(endDate);

        // 获取管理费和驻场费分配金额
        BigDecimal managementFee = getBigDecimal(incomeInfo.get("managementFee"));
        BigDecimal onsiteFee = getBigDecimal(incomeInfo.get("onsiteFee"));
        BigDecimal totalAllocatedFee = managementFee.add(onsiteFee);
        
        // 如果没有分配费用，使用部门分配金额
        if (totalAllocatedFee.compareTo(BigDecimal.ZERO) <= 0) {
            totalAllocatedFee = allocation.getAllocatedAmount();
        }

        // 如果还没开始
        if (cutoffDate.isBefore(startDate)) {
            detail.setTotalWorkDays(calculateWorkingDaysBetween(startDate, endDate));
            detail.setExecutedWorkDays(0);
            detail.setProgressRate(BigDecimal.ZERO);
            return BigDecimal.ZERO;
        }

        // 计算合同总工作日
        int totalWorkDays = calculateWorkingDaysBetween(startDate, endDate);
        detail.setTotalWorkDays(totalWorkDays);

        if (totalWorkDays <= 0) {
            return BigDecimal.ZERO;
        }

        // 计算截止日期（不超过合同结束日期）
        LocalDate actualCutoff = cutoffDate.isBefore(endDate) ? cutoffDate : endDate;

        // 计算已执行工作日
        int executedDays = calculateWorkingDaysBetween(startDate, actualCutoff);
        detail.setExecutedWorkDays(executedDays);

        // 计算进度百分比
        BigDecimal progressRate = new BigDecimal(executedDays * 100)
                .divide(new BigDecimal(totalWorkDays), 2, RoundingMode.HALF_UP);
        detail.setProgressRate(progressRate);

        // 计算已确认收入 = 分配金额 × (已执行工作日 / 总工作日)
        return totalAllocatedFee.multiply(new BigDecimal(executedDays))
                .divide(new BigDecimal(totalWorkDays), 2, RoundingMode.HALF_UP);
    }
    
    /**
     * 从 LocalDateTime 或 Date 对象获取 LocalDate
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
        return null;
    }
    
    /**
     * 安全获取 BigDecimal 值
     */
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

    /**
     * 计算安全服务/数据安全合同收入（按轮次计算，完成一次算一次钱）
     * 
     * 计算逻辑：
     * 1. 获取该合同部门分配下的所有服务项分配
     * 2. 对于每个服务项：单次收入 = 服务项分配金额 / 总轮次
     * 3. 已确认收入 = 单次收入 × 已完成轮次数
     */
    private BigDecimal calculateServiceContractIncome(ContractDeptAllocationDO allocation,
                                                       LocalDate cutoffDate, ContractIncomeDetail detail) {
        // 获取该合同部门分配下的所有服务项分配
        List<ServiceItemAllocationDO> serviceItemAllocations = 
                serviceItemAllocationMapper.selectByContractDeptAllocationId(allocation.getId());
        
        if (CollUtil.isEmpty(serviceItemAllocations)) {
            // 没有服务项分配，直接使用部门分配金额（视为全部确认）
            detail.setTotalWorkDays(0);
            detail.setExecutedWorkDays(0);
            detail.setProgressRate(new BigDecimal("100.00"));
            return allocation.getAllocatedAmount();
        }

        BigDecimal totalConfirmedIncome = BigDecimal.ZERO;
        int totalMaxRounds = 0;
        int totalCompletedRounds = 0;
        LocalDateTime cutoffDateTime = LocalDateTime.of(cutoffDate, LocalTime.MAX);

        for (ServiceItemAllocationDO itemAllocation : serviceItemAllocations) {
            BigDecimal itemAllocatedAmount = itemAllocation.getAllocatedAmount();
            if (itemAllocatedAmount == null || itemAllocatedAmount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            // 获取服务项信息
            Map<String, Object> serviceItem = serviceItemInfoMapper.selectServiceItemInfo(itemAllocation.getServiceItemId());
            if (serviceItem == null) {
                log.warn("未找到服务项信息，serviceItemId={}", itemAllocation.getServiceItemId());
                // 找不到服务项信息，视为全部确认
                totalConfirmedIncome = totalConfirmedIncome.add(itemAllocatedAmount);
                continue;
            }

            // 获取频次类型和最大轮次数
            Integer frequencyType = getIntValue(serviceItem.get("frequencyType"));
            Integer maxCount = getIntValue(serviceItem.get("maxCount"));
            
            // 按需（frequency_type=0）或没有设置最大轮次：直接全部确认
            if (frequencyType != null && frequencyType == 0) {
                // 按需：直接全部确认，不管完成没完成
                totalConfirmedIncome = totalConfirmedIncome.add(itemAllocatedAmount);
                log.debug("[安全服务收入] 服务项={} 按需类型，直接确认全部金额={}", 
                        serviceItem.get("name"), itemAllocatedAmount);
                continue;
            }
            
            if (maxCount == null || maxCount <= 0) {
                // 没有设置最大轮次，也视为全部确认
                totalConfirmedIncome = totalConfirmedIncome.add(itemAllocatedAmount);
                continue;
            }

            totalMaxRounds += maxCount;

            // 统计已完成轮次数
            Integer completedRounds = serviceItemInfoMapper.countCompletedRounds(
                    itemAllocation.getServiceItemId(), cutoffDateTime);
            completedRounds = completedRounds != null ? completedRounds : 0;
            totalCompletedRounds += completedRounds;

            // 计算已确认收入：分配金额 × (已完成轮次 / 最大轮次)
            if (completedRounds > 0) {
                BigDecimal confirmedIncome = itemAllocatedAmount
                        .multiply(new BigDecimal(completedRounds))
                        .divide(new BigDecimal(maxCount), 2, RoundingMode.HALF_UP);
                totalConfirmedIncome = totalConfirmedIncome.add(confirmedIncome);
                
                log.debug("[安全服务收入] 服务项={}, 分配金额={}, 总轮次={}, 已完成={}, 已确认收入={}",
                        serviceItem.get("name"), itemAllocatedAmount, maxCount, completedRounds, confirmedIncome);
            }
        }

        // 设置进度信息（用轮次表示）
        detail.setTotalWorkDays(totalMaxRounds);  // 复用字段：总轮次
        detail.setExecutedWorkDays(totalCompletedRounds);  // 复用字段：已完成轮次
        if (totalMaxRounds > 0) {
            BigDecimal progressRate = new BigDecimal(totalCompletedRounds * 100)
                    .divide(new BigDecimal(totalMaxRounds), 2, RoundingMode.HALF_UP);
            detail.setProgressRate(progressRate);
        } else {
            detail.setProgressRate(totalConfirmedIncome.compareTo(BigDecimal.ZERO) > 0 
                    ? new BigDecimal("100.00") : BigDecimal.ZERO);
        }

        return totalConfirmedIncome;
    }

    /**
     * 安全获取 Integer 值
     */
    private Integer getIntValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 计算两个日期之间的工作日数
     */
    private int calculateWorkingDaysBetween(LocalDate startDate, LocalDate endDate) {
        // 使用批量查询方法，避免 N+1 查询问题
        return holidayService.countWorkdaysBetween(startDate, endDate);
    }

    /**
     * 计算所有部门的总计
     */
    private TotalSummary calculateTotal(List<DeptSummary> summaries) {
        if (CollUtil.isEmpty(summaries)) {
            return buildEmptyTotal();
        }

        BigDecimal contractIncome = BigDecimal.ZERO;
        int contractCount = 0;
        BigDecimal outsideIncome = BigDecimal.ZERO;
        BigDecimal employeeCost = BigDecimal.ZERO;
        int employeeCount = 0;
        BigDecimal outsideExpense = BigDecimal.ZERO;

        for (DeptSummary s : summaries) {
            contractIncome = contractIncome.add(s.getContractIncome() != null ? s.getContractIncome() : BigDecimal.ZERO);
            contractCount += s.getContractCount() != null ? s.getContractCount() : 0;
            outsideIncome = outsideIncome.add(s.getOutsideIncome() != null ? s.getOutsideIncome() : BigDecimal.ZERO);
            employeeCost = employeeCost.add(s.getEmployeeCost() != null ? s.getEmployeeCost() : BigDecimal.ZERO);
            employeeCount += s.getEmployeeCount() != null ? s.getEmployeeCount() : 0;
            outsideExpense = outsideExpense.add(s.getOutsideExpense() != null ? s.getOutsideExpense() : BigDecimal.ZERO);
        }

        BigDecimal totalIncome = contractIncome.add(outsideIncome);
        BigDecimal totalExpense = employeeCost.add(outsideExpense);
        BigDecimal netProfit = totalIncome.subtract(totalExpense);
        BigDecimal profitRate = BigDecimal.ZERO;
        if (totalIncome.compareTo(BigDecimal.ZERO) > 0) {
            profitRate = netProfit.multiply(new BigDecimal("100"))
                    .divide(totalIncome, 2, RoundingMode.HALF_UP);
        }

        return TotalSummary.builder()
                .contractIncome(contractIncome)
                .contractCount(contractCount)
                .outsideIncome(outsideIncome)
                .employeeCost(employeeCost)
                .employeeCount(employeeCount)
                .outsideExpense(outsideExpense)
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .netProfit(netProfit)
                .profitRate(profitRate)
                .build();
    }

    /**
     * 构建空的总计
     */
    private TotalSummary buildEmptyTotal() {
        return TotalSummary.builder()
                .contractIncome(BigDecimal.ZERO)
                .contractCount(0)
                .outsideIncome(BigDecimal.ZERO)
                .employeeCost(BigDecimal.ZERO)
                .employeeCount(0)
                .outsideExpense(BigDecimal.ZERO)
                .totalIncome(BigDecimal.ZERO)
                .totalExpense(BigDecimal.ZERO)
                .netProfit(BigDecimal.ZERO)
                .profitRate(BigDecimal.ZERO)
                .build();
    }

    /**
     * 根据部门名称推断部门类型
     */
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

    /**
     * 安全获取 LocalDate
     */
    private LocalDate getLocalDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate) {
            return (LocalDate) value;
        }
        if (value instanceof java.sql.Date) {
            return ((java.sql.Date) value).toLocalDate();
        }
        if (value instanceof java.util.Date) {
            return new java.sql.Date(((java.util.Date) value).getTime()).toLocalDate();
        }
        return null;
    }

}
