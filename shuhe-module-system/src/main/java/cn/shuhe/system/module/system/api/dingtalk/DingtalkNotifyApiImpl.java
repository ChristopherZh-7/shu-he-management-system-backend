package cn.shuhe.system.module.system.api.dingtalk;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.shuhe.system.module.system.api.dingtalk.dto.DingtalkNotifySendReqDTO;
import cn.shuhe.system.module.system.api.user.AdminUserApi;
import cn.shuhe.system.module.system.api.user.dto.AdminUserRespDTO;
import cn.shuhe.system.module.system.dal.dataobject.dept.DeptDO;
import cn.shuhe.system.module.system.dal.dataobject.dingtalkconfig.DingtalkConfigDO;
import cn.shuhe.system.module.system.dal.dataobject.dingtalkmapping.DingtalkMappingDO;
import cn.shuhe.system.module.system.dal.mysql.dingtalkmapping.DingtalkMappingMapper;
import cn.shuhe.system.module.system.service.dept.DeptService;
import cn.shuhe.system.module.system.service.dingtalkconfig.DingtalkApiService;
import org.springframework.beans.factory.annotation.Value;
import cn.shuhe.system.module.system.service.dingtalkconfig.DingtalkConfigService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 钉钉通知 API 实现类
 */
@Service
@Validated
@Slf4j
public class DingtalkNotifyApiImpl implements DingtalkNotifyApi {

    @Resource
    private DingtalkConfigService dingtalkConfigService;

    @Resource
    private DingtalkApiService dingtalkApiService;

    /** 本地/环境配置：审批链接 baseUrl，钉钉配置为空时使用。如 http://localhost:5666 */
    @Value("${shuhe.dingtalk.business-audit.approve-base-url:}")
    private String approveBaseUrlFromConfig;

    @Resource
    private DingtalkMappingMapper dingtalkMappingMapper;

    @Resource
    private DeptService deptService;

    @Resource
    private AdminUserApi adminUserApi;

    @Override
    public boolean sendWorkNotice(DingtalkNotifySendReqDTO reqDTO) {
        if (CollUtil.isEmpty(reqDTO.getUserIds())) {
            log.warn("发送钉钉通知失败：接收人列表为空");
            return false;
        }

        // 1. 获取启用的钉钉配置
        List<DingtalkConfigDO> configs = dingtalkConfigService.getEnabledDingtalkConfigList();
        if (CollUtil.isEmpty(configs)) {
            log.warn("发送钉钉通知失败：没有启用的钉钉配置");
            return false;
        }
        DingtalkConfigDO config = configs.get(0); // 使用第一个启用的配置

        // 2. 获取钉钉用户ID列表
        List<String> dingtalkUserIds = new ArrayList<>();
        for (Long userId : reqDTO.getUserIds()) {
            String dingtalkUserId = getDingtalkUserIdByLocalUserId(userId);
            if (dingtalkUserId != null) {
                dingtalkUserIds.add(dingtalkUserId);
            } else {
                log.warn("发送钉钉通知：用户 {} 没有对应的钉钉用户ID", userId);
            }
        }

        if (CollUtil.isEmpty(dingtalkUserIds)) {
            log.warn("发送钉钉通知失败：没有找到对应的钉钉用户ID");
            return false;
        }

        // 3. 获取 accessToken 并发送消息
        try {
            String accessToken = dingtalkApiService.getAccessToken(config);
            return dingtalkApiService.sendWorkNotice(
                    accessToken,
                    config.getAgentId(),
                    dingtalkUserIds,
                    reqDTO.getTitle(),
                    reqDTO.getContent()
            );
        } catch (Exception e) {
            log.error("发送钉钉通知异常", e);
            return false;
        }
    }

    @Override
    public boolean sendWorkNoticeToDeptTypeLeaders(Integer deptType, String title, String content) {
        // 1. 获取部门类型对应的负责人用户ID列表
        List<Long> leaderUserIds = getLeaderUserIdsByDeptType(deptType);
        if (CollUtil.isEmpty(leaderUserIds)) {
            log.warn("发送钉钉通知失败：部门类型 {} 没有找到负责人", deptType);
            return false;
        }

        // 2. 发送通知
        DingtalkNotifySendReqDTO reqDTO = new DingtalkNotifySendReqDTO()
                .setUserIds(leaderUserIds)
                .setTitle(title)
                .setContent(content);
        return sendWorkNotice(reqDTO);
    }

    @Override
    public boolean sendPrivateMessage(DingtalkNotifySendReqDTO reqDTO) {
        if (CollUtil.isEmpty(reqDTO.getUserIds())) {
            log.warn("单聊机器人发送失败：接收人列表为空");
            return false;
        }

        // 1. 获取启用的钉钉配置
        List<DingtalkConfigDO> configs = dingtalkConfigService.getEnabledDingtalkConfigList();
        if (CollUtil.isEmpty(configs)) {
            log.warn("单聊机器人发送失败：没有启用的钉钉配置");
            return false;
        }
        DingtalkConfigDO config = configs.get(0);

        // 2. 获取钉钉用户ID列表
        List<String> dingtalkUserIds = new ArrayList<>();
        for (Long userId : reqDTO.getUserIds()) {
            String dingtalkUserId = getDingtalkUserIdByLocalUserId(userId);
            if (dingtalkUserId != null) {
                dingtalkUserIds.add(dingtalkUserId);
            } else {
                log.warn("单聊机器人发送：用户 {} 没有对应的钉钉用户ID", userId);
            }
        }

        if (CollUtil.isEmpty(dingtalkUserIds)) {
            log.warn("单聊机器人发送失败：没有找到对应的钉钉用户ID");
            return false;
        }

        // 3. 获取 accessToken 并发送消息（robotCode 使用应用的 clientId）
        try {
            String accessToken = dingtalkApiService.getAccessToken(config);
            DingtalkApiService.RobotPrivateSendResult result = dingtalkApiService.sendRobotPrivateMarkdown(
                    accessToken,
                    config.getClientId(),
                    dingtalkUserIds,
                    reqDTO.getTitle(),
                    reqDTO.getContent()
            );
            return result.isSuccess();
        } catch (Exception e) {
            log.error("单聊机器人发送异常", e);
            return false;
        }
    }

    @Override
    public String getDingtalkUserIdByLocalUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        DingtalkMappingDO mapping = dingtalkMappingMapper.selectByLocalId(userId, "USER");
        return mapping != null ? mapping.getDingtalkId() : null;
    }

    @Override
    public List<Long> getLeaderUserIdsByDeptType(Integer deptType) {
        if (deptType == null) {
            return new ArrayList<>();
        }
        
        // 获取该部门类型的所有部门
        List<DeptDO> depts = deptService.getDeptListByDeptType(deptType);
        if (CollUtil.isEmpty(depts)) {
            log.warn("部门类型 {} 没有找到对应的部门", deptType);
            return new ArrayList<>();
        }

        // 获取这些部门的负责人用户ID（去重）
        return depts.stream()
                .map(DeptDO::getLeaderUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public String createGroupChat(String chatName, Long ownerUserId, List<Long> memberUserIds) {
        // 1. 获取启用的钉钉配置
        List<DingtalkConfigDO> configs = dingtalkConfigService.getEnabledDingtalkConfigList();
        if (CollUtil.isEmpty(configs)) {
            log.warn("创建钉钉群聊失败：没有启用的钉钉配置");
            return null;
        }
        DingtalkConfigDO config = configs.get(0);

        // 2. 转换群主钉钉用户ID（若指定群主无映射，则从成员中选取第一个有映射的作为群主）
        String dingtalkOwnerUserId = getDingtalkUserIdByLocalUserId(ownerUserId);
        if (dingtalkOwnerUserId == null) {
            log.warn("创建钉钉群聊：群主用户 {} 没有钉钉映射，尝试从成员中选取群主", ownerUserId);
            for (Long uid : memberUserIds) {
                String did = getDingtalkUserIdByLocalUserId(uid);
                if (did != null) {
                    dingtalkOwnerUserId = did;
                    log.info("创建钉钉群聊：使用用户 {} 作为群主", uid);
                    break;
                }
            }
            if (dingtalkOwnerUserId == null) {
                log.warn("创建钉钉群聊失败：成员中无人有钉钉用户映射");
                return null;
            }
        }

        // 3. 转换群成员钉钉用户ID
        List<String> dingtalkUserIds = new ArrayList<>();
        dingtalkUserIds.add(dingtalkOwnerUserId);
        for (Long userId : memberUserIds) {
            String dingtalkUserId = getDingtalkUserIdByLocalUserId(userId);
            if (dingtalkUserId != null && !dingtalkUserIds.contains(dingtalkUserId)) {
                dingtalkUserIds.add(dingtalkUserId);
            }
        }

        if (dingtalkUserIds.size() < 2) {
            log.warn("创建钉钉群聊失败：群成员不足2人");
            return null;
        }

        // 4. 调用钉钉API创建群聊
        try {
            String accessToken = dingtalkApiService.getAccessToken(config);
            return dingtalkApiService.createGroupChat(accessToken, chatName, dingtalkOwnerUserId, dingtalkUserIds);
        } catch (Exception e) {
            log.error("创建钉钉群聊异常", e);
            return null;
        }
    }

    @Override
    public String createBusinessAuditGroupChat(String chatName, Long ownerUserId, List<Long> memberUserIds) {
        String templateId = getBusinessAuditTemplateId();
        if (StrUtil.isNotEmpty(templateId)) {
            return createSceneGroupInternal(chatName, ownerUserId, memberUserIds, templateId);
        }
        return createGroupChat(chatName, ownerUserId, memberUserIds);
    }

    private String createSceneGroupInternal(String chatName, Long ownerUserId, List<Long> memberUserIds, String templateId) {
        List<DingtalkConfigDO> configs = dingtalkConfigService.getEnabledDingtalkConfigList();
        if (CollUtil.isEmpty(configs)) {
            log.warn("创建场景群失败：没有启用的钉钉配置");
            return null;
        }
        DingtalkConfigDO config = configs.get(0);

        String dingtalkOwnerUserId = getDingtalkUserIdByLocalUserId(ownerUserId);
        if (dingtalkOwnerUserId == null) {
            log.warn("创建场景群：群主 {} 无钉钉映射，尝试从成员选取", ownerUserId);
            for (Long uid : memberUserIds) {
                String did = getDingtalkUserIdByLocalUserId(uid);
                if (did != null) {
                    dingtalkOwnerUserId = did;
                    break;
                }
            }
            if (dingtalkOwnerUserId == null) {
                log.warn("创建场景群失败：成员中无人有钉钉映射");
                return null;
            }
        }

        List<String> dingtalkUserIds = new ArrayList<>();
        dingtalkUserIds.add(dingtalkOwnerUserId);
        for (Long userId : memberUserIds) {
            String did = getDingtalkUserIdByLocalUserId(userId);
            if (did != null && !dingtalkUserIds.contains(did)) {
                dingtalkUserIds.add(did);
            }
        }
        if (dingtalkUserIds.size() < 2) {
            log.warn("创建场景群失败：群成员不足2人");
            return null;
        }

        try {
            String accessToken = dingtalkApiService.getAccessToken(config);
            return dingtalkApiService.createSceneGroup(accessToken, templateId, chatName, dingtalkOwnerUserId, dingtalkUserIds);
        } catch (Exception e) {
            log.error("创建场景群异常", e);
            return null;
        }
    }

    @Override
    public String getCallbackBaseUrl() {
        List<DingtalkConfigDO> configs = dingtalkConfigService.getEnabledDingtalkConfigList();
        if (CollUtil.isEmpty(configs) || StrUtil.isEmpty(configs.get(0).getCallbackBaseUrl())) {
            return null;
        }
        String url = configs.get(0).getCallbackBaseUrl();
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    @Override
    public String getApproveBaseUrl() {
        String url = null;
        List<DingtalkConfigDO> configs = dingtalkConfigService.getEnabledDingtalkConfigList();
        if (CollUtil.isNotEmpty(configs)) {
            DingtalkConfigDO config = configs.get(0);
            if (StrUtil.isNotEmpty(config.getApproveBaseUrl())) {
                url = config.getApproveBaseUrl();
            } else if (StrUtil.isNotEmpty(approveBaseUrlFromConfig)) {
                url = approveBaseUrlFromConfig;  // 钉钉配置未填时用 yaml
            } else {
                url = config.getCallbackBaseUrl();
            }
        } else if (StrUtil.isNotEmpty(approveBaseUrlFromConfig)) {
            url = approveBaseUrlFromConfig;
        }
        if (StrUtil.isEmpty(url)) return null;
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    @Override
    public String getBusinessAuditChatId() {
        List<DingtalkConfigDO> configs = dingtalkConfigService.getEnabledDingtalkConfigList();
        if (CollUtil.isEmpty(configs) || StrUtil.isEmpty(configs.get(0).getBusinessAuditChatId())) {
            return null;
        }
        return configs.get(0).getBusinessAuditChatId();
    }

    @Override
    public String getBusinessAuditTemplateId() {
        List<DingtalkConfigDO> configs = dingtalkConfigService.getEnabledDingtalkConfigList();
        if (CollUtil.isEmpty(configs) || StrUtil.isEmpty(configs.get(0).getBusinessAuditTemplateId())) {
            return null;
        }
        return configs.get(0).getBusinessAuditTemplateId();
    }

    @Override
    public boolean sendMessageToChat(String chatId, String title, String content) {
        if (chatId == null || chatId.isEmpty()) {
            log.warn("发送群消息失败：chatId 为空");
            return false;
        }
        List<DingtalkConfigDO> configs = dingtalkConfigService.getEnabledDingtalkConfigList();
        if (CollUtil.isEmpty(configs)) {
            log.warn("发送群消息失败：没有启用的钉钉配置");
            return false;
        }
        try {
            String accessToken = dingtalkApiService.getAccessToken(configs.get(0));
            return dingtalkApiService.sendChatMessage(accessToken, chatId, title, content);
        } catch (Exception e) {
            log.error("发送群消息异常: chatId={}", chatId, e);
            return false;
        }
    }

    @Override
    public boolean addMembersToGroupChat(String chatId, List<Long> memberUserIds) {
        if (chatId == null || chatId.isEmpty() || CollUtil.isEmpty(memberUserIds)) {
            log.warn("加群成员失败：chatId 或 memberUserIds 为空");
            return false;
        }
        List<DingtalkConfigDO> configs = dingtalkConfigService.getEnabledDingtalkConfigList();
        if (CollUtil.isEmpty(configs)) {
            log.warn("加群成员失败：没有启用的钉钉配置");
            return false;
        }
        try {
            String accessToken = dingtalkApiService.getAccessToken(configs.get(0));
            // 将系统用户ID批量转成钉钉用户ID，过滤掉找不到的
            List<String> dingtalkUserIds = memberUserIds.stream()
                    .map(this::getDingtalkUserIdByLocalUserId)
                    .filter(id -> id != null && !id.isEmpty())
                    .distinct()
                    .collect(Collectors.toList());
            if (dingtalkUserIds.isEmpty()) {
                log.warn("加群成员失败：所有用户均未找到对应钉钉ID, memberUserIds={}", memberUserIds);
                return false;
            }
            return dingtalkApiService.addGroupChatMembers(accessToken, chatId, dingtalkUserIds);
        } catch (Exception e) {
            log.error("加群成员异常: chatId={}", chatId, e);
            return false;
        }
    }

    @Override
    public boolean sendActionCardToChat(String chatId, String title, String content,
                                       String buttonTitle, String buttonUrl) {
        if (chatId == null || chatId.isEmpty()) {
            log.warn("发送群互动卡片失败：chatId 为空");
            return false;
        }
        List<DingtalkConfigDO> configs = dingtalkConfigService.getEnabledDingtalkConfigList();
        if (CollUtil.isEmpty(configs)) {
            log.warn("发送群互动卡片失败：没有启用的钉钉配置");
            return false;
        }
        try {
            String accessToken = dingtalkApiService.getAccessToken(configs.get(0));
            return dingtalkApiService.sendChatActionCardMessage(
                    accessToken, chatId, title, content, buttonTitle, buttonUrl);
        } catch (Exception e) {
            log.error("发送群互动卡片异常: chatId={}", chatId, e);
            return false;
        }
    }

    @Override
    public boolean sendActionCardMessage(List<Long> userIds, String title, String content,
                                          String buttonTitle, String buttonUrl) {
        if (CollUtil.isEmpty(userIds)) {
            log.warn("发送钉钉ActionCard消息失败：接收人列表为空");
            return false;
        }

        // 1. 获取启用的钉钉配置
        List<DingtalkConfigDO> configs = dingtalkConfigService.getEnabledDingtalkConfigList();
        if (CollUtil.isEmpty(configs)) {
            log.warn("发送钉钉ActionCard消息失败：没有启用的钉钉配置");
            return false;
        }
        DingtalkConfigDO config = configs.get(0);

        // 2. 获取钉钉用户ID列表
        List<String> dingtalkUserIds = new ArrayList<>();
        for (Long userId : userIds) {
            String dingtalkUserId = getDingtalkUserIdByLocalUserId(userId);
            if (dingtalkUserId != null) {
                dingtalkUserIds.add(dingtalkUserId);
            } else {
                log.warn("发送钉钉ActionCard消息：用户 {} 没有对应的钉钉用户ID", userId);
            }
        }

        if (CollUtil.isEmpty(dingtalkUserIds)) {
            log.warn("发送钉钉ActionCard消息失败：没有找到对应的钉钉用户ID");
            return false;
        }

        // 3. 获取 accessToken 并发送消息
        try {
            String accessToken = dingtalkApiService.getAccessToken(config);
            return dingtalkApiService.sendActionCardMessage(
                    accessToken,
                    config.getAgentId(),
                    dingtalkUserIds,
                    title,
                    content,
                    buttonTitle,
                    buttonUrl
            );
        } catch (Exception e) {
            log.error("发送钉钉ActionCard消息异常", e);
            return false;
        }
    }

    @Override
    public String startOutsideOaApproval(Long userId, String processCode,
                                          String outsideType, String startTime, String endTime,
                                          String duration, String projectName,
                                          String reason, String destination) {
        if (userId == null) {
            log.warn("发起钉钉OA外出申请失败：发起人用户ID为空");
            return null;
        }

        // 1. 获取启用的钉钉配置
        List<DingtalkConfigDO> configs = dingtalkConfigService.getEnabledDingtalkConfigList();
        if (CollUtil.isEmpty(configs)) {
            log.warn("发起钉钉OA外出申请失败：没有启用的钉钉配置");
            return null;
        }
        DingtalkConfigDO config = configs.get(0);

        // 2. 获取钉钉用户ID
        String dingtalkUserId = getDingtalkUserIdByLocalUserId(userId);
        if (dingtalkUserId == null) {
            log.warn("发起钉钉OA外出申请失败：用户 {} 没有对应的钉钉用户ID", userId);
            return null;
        }

        // 3. 获取用户的钉钉部门ID（使用映射表查询）
        Long dingtalkDeptId = getDingtalkDeptIdByLocalUserId(userId);
        if (dingtalkDeptId == null) {
            log.warn("发起钉钉OA外出申请：用户 {} 没有找到钉钉部门ID，使用默认部门1", userId);
            dingtalkDeptId = 1L; // 默认使用根部门
        }

        // 4. 获取 accessToken 并发起OA审批
        try {
            String accessToken = dingtalkApiService.getAccessToken(config);
            return dingtalkApiService.startOutsideApproval(
                    accessToken,
                    processCode,
                    dingtalkUserId,
                    dingtalkDeptId,
                    outsideType,
                    startTime,
                    endTime,
                    duration,
                    projectName,
                    reason,
                    destination
            );
        } catch (Exception e) {
            log.error("发起钉钉OA外出申请异常", e);
            return null;
        }
    }

    @Override
    public String startOutsideSuiteOaApproval(Long userId, String processCode,
                                               String outsideType, String startTime, String endTime,
                                               double durationValue, String projectName,
                                               String reason, String destination) {
        if (userId == null) {
            log.warn("发起钉钉OA外出申请(套件模式)失败：发起人用户ID为空");
            return null;
        }

        // 1. 获取启用的钉钉配置
        List<DingtalkConfigDO> configs = dingtalkConfigService.getEnabledDingtalkConfigList();
        if (CollUtil.isEmpty(configs)) {
            log.warn("发起钉钉OA外出申请(套件模式)失败：没有启用的钉钉配置");
            return null;
        }
        DingtalkConfigDO config = configs.get(0);

        // 2. 获取钉钉用户ID
        String dingtalkUserId = getDingtalkUserIdByLocalUserId(userId);
        if (dingtalkUserId == null) {
            log.warn("发起钉钉OA外出申请(套件模式)失败：用户 {} 没有对应的钉钉用户ID", userId);
            return null;
        }

        // 3. 获取用户的钉钉部门ID（使用映射表查询）
        Long dingtalkDeptId = getDingtalkDeptIdByLocalUserId(userId);
        if (dingtalkDeptId == null) {
            log.warn("发起钉钉OA外出申请(套件模式)：用户 {} 没有找到钉钉部门ID，使用默认部门1", userId);
            dingtalkDeptId = 1L; // 默认使用根部门
        }

        // 4. 获取 accessToken 并发起OA审批（套件模式）
        try {
            String accessToken = dingtalkApiService.getAccessToken(config);
            return dingtalkApiService.startOutsideSuiteApproval(
                    accessToken,
                    processCode,
                    dingtalkUserId,
                    dingtalkDeptId,
                    outsideType,
                    startTime,
                    endTime,
                    durationValue,
                    projectName,
                    reason,
                    destination
            );
        } catch (Exception e) {
            log.error("发起钉钉OA外出申请(套件模式)异常", e);
            return null;
        }
    }

    /**
     * 根据本地用户ID获取钉钉部门ID
     */
    private Long getDingtalkDeptIdByLocalUserId(Long userId) {
        // 1. 先获取用户的本地部门ID
        AdminUserRespDTO user = adminUserApi.getUser(userId);
        if (user == null || user.getDeptId() == null) {
            log.debug("用户不存在或无部门: userId={}", userId);
            return null;
        }
        Long localDeptId = user.getDeptId();
        
        // 2. 通过映射表获取钉钉部门ID
        DingtalkMappingDO deptMapping = dingtalkMappingMapper.selectByLocalId(localDeptId, "DEPT");
        if (deptMapping != null && deptMapping.getDingtalkId() != null) {
            try {
                return Long.parseLong(deptMapping.getDingtalkId());
            } catch (NumberFormatException e) {
                log.debug("钉钉部门ID格式转换失败: dingtalkId={}", deptMapping.getDingtalkId());
            }
        }
        return null;
    }

}
