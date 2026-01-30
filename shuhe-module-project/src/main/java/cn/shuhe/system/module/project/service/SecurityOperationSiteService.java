package cn.shuhe.system.module.project.service;

import cn.shuhe.system.module.project.controller.admin.vo.SecurityOperationSiteRespVO;
import cn.shuhe.system.module.project.controller.admin.vo.SecurityOperationSiteSaveReqVO;
import cn.shuhe.system.module.project.dal.dataobject.SecurityOperationSiteDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 安全运营驻场点 Service 接口
 */
public interface SecurityOperationSiteService {

    /**
     * 创建驻场点
     *
     * @param createReqVO 创建信息
     * @return 驻场点ID
     */
    Long createSite(@Valid SecurityOperationSiteSaveReqVO createReqVO);

    /**
     * 更新驻场点
     *
     * @param updateReqVO 更新信息
     */
    void updateSite(@Valid SecurityOperationSiteSaveReqVO updateReqVO);

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
    SecurityOperationSiteDO getSite(Long id);

    /**
     * 获得驻场点详情（包含人员列表）
     *
     * @param id 驻场点ID
     * @return 驻场点详情
     */
    SecurityOperationSiteRespVO getSiteDetail(Long id);

    /**
     * 根据项目ID获取驻场点列表
     *
     * @param projectId 项目ID
     * @return 驻场点列表
     */
    List<SecurityOperationSiteDO> getListByProjectId(Long projectId);

    /**
     * 根据项目ID获取驻场点详情列表（包含人员）
     *
     * @param projectId 项目ID
     * @return 驻场点详情列表
     */
    List<SecurityOperationSiteRespVO> getSiteDetailListByProjectId(Long projectId);

    /**
     * 更新驻场点状态
     *
     * @param id     驻场点ID
     * @param status 状态
     */
    void updateStatus(Long id, Integer status);

}
