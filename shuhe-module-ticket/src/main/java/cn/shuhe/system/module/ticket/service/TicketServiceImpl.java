package cn.shuhe.system.module.ticket.service;

import cn.shuhe.system.framework.common.exception.ServiceException;
import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.datapermission.core.annotation.DataPermission;
import cn.shuhe.system.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.shuhe.system.framework.security.core.util.SecurityFrameworkUtils;
import cn.shuhe.system.module.system.api.dept.DeptApi;
import cn.shuhe.system.module.system.api.dept.dto.DeptRespDTO;
import cn.shuhe.system.module.system.api.permission.PermissionApi;
import cn.shuhe.system.module.system.api.user.AdminUserApi;
import cn.shuhe.system.module.system.api.user.dto.AdminUserRespDTO;
import cn.shuhe.system.module.ticket.controller.admin.vo.TicketAcceptReqVO;
import cn.shuhe.system.module.ticket.controller.admin.vo.TicketAssignReqVO;
import cn.shuhe.system.module.ticket.controller.admin.vo.TicketFinishReqVO;
import cn.shuhe.system.module.ticket.controller.admin.vo.TicketPageReqVO;
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
import cn.shuhe.system.module.ticket.framework.statemachine.TicketStateMachine;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.shuhe.system.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_ASSIGNEE_NOT_EXISTS;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_ASSIGNEE_REQUIRED;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_ASSIGNEE_SAME;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_BUSINESS_TYPE_INVALID;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_CATEGORY_DISABLED;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_CATEGORY_NOT_EXISTS;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_CREATOR_DEPT_MISSING;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_DRIVER_FAILED;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_EXECUTOR_EMPTY;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_EXECUTOR_NOT_EXISTS;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_NOT_ASSIGNEE;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_NOT_DEPT_LEADER;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_NOT_EXISTS;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_NOT_OWN;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_NO_GENERATE_FAIL;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_NO_PERMISSION;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_STATUS_INVALID;

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

    // ========== CRUD ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTicket(TicketSaveReqVO createReqVO) {
        // 1. 业务工单一期禁写
        String businessType = createReqVO.getBusinessType();
        if (businessType == null) {
            businessType = TicketBusinessTypeEnum.GENERAL.getType();
        }
        if (!TicketBusinessTypeEnum.isWriteAllowed(businessType)) {
            throw exception(TICKET_BUSINESS_TYPE_INVALID);
        }

        // 2. 校验分类（如果有指定）
        if (createReqVO.getCategoryId() != null) {
            TicketCategoryDO category = categoryMapper.selectById(createReqVO.getCategoryId());
            if (category == null) {
                throw exception(TICKET_CATEGORY_NOT_EXISTS);
            }
            if (category.getStatus() != null && category.getStatus() == 1) {
                throw exception(TICKET_CATEGORY_DISABLED);
            }
        }

        // 3. 校验提单人
        Long creatorUserId = SecurityFrameworkUtils.getLoginUserId();
        AdminUserRespDTO creator = creatorUserId == null ? null : adminUserApi.getUser(creatorUserId);
        if (creator == null) {
            throw exception(TICKET_CREATOR_DEPT_MISSING);
        }
        if (creator.getDeptId() == null && createReqVO.getDeptId() == null) {
            throw exception(TICKET_CREATOR_DEPT_MISSING);
        }

        // 4. 校验指定的处理人
        AdminUserRespDTO assignee = null;
        if (createReqVO.getAssigneeId() != null) {
            assignee = adminUserApi.getUser(createReqVO.getAssigneeId());
            if (assignee == null) {
                throw exception(TICKET_ASSIGNEE_NOT_EXISTS);
            }
        }

        // 5. 装配 DO
        TicketDO ticket = TicketDO.builder()
                .title(createReqVO.getTitle())
                .content(createReqVO.getContent())
                .categoryId(createReqVO.getCategoryId())
                .priority(createReqVO.getPriority() == null ? 1 : createReqVO.getPriority())
                .source(TicketSourceEnum.MANUAL.getSource())
                .businessType(businessType)
                .businessId(createReqVO.getBusinessId())
                .status(TicketStatusEnum.PENDING.getStatus())
                .creatorId(creatorUserId)
                .creatorName(creator.getNickname())
                .deptId(createReqVO.getDeptId())
                .dueTime(createReqVO.getDueTime())
                .projectId(createReqVO.getProjectId())
                .customerId(createReqVO.getCustomerId())
                .extJson(createReqVO.getExtJson())
                .remark(createReqVO.getRemark())
                .notifyChannels("inner,dingtalk")
                .notifyStatus(0)
                .build();
        if (assignee != null) {
            ticket.setAssigneeId(assignee.getId());
            ticket.setAssigneeName(assignee.getNickname());
            ticket.setAssigneeDeptId(assignee.getDeptId());
        }

        // 6. 生成工单号（带重试，靠 uk_ticket_no 唯一索引兜底）
        ticket.setTicketNo(generateTicketNo());

        // 7. 入库 + 写 log
        ticketMapper.insert(ticket);
        writeLog(ticket.getId(), TicketActionEnum.CREATE, null, ticket.getStatus(),
                null, ticket.getAssigneeId(), "创建工单");

        return ticket.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTicket(TicketSaveReqVO updateReqVO) {
        TicketDO existing = mustExist(updateReqVO.getId());
        Long currentUserId = SecurityFrameworkUtils.getLoginUserId();
        // 仅提单人本人 / 超管可改，且必须 status=0
        if (!isSuperAdmin(currentUserId)
                && !Objects.equals(existing.getCreatorId(), currentUserId)) {
            throw exception(TICKET_NOT_OWN);
        }
        if (!TicketStatusEnum.PENDING.getStatus().equals(existing.getStatus())) {
            throw exception(TICKET_STATUS_INVALID,
                    TicketStatusEnum.nameOf(existing.getStatus()), "修改基本信息");
        }
        // 仅允许改可编辑字段；assignee / status 由专用接口处理
        TicketDO update = new TicketDO();
        update.setId(existing.getId());
        update.setTitle(updateReqVO.getTitle());
        update.setContent(updateReqVO.getContent());
        update.setCategoryId(updateReqVO.getCategoryId());
        update.setPriority(updateReqVO.getPriority());
        update.setDeptId(updateReqVO.getDeptId());
        update.setDueTime(updateReqVO.getDueTime());
        update.setProjectId(updateReqVO.getProjectId());
        update.setCustomerId(updateReqVO.getCustomerId());
        update.setExtJson(updateReqVO.getExtJson());
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
        wrapper.and(w -> w.eq(TicketDO::getCreatorId, userId)
                .or().eq(TicketDO::getAssigneeId, userId));
        return ticketMapper.selectPage(pageReqVO, wrapper);
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

        // 8. 发事件供业务驱动器消费（同步发布；异常会回滚事务）
        TicketAcceptedEvent event = TicketAcceptedEvent.builder()
                .ticketId(ticket.getId())
                .ticketNo(ticket.getTicketNo())
                .title(ticket.getTitle())
                .businessType(ticket.getBusinessType())
                .businessId(ticket.getBusinessId())
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
                && !isSuperAdmin(currentUserId)) {
            throw exception(TICKET_NOT_ASSIGNEE);
        }
        Integer toStatus = TicketStateMachine.checkTransition(ticket.getStatus(), TicketActionEnum.FINISH);

        TicketDO update = new TicketDO();
        update.setId(ticket.getId());
        update.setStatus(toStatus);
        update.setFinishTime(LocalDateTime.now());
        ticketMapper.updateById(update);

        writeLog(ticket.getId(), TicketActionEnum.FINISH, ticket.getStatus(), toStatus,
                ticket.getAssigneeId(), ticket.getAssigneeId(), reqVO.getResult());
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
        boolean isAdmin = isSuperAdmin(currentUserId);
        boolean isDeptLeader = isDeptLeader(ticket.getDeptId(), currentUserId);
        Integer status = ticket.getStatus();
        List<String> actions = new ArrayList<>();
        // comment 任意可访问者都能评论
        if (isCreator || isAssignee || isAdmin || isDeptLeader) {
            actions.add(TicketActionEnum.COMMENT.getAction());
        }
        // 基础 update：status=0 且是提单人或管理员
        if (TicketStatusEnum.PENDING.getStatus().equals(status) && (isCreator || isAdmin)) {
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
        // 接单：status=0 且是 assignee
        if (canDo(status, TicketActionEnum.START) && isAssignee) {
            actions.add(TicketActionEnum.START.getAction());
        }
        // 完成：status=1 且是 assignee 或管理员
        if (canDo(status, TicketActionEnum.FINISH) && (isAssignee || isAdmin)) {
            actions.add(TicketActionEnum.FINISH.getAction());
        }
        // 关闭：status=3 且是提单人或管理员
        if (canDo(status, TicketActionEnum.CLOSE) && (isCreator || isAdmin)) {
            actions.add(TicketActionEnum.CLOSE.getAction());
        }
        // 取消：status=0 且是提单人或管理员
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
                || isSuperAdmin(currentUserId)) {
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

    private boolean isSuperAdmin(Long userId) {
        return userId != null && permissionApi.hasAnyRoles(userId, ROLE_SUPER_ADMIN);
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
     */
    private boolean isDeptLeader(Long deptId, Long currentUserId) {
        if (deptId == null || currentUserId == null) {
            return false;
        }
        try {
            DeptRespDTO dept = deptApi.getDept(deptId);
            return dept != null && Objects.equals(dept.getLeaderUserId(), currentUserId);
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
}
