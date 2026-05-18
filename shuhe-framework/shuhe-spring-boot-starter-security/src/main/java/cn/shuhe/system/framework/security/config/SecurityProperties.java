package cn.shuhe.system.framework.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;

@ConfigurationProperties(prefix = "shuhe.security")
@Validated
@Data
public class SecurityProperties {

    /**
     * HTTP 请求时，访问令牌的请求 Header
     */
    @NotEmpty(message = "Token Header 不能为空")
    private String tokenHeader = "Authorization";
    /**
     * HTTP 请求时，访问令牌的请求参数
     *
     * 初始目的：解决 WebSocket 无法通过 header 传参，只能通过 token 参数拼接
     */
    @NotEmpty(message = "Token Parameter 不能为空")
    private String tokenParameter = "token";

    /**
     * mock 模式的开关
     */
    @NotNull(message = "mock 模式的开关不能为空")
    private Boolean mockEnable = false;
    /**
     * mock 模式的密钥
     * 一定要配置密钥，保证安全性
     */
    @NotEmpty(message = "mock 模式的密钥不能为空") // 这里设置了一个默认值，因为实际上只有 mockEnable 为 true 时才需要配置。
    private String mockSecret = "test";

    /**
     * 是否开放账号密码登录（关闭后 /system/auth/login 将拒绝，仅保留钉钉等社交登录）
     */
    private Boolean passwordLoginEnabled = true;

    /**
     * 是否允许已登录用户调用「切换为管理员账号」接口（需同时配置 {@link #switchAdminAllowedUserIds} 白名单）
     */
    private Boolean switchAdminEnabled = false;

    /**
     * 允许发起切换的用户 id 白名单（通常为已绑定钉钉的运营账号）；未包含的 id 调用接口会拒绝
     */
    private List<Long> switchAdminAllowedUserIds = Collections.emptyList();

    /**
     * 切换目标管理员登录名（默认 admin）
     */
    private String switchAdminTargetUsername = "admin";

    /**
     * 是否开启开发环境一键登录功能（仅 local / dev profile 启用，prod 永远禁用）
     *
     * 与 {@link #switchAdminEnabled} 的区别：
     * - switchAdminEnabled 只允许切到 1 个固定 admin、需要 admin 密码
     * - devLoginAsEnabled 支持切到任意 userId、无需密码（开发用）
     */
    private Boolean devLoginAsEnabled = false;

    /**
     * 允许调用「开发环境一键登录」的发起方 user id 白名单
     * 空列表 = 任何已登录用户都能调（仍受 devLoginAsEnabled 总开关约束）
     */
    private List<Long> devLoginAsAllowedUserIds = Collections.emptyList();

    /**
     * 「开发环境一键登录」头像下拉里展示的常用账号清单
     * username 必填，label 可选（用于前端展示更友好的名字）
     */
    private List<DevLoginAsPresetUser> devLoginAsPresetUsers = Collections.emptyList();

    /**
     * 「开发环境一键登录」常用账号配置项
     */
    @Data
    public static class DevLoginAsPresetUser {
        /**
         * 目标用户登录名（必须与 system_users.username 一致）
         */
        @NotEmpty(message = "username 不能为空")
        private String username;
        /**
         * 展示名（前端下拉里显示，缺省回退 username）
         */
        private String label;
    }

    /**
     * 免登录的 URL 列表
     */
    private List<String> permitAllUrls = Collections.emptyList();

    /**
     * PasswordEncoder 加密复杂度，越高开销越大
     */
    private Integer passwordEncoderLength = 4;
}
