package cn.shuhe.system.module.system.service.dingtalkrobot;

import cn.hutool.json.JSONUtil;
import cn.shuhe.system.framework.common.enums.CommonStatusEnum;
import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.common.util.object.BeanUtils;
import cn.shuhe.system.module.system.controller.admin.dingtalkrobot.vo.*;
import cn.shuhe.system.module.system.dal.dataobject.dingtalkrobot.DingtalkRobotDO;
import cn.shuhe.system.module.system.dal.dataobject.dingtalkrobot.DingtalkRobotMessageDO;
import cn.shuhe.system.module.system.dal.mysql.dingtalkrobot.DingtalkRobotMapper;
import cn.shuhe.system.module.system.dal.mysql.dingtalkrobot.DingtalkRobotMessageMapper;
import cn.shuhe.system.module.system.service.dingtalkconfig.DingtalkApiService;
import cn.shuhe.system.module.system.service.dingtalkconfig.DingtalkApiService.RobotSendResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static cn.shuhe.system.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.shuhe.system.module.system.enums.ErrorCodeConstants.*;

/**
 * 钉钉群机器人 Service 实现类
 *
 * @author shuhe
 */
@Slf4j
@Service
@Validated
public class DingtalkRobotServiceImpl implements DingtalkRobotService {

    @Resource
    private DingtalkRobotMapper dingtalkRobotMapper;

    @Resource
    private DingtalkRobotMessageMapper dingtalkRobotMessageMapper;

    @Resource
    private DingtalkApiService dingtalkApiService;

    // ==================== 机器人配置管理 ====================

    @Override
    public Long createDingtalkRobot(DingtalkRobotSaveReqVO createReqVO) {
        // 校验Webhook URL格式
        validateWebhookUrl(createReqVO.getWebhookUrl());
        
        // 插入
        DingtalkRobotDO robot = BeanUtils.toBean(createReqVO, DingtalkRobotDO.class);
        dingtalkRobotMapper.insert(robot);
        return robot.getId();
    }

    @Override
    public void updateDingtalkRobot(DingtalkRobotSaveReqVO updateReqVO) {
        // 校验存在
        validateDingtalkRobotExists(updateReqVO.getId());
        // 校验Webhook URL格式
        validateWebhookUrl(updateReqVO.getWebhookUrl());
        
        // 更新
        DingtalkRobotDO updateObj = BeanUtils.toBean(updateReqVO, DingtalkRobotDO.class);
        dingtalkRobotMapper.updateById(updateObj);
    }

    @Override
    public void deleteDingtalkRobot(Long id) {
        // 校验存在
        validateDingtalkRobotExists(id);
        // 删除
        dingtalkRobotMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDingtalkRobotListByIds(List<Long> ids) {
        // 校验存在
        ids.forEach(this::validateDingtalkRobotExists);
        // 删除
        dingtalkRobotMapper.deleteByIds(ids);
    }

    private void validateDingtalkRobotExists(Long id) {
        if (dingtalkRobotMapper.selectById(id) == null) {
            throw exception(DINGTALK_ROBOT_NOT_EXISTS);
        }
    }

    private void validateWebhookUrl(String webhookUrl) {
        if (webhookUrl == null || !webhookUrl.startsWith("https://oapi.dingtalk.com/robot/send")) {
            throw exception(DINGTALK_ROBOT_WEBHOOK_INVALID);
        }
    }

    @Override
    public DingtalkRobotDO getDingtalkRobot(Long id) {
        return dingtalkRobotMapper.selectById(id);
    }

    @Override
    public PageResult<DingtalkRobotDO> getDingtalkRobotPage(DingtalkRobotPageReqVO pageReqVO) {
        return dingtalkRobotMapper.selectPage(pageReqVO);
    }

    @Override
    public List<DingtalkRobotDO> getEnabledDingtalkRobotList() {
        return dingtalkRobotMapper.selectListByStatus(CommonStatusEnum.ENABLE.getStatus());
    }

    // ==================== 消息发送 ====================

    @Override
    public RobotSendResult sendTextMessage(Long robotId, String content,
                                           List<String> atMobiles, List<String> atUserIds,
                                           boolean isAtAll) {
        DingtalkRobotDO robot = getDingtalkRobotAndValidate(robotId);
        
        RobotSendResult result = dingtalkApiService.sendRobotTextMessage(
                robot, content, atMobiles, atUserIds, isAtAll);
        
        // 记录消息
        saveMessageRecord(robot, "text", content, atMobiles, atUserIds, isAtAll, result);
        
        return result;
    }

    @Override
    public RobotSendResult sendMarkdownMessage(Long robotId, String title, String text,
                                               List<String> atMobiles, List<String> atUserIds,
                                               boolean isAtAll) {
        DingtalkRobotDO robot = getDingtalkRobotAndValidate(robotId);
        
        RobotSendResult result = dingtalkApiService.sendRobotMarkdownMessage(
                robot, title, text, atMobiles, atUserIds, isAtAll);
        
        // 记录消息
        String content = JSONUtil.createObj()
                .put("title", title)
                .put("text", text)
                .toString();
        saveMessageRecord(robot, "markdown", content, atMobiles, atUserIds, isAtAll, result);
        
        return result;
    }

    @Override
    public RobotSendResult sendLinkMessage(Long robotId, String title, String text,
                                           String messageUrl, String picUrl) {
        DingtalkRobotDO robot = getDingtalkRobotAndValidate(robotId);
        
        RobotSendResult result = dingtalkApiService.sendRobotLinkMessage(
                robot, title, text, messageUrl, picUrl);
        
        // 记录消息
        String content = JSONUtil.createObj()
                .put("title", title)
                .put("text", text)
                .put("messageUrl", messageUrl)
                .put("picUrl", picUrl)
                .toString();
        saveMessageRecord(robot, "link", content, null, null, false, result);
        
        return result;
    }

    @Override
    public RobotSendResult sendActionCardMessage(Long robotId, String title, String text,
                                                 String singleTitle, String singleURL) {
        DingtalkRobotDO robot = getDingtalkRobotAndValidate(robotId);
        
        RobotSendResult result = dingtalkApiService.sendRobotActionCardMessage(
                robot, title, text, singleTitle, singleURL);
        
        // 记录消息
        String content = JSONUtil.createObj()
                .put("title", title)
                .put("text", text)
                .put("singleTitle", singleTitle)
                .put("singleURL", singleURL)
                .toString();
        saveMessageRecord(robot, "actionCard", content, null, null, false, result);
        
        return result;
    }

    @Override
    public RobotSendResult testRobotSend(Long robotId) {
        DingtalkRobotDO robot = getDingtalkRobotAndValidate(robotId);
        
        String testContent = "【测试消息】\n这是一条来自管理系统的测试消息，用于验证机器人配置是否正确。\n发送时间：" + 
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        return dingtalkApiService.sendRobotText(robot, testContent);
    }

    /**
     * 获取机器人配置并校验
     */
    private DingtalkRobotDO getDingtalkRobotAndValidate(Long robotId) {
        DingtalkRobotDO robot = dingtalkRobotMapper.selectById(robotId);
        if (robot == null) {
            throw exception(DINGTALK_ROBOT_NOT_EXISTS);
        }
        if (robot.getStatus() != null && robot.getStatus() == CommonStatusEnum.DISABLE.getStatus()) {
            throw exception(DINGTALK_ROBOT_DISABLED);
        }
        return robot;
    }

    /**
     * 保存消息发送记录
     */
    private void saveMessageRecord(DingtalkRobotDO robot, String msgType, String content,
                                   List<String> atMobiles, List<String> atUserIds,
                                   boolean isAtAll, RobotSendResult result) {
        try {
            DingtalkRobotMessageDO message = DingtalkRobotMessageDO.builder()
                    .robotId(robot.getId())
                    .msgType(msgType)
                    .content(content)
                    .atMobiles(atMobiles != null ? JSONUtil.toJsonStr(atMobiles) : null)
                    .atUserIds(atUserIds != null ? JSONUtil.toJsonStr(atUserIds) : null)
                    .isAtAll(isAtAll)
                    .sendStatus(result.isSuccess() ? 0 : 1)
                    .errorMsg(result.getErrmsg())
                    .responseData(result.getResponse())
                    .build();
            dingtalkRobotMessageMapper.insert(message);
        } catch (Exception e) {
            log.error("保存钉钉机器人消息记录失败", e);
        }
    }

    // ==================== 消息记录 ====================

    @Override
    public PageResult<DingtalkRobotMessageDO> getMessagePage(DingtalkRobotMessagePageReqVO pageReqVO) {
        return dingtalkRobotMessageMapper.selectPage(pageReqVO);
    }

}
