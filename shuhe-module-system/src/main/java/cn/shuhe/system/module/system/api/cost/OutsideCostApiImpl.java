package cn.shuhe.system.module.system.api.cost;

import cn.shuhe.system.module.system.dal.dataobject.cost.OutsideCostRecordDO;
import cn.shuhe.system.module.system.dal.dataobject.dept.DeptDO;
import cn.shuhe.system.module.system.dal.mysql.cost.OutsideCostRecordMapper;
import cn.shuhe.system.module.system.dal.mysql.cost.OutsideRequestInfoMapper;
import cn.shuhe.system.module.system.dal.mysql.dept.DeptMapper;
import cn.shuhe.system.module.system.service.cost.OutsideCostRecordService;
import cn.shuhe.system.module.system.api.dingtalk.DingtalkNotifyApi;

import java.util.Collections;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 外出费用 API 实现类
 *
 * 流程：A部门找B部门要人 → B部门人员完成 → B部门负责人选择结算人 → 结算人填写金额
 */
@Service
@Slf4j
public class OutsideCostApiImpl implements OutsideCostApi {

    @Resource
    private OutsideCostRecordService outsideCostRecordService;

    @Resource
    private OutsideCostRecordMapper outsideCostRecordMapper;

    @Resource
    private OutsideRequestInfoMapper outsideRequestInfoMapper;

    @Resource
    private DeptMapper deptMapper;

    @Resource
    private DingtalkNotifyApi dingtalkNotifyApi;

    @Override
    public Long createOutsideCostRecord(Long outsideRequestId) {
        try {
            Long recordId = outsideCostRecordService.createOutsideCostRecord(outsideRequestId);
            log.info("【外出费用】创建外出费用记录成功，outsideRequestId={}, recordId={}", outsideRequestId, recordId);
            return recordId;
        } catch (Exception e) {
            log.error("【外出费用】创建外出费用记录失败，outsideRequestId={}", outsideRequestId, e);
            return null;
        }
    }

    @Override
    public Long createCostRecordByServiceLaunch(Long serviceLaunchId) {
        try {
            Long recordId = outsideCostRecordService.createCostRecordByServiceLaunch(serviceLaunchId);
            log.info("【跨部门费用】通过服务发起创建费用记录成功，serviceLaunchId={}, recordId={}", serviceLaunchId, recordId);
            return recordId;
        } catch (Exception e) {
            log.error("【跨部门费用】通过服务发起创建费用记录失败，serviceLaunchId={}", serviceLaunchId, e);
            return null;
        }
    }

    @Override
    public void sendAssignNotifyToTargetDeptLeader(Long outsideRequestId) {
        try {
            // 查询外出费用记录
            OutsideCostRecordDO record = outsideCostRecordMapper.selectByOutsideRequestId(outsideRequestId);
            if (record == null) {
                log.warn("【外出费用】外出费用记录不存在，无法发送通知，outsideRequestId={}", outsideRequestId);
                return;
            }

            // 查询目标部门信息（B部门）
            Long targetDeptId = record.getTargetDeptId();
            if (targetDeptId == null) {
                log.warn("【外出费用】目标部门ID为空，无法发送通知，outsideRequestId={}", outsideRequestId);
                return;
            }

            DeptDO targetDept = deptMapper.selectById(targetDeptId);
            if (targetDept == null || targetDept.getLeaderUserId() == null) {
                log.warn("【外出费用】目标部门或部门负责人不存在，无法发送通知，targetDeptId={}", targetDeptId);
                return;
            }

            // 查询外出申请详情
            Map<String, Object> requestInfo = outsideRequestInfoMapper.selectOutsideRequestInfo(outsideRequestId);
            String destination = requestInfo != null ? (String) requestInfo.get("destination") : "";
            String reason = requestInfo != null ? (String) requestInfo.get("reason") : "";

            // 发送钉钉通知给目标部门负责人（B部门负责人）
            String title = "外出费用待指派";
            String content = String.format(
                    "### 外出费用待指派结算人\n\n" +
                    "**合同编号**：%s\n\n" +
                    "**服务项**：%s\n\n" +
                    "**发起部门**：%s\n\n" +
                    "**发起人**：%s\n\n" +
                    "**外出地点**：%s\n\n" +
                    "**外出事由**：%s\n\n" +
                    "外出已完成，请指派结算人（找谁要钱）。",
                    record.getContractNo(),
                    record.getServiceItemName(),
                    record.getRequestDeptName(),
                    record.getRequestUserName(),
                    destination,
                    reason
            );

            dingtalkNotifyApi.sendActionCardMessage(
                    Collections.singletonList(targetDept.getLeaderUserId()),
                    title,
                    content,
                    "立即指派",
                    "/cost-management/outside-cost"
            );

            log.info("【外出费用】发送指派通知成功，outsideRequestId={}, targetDeptLeaderId={}",
                    outsideRequestId, targetDept.getLeaderUserId());

        } catch (Exception e) {
            log.error("【外出费用】发送指派通知失败，outsideRequestId={}", outsideRequestId, e);
        }
    }

    @Override
    public void sendFillNotifyToSettleUser(Long outsideCostRecordId) {
        try {
            // 查询外出费用记录
            OutsideCostRecordDO record = outsideCostRecordMapper.selectById(outsideCostRecordId);
            if (record == null) {
                log.warn("【外出费用】外出费用记录不存在，无法发送通知，recordId={}", outsideCostRecordId);
                return;
            }

            // 获取结算人
            Long settleUserId = record.getSettleUserId();
            if (settleUserId == null) {
                log.warn("【外出费用】结算人为空，无法发送通知，recordId={}", outsideCostRecordId);
                return;
            }

            // 查询外出申请详情
            Map<String, Object> requestInfo = outsideRequestInfoMapper.selectOutsideRequestInfo(record.getOutsideRequestId());
            String destination = requestInfo != null ? (String) requestInfo.get("destination") : "";
            String reason = requestInfo != null ? (String) requestInfo.get("reason") : "";

            // 发送钉钉通知给结算人
            String title = "外出费用待填写";
            String content = String.format(
                    "### 外出费用待填写金额\n\n" +
                    "**合同编号**：%s\n\n" +
                    "**服务项**：%s\n\n" +
                    "**目标部门**：%s\n\n" +
                    "**指派人**：%s\n\n" +
                    "**外出地点**：%s\n\n" +
                    "**外出事由**：%s\n\n" +
                    "您被指派为此次外出的费用结算人，请填写费用金额。",
                    record.getContractNo(),
                    record.getServiceItemName(),
                    record.getTargetDeptName(),
                    record.getAssignUserName(),
                    destination,
                    reason
            );

            dingtalkNotifyApi.sendActionCardMessage(
                    Collections.singletonList(settleUserId),
                    title,
                    content,
                    "立即填写",
                    "/cost-management/outside-cost"
            );

            log.info("【外出费用】发送填写通知成功，recordId={}, settleUserId={}",
                    outsideCostRecordId, settleUserId);

        } catch (Exception e) {
            log.error("【外出费用】发送填写通知失败，recordId={}", outsideCostRecordId, e);
        }
    }
}
