package cn.shuhe.system.module.project.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.common.util.object.BeanUtils;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectPageReqVO;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectSaveReqVO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectDO;
import cn.shuhe.system.module.project.dal.mysql.ProjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

import static cn.shuhe.system.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.shuhe.system.module.project.enums.ErrorCodeConstants.*;

/**
 * 项目 Service 实现类
 */
@Service
@Validated
@Slf4j
public class ProjectServiceImpl implements ProjectService {

    @Resource
    private ProjectMapper projectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createProject(ProjectSaveReqVO createReqVO) {
        // 1. 生成项目编号
        String code = generateProjectCode(createReqVO.getDeptType());
        
        // 2. 转换并保存
        ProjectDO project = BeanUtils.toBean(createReqVO, ProjectDO.class);
        project.setCode(code);
        project.setProgress(0); // 初始进度为0
        if (project.getStatus() == null) {
            project.setStatus(0); // 默认草稿状态
        }
        if (project.getPriority() == null) {
            project.setPriority(1); // 默认中优先级
        }
        // 处理标签
        if (createReqVO.getTags() != null && !createReqVO.getTags().isEmpty()) {
            project.setTags(JSONUtil.toJsonStr(createReqVO.getTags()));
        }
        
        projectMapper.insert(project);
        return project.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProject(ProjectSaveReqVO updateReqVO) {
        // 1. 校验存在
        validateProjectExists(updateReqVO.getId());
        
        // 2. 更新
        ProjectDO updateObj = BeanUtils.toBean(updateReqVO, ProjectDO.class);
        // 处理标签
        if (updateReqVO.getTags() != null) {
            updateObj.setTags(JSONUtil.toJsonStr(updateReqVO.getTags()));
        }
        projectMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProject(Long id) {
        // 1. 校验存在
        validateProjectExists(id);
        
        // 2. 删除
        projectMapper.deleteById(id);
    }

    @Override
    public ProjectDO getProject(Long id) {
        return projectMapper.selectById(id);
    }

    @Override
    public PageResult<ProjectDO> getProjectPage(ProjectPageReqVO pageReqVO) {
        return projectMapper.selectPage(pageReqVO);
    }

    @Override
    public void updateProjectStatus(Long id, Integer status) {
        // 1. 校验存在
        ProjectDO project = validateProjectExists(id);
        
        // 2. 更新状态
        ProjectDO updateObj = new ProjectDO();
        updateObj.setId(id);
        updateObj.setStatus(status);
        
        // 如果状态变为进行中，且实际开始时间为空，则设置实际开始时间
        if (status == 1 && project.getActualStartTime() == null) {
            updateObj.setActualStartTime(LocalDateTime.now());
        }
        // 如果状态变为已完成，则设置实际结束时间和进度100%
        if (status == 3) {
            updateObj.setActualEndTime(LocalDateTime.now());
            updateObj.setProgress(100);
        }
        
        projectMapper.updateById(updateObj);
    }

    @Override
    public void updateProjectProgress(Long id, Integer progress) {
        // 1. 校验存在
        validateProjectExists(id);
        
        // 2. 更新进度
        ProjectDO updateObj = new ProjectDO();
        updateObj.setId(id);
        updateObj.setProgress(progress);
        projectMapper.updateById(updateObj);
    }

    /**
     * 校验项目是否存在
     */
    private ProjectDO validateProjectExists(Long id) {
        ProjectDO project = projectMapper.selectById(id);
        if (project == null) {
            throw exception(PROJECT_NOT_EXISTS);
        }
        return project;
    }

    /**
     * 生成项目编号
     * 格式：PRJ-{部门类型}-{年月日}-{4位随机数}
     * 例如：PRJ-1-20260115-0001
     */
    private String generateProjectCode(Integer deptType) {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String random = String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
        return StrUtil.format("PRJ-{}-{}-{}", deptType, date, random);
    }

}
