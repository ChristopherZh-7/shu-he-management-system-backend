package cn.shuhe.system.module.project.dal.dataobject;

import cn.shuhe.system.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 轮次测试目标 DO
 * 
 * 每个轮次可以有多个测试目标（如：官网、APP、小程序等）
 */
@TableName("project_round_target")
@KeySequence("project_round_target_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectRoundTargetDO extends BaseDO {

    /**
     * 目标ID
     */
    @TableId
    private Long id;

    /**
     * 轮次ID
     */
    private Long roundId;

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 目标名称（如：官网、APP、小程序）
     */
    private String name;

    /**
     * 目标地址/URL
     */
    private String url;

    /**
     * 目标类型：web/app/miniprogram/api/other
     */
    private String type;

    /**
     * 目标描述
     */
    private String description;

    /**
     * 排序
     */
    private Integer sort;

}
