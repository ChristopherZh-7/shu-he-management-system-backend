package cn.shuhe.system.module.crm.framework.web.config;

import cn.shuhe.system.framework.swagger.config.ShuheSwaggerAutoConfiguration;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * crm 模块的 web 组件的 Configuration
 *
 * @author ShuHe
 */
@Configuration(proxyBeanMethods = false)
public class CrmWebConfiguration {

    /**
     * crm 模块的 API 分组
     */
    @Bean
    public GroupedOpenApi crmGroupedOpenApi() {
        return ShuheSwaggerAutoConfiguration.buildGroupedOpenApi("crm");
    }

}
