package cn.shuhe.system.module.project.controller.admin;

import cn.shuhe.system.framework.common.pojo.CommonResult;
import cn.shuhe.system.framework.security.core.util.SecurityFrameworkUtils;
import cn.shuhe.system.module.project.dal.dataobject.GolishJobDO;
import cn.shuhe.system.module.project.golish.*;
import cn.shuhe.system.module.ticket.service.TicketService;
import jakarta.annotation.security.PermitAll;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import static cn.shuhe.system.framework.common.pojo.CommonResult.success;

@RestController
@RequiredArgsConstructor
@RequestMapping("/project/golish")
public class GolishIntegrationController {
    private final GolishIntegrationService service;
    private final GolishCallbackService callbacks;
    private final GolishClient client;
    private final TicketService ticketService;

    @GetMapping("/status")
    @PreAuthorize("@ss.hasPermission('ticket:ticket:query')")
    public CommonResult<Map<String, Object>> status(@RequestParam Long ticketId) { return success(service.status(ticketId)); }

    @PostMapping("/retest")
    @PreAuthorize("@ss.hasPermission('ticket:ticket:accept')")
    public CommonResult<Long> retest(@RequestParam Long ticketId, @RequestBody GolishIntegrationService.RetestInput input) {
        return success(service.requestRetest(ticketId, input));
    }

    @PostMapping("/retry")
    @PreAuthorize("@ss.hasPermission('ticket:ticket:accept')")
    public CommonResult<Boolean> retry(@RequestParam Long id) throws Exception {
        GolishJobDO job = service.readable(id);
        ticketService.validateTicketExecutionApproval(job.getTicketId(), SecurityFrameworkUtils.getLoginUserId());
        if ("attention_required".equals(job.getState())) client.command(job.getRemoteId(), "resume");
        else if ("report_failed".equals(job.getState())) client.command(job.getRemoteId(), "report/retry");
        else throw GolishScanSpec.invalid("当前任务会自动同步；只有暂停或报告失败的任务需要手动重试");
        return success(true);
    }

    @GetMapping("/report")
    @PreAuthorize("@ss.hasPermission('ticket:ticket:query')")
    public ResponseEntity<FileSystemResource> report(@RequestParam Long id) {
        GolishJobDO job = service.readable(id);
        if (!Boolean.TRUE.equals(job.getReportReady())) throw GolishScanSpec.invalid("报告尚未归档");
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("Golish-" + job.getKind() + "-" + id + ".docx", StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(new FileSystemResource(job.getReportFile()));
    }

    @PostMapping("/callback")
    @PermitAll
    public ResponseEntity<Void> callback(@RequestHeader(value = "X-Golish-Event-ID", required = false) String event,
            @RequestHeader(value = "X-Golish-Timestamp", required = false) String timestamp,
            @RequestHeader(value = "X-Golish-Signature", required = false) String signature, @RequestBody byte[] body) {
        // The application's JSON exception adapter otherwise changes failures into HTTP 200.
        // Webhook senders need the actual transport status to decide whether to retry.
        try {
            callbacks.receive(event, timestamp, signature, body);
            return ResponseEntity.noContent().build();
        } catch (ResponseStatusException ex) {
            return ResponseEntity.status(ex.getStatusCode()).build();
        } catch (RuntimeException ex) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
