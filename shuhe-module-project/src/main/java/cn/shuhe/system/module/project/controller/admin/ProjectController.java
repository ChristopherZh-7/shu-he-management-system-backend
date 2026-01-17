package cn.shuhe.system.module.project.controller.admin;

import cn.shuhe.system.framework.common.pojo.CommonResult;
import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.common.util.object.BeanUtils;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectPageReqVO;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectRespVO;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectSaveReqVO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectDO;
import cn.shuhe.system.module.project.dal.mysql.ServiceItemMapper;
import cn.shuhe.system.module.project.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
        projectService.updateProject(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除项目")
    @Parameter(name = "id", description = "项目编号", required = true)
    @PreAuthorize("@ss.hasPermission('project:project:delete')")
    public CommonResult<Boolean> deleteProject(@RequestParam("id") Long id) {
        projectService.deleteProject(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得项目详情")
    @Parameter(name = "id", description = "项目编号", required = true)
    @PreAuthorize("@ss.hasPermission('project:project:query')")
    public CommonResult<ProjectRespVO> getProject(@RequestParam("id") Long id) {
        ProjectDO project = projectService.getProject(id);
        ProjectRespVO respVO = BeanUtils.toBean(project, ProjectRespVO.class);
        if (respVO != null) {
            // 获取服务项数量
            Long count = serviceItemMapper.selectCountByProjectId(id);
            respVO.setServiceItemCount(count.intValue());
        }
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得项目分页")
    @PreAuthorize("@ss.hasPermission('project:project:query')")
    public CommonResult<PageResult<ProjectRespVO>> getProjectPage(@Valid ProjectPageReqVO pageReqVO) {
        PageResult<ProjectDO> pageResult = projectService.getProjectPage(pageReqVO, getLoginUserId());
        PageResult<ProjectRespVO> result = BeanUtils.toBean(pageResult, ProjectRespVO.class);
        // 获取每个项目的服务项数量
        for (int i = 0; i < pageResult.getList().size(); i++) {
            ProjectDO project = pageResult.getList().get(i);
            Long count = serviceItemMapper.selectCountByProjectId(project.getId());
            result.getList().get(i).setServiceItemCount(count.intValue());
        }
        return success(result);
    }

    @GetMapping("/list")
    @Operation(summary = "获得项目列表（根据部门类型）")
    @Parameter(name = "deptType", description = "部门类型", required = true)
    @PreAuthorize("@ss.hasPermission('project:project:query')")
    public CommonResult<List<ProjectRespVO>> getProjectList(@RequestParam("deptType") Integer deptType) {
        List<ProjectDO> list = projectService.getProjectListByDeptType(deptType);
        return success(BeanUtils.toBean(list, ProjectRespVO.class));
    }

    @PutMapping("/update-status")
    @Operation(summary = "更新项目状态")
    @PreAuthorize("@ss.hasPermission('project:project:update')")
    public CommonResult<Boolean> updateProjectStatus(@RequestParam("id") Long id,
            @RequestParam("status") Integer status) {
        projectService.updateProjectStatus(id, status);
        return success(true);
    }

}
