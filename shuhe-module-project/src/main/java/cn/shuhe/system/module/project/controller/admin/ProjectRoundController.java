package cn.shuhe.system.module.project.controller.admin;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.shuhe.system.framework.common.pojo.CommonResult;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectRoundRespVO;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectRoundSaveReqVO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectRoundDO;
import cn.shuhe.system.module.project.service.ProjectRoundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

import static cn.shuhe.system.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 项目轮次")
@RestController
@RequestMapping("/project/round")
@Validated
public class ProjectRoundController {

    @Resource
    private ProjectRoundService projectRoundService;

    @PostMapping("/create")
    @Operation(summary = "创建项目轮次")
    @PreAuthorize("@ss.hasPermission('project:info:update')")
    public CommonResult<Long> createProjectRound(@Valid @RequestBody ProjectRoundSaveReqVO createReqVO) {
        return success(projectRoundService.createProjectRound(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新项目轮次")
    @PreAuthorize("@ss.hasPermission('project:info:update')")
    public CommonResult<Boolean> updateProjectRound(@Valid @RequestBody ProjectRoundSaveReqVO updateReqVO) {
        projectRoundService.updateProjectRound(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除项目轮次")
    @Parameter(name = "id", description = "轮次编号", required = true)
    @PreAuthorize("@ss.hasPermission('project:info:update')")
    public CommonResult<Boolean> deleteProjectRound(@RequestParam("id") Long id) {
        projectRoundService.deleteProjectRound(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得项目轮次详情")
    @Parameter(name = "id", description = "轮次编号", required = true)
    @PreAuthorize("@ss.hasPermission('project:info:query')")
    public CommonResult<ProjectRoundRespVO> getProjectRound(@RequestParam("id") Long id) {
        ProjectRoundDO round = projectRoundService.getProjectRound(id);
        return success(convertToRespVO(round));
    }

    @GetMapping("/list")
    @Operation(summary = "获得项目的轮次列表")
    @Parameter(name = "projectId", description = "项目编号", required = true)
    @PreAuthorize("@ss.hasPermission('project:info:query')")
    public CommonResult<List<ProjectRoundRespVO>> getProjectRoundList(@RequestParam("projectId") Long projectId) {
        List<ProjectRoundDO> list = projectRoundService.getProjectRoundList(projectId);
        return success(list.stream().map(this::convertToRespVO).collect(Collectors.toList()));
    }

    /**
     * 将 DO 转换为 RespVO，处理 executorIds 的 JSON 转换
     */
    private ProjectRoundRespVO convertToRespVO(ProjectRoundDO round) {
        if (round == null) {
            return null;
        }
        ProjectRoundRespVO vo = new ProjectRoundRespVO();
        vo.setId(round.getId());
        vo.setProjectId(round.getProjectId());
        vo.setRoundNo(round.getRoundNo());
        vo.setName(round.getName());
        vo.setPlanStartTime(round.getPlanStartTime());
        vo.setPlanEndTime(round.getPlanEndTime());
        vo.setActualStartTime(round.getActualStartTime());
        vo.setActualEndTime(round.getActualEndTime());
        vo.setStatus(round.getStatus());
        vo.setProgress(round.getProgress());
        vo.setResult(round.getResult());
        vo.setAttachments(round.getAttachments());
        vo.setRemark(round.getRemark());
        vo.setCreateTime(round.getCreateTime());
        vo.setUpdateTime(round.getUpdateTime());
        
        // 处理执行人ID列表
        if (StrUtil.isNotBlank(round.getExecutorIds())) {
            vo.setExecutorIds(JSONUtil.toList(round.getExecutorIds(), Long.class));
        }
        vo.setExecutorNames(round.getExecutorNames());
        
        return vo;
    }

    @PutMapping("/update-status")
    @Operation(summary = "更新轮次状态")
    @PreAuthorize("@ss.hasPermission('project:info:update')")
    public CommonResult<Boolean> updateRoundStatus(@RequestParam("id") Long id,
                                                   @RequestParam("status") Integer status) {
        projectRoundService.updateRoundStatus(id, status);
        return success(true);
    }

    @PutMapping("/update-progress")
    @Operation(summary = "更新轮次进度")
    @PreAuthorize("@ss.hasPermission('project:info:update')")
    public CommonResult<Boolean> updateRoundProgress(@RequestParam("id") Long id,
                                                     @RequestParam("progress") Integer progress) {
        projectRoundService.updateRoundProgress(id, progress);
        return success(true);
    }

}
