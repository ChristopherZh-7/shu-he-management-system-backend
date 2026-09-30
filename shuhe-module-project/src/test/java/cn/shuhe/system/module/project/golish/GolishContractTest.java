package cn.shuhe.system.module.project.golish;

import cn.shuhe.system.module.project.controller.admin.GolishIntegrationController;
import cn.shuhe.system.module.project.dal.dataobject.GolishJobDO;
import cn.shuhe.system.module.project.dal.mysql.GolishJobMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Flow;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GolishContractTest {
    @Test void scopeRejectsWideningAndNormalizesDuplicateOrigins() {
        assertEquals("https://example.test/", GolishScanSpec.origin("HTTPS://EXAMPLE.TEST:443/"));
        for (String url : List.of("https://example.test/admin", "https://example.test/?x=1", "https://*.example.test/",
                "https://u:p@example.test/", "http://example.test:0/", "file:///tmp/x", "10.0.0.0/8")) {
            assertThrows(RuntimeException.class, () -> GolishScanSpec.origin(url), url);
        }
        Map<String,Object> spec = new HashMap<>(Map.of("enabled", true, "targets", List.of(
                Map.of("name", "one", "url", "https://example.test/"), Map.of("name", "two", "url", "https://example.test:443/"))));
        assertThrows(RuntimeException.class, () -> GolishScanSpec.parse("service_launch", Map.of("golish",spec)));
        spec.put("targets",List.of(Map.of("name","one","url","https://example.test/")));
        assertNotNull(GolishScanSpec.parse("service_launch",Map.of("golish",spec)));
        assertThrows(RuntimeException.class, () -> GolishScanSpec.parse("general",Map.of("golish",spec)));
        spec.put("exclusions",List.of("/private"));
        assertThrows(RuntimeException.class, () -> GolishScanSpec.parse("service_launch",Map.of("golish",spec)));
    }

    @Test void initialReportNeverCertifiesUnretestedFindingsAndLatestRoundWins() {
        GolishJobDO initial = job("initial", "completed", "{}", "[{\"id\":1},{\"id\":2}]");
        assertFalse(GolishIntegrationService.readyForReview(List.of(initial)));
        GolishJobDO first = retest("[1]", "[{\"id\":1,\"retest_result\":{\"status\":\"fixed\"}}]");
        assertFalse(GolishIntegrationService.readyForReview(List.of(initial, first)));
        GolishJobDO second = retest("[2]", "[{\"id\":2,\"retest_result\":{\"status\":\"fixed\"}}]");
        assertTrue(GolishIntegrationService.readyForReview(List.of(initial,first,second)));
        GolishJobDO incomplete = retest("[1,2]", "[{\"id\":1,\"retest_result\":{\"status\":\"fixed\"}}]");
        assertFalse(GolishIntegrationService.readyForReview(List.of(initial,first,second,incomplete)));
        GolishJobDO regression = retest("[1]", "[{\"id\":1,\"retest_result\":{\"status\":\"not_fixed\"}}]");
        assertFalse(GolishIntegrationService.readyForReview(List.of(initial,first,second,regression)));
        initial.setState("completed_with_gaps");
        assertFalse(GolishIntegrationService.readyForReview(List.of(initial,first,second)));
    }

    @Test void signedWebhookRejectsMutationExpiryAndReturnsRealHttpFailure() throws Exception {
        String secret="s".repeat(32), timestamp=Long.toString(Instant.now().getEpochSecond());
        byte[] body="{\"id\":\"event-1\"}".getBytes(StandardCharsets.UTF_8);
        Mac mac=Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));
        mac.update((timestamp+".").getBytes(StandardCharsets.UTF_8));
        String signature="sha256="+HexFormat.of().formatHex(mac.doFinal(body));
        assertTrue(GolishCallbackService.validSignature(secret,timestamp,signature,body,Long.parseLong(timestamp)));
        assertFalse(GolishCallbackService.validSignature(secret,timestamp,signature,"{}".getBytes(),Long.parseLong(timestamp)));
        assertFalse(GolishCallbackService.validSignature(secret,timestamp,signature,body,Long.parseLong(timestamp)+301));
        GolishCallbackService callbacks=mock(GolishCallbackService.class);
        var controller=new GolishIntegrationController(null,callbacks,null,null);
        doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED)).when(callbacks).receive(null,null,null,body);
        assertEquals(401,controller.callback(null,null,null,body).getStatusCode().value());
        doThrow(new IllegalStateException("database unavailable")).when(callbacks).receive(null,null,null,body);
        assertEquals(500,controller.callback(null,null,null,body).getStatusCode().value());
    }

    @Test void duplicateCallbackOnlyWakesPollingOnceWithoutApplyingReportedState() throws Exception {
        GolishProperties properties=new GolishProperties(); properties.setEnabled(true); properties.setCallbackSecret("s".repeat(32));
        GolishJobMapper jobs=mock(GolishJobMapper.class); JdbcTemplate jdbc=mock(JdbcTemplate.class);
        GolishJobDO job=new GolishJobDO();job.setId(9L);job.setRemoteId("remote-1");
        when(jobs.selectOne(any())).thenReturn(job);
        when(jdbc.update(eq("INSERT IGNORE INTO project_golish_event(event_id,job_id) VALUES (?,?)"),eq("event-1"),eq(9L))).thenReturn(1,0);
        var service=new GolishCallbackService(properties,jobs,jdbc,new ObjectMapper());
        byte[] body="{\"id\":\"event-1\",\"job\":{\"id\":\"remote-1\",\"external_id\":\"key\",\"state\":\"completed\"}}".getBytes();
        String timestamp=Long.toString(Instant.now().getEpochSecond());
        Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(properties.getCallbackSecret().getBytes(),"HmacSHA256"));
        mac.update((timestamp+".").getBytes());String signature="sha256="+HexFormat.of().formatHex(mac.doFinal(body));
        service.receive("event-1",timestamp,signature,body);service.receive("event-1",timestamp,signature,body);
        verify(jdbc,times(1)).update("UPDATE project_golish_job SET next_poll = NOW() WHERE id = ? AND imported = 0",9L);
        verify(jdbc,times(2)).update("INSERT IGNORE INTO project_golish_event(event_id,job_id) VALUES (?,?)","event-1",9L);
        verifyNoMoreInteractions(jdbc);
    }

    @Test void downloadCancelsBeforeAllocatingBeyondLimit() {
        var body=new GolishClient.BoundedBody(4);Flow.Subscription subscription=mock(Flow.Subscription.class);
        body.onSubscribe(subscription);body.onNext(List.of(ByteBuffer.wrap(new byte[]{1,2,3})));
        body.onNext(List.of(ByteBuffer.wrap(new byte[]{4,5})));
        assertTrue(body.getBody().toCompletableFuture().isCompletedExceptionally());verify(subscription).cancel();
    }

    private static GolishJobDO retest(String ids,String findings) { return job("retest","completed","{\"retest\":{\"finding_ids\":"+ids+"}}",findings); }
    private static GolishJobDO job(String kind,String state,String request,String findings) {
        GolishJobDO job=new GolishJobDO();job.setKind(kind);job.setState(state);job.setImported(true);job.setRequestJson(request);
        job.setResultJson("{\"snapshot\":{\"findings\":"+findings+"}}");return job;
    }
}
