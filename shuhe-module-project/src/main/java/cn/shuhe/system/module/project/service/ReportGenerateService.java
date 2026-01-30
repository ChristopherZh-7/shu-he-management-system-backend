package cn.shuhe.system.module.project.service;

import java.util.List;

/**
 * 报告生成 Service 接口
 * 基于 poi-tl 模板引擎生成 Word 报告
 */
public interface ReportGenerateService {

    /**
     * 报告模板信息
     */
    record ReportTemplate(
            String code,        // 模板编码（文件名，不含扩展名）
            String name,        // 模板名称（显示名）
            String type,        // 模板类型：outside-request, pentest, retest
            String description  // 模板描述
    ) {}

    /**
     * 获取可用的报告模板列表
     *
     * @param type 模板类型（可选）：pentest, retest
     * @return 模板列表
     */
    List<ReportTemplate> getReportTemplates(String type);

    /**
     * 生成项目报告
     *
     * @param projectId 项目ID
     * @param templateCode 模板编码
     * @return Word 文档字节数组
     */
    byte[] generateProjectReport(Long projectId, String templateCode);

    /**
     * 生成轮次渗透测试报告
     *
     * @param roundId 轮次ID
     * @param templateCode 模板编码
     * @return Word 文档字节数组
     */
    byte[] generateRoundPentestReport(Long roundId, String templateCode);

    /**
     * 生成轮次复测报告
     *
     * @param roundId 轮次ID
     * @param templateCode 模板编码
     * @return Word 文档字节数组
     */
    byte[] generateRoundRetestReport(Long roundId, String templateCode);

}
