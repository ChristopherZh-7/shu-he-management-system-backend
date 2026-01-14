package cn.shuhe.system.module.ticket.service;

import cn.shuhe.system.module.ticket.controller.admin.vo.CategorySaveReqVO;
import cn.shuhe.system.module.ticket.dal.dataobject.TicketCategoryDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 工单分类 Service 接口
 */
public interface TicketCategoryService {

    /**
     * 创建工单分类
     *
     * @param createReqVO 创建信息
     * @return 分类ID
     */
    Long createCategory(@Valid CategorySaveReqVO createReqVO);

    /**
     * 更新工单分类
     *
     * @param updateReqVO 更新信息
     */
    void updateCategory(@Valid CategorySaveReqVO updateReqVO);

    /**
     * 删除工单分类
     *
     * @param id 分类ID
     */
    void deleteCategory(Long id);

    /**
     * 获得工单分类
     *
     * @param id 分类ID
     * @return 工单分类
     */
    TicketCategoryDO getCategory(Long id);

    /**
     * 获得工单分类列表
     *
     * @param name 分类名称
     * @param status 状态
     * @return 工单分类列表
     */
    List<TicketCategoryDO> getCategoryList(String name, Integer status);

}
