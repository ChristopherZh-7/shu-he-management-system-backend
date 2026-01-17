package cn.shuhe.system.module.project.service;

import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectPageReqVO;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectSaveReqVO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 项目 Service 接口（顶层项目）
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
     * @param userId    当前用户ID（用于过滤只显示用户参与的项目）
     * @return 项目分页
     */
    PageResult<ProjectDO> getProjectPage(ProjectPageReqVO pageReqVO, Long userId);

    /**
     * 获得指定部门类型的项目列表
     *
     * @param deptType 部门类型
     * @return 项目列表
     */
    List<ProjectDO> getProjectListByDeptType(Integer deptType);

    /**
     * 更新项目状态
     *
     * @param id     项目编号
     * @param status 状态
     */
    void updateProjectStatus(Long id, Integer status);

    /**
     * 根据合同ID获取项目
     *
     * @param contractId 合同ID
     * @return 项目
     */
    ProjectDO getProjectByContractId(Long contractId);

    /**
     * 添加项目成员
     *
     * @param projectId 项目ID
     * @param userId    用户ID
     * @param nickname  用户昵称
     * @param roleType  角色类型（1-项目经理 2-执行人员 3-审核人员）
     */
    void addProjectMember(Long projectId, Long userId, String nickname, Integer roleType);

}
