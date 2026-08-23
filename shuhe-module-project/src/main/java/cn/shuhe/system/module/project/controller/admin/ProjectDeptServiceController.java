package cn.shuhe.system.module.project.controller.admin;

import cn.shuhe.system.framework.common.pojo.CommonResult;
import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.common.util.object.BeanUtils;
import cn.shuhe.system.framework.datapermission.core.util.DataPermissionUtils;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectDeptServicePageReqVO;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectDeptServiceRespVO;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectDeptServiceSaveReqVO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectDO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectDeptServiceDO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectRoundDO;
import cn.shuhe.system.module.project.dal.dataobject.ServiceItemDO;
import cn.shuhe.system.module.project.dal.mysql.ProjectRoundMapper;
import cn.shuhe.system.module.project.dal.mysql.ServiceItemMapper;
import cn.shuhe.system.module.project.service.ProjectDeptServiceService;
import cn.shuhe.system.module.project.service.ProjectService;
import cn.shuhe.system.module.project.service.access.ProjectAccessService;
import cn.shuhe.system.module.project.service.progress.ProjectProgressCalculator;
import cn.shuhe.system.module.system.api.user.AdminUserApi;
import cn.shuhe.system.module.system.api.user.dto.AdminUserRespDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.shuhe.system.framework.common.pojo.CommonResult.success;
import static cn.shuhe.system.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - 部门服务单")
@RestController
@RequestMapping("/project/dept-service")
@Validated
@Slf4j
public class ProjectDeptServiceController {

    @Resource
    private ProjectDeptServiceService deptServiceService;

    @Resource
    private ProjectService projectService;

    @Resource
    private ServiceItemMapper serviceItemMapper;

    @Resource
    private ProjectRoundMapper projectRoundMapper;

    @Resource
    private AdminUserApi adminUserApi;

    @Resource
    private ProjectAccessService projectAccessService;

    @PostMapping("/create")
    @Operation(summary = "创建部门服务单")
    @PreAuthorize("@ss.hasPermission('project:dept-service:create')")
    public CommonResult<Long> createDeptService(@Valid @RequestBody ProjectDeptServiceSaveReqVO createReqVO) {
        validateManageDept(createReqVO.getProjectId(), createReqVO.getDeptId());
        return success(deptServiceService.createDeptService(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新部门服务单")
    @PreAuthorize("@ss.hasPermission('project:dept-service:update')")
    public CommonResult<Boolean> updateDeptService(@Valid @RequestBody ProjectDeptServiceSaveReqVO updateReqVO) {
        ProjectDeptServiceDO existing = validateManageDeptService(updateReqVO.getId());
        updateReqVO.setProjectId(existing.getProjectId());
        validateManageDept(existing.getProjectId(), updateReqVO.getDeptId());
        deptServiceService.updateDeptService(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除部门服务单")
    @Parameter(name = "id", description = "部门服务单ID", required = true)
    @PreAuthorize("@ss.hasPermission('project:dept-service:delete')")
    public CommonResult<Boolean> deleteDeptService(@RequestParam("id") Long id) {
        validateManageDeptService(id);
        deptServiceService.deleteDeptService(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得部门服务单详情")
    @Parameter(name = "id", description = "部门服务单ID", required = true)
    @PreAuthorize("@ss.hasPermission('project:dept-service:query')")
    public CommonResult<ProjectDeptServiceRespVO> getDeptService(@RequestParam("id") Long id) {
        ProjectDeptServiceDO deptService = deptServiceService.getDeptService(id);
        validateReadDeptService(deptService);
        return success(convertToRespVO(deptService));
    }

    @GetMapping("/page")
    @Operation(summary = "获得部门服务单分页")
    @PreAuthorize("@ss.hasPermission('project:dept-service:query')")
    public CommonResult<PageResult<ProjectDeptServiceRespVO>> getDeptServicePage(@Valid ProjectDeptServicePageReqVO pageReqVO) {
        PageResult<ProjectDeptServiceDO> pageResult = deptServiceService.getDeptServicePage(pageReqVO, getLoginUserId());

        List<ProjectDeptServiceDO> readableList = pageResult.getList().stream()
                .filter(this::canReadDeptService)
                .toList();
        // 转换为 RespVO 并填充项目信息
        List<ProjectDeptServiceRespVO> respList = new ArrayList<>();
        for (ProjectDeptServiceDO deptService : readableList) {
            respList.add(convertToRespVO(deptService));
        }

        // 该入口当前前端未使用；安全上不返回不可读记录，总数也不暴露。
        return success(new PageResult<>(respList, (long) respList.size()));
    }

    @GetMapping("/list-by-project")
    @Operation(summary = "根据项目ID获取部门服务单列表")
    @Parameter(name = "projectId", description = "项目ID", required = true)
    @PreAuthorize("@ss.hasPermission('project:dept-service:query')")
    public CommonResult<List<ProjectDeptServiceRespVO>> getDeptServiceListByProject(@RequestParam("projectId") Long projectId) {
        projectAccessService.validateViewProject(projectId, getLoginUserId());
        List<ProjectDeptServiceDO> list = deptServiceService.getDeptServiceListByProjectId(projectId);
        List<ProjectDeptServiceRespVO> respList = new ArrayList<>();
        for (ProjectDeptServiceDO deptService : list.stream().filter(this::canReadDeptService).toList()) {
            respList.add(convertToRespVO(deptService));
        }
        return success(respList);
    }

    @PutMapping("/update-status")
    @Operation(summary = "更新部门服务单状态")
    @PreAuthorize("@ss.hasPermission('project:dept-service:update')")
    public CommonResult<Boolean> updateDeptServiceStatus(@RequestParam("id") Long id,
                                                          @RequestParam("status") Integer status) {
        validateManageDeptService(id);
        deptServiceService.updateDeptServiceStatus(id, status);
        return success(true);
    }

    @PutMapping("/set-security-service-managers")
    @Operation(summary = "设置安全服务的驻场和二线负责人")
    @PreAuthorize("@ss.hasPermission('project:dept-service:update')")
    public CommonResult<Boolean> setSecurityServiceManagers(
            @RequestParam("id") Long id,
            @RequestParam(value = "onsiteManagerIds", required = false) List<Long> onsiteManagerIds,
            @RequestParam(value = "secondLineManagerIds", required = false) List<Long> secondLineManagerIds) {

        validateManageDeptService(id);

        List<String> onsiteManagerNames = resolveUserNames(onsiteManagerIds);
        List<String> secondLineManagerNames = resolveUserNames(secondLineManagerIds);

        deptServiceService.setSecurityServiceManagers(id,
                onsiteManagerIds, onsiteManagerNames,
                secondLineManagerIds, secondLineManagerNames);
        return success(true);
    }

    @PutMapping("/set-data-security-managers")
    @Operation(summary = "设置数据安全的驻场和二线负责人")
    @PreAuthorize("@ss.hasPermission('project:dept-service:update')")
    public CommonResult<Boolean> setDataSecurityManagers(
            @RequestParam("id") Long id,
            @RequestParam(value = "onsiteManagerIds", required = false) List<Long> onsiteManagerIds,
            @RequestParam(value = "secondLineManagerIds", required = false) List<Long> secondLineManagerIds) {

        validateManageDeptService(id);

        List<String> onsiteManagerNames = resolveUserNames(onsiteManagerIds);
        List<String> secondLineManagerNames = resolveUserNames(secondLineManagerIds);

        deptServiceService.setDataSecurityManagers(id,
                onsiteManagerIds, onsiteManagerNames,
                secondLineManagerIds, secondLineManagerNames);
        return success(true);
    }

    @PutMapping("/set-managers")
    @Operation(summary = "设置安全运营的负责人")
    @PreAuthorize("@ss.hasPermission('project:dept-service:update')")
    public CommonResult<Boolean> setDeptServiceManagers(
            @RequestParam("id") Long id,
            @RequestParam("managerIds") List<Long> managerIds) {

        validateManageDeptService(id);

        List<String> managerNames = resolveUserNames(managerIds);
        deptServiceService.setDeptServiceManagers(id, managerIds, managerNames);
        return success(true);
    }

    private boolean canReadDeptService(ProjectDeptServiceDO deptService) {
        return deptService != null && projectAccessService.canReadDept(
                deptService.getProjectId(), getLoginUserId(), deptService.getDeptId());
    }

    private void validateReadDeptService(ProjectDeptServiceDO deptService) {
        if (!canReadDeptService(deptService)) {
            throwForbidden();
        }
    }

    private ProjectDeptServiceDO validateManageDeptService(Long id) {
        ProjectDeptServiceDO deptService = deptServiceService.getDeptService(id);
        if (deptService == null) {
            throwForbidden();
        }
        validateManageDept(deptService.getProjectId(), deptService.getDeptId());
        return deptService;
    }

    private void validateManageDept(Long projectId, Long deptId) {
        if (!projectAccessService.canManageDept(projectId, getLoginUserId(), deptId)) {
            throwForbidden();
        }
    }

    private void throwForbidden() {
        throw cn.shuhe.system.framework.common.exception.util.ServiceExceptionUtil.exception(
                cn.shuhe.system.framework.common.exception.enums.GlobalErrorCodeConstants.FORBIDDEN);
    }

    /** 批量解析用户姓名 */
    private List<String> resolveUserNames(List<Long> userIds) {
        List<String> names = new ArrayList<>();
        if (userIds != null) {
            for (Long uid : userIds) {
                AdminUserRespDTO user = adminUserApi.getUser(uid);
                if (user != null) names.add(user.getNickname());
            }
        }
        return names;
    }

    /**
     * 转换为响应 VO，填充项目信息和预算统计
     */
    private ProjectDeptServiceRespVO convertToRespVO(ProjectDeptServiceDO deptService) {
        if (deptService == null) {
            return null;
        }

        ProjectDeptServiceRespVO respVO = BeanUtils.toBean(deptService, ProjectDeptServiceRespVO.class);

        // 填充项目信息
        if (deptService.getProjectId() != null) {
            ProjectDO project = projectService.getProject(deptService.getProjectId());
            if (project != null) {
                respVO.setProjectName(project.getName());
                respVO.setProjectCode(project.getCode());
                respVO.setProjectType(project.getProjectType());
            }
        }

        // 以部门服务单为边界统计；兼容未执行迁移的历史数据。
        if (deptService.getProjectId() != null) {
            List<ServiceItemDO> items = DataPermissionUtils.executeIgnore(() ->
                    serviceItemMapper.selectListByProjectId(deptService.getProjectId()).stream()
                            .filter(item -> item.getDeptServiceId() != null
                                    ? item.getDeptServiceId().equals(deptService.getId())
                                    : deptService.getDeptType() != null
                                            && deptService.getDeptType().equals(item.getDeptType()))
                            .toList());
            respVO.setServiceItemCount(items.size());
            respVO.setCompletedServiceItemCount((int) items.stream()
                    .filter(item -> Integer.valueOf(3).equals(item.getStatus())).count());
            respVO.setOnsiteServiceItemCount((int) items.stream()
                    .filter(item -> Integer.valueOf(ServiceItemDO.SERVICE_MODE_ONSITE).equals(item.getServiceMode())
                            || Integer.valueOf(ServiceItemDO.SERVICE_MEMBER_TYPE_ONSITE)
                                    .equals(item.getServiceMemberType()))
                    .count());
            respVO.setRemoteServiceItemCount((int) items.stream()
                    .filter(item -> Integer.valueOf(ServiceItemDO.SERVICE_MODE_REMOTE).equals(item.getServiceMode()))
                    .count());
            respVO.setManagementServiceItemCount((int) items.stream()
                    .filter(item -> Integer.valueOf(ServiceItemDO.SERVICE_MEMBER_TYPE_MANAGEMENT)
                            .equals(item.getServiceMemberType()))
                    .count());
            Map<Long, List<ProjectRoundDO>> roundsByServiceItemId = DataPermissionUtils.executeIgnore(() ->
                    projectRoundMapper.selectListByProjectId(deptService.getProjectId()).stream()
                            .filter(round -> round.getServiceItemId() != null)
                            .collect(Collectors.groupingBy(ProjectRoundDO::getServiceItemId)));
            ProjectProgressCalculator.Summary fulfillment = ProjectProgressCalculator.calculate(
                    items, roundsByServiceItemId);
            respVO.setProgress(fulfillment.progress());
            respVO.setProgressWeight(fulfillment.progressWeight());
            respVO.setPlannedExecutionCount(fulfillment.plannedExecutionCount());
            respVO.setAcceptedExecutionCount(fulfillment.acceptedExecutionCount());
            respVO.setHasOnDemandService(fulfillment.hasOnDemandService());
        }
        respVO.setCanManage(projectAccessService.canManageDept(
                deptService.getProjectId(), getLoginUserId(), deptService.getDeptId()));

        return respVO;
    }

}
