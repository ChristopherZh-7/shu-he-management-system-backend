package cn.shuhe.system.module.project.dal.dataobject;

import cn.shuhe.system.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.*;

import java.util.List;

/**
 * 项目 DO（顶层项目）
 * 
 * 项目是第一层级，一个项目下可以有多个服务项（ServiceItemDO）
 */
@TableName(value = "project", autoResultMap = true)
@KeySequence("project_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDO extends BaseDO {

    /**
     * 项目 ID
     */
    @TableId
    private Long id;

    /**
     * 项目名称
     */
    private String name;

    /**
     * 项目编号
     */
    private String code;

    /**
     * 部门类型
     * 1-安全服务 2-安全运营 3-数据安全
     */
    private Integer deptType;

    // ========== 客户信息 ==========

    /**
     * CRM 客户 ID
     */
    private Long customerId;

    /**
     * 客户名称
     */
    private String customerName;

    /**
     * CRM 合同 ID
     */
    private Long contractId;

    /**
     * 合同编号
     */
    private String contractNo;

    // ========== 项目负责人 ==========

    /**
     * 项目负责人ID列表（JSON数组）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> managerIds;

    /**
     * 项目负责人姓名列表（JSON数组）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> managerNames;

    // ========== 状态 ==========

    /**
     * 项目状态
     * 0-草稿 1-进行中 2-已完成
     */
    private Integer status;

    /**
     * 项目描述
     */
    private String description;

}
