package cn.shuhe.system.module.bpm.framework.flowable.core.candidate.strategy.other;

import cn.hutool.core.collection.CollUtil;
import cn.shuhe.system.framework.common.util.collection.CollectionUtils;
import cn.shuhe.system.module.bpm.framework.flowable.core.candidate.BpmTaskCandidateStrategy;
import cn.shuhe.system.module.bpm.framework.flowable.core.enums.BpmTaskCandidateStrategyEnum;
import cn.shuhe.system.module.bpm.framework.flowable.core.util.FlowableUtils;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.javax.el.PropertyNotFoundException;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 流程表达式 {@link BpmTaskCandidateStrategy} 实现类
 *
 * @author ShuHe
 */
@Component
@Slf4j
public class BpmTaskCandidateExpressionStrategy implements BpmTaskCandidateStrategy {

    @Override
    public BpmTaskCandidateStrategyEnum getStrategy() {
        return BpmTaskCandidateStrategyEnum.EXPRESSION;
    }

    @Override
    public void validateParam(String param) {
        // do nothing 因为它基本做不了校验
    }

    @Override
    public Set<Long> calculateUsersByTask(DelegateExecution execution, String param) {
        try {
            Object result = FlowableUtils.getExpressionValue(execution, param);
            Set<Long> userIds = CollectionUtils.toLinkedHashSet(Long.class, result);
            log.info("[calculateUsersByTask][表达式({}) 解析结果: {}, 审批人: {}]", param, result, userIds);
            if (CollUtil.isEmpty(userIds)) {
                log.warn("[calculateUsersByTask][表达式({}) 解析结果为空，可能导致审批人无法确定。流程变量: {}]", 
                        param, execution.getVariables());
            }
            return userIds;
        } catch (Exception e) {
            log.error("[calculateUsersByTask][表达式({}) 解析失败。流程变量: {}", param, execution.getVariables(), e);
            return Sets.newHashSet();
        }
    }

    @Override
    public Set<Long> calculateUsersByActivity(BpmnModel bpmnModel, String activityId, String param,
                                              Long startUserId, String processDefinitionId, Map<String, Object> processVariables) {
        Map<String, Object> variables = processVariables == null ? new HashMap<>() : processVariables;
        try {
            Object result = FlowableUtils.getExpressionValue(variables, param);
            Set<Long> userIds = CollectionUtils.toLinkedHashSet(Long.class, result);
            log.info("[calculateUsersByActivity][表达式({}) 解析结果: {}, 审批人: {}, 流程变量: {}]", 
                    param, result, userIds, variables);
            if (CollUtil.isEmpty(userIds)) {
                log.warn("[calculateUsersByActivity][表达式({}) 解析结果为空，可能导致审批人无法确定。流程变量: {}", 
                        param, variables);
            }
            return userIds;
        } catch (FlowableException ex) {
            // 预测未运行的节点时候，表达式如果包含 execution 或者不存在的流程变量会抛异常，此时忽略该异常！相当于说，不做流程预测！！！
            if (ex.getCause() != null && ex.getCause() instanceof PropertyNotFoundException) {
                log.warn("[calculateUsersByActivity][表达式({}) 变量({}) 中找不到属性，返回空集合", param, variables);
                return Sets.newHashSet();
            }
            log.error("[calculateUsersByActivity][表达式({}) 变量({}) 解析报错", param, variables, ex);
            throw ex;
        }
    }

}