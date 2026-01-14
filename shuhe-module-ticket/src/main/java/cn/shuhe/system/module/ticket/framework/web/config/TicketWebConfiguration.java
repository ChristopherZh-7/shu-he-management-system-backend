package cn.shuhe.system.module.ticket.framework.web.config;

import cn.shuhe.system.framework.swagger.config.ShuheSwaggerAutoConfiguration;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ticket 模块的 web 组件的 Configuration
 *
 * @author shuhe
 */
@Configuration(proxyBeanMethods = false)
public class TicketWebConfiguration {

    /**
     * ticket 模块的 API 分组
     */
    @Bean
    public GroupedOpenApi ticketGroupedOpenApi() {
        return ShuheSwaggerAutoConfiguration.buildGroupedOpenApi("ticket");
    }

}
