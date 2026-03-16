package cn.shuhe.system.module.system.api.oauth2;

import cn.shuhe.system.framework.common.biz.system.oauth2.OAuth2TokenCommonApi;
import cn.shuhe.system.framework.common.util.object.BeanUtils;
import cn.shuhe.system.framework.common.biz.system.oauth2.dto.OAuth2AccessTokenCheckRespDTO;
import cn.shuhe.system.framework.common.biz.system.oauth2.dto.OAuth2AccessTokenCreateReqDTO;
import cn.shuhe.system.framework.common.biz.system.oauth2.dto.OAuth2AccessTokenRespDTO;
import cn.shuhe.system.framework.common.enums.UserTypeEnum;
import cn.shuhe.system.framework.security.core.LoginUser;
import cn.shuhe.system.module.system.dal.dataobject.oauth2.OAuth2AccessTokenDO;
import cn.shuhe.system.module.system.dal.dataobject.user.AdminUserDO;
import cn.shuhe.system.module.system.service.oauth2.OAuth2TokenService;
import cn.shuhe.system.module.system.service.user.AdminUserService;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * OAuth2.0 Token API 实现类
 *
 * @author ShuHe
 */
@Service
public class OAuth2TokenApiImpl implements OAuth2TokenCommonApi {

    @Resource
    private OAuth2TokenService oauth2TokenService;
    @Resource
    private AdminUserService adminUserService;

    @Override
    public OAuth2AccessTokenRespDTO createAccessToken(OAuth2AccessTokenCreateReqDTO reqDTO) {
        OAuth2AccessTokenDO accessTokenDO = oauth2TokenService.createAccessToken(
                reqDTO.getUserId(), reqDTO.getUserType(), reqDTO.getClientId(), reqDTO.getScopes());
        return BeanUtils.toBean(accessTokenDO, OAuth2AccessTokenRespDTO.class);
    }

    @Override
    public OAuth2AccessTokenCheckRespDTO checkAccessToken(String accessToken) {
        OAuth2AccessTokenDO accessTokenDO = oauth2TokenService.checkAccessToken(accessToken);
        OAuth2AccessTokenCheckRespDTO resp = BeanUtils.toBean(accessTokenDO, OAuth2AccessTokenCheckRespDTO.class);
        // 管理端用户：补充 passwordMustChange，用于后端强制修改密码校验
        if (resp != null && UserTypeEnum.ADMIN.getValue().equals(resp.getUserType()) && resp.getUserId() != null) {
            AdminUserDO user = adminUserService.getUser(resp.getUserId());
            if (user != null) {
                Map<String, String> userInfo = resp.getUserInfo() != null
                        ? new HashMap<>(resp.getUserInfo()) : new HashMap<>();
                userInfo.put(LoginUser.INFO_KEY_PASSWORD_MUST_CHANGE,
                        Integer.valueOf(1).equals(user.getPasswordMustChange()) ? "1" : "0");
                resp.setUserInfo(userInfo);
            }
        }
        return resp;
    }

    @Override
    public OAuth2AccessTokenRespDTO removeAccessToken(String accessToken) {
        OAuth2AccessTokenDO accessTokenDO = oauth2TokenService.removeAccessToken(accessToken);
        return BeanUtils.toBean(accessTokenDO, OAuth2AccessTokenRespDTO.class);
    }

    @Override
    public OAuth2AccessTokenRespDTO refreshAccessToken(String refreshToken, String clientId) {
        OAuth2AccessTokenDO accessTokenDO = oauth2TokenService.refreshAccessToken(refreshToken, clientId);
        return BeanUtils.toBean(accessTokenDO, OAuth2AccessTokenRespDTO.class);
    }

}
