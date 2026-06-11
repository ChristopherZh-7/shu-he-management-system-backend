package cn.shuhe.system.module.ticket.controller.admin;

import cn.shuhe.system.framework.common.pojo.CommonResult;
import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.common.util.object.BeanUtils;
import cn.shuhe.system.framework.datapermission.core.annotation.DataPermission;
import cn.shuhe.system.framework.security.core.util.SecurityFrameworkUtils;
import cn.shuhe.system.module.system.api.dept.DeptApi;
import cn.shuhe.system.module.system.api.dept.dto.DeptRespDTO;
import cn.shuhe.system.module.ticket.controller.admin.vo.TicketAcceptReqVO;
import cn.shuhe.system.module.ticket.controller.admin.vo.TicketAssignReqVO;
import cn.shuhe.system.module.ticket.controller.admin.vo.TicketFinishReqVO;
import cn.shuhe.system.module.ticket.controller.admin.vo.TicketPageReqVO;
import cn.shuhe.system.module.ticket.controller.admin.vo.TicketReopenReqVO;
import cn.shuhe.system.module.ticket.controller.admin.vo.TicketReturnReqVO;
import cn.shuhe.system.module.ticket.controller.admin.vo.TicketReviewPassReqVO;
import cn.shuhe.system.module.ticket.controller.admin.vo.TicketReviewRejectReqVO;
import cn.shuhe.system.module.ticket.controller.admin.vo.TicketRespVO;
import cn.shuhe.system.module.ticket.controller.admin.vo.TicketSaveReqVO;
import cn.shuhe.system.module.ticket.controller.admin.vo.TicketTransferReqVO;
import cn.shuhe.system.module.ticket.dal.dataobject.TicketCategoryDO;
import cn.shuhe.system.module.ticket.dal.dataobject.TicketDO;
import cn.shuhe.system.module.ticket.dal.mysql.TicketAttachmentMapper;
import cn.shuhe.system.module.ticket.dal.mysql.TicketCommentMapper;
import cn.shuhe.system.module.ticket.enums.TicketStatusEnum;
import cn.shuhe.system.module.ticket.service.TicketCategoryService;
import cn.shuhe.system.module.ticket.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static cn.shuhe.system.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 工单")
@RestController
@RequestMapping("/ticket/ticket")
@Validated
public class TicketController {

    @Resource
    private TicketService ticketService;

    @Resource
    private TicketCategoryService categoryService;

    @Resource
    private TicketCommentMapper commentMapper;

    @Resource
    private TicketAttachmentMapper attachmentMapper;

    @Resource
    private DeptApi deptApi;

    @PostMapping("/create")
    @Operation(summary = "创建工单")
    @PreAuthorize("@ss.hasPermission('ticket:ticket:create')")
    public CommonResult<Long> createTicket(@Valid @RequestBody TicketSaveReqVO createReqVO) {
        return success(ticketService.createTicket(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改工单（仅 status=0 的工单）")
    @PreAuthorize("@ss.hasPermission('ticket:ticket:update')")
    public CommonResult<Boolean> updateTicket(@Valid @RequestBody TicketSaveReqVO updateReqVO) {
        ticketService.updateTicket(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除工单（仅管理员）")
    @Parameter(name = "id", description = "工单 ID", required = true)
    @PreAuthorize("@ss.hasPermission('ticket:ticket:delete')")
    public CommonResult<Boolean> deleteTicket(@RequestParam("id") Long id) {
        ticketService.deleteTicket(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取工单详情")
    @Parameter(name = "id", description = "工单 ID", required = true)
    @PreAuthorize("@ss.hasPermission('ticket:ticket:query')")
    public CommonResult<TicketRespVO> getTicket(@RequestParam("id") Long id) {
        Long currentUserId = SecurityFrameworkUtils.getLoginUserId();
        TicketDO ticket = ticketService.validateTicketAccess(id, currentUserId);
        TicketRespVO vo = convertToRespVO(ticket, true);
        vo.setActions(ticketService.calculateAvailableActions(ticket, currentUserId));
        Long commentCount = commentMapper.selectCountByTicketId(id);
        vo.setCommentCount(commentCount == null ? 0 : commentCount.intValue());
        Long attachmentCount = attachmentMapper.selectCountByTicketId(id);
        vo.setAttachmentCount(attachmentCount == null ? 0 : attachmentCount.intValue());
        return success(vo);
    }

    @GetMapping("/page")
    @Operation(summary = "工单分页列表（走数据权限）")
    @PreAuthorize("@ss.hasPermission('ticket:ticket:query')")
    public CommonResult<PageResult<TicketRespVO>> getTicketPage(@Valid TicketPageReqVO pageReqVO) {
        PageResult<TicketDO> pageResult = ticketService.getTicketPage(pageReqVO);
        return success(convertPage(pageResult));
    }

    @GetMapping("/my-page")
    @Operation(summary = "我的工单（提的 + 处理的；跳过部门数据权限）")
    @PreAuthorize("@ss.hasPermission('ticket:ticket:query')")
    @DataPermission(enable = false)
    public CommonResult<PageResult<TicketRespVO>> getMyTicketPage(@Valid TicketPageReqVO pageReqVO) {
        Long currentUserId = SecurityFrameworkUtils.getLoginUserId();
        PageResult<TicketDO> pageResult = ticketService.getMyTicketPage(pageReqVO, currentUserId);
        return success(convertPage(pageResult));
    }

    // ========== 状态机 ==========

    @PutMapping("/assign")
    @Operation(summary = "分派处理人")
    @PreAuthorize("@ss.hasPermission('ticket:ticket:assign')")
    public CommonResult<Boolean> assignTicket(@Valid @RequestBody TicketAssignReqVO reqVO) {
        ticketService.assignTicket(reqVO);
        return success(true);
    }

    @PutMapping("/accept")
    @Operation(summary = "主管接单（指派 1+ 执行人，状态 0→1，发 TicketAcceptedEvent）")
    @PreAuthorize("@ss.hasPermission('ticket:ticket:accept')")
    public CommonResult<Boolean> acceptTicket(@Valid @RequestBody TicketAcceptReqVO reqVO) {
        ticketService.acceptTicket(reqVO);
        return success(true);
    }

    @PutMapping("/start")
    @Operation(summary = "接单开始（处理人本人）")
    @Parameter(name = "id", description = "工单 ID", required = true)
    @PreAuthorize("@ss.hasPermission('ticket:ticket:update')")
    public CommonResult<Boolean> startTicket(@RequestParam("id") Long id) {
        ticketService.startTicket(id);
        return success(true);
    }

    @PutMapping("/finish")
    @Operation(summary = "完成工单（处理人提交验收，状态 1→2）")
    @PreAuthorize("@ss.hasPermission('ticket:ticket:finish')")
    public CommonResult<Boolean> finishTicket(@Valid @RequestBody TicketFinishReqVO reqVO) {
        ticketService.finishTicket(reqVO);
        return success(true);
    }

    @PutMapping("/review-pass")
    @Operation(summary = "验收通过（提单人，状态 2→3）")
    @PreAuthorize("@ss.hasPermission('ticket:ticket:close')")
    public CommonResult<Boolean> reviewPassTicket(@Valid @RequestBody TicketReviewPassReqVO reqVO) {
        ticketService.reviewPassTicket(reqVO);
        return success(true);
    }

    @PutMapping("/review-reject")
    @Operation(summary = "验收驳回（提单人，状态 2→1 退回执行人重做）")
    @PreAuthorize("@ss.hasPermission('ticket:ticket:close')")
    public CommonResult<Boolean> reviewRejectTicket(@Valid @RequestBody TicketReviewRejectReqVO reqVO) {
        ticketService.reviewRejectTicket(reqVO);
        return success(true);
    }

    @PutMapping("/reopen")
    @Operation(summary = "重开工单（提单人，状态 3/4→1；7 天窗口、最多 3 次）")
    @PreAuthorize("@ss.hasPermission('ticket:ticket:update')")
    public CommonResult<Boolean> reopenTicket(@Valid @RequestBody TicketReopenReqVO reqVO) {
        ticketService.reopenTicket(reqVO);
        return success(true);
    }

    @PutMapping("/return")
    @Operation(summary = "拒单退回（部门负责人，状态 0→6）")
    @PreAuthorize("@ss.hasPermission('ticket:ticket:accept')")
    public CommonResult<Boolean> returnTicket(@Valid @RequestBody TicketReturnReqVO reqVO) {
        ticketService.returnTicket(reqVO);
        return success(true);
    }

    @PutMapping("/resubmit")
    @Operation(summary = "重新提交（提单人，状态 6→0）")
    @Parameter(name = "id", description = "工单 ID", required = true)
    @PreAuthorize("@ss.hasPermission('ticket:ticket:update')")
    public CommonResult<Boolean> resubmitTicket(@RequestParam("id") Long id) {
        ticketService.resubmitTicket(id);
        return success(true);
    }

    @PutMapping("/close")
    @Operation(summary = "关闭工单（提单人确认 / 管理员）")
    @Parameter(name = "id", description = "工单 ID", required = true)
    @PreAuthorize("@ss.hasPermission('ticket:ticket:close')")
    public CommonResult<Boolean> closeTicket(@RequestParam("id") Long id) {
        ticketService.closeTicket(id);
        return success(true);
    }

    @PutMapping("/cancel")
    @Operation(summary = "取消工单（一期仅 status=0 可取消）")
    @Parameter(name = "id", description = "工单 ID", required = true)
    @PreAuthorize("@ss.hasPermission('ticket:ticket:update')")
    public CommonResult<Boolean> cancelTicket(@RequestParam("id") Long id) {
        ticketService.cancelTicket(id);
        return success(true);
    }

    @PutMapping("/transfer")
    @Operation(summary = "转交工单")
    @PreAuthorize("@ss.hasPermission('ticket:ticket:transfer')")
    public CommonResult<Boolean> transferTicket(@Valid @RequestBody TicketTransferReqVO reqVO) {
        ticketService.transferTicket(reqVO);
        return success(true);
    }

    // ========== 转换 ==========

    private PageResult<TicketRespVO> convertPage(PageResult<TicketDO> page) {
        PageResult<TicketRespVO> result = BeanUtils.toBean(page, TicketRespVO.class);
        if (result == null || result.getList() == null) {
            return result;
        }
        Long currentUserId = SecurityFrameworkUtils.getLoginUserId();
        for (int i = 0; i < page.getList().size(); i++) {
            TicketDO src = page.getList().get(i);
            TicketRespVO target = result.getList().get(i);
            enrichDisplayNames(target, src, false);
            // 列表行快捷操作（接单 / 验收）依赖 actions
            target.setActions(ticketService.calculateAvailableActions(src, currentUserId));
        }
        return result;
    }

    private TicketRespVO convertToRespVO(TicketDO ticket, boolean fillExt) {
        TicketRespVO vo = BeanUtils.toBean(ticket, TicketRespVO.class);
        enrichDisplayNames(vo, ticket, fillExt);
        return vo;
    }

    /**
     * 补展示字段：{@code statusName / categoryName / deptName / assigneeDeptName / outside}。
     */
    private void enrichDisplayNames(TicketRespVO vo, TicketDO ticket, boolean fillExt) {
        if (vo == null || ticket == null) {
            return;
        }
        vo.setStatusName(TicketStatusEnum.nameOf(ticket.getStatus()));
        // 外出标记在 extJson 置空前提取，列表（fillExt=false）也能拿到
        Map<String, Object> ext = ticket.getExtJson();
        if (ext != null) {
            Object isOutside = ext.get("isOutside");
            vo.setOutside(isOutside instanceof Boolean
                    ? (Boolean) isOutside
                    : isOutside != null && Boolean.parseBoolean(isOutside.toString()));
        }
        if (ticket.getCategoryId() != null) {
            TicketCategoryDO category = categoryService.getCategory(ticket.getCategoryId());
            if (category != null) {
                vo.setCategoryName(category.getName());
            }
        }
        if (ticket.getDeptId() != null) {
            DeptRespDTO dept = deptApi.getDept(ticket.getDeptId());
            if (dept != null) {
                vo.setDeptName(dept.getName());
            }
        }
        if (ticket.getAssigneeDeptId() != null) {
            DeptRespDTO dept = deptApi.getDept(ticket.getAssigneeDeptId());
            if (dept != null) {
                vo.setAssigneeDeptName(dept.getName());
            }
        }
        if (!fillExt) {
            vo.setExtJson(null);
        }
    }
}
