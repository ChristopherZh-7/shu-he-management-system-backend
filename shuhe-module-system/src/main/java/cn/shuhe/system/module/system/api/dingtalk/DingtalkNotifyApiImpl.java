package cn.shuhe.system.module.system.api.dingtalk;

import cn.hutool.core.collection.CollUtil;
import cn.shuhe.system.module.system.api.dingtalk.dto.DingtalkNotifySendReqDTO;
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

}
