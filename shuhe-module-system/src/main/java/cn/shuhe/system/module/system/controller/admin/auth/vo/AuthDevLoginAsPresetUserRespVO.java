package cn.shuhe.system.module.system.controller.admin.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理后台 - 开发环境一键登录·常用账号清单（单条）
 *
 * 后端从 application-local.yaml / application-dev.yaml 的
 * shuhe.security.dev-login-as-preset-users 配置项读取，
 * 并 enrich userId + nickname 后返回给前端用于下拉展示。
 */
@Schema(description = "管理后台 - 开发环境一键登录·常用账号清单条目")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthDevLoginAsPresetUserRespVO {

    @Schema(description = "用户 id", example = "249")
    private Long userId;

    @Schema(description = "用户登录名", example = "zhengyi")
    private String username;

    @Schema(description = "用户昵称（system_users.nickname）", example = "郑屹")
    private String nickname;

    @Schema(description = "前端展示用 label（配置项填的 label，缺省回退 nickname / username）", example = "郑屹（安全服务主管）")
    private String label;
}
