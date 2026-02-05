package cn.shuhe.system.module.system.service.cost;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.module.system.controller.admin.cost.vo.UserCostPageReqVO;
import cn.shuhe.system.module.system.controller.admin.cost.vo.UserCostRespVO;
import cn.shuhe.system.module.system.controller.admin.cost.vo.WorkingDaysRespVO;
import cn.shuhe.system.module.system.dal.dataobject.cost.PositionLevelHistoryDO;
import cn.shuhe.system.module.system.dal.dataobject.dept.DeptDO;
import cn.shuhe.system.module.system.dal.dataobject.holiday.HolidayDO;
import cn.shuhe.system.module.system.dal.dataobject.user.AdminUserDO;
import cn.shuhe.system.module.system.dal.mysql.holiday.HolidayMapper;
import cn.shuhe.system.module.system.dal.mysql.user.AdminUserMapper;
import cn.shuhe.system.module.system.service.dept.DeptService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

import static cn.shuhe.system.framework.common.util.collection.CollectionUtils.convertSet;

/**
 * 成本计算 Service 实现类
 *
 * @author system
 */
@Slf4j
@Service
public class CostCalculationServiceImpl implements CostCalculationService {

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

    /**
     * 职级列表（按顺序）
     */
    private static final List<String> POSITION_LEVELS = List.of(
            "P1-1", "P1-2", "P1-3",
            "P2-1", "P2-2", "P2-3",
            "P3-1", "P3-2", "P3-3"
    );

    /**
     * 安全服务/数据安全 工资表
     * P1-1=6000, 每级+1000, P3每级+2000
     */
    private static final Map<String, BigDecimal> SALARY_SERVICE_DATA = new LinkedHashMap<>();

    /**
     * 安全运营 工资表
     * P1-1=5500, P1-1到P2-1每级+500, P2-1到P3-1每级+1000, P3每级+2000
     */
    private static final Map<String, BigDecimal> SALARY_OPERATION = new LinkedHashMap<>();

    static {
        // 安全服务/数据安全工资表
        SALARY_SERVICE_DATA.put("INTERN", new BigDecimal("3000")); // 实习生
        SALARY_SERVICE_DATA.put("P1-1", new BigDecimal("6000"));
        SALARY_SERVICE_DATA.put("P1-2", new BigDecimal("7000"));
        SALARY_SERVICE_DATA.put("P1-3", new BigDecimal("8000"));
        SALARY_SERVICE_DATA.put("P2-1", new BigDecimal("9000"));
        SALARY_SERVICE_DATA.put("P2-2", new BigDecimal("10000"));
        SALARY_SERVICE_DATA.put("P2-3", new BigDecimal("11000"));
        SALARY_SERVICE_DATA.put("P3-1", new BigDecimal("13000"));
        SALARY_SERVICE_DATA.put("P3-2", new BigDecimal("15000"));
        SALARY_SERVICE_DATA.put("P3-3", new BigDecimal("17000"));

        // 安全运营工资表
        SALARY_OPERATION.put("INTERN", new BigDecimal("3000")); // 实习生
        SALARY_OPERATION.put("P1-1", new BigDecimal("5500"));
        SALARY_OPERATION.put("P1-2", new BigDecimal("6000"));
        SALARY_OPERATION.put("P1-3", new BigDecimal("6500"));
        SALARY_OPERATION.put("P2-1", new BigDecimal("7000"));
        SALARY_OPERATION.put("P2-2", new BigDecimal("8000"));
        SALARY_OPERATION.put("P2-3", new BigDecimal("9000"));
        SALARY_OPERATION.put("P3-1", new BigDecimal("10000"));
        SALARY_OPERATION.put("P3-2", new BigDecimal("12000"));
        SALARY_OPERATION.put("P3-3", new BigDecimal("14000"));
    }

    @Resource
    private AdminUserMapper adminUserMapper;

    @Resource
    private DeptService deptService;

    @Resource
    private HolidayMapper holidayMapper;

    @Resource
    private HolidayService holidayService;

    @Resource
    private PositionLevelHistoryService positionLevelHistoryService;

    @Override
    public BigDecimal calculateBaseSalary(Integer deptType, String positionLevel) {
        if (deptType == null) {
            log.debug("部门类型为空，无法计算工资");
            return BigDecimal.ZERO;
        }

        // 标准化职级格式（支持多种格式：P1-1, 初级P1-1, 中级P2-1 等）
        // 如果职级为空或无法识别，normalizePositionLevel 会返回默认值 P1-1
        String normalizedLevel = normalizePositionLevel(positionLevel);

        // 根据部门类型选择工资表
        Map<String, BigDecimal> salaryTable;
        if (deptType == DEPT_TYPE_SECURITY_OPERATION) {
            salaryTable = SALARY_OPERATION;
        } else {
            // 安全服务和数据安全使用相同的工资表
            salaryTable = SALARY_SERVICE_DATA;
        }

        BigDecimal salary = salaryTable.get(normalizedLevel);
        return salary != null ? salary : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal calculateMonthlyCost(BigDecimal salary) {
        if (salary == null || salary.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        // 月成本 = 工资 * 1.3 + 3000
        return salary.multiply(new BigDecimal("1.3")).add(new BigDecimal("3000"));
    }

    @Override
    public int getWorkingDays(int year, int month) {
        // 先尝试从缓存获取
        List<HolidayDO> holidays = holidayMapper.selectByYearAndMonth(year, month);

        if (CollUtil.isEmpty(holidays)) {
            // 如果缓存没有，先同步节假日数据
            holidayService.syncHolidayData(year);
            holidays = holidayMapper.selectByYearAndMonth(year, month);
        }

        // 如果还是没有数据，使用默认计算（只排除周末）
        if (CollUtil.isEmpty(holidays)) {
            return calculateDefaultWorkingDays(year, month);
        }

        // 统计工作日数量
        return (int) holidays.stream()
                .filter(h -> h.getIsWorkday() != null && h.getIsWorkday() == 1)
                .count();
    }

    @Override
    public BigDecimal calculateDailyCost(BigDecimal monthlyCost, int year, int month) {
        if (monthlyCost == null || monthlyCost.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        int workingDays = getWorkingDays(year, month);
        if (workingDays <= 0) {
            return BigDecimal.ZERO;
        }

        // 日成本 = 月成本 / 工作日数，保留2位小数
        return monthlyCost.divide(new BigDecimal(workingDays), 2, RoundingMode.HALF_UP);
    }

    @Override
    public PageResult<UserCostRespVO> getUserCostPage(UserCostPageReqVO reqVO) {
        // 设置默认年份为当前年
        if (reqVO.getYear() == null) {
            reqVO.setYear(LocalDate.now().getYear());
        }

        // 获取部门条件：包含指定部门及其所有子部门
        Set<Long> deptCondition = getDeptCondition(reqVO.getDeptId());

        // 查询用户列表 - 必须有部门的用户
        IPage<AdminUserDO> userPage = adminUserMapper.selectPage(
                new Page<>(reqVO.getPageNo(), reqVO.getPageSize()),
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AdminUserDO>()
                        .isNotNull(AdminUserDO::getDeptId) // 必须有部门
                        .like(StrUtil.isNotEmpty(reqVO.getNickname()), AdminUserDO::getNickname, reqVO.getNickname())
                        .in(CollUtil.isNotEmpty(deptCondition), AdminUserDO::getDeptId, deptCondition)
                        .like(StrUtil.isNotEmpty(reqVO.getPositionLevel()), AdminUserDO::getPositionLevel, reqVO.getPositionLevel())
                        .eq(AdminUserDO::getStatus, 0) // 只查询正常状态的用户
                        .orderByAsc(AdminUserDO::getDeptId, AdminUserDO::getId) // 按部门排序
        );

        if (CollUtil.isEmpty(userPage.getRecords())) {
            return new PageResult<>(Collections.emptyList(), 0L);
        }

        // 获取部门信息
        Set<Long> deptIds = userPage.getRecords().stream()
                .map(AdminUserDO::getDeptId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, DeptDO> deptMap = deptService.getDeptMap(deptIds);

        // 转换为VO，计算年度累计成本
        List<UserCostRespVO> list = userPage.getRecords().stream()
                .map(user -> convertToUserCostVO(user, deptMap, reqVO.getYear()))
                .filter(vo -> {
                    // 如果指定了部门类型，需要过滤
                    if (reqVO.getDeptType() != null) {
                        return reqVO.getDeptType().equals(vo.getDeptType());
                    }
                    return true;
                })
                .filter(vo -> vo.getDeptId() != null) // 再次确保有部门
                .collect(Collectors.toList());

        return new PageResult<>(list, userPage.getTotal());
    }

    @Override
    public UserCostRespVO getUserCost(Long userId, int year, int month) {
        AdminUserDO user = adminUserMapper.selectById(userId);
        if (user == null) {
            return null;
        }

        Map<Long, DeptDO> deptMap = new HashMap<>();
        if (user.getDeptId() != null) {
            DeptDO dept = deptService.getDept(user.getDeptId());
            if (dept != null) {
                deptMap.put(dept.getId(), dept);
            }
        }

        return convertToUserCostVO(user, deptMap, year);
    }

    @Override
    public WorkingDaysRespVO getWorkingDaysInfo(int year, int month) {
        WorkingDaysRespVO respVO = new WorkingDaysRespVO();
        respVO.setYear(year);
        respVO.setMonth(month);

        YearMonth yearMonth = YearMonth.of(year, month);
        int totalDays = yearMonth.lengthOfMonth();
        respVO.setTotalDays(totalDays);

        // 获取节假日数据
        List<HolidayDO> holidays = holidayMapper.selectByYearAndMonth(year, month);
        if (CollUtil.isEmpty(holidays)) {
            // 同步数据
            holidayService.syncHolidayData(year);
            holidays = holidayMapper.selectByYearAndMonth(year, month);
        }

        if (CollUtil.isEmpty(holidays)) {
            // 使用默认计算
            int weekendDays = calculateWeekendDays(year, month);
            respVO.setWeekendDays(weekendDays);
            respVO.setHolidayDays(0);
            respVO.setMakeupWorkDays(0);
            respVO.setWorkingDays(totalDays - weekendDays);
            respVO.setHolidays(Collections.emptyList());
            return respVO;
        }

        // 统计各类天数
        int weekendDays = 0;
        int holidayDays = 0;
        int makeupWorkDays = 0;
        int workingDays = 0;
        List<WorkingDaysRespVO.HolidayDetail> holidayDetails = new ArrayList<>();

        for (HolidayDO holiday : holidays) {
            LocalDate date = holiday.getDate();
            boolean isWeekend = date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
            boolean isHoliday = holiday.getIsHoliday() != null && holiday.getIsHoliday() == 1;
            boolean isWorkday = holiday.getIsWorkday() != null && holiday.getIsWorkday() == 1;

            if (isWeekend && !isWorkday) {
                weekendDays++;
            }
            if (isHoliday && !isWeekend) {
                holidayDays++;
            }
            if (isWeekend && isWorkday) {
                makeupWorkDays++;
            }
            if (isWorkday) {
                workingDays++;
            }

            // 添加节假日详情（只添加非普通工作日的）
            if (isHoliday || (isWeekend && isWorkday)) {
                WorkingDaysRespVO.HolidayDetail detail = new WorkingDaysRespVO.HolidayDetail();
                detail.setDate(date.toString());
                detail.setName(holiday.getHolidayName());
                detail.setIsWorkday(isWorkday);
                holidayDetails.add(detail);
            }
        }

        respVO.setWeekendDays(weekendDays);
        respVO.setHolidayDays(holidayDays);
        respVO.setMakeupWorkDays(makeupWorkDays);
        respVO.setWorkingDays(workingDays);
        respVO.setHolidays(holidayDetails);

        return respVO;
    }

    @Override
    public Map<String, BigDecimal> getSalaryRules() {
        Map<String, BigDecimal> rules = new LinkedHashMap<>();

        // 安全服务
        for (String level : POSITION_LEVELS) {
            rules.put(DEPT_TYPE_SECURITY_SERVICE + "-" + level, SALARY_SERVICE_DATA.get(level));
        }
        // 安全运营
        for (String level : POSITION_LEVELS) {
            rules.put(DEPT_TYPE_SECURITY_OPERATION + "-" + level, SALARY_OPERATION.get(level));
        }
        // 数据安全（同安全服务）
        for (String level : POSITION_LEVELS) {
            rules.put(DEPT_TYPE_DATA_SECURITY + "-" + level, SALARY_SERVICE_DATA.get(level));
        }

        return rules;
    }

    /**
     * 转换用户信息为成本VO（计算年度累计成本）
     */
    private UserCostRespVO convertToUserCostVO(AdminUserDO user, Map<Long, DeptDO> deptMap, int year) {
        UserCostRespVO vo = new UserCostRespVO();
        vo.setUserId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setUsername(user.getUsername());
        vo.setDeptId(user.getDeptId());
        vo.setPositionLevel(user.getPositionLevel());
        vo.setHireDate(user.getHireDate());
        vo.setYear(year);

        // 获取部门信息
        Integer deptType = null;
        String deptName = null;
        if (user.getDeptId() != null && deptMap != null) {
            DeptDO dept = deptMap.get(user.getDeptId());
            if (dept != null) {
                deptName = dept.getName();
                vo.setDeptName(deptName);
                deptType = dept.getDeptType();
                
                // 如果部门没有设置类型，根据部门名称自动推断
                if (deptType == null && StrUtil.isNotEmpty(deptName)) {
                    deptType = inferDeptTypeFromName(deptName);
                }
                
                vo.setDeptType(deptType);
                if (deptType != null) {
                    vo.setDeptTypeName(DEPT_TYPE_NAMES.get(deptType));
                }
            }
        }

        // 计算基础工资和成本
        BigDecimal baseSalary = calculateBaseSalary(deptType, user.getPositionLevel());
        BigDecimal monthlyCost = calculateMonthlyCost(baseSalary);
        
        vo.setBaseSalary(baseSalary);
        vo.setMonthlyCost(monthlyCost);

        // 计算年度累计成本
        calculateYearToDateCost(vo, user, year, monthlyCost);

        return vo;
    }

    /**
     * 计算年度累计成本
     * 支持分段计算：如果员工中途升职，按不同职级分段计算成本
     */
    private void calculateYearToDateCost(UserCostRespVO vo, AdminUserDO user, int year, BigDecimal monthlyCost) {
        LocalDate today = LocalDate.now();
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);
        
        // 计算起始日期：入职日期和年初的较大者
        LocalDate startDate = yearStart;
        if (user.getHireDate() != null) {
            LocalDate hireDate = user.getHireDate().toLocalDate();
            if (hireDate.isAfter(yearStart)) {
                startDate = hireDate;
            }
        }
        
        // 计算截止日期：今天、年末、离职日期的最小者
        LocalDate endDate = today.isBefore(yearEnd) ? today : yearEnd;
        if (user.getResignDate() != null) {
            LocalDate resignDate = user.getResignDate().toLocalDate();
            if (resignDate.isBefore(endDate)) {
                endDate = resignDate;
            }
        }
        
        // 如果起始日期晚于截止日期，说明该年度内没有工作
        if (startDate.isAfter(endDate)) {
            vo.setYearToDateWorkingDays(0);
            vo.setYearToDateCost(BigDecimal.ZERO);
            vo.setCostStartDate(startDate.toString());
            vo.setCostEndDate(endDate.toString());
            vo.setDailyCost(BigDecimal.ZERO);
            vo.setWorkingDays(0);
            return;
        }
        
        vo.setCostStartDate(startDate.toString());
        vo.setCostEndDate(endDate.toString());
        
        // 获取部门类型
        Integer deptType = vo.getDeptType();
        
        // 获取该用户在指定年份的职级变更记录
        List<PositionLevelHistoryDO> histories = positionLevelHistoryService.getHistoryByUserIdAndYear(user.getId(), year);
        log.info("[成本计算] 用户 {} (ID={}) 在 {} 年的职级变更记录数: {}", 
                user.getNickname(), user.getId(), year, histories != null ? histories.size() : 0);
        
        // 计算分段成本
        BigDecimal ytdCost = BigDecimal.ZERO;
        int ytdWorkingDays = 0;
        
        if (CollUtil.isEmpty(histories)) {
            // 没有职级变更记录，使用当前职级计算整段
            log.info("[成本计算] 用户 {} 无职级变更记录，使用当前职级 {} 计算", user.getNickname(), user.getPositionLevel());
            ytdWorkingDays = calculateWorkingDaysBetween(startDate, endDate);
            BigDecimal dailyCost = calculateDailyCostForMonth(monthlyCost, startDate.getYear(), startDate.getMonthValue());
            ytdCost = dailyCost.multiply(new BigDecimal(ytdWorkingDays));
        } else {
            // 有职级变更记录，分段计算
            // 首先需要确定起始职级（可能是年初之前就有的职级）
            String initialLevel = positionLevelHistoryService.getPositionLevelAtDate(user.getId(), startDate.minusDays(1));
            if (initialLevel == null) {
                // 如果找不到历史记录，使用第一条变更记录的旧职级
                initialLevel = histories.get(0).getOldPositionLevel();
                if (initialLevel == null) {
                    initialLevel = user.getPositionLevel(); // 最后回退到当前职级
                }
            }
            
            // 构建分段列表
            List<CostSegment> segments = buildCostSegments(startDate, endDate, initialLevel, histories);
            
            // 计算每段的成本
            for (CostSegment segment : segments) {
                int segmentWorkingDays = calculateWorkingDaysBetween(segment.startDate, segment.endDate);
                ytdWorkingDays += segmentWorkingDays;
                
                // 计算该段的日成本
                BigDecimal segmentBaseSalary = calculateBaseSalary(deptType, segment.positionLevel);
                BigDecimal segmentMonthlyCost = calculateMonthlyCost(segmentBaseSalary);
                BigDecimal segmentDailyCost = calculateDailyCostForMonth(segmentMonthlyCost, 
                        segment.startDate.getYear(), segment.startDate.getMonthValue());
                
                BigDecimal segmentCost = segmentDailyCost.multiply(new BigDecimal(segmentWorkingDays));
                ytdCost = ytdCost.add(segmentCost);
                
                log.info("[成本分段] {} ~ {}: 职级={}, 工作日={}天, 日成本={}, 成本={}", 
                        segment.startDate, segment.endDate, segment.positionLevel, segmentWorkingDays, segmentDailyCost, segmentCost);
            }
        }
        
        vo.setYearToDateWorkingDays(ytdWorkingDays);
        vo.setYearToDateCost(ytdCost);
        
        // 计算当月工作日数和日成本（用于参考，使用当前职级）
        int currentMonthWorkingDays = getWorkingDays(today.getYear(), today.getMonthValue());
        vo.setWorkingDays(currentMonthWorkingDays);
        vo.setMonth(today.getMonthValue());
        
        BigDecimal dailyCost = currentMonthWorkingDays > 0
                ? monthlyCost.divide(new BigDecimal(currentMonthWorkingDays), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        vo.setDailyCost(dailyCost);
    }

    /**
     * 成本计算分段
     */
    private static class CostSegment {
        LocalDate startDate;
        LocalDate endDate;
        String positionLevel;

        CostSegment(LocalDate startDate, LocalDate endDate, String positionLevel) {
            this.startDate = startDate;
            this.endDate = endDate;
            this.positionLevel = positionLevel;
        }
    }

    /**
     * 构建成本计算分段
     */
    private List<CostSegment> buildCostSegments(LocalDate startDate, LocalDate endDate, 
                                                 String initialLevel, List<PositionLevelHistoryDO> histories) {
        List<CostSegment> segments = new ArrayList<>();
        
        LocalDate currentStart = startDate;
        String currentLevel = initialLevel;
        
        for (PositionLevelHistoryDO history : histories) {
            LocalDate effectiveDate = history.getEffectiveDate();
            
            // 如果生效日期在起始日期之前，直接更新当前职级
            if (!effectiveDate.isAfter(startDate)) {
                currentLevel = history.getNewPositionLevel();
                continue;
            }
            
            // 如果生效日期在截止日期之后，忽略
            if (effectiveDate.isAfter(endDate)) {
                break;
            }
            
            // 创建从当前开始到变更前一天的分段
            if (!currentStart.isAfter(effectiveDate.minusDays(1))) {
                segments.add(new CostSegment(currentStart, effectiveDate.minusDays(1), currentLevel));
            }
            
            // 更新为新的职级
            currentStart = effectiveDate;
            currentLevel = history.getNewPositionLevel();
        }
        
        // 添加最后一段
        if (!currentStart.isAfter(endDate)) {
            segments.add(new CostSegment(currentStart, endDate, currentLevel));
        }
        
        return segments;
    }

    /**
     * 计算指定月份的日成本
     */
    private BigDecimal calculateDailyCostForMonth(BigDecimal monthlyCost, int year, int month) {
        int workingDays = getWorkingDays(year, month);
        if (workingDays <= 0) {
            return BigDecimal.ZERO;
        }
        return monthlyCost.divide(new BigDecimal(workingDays), 2, RoundingMode.HALF_UP);
    }

    /**
     * 计算两个日期之间的工作日数
     */
    private int calculateWorkingDaysBetween(LocalDate startDate, LocalDate endDate) {
        // 使用批量查询方法，避免 N+1 查询问题
        return holidayService.countWorkdaysBetween(startDate, endDate);
    }

    /**
     * 根据部门名称推断部门类型
     * 运营服务部 -> 安全运营 (2)
     * 技术服务部/安服部 -> 安全服务 (1)
     * 数据安全部 -> 数据安全 (3)
     */
    private Integer inferDeptTypeFromName(String deptName) {
        if (StrUtil.isEmpty(deptName)) {
            return null;
        }
        
        // 运营服务 -> 安全运营
        if (deptName.contains("运营")) {
            return DEPT_TYPE_SECURITY_OPERATION;
        }
        // 数据安全
        if (deptName.contains("数据安全") || deptName.contains("数据部")) {
            return DEPT_TYPE_DATA_SECURITY;
        }
        // 技术服务、安服 -> 安全服务
        if (deptName.contains("技术服务") || deptName.contains("安服") || deptName.contains("安全服务")) {
            return DEPT_TYPE_SECURITY_SERVICE;
        }
        
        return null;
    }

    /**
     * 标准化职级格式
     * 支持格式：P1-1, 初级P1-1, 中级P2-1, 高级P3-2, 实习 等
     */
    private String normalizePositionLevel(String positionLevel) {
        if (StrUtil.isEmpty(positionLevel)) {
            // 职级为空时，使用默认最低职级 P1-1 进行成本核算
            log.debug("职级为空，使用默认职级 P1-1 进行成本核算");
            return "P1-1";
        }

        // 处理特殊职级：实习
        String trimmedLevel = positionLevel.trim();
        if ("实习".equals(trimmedLevel) || "实习生".equals(trimmedLevel) || "intern".equalsIgnoreCase(trimmedLevel)) {
            return "INTERN";
        }

        // 尝试提取 Px-y 格式
        String pattern = "P[1-3]-[1-3]";
        java.util.regex.Pattern regex = java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = regex.matcher(positionLevel.toUpperCase());

        if (matcher.find()) {
            return matcher.group().toUpperCase();
        }

        // 对于无法识别的职级格式，使用默认最低职级 P1-1
        log.warn("无法识别的职级格式: {}，使用默认职级 P1-1 进行成本核算", positionLevel);
        return "P1-1";
    }

    /**
     * 计算默认工作日数（只排除周末）
     */
    private int calculateDefaultWorkingDays(int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        int totalDays = yearMonth.lengthOfMonth();
        int weekendDays = calculateWeekendDays(year, month);
        return totalDays - weekendDays;
    }

    /**
     * 计算周末天数
     */
    private int calculateWeekendDays(int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        int weekendCount = 0;

        for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
            LocalDate date = LocalDate.of(year, month, day);
            DayOfWeek dayOfWeek = date.getDayOfWeek();
            if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
                weekendCount++;
            }
        }

        return weekendCount;
    }

    /**
     * 获得部门条件：查询指定部门的子部门编号们，包括自身
     *
     * @param deptId 部门编号
     * @return 部门编号集合
     */
    private Set<Long> getDeptCondition(Long deptId) {
        if (deptId == null) {
            return Collections.emptySet();
        }
        Set<Long> deptIds = convertSet(deptService.getChildDeptList(deptId), DeptDO::getId);
        deptIds.add(deptId); // 包括自身
        return deptIds;
    }

    // ========== 批量查询方法（性能优化） ==========

    @Override
    public Map<Long, BigDecimal> batchGetUserYearToDateCost(Collection<Long> userIds, int year, int month) {
        if (CollUtil.isEmpty(userIds)) {
            return Collections.emptyMap();
        }

        log.info("[批量成本计算] 开始计算 {} 个用户的年度累计成本，year={}, month={}", userIds.size(), year, month);
        long startTime = System.currentTimeMillis();

        // 1. 批量查询用户信息
        List<AdminUserDO> users = adminUserMapper.selectBatchIds(userIds);
        if (CollUtil.isEmpty(users)) {
            return Collections.emptyMap();
        }

        // 2. 批量查询部门信息
        Set<Long> deptIds = users.stream()
                .map(AdminUserDO::getDeptId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, DeptDO> deptMap = new HashMap<>();
        if (CollUtil.isNotEmpty(deptIds)) {
            List<DeptDO> depts = deptService.getDeptList(
                    new cn.shuhe.system.module.system.controller.admin.dept.vo.dept.DeptListReqVO());
            for (DeptDO dept : depts) {
                if (deptIds.contains(dept.getId())) {
                    deptMap.put(dept.getId(), dept);
                }
            }
        }

        // 3. 批量计算成本
        Map<Long, BigDecimal> result = new HashMap<>();
        for (AdminUserDO user : users) {
            try {
                UserCostRespVO costVO = convertToUserCostVO(user, deptMap, year);
                BigDecimal cost = costVO != null && costVO.getYearToDateCost() != null 
                        ? costVO.getYearToDateCost() : BigDecimal.ZERO;
                result.put(user.getId(), cost);
            } catch (Exception e) {
                log.warn("[批量成本计算] 用户 {} 成本计算失败: {}", user.getId(), e.getMessage());
                result.put(user.getId(), BigDecimal.ZERO);
            }
        }

        log.info("[批量成本计算] 完成，耗时={}ms", System.currentTimeMillis() - startTime);
        return result;
    }

}
