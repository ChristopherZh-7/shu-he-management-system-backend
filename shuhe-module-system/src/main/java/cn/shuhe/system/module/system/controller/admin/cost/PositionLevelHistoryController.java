package cn.shuhe.system.module.system.controller.admin.cost;

import cn.shuhe.system.framework.common.pojo.CommonResult;
import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.module.system.controller.admin.cost.vo.PositionLevelHistoryPageReqVO;
import cn.shuhe.system.module.system.controller.admin.cost.vo.PositionLevelHistoryRespVO;
import cn.shuhe.system.module.system.controller.admin.cost.vo.PositionLevelHistorySaveReqVO;
import cn.shuhe.system.module.system.service.cost.PositionLevelHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.shuhe.system.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 职级变更记录
 */
@Tag(name = "管理后台 - 职级变更记录")
@RestController
@RequestMapping("/system/position-history")
@Validated
public class PositionLevelHistoryController {

    @Resource
    private PositionLevelHistoryService positionLevelHistoryService;

    @PostMapping("/create")
    @Operation(summary = "创建职级变更记录")
    @PreAuthorize("@ss.hasPermission('system:position-history:create')")
    public CommonResult<Long> createHistory(@Valid @RequestBody PositionLevelHistorySaveReqVO reqVO) {
        return success(positionLevelHistoryService.createHistory(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新职级变更记录")
    @PreAuthorize("@ss.hasPermission('system:position-history:update')")
    public CommonResult<Boolean> updateHistory(@Valid @RequestBody PositionLevelHistorySaveReqVO reqVO) {
        positionLevelHistoryService.updateHistory(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除职级变更记录")
    @Parameter(name = "id", description = "记录ID", required = true)
    @PreAuthorize("@ss.hasPermission('system:position-history:delete')")
    public CommonResult<Boolean> deleteHistory(@RequestParam("id") Long id) {
        positionLevelHistoryService.deleteHistory(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取职级变更记录详情")
    @Parameter(name = "id", description = "记录ID", required = true)
    @PreAuthorize("@ss.hasPermission('system:position-history:query')")
    public CommonResult<PositionLevelHistoryRespVO> getHistory(@RequestParam("id") Long id) {
        return success(positionLevelHistoryService.getHistory(id));
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询职级变更记录")
    @PreAuthorize("@ss.hasPermission('system:position-history:query')")
    public CommonResult<PageResult<PositionLevelHistoryRespVO>> getHistoryPage(@Valid PositionLevelHistoryPageReqVO reqVO) {
        return success(positionLevelHistoryService.getHistoryPage(reqVO));
    }

}
