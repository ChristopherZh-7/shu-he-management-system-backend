package cn.shuhe.system.module.ticket.service;

import cn.shuhe.system.module.ticket.controller.admin.vo.TicketCategorySaveReqVO;
import cn.shuhe.system.module.ticket.dal.dataobject.TicketCategoryDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 工单分类 Service 接口（{@code shuhe_ticket_category}）。
 */
public interface TicketCategoryService {

    Long createCategory(@Valid TicketCategorySaveReqVO createReqVO);

    void updateCategory(@Valid TicketCategorySaveReqVO updateReqVO);

    /** 删除前校验：① 无子分类 ② 无工单引用。 */
    void deleteCategory(Long id);

    TicketCategoryDO getCategory(Long id);

    /** 列出所有启用的分类（平铺，给 Controller 自行组装树）。 */
    List<TicketCategoryDO> getEnabledCategoryList();

}
