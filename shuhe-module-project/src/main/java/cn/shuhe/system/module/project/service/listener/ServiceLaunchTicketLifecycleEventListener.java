package cn.shuhe.system.module.project.service.listener;

import cn.shuhe.system.module.project.dal.dataobject.ProjectRoundDO;
import cn.shuhe.system.module.project.dal.dataobject.ServiceLaunchDO;
import cn.shuhe.system.module.project.dal.mysql.ProjectRoundMapper;
import cn.shuhe.system.module.project.dal.mysql.ServiceLaunchMapper;
import cn.shuhe.system.module.project.service.ProjectRoundService;
import cn.shuhe.system.module.ticket.enums.TicketActionEnum;
import cn.shuhe.system.module.ticket.enums.TicketBusinessTypeEnum;
import cn.shuhe.system.module.ticket.framework.event.TicketLifecycleEvent;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** 将服务工单的交付/重做状态同步到该工单自动创建的项目轮次。 */
@Component
@Slf4j
public class ServiceLaunchTicketLifecycleEventListener {

    @Resource
    private ServiceLaunchMapper serviceLaunchMapper;

    @Resource
    private ProjectRoundMapper projectRoundMapper;

    @Resource
    private ProjectRoundService projectRoundService;

    @EventListener
    public void onTicketLifecycleChanged(TicketLifecycleEvent event) {
        if (!TicketBusinessTypeEnum.SERVICE_LAUNCH.getType().equals(event.getBusinessType())
                || event.getBusinessId() == null) {
            return;
        }

        ServiceLaunchDO launch = serviceLaunchMapper.selectById(event.getBusinessId());
        if (launch == null || launch.getRoundId() == null) {
            throw new IllegalStateException("服务工单缺少可同步的项目轮次，ticketId=" + event.getTicketId());
        }

        Long roundId = launch.getRoundId();
        ProjectRoundDO round = projectRoundMapper.selectById(roundId);
        if (round == null) {
            throw new IllegalStateException("服务工单关联的项目轮次不存在，ticketId=" + event.getTicketId());
        }
        if (TicketActionEnum.FINISH.getAction().equals(event.getAction())) {
            ProjectRoundDO delivery = new ProjectRoundDO();
            delivery.setId(roundId);
            delivery.setResult(event.getResult());
            projectRoundMapper.updateById(delivery);
            projectRoundService.updateRoundProgress(roundId, 90);
            projectRoundService.updateRoundStatus(roundId, 1);
            log.info("【服务工单履约同步】项目轮次已提交待验收，ticketId={} roundId={}", event.getTicketId(), roundId);
            return;
        }

        if (TicketActionEnum.REVIEW_PASS.getAction().equals(event.getAction())) {
            ProjectRoundDO accepted = new ProjectRoundDO();
            accepted.setId(roundId);
            accepted.setActualEndTime(LocalDateTime.now());
            projectRoundMapper.updateById(accepted);
            projectRoundService.updateRoundProgress(roundId, 100);
            projectRoundService.updateRoundStatus(roundId, 2);
            log.info("【服务工单履约同步】验收通过，项目轮次正式完成，ticketId={} roundId={}",
                    event.getTicketId(), roundId);
            return;
        }

        if (TicketActionEnum.REVIEW_REJECT.getAction().equals(event.getAction())
                || TicketActionEnum.REOPEN.getAction().equals(event.getAction())) {
            // 兼容历史上未回写实际开始时间的轮次，重做时补齐履约时间轴。
            if (round.getActualStartTime() == null) {
                ProjectRoundDO execution = new ProjectRoundDO();
                execution.setId(roundId);
                execution.setActualStartTime(LocalDateTime.now());
                projectRoundMapper.updateById(execution);
            }
            projectRoundMapper.update(null, new LambdaUpdateWrapper<ProjectRoundDO>()
                    .eq(ProjectRoundDO::getId, roundId)
                    .set(ProjectRoundDO::getProgress, 0)
                    .set(ProjectRoundDO::getActualEndTime, null)
                    .set(ProjectRoundDO::getResult, null));
            projectRoundService.updateRoundStatus(roundId, 1);
            log.info("【服务工单履约同步】项目轮次已恢复执行中，ticketId={} roundId={} action={}",
                    event.getTicketId(), roundId, event.getAction());
        }
    }
}
