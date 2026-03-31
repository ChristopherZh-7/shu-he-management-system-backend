package cn.shuhe.system.module.bpm.framework.flowable.core.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.shuhe.system.framework.common.core.KeyValue;
import cn.shuhe.system.framework.common.util.json.JsonUtils;


import cn.shuhe.system.module.bpm.controller.admin.definition.vo.form.BpmFormFieldVO;
import cn.shuhe.system.module.bpm.dal.dataobject.definition.BpmProcessDefinitionInfoDO;
import cn.shuhe.system.module.bpm.enums.definition.BpmModelFormTypeEnum;
import cn.shuhe.system.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import lombok.SneakyThrows;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.common.engine.impl.variable.MapDelegateVariableContainer;
import org.flowable.engine.ManagementService;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.TaskInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static cn.shuhe.system.framework.common.util.collection.CollectionUtils.convertList;

/**
 * Flowable 相关的工具方法
 *
 * @author ShuHe
 */
public class FlowableUtils {

    // ========== User 相关的工具方法 ==========

    public static void setAuthenticatedUserId(Long userId) {
        Authentication.setAuthenticatedUserId(String.valueOf(userId));
    }

    public static void clearAuthenticatedUserId() {
        Authentication.setAuthenticatedUserId(null);
    }

    public static <V> V executeAuthenticatedUserId(Long userId, Callable<V> callable) {
        setAuthenticatedUserId(userId);
        try {
            return callable.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            clearAuthenticatedUserId();
        }
    }

    public static String getTenantId() {
        return ProcessEngineConfiguration.NO_TENANT_ID;
    }

    public static void execute(String tenantIdStr, Runnable runnable) {
        runnable.run();
    }

    @SneakyThrows
    public static <V> V execute(String tenantIdStr, Callable<V> callable) {
        return callable.call();
    }

    // ========== Execution 相关的工具方法 ==========

    /**
     * 格式化多实例（并签、或签）的 collectionVariable 变量（多实例对应的多审批人列表）
     *
     * @param activityId 活动编号
     * @return collectionVariable 变量
     */
    public static String formatExecutionCollectionVariable(String activityId) {
        return activityId + "_assignees";
    }

    /**
     * 格式化多实例（并签、或签）的 collectionElementVariable 变量（当前实例对应的一个审批人）
     *
     * @param activityId 活动编号
     * @return collectionElementVariable 变量
     */
    public static String formatExecutionCollectionElementVariable(String activityId) {
        return activityId + "_assignee";
    }

    // ========== ProcessInstance 相关的工具方法 ==========

    public static Integer getProcessInstanceStatus(ProcessInstance processInstance) {
        return getProcessInstanceStatus(processInstance.getProcessVariables());
    }

    public static Integer getProcessInstanceStatus(HistoricProcessInstance processInstance) {
        return getProcessInstanceStatus(processInstance.getProcessVariables());
    }

    /**
     * 获得流程实例的状态
     *
     * @param processVariables 流程实例的 variables
     * @return 状态
     */
    private static Integer getProcessInstanceStatus(Map<String, Object> processVariables) {
        return (Integer) processVariables.get(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_STATUS);
    }

    /**
     * 获得流程实例的审批原因
     *
     * @param processInstance 流程实例
     * @return 审批原因
     */
    public static String getProcessInstanceReason(HistoricProcessInstance processInstance) {
        return (String) processInstance.getProcessVariables().get(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_REASON);
    }

    /**
     * 获得流程实例的表单
     *
     * @param processInstance 流程实例
     * @return 表单
     */
    public static Map<String, Object> getProcessInstanceFormVariable(ProcessInstance processInstance) {
        Map<String, Object> processVariables = new HashMap<>(processInstance.getProcessVariables());
        return filterProcessInstanceFormVariable(processVariables);
    }

    /**
     * 获得流程实例的表单
     *
     * @param processInstance 流程实例
     * @return 表单
     */
    public static Map<String, Object> getProcessInstanceFormVariable(HistoricProcessInstance processInstance) {
        Map<String, Object> processVariables = new HashMap<>(processInstance.getProcessVariables());
        return filterProcessInstanceFormVariable(processVariables);
    }

    /**
     * 过滤流程实例的表单
     *
     * 为什么要过滤？目前使用 processVariables 存储所有流程实例的拓展字段，需要过滤掉一部分的系统字段，从而实现表单的展示
     *
     * @param processVariables 流程实例的 variables
     * @return 过滤后的表单
     */
    public static Map<String, Object> filterProcessInstanceFormVariable(Map<String, Object> processVariables) {
        processVariables.remove(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_STATUS);
        return processVariables;
    }

    /**
     * 获得流程实例的发起用户选择的审批人 Map
     *
     * @param processInstance 流程实例
     * @return 发起用户选择的审批人 Map
     */
    public static Map<String, List<Long>> getStartUserSelectAssignees(ProcessInstance processInstance) {
        return processInstance != null ? getStartUserSelectAssignees(processInstance.getProcessVariables()) : null;
    }

    /**
     * 获得流程实例的发起用户选择的审批人 Map
     *
     * @param processVariables 流程变量
     * @return 发起用户选择的审批人 Map
     */
    @SuppressWarnings("unchecked")
    public static Map<String, List<Long>> getStartUserSelectAssignees(Map<String, Object> processVariables) {
        if (processVariables == null) {
            return new HashMap<>();
        }
        return (Map<String, List<Long>>) processVariables.get(
                BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_START_USER_SELECT_ASSIGNEES);
    }

    /**
     * 获得流程实例的审批用户选择的下一个节点的审批人 Map
     *
     * @param processInstance 流程实例
     * @return 审批用户选择的下一个节点的审批人Map
     */
    public static Map<String, List<Long>> getApproveUserSelectAssignees(ProcessInstance processInstance) {
        return processInstance != null ? getApproveUserSelectAssignees(processInstance.getProcessVariables()) : null;
    }

    /**
     * 获得流程实例的审批用户选择的下一个节点的审批人 Map
     *
     * @param processVariables 流程变量
     * @return 审批用户选择的下一个节点的审批人Map Map
     */
    @SuppressWarnings("unchecked")
    public static Map<String, List<Long>> getApproveUserSelectAssignees(Map<String, Object> processVariables) {
        if (processVariables == null) {
            return new HashMap<>();
        }
        return (Map<String, List<Long>>) processVariables.get(
                BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_APPROVE_USER_SELECT_ASSIGNEES);
    }

    /**
     * 业务表单（CUSTOM）列表摘要：默认展示的流程变量顺序（与 CRM 合同/商机等发起时写入的变量一致）
     */
    private static final List<String> DEFAULT_CUSTOM_SUMMARY_VARIABLE_KEYS = Arrays.asList(
            "customerName", "name", "no", "totalPrice", "ownerUserName");

    /**
     * 业务表单流程变量名 → 列表展示标题
     */
    private static final Map<String, String> CUSTOM_SUMMARY_VARIABLE_LABELS;

    static {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("customerName", "最终客户");
        labels.put("name", "名称");
        labels.put("no", "编号");
        labels.put("totalPrice", "金额");
        labels.put("ownerUserName", "负责人");
        labels.put("intermediaryName", "合作商");
        labels.put("deptAllocationsText", "部门分配");
        CUSTOM_SUMMARY_VARIABLE_LABELS = Collections.unmodifiableMap(labels);
    }

    /**
     * 获得流程实例的摘要
     *
     * <p>流程表单（{@link BpmModelFormTypeEnum#NORMAL}）：根据动态表单字段生成摘要。
     *
     * <p>业务表单（{@link BpmModelFormTypeEnum#CUSTOM}）：根据流程变量生成摘要（如 CRM 写入的 customerName 等），
     * 便于「我的流程」列表无需点进详情即可看到关键信息。
     *
     * @param processDefinitionInfo 流程定义
     * @param processVariables      流程实例的 variables
     * @return 摘要
     */
    public static List<KeyValue<String, String>> getSummary(BpmProcessDefinitionInfoDO processDefinitionInfo,
                                                            Map<String, Object> processVariables) {
        if (ObjectUtil.isNull(processDefinitionInfo)) {
            return null;
        }
        Integer formType = processDefinitionInfo.getFormType();
        if (BpmModelFormTypeEnum.NORMAL.getType().equals(formType)) {
            return getNormalFormSummary(processDefinitionInfo, processVariables);
        }
        if (BpmModelFormTypeEnum.CUSTOM.getType().equals(formType)) {
            return getCustomFormSummary(processDefinitionInfo, processVariables);
        }
        return null;
    }

    private static List<KeyValue<String, String>> getNormalFormSummary(BpmProcessDefinitionInfoDO processDefinitionInfo,
                                                                       Map<String, Object> processVariables) {
        // 解析表单配置
        Map<String, BpmFormFieldVO> formFieldsMap = new HashMap<>();
        processDefinitionInfo.getFormFields().forEach(formFieldStr -> {
            BpmFormFieldVO formField = JsonUtils.parseObject(formFieldStr, BpmFormFieldVO.class);
            if (formField != null) {
                formFieldsMap.put(formField.getField(), formField);
            }
        });

        // 情况一：当自定义了摘要
        if (ObjectUtil.isNotNull(processDefinitionInfo.getSummarySetting())
                && Boolean.TRUE.equals(processDefinitionInfo.getSummarySetting().getEnable())) {
            return convertList(processDefinitionInfo.getSummarySetting().getSummary(), item -> {
                BpmFormFieldVO formField = formFieldsMap.get(item);
                if (formField != null) {
                    return new KeyValue<String, String>(formField.getTitle(),
                            processVariables != null ? processVariables.getOrDefault(item, "").toString() : "");
                }
                return null;
            });
        }

        // 情况二：默认摘要展示前三个表单字段
        return formFieldsMap.entrySet().stream()
                .limit(3)
                .map(entry -> new KeyValue<>(entry.getValue().getTitle(),
                        processVariables != null
                                ? MapUtil.getStr(processVariables, entry.getValue().getField(), "")
                                : ""))
                .collect(Collectors.toList());
    }

    /**
     * 业务表单：从流程变量组装列表摘要（变量名需在流程设计器中配置，或使用系统默认的一组变量）
     */
    private static List<KeyValue<String, String>> getCustomFormSummary(BpmProcessDefinitionInfoDO processDefinitionInfo,
                                                                       Map<String, Object> processVariables) {
        Map<String, Object> vars = processVariables != null ? processVariables : Collections.emptyMap();
        List<String> keys;
        if (ObjectUtil.isNotNull(processDefinitionInfo.getSummarySetting())
                && Boolean.TRUE.equals(processDefinitionInfo.getSummarySetting().getEnable())
                && CollUtil.isNotEmpty(processDefinitionInfo.getSummarySetting().getSummary())) {
            keys = processDefinitionInfo.getSummarySetting().getSummary();
        } else {
            keys = DEFAULT_CUSTOM_SUMMARY_VARIABLE_KEYS;
        }
        List<KeyValue<String, String>> result = new ArrayList<>(keys.size());
        for (String key : keys) {
            if (ObjectUtil.isEmpty(key)) {
                continue;
            }
            String label = CUSTOM_SUMMARY_VARIABLE_LABELS.getOrDefault(key, key);
            String value = MapUtil.getStr(vars, key, "");
            if (value == null) {
                value = "";
            }
            result.add(new KeyValue<>(label, value));
        }
        return result.isEmpty() ? null : result;
    }

    // ========== Task 相关的工具方法 ==========

    /**
     * 获得任务的状态
     *
     * @param task 任务
     * @return 状态
     */
    public static Integer getTaskStatus(TaskInfo task) {
        return (Integer) task.getTaskLocalVariables().get(BpmnVariableConstants.TASK_VARIABLE_STATUS);
    }

    /**
     * 获得任务的审批原因
     *
     * @param task 任务
     * @return 审批原因
     */
    public static String getTaskReason(TaskInfo task) {
        return (String) task.getTaskLocalVariables().get(BpmnVariableConstants.TASK_VARIABLE_REASON);
    }

    /**
     * 获得任务的签名图片 URL
     *
     * @param task 任务
     * @return 签名图片 URL
     */
    public static String getTaskSignPicUrl(TaskInfo task) {
        return (String) task.getTaskLocalVariables().get(BpmnVariableConstants.TASK_SIGN_PIC_URL);
    }

    /**
     * 获得任务的表单
     *
     * @param task 任务
     * @return 表单
     */
    public static Map<String, Object> getTaskFormVariable(TaskInfo task) {
        Map<String, Object> formVariables = new HashMap<>(task.getTaskLocalVariables());
        filterTaskFormVariable(formVariables);
        return formVariables;
    }

    /**
     * 过滤任务的表单
     *
     * 为什么要过滤？目前使用 taskLocalVariables 存储所有任务的拓展字段，需要过滤掉一部分的系统字段，从而实现表单的展示
     *
     * @param taskLocalVariables 任务的 taskLocalVariables
     * @return 过滤后的表单
     */
    public static Map<String, Object> filterTaskFormVariable(Map<String, Object> taskLocalVariables) {
        taskLocalVariables.remove(BpmnVariableConstants.TASK_VARIABLE_STATUS);
        taskLocalVariables.remove(BpmnVariableConstants.TASK_VARIABLE_REASON);
        return taskLocalVariables;
    }

    // ========== Expression 相关的工具方法 ==========

    private static Object getExpressionValue(VariableContainer variableContainer, String expressionString,
                                             ProcessEngineConfigurationImpl processEngineConfiguration) {
        assert processEngineConfiguration != null;
        ExpressionManager expressionManager = processEngineConfiguration.getExpressionManager();
        assert expressionManager != null;
        Expression expression = expressionManager.createExpression(expressionString);
        return expression.getValue(variableContainer);
    }

    public static Object getExpressionValue(VariableContainer variableContainer, String expressionString) {
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
        if (processEngineConfiguration != null) {
            return getExpressionValue(variableContainer, expressionString, processEngineConfiguration);
        }
        // 如果 ProcessEngineConfigurationImpl 获取不到，则需要通过 ManagementService 来获取
        ManagementService managementService = SpringUtil.getBean(ManagementService.class);
        assert managementService != null;
        return managementService.executeCommand(context ->
                getExpressionValue(variableContainer, expressionString, CommandContextUtil.getProcessEngineConfiguration()));
    }

    public static Object getExpressionValue(Map<String, Object> variable, String expressionString) {
        VariableContainer variableContainer = new MapDelegateVariableContainer(variable, VariableContainer.empty());
        return getExpressionValue(variableContainer, expressionString);
    }

}
