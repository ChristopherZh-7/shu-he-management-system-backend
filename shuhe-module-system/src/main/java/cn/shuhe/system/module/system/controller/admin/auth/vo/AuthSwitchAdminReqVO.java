package cn.shuhe.system.module.system.controller.admin.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

@Schema(description = "管理后台 - 已登录用户切换为管理员：须验证配置中的目标管理员账号密码")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthSwitchAdminReqVO {

    @Schema(description = "管理员登录名（须与配置 shuhe.security.switch-admin-target-username 一致）", requiredMode = Schema.RequiredMode.REQUIRED, example = "admin")
    @NotEmpty(message = "管理员账号不能为空")
    @Length(min = 4, max = 16, message = "账号长度为 4-16 位")
    @Pattern(regexp = "^[A-Za-z0-9]+$", message = "账号格式为数字以及字母")
    private String username;

    @Schema(description = "管理员密码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "密码不能为空")
    @Length(min = 4, max = 32, message = "密码长度为 4-32 位")
    private String password;
}
