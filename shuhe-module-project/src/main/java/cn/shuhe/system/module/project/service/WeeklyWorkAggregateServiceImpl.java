package cn.shuhe.system.module.project.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import cn.shuhe.system.framework.common.util.object.BeanUtils;
import cn.shuhe.system.framework.security.core.util.SecurityFrameworkUtils;
import cn.shuhe.system.module.project.controller.admin.vo.GlobalOverviewRespVO;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectReportRespVO;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectWorkRecordRespVO;
import cn.shuhe.system.module.project.controller.admin.vo.TeamOverviewRespVO;
import cn.shuhe.system.module.project.controller.admin.vo.WeeklyWorkAggregateReqVO;
import cn.shuhe.system.module.project.controller.admin.vo.WeeklyWorkAggregateRespVO;
import cn.shuhe.system.module.project.controller.admin.vo.WeeklyWorkAggregateRespVO.DailyWorkVO;
import cn.shuhe.system.module.project.dal.dataobject.DailyManagementRecordDO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectReportDO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectSiteMemberDO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectWorkRecordDO;
import cn.shuhe.system.module.project.dal.mysql.DailyManagementRecordMapper;
import cn.shuhe.system.module.project.dal.mysql.ProjectReportMapper;
import cn.shuhe.system.module.project.dal.mysql.ProjectSiteMemberMapper;
import cn.shuhe.system.module.project.dal.mysql.ProjectWorkRecordMapper;
import cn.shuhe.system.module.system.api.dept.DeptApi;
import cn.shuhe.system.module.system.api.dept.dto.DeptRespDTO;
import cn.shuhe.system.module.system.api.user.AdminUserApi;
import cn.shuhe.system.module.system.api.user.dto.AdminUserRespDTO;
import cn.shuhe.system.module.system.dal.dataobject.holiday.HolidayDO;
import cn.shuhe.system.module.system.service.cost.HolidayService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 周工作聚合服务实现类
 * 聚合日常管理记录和项目管理记录，按日期展示
 */
@Service
@Validated
@Slf4j
public class WeeklyWorkAggregateServiceImpl implements WeeklyWorkAggregateService {

    @Resource
    private DailyManagementRecordMapper dailyManagementRecordMapper;

    @Resource
    private ProjectWorkRecordMapper projectWorkRecordMapper;

    @Resource
    private ProjectSiteMemberMapper projectSiteMemberMapper;

    @Resource
    private ProjectReportMapper projectReportMapper;

    @Resource
    private AdminUserApi adminUserApi;

    @Resource
    private DeptApi deptApi;

    @Resource
    private HolidayService holidayService;

    /**
     * 星期名称映射
     */
    private static final Map<Integer, String> DAY_OF_WEEK_NAMES = Map.of(
            1, "周一",
            2, "周二",
            3, "周三",
            4, "周四",
            5, "周五",
            6, "周六",
            7, "周日"
    );

    @Override
    public WeeklyWorkAggregateRespVO getWeeklyWorkAggregate(WeeklyWorkAggregateReqVO reqVO) {
        // 确定查询的用户ID
        Long userId = reqVO.getUserId();
        if (userId == null) {
            userId = SecurityFrameworkUtils.getLoginUserId();
        }

        // 计算该周的日期范围
        LocalDate[] weekDates = calculateWeekDates(reqVO.getYear(), reqVO.getWeekNumber());
        LocalDate weekStartDate = weekDates[0];
        LocalDate weekEndDate = weekDates[1];

        // 1. 查询日常管理记录
        DailyManagementRecordDO dailyRecord = dailyManagementRecordMapper.selectByCreatorAndYearAndWeek(
                String.valueOf(userId), reqVO.getYear(), reqVO.getWeekNumber());

        // 2. 查询项目管理记录（该周内的所有记录）
        List<ProjectWorkRecordDO> projectRecords = new ArrayList<>();
        if (Boolean.TRUE.equals(reqVO.getIncludeProjectRecords())) {
            projectRecords = projectWorkRecordMapper.selectListByCreatorAndDateRange(userId, weekStartDate, weekEndDate);
        }

        // 3. 按日期分组项目记录
        Map<LocalDate, List<ProjectWorkRecordDO>> projectRecordsByDate = projectRecords.stream()
                .collect(Collectors.groupingBy(ProjectWorkRecordDO::getRecordDate));

        // 4. 查询本周的节假日数据
        Map<LocalDate, HolidayDO> holidayMap = new HashMap<>();
        try {
            List<HolidayDO> monthHolidays = holidayService.getHolidaysByMonth(
                    weekStartDate.getYear(), weekStartDate.getMonthValue());
            for (HolidayDO h : monthHolidays) {
                holidayMap.put(h.getDate(), h);
            }
            // 如果跨月，也加载下个月的
            if (weekStartDate.getMonthValue() != weekEndDate.getMonthValue()) {
                List<HolidayDO> nextMonthHolidays = holidayService.getHolidaysByMonth(
                        weekEndDate.getYear(), weekEndDate.getMonthValue());
                for (HolidayDO h : nextMonthHolidays) {
                    holidayMap.put(h.getDate(), h);
                }
            }
        } catch (Exception e) {
            log.warn("获取节假日数据失败，使用默认周末判断", e);
        }

        // 5. 构建每日工作列表（周一到周日，前端根据工作日状态决定显示哪些天）
        List<DailyWorkVO> dailyWorks = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = 0; i < 7; i++) {
            LocalDate currentDate = weekStartDate.plusDays(i);
            DailyWorkVO dailyWork = new DailyWorkVO();
            dailyWork.setDate(currentDate);
            dailyWork.setDayOfWeek(currentDate.getDayOfWeek().getValue());
            dailyWork.setDayOfWeekName(DAY_OF_WEEK_NAMES.get(currentDate.getDayOfWeek().getValue()));
            dailyWork.setIsToday(currentDate.equals(today));

            // 设置工作日状态
            HolidayDO holiday = holidayMap.get(currentDate);
            if (holiday != null) {
                dailyWork.setIsWorkday(holiday.getIsWorkday() != null && holiday.getIsWorkday() == 1);
                dailyWork.setHolidayName(holiday.getHolidayName());
            } else {
                // 无节假日数据时按周末判断
                boolean isWeekend = currentDate.getDayOfWeek() == DayOfWeek.SATURDAY
                        || currentDate.getDayOfWeek() == DayOfWeek.SUNDAY;
                dailyWork.setIsWorkday(!isWeekend);
            }

            // 设置日常管理内容
            if (dailyRecord != null) {
                dailyWork.setDailyContent(getDailyContentByDayOfWeek(dailyRecord, currentDate.getDayOfWeek().getValue()));
            }

            // 设置项目记录
            List<ProjectWorkRecordDO> dayProjectRecords = projectRecordsByDate.getOrDefault(currentDate, Collections.emptyList());
            if (Boolean.TRUE.equals(reqVO.getIncludeProjectRecords())) {
                dailyWork.setProjectRecords(convertToProjectRecordRespVOs(dayProjectRecords));
            }
            dailyWork.setProjectRecordCount(dayProjectRecords.size());

            dailyWorks.add(dailyWork);
        }

        // 5. 构建响应
        WeeklyWorkAggregateRespVO respVO = new WeeklyWorkAggregateRespVO();
        respVO.setYear(reqVO.getYear());
        respVO.setWeekNumber(reqVO.getWeekNumber());
        respVO.setWeekStartDate(weekStartDate);
        respVO.setWeekEndDate(weekEndDate);
        respVO.setDailyWorks(dailyWorks);

        if (dailyRecord != null) {
            respVO.setDailyRecordId(dailyRecord.getId());
            respVO.setWeeklySummary(dailyRecord.getWeeklySummary());
            respVO.setNextWeekPlan(dailyRecord.getNextWeekPlan());
            if (dailyRecord.getAttachments() != null && !dailyRecord.getAttachments().isEmpty()) {
                respVO.setDailyRecordAttachments(JSONUtil.toList(dailyRecord.getAttachments(), String.class));
            }
        }

        return respVO;
    }

    @Override
    public WeeklyWorkAggregateRespVO getCurrentWeekAggregate() {
        LocalDate today = LocalDate.now();
        WeekFields weekFields = WeekFields.of(Locale.getDefault());

        int year = today.getYear();
        int weekNumber = today.get(weekFields.weekOfWeekBasedYear());

        // 计算本周一
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        // 如果周一在下一年，调整年份和周数
        if (monday.getYear() != year) {
            year = monday.getYear();
            weekNumber = monday.get(weekFields.weekOfWeekBasedYear());
        }

        WeeklyWorkAggregateReqVO reqVO = new WeeklyWorkAggregateReqVO();
        reqVO.setYear(year);
        reqVO.setWeekNumber(weekNumber);
        reqVO.setIncludeProjectRecords(true);

        return getWeeklyWorkAggregate(reqVO);
    }

    /**
     * 计算指定年份和周数的周一和周五日期
     */
    private LocalDate[] calculateWeekDates(int year, int weekNumber) {
        WeekFields weekFields = WeekFields.of(Locale.getDefault());

        // 获取该年第一天
        LocalDate firstDayOfYear = LocalDate.of(year, 1, 1);
        
        // 计算该周的周一
        LocalDate monday = firstDayOfYear
                .with(weekFields.weekOfWeekBasedYear(), weekNumber)
                .with(DayOfWeek.MONDAY);
        
        // 周五
        LocalDate friday = monday.plusDays(4);

        return new LocalDate[]{monday, friday};
    }

    /**
     * 根据星期几获取日常管理记录中对应的内容
     */
    private String getDailyContentByDayOfWeek(DailyManagementRecordDO record, int dayOfWeek) {
        return switch (dayOfWeek) {
            case 1 -> record.getMondayContent();
            case 2 -> record.getTuesdayContent();
            case 3 -> record.getWednesdayContent();
            case 4 -> record.getThursdayContent();
            case 5 -> record.getFridayContent();
            default -> null;
        };
    }

    /**
     * 将项目工作记录 DO 转换为 RespVO
     */
    private List<ProjectWorkRecordRespVO> convertToProjectRecordRespVOs(List<ProjectWorkRecordDO> records) {
        if (CollUtil.isEmpty(records)) {
            return Collections.emptyList();
        }
        return records.stream()
                .map(this::convertToProjectRecordRespVO)
                .collect(Collectors.toList());
    }

    /**
     * 单个项目工作记录 DO 转 RespVO
     */
    private ProjectWorkRecordRespVO convertToProjectRecordRespVO(ProjectWorkRecordDO record) {
        ProjectWorkRecordRespVO respVO = BeanUtils.toBean(record, ProjectWorkRecordRespVO.class);
        // 处理附件（JSON转List）
        if (record.getAttachments() != null && !record.getAttachments().isEmpty()) {
            respVO.setAttachments(JSONUtil.toList(record.getAttachments(), String.class));
        }
        return respVO;
    }

    /**
     * 总经办部门名称（总经理所在部门）
     */
    private static final String GENERAL_MANAGER_DEPT_NAME = "总经办";

    @Override
    public List<Map<String, Object>> getViewableUserList() {
        Long currentUserId = SecurityFrameworkUtils.getLoginUserId();
        AdminUserRespDTO currentUser = adminUserApi.getUser(currentUserId);
        
        List<Map<String, Object>> result = new ArrayList<>();
        
        // 1. 先添加自己（放在第一位）
        Map<String, Object> selfMap = new HashMap<>();
        selfMap.put("id", currentUserId);
        selfMap.put("nickname", currentUser != null ? currentUser.getNickname() : "当前用户");
        selfMap.put("deptName", "");
        selfMap.put("isSelf", true);
        
        DeptRespDTO currentDept = null;
        if (currentUser != null && currentUser.getDeptId() != null) {
            currentDept = deptApi.getDept(currentUser.getDeptId());
            if (currentDept != null) {
                selfMap.put("deptName", currentDept.getName());
            }
        }
        result.add(selfMap);
        
        // 2. 获取当前用户的部门
        if (currentUser == null || currentUser.getDeptId() == null) {
            return result;
        }
        
        Long deptId = currentUser.getDeptId();
        
        // 3. 判断是否是总经办（总经理）- 可以查看所有部门
        boolean isGeneralManager = currentDept != null && 
                                   GENERAL_MANAGER_DEPT_NAME.equals(currentDept.getName());
        
        // 4. 判断当前用户是否是部门领导
        boolean isLeader = currentDept != null && 
                           currentDept.getLeaderUserId() != null && 
                           currentDept.getLeaderUserId().equals(currentUserId);
        
        // 如果不是领导且不是总经办，只能看自己
        if (!isLeader && !isGeneralManager) {
            log.info("【周工作日历】用户 {} 不是部门领导也不是总经办，只能查看自己的数据", currentUserId);
            return result;
        }
        
        Set<Long> allDeptIds = new HashSet<>();
        Map<Long, String> deptNameMap = new HashMap<>();
        
        if (isGeneralManager) {
            // 5a. 总经办可以看所有业务部门（安全服务、安全运营、数据安全）
            log.info("【周工作日历】用户 {} 是总经办成员，可查看所有部门数据", currentUserId);
            
            // 获取三种类型的业务部门：1-安全服务 2-安全运营 3-数据安全
            for (int deptType = 1; deptType <= 3; deptType++) {
                List<DeptRespDTO> deptsByType = deptApi.getDeptListByDeptType(deptType);
                if (CollUtil.isNotEmpty(deptsByType)) {
                    for (DeptRespDTO dept : deptsByType) {
                        allDeptIds.add(dept.getId());
                        deptNameMap.put(dept.getId(), dept.getName());
                        
                        // 获取该部门的所有子部门
                        List<DeptRespDTO> childDepts = deptApi.getChildDeptList(dept.getId());
                        if (CollUtil.isNotEmpty(childDepts)) {
                            for (DeptRespDTO child : childDepts) {
                                allDeptIds.add(child.getId());
                                deptNameMap.put(child.getId(), child.getName());
                            }
                        }
                    }
                }
            }
            
            // 也添加总经办和人事行政部
            allDeptIds.add(deptId);
            deptNameMap.put(deptId, currentDept.getName());
        } else {
            // 5b. 普通领导只能看本部门及子部门
            allDeptIds.add(deptId);
            if (currentDept != null) {
                deptNameMap.put(deptId, currentDept.getName());
            }
            
            // 获取所有子部门
            List<DeptRespDTO> childDepts = deptApi.getChildDeptList(deptId);
            if (CollUtil.isNotEmpty(childDepts)) {
                for (DeptRespDTO child : childDepts) {
                    allDeptIds.add(child.getId());
                    deptNameMap.put(child.getId(), child.getName());
                }
            }
            
            log.info("【周工作日历】领导 {} 可查看部门: {}", currentUserId, allDeptIds);
        }
        
        // 6. 获取这些部门的所有用户
        List<AdminUserRespDTO> users = adminUserApi.getUserListByDeptIds(allDeptIds);
        if (CollUtil.isEmpty(users)) {
            return result;
        }
        
        // 7. 构建用户列表（排除自己，因为已经添加过了）
        for (AdminUserRespDTO user : users) {
            // 跳过自己（已经在第一位了）
            if (user.getId().equals(currentUserId)) {
                continue;
            }
            
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", user.getId());
            userMap.put("nickname", user.getNickname());
            userMap.put("deptName", user.getDeptId() != null ? deptNameMap.getOrDefault(user.getDeptId(), "") : "");
            userMap.put("isSelf", false);
            result.add(userMap);
        }
        
        log.info("【周工作日历】用户 {} 可查看 {} 个员工", currentUserId, result.size());
        
        return result;
    }

    @Override
    public TeamOverviewRespVO getTeamOverview(Integer year, Integer weekNumber) {
        // 1. 获取可查看的用户列表
        List<Map<String, Object>> viewableUsers = getViewableUserList();

        // 2. 计算周日期范围
        LocalDate[] weekDates = calculateWeekDates(year, weekNumber);
        LocalDate weekStartDate = weekDates[0];
        LocalDate weekEndDate = weekDates[1];

        // 3. 构建响应
        TeamOverviewRespVO respVO = new TeamOverviewRespVO();
        respVO.setYear(year);
        respVO.setWeekNumber(weekNumber);
        respVO.setWeekStartDate(weekStartDate);
        respVO.setWeekEndDate(weekEndDate);

        List<TeamOverviewRespVO.MemberWorkSummary> memberSummaries = new ArrayList<>();

        for (Map<String, Object> userMap : viewableUsers) {
            Long userId = ((Number) userMap.get("id")).longValue();
            String nickname = (String) userMap.get("nickname");
            String deptName = (String) userMap.get("deptName");

            TeamOverviewRespVO.MemberWorkSummary summary = new TeamOverviewRespVO.MemberWorkSummary();
            summary.setUserId(userId);
            summary.setNickname(nickname);
            summary.setDeptName(deptName);

            summary.setWorkMode(resolveUserWorkMode(userId));

            // 判断是否为管理人员（部门负责人）
            AdminUserRespDTO memberUser = adminUserApi.getUser(userId);
            boolean isManager = false;
            if (memberUser != null && memberUser.getDeptId() != null) {
                DeptRespDTO memberDept = deptApi.getDept(memberUser.getDeptId());
                if (memberDept != null && memberDept.getLeaderUserId() != null
                        && memberDept.getLeaderUserId().equals(userId)) {
                    isManager = true;
                }
                // 也检查上级部门负责人
                if (!isManager && memberDept != null && memberDept.getParentId() != null) {
                    DeptRespDTO parentDept = deptApi.getDept(memberDept.getParentId());
                    if (parentDept != null && parentDept.getLeaderUserId() != null
                            && parentDept.getLeaderUserId().equals(userId)) {
                        isManager = true;
                    }
                }
            }
            // 也检查 project_site_member 里的管理人员类型
            if (!isManager) {
                List<ProjectSiteMemberDO> activeMembers = projectSiteMemberMapper.selectActiveByUserIds(
                        Collections.singletonList(userId));
                isManager = activeMembers.stream().anyMatch(m ->
                        m.getMemberType() != null && m.getMemberType() == ProjectSiteMemberDO.MEMBER_TYPE_MANAGEMENT);
            }
            summary.setIsManager(isManager);

            // 查询日常管理记录
            DailyManagementRecordDO dailyRecord = dailyManagementRecordMapper.selectByCreatorAndYearAndWeek(
                    String.valueOf(userId), year, weekNumber);

            // 查询项目工作记录
            List<ProjectWorkRecordDO> projectRecords = projectWorkRecordMapper.selectListByCreatorAndDateRange(
                    userId, weekStartDate, weekEndDate);

            // 按日期分组项目记录
            Map<LocalDate, List<ProjectWorkRecordDO>> projectRecordsByDate = projectRecords.stream()
                    .collect(Collectors.groupingBy(ProjectWorkRecordDO::getRecordDate));

            // 构建每日详情
            List<TeamOverviewRespVO.DayDetail> dayDetails = new ArrayList<>();
            int dailyContentDays = 0;

            for (int i = 0; i < 5; i++) {
                LocalDate currentDate = weekStartDate.plusDays(i);
                TeamOverviewRespVO.DayDetail dayDetail = new TeamOverviewRespVO.DayDetail();
                dayDetail.setDate(currentDate);
                dayDetail.setDayOfWeekName(DAY_OF_WEEK_NAMES.get(currentDate.getDayOfWeek().getValue()));

                if (dailyRecord != null) {
                    String content = getDailyContentByDayOfWeek(dailyRecord, currentDate.getDayOfWeek().getValue());
                    dayDetail.setDailyContent(content);
                    if (content != null && !content.trim().isEmpty()) {
                        dailyContentDays++;
                    }
                }

                List<ProjectWorkRecordDO> dayProjectRecords = projectRecordsByDate.getOrDefault(currentDate, Collections.emptyList());
                dayDetail.setProjectRecords(convertToProjectRecordRespVOs(dayProjectRecords));
                dayDetail.setProjectRecordCount(dayProjectRecords.size());

                dayDetails.add(dayDetail);
            }

            summary.setDayDetails(dayDetails);
            summary.setProjectRecordCount(projectRecords.size());
            summary.setDailyContentDays(dailyContentDays);
            summary.setHasWeeklySummary(dailyRecord != null && dailyRecord.getWeeklySummary() != null
                    && !dailyRecord.getWeeklySummary().trim().isEmpty());
            summary.setWeeklySummary(dailyRecord != null ? dailyRecord.getWeeklySummary() : null);
            summary.setNextWeekPlan(dailyRecord != null ? dailyRecord.getNextWeekPlan() : null);

            memberSummaries.add(summary);
        }

        respVO.setMemberSummaries(memberSummaries);
        return respVO;
    }

    @Override
    public Integer getCurrentUserWorkMode() {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        return resolveUserWorkMode(userId);
    }

    @Resource
    private cn.shuhe.system.module.project.dal.mysql.ProjectMapper projectMapper;

    @Resource
    private cn.shuhe.system.module.project.dal.mysql.ProjectSiteMapper projectSiteMapper;

    @Override
    public Map<String, Object> getCurrentUserWorkModeInfo() {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        Integer workMode = resolveUserWorkMode(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("workMode", workMode);

        if (workMode == 1) {
            // 查询在岗的驻场分配信息
            List<ProjectSiteMemberDO> activeMembers = projectSiteMemberMapper.selectActiveByUserIds(
                    Collections.singletonList(userId));
            if (CollUtil.isNotEmpty(activeMembers)) {
                List<Map<String, Object>> siteProjects = new ArrayList<>();
                for (ProjectSiteMemberDO member : activeMembers) {
                    Map<String, Object> projectInfo = new HashMap<>();
                    projectInfo.put("projectId", member.getProjectId());
                    projectInfo.put("siteId", member.getSiteId());
                    projectInfo.put("positionName", member.getPositionName());
                    projectInfo.put("isLeader", member.getIsLeader());
                    projectInfo.put("startDate", member.getStartDate());

                    // 获取项目名称和客户信息
                    if (member.getProjectId() != null) {
                        cn.shuhe.system.module.project.dal.dataobject.ProjectDO project =
                                projectMapper.selectById(member.getProjectId());
                        if (project != null) {
                            projectInfo.put("projectName", project.getName());
                            projectInfo.put("customerName", project.getCustomerName());
                        }
                    }

                    // 获取驻场点名称
                    if (member.getSiteId() != null) {
                        cn.shuhe.system.module.project.dal.dataobject.ProjectSiteDO site =
                                projectSiteMapper.selectById(member.getSiteId());
                        if (site != null) {
                            projectInfo.put("siteName", site.getName());
                            projectInfo.put("siteAddress", site.getAddress());
                        }
                    }

                    siteProjects.add(projectInfo);
                }
                result.put("siteProjects", siteProjects);
            }
        }

        return result;
    }

    /**
     * 综合判断用户的工作模式
     * 返回值：1-驻场(在岗) 2-二线 3-未入场(驻场部门但不在项目上)
     *
     * 逻辑：
     * 1. 沿部门树向上查找 workMode 设置
     * 2. 驻场部门 + 在岗 → 1(驻场)
     * 3. 驻场部门 + 不在岗 → 3(未入场)
     * 4. 二线部门 → 2(二线)
     * 5. 查不到部门 → 用 project_site_member 兜底
     */
    private Integer resolveUserWorkMode(Long userId) {
        AdminUserRespDTO user = adminUserApi.getUser(userId);
        if (user == null || user.getDeptId() == null) {
            return projectSiteMemberMapper.isUserOnSite(userId) ? 1 : 2;
        }

        try {
            Integer deptWorkMode = findDeptWorkModeRecursively(user.getDeptId());

            if (deptWorkMode != null && deptWorkMode == 1) {
                // 驻场部门：在岗→1, 不在岗→3(未入场)
                return projectSiteMemberMapper.isUserOnSite(userId) ? 1 : 3;
            }

            if (deptWorkMode != null) {
                return 2; // 明确设置为二线
            }
        } catch (Exception e) {
            log.warn("【工作模式】查询部门workMode失败，使用驻场人员表兜底，userId={}", userId, e);
        }

        return projectSiteMemberMapper.isUserOnSite(userId) ? 1 : 2;
    }

    /**
     * 沿部门树向上递归查找 workMode（最多向上查5级防止死循环）
     */
    private Integer findDeptWorkModeRecursively(Long deptId) {
        return findDeptWorkModeRecursively(deptId, 5);
    }

    private Integer findDeptWorkModeRecursively(Long deptId, int maxDepth) {
        if (deptId == null || deptId == 0 || maxDepth <= 0) {
            return null;
        }
        DeptRespDTO dept = deptApi.getDept(deptId);
        if (dept == null) {
            return null;
        }
        if (dept.getWorkMode() != null) {
            return dept.getWorkMode();
        }
        return findDeptWorkModeRecursively(dept.getParentId(), maxDepth - 1);
    }

    @Override
    public GlobalOverviewRespVO getGlobalOverview(Integer year, Integer weekNumber) {
        LocalDate[] weekDates = calculateWeekDates(year, weekNumber);
        LocalDate weekStartDate = weekDates[0];
        LocalDate weekEndDate = weekDates[1];

        GlobalOverviewRespVO resp = new GlobalOverviewRespVO();
        resp.setYear(year);
        resp.setWeekNumber(weekNumber);
        resp.setWeekStartDate(weekStartDate);
        resp.setWeekEndDate(weekEndDate);

        // 获取所有业务部门及其子部门
        Set<Long> allDeptIds = new HashSet<>();
        Map<Long, String> deptNameMap = new HashMap<>();
        // 收集顶级业务部门 ID 用于按部门分组
        Map<Long, String> topDeptMap = new LinkedHashMap<>();

        for (int deptType = 1; deptType <= 3; deptType++) {
            List<DeptRespDTO> deptsByType = deptApi.getDeptListByDeptType(deptType);
            if (CollUtil.isNotEmpty(deptsByType)) {
                for (DeptRespDTO dept : deptsByType) {
                    allDeptIds.add(dept.getId());
                    deptNameMap.put(dept.getId(), dept.getName());
                    if (dept.getParentId() == null || dept.getParentId() == 0) {
                        topDeptMap.put(dept.getId(), dept.getName());
                    }
                    List<DeptRespDTO> childDepts = deptApi.getChildDeptList(dept.getId());
                    if (CollUtil.isNotEmpty(childDepts)) {
                        for (DeptRespDTO child : childDepts) {
                            allDeptIds.add(child.getId());
                            deptNameMap.put(child.getId(), child.getName());
                        }
                    }
                }
            }
        }

        // 获取所有用户
        List<AdminUserRespDTO> allUsers = adminUserApi.getUserListByDeptIds(allDeptIds);
        if (CollUtil.isEmpty(allUsers)) {
            resp.setTotalStaff(0);
            resp.setOnSiteCount(0);
            resp.setPendingCount(0);
            resp.setBackOfficeCount(0);
            resp.setFilledCount(0);
            resp.setUnfilledCount(0);
            resp.setDeptStats(Collections.emptyList());
            resp.setProjectStats(Collections.emptyList());
            resp.setManagerReports(Collections.emptyList());
            resp.setUnfilledMembers(Collections.emptyList());
            return resp;
        }

        // 计算每个用户的工作模式
        Map<Long, Integer> userWorkModeMap = new HashMap<>();
        int onSite = 0, pending = 0, backOffice = 0;
        for (AdminUserRespDTO user : allUsers) {
            Integer mode = resolveUserWorkMode(user.getId());
            userWorkModeMap.put(user.getId(), mode);
            if (mode == 1) onSite++;
            else if (mode == 3) pending++;
            else backOffice++;
        }

        resp.setTotalStaff(allUsers.size());
        resp.setOnSiteCount(onSite);
        resp.setPendingCount(pending);
        resp.setBackOfficeCount(backOffice);

        // 查询所有人本周的记录填写情况
        Set<Long> filledUserIds = new HashSet<>();

        // 日常管理记录
        List<DailyManagementRecordDO> allDailyRecords = dailyManagementRecordMapper.selectListByYearAndWeek(year, weekNumber);
        for (DailyManagementRecordDO r : allDailyRecords) {
            if (r.getCreator() != null) {
                filledUserIds.add(Long.parseLong(r.getCreator()));
            }
        }

        // 项目工作记录
        Map<Long, List<ProjectWorkRecordDO>> allProjectRecords = new HashMap<>();
        for (AdminUserRespDTO user : allUsers) {
            List<ProjectWorkRecordDO> records = projectWorkRecordMapper.selectListByCreatorAndDateRange(
                    user.getId(), weekStartDate, weekEndDate);
            if (CollUtil.isNotEmpty(records)) {
                filledUserIds.add(user.getId());
                allProjectRecords.put(user.getId(), records);
            }
        }

        resp.setFilledCount(filledUserIds.size());
        resp.setUnfilledCount(allUsers.size() - filledUserIds.size());

        // 未填写记录的人员名单
        List<GlobalOverviewRespVO.UnfilledMember> unfilledMembers = new ArrayList<>();
        for (AdminUserRespDTO user : allUsers) {
            if (!filledUserIds.contains(user.getId())) {
                GlobalOverviewRespVO.UnfilledMember m = new GlobalOverviewRespVO.UnfilledMember();
                m.setUserId(user.getId());
                m.setNickname(user.getNickname());
                m.setDeptName(user.getDeptId() != null ? deptNameMap.getOrDefault(user.getDeptId(), "") : "");
                m.setWorkMode(userWorkModeMap.getOrDefault(user.getId(), 2));
                unfilledMembers.add(m);
            }
        }
        resp.setUnfilledMembers(unfilledMembers);

        // 按顶级部门统计
        List<GlobalOverviewRespVO.DeptStat> deptStats = new ArrayList<>();
        for (Map.Entry<Long, String> entry : topDeptMap.entrySet()) {
            Long topDeptId = entry.getKey();
            String topDeptName = entry.getValue();

            // 获取该顶级部门下所有子部门ID
            Set<Long> subDeptIds = new HashSet<>();
            subDeptIds.add(topDeptId);
            List<DeptRespDTO> children = deptApi.getChildDeptList(topDeptId);
            if (CollUtil.isNotEmpty(children)) {
                for (DeptRespDTO c : children) {
                    subDeptIds.add(c.getId());
                }
            }

            // 筛选该部门的用户
            List<AdminUserRespDTO> deptUsers = allUsers.stream()
                    .filter(u -> u.getDeptId() != null && subDeptIds.contains(u.getDeptId()))
                    .collect(Collectors.toList());

            GlobalOverviewRespVO.DeptStat stat = new GlobalOverviewRespVO.DeptStat();
            stat.setDeptId(topDeptId);
            stat.setDeptName(topDeptName);
            stat.setTotalMembers(deptUsers.size());
            stat.setOnSiteCount((int) deptUsers.stream()
                    .filter(u -> userWorkModeMap.getOrDefault(u.getId(), 2) == 1).count());
            stat.setPendingCount((int) deptUsers.stream()
                    .filter(u -> userWorkModeMap.getOrDefault(u.getId(), 2) == 3).count());
            stat.setBackOfficeCount((int) deptUsers.stream()
                    .filter(u -> userWorkModeMap.getOrDefault(u.getId(), 2) == 2).count());

            // 该部门的项目记录数
            int deptProjectRecords = 0;
            int deptFilledCount = 0;
            for (AdminUserRespDTO u : deptUsers) {
                List<ProjectWorkRecordDO> userRecords = allProjectRecords.get(u.getId());
                if (userRecords != null) {
                    deptProjectRecords += userRecords.size();
                }
                if (filledUserIds.contains(u.getId())) {
                    deptFilledCount++;
                }
            }
            stat.setProjectRecordCount(deptProjectRecords);
            stat.setFilledCount(deptFilledCount);
            stat.setUnfilledCount(deptUsers.size() - deptFilledCount);

            deptStats.add(stat);
        }
        resp.setDeptStats(deptStats);

        // 按项目统计（从所有项目工作记录中提取）
        Map<Long, GlobalOverviewRespVO.ProjectStat> projectStatMap = new LinkedHashMap<>();
        for (Map.Entry<Long, List<ProjectWorkRecordDO>> e : allProjectRecords.entrySet()) {
            Long userId = e.getKey();
            AdminUserRespDTO user = allUsers.stream().filter(u -> u.getId().equals(userId)).findFirst().orElse(null);
            String userName = user != null ? user.getNickname() : "";

            for (ProjectWorkRecordDO record : e.getValue()) {
                if (record.getProjectId() == null) continue;
                GlobalOverviewRespVO.ProjectStat ps = projectStatMap.computeIfAbsent(record.getProjectId(), k -> {
                    GlobalOverviewRespVO.ProjectStat s = new GlobalOverviewRespVO.ProjectStat();
                    s.setProjectId(record.getProjectId());
                    s.setProjectName(record.getProjectName());
                    s.setRecordCount(0);
                    s.setMemberCount(0);
                    s.setMemberNames(new ArrayList<>());
                    return s;
                });
                ps.setRecordCount(ps.getRecordCount() + 1);
                if (!ps.getMemberNames().contains(userName)) {
                    ps.getMemberNames().add(userName);
                    ps.setMemberCount(ps.getMemberNames().size());
                }
            }
        }

        // 获取项目客户名称
        for (GlobalOverviewRespVO.ProjectStat ps : projectStatMap.values()) {
            try {
                cn.shuhe.system.module.project.dal.dataobject.ProjectDO project = projectMapper.selectById(ps.getProjectId());
                if (project != null) {
                    ps.setCustomerName(project.getCustomerName());
                }
            } catch (Exception ignored) {}
        }

        List<GlobalOverviewRespVO.ProjectStat> projectStats = new ArrayList<>(projectStatMap.values());
        projectStats.sort((a, b) -> b.getRecordCount() - a.getRecordCount());
        resp.setProjectStats(projectStats);

        // 各主管的周报/管理总结
        List<GlobalOverviewRespVO.ManagerReport> managerReports = new ArrayList<>();
        for (Map.Entry<Long, String> entry : topDeptMap.entrySet()) {
            Long topDeptId = entry.getKey();
            // 找这个部门及子部门的负责人
            Set<Long> subDeptIds = new HashSet<>();
            subDeptIds.add(topDeptId);
            List<DeptRespDTO> children = deptApi.getChildDeptList(topDeptId);
            if (CollUtil.isNotEmpty(children)) {
                for (DeptRespDTO c : children) {
                    subDeptIds.add(c.getId());
                }
            }

            // 收集所有负责人
            Set<Long> leaderIds = new HashSet<>();
            for (Long did : subDeptIds) {
                DeptRespDTO d = deptApi.getDept(did);
                if (d != null && d.getLeaderUserId() != null) {
                    leaderIds.add(d.getLeaderUserId());
                }
            }

            for (Long leaderId : leaderIds) {
                // 避免重复
                if (managerReports.stream().anyMatch(r -> r.getUserId().equals(leaderId))) continue;

                AdminUserRespDTO leader = adminUserApi.getUser(leaderId);
                if (leader == null) continue;

                GlobalOverviewRespVO.ManagerReport report = new GlobalOverviewRespVO.ManagerReport();
                report.setUserId(leaderId);
                report.setNickname(leader.getNickname());
                report.setDeptName(leader.getDeptId() != null ? deptNameMap.getOrDefault(leader.getDeptId(), "") : "");

                // 查询该主管的日常管理记录（周总结）
                DailyManagementRecordDO leaderDailyRecord = dailyManagementRecordMapper.selectByCreatorAndYearAndWeek(
                        String.valueOf(leaderId), year, weekNumber);
                if (leaderDailyRecord != null) {
                    report.setWeeklySummary(leaderDailyRecord.getWeeklySummary());
                    report.setNextWeekPlan(leaderDailyRecord.getNextWeekPlan());
                    report.setHasFilled(true);
                } else {
                    report.setHasFilled(false);
                }

                // 查询该主管写的项目反馈
                List<ProjectReportDO> leaderProjectReports = projectReportMapper.selectList(
                        new cn.shuhe.system.framework.mybatis.core.query.LambdaQueryWrapperX<ProjectReportDO>()
                                .eq(ProjectReportDO::getCreator, String.valueOf(leaderId))
                                .eq(ProjectReportDO::getYear, year)
                                .eq(ProjectReportDO::getWeekNumber, weekNumber));
                if (CollUtil.isNotEmpty(leaderProjectReports)) {
                    report.setProjectReports(leaderProjectReports.stream()
                            .map(r -> {
                                ProjectReportRespVO vo = BeanUtils.toBean(r, ProjectReportRespVO.class);
                                if (r.getAttachments() != null && !r.getAttachments().isEmpty()) {
                                    vo.setAttachments(cn.hutool.json.JSONUtil.toList(r.getAttachments(), String.class));
                                }
                                return vo;
                            }).collect(Collectors.toList()));
                }

                managerReports.add(report);
            }
        }
        resp.setManagerReports(managerReports);

        return resp;
    }

}
