package cn.shuhe.system.module.ticket.service;

import cn.shuhe.system.framework.test.core.ut.BaseMockitoUnitTest;
import cn.shuhe.system.module.ticket.controller.admin.vo.TicketCategorySaveReqVO;
import cn.shuhe.system.module.ticket.dal.dataobject.TicketCategoryDO;
import cn.shuhe.system.module.ticket.dal.mysql.TicketCategoryMapper;
import cn.shuhe.system.module.ticket.dal.mysql.TicketMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static cn.shuhe.system.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.shuhe.system.framework.test.core.util.RandomUtils.randomLongId;
import static cn.shuhe.system.framework.test.core.util.RandomUtils.randomPojo;
import static cn.shuhe.system.framework.test.core.util.RandomUtils.randomString;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_CATEGORY_HAS_CHILDREN;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_CATEGORY_HAS_TICKETS;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_CATEGORY_NAME_DUPLICATE;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_CATEGORY_NOT_EXISTS;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_CATEGORY_PARENT_INVALID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link TicketCategoryServiceImpl} 单元测试。覆盖：
 * <ul>
 *     <li>创建：父分类存在性、同级 name 重复</li>
 *     <li>修改：环路检测（不能把自己设为自己的子孙节点的子节点）</li>
 *     <li>删除：子分类约束、关联工单约束</li>
 * </ul>
 */
class TicketCategoryServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private TicketCategoryServiceImpl categoryService;

    @Mock
    private TicketCategoryMapper categoryMapper;

    @Mock
    private TicketMapper ticketMapper;

    // ========== createCategory ==========

    @Test
    void createCategory_success_topLevel() {
        TicketCategorySaveReqVO req = new TicketCategorySaveReqVO();
        req.setName("故障报修");
        req.setParentId(0L);
        when(categoryMapper.selectByParentIdAndName(eq(0L), eq("故障报修"))).thenReturn(null);

        Long id = categoryService.createCategory(req);

        ArgumentCaptor<TicketCategoryDO> captor = ArgumentCaptor.forClass(TicketCategoryDO.class);
        verify(categoryMapper).insert(captor.capture());
        TicketCategoryDO saved = captor.getValue();
        assertEquals(0L, saved.getParentId());
        assertEquals("故障报修", saved.getName());
        assertEquals(0, saved.getStatus().intValue(), "未传 status 默认启用");
        assertEquals(0, saved.getSort().intValue(), "未传 sort 默认 0");
    }

    @Test
    void createCategory_parentNotExists_throws() {
        TicketCategorySaveReqVO req = new TicketCategorySaveReqVO();
        req.setName(randomString());
        req.setParentId(99L);
        when(categoryMapper.selectById(eq(99L))).thenReturn(null);

        assertServiceException(() -> categoryService.createCategory(req), TICKET_CATEGORY_NOT_EXISTS);
        verify(categoryMapper, never()).insert(any(TicketCategoryDO.class));
    }

    @Test
    void createCategory_nameDuplicateUnderSameParent_throws() {
        TicketCategorySaveReqVO req = new TicketCategorySaveReqVO();
        req.setName("故障报修");
        req.setParentId(0L);
        TicketCategoryDO dup = new TicketCategoryDO();
        dup.setId(randomLongId());
        dup.setName("故障报修");
        when(categoryMapper.selectByParentIdAndName(eq(0L), eq("故障报修"))).thenReturn(dup);

        assertServiceException(() -> categoryService.createCategory(req), TICKET_CATEGORY_NAME_DUPLICATE);
        verify(categoryMapper, never()).insert(any(TicketCategoryDO.class));
    }

    // ========== updateCategory ==========

    @Test
    void updateCategory_selectSelfAsParent_throws() {
        TicketCategoryDO existing = randomPojo(TicketCategoryDO.class, o -> {
            o.setId(5L);
            o.setParentId(0L);
        });
        when(categoryMapper.selectById(eq(5L))).thenReturn(existing);

        TicketCategorySaveReqVO req = new TicketCategorySaveReqVO();
        req.setId(5L);
        req.setName(randomString());
        req.setParentId(5L);

        assertServiceException(() -> categoryService.updateCategory(req), TICKET_CATEGORY_PARENT_INVALID);
        verify(categoryMapper, never()).updateById(any(TicketCategoryDO.class));
    }

    @Test
    void updateCategory_selectDescendantAsParent_throws() {
        // 树：1（root） → 2 → 3。 现在把 1 的 parent 改成 3 → 形成环
        TicketCategoryDO node1 = makeNode(1L, 0L);
        TicketCategoryDO node3 = makeNode(3L, 2L);
        TicketCategoryDO node2 = makeNode(2L, 1L);
        when(categoryMapper.selectById(eq(1L))).thenReturn(node1);
        when(categoryMapper.selectById(eq(3L))).thenReturn(node3);
        when(categoryMapper.selectById(eq(2L))).thenReturn(node2);

        TicketCategorySaveReqVO req = new TicketCategorySaveReqVO();
        req.setId(1L);
        req.setName(randomString());
        req.setParentId(3L);

        assertServiceException(() -> categoryService.updateCategory(req), TICKET_CATEGORY_PARENT_INVALID);
        verify(categoryMapper, never()).updateById(any(TicketCategoryDO.class));
    }

    @Test
    void updateCategory_success_renameOnly() {
        TicketCategoryDO existing = makeNode(7L, 0L);
        existing.setName("旧名");
        when(categoryMapper.selectById(eq(7L))).thenReturn(existing);
        when(categoryMapper.selectByParentIdAndName(eq(0L), eq("新名"))).thenReturn(null);

        TicketCategorySaveReqVO req = new TicketCategorySaveReqVO();
        req.setId(7L);
        req.setName("新名");
        req.setParentId(0L);
        categoryService.updateCategory(req);

        verify(categoryMapper).updateById(any(TicketCategoryDO.class));
    }

    // ========== deleteCategory ==========

    @Test
    void deleteCategory_hasChildren_throws() {
        TicketCategoryDO existing = makeNode(9L, 0L);
        when(categoryMapper.selectById(eq(9L))).thenReturn(existing);
        when(categoryMapper.selectCountByParentId(eq(9L))).thenReturn(3L);

        assertServiceException(() -> categoryService.deleteCategory(9L), TICKET_CATEGORY_HAS_CHILDREN);
        verify(categoryMapper, never()).deleteById(anyLong());
    }

    @Test
    void deleteCategory_hasTickets_throws() {
        TicketCategoryDO existing = makeNode(9L, 0L);
        when(categoryMapper.selectById(eq(9L))).thenReturn(existing);
        when(categoryMapper.selectCountByParentId(eq(9L))).thenReturn(0L);
        when(ticketMapper.selectCountByCategoryId(eq(9L))).thenReturn(5L);

        assertServiceException(() -> categoryService.deleteCategory(9L), TICKET_CATEGORY_HAS_TICKETS);
        verify(categoryMapper, never()).deleteById(anyLong());
    }

    @Test
    void deleteCategory_success() {
        TicketCategoryDO existing = makeNode(9L, 0L);
        when(categoryMapper.selectById(eq(9L))).thenReturn(existing);
        when(categoryMapper.selectCountByParentId(eq(9L))).thenReturn(0L);
        when(ticketMapper.selectCountByCategoryId(eq(9L))).thenReturn(0L);

        categoryService.deleteCategory(9L);

        verify(categoryMapper, times(1)).deleteById(eq(9L));
    }

    private static TicketCategoryDO makeNode(Long id, Long parentId) {
        TicketCategoryDO node = new TicketCategoryDO();
        node.setId(id);
        node.setParentId(parentId);
        node.setName("c-" + id);
        return node;
    }
}
