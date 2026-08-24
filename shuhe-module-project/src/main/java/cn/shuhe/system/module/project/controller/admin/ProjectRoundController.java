package cn.shuhe.system.module.project.controller.admin;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.shuhe.system.framework.common.pojo.CommonResult;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectRoundRespVO;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectRoundSaveReqVO;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectRoundMemberRespVO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectRoundDO;
import cn.shuhe.system.module.project.dal.dataobject.ServiceLaunchDO;
import cn.shuhe.system.module.project.dal.dataobject.ServiceItemDO;
import cn.shuhe.system.module.project.dal.mysql.ServiceLaunchMapper;
import cn.shuhe.system.module.project.dal.mysql.ServiceItemMapper;
import cn.shuhe.system.module.project.dal.mysql.ProjectRoundMemberMapper;
import cn.shuhe.system.framework.common.util.object.BeanUtils;
import cn.shuhe.system.module.project.enums.ProjectMemberRoleEnum;
import cn.shuhe.system.module.project.service.ProjectRoundService;
import cn.shuhe.system.module.project.service.ReportGenerateService;
import cn.shuhe.system.module.project.service.ProjectRoundReportArtifactService;
import cn.shuhe.system.module.project.dal.dataobject.ProjectRoundReportArtifactDO;
import cn.hutool.crypto.digest.DigestUtil;
import cn.shuhe.system.module.project.service.access.ProjectAccessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import static cn.shuhe.system.framework.common.pojo.CommonResult.success;
import static cn.shuhe.system.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - 项目轮次")
@RestController
@RequestMapping("/project/round")
@Validated
public class ProjectRoundController {

    @Resource
    private ProjectRoundService projectRoundService;

    @Resource
    private ReportGenerateService reportGenerateService;

    @Resource
    private ServiceLaunchMapper serviceLaunchMapper;

    @Resource
    private ServiceItemMapper serviceItemMapper;

    @Resource
    private ProjectAccessService projectAccessService;

    @Resource
    private ProjectRoundReportArtifactService reportArtifactService;

    @Resource
    private ProjectRoundMemberMapper projectRoundMemberMapper;

    @PostMapping("/create")
    @Operation(summary = "创建项目轮次")
    @PreAuthorize("@ss.hasPermission('project:info:update')")
    public CommonResult<Long> createProjectRound(@Valid @RequestBody ProjectRoundSaveReqVO createReqVO) {
        validateWritableTarget(createReqVO.getProjectId(), createReqVO.getServiceItemId());
        return success(projectRoundService.createProjectRound(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新项目轮次")
    @PreAuthorize("@ss.hasPermission('project:info:update')")
    public CommonResult<Boolean> updateProjectRound(@Valid @RequestBody ProjectRoundSaveReqVO updateReqVO) {
        ProjectRoundDO existing = projectRoundService.getProjectRound(updateReqVO.getId());
        validateWriteRound(existing);
        validateRoundManager(existing);
        updateReqVO.setProjectId(existing.getProjectId());
        if (updateReqVO.getServiceItemId() == null) {
            updateReqVO.setServiceItemId(existing.getServiceItemId());
        }
        validateWritableTarget(updateReqVO.getProjectId(), updateReqVO.getServiceItemId());
        projectRoundService.updateProjectRound(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除项目轮次")
    @Parameter(name = "id", description = "轮次编号", required = true)
    @PreAuthorize("@ss.hasPermission('project:info:update')")
    public CommonResult<Boolean> deleteProjectRound(@RequestParam("id") Long id) {
        ProjectRoundDO round = projectRoundService.getProjectRound(id);
        validateWriteRound(round);
        validateRoundManager(round);
        projectRoundService.deleteProjectRound(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得项目轮次详情")
    @Parameter(name = "id", description = "轮次编号", required = true)
    @PreAuthorize("@ss.hasAnyPermissions('project:info:query', 'project:my-tasks:query')")
    public CommonResult<ProjectRoundRespVO> getProjectRound(@RequestParam("id") Long id) {
        ProjectRoundDO round = projectRoundService.getProjectRound(id);
        validateReadRound(round);
        return success(convertToRespVO(round));
    }

    @GetMapping("/list")
    @Operation(summary = "获得服务项的轮次列表")
    @Parameter(name = "serviceItemId", description = "服务项编号", required = true)
    @PreAuthorize("@ss.hasAnyPermissions('project:info:query', 'project:my-tasks:query')")
    public CommonResult<List<ProjectRoundRespVO>> getProjectRoundList(@RequestParam("serviceItemId") Long serviceItemId) {
        validateReadableServiceItem(serviceItemId);
        List<ProjectRoundDO> list = projectRoundService.getProjectRoundListByServiceItemId(serviceItemId);
        return success(list.stream().map(this::convertToRespVO).collect(Collectors.toList()));
    }

    /**
     * 将 DO 转换为 RespVO，处理 executorIds 的 JSON 转换
     */
    private ProjectRoundRespVO convertToRespVO(ProjectRoundDO round) {
        if (round == null) {
            return null;
        }
        ProjectRoundRespVO vo = new ProjectRoundRespVO();
        vo.setId(round.getId());
        vo.setProjectId(round.getProjectId());
        vo.setServiceItemId(round.getServiceItemId());
        vo.setTicketId(round.getTicketId());
        vo.setSourceType(round.getSourceType());
        vo.setRoundNo(round.getRoundNo());
        vo.setName(round.getName());
        vo.setDeadline(round.getDeadline());
        vo.setPlanStartTime(round.getPlanStartTime());
        vo.setPlanEndTime(round.getPlanEndTime());
        vo.setActualStartTime(round.getActualStartTime());
        vo.setActualEndTime(round.getActualEndTime());
        vo.setStatus(round.getStatus());
        vo.setSubStatus(round.getSubStatus());
        vo.setCurrentPhase(round.getCurrentPhase());
        vo.setProgress(round.getProgress());
        vo.setResult(round.getResult());
        vo.setAttachments(round.getAttachments());
        vo.setRemark(round.getRemark());
        vo.setScopeSummary(round.getScopeSummary());
        vo.setExcludedScope(round.getExcludedScope());
        vo.setDeliverableRequirements(round.getDeliverableRequirements());
        vo.setAuthorizationStatus(round.getAuthorizationStatus());
        vo.setAuthorizationValidUntil(round.getAuthorizationValidUntil());
        vo.setTestMode(round.getTestMode());
        vo.setTestWindow(round.getTestWindow());
        vo.setSourceIps(round.getSourceIps());
        vo.setEmergencyContact(round.getEmergencyContact());
        vo.setStopConditions(round.getStopConditions());
        vo.setRetestPolicy(round.getRetestPolicy());
        vo.setScopeLockedBy(round.getScopeLockedBy());
        vo.setScopeLockedAt(round.getScopeLockedAt());
        vo.setMembers(projectRoundMemberMapper.selectListByRoundId(round.getId()).stream()
                .map(member -> BeanUtils.toBean(member, ProjectRoundMemberRespVO.class))
                .toList());
        vo.setIsOutside(round.getIsOutside());
        vo.setIsCrossDept(round.getIsCrossDept());
        vo.setServiceLaunchId(round.getServiceLaunchId());
        vo.setCreateTime(round.getCreateTime());
        vo.setUpdateTime(round.getUpdateTime());
        
        // 处理执行人ID列表
        if (StrUtil.isNotBlank(round.getExecutorIds())) {
            vo.setExecutorIds(JSONUtil.toList(round.getExecutorIds(), Long.class));
        }
        vo.setExecutorNames(round.getExecutorNames());
        
        // 填充渗透测试附件（从服务发起记录中获取）
        try {
            if (round.getServiceLaunchId() != null) {
                ServiceLaunchDO launch = serviceLaunchMapper.selectById(round.getServiceLaunchId());
                if (launch != null) {
                    if (StrUtil.isNotBlank(launch.getAuthorizationUrls())) {
                        vo.setAuthorizationUrls(JSONUtil.toList(launch.getAuthorizationUrls(), String.class));
                    }
                    if (StrUtil.isNotBlank(launch.getTestScopeUrls())) {
                        vo.setTestScopeUrls(JSONUtil.toList(launch.getTestScopeUrls(), String.class));
                    }
                    if (StrUtil.isNotBlank(launch.getCredentialsUrls())) {
                        vo.setCredentialsUrls(JSONUtil.toList(launch.getCredentialsUrls(), String.class));
                    }
                }
            }
        } catch (Exception e) {
            // 忽略附件加载错误，不影响主流程
        }
        
        return vo;
    }

    @PutMapping("/update-status")
    @Operation(summary = "更新轮次状态")
    @PreAuthorize("@ss.hasAnyPermissions('project:info:update', 'project:my-tasks:update')")
    public CommonResult<Boolean> updateRoundStatus(@RequestParam("id") Long id,
                                                   @RequestParam("status") Integer status) {
        ProjectRoundDO round = projectRoundService.getProjectRound(id);
        validateWriteRound(round);
        validateRoundStatusActor(round, status);
        projectRoundService.updateRoundStatus(id, status);
        return success(true);
    }

    @PutMapping("/update-progress")
    @Operation(summary = "更新轮次进度")
    @PreAuthorize("@ss.hasAnyPermissions('project:info:update', 'project:my-tasks:update')")
    public CommonResult<Boolean> updateRoundProgress(@RequestParam("id") Long id,
                                                     @RequestParam("progress") Integer progress) {
        validateWriteRound(projectRoundService.getProjectRound(id));
        projectRoundService.updateRoundProgress(id, progress);
        return success(true);
    }

    // ==================== 报告生成 ====================

    @GetMapping("/report-templates")
    @Operation(summary = "获取可用的报告模板列表")
    @Parameter(name = "type", description = "模板类型：pentest（渗透测试）, retest（复测）", required = false)
    @PreAuthorize("@ss.hasPermission('project:info:query')")
    public CommonResult<List<ReportGenerateService.ReportTemplate>> getReportTemplates(
            @RequestParam(value = "type", required = false) String type) {
        return success(reportGenerateService.getReportTemplates(type));
    }

    @GetMapping("/export-pentest-report")
    @Operation(summary = "导出渗透测试报告", description = "根据模板生成 Word 报告并下载")
    @Parameter(name = "id", description = "轮次ID", required = true)
    @Parameter(name = "templateCode", description = "模板编码", required = true)
    @PreAuthorize("@ss.hasPermission('project:info:query')")
    public void exportPentestReport(
            @RequestParam("id") Long id,
            @RequestParam("templateCode") String templateCode,
            HttpServletResponse response) throws IOException {
        validateReadRound(projectRoundService.getProjectRound(id));
        // 生成报告
        byte[] reportData = reportGenerateService.generateRoundPentestReport(id, templateCode);

        // 获取轮次信息用于文件名
        ProjectRoundDO round = projectRoundService.getProjectRound(id);
        String roundName = round.getName() != null ? round.getName() : "第" + round.getRoundNo() + "次执行";
        String fileName = String.format("渗透测试报告_%s.docx", roundName);
        reportArtifactService.recordGenerated(id, "pentest", templateCode, fileName,
                DigestUtil.sha256Hex(reportData));

        // 设置响应头
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));
        response.setContentLength(reportData.length);

        // 写入响应
        response.getOutputStream().write(reportData);
        response.getOutputStream().flush();
    }

    @GetMapping("/export-retest-report")
    @Operation(summary = "导出复测报告", description = "根据模板生成 Word 复测报告并下载")
    @Parameter(name = "id", description = "轮次ID", required = true)
    @Parameter(name = "templateCode", description = "模板编码", required = true)
    @PreAuthorize("@ss.hasPermission('project:info:query')")
    public void exportRetestReport(
            @RequestParam("id") Long id,
            @RequestParam("templateCode") String templateCode,
            HttpServletResponse response) throws IOException {
        validateReadRound(projectRoundService.getProjectRound(id));
        // 生成报告
        byte[] reportData = reportGenerateService.generateRoundRetestReport(id, templateCode);

        // 获取轮次信息用于文件名
        ProjectRoundDO round = projectRoundService.getProjectRound(id);
        String roundName = round.getName() != null ? round.getName() : "第" + round.getRoundNo() + "次执行";
        String fileName = String.format("复测报告_%s.docx", roundName);
        reportArtifactService.recordGenerated(id, "retest", templateCode, fileName,
                DigestUtil.sha256Hex(reportData));

        // 设置响应头
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));
        response.setContentLength(reportData.length);

        // 写入响应
        response.getOutputStream().write(reportData);
        response.getOutputStream().flush();
    }

    @GetMapping("/export-pentest-reports-zip")
    @Operation(summary = "按系统导出渗透测试报告（ZIP）", description = "每个测试目标生成独立报告，打包为 ZIP 下载")
    @Parameter(name = "id", description = "轮次ID", required = true)
    @Parameter(name = "templateCode", description = "模板编码", required = true)
    @PreAuthorize("@ss.hasPermission('project:info:query')")
    public void exportPentestReportsZip(
            @RequestParam("id") Long id,
            @RequestParam("templateCode") String templateCode,
            HttpServletResponse response) throws IOException {
        validateReadRound(projectRoundService.getProjectRound(id));
        byte[] zipData = reportGenerateService.generateRoundPentestReportsZip(id, templateCode);

        ProjectRoundDO round = projectRoundService.getProjectRound(id);
        String roundName = round.getName() != null ? round.getName() : "第" + round.getRoundNo() + "次执行";
        String fileName = String.format("渗透测试报告_按系统_%s.zip", roundName);
        reportArtifactService.recordGenerated(id, "pentest", templateCode, fileName,
                DigestUtil.sha256Hex(zipData));

        response.setContentType("application/zip");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));
        response.setContentLength(zipData.length);

        response.getOutputStream().write(zipData);
        response.getOutputStream().flush();
    }

    @GetMapping("/export-retest-reports-zip")
    @Operation(summary = "按系统导出复测报告（ZIP）", description = "每个测试目标生成独立复测报告，打包为 ZIP 下载")
    @Parameter(name = "id", description = "轮次ID", required = true)
    @Parameter(name = "templateCode", description = "模板编码", required = true)
    @PreAuthorize("@ss.hasPermission('project:info:query')")
    public void exportRetestReportsZip(
            @RequestParam("id") Long id,
            @RequestParam("templateCode") String templateCode,
            HttpServletResponse response) throws IOException {
        validateReadRound(projectRoundService.getProjectRound(id));
        byte[] zipData = reportGenerateService.generateRoundRetestReportsZip(id, templateCode);

        ProjectRoundDO round = projectRoundService.getProjectRound(id);
        String roundName = round.getName() != null ? round.getName() : "第" + round.getRoundNo() + "次执行";
        String fileName = String.format("复测报告_按系统_%s.zip", roundName);
        reportArtifactService.recordGenerated(id, "retest", templateCode, fileName,
                DigestUtil.sha256Hex(zipData));

        response.setContentType("application/zip");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));
        response.setContentLength(zipData.length);

        response.getOutputStream().write(zipData);
        response.getOutputStream().flush();
    }

    @GetMapping("/report-artifacts")
    @Operation(summary = "查看报告版本、审核与交付记录")
    @PreAuthorize("@ss.hasAnyPermissions('project:info:query', 'project:my-tasks:query')")
    public CommonResult<List<ProjectRoundReportArtifactDO>> getReportArtifacts(
            @RequestParam("id") Long id) {
        validateReadRound(projectRoundService.getProjectRound(id));
        return success(reportArtifactService.listByRoundId(id));
    }

    private ServiceItemDO validateReadableServiceItem(Long serviceItemId) {
        ServiceItemDO item = serviceItemMapper.selectById(serviceItemId);
        if (item == null || !projectAccessService.canReadDept(
                item.getProjectId(), getLoginUserId(), item.getDeptId())) {
            throwForbidden();
        }
        return item;
    }

    private void validateWritableTarget(Long projectId, Long serviceItemId) {
        if (serviceItemId == null) {
            projectAccessService.validateManageProject(projectId, getLoginUserId());
            return;
        }
        ServiceItemDO item = validateReadableServiceItem(serviceItemId);
        if (!projectId.equals(item.getProjectId()) || !canOperateProject(item.getProjectId())) {
            throwForbidden();
        }
    }

    private void validateReadRound(ProjectRoundDO round) {
        if (round == null) {
            throwForbidden();
        }
        if (round.getServiceItemId() != null) {
            validateReadableServiceItem(round.getServiceItemId());
        } else {
            projectAccessService.validateViewProject(round.getProjectId(), getLoginUserId());
        }
    }

    private void validateWriteRound(ProjectRoundDO round) {
        validateReadRound(round);
        if (!canOperateProject(round.getProjectId())) {
            throwForbidden();
        }
    }

    /**
     * 状态动作按责任链授权。工单来源轮次除“主执行人开始”外，只能由工单生命周期同步，
     * 防止用户从轮次接口绕过项目经理、技术审核或最终验收。
     */
    private void validateRoundStatusActor(ProjectRoundDO round, Integer toStatus) {
        Long userId = getLoginUserId();
        int fromStatus = round.getStatus() == null ? 0 : round.getStatus();
        boolean primaryExecutor = projectRoundMemberMapper.existsByRoundIdAndUserIdAndRole(
                round.getId(), userId, "primary_executor");
        boolean techReviewer = projectRoundMemberMapper.existsByRoundIdAndUserIdAndRole(
                round.getId(), userId, "tech_reviewer");
        boolean projectManager = projectRoundMemberMapper.existsByRoundIdAndUserIdAndRole(
                round.getId(), userId, "project_manager")
                || projectAccessService.canManageProject(round.getProjectId(), userId);

        if ("ticket".equals(round.getSourceType())) {
            if (!(fromStatus == 0 && Integer.valueOf(1).equals(toStatus) && primaryExecutor)) {
                throwForbidden();
            }
            return;
        }

        boolean allowedActor = switch (fromStatus) {
            case 0 -> Integer.valueOf(1).equals(toStatus) ? primaryExecutor : projectManager;
            case 1 -> Integer.valueOf(4).equals(toStatus) ? primaryExecutor : projectManager;
            case 2, 5, 6 -> projectManager;
            case 4 -> techReviewer;
            case 7 -> primaryExecutor;
            default -> false;
        };
        if (!allowedActor) {
            throwForbidden();
        }
    }

    private void validateRoundManager(ProjectRoundDO round) {
        Long userId = getLoginUserId();
        boolean projectManager = projectRoundMemberMapper.existsByRoundIdAndUserIdAndRole(
                round.getId(), userId, "project_manager")
                || projectAccessService.canManageProject(round.getProjectId(), userId);
        if (!projectManager) {
            throwForbidden();
        }
    }

    private boolean canOperateProject(Long projectId) {
        Integer role = projectAccessService.getEffectiveRole(projectId, getLoginUserId());
        return ProjectMemberRoleEnum.MANAGER.getValue().equals(role)
                || ProjectMemberRoleEnum.EXECUTOR.getValue().equals(role)
                || ProjectMemberRoleEnum.DEPT_LEADER.getValue().equals(role);
    }

    private void throwForbidden() {
        throw cn.shuhe.system.framework.common.exception.util.ServiceExceptionUtil.exception(
                cn.shuhe.system.framework.common.exception.enums.GlobalErrorCodeConstants.FORBIDDEN);
    }

}
