package cn.shuhe.system.module.system.controller.admin.dingtalk;

import cn.shuhe.system.framework.common.pojo.CommonResult;
import cn.shuhe.system.module.system.service.dingtalk.OutsideConfirmService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

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
    private OutsideConfirmService outsideConfirmService;

    /**
     * 外出确认回调接口
     * 
     * 员工在钉钉点击"确认外出"按钮后，会跳转到此页面
     * 系统自动发起钉钉OA外出申请
     */
    @GetMapping("/outside-confirm")
    @PermitAll // 允许匿名访问，因为这是钉钉回调接口，不需要登录
    @Operation(summary = "外出确认回调", description = "员工点击钉钉消息中的确认按钮后触发")
    @Parameter(name = "memberId", description = "外出人员记录ID", required = true)
    @Parameter(name = "token", description = "验证令牌", required = true)
    public void handleOutsideConfirm(
            @RequestParam("memberId") Long memberId,
            @RequestParam("token") String token,
            HttpServletResponse response) throws IOException {
        
        log.info("【外出确认回调】收到确认请求，memberId={}", memberId);
        
        try {
            // 1. 验证 token 并处理确认
            OutsideConfirmService.ConfirmResult result = outsideConfirmService.confirmOutside(memberId, token);
            
            // 2. 返回结果页面（简单的HTML页面）
            response.setContentType("text/html;charset=UTF-8");
            if (result.isSuccess()) {
                response.getWriter().write(buildSuccessHtml(result.getMessage()));
            } else {
                response.getWriter().write(buildErrorHtml(result.getMessage()));
            }
        } catch (Exception e) {
            log.error("【外出确认回调】处理失败", e);
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().write(buildErrorHtml("处理失败：" + e.getMessage()));
        }
    }

    /**
     * 外出确认API接口（供前端调用）
     */
    @PostMapping("/outside-confirm-api")
    @PermitAll // 允许匿名访问，因为这是钉钉回调接口，不需要登录
    @Operation(summary = "外出确认API", description = "通过API确认外出")
    public CommonResult<Boolean> confirmOutsideApi(
            @RequestParam("memberId") Long memberId,
            @RequestParam("token") String token) {
        
        log.info("【外出确认API】收到确认请求，memberId={}", memberId);
        
        OutsideConfirmService.ConfirmResult result = outsideConfirmService.confirmOutside(memberId, token);
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

}
