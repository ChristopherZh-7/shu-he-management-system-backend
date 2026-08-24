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

@TableName("project_round_member")
@KeySequence("project_round_member_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectRoundMemberDO extends BaseDO {
    @TableId
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
