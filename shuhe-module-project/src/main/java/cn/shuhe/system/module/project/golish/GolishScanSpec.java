package cn.shuhe.system.module.project.golish;

import cn.shuhe.system.framework.common.exception.ServiceException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import java.net.URI;
import java.util.*;

/** The submitted origins are the approval scope; paths are never widened. */
@Data
public class GolishScanSpec {
    private boolean enabled;
    private List<Target> targets = new ArrayList<>();
    private String requirements = "";
    private int timeLimitMinutes = 30;

    @Data
    public static class Target {
        private String name;
        private String url;
    }

    public static ServiceException invalid(String message) {
        return new ServiceException(1_040_050_001, message);
    }

    public static GolishScanSpec parse(String businessType, Map<String, Object> ext) {
        if (ext == null || ext.get("golish") == null) return null;
        GolishScanSpec spec;
        try {
            spec = new ObjectMapper().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .convertValue(ext.get("golish"), GolishScanSpec.class);
        } catch (IllegalArgumentException ex) {
            throw invalid("自动测试参数不完整或包含不支持的范围字段");
        }
        if (!spec.enabled) return null;
        if (!"service_launch".equals(businessType)) throw invalid("自动安全测试只适用于服务派遣工单");
        if (spec.targets == null || spec.targets.isEmpty() || spec.targets.size() > 50) throw invalid("请填写 1–50 个测试站点");
        if (spec.timeLimitMinutes < 1 || spec.timeLimitMinutes > 10080) throw invalid("每个站点的测试时长须为 1–10080 分钟");
        if (spec.requirements == null || spec.requirements.length() > 8000) throw invalid("测试要求不能超过 8000 字");
        Set<String> origins = new HashSet<>();
        for (Target target : spec.targets) {
            if (target == null || target.name == null || target.name.isBlank() || target.name.length() > 400) throw invalid("请填写站点名称");
            String origin = origin(target.url);
            if (!origins.add(origin)) throw invalid("测试站点不能重复");
            target.url = origin;
        }
        return spec;
    }

    static String origin(String input) {
        try {
            URI uri = URI.create(Objects.requireNonNull(input).trim());
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null || uri.getUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null
                    || !(uri.getRawPath().isEmpty() || "/".equals(uri.getRawPath())) || uri.getPort() == 0 || uri.getPort() > 65535) {
                throw new IllegalArgumentException();
            }
            int port = uri.getPort();
            if (("http".equalsIgnoreCase(uri.getScheme()) && port == 80) || ("https".equalsIgnoreCase(uri.getScheme()) && port == 443)) port = -1;
            return new URI(uri.getScheme().toLowerCase(Locale.ROOT), null, uri.getHost().toLowerCase(Locale.ROOT), port, "/", null, null).toASCIIString();
        } catch (Exception ex) {
            throw invalid("目标须为完整 HTTP/HTTPS 站点根地址，不支持路径、账号、通配符、排除范围或 CIDR");
        }
    }
}
