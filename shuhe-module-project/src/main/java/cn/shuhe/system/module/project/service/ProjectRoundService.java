package cn.shuhe.system.module.project.service;

import cn.shuhe.system.module.project.controller.admin.vo.ProjectRoundSaveReqVO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectRoundDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 项目轮次 Service 接口
 */
public interface ProjectRoundService {

    /**
     * 创建项目轮次
     *
     * @param createReqVO 创建信息
     * @return 轮次编号
     */
    Long createProjectRound(@Valid ProjectRoundSaveReqVO createReqVO);

    /**
     * 更新项目轮次
     *
     * @param updateReqVO 更新信息
     */
    void updateProjectRound(@Valid ProjectRoundSaveReqVO updateReqVO);

    /**
     * 删除项目轮次
     *
     * @param id 轮次编号
     */
    void deleteProjectRound(Long id);

    /**
     * 获得项目轮次
     *
     * @param id 轮次编号
     * @return 项目轮次
     */
    ProjectRoundDO getProjectRound(Long id);

    /**
     * 获得项目的轮次列表
     *
     * @param projectId 项目编号
     * @return 轮次列表
     */
    List<ProjectRoundDO> getProjectRoundList(Long projectId);

    /**
     * 获得服务项的轮次列表
     *
     * @param serviceItemId 服务项编号
     * @return 轮次列表
     */
    List<ProjectRoundDO> getProjectRoundListByServiceItemId(Long serviceItemId);

    /**
     * 更新轮次状态
     *
     * @param id 轮次编号
     * @param status 状态
     */
    void updateRoundStatus(Long id, Integer status);

    /**
     * 更新轮次进度
     *
     * @param id 轮次编号
     * @param progress 进度
     */
    void updateRoundProgress(Long id, Integer progress);

    /**
     * 根据服务项创建新的轮次（工作流审批通过后调用）
     *
     * @param serviceItemId 服务项ID
     * @param processInstanceId 工作流实例ID
     * @return 轮次编号
     */
    Long createRoundByServiceItem(Long serviceItemId, String processInstanceId);

    /**
     * 根据服务项创建执行轮次（带计划时间）
     *
     * @param serviceItemId 服务项ID
     * @param processInstanceId 工作流实例ID
     * @param planStartTime 计划开始时间
     * @param planEndTime 计划结束时间
     * @return 轮次编号
     */
    Long createRoundByServiceItem(Long serviceItemId, String processInstanceId, 
            java.time.LocalDateTime planStartTime, java.time.LocalDateTime planEndTime);

    /**
     * 获得服务项的轮次列表
     *
     * @param serviceItemId 服务项ID
     * @return 轮次列表
     */
    List<ProjectRoundDO> getRoundListByServiceItemId(Long serviceItemId);

    /**
     * 获得服务项的轮次数量
     *
     * @param serviceItemId 服务项ID
     * @return 轮次数量
     */
    int getRoundCountByServiceItemId(Long serviceItemId);

}
