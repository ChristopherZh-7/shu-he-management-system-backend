package cn.shuhe.system.module.system.controller.admin.dingtalkmapping;

import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.constraints.*;
import jakarta.validation.*;
import jakarta.servlet.http.*;
import java.util.*;
import java.io.IOException;

import cn.shuhe.system.framework.common.pojo.PageParam;
import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.common.pojo.CommonResult;
import cn.shuhe.system.framework.common.util.object.BeanUtils;
import static cn.shuhe.system.framework.common.pojo.CommonResult.success;

import cn.shuhe.system.framework.excel.core.util.ExcelUtils;

import cn.shuhe.system.framework.apilog.core.annotation.ApiAccessLog;
import static cn.shuhe.system.framework.apilog.core.enums.OperateTypeEnum.*;

import cn.shuhe.system.module.system.controller.admin.dingtalkmapping.vo.*;
import cn.shuhe.system.module.system.dal.dataobject.dingtalkmapping.DingtalkMappingDO;
import cn.shuhe.system.module.system.service.dingtalkmapping.DingtalkMappingService;

@Tag(name = "管理后台 - 钉钉数据映射")
@RestController
@RequestMapping("/system/dingtalk-mapping")
@Validated
public class DingtalkMappingController {

    @Resource
    private DingtalkMappingService dingtalkMappingService;

    @PostMapping("/create")
    @Operation(summary = "创建钉钉数据映射")
    @PreAuthorize("@ss.hasPermission('system:dingtalk-mapping:create')")
    public CommonResult<Long> createDingtalkMapping(@Valid @RequestBody DingtalkMappingSaveReqVO createReqVO) {
        return success(dingtalkMappingService.createDingtalkMapping(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新钉钉数据映射")
    @PreAuthorize("@ss.hasPermission('system:dingtalk-mapping:update')")
    public CommonResult<Boolean> updateDingtalkMapping(@Valid @RequestBody DingtalkMappingSaveReqVO updateReqVO) {
        dingtalkMappingService.updateDingtalkMapping(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除钉钉数据映射")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('system:dingtalk-mapping:delete')")
    public CommonResult<Boolean> deleteDingtalkMapping(@RequestParam("id") Long id) {
        dingtalkMappingService.deleteDingtalkMapping(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除钉钉数据映射")
                @PreAuthorize("@ss.hasPermission('system:dingtalk-mapping:delete')")
    public CommonResult<Boolean> deleteDingtalkMappingList(@RequestParam("ids") List<Long> ids) {
        dingtalkMappingService.deleteDingtalkMappingListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得钉钉数据映射")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:dingtalk-mapping:query')")
    public CommonResult<DingtalkMappingRespVO> getDingtalkMapping(@RequestParam("id") Long id) {
        DingtalkMappingDO dingtalkMapping = dingtalkMappingService.getDingtalkMapping(id);
        return success(BeanUtils.toBean(dingtalkMapping, DingtalkMappingRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得钉钉数据映射分页")
    @PreAuthorize("@ss.hasPermission('system:dingtalk-mapping:query')")
    public CommonResult<PageResult<DingtalkMappingRespVO>> getDingtalkMappingPage(@Valid DingtalkMappingPageReqVO pageReqVO) {
        PageResult<DingtalkMappingDO> pageResult = dingtalkMappingService.getDingtalkMappingPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, DingtalkMappingRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出钉钉数据映射 Excel")
    @PreAuthorize("@ss.hasPermission('system:dingtalk-mapping:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportDingtalkMappingExcel(@Valid DingtalkMappingPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<DingtalkMappingDO> list = dingtalkMappingService.getDingtalkMappingPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "钉钉数据映射.xls", "数据", DingtalkMappingRespVO.class,
                        BeanUtils.toBean(list, DingtalkMappingRespVO.class));
    }

}