package cn.shuhe.system.module.crm.api.dashboard;

import cn.shuhe.system.framework.common.biz.system.permission.PermissionCommonApi;
import cn.shuhe.system.module.crm.controller.admin.contract.vo.contract.CrmContractPageReqVO;
import cn.shuhe.system.module.crm.dal.dataobject.contract.CrmContractDO;
import cn.shuhe.system.module.crm.dal.dataobject.customer.CrmCustomerDO;
import cn.shuhe.system.module.crm.dal.mysql.contract.CrmContractMapper;
import cn.shuhe.system.module.crm.dal.mysql.customer.CrmCustomerMapper;
import cn.shuhe.system.module.crm.dal.mysql.receivable.CrmReceivableMapper;
import cn.shuhe.system.module.crm.enums.common.CrmSceneTypeEnum;
import cn.shuhe.system.module.crm.controller.admin.statistics.vo.rank.CrmStatisticsRankReqVO;
import cn.shuhe.system.module.crm.controller.admin.statistics.vo.rank.CrmStatisticsRankRespVO;
import cn.shuhe.system.module.crm.service.contract.CrmContractConfigService;
import cn.shuhe.system.module.crm.service.contract.CrmContractService;
import cn.shuhe.system.module.crm.service.customer.CrmCustomerService;
import cn.shuhe.system.module.crm.service.statistics.CrmStatisticsRankService;
import cn.shuhe.system.module.system.api.dashboard.DashboardCrmApi;
import cn.shuhe.system.module.system.api.dept.DeptApi;
import cn.shuhe.system.module.system.api.dept.dto.DeptRespDTO;
import cn.shuhe.system.module.system.controller.admin.dashboard.vo.DashboardStatisticsRespVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 仪表板 - CRM 合同/客户/收入统计 API 实现
 * 为工作台/分析页提供真实合同、客户、回款数据
 */
@Slf4j
@Service
public class DashboardCrmApiImpl implements DashboardCrmApi {

    private static final String SUPER_ADMIN_ROLE = "super_admin";

    @Resource
    private PermissionCommonApi permissionCommonApi;
    @Resource
    private CrmContractMapper contractMapper;
    @Resource
    private CrmContractConfigService contractConfigService;
    @Resource
    private CrmContractService contractService;
    @Resource
    private CrmCustomerMapper customerMapper;
    @Resource
    private CrmCustomerService customerService;
    @Resource
    private CrmReceivableMapper receivableMapper;
    @Resource
    private CrmStatisticsRankService rankService;
    @Resource
    private DeptApi deptApi;

    @Override
    public DashboardStatisticsRespVO.ContractStats getContractStats(Long userId) {
        Integer sceneType = resolveSceneType(userId);
        long totalCount = contractMapper.selectCountForDashboard(userId, sceneType);
        long activeCount = contractMapper.selectCountActiveForDashboard(userId, sceneType);
        long pendingAuditCount = contractMapper.selectCountByAudit(userId, sceneType);
        long expiringCount = 0L;
        var config = contractConfigService.getContractConfig();
        if (config != null && Boolean.TRUE.equals(config.getNotifyEnabled())) {
            expiringCount = contractMapper.selectCountByRemind(userId, sceneType, config);
        }
        BigDecimal totalPriceFen = contractMapper.selectSumTotalPriceForDashboard(userId, sceneType);
        // 合同金额存储为分，转为元
        BigDecimal totalAmount = totalPriceFen == null ? BigDecimal.ZERO : totalPriceFen.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        return DashboardStatisticsRespVO.ContractStats.builder()
                .totalCount((int) totalCount)
                .activeCount((int) activeCount)
                .pendingAuditCount((int) pendingAuditCount)
                .expiringCount((int) expiringCount)
                .totalAmount(totalAmount)
                .build();
    }

    @Override
    public DashboardStatisticsRespVO.CustomerStats getCustomerStats(Long userId) {
        Integer sceneType = resolveSceneType(userId);
        long totalCount = customerMapper.selectCountForDashboard(userId, sceneType);
        long todayContactCount = customerMapper.selectCountByTodayContact(userId, sceneType);
        long followUpCount = customerMapper.selectCountByFollow(userId, sceneType);
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        long monthlyNewCount = customerMapper.selectCountByCreateTimeAfter(userId, sceneType, startOfMonth);

        return DashboardStatisticsRespVO.CustomerStats.builder()
                .totalCount((int) totalCount)
                .todayContactCount((int) todayContactCount)
                .followUpCount((int) followUpCount)
                .monthlyNewCount((int) monthlyNewCount)
                .build();
    }

    @Override
    public DashboardStatisticsRespVO.RevenueStats getRevenueStats(Long userId) {
        Integer sceneType = resolveSceneType(userId);
        LocalDate now = LocalDate.now();
        LocalDateTime startOfMonth = now.withDayOfMonth(1).atStartOfDay();
        LocalDateTime endOfMonth = now.withDayOfMonth(now.lengthOfMonth()).atTime(23, 59, 59);
        LocalDateTime startOfYear = now.withDayOfYear(1).atStartOfDay();
        LocalDateTime endOfYear = now.atTime(23, 59, 59);

        BigDecimal monthlyRevenue = receivableMapper.selectSumPriceForDashboard(userId, sceneType, startOfMonth, endOfMonth);
        BigDecimal yearlyRevenue = receivableMapper.selectSumPriceForDashboard(userId, sceneType, startOfYear, endOfYear);

        return DashboardStatisticsRespVO.RevenueStats.builder()
                .monthlyRevenue(monthlyRevenue != null ? monthlyRevenue : BigDecimal.ZERO)
                .monthlyCost(BigDecimal.ZERO)
                .monthlyProfit(monthlyRevenue != null ? monthlyRevenue : BigDecimal.ZERO)
                .growthRate(null)
                .yearlyRevenue(yearlyRevenue != null ? yearlyRevenue : BigDecimal.ZERO)
                .build();
    }

    @Override
    public List<DashboardStatisticsRespVO.TrendData> getTrendData(Long userId, boolean isAdmin) {
        Integer sceneType = isAdmin ? null : CrmSceneTypeEnum.OWNER.getType();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年M月");
        List<DashboardStatisticsRespVO.TrendData> list = new ArrayList<>();
        LocalDate now = LocalDate.now();

        for (int i = 11; i >= 0; i--) {
            LocalDate month = now.minusMonths(i);
            LocalDateTime start = month.withDayOfMonth(1).atStartOfDay();
            LocalDateTime end = month.withDayOfMonth(month.lengthOfMonth()).atTime(23, 59, 59);
            BigDecimal revenue = receivableMapper.selectSumPriceForDashboard(userId, sceneType, start, end);
            if (revenue == null) {
                revenue = BigDecimal.ZERO;
            }
            list.add(DashboardStatisticsRespVO.TrendData.builder()
                    .month(month.format(formatter))
                    .revenue(revenue)
                    .cost(BigDecimal.ZERO)
                    .profit(revenue)
                    .build());
        }
        return list;
    }

    @Override
    public List<DashboardStatisticsRespVO.ContractRemindItem> getContractReminders(Long userId, boolean isAdmin) {
        var config = contractConfigService.getContractConfig();
        if (config == null || Boolean.FALSE.equals(config.getNotifyEnabled())) {
            return List.of();
        }
        CrmContractPageReqVO reqVO = new CrmContractPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setExpiryType(CrmContractPageReqVO.EXPIRY_TYPE_ABOUT_TO_EXPIRE);
        reqVO.setSceneType(isAdmin ? null : CrmSceneTypeEnum.OWNER.getType());

        var pageResult = contractService.getContractPage(reqVO, userId);
        List<CrmContractDO> contracts = pageResult.getList();
        if (contracts == null || contracts.isEmpty()) {
            return List.of();
        }

        Set<Long> customerIds = contracts.stream().map(CrmContractDO::getCustomerId).filter(id -> id != null).collect(Collectors.toSet());
        Map<Long, CrmCustomerDO> customerMap = customerIds.isEmpty() ? Map.of() : customerService.getCustomerMap(customerIds);

        LocalDate today = LocalDate.now();
        List<DashboardStatisticsRespVO.ContractRemindItem> result = new ArrayList<>();
        for (CrmContractDO c : contracts) {
            String customerName = "";
            if (c.getCustomerId() != null) {
                CrmCustomerDO cust = customerMap.get(c.getCustomerId());
                if (cust != null && cust.getName() != null) {
                    customerName = cust.getName();
                }
            }
            String endDateStr = c.getEndTime() == null ? "" : c.getEndTime().toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
            int remainingDays = c.getEndTime() == null ? 0 : (int) ChronoUnit.DAYS.between(today, c.getEndTime().toLocalDate());
            // 合同金额为分，转为元
            BigDecimal amount = c.getTotalPrice() == null ? BigDecimal.ZERO : c.getTotalPrice().divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            result.add(DashboardStatisticsRespVO.ContractRemindItem.builder()
                    .id(c.getId())
                    .contractNo(c.getNo())
                    .contractName(c.getName())
                    .customerName(customerName)
                    .endDate(endDateStr)
                    .remainingDays(remainingDays)
                    .amount(amount)
                    .build());
        }
        return result;
    }

    @Override
    public List<DashboardStatisticsRespVO.RankData> getDeptRanking(Long userId, boolean isAdmin) {
        if (!isAdmin) {
            return List.of();
        }
        LocalDate now = LocalDate.now();
        LocalDateTime startOfYear = now.withDayOfYear(1).atStartOfDay();
        LocalDateTime endOfNow = LocalDateTime.now();
        List<DeptRespDTO> firstLevelDepts = deptApi.getChildDeptList(0L);
        if (firstLevelDepts == null) {
            return List.of();
        }
        List<DeptRespDTO> topDepts = firstLevelDepts.stream()
                .filter(d -> d.getParentId() != null && d.getParentId().equals(0L))
                .collect(Collectors.toList());
        if (topDepts.isEmpty()) {
            topDepts = firstLevelDepts;
        }
        List<DashboardStatisticsRespVO.RankData> result = new ArrayList<>();
        for (DeptRespDTO dept : topDepts) {
            CrmStatisticsRankReqVO reqVO = new CrmStatisticsRankReqVO();
            reqVO.setDeptId(dept.getId());
            reqVO.setTimes(new LocalDateTime[]{startOfYear, endOfNow});
            List<CrmStatisticsRankRespVO> ranks = rankService.getReceivablePriceRank(reqVO);
            BigDecimal amount = BigDecimal.ZERO;
            if (ranks != null) {
                for (CrmStatisticsRankRespVO r : ranks) {
                    if (r.getCount() != null) {
                        amount = amount.add(r.getCount());
                    }
                }
            }
            result.add(DashboardStatisticsRespVO.RankData.builder()
                    .deptName(dept.getName())
                    .amount(amount)
                    .completionRate(null)
                    .build());
        }
        result.sort(Comparator.comparing(DashboardStatisticsRespVO.RankData::getAmount).reversed());
        for (int i = 0; i < result.size(); i++) {
            DashboardStatisticsRespVO.RankData r = result.get(i);
            result.set(i, DashboardStatisticsRespVO.RankData.builder()
                    .rank(i + 1)
                    .deptName(r.getDeptName())
                    .amount(r.getAmount())
                    .completionRate(r.getCompletionRate())
                    .build());
        }
        return result;
    }

    private Integer resolveSceneType(Long userId) {
        if (permissionCommonApi.hasAnyRoles(userId, SUPER_ADMIN_ROLE)) {
            return null;
        }
        return CrmSceneTypeEnum.OWNER.getType();
    }
}
