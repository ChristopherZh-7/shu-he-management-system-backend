package cn.shuhe.system.module.system.api.cost;

/**
 * 外出费用 API 接口
 *
 * 流程：A部门找B部门要人 → B部门人员完成 → B部门负责人选择结算人 → 结算人填写金额
 */
public interface OutsideCostApi {

    /**
     * 创建外出费用记录（外出申请完成时调用）
     *
     * @param outsideRequestId 外出申请ID
     * @return 外出费用记录ID
     */
    Long createOutsideCostRecord(Long outsideRequestId);

    /**
     * 通过统一服务发起创建跨部门费用记录
     *
     * @param serviceLaunchId 服务发起ID
     * @return 费用记录ID
     */
    Long createCostRecordByServiceLaunch(Long serviceLaunchId);

    /**
     * 发送通知给目标部门负责人（B部门负责人）选择结算人
     *
     * @param outsideRequestId 外出申请ID
     */
    void sendAssignNotifyToTargetDeptLeader(Long outsideRequestId);

    /**
     * 发送通知给结算人填写金额
     *
     * @param outsideCostRecordId 外出费用记录ID
     */
    void sendFillNotifyToSettleUser(Long outsideCostRecordId);
}
