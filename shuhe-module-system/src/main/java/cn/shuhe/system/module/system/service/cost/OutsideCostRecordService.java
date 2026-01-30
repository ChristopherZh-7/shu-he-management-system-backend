package cn.shuhe.system.module.system.service.cost;

import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.module.system.controller.admin.cost.vo.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 外出费用记录 Service 接口
 *
 * 流程：A部门找B部门要人 → B部门人员完成 → B部门负责人选择结算人 → 结算人填写金额
 */
public interface OutsideCostRecordService {

    /**
     * 分页查询外出费用记录
     */
    PageResult<OutsideCostRecordRespVO> getOutsideCostRecordPage(OutsideCostRecordPageReqVO reqVO);

    /**
     * 获取外出费用记录详情
     */
    OutsideCostRecordRespVO getOutsideCostRecord(Long id);

    /**
     * 指派结算人（B部门负责人操作）
     */
    void assignSettleUser(OutsideCostAssignReqVO reqVO);

    /**
     * 填写外出费用金额（结算人操作）
     */
    void fillOutsideCost(OutsideCostFillReqVO reqVO);

    /**
     * 创建外出费用记录（外出申请完成时调用）
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
     * 获取可选的结算人列表
     */
    List<SettleUserVO> getSettleUserList(Long outsideCostRecordId);

    /**
     * 统计合同的外出费用总额
     */
    BigDecimal sumAmountByContractId(Long contractId);

    /**
     * 统计合同的外出费用笔数
     */
    Integer countByContractId(Long contractId);
}
