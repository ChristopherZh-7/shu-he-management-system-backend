package cn.shuhe.system.module.system.service.user;

import cn.shuhe.system.module.infra.api.config.ConfigApi;
import cn.shuhe.system.module.system.controller.admin.auth.vo.AuthRegisterReqVO;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static cn.shuhe.system.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.shuhe.system.module.system.enums.ErrorCodeConstants.USER_REGISTER_DISABLED;
import static cn.shuhe.system.module.system.service.user.AdminUserServiceImpl.USER_REGISTER_ENABLED_KEY;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AdminSelfRegistrationTest {

    @Test
    void blocksRegistrationEvenWhenPreviouslyEnabledInTheDatabase() {
        ConfigApi configApi = mock(ConfigApi.class);
        when(configApi.getConfigValueByKey(USER_REGISTER_ENABLED_KEY)).thenReturn("true");
        AdminUserServiceImpl service = new AdminUserServiceImpl();
        ReflectionTestUtils.setField(service, "configApi", configApi);

        assertServiceException(() -> service.registerUser(new AuthRegisterReqVO()), USER_REGISTER_DISABLED);
        verifyNoInteractions(configApi);
    }
}
