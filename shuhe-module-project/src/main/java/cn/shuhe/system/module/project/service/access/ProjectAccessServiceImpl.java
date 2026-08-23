package cn.shuhe.system.module.project.service.access;

import cn.hutool.core.collection.CollUtil;
import cn.shuhe.system.framework.datapermission.core.util.DataPermissionUtils;
import cn.shuhe.system.module.project.dal.dataobject.ProjectDeptVisibilityDO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectMemberDO;
import cn.shuhe.system.module.project.dal.mysql.ProjectDeptVisibilityMapper;
import cn.shuhe.system.module.project.dal.mysql.ProjectMemberMapper;
import cn.shuhe.system.module.project.enums.ProjectMemberRoleEnum;
import cn.shuhe.system.module.system.api.dept.DeptApi;
import cn.shuhe.system.module.system.api.dept.dto.DeptRespDTO;
import cn.shuhe.system.module.system.api.permission.PermissionApi;
import cn.shuhe.system.module.system.api.user.AdminUserApi;
import cn.shuhe.system.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static cn.shuhe.system.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.shuhe.system.module.project.enums.ErrorCodeConstants.PROJECT_MANAGE_FORBIDDEN;
import static cn.shuhe.system.module.project.enums.ErrorCodeConstants.PROJECT_VIEW_FORBIDDEN;

@Service
public class ProjectAccessServiceImpl implements ProjectAccessService {

    @Resource
    private ProjectMemberMapper projectMemberMapper;

    @Resource
    private ProjectDeptVisibilityMapper projectDeptVisibilityMapper;

    @Resource
    private PermissionApi permissionApi;

    @Resource
    private AdminUserApi adminUserApi;

    @Resource
    private DeptApi deptApi;

    @Override
    public boolean canViewProject(Long projectId, Long userId) {
        if (projectId == null || userId == null) {
            return false;
        }
        if (isSuperAdmin(userId) || getEffectiveRole(projectId, userId) != null) {
            return true;
        }
        Long userDeptId = getUserDeptId(userId);
        if (userDeptId == null) {
            return false;
        }
        Set<Long> userDeptPath = getDeptPathToRoot(userDeptId);
        return getProjectVisibleDeptIds(projectId).stream().anyMatch(userDeptPath::contains);
    }

    @Override
    public boolean canManageProject(Long projectId, Long userId) {
        if (projectId == null || userId == null) {
            return false;
        }
        if (isSuperAdmin(userId)) {
            return true;
        }
        ProjectMemberDO member = getProjectMember(projectId, userId);
        return member != null && ProjectMemberRoleEnum.MANAGER.getValue().equals(member.getRoleType());
    }

    @Override
    public void validateViewProject(Long projectId, Long userId) {
        if (!canViewProject(projectId, userId)) {
            throw exception(PROJECT_VIEW_FORBIDDEN);
        }
    }

    @Override
    public void validateManageProject(Long projectId, Long userId) {
        if (!canManageProject(projectId, userId)) {
            throw exception(PROJECT_MANAGE_FORBIDDEN);
        }
    }

    @Override
    public Integer getEffectiveRole(Long projectId, Long userId) {
        if (projectId == null || userId == null) {
            return null;
        }
        if (isSuperAdmin(userId)) {
            return ProjectMemberRoleEnum.MANAGER.getValue();
        }
        ProjectMemberDO member = getProjectMember(projectId, userId);
        if (member != null && (ProjectMemberRoleEnum.MANAGER.getValue().equals(member.getRoleType())
                || ProjectMemberRoleEnum.EXECUTOR.getValue().equals(member.getRoleType()))) {
            return member.getRoleType();
        }

        List<Long> visibleDeptIds = getProjectVisibleDeptIds(projectId);
        if (visibleDeptIds.isEmpty()) {
            return null;
        }
        // 参与部门的直接负责人：只能管理该部门范围。
        for (Long deptId : visibleDeptIds) {
            DeptRespDTO dept = deptApi.getDept(deptId);
            if (dept != null && userId.equals(dept.getLeaderUserId())) {
                return ProjectMemberRoleEnum.DEPT_LEADER.getValue();
            }
        }
        // 更上级负责人：可见但只读。
        for (Long deptId : visibleDeptIds) {
            Set<Long> leaders = deptApi.getAncestorChainLeaderUserIds(deptId);
            if (leaders.contains(userId)) {
                return ProjectMemberRoleEnum.UPPER_LEADER.getValue();
            }
        }
        // 兼容历史 roleType=3 数据：若已不是组织架构中的真实负责人，
        // 降级为普通项目执行成员，不再授予部门管理权限。
        return member != null ? ProjectMemberRoleEnum.EXECUTOR.getValue() : null;
    }

    @Override
    public List<Long> getReadableDeptIds(Long projectId, Long userId) {
        if (projectId == null || userId == null) {
            return List.of();
        }
        if (isSuperAdmin(userId) || isExplicitProjectManager(projectId, userId)) {
            return null;
        }
        Integer role = getEffectiveRole(projectId, userId);
        Set<Long> readableScope = new LinkedHashSet<>(resolveEmployeeReadableDeptScope(projectId, userId));
        if (ProjectMemberRoleEnum.UPPER_LEADER.getValue().equals(role)) {
            // 上级负责人可以只读其管理链下的部门服务包，不授予任何写权限。
            readableScope.addAll(resolveLeaderReadableDeptScope(projectId, userId));
        }
        if (ProjectMemberRoleEnum.DEPT_LEADER.getValue().equals(role)) {
            // 负责人身份不应覆盖其作为部门成员的基础只读范围。
            readableScope.addAll(resolveManagedDeptScope(projectId, userId));
        }
        if (!readableScope.isEmpty()) {
            return new ArrayList<>(readableScope);
        }
        Long userDeptId = getUserDeptId(userId);
        if (ProjectMemberRoleEnum.EXECUTOR.getValue().equals(role) && userDeptId != null) {
            return List.of(userDeptId);
        }
        return List.of();
    }

    @Override
    public List<Long> getManageableDeptIds(Long projectId, Long userId) {
        if (projectId == null || userId == null) {
            return List.of();
        }
        if (isSuperAdmin(userId) || isExplicitProjectManager(projectId, userId)) {
            return null;
        }
        if (ProjectMemberRoleEnum.DEPT_LEADER.getValue().equals(getEffectiveRole(projectId, userId))) {
            return resolveManagedDeptScope(projectId, userId);
        }
        return List.of();
    }

    @Override
    public boolean canReadDept(Long projectId, Long userId, Long deptId) {
        return containsDept(getReadableDeptIds(projectId, userId), deptId);
    }

    @Override
    public boolean canManageDept(Long projectId, Long userId, Long deptId) {
        return containsDept(getManageableDeptIds(projectId, userId), deptId);
    }

    @Override
    public List<Long> getVisibleProjectIds(Long userId) {
        if (userId == null) {
            return List.of();
        }
        if (isSuperAdmin(userId)) {
            return null;
        }
        // 以下两张关系表本身就是项目授权依据，读取时必须绕开通用部门数据权限，
        // 否则子部门员工会在授权判定前丢失父部门项目关系。
        Set<Long> result = new LinkedHashSet<>(DataPermissionUtils.executeIgnore(
                () -> projectMemberMapper.selectProjectIdsByUserId(userId)));

        Long userDeptId = getUserDeptId(userId);
        if (userDeptId != null) {
            // 项目选中业务大部门时，其下排/班员工也应继承可见。
            result.addAll(DataPermissionUtils.executeIgnore(
                    () -> projectDeptVisibilityMapper.selectProjectIdsByDeptIds(getDeptPathToRoot(userDeptId))));
        }

        Set<Long> leaderScopeDeptIds = getLeaderScopeDeptIds(userId);
        if (!leaderScopeDeptIds.isEmpty()) {
            result.addAll(DataPermissionUtils.executeIgnore(
                    () -> projectDeptVisibilityMapper.selectProjectIdsByDeptIds(leaderScopeDeptIds)));
        }
        return new ArrayList<>(result);
    }

    private boolean isSuperAdmin(Long userId) {
        return permissionApi.hasAnyRoles(userId, "super_admin");
    }

    private boolean isExplicitProjectManager(Long projectId, Long userId) {
        ProjectMemberDO member = getProjectMember(projectId, userId);
        return member != null && ProjectMemberRoleEnum.MANAGER.getValue().equals(member.getRoleType());
    }

    private ProjectMemberDO getProjectMember(Long projectId, Long userId) {
        return DataPermissionUtils.executeIgnore(
                () -> projectMemberMapper.selectByProjectIdAndUserId(projectId, userId));
    }

    private Long getUserDeptId(Long userId) {
        AdminUserRespDTO user = adminUserApi.getUser(userId);
        return user != null ? user.getDeptId() : null;
    }

    private List<Long> getProjectVisibleDeptIds(Long projectId) {
        return DataPermissionUtils.executeIgnore(
                        () -> projectDeptVisibilityMapper.selectListByProjectId(projectId)).stream()
                .map(ProjectDeptVisibilityDO::getDeptId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
    }

    private Set<Long> getLeaderScopeDeptIds(Long userId) {
        Set<Long> result = new LinkedHashSet<>();
        List<DeptRespDTO> leaderDepts = deptApi.getDeptListByLeaderUserId(userId);
        if (CollUtil.isEmpty(leaderDepts)) {
            return result;
        }
        for (DeptRespDTO leaderDept : leaderDepts) {
            if (leaderDept == null || leaderDept.getId() == null) {
                continue;
            }
            result.add(leaderDept.getId());
            List<DeptRespDTO> children = deptApi.getChildDeptList(leaderDept.getId());
            if (CollUtil.isNotEmpty(children)) {
                children.stream().map(DeptRespDTO::getId).forEach(result::add);
            }
        }
        return result;
    }

    /**
     * 返回员工在当前项目可读的完整部门服务包范围。
     *
     * <p>例如项目选中「安全运营服务部」，员工在其下的某个班，
     * 则返回该大部门及其全部子部门。这样服务项无论尚未分班（dept_id=大部门）
     * 还是已分到具体班组，同一部门服务包内的员工都可以查看。</p>
     */
    private List<Long> resolveEmployeeReadableDeptScope(Long projectId, Long userId) {
        Long userDeptId = getUserDeptId(userId);
        if (userDeptId == null) {
            return List.of();
        }
        Set<Long> userDeptPath = getDeptPathToRoot(userDeptId);
        Set<Long> result = new LinkedHashSet<>();
        for (Long selectedDeptId : getProjectVisibleDeptIds(projectId)) {
            if (userDeptPath.contains(selectedDeptId)) {
                addDeptTree(result, selectedDeptId);
            }
        }
        return new ArrayList<>(result);
    }

    private List<Long> resolveLeaderReadableDeptScope(Long projectId, Long userId) {
        Set<Long> result = new LinkedHashSet<>();
        for (Long selectedDeptId : getProjectVisibleDeptIds(projectId)) {
            if (deptApi.getAncestorChainLeaderUserIds(selectedDeptId).contains(userId)) {
                addDeptTree(result, selectedDeptId);
            }
        }
        // 角色可重叠：上级负责人若同时也在参与部门内，保留员工视角。
        result.addAll(resolveEmployeeReadableDeptScope(projectId, userId));
        return new ArrayList<>(result);
    }

    private Set<Long> getDeptPathToRoot(Long deptId) {
        Set<Long> result = new LinkedHashSet<>();
        Long currentDeptId = deptId;
        while (currentDeptId != null && currentDeptId != 0L && result.add(currentDeptId)) {
            DeptRespDTO dept = deptApi.getDept(currentDeptId);
            currentDeptId = dept != null ? dept.getParentId() : null;
        }
        return result;
    }

    private void addDeptTree(Set<Long> target, Long deptId) {
        if (deptId == null) {
            return;
        }
        target.add(deptId);
        List<DeptRespDTO> children = deptApi.getChildDeptList(deptId);
        if (CollUtil.isNotEmpty(children)) {
            children.stream().map(DeptRespDTO::getId).filter(java.util.Objects::nonNull).forEach(target::add);
        }
    }

    private List<Long> resolveManagedDeptScope(Long projectId, Long userId) {
        Set<Long> result = new LinkedHashSet<>();
        List<Long> projectVisibleDeptIds = getProjectVisibleDeptIds(projectId);
        List<DeptRespDTO> leaderDepts = deptApi.getDeptListByLeaderUserId(userId);
        if (CollUtil.isNotEmpty(leaderDepts)) {
            Set<Long> visibleDeptSet = new LinkedHashSet<>(projectVisibleDeptIds);
            for (DeptRespDTO leaderDept : leaderDepts) {
                if (leaderDept == null || leaderDept.getId() == null || !visibleDeptSet.contains(leaderDept.getId())) {
                    continue;
                }
                result.add(leaderDept.getId());
                List<DeptRespDTO> children = deptApi.getChildDeptList(leaderDept.getId());
                if (CollUtil.isNotEmpty(children)) {
                    children.stream().map(DeptRespDTO::getId).forEach(result::add);
                }
            }
        }
        return new ArrayList<>(result);
    }

    private boolean containsDept(List<Long> deptIds, Long deptId) {
        if (deptIds == null) {
            return true;
        }
        return deptId != null && deptIds.contains(deptId);
    }

}
