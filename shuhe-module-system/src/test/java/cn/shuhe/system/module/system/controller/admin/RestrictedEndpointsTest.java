package cn.shuhe.system.module.system.controller.admin;

import cn.shuhe.system.framework.common.biz.system.permission.PermissionCommonApi;
import cn.shuhe.system.framework.common.enums.CommonStatusEnum;
import cn.shuhe.system.framework.common.enums.UserTypeEnum;
import cn.shuhe.system.framework.security.core.LoginUser;
import cn.shuhe.system.framework.security.core.service.SecurityFrameworkServiceImpl;
import cn.shuhe.system.module.infra.controller.admin.file.FileController;
import cn.shuhe.system.module.infra.controller.admin.file.vo.file.FileUploadReqVO;
import cn.shuhe.system.module.infra.service.file.FileService;
import cn.shuhe.system.module.system.controller.admin.dept.DeptController;
import cn.shuhe.system.module.system.controller.admin.permission.RoleController;
import cn.shuhe.system.module.system.controller.admin.user.UserController;
import cn.shuhe.system.module.system.service.dept.DeptService;
import cn.shuhe.system.module.system.service.permission.RoleService;
import cn.shuhe.system.module.system.service.user.AdminUserService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SpringJUnitConfig({RestrictedEndpointsTest.MethodSecurityConfig.class, UserController.class,
        DeptController.class, RoleController.class, FileController.class})
class RestrictedEndpointsTest {

    private static final Long USER_ID = 42L;

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class MethodSecurityConfig {

        @Bean("ss")
        SecurityFrameworkServiceImpl securityService(PermissionCommonApi permissionApi) {
            return new SecurityFrameworkServiceImpl(permissionApi);
        }

    }

    @MockitoBean
    private PermissionCommonApi permissionApi;
    @MockitoBean
    private AdminUserService userService;
    @MockitoBean
    private DeptService deptService;
    @MockitoBean
    private RoleService roleService;
    @MockitoBean
    private FileService fileService;

    @Resource
    private UserController userController;
    @Resource
    private DeptController deptController;
    @Resource
    private RoleController roleController;
    @Resource
    private FileController fileController;

    @BeforeEach
    void setAuthenticatedUser() {
        LoginUser user = new LoginUser().setId(USER_ID).setUserType(UserTypeEnum.ADMIN.getValue());
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(user, null, List.of()));
    }

    @AfterEach
    void clearAuthenticatedUser() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deniesDirectoryDataAndUploadWithoutPermissions() {
        assertThrows(AccessDeniedException.class, userController::getSimpleUserList);
        assertThrows(AccessDeniedException.class, userController::getDeptLeaderSimpleList);
        assertThrows(AccessDeniedException.class, deptController::getSimpleDeptList);
        assertThrows(AccessDeniedException.class, deptController::getBusinessDeptList);
        assertThrows(AccessDeniedException.class, roleController::getSimpleRoleList);
        assertThrows(AccessDeniedException.class, () -> fileController.uploadFile(new FileUploadReqVO()));
        verifyNoInteractions(userService, deptService, roleService, fileService);
    }

    @Test
    void allowsRoleLookupWithItsOwnPermission() {
        when(permissionApi.hasAnyPermissions(USER_ID, "system:role:query", "system:role:lookup",
                "system:permission:assign-user-role")).thenReturn(true);
        when(roleService.getRoleListByStatus(Set.of(CommonStatusEnum.ENABLE.getStatus())))
                .thenReturn(new ArrayList<>());
        assertTrue(roleController.getSimpleRoleList().getData().isEmpty());
    }

}
