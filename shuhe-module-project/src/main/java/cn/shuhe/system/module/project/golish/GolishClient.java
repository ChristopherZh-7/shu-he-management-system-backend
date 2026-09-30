package cn.shuhe.system.module.project.golish;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.*;

@Component
@RequiredArgsConstructor
public class GolishClient {
    private static final int MAX_BYTES = 128 * 1024 * 1024;
    private final GolishProperties properties;
    private final ObjectMapper json;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER).build();

    public JsonNode submit(String body, String key) throws IOException, InterruptedException {
        return json.readTree(request("POST", "/jobs", body, key, 2 * 1024 * 1024));
    }
    public JsonNode job(String id) throws IOException, InterruptedException {
        return json.readTree(request("GET", path(id), null, null, 2 * 1024 * 1024));
    }
    public byte[] download(String id, String kind) throws IOException, InterruptedException {
        if (!"result".equals(kind) && !"report".equals(kind)) throw new IllegalArgumentException("Invalid artifact kind");
        return request("GET", path(id) + "/" + kind, null, null, MAX_BYTES);
    }
    public void command(String id, String command) throws IOException, InterruptedException {
        if (!java.util.Set.of("resume", "report/retry", "cancel").contains(command)) throw new IllegalArgumentException("Invalid command");
        request("POST", path(id) + "/" + command, "{}", null, 1024 * 1024);
    }
    private String path(String id) {
        if (id == null || !id.matches("[A-Za-z0-9-]{1,100}")) throw new IllegalArgumentException("Invalid Golish job ID");
        return "/jobs/" + id;
    }
    private byte[] request(String method, String path, String body, String key, int limit) throws IOException, InterruptedException {
        if (!properties.isEnabled()) throw new IOException("Golish integration disabled");
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(properties.getBaseUrl() + "/golish/integration/v1" + path))
                .timeout(Duration.ofSeconds(40)).header("Authorization", "Bearer " + properties.getApiKey())
                .header("Content-Type", "application/json");
        if (key != null) request.header("Idempotency-Key", key);
        request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        var pending = http.sendAsync(request.build(), info -> new BoundedBody(limit));
        try {
            // Bound the entire body transfer, not just the response headers, below the worker lease.
            HttpResponse<byte[]> response = pending.get(40, TimeUnit.SECONDS);
            if (response.statusCode() < 200 || response.statusCode() >= 300) throw new IOException("Golish HTTP " + response.statusCode());
            return response.body();
        } catch (TimeoutException ex) {
            pending.cancel(true);
            throw new IOException("Golish response timed out", ex);
        } catch (ExecutionException ex) {
            throw new IOException("Golish response failed", ex.getCause());
        } catch (InterruptedException ex) {
            pending.cancel(true);
            throw ex;
        }
    }

    static final class BoundedBody implements HttpResponse.BodySubscriber<byte[]> {
        private final int limit;
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        private final CompletableFuture<byte[]> result = new CompletableFuture<>();
        private Flow.Subscription subscription;
        BoundedBody(int limit) { this.limit = limit; }
        public CompletionStage<byte[]> getBody() { return result; }
        public void onSubscribe(Flow.Subscription value) { subscription = value; value.request(1); }
        public void onNext(List<ByteBuffer> chunks) {
            for (ByteBuffer chunk : chunks) {
                if (chunk.remaining() > limit - bytes.size()) {
                    subscription.cancel();
                    result.completeExceptionally(new IOException("Golish artifact exceeds size limit"));
                    return;
                }
                byte[] part = new byte[chunk.remaining()]; chunk.get(part); bytes.writeBytes(part);
            }
            subscription.request(1);
        }
        public void onError(Throwable error) { result.completeExceptionally(error); }
        public void onComplete() { result.complete(bytes.toByteArray()); }
    }
}
