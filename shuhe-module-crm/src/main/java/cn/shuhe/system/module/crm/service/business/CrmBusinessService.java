package cn.shuhe.system.module.crm.service.business;

import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.module.crm.controller.admin.business.vo.business.CrmBusinessEarlyInvestmentSubmitReqVO;
import cn.shuhe.system.module.crm.controller.admin.business.vo.business.CrmBusinessPageReqVO;
import cn.shuhe.system.module.crm.controller.admin.business.vo.business.CrmBusinessSaveReqVO;
import cn.shuhe.system.module.crm.controller.admin.business.vo.business.CrmBusinessTransferReqVO;
import cn.shuhe.system.module.crm.controller.admin.business.vo.business.CrmBusinessUpdateStatusReqVO;
import cn.shuhe.system.module.crm.controller.admin.statistics.vo.funnel.CrmStatisticsFunnelReqVO;
import cn.shuhe.system.module.crm.dal.dataobject.business.CrmBusinessDO;
import cn.shuhe.system.module.crm.dal.dataobject.contact.CrmContactDO;
import cn.shuhe.system.module.crm.dal.dataobject.customer.CrmCustomerDO;
import jakarta.validation.Valid;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static cn.shuhe.system.framework.common.util.collection.CollectionUtils.convertMap;

/**
 * 商机 Service 接口
 *
 * @author ljlleo
 */
public interface CrmBusinessService {

    /**
     * 创建商机
     *
     * @param createReqVO 创建信息
     * @param userId      用户编号
     * @return 编号
     */
    Long createBusiness(@Valid CrmBusinessSaveReqVO createReqVO, Long userId);

    /**
     * 更新商机
     *
     * @param updateReqVO 更新信息
     */
    void updateBusiness(@Valid CrmBusinessSaveReqVO updateReqVO);

    /**
     * 提交商机审核（启动逐级 BPM 审批流程）
     *
     * @param id     商机编号
     * @param userId 用户编号（发起人）
     */
    void submitBusinessAudit(Long id, Long userId);

    /**
     * 计算逐级审批链路（从发起人部门向上直到总经办）
     *
     * @param userId 发起人用户编号
     * @return 有序的审批人 ID 列表
     */
    List<Long> calculateApprovalChain(Long userId);

    /**
     * 更新商机审批状态（由 BPM 事件监听器回调）
     *
     * @param id        商机编号
     * @param bpmResult BPM 审批结果（2=通过/3=驳回/4=取消）
     */
    void updateBusinessAuditStatus(Long id, Integer bpmResult);

    /**
     * 提交提前投入审批
     * 商机审批通过后，可发起提前投入审批，审批通过后自动创建项目
     *
     * @param reqVO  提前投入申请表单（人员、费用、工作内容等）
     * @param userId 发起人用户编号
     */
    void submitEarlyInvestment(@jakarta.validation.Valid CrmBusinessEarlyInvestmentSubmitReqVO reqVO, Long userId);

    /**
     * 更新提前投入审批状态（由 BPM 事件监听器回调）
     * 审批通过后自动创建项目
     *
     * @param businessId 商机编号
     * @param bpmResult  BPM 审批结果
     */
    void updateEarlyInvestmentAuditStatus(Long businessId, Integer bpmResult);

    /**
     * 根据商机信息创建项目（提前投入审批通过 或 合同签订时调用）
     *
     * @param businessId 商机编号
     * @param contractId 合同编号（可为空，合同签订时传入）
     * @param contractNo 合同编号字符串（可为空）
     */
    void createProjectFromBusiness(Long businessId, Long contractId, String contractNo);

    /**
     * 将指定的部门金额分配同步写入 contract_dept_allocation 表
     * 优先使用合同自身的分配，而非商机的分配
     *
     * @param contractId   合同ID
     * @param contractNo   合同编号
     * @param customerName 客户名称
     * @param allocations  部门金额分配列表
     */
    void syncContractDeptAllocations(Long contractId, String contractNo, String customerName,
                                     List<cn.shuhe.system.module.crm.dal.dataobject.business.CrmBusinessDO.DeptAllocation> allocations);

    /**
     * 更新商机相关跟进信息
     *
     * @param id                 编号
     * @param contactNextTime    下次联系时间
     * @param contactLastContent 最后联系内容
     */
    void updateBusinessFollowUp(Long id, LocalDateTime contactNextTime, String contactLastContent);

    /**
     * 更新商机的下次联系时间
     *
     * @param ids             编号数组
     * @param contactNextTime 下次联系时间
     */
    void updateBusinessContactNextTime(Collection<Long> ids, LocalDateTime contactNextTime);

    /**
     * 更新商机的结束状态（赢单/输单/无效）
     *
     * @param reqVO 更新请求
     */
    void updateBusinessStatus(CrmBusinessUpdateStatusReqVO reqVO);

    /**
     * 删除商机
     *
     * @param id 编号
     */
    void deleteBusiness(Long id);

    /**
     * 商机转移
     *
     * @param reqVO  请求
     * @param userId 用户编号
     */
    void transferBusiness(CrmBusinessTransferReqVO reqVO, Long userId);

    /**
     * 获得商机
     *
     * @param id 编号
     * @return 商机
     */
    CrmBusinessDO getBusiness(Long id);

    /**
     * 校验商机是否有效
     *
     * @param id 编号
     * @return 商机
     */
    CrmBusinessDO validateBusiness(Long id);

    /**
     * 获得商机列表
     *
     * @param ids 编号
     * @return 商机列表
     */
    List<CrmBusinessDO> getBusinessList(Collection<Long> ids);

    /**
     * 获得商机 Map
     *
     * @param ids 编号
     * @return 商机 Map
     */
    default Map<Long, CrmBusinessDO> getBusinessMap(Collection<Long> ids) {
        return convertMap(getBusinessList(ids), CrmBusinessDO::getId);
    }

    /**
     * 获得商机分页
     *
     * 数据权限：基于 {@link CrmBusinessDO}
     *
     * @param pageReqVO 分页查询
     * @param userId    用户编号
     * @return 商机分页
     */
    PageResult<CrmBusinessDO> getBusinessPage(CrmBusinessPageReqVO pageReqVO, Long userId);

    /**
     * 获得商机分页，基于指定客户
     *
     * 数据权限：基于 {@link CrmCustomerDO} 读取
     *
     * @param pageReqVO 分页查询
     * @return 商机分页
     */
    PageResult<CrmBusinessDO> getBusinessPageByCustomerId(CrmBusinessPageReqVO pageReqVO);

    /**
     * 获得商机分页，基于指定联系人
     *
     * 数据权限：基于 {@link CrmContactDO} 读取
     *
     * @param pageReqVO 分页参数
     * @return 商机分页
     */
    PageResult<CrmBusinessDO> getBusinessPageByContact(CrmBusinessPageReqVO pageReqVO);

    /**
     * 获取关联客户的商机数量
     *
     * @param customerId 客户编号
     * @return 数量
     */
    Long getBusinessCountByCustomerId(Long customerId);

    /**
     * 获得商机列表
     *
     * @param customerId  客户编号
     * @param ownerUserId 负责人编号
     * @return 商机列表
     */
    List<CrmBusinessDO> getBusinessListByCustomerIdOwnerUserId(Long customerId, Long ownerUserId);

    /**
     * 获得商机分页，目前用于【数据统计】
     *
     * @param pageVO 请求
     * @return 商机分页
     */
    PageResult<CrmBusinessDO> getBusinessPageByDate(CrmStatisticsFunnelReqVO pageVO);

    /**
     * 向商机关联的钉钉群发送纯通知消息（不含审批操作链接）
     * 用于合同创建、项目进展等事件通知，消息发送到商机审批时建立的群里
     *
     * @param businessId 商机ID
     * @param message    消息正文（Markdown 格式）
     * @param title      消息标题
     */
    void sendBusinessGroupNotification(Long businessId, String message, String title);

    /**
     * 向商机关联的钉钉群发送带操作按钮的消息
     * 用于合同确认等需要在群内操作的场景
     *
     * @param businessId  商机ID
     * @param message     消息正文（Markdown 格式，可包含 markdown 链接）
     * @param title       消息标题
     * @param actionLabel 操作按钮文字（fallback 工作通知时使用）
     * @param actionUrl   操作按钮跳转URL（fallback 工作通知时使用）
     */
    void sendBusinessGroupNotificationWithAction(Long businessId, String message, String title,
                                                 String actionLabel, String actionUrl);

}
