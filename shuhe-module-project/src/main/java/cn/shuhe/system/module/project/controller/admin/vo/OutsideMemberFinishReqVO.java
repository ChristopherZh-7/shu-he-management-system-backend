package cn.shuhe.system.module.project.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 外出人员确认完成 Request VO")
@Data
public class OutsideMemberFinishReqVO {

    @Schema(description = "外出人员记录ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "外出人员记录ID不能为空")
    private Long memberId;

    @Schema(description = "是否有附件", example = "true")
    private Boolean hasAttachment;

    @Schema(description = "附件URL（多个附件用逗号分隔）", example = "https://example.com/file1.pdf,https://example.com/file2.pdf")
    private String attachmentUrl;

    @Schema(description = "完成备注", example = "已完成渗透测试，报告已上传")
    private String finishRemark;

}
