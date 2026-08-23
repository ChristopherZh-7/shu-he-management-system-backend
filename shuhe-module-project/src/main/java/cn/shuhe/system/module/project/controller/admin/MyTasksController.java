package cn.shuhe.system.module.project.controller.admin;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import cn.shuhe.system.framework.common.pojo.CommonResult;
import cn.shuhe.system.framework.datapermission.core.annotation.DataPermission;
import cn.shuhe.system.framework.security.core.util.SecurityFrameworkUtils;
import cn.shuhe.system.module.project.controller.admin.vo.MyTasksRespVO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectDO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectRoundDO;
import cn.shuhe.system.module.project.dal.dataobject.ServiceItemDO;
import cn.shuhe.system.module.project.dal.mysql.ProjectMapper;
import cn.shuhe.system.module.project.dal.mysql.ProjectRoundMapper;
import cn.shuhe.system.module.project.dal.mysql.ServiceItemMapper;
import cn.shuhe.system.module.system.api.dept.DeptApi;
import cn.shuhe.system.module.system.api.dept.dto.DeptRespDTO;
import cn.shuhe.system.module.system.api.user.AdminUserApi;
import cn.shuhe.system.module.system.api.user.dto.AdminUserRespDTO;
import cn.shuhe.system.module.ticket.dal.dataobject.TicketDO;
import cn.shuhe.system.module.ticket.dal.mysql.TicketExecutorMapper;
import cn.shuhe.system.module.ticket.dal.mysql.TicketMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

import static cn.shuhe.system.framework.common.pojo.CommonResult.success;

/**
 * My Tasks Controller - aggregates projects, service items, and rounds
 * for the current user's workbench view.
 *
 * Designed for regular employees who only have workbench access.
 */
@Tag(name = "管理后台 - 我的任务")
@RestController
@RequestMapping("/project/my-tasks")
@Validated
@Slf4j
public class MyTasksController {

    @Resource
    private ProjectMapper projectMapper;

    @Resource
    private cn.shuhe.system.module.project.service.ServiceItemService serviceItemService;

    @Resource
    private ServiceItemMapper serviceItemMapper;

    @Resource
    private ProjectRoundMapper projectRoundMapper;

    @Resource
    private TicketMapper ticketMapper;

    @Resource
    private TicketExecutorMapper ticketExecutorMapper;

    @Resource
    private AdminUserApi adminUserApi;

    @Resource
    private DeptApi deptApi;

    @GetMapping("/list")
    @Operation(summary = "获取我的任务列表", description = "聚合当前用户相关的项目、服务项、轮次")
    @PreAuthorize("@ss.hasAnyPermissions('project:my-tasks:query', 'project:my-work-record:query')")
    @DataPermission(enable = false)
    public CommonResult<MyTasksRespVO> getMyTasks() {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        AdminUserRespDTO user = adminUserApi.getUser(userId);

        MyTasksRespVO resp = new MyTasksRespVO();
        resp.setProjects(new ArrayList<>());

        if (user == null) {
            return success(resp);
        }

        Integer userDeptType = getUserDeptType(user.getDeptId());

        // Step 1: Get project IDs that have service items of this dept type
        Set<Long> candidateProjectIds = new LinkedHashSet<>();
        if (userDeptType != null) {
            candidateProjectIds.addAll(serviceItemMapper.selectProjectIdsByDeptType(userDeptType));
        }

        List<ServiceItemDO> myServiceItems = serviceItemMapper.selectListByExecutorId(userId);
        myServiceItems.stream().map(ServiceItemDO::getProjectId).filter(Objects::nonNull)
                .forEach(candidateProjectIds::add);

        // 明确分派给我的轮次不受当前部门类型限制（如总经办协作执行）
        List<ProjectRoundDO> myRounds = projectRoundMapper.selectListByExecutorUserId(userId);
        myRounds.stream().map(ProjectRoundDO::getProjectId).filter(Objects::nonNull)
                .forEach(candidateProjectIds::add);

        // 工单可能来自跨部门协作，将明确分派给我的工单项目补进候选范围
        Set<Long> myTicketIds = new HashSet<>(ticketExecutorMapper.selectTicketIdsByUserId(userId));
        List<TicketDO> ticketCandidates = new ArrayList<>(ticketMapper.selectListByAssigneeId(userId));
        myTicketIds.addAll(ticketCandidates.stream().map(TicketDO::getId).collect(Collectors.toSet()));
        List<TicketDO> myTickets;
        if (!myTicketIds.isEmpty()) {
            Map<Long, TicketDO> ticketById = ticketCandidates.stream()
                    .collect(Collectors.toMap(TicketDO::getId, ticket -> ticket, (left, right) -> left));
            for (TicketDO ticket : ticketMapper.selectBatchIds(myTicketIds)) {
                ticketById.put(ticket.getId(), ticket);
            }
            myTickets = ticketById.values().stream()
                    .filter(ticket -> ticket.getStatus() == null || ticket.getStatus() != 5)
                    .toList();
            myTickets.stream().map(TicketDO::getProjectId).filter(Objects::nonNull)
                    .forEach(candidateProjectIds::add);
        } else {
            myTickets = Collections.emptyList();
        }

        if (candidateProjectIds.isEmpty()) {
            return success(resp);
        }
        List<Long> projectIds = new ArrayList<>(candidateProjectIds);

        // Step 2: Load projects
        List<ProjectDO> projects = projectMapper.selectBatchIds(projectIds);
        if (CollUtil.isEmpty(projects)) {
            return success(resp);
        }

        // Step 3: Load service items for these projects (filtered by dept type, only active)
        Map<Long, List<ServiceItemDO>> serviceItemsByProject = new HashMap<>();
        for (Long projectId : projectIds) {
            List<ServiceItemDO> items = userDeptType == null ? new ArrayList<>() : new ArrayList<>(
                    serviceItemMapper.selectListByProjectIdAndDeptType(projectId, userDeptType));
            Set<Long> existingItemIds = items.stream().map(ServiceItemDO::getId).collect(Collectors.toSet());
            myServiceItems.stream()
                    .filter(item -> Objects.equals(item.getProjectId(), projectId))
                    .filter(item -> existingItemIds.add(item.getId()))
                    .forEach(items::add);
            myRounds.stream()
                    .filter(round -> Objects.equals(round.getProjectId(), projectId))
                    .map(ProjectRoundDO::getServiceItemId)
                    .filter(Objects::nonNull)
                    .filter(serviceItemId -> existingItemIds.add(serviceItemId))
                    .map(serviceItemMapper::selectById)
                    .filter(Objects::nonNull)
                    .forEach(items::add);
            myTickets.stream()
                    .filter(ticket -> Objects.equals(ticket.getProjectId(), projectId))
                    .map(TicketDO::getServiceItemId)
                    .filter(Objects::nonNull)
                    .filter(existingItemIds::add)
                    .map(serviceItemMapper::selectById)
                    .filter(Objects::nonNull)
                    .forEach(items::add);
            if (CollUtil.isNotEmpty(items)) {
                // Only include non-cancelled items
                items = items.stream()
                        .filter(i -> i.getStatus() != null && i.getStatus() != 4)
                        .collect(Collectors.toList());
                if (!items.isEmpty()) {
                    serviceItemsByProject.put(projectId, items);
                }
            }
        }

        // Step 4: Load rounds for all relevant service items
        Set<Long> allServiceItemIds = serviceItemsByProject.values().stream()
                .flatMap(Collection::stream)
                .map(ServiceItemDO::getId)
                .collect(Collectors.toSet());

        Map<Long, List<ProjectRoundDO>> roundsByServiceItem = new HashMap<>();
        for (Long siId : allServiceItemIds) {
            List<ProjectRoundDO> rounds = projectRoundMapper.selectListByServiceItemId(siId);
            if (CollUtil.isNotEmpty(rounds)) {
                roundsByServiceItem.put(siId, rounds);
            }
        }

        Map<Long, List<TicketDO>> ticketsByServiceItem = myTickets.stream()
                .filter(ticket -> ticket.getServiceItemId() != null)
                .collect(Collectors.groupingBy(TicketDO::getServiceItemId));

        // Step 5: Assemble response，只保留明确分派给当前用户的任务
        String userIdStr = String.valueOf(userId);
        List<MyTasksRespVO.TaskProject> taskProjects = new ArrayList<>();

        for (ProjectDO project : projects) {
            List<ServiceItemDO> items = serviceItemsByProject.get(project.getId());
            if (CollUtil.isEmpty(items)) {
                continue;
            }

            MyTasksRespVO.TaskProject tp = new MyTasksRespVO.TaskProject();
            tp.setProjectId(project.getId());
            tp.setProjectName(project.getName());
            tp.setCustomerName(project.getCustomerName());

            List<MyTasksRespVO.TaskServiceItem> taskItems = new ArrayList<>();
            for (ServiceItemDO item : items) {
                MyTasksRespVO.TaskServiceItem tsi = new MyTasksRespVO.TaskServiceItem();
                tsi.setServiceItemId(item.getId());
                tsi.setServiceType(item.getServiceType());
                tsi.setDeptType(item.getDeptType());
                tsi.setServiceTypeName(
                        serviceItemService.resolveServiceTypeLabel(item.getDeptType(), item.getServiceType()));
                tsi.setServiceMode(item.getServiceMode());
                tsi.setStatus(item.getStatus());
                tsi.setProgress(item.getProgress());
                tsi.setIsMyServiceItem(Objects.equals(item.getExecutorId(), userId));

                List<ProjectRoundDO> rounds = roundsByServiceItem.get(item.getId());
                List<MyTasksRespVO.TaskRound> taskRounds = new ArrayList<>();

                if (CollUtil.isNotEmpty(rounds)) {
                    for (ProjectRoundDO round : rounds) {
                        if (Integer.valueOf(3).equals(round.getStatus())) {
                            continue;
                        }
                        MyTasksRespVO.TaskRound tr = new MyTasksRespVO.TaskRound();
                        tr.setRoundId(round.getId());
                        tr.setName(round.getName());
                        tr.setRoundNo(round.getRoundNo());
                        tr.setStatus(round.getStatus());
                        tr.setProgress(round.getProgress());
                        tr.setDeadline(round.getDeadline());
                        tr.setExecutorNames(round.getExecutorNames());

                        boolean isMyRound = isUserInExecutors(round.getExecutorIds(), userIdStr);
                        tr.setIsMyRound(isMyRound);
                        if (isMyRound) {
                            taskRounds.add(tr);
                        }
                    }
                }

                tsi.setRounds(taskRounds);
                List<MyTasksRespVO.TaskTicket> taskTickets = ticketsByServiceItem
                        .getOrDefault(item.getId(), Collections.emptyList()).stream()
                        .map(ticket -> {
                            MyTasksRespVO.TaskTicket taskTicket = new MyTasksRespVO.TaskTicket();
                            taskTicket.setTicketId(ticket.getId());
                            taskTicket.setTicketNo(ticket.getTicketNo());
                            taskTicket.setTitle(ticket.getTitle());
                            taskTicket.setStatus(ticket.getStatus());
                            taskTicket.setDueTime(ticket.getDueTime());
                            return taskTicket;
                        }).toList();
                tsi.setTickets(taskTickets);
                if (Boolean.TRUE.equals(tsi.getIsMyServiceItem()) || !taskRounds.isEmpty() || !taskTickets.isEmpty()) {
                    taskItems.add(tsi);
                }
            }

            if (taskItems.isEmpty()) {
                continue;
            }
            tp.setServiceItems(taskItems);
            tp.setDeptType(taskItems.get(0).getDeptType());
            taskProjects.add(tp);
        }

        // Sort: projects with "my rounds" come first
        taskProjects.sort((a, b) -> {
            boolean aHasMy = a.getServiceItems().stream()
                    .flatMap(si -> si.getRounds().stream())
                    .anyMatch(r -> Boolean.TRUE.equals(r.getIsMyRound()));
            boolean bHasMy = b.getServiceItems().stream()
                    .flatMap(si -> si.getRounds().stream())
                    .anyMatch(r -> Boolean.TRUE.equals(r.getIsMyRound()));
            if (aHasMy && !bHasMy) return -1;
            if (!aHasMy && bHasMy) return 1;
            return 0;
        });

        resp.setProjects(taskProjects);
        return success(resp);
    }

    private boolean isUserInExecutors(String executorIdsJson, String userIdStr) {
        if (executorIdsJson == null || executorIdsJson.isEmpty()) {
            return false;
        }
        try {
            List<Object> ids = JSONUtil.toList(executorIdsJson, Object.class);
            return ids.stream().anyMatch(id -> String.valueOf(id).equals(userIdStr));
        } catch (Exception e) {
            return executorIdsJson.contains(userIdStr);
        }
    }

    private Integer getUserDeptType(Long deptId) {
        if (deptId == null) {
            return null;
        }
        Long currentDeptId = deptId;
        while (currentDeptId != null && currentDeptId != 0) {
            DeptRespDTO dept = deptApi.getDept(currentDeptId);
            if (dept == null) {
                break;
            }
            if (dept.getDeptType() != null) {
                return dept.getDeptType();
            }
            if (dept.getParentId() == null || dept.getParentId() == 0) {
                break;
            }
            currentDeptId = dept.getParentId();
        }
        return null;
    }
}
