package cn.shuhe.system.module.ticket.controller.admin;

import cn.shuhe.system.framework.common.pojo.CommonResult;
import cn.shuhe.system.framework.common.util.object.BeanUtils;
import cn.shuhe.system.framework.security.core.util.SecurityFrameworkUtils;
import cn.shuhe.system.module.ticket.controller.admin.vo.TicketCommentRespVO;
import cn.shuhe.system.module.ticket.controller.admin.vo.TicketCommentSaveReqVO;
import cn.shuhe.system.module.ticket.dal.dataobject.TicketCommentDO;
import cn.shuhe.system.module.ticket.service.TicketCommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.shuhe.system.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 工单评论")
@RestController
@RequestMapping("/ticket/comment")
@Validated
public class TicketCommentController {

    @Resource
    private TicketCommentService commentService;

    @PostMapping("/create")
    @Operation(summary = "添加评论")
    @PreAuthorize("@ss.hasPermission('ticket:comment:create')")
    public CommonResult<Long> create(@Valid @RequestBody TicketCommentSaveReqVO reqVO) {
        return success(commentService.createComment(reqVO));
    }

    @GetMapping("/list")
    @Operation(summary = "列出工单评论（按当前用户身份过滤内部评论）")
    @Parameter(name = "ticketId", description = "工单 ID", required = true)
    @PreAuthorize("@ss.hasPermission('ticket:ticket:query')")
    public CommonResult<List<TicketCommentRespVO>> list(@RequestParam("ticketId") Long ticketId) {
        Long currentUserId = SecurityFrameworkUtils.getLoginUserId();
        List<TicketCommentDO> comments = commentService.listByTicket(ticketId, currentUserId);
        return success(BeanUtils.toBean(comments, TicketCommentRespVO.class));
    }
}
