package cn.shuhe.system.module.project.service.golish;

import cn.shuhe.system.module.project.dal.dataobject.ProjectDO;
import cn.shuhe.system.module.project.dal.dataobject.ServiceItemDO;
import cn.shuhe.system.module.project.dal.mysql.ProjectMapper;
import cn.shuhe.system.module.project.dal.mysql.ServiceItemMapper;
import cn.shuhe.system.module.ticket.framework.event.TicketAcceptedEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GolishApprovalService {
    private final GolishProperties properties;
    private final GolishRepository repository;
    private final ProjectMapper projectMapper;
    private final ServiceItemMapper serviceItemMapper;
    private final ObjectMapper json;

    /** Only called after the authenticated department approver confirms the frozen scope. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueueApproved(TicketAcceptedEvent event, Long serviceItemId, Long roundId) {
        JsonNode request = json.valueToTree(event.getExtJson() == null ? null : event.getExtJson().get("golish"));
        if (!request.path("enabled").asBoolean(false)) return;
        require(properties.isEnabled(), "GolishAI 自动测试尚未启用");
        require(Boolean.TRUE.equals(event.getGolishAuthorizationApproved()), "请核对目标、授权文件和有效期，并明确批准自动测试");
        require(roundId != null && event.getAcceptedBy() != null, "缺少审批人或执行轮次");
        ServiceItemDO service = serviceItemMapper.selectById(serviceItemId);
        require(service != null && "penetration_test".equals(service.getServiceType()), "自动测试仅支持渗透测试服务项");
        ProjectDO project = projectMapper.selectById(service.getProjectId());
        require(project != null && service.getCustomerId() != null, "服务项缺少关联项目或客户");
        Instant now = Instant.now();
        Instant from = Instant.parse(request.path("validFrom").asText());
        Instant until = Instant.parse(request.path("validUntil").asText());
        require(from.isBefore(until) && until.isAfter(now), "授权有效期无效或已经到期");

        JsonNode targets = request.path("targets");
        require(targets.isArray() && targets.size() >= 1 && targets.size() <= 50, "请填写 1–50 个授权网站");
        var sites = new HashSet<String>();
        List<Map<String, Object>> targetList = new ArrayList<>();
        for (JsonNode target : targets) {
            String site = normalizeSite(target.path("url").asText());
            require(sites.add(site), "同一网站不能重复填写");
            targetList.add(Map.of("id", "target-" + (targetList.size()+1), "name", text(target.path("name").asText()), "url", site, "boundary", "site"));
        }
        JsonNode files = request.path("documentIds");
        require(files.isArray() && files.size() >= 1 && files.size() <= 10, "请上传 1–10 份授权文件");
        JsonNode displayed = request.path("documents");
        require(displayed.isArray() && displayed.size() == files.size(), "审批页面需展示所有授权文件");
        var seen = new HashSet<String>();
        List<Map<String, Object>> documents = new ArrayList<>();
        for (JsonNode file : files) {
            require(seen.add(file.asText()), "授权文件重复");
            GolishRepository.Document doc = repository.document(file.asText());
            require(doc.ownerId() == event.getCreatorId(), "授权文件必须由本工单申请人上传");
            JsonNode reviewed = displayed.get(documents.size());
            require(doc.fileId().equals(reviewed.path("fileId").asText()) && doc.name().equals(reviewed.path("name").asText())
                    && doc.sha256().equals(reviewed.path("sha256").asText()), "审批展示的授权文件信息与原件不一致");
            documents.add(Map.of("file_id", doc.fileId(), "name", doc.name(), "sha256", doc.sha256()));
        }
        String externalId = "ticket-"+event.getTicketId()+":round-"+roundId;
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("external_id", externalId);
        body.put("title", text(event.getTitle()));
        body.put("project", reference(project.getId(), project.getName()));
        body.put("service_item", reference(service.getId(), service.getCode() == null ? "渗透测试服务项" : service.getCode()));
        body.put("service_type", "penetration_test");
        body.put("round_id", roundId.toString());
        body.put("customer", reference(service.getCustomerId(), service.getCustomerName()));
        body.put("requester", reference(event.getCreatorId(), event.getCreatorName()));
        body.put("targets", targetList);
        body.put("authorization", Map.of("valid_from", from.toString(), "valid_until", until.toString(), "documents", documents));
        body.put("approval", Map.of("status", "approved", "id", "ticket-"+event.getTicketId()+":accept", "approver_id", event.getAcceptedBy().toString(), "approved_at", now.toString()));
        try { repository.enqueue(event.getTicketId(), externalId, json.writeValueAsString(body)); }
        catch (com.fasterxml.jackson.core.JsonProcessingException e) { throw new IllegalStateException("无法保存审批快照", e); }
    }

    static Map<String, String> reference(Long id, String name) {
        require(id != null, "缺少业务编号");
        return Map.of("id", id.toString(), "name", text(name));
    }
    static String text(String value) {
        require(value != null && !value.isBlank() && value.getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= 500
                && !value.contains("\n") && !value.contains("\r") && !value.contains("\0"), "名称不能为空、过长或包含控制字符");
        return value;
    }
    static void require(boolean ok, String message) { if (!ok) throw new IllegalArgumentException(message); }

    static String normalizeSite(String raw) {
        URI u = URI.create(raw);
        String scheme = u.getScheme() == null ? "" : u.getScheme().toLowerCase(Locale.ROOT);
        String host = u.getHost();
        require((scheme.equals("https") || scheme.equals("http")) && host != null && u.getUserInfo() == null
                && u.getQuery() == null && u.getFragment() == null && (u.getRawPath().isEmpty() || u.getRawPath().equals("/"))
                && (u.getPort() == -1 || u.getPort() > 0 && u.getPort() <= 65535), "目标必须是完整的网站地址（协议、主机、可选端口），授权覆盖整个网站");
        int port = u.getPort();
        String suffix = port == -1 || scheme.equals("https") && port == 443 || scheme.equals("http") && port == 80 ? "" : ":"+port;
        return scheme+"://"+host.toLowerCase(Locale.ROOT)+suffix+"/";
    }
}
