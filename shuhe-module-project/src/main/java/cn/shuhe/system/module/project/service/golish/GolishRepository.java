package cn.shuhe.system.module.project.service.golish;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/** Uses the business datasource/transaction; never calls Golish in an approval transaction. */
@Repository
@RequiredArgsConstructor
public class GolishRepository {
    private final JdbcTemplate jdbc;

    public record Document(String fileId, long ownerId, String name, String sha256, byte[] content) {}
    public record Delivery(long ticketId, String externalId, String payload, String status,
                           String reason, String receipt, int attempts, String leaseToken, long updatedAt) {}

    public void saveDocument(Document doc) {
        jdbc.update("INSERT INTO shuhe_golish_document(id,owner_id,name,sha256,content,created_at) VALUES(?,?,?,?,?,?)",
                doc.fileId(), doc.ownerId(), doc.name(), doc.sha256(), doc.content(), System.currentTimeMillis());
    }

    public Document document(String id) {
        return jdbc.query("SELECT * FROM shuhe_golish_document WHERE id=?", (r, n) ->
                new Document(r.getString("id"), r.getLong("owner_id"), r.getString("name"), r.getString("sha256"), r.getBytes("content")), id)
                .stream().findFirst().orElseThrow(() -> new IllegalArgumentException("授权文件不存在"));
    }

    public void enqueue(long ticketId, String externalId, String payload) {
        long now = System.currentTimeMillis();
        jdbc.update("INSERT INTO shuhe_golish_delivery(ticket_id,external_id,payload,status,next_attempt_at,created_at,updated_at) VALUES(?,?,?,'pending',?,?,?)",
                ticketId, externalId, payload, now, now, now);
    }

    public Delivery delivery(long id) {
        return rows("SELECT * FROM shuhe_golish_delivery WHERE ticket_id=?", id).stream().findFirst().orElse(null);
    }

    public Delivery claim(long now) {
        List<Delivery> candidates = rows("SELECT * FROM shuhe_golish_delivery WHERE next_attempt_at<=? AND lease_until<=? AND status IN ('pending','submitted') ORDER BY next_attempt_at,ticket_id LIMIT 1", now, now);
        if (candidates.isEmpty()) return null;
        Delivery row = candidates.get(0);
        String token = UUID.randomUUID().toString();
        int changed = jdbc.update("UPDATE shuhe_golish_delivery SET lease_until=?,lease_token=? WHERE ticket_id=? AND lease_until<=? AND status IN ('pending','submitted')",
                now + 120_000, token, row.ticketId(), now);
        return changed == 1 ? delivery(row.ticketId()) : null;
    }

    public void finish(Delivery row, String status, String reason, String receipt, long nextAt) {
        jdbc.update("UPDATE shuhe_golish_delivery SET status=?,reason=?,receipt=?,attempts=attempts+1,next_attempt_at=?,lease_until=0,updated_at=? WHERE ticket_id=? AND lease_token=?",
                status, reason, receipt, nextAt, System.currentTimeMillis(), row.ticketId(), row.leaseToken());
    }

    private List<Delivery> rows(String query, Object... args) {
        return jdbc.query(query, (r, n) -> new Delivery(r.getLong("ticket_id"), r.getString("external_id"), r.getString("payload"),
                r.getString("status"), r.getString("reason"), r.getString("receipt"), r.getInt("attempts"), r.getString("lease_token"), r.getLong("updated_at")), args);
    }
}
