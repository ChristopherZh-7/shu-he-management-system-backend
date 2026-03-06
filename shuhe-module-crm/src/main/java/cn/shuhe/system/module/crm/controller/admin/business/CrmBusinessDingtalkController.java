package cn.shuhe.system.module.crm.controller.admin.business;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.shuhe.system.module.crm.dal.dataobject.business.CrmBusinessDO;
import cn.shuhe.system.module.crm.dal.mysql.business.CrmBusinessMapper;
import cn.shuhe.system.module.crm.enums.common.CrmAuditStatusEnum;
import cn.shuhe.system.module.crm.service.business.CrmBusinessServiceImpl;
import cn.shuhe.system.module.system.api.dingtalk.DingtalkNotifyApi;
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

/**
 * 商机钉钉回调（互动卡片按钮点击等）
 * <p>
 * 替代群内 @机器人：通过工作通知的互动卡片「通过审批」按钮，点击跳转此接口完成审批
 */
@Tag(name = "管理后台 - CRM 商机钉钉回调")
@RestController
@RequestMapping("/crm/business/dingtalk")
@Validated
@Slf4j
public class CrmBusinessDingtalkController {

    private static final String APPROVE_TOKEN_SALT = "shuhe-business-approve-2026";

    @Resource
    private CrmBusinessMapper businessMapper;
    @Resource
    private CrmBusinessServiceImpl businessService;
    @Resource
    private DingtalkNotifyApi dingtalkNotifyApi;

    /**
     * 商机审批通过回调（钉钉链接点击跳转，支持 GET/POST）
     * 浏览器或钉钉内打开链接时一般为 GET，兼容 POST
     */
    @RequestMapping(value = "/approve", method = {RequestMethod.GET, RequestMethod.POST})
    @PermitAll
    @Operation(summary = "商机审批通过", description = "钉钉互动卡片「通过审批」按钮点击后跳转，免登直接执行审批")
    @Parameter(name = "businessId", description = "商机ID", required = true)
    @Parameter(name = "token", description = "验证令牌", required = true)
    public void approve(
            @RequestParam("businessId") Long businessId,
            @RequestParam("token") String token,
            HttpServletResponse response) throws IOException {

        log.info("[商机钉钉审批] 收到审批请求, businessId={}", businessId);

        response.setContentType("text/html;charset=UTF-8");

        String html;
        try {
            if (businessId == null || StrUtil.isEmpty(token)) {
                html = buildErrorHtml("参数错误");
                response.getWriter().write(html);
                return;
            }

            CrmBusinessDO business = businessMapper.selectById(businessId);
            if (business == null) {
                html = buildErrorHtml("商机不存在");
                response.getWriter().write(html);
                return;
            }

            String expectedToken = SecureUtil.md5(businessId + "-" + APPROVE_TOKEN_SALT);
            if (!expectedToken.equals(token)) {
                html = buildErrorHtml("验证失败，请勿重复操作或链接已失效");
                response.getWriter().write(html);
                return;
            }

            if (!CrmAuditStatusEnum.PROCESS.getStatus().equals(business.getAuditStatus())) {
                String msg = "该商机已处理，当前状态：" + getStatusText(business.getAuditStatus());
                html = buildErrorHtml(msg);
                response.getWriter().write(html);
                return;
            }

            businessService.updateBusinessAuditStatus(businessId, 2); // 2=审批通过
            html = buildSuccessHtml("审批已通过！商机【" + escapeHtml(business.getName()) + "】已通过审核。", businessId);
        } catch (Exception e) {
            log.error("[商机钉钉审批] 处理异常", e);
            html = buildErrorHtml("处理失败：" + (e.getMessage() != null ? escapeHtml(e.getMessage()) : "系统异常"));
        }
        response.getWriter().write(html);
    }

    /**
     * 商机审批驳回回调（钉钉链接点击，支持 GET/POST）
     */
    @RequestMapping(value = "/reject", method = {RequestMethod.GET, RequestMethod.POST})
    @PermitAll
    @Operation(summary = "商机审批驳回")
    @Parameter(name = "businessId", description = "商机ID", required = true)
    @Parameter(name = "token", description = "验证令牌", required = true)
    public void reject(
            @RequestParam("businessId") Long businessId,
            @RequestParam("token") String token,
            HttpServletResponse response) throws IOException {

        log.info("[商机钉钉审批] 收到驳回请求, businessId={}", businessId);

        response.setContentType("text/html;charset=UTF-8");

        String html;
        try {
            if (businessId == null || StrUtil.isEmpty(token)) {
                html = buildErrorHtml("参数错误");
                response.getWriter().write(html);
                return;
            }

            CrmBusinessDO business = businessMapper.selectById(businessId);
            if (business == null) {
                html = buildErrorHtml("商机不存在");
                response.getWriter().write(html);
                return;
            }

            String expectedToken = SecureUtil.md5(businessId + "-" + APPROVE_TOKEN_SALT);
            if (!expectedToken.equals(token)) {
                html = buildErrorHtml("验证失败，请勿重复操作或链接已失效");
                response.getWriter().write(html);
                return;
            }

            if (!CrmAuditStatusEnum.PROCESS.getStatus().equals(business.getAuditStatus())) {
                String msg = "该商机已处理，当前状态：" + getStatusText(business.getAuditStatus());
                html = buildErrorHtml(msg);
                response.getWriter().write(html);
                return;
            }

            businessService.updateBusinessAuditStatus(businessId, 3); // 3=审批驳回
            html = buildSuccessHtml("已驳回！商机【" + escapeHtml(business.getName()) + "】已驳回，请申请人修改后重新提交。", businessId);
        } catch (Exception e) {
            log.error("[商机钉钉审批] 驳回处理异常", e);
            html = buildErrorHtml("处理失败：" + (e.getMessage() != null ? escapeHtml(e.getMessage()) : "系统异常"));
        }
        response.getWriter().write(html);
    }

    private String getStatusText(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 0  -> "未提交";
            case 10 -> "审批中";
            case 20 -> "审批通过";
            case 30 -> "审批驳回";
            case 40 -> "已取消";
            default -> "未知";
        };
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    private String buildSuccessHtml(String message, Long businessId) {
        String detailLink = "";
        if (businessId != null) {
            String baseUrl = dingtalkNotifyApi.getApproveBaseUrl();
            if (StrUtil.isNotEmpty(baseUrl)) {
                String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
                String url = base + "/crm/business/detail/" + businessId;
                detailLink = "<p class=\"link\"><a href=\"" + escapeHtml(url) + "\">→ 查看商机详情</a></p>";
            }
        }
        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">" +
                "<title>审批成功</title><style>*{box-sizing:border-box}body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;margin:0;min-height:100vh;display:flex;align-items:center;justify-content:center;background:#f5f5f5;padding:16px}" +
                ".card{background:#fff;border-radius:12px;box-shadow:0 2px 12px rgba(0,0,0,.08);padding:32px;max-width:400px;text-align:center}" +
                ".icon{font-size:56px;line-height:1;color:#52c41a;margin-bottom:16px}" +
                ".msg{font-size:16px;color:#333;line-height:1.6;margin:0}" +
                ".link{margin-top:24px;font-size:14px}.link a{color:#1677ff;text-decoration:none}.link a:hover{text-decoration:underline}</style></head><body>" +
                "<div class=\"card\"><div class=\"icon\">✓</div><p class=\"msg\">" + message + "</p>" + detailLink + "</div></body></html>";
    }

    private static String buildErrorHtml(String message) {
        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">" +
                "<title>操作失败</title><style>*{box-sizing:border-box}body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;margin:0;min-height:100vh;display:flex;align-items:center;justify-content:center;background:#f5f5f5;padding:16px}" +
                ".card{background:#fff;border-radius:12px;box-shadow:0 2px 12px rgba(0,0,0,.08);padding:32px;max-width:400px;text-align:center}" +
                ".icon{font-size:56px;line-height:1;color:#ff4d4f;margin-bottom:16px}" +
                ".msg{font-size:16px;color:#333;line-height:1.6;margin:0}</style></head><body>" +
                "<div class=\"card\"><div class=\"icon\">✗</div><p class=\"msg\">" + message + "</p></div></body></html>";
    }
}
