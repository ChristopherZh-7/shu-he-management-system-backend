package cn.shuhe.system.module.crm.service.contract;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjUtil;
import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.common.util.object.BeanUtils;
import cn.shuhe.system.framework.common.util.object.ObjectUtils;
import cn.shuhe.system.module.bpm.api.task.BpmProcessInstanceApi;
import cn.shuhe.system.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.shuhe.system.module.crm.controller.admin.contract.vo.contract.CrmContractPageReqVO;
import cn.shuhe.system.module.crm.controller.admin.contract.vo.contract.CrmContractSaveReqVO;
import cn.shuhe.system.module.crm.controller.admin.contract.vo.contract.CrmContractTransferReqVO;
import cn.shuhe.system.module.crm.dal.dataobject.contract.CrmContractConfigDO;
import cn.shuhe.system.module.crm.dal.dataobject.contract.CrmContractDO;
import cn.shuhe.system.module.crm.dal.mysql.contract.CrmContractMapper;
import cn.shuhe.system.module.crm.dal.redis.no.CrmNoRedisDAO;
import cn.shuhe.system.module.crm.enums.common.CrmAuditStatusEnum;
import cn.shuhe.system.module.crm.enums.common.CrmBizTypeEnum;
import cn.shuhe.system.module.crm.enums.permission.CrmPermissionLevelEnum;
import cn.shuhe.system.module.crm.framework.permission.core.annotations.CrmPermission;
import cn.shuhe.system.module.crm.dal.dataobject.business.CrmBusinessDO;
import cn.shuhe.system.module.crm.service.business.CrmBusinessService;
import cn.shuhe.system.module.crm.service.contact.CrmContactService;
import cn.shuhe.system.module.crm.service.customer.CrmCustomerService;
import cn.shuhe.system.module.crm.service.permission.CrmPermissionService;
import cn.shuhe.system.module.crm.service.permission.bo.CrmPermissionCreateReqBO;
import cn.shuhe.system.module.crm.service.permission.bo.CrmPermissionTransferReqBO;
import cn.shuhe.system.module.crm.service.receivable.CrmReceivableService;
import cn.shuhe.system.module.system.api.dept.DeptApi;
import cn.shuhe.system.module.system.api.user.AdminUserApi;
import cn.shuhe.system.module.system.api.user.dto.AdminUserRespDTO;
import cn.shuhe.system.module.system.service.dingtalkconfig.DingtalkApiService;
import cn.shuhe.system.module.system.service.dingtalkconfig.DingtalkConfigService;
import cn.shuhe.system.module.system.service.dingtalkrobot.event.DingtalkNotificationEventPublisher;
import cn.shuhe.system.module.system.dal.mysql.dingtalkmapping.DingtalkMappingMapper;
import cn.shuhe.system.module.project.service.ProjectService;
import cn.shuhe.system.module.crm.dal.mysql.customer.CrmCustomerMapper;
import com.mzt.logapi.context.LogRecordContext;
import com.mzt.logapi.service.impl.DiffParseFunction;
import com.mzt.logapi.starter.annotation.LogRecord;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static cn.shuhe.system.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.shuhe.system.framework.common.util.collection.CollectionUtils.*;
import static cn.shuhe.system.module.crm.enums.ErrorCodeConstants.*;
import static cn.shuhe.system.module.crm.enums.LogRecordConstants.*;
import static cn.shuhe.system.module.crm.util.CrmAuditStatusUtils.convertBpmResultToAuditStatus;

/**
 * CRM 合同 Service 实现类
 */
@Service
@Validated
@Slf4j
public class CrmContractServiceImpl implements CrmContractService {

    public static final String BPM_PROCESS_DEFINITION_KEY = "crm-contract-audit";

    @Resource
    private CrmContractMapper contractMapper;
    @Resource
    private CrmNoRedisDAO noRedisDAO;
    @Resource
    private CrmPermissionService crmPermissionService;
    @Resource
    @Lazy
    private CrmCustomerService customerService;
    @Resource
    private CrmBusinessService businessService;
    @Resource
    @Lazy
    private CrmContactService contactService;
    @Resource
    private CrmContractConfigService contractConfigService;
    @Resource
    @Lazy
    private CrmReceivableService receivableService;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;
    @Resource
    private BpmProcessInstanceApi bpmProcessInstanceApi;
    @Resource
    private DingtalkApiService dingtalkApiService;
    @Resource
    private DingtalkConfigService dingtalkConfigService;
    @Resource
    private cn.shuhe.system.module.system.api.dingtalk.DingtalkNotifyApi dingtalkNotifyApi;
    @Resource
    private DingtalkMappingMapper dingtalkMappingMapper;
    @Resource
    private DingtalkNotificationEventPublisher dingtalkNotificationEventPublisher;
    @Resource
    @Lazy
    private ProjectService projectService;
    @Resource
    @Lazy
    private cn.shuhe.system.module.project.service.ServiceItemService serviceItemService;
    @Resource
    @Lazy
    private cn.shuhe.system.module.project.service.ProjectDeptServiceService projectDeptServiceService;
    @Resource
    private CrmCustomerMapper customerMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = CRM_CONTRACT_TYPE, subType = CRM_CONTRACT_CREATE_SUB_TYPE, bizNo = "{{#contract.id}}", success = CRM_CONTRACT_CREATE_SUCCESS)
    public Long createContract(CrmContractSaveReqVO createReqVO, Long userId) {
        // 1.1 校验关联字段
        validateRelationDataExists(createReqVO);
        // 1.2 生成序号
        String no = noRedisDAO.generate(CrmNoRedisDAO.CONTRACT_NO_PREFIX);
        if (contractMapper.selectByNo(no) != null) {
            throw exception(CONTRACT_NO_EXISTS);
        }

        // 2. 插入合同
        CrmContractDO contract = BeanUtils.toBean(createReqVO, CrmContractDO.class).setNo(no);
        if (contract.getOwnerUserId() == null) {
            contract.setOwnerUserId(userId);
        }
        // 校验：关联商机时，若提前投入审批进行中则不允许创建合同
        if (contract.getBusinessId() != null) {
            CrmBusinessDO linkedBusiness = businessService.getBusiness(contract.getBusinessId());
            if (linkedBusiness != null
                    && CrmAuditStatusEnum.PROCESS.getStatus().equals(linkedBusiness.getEarlyInvestmentStatus())) {
                throw exception(CONTRACT_CREATE_FAIL_EARLY_INVESTMENT_PROCESSING);
            }
        }
        // 所有合同创建后均为草稿，需手动发起审批
        contract.setAuditStatus(CrmAuditStatusEnum.DRAFT.getStatus());
        contractMapper.insert(contract);

        // 3. 创建数据权限
        crmPermissionService.createPermission(new CrmPermissionCreateReqBO().setUserId(contract.getOwnerUserId())
                .setBizType(CrmBizTypeEnum.CRM_CONTRACT.getType()).setBizId(contract.getId())
                .setLevel(CrmPermissionLevelEnum.OWNER.getLevel()));

        LogRecordContext.putVariable("contract", contract);
        return contract.getId();
    }

    /**
     * 创建合同后自动将关联商机标记为赢单
     */
    private void markBusinessAsWin(Long businessId) {
        try {
            CrmBusinessDO business = businessService.validateBusiness(businessId);
            if (business == null || business.getEndStatus() != null) {
                return;
            }
            if (!cn.shuhe.system.module.crm.enums.common.CrmAuditStatusEnum.APPROVE.getStatus()
                    .equals(business.getAuditStatus())) {
                return;
            }
            businessService.updateBusinessStatus(
                    new cn.shuhe.system.module.crm.controller.admin.business.vo.business.CrmBusinessUpdateStatusReqVO()
                            .setId(businessId)
                            .setEndStatus(cn.shuhe.system.module.crm.enums.business.CrmBusinessEndStatusEnum.WIN.getStatus())
                            .setEndRemark("合同已签单确认，自动标记赢单")
            );
            log.info("[markBusinessAsWin] businessId={}", businessId);
        } catch (Exception e) {
            log.warn("[markBusinessAsWin] failed, businessId={}: {}", businessId, e.getMessage());
        }
    }

    /**
     * 签单成功，通知关联商机群（纯通知，无需审批操作）
     */
    private void notifyBusinessGroupOnContractSigned(CrmContractDO contract) {
        try {
            List<cn.shuhe.system.module.crm.dal.dataobject.business.CrmBusinessDO.DeptAllocation> bizAllocations = null;
            if (contract.getBusinessId() != null) {
                cn.shuhe.system.module.crm.dal.dataobject.business.CrmBusinessDO biz =
                        businessService.validateBusiness(contract.getBusinessId());
                if (biz != null) {
                    bizAllocations = biz.getDeptAllocations();
                }
            }

            StringBuilder msg = new StringBuilder();
            msg.append("🎉 **合同已签单，项目正式开始！**\n\n");
            msg.append("合同名称：").append(contract.getName()).append("\n\n");
            msg.append("合同编号：").append(contract.getNo()).append("\n\n");
            if (contract.getTotalPrice() != null) {
                msg.append("合同金额：").append(String.format("%,.2f", contract.getTotalPrice())).append(" 元\n\n");
            }

            appendAllocationComparison(msg, bizAllocations, contract.getDeptAllocations());

            businessService.sendBusinessGroupNotification(
                    contract.getBusinessId(), msg.toString(), "合同签单通知");
        } catch (Exception e) {
            log.warn("[notifyBusinessGroupOnContractSigned] failed, contractId={}: {}",
                    contract.getId(), e.getMessage());
        }
    }

    /**
     * 生成部门金额分配对比文字
     */
    private void appendAllocationComparison(
            StringBuilder msg,
            List<cn.shuhe.system.module.crm.dal.dataobject.business.CrmBusinessDO.DeptAllocation> bizAllocations,
            List<cn.shuhe.system.module.crm.dal.dataobject.business.CrmBusinessDO.DeptAllocation> contractAllocations) {

        boolean hasBiz      = CollUtil.isNotEmpty(bizAllocations);
        boolean hasContract = CollUtil.isNotEmpty(contractAllocations);

        if (!hasBiz && !hasContract) {
            return;
        }

        msg.append("**部门金额分配**：\n\n");

        if (hasBiz && !hasContract) {
            msg.append("（沿用商机分配）\n\n");
            for (var a : bizAllocations) {
                msg.append("- ").append(a.getDeptName()).append("：")
                        .append(String.format("%,.2f", a.getAmount())).append(" 元\n\n");
            }
            return;
        }

        java.util.Map<Long, java.math.BigDecimal> bizMap = new java.util.HashMap<>();
        if (hasBiz) {
            for (var a : bizAllocations) {
                if (a.getDeptId() != null && a.getAmount() != null) {
                    bizMap.put(a.getDeptId(), a.getAmount());
                }
            }
        }

        for (var ca : contractAllocations) {
            if (ca.getDeptName() == null || ca.getAmount() == null) continue;
            java.math.BigDecimal oldAmt = bizMap.get(ca.getDeptId());
            if (oldAmt == null) {
                msg.append("- ").append(ca.getDeptName()).append("：")
                        .append(String.format("%,.2f", ca.getAmount())).append(" 元（新增）\n\n");
            } else if (oldAmt.compareTo(ca.getAmount()) == 0) {
                msg.append("- ").append(ca.getDeptName()).append("：")
                        .append(String.format("%,.2f", ca.getAmount())).append(" 元（不变）\n\n");
            } else {
                java.math.BigDecimal diff = ca.getAmount().subtract(oldAmt);
                String arrow = diff.compareTo(java.math.BigDecimal.ZERO) > 0 ? "↑" : "↓";
                msg.append("- ").append(ca.getDeptName()).append("：")
                        .append(String.format("%,.2f", oldAmt)).append(" → ")
                        .append(String.format("%,.2f", ca.getAmount())).append(" 元 ")
                        .append(arrow).append(String.format("%,.2f", diff.abs())).append("\n\n");
            }
            bizMap.remove(ca.getDeptId());
        }

        if (hasBiz) {
            for (var a : bizAllocations) {
                if (a.getDeptId() != null && bizMap.containsKey(a.getDeptId())) {
                    msg.append("- ").append(a.getDeptName()).append("：")
                            .append(String.format("%,.2f", a.getAmount())).append(" 元（已移除）\n\n");
                }
            }
        }
    }

    /**
     * 合同审批通过后，通知关联商机群
     */
    private void notifyBusinessGroupOnContractApproved(CrmContractDO contract) {
        if (contract.getBusinessId() == null) {
            return;
        }
        try {
            String msg = "✅ **合同签单已确认**\n\n" +
                    "合同名称：" + contract.getName() + "\n\n" +
                    "合同编号：" + contract.getNo() + "\n\n" +
                    (contract.getTotalPrice() != null
                            ? "合同金额：" + String.format("%,.2f", contract.getTotalPrice()) + " 元\n\n"
                            : "") +
                    "合同已正式生效，继续服务！";
            businessService.sendBusinessGroupNotification(contract.getBusinessId(), msg, "合同签单确认");
        } catch (Exception e) {
            log.warn("[notifyBusinessGroupOnContractApproved] failed, contractId={}: {}",
                    contract.getId(), e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = CRM_CONTRACT_TYPE, subType = CRM_CONTRACT_UPDATE_SUB_TYPE, bizNo = "{{#updateReqVO.id}}", success = CRM_CONTRACT_UPDATE_SUCCESS)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CONTRACT, bizId = "#updateReqVO.id", level = CrmPermissionLevelEnum.WRITE)
    public void updateContract(CrmContractSaveReqVO updateReqVO) {
        Assert.notNull(updateReqVO.getId(), "合同编号不能为空");
        updateReqVO.setOwnerUserId(null);
        // 1.1 校验存在
        CrmContractDO oldContract = validateContractExists(updateReqVO.getId());
        // 1.2 只有草稿、审批中，可以编辑
        if (!ObjectUtils.equalsAny(oldContract.getAuditStatus(), CrmAuditStatusEnum.DRAFT.getStatus(),
                CrmAuditStatusEnum.PROCESS.getStatus())) {
            throw exception(CONTRACT_UPDATE_FAIL_NOT_DRAFT);
        }
        // 1.3 校验关联字段
        validateRelationDataExists(updateReqVO);

        // 2. 更新合同
        CrmContractDO updateObj = BeanUtils.toBean(updateReqVO, CrmContractDO.class);
        contractMapper.updateById(updateObj);

        updateReqVO.setOwnerUserId(oldContract.getOwnerUserId());
        LogRecordContext.putVariable(DiffParseFunction.OLD_OBJECT,
                BeanUtils.toBean(oldContract, CrmContractSaveReqVO.class));
        LogRecordContext.putVariable("contractName", oldContract.getName());
    }

    private void validateRelationDataExists(CrmContractSaveReqVO reqVO) {
        if (reqVO.getCustomerId() != null) {
            customerService.validateCustomer(reqVO.getCustomerId());
        }
        if (reqVO.getOwnerUserId() != null) {
            adminUserApi.validateUser(reqVO.getOwnerUserId());
        }
        if (reqVO.getBusinessId() != null) {
            businessService.validateBusiness(reqVO.getBusinessId());
        }
        if (reqVO.getSignContactId() != null) {
            contactService.validateContact(reqVO.getSignContactId());
        }
        if (reqVO.getSignUserId() != null) {
            adminUserApi.validateUser(reqVO.getSignUserId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = CRM_CONTRACT_TYPE, subType = CRM_CONTRACT_DELETE_SUB_TYPE, bizNo = "{{#id}}", success = CRM_CONTRACT_DELETE_SUCCESS)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CONTRACT, bizId = "#id", level = CrmPermissionLevelEnum.OWNER)
    public void deleteContract(Long id) {
        // 1.1 校验存在
        CrmContractDO contract = validateContractExists(id);
        // 1.2 如果被 CrmReceivableDO 所使用，则不允许删除
        if (receivableService.getReceivableCountByContractId(contract.getId()) > 0) {
            throw exception(CONTRACT_DELETE_FAIL);
        }

        // 2. 级联删除关联的服务项
        List<cn.shuhe.system.module.project.dal.dataobject.ServiceItemDO> serviceItems =
                serviceItemService.getServiceItemListByContractId(id);
        if (CollUtil.isNotEmpty(serviceItems)) {
            for (cn.shuhe.system.module.project.dal.dataobject.ServiceItemDO serviceItem : serviceItems) {
                serviceItemService.deleteServiceItem(serviceItem.getId());
            }
            log.info("[deleteContract] deleted {} service items for contract {}", serviceItems.size(), id);
        }

        // 3.1 删除合同
        contractMapper.deleteById(id);
        // 3.2 删除数据权限
        crmPermissionService.deletePermission(CrmBizTypeEnum.CRM_CONTRACT.getType(), id);

        LogRecordContext.putVariable("contractName", contract.getName());
    }

    private CrmContractDO validateContractExists(Long id) {
        CrmContractDO contract = contractMapper.selectById(id);
        if (contract == null) {
            throw exception(CONTRACT_NOT_EXISTS);
        }
        return contract;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = CRM_CONTRACT_TYPE, subType = CRM_CONTRACT_TRANSFER_SUB_TYPE, bizNo = "{{#reqVO.id}}", success = CRM_CONTRACT_TRANSFER_SUCCESS)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CONTRACT, bizId = "#reqVO.id", level = CrmPermissionLevelEnum.OWNER)
    public void transferContract(CrmContractTransferReqVO reqVO, Long userId) {
        CrmContractDO contract = validateContractExists(reqVO.getId());

        crmPermissionService
                .transferPermission(new CrmPermissionTransferReqBO(userId, CrmBizTypeEnum.CRM_CONTRACT.getType(),
                        reqVO.getId(), reqVO.getNewOwnerUserId(), reqVO.getOldOwnerPermissionLevel()));
        contractMapper.updateById(new CrmContractDO().setId(reqVO.getId()).setOwnerUserId(reqVO.getNewOwnerUserId()));

        LogRecordContext.putVariable("contract", contract);
    }

    @Override
    @LogRecord(type = CRM_CONTRACT_TYPE, subType = CRM_CONTRACT_FOLLOW_UP_SUB_TYPE, bizNo = "{{#id}}", success = CRM_CONTRACT_FOLLOW_UP_SUCCESS)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CONTRACT, bizId = "#id", level = CrmPermissionLevelEnum.WRITE)
    public void updateContractFollowUp(Long id, LocalDateTime contactNextTime, String contactLastContent) {
        CrmContractDO contract = validateContractExists(id);
        contractMapper.updateById(new CrmContractDO().setId(id).setContactLastTime(LocalDateTime.now()));
        LogRecordContext.putVariable("contractName", contract.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = CRM_CONTRACT_TYPE, subType = CRM_CONTRACT_SUBMIT_SUB_TYPE, bizNo = "{{#id}}", success = CRM_CONTRACT_SUBMIT_SUCCESS)
    public void submitContract(Long id, Long userId) {
        // 1. 校验合同是否在草稿状态
        CrmContractDO contract = validateContractExists(id);
        if (ObjUtil.notEqual(contract.getAuditStatus(), CrmAuditStatusEnum.DRAFT.getStatus())) {
            throw exception(CONTRACT_SUBMIT_FAIL_NOT_DRAFT);
        }

        // 2. 构建流程变量（供审批人在工作流页面查看合同摘要）
        java.util.Map<String, Object> variables = new java.util.HashMap<>();
        // 逐级审批链路（复用商机审批的链路计算逻辑）
        List<Long> approvalChain = businessService.calculateApprovalChain(userId);
        variables.put("approvalChain", approvalChain);
        variables.put("name", contract.getName());
        variables.put("no", contract.getNo());
        variables.put("totalPrice", contract.getTotalPrice() != null ? contract.getTotalPrice().toPlainString() : "");
        // 最终客户名称
        if (contract.getCustomerId() != null) {
            try {
                cn.shuhe.system.module.crm.dal.dataobject.customer.CrmCustomerDO customer =
                        customerMapper.selectById(contract.getCustomerId());
                variables.put("customerName", customer != null ? customer.getName() : "");
            } catch (Exception e) {
                variables.put("customerName", "");
            }
        }
        // 负责人姓名
        if (contract.getOwnerUserId() != null) {
            try {
                cn.shuhe.system.module.system.api.user.dto.AdminUserRespDTO owner =
                        adminUserApi.getUser(contract.getOwnerUserId());
                variables.put("ownerUserName", owner != null ? owner.getNickname() : "");
            } catch (Exception e) {
                variables.put("ownerUserName", "");
            }
        }
        // 部门分配明细
        variables.put("deptAllocationsText", buildContractDeptAllocationsText(contract.getDeptAllocations()));

        // 3. 发起 BPM 合同审批流程
        String processInstanceId = bpmProcessInstanceApi.createProcessInstance(userId,
                new BpmProcessInstanceCreateReqDTO()
                        .setProcessDefinitionKey(BPM_PROCESS_DEFINITION_KEY)
                        .setBusinessKey(String.valueOf(id))
                        .setVariables(variables));

        // 4. 更新合同状态为审批中
        contractMapper.updateById(new CrmContractDO().setId(id)
                .setProcessInstanceId(processInstanceId)
                .setAuditStatus(CrmAuditStatusEnum.PROCESS.getStatus()));

        // 5. 群通知：签合同审批已提交
        if (contract.getBusinessId() != null) {
            try {
                StringBuilder msg = new StringBuilder();
                msg.append("## ⏳ 签合同审批已提交\n\n");
                msg.append("**合同名称：** ").append(contract.getName()).append("\n\n");
                msg.append("**合同编号：** ").append(contract.getNo()).append("\n\n");
                if (contract.getTotalPrice() != null) {
                    msg.append("**合同金额：** ¥")
                            .append(String.format("%,.2f", contract.getTotalPrice()))
                            .append(" 元\n\n");
                }
                if (CollUtil.isNotEmpty(contract.getDeptAllocations())) {
                    msg.append("**部门分配：**\n\n");
                    for (var a : contract.getDeptAllocations()) {
                        msg.append("- ").append(a.getDeptName()).append("：")
                                .append(String.format("%,.2f", a.getAmount())).append(" 元\n\n");
                    }
                }
                msg.append("---\n\n");
                msg.append("> 申请正在逐级审批中，请等待审批结果。");
                businessService.sendBusinessGroupNotification(
                        contract.getBusinessId(), msg.toString(), "签合同审批已提交");
            } catch (Exception e) {
                log.warn("[submitContract] 发送群通知失败, contractId={}: {}", id, e.getMessage());
            }
        }

        LogRecordContext.putVariable("contractName", contract.getName());
    }

    /**
     * 构建合同部门分配明细文本（供 BPM 审批页展示）
     */
    private String buildContractDeptAllocationsText(
            List<cn.shuhe.system.module.crm.dal.dataobject.business.CrmBusinessDO.DeptAllocation> allocations) {
        if (CollUtil.isEmpty(allocations)) {
            return "（未分配）";
        }
        StringBuilder sb = new StringBuilder();
        for (var alloc : allocations) {
            if (sb.length() > 0) sb.append("\n");
            String deptName = alloc.getDeptName() != null ? alloc.getDeptName() : "未知部门";
            String amountStr = alloc.getAmount() != null ? "¥" + alloc.getAmount().toPlainString() : "-";
            String leaderName = "（未设置）";
            if (alloc.getDeptId() != null) {
                try {
                    cn.shuhe.system.module.system.api.dept.dto.DeptRespDTO dept = deptApi.getDept(alloc.getDeptId());
                    if (dept != null && dept.getLeaderUserId() != null) {
                        cn.shuhe.system.module.system.api.user.dto.AdminUserRespDTO leader =
                                adminUserApi.getUser(dept.getLeaderUserId());
                        if (leader != null) leaderName = leader.getNickname();
                    }
                } catch (Exception ignored) {
                }
            }
            sb.append(deptName).append("  ").append(amountStr).append("  负责人：").append(leaderName);
        }
        return sb.toString();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateContractAuditStatus(Long id, Integer bpmResult, java.util.Map<String, Object> processVariables) {
        CrmContractDO contract = validateContractExists(id);
        if (ObjUtil.notEqual(contract.getAuditStatus(), CrmAuditStatusEnum.PROCESS.getStatus())) {
            log.error("[updateContractAuditStatus] contract({}) not in PROCESS status, bpmResult={}",
                    contract.getId(), bpmResult);
            throw exception(CONTRACT_UPDATE_AUDIT_STATUS_FAIL_NOT_PROCESS);
        }

        Integer auditStatus = convertBpmResultToAuditStatus(bpmResult);
        contractMapper.updateById(new CrmContractDO().setId(id).setAuditStatus(auditStatus));

        if (CrmAuditStatusEnum.APPROVE.getStatus().equals(auditStatus)) {
            log.info("[updateContractAuditStatus] contract {} approved", contract.getNo());
            publishContractAuditPassEvent(contract);
            // 审批通过后执行签单下游操作
            if (contract.getBusinessId() != null) {
                markBusinessAsWin(contract.getBusinessId());
                notifyBusinessGroupOnContractSigned(contract);
                try {
                    businessService.createProjectFromBusiness(
                            contract.getBusinessId(), contract.getId(), contract.getNo());
                    // 合同自身的部门分配优先于商机的分配，覆盖同步
                    if (CollUtil.isNotEmpty(contract.getDeptAllocations())) {
                        String customerName = "";
                        if (contract.getCustomerId() != null) {
                            try {
                                cn.shuhe.system.module.crm.dal.dataobject.customer.CrmCustomerDO c =
                                        customerMapper.selectById(contract.getCustomerId());
                                if (c != null) customerName = c.getName();
                            } catch (Exception ignored) {}
                        }
                        businessService.syncContractDeptAllocations(
                                contract.getId(), contract.getNo(), customerName,
                                contract.getDeptAllocations());
                    }
                } catch (Exception e) {
                    log.error("[updateContractAuditStatus] 创建项目失败, businessId={}, contractId={}: {}",
                            contract.getBusinessId(), contract.getId(), e.getMessage(), e);
                }
            } else {
                notifyBusinessGroupOnContractApproved(contract);
            }
        } else if (CrmAuditStatusEnum.REJECT.getStatus().equals(auditStatus)) {
            // 审批驳回：群通知
            if (contract.getBusinessId() != null) {
                try {
                    String msg = "❌ **签合同审批被驳回**\n\n" +
                            "合同名称：" + contract.getName() + "\n\n" +
                            "合同编号：" + contract.getNo() + "\n\n" +
                            "请修改合同信息后重新提交审批。";
                    businessService.sendBusinessGroupNotification(
                            contract.getBusinessId(), msg, "签合同审批被驳回");
                } catch (Exception e) {
                    log.warn("[updateContractAuditStatus] 发送驳回群通知失败, contractId={}: {}", id, e.getMessage());
                }
            }
        }
    }

    @Override
    public void approveContractByDingtalk(Long id) {
        CrmContractDO contract = validateContractExists(id);
        if (!cn.shuhe.system.module.crm.enums.common.CrmAuditStatusEnum.PROCESS.getStatus()
                .equals(contract.getAuditStatus())) {
            throw exception(CONTRACT_UPDATE_AUDIT_STATUS_FAIL_NOT_PROCESS);
        }
        contractMapper.updateById(new CrmContractDO().setId(id)
                .setAuditStatus(cn.shuhe.system.module.crm.enums.common.CrmAuditStatusEnum.APPROVE.getStatus()));
        log.info("[approveContractByDingtalk] contract {} approved via dingtalk", contract.getNo());
        if (contract.getBusinessId() != null) {
            markBusinessAsWin(contract.getBusinessId());
            notifyBusinessGroupOnContractSigned(contract);
            try {
                businessService.createProjectFromBusiness(
                        contract.getBusinessId(), contract.getId(), contract.getNo());
                if (CollUtil.isNotEmpty(contract.getDeptAllocations())) {
                    String customerName = "";
                    if (contract.getCustomerId() != null) {
                        try {
                            cn.shuhe.system.module.crm.dal.dataobject.customer.CrmCustomerDO c =
                                    customerMapper.selectById(contract.getCustomerId());
                            if (c != null) customerName = c.getName();
                        } catch (Exception ignored) {}
                    }
                    businessService.syncContractDeptAllocations(
                            contract.getId(), contract.getNo(), customerName,
                            contract.getDeptAllocations());
                }
            } catch (Exception e) {
                log.error("[approveContractByDingtalk] 创建项目失败, businessId={}, contractId={}: {}",
                        contract.getBusinessId(), contract.getId(), e.getMessage(), e);
            }
        } else {
            notifyBusinessGroupOnContractApproved(contract);
        }
    }

    @Override
    public void rejectContractByDingtalk(Long id) {
        CrmContractDO contract = validateContractExists(id);
        if (!cn.shuhe.system.module.crm.enums.common.CrmAuditStatusEnum.PROCESS.getStatus()
                .equals(contract.getAuditStatus())) {
            throw exception(CONTRACT_UPDATE_AUDIT_STATUS_FAIL_NOT_PROCESS);
        }
        contractMapper.updateById(new CrmContractDO().setId(id)
                .setAuditStatus(cn.shuhe.system.module.crm.enums.common.CrmAuditStatusEnum.REJECT.getStatus()));
        log.info("[rejectContractByDingtalk] contract {} rejected via dingtalk", contract.getNo());
        if (contract.getBusinessId() != null) {
            try {
                String msg = "❌ **签单申请被驳回**\n\n" +
                        "合同名称：" + contract.getName() + "\n\n" +
                        "合同编号：" + contract.getNo() + "\n\n" +
                        "请修改合同后重新提交签单申请。";
                businessService.sendBusinessGroupNotification(contract.getBusinessId(), msg, "签单申请已驳回");
            } catch (Exception e) {
                log.warn("[rejectContractByDingtalk] notify failed: {}", e.getMessage());
            }
        }
    }

    private void publishContractAuditPassEvent(CrmContractDO contract) {
        try {
            String customerName = "";
            if (contract.getCustomerId() != null) {
                var customer = customerMapper.selectById(contract.getCustomerId());
                if (customer != null) {
                    customerName = customer.getName();
                }
            }

            String ownerUserName = "";
            if (contract.getOwnerUserId() != null) {
                AdminUserRespDTO owner = adminUserApi.getUser(contract.getOwnerUserId());
                if (owner != null) {
                    ownerUserName = owner.getNickname();
                }
            }

            java.util.Map<String, Object> variables = new java.util.HashMap<>();
            variables.put("name", contract.getName());
            variables.put("no", contract.getNo());
            variables.put("totalPrice", contract.getTotalPrice());
            variables.put("customerName", customerName);
            variables.put("ownerUserName", ownerUserName);
            variables.put("auditTime", LocalDateTime.now());
            variables.put("auditorName", "系统");

            dingtalkNotificationEventPublisher.publishCrmEvent(
                    "contract_audit_pass",
                    contract.getId(),
                    contract.getNo(),
                    variables,
                    contract.getOwnerUserId()
            );
            log.debug("[publishContractAuditPassEvent] published, contractId={}", contract.getId());
        } catch (Exception e) {
            log.warn("[publishContractAuditPassEvent] failed, non-critical", e);
        }
    }

    // ======================= 查询相关 =======================

    @Override
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CONTRACT, bizId = "#id", level = CrmPermissionLevelEnum.READ)
    public CrmContractDO getContract(Long id) {
        return contractMapper.selectById(id);
    }

    @Override
    public CrmContractDO validateContract(Long id) {
        return validateContractExists(id);
    }

    @Override
    public List<CrmContractDO> getContractList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return ListUtil.empty();
        }
        return contractMapper.selectByIds(ids);
    }

    @Override
    public PageResult<CrmContractDO> getContractPage(CrmContractPageReqVO pageReqVO, Long userId) {
        CrmContractConfigDO config = null;
        if (CrmContractPageReqVO.EXPIRY_TYPE_ABOUT_TO_EXPIRE.equals(pageReqVO.getExpiryType())) {
            config = contractConfigService.getContractConfig();
            if (config != null && Boolean.FALSE.equals(config.getNotifyEnabled())) {
                config = null;
            }
            if (config == null) {
                return PageResult.empty();
            }
        }
        return contractMapper.selectPage(pageReqVO, userId, config);
    }

    @Override
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CUSTOMER, bizId = "#pageReqVO.customerId", level = CrmPermissionLevelEnum.READ)
    public PageResult<CrmContractDO> getContractPageByCustomerId(CrmContractPageReqVO pageReqVO) {
        return contractMapper.selectPageByCustomerId(pageReqVO);
    }

    @Override
    public List<CrmContractDO> getContractSimpleListByCustomerId(Long customerId) {
        return contractMapper.selectList(CrmContractDO::getCustomerId, customerId);
    }

    @Override
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_BUSINESS, bizId = "#pageReqVO.businessId", level = CrmPermissionLevelEnum.READ)
    public PageResult<CrmContractDO> getContractPageByBusinessId(CrmContractPageReqVO pageReqVO) {
        return contractMapper.selectPageByBusinessId(pageReqVO);
    }

    @Override
    public Long getContractCountByContactId(Long contactId) {
        return contractMapper.selectCountByContactId(contactId);
    }

    @Override
    public Long getContractCountByCustomerId(Long customerId) {
        return contractMapper.selectCount(CrmContractDO::getCustomerId, customerId);
    }

    @Override
    public Long getContractCountByBusinessId(Long businessId) {
        return contractMapper.selectCountByBusinessId(businessId);
    }

    @Override
    public Long getAuditContractCount(Long userId) {
        return contractMapper.selectCountByAudit(userId);
    }

    @Override
    public Long getRemindContractCount(Long userId) {
        CrmContractConfigDO config = contractConfigService.getContractConfig();
        if (config == null || Boolean.FALSE.equals(config.getNotifyEnabled())) {
            return 0L;
        }
        return contractMapper.selectCountByRemind(userId, config);
    }

    @Override
    public List<CrmContractDO> getContractListByCustomerIdOwnerUserId(Long customerId, Long ownerUserId) {
        return contractMapper.selectListByCustomerIdOwnerUserId(customerId, ownerUserId);
    }

}
