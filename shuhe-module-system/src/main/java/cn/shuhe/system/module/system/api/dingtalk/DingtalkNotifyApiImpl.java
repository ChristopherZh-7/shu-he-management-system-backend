package cn.shuhe.system.module.system.api.dingtalk;

import cn.hutool.core.collection.CollUtil;
import cn.shuhe.system.module.system.api.dingtalk.dto.DingtalkNotifySendReqDTO;
import cn.shuhe.system.module.system.api.user.AdminUserApi;
import cn.shuhe.system.module.system.api.user.dto.AdminUserRespDTO;
import cn.shuhe.system.module.system.dal.dataobject.dept.DeptDO;
import cn.shuhe.system.module.system.dal.dataobject.dingtalkconfig.DingtalkConfigDO;
import cn.shuhe.system.module.system.dal.dataobject.dingtalkmapping.DingtalkMappingDO;
import cn.shuhe.system.module.system.dal.mysql.dingtalkmapping.DingtalkMappingMapper;
import cn.shuhe.system.module.system.service.dept.DeptService;
import cn.shuhe.system.module.system.service.dingtalkconfig.DingtalkApiService;
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
