package cn.shuhe.system.module.project.controller.admin;

import cn.shuhe.system.framework.common.pojo.CommonResult;
import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.common.util.object.BeanUtils;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectDeptServicePageReqVO;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectDeptServiceRespVO;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectDeptServiceSaveReqVO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectDO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectDeptServiceDO;
import cn.shuhe.system.module.project.dal.mysql.ServiceItemMapper;
import cn.shuhe.system.module.project.service.ProjectDeptServiceService;
import cn.shuhe.system.module.project.service.ProjectService;
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

import java.util.ArrayList;
import java.util.List;

import static cn.shuhe.system.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 部门服务单")
@RestController
@RequestMapping("/project/dept-service")
@Validated
public class ProjectDeptServiceController {

    @Resource
    private ProjectDeptServiceService deptServiceService;

    @Resource
    private ProjectService projectService;

    @Resource
    private ServiceItemMapper serviceItemMapper;

    @Resource
    private AdminUserApi adminUserApi;

    @PostMapping("/create")
    @Operation(summary = "创建部门服务单")
    @PreAuthorize("@ss.hasPermission('project:dept-service:create')")
    public CommonResult<Long> createDeptService(@Valid @RequestBody ProjectDeptServiceSaveReqVO createReqVO) {
        return success(deptServiceService.createDeptService(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新部门服务单")
    @PreAuthorize("@ss.hasPermission('project:dept-service:update')")
    public CommonResult<Boolean> updateDeptService(@Valid @RequestBody ProjectDeptServiceSaveReqVO updateReqVO) {
        deptServiceService.updateDeptService(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除部门服务单")
    @Parameter(name = "id", description = "部门服务单ID", required = true)
    @PreAuthorize("@ss.hasPermission('project:dept-service:delete')")
    public CommonResult<Boolean> deleteDeptService(@RequestParam("id") Long id) {
        deptServiceService.deleteDeptService(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得部门服务单详情")
    @Parameter(name = "id", description = "部门服务单ID", required = true)
    @PreAuthorize("@ss.hasPermission('project:dept-service:query')")
    public CommonResult<ProjectDeptServiceRespVO> getDeptService(@RequestParam("id") Long id) {
        ProjectDeptServiceDO deptService = deptServiceService.getDeptService(id);
        return success(convertToRespVO(deptService));
    }

    @GetMapping("/page")
    @Operation(summary = "获得部门服务单分页")
    @PreAuthorize("@ss.hasPermission('project:dept-service:query')")
    public CommonResult<PageResult<ProjectDeptServiceRespVO>> getDeptServicePage(@Valid ProjectDeptServicePageReqVO pageReqVO) {
        PageResult<ProjectDeptServiceDO> pageResult = deptServiceService.getDeptServicePage(pageReqVO);
        
        // 转换为 RespVO 并填充项目信息
        List<ProjectDeptServiceRespVO> respList = new ArrayList<>();
        for (ProjectDeptServiceDO deptService : pageResult.getList()) {
            respList.add(convertToRespVO(deptService));
        }
        
        return success(new PageResult<>(respList, pageResult.getTotal()));
    }

    @GetMapping("/list-by-project")
    @Operation(summary = "根据项目ID获取部门服务单列表")
    @Parameter(name = "projectId", description = "项目ID", required = true)
    @PreAuthorize("@ss.hasPermission('project:dept-service:query')")
    public CommonResult<List<ProjectDeptServiceRespVO>> getDeptServiceListByProject(@RequestParam("projectId") Long projectId) {
        List<ProjectDeptServiceDO> list = deptServiceService.getDeptServiceListByProjectId(projectId);
        List<ProjectDeptServiceRespVO> respList = new ArrayList<>();
        for (ProjectDeptServiceDO deptService : list) {
            respList.add(convertToRespVO(deptService));
        }
        return success(respList);
    }

    @PutMapping("/update-status")
    @Operation(summary = "更新部门服务单状态")
    @PreAuthorize("@ss.hasPermission('project:dept-service:update')")
    public CommonResult<Boolean> updateDeptServiceStatus(@RequestParam("id") Long id,
                                                          @RequestParam("status") Integer status) {
        deptServiceService.updateDeptServiceStatus(id, status);
        return success(true);
    }

    @PutMapping("/set-managers")
    @Operation(summary = "设置部门服务单负责人")
    @PreAuthorize("@ss.hasPermission('project:dept-service:update')")
    public CommonResult<Boolean> setDeptServiceManagers(@RequestParam("id") Long id,
                                                         @RequestParam("managerIds") List<Long> managerIds) {
        // 获取负责人姓名
        List<String> managerNames = new ArrayList<>();
        for (Long managerId : managerIds) {
            AdminUserRespDTO user = adminUserApi.getUser(managerId);
            if (user != null) {
                managerNames.add(user.getNickname());
            }
        }
        deptServiceService.setDeptServiceManagers(id, managerIds, managerNames);
        return success(true);
    }

    /**
     * 转换为响应 VO，填充项目信息
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
            }
        }
        
        // 统计服务项数量（根据部门服务单ID统计）
        // TODO: 等服务项关联到部门服务单后，这里需要根据 deptServiceId 统计
        // 目前先根据 projectId 和 deptType 统计
        if (deptService.getProjectId() != null && deptService.getDeptType() != null) {
            Long count = serviceItemMapper.selectCountByProjectIdAndDeptType(
                    deptService.getProjectId(), deptService.getDeptType());
            respVO.setServiceItemCount(count != null ? count.intValue() : 0);
        }
        
        return respVO;
    }

}
