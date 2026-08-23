package cn.shuhe.system.module.project.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 我的任务响应 VO")
@Data
public class MyTasksRespVO {

    @Schema(description = "项目列表")
    private List<TaskProject> projects;

    @Data
    public static class TaskProject {
        @Schema(description = "项目ID")
        private Long projectId;

        @Schema(description = "项目名称")
        private String projectName;

        @Schema(description = "客户名称")
        private String customerName;

        @Schema(description = "部门类型：1-安全服务 2-安全运营 3-数据安全")
        private Integer deptType;

        @Schema(description = "服务项列表")
        private List<TaskServiceItem> serviceItems;
    }

    @Data
    public static class TaskServiceItem {
        @Schema(description = "服务项ID")
        private Long serviceItemId;

        @Schema(description = "服务类型（字典值）")
        private String serviceType;

        @Schema(description = "服务事项所属部门类型：1-安全服务 2-安全运营 3-数据安全")
        private Integer deptType;

        @Schema(description = "服务类型中文 label（后端预翻译，前端可直接展示）")
        private String serviceTypeName;

        @Schema(description = "服务模式：1-驻场 2-二线")
        private Integer serviceMode;

        @Schema(description = "状态：0-草稿 1-进行中 2-已暂停 3-已完成 4-已取消")
        private Integer status;

        @Schema(description = "进度 0-100")
        private Integer progress;

        @Schema(description = "当前用户是否为服务项固定执行人")
        private Boolean isMyServiceItem;

        @Schema(description = "轮次列表")
        private List<TaskRound> rounds;

        @Schema(description = "工单列表")
        private List<TaskTicket> tickets;
    }

    @Data
    public static class TaskRound {
        @Schema(description = "轮次ID")
        private Long roundId;

        @Schema(description = "轮次名称")
        private String name;

        @Schema(description = "轮次序号")
        private Integer roundNo;

        @Schema(description = "状态：0-待执行 1-执行中 2-已完成 3-已取消")
        private Integer status;

        @Schema(description = "进度 0-100")
        private Integer progress;

        @Schema(description = "截止日期")
        private LocalDateTime deadline;

        @Schema(description = "执行人姓名")
        private String executorNames;

        @Schema(description = "当前用户是否是此轮次的执行人")
        private Boolean isMyRound;
    }

    @Data
    public static class TaskTicket {
        @Schema(description = "工单ID")
        private Long ticketId;

        @Schema(description = "工单编号")
        private String ticketNo;

        @Schema(description = "工单标题")
        private String title;

        @Schema(description = "状态")
        private Integer status;

        @Schema(description = "截止时间")
        private LocalDateTime dueTime;
    }
}
