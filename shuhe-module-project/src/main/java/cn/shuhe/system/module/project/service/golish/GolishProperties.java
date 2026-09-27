package cn.shuhe.system.module.project.service.golish;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "shuhe.golish")
public class GolishProperties {
    private boolean enabled;
    private String baseUrl;
    private String clientId = "shuhe";
    private String token;
}
