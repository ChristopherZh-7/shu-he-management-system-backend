package cn.shuhe.system.framework.security.config;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;

/**
 * 安全过滤器自定义器，用于在 Security 过滤链中追加自定义过滤器
 *
 * @author ShuHe
 */
@FunctionalInterface
public interface SecurityFilterCustomizer {

    /**
     * 自定义过滤链，可添加额外过滤器
     *
     * @param httpSecurity HttpSecurity
     */
    void customize(HttpSecurity httpSecurity) throws Exception;
}
