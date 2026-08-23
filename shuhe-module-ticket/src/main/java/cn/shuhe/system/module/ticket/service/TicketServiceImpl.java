package cn.shuhe.system.module.ticket.service;

import cn.shuhe.system.framework.common.exception.ServiceException;
import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.datapermission.core.annotation.DataPermission;
import cn.shuhe.system.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.shuhe.system.framework.security.core.util.SecurityFrameworkUtils;
import cn.shuhe.system.module.system.api.dept.DeptApi;
import cn.shuhe.system.module.system.api.dept.dto.DeptRespDTO;
import cn.shuhe.system.module.system.api.dingtalk.DingtalkNotifyApi;
import cn.shuhe.system.module.system.api.dingtalk.dto.DingtalkNotifySendReqDTO;
import cn.shuhe.system.module.system.api.permission.PermissionApi;
import cn.shuhe.system.module.system.api.user.AdminUserApi;
import cn.shuhe.system.module.system.api.user.dto.AdminUserRespDTO;
import cn.shuhe.system.module.ticket.controller.admin.vo.TicketAcceptReqVO;
import cn.shuhe.system.module.ticket.controller.admin.vo.TicketAssignReqVO;
import cn.shuhe.system.module.ticket.controller.admin.vo.TicketFinishReqVO;
import cn.shuhe.system.module.ticket.controller.admin.vo.TicketPageReqVO;
import cn.shuhe.system.module.ticket.controller.admin.vo.TicketReopenReqVO;
import cn.shuhe.system.module.ticket.controller.admin.vo.TicketReturnReqVO;
import cn.shuhe.system.module.ticket.controller.admin.vo.TicketReviewPassReqVO;
import cn.shuhe.system.module.ticket.controller.admin.vo.TicketReviewRejectReqVO;
import cn.shuhe.system.module.ticket.controller.admin.vo.TicketSaveReqVO;
import cn.shuhe.system.module.ticket.controller.admin.vo.TicketTransferReqVO;
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
import cn.shuhe.system.module.ticket.enums.TicketSourceEnum;
import cn.shuhe.system.module.ticket.enums.TicketStatusEnum;
import cn.shuhe.system.module.ticket.framework.event.TicketAcceptedEvent;
import cn.shuhe.system.module.ticket.framework.event.TicketLifecycleEvent;
import cn.shuhe.system.module.ticket.framework.statemachine.TicketStateMachine;
import cn.shuhe.system.module.ticket.service.context.TicketServiceContext;
import cn.shuhe.system.module.ticket.service.context.TicketServiceContextResolver;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.shuhe.system.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_ASSIGNEE_NOT_EXISTS;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_ASSIGNEE_REQUIRED;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_ASSIGNEE_SAME;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_CATEGORY_DISABLED;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_CATEGORY_NOT_EXISTS;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_CREATOR_DEPT_MISSING;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_DRIVER_FAILED;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_EXECUTOR_EMPTY;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_EXECUTOR_NOT_EXISTS;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_EXECUTOR_OUT_OF_SCOPE;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_NOT_ASSIGNEE;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_NOT_DEPT_LEADER;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_NOT_EXISTS;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_NOT_OWN;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_NO_GENERATE_FAIL;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_NO_PERMISSION;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_REOPEN_EXPIRED;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_REOPEN_LIMIT;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_STATUS_INVALID;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_SERVICE_ITEM_REQUIRED;

/**
 * 工单 Service 实现。
 *
 * <p>核心约束：所有状态变更入口都先调 {@link TicketStateMachine#checkTransition} 校验，再写 log。
 * IDOR 校验由 {@link #validateTicketAccess} 集中负责。
 */
@Service
@Validated
@Slf4j
public class TicketServiceImpl implements TicketService {

    private static final String ROLE_SUPER_ADMIN = "super_admin";
    private static final DateTimeFormatter TICKET_NO_DAY_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int TICKET_NO_GENERATE_RETRIES = 5;

    /** 重开窗口：完成/关闭后 N 天内允许 reopen。 */
    private static final int REOPEN_WINDOW_DAYS = 7;
    /** 重开次数上限（防滥用）。 */
    private static final int REOPEN_MAX_COUNT = 3;

    @Resource
    private TicketMapper ticketMapper;

    @Resource
    private TicketLogMapper logMapper;

    @Resource
    private TicketCategoryMapper categoryMapper;

    @Resource
    private TicketExecutorMapper executorMapper;

    @Resource
    private AdminUserApi adminUserApi;

    @Resource
    private PermissionApi permissionApi;

    @Resource
    private DeptApi deptApi;

    @Resource
    private ApplicationEventPublisher eventPublisher;

    @Resource
    private DingtalkNotifyApi dingtalkNotifyApi;

    @Resource
    private TicketServiceContextResolver serviceContextResolver;

    // ========== CRUD ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTicket(TicketSaveReqVO createReqVO) {
        // 1. 校验分类（如果有指定）
        if (createReqVO.getCategoryId() != null) {
            TicketCategoryDO category = categoryMapper.selectById(createReqVO.getCategoryId());
            if (category == null) {
                throw exception(TICKET_CATEGORY_NOT_EXISTS);
            }
            if (category.getStatus() != null && category.getStatus() == 1) {
                throw exception(TICKET_CATEGORY_DISABLED);
            }
        }

        // 2. 校验提单人
        Long creatorUserId = SecurityFrameworkUtils.getLoginUserId();
        AdminUserRespDTO creator = creatorUserId == null ? null : adminUserApi.getUser(creatorUserId);
        if (creator == null) {
            throw exception(TICKET_CREATOR_DEPT_MISSING);
        }
        TicketServiceContext context = resolveServiceContext(createReqVO.getServiceItemId(), creatorUserId);

        // 3. 只信任服务项 ID；项目、客户、服务类型和接单部门全由后端推导。
        TicketDO ticket = TicketDO.builder()
                .title(createReqVO.getTitle())
                .content(createReqVO.getContent())
                .categoryId(createReqVO.getCategoryId())
                .priority(createReqVO.getPriority() == null ? 1 : createReqVO.getPriority())
                .source(TicketSourceEnum.MANUAL.getSource())
                .businessType(TicketBusinessTypeEnum.SERVICE_LAUNCH.getType())
                .status(TicketStatusEnum.PENDING.getStatus())
                .creatorId(creatorUserId)
                .creatorName(creator.getNickname())
                .deptId(context.getResponsibleDeptId())
                .dueTime(createReqVO.getDueTime())
                .projectId(context.getProjectId())
                .serviceItemId(context.getServiceItemId())
                .customerId(context.getCustomerId())
                .extJson(buildServiceSnapshot(createReqVO.getExtJson(), context))
                .remark(createReqVO.getRemark())
                .notifyChannels("inner,dingtalk")
                .notifyStatus(0)
                .build();

        // 4. 生成工单号（带重试，靠 uk_ticket_no 唯一索引兜底）
        ticket.setTicketNo(generateTicketNo());

        // 5. 入库 + 写 log
        ticketMapper.insert(ticket);
        writeLog(ticket.getId(), TicketActionEnum.CREATE, null, ticket.getStatus(),
                null, ticket.getAssigneeId(), "创建工单");

        // 6. 钉钉通知负责部门接单
        notifyDingtalk(ticket, Collections.singletonList(resolveDeptLeaderId(ticket.getDeptId())),
                "新工单待接单",
                String.format("- 提单人：%s%n%n您是派单部门负责人，请尽快接单派工", ticket.getCreatorName()));

        return ticket.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTicket(TicketSaveReqVO updateReqVO) {
        TicketDO existing = mustExist(updateReqVO.getId());
        Long currentUserId = SecurityFrameworkUtils.getLoginUserId();
        // 仅提单人本人 / 超管可改，且必须 status=0 待处理 或 status=6 已退回（退回后修改再重提）
        if (!isSuperAdmin(currentUserId)
                && !Objects.equals(existing.getCreatorId(), currentUserId)) {
            throw exception(TICKET_NOT_OWN);
        }
        if (!TicketStatusEnum.PENDING.getStatus().equals(existing.getStatus())
                && !TicketStatusEnum.RETURNED.getStatus().equals(existing.getStatus())) {
            throw exception(TICKET_STATUS_INVALID,
                    TicketStatusEnum.nameOf(existing.getStatus()), "修改基本信息");
        }
        TicketServiceContext context = resolveServiceContext(updateReqVO.getServiceItemId(), currentUserId);
        // 仅允许改可编辑字段；assignee / status 由专用接口处理
        TicketDO update = new TicketDO();
        update.setId(existing.getId());
        update.setTitle(updateReqVO.getTitle());
        update.setContent(updateReqVO.getContent());
        update.setCategoryId(updateReqVO.getCategoryId());
        update.setPriority(updateReqVO.getPriority());
        update.setDeptId(context.getResponsibleDeptId());
        update.setDueTime(updateReqVO.getDueTime());
        update.setProjectId(context.getProjectId());
        update.setServiceItemId(context.getServiceItemId());
        update.setCustomerId(context.getCustomerId());
        update.setBusinessType(TicketBusinessTypeEnum.SERVICE_LAUNCH.getType());
        update.setBusinessId(null);
        update.setExtJson(buildServiceSnapshot(updateReqVO.getExtJson(), context));
        update.setRemark(updateReqVO.getRemark());
        ticketMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTicket(Long id) {
        TicketDO existing = mustExist(id);
        Long currentUserId = SecurityFrameworkUtils.getLoginUserId();
        if (!isSuperAdmin(currentUserId)) {
            throw exception(TICKET_NO_PERMISSION);
        }
        ticketMapper.deleteById(existing.getId());
    }

    @Override
    public TicketDO getTicket(Long id) {
        return ticketMapper.selectById(id);
    }

    @Override
    public PageResult<TicketDO> getTicketPage(TicketPageReqVO pageReqVO) {
        return ticketMapper.selectPage(pageReqVO, buildPageWrapper(pageReqVO));
    }

    @Override
    @DataPermission(enable = false)
    public PageResult<TicketDO> getMyTicketPage(TicketPageReqVO pageReqVO, Long userId) {
        if (userId == null) {
            return new PageResult<>(Collections.emptyList(), 0L);
        }
        LambdaQueryWrapperX<TicketDO> wrapper = buildPageWrapper(pageReqVO);
        if ("created".equals(pageReqVO.getMyScope())) {
            wrapper.eq(TicketDO::getCreatorId, userId);
            return ticketMapper.selectPage(pageReqVO, wrapper);
        }

        // 「与我相关」的另外两个维度：
        // 1. 我是执行人（接单派工后只写 shuhe_ticket_executor，不在 assignee_id 上）
        List<Long> executorTicketIds = executorMapper.selectTicketIdsByUserId(userId);
        // 2. 待我接单（我负责的部门及其子孙部门的 status=0 工单；assignee 此时为 null）
        Set<Long> ledDeptIds = resolveLedDeptIds(userId);

        boolean assignedOnly = "assigned".equals(pageReqVO.getMyScope());
        wrapper.and(w -> {
            if (assignedOnly) {
                w.eq(TicketDO::getAssigneeId, userId);
            } else {
                w.eq(TicketDO::getCreatorId, userId)
                        .or().eq(TicketDO::getAssigneeId, userId);
            }
            if (!executorTicketIds.isEmpty()) {
                w.or().in(TicketDO::getId, executorTicketIds);
            }
            if (!ledDeptIds.isEmpty()) {
                w.or(w2 -> w2.in(TicketDO::getDeptId, ledDeptIds)
                        .eq(TicketDO::getStatus, TicketStatusEnum.PENDING.getStatus()));
            }
        });
        return ticketMapper.selectPage(pageReqVO, wrapper);
    }

    @Override
    public List<TicketServiceContext> getEligibleServiceItems(Long userId, Long projectId) {
        return serviceContextResolver.listEligible(userId, projectId);
    }

    /**
     * 我负责的部门集合：直接负责的部门 + 其全部子孙部门。
     *
     * <p>子孙部门未单独配置负责人时按递归语义归我管；即便配置了他人，父级负责人
     * 在「待我接单」里看见下级待接单工单也合理（能否操作仍由 {@link #isDeptLeader}
     * 精确鉴权）。查询失败容错返回空集，不阻断列表主流程。
     */
    private Set<Long> resolveLedDeptIds(Long userId) {
        Set<Long> result = new LinkedHashSet<>();
        try {
            List<DeptRespDTO> ledDepts = deptApi.getDeptListByLeaderUserId(userId);
            if (ledDepts == null || ledDepts.isEmpty()) {
                return result;
            }
            for (DeptRespDTO dept : ledDepts) {
                if (dept == null || dept.getId() == null) {
                    continue;
                }
                result.add(dept.getId());
                List<DeptRespDTO> children = deptApi.getChildDeptList(dept.getId());
                if (children != null) {
                    for (DeptRespDTO child : children) {
                        if (child != null && child.getId() != null) {
                            result.add(child.getId());
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("【工单】查询用户负责部门失败 userId={}: {}", userId, ex.getMessage());
        }
        return result;
    }

    private LambdaQueryWrapperX<TicketDO> buildPageWrapper(TicketPageReqVO pageReqVO) {
        LambdaQueryWrapperX<TicketDO> wrapper = new LambdaQueryWrapperX<TicketDO>()
                .likeIfPresent(TicketDO::getTicketNo, pageReqVO.getTicketNo())
                .likeIfPresent(TicketDO::getTitle, pageReqVO.getTitle())
                .eqIfPresent(TicketDO::getCategoryId, pageReqVO.getCategoryId())
                .eqIfPresent(TicketDO::getPriority, pageReqVO.getPriority())
                .eqIfPresent(TicketDO::getStatus, pageReqVO.getStatus())
                .eqIfPresent(TicketDO::getBusinessType, pageReqVO.getBusinessType())
                .eqIfPresent(TicketDO::getCreatorId, pageReqVO.getCreatorId())
                .eqIfPresent(TicketDO::getAssigneeId, pageReqVO.getAssigneeId())
                .eqIfPresent(TicketDO::getDeptId, pageReqVO.getDeptId());
        LocalDateTime[] createRange = pageReqVO.getCreateTime();
        if (createRange != null && createRange.length == 2) {
            wrapper.betweenIfPresent(TicketDO::getCreateTime, createRange);
        }
        LocalDateTime[] dueRange = pageReqVO.getDueTime();
        if (dueRange != null && dueRange.length == 2) {
            wrapper.betweenIfPresent(TicketDO::getDueTime, dueRange);
        }
        wrapper.orderByDesc(TicketDO::getCreateTime);
        return wrapper;
    }

    // ========== 状态机 ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignTicket(TicketAssignReqVO reqVO) {
        TicketDO ticket = mustExist(reqVO.getId());
        if (reqVO.getAssigneeId() == null) {
            throw exception(TICKET_ASSIGNEE_REQUIRED);
        }
        if (Objects.equals(reqVO.getAssigneeId(), ticket.getAssigneeId())) {
            throw exception(TICKET_ASSIGNEE_SAME);
        }
        AdminUserRespDTO assignee = adminUserApi.getUser(reqVO.getAssigneeId());
        if (assignee == null) {
            throw exception(TICKET_ASSIGNEE_NOT_EXISTS);
        }
        Integer toStatus = TicketStateMachine.checkTransition(ticket.getStatus(), TicketActionEnum.ASSIGN);

        Long fromAssigneeId = ticket.getAssigneeId();
        TicketDO update = new TicketDO();
        update.setId(ticket.getId());
        update.setAssigneeId(assignee.getId());
        update.setAssigneeName(assignee.getNickname());
        update.setAssigneeDeptId(assignee.getDeptId());
        update.setStatus(toStatus);
        ticketMapper.updateById(update);

        writeLog(ticket.getId(), TicketActionEnum.ASSIGN, ticket.getStatus(), toStatus,
                fromAssigneeId, assignee.getId(),
                reqVO.getRemark() == null ? "分派给 " + assignee.getNickname() : reqVO.getRemark());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void acceptTicket(TicketAcceptReqVO reqVO) {
        TicketDO ticket = mustExist(reqVO.getId());
        Long currentUserId = SecurityFrameworkUtils.getLoginUserId();

        // 1. 鉴权：必须是 ticket.dept_id 的部门负责人（或 super_admin）
        ensureDeptLeaderOrAdmin(ticket.getDeptId(), currentUserId);

        // 2. 状态机校验 0 → 1
        Integer toStatus = TicketStateMachine.checkTransition(ticket.getStatus(), TicketActionEnum.ACCEPT);

        // 3. 执行人列表：去重 + 去 null + 必须 ≥ 1
        List<Long> executorIds = sanitizeExecutorIds(reqVO.getExecutorIds());

        // 4. 批量校验执行人存在
        List<AdminUserRespDTO> executors = adminUserApi.getUserList(executorIds);
        Map<Long, AdminUserRespDTO> executorMap = executors.stream()
                .collect(Collectors.toMap(AdminUserRespDTO::getId, u -> u, (a, b) -> a));
        List<Long> missing = executorIds.stream()
                .filter(uid -> !executorMap.containsKey(uid))
                .collect(Collectors.toList());
        if (!missing.isEmpty()) {
            throw exception(TICKET_EXECUTOR_NOT_EXISTS, missing);
        }

        // 4.5 范围校验：执行人只能是接单人自己或其负责部门（含子部门）的成员；超管豁免。
        //     前端下拉已按同口径过滤，这里兜底防直接调接口越权指派。
        if (!isSuperAdmin(currentUserId)) {
            Set<Long> ledDeptIds = resolveLedDeptIds(currentUserId);
            for (Long executorId : executorIds) {
                AdminUserRespDTO executor = executorMap.get(executorId);
                boolean isSelf = Objects.equals(executor.getId(), currentUserId);
                boolean inScope = executor.getDeptId() != null
                        && ledDeptIds.contains(executor.getDeptId());
                if (!isSelf && !inScope) {
                    throw exception(TICKET_EXECUTOR_OUT_OF_SCOPE, executor.getNickname());
                }
            }
        }

        // 5. 回写 assignee_id（主管自己作为处理人入口；多执行人在 executor 表）
        AdminUserRespDTO acceptor = adminUserApi.getUser(currentUserId);
        if (acceptor == null) {
            throw exception(TICKET_NOT_DEPT_LEADER);
        }
        Long fromAssigneeId = ticket.getAssigneeId();
        TicketDO update = new TicketDO();
        update.setId(ticket.getId());
        update.setAssigneeId(acceptor.getId());
        update.setAssigneeName(acceptor.getNickname());
        update.setAssigneeDeptId(acceptor.getDeptId());
        update.setStatus(toStatus);
        update.setFirstResponseTime(LocalDateTime.now());
        ticketMapper.updateById(update);

        // 6. 写执行人表（按顺序 insert；唯一索引兜底）
        for (Long executorId : executorIds) {
            AdminUserRespDTO u = executorMap.get(executorId);
            TicketExecutorDO row = TicketExecutorDO.builder()
                    .ticketId(ticket.getId())
                    .userId(executorId)
                    .userName(u.getNickname())
                    .userDeptId(u.getDeptId())
                    .status(0)
                    .assignedBy(currentUserId)
                    .remark(reqVO.getRemark())
                    .build();
            executorMapper.insert(row);
        }

        // 7. 写 log
        writeLog(ticket.getId(), TicketActionEnum.ACCEPT, ticket.getStatus(), toStatus,
                fromAssigneeId, acceptor.getId(),
                reqVO.getRemark() == null
                        ? "接单并指派 " + executorIds.size() + " 名执行人"
                        : reqVO.getRemark());

        // 7.5 钉钉通知执行人 + 提单人
        String executorNames = executorIds.stream()
                .map(uid -> executorMap.get(uid).getNickname())
                .collect(Collectors.joining("、"));
        List<Long> acceptNotifyTargets = new ArrayList<>(executorIds);
        acceptNotifyTargets.add(ticket.getCreatorId());
        notifyDingtalk(ticket, acceptNotifyTargets, "工单已接单派工",
                String.format("- 接单人：%s%n- 执行人：%s%n%n请执行人尽快开始处理",
                        acceptor.getNickname(), executorNames));

        // 8. 发事件供业务驱动器消费（同步发布；异常会回滚事务）
        TicketAcceptedEvent event = TicketAcceptedEvent.builder()
                .ticketId(ticket.getId())
                .ticketNo(ticket.getTicketNo())
                .title(ticket.getTitle())
                .businessType(ticket.getBusinessType())
                .businessId(ticket.getBusinessId())
                .serviceItemId(ticket.getServiceItemId())
                .deptId(ticket.getDeptId())
                .creatorId(ticket.getCreatorId())
                .creatorName(ticket.getCreatorName())
                .acceptedBy(currentUserId)
                .acceptedByName(acceptor.getNickname())
                .executorIds(executorIds)
                .remark(reqVO.getRemark())
                .extJson(ticket.getExtJson())
                .build();
        try {
            eventPublisher.publishEvent(event);
        } catch (ServiceException ex) {
            // 业务驱动器抛 ServiceException：原样冒泡（让全局错码生效），事务回滚
            throw ex;
        } catch (RuntimeException ex) {
            // 其它运行时异常：包成 TICKET_DRIVER_FAILED；事务仍然回滚（因抛出未捕获异常）
            log.error("【工单接单】业务驱动器处理失败 ticketId={} businessType={}: {}",
                    ticket.getId(), ticket.getBusinessType(), ex.getMessage(), ex);
            throw exception(TICKET_DRIVER_FAILED, ex.getMessage() == null ? "未知错误" : ex.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startTicket(Long id) {
        TicketDO ticket = mustExist(id);
        Long currentUserId = SecurityFrameworkUtils.getLoginUserId();
        if (!Objects.equals(ticket.getAssigneeId(), currentUserId)) {
            throw exception(TICKET_NOT_ASSIGNEE);
        }
        Integer toStatus = TicketStateMachine.checkTransition(ticket.getStatus(), TicketActionEnum.START);

        TicketDO update = new TicketDO();
        update.setId(ticket.getId());
        update.setStatus(toStatus);
        update.setFirstResponseTime(LocalDateTime.now());
        ticketMapper.updateById(update);

        writeLog(ticket.getId(), TicketActionEnum.START, ticket.getStatus(), toStatus,
                ticket.getAssigneeId(), ticket.getAssigneeId(), "接单开始处理");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finishTicket(TicketFinishReqVO reqVO) {
        TicketDO ticket = mustExist(reqVO.getId());
        Long currentUserId = SecurityFrameworkUtils.getLoginUserId();
        if (!Objects.equals(ticket.getAssigneeId(), currentUserId)
                && !isTicketExecutor(ticket.getId(), currentUserId)
                && !isSuperAdmin(currentUserId)) {
            throw exception(TICKET_NOT_ASSIGNEE);
        }
        // 1 → 2 进入待验收；提交服务结果即代表执行完成
        Integer toStatus = TicketStateMachine.checkTransition(ticket.getStatus(), TicketActionEnum.FINISH);

        TicketDO update = new TicketDO();
        update.setId(ticket.getId());
        update.setStatus(toStatus);
        update.setFinishTime(LocalDateTime.now());
        ticketMapper.updateById(update);
        executorMapper.updateStatusByTicketId(ticket.getId(), 1);

        writeLog(ticket.getId(), TicketActionEnum.FINISH, ticket.getStatus(), toStatus,
                ticket.getAssigneeId(), ticket.getAssigneeId(), reqVO.getResult());
        publishLifecycleEvent(ticket, TicketActionEnum.FINISH, reqVO.getResult());

        notifyDingtalk(ticket, Collections.singletonList(ticket.getCreatorId()),
                "工单待验收",
                String.format("- 处理结果：%s%n%n执行已完成，请尽快验收（通过 / 驳回）",
                        truncateForNotify(reqVO.getResult())));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewPassTicket(TicketReviewPassReqVO reqVO) {
        TicketDO ticket = mustExist(reqVO.getId());
        Long currentUserId = SecurityFrameworkUtils.getLoginUserId();
        ensureCreatorOrAdmin(ticket, currentUserId);
        Integer toStatus = TicketStateMachine.checkTransition(ticket.getStatus(), TicketActionEnum.REVIEW_PASS);

        AdminUserRespDTO reviewer = adminUserApi.getUser(currentUserId);
        TicketDO update = new TicketDO();
        update.setId(ticket.getId());
        update.setStatus(toStatus);
        update.setReviewerId(currentUserId);
        update.setReviewerName(reviewer == null ? null : reviewer.getNickname());
        update.setReviewTime(LocalDateTime.now());
        update.setReviewComment(reqVO.getComment());
        ticketMapper.updateById(update);

        writeLog(ticket.getId(), TicketActionEnum.REVIEW_PASS, ticket.getStatus(), toStatus,
                ticket.getAssigneeId(), ticket.getAssigneeId(),
                reqVO.getComment() == null ? "验收通过" : reqVO.getComment());
        // “提交结果”只代表执行人已交付，必须由提单人验收通过后，业务轮次才正式完成。
        publishLifecycleEvent(ticket, TicketActionEnum.REVIEW_PASS, reqVO.getComment());

        notifyDingtalk(ticket, Collections.singletonList(ticket.getAssigneeId()),
                "工单验收通过",
                String.format("- 验收人：%s%n- 验收意见：%s",
                        reviewer == null ? "-" : reviewer.getNickname(),
                        reqVO.getComment() == null ? "无" : truncateForNotify(reqVO.getComment())));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewRejectTicket(TicketReviewRejectReqVO reqVO) {
        TicketDO ticket = mustExist(reqVO.getId());
        Long currentUserId = SecurityFrameworkUtils.getLoginUserId();
        ensureCreatorOrAdmin(ticket, currentUserId);
        // 2 → 1 退回原执行人重做；dueTime 不重置（驳回重做仍占用原 SLA）
        Integer toStatus = TicketStateMachine.checkTransition(ticket.getStatus(), TicketActionEnum.REVIEW_REJECT);

        AdminUserRespDTO reviewer = adminUserApi.getUser(currentUserId);
        TicketDO update = new TicketDO();
        update.setId(ticket.getId());
        update.setStatus(toStatus);
        update.setReviewerId(currentUserId);
        update.setReviewerName(reviewer == null ? null : reviewer.getNickname());
        update.setReviewTime(LocalDateTime.now());
        update.setReviewComment(reqVO.getReason());
        ticketMapper.updateById(update);
        // 验收驳回后回到执行中，上一次交付的完成时间不再代表当前状态
        ticketMapper.update(null, new LambdaUpdateWrapper<TicketDO>()
                .eq(TicketDO::getId, ticket.getId())
                .set(TicketDO::getFinishTime, null));
        executorMapper.updateStatusByTicketId(ticket.getId(), 0);

        writeLog(ticket.getId(), TicketActionEnum.REVIEW_REJECT, ticket.getStatus(), toStatus,
                ticket.getAssigneeId(), ticket.getAssigneeId(), reqVO.getReason());
        publishLifecycleEvent(ticket, TicketActionEnum.REVIEW_REJECT, reqVO.getReason());

        notifyDingtalk(ticket, Collections.singletonList(ticket.getAssigneeId()),
                "工单验收驳回",
                String.format("- 驳回原因：%s%n%n工单已退回，请重新处理后再次提交",
                        truncateForNotify(reqVO.getReason())));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeTicket(Long id) {
        TicketDO ticket = mustExist(id);
        Long currentUserId = SecurityFrameworkUtils.getLoginUserId();
        if (!Objects.equals(ticket.getCreatorId(), currentUserId)
                && !isSuperAdmin(currentUserId)) {
            throw exception(TICKET_NO_PERMISSION);
        }
        Integer toStatus = TicketStateMachine.checkTransition(ticket.getStatus(), TicketActionEnum.CLOSE);

        TicketDO update = new TicketDO();
        update.setId(ticket.getId());
        update.setStatus(toStatus);
        update.setCloseTime(LocalDateTime.now());
        ticketMapper.updateById(update);

        writeLog(ticket.getId(), TicketActionEnum.CLOSE, ticket.getStatus(), toStatus,
                ticket.getAssigneeId(), ticket.getAssigneeId(), "关闭工单");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reopenTicket(TicketReopenReqVO reqVO) {
        TicketDO ticket = mustExist(reqVO.getId());
        Long currentUserId = SecurityFrameworkUtils.getLoginUserId();
        ensureCreatorOrAdmin(ticket, currentUserId);
        Integer toStatus = TicketStateMachine.checkTransition(ticket.getStatus(), TicketActionEnum.REOPEN);

        // 窗口期：finishTime/closeTime 起 REOPEN_WINDOW_DAYS 天内
        if (!isWithinReopenWindow(ticket)) {
            throw exception(TICKET_REOPEN_EXPIRED, REOPEN_WINDOW_DAYS);
        }
        // 次数上限
        int reopenCount = ticket.getReopenCount() == null ? 0 : ticket.getReopenCount();
        if (reopenCount >= REOPEN_MAX_COUNT) {
            throw exception(TICKET_REOPEN_LIMIT, REOPEN_MAX_COUNT);
        }

        // 保留原 assignee/executors；清空完成/关闭/验收痕迹（updateById 不更新 null，须用 UpdateWrapper）
        ticketMapper.update(null, new LambdaUpdateWrapper<TicketDO>()
                .eq(TicketDO::getId, ticket.getId())
                .set(TicketDO::getStatus, toStatus)
                .set(TicketDO::getFinishTime, null)
                .set(TicketDO::getCloseTime, null)
                .set(TicketDO::getReviewerId, null)
                .set(TicketDO::getReviewerName, null)
                .set(TicketDO::getReviewTime, null)
                .set(TicketDO::getReviewComment, null)
                .set(TicketDO::getReopenCount, reopenCount + 1));
        executorMapper.updateStatusByTicketId(ticket.getId(), 0);

        writeLog(ticket.getId(), TicketActionEnum.REOPEN, ticket.getStatus(), toStatus,
                ticket.getAssigneeId(), ticket.getAssigneeId(),
                "第 " + (reopenCount + 1) + " 次重开：" + reqVO.getReason());
        publishLifecycleEvent(ticket, TicketActionEnum.REOPEN, reqVO.getReason());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void returnTicket(TicketReturnReqVO reqVO) {
        TicketDO ticket = mustExist(reqVO.getId());
        Long currentUserId = SecurityFrameworkUtils.getLoginUserId();
        // 与 accept 同权限：工单归属部门负责人（或超管）
        ensureDeptLeaderOrAdmin(ticket.getDeptId(), currentUserId);
        Integer toStatus = TicketStateMachine.checkTransition(ticket.getStatus(), TicketActionEnum.RETURN);

        TicketDO update = new TicketDO();
        update.setId(ticket.getId());
        update.setStatus(toStatus);
        update.setReturnReason(reqVO.getReason());
        ticketMapper.updateById(update);

        writeLog(ticket.getId(), TicketActionEnum.RETURN, ticket.getStatus(), toStatus,
                ticket.getAssigneeId(), ticket.getAssigneeId(), reqVO.getReason());

        notifyDingtalk(ticket, Collections.singletonList(ticket.getCreatorId()),
                "工单被退回",
                String.format("- 退回原因：%s%n%n您可修改工单（含派单部门）后重新提交，或取消工单",
                        truncateForNotify(reqVO.getReason())));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resubmitTicket(Long id) {
        TicketDO ticket = mustExist(id);
        Long currentUserId = SecurityFrameworkUtils.getLoginUserId();
        ensureCreatorOrAdmin(ticket, currentUserId);
        Integer toStatus = TicketStateMachine.checkTransition(ticket.getStatus(), TicketActionEnum.RESUBMIT);

        ticketMapper.update(null, new LambdaUpdateWrapper<TicketDO>()
                .eq(TicketDO::getId, ticket.getId())
                .set(TicketDO::getStatus, toStatus)
                .set(TicketDO::getReturnReason, null));

        writeLog(ticket.getId(), TicketActionEnum.RESUBMIT, ticket.getStatus(), toStatus,
                ticket.getAssigneeId(), ticket.getAssigneeId(), "修改后重新提交");

        // 重新提交后派单部门可能已被修改，重读最新数据再通知负责人
        TicketDO latest = ticketMapper.selectById(ticket.getId());
        notifyDingtalk(latest, Collections.singletonList(resolveDeptLeaderId(latest.getDeptId())),
                "工单重新提交待接单",
                String.format("- 提单人：%s%n%n退回工单已修改并重新提交，请尽快接单派工", latest.getCreatorName()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelTicket(Long id) {
        TicketDO ticket = mustExist(id);
        Long currentUserId = SecurityFrameworkUtils.getLoginUserId();
        if (!Objects.equals(ticket.getCreatorId(), currentUserId)
                && !isSuperAdmin(currentUserId)) {
            throw exception(TICKET_NO_PERMISSION);
        }
        Integer toStatus = TicketStateMachine.checkTransition(ticket.getStatus(), TicketActionEnum.CANCEL);

        TicketDO update = new TicketDO();
        update.setId(ticket.getId());
        update.setStatus(toStatus);
        ticketMapper.updateById(update);

        writeLog(ticket.getId(), TicketActionEnum.CANCEL, ticket.getStatus(), toStatus,
                ticket.getAssigneeId(), ticket.getAssigneeId(), "取消工单");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transferTicket(TicketTransferReqVO reqVO) {
        TicketDO ticket = mustExist(reqVO.getId());
        Long currentUserId = SecurityFrameworkUtils.getLoginUserId();
        // 当前 assignee 或超管可转交（一期暂不开放给部门负责人，避免越权）
        if (!Objects.equals(ticket.getAssigneeId(), currentUserId)
                && !isSuperAdmin(currentUserId)) {
            throw exception(TICKET_NO_PERMISSION);
        }
        if (Objects.equals(reqVO.getNewAssigneeId(), ticket.getAssigneeId())) {
            throw exception(TICKET_ASSIGNEE_SAME);
        }
        AdminUserRespDTO newAssignee = adminUserApi.getUser(reqVO.getNewAssigneeId());
        if (newAssignee == null) {
            throw exception(TICKET_ASSIGNEE_NOT_EXISTS);
        }
        Integer toStatus = TicketStateMachine.checkTransition(ticket.getStatus(), TicketActionEnum.TRANSFER);

        Long fromAssigneeId = ticket.getAssigneeId();
        TicketDO update = new TicketDO();
        update.setId(ticket.getId());
        update.setAssigneeId(newAssignee.getId());
        update.setAssigneeName(newAssignee.getNickname());
        update.setAssigneeDeptId(newAssignee.getDeptId());
        update.setStatus(toStatus);
        ticketMapper.updateById(update);

        writeLog(ticket.getId(), TicketActionEnum.TRANSFER, ticket.getStatus(), toStatus,
                fromAssigneeId, newAssignee.getId(), reqVO.getReason());
    }

    // ========== 详情扩展 ==========

    @Override
    public List<TicketLogDO> getTicketLogs(Long ticketId) {
        List<TicketLogDO> list = logMapper.selectListByTicketId(ticketId);
        return list == null ? Collections.emptyList() : list;
    }

    @Override
    public List<TicketExecutorDO> getTicketExecutors(Long ticketId) {
        if (ticketId == null) {
            return Collections.emptyList();
        }
        List<TicketExecutorDO> list = executorMapper.selectListByTicketId(ticketId);
        return list == null ? Collections.emptyList() : list;
    }

    @Override
    public List<String> calculateAvailableActions(TicketDO ticket, Long currentUserId) {
        if (ticket == null) {
            return Collections.emptyList();
        }
        boolean isCreator = Objects.equals(ticket.getCreatorId(), currentUserId);
        boolean isAssignee = currentUserId != null && Objects.equals(ticket.getAssigneeId(), currentUserId);
        boolean isExecutor = isTicketExecutor(ticket.getId(), currentUserId);
        boolean isAdmin = isSuperAdmin(currentUserId);
        boolean isDeptLeader = isDeptLeader(ticket.getDeptId(), currentUserId);
        Integer status = ticket.getStatus();
        List<String> actions = new ArrayList<>();
        // comment 任意可访问者都能评论
        if (isCreator || isAssignee || isExecutor || isAdmin || isDeptLeader) {
            actions.add(TicketActionEnum.COMMENT.getAction());
        }
        // 基础 update：status=0 / 6（退回后修改再重提）且是提单人或管理员
        if ((TicketStatusEnum.PENDING.getStatus().equals(status)
                || TicketStatusEnum.RETURNED.getStatus().equals(status))
                && (isCreator || isAdmin)) {
            actions.add("update");
        }
        // 分派：status=0 且是管理员
        if (canDo(status, TicketActionEnum.ASSIGN) && isAdmin) {
            actions.add(TicketActionEnum.ASSIGN.getAction());
        }
        // 主管接单：status=0 且当前用户是工单 dept_id 的部门负责人（或管理员）
        if (canDo(status, TicketActionEnum.ACCEPT) && (isDeptLeader || isAdmin)) {
            actions.add(TicketActionEnum.ACCEPT.getAction());
        }
        // 拒单退回：status=0 且是部门负责人（或管理员）
        if (canDo(status, TicketActionEnum.RETURN) && (isDeptLeader || isAdmin)) {
            actions.add("return");
        }
        // 接单：status=0 且是 assignee
        if (canDo(status, TicketActionEnum.START) && isAssignee) {
            actions.add(TicketActionEnum.START.getAction());
        }
        // 完成（提交验收）：任一被指派执行人、当前负责人或管理员
        if (canDo(status, TicketActionEnum.FINISH) && (isAssignee || isExecutor || isAdmin)) {
            actions.add(TicketActionEnum.FINISH.getAction());
        }
        // 验收：status=2 且是提单人或管理员（actions 用 camelCase 与前端约定一致）
        if (canDo(status, TicketActionEnum.REVIEW_PASS) && (isCreator || isAdmin)) {
            actions.add("reviewPass");
            actions.add("reviewReject");
        }
        // 关闭：status=3 且是提单人或管理员
        if (canDo(status, TicketActionEnum.CLOSE) && (isCreator || isAdmin)) {
            actions.add(TicketActionEnum.CLOSE.getAction());
        }
        // 重开：status=3/4 且是提单人或管理员，且窗口期内、未超次数
        if (canDo(status, TicketActionEnum.REOPEN) && (isCreator || isAdmin)
                && isWithinReopenWindow(ticket)
                && (ticket.getReopenCount() == null || ticket.getReopenCount() < REOPEN_MAX_COUNT)) {
            actions.add(TicketActionEnum.REOPEN.getAction());
        }
        // 重新提交：status=6 且是提单人或管理员
        if (canDo(status, TicketActionEnum.RESUBMIT) && (isCreator || isAdmin)) {
            actions.add("resubmit");
        }
        // 取消：status=0/6 且是提单人或管理员
        if (canDo(status, TicketActionEnum.CANCEL) && (isCreator || isAdmin)) {
            actions.add(TicketActionEnum.CANCEL.getAction());
        }
        // 转交：非终态 + assignee 或管理员
        if (canDo(status, TicketActionEnum.TRANSFER) && (isAssignee || isAdmin)) {
            actions.add(TicketActionEnum.TRANSFER.getAction());
        }
        // 删除：仅管理员
        if (isAdmin) {
            actions.add("delete");
        }
        return actions;
    }

    @Override
    public TicketDO validateTicketAccess(Long ticketId, Long currentUserId) {
        TicketDO ticket = mustExist(ticketId);
        if (Objects.equals(ticket.getCreatorId(), currentUserId)
                || Objects.equals(ticket.getAssigneeId(), currentUserId)
                || isTicketExecutor(ticket.getId(), currentUserId)
                || isSuperAdmin(currentUserId)
                // 工单归属部门负责人需进详情接单 / 拒单
                || isDeptLeader(ticket.getDeptId(), currentUserId)) {
            return ticket;
        }
        throw exception(TICKET_NO_PERMISSION);
    }

    // ========== Helpers ==========

    private TicketDO mustExist(Long id) {
        if (id == null) {
            throw exception(TICKET_NOT_EXISTS);
        }
        TicketDO ticket = ticketMapper.selectById(id);
        if (ticket == null) {
            throw exception(TICKET_NOT_EXISTS);
        }
        return ticket;
    }

    private TicketServiceContext resolveServiceContext(Long serviceItemId, Long userId) {
        if (serviceItemId == null) {
            throw exception(TICKET_SERVICE_ITEM_REQUIRED);
        }
        return serviceContextResolver.resolve(serviceItemId, userId);
    }

    private Map<String, Object> buildServiceSnapshot(Map<String, Object> requestExt,
                                                     TicketServiceContext context) {
        Map<String, Object> ext = new LinkedHashMap<>();
        if (requestExt != null) {
            // 只保留用户可填的执行参数，权限与路由字段在下方强制覆盖。
            copyExt(requestExt, ext, "isOutside", "destination", "reason",
                    "planStartTime", "planEndTime");
        }
        ext.put("serviceItemId", context.getServiceItemId());
        ext.put("serviceItemCode", context.getServiceItemCode());
        ext.put("serviceType", context.getServiceType());
        ext.put("serviceTypeName", context.getServiceTypeName());
        ext.put("serviceMode", context.getServiceMode());
        ext.put("serviceMemberType", context.getServiceMemberType());
        ext.put("deptType", context.getDeptType());
        ext.put("projectId", context.getProjectId());
        ext.put("projectCode", context.getProjectCode());
        ext.put("projectName", context.getProjectName());
        ext.put("executeDeptId", context.getResponsibleDeptId());
        ext.put("responsibleDeptName", context.getResponsibleDeptName());
        ext.put("customerId", context.getCustomerId());
        ext.put("customerName", context.getCustomerName());
        ext.put("contractId", context.getContractId());
        ext.put("contractNo", context.getContractNo());
        ext.put("serviceSourceType", context.getSourceType());
        ext.values().removeIf(Objects::isNull);
        return ext;
    }

    private void copyExt(Map<String, Object> source, Map<String, Object> target, String... keys) {
        for (String key : keys) {
            if (source.containsKey(key)) {
                target.put(key, source.get(key));
            }
        }
    }

    private boolean isTicketExecutor(Long ticketId, Long userId) {
        return ticketId != null && userId != null
                && executorMapper.existsByTicketIdAndUserId(ticketId, userId);
    }

    private boolean isSuperAdmin(Long userId) {
        return userId != null && permissionApi.hasAnyRoles(userId, ROLE_SUPER_ADMIN);
    }

    /**
     * 鉴权：当前用户必须是工单提单人，或 super_admin（验收 / 重开 / 重新提交入口用）。
     */
    private void ensureCreatorOrAdmin(TicketDO ticket, Long currentUserId) {
        if (isSuperAdmin(currentUserId)) {
            return;
        }
        if (!Objects.equals(ticket.getCreatorId(), currentUserId)) {
            throw exception(TICKET_NO_PERMISSION);
        }
    }

    /**
     * 重开窗口判定：以 closeTime（已关闭）或 finishTime（已完成）为基准，{@code REOPEN_WINDOW_DAYS}
     * 天内允许；基准时间缺失时放行（数据异常不阻断业务）。
     */
    private boolean isWithinReopenWindow(TicketDO ticket) {
        LocalDateTime base = TicketStatusEnum.CLOSED.getStatus().equals(ticket.getStatus())
                ? ticket.getCloseTime()
                : ticket.getFinishTime();
        if (base == null) {
            return true;
        }
        return base.plusDays(REOPEN_WINDOW_DAYS).isAfter(LocalDateTime.now());
    }

    /**
     * 鉴权：当前用户必须是 {@code deptId} 部门负责人，或 super_admin。
     * 否则抛 {@link cn.shuhe.system.module.ticket.enums.ErrorCodeConstants#TICKET_NOT_DEPT_LEADER}。
     */
    private void ensureDeptLeaderOrAdmin(Long deptId, Long currentUserId) {
        if (isSuperAdmin(currentUserId)) {
            return;
        }
        if (!isDeptLeader(deptId, currentUserId)) {
            throw exception(TICKET_NOT_DEPT_LEADER);
        }
    }

    /**
     * 当前用户是否为 {@code deptId} 部门的负责人。仅用于鉴权 + actions 计算，不抛异常。
     *
     * <p>本级未配置负责人时<b>递归向上</b>取最近一级负责人，与前端派单部门选择器
     * （resolveDeptLeaderName）及借调模块（findLeaderUserIdRecursively）语义对齐；
     * 否则会出现「表单显示负责人是 A，A 打开工单却报无权操作」的割裂。
     */
    private boolean isDeptLeader(Long deptId, Long currentUserId) {
        if (deptId == null || currentUserId == null) {
            return false;
        }
        try {
            Long leaderUserId = deptApi.findLeaderUserIdRecursively(deptId);
            return Objects.equals(leaderUserId, currentUserId);
        } catch (Exception ex) {
            // actions 计算阶段对外部依赖失败容错；不要因为部门查询抖动让整个详情页崩溃
            log.warn("【工单】查询部门负责人失败 deptId={} userId={}: {}",
                    deptId, currentUserId, ex.getMessage());
            return false;
        }
    }

    /**
     * 清洗执行人 ID 列表：去 null、去重；空时抛 {@link
     * cn.shuhe.system.module.ticket.enums.ErrorCodeConstants#TICKET_EXECUTOR_EMPTY}。
     * 保留首次出现的顺序便于审计。
     */
    private List<Long> sanitizeExecutorIds(List<Long> raw) {
        if (raw == null || raw.isEmpty()) {
            throw exception(TICKET_EXECUTOR_EMPTY);
        }
        Set<Long> dedup = new LinkedHashSet<>();
        for (Long id : raw) {
            if (id != null) {
                dedup.add(id);
            }
        }
        if (dedup.isEmpty()) {
            throw exception(TICKET_EXECUTOR_EMPTY);
        }
        return new ArrayList<>(dedup);
    }

    private boolean canDo(Integer status, TicketActionEnum action) {
        return TicketStateMachine.nextStatus(status, action) != null;
    }

    /**
     * 生成 {@code TKyyyyMMdd + 3位流水} 工单号；同一天内并发用唯一索引兜底重试。
     */
    private String generateTicketNo() {
        LocalDate today = LocalDate.now();
        String dayPart = today.format(TICKET_NO_DAY_FMT);
        LocalDateTime dayStart = LocalDateTime.of(today, LocalTime.MIN);
        LocalDateTime dayEnd = dayStart.plusDays(1);
        for (int attempt = 0; attempt < TICKET_NO_GENERATE_RETRIES; attempt++) {
            int next = nextDailySequence(dayStart, dayEnd, dayPart);
            String candidate = "TK" + dayPart + String.format("%03d", next);
            if (ticketMapper.selectByTicketNo(candidate) == null) {
                return candidate;
            }
        }
        throw exception(TICKET_NO_GENERATE_FAIL);
    }

    private int nextDailySequence(LocalDateTime dayStart, LocalDateTime dayEnd, String dayPart) {
        TicketDO latest = ticketMapper.selectLatestByDay(dayStart, dayEnd);
        if (latest == null || latest.getTicketNo() == null
                || !latest.getTicketNo().startsWith("TK" + dayPart)) {
            return 1;
        }
        String tail = latest.getTicketNo().substring(2 + dayPart.length());
        try {
            return Integer.parseInt(tail) + 1;
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    /** 钉钉通道开关：{@code notify_channels} 为空或包含 dingtalk 才发。 */
    private boolean dingtalkEnabled(TicketDO ticket) {
        String channels = ticket.getNotifyChannels();
        return channels == null || channels.contains("dingtalk");
    }

    /**
     * 发钉钉工作通知。同步发送、异常仅告警不抛出（与 BpmMessageServiceImpl 风格一致），
     * 不影响主事务。
     */
    private void notifyDingtalk(TicketDO ticket, List<Long> userIds, String title, String detail) {
        if (!dingtalkEnabled(ticket)) {
            return;
        }
        List<Long> targets = userIds == null
                ? Collections.emptyList()
                : userIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (targets.isEmpty()) {
            return;
        }
        try {
            String content = String.format("**%s**%n%n- 工单号：%s%n- 标题：%s%n%s",
                    title, ticket.getTicketNo(), ticket.getTitle(), detail == null ? "" : detail);
            dingtalkNotifyApi.sendWorkNotice(new DingtalkNotifySendReqDTO()
                    .setUserIds(targets)
                    .setTitle(title)
                    .setContent(content));
        } catch (Exception ex) {
            log.warn("【工单】钉钉通知发送失败 ticketId={} title={}: {}",
                    ticket.getId(), title, ex.getMessage());
        }
    }

    /**
     * 取部门负责人 userId（容错，查不到返回 null）。
     *
     * <p>与 {@link #isDeptLeader} 同语义：本级未配置时递归向上，保证钉钉「待接单」
     * 通知发给实际有接单权限的人（派单部门未配置负责人时通知曾经发不出去）。
     */
    private Long resolveDeptLeaderId(Long deptId) {
        if (deptId == null) {
            return null;
        }
        try {
            return deptApi.findLeaderUserIdRecursively(deptId);
        } catch (Exception ex) {
            log.warn("【工单】查询部门负责人失败 deptId={}: {}", deptId, ex.getMessage());
            return null;
        }
    }

    /** 通知内容截断（处理结果等长文本防爆通知卡片）。 */
    private String truncateForNotify(String text) {
        if (text == null) {
            return "";
        }
        return text.length() <= 200 ? text : text.substring(0, 200) + "…";
    }

    private void writeLog(Long ticketId, TicketActionEnum action,
                          Integer fromStatus, Integer toStatus,
                          Long fromAssigneeId, Long toAssigneeId,
                          String content) {
        Long operatorId = SecurityFrameworkUtils.getLoginUserId();
        String operatorName = SecurityFrameworkUtils.getLoginUserNickname();
        if (operatorName == null && operatorId != null) {
            AdminUserRespDTO user = adminUserApi.getUser(operatorId);
            if (user != null) {
                operatorName = user.getNickname();
            }
        }
        TicketLogDO logDO = TicketLogDO.builder()
                .ticketId(ticketId)
                .operatorId(operatorId)
                .operatorName(operatorName)
                .action(action.getAction())
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .fromAssigneeId(fromAssigneeId)
                .toAssigneeId(toAssigneeId)
                .content(content)
                .build();
        try {
            logMapper.insert(logDO);
        } catch (ServiceException ex) {
            log.warn("写工单日志失败 ticketId={} action={}: {}", ticketId, action, ex.getMessage());
        }
    }

    /** 同步发布履约状态事件；业务监听器失败时由当前工单事务统一回滚。 */
    private void publishLifecycleEvent(TicketDO ticket, TicketActionEnum action, String result) {
        eventPublisher.publishEvent(TicketLifecycleEvent.builder()
                .ticketId(ticket.getId())
                .ticketNo(ticket.getTicketNo())
                .businessType(ticket.getBusinessType())
                .businessId(ticket.getBusinessId())
                .action(action.getAction())
                .result(result)
                .build());
    }
}
