package cn.shuhe.system.module.ticket.service;

import cn.shuhe.system.framework.security.core.util.SecurityFrameworkUtils;
import cn.shuhe.system.framework.test.core.ut.BaseMockitoUnitTest;
import cn.shuhe.system.module.system.api.permission.PermissionApi;
import cn.shuhe.system.module.system.api.user.AdminUserApi;
import cn.shuhe.system.module.system.api.user.dto.AdminUserRespDTO;
import cn.shuhe.system.module.ticket.controller.admin.vo.TicketCommentSaveReqVO;
import cn.shuhe.system.module.ticket.dal.dataobject.TicketCommentDO;
import cn.shuhe.system.module.ticket.dal.dataobject.TicketDO;
import cn.shuhe.system.module.ticket.dal.mysql.TicketCommentMapper;
import cn.shuhe.system.module.ticket.dal.mysql.TicketExecutorMapper;
import cn.shuhe.system.module.ticket.dal.mysql.TicketMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import java.util.Arrays;
import java.util.List;

import static cn.shuhe.system.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.shuhe.system.framework.test.core.util.RandomUtils.randomLongId;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_COMMENT_INTERNAL_DENY;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_NOT_EXISTS;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_NO_PERMISSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link TicketCommentServiceImpl} 单元测试。覆盖：
 * <ul>
 *     <li>IDOR：非提单人 / 非处理人 / 非超管 → 拒绝</li>
 *     <li>内部评论：非处理人创建 → 拒绝（{@code TICKET_COMMENT_INTERNAL_DENY}）</li>
 *     <li>列表过滤：仅提单人身份过滤掉 isInternal=true 的条目</li>
 * </ul>
 */
class TicketCommentServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private TicketCommentServiceImpl commentService;

    @Mock
    private TicketCommentMapper commentMapper;
    @Mock
    private TicketMapper ticketMapper;
    @Mock
    private TicketExecutorMapper ticketExecutorMapper;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private PermissionApi permissionApi;

    // ========== createComment ==========

    @Test
    void createComment_normal_byCreator_success() {
        long ticketId = 10L;
        long creatorId = 100L;
        TicketDO ticket = makeTicket(ticketId, creatorId, 200L);
        try (MockedStatic<SecurityFrameworkUtils> sec = mockStatic(SecurityFrameworkUtils.class)) {
            sec.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(creatorId);
            when(ticketMapper.selectById(eq(ticketId))).thenReturn(ticket);
            AdminUserRespDTO user = new AdminUserRespDTO();
            user.setId(creatorId);
            user.setNickname("Tom");
            user.setDeptId(5L);
            when(adminUserApi.getUser(eq(creatorId))).thenReturn(user);

            TicketCommentSaveReqVO req = new TicketCommentSaveReqVO();
            req.setTicketId(ticketId);
            req.setContent("我有问题");
            req.setIsInternal(false);

            commentService.createComment(req);

            ArgumentCaptor<TicketCommentDO> captor = ArgumentCaptor.forClass(TicketCommentDO.class);
            verify(commentMapper).insert(captor.capture());
            assertEquals(ticketId, captor.getValue().getTicketId());
            assertEquals(creatorId, captor.getValue().getUserId());
            assertEquals("Tom", captor.getValue().getUserName());
            assertEquals("我有问题", captor.getValue().getContent());
            assertFalse(captor.getValue().getIsInternal());
        }
    }

    @Test
    void createComment_internalByCreator_throws() {
        long ticketId = 10L;
        long creatorId = 100L;
        long assigneeId = 200L;
        TicketDO ticket = makeTicket(ticketId, creatorId, assigneeId);
        try (MockedStatic<SecurityFrameworkUtils> sec = mockStatic(SecurityFrameworkUtils.class)) {
            sec.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(creatorId);
            when(ticketMapper.selectById(eq(ticketId))).thenReturn(ticket);
            when(permissionApi.hasAnyRoles(eq(creatorId), eq("super_admin"))).thenReturn(false);

            TicketCommentSaveReqVO req = new TicketCommentSaveReqVO();
            req.setTicketId(ticketId);
            req.setContent("private");
            req.setIsInternal(true);

            assertServiceException(() -> commentService.createComment(req), TICKET_COMMENT_INTERNAL_DENY);
            verify(commentMapper, never()).insert(any(TicketCommentDO.class));
        }
    }

    @Test
    void createComment_byOutsider_throws() {
        long ticketId = 10L;
        long creatorId = 100L;
        long assigneeId = 200L;
        long outsiderId = 999L;
        TicketDO ticket = makeTicket(ticketId, creatorId, assigneeId);
        try (MockedStatic<SecurityFrameworkUtils> sec = mockStatic(SecurityFrameworkUtils.class)) {
            sec.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(outsiderId);
            when(ticketMapper.selectById(eq(ticketId))).thenReturn(ticket);
            when(permissionApi.hasAnyRoles(eq(outsiderId), eq("super_admin"))).thenReturn(false);

            TicketCommentSaveReqVO req = new TicketCommentSaveReqVO();
            req.setTicketId(ticketId);
            req.setContent("hi");
            req.setIsInternal(false);

            assertServiceException(() -> commentService.createComment(req), TICKET_NO_PERMISSION);
            verify(commentMapper, never()).insert(any(TicketCommentDO.class));
        }
    }

    @Test
    void createComment_ticketNotExists_throws() {
        try (MockedStatic<SecurityFrameworkUtils> sec = mockStatic(SecurityFrameworkUtils.class)) {
            sec.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(100L);
            when(ticketMapper.selectById(anyLong())).thenReturn(null);

            TicketCommentSaveReqVO req = new TicketCommentSaveReqVO();
            req.setTicketId(99L);
            req.setContent("hi");

            assertServiceException(() -> commentService.createComment(req), TICKET_NOT_EXISTS);
        }
    }

    // ========== listByTicket ==========

    @Test
    void listByTicket_creatorOnly_filtersInternal() {
        long ticketId = 10L;
        long creatorId = 100L;
        long assigneeId = 200L;
        TicketDO ticket = makeTicket(ticketId, creatorId, assigneeId);
        when(ticketMapper.selectById(eq(ticketId))).thenReturn(ticket);
        when(permissionApi.hasAnyRoles(eq(creatorId), eq("super_admin"))).thenReturn(false);

        TicketCommentDO normal = new TicketCommentDO();
        normal.setId(1L);
        normal.setIsInternal(false);
        TicketCommentDO internal = new TicketCommentDO();
        internal.setId(2L);
        internal.setIsInternal(true);
        when(commentMapper.selectListByTicketId(eq(ticketId))).thenReturn(Arrays.asList(normal, internal));

        List<TicketCommentDO> visible = commentService.listByTicket(ticketId, creatorId);

        assertEquals(1, visible.size(), "提单人只能看到非内部评论");
        assertEquals(1L, visible.get(0).getId());
    }

    @Test
    void listByTicket_assignee_seesAll() {
        long ticketId = 10L;
        long creatorId = 100L;
        long assigneeId = 200L;
        TicketDO ticket = makeTicket(ticketId, creatorId, assigneeId);
        when(ticketMapper.selectById(eq(ticketId))).thenReturn(ticket);
        when(permissionApi.hasAnyRoles(eq(assigneeId), eq("super_admin"))).thenReturn(false);

        TicketCommentDO normal = new TicketCommentDO();
        normal.setId(1L);
        normal.setIsInternal(false);
        TicketCommentDO internal = new TicketCommentDO();
        internal.setId(2L);
        internal.setIsInternal(true);
        when(commentMapper.selectListByTicketId(eq(ticketId))).thenReturn(Arrays.asList(normal, internal));

        List<TicketCommentDO> visible = commentService.listByTicket(ticketId, assigneeId);

        assertEquals(2, visible.size(), "处理人能看到全部评论（包含内部）");
    }

    private static TicketDO makeTicket(long id, long creatorId, long assigneeId) {
        TicketDO t = new TicketDO();
        t.setId(id);
        t.setCreatorId(creatorId);
        t.setAssigneeId(assigneeId);
        t.setStatus(0);
        t.setTicketNo("TK" + randomLongId());
        return t;
    }
}
