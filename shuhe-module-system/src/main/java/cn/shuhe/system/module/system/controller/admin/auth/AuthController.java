package cn.shuhe.system.module.system.controller.admin.auth;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.shuhe.system.framework.common.enums.CommonStatusEnum;
import cn.shuhe.system.framework.common.enums.UserTypeEnum;
import cn.shuhe.system.framework.common.pojo.CommonResult;
import cn.shuhe.system.framework.security.config.SecurityProperties;
import cn.shuhe.system.framework.security.core.util.SecurityFrameworkUtils;
import cn.shuhe.system.module.system.controller.admin.auth.vo.*;
import cn.shuhe.system.module.system.convert.auth.AuthConvert;
import cn.shuhe.system.module.system.dal.dataobject.permission.MenuDO;
import cn.shuhe.system.module.system.dal.dataobject.permission.RoleDO;
import cn.shuhe.system.module.system.dal.dataobject.user.AdminUserDO;
import cn.shuhe.system.module.system.enums.logger.LoginLogTypeEnum;
import cn.shuhe.system.module.system.service.auth.AdminAuthService;
import cn.shuhe.system.module.system.service.permission.MenuService;
import cn.shuhe.system.module.system.service.permission.PermissionService;
import cn.shuhe.system.module.system.service.permission.RoleService;
import cn.shuhe.system.module.system.service.social.SocialClientService;
import cn.shuhe.system.module.system.service.user.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static cn.shuhe.system.module.system.enums.ErrorCodeConstants.AUTH_PASSWORD_LOGIN_DISABLED;

import static cn.shuhe.system.framework.common.pojo.CommonResult.success;
import static cn.shuhe.system.framework.common.pojo.CommonResult.error;
import static cn.shuhe.system.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.shuhe.system.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - 认证")
@RestController
@RequestMapping("/system/auth")
@Validated
@Slf4j
public class AuthController {

    @Resource
    private AdminAuthService authService;
    @Resource
    private AdminUserService userService;
    @Resource
    private RoleService roleService;
    @Resource
    private MenuService menuService;
    @Resource
    private PermissionService permissionService;
    @Resource
    private SocialClientService socialClientService;

    @Resource
    private SecurityProperties securityProperties;

    @PostMapping("/login")
    @PermitAll
    @Operation(summary = "使用账号密码登录")
    public CommonResult<AuthLoginRespVO> login(@RequestBody @Valid AuthLoginReqVO reqVO) {
        if (Boolean.FALSE.equals(securityProperties.getPasswordLoginEnabled())) {
            return error(AUTH_PASSWORD_LOGIN_DISABLED);
        }
        return success(authService.login(reqVO));
    }

    @PostMapping("/logout")
    @PermitAll
    @Operation(summary = "登出系统")
    public CommonResult<Boolean> logout(HttpServletRequest request) {
        String token = SecurityFrameworkUtils.obtainAuthorization(request,
                securityProperties.getTokenHeader(), securityProperties.getTokenParameter());
        if (StrUtil.isNotBlank(token)) {
            authService.logout(token, LoginLogTypeEnum.LOGOUT_SELF.getType());
        }
        return success(true);
    }

    @PostMapping("/refresh-token")
    @PermitAll
    @Operation(summary = "刷新令牌")
    @Parameter(name = "refreshToken", description = "刷新令牌", required = true)
    public CommonResult<AuthLoginRespVO> refreshToken(@RequestParam("refreshToken") String refreshToken) {
        return success(authService.refreshToken(refreshToken));
    }

    @GetMapping("/get-permission-info")
    @Operation(summary = "获取登录用户的权限信息")
    public CommonResult<AuthPermissionInfoRespVO> getPermissionInfo() {
        // 1.1 获得用户信息
        AdminUserDO user = userService.getUser(getLoginUserId());
        if (user == null) {
            return success(null);
        }

        // 1.2 获得角色列表
        Set<Long> roleIds = permissionService.getUserRoleIdListByUserId(getLoginUserId());
        if (CollUtil.isEmpty(roleIds)) {
            return success(AuthConvert.INSTANCE.convert(user, Collections.emptyList(), Collections.emptyList()));
        }
        List<RoleDO> roles = roleService.getRoleList(roleIds);
        roles.removeIf(role -> !CommonStatusEnum.ENABLE.getStatus().equals(role.getStatus())); // 移除禁用的角色

        // 1.3 获得菜单列表
        Set<Long> menuIds = permissionService.getRoleMenuListByRoleId(convertSet(roles, RoleDO::getId));
        List<MenuDO> menuList = menuService.getMenuList(menuIds);
        menuList = menuService.filterDisableMenus(menuList);

        // 2. 拼接结果返回
        return success(AuthConvert.INSTANCE.convert(user, roles, menuList));
    }

    @PostMapping("/register")
    @PermitAll
    @Operation(summary = "注册用户")
    public CommonResult<AuthLoginRespVO> register(@RequestBody @Valid AuthRegisterReqVO registerReqVO) {
        return success(authService.register(registerReqVO));
    }

    // ========== 短信登录相关 ==========

    @PostMapping("/sms-login")
    @PermitAll
    @Operation(summary = "使用短信验证码登录")
    // 可按需开启限流：https://github.com/YunaiV/ruoyi-vue-pro/issues/851
    // @RateLimiter(time = 60, count = 6, keyResolver = ExpressionRateLimiterKeyResolver.class, keyArg = "#reqVO.mobile")
    public CommonResult<AuthLoginRespVO> smsLogin(@RequestBody @Valid AuthSmsLoginReqVO reqVO) {
        return success(authService.smsLogin(reqVO));
    }

    @PostMapping("/send-sms-code")
    @PermitAll
    @Operation(summary = "发送手机验证码")
    public CommonResult<Boolean> sendLoginSmsCode(@RequestBody @Valid AuthSmsSendReqVO reqVO) {
        authService.sendSmsCode(reqVO);
        return success(true);
    }

    @PostMapping("/reset-password")
    @PermitAll
    @Operation(summary = "重置密码")
    public CommonResult<Boolean> resetPassword(@RequestBody @Valid AuthResetPasswordReqVO reqVO) {
        authService.resetPassword(reqVO);
        return success(true);
    }

    // ========== 社交登录相关 ==========

    @GetMapping("/social-auth-redirect")
    @PermitAll
    @Operation(summary = "社交授权的跳转")
    @Parameters({
            @Parameter(name = "type", description = "社交类型", required = true),
            @Parameter(name = "redirectUri", description = "回调路径")
    })
    public CommonResult<String> socialLogin(@RequestParam("type") Integer type,
                                            @RequestParam("redirectUri") String redirectUri) {
        return success(socialClientService.getAuthorizeUrl(
                type, UserTypeEnum.ADMIN.getValue(), redirectUri));
    }

    @PostMapping("/social-login")
    @PermitAll
    @Operation(summary = "社交快捷登录，使用 code 授权码", description = "适合未登录的用户，但是社交账号已绑定用户")
    public CommonResult<AuthLoginRespVO> socialQuickLogin(@RequestBody @Valid AuthSocialLoginReqVO reqVO) {
        return success(authService.socialLogin(reqVO));
    }

    @GetMapping("/dingtalk-jsapi-config")
    @PermitAll
    @Operation(summary = "钉钉 JSAPI 签名", description = "供钉钉内 H5 调用 dd.config，参数 url 为当前页完整地址（不含 hash）")
    @Parameter(name = "url", description = "当前页面 URL", required = true)
    public CommonResult<AuthDingtalkJsapiConfigRespVO> getDingtalkJsapiConfig(@RequestParam("url") String url) {
        return success(authService.getDingtalkJsapiConfig(url));
    }

    @PostMapping("/dingtalk-inapp-login")
    @PermitAll
    @Operation(summary = "钉钉端内免登", description = "使用 JSAPI 获取的 authCode 换票登录，需与扫码绑定同一 unionId")
    public CommonResult<AuthLoginRespVO> dingtalkInAppLogin(@RequestBody @Valid AuthDingtalkInAppLoginReqVO reqVO) {
        return success(authService.dingtalkInAppLogin(reqVO));
    }

    @PostMapping("/switch-admin")
    @Operation(summary = "切换为管理员账号",
            description = "需已登录且当前用户 id 在配置白名单内；须提交配置中目标管理员的账号与密码验证通过后，返回新 token，原 token 失效")
    public CommonResult<AuthLoginRespVO> switchToAdmin(HttpServletRequest request,
                                                       @RequestBody @Valid AuthSwitchAdminReqVO reqVO) {
        String token = SecurityFrameworkUtils.obtainAuthorization(request,
                securityProperties.getTokenHeader(), securityProperties.getTokenParameter());
        return success(authService.switchToAdmin(getLoginUserId(), token, reqVO));
    }

}
