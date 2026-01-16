package cn.shuhe.system.module.project.framework.web.config;

import cn.shuhe.system.framework.common.enums.WebFilterOrderEnum;
import cn.shuhe.system.framework.web.config.WebProperties;
import cn.shuhe.system.framework.web.core.filter.DemoFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.servlet.Filter;

/**
 * project 模块的 Web 配置
 */
@Configuration(proxyBeanMethods = false)
public class ProjectWebConfiguration {

    /**
     * project 模块的 API 分组
     */
    public static final String API_GROUP_PROJECT = "project";

}
