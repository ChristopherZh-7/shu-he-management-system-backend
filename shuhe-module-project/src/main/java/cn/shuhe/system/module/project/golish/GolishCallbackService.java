package cn.shuhe.system.module.project.golish;

import cn.shuhe.system.module.project.dal.dataobject.GolishJobDO;
import cn.shuhe.system.module.project.dal.mysql.GolishJobMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class GolishCallbackService {
    private final GolishProperties properties;
    private final GolishJobMapper jobs;
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    @Transactional(rollbackFor = Exception.class)
    public void receive(String eventId, String timestamp, String signature, byte[] body) {
        if (!properties.isEnabled() || !validSignature(properties.getCallbackSecret(), timestamp, signature, body, Instant.now().getEpochSecond())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Golish signature");
        }
        try {
            JsonNode event = json.readTree(body);
            if (eventId == null || eventId.length() > 100 || !eventId.equals(event.path("id").asText())) throw new IllegalArgumentException();
            JsonNode remote = event.path("job");
            GolishJobDO job = jobs.selectOne(new LambdaQueryWrapper<GolishJobDO>().eq(GolishJobDO::getRequestKey, remote.path("external_id").asText()));
            if (job == null || (job.getRemoteId() != null && !job.getRemoteId().equals(remote.path("id").asText()))) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown Golish job");
            }
            int inserted = jdbc.update("INSERT IGNORE INTO project_golish_event(event_id,job_id) VALUES (?,?)", eventId, job.getId());
            // Notifications may be duplicated or out of order. Fetch current state; never apply callback state directly.
            if (inserted == 1) jdbc.update("UPDATE project_golish_job SET next_poll = NOW() WHERE id = ? AND imported = 0", job.getId());
        } catch (ResponseStatusException ex) { throw ex;
        } catch (java.io.IOException | IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Golish event");
        }
    }

    static boolean validSignature(String secret, String timestamp, String signature, byte[] body, long now) {
        try {
            if (secret.length() < 32 || body.length > 256 * 1024 || timestamp == null || !timestamp.matches("[0-9]{1,12}")
                    || signature == null || !signature.matches("sha256=[0-9a-f]{64}") || Math.abs(now - Long.parseLong(timestamp)) > 300) return false;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            mac.update((timestamp + ".").getBytes(StandardCharsets.UTF_8));
            return MessageDigest.isEqual(mac.doFinal(body), HexFormat.of().parseHex(signature.substring(7)));
        } catch (Exception ex) { return false; }
    }
}
