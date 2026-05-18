package cn.shuhe.system.module.system.controller.admin.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理后台 - 开发环境一键登录请求
 *
 * 与 {@link AuthSwitchAdminReqVO} 的区别：
 * - 无密码（开发用便利）
 * - 支持任意 userId（不限定 admin）
 * - 仅 local / dev profile 启用，prod 强制禁用
 */
@Schema(description = "管理后台 - 开发环境一键切换登录用户：仅 local/dev profile 启用，须在白名单内")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthDevLoginAsReqVO {

    @Schema(description = "目标用户 id（system_users.id）", requiredMode = Schema.RequiredMode.REQUIRED, example = "249")
    @NotNull(message = "目标用户 id 不能为空")
    private Long targetUserId;
}
