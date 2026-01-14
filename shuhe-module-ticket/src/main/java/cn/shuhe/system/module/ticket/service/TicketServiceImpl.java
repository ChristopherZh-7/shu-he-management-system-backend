package cn.shuhe.system.module.ticket.service;

import cn.hutool.core.util.StrUtil;
import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.common.util.object.BeanUtils;
import cn.shuhe.system.module.system.dal.dataobject.dingtalkconfig.DingtalkConfigDO;
import cn.shuhe.system.module.system.dal.dataobject.dingtalkmapping.DingtalkMappingDO;
import cn.shuhe.system.module.system.dal.dataobject.user.AdminUserDO;
import cn.shuhe.system.module.system.dal.mysql.dingtalkmapping.DingtalkMappingMapper;
import cn.shuhe.system.module.system.dal.mysql.user.AdminUserMapper;
import cn.shuhe.system.module.system.service.dingtalkconfig.DingtalkApiService;
import cn.shuhe.system.module.system.service.dingtalkconfig.DingtalkConfigService;
import cn.shuhe.system.module.ticket.controller.admin.vo.TicketPageReqVO;
import cn.shuhe.system.module.ticket.controller.admin.vo.TicketSaveReqVO;
import cn.shuhe.system.module.ticket.dal.dataobject.TicketDO;
import cn.shuhe.system.module.ticket.dal.dataobject.TicketLogDO;
import cn.shuhe.system.module.ticket.dal.mysql.TicketLogMapper;
import cn.shuhe.system.module.ticket.dal.mysql.TicketMapper;
import cn.shuhe.system.module.ticket.enums.TicketStatusEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static cn.shuhe.system.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.shuhe.system.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.shuhe.system.module.ticket.enums.ErrorCodeConstants.*;

/**
 * 工单 Service 实现类
 */
@Slf4j
@Service
@Validated
public class TicketServiceImpl implements TicketService {

    @Resource
    private TicketMapper ticketMapper;
    
    @Resource
    private TicketLogMapper ticketLogMapper;

    @Resource
    private AdminUserMapper adminUserMapper;

    @Resource
    private DingtalkMappingMapper dingtalkMappingMapper;

    @Resource
    private DingtalkConfigService dingtalkConfigService;

    @Resource
    private DingtalkApiService dingtalkApiService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTicket(TicketSaveReqVO createReqVO) {
        // 生成工单编号
        String ticketNo = generateTicketNo();
        
        // 创建工单
        TicketDO ticket = BeanUtils.toBean(createReqVO, TicketDO.class);
        ticket.setTicketNo(ticketNo);
        ticket.setStatus(TicketStatusEnum.PENDING.getStatus());
        ticket.setCreatorId(getLoginUserId());
        ticketMapper.insert(ticket);
        
        // 记录日志
        createLog(ticket.getId(), "create", "创建工单");
        
        return ticket.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTicket(TicketSaveReqVO updateReqVO) {
        // 校验存在
        validateTicketExists(updateReqVO.getId());
        
        // 更新
        TicketDO updateObj = BeanUtils.toBean(updateReqVO, TicketDO.class);
        ticketMapper.updateById(updateObj);
        
        // 记录日志
        createLog(updateReqVO.getId(), "update", "更新工单信息");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTicket(Long id) {
        // 校验存在
        validateTicketExists(id);
        // 删除
        ticketMapper.deleteById(id);
    }

    @Override
    public TicketDO getTicket(Long id) {
        return ticketMapper.selectById(id);
    }

    @Override
    public PageResult<TicketDO> getTicketPage(TicketPageReqVO pageReqVO) {
        return ticketMapper.selectPage(pageReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignTicket(Long id, Long assigneeId) {
        // 校验存在
        TicketDO ticket = validateTicketExists(id);
        
        // 更新状态和处理人
        TicketDO updateObj = new TicketDO();
        updateObj.setId(id);
        updateObj.setAssigneeId(assigneeId);
        updateObj.setStatus(TicketStatusEnum.ASSIGNED.getStatus());
        ticketMapper.updateById(updateObj);
        
        // 记录日志
        AdminUserDO assignee = adminUserMapper.selectById(assigneeId);
        String assigneeName = assignee != null ? assignee.getNickname() : String.valueOf(assigneeId);
        createLog(id, "assign", "分配给: " + assigneeName);
        
        // 发送钉钉通知（同步执行，方便调试）
        try {
            sendDingtalkNotify(ticket, assigneeId, "assign");
        } catch (Exception e) {
            log.error("发送钉钉通知失败", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startProcess(Long id) {
        // 校验存在
        TicketDO ticket = validateTicketExists(id);
        
        // 更新状态
        TicketDO updateObj = new TicketDO();
        updateObj.setId(id);
        updateObj.setStatus(TicketStatusEnum.PROCESSING.getStatus());
        ticketMapper.updateById(updateObj);
        
        // 记录日志
        createLog(id, "process", "开始处理工单");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finishTicket(Long id, String remark) {
        // 校验存在
        TicketDO ticket = validateTicketExists(id);
        
        // 更新状态
        TicketDO updateObj = new TicketDO();
        updateObj.setId(id);
        updateObj.setStatus(TicketStatusEnum.COMPLETED.getStatus());
        updateObj.setFinishTime(LocalDateTime.now());
        if (remark != null) {
            updateObj.setRemark(remark);
        }
        ticketMapper.updateById(updateObj);
        
        // 记录日志
        createLog(id, "finish", "完成工单" + (remark != null ? "：" + remark : ""));
        
        // 通知工单创建人
        if (ticket.getCreatorId() != null) {
            try {
                sendDingtalkNotify(ticket, ticket.getCreatorId(), "finish");
            } catch (Exception e) {
                log.error("发送钉钉通知失败", e);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeTicket(Long id, String remark) {
        // 校验存在
        validateTicketExists(id);
        
        // 更新状态
        TicketDO updateObj = new TicketDO();
        updateObj.setId(id);
        updateObj.setStatus(TicketStatusEnum.CLOSED.getStatus());
        ticketMapper.updateById(updateObj);
        
        // 记录日志
        createLog(id, "close", "关闭工单" + (remark != null ? "：" + remark : ""));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelTicket(Long id, String remark) {
        // 校验存在
        validateTicketExists(id);
        
        // 更新状态
        TicketDO updateObj = new TicketDO();
        updateObj.setId(id);
        updateObj.setStatus(TicketStatusEnum.CANCELLED.getStatus());
        ticketMapper.updateById(updateObj);
        
        // 记录日志
        createLog(id, "cancel", "取消工单" + (remark != null ? "：" + remark : ""));
    }

    /**
     * 校验工单是否存在
     */
    private TicketDO validateTicketExists(Long id) {
        TicketDO ticket = ticketMapper.selectById(id);
        if (ticket == null) {
            throw exception(TICKET_NOT_EXISTS);
        }
        return ticket;
    }

    /**
     * 生成工单编号
     */
    private String generateTicketNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        // 简单实现，实际可以用Redis自增
        long timestamp = System.currentTimeMillis() % 10000;
        return "TK" + dateStr + String.format("%04d", timestamp);
    }

    /**
     * 创建操作日志
     */
    private void createLog(Long ticketId, String action, String content) {
        TicketLogDO logDO = TicketLogDO.builder()
                .ticketId(ticketId)
                .action(action)
                .content(content)
                .operatorId(getLoginUserId())
                .build();
        ticketLogMapper.insert(logDO);
    }

    /**
     * 发送钉钉通知
     *
     * @param ticket 工单
     * @param userId 接收人用户ID
     * @param action 操作类型：assign-分配, finish-完成
     */
    public void sendDingtalkNotify(TicketDO ticket, Long userId, String action) {
        log.info("【钉钉通知】开始发送，工单号={}, 用户ID={}, 操作={}", ticket.getTicketNo(), userId, action);
        
        // 获取用户的钉钉ID映射
        DingtalkMappingDO mapping = dingtalkMappingMapper.selectByLocalId(userId, "USER");
        if (mapping == null || StrUtil.isEmpty(mapping.getDingtalkId())) {
            log.warn("【钉钉通知】用户 {} 没有钉钉映射，跳过钉钉通知", userId);
            return;
        }
        log.info("【钉钉通知】用户钉钉ID={}", mapping.getDingtalkId());

        // 获取钉钉配置
        List<DingtalkConfigDO> configs = dingtalkConfigService.getEnabledDingtalkConfigList();
        if (configs.isEmpty()) {
            log.warn("【钉钉通知】没有可用的钉钉配置，跳过钉钉通知");
            return;
        }
        DingtalkConfigDO config = configs.get(0);
        log.info("【钉钉通知】使用配置：name={}, agentId={}", config.getName(), config.getAgentId());

        if (StrUtil.isEmpty(config.getAgentId())) {
            log.warn("【钉钉通知】钉钉配置缺少agentId，跳过钉钉通知");
            return;
        }

        // 获取 access_token
        String accessToken = dingtalkApiService.getAccessToken(config);
        log.info("【钉钉通知】获取accessToken成功");

        // 构建消息内容
        String title;
        String content;
        if ("assign".equals(action)) {
            title = "您有新的工单待处理";
            content = String.format(
                    "### 📋 %s\n\n" +
                    "**工单编号：** %s\n\n" +
                    "**工单标题：** %s\n\n" +
                    "**详细描述：**\n%s\n\n" +
                    "---\n" +
                    "请及时登录系统处理",
                    title,
                    ticket.getTicketNo(),
                    ticket.getTitle(),
                    StrUtil.isNotEmpty(ticket.getDescription()) ? ticket.getDescription() : "无"
            );
        } else if ("finish".equals(action)) {
            title = "您的工单已完成";
            content = String.format(
                    "### ✅ %s\n\n" +
                    "**工单编号：** %s\n\n" +
                    "**工单标题：** %s\n\n" +
                    "---\n" +
                    "工单已处理完成，请登录系统查看",
                    title,
                    ticket.getTicketNo(),
                    ticket.getTitle()
            );
        } else {
            log.warn("【钉钉通知】未知操作类型：{}", action);
            return;
        }

        // 发送钉钉工作通知
        log.info("【钉钉通知】准备发送消息，title={}", title);
        boolean success = dingtalkApiService.sendWorkNotice(
                accessToken,
                config.getAgentId(),
                mapping.getDingtalkId(),
                title,
                content
        );

        if (success) {
            log.info("【钉钉通知】发送成功：ticketNo={}, userId={}, dingtalkId={}", 
                    ticket.getTicketNo(), userId, mapping.getDingtalkId());
        } else {
            log.error("【钉钉通知】发送失败：ticketNo={}, userId={}", ticket.getTicketNo(), userId);
        }
    }

    @Override
    public List<TicketLogDO> getTicketLogs(Long ticketId) {
        return ticketLogMapper.selectListByTicketId(ticketId);
    }

}
