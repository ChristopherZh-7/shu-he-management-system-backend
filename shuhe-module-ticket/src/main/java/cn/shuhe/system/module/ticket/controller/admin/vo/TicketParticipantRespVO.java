package cn.shuhe.system.module.ticket.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 工单角色与责任分工")
@Data
public class TicketParticipantRespVO {

    private Long userId;
    private String userName;
    private Long userDeptId;

    @Schema(description = "primary_executor / collaborator / tech_reviewer")
    private String roleType;

    @Schema(description = "责任边界")
    private String responsibility;

    @Schema(description = "pending / working / submitted / completed")
    private String taskStatus;
}
