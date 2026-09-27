package cn.shuhe.system.module.project.service.golish;

import cn.shuhe.system.framework.security.core.util.SecurityFrameworkUtils;
import cn.shuhe.system.module.project.controller.admin.GolishIntegrationController;
import cn.shuhe.system.module.project.dal.dataobject.ProjectDO;
import cn.shuhe.system.module.project.dal.dataobject.ServiceItemDO;
import cn.shuhe.system.module.project.dal.mysql.ProjectMapper;
import cn.shuhe.system.module.project.dal.mysql.ServiceItemMapper;
import cn.shuhe.system.module.ticket.framework.event.TicketAcceptedEvent;
import cn.shuhe.system.module.ticket.dal.dataobject.TicketDO;
import cn.shuhe.system.module.ticket.service.TicketService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GolishIntegrationTest {
    private JdbcTemplate jdbc;
    private GolishRepository repository;
    private GolishApprovalService approval;
    private GolishProperties properties;
    private TransactionTemplate tx;
    private final ObjectMapper json = new ObjectMapper();
    private TicketAcceptedEvent event;
    private byte[] original;

    @BeforeEach
    void setup() throws Exception {
        var ds = new DriverManagerDataSource("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(ds);
        jdbc.execute("CREATE TABLE shuhe_golish_document(id VARCHAR(36) PRIMARY KEY,owner_id BIGINT,name VARCHAR(200),sha256 VARCHAR(64),content BLOB,created_at BIGINT)");
        jdbc.execute("CREATE TABLE shuhe_golish_delivery(ticket_id BIGINT PRIMARY KEY,external_id VARCHAR(128) UNIQUE,payload CLOB,status VARCHAR(32),reason VARCHAR(200) DEFAULT '',receipt CLOB,attempts INT DEFAULT 0,next_attempt_at BIGINT,lease_until BIGINT DEFAULT 0,lease_token VARCHAR(36) DEFAULT '',created_at BIGINT,updated_at BIGINT)");
        repository = new GolishRepository(jdbc);
        tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
        properties = new GolishProperties(); properties.setEnabled(true); properties.setToken("local-test-token-01234567890123456789");
        var projects = mock(ProjectMapper.class);
        var services = mock(ServiceItemMapper.class);
        when(projects.selectById(1L)).thenReturn(ProjectDO.builder().id(1L).name("Fixture project").build());
        when(services.selectById(2L)).thenReturn(ServiceItemDO.builder().id(2L).projectId(1L).serviceType("penetration_test").code("TEST-2").customerId(3L).customerName("Fixture customer").build());
        approval = new GolishApprovalService(properties, repository, projects, services, json);
        original = "local authorization fixture".getBytes(StandardCharsets.UTF_8);
        String sha = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(original));
        repository.saveDocument(new GolishRepository.Document("file-one", 5L, "authorization.txt", sha, original));
        Map<String,Object> request = new HashMap<>(Map.of("enabled",true,"validFrom", Instant.now().minusSeconds(60).toString(),"validUntil",Instant.now().plusSeconds(3600).toString(),
                "targets", List.of(Map.of("name","Fixture site","url","https://portal.example.com/")),
                "documentIds", List.of("file-one"), "documents", List.of(Map.of("fileId","file-one","name","authorization.txt","sha256",sha))));
        event = TicketAcceptedEvent.builder().ticketId(4L).title("Fixture work order").creatorId(5L).creatorName("Requester")
                .acceptedBy(6L).golishAuthorizationApproved(true).extJson(Map.of("golish", request)).build();
    }

    @Test
    void approvalFreezesServerOwnedIdentityAndRollsBackWithBusinessTransaction() throws Exception {
        tx.execute(status -> { approval.enqueueApproved(event, 2L, 7L); status.setRollbackOnly(); return null; });
        assertNull(repository.delivery(4L));
        tx.execute(status -> { approval.enqueueApproved(event,2L,7L); return null; });
        var payload = json.readTree(repository.delivery(4L).payload());
        assertEquals("6", payload.at("/approval/approver_id").asText());
        assertEquals("5", payload.at("/requester/id").asText());
        assertEquals("3", payload.at("/customer/id").asText());
        assertEquals("ticket-4:round-7", payload.path("external_id").asText());
        assertEquals("site", payload.at("/targets/0/boundary").asText());
        assertEquals("pending", repository.delivery(4L).status());
        // Approval changes cannot overwrite the immutable delivery.
        assertThrows(Exception.class, () -> tx.execute(status -> { approval.enqueueApproved(event,2L,8L); return null; }));
        assertEquals("ticket-4:round-7", repository.delivery(4L).externalId());
    }

    @Test
    void rejectsMissingConfirmationWrongOwnerAndPathScope() {
        event.setGolishAuthorizationApproved(false);
        assertThrows(IllegalArgumentException.class, () -> approval.enqueueApproved(event,2L,7L));
        event.setGolishAuthorizationApproved(true); event.setCreatorId(999L);
        assertThrows(IllegalArgumentException.class, () -> approval.enqueueApproved(event,2L,7L));
        event.setCreatorId(5L);
        for (String url : List.of("https://portal.example.com/admin/", "https://user:pass@example.com/", "https://example.com/?q=x", "file:///etc/passwd", "https://*.example.com/"))
            assertThrows(IllegalArgumentException.class, () -> GolishApprovalService.normalizeSite(url));
        assertNull(repository.delivery(4L));
    }

    @Test
    void restartAndPartialUploadRetryKeepSameWorkOrderAndOriginalBytes() throws Exception {
        tx.execute(status -> { approval.enqueueApproved(event,2L,7L); return null; });
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        AtomicInteger creates = new AtomicInteger();
        AtomicInteger uploads = new AtomicInteger();
        String frozen = repository.delivery(4L).payload();
        server.createContext("/golish/api/integrations/v1", exchange -> {
            assertEquals(properties.getToken(), exchange.getRequestHeaders().getFirst("Authorization").substring(7));
            assertEquals("shuhe", exchange.getRequestHeaders().getFirst("X-Integration-Client"));
            int status = 200;
            String response = "{}";
            String path = exchange.getRequestURI().getPath();
            if (path.endsWith("/capabilities")) response = "{\"automatic_execution\":true}";
            else if (exchange.getRequestMethod().equals("POST") && path.endsWith("/work-orders")) {
                creates.incrementAndGet(); assertEquals(frozen,new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));
            } else if (path.endsWith("/start")) {
                assertEquals("{}",new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));
            } else if (exchange.getRequestMethod().equals("PUT")) {
                assertArrayEquals(original,exchange.getRequestBody().readAllBytes());
                if (uploads.incrementAndGet()==1) status=503;
            } else response = "{\"data\":{\"work_order\":{\"external_id\":\"ticket-4:round-7\"},\"execution_status\":\"running\",\"report_status\":\"not_generated\"}}";
            byte[] bytes=response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status,bytes.length); exchange.getResponseBody().write(bytes); exchange.close();
        });
        server.start();
        try {
            properties.setBaseUrl("http://127.0.0.1:"+server.getAddress().getPort());
            var worker = new GolishDeliveryWorker(properties, repository, json);
            worker.deliverOne();
            assertEquals("pending",repository.delivery(4L).status());
            jdbc.update("UPDATE shuhe_golish_delivery SET next_attempt_at=0");
            new GolishDeliveryWorker(properties,repository,json).deliverOne();
            assertEquals("submitted",repository.delivery(4L).status());
            assertEquals(2,creates.get()); assertEquals(2,uploads.get());
            jdbc.update("UPDATE shuhe_golish_delivery SET next_attempt_at=0");
            worker.deliverOne();
            assertEquals(2,creates.get(),"status polling must not redeliver completed intake");
            assertEquals("running",json.readTree(repository.delivery(4L).receipt()).path("execution_status").asText());
        } finally { server.stop(0); }
    }

    @Test
    void leaseExcludesOtherWorkersAndRecoversAfterCrash() {
        tx.execute(status -> { approval.enqueueApproved(event,2L,7L); return null; });
        long now=System.currentTimeMillis();
        var first=repository.claim(now);
        assertNotNull(first); assertNull(repository.claim(now));
        var next=repository.claim(now+120001);
        assertNotNull(next); assertNotEquals(first.leaseToken(), next.leaseToken());
        repository.finish(first,"completed","",null,now);
        assertEquals("pending",repository.delivery(4L).status(),"expired worker cannot overwrite new lease");
    }

    @Test
    void documentDownloadRequiresOwnerAndDeclaredTicketAssociation() {
        var tickets=mock(TicketService.class);
        var ticket=new TicketDO();
        ticket.setCreatorId(999L);
        ticket.setExtJson(Map.of("golish",Map.of("documentIds",List.of("file-one"))));
        when(tickets.validateTicketAccess(4L,6L)).thenReturn(ticket);
        var controller=new GolishIntegrationController(properties,repository,tickets,json);
        try (var security=mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(6L);
            assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> controller.download("file-one",4L),
                    "declaring another requester's file in an accessible ticket must not grant access");
            ticket.setCreatorId(5L);
            assertArrayEquals(original,controller.download("file-one",4L).getBody());
            ticket.setExtJson(Map.of());
            assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> controller.download("file-one",4L));
        }
    }

    @Test
    void liveGolishContractWhenConfigured() throws Exception {
        String ready = System.getProperty("golish.interop.readyFile");
        org.junit.jupiter.api.Assumptions.assumeTrue(ready != null, "optional local Go/Java test");
        var connection = json.readTree(java.nio.file.Files.readString(java.nio.file.Path.of(ready)));
        properties.setBaseUrl(connection.path("url").asText());
        properties.setToken(connection.path("token").asText());
        tx.execute(status -> { approval.enqueueApproved(event,2L,7L); return null; });
        var worker = new GolishDeliveryWorker(properties,repository,json);
        for (int attempt=0; attempt<150; attempt++) {
            jdbc.update("UPDATE shuhe_golish_delivery SET next_attempt_at=0");
            worker.deliverOne();
            var row=repository.delivery(4L);
            if (row.receipt()!=null && json.readTree(row.receipt()).path("execution_status").asText().equals("running")) {
                assertEquals("submitted",row.status());
                // Repeat intake/start is safe even across independent sender processes.
                jdbc.update("UPDATE shuhe_golish_delivery SET status='pending',next_attempt_at=0");
                new GolishDeliveryWorker(properties,repository,json).deliverOne();
                assertEquals("submitted",repository.delivery(4L).status());
                java.nio.file.Files.writeString(java.nio.file.Path.of(ready+".done"),"passed");
                return;
            }
            Thread.sleep(200);
        }
        var result=repository.delivery(4L);
        fail("Go did not start the approved fixture: "+result.status()+" / "+result.reason()+" / "+result.receipt());
    }
}
