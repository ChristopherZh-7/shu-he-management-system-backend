package cn.shuhe.system.module.project.service;

import cn.shuhe.system.module.project.controller.admin.vo.ProjectRoundTargetSaveReqVO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectRoundTargetDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 轮次测试目标 Service 接口
 */
public interface ProjectRoundTargetService {

    /**
     * 创建测试目标
     *
     * @param createReqVO 创建信息
     * @return 目标ID
     */
    Long createTarget(@Valid ProjectRoundTargetSaveReqVO createReqVO);

    /**
     * 更新测试目标
     *
     * @param updateReqVO 更新信息
     */
    void updateTarget(@Valid ProjectRoundTargetSaveReqVO updateReqVO);

    /**
     * 删除测试目标
     *
     * @param id 目标ID
     */
    void deleteTarget(Long id);

    /**
     * 获取测试目标
     *
     * @param id 目标ID
     * @return 测试目标
     */
    ProjectRoundTargetDO getTarget(Long id);

    /**
     * 获取轮次的测试目标列表
     *
     * @param roundId 轮次ID
     * @return 测试目标列表
     */
    List<ProjectRoundTargetDO> getTargetListByRoundId(Long roundId);

    /**
     * 批量创建测试目标
     *
     * @param roundId 轮次ID
     * @param targets 目标列表
     */
    void batchCreateTargets(Long roundId, List<ProjectRoundTargetSaveReqVO> targets);

}
