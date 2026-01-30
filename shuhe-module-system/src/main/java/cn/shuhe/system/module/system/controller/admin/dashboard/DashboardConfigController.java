package cn.shuhe.system.module.system.controller.admin.dashboard;

import cn.shuhe.system.framework.common.pojo.CommonResult;
import cn.shuhe.system.framework.common.util.object.BeanUtils;
import cn.shuhe.system.module.system.controller.admin.dashboard.vo.DashboardConfigRespVO;
import cn.shuhe.system.module.system.controller.admin.dashboard.vo.DashboardConfigSaveReqVO;
import cn.shuhe.system.module.system.dal.dataobject.dashboard.DashboardConfigDO;
import cn.shuhe.system.module.system.service.dashboard.DashboardConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.shuhe.system.framework.common.pojo.CommonResult.success;
import static cn.shuhe.system.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

/**
 * 管理后台 - 仪表板配置
 */
@Tag(name = "管理后台 - 仪表板配置")
@RestController
@RequestMapping("/system/dashboard-config")
@Validated
public class DashboardConfigController {

    @Resource
    private DashboardConfigService dashboardConfigService;

    @GetMapping("/get")
    @Operation(summary = "获取用户仪表板配置")
    @Parameter(name = "pageType", description = "页面类型", required = true, example = "analytics")
    public CommonResult<DashboardConfigRespVO> getConfig(@RequestParam("pageType") String pageType) {
        Long userId = getLoginUserId();
        DashboardConfigDO config = dashboardConfigService.getConfig(userId, pageType);
        
        // 如果没有自定义配置，返回默认配置
        if (config == null) {
            DashboardConfigRespVO respVO = new DashboardConfigRespVO();
            respVO.setUserId(userId);
            respVO.setPageType(pageType);
            respVO.setLayoutConfig(dashboardConfigService.getDefaultLayoutConfig(pageType));
            return success(respVO);
        }
        
        return success(BeanUtils.toBean(config, DashboardConfigRespVO.class));
    }

    @PostMapping("/save")
    @Operation(summary = "保存用户仪表板配置")
    public CommonResult<Long> saveConfig(@Valid @RequestBody DashboardConfigSaveReqVO saveReqVO) {
        Long userId = getLoginUserId();
        Long configId = dashboardConfigService.saveConfig(userId, saveReqVO);
        return success(configId);
    }

    @DeleteMapping("/reset")
    @Operation(summary = "重置为默认配置")
    @Parameter(name = "pageType", description = "页面类型", required = true, example = "analytics")
    public CommonResult<Boolean> resetConfig(@RequestParam("pageType") String pageType) {
        Long userId = getLoginUserId();
        dashboardConfigService.resetConfig(userId, pageType);
        return success(true);
    }

    @GetMapping("/default")
    @Operation(summary = "获取默认布局配置")
    @Parameter(name = "pageType", description = "页面类型", required = true, example = "analytics")
    public CommonResult<String> getDefaultConfig(@RequestParam("pageType") String pageType) {
        return success(dashboardConfigService.getDefaultLayoutConfig(pageType));
    }

}
