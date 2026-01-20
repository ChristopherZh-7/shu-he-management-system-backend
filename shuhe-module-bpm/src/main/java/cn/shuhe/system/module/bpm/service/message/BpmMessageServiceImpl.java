package cn.shuhe.system.module.bpm.service.message;

import cn.shuhe.system.framework.web.config.WebProperties;
import cn.shuhe.system.module.bpm.service.message.dto.BpmMessageSendWhenProcessInstanceApproveReqDTO;
import cn.shuhe.system.module.bpm.service.message.dto.BpmMessageSendWhenProcessInstanceRejectReqDTO;
import cn.shuhe.system.module.bpm.service.message.dto.BpmMessageSendWhenTaskCreatedReqDTO;
import cn.shuhe.system.module.bpm.service.message.dto.BpmMessageSendWhenTaskTimeoutReqDTO;
import cn.shuhe.system.module.system.api.dingtalk.DingtalkNotifyApi;
import cn.shuhe.system.module.system.api.dingtalk.dto.DingtalkNotifySendReqDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.Resource;
import java.util.Collections;

/**
 * BPM 消息 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class BpmMessageServiceImpl implements BpmMessageService {

    @Resource
    private DingtalkNotifyApi dingtalkNotifyApi;

    @Resource
    private WebProperties webProperties;

    @Override
    public void sendMessageWhenProcessInstanceApprove(BpmMessageSendWhenProcessInstanceApproveReqDTO reqDTO) {
        // 发送钉钉通知给发起人
        sendDingtalkNotifyWhenProcessInstanceApprove(reqDTO);
    }

    /**
     * 发送钉钉通知 - 流程审批通过
     */
    private void sendDingtalkNotifyWhenProcessInstanceApprove(BpmMessageSendWhenProcessInstanceApproveReqDTO reqDTO) {
        try {
            String detailUrl = getProcessInstanceDetailUrl(reqDTO.getProcessInstanceId());
            String title = "流程审批通过通知";
            String content = String.format(
                    "**%s**\n\n" +
                    "- 流程名称：%s\n" +
                    "- 状态：审批通过\n\n" +
                    "[点击查看详情](%s)",
                    title,
                    reqDTO.getProcessInstanceName(),
                    detailUrl
            );

            DingtalkNotifySendReqDTO notifyReqDTO = new DingtalkNotifySendReqDTO()
                    .setUserIds(Collections.singletonList(reqDTO.getStartUserId()))
                    .setTitle(title)
                    .setContent(content);

            boolean success = dingtalkNotifyApi.sendWorkNotice(notifyReqDTO);
            if (success) {
                log.info("发送审批通过钉钉通知成功：流程={}, 发起人={}",
                        reqDTO.getProcessInstanceName(), reqDTO.getStartUserId());
            } else {
                log.warn("发送审批通过钉钉通知失败：流程={}, 发起人={}",
                        reqDTO.getProcessInstanceName(), reqDTO.getStartUserId());
            }
        } catch (Exception e) {
            log.error("发送审批通过钉钉通知异常：流程={}, 发起人={}",
                    reqDTO.getProcessInstanceName(), reqDTO.getStartUserId(), e);
        }
    }

    @Override
    public void sendMessageWhenProcessInstanceReject(BpmMessageSendWhenProcessInstanceRejectReqDTO reqDTO) {
        // 发送钉钉通知给发起人
        sendDingtalkNotifyWhenProcessInstanceReject(reqDTO);
    }

    /**
     * 发送钉钉通知 - 流程审批拒绝
     */
    private void sendDingtalkNotifyWhenProcessInstanceReject(BpmMessageSendWhenProcessInstanceRejectReqDTO reqDTO) {
        try {
            String detailUrl = getProcessInstanceDetailUrl(reqDTO.getProcessInstanceId());
            String title = "流程审批拒绝通知";
            String content = String.format(
                    "**%s**\n\n" +
                    "- 流程名称：%s\n" +
                    "- 状态：审批拒绝\n" +
                    "- 拒绝原因：%s\n\n" +
                    "[点击查看详情](%s)",
                    title,
                    reqDTO.getProcessInstanceName(),
                    reqDTO.getReason(),
                    detailUrl
            );

            DingtalkNotifySendReqDTO notifyReqDTO = new DingtalkNotifySendReqDTO()
                    .setUserIds(Collections.singletonList(reqDTO.getStartUserId()))
                    .setTitle(title)
                    .setContent(content);

            boolean success = dingtalkNotifyApi.sendWorkNotice(notifyReqDTO);
            if (success) {
                log.info("发送审批拒绝钉钉通知成功：流程={}, 发起人={}",
                        reqDTO.getProcessInstanceName(), reqDTO.getStartUserId());
            } else {
                log.warn("发送审批拒绝钉钉通知失败：流程={}, 发起人={}",
                        reqDTO.getProcessInstanceName(), reqDTO.getStartUserId());
            }
        } catch (Exception e) {
            log.error("发送审批拒绝钉钉通知异常：流程={}, 发起人={}",
                    reqDTO.getProcessInstanceName(), reqDTO.getStartUserId(), e);
        }
    }

    @Override
    public void sendMessageWhenTaskAssigned(BpmMessageSendWhenTaskCreatedReqDTO reqDTO) {
        // 发送钉钉通知给审批人
        sendDingtalkNotifyWhenTaskAssigned(reqDTO);
    }

    /**
     * 发送钉钉通知 - 任务分配
     */
    private void sendDingtalkNotifyWhenTaskAssigned(BpmMessageSendWhenTaskCreatedReqDTO reqDTO) {
        try {
            String detailUrl = getProcessInstanceDetailUrl(reqDTO.getProcessInstanceId());
            String title = "待审批任务提醒";
            String content = String.format(
                    "**%s**\n\n" +
                    "- 流程名称：%s\n" +
                    "- 任务名称：%s\n" +
                    "- 发起人：%s\n\n" +
                    "[点击查看详情](%s)",
                    title,
                    reqDTO.getProcessInstanceName(),
                    reqDTO.getTaskName(),
                    reqDTO.getStartUserNickname(),
                    detailUrl
            );

            DingtalkNotifySendReqDTO notifyReqDTO = new DingtalkNotifySendReqDTO()
                    .setUserIds(Collections.singletonList(reqDTO.getAssigneeUserId()))
                    .setTitle(title)
                    .setContent(content);

            boolean success = dingtalkNotifyApi.sendWorkNotice(notifyReqDTO);
            if (success) {
                log.info("发送钉钉通知成功：流程={}, 任务={}, 审批人={}",
                        reqDTO.getProcessInstanceName(), reqDTO.getTaskName(), reqDTO.getAssigneeUserId());
            } else {
                log.warn("发送钉钉通知失败：流程={}, 任务={}, 审批人={}",
                        reqDTO.getProcessInstanceName(), reqDTO.getTaskName(), reqDTO.getAssigneeUserId());
            }
        } catch (Exception e) {
            log.error("发送钉钉通知异常：流程={}, 任务={}, 审批人={}",
                    reqDTO.getProcessInstanceName(), reqDTO.getTaskName(), reqDTO.getAssigneeUserId(), e);
        }
    }

    @Override
    public void sendMessageWhenTaskTimeout(BpmMessageSendWhenTaskTimeoutReqDTO reqDTO) {
        // 发送钉钉通知给审批人
        sendDingtalkNotifyWhenTaskTimeout(reqDTO);
    }

    /**
     * 发送钉钉通知 - 任务超时
     */
    private void sendDingtalkNotifyWhenTaskTimeout(BpmMessageSendWhenTaskTimeoutReqDTO reqDTO) {
        try {
            String detailUrl = getProcessInstanceDetailUrl(reqDTO.getProcessInstanceId());
            String title = "任务超时提醒";
            String content = String.format(
                    "**%s**\n\n" +
                    "- 流程名称：%s\n" +
                    "- 任务名称：%s\n" +
                    "- 状态：任务已超时，请尽快处理\n\n" +
                    "[点击查看详情](%s)",
                    title,
                    reqDTO.getProcessInstanceName(),
                    reqDTO.getTaskName(),
                    detailUrl
            );

            DingtalkNotifySendReqDTO notifyReqDTO = new DingtalkNotifySendReqDTO()
                    .setUserIds(Collections.singletonList(reqDTO.getAssigneeUserId()))
                    .setTitle(title)
                    .setContent(content);

            boolean success = dingtalkNotifyApi.sendWorkNotice(notifyReqDTO);
            if (success) {
                log.info("发送任务超时钉钉通知成功：流程={}, 任务={}, 审批人={}",
                        reqDTO.getProcessInstanceName(), reqDTO.getTaskName(), reqDTO.getAssigneeUserId());
            } else {
                log.warn("发送任务超时钉钉通知失败：流程={}, 任务={}, 审批人={}",
                        reqDTO.getProcessInstanceName(), reqDTO.getTaskName(), reqDTO.getAssigneeUserId());
            }
        } catch (Exception e) {
            log.error("发送任务超时钉钉通知异常：流程={}, 任务={}, 审批人={}",
                    reqDTO.getProcessInstanceName(), reqDTO.getTaskName(), reqDTO.getAssigneeUserId(), e);
        }
    }

    private String getProcessInstanceDetailUrl(String taskId) {
        return webProperties.getAdminUi().getUrl() + "/bpm/process-instance/detail?id=" + taskId;
    }

}
