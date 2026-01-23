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
import cn.shuhe.system.module.system.service.dingtalkconfig.DingtalkApiService;

@Tag(name = "管理后台 - 钉钉配置")
@RestController
@RequestMapping("/system/dingtalk-config")
@Validated
public class DingtalkConfigController {

    @Resource
    private DingtalkConfigService dingtalkConfigService;

    @Resource
    private DingtalkApiService dingtalkApiService;

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

    @PostMapping("/sync-post")
    @Operation(summary = "同步钉钉岗位（从用户职位中提取）")
    @Parameter(name = "configId", description = "配置编号", required = true)
    @PreAuthorize("@ss.hasPermission('system:post:create')")
    public CommonResult<Boolean> syncDingtalkPost(@RequestParam("configId") Long configId) {
        dingtalkConfigService.syncDingtalkPost(configId);
        return success(true);
    }

    @GetMapping("/get-form-schema")
    @Operation(summary = "获取OA审批表单Schema（查看表单组件类型）", 
               description = "用于查看表单中所有组件的类型信息，特别是时间组件的类型（日期选择器/日期时间选择器）")
    @Parameter(name = "configId", description = "配置编号（可选，不传则使用第一个启用的配置）", required = false)
    @Parameter(name = "processCode", description = "流程编码（可选，不传则从配置中读取outsideProcessCode）", required = false)
    @PreAuthorize("@ss.hasPermission('system:dingtalk-config:query')")
    public CommonResult<Object> getFormSchema(
            @RequestParam(value = "configId", required = false) Long configId,
            @RequestParam(value = "processCode", required = false) String processCode) {
        
        try {
            // 1. 获取钉钉配置
            DingtalkConfigDO config;
            if (configId != null) {
                config = dingtalkConfigService.getDingtalkConfig(configId);
                if (config == null) {
                    return CommonResult.error(404, "配置不存在");
                }
            } else {
                // 使用第一个启用的配置
                var configs = dingtalkConfigService.getEnabledDingtalkConfigList();
                if (configs.isEmpty()) {
                    return CommonResult.error(404, "没有启用的钉钉配置");
                }
                config = configs.get(0);
            }
            
            // 2. 确定processCode
            String finalProcessCode = processCode;
            if (finalProcessCode == null || finalProcessCode.isEmpty()) {
                finalProcessCode = config.getOutsideProcessCode();
                if (finalProcessCode == null || finalProcessCode.isEmpty()) {
                    return CommonResult.error(400, "流程编码未配置，请在参数中传入processCode或在配置中设置outsideProcessCode");
                }
            }
            
            // 3. 获取accessToken
            String accessToken = dingtalkApiService.getAccessToken(config);
            
            // 4. 获取表单Schema
            String schemaJson = dingtalkApiService.getFormSchema(accessToken, finalProcessCode);
            
            if (schemaJson == null) {
                return CommonResult.error(500, "获取表单Schema失败，请检查流程编码是否正确");
            }
            
            // 5. 解析并返回（格式化JSON以便查看）
            return success(cn.hutool.json.JSONUtil.parse(schemaJson));
            
        } catch (Exception e) {
            return CommonResult.error(500, "获取表单Schema异常: " + e.getMessage());
        }
    }

}