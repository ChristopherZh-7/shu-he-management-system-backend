package cn.shuhe.system.module.project.service;

import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.module.project.controller.admin.vo.OutsideRequestPageReqVO;
import cn.shuhe.system.module.project.controller.admin.vo.OutsideRequestRespVO;
import cn.shuhe.system.module.project.controller.admin.vo.OutsideRequestSaveReqVO;
import cn.shuhe.system.module.project.dal.dataobject.OutsideMemberDO;
import cn.shuhe.system.module.project.dal.dataobject.OutsideRequestDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 外出请求 Service 接口
 */
public interface OutsideRequestService {

    /**
     * 创建外出请求
     *
     * @param createReqVO 创建信息
     * @return 外出请求ID
     */
    Long createOutsideRequest(@Valid OutsideRequestSaveReqVO createReqVO);

    /**
     * 更新外出请求
     *
     * @param updateReqVO 更新信息
     */
    void updateOutsideRequest(@Valid OutsideRequestSaveReqVO updateReqVO);

    /**
     * 删除外出请求
     *
     * @param id 外出请求ID
     */
    void deleteOutsideRequest(Long id);

    /**
     * 获得外出请求
     *
     * @param id 外出请求ID
     * @return 外出请求
     */
    OutsideRequestDO getOutsideRequest(Long id);

    /**
     * 获得外出请求详情（包含关联信息）
     *
     * @param id 外出请求ID
     * @return 外出请求详情
     */
    OutsideRequestRespVO getOutsideRequestDetail(Long id);

    /**
     * 获得外出请求分页
     *
     * @param pageReqVO 分页查询条件
     * @return 外出请求分页
     */
    PageResult<OutsideRequestRespVO> getOutsideRequestPage(OutsideRequestPageReqVO pageReqVO);

    /**
     * 获得项目的外出请求列表
     *
     * @param projectId 项目ID
     * @return 外出请求列表
     */
    List<OutsideRequestDO> getOutsideRequestListByProjectId(Long projectId);

    /**
     * 获得服务项的外出请求列表
     *
     * @param serviceItemId 服务项ID
     * @return 外出请求列表
     */
    List<OutsideRequestDO> getOutsideRequestListByServiceItemId(Long serviceItemId);

    /**
     * 获得服务项的外出请求列表（包含完整关联信息）
     *
     * @param serviceItemId 服务项ID
     * @return 外出请求详情列表
     */
    List<OutsideRequestRespVO> getOutsideRequestListByServiceItemIdWithDetail(Long serviceItemId);

    /**
     * 更新外出请求状态
     *
     * @param id 外出请求ID
     * @param status 状态
     */
    void updateOutsideRequestStatus(Long id, Integer status);

    /**
     * 根据流程实例ID获取外出请求
     *
     * @param processInstanceId 流程实例ID
     * @return 外出请求
     */
    OutsideRequestDO getOutsideRequestByProcessInstanceId(String processInstanceId);

    /**
     * 设置外出人员（审批通过时由目标部门负责人选择）
     *
     * @param requestId 外出请求ID
     * @param memberUserIds 外出人员ID列表
     */
    void setOutsideMembers(Long requestId, List<Long> memberUserIds);

    /**
     * 获得外出请求的人员列表
     *
     * @param requestId 外出请求ID
     * @return 外出人员列表
     */
    List<OutsideMemberDO> getOutsideMembers(Long requestId);

    /**
     * 统计服务项的外出次数
     *
     * @param serviceItemId 服务项ID
     * @return 外出次数
     */
    Long getOutsideCountByServiceItemId(Long serviceItemId);

    /**
     * 外出人员确认完成
     *
     * @param memberId 外出人员记录ID
     * @param hasAttachment 是否有附件
     * @param attachmentUrl 附件URL
     * @param finishRemark 完成备注
     */
    void finishOutsideMember(Long memberId, Boolean hasAttachment, String attachmentUrl, String finishRemark);

    /**
     * 获得外出人员记录
     *
     * @param memberId 外出人员记录ID
     * @return 外出人员记录
     */
    OutsideMemberDO getOutsideMember(Long memberId);

}
