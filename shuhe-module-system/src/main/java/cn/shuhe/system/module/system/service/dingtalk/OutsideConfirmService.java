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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

        // 7. 根据时长自动判断外出类型
        String outsideType = determineOutsideType(requestInfo.getPlanStartTime(), requestInfo.getPlanEndTime());

        // 8. 计算时长数值（根据外出类型决定单位：短期用小时，长期用天，支持小数）
        double durationValue = calculateDurationValue(requestInfo.getPlanStartTime(), requestInfo.getPlanEndTime(), outsideType);

        // 9. 格式化时间（根据外出类型选择格式）
        // 短期外出（hour单位）使用 yyyy-MM-dd HH:mm，长期外出（day单位）使用 yyyy-MM-dd
        String startTimeStr = formatDateTime(requestInfo.getPlanStartTime(), outsideType);
        String endTimeStr = formatDateTime(requestInfo.getPlanEndTime(), outsideType);
        String timeFormat = "1天内短期外出".equals(outsideType) ? "yyyy-MM-dd HH:mm" : "yyyy-MM-dd";
        log.info("【外出确认】准备发起OA审批(DDBizSuite套件模式)，userId={}, processCode={}, outsideType={}, 时间格式={}, startTime={}, endTime={}, duration={}, projectName={}, reason={}, destination={}",
                memberInfo.getUserId(), outsideProcessCode, outsideType, timeFormat, startTimeStr, endTimeStr, durationValue,
                requestInfo.getProjectName(), requestInfo.getReason(), requestInfo.getDestination());

        // 10. 发起钉钉OA外出申请（使用DDBizSuite套件模式）
        String oaProcessInstanceId = dingtalkNotifyApi.startOutsideSuiteOaApproval(
                memberInfo.getUserId(),
                outsideProcessCode,
                outsideType,
                startTimeStr,
                endTimeStr,
                durationValue,
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

    // ==================== 工作时间配置（根据钉钉考勤配置）====================
    /** 工作开始时间 */
    private static final LocalTime WORK_START = LocalTime.of(8, 30);
    /** 工作结束时间 */
    private static final LocalTime WORK_END = LocalTime.of(17, 30);
    /** 午休开始时间 */
    private static final LocalTime LUNCH_START = LocalTime.of(12, 0);
    /** 午休结束时间 */
    private static final LocalTime LUNCH_END = LocalTime.of(13, 0);
    /** 每天有效工作小时数 (08:30-12:00=3.5h + 13:00-17:30=4.5h = 8h) */
    private static final double DAILY_WORK_HOURS = 8.0;
    /** 午休时长（分钟）*/
    private static final long LUNCH_DURATION_MINUTES = 60;

    /**
     * 计算时长数值（根据外出类型决定单位）- 严格按照钉钉逻辑
     * - "1天内短期外出"：返回工作小时数（精确到小数）
     * - "超过1天连续外出"：返回工作天数
     * 
     * 钉钉计算规则：
     * - 工作时间：08:30 - 17:30
     * - 午休时间：12:00 - 13:00（从时间差中扣除）
     * - 计算方式：时间差 - 午休重叠时间
     */
    private double calculateDurationValue(LocalDateTime startTime, LocalDateTime endTime, String outsideType) {
        if (startTime == null || endTime == null) {
            return "超过1天连续外出".equals(outsideType) ? 1.0 : 8.0;
        }
        
        double workHours = calculateWorkingHours(startTime, endTime);
        
        if ("超过1天连续外出".equals(outsideType)) {
            // 超过1天连续外出：返回工作天数（向上取整）
            double days = Math.ceil(workHours / DAILY_WORK_HOURS);
            return days <= 0 ? 1.0 : days;
        } else {
            // 1天内短期外出：返回工作小时数（保留2位小数）
            return workHours <= 0 ? 1.0 : Math.round(workHours * 100.0) / 100.0;
        }
    }

    /**
     * 计算两个时间点之间的有效工作小时数（严格按照钉钉逻辑）
     * 
     * 钉钉计算规则：
     * - 计算时间差（分钟）
     * - 如果时间段跨越午休（12:00-13:00），扣除午休重叠部分
     * - 跨天时分别计算每天的有效工作时间
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 有效工作小时数（精确到小数）
     */
    private double calculateWorkingHours(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null || !startTime.isBefore(endTime)) {
            return 0.0;
        }

        double totalMinutes = 0.0;
        LocalDate currentDate = startTime.toLocalDate();
        LocalDate endDate = endTime.toLocalDate();

        while (!currentDate.isAfter(endDate)) {
            LocalTime dayStartTime;
            LocalTime dayEndTime;

            if (currentDate.equals(startTime.toLocalDate())) {
                // 第一天：从开始时间算起
                dayStartTime = startTime.toLocalTime();
            } else {
                // 中间天或最后一天：从当天工作开始时间算起
                dayStartTime = WORK_START;
            }

            if (currentDate.equals(endDate)) {
                // 最后一天：算到结束时间
                dayEndTime = endTime.toLocalTime();
            } else {
                // 第一天或中间天：算到当天工作结束时间
                dayEndTime = WORK_END;
            }

            // 计算当天的有效工作分钟数
            totalMinutes += calculateDayWorkingMinutes(dayStartTime, dayEndTime);
            
            currentDate = currentDate.plusDays(1);
        }

        return totalMinutes / 60.0;
    }

    /**
     * 计算单天内的有效工作分钟数（严格按照钉钉逻辑）
     * 
     * 钉钉计算规则：时间差 - 午休重叠时间
     * 
     * @param startTime 当天开始时间
     * @param endTime 当天结束时间
     * @return 有效工作分钟数
     */
    private double calculateDayWorkingMinutes(LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null || !startTime.isBefore(endTime)) {
            return 0.0;
        }

        // 将时间限制在工作时间范围内
        if (startTime.isBefore(WORK_START)) {
            startTime = WORK_START;
        }
        if (endTime.isAfter(WORK_END)) {
            endTime = WORK_END;
        }

        // 如果调整后开始时间不早于结束时间，返回0
        if (!startTime.isBefore(endTime)) {
            return 0.0;
        }

        // 计算总时间差（分钟）
        long totalMinutes = Duration.between(startTime, endTime).toMinutes();

        // 计算午休重叠时间并扣除
        long lunchOverlapMinutes = calculateLunchOverlap(startTime, endTime);
        
        return totalMinutes - lunchOverlapMinutes;
    }

    /**
     * 计算时间段与午休时间的重叠分钟数
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 与午休重叠的分钟数
     */
    private long calculateLunchOverlap(LocalTime startTime, LocalTime endTime) {
        // 如果时间段不与午休重叠，返回0
        if (endTime.isBefore(LUNCH_START) || endTime.equals(LUNCH_START) ||
            startTime.isAfter(LUNCH_END) || startTime.equals(LUNCH_END)) {
            return 0;
        }

        // 计算重叠部分
        LocalTime overlapStart = startTime.isBefore(LUNCH_START) ? LUNCH_START : startTime;
        LocalTime overlapEnd = endTime.isAfter(LUNCH_END) ? LUNCH_END : endTime;

        if (overlapStart.isBefore(overlapEnd)) {
            return Duration.between(overlapStart, overlapEnd).toMinutes();
        }
        return 0;
    }

    /**
     * 计算时长（根据外出类型决定单位）- 返回字符串
     * - "1天内短期外出"：返回小时数
     * - "超过1天连续外出"：返回天数
     */
    private String calculateDuration(LocalDateTime startTime, LocalDateTime endTime, String outsideType) {
        double value = calculateDurationValue(startTime, endTime, outsideType);
        // 如果是整数，不显示小数点
        if (value == Math.floor(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    /**
     * 计算工作小时数（精确值，用于显示）
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 工作小时数（保留2位小数）
     */
    public double calculateWorkingHoursExact(LocalDateTime startTime, LocalDateTime endTime) {
        double hours = calculateWorkingHours(startTime, endTime);
        return Math.round(hours * 100.0) / 100.0; // 保留2位小数
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
