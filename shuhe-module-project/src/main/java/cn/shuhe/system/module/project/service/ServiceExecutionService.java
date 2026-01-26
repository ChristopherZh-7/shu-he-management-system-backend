package cn.shuhe.system.module.project.service;

import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.module.project.controller.admin.vo.ServiceExecutionPageReqVO;
import cn.shuhe.system.module.project.controller.admin.vo.ServiceExecutionRespVO;
import cn.shuhe.system.module.project.controller.admin.vo.ServiceExecutionSaveReqVO;
import cn.shuhe.system.module.project.dal.dataobject.ServiceExecutionDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 服务执行申请 Service 接口
 */
public interface ServiceExecutionService {

    /**
     * 创建服务执行申请
     *
     * @param createReqVO 创建信息
     * @return 申请ID
     */
    Long createServiceExecution(@Valid ServiceExecutionSaveReqVO createReqVO);

    /**
     * 更新服务执行申请（设置执行人等）
     *
     * @param updateReqVO 更新信息
     */
    void updateServiceExecution(@Valid ServiceExecutionSaveReqVO updateReqVO);

    /**
     * 设置执行人（审批时调用）
     *
     * @param id 申请ID
     * @param executorIds 执行人ID列表
     */
    void setExecutors(Long id, List<Long> executorIds);

    /**
     * 更新流程实例ID
     *
     * @param id 申请ID
     * @param processInstanceId 流程实例ID
     */
    void updateProcessInstanceId(Long id, String processInstanceId);

    /**
     * 更新状态
     *
     * @param id 申请ID
     * @param status 状态
     */
    void updateStatus(Long id, Integer status);

    /**
     * 审批通过处理（创建轮次）
     *
     * @param id 申请ID
     * @param processInstanceId 流程实例ID
     * @return 创建的轮次ID
     */
    Long handleApproved(Long id, String processInstanceId);

    /**
     * 删除服务执行申请
     *
     * @param id 申请ID
     */
    void deleteServiceExecution(Long id);

    /**
     * 获取服务执行申请
     *
     * @param id 申请ID
     * @return 申请信息
     */
    ServiceExecutionDO getServiceExecution(Long id);

    /**
     * 获取服务执行申请详情（带关联信息）
     *
     * @param id 申请ID
     * @return 申请详情
     */
    ServiceExecutionRespVO getServiceExecutionDetail(Long id);

    /**
     * 获取服务执行申请分页
     *
     * @param pageReqVO 分页请求
     * @return 分页结果
     */
    PageResult<ServiceExecutionRespVO> getServiceExecutionPage(ServiceExecutionPageReqVO pageReqVO);

    /**
     * 获取我发起的服务执行申请分页
     *
     * @param pageReqVO 分页请求
     * @return 分页结果
     */
    PageResult<ServiceExecutionRespVO> getMyServiceExecutionPage(ServiceExecutionPageReqVO pageReqVO);

    /**
     * 根据流程实例ID获取申请
     *
     * @param processInstanceId 流程实例ID
     * @return 申请信息
     */
    ServiceExecutionDO getByProcessInstanceId(String processInstanceId);

}
