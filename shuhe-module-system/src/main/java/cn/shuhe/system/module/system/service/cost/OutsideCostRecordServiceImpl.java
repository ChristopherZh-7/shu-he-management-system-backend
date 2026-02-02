package cn.shuhe.system.module.system.service.cost;

import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.common.util.object.BeanUtils;
import cn.shuhe.system.framework.security.core.util.SecurityFrameworkUtils;
import cn.shuhe.system.module.system.controller.admin.cost.vo.*;
import cn.shuhe.system.module.system.dal.dataobject.cost.OutsideCostRecordDO;
import cn.shuhe.system.module.system.dal.mysql.cost.OutsideCostRecordMapper;
import cn.shuhe.system.module.system.dal.mysql.cost.OutsideRequestInfoMapper;
import cn.shuhe.system.module.system.dal.mysql.dept.DeptMapper;
import cn.shuhe.system.module.system.dal.mysql.user.AdminUserMapper;
import cn.shuhe.system.module.system.dal.dataobject.dept.DeptDO;
import cn.shuhe.system.module.system.dal.dataobject.user.AdminUserDO;
import cn.shuhe.system.module.system.api.dingtalk.DingtalkNotifyApi;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static cn.shuhe.system.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.shuhe.system.module.system.enums.ErrorCodeConstants.*;

/**
 * 外出费用记录 Service 实现类
 *
 * 流程：A部门找B部门要人 → B部门人员完成 → B部门负责人选择结算人 → 结算人填写金额
 */
@Service
@Slf4j
public class OutsideCostRecordServiceImpl implements OutsideCostRecordService {

    @Resource
    private OutsideCostRecordMapper outsideCostRecordMapper;

    @Resource
    private OutsideRequestInfoMapper outsideRequestInfoMapper;

    @Resource
    private cn.shuhe.system.module.system.dal.mysql.cost.ServiceLaunchInfoMapper serviceLaunchInfoMapper;

    @Resource
    private DeptMapper deptMapper;

    @Resource
    private AdminUserMapper adminUserMapper;

    @Resource
    private DingtalkNotifyApi dingtalkNotifyApi;

    @Resource
    private ContractAllocationService contractAllocationService;

    @Override
    public PageResult<OutsideCostRecordRespVO> getOutsideCostRecordPage(OutsideCostRecordPageReqVO reqVO) {
        PageResult<OutsideCostRecordDO> pageResult = outsideCostRecordMapper.selectPage(reqVO);
        
        // 转换并填充额外信息
        List<OutsideCostRecordRespVO> list = new ArrayList<>();
        for (OutsideCostRecordDO record : pageResult.getList()) {
            OutsideCostRecordRespVO respVO = BeanUtils.toBean(record, OutsideCostRecordRespVO.class);
            // 根据记录类型查询额外信息
            fillExtraInfo(respVO, record);
            list.add(respVO);
        }
        
        return new PageResult<>(list, pageResult.getTotal());
    }

    /**
     * 填充额外信息（外出申请或服务发起）
     */
    private void fillExtraInfo(OutsideCostRecordRespVO respVO, OutsideCostRecordDO record) {
        // 优先从统一服务发起获取信息
        if (record.getServiceLaunchId() != null) {
            Map<String, Object> launchInfo = serviceLaunchInfoMapper.selectServiceLaunchInfo(record.getServiceLaunchId());
            if (launchInfo != null) {
                respVO.setDestination((String) launchInfo.get("destination"));
                respVO.setReason((String) launchInfo.get("reason"));
                respVO.setPlanStartTime((LocalDateTime) launchInfo.get("planStartTime"));
                respVO.setPlanEndTime((LocalDateTime) launchInfo.get("planEndTime"));
                // 如果合同名称为空，从关联数据中动态获取
                if (respVO.getContractName() == null || respVO.getContractName().isEmpty()) {
                    respVO.setContractName((String) launchInfo.get("contractName"));
                }
                // 填充服务类型和部门类型（用于前端字典转换）
                if (respVO.getServiceType() == null || respVO.getServiceType().isEmpty()) {
                    respVO.setServiceType((String) launchInfo.get("serviceType"));
                }
                if (respVO.getDeptType() == null) {
                    respVO.setDeptType(getIntegerValue(launchInfo.get("deptType")));
                }
                return;
            }
        }
        
        // 兼容旧版：从外出申请获取信息
        if (record.getOutsideRequestId() != null) {
            Map<String, Object> requestInfo = outsideRequestInfoMapper.selectOutsideRequestInfo(record.getOutsideRequestId());
            if (requestInfo != null) {
                respVO.setDestination((String) requestInfo.get("destination"));
                respVO.setReason((String) requestInfo.get("reason"));
                respVO.setPlanStartTime((LocalDateTime) requestInfo.get("planStartTime"));
                respVO.setPlanEndTime((LocalDateTime) requestInfo.get("planEndTime"));
                // 如果合同名称为空，从关联数据中动态获取
                if (respVO.getContractName() == null || respVO.getContractName().isEmpty()) {
                    respVO.setContractName((String) requestInfo.get("contractName"));
                }
                // 填充服务类型和部门类型（用于前端字典转换）
                if (respVO.getServiceType() == null || respVO.getServiceType().isEmpty()) {
                    respVO.setServiceType((String) requestInfo.get("serviceType"));
                }
                if (respVO.getDeptType() == null) {
                    respVO.setDeptType(getIntegerValue(requestInfo.get("deptType")));
                }
            }
        }
    }

    @Override
    public OutsideCostRecordRespVO getOutsideCostRecord(Long id) {
        OutsideCostRecordDO record = outsideCostRecordMapper.selectById(id);
        if (record == null) {
            throw exception(OUTSIDE_COST_RECORD_NOT_EXISTS);
        }
        
        OutsideCostRecordRespVO respVO = BeanUtils.toBean(record, OutsideCostRecordRespVO.class);
        
        // 填充额外信息（外出申请或服务发起）
        fillExtraInfo(respVO, record);
        
        // 填充合同分配相关信息
        fillContractAllocationInfo(respVO, record);
        
        return respVO;
    }

    /**
     * 填充合同分配相关信息
     */
    private void fillContractAllocationInfo(OutsideCostRecordRespVO respVO, OutsideCostRecordDO record) {
        if (record.getContractId() == null) {
            return;
        }
        
        try {
            // 1. 获取合同分配详情
            ContractAllocationDetailRespVO allocationDetail = contractAllocationService.getContractAllocationDetail(record.getContractId());
            if (allocationDetail != null) {
                respVO.setContractTotalAmount(allocationDetail.getTotalAmount());
                respVO.setContractAllocatedAmount(allocationDetail.getAllocatedAmount());
                
                // 2. 构建部门分配列表
                if (allocationDetail.getDeptAllocations() != null && !allocationDetail.getDeptAllocations().isEmpty()) {
                    List<OutsideCostRecordRespVO.DeptAllocationInfo> deptList = new ArrayList<>();
                    for (ContractDeptAllocationRespVO deptAllocation : allocationDetail.getDeptAllocations()) {
                        OutsideCostRecordRespVO.DeptAllocationInfo info = new OutsideCostRecordRespVO.DeptAllocationInfo();
                        info.setDeptId(deptAllocation.getDeptId());
                        info.setDeptName(deptAllocation.getDeptName());
                        info.setAllocatedAmount(deptAllocation.getAllocatedAmount());
                        // 标记是否为发起部门或目标部门
                        info.setIsRequestDept(record.getRequestDeptId() != null && 
                            record.getRequestDeptId().equals(deptAllocation.getDeptId()));
                        info.setIsTargetDept(record.getTargetDeptId() != null && 
                            record.getTargetDeptId().equals(deptAllocation.getDeptId()));
                        deptList.add(info);
                    }
                    respVO.setDeptAllocations(deptList);
                }
            }
            
            // 3. 查询该合同已产生的跨部门费用统计
            BigDecimal costTotal = sumAmountByContractId(record.getContractId());
            Integer costCount = countByContractId(record.getContractId());
            respVO.setContractOutsideCostTotal(costTotal != null ? costTotal : BigDecimal.ZERO);
            respVO.setContractOutsideCostCount(costCount != null ? costCount : 0);
            
        } catch (Exception e) {
            log.warn("填充合同分配信息失败，contractId={}", record.getContractId(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignSettleUser(OutsideCostAssignReqVO reqVO) {
        // 校验记录存在
        OutsideCostRecordDO record = outsideCostRecordMapper.selectById(reqVO.getId());
        if (record == null) {
            throw exception(OUTSIDE_COST_RECORD_NOT_EXISTS);
        }
        
        // 校验状态（必须是待指派结算人状态）
        if (record.getStatus() != OutsideCostRecordDO.STATUS_PENDING_ASSIGN) {
            throw exception(OUTSIDE_COST_STATUS_ERROR);
        }
        
        // 获取结算人信息
        AdminUserDO settleUser = adminUserMapper.selectById(reqVO.getSettleUserId());
        if (settleUser == null) {
            throw exception(OUTSIDE_COST_SETTLE_USER_NOT_EXISTS);
        }
        
        String settleDeptName = null;
        if (settleUser.getDeptId() != null) {
            DeptDO settleDept = deptMapper.selectById(settleUser.getDeptId());
            if (settleDept != null) {
                settleDeptName = settleDept.getName();
            }
        }
        
        // 获取当前操作人信息（B部门负责人）
        Long assignUserId = SecurityFrameworkUtils.getLoginUserId();
        AdminUserDO assignUser = adminUserMapper.selectById(assignUserId);
        
        // 更新记录
        OutsideCostRecordDO updateObj = new OutsideCostRecordDO();
        updateObj.setId(reqVO.getId());
        updateObj.setSettleUserId(reqVO.getSettleUserId());
        updateObj.setSettleUserName(settleUser.getNickname());
        updateObj.setSettleDeptId(settleUser.getDeptId());
        updateObj.setSettleDeptName(settleDeptName);
        updateObj.setAssignUserId(assignUserId);
        updateObj.setAssignUserName(assignUser != null ? assignUser.getNickname() : null);
        updateObj.setAssignTime(LocalDateTime.now());
        updateObj.setStatus(OutsideCostRecordDO.STATUS_PENDING_FILL);
        
        outsideCostRecordMapper.updateById(updateObj);
        
        log.info("【外出费用】指派结算人成功，recordId={}, settleUserId={}, assignUserId={}", 
                reqVO.getId(), reqVO.getSettleUserId(), assignUserId);

        // 发送钉钉通知给结算人
        try {
            sendFillNotifyToSettleUser(record, settleUser, settleDeptName);
        } catch (Exception e) {
            log.error("【外出费用】发送通知给结算人失败，recordId={}", reqVO.getId(), e);
        }
    }

    /**
     * 发送通知给结算人填写金额
     */
    private void sendFillNotifyToSettleUser(OutsideCostRecordDO record, AdminUserDO settleUser, String settleDeptName) {
        Map<String, Object> requestInfo = outsideRequestInfoMapper.selectOutsideRequestInfo(record.getOutsideRequestId());
        String destination = requestInfo != null ? (String) requestInfo.get("destination") : "";
        String reason = requestInfo != null ? (String) requestInfo.get("reason") : "";

        String title = "外出费用待填写";
        String content = String.format(
                "### 外出费用待填写金额\n\n" +
                "**合同编号**：%s\n\n" +
                "**服务项**：%s\n\n" +
                "**目标部门**：%s\n\n" +
                "**外出地点**：%s\n\n" +
                "**外出事由**：%s\n\n" +
                "您被指派为此次外出的费用结算人，请填写费用金额。",
                record.getContractNo(),
                record.getServiceItemName(),
                record.getTargetDeptName(),
                destination,
                reason
        );

        dingtalkNotifyApi.sendActionCardMessage(
                Collections.singletonList(settleUser.getId()),
                title,
                content,
                "立即填写",
                "/cost-management/outside-cost"
        );

        log.info("【外出费用】发送填写通知成功，recordId={}, settleUserId={}", record.getId(), settleUser.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void fillOutsideCost(OutsideCostFillReqVO reqVO) {
        // 校验记录存在
        OutsideCostRecordDO record = outsideCostRecordMapper.selectById(reqVO.getId());
        if (record == null) {
            throw exception(OUTSIDE_COST_RECORD_NOT_EXISTS);
        }
        
        // 校验状态（必须是待填写金额状态）
        if (record.getStatus() != OutsideCostRecordDO.STATUS_PENDING_FILL) {
            throw exception(OUTSIDE_COST_STATUS_ERROR);
        }
        
        // 获取当前填写人信息（结算人）
        Long fillUserId = SecurityFrameworkUtils.getLoginUserId();
        AdminUserDO fillUser = adminUserMapper.selectById(fillUserId);
        
        // 更新记录
        OutsideCostRecordDO updateObj = new OutsideCostRecordDO();
        updateObj.setId(reqVO.getId());
        updateObj.setAmount(reqVO.getAmount());
        updateObj.setFillUserId(fillUserId);
        updateObj.setFillUserName(fillUser != null ? fillUser.getNickname() : null);
        updateObj.setFillTime(LocalDateTime.now());
        updateObj.setStatus(OutsideCostRecordDO.STATUS_COMPLETED);
        updateObj.setRemark(reqVO.getRemark());
        
        outsideCostRecordMapper.updateById(updateObj);
        
        log.info("【外出费用】填写金额成功，recordId={}, amount={}, fillUserId={}", 
                reqVO.getId(), reqVO.getAmount(), fillUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOutsideCostRecord(Long outsideRequestId) {
        // 检查是否已存在记录
        OutsideCostRecordDO existing = outsideCostRecordMapper.selectByOutsideRequestId(outsideRequestId);
        if (existing != null) {
            return existing.getId();
        }
        
        // 查询外出申请信息
        Map<String, Object> requestInfo = outsideRequestInfoMapper.selectOutsideRequestInfo(outsideRequestId);
        if (requestInfo == null) {
            log.warn("外出申请不存在: {}", outsideRequestId);
            return null;
        }
        
        // 检查是否有合同关联
        Long contractId = getLongValue(requestInfo.get("contractId"));
        if (contractId == null) {
            log.warn("外出申请未关联合同: {}", outsideRequestId);
            return null;
        }
        
        // 检查是否为跨部门（发起部门 ≠ 目标部门）
        Long requestDeptId = getLongValue(requestInfo.get("requestDeptId"));
        Long targetDeptId = getLongValue(requestInfo.get("targetDeptId"));
        if (requestDeptId != null && requestDeptId.equals(targetDeptId)) {
            log.info("非跨部门外出，不创建费用记录: outsideRequestId={}, deptId={}", outsideRequestId, requestDeptId);
            return null;
        }
        
        // 创建外出费用记录
        OutsideCostRecordDO record = new OutsideCostRecordDO();
        record.setOutsideRequestId(outsideRequestId);
        record.setContractId(contractId);
        record.setContractNo((String) requestInfo.get("contractNo"));
        record.setContractName((String) requestInfo.get("contractName"));
        record.setServiceItemId(getLongValue(requestInfo.get("serviceItemId")));
        record.setServiceItemName((String) requestInfo.get("serviceItemName"));
        record.setRequestDeptId(requestDeptId);
        record.setRequestDeptName((String) requestInfo.get("requestDeptName"));
        record.setRequestUserId(getLongValue(requestInfo.get("requestUserId")));
        record.setRequestUserName((String) requestInfo.get("requestUserName"));
        record.setTargetDeptId(targetDeptId);
        record.setTargetDeptName((String) requestInfo.get("targetDeptName"));
        record.setStatus(OutsideCostRecordDO.STATUS_PENDING_ASSIGN);
        
        outsideCostRecordMapper.insert(record);
        
        return record.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public Long createCostRecordByServiceLaunch(Long serviceLaunchId) {
        // 检查是否已存在记录
        OutsideCostRecordDO existing = outsideCostRecordMapper.selectByServiceLaunchId(serviceLaunchId);
        if (existing != null) {
            log.info("【跨部门费用】记录已存在，serviceLaunchId={}, recordId={}", serviceLaunchId, existing.getId());
            return existing.getId();
        }
        
        // 查询服务发起信息
        Map<String, Object> launchInfo = serviceLaunchInfoMapper.selectServiceLaunchInfo(serviceLaunchId);
        if (launchInfo == null) {
            log.warn("【跨部门费用】服务发起不存在: {}", serviceLaunchId);
            return null;
        }
        
        // 检查是否有合同关联
        Long contractId = getLongValue(launchInfo.get("contractId"));
        if (contractId == null) {
            log.warn("【跨部门费用】服务发起未关联合同: {}", serviceLaunchId);
            return null;
        }
        
        // 检查是否为跨部门
        Object isCrossDeptObj = launchInfo.get("isCrossDept");
        boolean isCrossDept = false;
        if (isCrossDeptObj instanceof Boolean) {
            isCrossDept = (Boolean) isCrossDeptObj;
        } else if (isCrossDeptObj instanceof Number) {
            isCrossDept = ((Number) isCrossDeptObj).intValue() == 1;
        }
        
        if (!isCrossDept) {
            log.info("【跨部门费用】非跨部门服务，不创建费用记录: serviceLaunchId={}", serviceLaunchId);
            return null;
        }
        
        // 获取部门信息
        // 跨部门费用的正确逻辑：
        // - 支出部门（requestDeptId）= 请求方部门（考虑代发起后的实际部门，谁提出需求谁付钱）
        // - 收入部门（targetDeptId）= 执行部门（谁干活谁收钱）
        // 
        // 业务场景示例：
        // - A部门的人代B部门发起服务，让A部门执行
        // - 请求方部门 = B（代发起人的部门，实际的业务归属方）
        // - 执行部门 = A（被借调来干活的部门）
        // - 费用记录：B部门支出，A部门收入
        Long requestDeptId = getLongValue(launchInfo.get("requestDeptId"));
        Long executeDeptId = getLongValue(launchInfo.get("executeDeptId"));
        
        // 创建跨部门费用记录
        OutsideCostRecordDO record = new OutsideCostRecordDO();
        record.setServiceLaunchId(serviceLaunchId);
        record.setContractId(contractId);
        record.setContractNo((String) launchInfo.get("contractNo"));
        record.setContractName((String) launchInfo.get("contractName"));
        record.setServiceItemId(getLongValue(launchInfo.get("serviceItemId")));
        record.setServiceItemName((String) launchInfo.get("serviceItemName"));
        record.setServiceType((String) launchInfo.get("serviceType"));
        record.setDeptType(getIntegerValue(launchInfo.get("deptType")));
        // 支出部门 = 请求方部门（考虑代发起后的实际部门）
        record.setRequestDeptId(requestDeptId);
        record.setRequestDeptName((String) launchInfo.get("requestDeptName"));
        record.setRequestUserId(getLongValue(launchInfo.get("requestUserId")));
        record.setRequestUserName((String) launchInfo.get("requestUserName"));
        // 收入部门 = 执行部门（被借调来干活的部门）
        record.setTargetDeptId(executeDeptId);
        record.setTargetDeptName((String) launchInfo.get("executeDeptName"));
        record.setStatus(OutsideCostRecordDO.STATUS_PENDING_ASSIGN);
        
        outsideCostRecordMapper.insert(record);
        
        log.info("【跨部门费用】创建费用记录成功，serviceLaunchId={}, recordId={}, 支出部门={}({}), 收入部门={}({})",
                serviceLaunchId, record.getId(), 
                launchInfo.get("requestDeptName"), requestDeptId,
                launchInfo.get("executeDeptName"), executeDeptId);
        
        return record.getId();
    }

    @Override
    public List<SettleUserVO> getSettleUserList(Long outsideCostRecordId) {
        OutsideCostRecordDO record = outsideCostRecordMapper.selectById(outsideCostRecordId);
        if (record == null) {
            return Collections.emptyList();
        }
        
        List<SettleUserVO> result = new ArrayList<>();
        Set<Long> addedUserIds = new HashSet<>();
        
        // 1. 添加发起人（默认选项）- A部门的发起人
        Long requestUserId = record.getRequestUserId();
        if (requestUserId != null) {
            AdminUserDO requestUser = adminUserMapper.selectById(requestUserId);
            if (requestUser != null) {
                String deptName = null;
                if (requestUser.getDeptId() != null) {
                    DeptDO dept = deptMapper.selectById(requestUser.getDeptId());
                    if (dept != null) {
                        deptName = dept.getName();
                    }
                }
                result.add(new SettleUserVO(
                        requestUser.getId(),
                        requestUser.getNickname(),
                        requestUser.getDeptId(),
                        deptName,
                        true,
                        "发起人"
                ));
                addedUserIds.add(requestUser.getId());
            }
        }
        
        // 2. 添加各部门主管
        List<DeptDO> allDepts = deptMapper.selectList();
        for (DeptDO dept : allDepts) {
            if (dept.getLeaderUserId() != null && !addedUserIds.contains(dept.getLeaderUserId())) {
                AdminUserDO leader = adminUserMapper.selectById(dept.getLeaderUserId());
                if (leader != null) {
                    result.add(new SettleUserVO(
                            leader.getId(),
                            leader.getNickname(),
                            dept.getId(),
                            dept.getName(),
                            false,
                            dept.getName() + "主管"
                    ));
                    addedUserIds.add(leader.getId());
                }
            }
        }
        
        return result;
    }

    @Override
    public BigDecimal sumAmountByContractId(Long contractId) {
        return outsideCostRecordMapper.sumAmountByContractId(contractId);
    }

    @Override
    public Integer countByContractId(Long contractId) {
        return outsideCostRecordMapper.countByContractId(contractId);
    }

    private Long getLongValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer getIntegerValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
