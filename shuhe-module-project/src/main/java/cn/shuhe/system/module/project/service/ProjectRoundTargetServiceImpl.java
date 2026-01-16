package cn.shuhe.system.module.project.service;

import cn.shuhe.system.module.project.controller.admin.vo.ProjectRoundTargetSaveReqVO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectRoundDO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectRoundTargetDO;
import cn.shuhe.system.module.project.dal.mysql.ProjectRoundMapper;
import cn.shuhe.system.module.project.dal.mysql.ProjectRoundTargetMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static cn.shuhe.system.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.shuhe.system.module.project.enums.ErrorCodeConstants.*;

/**
 * 轮次测试目标 Service 实现类
 */
@Service
@Validated
public class ProjectRoundTargetServiceImpl implements ProjectRoundTargetService {

    @Resource
    private ProjectRoundTargetMapper targetMapper;

    @Resource
    private ProjectRoundMapper roundMapper;

    @Override
    public Long createTarget(ProjectRoundTargetSaveReqVO createReqVO) {
        // 校验轮次是否存在
        ProjectRoundDO round = roundMapper.selectById(createReqVO.getRoundId());
        if (round == null) {
            throw exception(PROJECT_ROUND_NOT_EXISTS);
        }

        // 获取最大排序值
        Integer maxSort = targetMapper.selectMaxSort(createReqVO.getRoundId());
        int newSort = createReqVO.getSort() != null ? createReqVO.getSort() : maxSort + 1;

        // 创建目标
        ProjectRoundTargetDO target = ProjectRoundTargetDO.builder()
                .roundId(createReqVO.getRoundId())
                .projectId(round.getProjectId())
                .name(createReqVO.getName())
                .url(createReqVO.getUrl())
                .type(createReqVO.getType())
                .description(createReqVO.getDescription())
                .sort(newSort)
                .build();

        targetMapper.insert(target);
        return target.getId();
    }

    @Override
    public void updateTarget(ProjectRoundTargetSaveReqVO updateReqVO) {
        // 校验存在
        validateTargetExists(updateReqVO.getId());

        // 更新
        ProjectRoundTargetDO updateObj = new ProjectRoundTargetDO();
        updateObj.setId(updateReqVO.getId());
        updateObj.setName(updateReqVO.getName());
        updateObj.setUrl(updateReqVO.getUrl());
        updateObj.setType(updateReqVO.getType());
        updateObj.setDescription(updateReqVO.getDescription());
        updateObj.setSort(updateReqVO.getSort());

        targetMapper.updateById(updateObj);
    }

    @Override
    public void deleteTarget(Long id) {
        // 校验存在
        validateTargetExists(id);
        // 删除
        targetMapper.deleteById(id);
    }

    private void validateTargetExists(Long id) {
        if (targetMapper.selectById(id) == null) {
            throw exception(PROJECT_ROUND_TARGET_NOT_EXISTS);
        }
    }

    @Override
    public ProjectRoundTargetDO getTarget(Long id) {
        return targetMapper.selectById(id);
    }

    @Override
    public List<ProjectRoundTargetDO> getTargetListByRoundId(Long roundId) {
        return targetMapper.selectListByRoundId(roundId);
    }

    @Override
    public void batchCreateTargets(Long roundId, List<ProjectRoundTargetSaveReqVO> targets) {
        // 校验轮次是否存在
        ProjectRoundDO round = roundMapper.selectById(roundId);
        if (round == null) {
            throw exception(PROJECT_ROUND_NOT_EXISTS);
        }

        int sort = 0;
        for (ProjectRoundTargetSaveReqVO target : targets) {
            ProjectRoundTargetDO targetDO = ProjectRoundTargetDO.builder()
                    .roundId(roundId)
                    .projectId(round.getProjectId())
                    .name(target.getName())
                    .url(target.getUrl())
                    .type(target.getType())
                    .description(target.getDescription())
                    .sort(sort++)
                    .build();
            targetMapper.insert(targetDO);
        }
    }

}
