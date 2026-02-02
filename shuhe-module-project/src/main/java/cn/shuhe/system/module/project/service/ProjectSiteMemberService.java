package cn.shuhe.system.module.project.service;

import cn.shuhe.system.module.project.controller.admin.vo.ProjectSiteMemberRespVO;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectSiteMemberSaveReqVO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectSiteMemberDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 项目驻场人员 Service 接口
 */
public interface ProjectSiteMemberService {

    /**
     * 创建驻场人员
     *
     * @param createReqVO 创建信息
     * @return 人员ID
     */
    Long createMember(@Valid ProjectSiteMemberSaveReqVO createReqVO);

    /**
     * 更新驻场人员
     *
     * @param updateReqVO 更新信息
     */
    void updateMember(@Valid ProjectSiteMemberSaveReqVO updateReqVO);

    /**
     * 删除驻场人员
     *
     * @param id 人员ID
     */
    void deleteMember(Long id);

    /**
     * 获得驻场人员
     *
     * @param id 人员ID
     * @return 驻场人员
     */
    ProjectSiteMemberDO getMember(Long id);

    /**
     * 获得驻场人员详情
     *
     * @param id 人员ID
     * @return 驻场人员详情
     */
    ProjectSiteMemberRespVO getMemberDetail(Long id);

    /**
     * 根据驻场点ID获取人员列表
     *
     * @param siteId 驻场点ID
     * @return 人员列表
     */
    List<ProjectSiteMemberRespVO> getListBySiteId(Long siteId);

    /**
     * 根据项目ID获取人员列表
     *
     * @param projectId 项目ID
     * @return 人员列表
     */
    List<ProjectSiteMemberRespVO> getListByProjectId(Long projectId);

    /**
     * 标记人员已离开
     *
     * @param id 人员ID
     */
    void setMemberLeft(Long id);

}
