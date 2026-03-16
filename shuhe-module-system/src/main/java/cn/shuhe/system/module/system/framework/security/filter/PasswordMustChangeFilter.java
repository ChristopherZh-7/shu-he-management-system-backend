package cn.shuhe.system.module.system.framework.security.filter;

import cn.shuhe.system.framework.common.exception.ServiceException;
import cn.shuhe.system.framework.common.pojo.CommonResult;
import cn.shuhe.system.framework.common.util.servlet.ServletUtils;
import cn.shuhe.system.framework.security.core.LoginUser;
import cn.shuhe.system.framework.security.core.util.SecurityFrameworkUtils;
import cn.shuhe.system.framework.web.core.handler.GlobalExceptionHandler;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

import static cn.shuhe.system.module.system.enums.ErrorCodeConstants.AUTH_PASSWORD_MUST_CHANGE;

/**
 * 首次登录需强制修改密码校验过滤器
 * 当用户 password_must_change=1 时，仅允许访问：get-permission-info、force-update-password、logout
 *
 * @author ShuHe
 */
@RequiredArgsConstructor
public class PasswordMustChangeFilter extends OncePerRequestFilter {

    private static final Set<String> ALLOWED_PATH_PATTERNS = Set.of(
            "system/auth/get-permission-info",
            "system/user/profile/force-update-password",
            "system/auth/logout"
    );

    private final GlobalExceptionHandler globalExceptionHandler;

    @Override
    @SuppressWarnings("NullableProblems")
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        LoginUser loginUser = SecurityFrameworkUtils.getLoginUser();
        if (loginUser == null || loginUser.getInfo() == null) {
            chain.doFilter(request, response);
            return;
        }
        String passwordMustChange = loginUser.getInfo().get(LoginUser.INFO_KEY_PASSWORD_MUST_CHANGE);
        if (!"1".equals(passwordMustChange)) {
            chain.doFilter(request, response);
            return;
        }
        String path = request.getRequestURI();
        if (path != null && ALLOWED_PATH_PATTERNS.stream().anyMatch(path::contains)) {
            chain.doFilter(request, response);
            return;
        }
        CommonResult<?> result = globalExceptionHandler.allExceptionHandler(request,
                new ServiceException(AUTH_PASSWORD_MUST_CHANGE));
        ServletUtils.writeJSON(response, result);
    }
}
