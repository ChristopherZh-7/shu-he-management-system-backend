package cn.shuhe.system.module.project.service.listener;

import cn.shuhe.system.framework.test.core.ut.BaseMockitoUnitTest;
import cn.shuhe.system.module.project.dal.dataobject.ProjectRoundDO;
import cn.shuhe.system.module.project.dal.dataobject.ServiceLaunchDO;
import cn.shuhe.system.module.project.dal.mysql.ProjectRoundMapper;
import cn.shuhe.system.module.project.dal.mysql.ServiceLaunchMapper;
import cn.shuhe.system.module.project.service.ProjectRoundService;
import cn.shuhe.system.module.ticket.enums.TicketActionEnum;
import cn.shuhe.system.module.ticket.enums.TicketBusinessTypeEnum;
import cn.shuhe.system.module.ticket.framework.event.TicketLifecycleEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServiceLaunchTicketLifecycleEventListenerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ServiceLaunchTicketLifecycleEventListener listener;
    @Mock
    private ServiceLaunchMapper serviceLaunchMapper;
    @Mock
    private ProjectRoundMapper projectRoundMapper;
    @Mock
    private ProjectRoundService projectRoundService;

    @Test
    void finishOnlyMovesRoundToPendingTechnicalReview() {
        prepare(88L, 9L);

        listener.onTicketLifecycleChanged(event(TicketActionEnum.FINISH, "报告已提交"));

        ArgumentCaptor<ProjectRoundDO> captor = ArgumentCaptor.forClass(ProjectRoundDO.class);
        verify(projectRoundMapper).updateById(captor.capture());
        assertNull(captor.getValue().getActualEndTime());
        verify(projectRoundService).updateRoundProgress(9L, 80);
        verify(projectRoundService).updateRoundStatus(9L, 4);
    }

    @Test
    void technicalReviewPassMovesRoundToPendingProjectAcceptance() {
        prepare(88L, 9L);

        listener.onTicketLifecycleChanged(event(TicketActionEnum.TECH_REVIEW_PASS, "技术审核通过"));

        verify(projectRoundService).updateRoundProgress(9L, 90);
        verify(projectRoundService).updateRoundStatus(9L, 5);
    }

    @Test
    void reviewPassCompletesRoundAndWritesActualEndTime() {
        prepare(88L, 9L);

        listener.onTicketLifecycleChanged(event(TicketActionEnum.REVIEW_PASS, "验收通过"));

        ArgumentCaptor<ProjectRoundDO> captor = ArgumentCaptor.forClass(ProjectRoundDO.class);
        verify(projectRoundMapper).updateById(captor.capture());
        assertNotNull(captor.getValue().getActualEndTime());
        verify(projectRoundService).updateRoundProgress(9L, 100);
        verify(projectRoundService).updateRoundStatus(9L, 2);
    }

    private void prepare(Long launchId, Long roundId) {
        ServiceLaunchDO launch = new ServiceLaunchDO();
        launch.setId(launchId);
        launch.setRoundId(roundId);
        when(serviceLaunchMapper.selectById(eq(launchId))).thenReturn(launch);
        when(projectRoundMapper.selectById(eq(roundId)))
                .thenReturn(ProjectRoundDO.builder().id(roundId).status(1).progress(0).build());
    }

    private TicketLifecycleEvent event(TicketActionEnum action, String result) {
        return TicketLifecycleEvent.builder()
                .ticketId(3L)
                .businessType(TicketBusinessTypeEnum.SERVICE_LAUNCH.getType())
                .businessId(88L)
                .action(action.getAction())
                .result(result)
                .build();
    }
}
