package cn.shuhe.system.module.crm.service.business;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjUtil;
import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.common.util.object.BeanUtils;
import cn.shuhe.system.module.bpm.api.task.BpmProcessInstanceApi;
import cn.shuhe.system.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.shuhe.system.module.crm.controller.admin.business.vo.business.CrmBusinessEarlyInvestmentSubmitReqVO;
import cn.shuhe.system.module.crm.controller.admin.business.vo.business.CrmBusinessPageReqVO;
import cn.shuhe.system.module.crm.controller.admin.business.vo.business.CrmBusinessSaveReqVO;
import cn.shuhe.system.module.crm.controller.admin.business.vo.business.CrmBusinessTransferReqVO;
import cn.shuhe.system.module.crm.controller.admin.business.vo.business.CrmBusinessUpdateStatusReqVO;
import cn.shuhe.system.module.crm.controller.admin.contact.vo.CrmContactBusinessReqVO;
import cn.shuhe.system.module.crm.controller.admin.statistics.vo.funnel.CrmStatisticsFunnelReqVO;
import cn.shuhe.system.module.crm.dal.dataobject.business.CrmBusinessDO;
import cn.shuhe.system.module.crm.dal.dataobject.customer.CrmCustomerDO;
import cn.shuhe.system.module.crm.dal.dataobject.contact.CrmContactBusinessDO;
import cn.shuhe.system.module.crm.dal.mysql.business.CrmBusinessMapper;
import cn.shuhe.system.module.crm.enums.common.CrmAuditStatusEnum;
import cn.shuhe.system.module.crm.enums.common.CrmBizTypeEnum;
import cn.shuhe.system.module.crm.enums.permission.CrmPermissionLevelEnum;
import cn.shuhe.system.module.crm.framework.permission.core.annotations.CrmPermission;
import cn.shuhe.system.module.crm.service.contact.CrmContactBusinessService;
import cn.shuhe.system.module.crm.service.contact.CrmContactService;
import cn.shuhe.system.module.crm.service.contract.CrmContractService;
import cn.shuhe.system.module.crm.service.customer.CrmCustomerService;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectSaveReqVO;
import cn.shuhe.system.module.project.service.ProjectDeptServiceService;
import cn.shuhe.system.module.project.service.ProjectService;
import cn.shuhe.system.module.system.dal.dataobject.cost.ContractDeptAllocationDO;
import cn.shuhe.system.module.system.dal.mysql.cost.ContractDeptAllocationMapper;
import cn.shuhe.system.module.crm.service.permission.CrmPermissionService;
import cn.shuhe.system.module.crm.service.permission.bo.CrmPermissionCreateReqBO;
import cn.shuhe.system.module.crm.service.permission.bo.CrmPermissionTransferReqBO;
import cn.shuhe.system.module.system.api.dept.DeptApi;
import cn.shuhe.system.module.system.api.dept.dto.DeptRespDTO;
import cn.shuhe.system.module.system.api.dingtalk.DingtalkNotifyApi;
import cn.shuhe.system.module.system.api.dingtalk.dto.DingtalkNotifySendReqDTO;
import cn.shuhe.system.module.system.api.user.AdminUserApi;
import cn.shuhe.system.module.system.api.user.dto.AdminUserRespDTO;
import com.mzt.logapi.context.LogRecordContext;
import com.mzt.logapi.service.impl.DiffParseFunction;
import com.mzt.logapi.starter.annotation.LogRecord;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static cn.shuhe.system.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.shuhe.system.framework.common.util.collection.CollectionUtils.*;
import static cn.shuhe.system.module.crm.enums.ErrorCodeConstants.*;
import static cn.shuhe.system.module.crm.enums.LogRecordConstants.*;

@Slf4j
@Service
@Validated
public class CrmBusinessServiceImpl implements CrmBusinessService {

    public static final String BPM_PROCESS_DEFINITION_KEY = "crm-business-audit";
    public static final String EARLY_INVESTMENT_PROCESS_DEFINITION_KEY = "crm-business-early-investment";

    @Resource
    private CrmBusinessMapper businessMapper;

    @Resource
    @Lazy
    private CrmContractService contractService;
    @Resource
    private CrmCustomerService customerService;
    @Resource
    private ProjectService projectService;
    @Resource
    private ProjectDeptServiceService projectDeptServiceService;
    @Resource
    @Lazy
    private CrmContactService contactService;
    @Resource
    private CrmPermissionService permissionService;
    @Resource
    private CrmContactBusinessService contactBusinessService;

    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;
    @Resource
    private DingtalkNotifyApi dingtalkNotifyApi;
    @Resource
    private BpmProcessInstanceApi bpmProcessInstanceApi;
    @Resource
    private cn.shuhe.system.module.system.service.cost.CostCalculationService costCalculationService;
    @Resource
    private ContractDeptAllocationMapper contractDeptAllocationMapper;

    /** 测试阶段：不拉老板（总经办），仅部门主管。配置：shuhe.dingtalk.business-audit.skip-boss=true */
    @Value("${shuhe.dingtalk.business-audit.skip-boss:false}")
    private boolean businessAuditSkipBoss;
    /** 测试阶段：仅拉此用户进群（钉钉需至少2人，会加商机负责人作第二人）。配置：shuhe.dingtalk.business-audit.test-only-user-id=249 */
    @Value("${shuhe.dingtalk.business-audit.test-only-user-id:0}")
    private Long businessAuditTestOnlyUserId;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = CRM_BUSINESS_TYPE, subType = CRM_BUSINESS_CREATE_SUB_TYPE, bizNo = "{{#business.id}}",
            success = CRM_BUSINESS_CREATE_SUCCESS)
    public Long createBusiness(CrmBusinessSaveReqVO createReqVO, Long userId) {
        // 1. 校验关联字段
        validateRelationDataExists(createReqVO);

        // 2. 填充部门名称到分配列表
        fillDeptNames(createReqVO);

        // 3. 校验：部门分配金额之和 = 预计总金额（先填总金额，再分配各部门）
        validateAllocationSum(createReqVO.getTotalPrice(), createReqVO.getDeptAllocations());

        // 4. 插入商机（初始为未提交状态）
        CrmBusinessDO business = BeanUtils.toBean(createReqVO, CrmBusinessDO.class);
        business.setAuditStatus(CrmAuditStatusEnum.DRAFT.getStatus());
        businessMapper.insert(business);

        // 5. 创建数据权限
        permissionService.createPermission(new CrmPermissionCreateReqBO().setUserId(business.getOwnerUserId())
                .setBizType(CrmBizTypeEnum.CRM_BUSINESS.getType()).setBizId(business.getId())
                .setLevel(CrmPermissionLevelEnum.OWNER.getLevel()));

        // 6. 联系人关联
        if (createReqVO.getContactId() != null) {
            contactBusinessService.createContactBusinessList(new CrmContactBusinessReqVO().setContactId(createReqVO.getContactId())
                    .setBusinessIds(Collections.singletonList(business.getId())));
        }

        // 7. 记录操作日志（钉钉群在提交审核时创建）
        LogRecordContext.putVariable("business", business);
        return business.getId();
    }

    /**
     * 校验部门分配金额之和等于预计总金额（先填总金额，再分配各部门）
     */
    private void validateAllocationSum(BigDecimal totalPrice, List<CrmBusinessSaveReqVO.DeptAllocation> allocations) {
        if (totalPrice == null || CollUtil.isEmpty(allocations)) {
            return;
        }
        BigDecimal sum = allocations.stream()
                .map(CrmBusinessSaveReqVO.DeptAllocation::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sum.compareTo(totalPrice) != 0) {
            throw exception(BUSINESS_ALLOCATION_SUM_MISMATCH);
        }
    }

    private void fillDeptNames(CrmBusinessSaveReqVO reqVO) {
        if (CollUtil.isEmpty(reqVO.getDeptAllocations())) {
            return;
        }
        for (CrmBusinessSaveReqVO.DeptAllocation allocation : reqVO.getDeptAllocations()) {
            if (allocation.getDeptName() == null && allocation.getDeptId() != null) {
                DeptRespDTO dept = deptApi.getDept(allocation.getDeptId());
                if (dept != null) {
                    allocation.setDeptName(dept.getName());
                }
            }
        }
    }

    /**
     * 更新商机审批状态（由 CrmBusinessStatusListener 监听 BPM 事件后回调）
     */
    @Override
    public void updateBusinessAuditStatus(Long id, Integer bpmResult) {
        CrmBusinessDO business = validateBusinessExists(id);
        Integer auditStatus = convertBpmResultToAuditStatus(bpmResult);
        businessMapper.updateById(new CrmBusinessDO().setId(id).setAuditStatus(auditStatus));

        if (CrmAuditStatusEnum.APPROVE.getStatus().equals(auditStatus)) {
            // 审批全部通过：创建钉钉群（纯通知，无操作按钮），项目由前端选择创建路径
            try {
                CrmBusinessDO updatedBusiness = businessMapper.selectById(id);
                createDingtalkGroupChatAfterApproval(updatedBusiness);
            } catch (Exception e) {
                log.warn("[updateBusinessAuditStatus] 创建审批通过钉钉群失败, businessId={}", id, e);
            }
        } else if (CrmAuditStatusEnum.REJECT.getStatus().equals(auditStatus)) {
            // 审批驳回：工作通知商机负责人
            try {
                dingtalkNotifyApi.sendWorkNotice(new DingtalkNotifySendReqDTO()
                        .setUserIds(Collections.singletonList(business.getOwnerUserId()))
                        .setTitle("商机审批被驳回")
                        .setContent("❌ 您的商机【" + business.getName() + "】审批被驳回，请修改后重新提交。"));
            } catch (Exception e) {
                log.warn("[updateBusinessAuditStatus] 发送驳回通知失败, businessId={}", id, e);
            }
        }
    }

    /**
     * 审批全部通过后创建钉钉群（纯通知消息，无审批操作按钮）
     */
    private void createDingtalkGroupChatAfterApproval(CrmBusinessDO business) {
        if (business.getDingtalkChatId() != null) {
            log.info("[createDingtalkGroupChatAfterApproval] 商机 {} 钉钉群已存在，跳过", business.getId());
            return;
        }
        Set<Long> memberUserIds = collectAuditGroupMembers(business);
        Long ownerUserId = getBossUserId();
        if (ownerUserId == null) {
            ownerUserId = business.getOwnerUserId();
        } else {
            memberUserIds.add(ownerUserId);
        }
        memberUserIds.add(business.getOwnerUserId()); // 商机负责人也入群

        if (memberUserIds.size() < 2) {
            log.warn("[createDingtalkGroupChatAfterApproval] 群成员不足2人，跳过建群, businessId={}", business.getId());
            return;
        }

        String chatName = "项目-" + business.getName();
        String chatId = dingtalkNotifyApi.createBusinessAuditGroupChat(
                chatName, ownerUserId, new ArrayList<>(memberUserIds));
        if (chatId == null) {
            log.warn("[createDingtalkGroupChatAfterApproval] 建群失败, businessId={}", business.getId());
            return;
        }

        businessMapper.updateById(new CrmBusinessDO().setId(business.getId()).setDingtalkChatId(chatId));
        log.info("[createDingtalkGroupChatAfterApproval] 商机审批通过后建群成功: chatId={}", chatId);

        // 发送纯通知消息（无操作按钮）
        StringBuilder msg = new StringBuilder();
        msg.append("✅ **商机【").append(business.getName()).append("】审批已全部通过**\n\n");
        msg.append("💰 预计合同金额：").append(String.format("%,.2f", business.getTotalPrice())).append(" 元\n\n");
        if (CollUtil.isNotEmpty(business.getDeptAllocations())) {
            msg.append("**部门预计分配**：\n\n");
            for (CrmBusinessDO.DeptAllocation allocation : business.getDeptAllocations()) {
                msg.append("- ").append(allocation.getDeptName()).append("：")
                        .append(String.format("%,.2f", allocation.getAmount())).append(" 元\n\n");
            }
        }
        msg.append("请负责人在系统中选择下一步：**提前投入** 或 **等待签订合同**。");
        dingtalkNotifyApi.sendMessageToChat(chatId, "商机审批通过通知", msg.toString());
    }

    /**
     * 提交提前投入审批
     * 商机审批通过后，可由负责人发起；先保存申请详情，再启动 BPM 审批流程
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_BUSINESS, bizId = "#reqVO.businessId", level = CrmPermissionLevelEnum.WRITE)
    public void submitEarlyInvestment(CrmBusinessEarlyInvestmentSubmitReqVO reqVO, Long userId) {
        Long businessId = reqVO.getBusinessId();
        CrmBusinessDO business = validateBusinessExists(businessId);
        // 校验：商机必须已审批通过
        if (!CrmAuditStatusEnum.APPROVE.getStatus().equals(business.getAuditStatus())) {
            throw exception(BUSINESS_SUBMIT_FAIL_NOT_DRAFT);
        }
        // 校验：提前投入审批不能处于审批中
        if (CrmAuditStatusEnum.PROCESS.getStatus().equals(business.getEarlyInvestmentStatus())) {
            throw exception(BUSINESS_SUBMIT_FAIL_NOT_DRAFT);
        }
        // 计算审批链路
        List<Long> approvalChain = calculateApprovalChain(userId);
        log.info("[submitEarlyInvestment] businessId={}, 审批链路: {}", businessId, approvalChain);

        // 构建流程变量
        Map<String, Object> eiVariables = new HashMap<>();
        eiVariables.put("approvalChain", approvalChain);
        eiVariables.put("name", business.getName());
        eiVariables.put("totalPrice", business.getTotalPrice());
        String eiCustomerName = "";
        if (business.getCustomerId() != null) {
            List<CrmCustomerDO> cs = customerService.getCustomerList(Collections.singleton(business.getCustomerId()));
            if (!cs.isEmpty()) eiCustomerName = cs.get(0).getName();
        }
        eiVariables.put("customerName", eiCustomerName);
        String eiIntermediaryName = "";
        if (business.getIntermediaryId() != null) {
            List<CrmCustomerDO> im = customerService.getCustomerList(Collections.singleton(business.getIntermediaryId()));
            if (!im.isEmpty()) eiIntermediaryName = im.get(0).getName();
        }
        eiVariables.put("intermediaryName", eiIntermediaryName);
        if (business.getOwnerUserId() != null) {
            AdminUserRespDTO owner = adminUserApi.getUser(business.getOwnerUserId());
            eiVariables.put("ownerUserName", owner != null ? owner.getNickname() : "");
        } else {
            eiVariables.put("ownerUserName", "");
        }
        eiVariables.put("deptAllocationsText", buildDeptAllocationsText(business.getDeptAllocations()));
        // 将申请详情也传入流程变量，供审批页面展示
        eiVariables.put("eiWorkScope", reqVO.getWorkScope());
        eiVariables.put("eiReason", reqVO.getReason());
        eiVariables.put("eiEstimatedCost", reqVO.getEstimatedCost());

        // 创建 BPM 流程
        String processInstanceId = bpmProcessInstanceApi.createProcessInstance(userId,
                new BpmProcessInstanceCreateReqDTO()
                        .setProcessDefinitionKey(EARLY_INVESTMENT_PROCESS_DEFINITION_KEY)
                        .setBusinessKey(String.valueOf(businessId))
                        .setVariables(eiVariables));

        // 保存申请详情到商机记录
        businessMapper.updateById(new CrmBusinessDO().setId(businessId)
                .setEarlyInvestmentStatus(CrmAuditStatusEnum.PROCESS.getStatus())
                .setEarlyInvestmentProcessInstanceId(processInstanceId)
                .setEarlyInvestmentPersonnel(reqVO.getPersonnel())
                .setEarlyInvestmentEstimatedCost(reqVO.getEstimatedCost())
                .setEarlyInvestmentWorkScope(reqVO.getWorkScope())
                .setEarlyInvestmentPlanStart(reqVO.getPlanStart())
                .setEarlyInvestmentPlanEnd(reqVO.getPlanEnd())
                .setEarlyInvestmentRiskHandling(reqVO.getRiskHandling())
                .setEarlyInvestmentReason(reqVO.getReason()));

        // 群通知（含申请详情）
        if (business.getDingtalkChatId() != null) {
            try {
                StringBuilder msg = new StringBuilder();
                // 标题
                msg.append("## ⏳ 提前投入审批已提交\n\n");
                msg.append("**商机：** ").append(business.getName()).append("\n\n");
                msg.append("---\n\n");
                // 申请内容
                msg.append("**📋 申请内容**\n\n");
                if (reqVO.getWorkScope() != null) {
                    msg.append("**工作内容：** ").append(reqVO.getWorkScope()).append("\n\n");
                }
                if (reqVO.getReason() != null) {
                    msg.append("**申请理由：** ").append(reqVO.getReason()).append("\n\n");
                }
                if (reqVO.getEstimatedCost() != null) {
                    msg.append("**预计自垫资金：** ¥").append(reqVO.getEstimatedCost().toPlainString()).append(" 元\n\n");
                }
                if (reqVO.getPlanStart() != null || reqVO.getPlanEnd() != null) {
                    msg.append("**投入周期：** ")
                            .append(reqVO.getPlanStart() != null ? reqVO.getPlanStart().toString() : "?")
                            .append(" ~ ")
                            .append(reqVO.getPlanEnd() != null ? reqVO.getPlanEnd().toString() : "?")
                            .append("\n\n");
                }
                if (reqVO.getRiskHandling() != null) {
                    msg.append("**若合同未签：** ").append(reqVO.getRiskHandling()).append("\n\n");
                }
                // 投入人员
                if (reqVO.getPersonnel() != null && !reqVO.getPersonnel().isEmpty()) {
                    msg.append("**👥 投入人员**\n\n");
                    for (CrmBusinessDO.Personnel p : reqVO.getPersonnel()) {
                        msg.append("- ").append(p.getUserName());
                        if (p.getWorkDays() != null) {
                            msg.append("（").append(p.getWorkDays()).append(" 天）");
                        }
                        msg.append("\n");
                    }
                    msg.append("\n");
                }
                // 底部状态
                msg.append("---\n\n");
                msg.append("> 申请正在逐级审批中，请等待审批结果。");
                dingtalkNotifyApi.sendMessageToChat(business.getDingtalkChatId(),
                        "提前投入审批已提交", msg.toString());
            } catch (Exception e) {
                log.warn("[submitEarlyInvestment] 发送群通知失败", e);
            }
        }
    }

    /**
     * 更新提前投入审批状态（由 CrmBusinessEarlyInvestmentStatusListener 回调）
     */
    @Override
    public void updateEarlyInvestmentAuditStatus(Long businessId, Integer bpmResult) {
        CrmBusinessDO business = validateBusinessExists(businessId);
        Integer auditStatus = convertBpmResultToAuditStatus(bpmResult);
        businessMapper.updateById(new CrmBusinessDO().setId(businessId).setEarlyInvestmentStatus(auditStatus));

        if (CrmAuditStatusEnum.APPROVE.getStatus().equals(auditStatus)) {
            // 提前投入审批通过：创建项目
            try {
                createProjectInternally(business, null, null);
            } catch (Exception e) {
                log.error("[updateEarlyInvestmentAuditStatus] 创建项目失败, businessId={}: {}", businessId, e.getMessage(), e);
            }
            // 将投入人员拉入商机群
            if (business.getDingtalkChatId() != null
                    && business.getEarlyInvestmentPersonnel() != null
                    && !business.getEarlyInvestmentPersonnel().isEmpty()) {
                try {
                    List<Long> personnelUserIds = business.getEarlyInvestmentPersonnel().stream()
                            .map(CrmBusinessDO.Personnel::getUserId)
                            .filter(Objects::nonNull)
                            .collect(java.util.stream.Collectors.toList());
                    if (!personnelUserIds.isEmpty()) {
                        dingtalkNotifyApi.addMembersToGroupChat(business.getDingtalkChatId(), personnelUserIds);
                        log.info("[updateEarlyInvestmentAuditStatus] 已将提前投入人员加入商机群: chatId={}, userIds={}",
                                business.getDingtalkChatId(), personnelUserIds);
                    }
                } catch (Exception e) {
                    log.warn("[updateEarlyInvestmentAuditStatus] 将投入人员加入群失败", e);
                }
            }
            // 群通知
            if (business.getDingtalkChatId() != null) {
                try {
                    dingtalkNotifyApi.sendMessageToChat(business.getDingtalkChatId(),
                            "提前投入审批通过",
                            "✅ **提前投入审批已全部通过，项目已创建！**\n\n商机【" + business.getName() + "】已进入项目管理阶段。");
                } catch (Exception e) {
                    log.warn("[updateEarlyInvestmentAuditStatus] 发送群通知失败", e);
                }
            }
        } else if (CrmAuditStatusEnum.REJECT.getStatus().equals(auditStatus)) {
            // 驳回：通知负责人
            try {
                dingtalkNotifyApi.sendWorkNotice(new DingtalkNotifySendReqDTO()
                        .setUserIds(Collections.singletonList(business.getOwnerUserId()))
                        .setTitle("提前投入申请被驳回")
                        .setContent("❌ 商机【" + business.getName() + "】的提前投入申请被驳回，如有需要请修改后重新提交。"));
            } catch (Exception e) {
                log.warn("[updateEarlyInvestmentAuditStatus] 发送驳回通知失败", e);
            }
        }
    }

    /**
     * 根据商机信息创建项目（供合同签订或提前投入审批通过时调用）
     * 若项目已存在，则只更新合同关联信息
     */
    @Override
    public void createProjectFromBusiness(Long businessId, Long contractId, String contractNo) {
        CrmBusinessDO business = businessMapper.selectById(businessId);
        if (business == null) {
            log.warn("[createProjectFromBusiness] 商机 {} 不存在", businessId);
            return;
        }
        createProjectInternally(business, contractId, contractNo);
    }

    /**
     * 内部创建项目逻辑（可复用）
     */
    private void createProjectInternally(CrmBusinessDO business, Long contractId, String contractNo) {
        // 若项目已存在，只更新合同关联
        var existingProject = projectService.getProjectByBusinessId(business.getId());
        if (existingProject != null) {
            if (contractId != null) {
                log.info("[createProjectInternally] 项目已存在，更新合同关联: projectId={}, contractId={}",
                        existingProject.getId(), contractId);
                projectService.updateProjectContractInfo(existingProject.getId(), contractId, contractNo);
                // 提前投入转合同：同步部门预算到 project_dept_service.dept_budget
                Map<Integer, BigDecimal> budgetMap = buildDeptTypeBudgetMap(business);
                if (!budgetMap.isEmpty()) {
                    projectDeptServiceService.updateDeptBudgetByProjectId(existingProject.getId(), budgetMap);
                }
                // 同步历史数据到旧表（保留数据轨迹）
                String existingCustomerName = existingProject.getCustomerName() != null
                        ? existingProject.getCustomerName() : "";
                syncDeptAllocationsToContract(business, contractId, contractNo, existingCustomerName);
            } else {
                log.info("[createProjectInternally] 项目已存在，跳过创建, businessId={}", business.getId());
            }
            return;
        }

        // 获取客户名称
        String customerName = "";
        if (business.getCustomerId() != null) {
            List<CrmCustomerDO> customers = customerService.getCustomerList(
                    Collections.singleton(business.getCustomerId()));
            if (!customers.isEmpty()) {
                customerName = customers.get(0).getName();
            }
        }

        List<Integer> deptTypes = getDeptTypesFromBusinessAllocations(business);
        Integer primaryDeptType = deptTypes.isEmpty() ? 1 : deptTypes.get(0);

        ProjectSaveReqVO projectReqVO = new ProjectSaveReqVO();
        projectReqVO.setName(business.getName());
        projectReqVO.setDeptType(primaryDeptType);
        projectReqVO.setCustomerId(business.getCustomerId());
        projectReqVO.setCustomerName(customerName);
        projectReqVO.setBusinessId(business.getId());
        projectReqVO.setContractId(contractId);
        projectReqVO.setContractNo(contractNo);
        projectReqVO.setDingtalkChatId(business.getDingtalkChatId());
        projectReqVO.setStatus(1); // 进行中
        projectReqVO.setDescription("由商机【" + business.getName() + "】自动创建");

        Long projectId = projectService.createProject(projectReqVO);

        // 构建 deptType -> budget 映射，带入各部门的合同预算
        Map<Integer, BigDecimal> deptTypeBudgetMap = buildDeptTypeBudgetMap(business);
        projectDeptServiceService.batchCreateDeptServiceForBusiness(
                projectId, business.getId(), business.getCustomerId(), customerName, deptTypes, deptTypeBudgetMap);

        // 合同签订时，把商机的部门金额分配同步到 contract_dept_allocation（历史数据保留）
        if (contractId != null) {
            syncDeptAllocationsToContract(business, contractId, contractNo, customerName);
        }

        log.info("[createProjectInternally] 商机 {} 创建项目 {} 成功，合同关联: {}", business.getName(), projectId, contractId);

        // 群通知（如有群）
        if (business.getDingtalkChatId() != null) {
            try {
                String msg = contractId != null
                        ? "📁 **合同已签订，项目已创建！**\n\n商机【" + business.getName() + "】对应项目已创建，请相关部门进入项目管理。"
                        : "📁 **提前投入项目已创建！**\n\n商机【" + business.getName() + "】对应项目已创建，请相关部门进入项目管理。";
                dingtalkNotifyApi.sendMessageToChat(business.getDingtalkChatId(), "项目创建通知", msg);
            } catch (Exception e) {
                log.warn("[createProjectInternally] 发送群通知失败", e);
            }
        }
    }

    /**
     * 将商机的部门金额分配同步写入 contract_dept_allocation 表。
     * 合同签订时（包括提前投入转合同）调用。已存在的记录不重复创建。
     */
    @Override
    public void syncContractDeptAllocations(Long contractId, String contractNo, String customerName,
                                            List<CrmBusinessDO.DeptAllocation> allocations) {
        if (cn.hutool.core.collection.CollUtil.isEmpty(allocations) || contractId == null) {
            return;
        }
        // overwrite=true：合同金额为准，强制覆盖已有商机估算金额
        CrmBusinessDO fakeBusiness = new CrmBusinessDO();
        fakeBusiness.setDeptAllocations(allocations);
        syncDeptAllocationsToContract(fakeBusiness, contractId, contractNo, customerName, true);
    }

    /**
     * 将部门金额分配写入 contract_dept_allocation 表（UPSERT 语义）
     *
     * <p>以合同为准：若记录已存在且金额不同，则更新为新金额；若不存在则插入。
     * 商机阶段的金额是估算，合同阶段的金额才是最终确认值。
     *
     * @param overwrite true=合同来源，强制更新已有记录；false=商机来源，仅插入不存在的记录
     */
    private void syncDeptAllocationsToContract(CrmBusinessDO business, Long contractId,
                                               String contractNo, String customerName,
                                               boolean overwrite) {
        if (CollUtil.isEmpty(business.getDeptAllocations()) || contractId == null) {
            return;
        }
        for (CrmBusinessDO.DeptAllocation alloc : business.getDeptAllocations()) {
            if (alloc.getDeptId() == null || alloc.getAmount() == null) {
                continue;
            }
            try {
                ContractDeptAllocationDO existing = contractDeptAllocationMapper
                        .selectByContractIdAndDeptId(contractId, alloc.getDeptId());
                if (existing != null) {
                    if (overwrite && existing.getAllocatedAmount().compareTo(alloc.getAmount()) != 0) {
                        // 合同金额覆盖商机估算金额
                        existing.setAllocatedAmount(alloc.getAmount());
                        existing.setReceivedAmount(alloc.getAmount());
                        contractDeptAllocationMapper.updateById(existing);
                        log.info("[syncDeptAllocationsToContract] 更新部门分配(合同覆盖): contractId={}, deptId={}, amount={}",
                                contractId, alloc.getDeptId(), alloc.getAmount());
                    }
                    continue;
                }
                // 查部门信息（名称、类型）
                DeptRespDTO dept = deptApi.getDept(alloc.getDeptId());
                String deptName = alloc.getDeptName() != null ? alloc.getDeptName()
                        : (dept != null ? dept.getName() : "");
                Integer deptType = dept != null ? dept.getDeptType() : null;

                ContractDeptAllocationDO record = new ContractDeptAllocationDO();
                record.setContractId(contractId);
                record.setContractNo(contractNo);
                record.setCustomerName(customerName);
                record.setDeptId(alloc.getDeptId());
                record.setDeptName(deptName);
                record.setDeptType(deptType);
                record.setParentAllocationId(null);
                record.setAllocationLevel(1);
                record.setAllocatedAmount(alloc.getAmount());
                record.setReceivedAmount(alloc.getAmount());
                record.setDistributedAmount(BigDecimal.ZERO);
                contractDeptAllocationMapper.insert(record);
                log.info("[syncDeptAllocationsToContract] 插入部门分配: contractId={}, deptId={}, amount={}",
                        contractId, alloc.getDeptId(), alloc.getAmount());
            } catch (Exception e) {
                log.warn("[syncDeptAllocationsToContract] 同步失败，deptId={}: {}", alloc.getDeptId(), e.getMessage());
            }
        }
    }

    // 兼容旧调用（商机来源，仅插入）
    private void syncDeptAllocationsToContract(CrmBusinessDO business, Long contractId,
                                               String contractNo, String customerName) {
        syncDeptAllocationsToContract(business, contractId, contractNo, customerName, false);
    }

    /**
     * 从商机的部门分配列表构建 deptType -> budget 映射。
     * 用于在创建部门服务单时自动带入合同预算。
     */
    private Map<Integer, BigDecimal> buildDeptTypeBudgetMap(CrmBusinessDO business) {
        Map<Integer, BigDecimal> map = new java.util.HashMap<>();
        if (CollUtil.isEmpty(business.getDeptAllocations())) {
            return map;
        }
        List<Long> deptIds = business.getDeptAllocations().stream()
                .map(CrmBusinessDO.DeptAllocation::getDeptId)
                .filter(Objects::nonNull)
                .toList();
        if (deptIds.isEmpty()) return map;

        List<DeptRespDTO> depts = deptApi.getDeptList(deptIds);
        Map<Long, Integer> deptIdTypeMap = depts.stream()
                .filter(d -> d.getDeptType() != null)
                .collect(java.util.stream.Collectors.toMap(DeptRespDTO::getId, DeptRespDTO::getDeptType, (a, b) -> a));

        for (CrmBusinessDO.DeptAllocation alloc : business.getDeptAllocations()) {
            if (alloc.getDeptId() == null || alloc.getAmount() == null) continue;
            Integer deptType = deptIdTypeMap.get(alloc.getDeptId());
            if (deptType != null) {
                map.put(deptType, alloc.getAmount());
            }
        }
        return map;
    }

    /**
     * 从商机的部门分配列表中解析部门类型
     */
    private List<Integer> getDeptTypesFromBusinessAllocations(CrmBusinessDO business) {
        List<Integer> deptTypes = new ArrayList<>();
        if (CollUtil.isEmpty(business.getDeptAllocations())) {
            deptTypes.add(1); // 默认安全服务
            return deptTypes;
        }
        List<Long> deptIds = business.getDeptAllocations().stream()
                .map(CrmBusinessDO.DeptAllocation::getDeptId)
                .filter(Objects::nonNull)
                .toList();
        if (!deptIds.isEmpty()) {
            List<DeptRespDTO> depts = deptApi.getDeptList(deptIds);
            for (DeptRespDTO dept : depts) {
                Integer deptType = dept.getDeptType();
                if (deptType != null && !deptTypes.contains(deptType)) {
                    deptTypes.add(deptType);
                }
            }
        }
        if (deptTypes.isEmpty()) {
            deptTypes.add(1);
        }
        return deptTypes;
    }

    private Integer convertBpmResultToAuditStatus(Integer bpmResult) {
        if (bpmResult == null) {
            return CrmAuditStatusEnum.PROCESS.getStatus();
        }
        return switch (bpmResult) {
            case 2 -> CrmAuditStatusEnum.APPROVE.getStatus();
            case 3 -> CrmAuditStatusEnum.REJECT.getStatus();
            case 4 -> CrmAuditStatusEnum.CANCEL.getStatus();
            default -> CrmAuditStatusEnum.PROCESS.getStatus();
        };
    }

    /**
     * 更新商机的部门分配金额（由钉钉机器人回调触发）
     */
    public void updateDeptAllocations(Long businessId, List<CrmBusinessDO.DeptAllocation> newAllocations) {
        CrmBusinessDO business = validateBusinessExists(businessId);
        businessMapper.updateById(new CrmBusinessDO().setId(businessId).setDeptAllocations(newAllocations));

        // 计算新的总金额
        java.math.BigDecimal newTotal = newAllocations.stream()
                .map(CrmBusinessDO.DeptAllocation::getAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        businessMapper.updateById(new CrmBusinessDO().setId(businessId).setTotalPrice(newTotal));

        // 在钉钉群发送更新后的分配详情
        if (business.getDingtalkChatId() != null) {
            try {
                sendAllocationMessage(business.getId());
            } catch (Exception e) {
                log.warn("[updateDeptAllocations] 发送钉钉群通知失败", e);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = CRM_BUSINESS_TYPE, subType = CRM_BUSINESS_UPDATE_SUB_TYPE, bizNo = "{{#updateReqVO.id}}",
            success = CRM_BUSINESS_UPDATE_SUCCESS)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_BUSINESS, bizId = "#updateReqVO.id", level = CrmPermissionLevelEnum.WRITE)
    public void updateBusiness(CrmBusinessSaveReqVO updateReqVO) {
        updateReqVO.setOwnerUserId(null);
        CrmBusinessDO oldBusiness = validateBusinessExists(updateReqVO.getId());
        validateRelationDataExists(updateReqVO);
        fillDeptNames(updateReqVO);
        validateAllocationSum(updateReqVO.getTotalPrice(), updateReqVO.getDeptAllocations());

        CrmBusinessDO updateObj = BeanUtils.toBean(updateReqVO, CrmBusinessDO.class);
        businessMapper.updateById(updateObj);

        // 审批中时，同步发送更新后的分配详情到钉钉（自有群或固定群）
        if (CrmAuditStatusEnum.PROCESS.getStatus().equals(oldBusiness.getAuditStatus())
                && CollUtil.isNotEmpty(updateReqVO.getDeptAllocations())) {
            try {
                CrmBusinessDO updated = businessMapper.selectById(updateReqVO.getId());
                if (updated.getDingtalkChatId() != null) {
                    sendAllocationMessage(updateReqVO.getId(), true);
                } else if (StrUtil.isNotEmpty(dingtalkNotifyApi.getBusinessAuditChatId())) {
                    sendAllocationMessageToFixedGroup(updated, true);
                }
            } catch (Exception e) {
                log.warn("[updateBusiness] 发送钉钉群更新通知失败, businessId={}", updateReqVO.getId(), e);
            }
        }

        updateReqVO.setOwnerUserId(oldBusiness.getOwnerUserId());
        LogRecordContext.putVariable(DiffParseFunction.OLD_OBJECT, BeanUtils.toBean(oldBusiness, CrmBusinessSaveReqVO.class));
        LogRecordContext.putVariable("businessName", oldBusiness.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_BUSINESS, bizId = "#id", level = CrmPermissionLevelEnum.WRITE)
    public void submitBusinessAudit(Long id, Long userId) {
        // 1. 校验商机是否处于未提交状态
        CrmBusinessDO business = validateBusinessExists(id);
        if (ObjUtil.notEqual(business.getAuditStatus(), CrmAuditStatusEnum.DRAFT.getStatus())) {
            throw exception(BUSINESS_SUBMIT_FAIL_NOT_DRAFT);
        }
        // 2. 计算逐级审批链路（发起人部门→直属上级→...→总经办）
        List<Long> approvalChain = calculateApprovalChain(userId);
        log.info("[submitBusinessAudit] businessId={}, 审批链路(共{}级): {}", id, approvalChain.size(), approvalChain);
        if (approvalChain.isEmpty()) {
            log.warn("[submitBusinessAudit] 审批链路为空，businessId={}", id);
        }
        // 3. 构建流程变量（包含商机完整信息，供审批页面展示）
        Map<String, Object> variables = new HashMap<>();
        variables.put("approvalChain", approvalChain);
        variables.put("name", business.getName());
        variables.put("totalPrice", business.getTotalPrice());
        // 最终客户名称
        String customerName = "";
        if (business.getCustomerId() != null) {
            List<CrmCustomerDO> customers = customerService.getCustomerList(
                    Collections.singleton(business.getCustomerId()));
            if (!customers.isEmpty()) {
                customerName = customers.get(0).getName();
            }
        }
        variables.put("customerName", customerName);
        // 合作商名称
        String intermediaryName = "";
        if (business.getIntermediaryId() != null) {
            List<CrmCustomerDO> intermediaries = customerService.getCustomerList(
                    Collections.singleton(business.getIntermediaryId()));
            if (!intermediaries.isEmpty()) {
                intermediaryName = intermediaries.get(0).getName();
            }
        }
        variables.put("intermediaryName", intermediaryName);
        // 商机负责人姓名
        String ownerUserName = "";
        if (business.getOwnerUserId() != null) {
            AdminUserRespDTO owner = adminUserApi.getUser(business.getOwnerUserId());
            if (owner != null) {
                ownerUserName = owner.getNickname();
            }
        }
        variables.put("ownerUserName", ownerUserName);
        // 部门分配明细（含部门名称、金额、部门负责人）
        variables.put("deptAllocationsText", buildDeptAllocationsText(business.getDeptAllocations()));

        // 4. 创建 BPM 审批流程实例
        String processInstanceId = bpmProcessInstanceApi.createProcessInstance(userId,
                new BpmProcessInstanceCreateReqDTO()
                        .setProcessDefinitionKey(BPM_PROCESS_DEFINITION_KEY)
                        .setBusinessKey(String.valueOf(id))
                        .setVariables(variables));
        // 5. 更新商机状态为审批中（钉钉群在审批全部通过后才创建）
        businessMapper.updateById(new CrmBusinessDO().setId(id)
                .setProcessInstanceId(processInstanceId)
                .setAuditStatus(CrmAuditStatusEnum.PROCESS.getStatus()));
    }

    /**
     * 计算逐级审批链路：从发起人所在部门的直属上级，逐级向上直到总经办负责人
     * 返回有序的审批人 ID 列表
     */
    @Override
    public List<Long> calculateApprovalChain(Long startUserId) {
        List<Long> chain = new ArrayList<>();
        Long bossUserId = getBossUserId();

        // 获取发起人的部门
        AdminUserRespDTO user = adminUserApi.getUser(startUserId);
        if (user == null || user.getDeptId() == null) {
            log.warn("[calculateApprovalChain] 用户 {} 无部门信息，直接指定总经办审批", startUserId);
            if (bossUserId != null) {
                chain.add(bossUserId);
            }
            return chain;
        }

        // 从发起人部门逐级向上收集负责人，直到遇到总经办或根节点
        Set<Long> seen = new HashSet<>();
        seen.add(startUserId); // 发起人自己不加入审批链

        DeptRespDTO dept = deptApi.getDept(user.getDeptId());
        while (dept != null) {
            Long leaderId = dept.getLeaderUserId();
            if (leaderId != null && seen.add(leaderId)) {
                chain.add(leaderId);
                if (leaderId.equals(bossUserId)) {
                    break; // 已到总经办，停止
                }
            }
            // 无父部门或父部门为根节点，结束循环
            if (dept.getParentId() == null || dept.getParentId() == 0L) {
                if (bossUserId != null && !chain.contains(bossUserId)) {
                    chain.add(bossUserId); // 补充总经办（防止漏掉）
                }
                break;
            }
            dept = deptApi.getDept(dept.getParentId());
        }

        if (chain.isEmpty() && bossUserId != null) {
            chain.add(bossUserId);
        }
        return chain;
    }

    /**
     * 收集审批群成员：老板（总经办）+ 被分配部门的部门主管
     * 不含商机负责人，仅拉老板和被分配部门的主管进群
     */
    private Set<Long> collectAuditGroupMembers(CrmBusinessDO business) {
        Set<Long> memberUserIds = new LinkedHashSet<>();
        if (CollUtil.isNotEmpty(business.getDeptAllocations())) {
            for (CrmBusinessDO.DeptAllocation allocation : business.getDeptAllocations()) {
                DeptRespDTO dept = deptApi.getDept(allocation.getDeptId());
                if (dept != null && dept.getLeaderUserId() != null) {
                    memberUserIds.add(dept.getLeaderUserId());
                }
            }
        }
        return memberUserIds;
    }

    /**
     * 将部门分配列表格式化为可读文本，每行包含部门名称、分配金额和部门负责人。
     * 例：安全服务部 ¥500,000 / 负责人：张三
     */
    private String buildDeptAllocationsText(List<CrmBusinessDO.DeptAllocation> allocations) {
        if (CollUtil.isEmpty(allocations)) {
            return "（未分配）";
        }
        StringBuilder sb = new StringBuilder();
        for (CrmBusinessDO.DeptAllocation alloc : allocations) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            String deptName = alloc.getDeptName() != null ? alloc.getDeptName() : "未知部门";
            String amountStr = alloc.getAmount() != null
                    ? "¥" + alloc.getAmount().toPlainString()
                    : "-";
            // 查询部门负责人姓名
            String leaderName = "（未设置）";
            if (alloc.getDeptId() != null) {
                try {
                    DeptRespDTO dept = deptApi.getDept(alloc.getDeptId());
                    if (dept != null && dept.getLeaderUserId() != null) {
                        AdminUserRespDTO leader = adminUserApi.getUser(dept.getLeaderUserId());
                        if (leader != null) {
                            leaderName = leader.getNickname();
                        }
                    }
                } catch (Exception e) {
                    log.warn("[buildDeptAllocationsText] 获取部门负责人失败, deptId={}", alloc.getDeptId(), e);
                }
            }
            sb.append(deptName).append("  ").append(amountStr).append("  负责人：").append(leaderName);
        }
        return sb.toString();
    }

    /** 获取总经办负责人（老板） */
    private Long getBossUserId() {
        List<DeptRespDTO> topDepts = deptApi.getChildDeptList(0L);
        if (CollUtil.isEmpty(topDepts)) {
            return null;
        }
        return topDepts.stream()
                .filter(d -> d.getName() != null && d.getName().contains("总经"))
                .filter(d -> d.getLeaderUserId() != null)
                .map(DeptRespDTO::getLeaderUserId)
                .findFirst()
                .orElse(null);
    }

    @Override
    @LogRecord(type = CRM_BUSINESS_TYPE, subType = CRM_BUSINESS_FOLLOW_UP_SUB_TYPE, bizNo = "{{#id}}",
            success = CRM_BUSINESS_FOLLOW_UP_SUCCESS)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_BUSINESS, bizId = "#id", level = CrmPermissionLevelEnum.WRITE)
    public void updateBusinessFollowUp(Long id, LocalDateTime contactNextTime, String contactLastContent) {
        CrmBusinessDO business = validateBusinessExists(id);
        businessMapper.updateById(new CrmBusinessDO().setId(id).setFollowUpStatus(true).setContactNextTime(contactNextTime)
                .setContactLastTime(LocalDateTime.now()));
        LogRecordContext.putVariable("businessName", business.getName());
    }

    @Override
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_BUSINESS, bizId = "#ids", level = CrmPermissionLevelEnum.WRITE)
    public void updateBusinessContactNextTime(Collection<Long> ids, LocalDateTime contactNextTime) {
        businessMapper.updateBatch(convertList(ids, id -> new CrmBusinessDO().setId(id).setContactNextTime(contactNextTime)));
    }

    private void validateRelationDataExists(CrmBusinessSaveReqVO saveReqVO) {
        if (saveReqVO.getCustomerId() != null) {
            customerService.validateCustomer(saveReqVO.getCustomerId());
        }
        if (saveReqVO.getContactId() != null) {
            contactService.validateContact(saveReqVO.getContactId());
        }
        if (saveReqVO.getOwnerUserId() != null) {
            adminUserApi.validateUser(saveReqVO.getOwnerUserId());
        }
    }

    @Override
    @LogRecord(type = CRM_BUSINESS_TYPE, subType = CRM_BUSINESS_UPDATE_STATUS_SUB_TYPE, bizNo = "{{#reqVO.id}}",
            success = CRM_BUSINESS_UPDATE_STATUS_SUCCESS)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_BUSINESS, bizId = "#reqVO.id", level = CrmPermissionLevelEnum.WRITE)
    public void updateBusinessStatus(CrmBusinessUpdateStatusReqVO reqVO) {
        CrmBusinessDO business = validateBusinessExists(reqVO.getId());
        if (business.getEndStatus() != null) {
            throw exception(BUSINESS_UPDATE_STATUS_FAIL_END_STATUS);
        }
        if (!CrmAuditStatusEnum.APPROVE.getStatus().equals(business.getAuditStatus())) {
            throw exception(BUSINESS_UPDATE_STATUS_FAIL_AUDIT_NOT_PASS);
        }
        businessMapper.updateById(new CrmBusinessDO().setId(reqVO.getId())
                .setEndStatus(reqVO.getEndStatus())
                .setEndRemark(reqVO.getEndRemark()));
        LogRecordContext.putVariable("businessName", business.getName());

        // 输单/无效时：自动退场关联项目 + 发钉钉群通知
        cn.shuhe.system.module.crm.enums.business.CrmBusinessEndStatusEnum endStatusEnum =
                cn.shuhe.system.module.crm.enums.business.CrmBusinessEndStatusEnum.fromStatus(reqVO.getEndStatus());
        if (endStatusEnum != null && endStatusEnum != cn.shuhe.system.module.crm.enums.business.CrmBusinessEndStatusEnum.WIN) {
            // 自动退场关联项目（如果已创建）
            try {
                cn.shuhe.system.module.project.dal.dataobject.ProjectDO relatedProject =
                        projectService.getProjectByBusinessId(reqVO.getId());
                if (relatedProject != null && !Integer.valueOf(3).equals(relatedProject.getStatus())) {
                    String exitRemark = "商机" + endStatusEnum.getName()
                            + "，自动退场。" + (cn.hutool.core.util.StrUtil.isNotBlank(reqVO.getEndRemark())
                            ? "原因：" + reqVO.getEndRemark() : "");
                    projectService.exitProject(relatedProject.getId(), exitRemark);
                    log.info("[updateBusinessStatus] 商机{}，自动退场关联项目 {}", endStatusEnum.getName(), relatedProject.getId());
                }
            } catch (Exception e) {
                log.warn("[updateBusinessStatus] 自动退场项目失败, businessId={}: {}", reqVO.getId(), e.getMessage());
            }
            try {
                String statusName = endStatusEnum.getName(); // 输单 / 无效
                StringBuilder msg = new StringBuilder();
                // 标题
                msg.append("## ❌ 商机").append(statusName).append("\n\n");
                msg.append("**商机名称：** ").append(business.getName()).append("\n\n");
                if (cn.hutool.core.util.StrUtil.isNotBlank(reqVO.getEndRemark())) {
                    msg.append("**原因备注：** ").append(reqVO.getEndRemark()).append("\n\n");
                }
                // 预计合同金额（未实现收入）
                if (business.getTotalPrice() != null && business.getTotalPrice().compareTo(java.math.BigDecimal.ZERO) > 0) {
                    msg.append("**预计合同金额（未实现）：** ¥")
                            .append(business.getTotalPrice().toPlainString()).append(" 元\n\n");
                }
                msg.append("---\n\n");
                // 提前投入损失（如果发生过）
                boolean hasEarlyInvestment = business.getEarlyInvestmentStatus() != null
                        && (business.getEarlyInvestmentStatus() == 10 || business.getEarlyInvestmentStatus() == 20);
                if (hasEarlyInvestment) {
                    msg.append("**💸 提前投入损失估算**\n\n");

                    // 自垫资金
                    java.math.BigDecimal selfFundedCost = business.getEarlyInvestmentEstimatedCost() != null
                            ? business.getEarlyInvestmentEstimatedCost() : java.math.BigDecimal.ZERO;
                    if (selfFundedCost.compareTo(java.math.BigDecimal.ZERO) > 0) {
                        msg.append("**自垫资金：** ¥")
                                .append(selfFundedCost.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()).append(" 元\n\n");
                    }

                    // 投入人员 & 计划工时（含费用估算）
                    java.math.BigDecimal personnelTotalCost = java.math.BigDecimal.ZERO;
                    if (business.getEarlyInvestmentPersonnel() != null && !business.getEarlyInvestmentPersonnel().isEmpty()) {
                        msg.append("**投入人员：**\n");
                        int nowYear = java.time.LocalDate.now().getYear();
                        int nowMonth = java.time.LocalDate.now().getMonthValue();
                        for (CrmBusinessDO.Personnel p : business.getEarlyInvestmentPersonnel()) {
                            msg.append("- ").append(p.getUserName());
                            if (p.getWorkDays() != null) {
                                msg.append("（计划 ").append(p.getWorkDays()).append(" 天");
                                if (p.getUserId() != null) {
                                    try {
                                        cn.shuhe.system.module.system.controller.admin.cost.vo.UserCostRespVO costVO =
                                                costCalculationService.getUserCost(p.getUserId(), nowYear, nowMonth);
                                        if (costVO != null && costVO.getDailyCost() != null) {
                                            java.math.BigDecimal memberCost = costVO.getDailyCost()
                                                    .multiply(java.math.BigDecimal.valueOf(p.getWorkDays()))
                                                    .setScale(2, java.math.RoundingMode.HALF_UP);
                                            personnelTotalCost = personnelTotalCost.add(memberCost);
                                            msg.append(" × ¥")
                                               .append(costVO.getDailyCost().setScale(0, java.math.RoundingMode.HALF_UP))
                                               .append("/天 ≈ **¥").append(memberCost.toPlainString()).append("**");
                                        }
                                    } catch (Exception ex) {
                                        log.warn("[updateBusinessStatus] 获取人员 {} 成本失败", p.getUserName(), ex);
                                    }
                                }
                                msg.append("）");
                            }
                            msg.append("\n");
                        }
                        if (personnelTotalCost.compareTo(java.math.BigDecimal.ZERO) > 0) {
                            msg.append("\n**人力成本合计：¥").append(personnelTotalCost.toPlainString()).append("**\n");
                        }
                        msg.append("\n");
                    }

                    // 投入周期
                    if (business.getEarlyInvestmentPlanStart() != null || business.getEarlyInvestmentPlanEnd() != null) {
                        msg.append("**投入周期：** ")
                                .append(business.getEarlyInvestmentPlanStart() != null ? business.getEarlyInvestmentPlanStart() : "?")
                                .append(" ~ ")
                                .append(business.getEarlyInvestmentPlanEnd() != null ? business.getEarlyInvestmentPlanEnd() : "?")
                                .append("\n\n");
                    }

                    // 损失合计（自垫 + 人力）
                    java.math.BigDecimal totalLoss = selfFundedCost.add(personnelTotalCost);
                    if (totalLoss.compareTo(java.math.BigDecimal.ZERO) > 0) {
                        msg.append("**📊 预估总损失：¥").append(totalLoss.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString())
                                .append("**（自垫 ¥").append(selfFundedCost.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString())
                                .append(" + 人力 ¥").append(personnelTotalCost.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString())
                                .append("）\n\n");
                    }

                    // 提示
                    String investStatus = business.getEarlyInvestmentStatus() == 10 ? "审批中（尚未通过）" : "已批准";
                    msg.append("> 提前投入状态：").append(investStatus).append("，以上为申请时填报数据，仅供参考。\n\n");
                    msg.append("---\n\n");
                }
                msg.append("> 该商机已结束，不再进行跟进。");
                sendBusinessGroupNotification(reqVO.getId(), msg.toString(), "商机" + statusName + "通知");
            } catch (Exception e) {
                log.warn("[updateBusinessStatus] 发送{}群通知失败, businessId={}: {}",
                        reqVO.getEndStatus(), reqVO.getId(), e.getMessage());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = CRM_BUSINESS_TYPE, subType = CRM_BUSINESS_DELETE_SUB_TYPE, bizNo = "{{#id}}",
            success = CRM_BUSINESS_DELETE_SUCCESS)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_BUSINESS, bizId = "#id", level = CrmPermissionLevelEnum.OWNER)
    public void deleteBusiness(Long id) {
        CrmBusinessDO business = validateBusinessExists(id);
        validateContractExists(id);
        businessMapper.deleteById(id);
        permissionService.deletePermission(CrmBizTypeEnum.CRM_BUSINESS.getType(), id);
        LogRecordContext.putVariable("businessName", business.getName());
    }

    private void validateContractExists(Long businessId) {
        if (contractService.getContractCountByBusinessId(businessId) > 0) {
            throw exception(BUSINESS_DELETE_FAIL_CONTRACT_EXISTS);
        }
    }

    private CrmBusinessDO validateBusinessExists(Long id) {
        CrmBusinessDO crmBusiness = businessMapper.selectById(id);
        if (crmBusiness == null) {
            throw exception(BUSINESS_NOT_EXISTS);
        }
        return crmBusiness;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = CRM_BUSINESS_TYPE, subType = CRM_BUSINESS_TRANSFER_SUB_TYPE, bizNo = "{{#reqVO.id}}",
            success = CRM_BUSINESS_TRANSFER_SUCCESS)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_BUSINESS, bizId = "#reqVO.id", level = CrmPermissionLevelEnum.OWNER)
    public void transferBusiness(CrmBusinessTransferReqVO reqVO, Long userId) {
        CrmBusinessDO business = validateBusinessExists(reqVO.getId());
        permissionService.transferPermission(new CrmPermissionTransferReqBO(userId, CrmBizTypeEnum.CRM_BUSINESS.getType(),
                reqVO.getId(), reqVO.getNewOwnerUserId(), reqVO.getOldOwnerPermissionLevel()));
        businessMapper.updateOwnerUserIdById(reqVO.getId(), reqVO.getNewOwnerUserId());
        LogRecordContext.putVariable("business", business);
    }

    //======================= 钉钉群聊（提交审核时创建） =======================

    /** 构建商机分配消息正文（不含审批链接），不包含 @机器人 指令（普通群无机器人） */
    private StringBuilder buildAllocationMessageBody(CrmBusinessDO business, boolean withBusinessId, boolean isUpdate) {
        StringBuilder msg = new StringBuilder();
        if (isUpdate) {
            msg.append("🔄 **已更新**\n\n");
        }
        if (withBusinessId) {
            msg.append("📋 **【商机#").append(business.getId()).append("】** ").append(business.getName()).append("\n\n");
        } else {
            msg.append("📋 商机【").append(business.getName()).append("】\n\n");
        }
        msg.append("💰 **预计总合同**：").append(String.format("%,.2f", business.getTotalPrice())).append(" 元\n\n");
        msg.append("**部门预计分配**：\n\n");
        for (CrmBusinessDO.DeptAllocation allocation : business.getDeptAllocations()) {
            msg.append("- ").append(allocation.getDeptName()).append("：")
                    .append(String.format("%,.2f", allocation.getAmount())).append(" 元\n\n");
        }
        return msg;
    }

    /** 追加审批操作链接（通过、驳回、修改金额） */
    private void appendApprovalLinks(StringBuilder msg, Long businessId) {
        String approveUrl = generateBusinessApprovalUrl(businessId);
        String rejectUrl = generateBusinessRejectUrl(businessId);
        String editUrl = generateBusinessEditUrl(businessId);
        if (StrUtil.isEmpty(approveUrl)) return;
        msg.append("---\n\n**操作**：\n\n");
        msg.append("- [通过审批](").append(approveUrl).append(")\n\n");
        if (StrUtil.isNotEmpty(rejectUrl)) {
            msg.append("- [驳回](").append(rejectUrl).append(")\n\n");
        }
        if (StrUtil.isNotEmpty(editUrl)) {
            msg.append("- [修改金额](").append(editUrl).append(")（跳转系统编辑）\n\n");
        }
    }

    /**
     * 在钉钉群里发送部门金额分配详情
     * @param isUpdate true 表示金额已更新，会在消息前加「已更新」提示
     */
    public void sendAllocationMessage(Long businessId, boolean isUpdate) {
        CrmBusinessDO business = businessMapper.selectById(businessId);
        if (business == null || business.getDingtalkChatId() == null || CollUtil.isEmpty(business.getDeptAllocations())) {
            return;
        }

        StringBuilder msg = buildAllocationMessageBody(business, false, isUpdate);
        String title = isUpdate ? "商机分配已更新" : "商机分配通知";
        sendDingtalkGroupMessage(business, msg.toString(), title, true);
    }

    public void sendAllocationMessage(Long businessId) {
        sendAllocationMessage(businessId, false);
    }

    /**
     * 发送到商机审批固定群（配置了 businessAuditChatId 时使用）
     * 消息格式带商机ID，便于 @机器人 确定 123 / 修改 123 时解析
     */
    private void sendAllocationMessageToFixedGroup(CrmBusinessDO business, boolean isUpdate) {
        String fixedChatId = dingtalkNotifyApi.getBusinessAuditChatId();
        if (StrUtil.isEmpty(fixedChatId) || business == null || CollUtil.isEmpty(business.getDeptAllocations())) {
            return;
        }
        StringBuilder msg = buildAllocationMessageBody(business, true, isUpdate);
        appendApprovalLinks(msg, business.getId());
        String title = isUpdate ? "商机分配已更新" : "商机分配通知";
        boolean sent = dingtalkNotifyApi.sendMessageToChat(fixedChatId, title, msg.toString());
        if (sent) {
            log.info("[sendAllocationMessageToFixedGroup] 已发送到固定群, businessId={}", business.getId());
        } else {
            log.warn("[sendAllocationMessageToFixedGroup] 发送失败, businessId={}", business.getId());
        }
    }

    /** 发送不带审批操作链接的纯通知群消息（用于审批通过/驳回后的结果通知、项目进展等） */
    private void sendDingtalkGroupNotification(CrmBusinessDO business, String message, String title) {
        sendDingtalkGroupMessage(business, message, title, false);
    }

    @Override
    public void sendBusinessGroupNotification(Long businessId, String message, String title) {
        if (businessId == null) {
            return;
        }
        CrmBusinessDO business = businessMapper.selectById(businessId);
        if (business == null) {
            log.info("[sendBusinessGroupNotification] 商机 {} 不存在，跳过通知", businessId);
            return;
        }
        // 若商机无专属群，则注入固定审批群 chatId 后再发
        if (StrUtil.isEmpty(business.getDingtalkChatId())) {
            String fixedChatId = dingtalkNotifyApi.getBusinessAuditChatId();
            if (StrUtil.isEmpty(fixedChatId)) {
                log.info("[sendBusinessGroupNotification] 商机 {} 无关联群也无固定群配置，跳过通知", businessId);
                return;
            }
            business.setDingtalkChatId(fixedChatId);
        }
        try {
            sendDingtalkGroupNotification(business, message, title);
        } catch (Exception e) {
            log.warn("[sendBusinessGroupNotification] 发送群通知失败, businessId={}: {}", businessId, e.getMessage());
        }
    }

    @Override
    public void sendBusinessGroupNotificationWithAction(Long businessId, String message, String title,
                                                        String actionLabel, String actionUrl) {
        if (businessId == null) {
            return;
        }
        CrmBusinessDO business = businessMapper.selectById(businessId);
        if (business == null) {
            log.info("[sendBusinessGroupNotificationWithAction] 商机 {} 不存在，跳过通知", businessId);
            return;
        }
        // 优先使用商机自己的群（建群审批模式），若无则 fallback 到全局固定审批群
        String resolvedChatId = business.getDingtalkChatId();
        if (StrUtil.isEmpty(resolvedChatId)) {
            resolvedChatId = dingtalkNotifyApi.getBusinessAuditChatId();
        }
        if (StrUtil.isEmpty(resolvedChatId)) {
            log.info("[sendBusinessGroupNotificationWithAction] 商机 {} 无关联群也无固定群配置，跳过通知", businessId);
            return;
        }
        try {
            String chatId = resolvedChatId;
            // 直接发到群（消息正文里已包含 markdown 操作链接）
            boolean sent = dingtalkNotifyApi.sendMessageToChat(chatId, title, message);
            if (sent) {
                return;
            }
            // fallback：工作通知互动卡片，发给老板+部门主管
            log.warn("[sendBusinessGroupNotificationWithAction] 群消息失败，fallback 工作通知, businessId={}", businessId);
            Set<Long> notifyUserIds = new LinkedHashSet<>();
            if (CollUtil.isNotEmpty(business.getDeptAllocations())) {
                for (CrmBusinessDO.DeptAllocation allocation : business.getDeptAllocations()) {
                    DeptRespDTO dept = deptApi.getDept(allocation.getDeptId());
                    if (dept != null && dept.getLeaderUserId() != null) {
                        notifyUserIds.add(dept.getLeaderUserId());
                    }
                }
            }
            Long bossUserId = getBossUserId();
            if (bossUserId != null) {
                notifyUserIds.add(bossUserId);
            }
            if (StrUtil.isNotEmpty(actionUrl)) {
                dingtalkNotifyApi.sendActionCardMessage(
                        new ArrayList<>(notifyUserIds), title, message, actionLabel, actionUrl);
            } else {
                dingtalkNotifyApi.sendWorkNotice(new DingtalkNotifySendReqDTO()
                        .setUserIds(new ArrayList<>(notifyUserIds))
                        .setTitle(title)
                        .setContent(message));
            }
        } catch (Exception e) {
            log.warn("[sendBusinessGroupNotificationWithAction] 发送失败, businessId={}: {}", businessId, e.getMessage());
        }
    }

    /**
     * 向商机群发送消息
     *
     * @param withApprovalLinks true = 消息末尾追加「通过审批/驳回/修改金额」操作链接（仅审批中时使用）
     *                          false = 纯通知，不追加操作链接（审批结果通知、项目状态更新等）
     */
    private void sendDingtalkGroupMessage(CrmBusinessDO business, String message, String title, boolean withApprovalLinks) {
        if (CollUtil.isEmpty(business.getDeptAllocations())) {
            return;
        }
        String msgTitle = StrUtil.isEmpty(title) ? "商机通知" : title;
        // 1. 若有群聊ID，发 markdown 消息到群（钉钉 chat/send 不支持 actionCard，仅工作通知支持互动卡片）
        String chatId = business.getDingtalkChatId();
        if (chatId != null) {
            StringBuilder msg = new StringBuilder(message);
            if (withApprovalLinks) {
                appendApprovalLinks(msg, business.getId());
            }
            boolean sent = dingtalkNotifyApi.sendMessageToChat(chatId, msgTitle, msg.toString());
            if (sent) {
                return;
            }
            log.warn("[sendDingtalkGroupMessage] 群消息发送失败(可能机器人未入群)，fallback 工作通知, businessId={}", business.getId());
        }
        // 2. fallback：通过工作通知发给老板+部门主管
        Set<Long> notifyUserIds = new LinkedHashSet<>();
        for (CrmBusinessDO.DeptAllocation allocation : business.getDeptAllocations()) {
            DeptRespDTO dept = deptApi.getDept(allocation.getDeptId());
            if (dept != null && dept.getLeaderUserId() != null) {
                notifyUserIds.add(dept.getLeaderUserId());
            }
        }
        Long bossUserId = getBossUserId();
        if (bossUserId != null) {
            notifyUserIds.add(bossUserId);
        }
        StringBuilder fallbackMsg = new StringBuilder(message);
        if (withApprovalLinks) {
            appendApprovalLinks(fallbackMsg, business.getId());
            String approveUrl = generateBusinessApprovalUrl(business.getId());
            if (StrUtil.isNotEmpty(approveUrl)) {
                dingtalkNotifyApi.sendActionCardMessage(
                        new ArrayList<>(notifyUserIds),
                        msgTitle,
                        fallbackMsg.toString(),
                        "通过审批",
                        approveUrl);
                return;
            }
        }
        dingtalkNotifyApi.sendWorkNotice(new DingtalkNotifySendReqDTO()
                    .setUserIds(new ArrayList<>(notifyUserIds))
                    .setTitle(msgTitle)
                    .setContent(fallbackMsg.toString()));
    }

    private static final String APPROVE_TOKEN_SALT = "shuhe-business-approve-2026";

    /** 生成商机审批/驳回 URL（baseUrl 来自钉钉配置的 approveBaseUrl 或 callbackBaseUrl） */
    private String getBaseUrlForLinks() {
        String baseUrl = dingtalkNotifyApi.getApproveBaseUrl();
        if (StrUtil.isEmpty(baseUrl)) {
            log.warn("[getBaseUrlForLinks] 未配置 approveBaseUrl 或 callbackBaseUrl");
            return null;
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private String generateBusinessApprovalUrl(Long businessId) {
        String baseUrl = getBaseUrlForLinks();
        if (baseUrl == null) return null;
        String token = SecureUtil.md5(businessId + "-" + APPROVE_TOKEN_SALT);
        return baseUrl + "/admin-api/crm/business/dingtalk/approve?businessId=" + businessId + "&token=" + token;
    }

    private String generateBusinessRejectUrl(Long businessId) {
        String baseUrl = getBaseUrlForLinks();
        if (baseUrl == null) return null;
        String token = SecureUtil.md5(businessId + "-" + APPROVE_TOKEN_SALT);
        return baseUrl + "/admin-api/crm/business/dingtalk/reject?businessId=" + businessId + "&token=" + token;
    }

    private String generateBusinessEditUrl(Long businessId) {
        String baseUrl = getBaseUrlForLinks();
        if (baseUrl == null) return null;
        return baseUrl + "/crm/business/detail/" + businessId;
    }

    //======================= 查询相关 =======================

    @Override
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_BUSINESS, bizId = "#id", level = CrmPermissionLevelEnum.READ)
    public CrmBusinessDO getBusiness(Long id) {
        return businessMapper.selectById(id);
    }

    @Override
    public CrmBusinessDO validateBusiness(Long id) {
        return validateBusinessExists(id);
    }

    @Override
    public List<CrmBusinessDO> getBusinessList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return ListUtil.empty();
        }
        return businessMapper.selectByIds(ids);
    }

    @Override
    public PageResult<CrmBusinessDO> getBusinessPage(CrmBusinessPageReqVO pageReqVO, Long userId) {
        return businessMapper.selectPage(pageReqVO, userId);
    }

    @Override
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CUSTOMER, bizId = "#pageReqVO.customerId", level = CrmPermissionLevelEnum.READ)
    public PageResult<CrmBusinessDO> getBusinessPageByCustomerId(CrmBusinessPageReqVO pageReqVO) {
        return businessMapper.selectPageByCustomerId(pageReqVO);
    }

    @Override
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CONTACT, bizId = "#pageReqVO.contactId", level = CrmPermissionLevelEnum.READ)
    public PageResult<CrmBusinessDO> getBusinessPageByContact(CrmBusinessPageReqVO pageReqVO) {
        List<CrmContactBusinessDO> contactBusinessList = contactBusinessService.getContactBusinessListByContactId(
                pageReqVO.getContactId());
        if (CollUtil.isEmpty(contactBusinessList)) {
            return PageResult.empty();
        }
        return businessMapper.selectPageByContactId(pageReqVO,
                convertSet(contactBusinessList, CrmContactBusinessDO::getBusinessId));
    }

    @Override
    public Long getBusinessCountByCustomerId(Long customerId) {
        return businessMapper.selectCount(CrmBusinessDO::getCustomerId, customerId);
    }

    @Override
    public List<CrmBusinessDO> getBusinessListByCustomerIdOwnerUserId(Long customerId, Long ownerUserId) {
        return businessMapper.selectListByCustomerIdOwnerUserId(customerId, ownerUserId);
    }

    @Override
    public PageResult<CrmBusinessDO> getBusinessPageByDate(CrmStatisticsFunnelReqVO pageVO) {
        return businessMapper.selectPage(pageVO);
    }

}
