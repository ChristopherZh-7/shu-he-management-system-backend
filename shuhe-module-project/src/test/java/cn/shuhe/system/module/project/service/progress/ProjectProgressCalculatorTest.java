package cn.shuhe.system.module.project.service.progress;

import cn.shuhe.system.module.project.dal.dataobject.ProjectRoundDO;
import cn.shuhe.system.module.project.dal.dataobject.ServiceItemDO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectProgressCalculatorTest {

    @Test
    void monthlyServiceUsesContractPeriodsAndAcceptedRounds() {
        ServiceItemDO item = ServiceItemDO.builder()
                .id(1L).status(1).frequencyType(1).maxCount(2)
                .planStartTime(LocalDateTime.of(2026, 1, 1, 0, 0))
                .planEndTime(LocalDateTime.of(2026, 3, 31, 23, 59))
                .build();
        List<ProjectRoundDO> rounds = List.of(
                ProjectRoundDO.builder().serviceItemId(1L).status(2).progress(100).build(),
                ProjectRoundDO.builder().serviceItemId(1L).status(1).progress(90).build());

        ProjectProgressCalculator.Summary summary = ProjectProgressCalculator.calculate(
                List.of(item), Map.of(1L, rounds));

        assertEquals(6, summary.plannedExecutionCount());
        assertEquals(1, summary.acceptedExecutionCount());
        assertEquals(32, summary.progress());
    }

    @Test
    void rejectedOrReopenedRoundDoesNotPretendToBeComplete() {
        ServiceItemDO item = ServiceItemDO.builder()
                .id(1L).status(1).frequencyType(5).maxCount(1).build();
        ProjectRoundDO round = ProjectRoundDO.builder()
                .serviceItemId(1L).status(1).progress(0).build();

        ProjectProgressCalculator.Summary summary = ProjectProgressCalculator.calculate(
                List.of(item), Map.of(1L, List.of(round)));

        assertEquals(0, summary.progress());
        assertEquals(0, summary.acceptedExecutionCount());
    }

    @Test
    void onDemandServiceStaysBelowCompleteUntilServiceItemClosed() {
        ServiceItemDO item = ServiceItemDO.builder()
                .id(1L).status(1).frequencyType(0).build();
        ProjectRoundDO accepted = ProjectRoundDO.builder()
                .serviceItemId(1L).status(2).progress(100).build();

        ProjectProgressCalculator.Summary summary = ProjectProgressCalculator.calculate(
                List.of(item), Map.of(1L, List.of(accepted)));

        assertEquals(99, summary.progress());
        assertTrue(summary.hasOnDemandService());
        assertEquals(1, summary.acceptedExecutionCount());
    }
}
