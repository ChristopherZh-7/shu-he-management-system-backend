package cn.shuhe.system.module.ticket.controller.admin;

import cn.hutool.core.collection.CollUtil;
import cn.shuhe.system.framework.common.pojo.CommonResult;
import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.common.util.object.BeanUtils;
import cn.shuhe.system.module.system.dal.dataobject.user.AdminUserDO;
import cn.shuhe.system.module.system.dal.mysql.user.AdminUserMapper;
import cn.shuhe.system.module.ticket.controller.admin.vo.TicketPageReqVO;
import cn.shuhe.system.module.ticket.controller.admin.vo.TicketRespVO;
import cn.shuhe.system.module.ticket.controller.admin.vo.TicketSaveReqVO;
import cn.shuhe.system.module.ticket.dal.dataobject.TicketCategoryDO;
import cn.shuhe.system.module.ticket.dal.dataobject.TicketDO;
import cn.shuhe.system.module.ticket.dal.dataobject.TicketLogDO;
import cn.shuhe.system.module.ticket.dal.mysql.TicketCategoryMapper;
import cn.shuhe.system.module.ticket.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

import static cn.shuhe.system.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 工单")
@RestController
@RequestMapping("/ticket/ticket")
@Validated
public class TicketController {

    @Resource
    private TicketService ticketService;

    @Resource
    private AdminUserMapper adminUserMapper;

    @Resource
    private TicketCategoryMapper ticketCategoryMapper;

    @PostMapping("/create")
    @Operation(summary = "创建工单")
    @PreAuthorize("@ss.hasPermission('ticket:ticket:create')")
    public CommonResult<Long> createTicket(@Valid @RequestBody TicketSaveReqVO createReqVO) {
        return success(ticketService.createTicket(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新工单")
    @PreAuthorize("@ss.hasPermission('ticket:ticket:update')")
    public CommonResult<Boolean> updateTicket(@Valid @RequestBody TicketSaveReqVO updateReqVO) {
        ticketService.updateTicket(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除工单")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('ticket:ticket:delete')")
    public CommonResult<Boolean> deleteTicket(@RequestParam("id") Long id) {
        ticketService.deleteTicket(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得工单")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('ticket:ticket:query')")
    public CommonResult<TicketRespVO> getTicket(@RequestParam("id") Long id) {
        TicketDO ticket = ticketService.getTicket(id);
        TicketRespVO respVO = BeanUtils.toBean(ticket, TicketRespVO.class);
        fillTicketRelatedInfo(Collections.singletonList(respVO));
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得工单分页")
    @PreAuthorize("@ss.hasPermission('ticket:ticket:query')")
    public CommonResult<PageResult<TicketRespVO>> getTicketPage(@Valid TicketPageReqVO pageReqVO) {
        PageResult<TicketDO> pageResult = ticketService.getTicketPage(pageReqVO);
        PageResult<TicketRespVO> result = BeanUtils.toBean(pageResult, TicketRespVO.class);
        // 填充关联信息
        fillTicketRelatedInfo(result.getList());
        return success(result);
    }

    /**
     * 填充工单的关联信息（分类名称、创建人、处理人等）
     */
    private void fillTicketRelatedInfo(List<TicketRespVO> list) {
        if (CollUtil.isEmpty(list)) {
            return;
        }
        
        // 收集所有需要查询的ID
        Set<Long> userIds = new HashSet<>();
        Set<Long> categoryIds = new HashSet<>();
        for (TicketRespVO vo : list) {
            if (vo.getCreatorId() != null) {
                userIds.add(vo.getCreatorId());
            }
            if (vo.getAssigneeId() != null) {
                userIds.add(vo.getAssigneeId());
            }
            if (vo.getCategoryId() != null) {
                categoryIds.add(vo.getCategoryId());
            }
        }
        
        // 批量查询用户
        Map<Long, AdminUserDO> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<AdminUserDO> users = adminUserMapper.selectBatchIds(userIds);
            userMap = users.stream().collect(Collectors.toMap(AdminUserDO::getId, u -> u));
        }
        
        // 批量查询分类
        Map<Long, TicketCategoryDO> categoryMap = new HashMap<>();
        if (!categoryIds.isEmpty()) {
            List<TicketCategoryDO> categories = ticketCategoryMapper.selectBatchIds(categoryIds);
            categoryMap = categories.stream().collect(Collectors.toMap(TicketCategoryDO::getId, c -> c));
        }
        
        // 填充信息
        for (TicketRespVO vo : list) {
            // 填充创建人
            if (vo.getCreatorId() != null) {
                AdminUserDO creator = userMap.get(vo.getCreatorId());
                if (creator != null) {
                    vo.setCreatorName(creator.getNickname());
                }
            }
            // 填充处理人
            if (vo.getAssigneeId() != null) {
                AdminUserDO assignee = userMap.get(vo.getAssigneeId());
                if (assignee != null) {
                    vo.setAssigneeName(assignee.getNickname());
                }
            }
            // 填充分类
            if (vo.getCategoryId() != null) {
                TicketCategoryDO category = categoryMap.get(vo.getCategoryId());
                if (category != null) {
                    vo.setCategoryName(category.getName());
                }
            }
        }
    }

    @PutMapping("/assign")
    @Operation(summary = "分配工单")
    @PreAuthorize("@ss.hasPermission('ticket:ticket:assign')")
    public CommonResult<Boolean> assignTicket(@RequestParam("id") Long id,
                                              @RequestParam("assigneeId") Long assigneeId) {
        ticketService.assignTicket(id, assigneeId);
        return success(true);
    }

    @PutMapping("/start-process")
    @Operation(summary = "开始处理工单")
    @PreAuthorize("@ss.hasPermission('ticket:ticket:process')")
    public CommonResult<Boolean> startProcess(@RequestParam("id") Long id) {
        ticketService.startProcess(id);
        return success(true);
    }

    @PutMapping("/finish")
    @Operation(summary = "完成工单")
    @PreAuthorize("@ss.hasPermission('ticket:ticket:process')")
    public CommonResult<Boolean> finishTicket(@RequestParam("id") Long id,
                                              @RequestParam(value = "remark", required = false) String remark) {
        ticketService.finishTicket(id, remark);
        return success(true);
    }

    @PutMapping("/close")
    @Operation(summary = "关闭工单")
    @PreAuthorize("@ss.hasPermission('ticket:ticket:close')")
    public CommonResult<Boolean> closeTicket(@RequestParam("id") Long id,
                                             @RequestParam(value = "remark", required = false) String remark) {
        ticketService.closeTicket(id, remark);
        return success(true);
    }

    @PutMapping("/cancel")
    @Operation(summary = "取消工单")
    @PreAuthorize("@ss.hasPermission('ticket:ticket:cancel')")
    public CommonResult<Boolean> cancelTicket(@RequestParam("id") Long id,
                                              @RequestParam(value = "remark", required = false) String remark) {
        ticketService.cancelTicket(id, remark);
        return success(true);
    }

    @GetMapping("/logs")
    @Operation(summary = "获取工单操作日志")
    @Parameter(name = "ticketId", description = "工单ID", required = true)
    @PreAuthorize("@ss.hasPermission('ticket:ticket:query')")
    public CommonResult<List<TicketLogDO>> getTicketLogs(@RequestParam("ticketId") Long ticketId) {
        return success(ticketService.getTicketLogs(ticketId));
    }

}
