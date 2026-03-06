package cn.shuhe.system.module.system.controller.admin.dingtalk;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.shuhe.system.framework.common.pojo.CommonResult;
import cn.shuhe.system.module.system.dal.dataobject.dingtalkconfig.DingtalkConfigDO;
import cn.shuhe.system.module.system.service.dingtalk.ServiceLaunchConfirmService;
import cn.shuhe.system.module.infra.event.dingtalk.DingtalkRobotMessageEvent;
import cn.shuhe.system.module.system.service.dingtalkconfig.DingtalkApiService;
import cn.shuhe.system.module.system.service.dingtalkconfig.DingtalkConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

import static cn.shuhe.system.framework.common.pojo.CommonResult.success;

/**
 * 钉钉回调控制器
 * 
 * 处理钉钉互动卡片按钮点击等回调事件
 */
@Tag(name = "管理后台 - 钉钉回调")
@RestController
@RequestMapping("/system/dingtalk/callback")
@Validated
@Slf4j
public class DingtalkCallbackController {

    @Resource
    private ServiceLaunchConfirmService serviceLaunchConfirmService;
    @Resource
    private ApplicationEventPublisher applicationEventPublisher;
    @Resource
    private DingtalkApiService dingtalkApiService;
    @Resource
    private DingtalkConfigService dingtalkConfigService;

    /**
     * 服务发起确认回调接口
     * 
     * 员工在钉钉点击"确认外出"按钮后，会跳转到此页面
     * 系统自动发起钉钉OA外出申请
     */
    @GetMapping("/service-launch-confirm")
    @PermitAll
    @Operation(summary = "服务发起确认回调", description = "员工点击钉钉消息中的确认按钮后触发（统一服务发起版本）")
    @Parameter(name = "memberId", description = "执行人记录ID", required = true)
    @Parameter(name = "token", description = "验证令牌", required = true)
    public void handleServiceLaunchConfirm(
            @RequestParam("memberId") Long memberId,
            @RequestParam("token") String token,
            HttpServletResponse response) throws IOException {
        
        log.info("【服务发起确认回调】收到确认请求，memberId={}", memberId);
        
        try {
            ServiceLaunchConfirmService.ConfirmResult result = serviceLaunchConfirmService.confirmOutside(memberId, token);
            
            response.setContentType("text/html;charset=UTF-8");
            if (result.isSuccess()) {
                response.getWriter().write(buildSuccessHtml(result.getMessage()));
            } else {
                response.getWriter().write(buildErrorHtml(result.getMessage()));
            }
        } catch (Exception e) {
            log.error("【服务发起确认回调】处理失败", e);
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().write(buildErrorHtml("处理失败：" + e.getMessage()));
        }
    }

    /**
     * 服务发起确认API接口（供前端调用）
     */
    @PostMapping("/service-launch-confirm-api")
    @PermitAll
    @Operation(summary = "服务发起确认API", description = "通过API确认外出（统一服务发起版本）")
    public CommonResult<Boolean> confirmServiceLaunchApi(
            @RequestParam("memberId") Long memberId,
            @RequestParam("token") String token) {
        
        log.info("【服务发起确认API】收到确认请求，memberId={}", memberId);
        
        ServiceLaunchConfirmService.ConfirmResult result = serviceLaunchConfirmService.confirmOutside(memberId, token);
        if (result.isSuccess()) {
            return success(true);
        } else {
            return CommonResult.error(500, result.getMessage());
        }
    }

    /**
     * 构建成功页面HTML
     */
    private String buildSuccessHtml(String message) {
        // 转义HTML特殊字符，防止XSS攻击
        String escapedMessage = message != null ? message
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;") : "";
        
        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>确认成功</title>\n" +
                "    <style>\n" +
                "        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; \n" +
                "               display: flex; justify-content: center; align-items: center; \n" +
                "               min-height: 100vh; margin: 0; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }\n" +
                "        .card { background: white; padding: 40px; border-radius: 16px; \n" +
                "                box-shadow: 0 10px 40px rgba(0,0,0,0.2); text-align: center; max-width: 400px; }\n" +
                "        .icon { font-size: 64px; margin-bottom: 20px; }\n" +
                "        h1 { color: #10b981; margin: 0 0 16px 0; font-size: 24px; }\n" +
                "        p { color: #6b7280; margin: 0; line-height: 1.6; }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"card\">\n" +
                "        <div class=\"icon\">✅</div>\n" +
                "        <h1>确认成功</h1>\n" +
                "        <p>" + escapedMessage + "</p>\n" +
                "    </div>\n" +
                "</body>\n" +
                "</html>";
    }

    /**
     * 构建失败页面HTML
     */
    private String buildErrorHtml(String message) {
        // 转义HTML特殊字符，防止XSS攻击
        String escapedMessage = message != null ? message
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;") : "";
        
        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>确认失败</title>\n" +
                "    <style>\n" +
                "        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; \n" +
                "               display: flex; justify-content: center; align-items: center; \n" +
                "               min-height: 100vh; margin: 0; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }\n" +
                "        .card { background: white; padding: 40px; border-radius: 16px; \n" +
                "                box-shadow: 0 10px 40px rgba(0,0,0,0.2); text-align: center; max-width: 400px; }\n" +
                "        .icon { font-size: 64px; margin-bottom: 20px; }\n" +
                "        h1 { color: #ef4444; margin: 0 0 16px 0; font-size: 24px; }\n" +
                "        p { color: #6b7280; margin: 0; line-height: 1.6; }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"card\">\n" +
                "        <div class=\"icon\">❌</div>\n" +
                "        <h1>确认失败</h1>\n" +
                "        <p>" + escapedMessage + "</p>\n" +
                "    </div>\n" +
                "</body>\n" +
                "</html>";
    }

    // ==================== 钉钉机器人消息回调 ====================

    /**
     * 钉钉机器人消息回调
     * 当用户在群里@机器人发消息时，钉钉会将消息POST到此接口
     * 需要在钉钉开放平台配置消息接收地址为: {域名}/admin-api/system/dingtalk/callback/robot-message
     */
    @PostMapping("/robot-message")
    @PermitAll
    @Operation(summary = "钉钉机器人消息回调", description = "处理群内@机器人的消息")
    public CommonResult<String> handleRobotMessage(@RequestBody String body) {
        log.info("[handleRobotMessage] 收到钉钉机器人回调: {}", body);
        try {
            JSONObject json = cn.hutool.json.JSONUtil.parseObj(body);
            String chatId = json.getStr("conversationId");
            String senderUserId = json.getStr("senderStaffId");
            // 消息内容（去掉@机器人的部分）
            JSONObject text = json.getJSONObject("text");
            String content = text != null ? text.getStr("content", "").trim() : "";

            if (StrUtil.isEmpty(chatId) || StrUtil.isEmpty(content)) {
                return success("ignored");
            }

            applicationEventPublisher.publishEvent(
                    new DingtalkRobotMessageEvent(this, chatId, senderUserId, content));
            return success("ok");
        } catch (Exception e) {
            log.error("[handleRobotMessage] 处理机器人消息失败", e);
            return success("error");
        }
    }

    // ==================== 测试接口（调试完成后可删除）====================

    /**
     * 测试发起外出审批（仅用于调试）
     * 
     * 使用"超过1天连续外出"类型测试，避免时间跨天问题
     */
    @PostMapping("/test-outside-approval")
    @PermitAll
    @Operation(summary = "测试发起外出审批", description = "仅用于调试，测试钉钉外出审批发起")
    public CommonResult<String> testOutsideApproval(
            @RequestParam("dingtalkUserId") String dingtalkUserId,
            @RequestParam("deptId") Long deptId) {
        
        log.info("【测试外出审批】dingtalkUserId={}, deptId={}", dingtalkUserId, deptId);
        
        try {
            // 获取钉钉配置
            List<DingtalkConfigDO> configList = dingtalkConfigService.getEnabledDingtalkConfigList();
            if (configList == null || configList.isEmpty()) {
                return CommonResult.error(500, "未找到有效的钉钉配置");
            }
            DingtalkConfigDO config = configList.get(0);
            
            // 获取 accessToken
            String accessToken = dingtalkApiService.getAccessToken(config);
            
            // 测试参数 - 使用"超过1天连续外出"避免时间格式问题
            String processCode = config.getOutsideProcessCode();
            String outsideType = "超过1天连续外出";
            String startTime = "2026-01-27";  // 明天
            String endTime = "2026-01-28";    // 后天
            double duration = 1.0;            // 1天
            
            // 发起审批
            String instanceId = dingtalkApiService.startOutsideSuiteApproval(
                    accessToken, processCode, dingtalkUserId, deptId,
                    outsideType, startTime, endTime, duration,
                    "测试项目", "测试外出", "测试地点");
            
            if (instanceId != null) {
                return success("发起成功！审批实例ID: " + instanceId);
            } else {
                return CommonResult.error(500, "发起失败，请查看后台日志");
            }
        } catch (Exception e) {
            log.error("【测试外出审批】异常", e);
            return CommonResult.error(500, "异常: " + e.getMessage());
        }
    }

    /**
     * 获取表单Schema（用于查看表单结构）
     */
    @GetMapping("/test-form-schema")
    @PermitAll
    @Operation(summary = "获取表单Schema", description = "查看外出申请表单的结构")
    public CommonResult<String> testFormSchema() {
        try {
            List<DingtalkConfigDO> configList = dingtalkConfigService.getEnabledDingtalkConfigList();
            if (configList == null || configList.isEmpty()) {
                return CommonResult.error(500, "未找到有效的钉钉配置");
            }
            DingtalkConfigDO config = configList.get(0);
            
            String accessToken = dingtalkApiService.getAccessToken(config);
            String schema = dingtalkApiService.getFormSchema(accessToken, config.getOutsideProcessCode());
            
            return success(schema);
        } catch (Exception e) {
            log.error("【获取表单Schema】异常", e);
            return CommonResult.error(500, "异常: " + e.getMessage());
        }
    }

}
