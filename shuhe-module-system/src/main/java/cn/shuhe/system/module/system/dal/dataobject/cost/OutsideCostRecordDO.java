package cn.shuhe.system.module.system.dal.dataobject.cost;

import cn.shuhe.system.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 外出费用记录 DO
 *
 * 流程：A部门找B部门要人 → B部门人员完成 → B部门负责人选择结算人 → 结算人填写金额
 */
@TableName("outside_cost_record")
@Data
@EqualsAndHashCode(callSuper = true)
public class OutsideCostRecordDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 外出申请ID（旧版，已废弃）
     */
    private Long outsideRequestId;

    /**
     * 服务发起ID（统一服务发起）
     */
    private Long serviceLaunchId;

    /**
     * 合同ID
     */
    private Long contractId;

    /**
     * 合同编号（快照）
     */
    private String contractNo;

    /**
     * 合同名称（快照）
     */
    private String contractName;

    /**
     * 服务项ID
     */
    private Long serviceItemId;

    /**
     * 服务项名称（快照）
     */
    private String serviceItemName;

    /**
     * 发起部门ID（A部门，找人的部门）
     */
    private Long requestDeptId;

    /**
     * 发起部门名称（快照）
     */
    private String requestDeptName;

    /**
     * 发起人ID
     */
    private Long requestUserId;

    /**
     * 发起人姓名（快照）
     */
    private String requestUserName;

    /**
     * 目标部门ID（B部门，被借调的部门）
     */
    private Long targetDeptId;

    /**
     * 目标部门名称（快照）
     */
    private String targetDeptName;

    /**
     * 费用金额
     */
    private BigDecimal amount;

    /**
     * 结算人ID（找谁要钱）
     */
    private Long settleUserId;

    /**
     * 结算人姓名（快照）
     */
    private String settleUserName;

    /**
     * 结算人部门ID
     */
    private Long settleDeptId;

    /**
     * 结算人部门名称（快照）
     */
    private String settleDeptName;

    /**
     * 指派人ID（B部门负责人，选择结算人的人）
     */
    private Long assignUserId;

    /**
     * 指派人姓名（快照）
     */
    private String assignUserName;

    /**
     * 指派时间
     */
    private LocalDateTime assignTime;

    /**
     * 填写人ID（结算人填写金额）
     */
    private Long fillUserId;

    /**
     * 填写人姓名（快照）
     */
    private String fillUserName;

    /**
     * 填写时间
     */
    private LocalDateTime fillTime;

    /**
     * 状态：0-待指派结算人 1-待填写金额 2-已完成
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 状态常量
     */
    public static final int STATUS_PENDING_ASSIGN = 0;  // 待指派结算人（B部门负责人操作）
    public static final int STATUS_PENDING_FILL = 1;    // 待填写金额（结算人操作）
    public static final int STATUS_COMPLETED = 2;       // 已完成
}
