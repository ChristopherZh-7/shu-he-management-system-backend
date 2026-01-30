package cn.shuhe.system.module.project.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 服务发起执行人 Response VO")
@Data
public class ServiceLaunchMemberRespVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "服务发起ID")
    private Long launchId;

    @Schema(description = "执行人ID")
    private Long userId;

    @Schema(description = "执行人姓名")
    private String userName;

    @Schema(description = "执行人部门ID")
    private Long userDeptId;

    @Schema(description = "执行人部门名称")
    private String userDeptName;

    @Schema(description = "确认状态：0未确认 1已确认 2已提交OA 3OA已通过")
    private Integer confirmStatus;

    @Schema(description = "确认时间")
    private LocalDateTime confirmTime;

    @Schema(description = "钉钉OA审批实例ID")
    private String oaProcessInstanceId;

    @Schema(description = "完成状态：0未完成 1已完成（无附件） 2已完成（有附件）")
    private Integer finishStatus;

    @Schema(description = "完成时间")
    private LocalDateTime finishTime;

    @Schema(description = "附件URL")
    private String attachmentUrl;

    @Schema(description = "完成备注")
    private String finishRemark;

}
