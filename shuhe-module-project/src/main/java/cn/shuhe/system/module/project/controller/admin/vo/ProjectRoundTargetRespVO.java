package cn.shuhe.system.module.project.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 轮次测试目标 Response VO")
@Data
public class ProjectRoundTargetRespVO {

    @Schema(description = "目标ID", example = "1")
    private Long id;

    @Schema(description = "轮次ID", example = "1")
    private Long roundId;

    @Schema(description = "项目ID", example = "1")
    private Long projectId;

    @Schema(description = "目标名称", example = "官网系统")
    private String name;

    @Schema(description = "目标地址/URL", example = "https://www.example.com")
    private String url;

    @Schema(description = "目标类型", example = "web")
    private String type;

    @Schema(description = "目标描述", example = "公司官网系统")
    private String description;

    @Schema(description = "排序", example = "1")
    private Integer sort;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    // ========== 扩展字段（漏洞统计）==========

    @Schema(description = "漏洞总数", example = "10")
    private Integer vulnerabilityCount;

    @Schema(description = "高危漏洞数", example = "2")
    private Integer highCount;

    @Schema(description = "中危漏洞数", example = "5")
    private Integer mediumCount;

    @Schema(description = "低危漏洞数", example = "3")
    private Integer lowCount;

}
