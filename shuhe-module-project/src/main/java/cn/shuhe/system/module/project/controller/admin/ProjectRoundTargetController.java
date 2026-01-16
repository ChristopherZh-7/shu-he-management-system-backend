package cn.shuhe.system.module.project.controller.admin;

import cn.shuhe.system.framework.common.pojo.CommonResult;
import cn.shuhe.system.framework.common.util.object.BeanUtils;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectRoundTargetRespVO;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectRoundTargetSaveReqVO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectRoundTargetDO;
import cn.shuhe.system.module.project.service.ProjectRoundTargetService;
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

@Tag(name = "管理后台 - 轮次测试目标")
@RestController
@RequestMapping("/project/round-target")
@Validated
public class ProjectRoundTargetController {

    @Resource
    private ProjectRoundTargetService targetService;

    @PostMapping("/create")
    @Operation(summary = "创建测试目标")
    @PreAuthorize("@ss.hasPermission('project:info:create')")
    public CommonResult<Long> createTarget(@Valid @RequestBody ProjectRoundTargetSaveReqVO createReqVO) {
        return success(targetService.createTarget(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新测试目标")
    @PreAuthorize("@ss.hasPermission('project:info:update')")
    public CommonResult<Boolean> updateTarget(@Valid @RequestBody ProjectRoundTargetSaveReqVO updateReqVO) {
        targetService.updateTarget(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除测试目标")
    @Parameter(name = "id", description = "目标ID", required = true)
    @PreAuthorize("@ss.hasPermission('project:info:delete')")
    public CommonResult<Boolean> deleteTarget(@RequestParam("id") Long id) {
        targetService.deleteTarget(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取测试目标")
    @Parameter(name = "id", description = "目标ID", required = true)
    @PreAuthorize("@ss.hasPermission('project:info:query')")
    public CommonResult<ProjectRoundTargetRespVO> getTarget(@RequestParam("id") Long id) {
        ProjectRoundTargetDO target = targetService.getTarget(id);
        return success(BeanUtils.toBean(target, ProjectRoundTargetRespVO.class));
    }

    @GetMapping("/list-by-round")
    @Operation(summary = "获取轮次的测试目标列表")
    @Parameter(name = "roundId", description = "轮次ID", required = true)
    @PreAuthorize("@ss.hasPermission('project:info:query')")
    public CommonResult<List<ProjectRoundTargetRespVO>> getTargetListByRoundId(@RequestParam("roundId") Long roundId) {
        List<ProjectRoundTargetDO> list = targetService.getTargetListByRoundId(roundId);
        return success(BeanUtils.toBean(list, ProjectRoundTargetRespVO.class));
    }

    @PostMapping("/batch-create")
    @Operation(summary = "批量创建测试目标")
    @PreAuthorize("@ss.hasPermission('project:info:create')")
    public CommonResult<Boolean> batchCreateTargets(
            @RequestParam("roundId") Long roundId,
            @Valid @RequestBody List<ProjectRoundTargetSaveReqVO> targets) {
        targetService.batchCreateTargets(roundId, targets);
        return success(true);
    }

}
