package cn.shuhe.system.module.project.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 外出人员 Response VO")
@Data
public class OutsideMemberRespVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "外出请求ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long requestId;

    @Schema(description = "外出人员ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long userId;

    @Schema(description = "人员姓名", example = "李四")
    private String userName;

    @Schema(description = "人员部门ID", example = "2")
    private Long userDeptId;

    @Schema(description = "部门名称", example = "安全运营部")
    private String userDeptName;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "确认状态：0未确认 1已确认 2已提交OA 3OA已通过", example = "0")
    private Integer confirmStatus;

    @Schema(description = "确认时间")
    private LocalDateTime confirmTime;

    @Schema(description = "完成状态：0未完成 1已完成（无附件） 2已完成（有附件）", example = "0")
    private Integer finishStatus;

    @Schema(description = "完成时间")
    private LocalDateTime finishTime;

    @Schema(description = "附件URL（多个附件用逗号分隔）", example = "https://example.com/file1.pdf,https://example.com/file2.pdf")
    private String attachmentUrl;

    @Schema(description = "完成备注", example = "已完成渗透测试，报告已上传")
    private String finishRemark;

}
