package cn.shuhe.system.module.project.controller.admin;

import cn.shuhe.system.framework.common.pojo.CommonResult;
import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.common.util.object.BeanUtils;
import cn.shuhe.system.framework.datapermission.core.util.DataPermissionUtils;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectDepartmentSummaryRespVO;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectPageReqVO;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectRespVO;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectSaveReqVO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectDO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectDeptServiceDO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectMemberDO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectRoundDO;
import cn.shuhe.system.module.project.dal.dataobject.ServiceItemDO;
import cn.shuhe.system.module.project.dal.mysql.ProjectDeptServiceMapper;
import cn.shuhe.system.module.project.dal.mysql.ProjectRoundMapper;
import cn.shuhe.system.module.project.dal.mysql.ServiceItemMapper;
import cn.shuhe.system.module.project.service.ProjectService;
import cn.shuhe.system.module.project.service.access.ProjectAccessService;
import cn.shuhe.system.module.project.service.progress.ProjectProgressCalculator;
import cn.shuhe.system.module.system.api.dept.dto.DeptRespDTO;
import cn.shuhe.system.module.system.api.user.AdminUserApi;
import cn.shuhe.system.module.system.api.user.dto.AdminUserRespDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.shuhe.system.framework.common.pojo.CommonResult.success;
import static cn.shuhe.system.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - 项目管理")
@RestController
@RequestMapping("/project/project")
@Validated
public class ProjectController {

    @Resource
    private ProjectService projectService;

    @Resource
    private ServiceItemMapper serviceItemMapper;

    @Resource
    private ProjectDeptServiceMapper projectDeptServiceMapper;

    @Resource
    private ProjectRoundMapper projectRoundMapper;

    @Resource
    private AdminUserApi adminUserApi;

    @Resource
    private cn.shuhe.system.module.system.api.dept.DeptApi deptApi;

    @Resource
    private ProjectAccessService projectAccessService;

    @PostMapping("/create")
    @Operation(summary = "创建项目")
    @PreAuthorize("@ss.hasPermission('project:project:create')")
    public CommonResult<Long> createProject(@Valid @RequestBody ProjectSaveReqVO createReqVO) {
        return success(projectService.createProject(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新项目")
    @PreAuthorize("@ss.hasPermission('project:project:update')")
    public CommonResult<Boolean> updateProject(@Valid @RequestBody ProjectSaveReqVO updateReqVO) {
        projectAccessService.validateManageProject(updateReqVO.getId(), getLoginUserId());
        projectService.updateProject(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除项目")
    @Parameter(name = "id", description = "项目编号", required = true)
    @PreAuthorize("@ss.hasPermission('project:project:delete')")
    public CommonResult<Boolean> deleteProject(@RequestParam("id") Long id) {
        projectAccessService.validateManageProject(id, getLoginUserId());
        projectService.deleteProject(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得项目详情")
    @Parameter(name = "id", description = "项目编号", required = true)
    @PreAuthorize("@ss.hasPermission('project:project:query')")
    public CommonResult<ProjectRespVO> getProject(@RequestParam("id") Long id) {
        Long userId = getLoginUserId();
        projectAccessService.validateViewProject(id, userId);
        ProjectDO project = projectService.getProject(id);
        ProjectRespVO respVO = BeanUtils.toBean(project, ProjectRespVO.class);
        if (respVO != null) {
            fillProjectAccessSummary(respVO, project, userId);
        }
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得项目分页")
    @PreAuthorize("@ss.hasPermission('project:project:query')")
    public CommonResult<PageResult<ProjectRespVO>> getProjectPage(@Valid ProjectPageReqVO pageReqVO) {
        Long userId = getLoginUserId();
        PageResult<ProjectDO> pageResult = projectService.getProjectPage(pageReqVO, userId);
        PageResult<ProjectRespVO> result = BeanUtils.toBean(pageResult, ProjectRespVO.class);
        // 获取每个项目的服务项数量
        for (int i = 0; i < pageResult.getList().size(); i++) {
            ProjectDO project = pageResult.getList().get(i);
            fillProjectAccessSummary(result.getList().get(i), project, userId);
        }
        return success(result);
    }

    private void fillProjectAccessSummary(ProjectRespVO respVO, ProjectDO project, Long userId) {
        List<Long> readableDeptIds = projectAccessService.getReadableDeptIds(project.getId(), userId);
        List<ProjectDeptServiceDO> deptServices = DataPermissionUtils.executeIgnore(
                () -> projectDeptServiceMapper.selectListByProjectId(project.getId()));
        List<ServiceItemDO> serviceItems = DataPermissionUtils.executeIgnore(
                () -> serviceItemMapper.selectListByProjectId(project.getId()));
        List<ProjectRoundDO> rounds = DataPermissionUtils.executeIgnore(
                () -> projectRoundMapper.selectListByProjectId(project.getId()));
        Map<Long, List<ProjectRoundDO>> roundsByServiceItemId = rounds.stream()
                .filter(round -> round.getServiceItemId() != null)
                .collect(Collectors.groupingBy(ProjectRoundDO::getServiceItemId));

        List<ProjectDepartmentSummaryRespVO> summaries = deptServices.stream()
                .filter(deptService -> readableDeptIds == null
                        || readableDeptIds.contains(deptService.getDeptId()))
                .map(deptService -> buildDepartmentSummary(
                        deptService, serviceItems, roundsByServiceItemId, userId))
                .toList();
        respVO.setDepartmentSummaries(summaries);
        respVO.setServiceItemCount(summaries.stream()
                .mapToInt(summary -> summary.getServiceItemCount() != null ? summary.getServiceItemCount() : 0)
                .sum());
        int totalWeight = summaries.stream()
                .mapToInt(summary -> summary.getProgressWeight() == null ? 0 : summary.getProgressWeight())
                .sum();
        respVO.setProgress(totalWeight == 0 ? 0 : (int) Math.round(summaries.stream()
                .mapToDouble(summary -> (double) (summary.getProgress() == null ? 0 : summary.getProgress())
                        * (summary.getProgressWeight() == null ? 0 : summary.getProgressWeight()))
                .sum() / totalWeight));
        respVO.setPlannedExecutionCount(summaries.stream()
                .mapToInt(summary -> summary.getPlannedExecutionCount() == null
                        ? 0 : summary.getPlannedExecutionCount()).sum());
        respVO.setAcceptedExecutionCount(summaries.stream()
                .mapToInt(summary -> summary.getAcceptedExecutionCount() == null
                        ? 0 : summary.getAcceptedExecutionCount()).sum());
        respVO.setHasOnDemandService(summaries.stream()
                .anyMatch(summary -> Boolean.TRUE.equals(summary.getHasOnDemandService())));
        respVO.setCanManage(projectAccessService.canManageProject(project.getId(), userId));
    }

    private ProjectDepartmentSummaryRespVO buildDepartmentSummary(ProjectDeptServiceDO deptService,
            List<ServiceItemDO> projectItems,
            Map<Long, List<ProjectRoundDO>> roundsByServiceItemId,
            Long userId) {
        List<ServiceItemDO> items = projectItems.stream()
                .filter(item -> belongsToDeptService(item, deptService))
                .toList();
        int completed = (int) items.stream().filter(item -> Integer.valueOf(3).equals(item.getStatus())).count();
        ProjectProgressCalculator.Summary fulfillment = ProjectProgressCalculator.calculate(
                items, roundsByServiceItemId);

        ProjectDepartmentSummaryRespVO summary = new ProjectDepartmentSummaryRespVO();
        summary.setDeptServiceId(deptService.getId());
        summary.setDeptId(deptService.getDeptId());
        summary.setDeptName(deptService.getDeptName());
        summary.setDeptType(deptService.getDeptType());
        summary.setStatus(deptService.getStatus());
        summary.setServiceItemCount(items.size());
        summary.setCompletedServiceItemCount(completed);
        summary.setOnsiteServiceItemCount((int) items.stream().filter(this::isOnsiteItem).count());
        summary.setRemoteServiceItemCount((int) items.stream()
                .filter(item -> Integer.valueOf(ServiceItemDO.SERVICE_MODE_REMOTE).equals(item.getServiceMode()))
                .count());
        summary.setManagementServiceItemCount((int) items.stream()
                .filter(item -> Integer.valueOf(ServiceItemDO.SERVICE_MEMBER_TYPE_MANAGEMENT)
                        .equals(item.getServiceMemberType()))
                .count());
        summary.setProgress(items.isEmpty()
                ? (deptService.getProgress() != null ? deptService.getProgress() : 0)
                : fulfillment.progress());
        summary.setProgressWeight(fulfillment.progressWeight());
        summary.setPlannedExecutionCount(fulfillment.plannedExecutionCount());
        summary.setAcceptedExecutionCount(fulfillment.acceptedExecutionCount());
        summary.setHasOnDemandService(fulfillment.hasOnDemandService());
        summary.setCanManage(projectAccessService.canManageDept(
                deptService.getProjectId(), userId, deptService.getDeptId()));
        return summary;
    }

    private boolean belongsToDeptService(ServiceItemDO item, ProjectDeptServiceDO deptService) {
        if (item.getDeptServiceId() != null) {
            return item.getDeptServiceId().equals(deptService.getId());
        }
        // 兼容尚未执行数据迁移的历史服务项。
        return item.getDeptType() != null && item.getDeptType().equals(deptService.getDeptType());
    }

    private boolean isOnsiteItem(ServiceItemDO item) {
        return Integer.valueOf(ServiceItemDO.SERVICE_MODE_ONSITE).equals(item.getServiceMode())
                || Integer.valueOf(ServiceItemDO.SERVICE_MEMBER_TYPE_ONSITE).equals(item.getServiceMemberType());
    }

    @GetMapping("/list")
    @Operation(summary = "获得项目列表（根据部门类型，带用户可见性过滤）")
    @Parameter(name = "deptType", description = "部门类型", required = true)
    @PreAuthorize("@ss.hasPermission('project:project:query')")
    public CommonResult<List<ProjectRespVO>> getProjectList(@RequestParam("deptType") Integer deptType) {
        Long userId = getLoginUserId();
        List<ProjectDO> list = projectService.getProjectListByDeptType(deptType, userId);
        List<ProjectRespVO> result = BeanUtils.toBean(list, ProjectRespVO.class);
        for (int i = 0; i < list.size(); i++) {
            fillProjectAccessSummary(result.get(i), list.get(i), userId);
        }
        return success(result);
    }

    @PutMapping("/update-status")
    @Operation(summary = "更新项目状态")
    @PreAuthorize("@ss.hasPermission('project:project:update')")
    public CommonResult<Boolean> updateProjectStatus(@RequestParam("id") Long id,
            @RequestParam("status") Integer status) {
        projectAccessService.validateManageProject(id, getLoginUserId());
        projectService.updateProjectStatus(id, status);
        return success(true);
    }

    @PutMapping("/exit")
    @Operation(summary = "项目退场", description = "将项目置为已退场状态，发送钉钉群通知，退场后不可再管理")
    @Parameter(name = "id", description = "项目编号", required = true)
    @Parameter(name = "exitRemark", description = "退场备注")
    @PreAuthorize("@ss.hasPermission('project:project:update')")
    public CommonResult<Boolean> exitProject(@RequestParam("id") Long id,
            @RequestParam(value = "exitRemark", required = false) String exitRemark) {
        projectAccessService.validateManageProject(id, getLoginUserId());
        projectService.exitProject(id, exitRemark);
        return success(true);
    }

    @GetMapping("/my-role")
    @Operation(summary = "获取当前用户在项目中的角色及部门信息")
    @Parameter(name = "projectId", description = "项目编号", required = true)
    @PreAuthorize("@ss.hasPermission('project:project:query')")
    public CommonResult<Map<String, Object>> getMyRoleInProject(@RequestParam("projectId") Long projectId) {
        Long userId = getLoginUserId();
        projectAccessService.validateViewProject(projectId, userId);
        Integer roleType = projectService.getUserRoleInProject(projectId, userId);
        AdminUserRespDTO user = adminUserApi.getUser(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("roleType", roleType);
        result.put("canManage", projectAccessService.canManageProject(projectId, userId));
        result.put("deptId", user != null ? user.getDeptId() : null);
        result.put("readableDeptIds", projectAccessService.getReadableDeptIds(projectId, userId));
        result.put("manageableDeptIds", projectAccessService.getManageableDeptIds(projectId, userId));

        if (user != null && user.getDeptId() != null) {
            cn.shuhe.system.module.system.api.dept.dto.DeptRespDTO dept = deptApi.getDept(user.getDeptId());
            result.put("deptType", dept != null ? dept.getDeptType() : null);
        }

        return success(result);
    }

    // ========== 项目成员管理 ==========

    private static final DateTimeFormatter MEMBER_JOIN_TIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @GetMapping("/member-list")
    @Operation(summary = "获得项目成员列表")
    @Parameter(name = "projectId", description = "项目编号", required = true)
    @PreAuthorize("@ss.hasPermission('project:project:query')")
    public CommonResult<List<Map<String, Object>>> getProjectMemberList(@RequestParam("projectId") Long projectId) {
        projectAccessService.validateViewProject(projectId, getLoginUserId());
        List<ProjectMemberDO> members = projectService.getProjectMembers(projectId);
        if (members.isEmpty()) {
            return success(Collections.emptyList());
        }

        // 批量回填最新 nickname / deptName，绕过数据权限避免跨部门用户被拦
        Set<Long> userIds = members.stream()
                .map(ProjectMemberDO::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, AdminUserRespDTO> userMap = userIds.isEmpty()
                ? Collections.emptyMap()
                : DataPermissionUtils.executeIgnore(() -> adminUserApi.getUserMap(userIds));
        Set<Long> deptIds = userMap.values().stream()
                .map(AdminUserRespDTO::getDeptId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, DeptRespDTO> deptMap = deptIds.isEmpty()
                ? Collections.emptyMap()
                : DataPermissionUtils.executeIgnore(() -> deptApi.getDeptMap(deptIds));

        List<Map<String, Object>> result = members.stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", m.getId());
            map.put("projectId", m.getProjectId());
            map.put("userId", m.getUserId());
            map.put("roleType", m.getRoleType());
            map.put("remark", m.getRemark());
            map.put("joinTime", m.getJoinTime() != null
                    ? m.getJoinTime().format(MEMBER_JOIN_TIME_FMT) : null);

            AdminUserRespDTO user = userMap.get(m.getUserId());
            String nickname = user != null && user.getNickname() != null
                    && !user.getNickname().isEmpty()
                    ? user.getNickname()
                    : m.getNickname();
            map.put("nickname", nickname);
            if (user != null && user.getDeptId() != null) {
                map.put("deptId", user.getDeptId());
                DeptRespDTO dept = deptMap.get(user.getDeptId());
                map.put("deptName", dept != null ? dept.getName() : null);
            }
            return map;
        }).toList();
        return success(result);
    }

    @PostMapping("/add-member")
    @Operation(summary = "添加项目成员")
    @PreAuthorize("@ss.hasPermission('project:project:update')")
    public CommonResult<Boolean> addProjectMember(@RequestParam("projectId") Long projectId,
                                                   @RequestParam("userId") Long userId,
                                                   @RequestParam(value = "roleType", defaultValue = "2") Integer roleType,
                                                   @RequestParam(value = "remark", required = false) String remark) {
        projectAccessService.validateManageProject(projectId, getLoginUserId());
        validateManualMemberRole(roleType);
        AdminUserRespDTO user = adminUserApi.getUser(userId);
        String nickname = user != null ? user.getNickname() : "";
        projectService.addProjectMember(projectId, userId, nickname, roleType);
        return success(true);
    }

    @DeleteMapping("/delete-member")
    @Operation(summary = "删除项目成员")
    @Parameter(name = "id", description = "成员记录编号", required = true)
    @PreAuthorize("@ss.hasPermission('project:project:update')")
    public CommonResult<Boolean> deleteProjectMember(@RequestParam("id") Long id) {
        projectService.deleteProjectMember(id);
        return success(true);
    }

    @PutMapping("/update-member-role")
    @Operation(summary = "修改项目成员的角色（roleType）",
            description = "1=项目经理 / 2=执行人员；部门负责人权限由组织架构自动计算")
    @PreAuthorize("@ss.hasPermission('project:project:update')")
    public CommonResult<Boolean> updateProjectMemberRole(
            @RequestParam("id") Long id,
            @RequestParam("roleType") Integer roleType) {
        validateManualMemberRole(roleType);
        projectService.updateProjectMemberRole(id, roleType);
        return success(true);
    }

    private void validateManualMemberRole(Integer roleType) {
        if (roleType == null || !List.of(1, 2).contains(roleType)) {
            throw cn.shuhe.system.framework.common.exception.util.ServiceExceptionUtil.exception(
                    cn.shuhe.system.framework.common.exception.enums.GlobalErrorCodeConstants.BAD_REQUEST);
        }
    }

    // ========== 项目部门可见性管理（业界路径 2·部门下所有人都能看到项目） ==========

    @GetMapping("/dept-visibility")
    @Operation(summary = "获取项目的可见部门 id 列表",
            description = "返回该项目在 project_dept_visibility 表中配置的所有 dept_id，部门下用户都能看到该项目")
    @Parameter(name = "projectId", description = "项目编号", required = true)
    @PreAuthorize("@ss.hasPermission('project:project:query')")
    public CommonResult<List<Long>> getProjectDeptVisibility(@RequestParam("projectId") Long projectId) {
        projectAccessService.validateViewProject(projectId, getLoginUserId());
        return success(projectService.getProjectDeptVisibilityIds(projectId));
    }

    @PutMapping("/dept-visibility/replace")
    @Operation(summary = "全量替换项目的可见部门 id 列表",
            description = "幂等覆盖：传入的 deptIds 集合即为最终状态；传空数组 = 清空所有可见部门")
    @PreAuthorize("@ss.hasPermission('project:project:update')")
    public CommonResult<Boolean> replaceProjectDeptVisibility(
            @RequestParam("projectId") Long projectId,
            @RequestParam("deptIds") List<Long> deptIds) {
        projectAccessService.validateManageProject(projectId, getLoginUserId());
        projectService.replaceProjectDeptVisibility(projectId, deptIds);
        return success(true);
    }

}
