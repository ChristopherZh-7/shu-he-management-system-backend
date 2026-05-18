package cn.shuhe.system.module.ticket.service;

import cn.shuhe.system.framework.security.core.util.SecurityFrameworkUtils;
import cn.shuhe.system.module.system.api.permission.PermissionApi;
import cn.shuhe.system.module.system.api.user.AdminUserApi;
import cn.shuhe.system.module.system.api.user.dto.AdminUserRespDTO;
import cn.shuhe.system.module.ticket.controller.admin.vo.TicketCommentSaveReqVO;
import cn.shuhe.system.module.ticket.dal.dataobject.TicketCommentDO;
import cn.shuhe.system.module.ticket.dal.dataobject.TicketDO;
import cn.shuhe.system.module.ticket.dal.mysql.TicketCommentMapper;
import cn.shuhe.system.module.ticket.dal.mysql.TicketMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static cn.shuhe.system.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_COMMENT_INTERNAL_DENY;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_NOT_EXISTS;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.TICKET_NO_PERMISSION;

/**
 * 工单评论 Service 实现。
 *
 * <p>不依赖 {@link TicketService}（避免循环依赖），自己用 {@link TicketMapper} 校验工单存在和 IDOR。
 */
@Service
@Validated
@Slf4j
public class TicketCommentServiceImpl implements TicketCommentService {

    @Resource
    private TicketCommentMapper commentMapper;

    @Resource
    private TicketMapper ticketMapper;

    @Resource
    private AdminUserApi adminUserApi;

    @Resource
    private PermissionApi permissionApi;

    private static final String ROLE_SUPER_ADMIN = "super_admin";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createComment(TicketCommentSaveReqVO reqVO) {
        Long currentUserId = SecurityFrameworkUtils.getLoginUserId();
        TicketDO ticket = validateTicketAccess(reqVO.getTicketId(), currentUserId);

        boolean wantInternal = Boolean.TRUE.equals(reqVO.getIsInternal());
        if (wantInternal && !canCreateInternal(ticket, currentUserId)) {
            throw exception(TICKET_COMMENT_INTERNAL_DENY);
        }

        AdminUserRespDTO user = adminUserApi.getUser(currentUserId);
        TicketCommentDO comment = TicketCommentDO.builder()
                .ticketId(reqVO.getTicketId())
                .userId(currentUserId)
                .userName(user == null ? null : user.getNickname())
                .userDeptId(user == null ? null : user.getDeptId())
                .parentId(reqVO.getParentId())
                .content(reqVO.getContent())
                .isInternal(wantInternal)
                .build();
        commentMapper.insert(comment);
        return comment.getId();
    }

    @Override
    public List<TicketCommentDO> listByTicket(Long ticketId, Long currentUserId) {
        TicketDO ticket = validateTicketAccess(ticketId, currentUserId);
        List<TicketCommentDO> all = commentMapper.selectListByTicketId(ticketId);
        if (all == null || all.isEmpty()) {
            return Collections.emptyList();
        }
        if (isCreatorOnly(ticket, currentUserId)) {
            return all.stream()
                    .filter(c -> !Boolean.TRUE.equals(c.getIsInternal()))
                    .collect(Collectors.toList());
        }
        return all;
    }

    @Override
    public Long countByTicket(Long ticketId) {
        return commentMapper.selectCountByTicketId(ticketId);
    }

    // ========== Helpers ==========

    private TicketDO validateTicketAccess(Long ticketId, Long currentUserId) {
        if (ticketId == null) {
            throw exception(TICKET_NOT_EXISTS);
        }
        TicketDO ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            throw exception(TICKET_NOT_EXISTS);
        }
        // 简单 IDOR：是提单人 / 处理人 / 超管时通过；其它角色（部门负责人 / 工单管理员）
        // 由 Controller 层 @PreAuthorize + @DataPermission 兜底（能查到列表说明能看详情）。
        if (Objects.equals(ticket.getCreatorId(), currentUserId)
                || Objects.equals(ticket.getAssigneeId(), currentUserId)
                || isSuperAdmin(currentUserId)) {
            return ticket;
        }
        throw exception(TICKET_NO_PERMISSION);
    }

    private boolean canCreateInternal(TicketDO ticket, Long currentUserId) {
        if (isSuperAdmin(currentUserId)) {
            return true;
        }
        return Objects.equals(ticket.getAssigneeId(), currentUserId);
    }

    /**
     * 是否「只是提单人」—— 提单人但不同时是处理人，也不是管理员；这种身份下应过滤内部评论。
     */
    private boolean isCreatorOnly(TicketDO ticket, Long currentUserId) {
        if (isSuperAdmin(currentUserId)) {
            return false;
        }
        if (Objects.equals(ticket.getAssigneeId(), currentUserId)) {
            return false;
        }
        return Objects.equals(ticket.getCreatorId(), currentUserId);
    }

    private boolean isSuperAdmin(Long userId) {
        return userId != null && permissionApi.hasAnyRoles(userId, ROLE_SUPER_ADMIN);
    }
}
