package cn.shuhe.system.module.project.controller.admin;

import cn.shuhe.system.framework.common.pojo.CommonResult;
import cn.shuhe.system.framework.security.core.util.SecurityFrameworkUtils;
import cn.shuhe.system.module.project.service.golish.GolishProperties;
import cn.shuhe.system.module.project.service.golish.GolishRepository;
import cn.shuhe.system.module.ticket.service.TicketService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

import static cn.shuhe.system.framework.common.pojo.CommonResult.success;

@RestController
@RequestMapping("/project/golish")
@RequiredArgsConstructor
public class GolishIntegrationController {
    private final GolishProperties properties;
    private final GolishRepository repository;
    private final TicketService tickets;
    private final ObjectMapper json;

    @GetMapping("/capabilities")
    @PreAuthorize("@ss.hasPermission('ticket:ticket:query')")
    public CommonResult<Map<String, Object>> capabilities() {
        return success(Map.of("enabled", properties.isEnabled(), "serviceType", "penetration_test", "maxDocumentBytes", 8<<20));
    }

    @PostMapping("/documents")
    @PreAuthorize("@ss.hasPermission('ticket:ticket:create')")
    public CommonResult<Map<String, String>> upload(@RequestParam("file") MultipartFile file) throws Exception {
        if (!properties.isEnabled() || file.isEmpty() || file.getSize() > 8<<20) throw new IllegalArgumentException("请先启用自动测试，并上传非空且不超过 8 MiB 的授权文件");
        String name = file.getOriginalFilename();
        if (name == null || name.isBlank() || name.length() > 160 || name.contains("/") || name.contains("\\") || name.chars().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("授权文件名无效");
        byte[] content = file.getBytes();
        String id = UUID.randomUUID().toString();
        String sha = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        repository.saveDocument(new GolishRepository.Document(id, SecurityFrameworkUtils.getLoginUserId(), name, sha, content));
        return success(Map.of("fileId", id, "name", name, "sha256", sha));
    }

    @GetMapping("/documents/{id}")
    @PreAuthorize("@ss.hasPermission('ticket:ticket:query')")
    public ResponseEntity<byte[]> download(@PathVariable String id, @RequestParam(required=false) Long ticketId) {
        var doc = repository.document(id);
        boolean allowed = doc.ownerId() == SecurityFrameworkUtils.getLoginUserId();
        if (ticketId != null) {
            var ticket = tickets.validateTicketAccess(ticketId, SecurityFrameworkUtils.getLoginUserId());
            JsonNode request = json.valueToTree(ticket.getExtJson());
            allowed = false;
            if (java.util.Objects.equals(ticket.getCreatorId(), doc.ownerId())) {
                for (JsonNode file : request.path("golish").path("documentIds")) if (file.asText().equals(id)) allowed = true;
            }
        }
        if (!allowed) throw new org.springframework.security.access.AccessDeniedException("无权访问此授权文件");
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(doc.name(), StandardCharsets.UTF_8).build().toString())
                .body(doc.content());
    }

    @GetMapping("/tickets/{ticketId}")
    @PreAuthorize("@ss.hasPermission('ticket:ticket:query')")
    public CommonResult<Map<String, Object>> status(@PathVariable long ticketId) throws Exception {
        tickets.validateTicketAccess(ticketId, SecurityFrameworkUtils.getLoginUserId());
        if (!properties.isEnabled()) return success(Map.of("status", "disabled"));
        var row = repository.delivery(ticketId);
        if (row == null) return success(Map.of("status", "awaiting_approval"));
        return success(Map.of("status", row.status(), "reason", row.reason(), "attempts", row.attempts(), "updatedAt", row.updatedAt(),
                "execution", row.receipt() == null ? json.createObjectNode() : json.readTree(row.receipt())));
    }

    @PostMapping("/tickets/{ticketId}/retry")
    @PreAuthorize("@ss.hasPermission('ticket:ticket:accept')")
    public CommonResult<Boolean> retry(@PathVariable long ticketId) {
        var ticket = tickets.validateTicketAccess(ticketId, SecurityFrameworkUtils.getLoginUserId());
        if (!java.util.Objects.equals(ticket.getAssigneeId(), SecurityFrameworkUtils.getLoginUserId()))
            throw new org.springframework.security.access.AccessDeniedException("由原审批人重试投递");
        var row = repository.delivery(ticketId);
        if (row != null && row.status().equals("blocked")) repository.finish(row, "pending", "", row.receipt(), System.currentTimeMillis());
        return success(true);
    }
}
