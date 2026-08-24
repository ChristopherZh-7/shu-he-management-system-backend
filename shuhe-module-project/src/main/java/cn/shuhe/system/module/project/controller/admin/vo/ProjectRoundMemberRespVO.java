package cn.shuhe.system.module.project.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 轮次角色与责任")
@Data
public class ProjectRoundMemberRespVO {
    private Long id;
    private Long roundId;
    private Long userId;
    private String userName;
    private Long userDeptId;
    private String roleType;
    private String responsibility;
    private String taskStatus;
    private Long assignedBy;
    private LocalDateTime completedAt;
    private String remark;
}
