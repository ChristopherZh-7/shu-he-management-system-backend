package cn.shuhe.system.module.infra.framework.web.config;

import cn.shuhe.system.framework.swagger.config.ShuheSwaggerAutoConfiguration;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * infra 模块的 web 组件的 Configuration
 *
 * @author ShuHe
 */
@Configuration(proxyBeanMethods = false)
public class InfraWebConfiguration {

    /**
     * infra 模块的 API 分组
     */
    @Bean
    public GroupedOpenApi infraGroupedOpenApi() {
        return ShuheSwaggerAutoConfiguration.buildGroupedOpenApi("infra");
    }

}
