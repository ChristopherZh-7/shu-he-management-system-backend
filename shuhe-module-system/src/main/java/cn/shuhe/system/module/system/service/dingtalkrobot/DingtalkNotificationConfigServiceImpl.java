package cn.shuhe.system.module.system.service.dingtalkrobot;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.common.util.object.BeanUtils;
import cn.shuhe.system.module.system.controller.admin.dingtalkrobot.vo.*;
import cn.shuhe.system.module.system.dal.dataobject.dingtalkrobot.DingtalkNotificationConfigDO;
import cn.shuhe.system.module.system.dal.dataobject.dingtalkrobot.DingtalkNotificationLogDO;
import cn.shuhe.system.module.system.dal.dataobject.dingtalkrobot.DingtalkRobotDO;
import cn.shuhe.system.module.system.dal.dataobject.user.AdminUserDO;
import cn.shuhe.system.module.system.dal.mysql.dingtalkrobot.DingtalkNotificationConfigMapper;
import cn.shuhe.system.module.system.dal.mysql.dingtalkrobot.DingtalkNotificationLogMapper;
import cn.shuhe.system.module.system.service.user.AdminUserService;
import cn.shuhe.system.module.system.service.dingtalkconfig.DingtalkApiService.RobotSendResult;

import static cn.shuhe.system.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.shuhe.system.module.system.enums.ErrorCodeConstants.*;

/**
 * 钉钉通知场景配置 Service 实现类
 *
 * @author shuhe
 */
@Slf4j
@Service
@Validated
public class DingtalkNotificationConfigServiceImpl implements DingtalkNotificationConfigService {

    @Resource
    private DingtalkNotificationConfigMapper notificationConfigMapper;

    @Resource
    private DingtalkNotificationLogMapper notificationLogMapper;

    @Resource
    private DingtalkRobotService dingtalkRobotService;

    @Resource
    private AdminUserService adminUserService;

    // 模板变量正则：${variableName}
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    // ==================== 配置管理 ====================

    @Override
    public Long createNotificationConfig(DingtalkNotificationConfigSaveReqVO createReqVO) {
        // 验证机器人存在
        validateRobotExists(createReqVO.getRobotId());

        DingtalkNotificationConfigDO config = BeanUtils.toBean(createReqVO, DingtalkNotificationConfigDO.class);
        // 设置默认值
        if (config.getMsgType() == null) {
            config.setMsgType("markdown");
        }
        if (config.getAtType() == null) {
            config.setAtType(0);
        }
        if (config.getStatus() == null) {
            config.setStatus(0);
        }
        notificationConfigMapper.insert(config);
        return config.getId();
    }

    @Override
    public void updateNotificationConfig(DingtalkNotificationConfigSaveReqVO updateReqVO) {
        // 验证存在
        validateNotificationConfigExists(updateReqVO.getId());
        // 验证机器人存在
        validateRobotExists(updateReqVO.getRobotId());

        DingtalkNotificationConfigDO updateObj = BeanUtils.toBean(updateReqVO, DingtalkNotificationConfigDO.class);
        notificationConfigMapper.updateById(updateObj);
    }

    @Override
    public void deleteNotificationConfig(Long id) {
        // 验证存在
        validateNotificationConfigExists(id);
        notificationConfigMapper.deleteById(id);
    }

    private void validateNotificationConfigExists(Long id) {
        if (notificationConfigMapper.selectById(id) == null) {
            throw exception(DINGTALK_NOTIFICATION_CONFIG_NOT_EXISTS);
        }
    }

    private void validateRobotExists(Long robotId) {
        DingtalkRobotDO robot = dingtalkRobotService.getDingtalkRobot(robotId);
        if (robot == null) {
            throw exception(DINGTALK_ROBOT_NOT_EXISTS);
        }
    }

    @Override
    public DingtalkNotificationConfigDO getNotificationConfig(Long id) {
        return notificationConfigMapper.selectById(id);
    }

    @Override
    public PageResult<DingtalkNotificationConfigDO> getNotificationConfigPage(DingtalkNotificationConfigPageReqVO pageReqVO) {
        return notificationConfigMapper.selectPage(pageReqVO);
    }

    @Override
    public List<DingtalkNotificationConfigDO> getEnabledConfigsByEvent(String eventType, String eventModule) {
        return notificationConfigMapper.selectListByEventTypeAndModule(eventType, eventModule);
    }

    // ==================== 通知发送 ====================

    @Async
    @Override
    public void triggerNotification(String eventType, String eventModule,
                                    Long businessId, String businessNo,
                                    Map<String, Object> variables,
                                    Long ownerUserId, Long creatorUserId) {
        // 1. 查找匹配的配置
        List<DingtalkNotificationConfigDO> configs = getEnabledConfigsByEvent(eventType, eventModule);
        if (CollUtil.isEmpty(configs)) {
            log.debug("未找到事件[{}/{}]的通知配置", eventModule, eventType);
            return;
        }

        // 2. 逐个配置发送通知
        for (DingtalkNotificationConfigDO config : configs) {
            try {
                sendNotification(config, businessId, businessNo, variables, ownerUserId, creatorUserId);
            } catch (Exception e) {
                log.error("发送钉钉通知失败，配置ID={}，事件={}/{}", config.getId(), eventModule, eventType, e);
                // 记录失败日志
                saveNotificationLog(config, businessId, businessNo, null, null, null, false, e.getMessage());
            }
        }
    }

    /**
     * 发送单个通知
     */
    private void sendNotification(DingtalkNotificationConfigDO config,
                                  Long businessId, String businessNo,
                                  Map<String, Object> variables,
                                  Long ownerUserId, Long creatorUserId) {
        // 1. 替换模板变量
        String title = replaceVariables(config.getTitleTemplate(), variables);
        String content = replaceVariables(config.getContentTemplate(), variables);

        // 2. 处理@人员
        List<String> atMobiles = resolveAtMobiles(config, ownerUserId, creatorUserId);
        boolean isAtAll = (config.getAtType() != null && config.getAtType() == 4);

        // 3. 发送消息
        RobotSendResult result;
        String msgType = config.getMsgType();
        if ("text".equals(msgType)) {
            result = dingtalkRobotService.sendTextMessage(
                    config.getRobotId(), content, atMobiles, null, isAtAll);
        } else if ("markdown".equals(msgType)) {
            result = dingtalkRobotService.sendMarkdownMessage(
                    config.getRobotId(), title, content, atMobiles, null, isAtAll);
        } else if ("link".equals(msgType)) {
            result = dingtalkRobotService.sendLinkMessage(
                    config.getRobotId(), title, content, null, null);
        } else if ("actionCard".equals(msgType)) {
            result = dingtalkRobotService.sendActionCardMessage(
                    config.getRobotId(), title, content, "查看详情", null);
        } else {
            // 默认使用markdown
            result = dingtalkRobotService.sendMarkdownMessage(
                    config.getRobotId(), title, content, atMobiles, null, isAtAll);
        }

        // 4. 记录日志
        String atMobilesStr = CollUtil.isNotEmpty(atMobiles) ? JSONUtil.toJsonStr(atMobiles) : null;
        saveNotificationLog(config, businessId, businessNo, title, content, atMobilesStr,
                result != null && result.isSuccess(),
                result != null ? result.getErrmsg() : null);

        if (result != null && !result.isSuccess()) {
            log.warn("钉钉通知发送失败，配置ID={}，错误：{}", config.getId(), result.getErrmsg());
        }
    }

    /**
     * 替换模板变量
     */
    private String replaceVariables(String template, Map<String, Object> variables) {
        if (StrUtil.isBlank(template) || variables == null) {
            return template;
        }

        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String variableName = matcher.group(1);
            Object value = variables.get(variableName);
            String replacement = formatValue(value);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * 格式化变量值
     */
    private String formatValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof LocalDateTime) {
            return ((LocalDateTime) value).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        if (value instanceof java.math.BigDecimal) {
            return ((java.math.BigDecimal) value).setScale(2, java.math.RoundingMode.HALF_UP).toString();
        }
        return value.toString();
    }

    /**
     * 解析@的手机号列表
     */
    private List<String> resolveAtMobiles(DingtalkNotificationConfigDO config,
                                          Long ownerUserId, Long creatorUserId) {
        List<String> mobiles = new ArrayList<>();
        Integer atType = config.getAtType();
        if (atType == null || atType == 0 || atType == 4) {
            // 不@任何人 或 @所有人
            return mobiles;
        }

        if (atType == 1 && ownerUserId != null) {
            // @负责人
            String mobile = getUserMobile(ownerUserId);
            if (StrUtil.isNotBlank(mobile)) {
                mobiles.add(mobile);
            }
        } else if (atType == 2 && creatorUserId != null) {
            // @创建人
            String mobile = getUserMobile(creatorUserId);
            if (StrUtil.isNotBlank(mobile)) {
                mobiles.add(mobile);
            }
        } else if (atType == 3) {
            // @指定人员
            if (StrUtil.isNotBlank(config.getAtMobiles())) {
                try {
                    List<String> configMobiles = JSONUtil.toList(config.getAtMobiles(), String.class);
                    mobiles.addAll(configMobiles);
                } catch (Exception e) {
                    log.warn("解析@手机号列表失败：{}", config.getAtMobiles());
                }
            }
        }

        return mobiles;
    }

    /**
     * 获取用户手机号
     */
    private String getUserMobile(Long userId) {
        try {
            AdminUserDO user = adminUserService.getUser(userId);
            return user != null ? user.getMobile() : null;
        } catch (Exception e) {
            log.warn("获取用户手机号失败，userId={}", userId, e);
            return null;
        }
    }

    /**
     * 保存通知日志
     */
    private void saveNotificationLog(DingtalkNotificationConfigDO config,
                                     Long businessId, String businessNo,
                                     String title, String content, String atMobiles,
                                     boolean success, String errorMsg) {
        try {
            DingtalkNotificationLogDO log = DingtalkNotificationLogDO.builder()
                    .configId(config.getId())
                    .robotId(config.getRobotId())
                    .eventType(config.getEventType())
                    .eventModule(config.getEventModule())
                    .businessId(businessId)
                    .businessNo(businessNo)
                    .title(title)
                    .content(content)
                    .atMobiles(atMobiles)
                    .sendStatus(success ? 0 : 1)
                    .errorMsg(errorMsg)
                    .build();
            notificationLogMapper.insert(log);
        } catch (Exception e) {
            DingtalkNotificationConfigServiceImpl.log.error("保存通知日志失败", e);
        }
    }

    // ==================== 日志查询 ====================

    @Override
    public PageResult<DingtalkNotificationLogDO> getNotificationLogPage(DingtalkNotificationLogPageReqVO pageReqVO) {
        return notificationLogMapper.selectPage(pageReqVO);
    }

    // ==================== 元数据 ====================

    @Override
    public List<Map<String, Object>> getSupportedEventTypes() {
        List<Map<String, Object>> list = new ArrayList<>();

        // CRM模块事件
        list.add(createEventType("contract_create", "crm", "合同创建", 
                Arrays.asList("name", "no", "totalPrice", "customerName", "ownerUserName", "createTime")));
        list.add(createEventType("contract_update", "crm", "合同更新",
                Arrays.asList("name", "no", "totalPrice", "customerName", "ownerUserName", "updateTime")));
        list.add(createEventType("contract_audit_pass", "crm", "合同审核通过",
                Arrays.asList("name", "no", "totalPrice", "customerName", "auditorName", "auditTime")));
        list.add(createEventType("contract_audit_reject", "crm", "合同审核拒绝",
                Arrays.asList("name", "no", "customerName", "auditorName", "auditTime", "rejectReason")));
        list.add(createEventType("receivable_create", "crm", "回款创建",
                Arrays.asList("contractName", "customerName", "price", "returnTime", "ownerUserName")));
        list.add(createEventType("receivable_plan_remind", "crm", "回款计划提醒",
                Arrays.asList("contractName", "customerName", "price", "returnTime", "period")));
        list.add(createEventType("customer_create", "crm", "客户创建",
                Arrays.asList("name", "mobile", "industry", "ownerUserName", "createTime")));
        list.add(createEventType("business_create", "crm", "商机创建",
                Arrays.asList("name", "customerName", "price", "ownerUserName", "createTime")));

        // 项目模块事件
        list.add(createEventType("project_create", "project", "项目创建",
                Arrays.asList("name", "customerName", "managerName", "createTime")));
        list.add(createEventType("project_status_change", "project", "项目状态变更",
                Arrays.asList("name", "customerName", "oldStatus", "newStatus", "operatorName")));
        list.add(createEventType("service_launch_create", "project", "服务启动创建",
                Arrays.asList("projectName", "serviceName", "managerName", "createTime")));
        list.add(createEventType("round_deadline_remind", "project", "执行计划截止提醒",
                Arrays.asList("serviceItemName", "roundName", "deadline", "remainingDays", "customerName", "serviceType")));

        // 系统模块事件
        list.add(createEventType("user_login", "system", "用户登录",
                Arrays.asList("nickname", "username", "loginTime", "loginIp")));

        return list;
    }

    @Override
    public List<Map<String, String>> getSupportedEventModules() {
        List<Map<String, String>> list = new ArrayList<>();
        list.add(createModule("crm", "CRM客户关系"));
        list.add(createModule("project", "项目管理"));
        list.add(createModule("system", "系统管理"));
        list.add(createModule("bpm", "工作流"));
        return list;
    }

    private Map<String, Object> createEventType(String value, String module, String label, List<String> variables) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("value", value);
        map.put("module", module);
        map.put("label", label);
        map.put("variables", variables);
        return map;
    }

    private Map<String, String> createModule(String value, String label) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("value", value);
        map.put("label", label);
        return map;
    }

}
