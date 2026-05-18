package cn.shuhe.system.module.system.service.auth;

import cn.shuhe.system.module.system.controller.admin.auth.vo.*;
import cn.shuhe.system.module.system.dal.dataobject.user.AdminUserDO;

import jakarta.validation.Valid;

/**
 * 管理后台的认证 Service 接口
 *
 * 提供用户的登录、登出的能力
 *
 * @author ShuHe
 */
public interface AdminAuthService {

    /**
     * 验证账号 + 密码。如果通过，则返回用户
     *
     * @param username 账号
     * @param password 密码
     * @return 用户
     */
    AdminUserDO authenticate(String username, String password);

    /**
     * 账号登录
     *
     * @param reqVO 登录信息
     * @return 登录结果
     */
    AuthLoginRespVO login(@Valid AuthLoginReqVO reqVO);

    /**
     * 基于 token 退出登录
     *
     * @param token token
     * @param logType 登出类型
     */
    void logout(String token, Integer logType);

    /**
     * 短信验证码发送
     *
     * @param reqVO 发送请求
     */
    void sendSmsCode(AuthSmsSendReqVO reqVO);

    /**
     * 短信登录
     *
     * @param reqVO 登录信息
     * @return 登录结果
     */
    AuthLoginRespVO smsLogin(AuthSmsLoginReqVO reqVO);

    /**
     * 社交快捷登录，使用 code 授权码
     *
     * @param reqVO 登录信息
     * @return 登录结果
     */
    AuthLoginRespVO socialLogin(@Valid AuthSocialLoginReqVO reqVO);

    /**
     * 刷新访问令牌
     *
     * @param refreshToken 刷新令牌
     * @return 登录结果
     */
    AuthLoginRespVO refreshToken(String refreshToken);

    /**
     * 用户注册
     *
     * @param createReqVO 注册用户
     * @return 注册结果
     */
    AuthLoginRespVO register(AuthRegisterReqVO createReqVO);

    /**
     * 重置密码
     *
     * @param reqVO 验证码信息
     */
    void resetPassword(AuthResetPasswordReqVO reqVO);

    /**
     * 钉钉内打开页面时的 JSAPI 签名（dd.config）
     *
     * @param url 当前页 URL（不含 hash）
     */
    AuthDingtalkJsapiConfigRespVO getDingtalkJsapiConfig(String url);

    /**
     * 钉钉端内免登：JSAPI authCode 换票后按 unionId 与扫码一致绑定关系登录
     */
    AuthLoginRespVO dingtalkInAppLogin(@Valid AuthDingtalkInAppLoginReqVO reqVO);

    /**
     * 已登录用户切换为管理员：需验证配置中目标管理员的账号密码；返回新 token，原 token 作废
     *
     * @param sourceUserId 当前登录用户 id
     * @param accessToken  当前访问令牌（将失效）
     * @param reqVO        管理员账号、密码（用户名须与配置的 switch-admin-target-username 一致）
     */
    AuthLoginRespVO switchToAdmin(Long sourceUserId, String accessToken, @Valid AuthSwitchAdminReqVO reqVO);

    /**
     * 开发环境一键登录·切换到任意目标用户
     *
     * 仅当 shuhe.security.dev-login-as-enabled=true 时启用（local/dev profile 配置开启，prod 默认关闭）。
     * 与 {@link #switchToAdmin} 的区别：无需密码、可切到任意 userId。
     *
     * @param sourceUserId 当前登录用户 id
     * @param accessToken  当前访问令牌（将失效）
     * @param reqVO        目标用户 id
     * @return 新 token + 目标用户信息
     */
    AuthLoginRespVO devLoginAs(Long sourceUserId, String accessToken, @Valid AuthDevLoginAsReqVO reqVO);

    /**
     * 开发环境一键登录·获取「常用账号清单」
     *
     * 后端从 {@code shuhe.security.dev-login-as-preset-users} 读配置，
     * enrich userId + nickname 后返回。未启用功能时返回空列表（前端据此决定是否展示按钮）。
     *
     * @return 常用账号清单，未启用 / 配置为空时返回空 List
     */
    java.util.List<AuthDevLoginAsPresetUserRespVO> getDevLoginAsPresetUsers();

}
