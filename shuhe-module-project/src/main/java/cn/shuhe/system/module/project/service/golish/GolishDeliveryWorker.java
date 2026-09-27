package cn.shuhe.system.module.project.service.golish;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class GolishDeliveryWorker {
    private final GolishProperties properties;
    private final GolishRepository repository;
    private final ObjectMapper json;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER).build();

    @Scheduled(fixedDelayString = "${shuhe.golish.poll-ms:5000}")
    public void run() {
        if (!properties.isEnabled()) return;
        try { deliverOne(); }
        catch (Exception e) { log.warn("GolishAI 投递队列暂不可用，检查配置和数据库迁移 ({})", e.getClass().getSimpleName()); }
    }

    public void deliverOne() throws Exception {
        validateConnection();
        var row = repository.claim(System.currentTimeMillis());
        if (row == null) return;
        String receipt = row.receipt();
        String state = row.status();
        try {
            String path = "/work-orders/"+row.externalId();
            if (state.equals("pending")) {
                JsonNode capabilities = request("GET", "/capabilities", null, false);
                if (!capabilities.path("automatic_execution").asBoolean()) throw new DeliveryException("automatic_execution_disabled", false);
                request("POST", "/work-orders", row.payload().getBytes(StandardCharsets.UTF_8), false);
                JsonNode payload = json.readTree(row.payload());
                for (JsonNode expected : payload.path("authorization").path("documents")) {
                    var doc = repository.document(expected.path("file_id").asText());
                    if (!doc.sha256().equals(expected.path("sha256").asText())) throw new DeliveryException("authorization_document_changed", true);
                    request("PUT", path+"/authorization-documents/"+doc.fileId(), doc.content(), true);
                }
                // Also enroll receipts that were prepared before automatic
                // execution was enabled. The start endpoint is idempotent.
                request("POST", path+"/start", "{}".getBytes(StandardCharsets.UTF_8), false);
                state = "submitted";
            }
            JsonNode status = request("GET", path, null, false);
            if (!row.externalId().equals(status.path("data").path("work_order").path("external_id").asText()))
                throw new DeliveryException("receipt_identity_mismatch", true);
            receipt = json.writeValueAsString(status.path("data"));
            if (status.path("data").path("execution_status").asText().equals("completed")) state = "completed";
            repository.finish(row, state, "", receipt, System.currentTimeMillis()+15_000);
        } catch (Exception e) {
            boolean permanent = e instanceof DeliveryException failure && failure.permanent;
            String reason = e instanceof DeliveryException failure ? failure.code : "connection_failed";
            long delay = Math.min(300_000L, 5_000L << Math.min(row.attempts(), 6));
            repository.finish(row, permanent ? "blocked" : state, reason, receipt, System.currentTimeMillis()+delay);
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
        }
    }

    private void validateConnection() {
        URI uri = URI.create(properties.getBaseUrl() == null ? "" : properties.getBaseUrl());
        GolishApprovalService.require(("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
                && uri.getHost() != null && uri.getUserInfo() == null && uri.getQuery() == null && uri.getFragment() == null
                && (uri.getPath().isEmpty() || uri.getPath().equals("/")), "配置固定 GolishAI 服务地址");
        GolishApprovalService.require(properties.getToken() != null && properties.getToken().length() >= 32
                && properties.getToken().chars().noneMatch(Character::isWhitespace), "配置独立 GolishAI 服务密钥");
        GolishApprovalService.require(properties.getClientId().matches("[A-Za-z0-9._:-]{1,128}"), "配置 GolishAI 客户端编号");
    }

    private JsonNode request(String method, String path, byte[] content, boolean binary) throws Exception {
        String base = properties.getBaseUrl().replaceAll("/+$", "");
        HttpRequest request = HttpRequest.newBuilder(URI.create(base+"/golish/api/integrations/v1"+path))
                .timeout(Duration.ofSeconds(8)).header("X-Integration-Client", properties.getClientId())
                .header("Authorization", "Bearer "+properties.getToken())
                .header("Content-Type", binary ? "application/octet-stream" : "application/json")
                .method(method, content == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofByteArray(content)).build();
        HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            int code = response.statusCode();
            throw new DeliveryException("golish_http_"+code, code >= 400 && code < 500 && code != 429);
        }
        byte[] body = response.body();
        if (body.length > 1<<20) throw new DeliveryException("response_too_large", true);
        return json.readTree(body);
    }

    private static class DeliveryException extends Exception {
        final String code;
        final boolean permanent;
        DeliveryException(String code, boolean permanent) { super(code); this.code = code; this.permanent = permanent; }
    }
}
