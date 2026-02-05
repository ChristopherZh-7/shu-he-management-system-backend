package cn.shuhe.system.module.system.controller.admin.dingtalkrobot;

import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;

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

import cn.shuhe.system.module.system.controller.admin.dingtalkrobot.vo.*;
import cn.shuhe.system.module.system.dal.dataobject.dingtalkrobot.DingtalkRobotDO;
import cn.shuhe.system.module.system.dal.dataobject.dingtalkrobot.DingtalkRobotMessageDO;
import cn.shuhe.system.module.system.service.dingtalkrobot.DingtalkRobotService;
import cn.shuhe.system.module.system.service.dingtalkconfig.DingtalkApiService.RobotSendResult;

@Tag(name = "管理后台 - 钉钉群机器人")
@RestController
@RequestMapping("/system/dingtalk-robot")
@Validated
public class DingtalkRobotController {

    @Resource
    private DingtalkRobotService dingtalkRobotService;

    // ==================== 机器人配置管理 ====================

    @PostMapping("/create")
    @Operation(summary = "创建群机器人配置")
    @PreAuthorize("@ss.hasPermission('system:dingtalk-robot:create')")
    public CommonResult<Long> createDingtalkRobot(@Valid @RequestBody DingtalkRobotSaveReqVO createReqVO) {
        return success(dingtalkRobotService.createDingtalkRobot(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新群机器人配置")
    @PreAuthorize("@ss.hasPermission('system:dingtalk-robot:update')")
    public CommonResult<Boolean> updateDingtalkRobot(@Valid @RequestBody DingtalkRobotSaveReqVO updateReqVO) {
        dingtalkRobotService.updateDingtalkRobot(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除群机器人配置")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('system:dingtalk-robot:delete')")
    public CommonResult<Boolean> deleteDingtalkRobot(@RequestParam("id") Long id) {
        dingtalkRobotService.deleteDingtalkRobot(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除群机器人配置")
    @Parameter(name = "ids", description = "编号列表", required = true)
    @PreAuthorize("@ss.hasPermission('system:dingtalk-robot:delete')")
    public CommonResult<Boolean> deleteDingtalkRobotList(@RequestParam("ids") List<Long> ids) {
        dingtalkRobotService.deleteDingtalkRobotListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得群机器人配置")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:dingtalk-robot:query')")
    public CommonResult<DingtalkRobotRespVO> getDingtalkRobot(@RequestParam("id") Long id) {
        DingtalkRobotDO robot = dingtalkRobotService.getDingtalkRobot(id);
        return success(BeanUtils.toBean(robot, DingtalkRobotRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得群机器人配置分页")
    @PreAuthorize("@ss.hasPermission('system:dingtalk-robot:query')")
    public CommonResult<PageResult<DingtalkRobotRespVO>> getDingtalkRobotPage(@Valid DingtalkRobotPageReqVO pageReqVO) {
        PageResult<DingtalkRobotDO> pageResult = dingtalkRobotService.getDingtalkRobotPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, DingtalkRobotRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出群机器人配置 Excel")
    @PreAuthorize("@ss.hasPermission('system:dingtalk-robot:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportDingtalkRobotExcel(@Valid DingtalkRobotPageReqVO pageReqVO,
                                          HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<DingtalkRobotDO> list = dingtalkRobotService.getDingtalkRobotPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "群机器人配置.xls", "数据", DingtalkRobotRespVO.class,
                BeanUtils.toBean(list, DingtalkRobotRespVO.class));
    }

    @GetMapping("/list-enabled")
    @Operation(summary = "获取启用状态的群机器人配置列表")
    @PreAuthorize("@ss.hasPermission('system:dingtalk-robot:query')")
    public CommonResult<List<DingtalkRobotRespVO>> getEnabledDingtalkRobotList() {
        List<DingtalkRobotDO> list = dingtalkRobotService.getEnabledDingtalkRobotList();
        return success(BeanUtils.toBean(list, DingtalkRobotRespVO.class));
    }

    // ==================== 消息发送 ====================

    @PostMapping("/send")
    @Operation(summary = "发送群机器人消息", 
            description = "支持text/markdown/link/actionCard类型消息，可@指定人员或@所有人")
    @PreAuthorize("@ss.hasPermission('system:dingtalk-robot:send')")
    public CommonResult<RobotSendResult> sendMessage(@Valid @RequestBody DingtalkRobotSendReqVO reqVO) {
        RobotSendResult result;
        boolean isAtAll = reqVO.getIsAtAll() != null && reqVO.getIsAtAll();
        
        switch (reqVO.getMsgType()) {
            case "text":
                result = dingtalkRobotService.sendTextMessage(
                        reqVO.getRobotId(), reqVO.getContent(),
                        reqVO.getAtMobiles(), reqVO.getAtUserIds(), isAtAll);
                break;
            case "markdown":
                result = dingtalkRobotService.sendMarkdownMessage(
                        reqVO.getRobotId(), reqVO.getTitle(), reqVO.getContent(),
                        reqVO.getAtMobiles(), reqVO.getAtUserIds(), isAtAll);
                break;
            case "link":
                result = dingtalkRobotService.sendLinkMessage(
                        reqVO.getRobotId(), reqVO.getTitle(), reqVO.getContent(),
                        reqVO.getMessageUrl(), reqVO.getPicUrl());
                break;
            case "actionCard":
                result = dingtalkRobotService.sendActionCardMessage(
                        reqVO.getRobotId(), reqVO.getTitle(), reqVO.getContent(),
                        reqVO.getSingleTitle(), reqVO.getMessageUrl());
                break;
            default:
                return CommonResult.error(400, "不支持的消息类型: " + reqVO.getMsgType());
        }
        
        return success(result);
    }

    @PostMapping("/test-send")
    @Operation(summary = "测试群机器人发送", description = "发送一条测试消息验证机器人配置")
    @Parameter(name = "robotId", description = "机器人编号", required = true)
    @PreAuthorize("@ss.hasPermission('system:dingtalk-robot:send')")
    public CommonResult<RobotSendResult> testRobotSend(@RequestParam("robotId") Long robotId) {
        return success(dingtalkRobotService.testRobotSend(robotId));
    }

    // ==================== 消息记录 ====================

    @GetMapping("/message/page")
    @Operation(summary = "获得群机器人消息记录分页")
    @PreAuthorize("@ss.hasPermission('system:dingtalk-robot:query')")
    public CommonResult<PageResult<DingtalkRobotMessageRespVO>> getMessagePage(@Valid DingtalkRobotMessagePageReqVO pageReqVO) {
        PageResult<DingtalkRobotMessageDO> pageResult = dingtalkRobotService.getMessagePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, DingtalkRobotMessageRespVO.class));
    }

}
