package cn.shuhe.system.module.system.framework.dingtalk;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * 解析本机首选 IP，用于钉钉回调/审批链接等未配置 baseUrl 时的动态回退。
 * 优先级：58.x.x.x > 10.x.x.x
 *
 * @author ShuHe
 */
@Component
@Slf4j
public class LocalAddressResolver {

    /** 未配置时使用的端口。nginx 部署时用户通过 80 访问，默认 80；直连后端则填 48080 */
    @Value("${shuhe.dingtalk.auto-base-url-port:80}")
    private int port;

    private volatile String cachedPreferredIp;

    /**
     * 获取首选 IP（58 优先，其次 10），排除 loopback
     */
    public String getPreferredIp() {
        if (cachedPreferredIp != null) {
            return cachedPreferredIp;
        }
        synchronized (this) {
            if (cachedPreferredIp != null) {
                return cachedPreferredIp;
            }
            List<String> ip58 = new ArrayList<>();
            List<String> ip10 = new ArrayList<>();
            try {
                Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                while (interfaces.hasMoreElements()) {
                    NetworkInterface ni = interfaces.nextElement();
                    if (ni.isLoopback() || !ni.isUp()) continue;
                    Enumeration<InetAddress> addresses = ni.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress addr = addresses.nextElement();
                        if (!(addr instanceof Inet4Address)) continue;
                        if (addr.isLoopbackAddress()) continue;
                        String host = addr.getHostAddress();
                        if (host == null) continue;
                        if (host.startsWith("58.")) ip58.add(host);
                        else if (host.startsWith("10.")) ip10.add(host);
                    }
                }
            } catch (Exception e) {
                log.warn("[LocalAddressResolver] 获取本机 IP 失败", e);
            }
            String chosen = ip58.isEmpty() ? (ip10.isEmpty() ? null : ip10.get(0)) : ip58.get(0);
            if (chosen != null) {
                cachedPreferredIp = chosen;
                log.info("[LocalAddressResolver] 首选 IP: {} (58优先，其次10)", chosen);
            }
            return cachedPreferredIp;
        }
    }

    /**
     * 构建 baseUrl：http://{preferredIp} 或 http://{preferredIp}:{port}
     * 端口 80 时省略，其他端口显式写出。若无法解析 IP 则返回 null
     */
    public String buildAutoBaseUrl() {
        String ip = getPreferredIp();
        if (StrUtil.isEmpty(ip)) return null;
        return port == 80 ? "http://" + ip : "http://" + ip + ":" + port;
    }
}
