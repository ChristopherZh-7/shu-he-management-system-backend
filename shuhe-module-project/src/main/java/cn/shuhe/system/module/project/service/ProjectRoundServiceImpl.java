package cn.shuhe.system.module.project.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import cn.shuhe.system.framework.common.util.object.BeanUtils;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectRoundSaveReqVO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectDO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectRoundDO;
import cn.shuhe.system.module.project.dal.mysql.ProjectMapper;
import cn.shuhe.system.module.project.dal.mysql.ProjectRoundMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static cn.shuhe.system.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.shuhe.system.module.project.enums.ErrorCodeConstants.*;

/**
 * 项目轮次 Service 实现类
 */
@Service
@Validated
public class ProjectRoundServiceImpl implements ProjectRoundService {

    @Resource
    private ProjectRoundMapper projectRoundMapper;

    @Resource
    private ProjectMapper projectMapper;

    @Override
    public Long createProjectRound(ProjectRoundSaveReqVO createReqVO) {
        // 校验项目是否存在
        ProjectDO project = projectMapper.selectById(createReqVO.getProjectId());
        if (project == null) {
            throw exception(PROJECT_NOT_EXISTS);
        }

        // 获取最大轮次序号
        Integer maxRoundNo = projectRoundMapper.selectMaxRoundNo(createReqVO.getProjectId());
        int newRoundNo = maxRoundNo + 1;

        // 转换并保存
        ProjectRoundDO round = new ProjectRoundDO();
        round.setProjectId(createReqVO.getProjectId());
        round.setName(createReqVO.getName());
        round.setPlanStartTime(createReqVO.getPlanStartTime());
        round.setPlanEndTime(createReqVO.getPlanEndTime());
        round.setRemark(createReqVO.getRemark());
        round.setRoundNo(newRoundNo);
        round.setStatus(0); // 默认待执行
        round.setProgress(0);

        // 处理执行人ID列表，转换为JSON字符串
        if (CollUtil.isNotEmpty(createReqVO.getExecutorIds())) {
            round.setExecutorIds(JSONUtil.toJsonStr(createReqVO.getExecutorIds()));
        }
        round.setExecutorNames(createReqVO.getExecutorNames());

        // 如果没有设置名称，自动生成
        if (round.getName() == null || round.getName().isEmpty()) {
            round.setName("第" + newRoundNo + "次执行");
        }

        projectRoundMapper.insert(round);
        return round.getId();
    }

    @Override
    public void updateProjectRound(ProjectRoundSaveReqVO updateReqVO) {
        // 校验存在
        validateProjectRoundExists(updateReqVO.getId());

        // 更新
        ProjectRoundDO updateObj = new ProjectRoundDO();
        updateObj.setId(updateReqVO.getId());
        updateObj.setName(updateReqVO.getName());
        updateObj.setPlanStartTime(updateReqVO.getPlanStartTime());
        updateObj.setPlanEndTime(updateReqVO.getPlanEndTime());
        updateObj.setRemark(updateReqVO.getRemark());
        
        // 处理执行人ID列表
        if (CollUtil.isNotEmpty(updateReqVO.getExecutorIds())) {
            updateObj.setExecutorIds(JSONUtil.toJsonStr(updateReqVO.getExecutorIds()));
        } else {
            updateObj.setExecutorIds(null);
        }
        updateObj.setExecutorNames(updateReqVO.getExecutorNames());
        
        projectRoundMapper.updateById(updateObj);
    }

    @Override
    public void deleteProjectRound(Long id) {
        // 校验存在
        validateProjectRoundExists(id);
        // 删除
        projectRoundMapper.deleteById(id);
    }

    private void validateProjectRoundExists(Long id) {
        if (projectRoundMapper.selectById(id) == null) {
            throw exception(PROJECT_ROUND_NOT_EXISTS);
        }
    }

    @Override
    public ProjectRoundDO getProjectRound(Long id) {
        return projectRoundMapper.selectById(id);
    }

    @Override
    public List<ProjectRoundDO> getProjectRoundList(Long projectId) {
        return projectRoundMapper.selectListByProjectId(projectId);
    }

    @Override
    public void updateRoundStatus(Long id, Integer status) {
        // 校验存在
        validateProjectRoundExists(id);

        // 更新状态
        ProjectRoundDO updateObj = new ProjectRoundDO();
        updateObj.setId(id);
        updateObj.setStatus(status);
        projectRoundMapper.updateById(updateObj);
    }

    @Override
    public void updateRoundProgress(Long id, Integer progress) {
        // 校验存在
        validateProjectRoundExists(id);

        // 更新进度
        ProjectRoundDO updateObj = new ProjectRoundDO();
        updateObj.setId(id);
        updateObj.setProgress(progress);
        projectRoundMapper.updateById(updateObj);
    }

}
