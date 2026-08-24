package cn.shuhe.system.module.project.controller.admin.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProjectRoundRetestBatchSaveReqVO {
    @NotNull(message = "轮次ID不能为空")
    private Long roundId;
    private Long executorId;
    private String executorName;
    private LocalDateTime plannedTime;
    private String summary;
}
