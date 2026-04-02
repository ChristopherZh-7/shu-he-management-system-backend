package cn.shuhe.system.module.project.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectRoundSaveReqVO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectDO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectRoundDO;
import cn.shuhe.system.module.project.dal.dataobject.ServiceItemDO;
import cn.shuhe.system.module.project.dal.mysql.ProjectMapper;
import cn.shuhe.system.module.project.dal.mysql.ProjectRoundMapper;
import cn.shuhe.system.module.project.dal.mysql.ProjectRoundVulnerabilityMapper;
import cn.shuhe.system.module.project.dal.mysql.ServiceItemMapper;
import cn.shuhe.system.module.system.api.dingtalk.DingtalkNotifyApi;
import cn.shuhe.system.module.system.api.notify.NotifyMessageSendApi;
import cn.shuhe.system.module.system.api.notify.dto.NotifySendSingleToUserReqDTO;
import cn.shuhe.system.module.system.api.user.AdminUserApi;
import cn.shuhe.system.module.system.api.user.dto.AdminUserRespDTO;
import cn.shuhe.system.module.system.dal.dataobject.dingtalkconfig.DingtalkConfigDO;
import cn.shuhe.system.module.system.dal.dataobject.dingtalkmapping.DingtalkMappingDO;
import cn.shuhe.system.module.system.dal.mysql.dingtalkmapping.DingtalkMappingMapper;
import cn.shuhe.system.module.system.dal.dataobject.dict.DictDataDO;
import cn.shuhe.system.module.system.service.dict.DictDataService;
import cn.shuhe.system.module.system.service.dingtalkconfig.DingtalkApiService;
import cn.shuhe.system.module.system.service.dingtalkconfig.DingtalkConfigService;
import cn.shuhe.system.framework.security.core.util.SecurityFrameworkUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @Resource
    private DingtalkNotifyApi dingtalkNotifyApi;

    @Resource
    private AdminUserApi adminUserApi;

    @Resource
    private EmployeeScheduleService employeeScheduleService;

    @Resource
    private DictDataService dictDataService;

    @Resource
    private ProjectRoundVulnerabilityMapper vulnerabilityMapper;

    /**
     * 部门类型对应的服务类型字典类型
     */
    private static final Map<Integer, String> DEPT_TYPE_DICT_MAP = Map.of(
            1, "project_service_type_security",
            2, "project_service_type_operation",
            3, "project_service_type_data"
    );

    @Override
    public Long createProjectRound(ProjectRoundSaveReqVO createReqVO) {
        // 优先使用 serviceItemId，兼容历史数据（projectId 曾用于存储 serviceItemId）
        Long serviceItemId = createReqVO.getServiceItemId() != null 
                ? createReqVO.getServiceItemId() 
                : createReqVO.getProjectId();
        
        // 校验服务项是否存在
        ServiceItemDO serviceItem = serviceItemMapper.selectById(serviceItemId);
        if (serviceItem == null) {
            throw exception(SERVICE_ITEM_NOT_EXISTS);
        }

        // 校验轮次次数限制（frequencyType != 0 时有限制）
        if (serviceItem.getFrequencyType() != null && serviceItem.getFrequencyType() != 0) {
            Integer maxCount = serviceItem.getMaxCount();
            if (maxCount != null && maxCount > 0) {
                // 统计有效轮次数量（待执行0 + 执行中1 + 已完成2，不包括已取消3）
                Long validRoundCount = projectRoundMapper.selectValidRoundCount(serviceItemId);
                if (validRoundCount >= maxCount) {
                    throw exception(PROJECT_ROUND_COUNT_EXCEED_LIMIT, maxCount);
                }
            }
        }

        // 校验轮次时间在合同范围内
        Long contractId = getContractIdForRound(serviceItemId, serviceItem.getContractId());
        validateRoundTimeInContractRange(contractId, createReqVO.getDeadline(), createReqVO.getPlanEndTime());

        // 获取最大轮次序号（如果请求中指定了 roundNo，使用指定的；否则自动生成）
        int newRoundNo;
        if (createReqVO.getRoundNo() != null && createReqVO.getRoundNo() > 0) {
            newRoundNo = createReqVO.getRoundNo();
        } else {
            Integer maxRoundNo = projectRoundMapper.selectMaxRoundNoByServiceItemId(serviceItemId);
            newRoundNo = (maxRoundNo != null ? maxRoundNo : 0) + 1;
        }

        // 转换并保存
        ProjectRoundDO round = new ProjectRoundDO();
        round.setProjectId(createReqVO.getProjectId());
        round.setServiceItemId(serviceItemId); // 使用解析后的 serviceItemId
        round.setName(createReqVO.getName());
        round.setDeadline(createReqVO.getDeadline());
        round.setPlanEndTime(createReqVO.getPlanEndTime());
        round.setRemark(createReqVO.getRemark());
        round.setRoundNo(newRoundNo);
        // 状态：如果指定了就用指定的，否则默认待执行
        round.setStatus(createReqVO.getStatus() != null ? createReqVO.getStatus() : 0);
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

        // 设置提醒相关字段
        round.setRemindDays(3); // 默认提前3天提醒
        round.setReminded(false);
        if (round.getDeadline() != null) {
            round.setRemindTime(round.getDeadline().minusDays(3));
        }

        projectRoundMapper.insert(round);

        // 确保项目有钉钉群（没有则创建），拉执行人进群，发轮次通知
        try {
            ensureProjectGroupAndNotify(round, serviceItem, createReqVO.getExecutorIds());
        } catch (Exception e) {
            log.warn("[createProjectRound] 钉钉群操作失败, roundId={}", round.getId(), e);
        }

        return round.getId();
    }

    /**
     * 确保项目有钉钉群（没有则创建），拉执行人进群，发轮次通知
     */
    private void ensureProjectGroupAndNotify(ProjectRoundDO round, ServiceItemDO serviceItem, List<Long> executorIds) {
        if (serviceItem == null) {
            return;
        }

        Long projectId = serviceItem.getProjectId() != null ? serviceItem.getProjectId() : round.getProjectId();
        ProjectDO project = projectMapper.selectById(projectId);
        if (project == null) {
            log.info("[ensureProjectGroupAndNotify] 项目不存在: {}", projectId);
            return;
        }

        Integer serviceMode = serviceItem.getServiceMode();
        String chatId = getProjectChatId(project, serviceMode);
        Long currentUserId = SecurityFrameworkUtils.getLoginUserId();

        // 1. 如果项目还没有群，创建群
        if (chatId == null) {
            chatId = createProjectGroup(project, serviceMode, executorIds, currentUserId);
            if (chatId == null) {
                log.warn("[ensureProjectGroupAndNotify] 创建群失败, projectId={}, serviceMode={}", projectId, serviceMode);
                return;
            }
        } else {
            // 2. 群已存在，把新执行人加进去
            if (CollUtil.isNotEmpty(executorIds)) {
                try {
                    dingtalkNotifyApi.addMembersToGroupChat(chatId, executorIds);
                    log.info("[ensureProjectGroupAndNotify] 已将执行人加入群: chatId={}, executorIds={}", chatId, executorIds);
                } catch (Exception e) {
                    log.warn("[ensureProjectGroupAndNotify] 拉人进群失败", e);
                }
            }
        }

        // 3. 发送轮次通知卡片
        sendRoundCardMessage(chatId, round, project, serviceItem);
    }

    private String getProjectChatId(ProjectDO project, Integer serviceMode) {
        if (serviceMode != null && serviceMode == 1) {
            return project.getDingtalkOnsiteChatId();
        } else if (serviceMode != null && serviceMode == 2) {
            return project.getDingtalkSecondLineChatId();
        }
        return null;
    }

    private String createProjectGroup(ProjectDO project, Integer serviceMode, List<Long> executorIds, Long currentUserId) {
        java.util.List<Long> members = new java.util.ArrayList<>();
        if (CollUtil.isNotEmpty(executorIds)) {
            members.addAll(executorIds);
        }
        if (currentUserId != null && !members.contains(currentUserId)) {
            members.add(currentUserId);
        }
        if (members.size() < 2) {
            log.info("[createProjectGroup] 成员不足2人, 无法创建群: members={}", members);
            return null;
        }

        String projectName = project.getName() != null ? project.getName() : "项目";
        String dateStr = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String modeName = serviceMode != null && serviceMode == 1 ? "驻场" : "二线";
        String chatName = projectName + "-" + modeName + "-" + dateStr;

        Long owner = currentUserId != null ? currentUserId : members.get(0);
        String chatId = dingtalkNotifyApi.createGroupChat(chatName, owner, members);
        if (chatId != null) {
            ProjectDO upd = new ProjectDO();
            upd.setId(project.getId());
            if (serviceMode != null && serviceMode == 1) {
                upd.setDingtalkOnsiteChatId(chatId);
            } else {
                upd.setDingtalkSecondLineChatId(chatId);
            }
            projectMapper.updateById(upd);
            log.info("[createProjectGroup] 群创建成功: projectId={}, chatId={}, name={}", project.getId(), chatId, chatName);
        }
        return chatId;
    }

    private void sendRoundCardMessage(String chatId, ProjectRoundDO round, ProjectDO project, ServiceItemDO serviceItem) {
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String planStart = round.getDeadline() != null ? round.getDeadline().format(fmt) : "未设置";
        String planEnd = round.getPlanEndTime() != null ? round.getPlanEndTime().format(fmt) : "未设置";
        String executorNames = round.getExecutorNames() != null ? round.getExecutorNames() : "-";
        String serviceTypeName = getServiceTypeLabel(serviceItem);

        StringBuilder msg = new StringBuilder();
        msg.append("## 新轮次已创建\n\n");
        msg.append("**").append(round.getName()).append("**\n\n");
        msg.append("- 项目：").append(project.getName() != null ? project.getName() : "-").append("\n\n");
        msg.append("- 服务类型：").append(serviceTypeName).append("\n\n");
        msg.append("- 客户：").append(serviceItem.getCustomerName() != null ? serviceItem.getCustomerName() : "-").append("\n\n");
        msg.append("- 执行人：").append(executorNames).append("\n\n");
        msg.append("- 计划开始时间：").append(planStart).append("\n\n");
        msg.append("- 计划结束时间：").append(planEnd).append("\n\n");
        msg.append("请相关人员及时关注并执行任务！");

        dingtalkNotifyApi.sendMessageToChat(chatId, "轮次任务通知", msg.toString());
    }

    /**
     * 获取服务类型的中文标签
     */
    private String getServiceTypeLabel(ServiceItemDO serviceItem) {
        if (serviceItem == null || serviceItem.getServiceType() == null) {
            return "服务";
        }
        if (serviceItem.getDeptType() != null) {
            String dictType = DEPT_TYPE_DICT_MAP.get(serviceItem.getDeptType());
            if (dictType != null) {
                try {
                    DictDataDO dictData = dictDataService.getDictData(dictType, serviceItem.getServiceType());
                    if (dictData != null && dictData.getLabel() != null) {
                        return dictData.getLabel();
                    }
                } catch (Exception e) {
                    log.warn("[getServiceTypeLabel] 获取字典标签失败: {}", serviceItem.getServiceType());
                }
            }
        }
        return serviceItem.getServiceType();
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
        validateRoundTimeInContractRange(contractId, updateReqVO.getDeadline(), updateReqVO.getPlanEndTime());

        // 更新
        ProjectRoundDO updateObj = new ProjectRoundDO();
        updateObj.setId(updateReqVO.getId());
        updateObj.setName(updateReqVO.getName());
        updateObj.setDeadline(updateReqVO.getDeadline());
        updateObj.setPlanEndTime(updateReqVO.getPlanEndTime());
        updateObj.setRemark(updateReqVO.getRemark());
        
        // 如果截止日期变化了，重新计算提醒时间并重置提醒状态
        if (updateReqVO.getDeadline() != null) {
            Integer remindDays = existingRound.getRemindDays() != null ? existingRound.getRemindDays() : 3;
            updateObj.setRemindTime(updateReqVO.getDeadline().minusDays(remindDays));
            // 如果新的截止日期还没到提醒时间，重置提醒状态
            if (updateReqVO.getDeadline().minusDays(remindDays).isAfter(LocalDateTime.now())) {
                updateObj.setReminded(false);
            }
        }
        
        // 更新状态和实际时间
        if (updateReqVO.getStatus() != null) {
            updateObj.setStatus(updateReqVO.getStatus());
        }
        if (updateReqVO.getActualStartTime() != null) {
            updateObj.setActualStartTime(updateReqVO.getActualStartTime());
        }
        if (updateReqVO.getActualEndTime() != null) {
            updateObj.setActualEndTime(updateReqVO.getActualEndTime());
        }
        if (updateReqVO.getRoundNo() != null) {
            updateObj.setRoundNo(updateReqVO.getRoundNo());
        }
        
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
    private void validateRoundTimeInContractRange(Long contractId, LocalDateTime deadline, LocalDateTime planEndTime) {
        if (contractId == null) {
            return; // 没有关联合同，不校验
        }
        if (deadline == null && planEndTime == null) {
            return; // 没有设置时间，不校验
        }

        Map<String, LocalDateTime> contractTime = contractTimeMapper.selectContractTime(contractId);
        if (contractTime == null) {
            return; // 合同不存在，不校验
        }

        LocalDateTime contractStart = contractTime.get("startTime");
        LocalDateTime contractEnd = contractTime.get("endTime");

        // 校验截止日期不能早于合同开始时间（只比较日期，忽略时分秒）
        if (deadline != null && contractStart != null 
                && deadline.toLocalDate().isBefore(contractStart.toLocalDate())) {
            throw exception(PROJECT_ROUND_TIME_BEFORE_CONTRACT, contractStart.toLocalDate().toString());
        }

        // 校验截止日期不能晚于合同结束时间（只比较日期，忽略时分秒）
        if (deadline != null && contractEnd != null 
                && deadline.toLocalDate().isAfter(contractEnd.toLocalDate())) {
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
        
        // 当状态变为"已完成"(2)时，完成关联的排期记录 + 发钉钉完成通知
        if (status == 2 && oldStatus != null && oldStatus != 2) {
            log.info("【轮次状态】轮次 {} 已完成，更新关联排期", id);
            try {
                employeeScheduleService.completeScheduleByRoundId(id);
            } catch (Exception e) {
                log.warn("【轮次状态】完成排期失败，roundId={}, error={}", id, e.getMessage());
            }
            // 发送钉钉群完成通知
            try {
                sendRoundCompletionNotification(round);
            } catch (Exception e) {
                log.warn("[updateRoundStatus] 发送完成通知失败, roundId={}", id, e);
            }
        }
    }

    /**
     * 向项目钉钉群发送轮次完成通知
     */
    private void sendRoundCompletionNotification(ProjectRoundDO round) {
        // 通过服务项找到对应的项目群
        Long serviceItemIdToQuery = round.getServiceItemId() != null ? round.getServiceItemId() : round.getProjectId();
        ServiceItemDO serviceItem = serviceItemMapper.selectById(serviceItemIdToQuery);
        if (serviceItem == null) {
            log.info("[sendRoundCompletionNotification] 服务项不存在, roundId={}", round.getId());
            return;
        }

        Long projectId = serviceItem.getProjectId() != null ? serviceItem.getProjectId() : round.getProjectId();
        ProjectDO project = projectMapper.selectById(projectId);
        if (project == null) {
            return;
        }

        String chatId = null;
        Integer serviceMode = serviceItem.getServiceMode();
        if (serviceMode != null && serviceMode == 1) {
            chatId = project.getDingtalkOnsiteChatId();
        } else if (serviceMode != null && serviceMode == 2) {
            chatId = project.getDingtalkSecondLineChatId();
        }
        if (chatId == null) {
            log.info("[sendRoundCompletionNotification] 项目 {} 没有对应的钉钉群", projectId);
            return;
        }

        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String completedAt = java.time.LocalDateTime.now().format(fmt);
        String executorNames = round.getExecutorNames() != null ? round.getExecutorNames() : "-";
        String serviceTypeName = getServiceTypeLabel(serviceItem);

        Long highCount = vulnerabilityMapper.selectCountByRoundIdAndSeverity(round.getId(), "high");
        Long mediumCount = vulnerabilityMapper.selectCountByRoundIdAndSeverity(round.getId(), "medium");
        Long lowCount = vulnerabilityMapper.selectCountByRoundIdAndSeverity(round.getId(), "low");
        long totalCount = highCount + mediumCount + lowCount;

        StringBuilder msg = new StringBuilder();
        msg.append("## 轮次已完成\n\n");
        msg.append("**").append(round.getName()).append("**\n\n");
        msg.append("- 项目：").append(project.getName() != null ? project.getName() : "-").append("\n\n");
        msg.append("- 服务类型：").append(serviceTypeName).append("\n\n");
        msg.append("- 执行人：").append(executorNames).append("\n\n");
        msg.append("- 完成时间：").append(completedAt).append("\n\n");
        msg.append("### 漏洞成果\n\n");
        if (totalCount > 0) {
            msg.append("- 共发现 **").append(totalCount).append("** 个漏洞\n\n");
            if (highCount > 0) msg.append("- 🔴 高危：").append(highCount).append(" 个\n\n");
            if (mediumCount > 0) msg.append("- 🟡 中危：").append(mediumCount).append(" 个\n\n");
            if (lowCount > 0) msg.append("- 🟢 低危：").append(lowCount).append(" 个\n\n");
        } else {
            msg.append("- 本轮次未发现漏洞\n\n");
        }
        msg.append("轮次任务已顺利完成，感谢各位的配合！");

        dingtalkNotifyApi.sendMessageToChat(chatId, "轮次完成通知", msg.toString());
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
        // 优先使用 serviceItemId，如果为空则回退使用 projectId（历史数据中 projectId 实际存储的是 serviceItemId）
        Long serviceItemIdToQuery = round.getServiceItemId() != null ? round.getServiceItemId() : round.getProjectId();
        ServiceItemDO serviceItem = serviceItemMapper.selectById(serviceItemIdToQuery);
        String customerName = serviceItem != null ? serviceItem.getCustomerName() : "未知客户";
        
        // 获取服务类型中文名称（通过字典转换）
        String serviceTypeName = "未知服务类型";
        if (serviceItem != null && serviceItem.getServiceType() != null) {
            // 根据部门类型获取对应的字典类型
            Integer deptType = serviceItem.getDeptType();
            if (deptType != null) {
                String dictType = DEPT_TYPE_DICT_MAP.get(deptType);
                if (dictType != null) {
                    try {
                        DictDataDO dictData = dictDataService.getDictData(dictType, serviceItem.getServiceType());
                        if (dictData != null && dictData.getLabel() != null) {
                            serviceTypeName = dictData.getLabel();
                        } else {
                            serviceTypeName = serviceItem.getServiceType(); // 回退使用编码
                        }
                    } catch (Exception e) {
                        log.warn("【轮次通知】获取字典标签失败: dictType={}, value={}", dictType, serviceItem.getServiceType());
                        serviceTypeName = serviceItem.getServiceType(); // 回退使用编码
                    }
                } else {
                    log.warn("【轮次通知】未知的部门类型: deptType={}", deptType);
                    serviceTypeName = serviceItem.getServiceType(); // 回退使用编码
                }
            } else {
                log.warn("【轮次通知】服务项缺少部门类型: serviceItemId={}", serviceItem.getId());
                serviceTypeName = serviceItem.getServiceType(); // 回退使用编码
            }
        }

        // 构建通知内容
        String roundName = round.getName() != null ? round.getName() : "第" + round.getRoundNo() + "次执行";
        String title = "轮次任务开始通知";
        
        // 格式化时间
        String deadline = round.getDeadline() != null 
                ? round.getDeadline().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) 
                : "-";
        String endTime = round.getPlanEndTime() != null 
                ? round.getPlanEndTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) 
                : "-";
        
        String content = String.format("### %s\n\n" +
                "您有一个轮次任务已开始执行：\n\n" +
                "- **客户**：%s\n" +
                "- **服务类型**：%s\n" +
                "- **轮次**：%s\n" +
                "- **截止日期**：%s\n" +
                "- **计划结束**：%s\n\n" +
                "请及时处理！",
                title,
                customerName,
                serviceTypeName,
                roundName,
                deadline,
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
                templateParams.put("serviceTypeName", serviceTypeName);
                templateParams.put("roundName", roundName);
                templateParams.put("deadline", deadline);
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
            LocalDateTime deadline, LocalDateTime planEndTime) {
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
        round.setDeadline(deadline);
        round.setPlanEndTime(planEndTime);
        round.setStatus(0); // 待执行
        round.setProgress(0);
        projectRoundMapper.insert(round);

        // 注意：不在创建时计数，而是在点击"开始"时才增加已使用次数

        log.info("【服务执行】服务项 {} 创建了第 {} 轮执行，轮次ID: {}, 流程实例ID: {}, 截止日期: {}, 计划结束: {}",
                serviceItemId, newRoundNo, round.getId(), processInstanceId, deadline, planEndTime);

        return round.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRoundByServiceItem(Long serviceItemId, String processInstanceId,
            LocalDateTime deadline, LocalDateTime planEndTime,
            Boolean isOutside, Boolean isCrossDept, Long serviceLaunchId) {
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
        round.setDeadline(deadline);
        round.setPlanEndTime(planEndTime);
        round.setStatus(0); // 待执行
        round.setProgress(0);
        // 设置外出和跨部门标识
        round.setIsOutside(isOutside);
        round.setIsCrossDept(isCrossDept);
        round.setServiceLaunchId(serviceLaunchId);
        projectRoundMapper.insert(round);

        log.info("【服务执行】服务项 {} 创建了第 {} 轮执行，轮次ID: {}, 外出: {}, 跨部门: {}, 服务发起ID: {}",
                serviceItemId, newRoundNo, round.getId(), isOutside, isCrossDept, serviceLaunchId);

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

    @Override
    public void setExecutors(Long roundId, List<Long> executorIds) {
        if (roundId == null) {
            return;
        }

        ProjectRoundDO round = projectRoundMapper.selectById(roundId);
        if (round == null) {
            log.warn("【轮次执行人】轮次不存在，roundId={}", roundId);
            return;
        }

        // 获取执行人姓名
        String executorNames = "";
        if (CollUtil.isNotEmpty(executorIds)) {
            List<AdminUserRespDTO> users = adminUserApi.getUserList(executorIds);
            executorNames = users.stream()
                    .map(AdminUserRespDTO::getNickname)
                    .collect(Collectors.joining(","));
        }

        // 更新轮次执行人
        ProjectRoundDO updateObj = new ProjectRoundDO();
        updateObj.setId(roundId);
        updateObj.setExecutorIds(JSONUtil.toJsonStr(executorIds));
        updateObj.setExecutorNames(executorNames);
        projectRoundMapper.updateById(updateObj);

        log.info("【轮次执行人】设置成功，roundId={}, executorIds={}", roundId, executorIds);
    }

}
