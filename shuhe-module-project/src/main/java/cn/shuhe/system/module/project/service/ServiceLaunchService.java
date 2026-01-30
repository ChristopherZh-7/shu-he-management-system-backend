package cn.shuhe.system.module.project.service;

import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.module.project.controller.admin.vo.ServiceLaunchPageReqVO;
import cn.shuhe.system.module.project.controller.admin.vo.ServiceLaunchRespVO;
import cn.shuhe.system.module.project.controller.admin.vo.ServiceLaunchSaveReqVO;
import cn.shuhe.system.module.project.dal.dataobject.ServiceLaunchDO;

import java.util.List;
import java.util.Map;

/**
 * 统一服务发起 Service 接口
 */
public interface ServiceLaunchService {

    /**
     * 创建统一服务发起
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createServiceLaunch(ServiceLaunchSaveReqVO createReqVO);

    /**
     * 更新统一服务发起
     *
     * @param updateReqVO 更新信息
     */
    void updateServiceLaunch(ServiceLaunchSaveReqVO updateReqVO);

    /**
     * 更新流程实例ID
     *
     * @param id 编号
     * @param processInstanceId 流程实例ID
     */
    void updateProcessInstanceId(Long id, String processInstanceId);

    /**
     * 设置执行人
     *
     * @param id 编号
     * @param executorIds 执行人ID列表
     */
    void setExecutors(Long id, List<Long> executorIds);

    /**
     * 删除统一服务发起
     *
     * @param id 编号
     */
    void deleteServiceLaunch(Long id);

    /**
     * 获得统一服务发起
     *
     * @param id 编号
     * @return 统一服务发起
     */
    ServiceLaunchDO getServiceLaunch(Long id);

    /**
     * 获得统一服务发起详情（带关联信息）
     *
     * @param id 编号
     * @return 统一服务发起详情
     */
    ServiceLaunchRespVO getServiceLaunchDetail(Long id);

    /**
     * 获得统一服务发起分页
     *
     * @param pageReqVO 分页查询
     * @return 统一服务发起分页
     */
    PageResult<ServiceLaunchRespVO> getServiceLaunchPage(ServiceLaunchPageReqVO pageReqVO);

    /**
     * 获得我发起的统一服务发起分页
     *
     * @param pageReqVO 分页查询
     * @return 统一服务发起分页
     */
    PageResult<ServiceLaunchRespVO> getMyServiceLaunchPage(ServiceLaunchPageReqVO pageReqVO);

    /**
     * 获取可发起服务的合同列表
     *
     * @return 合同列表
     */
    List<Map<String, Object>> getContractListForLaunch();

    /**
     * 根据合同获取服务项列表
     *
     * @param contractId 合同ID
     * @return 服务项列表
     */
    List<Map<String, Object>> getServiceItemListByContract(Long contractId);

    /**
     * 获取可选的执行部门列表
     *
     * @return 部门列表
     */
    List<Map<String, Object>> getDeptList();

    /**
     * 根据部门ID获取用户列表（包含子部门）
     * 用于审批页面选择执行人
     *
     * @param deptId 部门ID
     * @return 用户列表
     */
    List<Map<String, Object>> getUserListByDept(Long deptId);

    /**
     * 获取所有安全部门的负责人列表
     * 用于"代他人发起"功能
     *
     * @return 部门负责人列表
     */
    List<Map<String, Object>> getDeptLeaderList();

    /**
     * 处理审批通过
     *
     * @param id 编号
     * @param executorUserIds 执行人员ID列表（可选）
     * @return 创建的轮次ID
     */
    Long handleApproved(Long id, List<Long> executorUserIds);

    /**
     * 处理审批拒绝
     *
     * @param id 编号
     */
    void handleRejected(Long id);

    /**
     * 处理审批取消
     *
     * @param id 编号
     */
    void handleCancelled(Long id);

    // ==================== 执行人相关 ====================

    /**
     * 获取服务发起的执行人列表
     *
     * @param launchId 服务发起ID
     * @return 执行人列表
     */
    List<cn.shuhe.system.module.project.dal.dataobject.ServiceLaunchMemberDO> getLaunchMembers(Long launchId);

    /**
     * 获取执行人详情
     *
     * @param memberId 执行人记录ID
     * @return 执行人信息
     */
    cn.shuhe.system.module.project.dal.dataobject.ServiceLaunchMemberDO getLaunchMember(Long memberId);

    /**
     * 完成外出任务
     *
     * @param memberId 执行人记录ID
     * @param hasAttachment 是否有附件
     * @param attachmentUrl 附件URL
     * @param remark 备注
     */
    void finishLaunchMember(Long memberId, Boolean hasAttachment, String attachmentUrl, String remark);

    /**
     * 获取外出服务发起分页（isOutside=true）
     *
     * @param pageReqVO 分页查询
     * @return 外出服务发起分页
     */
    PageResult<ServiceLaunchRespVO> getOutsideServiceLaunchPage(ServiceLaunchPageReqVO pageReqVO);

}
