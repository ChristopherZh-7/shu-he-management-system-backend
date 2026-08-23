package cn.shuhe.system.module.project.service.access;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectAccessServiceImplTest {

    private static final long PROJECT_ID = 99L;

    @InjectMocks
    private ProjectAccessServiceImpl accessService;

    @Mock
    private ProjectMemberMapper projectMemberMapper;
    @Mock
    private ProjectDeptVisibilityMapper projectDeptVisibilityMapper;
    @Mock
    private PermissionApi permissionApi;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private DeptApi deptApi;

    @BeforeEach
    void setUp() {
        when(permissionApi.hasAnyRoles(anyLong(), any(String[].class))).thenReturn(false);
    }

    @Test
    void selectedDepartmentAndChildDepartmentEmployeesShareTheDepartmentPackage() {
        long selectedDeptId = 101L;
        long exactUserId = 10L;
        long childUserId = 11L;
        stubProjectVisibility(selectedDeptId, 50L, Set.of());
        when(projectMemberMapper.selectByProjectIdAndUserId(PROJECT_ID, exactUserId)).thenReturn(null);
        when(projectMemberMapper.selectByProjectIdAndUserId(PROJECT_ID, childUserId)).thenReturn(null);
        when(adminUserApi.getUser(exactUserId)).thenReturn(user(exactUserId, selectedDeptId));
        when(adminUserApi.getUser(childUserId)).thenReturn(user(childUserId, 102L));
        when(deptApi.getDept(102L)).thenReturn(dept(102L, selectedDeptId, null));
        when(deptApi.getChildDeptList(selectedDeptId)).thenReturn(List.of(dept(102L, selectedDeptId, null)));

        assertThat(accessService.canViewProject(PROJECT_ID, exactUserId)).isTrue();
        assertThat(accessService.getReadableDeptIds(PROJECT_ID, exactUserId))
                .containsExactly(selectedDeptId, 102L);
        assertThat(accessService.canViewProject(PROJECT_ID, childUserId)).isTrue();
        assertThat(accessService.getReadableDeptIds(PROJECT_ID, childUserId))
                .containsExactly(selectedDeptId, 102L);
    }

    @Test
    void selectedDepartmentLeaderManagesOwnDepartmentTree() {
        long selectedDeptId = 101L;
        long leaderUserId = 50L;
        stubProjectVisibility(selectedDeptId, leaderUserId, Set.of(leaderUserId));
        when(projectMemberMapper.selectByProjectIdAndUserId(PROJECT_ID, leaderUserId)).thenReturn(null);
        when(deptApi.getDeptListByLeaderUserId(leaderUserId)).thenReturn(List.of(dept(selectedDeptId, leaderUserId)));
        when(deptApi.getChildDeptList(selectedDeptId)).thenReturn(List.of(dept(102L, null), dept(103L, null)));

        assertThat(accessService.getEffectiveRole(PROJECT_ID, leaderUserId))
                .isEqualTo(ProjectMemberRoleEnum.DEPT_LEADER.getValue());
        assertThat(accessService.getManageableDeptIds(PROJECT_ID, leaderUserId))
                .containsExactly(selectedDeptId, 102L, 103L);
        assertThat(accessService.canManageProject(PROJECT_ID, leaderUserId)).isFalse();
    }

    @Test
    void upperLeaderCanReadDepartmentPackageButCannotManageIt() {
        long selectedDeptId = 101L;
        long upperLeaderUserId = 60L;
        stubProjectVisibility(selectedDeptId, 50L, Set.of(50L, upperLeaderUserId));
        when(projectMemberMapper.selectByProjectIdAndUserId(PROJECT_ID, upperLeaderUserId)).thenReturn(null);

        assertThat(accessService.getEffectiveRole(PROJECT_ID, upperLeaderUserId))
                .isEqualTo(ProjectMemberRoleEnum.UPPER_LEADER.getValue());
        assertThat(accessService.canViewProject(PROJECT_ID, upperLeaderUserId)).isTrue();
        assertThat(accessService.getReadableDeptIds(PROJECT_ID, upperLeaderUserId))
                .containsExactly(selectedDeptId);
        assertThat(accessService.getManageableDeptIds(PROJECT_ID, upperLeaderUserId)).isEmpty();
    }

    @Test
    void upperLeaderInAnExactSelectedDepartmentKeepsOwnDepartmentReadScope() {
        long selectedDeptId = 101L;
        long overlappingUserId = 62L;
        stubProjectVisibility(selectedDeptId, 50L, Set.of(50L, overlappingUserId));
        when(projectMemberMapper.selectByProjectIdAndUserId(PROJECT_ID, overlappingUserId)).thenReturn(null);
        when(adminUserApi.getUser(overlappingUserId)).thenReturn(user(overlappingUserId, selectedDeptId));
        assertThat(accessService.getEffectiveRole(PROJECT_ID, overlappingUserId))
                .isEqualTo(ProjectMemberRoleEnum.UPPER_LEADER.getValue());
        assertThat(accessService.getReadableDeptIds(PROJECT_ID, overlappingUserId))
                .containsExactly(selectedDeptId);
        assertThat(accessService.getManageableDeptIds(PROJECT_ID, overlappingUserId)).isEmpty();
    }

    @Test
    void historicalRoleThreeDoesNotGrantDepartmentLeaderPermission() {
        long selectedDeptId = 101L;
        long historicalReviewerId = 61L;
        stubProjectVisibility(selectedDeptId, 50L, Set.of(50L));
        when(projectMemberMapper.selectByProjectIdAndUserId(PROJECT_ID, historicalReviewerId))
                .thenReturn(ProjectMemberDO.builder()
                        .projectId(PROJECT_ID)
                        .userId(historicalReviewerId)
                        .roleType(ProjectMemberRoleEnum.DEPT_LEADER.getValue())
                        .build());
        when(adminUserApi.getUser(historicalReviewerId)).thenReturn(user(historicalReviewerId, selectedDeptId));
        assertThat(accessService.getEffectiveRole(PROJECT_ID, historicalReviewerId))
                .isEqualTo(ProjectMemberRoleEnum.EXECUTOR.getValue());
        assertThat(accessService.getManageableDeptIds(PROJECT_ID, historicalReviewerId)).isEmpty();
        assertThat(accessService.getReadableDeptIds(PROJECT_ID, historicalReviewerId))
                .containsExactly(selectedDeptId);
    }

    @Test
    void projectManagerHasFullProjectAndDepartmentAccess() {
        long managerUserId = 70L;
        when(projectMemberMapper.selectByProjectIdAndUserId(PROJECT_ID, managerUserId))
                .thenReturn(ProjectMemberDO.builder()
                        .projectId(PROJECT_ID)
                        .userId(managerUserId)
                        .roleType(ProjectMemberRoleEnum.MANAGER.getValue())
                        .build());

        assertThat(accessService.canManageProject(PROJECT_ID, managerUserId)).isTrue();
        assertThat(accessService.getReadableDeptIds(PROJECT_ID, managerUserId)).isNull();
        assertThat(accessService.getManageableDeptIds(PROJECT_ID, managerUserId)).isNull();
        assertThat(accessService.canManageDept(PROJECT_ID, managerUserId, null)).isTrue();
    }

    @Test
    void leaderProjectListIncludesProjectsOfManagedChildDepartments() {
        long leaderUserId = 80L;
        when(projectMemberMapper.selectProjectIdsByUserId(leaderUserId)).thenReturn(List.of());
        when(adminUserApi.getUser(leaderUserId)).thenReturn(user(leaderUserId, 100L));
        when(deptApi.getDeptListByLeaderUserId(leaderUserId)).thenReturn(List.of(dept(100L, leaderUserId)));
        when(deptApi.getChildDeptList(100L)).thenReturn(List.of(dept(101L, null), dept(102L, null)));
        when(projectDeptVisibilityMapper.selectProjectIdsByDeptIds(any(Collection.class))).thenAnswer(invocation -> {
            Collection<?> deptIds = invocation.getArgument(0);
            return deptIds.size() > 1 ? List.of(9L) : List.of();
        });

        assertThat(accessService.getVisibleProjectIds(leaderUserId)).containsExactly(9L);
    }

    @Test
    void childDepartmentEmployeeProjectListInheritsSelectedParentDepartment() {
        long selectedDeptId = 101L;
        long childDeptId = 102L;
        long employeeId = 81L;
        when(projectMemberMapper.selectProjectIdsByUserId(employeeId)).thenReturn(List.of());
        when(adminUserApi.getUser(employeeId)).thenReturn(user(employeeId, childDeptId));
        when(deptApi.getDept(childDeptId)).thenReturn(dept(childDeptId, selectedDeptId, null));
        when(deptApi.getDept(selectedDeptId)).thenReturn(dept(selectedDeptId, null, null));
        when(projectDeptVisibilityMapper.selectProjectIdsByDeptIds(any(Collection.class)))
                .thenAnswer(invocation -> {
                    Collection<?> deptIds = invocation.getArgument(0);
                    return deptIds.contains(selectedDeptId) ? List.of(PROJECT_ID) : List.of();
                });

        assertThat(accessService.getVisibleProjectIds(employeeId)).containsExactly(PROJECT_ID);
    }

    private void stubProjectVisibility(Long deptId, Long directLeaderId, Set<Long> ancestorLeaders) {
        when(projectDeptVisibilityMapper.selectListByProjectId(PROJECT_ID)).thenReturn(List.of(visibility(deptId)));
        when(deptApi.getDept(deptId)).thenReturn(dept(deptId, directLeaderId));
        // 直接部门负责人在第一轮匹配后会短路，不会查上级负责人。
        lenient().when(deptApi.getAncestorChainLeaderUserIds(deptId)).thenReturn(ancestorLeaders);
    }

    private ProjectDeptVisibilityDO visibility(Long deptId) {
        return ProjectDeptVisibilityDO.builder().projectId(PROJECT_ID).deptId(deptId).build();
    }

    private DeptRespDTO dept(Long deptId, Long leaderUserId) {
        return dept(deptId, null, leaderUserId);
    }

    private DeptRespDTO dept(Long deptId, Long parentId, Long leaderUserId) {
        DeptRespDTO dept = new DeptRespDTO();
        dept.setId(deptId);
        dept.setParentId(parentId);
        dept.setLeaderUserId(leaderUserId);
        return dept;
    }

    private AdminUserRespDTO user(Long userId, Long deptId) {
        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setId(userId);
        user.setDeptId(deptId);
        return user;
    }

}
