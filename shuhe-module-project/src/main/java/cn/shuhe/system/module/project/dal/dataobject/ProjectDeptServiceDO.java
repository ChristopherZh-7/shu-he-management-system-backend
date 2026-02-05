package cn.shuhe.system.module.project.dal.dataobject;

import cn.shuhe.system.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 项目-部门服务单 DO
 * 
 * 解决一个项目被多个部门服务时，负责人、状态、进度等字段冲突的问题。
 * 每个被通知的部门会有一条独立的部门服务单记录，可以独立管理状态和负责人。
 * 
 * 层级关系：合同 → 项目 → 部门服务单（1-3个） → 服务项 → 轮次
 */
@TableName(value = "project_dept_service", autoResultMap = true)
@KeySequence("project_dept_service_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDeptServiceDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    // ========== 关联信息 ==========

    /**
     * 所属项目ID
     * 关联 {@link ProjectDO#getId()}
     */
    private Long projectId;

    /**
     * CRM合同ID
     */
    private Long contractId;

    /**
     * 合同编号
     */
    private String contractNo;

    /**
     * CRM客户ID
     */
    private Long customerId;

    /**
     * 客户名称
     */
    private String customerName;

    // ========== 部门信息 ==========

    /**
     * 所属部门ID（领取后填充）
     */
    private Long deptId;

    /**
     * 部门名称
     */
    private String deptName;

    /**
     * 部门类型
     * 1-安全服务 2-安全运营 3-数据安全
     */
    private Integer deptType;

    // ========== 领取信息 ==========

    /**
     * 领取人ID
     */
    private Long claimUserId;

    /**
     * 领取人姓名
     */
    private String claimUserName;

    /**
     * 领取时间
     */
    private LocalDateTime claimTime;

    /**
     * 是否已领取
     * 0-否 1-是
     */
    private Boolean claimed;

    // ========== 负责人（独立于其他部门） ==========

    /**
     * 负责人ID列表（JSON数组）
     * 注：对于安全服务(deptType=1)，此字段不再使用，改用 onsiteManagerIds 和 secondLineManagerIds
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> managerIds;

    /**
     * 负责人姓名列表（JSON数组）
     * 注：对于安全服务(deptType=1)，此字段不再使用，改用 onsiteManagerNames 和 secondLineManagerNames
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> managerNames;

    // ========== 安全服务/数据安全专用：驻场和二线负责人 ==========

    /**
     * 驻场负责人ID列表（JSON数组）
     * 安全服务(deptType=1)和数据安全(deptType=3)使用
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> onsiteManagerIds;

    /**
     * 驻场负责人姓名列表（JSON数组）
     * 安全服务(deptType=1)和数据安全(deptType=3)使用
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> onsiteManagerNames;

    /**
     * 二线负责人ID列表（JSON数组）
     * 安全服务(deptType=1)和数据安全(deptType=3)使用
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> secondLineManagerIds;

    /**
     * 二线负责人姓名列表（JSON数组）
     * 安全服务(deptType=1)和数据安全(deptType=3)使用
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> secondLineManagerNames;

    // ========== 状态和进度（每个部门独立） ==========

    /**
     * 状态
     * 0-待领取 1-待开始 2-进行中 3-已暂停 4-已完成 5-已取消
     */
    private Integer status;

    /**
     * 进度百分比 0-100
     */
    private Integer progress;

    // ========== 时间信息 ==========

    /**
     * 计划开始时间
     */
    private LocalDateTime planStartTime;

    /**
     * 计划结束时间
     */
    private LocalDateTime planEndTime;

    /**
     * 实际开始时间
     */
    private LocalDateTime actualStartTime;

    /**
     * 实际结束时间
     */
    private LocalDateTime actualEndTime;

    // ========== 扩展字段 ==========

    /**
     * 描述
     */
    private String description;

    /**
     * 备注
     */
    private String remark;

    // ========== 实际执行部门（根据负责人所在部门） ==========

    /**
     * 实际执行部门ID（根据负责人所在部门确定）
     * 当设置负责人时，会根据负责人的部门ID更新此字段
     * 用于合同收入分配时显示实际的子部门
     */
    private Long actualDeptId;

    /**
     * 实际执行部门名称
     */
    private String actualDeptName;

}
