package cn.shuhe.system.module.project.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectRoundSaveReqVO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectDO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectRoundDO;
import cn.shuhe.system.module.project.dal.dataobject.ServiceItemDO;
import cn.shuhe.system.module.project.dal.mysql.ProjectMapper;
import cn.shuhe.system.module.project.dal.mysql.ProjectRoundMapper;
import cn.shuhe.system.module.project.dal.mysql.ServiceItemMapper;
import cn.shuhe.system.module.system.api.notify.NotifyMessageSendApi;
import cn.shuhe.system.module.system.api.notify.dto.NotifySendSingleToUserReqDTO;
import cn.shuhe.system.module.system.dal.dataobject.dingtalkconfig.DingtalkConfigDO;
import cn.shuhe.system.module.system.dal.dataobject.dingtalkmapping.DingtalkMappingDO;
import cn.shuhe.system.module.system.dal.mysql.dingtalkmapping.DingtalkMappingMapper;
import cn.shuhe.system.module.system.service.dingtalkconfig.DingtalkApiService;
import cn.shuhe.system.module.system.service.dingtalkconfig.DingtalkConfigService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.shuhe.system.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.shuhe.system.module.project.enums.ErrorCodeConstants.*;

/**
 * 项目轮次 Service 实现类
 */
@Service
@Validated
@Slf4j
public class ProjectRoundServiceImpl implements ProjectRoundService {

    @Resource
    private ProjectRoundMapper projectRoundMapper;

    @Resource
    private ProjectMapper projectMapper;

    @Resource
    private ServiceItemMapper serviceItemMapper;

    @Resource
    private ServiceItemService serviceItemService;

    @Resource
    private cn.shuhe.system.module.project.dal.mysql.ContractTimeMapper contractTimeMapper;

    @Resource
    private NotifyMessageSendApi notifyMessageSendApi;

    @Resource
    private DingtalkApiService dingtalkApiService;

    @Resource
    private DingtalkConfigService dingtalkConfigService;

    @Resource
    private DingtalkMappingMapper dingtalkMappingMapper;

    @Override
    public Long createProjectRound(ProjectRoundSaveReqVO createReqVO) {
        // 校验服务项是否存在（projectId 实际上是 serviceItemId，历史原因）
        ServiceItemDO serviceItem = serviceItemMapper.selectById(createReqVO.getProjectId());
        if (serviceItem == null) {
            throw exception(SERVICE_ITEM_NOT_EXISTS);
        }

        // 校验轮次次数限制（frequencyType != 0 时有限制）
        if (serviceItem.getFrequencyType() != null && serviceItem.getFrequencyType() != 0) {
            Integer maxCount = serviceItem.getMaxCount();
            if (maxCount != null && maxCount > 0) {
                // 统计有效轮次数量（待执行0 + 执行中1 + 已完成2，不包括已取消3）
                Long validRoundCount = projectRoundMapper.selectValidRoundCount(createReqVO.getProjectId());
                if (validRoundCount >= maxCount) {
                    throw exception(PROJECT_ROUND_COUNT_EXCEED_LIMIT, maxCount);
                }
            }
        }

        // 校验轮次时间在合同范围内
        Long contractId = getContractIdForRound(createReqVO.getServiceItemId(), serviceItem.getContractId());
        validateRoundTimeInContractRange(contractId, createReqVO.getPlanStartTime(), createReqVO.getPlanEndTime());

        // 获取最大轮次序号
        Integer maxRoundNo = projectRoundMapper.selectMaxRoundNo(createReqVO.getProjectId());
        int newRoundNo = maxRoundNo + 1;

        // 转换并保存
        ProjectRoundDO round = new ProjectRoundDO();
        round.setProjectId(createReqVO.getProjectId());
        round.setServiceItemId(createReqVO.getServiceItemId());
        round.setName(createReqVO.getName());
        round.setPlanStartTime(createReqVO.getPlanStartTime());
        round.setPlanEndTime(createReqVO.getPlanEndTime());
        round.setRemark(createReqVO.getRemark());
        round.setRoundNo(newRoundNo);
        round.setStatus(0); // 默认待执行
        round.setProgress(0);

        // 处理执行人ID列表，转换为JSON字符串
        if (CollUtil.isNotEmpty(createReqVO.getExecutorIds())) {
            round.setExecutorIds(JSONUtil.toJsonStr(createReqVO.getExecutorIds()));
        }
        round.setExecutorNames(createReqVO.getExecutorNames());

        // 如果没有设置名称，自动生成
        if (round.getName() == null || round.getName().isEmpty()) {
            round.setName("第" + newRoundNo + "次执行");
        }

        projectRoundMapper.insert(round);
        return round.getId();
    }

    @Override
    public void updateProjectRound(ProjectRoundSaveReqVO updateReqVO) {
        // 校验存在
        ProjectRoundDO existingRound = projectRoundMapper.selectById(updateReqVO.getId());
        if (existingRound == null) {
            throw exception(PROJECT_ROUND_NOT_EXISTS);
        }

        // 校验轮次时间在合同范围内
        ProjectDO project = projectMapper.selectById(existingRound.getProjectId());
        Long contractId = getContractIdForRound(existingRound.getServiceItemId(), project != null ? project.getContractId() : null);
        validateRoundTimeInContractRange(contractId, updateReqVO.getPlanStartTime(), updateReqVO.getPlanEndTime());

        // 更新
        ProjectRoundDO updateObj = new ProjectRoundDO();
        updateObj.setId(updateReqVO.getId());
        updateObj.setName(updateReqVO.getName());
        updateObj.setPlanStartTime(updateReqVO.getPlanStartTime());
        updateObj.setPlanEndTime(updateReqVO.getPlanEndTime());
        updateObj.setRemark(updateReqVO.getRemark());
        
        // 处理执行人ID列表
        if (CollUtil.isNotEmpty(updateReqVO.getExecutorIds())) {
            updateObj.setExecutorIds(JSONUtil.toJsonStr(updateReqVO.getExecutorIds()));
        } else {
            updateObj.setExecutorIds(null);
        }
        updateObj.setExecutorNames(updateReqVO.getExecutorNames());
        
        projectRoundMapper.updateById(updateObj);
    }

    @Override
    public void deleteProjectRound(Long id) {
        // 校验存在
        validateProjectRoundExists(id);
        // 删除（已执行次数会通过实时查询自动更新，无需手动维护）
        projectRoundMapper.deleteById(id);
    }

    private void validateProjectRoundExists(Long id) {
        if (projectRoundMapper.selectById(id) == null) {
            throw exception(PROJECT_ROUND_NOT_EXISTS);
        }
    }

    /**
     * 获取轮次关联的合同ID
     * 优先从服务项获取，如果没有服务项则从项目获取
     */
    private Long getContractIdForRound(Long serviceItemId, Long projectContractId) {
        if (serviceItemId != null) {
            ServiceItemDO serviceItem = serviceItemMapper.selectById(serviceItemId);
            if (serviceItem != null && serviceItem.getContractId() != null) {
                return serviceItem.getContractId();
            }
        }
        return projectContractId;
    }

    /**
     * 校验轮次时间在合同范围内
     */
    private void validateRoundTimeInContractRange(Long contractId, LocalDateTime planStartTime, LocalDateTime planEndTime) {
        if (contractId == null) {
            return; // 没有关联合同，不校验
        }
        if (planStartTime == null && planEndTime == null) {
            return; // 没有设置时间，不校验
        }

        Map<String, LocalDateTime> contractTime = contractTimeMapper.selectContractTime(contractId);
        if (contractTime == null) {
            return; // 合同不存在，不校验
        }

        LocalDateTime contractStart = contractTime.get("startTime");
        LocalDateTime contractEnd = contractTime.get("endTime");

        // 校验开始时间不能早于合同开始时间
        if (planStartTime != null && contractStart != null && planStartTime.isBefore(contractStart)) {
            throw exception(PROJECT_ROUND_TIME_BEFORE_CONTRACT, contractStart.toLocalDate().toString());
        }

        // 校验结束时间不能晚于合同结束时间
        if (planEndTime != null && contractEnd != null && planEndTime.isAfter(contractEnd)) {
            throw exception(PROJECT_ROUND_TIME_AFTER_CONTRACT, contractEnd.toLocalDate().toString());
        }
    }

    @Override
    public ProjectRoundDO getProjectRound(Long id) {
        return projectRoundMapper.selectById(id);
    }

    @Override
    public List<ProjectRoundDO> getProjectRoundList(Long projectId) {
        return projectRoundMapper.selectListByProjectId(projectId);
    }

    @Override
    public List<ProjectRoundDO> getProjectRoundListByServiceItemId(Long serviceItemId) {
        return projectRoundMapper.selectListByServiceItemId(serviceItemId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRoundStatus(Long id, Integer status) {
        // 校验存在并获取轮次信息
        ProjectRoundDO round = projectRoundMapper.selectById(id);
        if (round == null) {
            throw exception(PROJECT_ROUND_NOT_EXISTS);
        }

        Integer oldStatus = round.getStatus();

        // 更新状态
        ProjectRoundDO updateObj = new ProjectRoundDO();
        updateObj.setId(id);
        updateObj.setStatus(status);
        projectRoundMapper.updateById(updateObj);

        // 当状态变为"执行中"(1)时，通知执行人
        if (status == 1 && (oldStatus == null || oldStatus == 0)) {
            log.info("【轮次状态】轮次 {} 开始执行", id);
            sendRoundStartNotification(round);
        }
    }

    /**
     * 发送轮次开始通知给执行人
     */
    private void sendRoundStartNotification(ProjectRoundDO round) {
        if (round.getExecutorIds() == null || round.getExecutorIds().isEmpty()) {
            log.info("【轮次通知】轮次 {} 没有执行人，跳过通知", round.getId());
            return;
        }

        // 解析执行人ID列表
        List<Long> executorIds;
        try {
            executorIds = JSONUtil.toList(round.getExecutorIds(), Long.class);
        } catch (Exception e) {
            log.error("【轮次通知】解析执行人ID失败: {}", round.getExecutorIds(), e);
            return;
        }

        if (CollUtil.isEmpty(executorIds)) {
            return;
        }

        // 获取服务项信息（用于通知内容）
        ServiceItemDO serviceItem = serviceItemMapper.selectById(round.getProjectId());
        String customerName = serviceItem != null ? serviceItem.getCustomerName() : "未知客户";
        String serviceType = serviceItem != null ? serviceItem.getServiceType() : "未知服务类型";

        // 构建通知内容
        String roundName = round.getName() != null ? round.getName() : "第" + round.getRoundNo() + "次执行";
        String title = "轮次任务开始通知";
        
        // 格式化时间
        String startTime = round.getPlanStartTime() != null 
                ? round.getPlanStartTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) 
                : "-";
        String endTime = round.getPlanEndTime() != null 
                ? round.getPlanEndTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) 
                : "-";
        
        String content = String.format("### %s\n\n" +
                "您有一个轮次任务已开始执行：\n\n" +
                "- **客户**：%s\n" +
                "- **服务类型**：%s\n" +
                "- **轮次**：%s\n" +
                "- **计划开始**：%s\n" +
                "- **计划结束**：%s\n\n" +
                "请及时处理！",
                title,
                customerName,
                serviceType,
                roundName,
                startTime,
                endTime
        );

        // 发送站内信通知
        for (Long userId : executorIds) {
            try {
                NotifySendSingleToUserReqDTO reqDTO = new NotifySendSingleToUserReqDTO();
                reqDTO.setUserId(userId);
                reqDTO.setTemplateCode("round-start-notification");
                Map<String, Object> templateParams = new HashMap<>();
                templateParams.put("customerName", customerName);
                templateParams.put("serviceType", serviceType);
                templateParams.put("roundName", roundName);
                templateParams.put("startTime", startTime);
                templateParams.put("endTime", endTime);
                reqDTO.setTemplateParams(templateParams);
                notifyMessageSendApi.sendSingleMessageToAdmin(reqDTO);
                log.info("【轮次通知】站内信发送成功：roundId={}, userId={}", round.getId(), userId);
            } catch (Exception e) {
                log.warn("【轮次通知】站内信发送失败：roundId={}, userId={}, error={}", round.getId(), userId, e.getMessage());
            }
        }

        // 发送钉钉通知
        try {
            List<DingtalkConfigDO> configs = dingtalkConfigService.getEnabledDingtalkConfigList();
            if (configs.isEmpty()) {
                log.warn("【轮次通知】未找到启用的钉钉配置，跳过钉钉通知");
                return;
            }
            DingtalkConfigDO config = configs.get(0);

            if (config.getAgentId() == null || config.getAgentId().isEmpty()) {
                log.warn("【轮次通知】钉钉配置缺少agentId，跳过钉钉通知");
                return;
            }

            String accessToken = dingtalkApiService.getAccessToken(config);

            for (Long userId : executorIds) {
                DingtalkMappingDO mapping = dingtalkMappingMapper.selectByLocalId(userId, "USER");
                if (mapping == null || mapping.getDingtalkId() == null) {
                    log.warn("【轮次通知】用户 {} 没有绑定钉钉账号，跳过钉钉通知", userId);
                    continue;
                }

                boolean success = dingtalkApiService.sendWorkNotice(
                        accessToken,
                        config.getAgentId(),
                        mapping.getDingtalkId(),
                        title,
                        content
                );

                if (success) {
                    log.info("【轮次通知】钉钉通知发送成功：roundId={}, userId={}, dingtalkId={}",
                            round.getId(), userId, mapping.getDingtalkId());
                }
            }
        } catch (Exception e) {
            log.error("【轮次通知】钉钉通知发送失败", e);
        }
    }

    @Override
    public void updateRoundProgress(Long id, Integer progress) {
        // 校验存在
        validateProjectRoundExists(id);

        // 更新进度
        ProjectRoundDO updateObj = new ProjectRoundDO();
        updateObj.setId(id);
        updateObj.setProgress(progress);
        projectRoundMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRoundByServiceItem(Long serviceItemId, String processInstanceId) {
        return createRoundByServiceItem(serviceItemId, processInstanceId, null, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRoundByServiceItem(Long serviceItemId, String processInstanceId,
            LocalDateTime planStartTime, LocalDateTime planEndTime) {
        // 1. 校验服务项存在
        ServiceItemDO serviceItem = serviceItemMapper.selectById(serviceItemId);
        if (serviceItem == null) {
            throw exception(SERVICE_ITEM_NOT_EXISTS);
        }

        // 2. 检查是否可以发起新的执行（次数限制）
        if (!serviceItemService.canStartExecution(serviceItemId)) {
            throw exception(SERVICE_ITEM_EXECUTION_LIMIT_EXCEEDED);
        }

        // 3. 获取最大轮次序号
        Integer maxRoundNo = projectRoundMapper.selectMaxRoundNoByServiceItemId(serviceItemId);
        int newRoundNo = maxRoundNo + 1;

        // 4. 创建轮次
        ProjectRoundDO round = new ProjectRoundDO();
        round.setProjectId(serviceItem.getProjectId());
        round.setServiceItemId(serviceItemId);
        round.setProcessInstanceId(processInstanceId);
        round.setRoundNo(newRoundNo);
        round.setName("第" + newRoundNo + "次");
        round.setPlanStartTime(planStartTime);
        round.setPlanEndTime(planEndTime);
        round.setStatus(0); // 待执行
        round.setProgress(0);
        projectRoundMapper.insert(round);

        // 注意：不在创建时计数，而是在点击"开始"时才增加已使用次数

        log.info("【服务执行】服务项 {} 创建了第 {} 轮执行，轮次ID: {}, 流程实例ID: {}, 计划开始: {}, 计划结束: {}",
                serviceItemId, newRoundNo, round.getId(), processInstanceId, planStartTime, planEndTime);

        return round.getId();
    }

    @Override
    public List<ProjectRoundDO> getRoundListByServiceItemId(Long serviceItemId) {
        return projectRoundMapper.selectListByServiceItemId(serviceItemId);
    }

    @Override
    public int getRoundCountByServiceItemId(Long serviceItemId) {
        return projectRoundMapper.selectCountByServiceItemId(serviceItemId);
    }

}
