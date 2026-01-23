package cn.shuhe.system.module.system.service.dingtalk;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.shuhe.system.module.system.api.dingtalk.DingtalkNotifyApi;
import cn.shuhe.system.module.system.dal.dataobject.dingtalkconfig.DingtalkConfigDO;
import cn.shuhe.system.module.system.service.dingtalkconfig.DingtalkConfigService;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 外出确认服务
 * 
 * 处理员工点击钉钉消息确认外出的逻辑
 */
@Service
@Slf4j
public class OutsideConfirmService {

    /**
     * Token 加密盐值
     */
    private static final String TOKEN_SALT = "shuhe-outside-confirm-2024";

    @Resource
    private DingtalkNotifyApi dingtalkNotifyApi;

    @Resource
    private DingtalkConfigService dingtalkConfigService;

    @Resource
    private OutsideConfirmCallback outsideConfirmCallback;

    /**
     * 确认结果
     */
    @Data
    @AllArgsConstructor
    public static class ConfirmResult {
        private boolean success;
        private String message;
        private String oaProcessInstanceId;
    }

    /**
     * 获取钉钉配置
     */
    private DingtalkConfigDO getConfig() {
        List<DingtalkConfigDO> configs = dingtalkConfigService.getEnabledDingtalkConfigList();
        if (CollUtil.isEmpty(configs)) {
            log.warn("【外出确认】没有找到启用的钉钉配置");
            return null;
        }
        DingtalkConfigDO config = configs.get(0);
        log.info("【外出确认】读取钉钉配置，configId={}, name={}, outsideProcessCode={}, callbackBaseUrl={}", 
                config.getId(), config.getName(), config.getOutsideProcessCode(), config.getCallbackBaseUrl());
        if (configs.size() > 1) {
            log.warn("【外出确认】存在多个启用的钉钉配置（共{}个），当前使用第一个：configId={}", configs.size(), config.getId());
        }
        return config;
    }

    /**
     * 生成确认链接的Token
     */
    public String generateConfirmToken(Long memberId, Long userId) {
        String raw = memberId + "-" + userId + "-" + TOKEN_SALT;
        return SecureUtil.md5(raw);
    }

    /**
     * 验证确认Token
     */
    public boolean verifyConfirmToken(Long memberId, Long userId, String token) {
        String expectedToken = generateConfirmToken(memberId, userId);
        return expectedToken.equals(token);
    }

    /**
     * 生成确认链接URL
     */
    public String generateConfirmUrl(Long memberId, Long userId) {
        String token = generateConfirmToken(memberId, userId);
        
        // 从数据库配置读取回调URL
        DingtalkConfigDO config = getConfig();
        String baseUrl = "http://localhost:48080"; // 默认本地地址
        if (config != null && StrUtil.isNotEmpty(config.getCallbackBaseUrl())) {
            baseUrl = config.getCallbackBaseUrl();
        }
        
        // 确保baseUrl不以斜杠结尾
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        
        // 添加 /admin-api 前缀（应用的API路径前缀）
        return baseUrl + "/admin-api/system/dingtalk/callback/outside-confirm?memberId=" + memberId + "&token=" + token;
    }

    /**
     * 处理外出确认
     */
    public ConfirmResult confirmOutside(Long memberId, String token) {
        log.info("【外出确认】开始处理，memberId={}", memberId);

        // 1. 获取外出人员信息
        OutsideConfirmCallback.OutsideMemberInfo memberInfo = outsideConfirmCallback.getOutsideMemberInfo(memberId);
        if (memberInfo == null) {
            return new ConfirmResult(false, "未找到外出记录", null);
        }

        // 2. 验证 token
        if (!verifyConfirmToken(memberId, memberInfo.getUserId(), token)) {
            log.warn("【外出确认】Token验证失败，memberId={}", memberId);
            return new ConfirmResult(false, "验证失败，请重新点击确认链接", null);
        }

        // 3. 检查是否已确认
        if (memberInfo.getConfirmStatus() != null && memberInfo.getConfirmStatus() >= 1) {
            return new ConfirmResult(true, "您已确认过此次外出任务", memberInfo.getOaProcessInstanceId());
        }

        // 4. 从数据库读取配置
        DingtalkConfigDO config = getConfig();
        if (config == null) {
            log.warn("【外出确认】未找到启用的钉钉配置");
            outsideConfirmCallback.updateConfirmStatus(memberId, 1, null);
            return new ConfirmResult(true, "确认成功！钉钉配置未启用，请手动提交。", null);
        }
        
        String outsideProcessCode = config.getOutsideProcessCode();
        log.info("【外出确认】读取到的配置信息：configId={}, outsideProcessCode={}, callbackBaseUrl={}", 
                config.getId(), outsideProcessCode, config.getCallbackBaseUrl());

        // 5. 检查 OA 流程编码是否配置
        if (StrUtil.isEmpty(outsideProcessCode)) {
            log.warn("【外出确认】OA流程编码未配置，configId={}", config.getId());
            outsideConfirmCallback.updateConfirmStatus(memberId, 1, null);
            return new ConfirmResult(true, "确认成功！OA外出申请流程未配置，请手动提交。", null);
        }

        // 6. 获取外出请求信息
        OutsideConfirmCallback.OutsideRequestInfo requestInfo = outsideConfirmCallback.getOutsideRequestInfo(memberInfo.getRequestId());
        if (requestInfo == null) {
            log.warn("【外出确认】未找到外出请求信息，requestId={}", memberInfo.getRequestId());
            return new ConfirmResult(false, "未找到外出请求信息", null);
        }

        // 7. 计算时长
        String duration = calculateDuration(requestInfo.getPlanStartTime(), requestInfo.getPlanEndTime());

        // 8. 根据时长自动判断外出类型
        String outsideType = determineOutsideType(requestInfo.getPlanStartTime(), requestInfo.getPlanEndTime());

        // 9. 格式化时间（根据外出类型选择格式）
        // 短期外出（hour单位）使用 yyyy-MM-dd HH:mm，长期外出（day单位）使用 yyyy-MM-dd
        String startTimeStr = formatDateTime(requestInfo.getPlanStartTime(), outsideType);
        String endTimeStr = formatDateTime(requestInfo.getPlanEndTime(), outsideType);
        String timeFormat = "1天内短期外出".equals(outsideType) ? "yyyy-MM-dd HH:mm" : "yyyy-MM-dd";
        log.info("【外出确认】准备发起OA审批，userId={}, processCode={}, outsideType={}, 时间格式={}, startTime={}, endTime={}, duration={}, projectName={}, reason={}, destination={}",
                memberInfo.getUserId(), outsideProcessCode, outsideType, timeFormat, startTimeStr, endTimeStr, duration,
                requestInfo.getProjectName(), requestInfo.getReason(), requestInfo.getDestination());

        // 10. 发起钉钉OA外出申请
        String oaProcessInstanceId = dingtalkNotifyApi.startOutsideOaApproval(
                memberInfo.getUserId(),
                outsideProcessCode,
                outsideType,
                startTimeStr,
                endTimeStr,
                duration,
                requestInfo.getProjectName(),
                requestInfo.getReason(),
                requestInfo.getDestination()
        );

        if (StrUtil.isNotEmpty(oaProcessInstanceId)) {
            outsideConfirmCallback.updateConfirmStatus(memberId, 2, oaProcessInstanceId);
            log.info("【外出确认】OA外出申请发起成功，memberId={}, oaProcessInstanceId={}", memberId, oaProcessInstanceId);
            return new ConfirmResult(true, "确认成功！已自动为您提交外出申请，请在钉钉中查看审批进度。", oaProcessInstanceId);
        } else {
            outsideConfirmCallback.updateConfirmStatus(memberId, 1, null);
            log.warn("【外出确认】OA外出申请发起失败，memberId={}", memberId);
            return new ConfirmResult(true, "确认成功！但自动提交外出申请失败，请手动在钉钉中提交。", null);
        }
    }

    private String calculateDuration(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            return "8";
        }
        Duration duration = Duration.between(startTime, endTime);
        long hours = duration.toHours();
        if (hours <= 0) {
            hours = 8;
        }
        return String.valueOf(hours);
    }

    /**
     * 根据外出时长自动判断外出类型
     * 超过24小时（即超过1天）用"超过1天连续外出"，否则用"1天内短期外出"
     */
    private String determineOutsideType(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            return "1天内短期外出"; // 默认短期外出
        }
        Duration duration = Duration.between(startTime, endTime);
        long hours = duration.toHours();
        if (hours > 24) {
            return "超过1天连续外出";
        } else {
            return "1天内短期外出";
        }
    }

    /**
     * 格式化时间为钉钉OA审批所需格式
     * 
     * 钉钉OA审批API文档：https://open.dingtalk.com/document/orgapp-server/initiate-an-approval-instance
     * Apifox文档：https://dingtalk.apifox.cn/api-9093218
     * 
     * 根据表单Schema分析：
     * - DDDateField组件根据外出类型使用不同格式：
     *   - "1天内短期外出"（unit: "hour"）→ yyyy-MM-dd HH:mm（日期+时间）
     *   - "超过1天连续外出"（unit: "day"）→ yyyy-MM-dd（仅日期）
     * 
     * @param dateTime 日期时间
     * @param outsideType 外出类型（"1天内短期外出" 或 "超过1天连续外出"）
     * @return 格式化后的时间字符串
     */
    private String formatDateTime(LocalDateTime dateTime, String outsideType) {
        if (dateTime == null) {
            return "";
        }
        
        // 根据外出类型选择格式
        if ("1天内短期外出".equals(outsideType)) {
            // 短期外出使用日期+时间格式（hour单位）
            return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        } else {
            // 长期外出使用仅日期格式（day单位）
            return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }
    }

}
