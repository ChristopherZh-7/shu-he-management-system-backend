package cn.shuhe.system.module.project.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.common.util.object.BeanUtils;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectPageReqVO;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectSaveReqVO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectDO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectMemberDO;
import cn.shuhe.system.module.project.dal.mysql.ProjectMapper;
import cn.shuhe.system.module.project.dal.mysql.ProjectMemberMapper;
import cn.shuhe.system.module.project.dal.mysql.ServiceItemMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static cn.shuhe.system.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.shuhe.system.module.project.enums.ErrorCodeConstants.*;

/**
 * 项目 Service 实现类（顶层项目）
 */
@Service
@Validated
@Slf4j
public class ProjectServiceImpl implements ProjectService {

    @Resource
    private ProjectMapper projectMapper;

    @Resource
    private ServiceItemMapper serviceItemMapper;

    @Resource
    private ProjectMemberMapper projectMemberMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createProject(ProjectSaveReqVO createReqVO) {
        // 1. 生成项目编号
        String code = generateProjectCode(createReqVO.getDeptType());

        // 2. 转换并保存
        ProjectDO project = BeanUtils.toBean(createReqVO, ProjectDO.class);
        project.setCode(code);
        if (project.getStatus() == null) {
            project.setStatus(0); // 默认草稿状态
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
        projectMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProject(Long id) {
        // 1. 校验存在
        validateProjectExists(id);

        // 2. 检查是否有服务项
        Long count = serviceItemMapper.selectCountByProjectId(id);
        if (count > 0) {
            throw exception(PROJECT_HAS_SERVICE_ITEMS);
        }

        // 3. 删除
        projectMapper.deleteById(id);
    }

    @Override
    public ProjectDO getProject(Long id) {
        return projectMapper.selectById(id);
    }

    @Override
    public PageResult<ProjectDO> getProjectPage(ProjectPageReqVO pageReqVO, Long userId) {
        // 1. 获取用户参与的项目ID列表
        List<Long> projectIds = projectMemberMapper.selectProjectIdsByUserId(userId);
        if (CollUtil.isEmpty(projectIds)) {
            return PageResult.empty();
        }
        // 2. 查询分页，只返回用户参与的项目
        return projectMapper.selectPageByIds(pageReqVO, projectIds);
    }

    @Override
    public List<ProjectDO> getProjectListByDeptType(Integer deptType) {
        return projectMapper.selectListByDeptType(deptType);
    }

    @Override
    public void updateProjectStatus(Long id, Integer status) {
        // 1. 校验存在
        validateProjectExists(id);

        // 2. 更新状态
        ProjectDO updateObj = new ProjectDO();
        updateObj.setId(id);
        updateObj.setStatus(status);
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
     * 例如：PRJ-1-20260116-0001
     */
    private String generateProjectCode(Integer deptType) {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String random = String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
        return StrUtil.format("PRJ-{}-{}-{}", deptType, date, random);
    }

    @Override
    public ProjectDO getProjectByContractId(Long contractId) {
        return projectMapper.selectByContractId(contractId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addProjectMember(Long projectId, Long userId, String nickname, Integer roleType) {
        // 检查是否已经是成员
        ProjectMemberDO existingMember = projectMemberMapper.selectByProjectIdAndUserId(projectId, userId);
        if (existingMember != null) {
            log.info("【项目成员】用户 {} 已经是项目 {} 的成员，跳过添加", userId, projectId);
            return;
        }

        // 添加成员
        ProjectMemberDO member = ProjectMemberDO.builder()
                .projectId(projectId)
                .userId(userId)
                .nickname(nickname)
                .roleType(roleType)
                .joinTime(java.time.LocalDateTime.now())
                .build();
        projectMemberMapper.insert(member);
        log.info("【项目成员】已将用户 {} ({}) 添加为项目 {} 的成员，角色类型={}", userId, nickname, projectId, roleType);
    }

}
