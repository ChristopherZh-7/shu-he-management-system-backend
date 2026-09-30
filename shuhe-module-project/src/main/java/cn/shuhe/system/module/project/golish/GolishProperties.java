package cn.shuhe.system.module.project.golish;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.net.URI;

@Data
@Component
@ConfigurationProperties(prefix = "shuhe.golish")
public class GolishProperties {
    private boolean enabled;
    private String baseUrl = "http://127.0.0.1:8112";
    private String apiKey = "";
    private String callbackSecret = "";
    private String storageDirectory = "./data/golish";

    @PostConstruct
    public void validate() {
        if (!enabled) return;
        URI uri = URI.create(baseUrl);
        boolean loopback = "127.0.0.1".equals(uri.getHost()) || "localhost".equals(uri.getHost()) || "[::1]".equals(uri.getHost());
        if (!("https".equals(uri.getScheme()) || (loopback && "http".equals(uri.getScheme())))
                || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null
                || !(uri.getPath().isEmpty() || "/".equals(uri.getPath()))) {
            throw new IllegalStateException("shuhe.golish.base-url must be an HTTPS origin (HTTP only on loopback)");
        }
        if (apiKey.length() < 32 || callbackSecret.length() < 32) {
            throw new IllegalStateException("Golish integration requires two private secrets of at least 32 characters");
        }
        baseUrl = baseUrl.replaceAll("/+$", "");
    }
}
