package cn.shuhe.system.module.project.service;

import cn.shuhe.system.module.project.controller.admin.vo.ProjectSiteRespVO;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectSiteSaveReqVO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectSiteDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 项目驻场点 Service 接口
 * 
 * 通用的驻场点管理，支持所有部门类型的项目
 */
public interface ProjectSiteService {

    /**
     * 创建驻场点
     *
     * @param createReqVO 创建信息
     * @return 驻场点ID
     */
    Long createSite(@Valid ProjectSiteSaveReqVO createReqVO);

    /**
     * 更新驻场点
     *
     * @param updateReqVO 更新信息
     */
    void updateSite(@Valid ProjectSiteSaveReqVO updateReqVO);

    /**
     * 删除驻场点
     *
     * @param id 驻场点ID
     */
    void deleteSite(Long id);

    /**
     * 获得驻场点
     *
     * @param id 驻场点ID
     * @return 驻场点
     */
    ProjectSiteDO getSite(Long id);

    /**
     * 获得驻场点详情（包含人员列表）
     *
     * @param id 驻场点ID
     * @return 驻场点详情
     */
    ProjectSiteRespVO getSiteDetail(Long id);

    /**
     * 根据项目ID获取驻场点列表
     *
     * @param projectId 项目ID
     * @return 驻场点列表
     */
    List<ProjectSiteDO> getListByProjectId(Long projectId);

    /**
     * 根据项目ID获取驻场点详情列表（包含人员）
     *
     * @param projectId 项目ID
     * @return 驻场点详情列表
     */
    List<ProjectSiteRespVO> getSiteDetailListByProjectId(Long projectId);

    /**
     * 更新驻场点状态
     *
     * @param id     驻场点ID
     * @param status 状态
     */
    void updateStatus(Long id, Integer status);

    /**
     * 判断项目是否有驻场点
     *
     * @param projectId 项目ID
     * @return 是否有驻场点
     */
    boolean hasSite(Long projectId);

}
