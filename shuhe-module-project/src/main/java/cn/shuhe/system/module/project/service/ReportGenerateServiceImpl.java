package cn.shuhe.system.module.project.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.shuhe.system.module.project.controller.admin.vo.OutsideMemberRespVO;
import cn.shuhe.system.module.project.controller.admin.vo.OutsideRequestRespVO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectRoundDO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectRoundTargetDO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectRoundVulnerabilityDO;
import cn.shuhe.system.module.project.dal.dataobject.ServiceItemDO;
import com.deepoove.poi.XWPFTemplate;
import com.deepoove.poi.config.Configure;
import com.deepoove.poi.data.PictureRenderData;
import com.deepoove.poi.data.PictureType;
import com.deepoove.poi.data.Pictures;
import com.deepoove.poi.data.TextRenderData;
import com.deepoove.poi.data.Texts;
import com.deepoove.poi.plugin.table.LoopRowTableRenderPolicy;
import org.ddr.poi.html.HtmlRenderPolicy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static cn.shuhe.system.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.shuhe.system.module.project.enums.ErrorCodeConstants.*;

/**
 * 报告生成 Service 实现
 * 基于 poi-tl 模板引擎生成 Word 报告
 *
 * poi-tl 模板语法：
 * - {{变量名}} 文本替换
 * - {{#循环变量}} 开始循环
 * - {{/循环变量}} 结束循环
 * - {{@图片变量}} 图片
 * - {{?条件变量}} 条件判断
 */
@Slf4j
@Service
public class ReportGenerateServiceImpl implements ReportGenerateService {

    private static final String TEMPLATE_PATH = "templates/report/";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm");

    @Resource
    private OutsideRequestService outsideRequestService;

    @Resource
    private ProjectService projectService;

    @Resource
    private ProjectRoundService projectRoundService;

    @Resource
    private ProjectRoundTargetService projectRoundTargetService;

    @Resource
    private ProjectRoundVulnerabilityService projectRoundVulnerabilityService;

    @Resource
    private ServiceItemService serviceItemService;

    // 预定义的模板信息（可以根据实际模板文件扩展）
    private static final Map<String, ReportTemplate> TEMPLATE_INFO = new LinkedHashMap<>();
    static {
        // 外协请求模板
        TEMPLATE_INFO.put("outside-request", new ReportTemplate(
                "outside-request", "外协请求报告模板", "outside-request", "用于生成外协请求报告"));
        // 渗透测试模板
        TEMPLATE_INFO.put("戍合科技渗透测试模板", new ReportTemplate(
                "戍合科技渗透测试模板", "戍合科技渗透测试模板", "pentest", "戍合科技标准渗透测试报告模板"));
        TEMPLATE_INFO.put("天融信渗透测试模版", new ReportTemplate(
                "天融信渗透测试模版", "天融信渗透测试模板", "pentest", "天融信标准渗透测试报告模板"));
        TEMPLATE_INFO.put("奇安信渗透测试模板", new ReportTemplate(
                "奇安信渗透测试模板", "奇安信渗透测试模板", "pentest", "奇安信标准渗透测试报告模板"));
        TEMPLATE_INFO.put("安恒渗透测试模板", new ReportTemplate(
                "安恒渗透测试模板", "安恒渗透测试模板", "pentest", "安恒标准渗透测试报告模板"));
        TEMPLATE_INFO.put("长亭渗透测试模板", new ReportTemplate(
                "长亭渗透测试模板", "长亭渗透测试模板", "pentest", "长亭标准渗透测试报告模板"));
        TEMPLATE_INFO.put("网信办-检测报告", new ReportTemplate(
                "网信办-检测报告", "网信办检测报告", "pentest", "网信办标准检测报告模板"));
        // 复测模板
        TEMPLATE_INFO.put("戍合科技渗透测试复测模板", new ReportTemplate(
                "戍合科技渗透测试复测模板", "戍合科技渗透测试复测模板", "retest", "戍合科技标准复测报告模板"));
        TEMPLATE_INFO.put("天融信渗透测试复测模版", new ReportTemplate(
                "天融信渗透测试复测模版", "天融信渗透测试复测模板", "retest", "天融信标准复测报告模板"));
        TEMPLATE_INFO.put("奇安信渗透测试复测模板", new ReportTemplate(
                "奇安信渗透测试复测模板", "奇安信渗透测试复测模板", "retest", "奇安信标准复测报告模板"));
        TEMPLATE_INFO.put("安恒渗透测试复测模板", new ReportTemplate(
                "安恒渗透测试复测模板", "安恒渗透测试复测模板", "retest", "安恒标准复测报告模板"));
        TEMPLATE_INFO.put("网信办-检测复测报告", new ReportTemplate(
                "网信办-检测复测报告", "网信办检测复测报告", "retest", "网信办标准复测报告模板"));
    }

    @Override
    public List<ReportTemplate> getReportTemplates(String type) {
        List<ReportTemplate> templates = new ArrayList<>();

        try {
            // 扫描 classpath 下的模板文件
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            org.springframework.core.io.Resource[] resources = resolver.getResources("classpath:" + TEMPLATE_PATH + "*.docx");

            for (org.springframework.core.io.Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename != null && filename.endsWith(".docx")) {
                    String code = filename.substring(0, filename.length() - 5); // 去掉 .docx

                    // 从预定义信息中获取模板详情，如果没有则自动生成
                    ReportTemplate info = TEMPLATE_INFO.get(code);
                    if (info != null) {
                        // 根据类型过滤
                        if (type == null || type.isEmpty() || type.equals(info.type())) {
                            templates.add(info);
                        }
                    } else {
                        // 自动生成模板信息
                        String templateType = guessTemplateType(code);
                        if (type == null || type.isEmpty() || type.equals(templateType)) {
                            templates.add(new ReportTemplate(
                                    code,
                                    code,
                                    templateType,
                                    "报告模板: " + code
                            ));
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.warn("扫描模板文件失败", e);
            // 如果扫描失败，返回预定义的模板列表
            for (ReportTemplate template : TEMPLATE_INFO.values()) {
                if (type == null || type.isEmpty() || type.equals(template.type())) {
                    templates.add(template);
                }
            }
        }

        return templates;
    }

    /**
     * 根据模板文件名猜测模板类型
     */
    private String guessTemplateType(String code) {
        String lower = code.toLowerCase();
        if (lower.contains("复测") || lower.contains("retest")) {
            return "retest";
        } else if (lower.contains("渗透") || lower.contains("pentest") || lower.contains("检测")) {
            return "pentest";
        } else if (lower.contains("外协") || lower.contains("外出") || lower.contains("outside")) {
            return "outside-request";
        }
        return "other";
    }

    @Override
    public byte[] generateOutsideRequestReport(Long requestId, String templateCode) {
        // 1. 获取外协请求详情
        OutsideRequestRespVO request = outsideRequestService.getOutsideRequestDetail(requestId);
        if (request == null) {
            throw exception(OUTSIDE_REQUEST_NOT_EXISTS);
        }

        // 2. 准备模板数据
        Map<String, Object> data = buildOutsideRequestData(request);

        // 3. 渲染模板生成报告
        return renderTemplate(templateCode, data);
    }

    @Override
    public byte[] generateProjectReport(Long projectId, String templateCode) {
        // TODO: 实现项目报告生成
        throw new UnsupportedOperationException("项目报告生成功能待实现");
    }

    @Override
    public byte[] generateRoundPentestReport(Long roundId, String templateCode) {
        // 1. 获取轮次信息
        ProjectRoundDO round = projectRoundService.getProjectRound(roundId);
        if (round == null) {
            throw exception(REPORT_GENERATE_FAILED);
        }

        // 2. 获取相关数据
        List<ProjectRoundTargetDO> targets = projectRoundTargetService.getTargetListByRoundId(roundId);
        List<ProjectRoundVulnerabilityDO> vulnerabilities = projectRoundVulnerabilityService.getVulnerabilityListByRoundId(roundId);

        // 3. 获取服务项信息
        ServiceItemDO serviceItem = null;
        if (round.getProjectId() != null) {
            serviceItem = serviceItemService.getServiceItem(round.getProjectId());
        }

        // 4. 构建数据
        Map<String, Object> data = buildRoundReportData(round, targets, vulnerabilities, serviceItem, false);

        // 5. 渲染模板
        return renderRoundTemplate(templateCode, data);
    }

    @Override
    public byte[] generateRoundRetestReport(Long roundId, String templateCode) {
        // 1. 获取轮次信息
        ProjectRoundDO round = projectRoundService.getProjectRound(roundId);
        if (round == null) {
            throw exception(REPORT_GENERATE_FAILED);
        }

        // 2. 获取相关数据
        List<ProjectRoundTargetDO> targets = projectRoundTargetService.getTargetListByRoundId(roundId);
        List<ProjectRoundVulnerabilityDO> vulnerabilities = projectRoundVulnerabilityService.getVulnerabilityListByRoundId(roundId);

        // 3. 获取服务项信息
        ServiceItemDO serviceItem = null;
        if (round.getProjectId() != null) {
            serviceItem = serviceItemService.getServiceItem(round.getProjectId());
        }

        // 4. 构建数据（复测报告）
        Map<String, Object> data = buildRoundReportData(round, targets, vulnerabilities, serviceItem, true);

        // 5. 渲染模板
        return renderRoundTemplate(templateCode, data);
    }

    /**
     * 构建轮次报告数据
     */
    private Map<String, Object> buildRoundReportData(ProjectRoundDO round,
                                                      List<ProjectRoundTargetDO> targets,
                                                      List<ProjectRoundVulnerabilityDO> vulnerabilities,
                                                      ServiceItemDO serviceItem,
                                                      boolean isRetest) {
        Map<String, Object> data = new HashMap<>();

        // 基本信息
        data.put("title", serviceItem != null ? serviceItem.getName() : "安全测试报告");
        data.put("subtitle", isRetest ? "复测报告" : "渗透测试报告");
        data.put("roundName", round.getName() != null ? round.getName() : "第" + round.getRoundNo() + "次执行");
        data.put("date", DateUtil.format(new Date(), "yyyy年MM月dd日"));
        data.put("today", DateUtil.format(new Date(), "yyyy年MM月dd日"));

        // 时间信息
        data.put("start_time", formatDateTime(round.getPlanStartTime()));
        data.put("end_time", formatDateTime(round.getPlanEndTime()));
        data.put("testers", StrUtil.blankToDefault(round.getExecutorNames(), "待分配"));

        // 服务项信息
        if (serviceItem != null) {
            data.put("customerName", serviceItem.getCustomerName());
            data.put("serviceType", serviceItem.getServiceType());
        }

        // 漏洞统计
        long highCount = vulnerabilities.stream().filter(v -> "high".equals(v.getSeverity())).count();
        long mediumCount = vulnerabilities.stream().filter(v -> "medium".equals(v.getSeverity())).count();
        long lowCount = vulnerabilities.stream().filter(v -> "low".equals(v.getSeverity())).count();

        data.put("vul_total_number", vulnerabilities.size());
        data.put("high_risk_num", highCount);
        data.put("medium_risk_num", mediumCount);
        data.put("low_risk_num", lowCount);

        // 系统状态判断
        String systemStatus;
        String systemStatusText;
        if (highCount >= 3) {
            systemStatus = "critical";
            systemStatusText = "紧急";
        } else if (highCount >= 1) {
            systemStatus = "serious";
            systemStatusText = "严重";
        } else if (mediumCount >= 3) {
            systemStatus = "warning";
            systemStatusText = "预警";
        } else {
            systemStatus = "good";
            systemStatusText = "良好";
        }
        data.put("system_status", systemStatus);
        data.put("system_status_text", systemStatusText);
        data.put("system_status_xml", systemStatusText);

        // 目标文本列表
        String targetsText = targets.stream()
                .map(t -> StrUtil.blankToDefault(t.getName(), t.getUrl()))
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.joining("、"));
        data.put("targets_text", StrUtil.blankToDefault(targetsText, "无"));
        // 兼容模板中的 {{targets}} 文本占位符
        data.put("targets", StrUtil.blankToDefault(targetsText, "无"));

        // 安全概况
        String overview;
        if (vulnerabilities.isEmpty()) {
            overview = "本次渗透测试未发现安全漏洞，系统安全状况良好。";
        } else {
            overview = String.format("本次渗透测试共发现 %d 个安全漏洞，其中高危 %d 个，中危 %d 个，低危 %d 个。建议尽快修复。",
                    vulnerabilities.size(), highCount, mediumCount, lowCount);
        }
        data.put("overview", overview);

        // 漏洞类型列表
        Set<String> vulnTypes = vulnerabilities.stream()
                .map(ProjectRoundVulnerabilityDO::getType)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toSet());
        data.put("vul_list_horizontal", String.join("、", vulnTypes));
        data.put("vul_list_vertical", String.join("\n", vulnTypes));
        
        // 将漏洞类型集合传递给模板，用于 SpEL 表达式判断
        // 模板中可使用：{{vulnTypes.contains('SQL注入') ? '不通过' : '通过'}}
        data.put("vulnTypes", vulnTypes);
        
        // 生成带颜色的检测结果（通过=绿色，不通过=红色）
        // 模板中用 {{check_SQL注入}} 等格式引用
        data.putAll(generateCheckResults(vulnTypes));

        // 测试目标表格
        List<Map<String, Object>> targetTable = new ArrayList<>();
        int targetSeq = 1;
        for (ProjectRoundTargetDO target : targets) {
            Map<String, Object> row = new HashMap<>();
            row.put("seq", targetSeq++);
            row.put("name", target.getName());
            row.put("url", StrUtil.blankToDefault(target.getUrl(), "-"));
            row.put("type", target.getType());
            targetTable.add(row);
        }
        data.put("targets_table", targetTable);

        // 漏洞详情列表
        List<Map<String, Object>> vulnList = new ArrayList<>();
        int vulnSeq = 1;
        for (ProjectRoundVulnerabilityDO vuln : vulnerabilities) {
            Map<String, Object> row = new HashMap<>();
            row.put("seq", vulnSeq++);
            row.put("location", vuln.getLocation());
            row.put("url", StrUtil.blankToDefault(vuln.getUrl(), "-"));
            row.put("vul_type", vuln.getType());
            row.put("risk_level", getSeverityText(vuln.getSeverity()));
            row.put("severity", vuln.getSeverity());
            row.put("vul_description", StrUtil.blankToDefault(vuln.getTypeDescription(), "-"));
            row.put("vul_advice", StrUtil.blankToDefault(vuln.getTypeAdvice(), "-"));
            
            // 直接传递 HTML 内容，使用 HtmlRenderPolicy 渲染
            row.put("process", StrUtil.blankToDefault(vuln.getProcess(), "-"));

            // 获取目标名称
            String targetName = targets.stream()
                    .filter(t -> t.getId().equals(vuln.getTargetId()))
                    .map(ProjectRoundTargetDO::getName)
                    .findFirst()
                    .orElse("未分配");
            row.put("target_name", targetName);
            row.put("targetName", targetName);  // 驼峰命名兼容

            // 复测信息
            if (isRetest) {
                row.put("retest_status", vuln.getRetestStatus());
                row.put("retest_status_text", getRetestStatusText(vuln.getRetestStatus()));
                row.put("retest_report", StrUtil.blankToDefault(vuln.getRetestReport(), "-"));
                row.put("retest_date", vuln.getRetestDate() != null ? 
                        vuln.getRetestDate().format(DATE_FORMATTER) : "-");
            }

            vulnList.add(row);
        }
        data.put("vulnerabilities", vulnList);
        data.put("vulnerability_table", vulnList);

        // 复测统计
        if (isRetest) {
            long fixedCount = vulnerabilities.stream().filter(v -> "fixed".equals(v.getRetestStatus())).count();
            long unfixedCount = vulnerabilities.stream().filter(v -> "unfixed".equals(v.getRetestStatus())).count();
            long partialCount = vulnerabilities.stream().filter(v -> "partially-fixed".equals(v.getRetestStatus())).count();

            data.put("fixed_count", fixedCount);
            data.put("unfixed_count", unfixedCount);
            data.put("partial_count", partialCount);

            // 复测结果
            if (unfixedCount == 0 && partialCount == 0) {
                data.put("retest_result", "已修复");
            } else if (fixedCount == 0) {
                data.put("retest_result", "未修复");
            } else {
                data.put("retest_result", "部分修复");
            }
        }

        return data;
    }

    /**
     * 渲染轮次报告模板
     */
    private byte[] renderRoundTemplate(String templateCode, Map<String, Object> data) {
        String templateFile = TEMPLATE_PATH + templateCode + ".docx";

        try {
            ClassPathResource resource = new ClassPathResource(templateFile);
            if (!resource.exists()) {
                log.error("模板文件不存在: {}", templateFile);
                throw exception(REPORT_TEMPLATE_NOT_EXISTS);
            }

            // 配置 poi-tl
            LoopRowTableRenderPolicy policy = new LoopRowTableRenderPolicy();
            HtmlRenderPolicy htmlRenderPolicy = new HtmlRenderPolicy();
            Configure config = Configure.builder()
                    .bind("targets_table", policy)
                    // vulnerabilities 使用默认区块语法，不绑定 LoopRowTableRenderPolicy
                    .bind("vulnerability_table", policy)
                    // process 字段使用 HTML 渲染策略
                    .bind("process", htmlRenderPolicy)
                    .useSpringEL(true)
                    .build();

            try (InputStream inputStream = resource.getInputStream();
                 XWPFTemplate template = XWPFTemplate.compile(inputStream, config).render(data);
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {

                template.writeAndClose(out);
                return out.toByteArray();
            }

        } catch (Exception e) {
            log.error("生成轮次报告失败, templateCode={}", templateCode, e);
            throw exception(REPORT_GENERATE_FAILED);
        }
    }

    /**
     * 获取危害程度文本
     */
    private String getSeverityText(String severity) {
        if (severity == null) return "未知";
        return switch (severity) {
            case "high" -> "高危";
            case "medium" -> "中危";
            case "low" -> "低危";
            default -> severity;
        };
    }

    /**
     * 获取复测状态文本
     */
    private String getRetestStatusText(String status) {
        if (status == null) return "未复测";
        return switch (status) {
            case "fixed" -> "已修复";
            case "unfixed" -> "未修复";
            case "partially-fixed" -> "部分修复";
            default -> status;
        };
    }

    /**
     * 构建外协请求报告数据
     */
    private Map<String, Object> buildOutsideRequestData(OutsideRequestRespVO request) {
        Map<String, Object> data = new HashMap<>();

        // 基本信息
        data.put("projectName", request.getProjectName());
        data.put("serviceItemName", request.getServiceItemName());
        data.put("serviceType", request.getServiceType());
        data.put("requestUserName", request.getRequestUserName());
        data.put("requestDeptName", request.getRequestDeptName());
        data.put("targetDeptName", request.getTargetDeptName());
        data.put("destination", request.getDestination());
        data.put("reason", request.getReason());
        data.put("remark", StrUtil.blankToDefault(request.getRemark(), "无"));

        // 时间信息
        data.put("planStartTime", formatDateTime(request.getPlanStartTime()));
        data.put("planEndTime", formatDateTime(request.getPlanEndTime()));
        data.put("actualStartTime", formatDateTime(request.getActualStartTime()));
        data.put("actualEndTime", formatDateTime(request.getActualEndTime()));
        data.put("planDateRange", buildDateRange(request.getPlanStartTime(), request.getPlanEndTime()));
        data.put("actualDateRange", buildDateRange(request.getActualStartTime(), request.getActualEndTime()));

        // 状态
        data.put("status", getStatusText(request.getStatus()));

        // 生成时间
        data.put("generateDate", DateUtil.format(new Date(), "yyyy年MM月dd日"));
        data.put("generateDateTime", DateUtil.format(new Date(), "yyyy年MM月dd日 HH:mm:ss"));

        // 外出人员列表
        List<OutsideMemberRespVO> members = request.getMembers();
        if (members != null && !members.isEmpty()) {
            // 人员名单（逗号分隔）
            String memberNames = members.stream()
                    .map(OutsideMemberRespVO::getUserName)
                    .collect(Collectors.joining("、"));
            data.put("memberNames", memberNames);
            data.put("memberCount", members.size());

            // 人员表格数据（用于循环）
            List<Map<String, Object>> memberTable = new ArrayList<>();
            int seq = 1;
            for (OutsideMemberRespVO member : members) {
                Map<String, Object> row = new HashMap<>();
                row.put("seq", seq++);
                row.put("userName", member.getUserName());
                row.put("userDeptName", member.getUserDeptName());
                memberTable.add(row);
            }
            data.put("members", memberTable);
        } else {
            data.put("memberNames", "待指派");
            data.put("memberCount", 0);
            data.put("members", Collections.emptyList());
        }

        return data;
    }

    /**
     * 渲染模板生成报告
     */
    private byte[] renderTemplate(String templateCode, Map<String, Object> data) {
        String templateFile = TEMPLATE_PATH + templateCode + ".docx";

        try {
            // 加载模板文件
            ClassPathResource resource = new ClassPathResource(templateFile);
            if (!resource.exists()) {
                log.error("模板文件不存在: {}", templateFile);
                throw exception(REPORT_TEMPLATE_NOT_EXISTS);
            }

            // 配置 poi-tl
            LoopRowTableRenderPolicy policy = new LoopRowTableRenderPolicy();
            Configure config = Configure.builder()
                    .bind("members", policy) // 人员表格循环
                    .useSpringEL(true) // 使用 Spring EL 表达式
                    .build();

            // 渲染模板
            try (InputStream inputStream = resource.getInputStream();
                 XWPFTemplate template = XWPFTemplate.compile(inputStream, config).render(data);
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {

                template.writeAndClose(out);
                return out.toByteArray();
            }

        } catch (Exception e) {
            log.error("生成报告失败, templateCode={}", templateCode, e);
            throw exception(REPORT_GENERATE_FAILED);
        }
    }

    /**
     * 格式化日期时间
     */
    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "未设置";
        }
        return dateTime.format(DATETIME_FORMATTER);
    }

    /**
     * 构建日期范围字符串
     */
    private String buildDateRange(LocalDateTime start, LocalDateTime end) {
        if (start == null && end == null) {
            return "未设置";
        }
        String startStr = start != null ? start.format(DATE_FORMATTER) : "未设置";
        String endStr = end != null ? end.format(DATE_FORMATTER) : "未设置";
        return startStr + " 至 " + endStr;
    }

    /**
     * 获取状态文本
     */
    private String getStatusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case 0 -> "待审批";
            case 1 -> "已通过";
            case 2 -> "已拒绝";
            case 3 -> "已完成";
            case 4 -> "已取消";
            default -> "未知";
        };
    }

    // ==================== 图片处理工具方法 ====================

    /**
     * 从 Base64 字符串创建图片数据
     *
     * @param base64Data Base64 编码的图片数据（可带 data:image/xxx;base64, 前缀）
     * @param width 显示宽度（像素）
     * @param height 显示高度（像素）
     * @return PictureRenderData 对象，用于模板渲染
     */
    protected PictureRenderData createPictureFromBase64(String base64Data, int width, int height) {
        if (StrUtil.isBlank(base64Data)) {
            return null;
        }

        // 去除 data:image/xxx;base64, 前缀
        String pureBase64 = base64Data;
        PictureType pictureType = PictureType.PNG;

        if (base64Data.contains(",")) {
            String[] parts = base64Data.split(",");
            pureBase64 = parts[1];

            // 解析图片类型
            String header = parts[0].toLowerCase();
            if (header.contains("jpeg") || header.contains("jpg")) {
                pictureType = PictureType.JPEG;
            } else if (header.contains("gif")) {
                pictureType = PictureType.GIF;
            } else if (header.contains("bmp")) {
                pictureType = PictureType.BMP;
            }
        }

        return Pictures.ofBase64(pureBase64, pictureType)
                .size(width, height)
                .create();
    }

    /**
     * 从 Base64 字符串创建自适应尺寸的图片数据
     * 自动计算最优显示尺寸，保持原图比例
     *
     * @param base64Data Base64 编码的图片数据
     * @param maxWidth 最大宽度（像素）
     * @param maxHeight 最大高度（像素）
     * @return PictureRenderData 对象
     */
    protected PictureRenderData createAdaptivePicture(String base64Data, int maxWidth, int maxHeight) {
        // 默认使用较大的尺寸，poi-tl 会自动处理
        return createPictureFromBase64(base64Data, maxWidth, maxHeight);
    }

    /**
     * 从字节数组创建图片数据
     *
     * @param imageBytes 图片字节数组
     * @param pictureType 图片类型
     * @param width 显示宽度（像素）
     * @param height 显示高度（像素）
     * @return PictureRenderData 对象
     */
    protected PictureRenderData createPictureFromBytes(byte[] imageBytes, PictureType pictureType, int width, int height) {
        if (imageBytes == null || imageBytes.length == 0) {
            return null;
        }
        return Pictures.ofBytes(imageBytes, pictureType)
                .size(width, height)
                .create();
    }

    /**
     * 从 URL 创建图片数据
     *
     * @param imageUrl 图片 URL
     * @param width 显示宽度（像素）
     * @param height 显示高度（像素）
     * @return PictureRenderData 对象
     */
    protected PictureRenderData createPictureFromUrl(String imageUrl, int width, int height) {
        if (StrUtil.isBlank(imageUrl)) {
            return null;
        }
        try {
            return Pictures.ofUrl(imageUrl)
                    .size(width, height)
                    .create();
        } catch (Exception e) {
            log.warn("从 URL 加载图片失败: {}", imageUrl, e);
            return null;
        }
    }

    /**
     * 检测项列表（用于生成带颜色的检测结果）
     */
    private static final String[] CHECK_ITEMS = {
        // Web安全
        "SQL注入", "XSS攻击", "XML外部实体注入", "CSRF攻击", "SSRF服务器端请求伪造",
        "文件上传漏洞", "任意文件下载", "目录遍历", "源代码泄漏", "信息泄露",
        "CRLF注入", "命令注入", "未验证的重定向", "JSON劫持", "不安全的第三方组件",
        "不安全的文件包含", "远程代码执行", "缺少X-XSS-Protection响应头", "Flash 跨域漏洞",
        "缺少CSRF保护", "明文传输", "通过 GET 传输用户名和密码", "未配置 X-Frame-Options 响应头",
        "任意文件删除", "越权漏洞", "短信/邮件轰炸", "验证码漏洞", "弱口令",
        "暴力破解", "会话固定", "会话劫持", "点击劫持", "敏感数据暴露",
        "SSL/TLS 弱加密", "SSL/TLS 证书问题", "SSL/TLS 存在 FREAK 攻击风险（CVE-2015-0204）",
        "允许 HTTP OPTIONS 方法", "不安全的HTTP方法", "物理路径泄漏", "敏感数据外泄",
        // 额外的检测项
        "绝对路径泄露", "Cookie 未设置 HttpOnly 属性", "X-Forwarded-For 伪造", "任意文件读取",
        "网络传输加密方式不安全", "使用不安全的 Telnet 协议", "验证码爆破,验证码失效,验证码绕过",
        "不安全的反序列化", "用户名枚举", "用户密码枚举", "会话固定攻击", "平行越权", "垂直越权",
        "未授权访问", "业务逻辑漏洞", "短信轰炸", "Flash 未混淆导致反编译",
        "中间件配置缺陷", "中间件弱口令", "JBoss 反序列化导致远程命令执行",
        "WebSphere 反序列化导致远程命令执行", "Jenkins 反序列化命令执行",
        "WebLogic 反序列化导致远程命令执行", "Apache Tomcat 样例目录与 Session 操纵",
        "文件解析导致代码执行", "域传送（DNS Zone Transfer）漏洞", "Redis 未授权访问",
        "MongoDB 未授权访问", "操作系统弱口令", "数据库弱口令", "本地权限提升", "已存在的脚本/木马",
        "永恒之蓝（EternalBlue）利用", "MSSQL 信息探测", "Windows 操作系统漏洞",
        "数据库远程连接暴露", "权限分配不合理", "HTTP.sys 远程代码执行漏洞（相关补丁缺失）",
        "存储型跨站脚本", "SNMP 使用默认团体字符串", "任意用户密码修改/重置（未授权）",
        "Apache Struts2 远程命令执行（S2-045）", "Drupal 版本过低导致多个漏洞",
        "PHP 版本过低导致多个漏洞", "Apache Tomcat 文件包含/读取（Ghostcat — CVE-2020-1938 / CNVD-2020-10487）",
        "Apache Tomcat 版本过低", "Apache Shiro RememberMe 反序列化命令执行",
        "Fastjson 远程代码执行漏洞", "OpenSSL 版本过低导致多个漏洞", "Host 头注入/Host 头攻击",
        "Flash 跨域（crossdomain.xml 配置过宽）", "IIS 短文件名（8.3）泄露",
        "Apache Tomcat 示例目录/示例应用泄露", "Apache Tomcat examples 目录可访问导致多个漏洞",
        "框架注入漏洞", "rsync 未授权访问", "FTP 匿名登录", "验证码绕过", "phpinfo 页面泄露",
        "源码泄漏", "SSL 3.0 POODLE 攻击（CVE-2014-3566）", "链接注入漏洞", "验证码失效",
        "管理后台泄漏", "备份文件泄漏", "版本信息泄漏", "内网 IP 泄露", "jQuery 版本过低",
        "密码暴力破解风险", "WEB-INF/web.xml 泄露", "Bazaar 存储库泄露 (.bzr)",
        "Snoop Servlet 信息泄露", ".DS_Store 文件泄漏", "目录列表", ".idea 目录信息泄露",
        "用户凭据明文传输", "ASP.NET 调试模式已启用（Remote/Local Debugging / customErrors 未关闭）",
        "SSL/TLS 存在 Bar Mitzvah 攻击风险（RC4 弱点）"
    };

    /**
     * 创建带颜色的检测结果文本
     * 
     * @param vulnTypes 发现的漏洞类型集合
     * @param checkItem 检测项名称
     * @return 带颜色的 TextRenderData（通过=绿色，不通过=红色）
     */
    private TextRenderData createCheckResult(Set<String> vulnTypes, String checkItem) {
        boolean pass = !vulnTypes.contains(checkItem);
        if (pass) {
            return Texts.of("通过").color("00B050").create(); // 绿色
        } else {
            return Texts.of("不通过").color("FF0000").create(); // 红色
        }
    }

    /**
     * 标准化 key 名称，去掉 SpEL 不支持的特殊字符
     * 
     * @param name 原始名称
     * @return 标准化后的名称
     */
    private String normalizeKeyName(String name) {
        // 替换特殊字符为下划线，避免 SpEL 解析错误
        // 包括：减号、斜杠、括号、逗号、空格、点号、特殊破折号（em dash、en dash）等
        // 注意：点号(.)在 SpEL 中是属性访问运算符，必须替换
        // em dash (—, U+2014), en dash (–, U+2013) 等特殊字符也需要替换
        return name.replaceAll("[\\-/\\(\\)（）,，\\.\\s—–―]+", "_")
                   .replaceAll("_+", "_")
                   .replaceAll("^_|_$", "");
    }

    /**
     * 为所有检测项生成带颜色的检测结果
     * 
     * @param vulnTypes 发现的漏洞类型集合
     * @return 检测结果 Map，key 为 "check_标准化名称"
     */
    private Map<String, TextRenderData> generateCheckResults(Set<String> vulnTypes) {
        Map<String, TextRenderData> results = new HashMap<>();
        for (String item : CHECK_ITEMS) {
            // 标准化 key 名称，模板中用 {{check_SQL注入}} 等格式引用
            String normalizedKey = "check_" + normalizeKeyName(item);
            results.put(normalizedKey, createCheckResult(vulnTypes, item));
        }
        return results;
    }

    /**
     * HTML 内容解析结果
     */
    protected static class HtmlContent {
        private String text;
        private List<PictureRenderData> images;
        
        public HtmlContent(String text, List<PictureRenderData> images) {
            this.text = text;
            this.images = images;
        }
        
        public String getText() {
            return text;
        }
        
        public List<PictureRenderData> getImages() {
            return images;
        }
    }

    /**
     * 解析 HTML 内容，提取文本和图片
     * 
     * @param html HTML 内容
     * @return HtmlContent 包含文本和图片列表
     */
    protected HtmlContent parseHtmlContent(String html) {
        if (StrUtil.isBlank(html)) {
            return new HtmlContent("-", new ArrayList<>());
        }
        
        List<PictureRenderData> images = new ArrayList<>();
        
        try {
            // 使用 Jsoup 解析 HTML
            Document doc = Jsoup.parse(html);
            
            // 处理图片
            for (Element img : doc.select("img")) {
                String src = img.attr("src");
                if (StrUtil.isNotBlank(src) && !src.startsWith("data:")) {
                    // URL 格式的图片，尝试下载并创建图片数据
                    try {
                        PictureRenderData picture = Pictures.ofUrl(src)
                                .size(400, 300) // 设置默认大小
                                .create();
                        images.add(picture);
                    } catch (Exception e) {
                        log.warn("加载图片失败: {}, 错误: {}", src, e.getMessage());
                    }
                }
                // 移除图片标签
                img.remove();
            }
            
            // 获取纯文本内容
            String text = doc.body().text();
            return new HtmlContent(StrUtil.blankToDefault(text.trim(), "-"), images);
            
        } catch (Exception e) {
            log.warn("解析 HTML 内容失败: {}", e.getMessage());
            return new HtmlContent(StrUtil.blankToDefault(html, "-"), images);
        }
    }

}
