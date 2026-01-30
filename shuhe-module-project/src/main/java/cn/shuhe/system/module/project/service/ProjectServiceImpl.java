package cn.shuhe.system.module.project.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.common.util.object.BeanUtils;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectPageReqVO;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectSaveReqVO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectDO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectMemberDO;
import cn.shuhe.system.module.project.dal.mysql.ProjectMapper;
import cn.shuhe.system.module.project.dal.mysql.ProjectMemberMapper;
import cn.shuhe.system.module.system.api.permission.PermissionApi;
import cn.shuhe.system.module.system.controller.admin.dashboard.vo.DashboardStatisticsRespVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
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
    private cn.shuhe.system.module.project.dal.mysql.ServiceItemMapper serviceItemMapper;

    @Resource
    @org.springframework.context.annotation.Lazy // 延迟加载，避免循环依赖
    private ServiceItemService serviceItemService;

    @Resource
    private PermissionApi permissionApi;

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
        validateProjectExists(updateReqVO.getId());

        // 2. 更新
        ProjectDO updateObj = BeanUtils.toBean(updateReqVO, ProjectDO.class);
        projectMapper.updateById(updateObj);
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
