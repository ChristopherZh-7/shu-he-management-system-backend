package cn.shuhe.system.module.project.golish;

import cn.hutool.http.HtmlUtil;
import cn.shuhe.system.framework.datapermission.core.annotation.DataPermission;
import cn.shuhe.system.framework.security.core.util.SecurityFrameworkUtils;
import cn.shuhe.system.module.project.dal.dataobject.*;
import cn.shuhe.system.module.project.dal.mysql.*;
import cn.shuhe.system.module.ticket.dal.dataobject.TicketDO;
import cn.shuhe.system.module.ticket.dal.dataobject.TicketLogDO;
import cn.shuhe.system.module.ticket.dal.mysql.TicketMapper;
import cn.shuhe.system.module.ticket.dal.mysql.TicketLogMapper;
import cn.shuhe.system.module.ticket.enums.TicketActionEnum;
import cn.shuhe.system.module.ticket.framework.event.*;
import cn.shuhe.system.module.ticket.framework.statemachine.TicketStateMachine;
import cn.shuhe.system.module.ticket.service.TicketService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import static cn.shuhe.system.module.project.golish.GolishScanSpec.invalid;

@Service
@RequiredArgsConstructor
public class GolishIntegrationService {
    private final GolishProperties properties;
    private final ObjectMapper json;
    private final GolishJobMapper jobs;
    private final ProjectRoundMapper rounds;
    private final ProjectRoundTargetMapper targets;
    private final ProjectRoundVulnerabilityMapper vulnerabilities;
    private final TicketMapper tickets;
    private final TicketLogMapper logs;
    private final TicketService ticketService;
    private final JdbcTemplate jdbc;

    @EventListener
    public void validateInput(TicketSavingEvent event) {
        GolishScanSpec spec = GolishScanSpec.parse(event.getBusinessType(), event.getExtJson());
        if (spec != null && !properties.isEnabled()) throw invalid("尚未启用 Golish 自动测试，请联系管理员配置服务");
    }

    /** Called within acceptTicket's transaction, before creating its business round. */
    public void lockApproval(TicketAcceptedEvent event) {
        if (GolishScanSpec.parse(event.getBusinessType(), event.getExtJson()) == null) return;
        if (!properties.isEnabled()) throw invalid("Golish 服务未启用，不能批准自动执行");
        jdbc.queryForObject("SELECT id FROM shuhe_ticket WHERE id = ? AND deleted = 0 FOR UPDATE", Long.class, event.getTicketId());
        if (!jobs.forTicket(event.getTicketId()).isEmpty()) throw invalid("该工单已批准执行，请刷新查看测试状态");
    }

    /** Only persists an outbox request. No network calls inside the approval transaction. */
    public void enqueue(TicketAcceptedEvent event, Long roundId) {
        GolishScanSpec spec = GolishScanSpec.parse(event.getBusinessType(), event.getExtJson());
        if (spec == null) return;
        ProjectRoundDO round = rounds.selectById(roundId);
        List<Map<String, String>> approvedTargets = new ArrayList<>();
        for (GolishScanSpec.Target target : spec.getTargets()) {
            ProjectRoundTargetDO row = ProjectRoundTargetDO.builder().roundId(roundId).projectId(round.getProjectId())
                    .name(target.getName()).url(target.getUrl()).type("web").sort(approvedTargets.size()).build();
            targets.insert(row);
            approvedTargets.add(Map.of("id", row.getId().toString(), "name", row.getName(), "url", row.getUrl()));
        }
        String key = "shuhe-ticket-" + event.getTicketId() + "-initial";
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("external_id", key);
        body.put("title", event.getTitle());
        body.put("approval", approval(key, event.getAcceptedBy()));
        body.put("targets", approvedTargets);
        body.put("requirements", spec.getRequirements());
        body.put("time_limit_minutes", spec.getTimeLimitMinutes());
        body.put("template_id", "shuhe-pentest");
        body.put("report", Map.of("testers", "Golish 自动安全测试"));
        GolishJobDO job = newJob(event.getTicketId(), roundId, "initial", key, body);
        jobs.insert(job);
        setStage(event.getTicketId(), "golish_scanning");
        ProjectRoundDO update = new ProjectRoundDO();
        update.setId(roundId); update.setStatus(1); update.setActualStartTime(LocalDateTime.now());
        rounds.updateById(update);
    }

    public record RetestInput(String requestId, List<Long> findingIds, String repairNote, String deployedAt, boolean confirmed) {}

    @Transactional(rollbackFor = Exception.class)
    public Long requestRetest(Long ticketId, RetestInput input) {
        jdbc.queryForObject("SELECT id FROM shuhe_ticket WHERE id = ? AND deleted = 0 FOR UPDATE", Long.class, ticketId);
        ticketService.validateTicketExecutionApproval(ticketId, SecurityFrameworkUtils.getLoginUserId());
        List<GolishJobDO> history = jobs.forTicket(ticketId);
        if (!properties.isEnabled() || history.isEmpty()) throw invalid("当前工单没有可复测的自动测试记录");
        if (input.requestId() == null || !input.requestId().matches("[A-Za-z0-9-]{10,100}") || !input.confirmed()
                || input.repairNote() == null || input.repairNote().isBlank() || input.repairNote().length() > 8000
                || input.deployedAt() == null || input.deployedAt().length() > 80
                || input.findingIds() == null || input.findingIds().isEmpty() || input.findingIds().size() > 50) {
            throw invalid("请确认原漏洞、填写整改说明，并选择 1–50 条漏洞批准复测");
        }
        String key = "shuhe-ticket-" + ticketId + "-retest-" + input.requestId();
        for (GolishJobDO job : history) {
            if (key.equals(job.getRequestKey())) {
                JsonNode prior = read(job.getRequestJson()).path("retest");
                Set<Long> before = new HashSet<>(); prior.path("finding_ids").forEach(v -> before.add(v.asLong()));
                if (!before.equals(new HashSet<>(input.findingIds())) || !prior.path("repair_note").asText().equals(input.repairNote())
                        || !prior.path("deployed_at").asText().equals(input.deployedAt())) throw invalid("同一复测请求编号不能修改内容");
                return job.getId();
            }
        }
        if (history.stream().anyMatch(j -> !Boolean.TRUE.equals(j.getImported()))) throw invalid("仍有测试或报告同步未完成，请先处理当前任务");
        GolishJobDO baseline = history.get(0);
        if (!Boolean.TRUE.equals(baseline.getReportReady())) throw invalid("请等待初测报告归档后再复测");
        Set<Long> selected = new LinkedHashSet<>(input.findingIds());
        if (selected.size() != input.findingIds().size() || selected.stream().anyMatch(id -> id == null || id <= 0)) throw invalid("复测漏洞编号必须唯一且有效");
        Set<Long> available = new HashSet<>();
        for (JsonNode finding : read(baseline.getResultJson()).path("snapshot").path("findings")) available.add(finding.path("id").asLong());
        if (!available.containsAll(selected)) throw invalid("复测只能选择本工单初测报告中的漏洞");
        ObjectNode body = (ObjectNode) read(baseline.getRequestJson()).deepCopy();
        body.put("external_id", key);
        body.put("title", body.path("title").asText() + " · 修复复测");
        body.put("template_id", "shuhe-retest");
        body.set("approval", json.valueToTree(approval(key, SecurityFrameworkUtils.getLoginUserId())));
        body.set("retest", json.valueToTree(Map.of("source_job_id", baseline.getRemoteId(), "finding_ids", selected,
                "repair_note", input.repairNote(), "deployed_at", input.deployedAt(), "confirm_findings", true)));
        GolishJobDO job = newJob(ticketId, baseline.getRoundId(), "retest", key, body);
        job.setSourceJobId(baseline.getId()); jobs.insert(job);
        setStage(ticketId, "golish_retesting");
        audit(ticketId, "golish_retest_approved", "批准修复复测：" + input.repairNote());
        return job.getId();
    }

    public Map<String, Object> status(Long ticketId) {
        ticketService.validateTicketAccess(ticketId, SecurityFrameworkUtils.getLoginUserId());
        if (!properties.isEnabled()) return Map.of("enabled", false, "jobs", List.of());
        List<Map<String, Object>> items = new ArrayList<>();
        for (GolishJobDO job : jobs.forTicket(ticketId)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", job.getId()); row.put("kind", job.getKind()); row.put("state", job.getState());
            row.put("error", job.getError()); row.put("reportReady", job.getReportReady());
            row.put("createTime", job.getCreateTime()); row.put("updateTime", job.getUpdateTime());
            row.put("approval", read(job.getRequestJson()).path("approval"));
            row.put("targets", read(job.getRequestJson()).path("targets"));
            if (job.getResultJson() != null) row.put("snapshot", read(job.getResultJson()).path("snapshot"));
            items.add(row);
        }
        return Map.of("enabled", true, "jobs", items);
    }

    public GolishJobDO readable(Long id) {
        GolishJobDO job = jobs.selectById(id);
        if (job == null) throw invalid("测试记录不存在");
        ticketService.validateTicketAccess(job.getTicketId(), SecurityFrameworkUtils.getLoginUserId());
        return job;
    }

    @EventListener
    public void ensureCanFinish(TicketFinishingEvent event) {
        TicketDO ticket = tickets.selectById(event.getTicketId());
        if (ticket == null || GolishScanSpec.parse(ticket.getBusinessType(), ticket.getExtJson()) == null) return;
        if (!readyForReview(jobs.forTicket(event.getTicketId()))) throw invalid("初测报告不表示整改完成；请完成所有漏洞的修复复测，并处理测试覆盖缺口后再提交验收");
    }

    @EventListener
    public void ensureCanDelete(TicketDeletingEvent event) {
        if (!properties.isEnabled()) return;
        jdbc.queryForObject("SELECT id FROM shuhe_ticket WHERE id = ? AND deleted = 0 FOR UPDATE", Long.class, event.getTicketId());
        if (jobs.forTicket(event.getTicketId()).stream().anyMatch(job -> !Boolean.TRUE.equals(job.getImported()))) {
            throw invalid("自动测试或报告同步尚未结束，不能删除正在执行的工单");
        }
    }

    static boolean readyForReview(List<GolishJobDO> history) {
        if (history.isEmpty() || history.stream().anyMatch(j -> !Boolean.TRUE.equals(j.getImported()))) return false;
        GolishJobDO baseline = history.get(0);
        if (!"completed".equals(baseline.getState())) return false;
        Set<Long> unresolved = new HashSet<>();
        for (JsonNode finding : read(baseline.getResultJson()).path("snapshot").path("findings")) unresolved.add(finding.path("id").asLong());
        // Each new round replaces only the findings actually verified in that round.
        for (GolishJobDO job : history) {
            if (!"retest".equals(job.getKind())) continue;
            if (!matchesRetestSelection(job, read(job.getResultJson()))) return false;
            for (JsonNode finding : read(job.getResultJson()).path("snapshot").path("findings")) {
                long id = finding.path("id").asLong();
                if ("completed".equals(job.getState()) && "fixed".equals(finding.path("retest_result").path("status").asText())) unresolved.remove(id);
                else unresolved.add(id);
            }
        }
        return unresolved.isEmpty();
    }

    static boolean matchesRetestSelection(GolishJobDO job, JsonNode result) {
        Set<Long> selected = new HashSet<>();
        read(job.getRequestJson()).path("retest").path("finding_ids").forEach(id -> selected.add(id.asLong()));
        Set<Long> returned = new HashSet<>();
        for (JsonNode finding : result.path("snapshot").path("findings")) {
            if (!returned.add(finding.path("id").asLong()) || !Set.of("fixed", "not_fixed", "inconclusive")
                    .contains(finding.path("retest_result").path("status").asText())) return false;
        }
        return !selected.isEmpty() && selected.equals(returned);
    }

    /** Artifact bytes are saved first, then results + ticket projection commit together. */
    @Transactional(rollbackFor = Exception.class)
    @DataPermission(enable = false)
    public void importResult(GolishJobDO job, JsonNode result, String reportFile) {
        GolishJobDO current = jobs.selectById(job.getId());
        if (current == null || Boolean.TRUE.equals(current.getImported()) || !Objects.equals(current.getLeaseToken(), job.getLeaseToken())) return;
        jdbc.queryForObject("SELECT id FROM shuhe_ticket WHERE id = ? FOR UPDATE", Long.class, job.getTicketId());
        ProjectRoundDO round = rounds.selectById(job.getRoundId());
        if ("retest".equals(job.getKind()) && !matchesRetestSelection(job, result)) throw invalid("复测报告未完整对应本轮批准的漏洞");
        Set<Long> findingIds = new HashSet<>();
        Map<Long, ProjectRoundTargetDO> targetMap = new HashMap<>();
        for (ProjectRoundTargetDO target : targets.selectListByRoundId(job.getRoundId())) targetMap.put(target.getId(), target);
        for (JsonNode finding : result.path("snapshot").path("findings")) {
            long findingId = finding.path("id").asLong();
            if (findingId <= 0 || !findingIds.add(findingId)) throw invalid("报告包含无效或重复漏洞编号");
            if ("initial".equals(job.getKind())) {
                long targetId = finding.path("target_id").asLong();
                if (!targetMap.containsKey(targetId)) throw invalid("报告目标与已批准范围不匹配");
                ProjectRoundVulnerabilityDO vulnerability = ProjectRoundVulnerabilityDO.builder().roundId(round.getId()).projectId(round.getProjectId())
                        .targetId(targetId).location(finding.path("title").asText()).url(finding.path("url").asText())
                        .severity(finding.path("severity").asText()).type(finding.path("type").asText())
                        .process("<pre>" + HtmlUtil.escape(finding.path("description").asText() + "\n" + finding.path("poc").asText()) + "</pre>")
                        .typeDescription(projectionText(finding.path("description").asText(), 12000))
                        .typeAdvice(projectionText(finding.path("remediation").asText(), 12000)).build();
                vulnerabilities.insert(vulnerability);
                jdbc.update("INSERT INTO project_golish_finding(job_id, finding_id, vulnerability_id) VALUES (?,?,?)", job.getId(), findingId, vulnerability.getId());
            } else {
                Long vulnerabilityId = jdbc.queryForObject("SELECT vulnerability_id FROM project_golish_finding WHERE job_id = ? AND finding_id = ?", Long.class, job.getSourceJobId(), findingId);
                JsonNode assessment = finding.path("retest_result");
                ProjectRoundVulnerabilityDO update = new ProjectRoundVulnerabilityDO();
                update.setId(vulnerabilityId);
                update.setRetestStatus(switch (assessment.path("status").asText()) { case "fixed" -> "fixed"; case "not_fixed" -> "unfixed"; default -> "inconclusive"; });
                update.setRetestReport("<pre>" + HtmlUtil.escape(finding.path("retest_report").asText()) + "</pre>");
                update.setRetestTime(LocalDateTime.now()); update.setRetestDate(LocalDate.now());
                vulnerabilities.updateById(update);
            }
        }
        current.setState(job.getState()); current.setReportReady(true); current.setReportFile(reportFile);
        current.setResultJson(write(result)); current.setImported(true); current.setError("");
        jobs.updateById(current);
        TicketDO ticket = tickets.selectById(job.getTicketId());
        boolean ready = readyForReview(jobs.forTicket(job.getTicketId()));
        setStage(job.getTicketId(), ready ? "golish_ready_for_review" : "golish_remediation");
        if ("retest".equals(job.getKind()) && ready && Integer.valueOf(1).equals(ticket.getStatus())) {
            int next = TicketStateMachine.checkTransition(ticket.getStatus(), TicketActionEnum.FINISH);
            TicketDO update = new TicketDO(); update.setId(ticket.getId()); update.setStatus(next); tickets.updateById(update);
            audit(ticket.getId(), TicketActionEnum.FINISH.getAction(), "所有原漏洞已完成修复复测，进入待验收；报告已归档", ticket.getStatus(), next);
        } else {
            audit(ticket.getId(), "golish_report", "initial".equals(job.getKind()) ? "初测报告已回传，工单保持处理中，等待整改和复测" : "复测报告已回传，仍有未修复或无法确认的项目，请继续整改");
        }
        ProjectRoundDO update = new ProjectRoundDO(); update.setId(round.getId());
        update.setResult(projectionText(result.path("snapshot").path("report").asText(), 12000));
        update.setProgress(ready ? 100 : 80); rounds.updateById(update);
    }

    private GolishJobDO newJob(Long ticketId, Long roundId, String kind, String key, Object body) {
        GolishJobDO job = new GolishJobDO();
        job.setTicketId(ticketId); job.setRoundId(roundId); job.setKind(kind); job.setRequestKey(key);
        job.setRequestJson(write(body)); job.setState("pending_dispatch"); job.setError("");
        job.setReportReady(false); job.setImported(false); job.setNextPoll(LocalDateTime.now());
        job.setLeaseToken(""); job.setLeaseUntil(LocalDateTime.of(1970, 1, 1, 0, 0));
        return job;
    }
    private Map<String, Object> approval(String reference, Long actor) {
        return Map.of("approved", true, "reference", reference, "by", "shuhe-user:" + actor, "at", Instant.now().toString());
    }
    private void setStage(Long ticketId, String stage) {
        TicketDO update = new TicketDO(); update.setId(ticketId); update.setSubStatus(stage); tickets.updateById(update);
    }
    private void audit(Long ticketId, String action, String message) {
        TicketDO ticket = tickets.selectById(ticketId);
        audit(ticketId, action, message, ticket.getStatus(), ticket.getStatus());
    }
    private void audit(Long ticketId, String action, String message, Integer from, Integer to) {
        Long actor = SecurityFrameworkUtils.getLoginUserId();
        logs.insert(TicketLogDO.builder().ticketId(ticketId).operatorId(actor == null ? 0L : actor)
                .operatorName(SecurityFrameworkUtils.getLoginUserId() == null ? "Golish 自动测试服务" : SecurityFrameworkUtils.getLoginUserNickname())
                .action(action).fromStatus(from).toStatus(to).content(projectionText(message, 1000)).build());
    }
    // The full unmodified snapshot and Word artifact remain in project_golish_job.
    private static String projectionText(String value, int limit) {
        if (value.length() <= limit) return value;
        return value.substring(0, Character.isHighSurrogate(value.charAt(limit - 1)) ? limit - 1 : limit);
    }
    private String write(Object value) {
        try { return json.writeValueAsString(value); } catch (Exception ex) { throw new IllegalStateException("Cannot encode Golish request", ex); }
    }
    static JsonNode read(String value) {
        try { return new ObjectMapper().readTree(value == null ? "{}" : value); } catch (Exception ex) { throw new IllegalStateException("Invalid persisted Golish JSON", ex); }
    }
}
