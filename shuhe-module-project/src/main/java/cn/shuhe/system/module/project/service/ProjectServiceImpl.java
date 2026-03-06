package cn.shuhe.system.module.project.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.common.util.object.BeanUtils;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectPageReqVO;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectSaveReqVO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectDO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectMemberDO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectDeptServiceDO;
import cn.shuhe.system.module.project.dal.mysql.ProjectDeptServiceMapper;
import cn.shuhe.system.module.project.dal.mysql.ProjectMapper;
import cn.shuhe.system.module.project.dal.mysql.ProjectMemberMapper;
import cn.shuhe.system.module.system.api.dingtalk.DingtalkNotifyApi;
import cn.shuhe.system.module.system.api.permission.PermissionApi;
import cn.shuhe.system.module.system.controller.admin.cost.vo.UserCostRespVO;
import cn.shuhe.system.module.system.controller.admin.dashboard.vo.DashboardStatisticsRespVO;
import cn.shuhe.system.module.system.service.cost.CostCalculationService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static cn.shuhe.system.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.shuhe.system.module.project.enums.ErrorCodeConstants.*;

/**
 * 项目 Service 实现类（顶层项目）
 */
@Service
@Validated
@Slf4j
public class ProjectServiceImpl implements ProjectService {

    @Resource
    private ProjectMapper projectMapper;

    @Resource
    private ProjectMemberMapper projectMemberMapper;

    @Resource
    private ProjectDeptServiceMapper projectDeptServiceMapper;

    @Resource
    private cn.shuhe.system.module.project.dal.mysql.ServiceItemMapper serviceItemMapper;

    @Resource
    @org.springframework.context.annotation.Lazy // 延迟加载，避免循环依赖
    private ServiceItemService serviceItemService;

    @Resource
    private PermissionApi permissionApi;

    @Resource
    private DingtalkNotifyApi dingtalkNotifyApi;

    @Resource
    private CostCalculationService costCalculationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createProject(ProjectSaveReqVO createReqVO) {
        // 1. 生成项目编号
        String code = generateProjectCode(createReqVO.getDeptType());

        // 2. 转换并保存
        ProjectDO project = BeanUtils.toBean(createReqVO, ProjectDO.class);
        project.setCode(code);
        if (project.getStatus() == null) {
            project.setStatus(0); // 默认草稿状态
        }

        projectMapper.insert(project);
        return project.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProject(ProjectSaveReqVO updateReqVO) {
        // 1. 校验存在
        ProjectDO existing = validateProjectExists(updateReqVO.getId());

        // 2. 更新
        ProjectDO updateObj = BeanUtils.toBean(updateReqVO, ProjectDO.class);
        projectMapper.updateById(updateObj);

        // 3. 检测新增的负责人，加入钉钉群
        if (updateReqVO.getManagerIds() != null && !updateReqVO.getManagerIds().isEmpty()) {
            List<Long> oldManagerIds = existing.getManagerIds() != null ? existing.getManagerIds() : List.of();
            List<Long> newManagerIds = updateReqVO.getManagerIds().stream()
                    .filter(id -> !oldManagerIds.contains(id))
                    .toList();
            if (!newManagerIds.isEmpty()) {
                addUsersToProjectGroupChat(updateReqVO.getId(), newManagerIds);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProject(Long id) {
        // 1. 校验存在
        validateProjectExists(id);

        // 2. 级联删除关联的服务项（服务项会自动级联删除轮次）
        List<cn.shuhe.system.module.project.dal.dataobject.ServiceItemDO> serviceItems = 
                serviceItemService.getServiceItemListByProjectId(id);
        if (CollUtil.isNotEmpty(serviceItems)) {
            for (cn.shuhe.system.module.project.dal.dataobject.ServiceItemDO serviceItem : serviceItems) {
                serviceItemService.deleteServiceItem(serviceItem.getId());
            }
            log.info("【删除项目】项目 {} 关联的 {} 个服务项已删除", id, serviceItems.size());
        }

        // 3. 删除项目成员
        projectMemberMapper.deleteByProjectId(id);

        // 4. 删除项目
        projectMapper.deleteById(id);
    }

    @Override
    public ProjectDO getProject(Long id) {
        return projectMapper.selectById(id);
    }

    @Override
    public PageResult<ProjectDO> getProjectPage(ProjectPageReqVO pageReqVO, Long userId) {
        log.info("【getProjectPage】开始查询项目列表, userId={}, pageReqVO={}", userId, pageReqVO);
        
        // 1. 检查是否是超级管理员
        boolean isSuperAdmin = permissionApi.hasAnyRoles(userId, "super_admin");
        log.info("【getProjectPage】用户 {} 是否是超级管理员: {}", userId, isSuperAdmin);
        
        if (isSuperAdmin) {
            // 超级管理员可以看到所有项目，不再按服务项deptType过滤
            // 原因：项目是主体，服务项是附属，不应该因为没有服务项就看不到项目
            log.info("【getProjectPage】超级管理员，查询所有项目（不按服务项deptType过滤）, pageReqVO.status={}", 
                    pageReqVO.getStatus());
            
            // 清除 deptType，超管直接查询所有项目
            ProjectPageReqVO newReqVO = BeanUtils.toBean(pageReqVO, ProjectPageReqVO.class);
            newReqVO.setDeptType(null);
            PageResult<ProjectDO> result = projectMapper.selectPage(newReqVO);
            log.info("【getProjectPage】超级管理员查询结果: total={}, listSize={}", 
                    result.getTotal(), result.getList() != null ? result.getList().size() : 0);
            return result;
        }

        // 3. 所有非超管用户（包括部门负责人）只能看到自己参与的项目
        // 说明：项目可见性基于项目成员关系，当用户领取合同时会被自动添加为项目成员
        List<Long> projectIds = projectMemberMapper.selectProjectIdsByUserId(userId);
        log.info("【getProjectPage】用户 {} 参与的项目IDs: {}", userId, projectIds);
        if (CollUtil.isEmpty(projectIds)) {
            log.info("【getProjectPage】用户没有参与任何项目，返回空结果");
            return PageResult.empty();
        }
        
        // 4. 用户参与的项目始终显示，不受 deptType 过滤影响
        // 原因：用户作为项目成员，应该能看到自己参与的项目，即使该项目暂时没有服务项
        // 如果有 deptType 过滤，只是作为附加筛选条件，但不会完全排除用户参与的项目
        // 注意：这里不再与 projectIdsByServiceItemDeptType 取交集，而是直接使用用户参与的项目
        log.info("【getProjectPage】用户参与的项目IDs（不进行deptType过滤）: {}", projectIds);
        
        // 清除 deptType，因为我们已经用项目ID列表来过滤了
        ProjectPageReqVO newReqVO = BeanUtils.toBean(pageReqVO, ProjectPageReqVO.class);
        newReqVO.setDeptType(null);
        return projectMapper.selectPageByIds(newReqVO, projectIds);
    }

    @Override
    public List<ProjectDO> getProjectListByDeptType(Integer deptType) {
        return projectMapper.selectListByDeptType(deptType);
    }

    @Override
    public void updateProjectStatus(Long id, Integer status) {
        // 1. 校验存在
        ProjectDO project = validateProjectExists(id);

        // 2. 如果是开始项目（status=1），检查是否设置了项目负责人（仅记录日志，不阻断）
        if (Integer.valueOf(1).equals(status)) {
            if (CollUtil.isEmpty(project.getManagerIds())) {
                log.warn("【项目状态更新】项目 {} 尚未设置负责人，但仍允许更新状态为进行中", id);
            }
        }

        // 3. 更新状态
        ProjectDO updateObj = new ProjectDO();
        updateObj.setId(id);
        updateObj.setStatus(status);
        projectMapper.updateById(updateObj);
    }

    /**
     * 校验项目是否存在
     */
    private ProjectDO validateProjectExists(Long id) {
        ProjectDO project = projectMapper.selectById(id);
        if (project == null) {
            throw exception(PROJECT_NOT_EXISTS);
        }
        return project;
    }

    /**
     * 生成项目编号
     * 格式：PRJ-{部门类型}-{年月日}-{4位随机数}
     * 例如：PRJ-1-20260116-0001
     */
    private String generateProjectCode(Integer deptType) {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String random = String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
        return StrUtil.format("PRJ-{}-{}-{}", deptType, date, random);
    }

    @Override
    public ProjectDO getProjectByContractId(Long contractId) {
        return projectMapper.selectByContractId(contractId);
    }

    @Override
    public ProjectDO getProjectByBusinessId(Long businessId) {
        return projectMapper.selectByBusinessId(businessId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addProjectMember(Long projectId, Long userId, String nickname, Integer roleType) {
        // 检查是否已经是成员
        ProjectMemberDO existingMember = projectMemberMapper.selectByProjectIdAndUserId(projectId, userId);
        if (existingMember != null) {
            log.info("【项目成员】用户 {} 已经是项目 {} 的成员，跳过添加", userId, projectId);
            return;
        }

        // 添加成员
        ProjectMemberDO member = ProjectMemberDO.builder()
                .projectId(projectId)
                .userId(userId)
                .nickname(nickname)
                .roleType(roleType)
                .joinTime(java.time.LocalDateTime.now())
                .build();
        projectMemberMapper.insert(member);
        log.info("【项目成员】已将用户 {} ({}) 添加为项目 {} 的成员，角色类型={}", userId, nickname, projectId, roleType);

        // 将新成员加入项目钉钉群
        addUsersToProjectGroupChat(projectId, List.of(userId));
    }

    @Override
    public void addUsersToProjectGroupChat(Long projectId, List<Long> userIds) {
        if (projectId == null || userIds == null || userIds.isEmpty()) {
            return;
        }
        try {
            ProjectDO project = projectMapper.selectById(projectId);
            if (project == null || StrUtil.isBlank(project.getDingtalkChatId())) {
                return;
            }
            boolean ok = dingtalkNotifyApi.addMembersToGroupChat(project.getDingtalkChatId(), userIds);
            if (ok) {
                log.info("【项目群】已将用户 {} 加入项目 {} 的钉钉群 {}", userIds, projectId, project.getDingtalkChatId());
            } else {
                log.warn("【项目群】加群失败，projectId={}, userIds={}", projectId, userIds);
            }
        } catch (Exception e) {
            log.warn("【项目群】加群异常，projectId={}, userIds={}: {}", projectId, userIds, e.getMessage());
        }
    }

    @Override
    public DashboardStatisticsRespVO.ProjectStats getProjectStats(Long userId) {
        List<Long> projectIds = resolveProjectIdsForUser(userId);
        if (projectIds != null && projectIds.isEmpty()) {
            return DashboardStatisticsRespVO.ProjectStats.builder()
                    .activeCount(0)
                    .totalCount(0)
                    .monthlyNewCount(0)
                    .completedCount(0)
                    .build();
        }
        long totalCount = projectMapper.selectCountByStatusAndIds(null, projectIds);
        long activeCount = projectMapper.selectCountByStatusAndIds(1, projectIds);   // 1-进行中
        long completedCount = projectMapper.selectCountByStatusAndIds(2, projectIds); // 2-已完成
        LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).with(ChronoField.NANO_OF_DAY, 0);
        long monthlyNewCount = projectMapper.selectCountByCreateTimeAfterAndIds(monthStart, projectIds);
        return DashboardStatisticsRespVO.ProjectStats.builder()
                .activeCount((int) activeCount)
                .totalCount((int) totalCount)
                .monthlyNewCount((int) monthlyNewCount)
                .completedCount((int) completedCount)
                .build();
    }

    @Override
    public List<DashboardStatisticsRespVO.PieChartData> getProjectDistribution(Long userId, boolean isAdmin) {
        List<Long> projectIds = resolveProjectIdsForUser(userId);
        if (projectIds != null && projectIds.isEmpty()) {
            return new ArrayList<>();
        }
        long active = projectMapper.selectCountByStatusAndIds(1, projectIds);
        long completed = projectMapper.selectCountByStatusAndIds(2, projectIds);
        long draft = projectMapper.selectCountByStatusAndIds(0, projectIds);
        List<DashboardStatisticsRespVO.PieChartData> list = new ArrayList<>();
        if (active > 0) {
            list.add(DashboardStatisticsRespVO.PieChartData.builder().name("进行中").value((int) active).color("#5470c6").build());
        }
        if (completed > 0) {
            list.add(DashboardStatisticsRespVO.PieChartData.builder().name("已完成").value((int) completed).color("#91cc75").build());
        }
        if (draft > 0) {
            list.add(DashboardStatisticsRespVO.PieChartData.builder().name("草稿").value((int) draft).color("#fac858").build());
        }
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void exitProject(Long id, String exitRemark) {
        // 1. 校验项目存在
        ProjectDO project = validateProjectExists(id);

        // 2. 只有非退场状态的项目才能退场
        if (Integer.valueOf(3).equals(project.getStatus())) {
            throw exception(PROJECT_ALREADY_EXITED);
        }

        // 3. 更新状态为已退场，记录退场备注
        ProjectDO updateObj = new ProjectDO();
        updateObj.setId(id);
        updateObj.setStatus(3);
        updateObj.setExitRemark(exitRemark);
        projectMapper.updateById(updateObj);
        log.info("【项目退场】项目 {} ({}) 已退场，备注：{}", id, project.getName(), exitRemark);

        // 3.1 同步将关联的部门服务单状态设为已退场（status=5）
        List<ProjectDeptServiceDO> deptServices = projectDeptServiceMapper.selectListByProjectId(id);
        for (ProjectDeptServiceDO ds : deptServices) {
            if (!Integer.valueOf(5).equals(ds.getStatus())) {
                ProjectDeptServiceDO dsUpdate = new ProjectDeptServiceDO();
                dsUpdate.setId(ds.getId());
                dsUpdate.setStatus(5);
                projectDeptServiceMapper.updateById(dsUpdate);
            }
        }

        // 4. 发送钉钉群退场通知
        if (StrUtil.isNotBlank(project.getDingtalkChatId())) {
            try {
                String content = buildExitNotificationContent(project, exitRemark);
                boolean sent = dingtalkNotifyApi.sendMessageToChat(project.getDingtalkChatId(), "项目退场通知", content);
                if (!sent) {
                    log.warn("【项目退场】钉钉群通知发送失败，项目 {} chatId={}", id, project.getDingtalkChatId());
                }
            } catch (Exception e) {
                log.error("【项目退场】发送钉钉群通知异常，项目 {}，异常信息：{}", id, e.getMessage(), e);
            }
        } else {
            log.info("【项目退场】项目 {} 未关联钉钉群，跳过退场通知", id);
        }
    }

    /**
     * 构建退场钉钉通知内容，含服务时长和成员成本消耗估算
     */
    private String buildExitNotificationContent(ProjectDO project, String exitRemark) {
        LocalDate today = LocalDate.now();
        LocalDate projectStart = project.getCreateTime() != null
                ? project.getCreateTime().toLocalDate() : today;
        long totalDays = ChronoUnit.DAYS.between(projectStart, today);

        StringBuilder msg = new StringBuilder();
        // 标题
        msg.append("## 🚪 项目退场通知\n\n");
        msg.append("**项目名称：** ").append(project.getName()).append("\n\n");
        msg.append("**服务时长：** ").append(totalDays).append(" 天")
                .append("（").append(projectStart).append(" ~ ").append(today).append("）\n\n");
        msg.append("**退场备注：** ").append(StrUtil.blankToDefault(exitRemark, "无")).append("\n\n");
        msg.append("---\n\n");

        // 成员成本估算
        try {
            List<ProjectMemberDO> members = projectMemberMapper.selectListByProjectId(project.getId());
            if (CollUtil.isNotEmpty(members)) {
                msg.append("**💰 成本消耗估算**\n\n");
                BigDecimal totalCost = BigDecimal.ZERO;
                int year = today.getYear();
                int month = today.getMonthValue();

                for (ProjectMemberDO member : members) {
                    try {
                        UserCostRespVO costVO = costCalculationService.getUserCost(member.getUserId(), year, month);
                        if (costVO == null || costVO.getDailyCost() == null) {
                            continue;
                        }
                        // 成员在项目中的天数：从 joinTime 或项目创建日，取较晚的那天
                        LocalDate memberJoinDate = member.getJoinTime() != null
                                ? member.getJoinTime().toLocalDate() : projectStart;
                        LocalDate startDate = memberJoinDate.isAfter(projectStart) ? memberJoinDate : projectStart;
                        long memberDays = ChronoUnit.DAYS.between(startDate, today);
                        if (memberDays <= 0) {
                            continue;
                        }
                        BigDecimal memberCost = costVO.getDailyCost()
                                .multiply(BigDecimal.valueOf(memberDays))
                                .setScale(2, RoundingMode.HALF_UP);
                        totalCost = totalCost.add(memberCost);

                        msg.append("- **").append(member.getNickname()).append("**")
                                .append("（").append(memberDays).append(" 天 × ¥")
                                .append(costVO.getDailyCost().setScale(0, RoundingMode.HALF_UP))
                                .append("/天）= **¥").append(memberCost.toPlainString()).append("**\n");
                    } catch (Exception e) {
                        log.warn("【项目退场】获取成员 {} 成本失败，跳过", member.getNickname(), e);
                        msg.append("- ").append(member.getNickname()).append("（成本数据不可用）\n");
                    }
                }

                msg.append("\n**合计预计成本：¥").append(totalCost.toPlainString()).append("**\n\n");
                msg.append("> 成本 = 日成本（月成本÷当月工作日数）× 实际在项天数，仅供参考。\n\n");
            }
        } catch (Exception e) {
            log.warn("【项目退场】成本估算失败，跳过成本部分", e);
        }

        msg.append("---\n\n");
        msg.append("> 该项目已终止，不再继续提供服务。");
        return msg.toString();
    }

    @Override
    public void updateProjectContractInfo(Long projectId, Long contractId, String contractNo) {
        ProjectDO updateObj = new ProjectDO();
        updateObj.setId(projectId);
        updateObj.setContractId(contractId);
        updateObj.setContractNo(contractNo);
        projectMapper.updateById(updateObj);
        log.info("[updateProjectContractInfo] 项目 {} 关联合同 {} ({})", projectId, contractId, contractNo);
    }

    /**
     * 解析当前用户可见的项目ID列表：超管为 null（表示全部），否则为参与的项目ID
     */
    private List<Long> resolveProjectIdsForUser(Long userId) {
        if (permissionApi.hasAnyRoles(userId, "super_admin")) {
            return null;
        }
        return projectMemberMapper.selectProjectIdsByUserId(userId);
    }

}
