package cn.shuhe.system.module.project.dal.dataobject;

import cn.shuhe.system.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@TableName("project_round_retest_batch")
@KeySequence("project_round_retest_batch_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectRoundRetestBatchDO extends BaseDO {
    @TableId private Long id;
    private Long roundId;
    private Integer batchNo;
    private Long executorId;
    private String executorName;
    private LocalDateTime plannedTime;
    private String status;
    private String summary;
    private LocalDateTime completedAt;
}
