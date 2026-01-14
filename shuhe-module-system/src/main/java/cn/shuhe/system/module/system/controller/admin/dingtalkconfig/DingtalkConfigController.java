package cn.shuhe.system.module.system.controller.admin.dingtalkconfig;

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

import cn.shuhe.system.module.system.controller.admin.dingtalkconfig.vo.*;
import cn.shuhe.system.module.system.dal.dataobject.dingtalkconfig.DingtalkConfigDO;
import cn.shuhe.system.module.system.service.dingtalkconfig.DingtalkConfigService;

@Tag(name = "管理后台 - 钉钉配置")
@RestController
@RequestMapping("/system/dingtalk-config")
@Validated
public class DingtalkConfigController {

    @Resource
    private DingtalkConfigService dingtalkConfigService;

    @PostMapping("/create")
    @Operation(summary = "创建钉钉配置")
    @PreAuthorize("@ss.hasPermission('system:dingtalk-config:create')")
    public CommonResult<Long> createDingtalkConfig(@Valid @RequestBody DingtalkConfigSaveReqVO createReqVO) {
        return success(dingtalkConfigService.createDingtalkConfig(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新钉钉配置")
    @PreAuthorize("@ss.hasPermission('system:dingtalk-config:update')")
    public CommonResult<Boolean> updateDingtalkConfig(@Valid @RequestBody DingtalkConfigSaveReqVO updateReqVO) {
        dingtalkConfigService.updateDingtalkConfig(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除钉钉配置")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('system:dingtalk-config:delete')")
    public CommonResult<Boolean> deleteDingtalkConfig(@RequestParam("id") Long id) {
        dingtalkConfigService.deleteDingtalkConfig(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除钉钉配置")
                @PreAuthorize("@ss.hasPermission('system:dingtalk-config:delete')")
    public CommonResult<Boolean> deleteDingtalkConfigList(@RequestParam("ids") List<Long> ids) {
        dingtalkConfigService.deleteDingtalkConfigListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得钉钉配置")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:dingtalk-config:query')")
    public CommonResult<DingtalkConfigRespVO> getDingtalkConfig(@RequestParam("id") Long id) {
        DingtalkConfigDO dingtalkConfig = dingtalkConfigService.getDingtalkConfig(id);
        return success(BeanUtils.toBean(dingtalkConfig, DingtalkConfigRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得钉钉配置分页")
    @PreAuthorize("@ss.hasPermission('system:dingtalk-config:query')")
    public CommonResult<PageResult<DingtalkConfigRespVO>> getDingtalkConfigPage(@Valid DingtalkConfigPageReqVO pageReqVO) {
        PageResult<DingtalkConfigDO> pageResult = dingtalkConfigService.getDingtalkConfigPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, DingtalkConfigRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出钉钉配置 Excel")
    @PreAuthorize("@ss.hasPermission('system:dingtalk-config:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportDingtalkConfigExcel(@Valid DingtalkConfigPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<DingtalkConfigDO> list = dingtalkConfigService.getDingtalkConfigPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "钉钉配置.xls", "数据", DingtalkConfigRespVO.class,
                        BeanUtils.toBean(list, DingtalkConfigRespVO.class));
    }

    @GetMapping("/list-enabled")
    @Operation(summary = "获取启用状态的钉钉配置列表")
    @PreAuthorize("@ss.hasPermission('system:dingtalk-config:query')")
    public CommonResult<List<DingtalkConfigRespVO>> getEnabledDingtalkConfigList() {
        List<DingtalkConfigDO> list = dingtalkConfigService.getEnabledDingtalkConfigList();
        return success(BeanUtils.toBean(list, DingtalkConfigRespVO.class));
    }

    @PostMapping("/sync-dept")
    @Operation(summary = "同步钉钉部门")
    @Parameter(name = "configId", description = "配置编号", required = true)
    @PreAuthorize("@ss.hasPermission('system:dept:create')")
    public CommonResult<Boolean> syncDingtalkDept(@RequestParam("configId") Long configId) {
        dingtalkConfigService.syncDingtalkDept(configId);
        return success(true);
    }

    @GetMapping("/test-dingtalk-api")
    @Operation(summary = "测试钉钉API - 获取部门列表（仅查看数据，不同步）")
    @Parameter(name = "configId", description = "配置编号", required = true)
    @PreAuthorize("@ss.hasPermission('system:dingtalk-config:query')")
    public CommonResult<Object> testDingtalkApi(@RequestParam("configId") Long configId) {
        return success(dingtalkConfigService.testDingtalkApi(configId));
    }

    @PostMapping("/sync-user")
    @Operation(summary = "同步钉钉用户")
    @Parameter(name = "configId", description = "配置编号", required = true)
    @PreAuthorize("@ss.hasPermission('system:user:create')")
    public CommonResult<Boolean> syncDingtalkUser(@RequestParam("configId") Long configId) {
        dingtalkConfigService.syncDingtalkUser(configId);
        return success(true);
    }

}