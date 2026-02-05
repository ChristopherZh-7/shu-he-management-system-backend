package cn.shuhe.system.module.project.dal.dataobject;

import cn.shuhe.system.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 员工工作排期 DO
 * 
 * 用于记录员工的工作安排，支持：
 * 1. 查看员工空置情况
 * 2. 跨部门服务申请时的排队机制
 * 3. 工作负载管理
 */
@TableName("employee_schedule")
@KeySequence("employee_schedule_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeScheduleDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 员工ID
     */
    private Long userId;

    /**
     * 员工姓名（快照）
     */
    private String userName;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 部门名称（快照）
     */
    private String deptName;

    // ========== 关联信息 ==========

    /**
     * 关联服务申请ID
     */
    private Long launchId;

    /**
     * 关联轮次ID
     */
    private Long roundId;

    /**
     * 关联服务项ID
     */
    private Long serviceItemId;

    /**
     * 关联项目ID
     */
    private Long projectId;

    // ========== 排期信息 ==========

    /**
     * 状态：0排队中 1进行中 2已完成 3已取消
     */
    private Integer status;

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

    // ========== 排队信息 ==========

    /**
     * 排队顺序（排队中时有效）
     */
    private Integer queueOrder;

    /**
     * 期望开始时间（申请人填写）
     */
    private LocalDateTime expectedStartTime;

    // ========== 任务描述 ==========

    /**
     * 任务类型：cross_dept=跨部门服务 local=本部门服务
     */
    private String taskType;

    /**
     * 任务描述
     */
    private String taskDescription;

    // ========== 状态常量 ==========

    /**
     * 状态：排队中
     */
    public static final int STATUS_QUEUED = 0;

    /**
     * 状态：进行中
     */
    public static final int STATUS_IN_PROGRESS = 1;

    /**
     * 状态：已完成
     */
    public static final int STATUS_COMPLETED = 2;

    /**
     * 状态：已取消
     */
    public static final int STATUS_CANCELLED = 3;

    // ========== 任务类型常量 ==========

    /**
     * 任务类型：跨部门服务
     */
    public static final String TASK_TYPE_CROSS_DEPT = "cross_dept";

    /**
     * 任务类型：本部门服务
     */
    public static final String TASK_TYPE_LOCAL = "local";

}
