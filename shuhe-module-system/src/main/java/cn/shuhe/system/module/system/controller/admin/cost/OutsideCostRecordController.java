package cn.shuhe.system.module.system.controller.admin.cost;

import cn.shuhe.system.framework.common.pojo.CommonResult;
import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.module.system.controller.admin.cost.vo.*;
import cn.shuhe.system.module.system.service.cost.OutsideCostRecordService;
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

@Tag(name = "管理后台 - 跨部门项目费用")
@RestController
@RequestMapping("/system/outside-cost")
@Validated
public class OutsideCostRecordController {

    @Resource
    private OutsideCostRecordService outsideCostRecordService;

    @GetMapping("/page")
    @Operation(summary = "获取外出费用记录分页")
    @PreAuthorize("@ss.hasPermission('system:outside-cost:query')")
    public CommonResult<PageResult<OutsideCostRecordRespVO>> getOutsideCostRecordPage(@Valid OutsideCostRecordPageReqVO reqVO) {
        return success(outsideCostRecordService.getOutsideCostRecordPage(reqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获取外出费用记录详情")
    @Parameter(name = "id", description = "记录ID", required = true)
    @PreAuthorize("@ss.hasPermission('system:outside-cost:query')")
    public CommonResult<OutsideCostRecordRespVO> getOutsideCostRecord(@RequestParam("id") Long id) {
        return success(outsideCostRecordService.getOutsideCostRecord(id));
    }

    @PutMapping("/assign")
    @Operation(summary = "指派结算人（B部门负责人操作）")
    @PreAuthorize("@ss.hasPermission('system:outside-cost:assign')")
    public CommonResult<Boolean> assignSettleUser(@Valid @RequestBody OutsideCostAssignReqVO reqVO) {
        outsideCostRecordService.assignSettleUser(reqVO);
        return success(true);
    }

    @PutMapping("/fill")
    @Operation(summary = "填写外出费用金额（结算人操作）")
    @PreAuthorize("@ss.hasPermission('system:outside-cost:fill')")
    public CommonResult<Boolean> fillOutsideCost(@Valid @RequestBody OutsideCostFillReqVO reqVO) {
        outsideCostRecordService.fillOutsideCost(reqVO);
        return success(true);
    }

    @GetMapping("/settle-user/list")
    @Operation(summary = "获取结算人列表")
    @Parameter(name = "id", description = "外出费用记录ID", required = true)
    @PreAuthorize("@ss.hasPermission('system:outside-cost:query')")
    public CommonResult<List<SettleUserVO>> getSettleUserList(@RequestParam("id") Long id) {
        return success(outsideCostRecordService.getSettleUserList(id));
    }
}
