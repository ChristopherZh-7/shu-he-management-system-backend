package cn.shuhe.system.module.system.service.dingtalkconfig;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.shuhe.system.module.system.controller.admin.auth.vo.AuthDingtalkJsapiConfigRespVO;
import cn.shuhe.system.module.system.dal.dataobject.dingtalkconfig.DingtalkConfigDO;
import cn.shuhe.system.module.system.dal.mysql.dingtalkconfig.DingtalkConfigMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.shuhe.system.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.shuhe.system.module.system.enums.ErrorCodeConstants.AUTH_DINGTALK_INAPP_TOKEN_FAIL;
import static cn.shuhe.system.module.system.enums.ErrorCodeConstants.AUTH_DINGTALK_JSAPI_CONFIG_FAIL;

/**
 * 钉钉客户端内打开 H5 时的免登：JSAPI 签名 + authCode 换用户身份（unionId）
 */
@Service
@Slf4j
public class DingtalkInAppAuthService {

    private static final String DINGTALK_GET_USER_INFO_URL = "https://oapi.dingtalk.com/user/getuserinfo";
    private static final String DINGTALK_GET_USER_URL = "https://oapi.dingtalk.com/topapi/v2/user/get";

    @Resource
    private DingtalkConfigMapper dingtalkConfigMapper;
    @Resource
    private DingtalkApiService dingtalkApiService;

    /**
     * 取一条启用的钉钉配置（与通讯录同步、扫码共用同一应用）
     */
    public DingtalkConfigDO requireEnabledConfig() {
        List<DingtalkConfigDO> list = dingtalkConfigMapper.selectListByStatus(0);
        if (CollUtil.isEmpty(list)) {
            throw exception(AUTH_DINGTALK_JSAPI_CONFIG_FAIL, "未找到启用的钉钉配置");
        }
        return list.get(0);
    }

    /**
     * 供前端 {@code dd.config} 使用的签名参数
     *
     * @param url 当前页完整 URL（不含 hash），需与前端传入一致
     */
    public AuthDingtalkJsapiConfigRespVO buildJsapiConfig(String url) {
        DingtalkConfigDO config = requireEnabledConfig();
        if (StrUtil.hasBlank(config.getCorpId(), config.getAgentId(), config.getClientId(), config.getClientSecret())) {
            throw exception(AUTH_DINGTALK_JSAPI_CONFIG_FAIL, "钉钉配置不完整（corpId/agentId/clientId/clientSecret）");
        }
        String accessToken = dingtalkApiService.getAccessToken(config);
        String ticket = dingtalkApiService.getJsapiTicket(accessToken);
        String nonceStr = RandomUtil.randomString(16);
        String timeStamp = String.valueOf(System.currentTimeMillis() / 1000);
        String normalized = normalizeJsapiUrl(url);
        String plain = "jsapi_ticket=" + ticket + "&noncestr=" + nonceStr + "&timestamp=" + timeStamp + "&url=" + normalized;
        String signature = DigestUtil.sha1Hex(plain);
        log.debug("DingTalk jsapi sign url={}", normalized);

        AuthDingtalkJsapiConfigRespVO vo = new AuthDingtalkJsapiConfigRespVO();
        vo.setCorpId(config.getCorpId());
        vo.setAgentId(config.getAgentId());
        vo.setNonceStr(nonceStr);
        vo.setTimeStamp(timeStamp);
        vo.setSignature(signature);
        return vo;
    }

    private String normalizeJsapiUrl(String url) {
        if (StrUtil.isBlank(url)) {
            throw exception(AUTH_DINGTALK_JSAPI_CONFIG_FAIL, "url 不能为空");
        }
        String u = url.trim();
        try {
            u = URLDecoder.decode(u, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            // 保持原样
        }
        int hash = u.indexOf('#');
        if (hash >= 0) {
            u = u.substring(0, hash);
        }
        return u;
    }

    /**
     * 用 JSAPI 临时码（{@code dd.runtime.permission.requestAuthCode}）换取当前用户 unionId。
     * <p>
     * 旧式 JSAPI 临时码须走旧版接口链路：
     * ① {@code GET /user/getuserinfo?access_token=&code=} 换 userid；
     * ② {@code POST /topapi/v2/user/get} 换 unionid。
     * 新版 {@code /v1.0/oauth2/userAccessToken} 仅接受 OAuth2 授权码，与 JSAPI 临时码不兼容。
     */
    public String exchangeAuthCodeForUnionId(String authCode) {
        if (StrUtil.isBlank(authCode)) {
            throw exception(AUTH_DINGTALK_INAPP_TOKEN_FAIL, "authCode 为空");
        }
        DingtalkConfigDO config = requireEnabledConfig();

        String accessToken = dingtalkApiService.getAccessToken(config);

        // ① JSAPI 临时码 → userid
        String getUserinfoUrl = DINGTALK_GET_USER_INFO_URL + "?access_token=" + accessToken + "&code=" + authCode;
        String getUserinfoResp = HttpUtil.get(getUserinfoUrl);
        JSONObject infoJson = JSONUtil.parseObj(getUserinfoResp);
        int errcode = infoJson.getInt("errcode", -1);
        if (errcode != 0) {
            log.warn("DingTalk getuserinfo 失败: errcode={}, errmsg={}, resp={}",
                    errcode, infoJson.getStr("errmsg"), getUserinfoResp);
            throw exception(AUTH_DINGTALK_INAPP_TOKEN_FAIL, infoJson.getStr("errmsg", "临时授权码换票失败"));
        }
        String userid = infoJson.getStr("userid");
        if (StrUtil.isEmpty(userid)) {
            log.warn("DingTalk getuserinfo 响应缺少 userid: {}", getUserinfoResp);
            throw exception(AUTH_DINGTALK_INAPP_TOKEN_FAIL, "无法获取 userid");
        }

        // ② userid → unionid
        Map<String, Object> body = new HashMap<>();
        body.put("userid", userid);
        String getUserUrl = DINGTALK_GET_USER_URL + "?access_token=" + accessToken;
        String getUserResp = HttpRequest.post(getUserUrl)
                .header("Content-Type", "application/json")
                .body(JSONUtil.toJsonStr(body))
                .execute().body();
        JSONObject userJson = JSONUtil.parseObj(getUserResp);
        int errcode2 = userJson.getInt("errcode", -1);
        if (errcode2 != 0) {
            log.warn("DingTalk topapi/v2/user/get 失败: errcode={}, errmsg={}",
                    errcode2, userJson.getStr("errmsg"));
            throw exception(AUTH_DINGTALK_INAPP_TOKEN_FAIL, userJson.getStr("errmsg", "获取用户信息失败"));
        }
        JSONObject result = userJson.getJSONObject("result");
        String unionId = result != null ? result.getStr("unionid") : null;
        if (StrUtil.isEmpty(unionId)) {
            log.warn("DingTalk topapi/v2/user/get 响应缺少 unionid: {}", getUserResp);
            throw exception(AUTH_DINGTALK_INAPP_TOKEN_FAIL, "无法解析 unionId");
        }
        return unionId;
    }
}
