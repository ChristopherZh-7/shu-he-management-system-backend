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

}
