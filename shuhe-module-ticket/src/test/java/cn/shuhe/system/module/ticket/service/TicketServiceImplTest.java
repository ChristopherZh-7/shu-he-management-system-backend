package cn.shuhe.system.module.ticket.service;

import cn.shuhe.system.framework.security.core.util.SecurityFrameworkUtils;
import cn.shuhe.system.framework.test.core.ut.BaseMockitoUnitTest;
import cn.shuhe.system.module.system.api.dept.DeptApi;
import cn.shuhe.system.module.system.api.dept.dto.DeptRespDTO;
import cn.shuhe.system.module.system.api.permission.PermissionApi;
import cn.shuhe.system.module.system.api.user.AdminUserApi;
import cn.shuhe.system.module.system.api.user.dto.AdminUserRespDTO;
import cn.shuhe.system.module.ticket.controller.admin.vo.TicketAcceptReqVO;
import cn.shuhe.system.module.ticket.controller.admin.vo.TicketAssignReqVO;
import cn.shuhe.system.module.ticket.controller.admin.vo.TicketFinishReqVO;
import cn.shuhe.system.module.ticket.controller.admin.vo.TicketReviewPassReqVO;
import cn.shuhe.system.module.ticket.controller.admin.vo.TicketSaveReqVO;
import cn.shuhe.system.module.ticket.dal.dataobject.TicketCategoryDO;
import cn.shuhe.system.module.ticket.dal.dataobject.TicketDO;
import cn.shuhe.system.module.ticket.dal.dataobject.TicketExecutorDO;
import cn.shuhe.system.module.ticket.dal.dataobject.TicketLogDO;
import cn.shuhe.system.module.ticket.dal.mysql.TicketCategoryMapper;
import cn.shuhe.system.module.ticket.dal.mysql.TicketExecutorMapper;
import cn.shuhe.system.module.ticket.dal.mysql.TicketLogMapper;
import cn.shuhe.system.module.ticket.dal.mysql.TicketMapper;
import cn.shuhe.system.module.ticket.enums.TicketActionEnum;
import cn.shuhe.system.module.ticket.enums.TicketBusinessTypeEnum;
import cn.shuhe.system.module.ticket.enums.TicketStatusEnum;
import cn.shuhe.system.module.ticket.framework.event.TicketAcceptedEvent;
import cn.shuhe.system.module.ticket.framework.event.TicketLifecycleEvent;
import cn.shuhe.system.module.ticket.service.context.TicketServiceContext;
import cn.shuhe.system.module.ticket.service.context.TicketServiceContextResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static cn.shuhe.system.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_ASSIGNEE_NOT_EXISTS;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_ASSIGNEE_REQUIRED;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_ASSIGNEE_SAME;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_CATEGORY_DISABLED;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_CATEGORY_NOT_EXISTS;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_CREATOR_DEPT_MISSING;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_EXECUTOR_NOT_EXISTS;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_NOT_ASSIGNEE;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_NOT_DEPT_LEADER;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_NOT_OWN;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_NO_PERMISSION;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_PRIMARY_EXECUTOR_REQUIRED;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_STATUS_INVALID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link TicketServiceImpl} 单元测试。覆盖状态机入口、IDOR、越权、错误码路径。
 *
 * <p>跳过覆盖：分页查询（{@link cn.shuhe.system.framework.mybatis.core.query.LambdaQueryWrapperX}
 * 难以纯 Mockito 验证），由后续 SQL/集成测试兜底。
 */
class TicketServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private TicketServiceImpl ticketService;

    @Mock
    private TicketMapper ticketMapper;
    @Mock
    private TicketLogMapper logMapper;
    @Mock
    private TicketCategoryMapper categoryMapper;
    @Mock
    private TicketExecutorMapper executorMapper;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private PermissionApi permissionApi;
    @Mock
    private DeptApi deptApi;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private TicketServiceContextResolver serviceContextResolver;

    @BeforeEach
    void setUpServiceContext() {
        lenient().when(serviceContextResolver.resolve(anyLong(), anyLong()))
                .thenReturn(makeServiceContext());
    }

    // ========== createTicket ==========

    @Test
    void createTicket_success_withoutAssignee() {
        long me = 100L;
        AdminUserRespDTO creator = makeUser(me, "Alice", 9L);
        try (MockedStatic<SecurityFrameworkUtils> sec = mockStatic(SecurityFrameworkUtils.class)) {
            sec.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(me);
            sec.when(SecurityFrameworkUtils::getLoginUserNickname).thenReturn("Alice");
            when(adminUserApi.getUser(eq(me))).thenReturn(creator);
            when(ticketMapper.selectLatestByDay(any(), any())).thenReturn(null);
            when(ticketMapper.selectByTicketNo(any())).thenReturn(null);

            TicketSaveReqVO req = baseCreateReq();
            ticketService.createTicket(req);

            ArgumentCaptor<TicketDO> captor = ArgumentCaptor.forClass(TicketDO.class);
            verify(ticketMapper).insert(captor.capture());
            TicketDO saved = captor.getValue();
            assertEquals("故障", saved.getTitle());
            assertEquals(me, saved.getCreatorId());
            assertEquals("Alice", saved.getCreatorName());
            assertNull(saved.getAssigneeId());
            assertNull(saved.getAssigneeName());
            assertEquals(TicketStatusEnum.PENDING_PM_REVIEW.getStatus(), saved.getStatus());
            assertEquals(me, saved.getProjectManagerId());
            assertNotNull(saved.getTicketNo());
            assertTrue(saved.getTicketNo().startsWith("TK"));
            verify(logMapper, times(1)).insert(any(TicketLogDO.class));
        }
    }

    @Test
    void createTicket_ignoresClientRoutingFields() {
        long me = 100L;
        try (MockedStatic<SecurityFrameworkUtils> sec = mockStatic(SecurityFrameworkUtils.class)) {
            sec.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(me);
            when(adminUserApi.getUser(eq(me))).thenReturn(makeUser(me, "Alice", 9L));
            when(ticketMapper.selectLatestByDay(any(), any())).thenReturn(null);
            when(ticketMapper.selectByTicketNo(any())).thenReturn(null);

            TicketSaveReqVO req = baseCreateReq();
            req.setBusinessType(TicketBusinessTypeEnum.OUTSIDE_REQUEST.getType());
            req.setDeptId(999L);
            ticketService.createTicket(req);

            ArgumentCaptor<TicketDO> captor = ArgumentCaptor.forClass(TicketDO.class);
            verify(ticketMapper).insert(captor.capture());
            assertEquals(TicketBusinessTypeEnum.SERVICE_LAUNCH.getType(), captor.getValue().getBusinessType());
            assertEquals(9L, captor.getValue().getDeptId());
            assertEquals(11L, captor.getValue().getServiceItemId());
        }
    }

    @Test
    void createTicket_categoryNotExists_throws() {
        TicketSaveReqVO req = baseCreateReq();
        req.setCategoryId(11L);
        when(categoryMapper.selectById(eq(11L))).thenReturn(null);

        assertServiceException(() -> ticketService.createTicket(req), TICKET_CATEGORY_NOT_EXISTS);
        verify(ticketMapper, never()).insert(any(TicketDO.class));
    }

    @Test
    void createTicket_categoryDisabled_throws() {
        TicketSaveReqVO req = baseCreateReq();
        req.setCategoryId(11L);
        TicketCategoryDO c = new TicketCategoryDO();
        c.setId(11L);
        c.setStatus(1);
        when(categoryMapper.selectById(eq(11L))).thenReturn(c);

        assertServiceException(() -> ticketService.createTicket(req), TICKET_CATEGORY_DISABLED);
    }

    @Test
    void createTicket_creatorMissing_throws() {
        long me = 100L;
        try (MockedStatic<SecurityFrameworkUtils> sec = mockStatic(SecurityFrameworkUtils.class)) {
            sec.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(me);
            when(adminUserApi.getUser(eq(me))).thenReturn(null);

            TicketSaveReqVO req = baseCreateReq();

            assertServiceException(() -> ticketService.createTicket(req), TICKET_CREATOR_DEPT_MISSING);
        }
    }

    // ========== updateTicket ==========

    @Test
    void updateTicket_notOwn_throws() {
        long me = 100L, otherCreator = 999L;
        TicketDO existing = makeTicket(1L, otherCreator, null, TicketStatusEnum.PENDING.getStatus());
        try (MockedStatic<SecurityFrameworkUtils> sec = mockStatic(SecurityFrameworkUtils.class)) {
            sec.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(me);
            when(ticketMapper.selectById(eq(1L))).thenReturn(existing);
            when(permissionApi.hasAnyRoles(eq(me), eq("super_admin"))).thenReturn(false);

            TicketSaveReqVO req = baseCreateReq();
            req.setId(1L);

            assertServiceException(() -> ticketService.updateTicket(req), TICKET_NOT_OWN);
            verify(ticketMapper, never()).updateById(any(TicketDO.class));
        }
    }

    @Test
    void updateTicket_statusInvalid_throws() {
        long me = 100L;
        TicketDO existing = makeTicket(1L, me, null, TicketStatusEnum.IN_PROGRESS.getStatus());
        try (MockedStatic<SecurityFrameworkUtils> sec = mockStatic(SecurityFrameworkUtils.class)) {
            sec.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(me);
            when(ticketMapper.selectById(eq(1L))).thenReturn(existing);
            lenient().when(permissionApi.hasAnyRoles(eq(me), eq("super_admin"))).thenReturn(false);

            TicketSaveReqVO req = baseCreateReq();
            req.setId(1L);

            assertServiceException(() -> ticketService.updateTicket(req), TICKET_STATUS_INVALID, "执行中", "修改基本信息");
            verify(ticketMapper, never()).updateById(any(TicketDO.class));
        }
    }

    @Test
    void updateTicket_success_creatorAtPendingProjectManagerReview() {
        long me = 100L;
        TicketDO existing = makeTicket(1L, me, null, TicketStatusEnum.PENDING_PM_REVIEW.getStatus());
        try (MockedStatic<SecurityFrameworkUtils> sec = mockStatic(SecurityFrameworkUtils.class)) {
            sec.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(me);
            when(ticketMapper.selectById(eq(1L))).thenReturn(existing);
            lenient().when(permissionApi.hasAnyRoles(eq(me), eq("super_admin"))).thenReturn(false);

            TicketSaveReqVO req = baseCreateReq();
            req.setId(1L);
            req.setTitle("改后标题");
            ticketService.updateTicket(req);

            ArgumentCaptor<TicketDO> captor = ArgumentCaptor.forClass(TicketDO.class);
            verify(ticketMapper).updateById(captor.capture());
            assertEquals("改后标题", captor.getValue().getTitle());
            assertNull(captor.getValue().getStatus(), "不能改 status");
            assertNull(captor.getValue().getAssigneeId(), "不能改 assigneeId");
        }
    }

    // ========== deleteTicket ==========

    @Test
    void deleteTicket_notSuperAdmin_throws() {
        long me = 100L;
        TicketDO existing = makeTicket(1L, me, null, TicketStatusEnum.PENDING.getStatus());
        try (MockedStatic<SecurityFrameworkUtils> sec = mockStatic(SecurityFrameworkUtils.class)) {
            sec.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(me);
            when(ticketMapper.selectById(eq(1L))).thenReturn(existing);
            when(permissionApi.hasAnyRoles(eq(me), eq("super_admin"))).thenReturn(false);

            assertServiceException(() -> ticketService.deleteTicket(1L), TICKET_NO_PERMISSION);
            verify(ticketMapper, never()).deleteById(anyLong());
        }
    }

    // ========== assignTicket ==========

    @Test
    void assignTicket_assigneeIdNull_throws() {
        TicketDO existing = makeTicket(1L, 100L, null, TicketStatusEnum.PENDING.getStatus());
        when(ticketMapper.selectById(eq(1L))).thenReturn(existing);

        TicketAssignReqVO req = new TicketAssignReqVO();
        req.setId(1L);
        req.setAssigneeId(null);

        assertServiceException(() -> ticketService.assignTicket(req), TICKET_ASSIGNEE_REQUIRED);
    }

    @Test
    void assignTicket_sameAsCurrent_throws() {
        TicketDO existing = makeTicket(1L, 100L, 200L, TicketStatusEnum.PENDING.getStatus());
        when(ticketMapper.selectById(eq(1L))).thenReturn(existing);

        TicketAssignReqVO req = new TicketAssignReqVO();
        req.setId(1L);
        req.setAssigneeId(200L);

        assertServiceException(() -> ticketService.assignTicket(req), TICKET_ASSIGNEE_SAME);
    }

    @Test
    void assignTicket_success_writesLog() {
        TicketDO existing = makeTicket(1L, 100L, null, TicketStatusEnum.PENDING.getStatus());
        when(ticketMapper.selectById(eq(1L))).thenReturn(existing);
        AdminUserRespDTO assignee = makeUser(200L, "Bob", 8L);
        when(adminUserApi.getUser(eq(200L))).thenReturn(assignee);
        try (MockedStatic<SecurityFrameworkUtils> sec = mockStatic(SecurityFrameworkUtils.class)) {
            sec.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(50L);

            TicketAssignReqVO req = new TicketAssignReqVO();
            req.setId(1L);
            req.setAssigneeId(200L);
            ticketService.assignTicket(req);

            ArgumentCaptor<TicketDO> captor = ArgumentCaptor.forClass(TicketDO.class);
            verify(ticketMapper).updateById(captor.capture());
            assertEquals(200L, captor.getValue().getAssigneeId());
            assertEquals("Bob", captor.getValue().getAssigneeName());
            assertEquals(TicketStatusEnum.PENDING.getStatus(), captor.getValue().getStatus());
            verify(logMapper, times(1)).insert(any(TicketLogDO.class));
        }
    }

    @Test
    void assignTicket_assigneeNotExists_throws() {
        TicketDO existing = makeTicket(1L, 100L, null, TicketStatusEnum.PENDING.getStatus());
        when(ticketMapper.selectById(eq(1L))).thenReturn(existing);
        when(adminUserApi.getUser(eq(999L))).thenReturn(null);

        TicketAssignReqVO req = new TicketAssignReqVO();
        req.setId(1L);
        req.setAssigneeId(999L);

        assertServiceException(() -> ticketService.assignTicket(req), TICKET_ASSIGNEE_NOT_EXISTS);
    }

    // ========== acceptTicket（主管接单 · 多执行人 · 发事件） ==========

    @Test
    void acceptTicket_success_publishesEventAndWritesExecutors() {
        long me = 500L, deptId = 9L;
        TicketDO ticket = makeTicket(1L, 100L, null, TicketStatusEnum.PENDING.getStatus());
        ticket.setDeptId(deptId);
        ticket.setBusinessType(TicketBusinessTypeEnum.GENERAL.getType());
        DeptRespDTO dept = makeDept(deptId, me);
        AdminUserRespDTO acceptor = makeUser(me, "Manager", deptId);
        AdminUserRespDTO e1 = makeUser(201L, "Exec1", 9L);
        AdminUserRespDTO e2 = makeUser(202L, "Exec2", 9L);

        try (MockedStatic<SecurityFrameworkUtils> sec = mockStatic(SecurityFrameworkUtils.class)) {
            sec.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(me);
            sec.when(SecurityFrameworkUtils::getLoginUserNickname).thenReturn("Manager");
            when(ticketMapper.selectById(eq(1L))).thenReturn(ticket);
            when(deptApi.findLeaderUserIdRecursively(eq(deptId))).thenReturn(me);
            when(deptApi.getDeptListByLeaderUserId(eq(me))).thenReturn(Collections.singletonList(dept));
            when(deptApi.getChildDeptList(eq(deptId))).thenReturn(Collections.emptyList());
            when(adminUserApi.getUserList(eq(Arrays.asList(201L, 202L, me))))
                    .thenReturn(Arrays.asList(e1, e2, acceptor));
            when(adminUserApi.getUser(eq(me))).thenReturn(acceptor);
            lenient().when(permissionApi.hasAnyRoles(eq(me), eq("super_admin"))).thenReturn(false);

            TicketAcceptReqVO req = new TicketAcceptReqVO();
            req.setId(1L);
            req.setExecutorIds(Arrays.asList(201L, 202L, 201L));
            req.setRemark("加急");

            ticketService.acceptTicket(req);

            ArgumentCaptor<TicketDO> ticketCap = ArgumentCaptor.forClass(TicketDO.class);
            verify(ticketMapper).updateById(ticketCap.capture());
            assertEquals(me, ticketCap.getValue().getAssigneeId());
            assertEquals("Manager", ticketCap.getValue().getAssigneeName());
            assertEquals(TicketStatusEnum.IN_PROGRESS.getStatus(), ticketCap.getValue().getStatus());
            assertNotNull(ticketCap.getValue().getFirstResponseTime());

            ArgumentCaptor<TicketExecutorDO> execCap = ArgumentCaptor.forClass(TicketExecutorDO.class);
            verify(executorMapper, times(3)).insert(execCap.capture());
            List<TicketExecutorDO> rows = execCap.getAllValues();
            assertEquals(201L, rows.get(0).getUserId());
            assertEquals(202L, rows.get(1).getUserId());
            assertEquals(TicketExecutorDO.ROLE_PRIMARY_EXECUTOR, rows.get(0).getRoleType());
            assertEquals(TicketExecutorDO.ROLE_COLLABORATOR, rows.get(1).getRoleType());
            assertEquals(TicketExecutorDO.ROLE_TECH_REVIEWER, rows.get(2).getRoleType());
            assertEquals(me, rows.get(0).getAssignedBy());

            verify(logMapper, times(1)).insert(any(TicketLogDO.class));

            ArgumentCaptor<TicketAcceptedEvent> evtCap = ArgumentCaptor.forClass(TicketAcceptedEvent.class);
            verify(eventPublisher).publishEvent(evtCap.capture());
            TicketAcceptedEvent evt = evtCap.getValue();
            assertEquals(1L, evt.getTicketId());
            assertEquals(2, evt.getExecutorIds().size(), "去重后 2 个执行人");
            assertEquals(me, evt.getAcceptedBy());
            assertEquals(ticket.getServiceItemId(), evt.getServiceItemId());
        }
    }

    @Test
    void acceptTicket_notDeptLeader_throws() {
        long me = 500L, deptId = 9L, otherLeader = 888L;
        TicketDO ticket = makeTicket(1L, 100L, null, TicketStatusEnum.PENDING.getStatus());
        ticket.setDeptId(deptId);
        DeptRespDTO dept = makeDept(deptId, otherLeader);

        try (MockedStatic<SecurityFrameworkUtils> sec = mockStatic(SecurityFrameworkUtils.class)) {
            sec.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(me);
            when(ticketMapper.selectById(eq(1L))).thenReturn(ticket);
            when(deptApi.findLeaderUserIdRecursively(eq(deptId))).thenReturn(otherLeader);
            when(permissionApi.hasAnyRoles(eq(me), eq("super_admin"))).thenReturn(false);

            TicketAcceptReqVO req = new TicketAcceptReqVO();
            req.setId(1L);
            req.setExecutorIds(Collections.singletonList(201L));

            assertServiceException(() -> ticketService.acceptTicket(req), TICKET_NOT_DEPT_LEADER);
            verify(eventPublisher, never()).publishEvent(any());
            verify(executorMapper, never()).insert(any(TicketExecutorDO.class));
        }
    }

    @Test
    void acceptTicket_executorMissing_throws() {
        long me = 500L, deptId = 9L;
        TicketDO ticket = makeTicket(1L, 100L, null, TicketStatusEnum.PENDING.getStatus());
        ticket.setDeptId(deptId);
        DeptRespDTO dept = makeDept(deptId, me);
        AdminUserRespDTO e1 = makeUser(201L, "Exec1", 9L);
        AdminUserRespDTO reviewer = makeUser(me, "Manager", 9L);

        try (MockedStatic<SecurityFrameworkUtils> sec = mockStatic(SecurityFrameworkUtils.class)) {
            sec.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(me);
            when(ticketMapper.selectById(eq(1L))).thenReturn(ticket);
            when(deptApi.findLeaderUserIdRecursively(eq(deptId))).thenReturn(me);
            when(adminUserApi.getUserList(eq(Arrays.asList(201L, 999L, me))))
                    .thenReturn(Arrays.asList(e1, reviewer));

            TicketAcceptReqVO req = new TicketAcceptReqVO();
            req.setId(1L);
            req.setExecutorIds(Arrays.asList(201L, 999L));

            assertServiceException(() -> ticketService.acceptTicket(req), TICKET_EXECUTOR_NOT_EXISTS, "[999]");
            verify(eventPublisher, never()).publishEvent(any());
        }
    }

    @Test
    void acceptTicket_emptyExecutors_throws() {
        long me = 500L, deptId = 9L;
        TicketDO ticket = makeTicket(1L, 100L, null, TicketStatusEnum.PENDING.getStatus());
        ticket.setDeptId(deptId);
        DeptRespDTO dept = makeDept(deptId, me);

        try (MockedStatic<SecurityFrameworkUtils> sec = mockStatic(SecurityFrameworkUtils.class)) {
            sec.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(me);
            when(ticketMapper.selectById(eq(1L))).thenReturn(ticket);
            when(deptApi.findLeaderUserIdRecursively(eq(deptId))).thenReturn(me);

            TicketAcceptReqVO req = new TicketAcceptReqVO();
            req.setId(1L);
            req.setExecutorIds(Collections.singletonList(null));

            assertServiceException(() -> ticketService.acceptTicket(req), TICKET_PRIMARY_EXECUTOR_REQUIRED);
        }
    }

    @Test
    void acceptTicket_statusNotPending_throws() {
        long me = 500L, deptId = 9L;
        TicketDO ticket = makeTicket(1L, 100L, null, TicketStatusEnum.IN_PROGRESS.getStatus());
        ticket.setDeptId(deptId);
        DeptRespDTO dept = makeDept(deptId, me);

        try (MockedStatic<SecurityFrameworkUtils> sec = mockStatic(SecurityFrameworkUtils.class)) {
            sec.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(me);
            when(ticketMapper.selectById(eq(1L))).thenReturn(ticket);
            when(deptApi.findLeaderUserIdRecursively(eq(deptId))).thenReturn(me);

            TicketAcceptReqVO req = new TicketAcceptReqVO();
            req.setId(1L);
            req.setExecutorIds(Collections.singletonList(201L));

            assertServiceException(() -> ticketService.acceptTicket(req), TICKET_STATUS_INVALID, "执行中", "接单");
        }
    }

    // ========== startTicket / finishTicket / cancelTicket ==========

    @Test
    void startTicket_notAssignee_throws() {
        long me = 100L, assigneeId = 200L;
        TicketDO existing = makeTicket(1L, me, assigneeId, TicketStatusEnum.PENDING.getStatus());
        try (MockedStatic<SecurityFrameworkUtils> sec = mockStatic(SecurityFrameworkUtils.class)) {
            sec.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(me);
            when(ticketMapper.selectById(eq(1L))).thenReturn(existing);

            assertServiceException(() -> ticketService.startTicket(1L), TICKET_NOT_ASSIGNEE);
            verify(ticketMapper, never()).updateById(any(TicketDO.class));
        }
    }

    @Test
    void startTicket_success_setsFirstResponseTime() {
        long me = 200L;
        TicketDO existing = makeTicket(1L, 100L, me, TicketStatusEnum.PENDING.getStatus());
        try (MockedStatic<SecurityFrameworkUtils> sec = mockStatic(SecurityFrameworkUtils.class)) {
            sec.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(me);
            when(ticketMapper.selectById(eq(1L))).thenReturn(existing);

            ticketService.startTicket(1L);

            ArgumentCaptor<TicketDO> captor = ArgumentCaptor.forClass(TicketDO.class);
            verify(ticketMapper).updateById(captor.capture());
            assertEquals(TicketStatusEnum.IN_PROGRESS.getStatus(), captor.getValue().getStatus());
            assertNotNull(captor.getValue().getFirstResponseTime());
        }
    }

    @Test
    void finishTicket_executor_success() {
        long executorId = 201L;
        TicketDO existing = makeTicket(1L, 100L, 500L, TicketStatusEnum.IN_PROGRESS.getStatus());
        existing.setPrimaryExecutorId(executorId);
        try (MockedStatic<SecurityFrameworkUtils> sec = mockStatic(SecurityFrameworkUtils.class)) {
            sec.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(executorId);
            when(ticketMapper.selectById(eq(1L))).thenReturn(existing);
            TicketFinishReqVO req = new TicketFinishReqVO();
            req.setId(1L);
            req.setResult("已完成服务交付");
            ticketService.finishTicket(req);

            ArgumentCaptor<TicketDO> captor = ArgumentCaptor.forClass(TicketDO.class);
            verify(ticketMapper).updateById(captor.capture());
            assertEquals(TicketStatusEnum.PENDING_TECH_REVIEW.getStatus(), captor.getValue().getStatus());
            assertNotNull(captor.getValue().getFinishTime());
        }
    }

    @Test
    void reviewPassTicket_publishesLifecycleCompletion() {
        long creatorId = 100L;
        TicketDO existing = makeTicket(1L, creatorId, 500L,
                TicketStatusEnum.PENDING_REVIEW.getStatus());
        existing.setBusinessType(TicketBusinessTypeEnum.SERVICE_LAUNCH.getType());
        existing.setBusinessId(88L);
        try (MockedStatic<SecurityFrameworkUtils> sec = mockStatic(SecurityFrameworkUtils.class)) {
            sec.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(creatorId);
            when(ticketMapper.selectById(eq(1L))).thenReturn(existing);
            when(adminUserApi.getUser(eq(creatorId))).thenReturn(makeUser(creatorId, "Alice", 9L));

            TicketReviewPassReqVO req = new TicketReviewPassReqVO();
            req.setId(1L);
            req.setComment("交付符合要求");
            ticketService.reviewPassTicket(req);

            ArgumentCaptor<TicketLifecycleEvent> eventCaptor =
                    ArgumentCaptor.forClass(TicketLifecycleEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertEquals(TicketActionEnum.REVIEW_PASS.getAction(), eventCaptor.getValue().getAction());
            assertEquals(88L, eventCaptor.getValue().getBusinessId());
        }
    }

    @Test
    void cancelTicket_byCreator_success() {
        long me = 100L;
        TicketDO existing = makeTicket(1L, me, null, TicketStatusEnum.PENDING.getStatus());
        try (MockedStatic<SecurityFrameworkUtils> sec = mockStatic(SecurityFrameworkUtils.class)) {
            sec.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(me);
            when(ticketMapper.selectById(eq(1L))).thenReturn(existing);
            lenient().when(permissionApi.hasAnyRoles(eq(me), eq("super_admin"))).thenReturn(false);

            ticketService.cancelTicket(1L);

            ArgumentCaptor<TicketDO> captor = ArgumentCaptor.forClass(TicketDO.class);
            verify(ticketMapper).updateById(captor.capture());
            assertEquals(TicketStatusEnum.CANCELLED.getStatus(), captor.getValue().getStatus());
        }
    }

    @Test
    void cancelTicket_byOutsider_throws() {
        long me = 999L;
        TicketDO existing = makeTicket(1L, 100L, null, TicketStatusEnum.PENDING.getStatus());
        try (MockedStatic<SecurityFrameworkUtils> sec = mockStatic(SecurityFrameworkUtils.class)) {
            sec.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(me);
            when(ticketMapper.selectById(eq(1L))).thenReturn(existing);
            when(permissionApi.hasAnyRoles(eq(me), eq("super_admin"))).thenReturn(false);

            assertServiceException(() -> ticketService.cancelTicket(1L), TICKET_NO_PERMISSION);
        }
    }

    // ========== validateTicketAccess / calculateAvailableActions ==========

    @Test
    void validateTicketAccess_unauthorized_throws() {
        long me = 999L;
        TicketDO existing = makeTicket(1L, 100L, 200L, TicketStatusEnum.PENDING.getStatus());
        when(ticketMapper.selectById(eq(1L))).thenReturn(existing);
        when(permissionApi.hasAnyRoles(eq(me), eq("super_admin"))).thenReturn(false);

        assertServiceException(() -> ticketService.validateTicketAccess(1L, me), TICKET_NO_PERMISSION);
    }

    @Test
    void validateTicketAccess_assignee_ok() {
        long me = 200L;
        TicketDO existing = makeTicket(1L, 100L, me, TicketStatusEnum.PENDING.getStatus());
        when(ticketMapper.selectById(eq(1L))).thenReturn(existing);

        TicketDO result = ticketService.validateTicketAccess(1L, me);
        assertEquals(existing, result);
    }

    @Test
    void validateTicketAccess_executor_ok() {
        long me = 201L;
        TicketDO existing = makeTicket(1L, 100L, 500L, TicketStatusEnum.IN_PROGRESS.getStatus());
        when(ticketMapper.selectById(eq(1L))).thenReturn(existing);
        when(executorMapper.existsByTicketIdAndUserId(eq(1L), eq(me))).thenReturn(true);

        assertEquals(existing, ticketService.validateTicketAccess(1L, me));
    }

    @Test
    void calculateAvailableActions_creatorAtPendingProjectReview_canUpdateCancelComment() {
        long me = 100L, deptId = 9L;
        TicketDO ticket = makeTicket(1L, me, 200L, TicketStatusEnum.PENDING_PM_REVIEW.getStatus());
        ticket.setDeptId(deptId);
        when(permissionApi.hasAnyRoles(eq(me), eq("super_admin"))).thenReturn(false);
        when(deptApi.findLeaderUserIdRecursively(eq(deptId))).thenReturn(888L); // 当前用户不是部门负责人

        List<String> actions = ticketService.calculateAvailableActions(ticket, me);
        assertTrue(actions.contains(TicketActionEnum.COMMENT.getAction()), "可评论");
        assertTrue(actions.contains("update"), "可改基本信息");
        assertTrue(actions.contains(TicketActionEnum.CANCEL.getAction()), "可取消");
        assertTrue(!actions.contains("delete"), "非超管不能删");
        assertTrue(!actions.contains(TicketActionEnum.ASSIGN.getAction()), "非超管不能分派");
        assertTrue(!actions.contains(TicketActionEnum.ACCEPT.getAction()), "非部门负责人不能接单");
    }

    @Test
    void calculateAvailableActions_deptLeaderAtPending_canAccept() {
        long me = 555L, deptId = 9L;
        TicketDO ticket = makeTicket(1L, 100L, null, TicketStatusEnum.PENDING.getStatus());
        ticket.setDeptId(deptId);
        lenient().when(permissionApi.hasAnyRoles(eq(me), eq("super_admin"))).thenReturn(false);
        when(deptApi.findLeaderUserIdRecursively(eq(deptId))).thenReturn(me);

        List<String> actions = ticketService.calculateAvailableActions(ticket, me);
        assertTrue(actions.contains(TicketActionEnum.ACCEPT.getAction()), "部门负责人可接单");
    }

    @Test
    void calculateAvailableActions_assigneeAtInProgress_canFinishTransferComment() {
        long me = 200L, deptId = 9L;
        TicketDO ticket = makeTicket(1L, 100L, me, TicketStatusEnum.IN_PROGRESS.getStatus());
        ticket.setDeptId(deptId);
        ticket.setPrimaryExecutorId(me);
        when(permissionApi.hasAnyRoles(eq(me), eq("super_admin"))).thenReturn(false);
        lenient().when(deptApi.findLeaderUserIdRecursively(eq(deptId))).thenReturn(888L);

        List<String> actions = ticketService.calculateAvailableActions(ticket, me);
        assertTrue(actions.contains(TicketActionEnum.FINISH.getAction()), "处理人可完成");
        assertTrue(actions.contains(TicketActionEnum.TRANSFER.getAction()), "处理人可转交");
        assertTrue(actions.contains(TicketActionEnum.COMMENT.getAction()));
        assertTrue(!actions.contains(TicketActionEnum.START.getAction()), "非待处理不能再次接单");
    }

    @Test
    void calculateAvailableActions_primaryExecutorAtInProgress_canFinishComment() {
        long me = 201L;
        TicketDO ticket = makeTicket(1L, 100L, 500L, TicketStatusEnum.IN_PROGRESS.getStatus());
        ticket.setDeptId(9L);
        ticket.setPrimaryExecutorId(me);
        when(executorMapper.existsByTicketIdAndUserId(eq(1L), eq(me))).thenReturn(true);
        lenient().when(permissionApi.hasAnyRoles(eq(me), eq("super_admin"))).thenReturn(false);
        lenient().when(deptApi.findLeaderUserIdRecursively(eq(9L))).thenReturn(888L);

        List<String> actions = ticketService.calculateAvailableActions(ticket, me);
        assertTrue(actions.contains(TicketActionEnum.FINISH.getAction()));
        assertTrue(actions.contains(TicketActionEnum.COMMENT.getAction()));
        assertTrue(!actions.contains(TicketActionEnum.TRANSFER.getAction()));
    }

    // ========== Helpers ==========

    private static TicketSaveReqVO baseCreateReq() {
        TicketSaveReqVO req = new TicketSaveReqVO();
        req.setTitle("故障");
        req.setContent("详情");
        req.setDeptId(1L);
        req.setServiceItemId(11L);
        req.setPriority(1);
        return req;
    }

    private static AdminUserRespDTO makeUser(Long id, String nickname, Long deptId) {
        AdminUserRespDTO u = new AdminUserRespDTO();
        u.setId(id);
        u.setNickname(nickname);
        u.setDeptId(deptId);
        return u;
    }

    private static TicketDO makeTicket(Long id, Long creatorId, Long assigneeId, Integer status) {
        TicketDO t = new TicketDO();
        t.setId(id);
        t.setCreatorId(creatorId);
        t.setAssigneeId(assigneeId);
        t.setStatus(status);
        t.setTicketNo("TK20260518001");
        return t;
    }

    private static DeptRespDTO makeDept(Long id, Long leaderUserId) {
        DeptRespDTO d = new DeptRespDTO();
        d.setId(id);
        d.setLeaderUserId(leaderUserId);
        d.setName("dept-" + id);
        return d;
    }

    private static TicketServiceContext makeServiceContext() {
        return TicketServiceContext.builder()
                .serviceItemId(11L)
                .serviceItemCode("SVC-11")
                .serviceType("penetration_test")
                .serviceTypeName("渗透测试")
                .serviceMode(2)
                .deptType(1)
                .projectId(3L)
                .projectCode("PRJ-3")
                .projectName("项目3")
                .responsibleDeptId(9L)
                .responsibleDeptName("安全服务部")
                .customerId(8L)
                .customerName("客户")
                .sourceType("approved_early_investment")
                .remainingCount(-1)
                .build();
    }
}
