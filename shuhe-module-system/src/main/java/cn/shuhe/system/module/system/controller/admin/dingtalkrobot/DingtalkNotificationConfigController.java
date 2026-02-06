package cn.shuhe.system.module.system.controller.admin.dingtalkrobot;

import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import cn.shuhe.system.framework.common.pojo.CommonResult;
import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.common.util.object.BeanUtils;
import cn.shuhe.system.module.system.controller.admin.dingtalkrobot.vo.*;
import cn.shuhe.system.module.system.dal.dataobject.dingtalkrobot.DingtalkNotificationConfigDO;
import cn.shuhe.system.module.system.dal.dataobject.dingtalkrobot.DingtalkNotificationLogDO;
import cn.shuhe.system.module.system.dal.dataobject.dingtalkrobot.DingtalkRobotDO;
import cn.shuhe.system.module.system.service.dingtalkrobot.DingtalkNotificationConfigService;
import cn.shuhe.system.module.system.service.dingtalkrobot.DingtalkRobotService;

import static cn.shuhe.system.framework.common.pojo.CommonResult.success;

/**
 * 钉钉通知场景配置 Controller
 *
 * @author shuhe
 */
@Tag(name = "管理后台 - 钉钉通知场景配置")
@RestController
@RequestMapping("/system/dingtalk-notification-config")
@Validated
public class DingtalkNotificationConfigController {

    @Resource
    private DingtalkNotificationConfigService notificationConfigService;

    @Resource
    private DingtalkRobotService dingtalkRobotService;

    // ==================== 配置管理 ====================

    @PostMapping("/create")
    @Operation(summary = "创建通知场景配置")
    @PreAuthorize("@ss.hasPermission('system:dingtalk-notification:create')")
    public CommonResult<Long> createNotificationConfig(@Valid @RequestBody DingtalkNotificationConfigSaveReqVO createReqVO) {
        return success(notificationConfigService.createNotificationConfig(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新通知场景配置")
    @PreAuthorize("@ss.hasPermission('system:dingtalk-notification:update')")
    public CommonResult<Boolean> updateNotificationConfig(@Valid @RequestBody DingtalkNotificationConfigSaveReqVO updateReqVO) {
        notificationConfigService.updateNotificationConfig(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除通知场景配置")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('system:dingtalk-notification:delete')")
    public CommonResult<Boolean> deleteNotificationConfig(@RequestParam("id") Long id) {
        notificationConfigService.deleteNotificationConfig(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得通知场景配置")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('system:dingtalk-notification:query')")
    public CommonResult<DingtalkNotificationConfigRespVO> getNotificationConfig(@RequestParam("id") Long id) {
        DingtalkNotificationConfigDO config = notificationConfigService.getNotificationConfig(id);
        DingtalkNotificationConfigRespVO respVO = BeanUtils.toBean(config, DingtalkNotificationConfigRespVO.class);
        // 填充机器人名称
        if (respVO != null && respVO.getRobotId() != null) {
            DingtalkRobotDO robot = dingtalkRobotService.getDingtalkRobot(respVO.getRobotId());
            if (robot != null) {
                respVO.setRobotName(robot.getName());
            }
        }
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得通知场景配置分页")
    @PreAuthorize("@ss.hasPermission('system:dingtalk-notification:query')")
    public CommonResult<PageResult<DingtalkNotificationConfigRespVO>> getNotificationConfigPage(
            @Valid DingtalkNotificationConfigPageReqVO pageReqVO) {
        PageResult<DingtalkNotificationConfigDO> pageResult = notificationConfigService.getNotificationConfigPage(pageReqVO);
        PageResult<DingtalkNotificationConfigRespVO> respPage = BeanUtils.toBean(pageResult, DingtalkNotificationConfigRespVO.class);
        // 填充机器人名称
        if (respPage.getList() != null) {
            for (DingtalkNotificationConfigRespVO respVO : respPage.getList()) {
                if (respVO.getRobotId() != null) {
                    DingtalkRobotDO robot = dingtalkRobotService.getDingtalkRobot(respVO.getRobotId());
                    if (robot != null) {
                        respVO.setRobotName(robot.getName());
                    }
                }
            }
        }
        return success(respPage);
    }

    // ==================== 日志查询 ====================

    @GetMapping("/log/page")
    @Operation(summary = "获得通知发送日志分页")
    @PreAuthorize("@ss.hasPermission('system:dingtalk-notification:query')")
    public CommonResult<PageResult<DingtalkNotificationLogRespVO>> getNotificationLogPage(
            @Valid DingtalkNotificationLogPageReqVO pageReqVO) {
        PageResult<DingtalkNotificationLogDO> pageResult = notificationConfigService.getNotificationLogPage(pageReqVO);
        PageResult<DingtalkNotificationLogRespVO> respPage = BeanUtils.toBean(pageResult, DingtalkNotificationLogRespVO.class);
        // 填充配置名称和机器人名称
        if (respPage.getList() != null) {
            for (DingtalkNotificationLogRespVO respVO : respPage.getList()) {
                if (respVO.getConfigId() != null) {
                    DingtalkNotificationConfigDO config = notificationConfigService.getNotificationConfig(respVO.getConfigId());
                    if (config != null) {
                        respVO.setConfigName(config.getName());
                    }
                }
                if (respVO.getRobotId() != null) {
                    DingtalkRobotDO robot = dingtalkRobotService.getDingtalkRobot(respVO.getRobotId());
                    if (robot != null) {
                        respVO.setRobotName(robot.getName());
                    }
                }
            }
        }
        return success(respPage);
    }

    // ==================== 元数据 ====================

    @GetMapping("/event-types")
    @Operation(summary = "获取支持的事件类型列表")
    @PreAuthorize("@ss.hasPermission('system:dingtalk-notification:query')")
    public CommonResult<List<Map<String, Object>>> getSupportedEventTypes() {
        return success(notificationConfigService.getSupportedEventTypes());
    }

    @GetMapping("/event-modules")
    @Operation(summary = "获取支持的事件模块列表")
    @PreAuthorize("@ss.hasPermission('system:dingtalk-notification:query')")
    public CommonResult<List<Map<String, String>>> getSupportedEventModules() {
        return success(notificationConfigService.getSupportedEventModules());
    }

}
