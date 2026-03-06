package cn.shuhe.system.module.crm.service.business.dingtalk;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.shuhe.system.module.crm.dal.dataobject.business.CrmBusinessDO;
import cn.shuhe.system.module.crm.dal.mysql.business.CrmBusinessMapper;
import cn.shuhe.system.module.crm.service.business.CrmBusinessServiceImpl;
import cn.shuhe.system.module.infra.event.dingtalk.DingtalkRobotMessageEvent;
import cn.shuhe.system.module.system.api.dingtalk.DingtalkNotifyApi;
import cn.shuhe.system.module.system.dal.dataobject.dingtalkmapping.DingtalkMappingDO;
import cn.shuhe.system.module.system.dal.mysql.dingtalkmapping.DingtalkMappingMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 商机钉钉群聊指令处理
 * <p>
 * 监听钉钉机器人消息事件，处理商机群内的指令：
 * - "确认" -> 审批通过
 * - "修改\n部门名:金额\n部门名:金额" -> 更新分配金额
 *
 * @author shuhe
 */
@Slf4j
@Service
public class BusinessChatCommandService {

    @Resource
    private CrmBusinessMapper businessMapper;
    @Resource
    private CrmBusinessServiceImpl businessService;
    @Resource
    private DingtalkMappingMapper dingtalkMappingMapper;

    @EventListener
    public void handleCommand(DingtalkRobotMessageEvent event) {
        String chatId = event.getChatId();
        String senderDingtalkUserId = event.getSenderDingtalkUserId();
        String content = event.getContent();

        // 1. 根据 chatId 找到对应的商机
        List<CrmBusinessDO> businesses = businessMapper.selectList(
                new LambdaQueryWrapper<CrmBusinessDO>().eq(CrmBusinessDO::getDingtalkChatId, chatId));
        if (CollUtil.isEmpty(businesses)) {
            log.warn("[handleCommand] 未找到chatId={}对应的商机", chatId);
            return;
        }
        CrmBusinessDO business = businesses.get(0);

        // 2. 验证发送者身份（可选，总经办负责人才能确认）
        Long senderLocalUserId = getLocalUserIdByDingtalkId(senderDingtalkUserId);

        // 3. 解析指令
        if (content.startsWith("确认") || content.startsWith("确定") || content.startsWith("通过")) {
            handleConfirm(business, senderLocalUserId);
        } else if (content.startsWith("修改")) {
            handleModify(business, content, senderLocalUserId);
        } else {
            log.debug("[handleCommand] 未识别的指令: {}", content);
        }
    }

    private void handleConfirm(CrmBusinessDO business, Long senderUserId) {
        log.info("[handleConfirm] 商机审批确认: businessId={}, userId={}", business.getId(), senderUserId);
        businessService.updateBusinessAuditStatus(business.getId(), 2); // 2=审批通过
    }

    private void handleModify(CrmBusinessDO business, String content, Long senderUserId) {
        log.info("[handleModify] 商机金额修改: businessId={}, content={}", business.getId(), content);

        List<CrmBusinessDO.DeptAllocation> currentAllocations = business.getDeptAllocations();
        if (CollUtil.isEmpty(currentAllocations)) {
            log.warn("[handleModify] 商机没有部门分配数据");
            return;
        }

        // 解析修改内容，格式: "修改\n部门名:金额\n部门名:金额"
        String[] lines = content.split("\n");
        List<CrmBusinessDO.DeptAllocation> newAllocations = new ArrayList<>(currentAllocations);
        boolean hasChange = false;

        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (StrUtil.isEmpty(line)) continue;

            String[] parts = line.split("[:：]");
            if (parts.length != 2) continue;

            String deptName = parts[0].trim();
            try {
                BigDecimal amount = new BigDecimal(parts[1].trim());
                for (CrmBusinessDO.DeptAllocation allocation : newAllocations) {
                    if (allocation.getDeptName() != null && allocation.getDeptName().contains(deptName)) {
                        allocation.setAmount(amount);
                        hasChange = true;
                    }
                }
            } catch (NumberFormatException e) {
                log.warn("[handleModify] 无法解析金额: {}", line);
            }
        }

        if (hasChange) {
            businessService.updateDeptAllocations(business.getId(), newAllocations);
            log.info("[handleModify] 商机金额修改成功: businessId={}", business.getId());
        }
    }

    private Long getLocalUserIdByDingtalkId(String dingtalkUserId) {
        if (StrUtil.isEmpty(dingtalkUserId)) {
            return null;
        }
        List<DingtalkMappingDO> mappings = dingtalkMappingMapper.selectList(
                new LambdaQueryWrapper<DingtalkMappingDO>()
                        .eq(DingtalkMappingDO::getDingtalkId, dingtalkUserId)
                        .eq(DingtalkMappingDO::getType, "USER"));
        return CollUtil.isNotEmpty(mappings) ? mappings.get(0).getLocalId() : null;
    }

}
