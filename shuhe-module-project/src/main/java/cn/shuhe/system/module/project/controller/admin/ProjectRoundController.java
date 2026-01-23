package cn.shuhe.system.module.project.controller.admin;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.shuhe.system.framework.common.pojo.CommonResult;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectRoundRespVO;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectRoundSaveReqVO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectRoundDO;
import cn.shuhe.system.module.project.service.ProjectRoundService;
import cn.shuhe.system.module.project.service.ReportGenerateService;
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

@Tag(name = "管理后台 - 项目轮次")
@RestController
@RequestMapping("/project/round")
@Validated
public class ProjectRoundController {

    @Resource
    private ProjectRoundService projectRoundService;

    @Resource
    private ReportGenerateService reportGenerateService;

    @PostMapping("/create")
    @Operation(summary = "创建项目轮次")
    @PreAuthorize("@ss.hasPermission('project:info:update')")
    public CommonResult<Long> createProjectRound(@Valid @RequestBody ProjectRoundSaveReqVO createReqVO) {
        return success(projectRoundService.createProjectRound(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新项目轮次")
    @PreAuthorize("@ss.hasPermission('project:info:update')")
    public CommonResult<Boolean> updateProjectRound(@Valid @RequestBody ProjectRoundSaveReqVO updateReqVO) {
        projectRoundService.updateProjectRound(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除项目轮次")
    @Parameter(name = "id", description = "轮次编号", required = true)
    @PreAuthorize("@ss.hasPermission('project:info:update')")
    public CommonResult<Boolean> deleteProjectRound(@RequestParam("id") Long id) {
        projectRoundService.deleteProjectRound(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得项目轮次详情")
    @Parameter(name = "id", description = "轮次编号", required = true)
    @PreAuthorize("@ss.hasPermission('project:info:query')")
    public CommonResult<ProjectRoundRespVO> getProjectRound(@RequestParam("id") Long id) {
        ProjectRoundDO round = projectRoundService.getProjectRound(id);
        return success(convertToRespVO(round));
    }

    @GetMapping("/list")
    @Operation(summary = "获得服务项的轮次列表")
    @Parameter(name = "serviceItemId", description = "服务项编号", required = true)
    @PreAuthorize("@ss.hasPermission('project:info:query')")
    public CommonResult<List<ProjectRoundRespVO>> getProjectRoundList(@RequestParam("serviceItemId") Long serviceItemId) {
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
        vo.setRoundNo(round.getRoundNo());
        vo.setName(round.getName());
        vo.setPlanStartTime(round.getPlanStartTime());
        vo.setPlanEndTime(round.getPlanEndTime());
        vo.setActualStartTime(round.getActualStartTime());
        vo.setActualEndTime(round.getActualEndTime());
        vo.setStatus(round.getStatus());
        vo.setProgress(round.getProgress());
        vo.setResult(round.getResult());
        vo.setAttachments(round.getAttachments());
        vo.setRemark(round.getRemark());
        vo.setCreateTime(round.getCreateTime());
        vo.setUpdateTime(round.getUpdateTime());
        
        // 处理执行人ID列表
        if (StrUtil.isNotBlank(round.getExecutorIds())) {
            vo.setExecutorIds(JSONUtil.toList(round.getExecutorIds(), Long.class));
        }
        vo.setExecutorNames(round.getExecutorNames());
        
        return vo;
    }

    @PutMapping("/update-status")
    @Operation(summary = "更新轮次状态")
    @PreAuthorize("@ss.hasPermission('project:info:update')")
    public CommonResult<Boolean> updateRoundStatus(@RequestParam("id") Long id,
                                                   @RequestParam("status") Integer status) {
        projectRoundService.updateRoundStatus(id, status);
        return success(true);
    }

    @PutMapping("/update-progress")
    @Operation(summary = "更新轮次进度")
    @PreAuthorize("@ss.hasPermission('project:info:update')")
    public CommonResult<Boolean> updateRoundProgress(@RequestParam("id") Long id,
                                                     @RequestParam("progress") Integer progress) {
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
        // 生成报告
        byte[] reportData = reportGenerateService.generateRoundPentestReport(id, templateCode);

        // 获取轮次信息用于文件名
        ProjectRoundDO round = projectRoundService.getProjectRound(id);
        String roundName = round.getName() != null ? round.getName() : "第" + round.getRoundNo() + "次执行";
        String fileName = String.format("渗透测试报告_%s.docx", roundName);

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
        // 生成报告
        byte[] reportData = reportGenerateService.generateRoundRetestReport(id, templateCode);

        // 获取轮次信息用于文件名
        ProjectRoundDO round = projectRoundService.getProjectRound(id);
        String roundName = round.getName() != null ? round.getName() : "第" + round.getRoundNo() + "次执行";
        String fileName = String.format("复测报告_%s.docx", roundName);

        // 设置响应头
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));
        response.setContentLength(reportData.length);

        // 写入响应
        response.getOutputStream().write(reportData);
        response.getOutputStream().flush();
    }

}
