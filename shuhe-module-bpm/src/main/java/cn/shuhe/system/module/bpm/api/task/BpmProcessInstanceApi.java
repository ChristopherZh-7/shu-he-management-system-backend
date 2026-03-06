package cn.shuhe.system.module.bpm.api.task;

import cn.shuhe.system.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 流程实例 Api 接口
 *
 * @author ShuHe
 */
public interface BpmProcessInstanceApi {

    /**
     * 创建流程实例（提供给内部）
     *
     * @param userId 用户编号
     * @param reqDTO 创建信息
     * @return 实例的编号
     */
    String createProcessInstance(Long userId, @Valid BpmProcessInstanceCreateReqDTO reqDTO);

    /**
     * 检查用户是否是指定流程实例的任务审批人（包括待办任务和已完成任务）
     * 用于：审批人在审批过程中和审批完成后，都能查看相关业务数据
     *
     * @param userId            用户编号
     * @param processInstanceId 流程实例编号
     * @return 是否是审批人（当前或曾经）
     */
    boolean isTaskAssignee(Long userId, String processInstanceId);

    /**
     * 获取流程实例首个任务的审批人用户ID列表
     * 用于：提交审批时创建钉钉群聊等场景
     *
     * @param processInstanceId 流程实例编号
     * @return 审批人用户ID列表，无任务或任务无 assignee 时返回空列表
     */
    List<Long> getFirstTaskAssignees(String processInstanceId);

}
