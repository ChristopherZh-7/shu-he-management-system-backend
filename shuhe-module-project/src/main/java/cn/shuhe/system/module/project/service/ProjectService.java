package cn.shuhe.system.module.project.service;

import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectPageReqVO;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectSaveReqVO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectDO;
import jakarta.validation.Valid;

/**
 * 项目 Service 接口
 */
public interface ProjectService {

    /**
     * 创建项目
     *
     * @param createReqVO 创建信息
     * @return 项目编号
     */
    Long createProject(@Valid ProjectSaveReqVO createReqVO);

    /**
     * 更新项目
     *
     * @param updateReqVO 更新信息
     */
    void updateProject(@Valid ProjectSaveReqVO updateReqVO);

    /**
     * 删除项目
     *
     * @param id 项目编号
     */
    void deleteProject(Long id);

    /**
     * 获得项目
     *
     * @param id 项目编号
     * @return 项目
     */
    ProjectDO getProject(Long id);

    /**
     * 获得项目分页
     *
     * @param pageReqVO 分页查询
     * @return 项目分页
     */
    PageResult<ProjectDO> getProjectPage(ProjectPageReqVO pageReqVO);

    /**
     * 更新项目状态
     *
     * @param id     项目编号
     * @param status 状态
     */
    void updateProjectStatus(Long id, Integer status);

    /**
     * 更新项目进度
     *
     * @param id       项目编号
     * @param progress 进度
     */
    void updateProjectProgress(Long id, Integer progress);

}
