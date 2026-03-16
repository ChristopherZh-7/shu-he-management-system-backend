package cn.shuhe.system.module.system.framework.security.config;

import cn.shuhe.system.framework.security.config.SecurityFilterCustomizer;
import cn.shuhe.system.framework.security.core.filter.TokenAuthenticationFilter;
import cn.shuhe.system.module.system.framework.security.filter.PasswordMustChangeFilter;
import cn.shuhe.system.framework.web.core.handler.GlobalExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import jakarta.annotation.Resource;

/**
 * System 模块安全配置：首次登录强制修改密码校验
 *
 * @author ShuHe
 */
@Configuration
public class SecurityConfiguration {

    @Resource
    private GlobalExceptionHandler globalExceptionHandler;

    @Bean
    public SecurityFilterCustomizer passwordMustChangeFilterCustomizer() {
        return httpSecurity -> {
            PasswordMustChangeFilter filter = new PasswordMustChangeFilter(globalExceptionHandler);
            httpSecurity.addFilterAfter(filter, TokenAuthenticationFilter.class);
        };
    }
}
