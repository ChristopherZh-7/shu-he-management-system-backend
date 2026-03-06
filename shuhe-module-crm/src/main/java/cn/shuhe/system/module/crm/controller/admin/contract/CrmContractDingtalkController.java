package cn.shuhe.system.module.crm.controller.admin.contract;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.shuhe.system.module.crm.dal.dataobject.contract.CrmContractDO;
import cn.shuhe.system.module.crm.dal.mysql.contract.CrmContractMapper;
import cn.shuhe.system.module.crm.enums.common.CrmAuditStatusEnum;
import cn.shuhe.system.module.crm.service.contract.CrmContractService;
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
 * 合同钉钉回调 —— 群内"确认合同"/"驳回"按钮点击
 */
@Tag(name = "管理后台 - CRM 合同钉钉回调")
@RestController
@RequestMapping("/crm/contract/dingtalk")
@Validated
@Slf4j
public class CrmContractDingtalkController {

    private static final String APPROVE_TOKEN_SALT = "shuhe-contract-approve-2026";

    @Resource
    private CrmContractMapper contractMapper;
    @Resource
    private CrmContractService contractService;
    @Resource
    private DingtalkNotifyApi dingtalkNotifyApi;

    /**
     * 合同确认（点击群内"确认合同"链接）
     */
    @RequestMapping(value = "/approve", method = {RequestMethod.GET, RequestMethod.POST})
    @PermitAll
    @Operation(summary = "确认合同", description = "钉钉群内「确认合同」按钮点击后跳转，免登直接确认")
    @Parameter(name = "contractId", description = "合同ID", required = true)
    @Parameter(name = "token",      description = "验证令牌", required = true)
    public void approve(
            @RequestParam("contractId") Long contractId,
            @RequestParam("token") String token,
            HttpServletResponse response) throws IOException {

        log.info("[合同钉钉确认] contractId={}", contractId);
        response.setContentType("text/html;charset=UTF-8");
        String html;
        try {
            if (contractId == null || StrUtil.isEmpty(token)) {
                html = buildErrorHtml("参数错误");
                response.getWriter().write(html);
                return;
            }
            CrmContractDO contract = contractMapper.selectById(contractId);
            if (contract == null) {
                html = buildErrorHtml("合同不存在");
                response.getWriter().write(html);
                return;
            }
            String expected = SecureUtil.md5(contractId + "-" + APPROVE_TOKEN_SALT);
            if (!expected.equals(token)) {
                html = buildErrorHtml("验证失败，链接已失效或已操作过");
                response.getWriter().write(html);
                return;
            }
            if (!CrmAuditStatusEnum.PROCESS.getStatus().equals(contract.getAuditStatus())) {
                html = buildErrorHtml("该合同已处理，当前状态：" + getStatusText(contract.getAuditStatus()));
                response.getWriter().write(html);
                return;
            }
            contractService.approveContractByDingtalk(contractId);
            html = buildSuccessHtml("合同【" + escapeHtml(contract.getName()) + "】已确认，合同正式生效！", contractId);
        } catch (Exception e) {
            log.error("[合同钉钉确认] 处理异常", e);
            html = buildErrorHtml("处理失败：" + (e.getMessage() != null ? escapeHtml(e.getMessage()) : "系统异常"));
        }
        response.getWriter().write(html);
    }

    /**
     * 合同驳回（点击群内"驳回"链接）
     */
    @RequestMapping(value = "/reject", method = {RequestMethod.GET, RequestMethod.POST})
    @PermitAll
    @Operation(summary = "驳回合同")
    @Parameter(name = "contractId", description = "合同ID", required = true)
    @Parameter(name = "token",      description = "验证令牌", required = true)
    public void reject(
            @RequestParam("contractId") Long contractId,
            @RequestParam("token") String token,
            HttpServletResponse response) throws IOException {

        log.info("[合同钉钉驳回] contractId={}", contractId);
        response.setContentType("text/html;charset=UTF-8");
        String html;
        try {
            if (contractId == null || StrUtil.isEmpty(token)) {
                html = buildErrorHtml("参数错误");
                response.getWriter().write(html);
                return;
            }
            CrmContractDO contract = contractMapper.selectById(contractId);
            if (contract == null) {
                html = buildErrorHtml("合同不存在");
                response.getWriter().write(html);
                return;
            }
            String expected = SecureUtil.md5(contractId + "-" + APPROVE_TOKEN_SALT);
            if (!expected.equals(token)) {
                html = buildErrorHtml("验证失败，链接已失效或已操作过");
                response.getWriter().write(html);
                return;
            }
            if (!CrmAuditStatusEnum.PROCESS.getStatus().equals(contract.getAuditStatus())) {
                html = buildErrorHtml("该合同已处理，当前状态：" + getStatusText(contract.getAuditStatus()));
                response.getWriter().write(html);
                return;
            }
            contractService.rejectContractByDingtalk(contractId);
            html = buildSuccessHtml("合同【" + escapeHtml(contract.getName()) + "】已驳回，请修改后重新提交。", contractId);
        } catch (Exception e) {
            log.error("[合同钉钉驳回] 处理异常", e);
            html = buildErrorHtml("处理失败：" + (e.getMessage() != null ? escapeHtml(e.getMessage()) : "系统异常"));
        }
        response.getWriter().write(html);
    }

    private String getStatusText(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 0  -> "待提交";
            case 10 -> "待确认";
            case 20 -> "已确认";
            case 30 -> "已驳回";
            default -> "未知";
        };
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    private String buildSuccessHtml(String message, Long contractId) {
        String detailLink = "";
        if (contractId != null) {
            String baseUrl = dingtalkNotifyApi.getApproveBaseUrl();
            if (StrUtil.isNotEmpty(baseUrl)) {
                String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
                detailLink = "<p class=\"link\"><a href=\"" + escapeHtml(base + "/crm/contract/detail/" + contractId) + "\">→ 查看合同详情</a></p>";
            }
        }
        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">" +
                "<title>操作成功</title><style>*{box-sizing:border-box}body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;margin:0;min-height:100vh;display:flex;align-items:center;justify-content:center;background:#f5f5f5;padding:16px}" +
                ".card{background:#fff;border-radius:12px;box-shadow:0 2px 12px rgba(0,0,0,.08);padding:32px;max-width:400px;text-align:center}" +
                ".icon{font-size:56px;line-height:1;color:#52c41a;margin-bottom:16px}" +
                ".msg{font-size:16px;color:#333;line-height:1.6;margin:0}" +
                ".link{margin-top:24px;font-size:14px}.link a{color:#1677ff;text-decoration:none}</style></head><body>" +
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
