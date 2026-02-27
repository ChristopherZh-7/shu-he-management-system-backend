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
    private ServiceItemMapper serviceItemMapper;

    @Resource
    private ProjectRoundMapper projectRoundMapper;

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

        if (user == null || user.getDeptId() == null) {
            return success(resp);
        }

        Integer userDeptType = getUserDeptType(user.getDeptId());
        if (userDeptType == null) {
            return success(resp);
        }

        // Step 1: Get project IDs that have service items of this dept type
        List<Long> projectIds = serviceItemMapper.selectProjectIdsByDeptType(userDeptType);
        if (CollUtil.isEmpty(projectIds)) {
            return success(resp);
        }

        // Step 2: Load projects
        List<ProjectDO> projects = projectMapper.selectBatchIds(projectIds);
        if (CollUtil.isEmpty(projects)) {
            return success(resp);
        }

        // Step 3: Load service items for these projects (filtered by dept type, only active)
        Map<Long, List<ServiceItemDO>> serviceItemsByProject = new HashMap<>();
        for (Long projectId : projectIds) {
            List<ServiceItemDO> items = serviceItemMapper.selectListByProjectIdAndDeptType(projectId, userDeptType);
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

        // Step 5: Assemble response, prioritizing projects where user is an executor
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
            tp.setDeptType(userDeptType);

            List<MyTasksRespVO.TaskServiceItem> taskItems = new ArrayList<>();
            boolean hasMyRound = false;

            for (ServiceItemDO item : items) {
                MyTasksRespVO.TaskServiceItem tsi = new MyTasksRespVO.TaskServiceItem();
                tsi.setServiceItemId(item.getId());
                tsi.setName(item.getName());
                tsi.setServiceType(item.getServiceType());
                tsi.setServiceMode(item.getServiceMode());
                tsi.setStatus(item.getStatus());
                tsi.setProgress(item.getProgress());

                List<ProjectRoundDO> rounds = roundsByServiceItem.get(item.getId());
                List<MyTasksRespVO.TaskRound> taskRounds = new ArrayList<>();

                if (CollUtil.isNotEmpty(rounds)) {
                    for (ProjectRoundDO round : rounds) {
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
                            hasMyRound = true;
                        }

                        taskRounds.add(tr);
                    }
                }

                tsi.setRounds(taskRounds);
                taskItems.add(tsi);
            }

            tp.setServiceItems(taskItems);
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
        DeptRespDTO topDept = null;

        while (currentDeptId != null && currentDeptId != 0) {
            DeptRespDTO dept = deptApi.getDept(currentDeptId);
            if (dept == null) {
                break;
            }
            topDept = dept;
            if (dept.getParentId() == null || dept.getParentId() == 0) {
                break;
            }
            currentDeptId = dept.getParentId();
        }

        if (topDept == null || topDept.getName() == null) {
            return null;
        }

        String deptName = topDept.getName();
        if (deptName.contains("数据安全")) {
            return 3;
        } else if (deptName.contains("安全运营") || deptName.contains("运营服务")) {
            return 2;
        } else if (deptName.contains("安全技术") || deptName.contains("安全服务") || deptName.contains("安服")) {
            return 1;
        }
        return null;
    }
}
