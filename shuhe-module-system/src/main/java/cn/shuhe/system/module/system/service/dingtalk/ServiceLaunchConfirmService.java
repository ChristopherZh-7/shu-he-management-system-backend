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
 * 服务发起确认服务
 * 
 * 处理员工点击钉钉消息确认外出的逻辑（统一服务发起版本）
 */
@Service
@Slf4j
public class ServiceLaunchConfirmService {

    /**
     * Token 加密盐值
     */
    private static final String TOKEN_SALT = "shuhe-service-launch-confirm-2026";

    @Resource
    private DingtalkNotifyApi dingtalkNotifyApi;

    @Resource
    private DingtalkConfigService dingtalkConfigService;

    @Resource
    private ServiceLaunchConfirmCallback serviceLaunchConfirmCallback;

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
            log.warn("【服务发起确认】没有找到启用的钉钉配置");
            return null;
        }
        DingtalkConfigDO config = configs.get(0);
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
        
        // 确保 baseUrl 不以 / 结尾
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        
        // 使用统一的服务发起确认接口
        String confirmUrl = baseUrl + "/admin-api/system/dingtalk/callback/service-launch-confirm?memberId=" 
                + memberId + "&token=" + token;
        log.info("【服务发起确认】生成确认链接: {}", confirmUrl);
        return confirmUrl;
    }

    /**
     * 确认外出
     * 
     * @param memberId 执行人记录ID
     * @param token 验证Token
     * @return 确认结果
     */
    public ConfirmResult confirmOutside(Long memberId, String token) {
        log.info("【服务发起确认】收到确认请求，memberId={}", memberId);

        // 1. 通过回调接口获取执行人信息并验证
        ServiceLaunchConfirmCallback.MemberInfo memberInfo = serviceLaunchConfirmCallback.getMemberInfo(memberId);
        if (memberInfo == null) {
            log.warn("【服务发起确认】未找到执行人记录，memberId={}", memberId);
            return new ConfirmResult(false, "未找到执行人记录", null);
        }

        // 2. 验证Token
        if (!verifyConfirmToken(memberId, memberInfo.getUserId(), token)) {
            log.warn("【服务发起确认】Token验证失败，memberId={}", memberId);
            return new ConfirmResult(false, "验证失败，请重新点击确认链接", null);
        }

        // 3. 检查是否已确认
        if (memberInfo.getConfirmStatus() != null && memberInfo.getConfirmStatus() > 0) {
            log.info("【服务发起确认】已确认，无需重复操作，memberId={}, status={}", 
                    memberId, memberInfo.getConfirmStatus());
            return new ConfirmResult(true, "您已确认过，无需重复操作", memberInfo.getOaProcessInstanceId());
        }

        // 4. 获取钉钉配置
        DingtalkConfigDO config = getConfig();
        if (config == null) {
            return new ConfirmResult(false, "系统配置错误", null);
        }

        // 5. 获取服务发起信息
        ServiceLaunchConfirmCallback.LaunchInfo launchInfo = serviceLaunchConfirmCallback.getLaunchInfo(memberInfo.getLaunchId());
        if (launchInfo == null) {
            return new ConfirmResult(false, "未找到服务发起记录", null);
        }

        // 6. 提交钉钉OA外出申请
        String oaProcessInstanceId = submitDingtalkOutsideApproval(memberInfo, launchInfo, config);

        // 7. 更新确认状态
        if (oaProcessInstanceId != null) {
            serviceLaunchConfirmCallback.updateConfirmStatus(memberId, 2, oaProcessInstanceId); // 2=已提交OA
            log.info("【服务发起确认】确认成功，已提交OA。memberId={}, oaProcessInstanceId={}", 
                    memberId, oaProcessInstanceId);
            return new ConfirmResult(true, "确认成功，已自动提交钉钉外出申请", oaProcessInstanceId);
        } else {
            // OA提交失败，但仍标记为已确认
            serviceLaunchConfirmCallback.updateConfirmStatus(memberId, 1, null); // 1=已确认
            log.warn("【服务发起确认】OA提交失败，仅更新确认状态。memberId={}", memberId);
            return new ConfirmResult(true, "确认成功，但钉钉外出申请提交失败，请手动申请", null);
        }
    }

    /**
     * 提交钉钉OA外出申请
     */
    private String submitDingtalkOutsideApproval(ServiceLaunchConfirmCallback.MemberInfo memberInfo,
                                                  ServiceLaunchConfirmCallback.LaunchInfo launchInfo,
                                                  DingtalkConfigDO config) {
        try {
            String processCode = config.getOutsideProcessCode();
            if (StrUtil.isEmpty(processCode)) {
                log.warn("【服务发起确认】未配置钉钉外出审批流程code");
                return null;
            }

            // 计算外出时长
            LocalDateTime startTime = launchInfo.getPlanStartTime();
            LocalDateTime endTime = launchInfo.getPlanEndTime();
            if (startTime == null || endTime == null) {
                log.warn("【服务发起确认】外出时间为空，launchId={}", launchInfo.getId());
                return null;
            }

            // 调整为当天时间（如果计划日期已过）
            LocalDate today = LocalDate.now();
            if (startTime.toLocalDate().isBefore(today)) {
                startTime = LocalDateTime.of(today, LocalTime.of(8, 30));
                endTime = LocalDateTime.of(today, LocalTime.of(17, 30));
            }

            // 根据时长自动判断外出类型（和外出请求逻辑一致）
            String outsideType = determineOutsideType(startTime, endTime);
            
            // 计算时长数值（根据外出类型决定单位：短期用小时，长期用天）
            double durationValue = calculateDurationValue(startTime, endTime, outsideType);

            // 格式化时间（根据外出类型选择格式）
            String startTimeStr = formatDateTime(startTime, outsideType);
            String endTimeStr = formatDateTime(endTime, outsideType);

            // 构建项目名称：客户名称-服务项名称
            String projectName = buildProjectName(launchInfo);
            
            // 构建外出原因
            String reason = launchInfo.getReason() != null ? launchInfo.getReason() : "工作需要";
            String destination = launchInfo.getDestination() != null ? launchInfo.getDestination() : "客户现场";

            log.info("【服务发起确认】准备发起OA审批，userId={}, outsideType={}, startTime={}, endTime={}, duration={}, projectName={}, reason={}, destination={}",
                    memberInfo.getUserId(), outsideType, startTimeStr, endTimeStr, durationValue, projectName, reason, destination);

            // 调用钉钉API提交外出申请（使用套件模式）
            String processInstanceId = dingtalkNotifyApi.startOutsideSuiteOaApproval(
                    memberInfo.getUserId(),
                    processCode,
                    outsideType,
                    startTimeStr,
                    endTimeStr,
                    durationValue,
                    projectName,
                    reason,
                    destination
            );

            log.info("【服务发起确认】提交钉钉外出申请成功，processInstanceId={}", processInstanceId);
            return processInstanceId;

        } catch (Exception e) {
            log.error("【服务发起确认】提交钉钉外出申请异常: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 构建项目名称：客户名称-服务类型中文名称
     * 
     * 例如：xx市xxxxx-渗透测试
     */
    private String buildProjectName(ServiceLaunchConfirmCallback.LaunchInfo launchInfo) {
        StringBuilder sb = new StringBuilder();
        // 添加客户名称
        if (StrUtil.isNotEmpty(launchInfo.getCustomerName())) {
            sb.append(launchInfo.getCustomerName());
        }
        // 添加服务类型中文名称（优先使用中文名称，其次使用服务项名称）
        String serviceTypeName = launchInfo.getServiceTypeName();
        if (StrUtil.isEmpty(serviceTypeName)) {
            serviceTypeName = launchInfo.getServiceItemName();
        }
        if (StrUtil.isNotEmpty(serviceTypeName)) {
            if (sb.length() > 0) {
                sb.append("-");
            }
            sb.append(serviceTypeName);
        }
        return sb.length() > 0 ? sb.toString() : "服务任务";
    }

    // ==================== 工作时间配置（和 OutsideConfirmService 保持一致）====================
    /** 工作开始时间 */
    private static final LocalTime WORK_START = LocalTime.of(8, 30);
    /** 工作结束时间 */
    private static final LocalTime WORK_END = LocalTime.of(17, 30);
    /** 午休开始时间 */
    private static final LocalTime LUNCH_START = LocalTime.of(12, 0);
    /** 午休结束时间 */
    private static final LocalTime LUNCH_END = LocalTime.of(13, 0);
    /** 每天有效工作小时数 */
    private static final double DAILY_WORK_HOURS = 8.0;
    /** 午休时长（分钟）*/
    private static final long LUNCH_DURATION_MINUTES = 60;

    /**
     * 根据时长判断外出类型
     * - 如果开始和结束在同一天，且时长小于等于8小时，则为"1天内短期外出"
     * - 否则为"超过1天连续外出"
     */
    private String determineOutsideType(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            return "1天内短期外出";
        }
        // 如果跨天，则为长期外出
        if (!startTime.toLocalDate().equals(endTime.toLocalDate())) {
            return "超过1天连续外出";
        }
        // 同一天内的外出为短期
        return "1天内短期外出";
    }

    /**
     * 计算时长数值（根据外出类型决定单位）
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
     * 计算两个时间点之间的有效工作小时数
     */
    private double calculateWorkingHours(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null || !startTime.isBefore(endTime)) {
            return 0.0;
        }

        double totalMinutes = 0.0;

        // 如果是同一天
        if (startTime.toLocalDate().equals(endTime.toLocalDate())) {
            totalMinutes = calculateSingleDayMinutes(startTime.toLocalTime(), endTime.toLocalTime());
        } else {
            // 跨天计算
            // 第一天：从开始时间到工作结束
            totalMinutes += calculateSingleDayMinutes(startTime.toLocalTime(), WORK_END);
            
            // 中间的完整工作日
            LocalDate currentDate = startTime.toLocalDate().plusDays(1);
            while (currentDate.isBefore(endTime.toLocalDate())) {
                totalMinutes += DAILY_WORK_HOURS * 60; // 8小时
                currentDate = currentDate.plusDays(1);
            }
            
            // 最后一天：从工作开始到结束时间
            totalMinutes += calculateSingleDayMinutes(WORK_START, endTime.toLocalTime());
        }

        return totalMinutes / 60.0;
    }

    /**
     * 计算单天内的有效工作分钟数（扣除午休）
     */
    private double calculateSingleDayMinutes(LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null || !startTime.isBefore(endTime)) {
            return 0.0;
        }

        // 限制在工作时间范围内
        LocalTime effectiveStart = startTime.isBefore(WORK_START) ? WORK_START : startTime;
        LocalTime effectiveEnd = endTime.isAfter(WORK_END) ? WORK_END : endTime;

        if (!effectiveStart.isBefore(effectiveEnd)) {
            return 0.0;
        }

        // 计算总分钟数
        long totalMinutes = java.time.Duration.between(effectiveStart, effectiveEnd).toMinutes();

        // 扣除午休重叠时间
        LocalTime lunchOverlapStart = effectiveStart.isAfter(LUNCH_START) ? effectiveStart : LUNCH_START;
        LocalTime lunchOverlapEnd = effectiveEnd.isBefore(LUNCH_END) ? effectiveEnd : LUNCH_END;
        
        if (lunchOverlapStart.isBefore(lunchOverlapEnd)) {
            long lunchOverlap = java.time.Duration.between(lunchOverlapStart, lunchOverlapEnd).toMinutes();
            totalMinutes -= lunchOverlap;
        }

        return Math.max(0, totalMinutes);
    }

    /**
     * 格式化时间（根据外出类型选择格式）
     */
    private String formatDateTime(LocalDateTime dateTime, String outsideType) {
        if (dateTime == null) {
            return "";
        }
        if ("超过1天连续外出".equals(outsideType)) {
            return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } else {
            return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        }
    }

}
