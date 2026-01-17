package cn.shuhe.system.module.project.service;

import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.module.project.controller.admin.vo.ServiceItemBatchSaveReqVO;
import cn.shuhe.system.module.project.controller.admin.vo.ServiceItemImportExcelVO;
import cn.shuhe.system.module.project.controller.admin.vo.ServiceItemImportRespVO;
import cn.shuhe.system.module.project.controller.admin.vo.ServiceItemPageReqVO;
import cn.shuhe.system.module.project.controller.admin.vo.ServiceItemSaveReqVO;
import cn.shuhe.system.module.project.dal.dataobject.ServiceItemDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 服务项 Service 接口
 */
public interface ServiceItemService {

    /**
     * 创建服务项
     *
     * @param createReqVO 创建信息
     * @return 服务项编号
     */
    Long createServiceItem(@Valid ServiceItemSaveReqVO createReqVO);

    /**
     * 更新服务项
     *
     * @param updateReqVO 更新信息
     */
    void updateServiceItem(@Valid ServiceItemSaveReqVO updateReqVO);

    /**
     * 删除服务项
     *
     * @param id 服务项编号
     */
    void deleteServiceItem(Long id);

    /**
     * 获得服务项
     *
     * @param id 服务项编号
     * @return 服务项
     */
    ServiceItemDO getServiceItem(Long id);

    /**
     * 获得服务项分页
     *
     * @param pageReqVO 分页查询
     * @return 服务项分页
     */
    PageResult<ServiceItemDO> getServiceItemPage(ServiceItemPageReqVO pageReqVO);

    /**
     * 获得指定项目的服务项列表
     *
     * @param projectId 项目ID
     * @return 服务项列表
     */
    List<ServiceItemDO> getServiceItemListByProjectId(Long projectId);

    /**
     * 获得指定项目和部门的服务项列表
     *
     * @param projectId 项目ID
     * @param deptId    部门ID
     * @return 服务项列表
     */
    List<ServiceItemDO> getServiceItemListByProjectIdAndDeptId(Long projectId, Long deptId);

    /**
     * 更新服务项状态
     *
     * @param id     服务项编号
     * @param status 状态
     */
    void updateServiceItemStatus(Long id, Integer status);

    /**
     * 更新服务项进度
     *
     * @param id       服务项编号
     * @param progress 进度
     */
    void updateServiceItemProgress(Long id, Integer progress);

    /**
     * 批量创建服务项
     *
     * @param batchReqVO 批量创建信息
     * @param deptId     部门ID（顶级）
     * @return 创建的服务项ID列表
     */
    List<Long> batchCreateServiceItem(@Valid ServiceItemBatchSaveReqVO batchReqVO, Long deptId);

    /**
     * 导入服务项
     *
     * @param projectId 项目ID
     * @param deptType  部门类型
     * @param list      导入数据列表
     * @param deptId    部门ID（顶级）
     * @return 导入响应
     */
    ServiceItemImportRespVO importServiceItemList(Long projectId, Integer deptType,
            List<ServiceItemImportExcelVO> list, Long deptId);

}
