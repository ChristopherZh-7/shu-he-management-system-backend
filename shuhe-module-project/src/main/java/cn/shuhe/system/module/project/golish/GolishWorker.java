package cn.shuhe.system.module.project.golish;

import cn.shuhe.system.framework.datapermission.core.annotation.DataPermission;
import cn.shuhe.system.module.project.dal.dataobject.GolishJobDO;
import cn.shuhe.system.module.project.dal.mysql.GolishJobMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.io.ByteArrayInputStream;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.zip.ZipInputStream;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "shuhe.golish", name = "enabled", havingValue = "true")
public class GolishWorker {
    private final GolishJobMapper jobs;
    private final GolishClient client;
    private final GolishIntegrationService service;
    private final GolishProperties properties;
    private final ObjectMapper json;

    @Scheduled(fixedDelayString = "${shuhe.golish.poll-delay-ms:5000}")
    @DataPermission(enable = false)
    public void tick() {
        for (GolishJobDO pending : jobs.pending()) {
            String token = UUID.randomUUID().toString();
            if (jobs.claim(pending.getId(), token) != 1) continue;
            GolishJobDO job = jobs.selectById(pending.getId());
            int delay = 5;
            try {
                synchronize(job);
            } catch (Exception ex) {
                if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
                delay = 30;
                // Never log request bodies, target data, credentials, or upstream response bodies.
                log.warn("Golish synchronization will retry job={} error={}", job.getId(), ex.getClass().getSimpleName());
                jobs.update(null, new LambdaUpdateWrapper<GolishJobDO>().eq(GolishJobDO::getId, job.getId())
                        .eq(GolishJobDO::getLeaseToken, token).set(GolishJobDO::getError, "服务同步暂未完成，系统将重试；请检查 Golish 服务与网络"));
            } finally {
                jobs.release(job.getId(), token, delay);
            }
        }
    }

    private void synchronize(GolishJobDO job) throws Exception {
        JsonNode remote;
        if (job.getRemoteId() == null || job.getRemoteId().isBlank()) {
            remote = client.submit(job.getRequestJson(), job.getRequestKey());
            if (!job.getRequestKey().equals(remote.path("external_id").asText()) || remote.path("id").asText().isBlank()) {
                throw new IllegalStateException("Golish admission identity mismatch");
            }
            job.setRemoteId(remote.path("id").asText());
            // The exact request and key remain unchanged when an acknowledgement is lost.
            jobs.update(null, new LambdaUpdateWrapper<GolishJobDO>().eq(GolishJobDO::getId, job.getId())
                    .eq(GolishJobDO::getLeaseToken, job.getLeaseToken()).set(GolishJobDO::getRemoteId, job.getRemoteId()));
        } else remote = client.job(job.getRemoteId());
        if (!job.getRemoteId().equals(remote.path("id").asText()) || !job.getRequestKey().equals(remote.path("external_id").asText())) {
            throw new IllegalStateException("Golish status identity mismatch");
        }
        job.setState(remote.path("state").asText());
        String detail = remote.path("error").asText("");
        job.setError(detail.substring(0, Math.min(detail.length(), 950)));
        jobs.update(null, new LambdaUpdateWrapper<GolishJobDO>().eq(GolishJobDO::getId, job.getId())
                .eq(GolishJobDO::getLeaseToken, job.getLeaseToken()).set(GolishJobDO::getState, job.getState())
                .set(GolishJobDO::getError, job.getError()).set(GolishJobDO::getUpdateTime, LocalDateTime.now()));
        if (!remote.path("report_ready").asBoolean() || !("completed".equals(job.getState()) || "completed_with_gaps".equals(job.getState()))) return;
        JsonNode result = json.readTree(client.download(job.getRemoteId(), "result"));
        if (!job.getRemoteId().equals(result.path("job").path("id").asText())
                || !job.getRequestKey().equals(result.path("job").path("external_id").asText())
                || !result.path("snapshot").path("findings").isArray()) throw new IllegalStateException("Golish report identity mismatch");
        byte[] report = client.download(job.getRemoteId(), "report");
        boolean isDocx = false;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(report))) {
            for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                if ("word/document.xml".equals(entry.getName())) { isDocx = true; break; }
            }
        }
        if (!isDocx) throw new IllegalStateException("Invalid Word report");
        Path folder = Path.of(properties.getStorageDirectory()).toAbsolutePath().resolve(job.getId().toString());
        Files.createDirectories(folder);
        Path temporary = Files.createTempFile(folder, "report-", ".tmp");
        Path destination = folder.resolve("report.docx");
        try {
            Files.write(temporary, report);
            Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temporary); }
        service.importResult(job, result, destination.toString());
    }
}
