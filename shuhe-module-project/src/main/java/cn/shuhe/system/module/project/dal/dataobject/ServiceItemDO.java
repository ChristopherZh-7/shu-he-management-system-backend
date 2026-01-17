package cn.shuhe.system.module.project.dal.dataobject;

import cn.shuhe.system.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 服务项 DO（原项目）
 * 
 * 服务项是第二层级，隶属于项目（ProjectDO），一个服务项下可以有多个轮次（ProjectRoundDO）
 */
@TableName("project_info")
@KeySequence("project_info_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceItemDO extends BaseDO {

    /**
     * 服务项 ID
     */
    @TableId
    private Long id;

    /**
     * 所属项目 ID
     * 
     * 关联 {@link ProjectDO#getId()}
     */
    private Long projectId;

    /**
     * 服务项编号
     */
    private String code;

    /**
     * 服务项名称
     */
    private String name;

    /**
     * 部门类型
     * 1-安全服务 2-安全运营 3-数据安全
     */
    private Integer deptType;

    /**
     * 服务类型（字典值）
     */
    private String serviceType;

    /**
     * 服务项描述
     */
    private String description;

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

    // ========== 人员信息 ==========

    /**
     * 服务项经理 ID
     */
    private Long managerId;

    /**
     * 服务项经理姓名
     */
    private String managerName;

    /**
     * 所属部门 ID
     */
    private Long deptId;

    // ========== 状态进度 ==========

    /**
     * 服务项状态
     * 0-草稿 1-进行中 2-已暂停 3-已完成 4-已取消
     */
    private Integer status;

    /**
     * 进度百分比 0-100
     */
    private Integer progress;

    /**
     * 优先级
     * 0-低 1-中 2-高
     */
    private Integer priority;

    // ========== 商务信息 ==========

    /**
     * 服务项金额
     */
    private BigDecimal amount;

    // ========== 扩展字段 ==========

    /**
     * 标签（JSON 数组）
     */
    private String tags;

    /**
     * 备注
     */
    private String remark;

}
