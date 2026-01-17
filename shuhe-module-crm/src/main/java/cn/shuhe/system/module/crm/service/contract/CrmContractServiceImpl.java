package cn.shuhe.system.module.crm.service.contract;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import java.util.ArrayList;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjUtil;
import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.common.util.number.MoneyUtils;
import cn.shuhe.system.framework.common.util.object.BeanUtils;
import cn.shuhe.system.framework.common.util.object.ObjectUtils;
import cn.shuhe.system.module.bpm.api.task.BpmProcessInstanceApi;
import cn.shuhe.system.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.shuhe.system.module.crm.controller.admin.contract.vo.contract.CrmContractPageReqVO;
import cn.shuhe.system.module.crm.controller.admin.contract.vo.contract.CrmContractSaveReqVO;
import cn.shuhe.system.module.crm.controller.admin.contract.vo.contract.CrmContractTransferReqVO;
import cn.shuhe.system.module.crm.dal.dataobject.contract.CrmContractConfigDO;
import cn.shuhe.system.module.crm.dal.dataobject.contract.CrmContractDO;
import cn.shuhe.system.module.crm.dal.dataobject.contract.ContractAssignDeptInfo;
import cn.shuhe.system.module.crm.dal.dataobject.contract.CrmContractProductDO;
import cn.shuhe.system.module.crm.dal.mysql.contract.CrmContractMapper;
import cn.shuhe.system.module.crm.dal.mysql.contract.CrmContractProductMapper;
import cn.shuhe.system.module.crm.dal.redis.no.CrmNoRedisDAO;
import cn.shuhe.system.module.crm.enums.common.CrmAuditStatusEnum;
import cn.shuhe.system.module.crm.enums.common.CrmBizTypeEnum;
import cn.shuhe.system.module.crm.enums.permission.CrmPermissionLevelEnum;
import cn.shuhe.system.module.crm.framework.permission.core.annotations.CrmPermission;
import cn.shuhe.system.module.crm.service.business.CrmBusinessService;
import cn.shuhe.system.module.crm.service.contact.CrmContactService;
import cn.shuhe.system.module.crm.service.customer.CrmCustomerService;
import cn.shuhe.system.module.crm.service.permission.CrmPermissionService;
import cn.shuhe.system.module.crm.service.permission.bo.CrmPermissionCreateReqBO;
import cn.shuhe.system.module.crm.service.permission.bo.CrmPermissionTransferReqBO;
import cn.shuhe.system.module.crm.service.product.CrmProductService;
import cn.shuhe.system.module.crm.service.receivable.CrmReceivableService;
import cn.shuhe.system.module.system.api.dept.DeptApi;
import cn.shuhe.system.module.system.api.dept.dto.DeptRespDTO;
import cn.shuhe.system.module.system.api.user.AdminUserApi;
import cn.shuhe.system.module.system.api.user.dto.AdminUserRespDTO;
import cn.shuhe.system.module.system.service.dingtalkconfig.DingtalkApiService;
import cn.shuhe.system.module.system.service.dingtalkconfig.DingtalkConfigService;
import cn.shuhe.system.module.system.dal.dataobject.dingtalkconfig.DingtalkConfigDO;
import cn.shuhe.system.module.system.dal.dataobject.dingtalkmapping.DingtalkMappingDO;
import cn.shuhe.system.module.system.dal.mysql.dingtalkmapping.DingtalkMappingMapper;
import cn.shuhe.system.module.project.controller.admin.vo.ProjectSaveReqVO;
import cn.shuhe.system.module.project.dal.dataobject.ProjectDO;
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
 *
 * @author dhb52
 */
@Service
@Validated
@Slf4j
public class CrmContractServiceImpl implements CrmContractService {

    /**
     * BPM 合同审批流程标识
     */
    public static final String BPM_PROCESS_DEFINITION_KEY = "crm-contract-audit";

    @Resource
    private CrmContractMapper contractMapper;
    @Resource
    private CrmContractProductMapper contractProductMapper;

    @Resource
    private CrmNoRedisDAO noRedisDAO;

    @Resource
    private CrmPermissionService crmPermissionService;
    @Resource
    private CrmProductService productService;
    @Resource
    private CrmCustomerService customerService;
    @Resource
    private CrmBusinessService businessService;
    @Resource
    private CrmContactService contactService;
    @Resource
    private CrmContractConfigService contractConfigService;
    @Resource
    @Lazy // 延迟加载，避免循环依赖
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
    private DingtalkMappingMapper dingtalkMappingMapper;
    @Resource
    private ProjectService projectService;
    @Resource
    private CrmCustomerMapper customerMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = CRM_CONTRACT_TYPE, subType = CRM_CONTRACT_CREATE_SUB_TYPE, bizNo = "{{#contract.id}}", success = CRM_CONTRACT_CREATE_SUCCESS)
    public Long createContract(CrmContractSaveReqVO createReqVO, Long userId) {
        // 1.1 校验产品项的有效性（允许为空）
        List<CrmContractProductDO> contractProducts = CollUtil.isEmpty(createReqVO.getProducts())
                ? new ArrayList<>()
                : validateContractProducts(createReqVO.getProducts());
        // 1.2 校验关联字段
        validateRelationDataExists(createReqVO);
        // 1.3 生成序号
        String no = noRedisDAO.generate(CrmNoRedisDAO.CONTRACT_NO_PREFIX);
        if (contractMapper.selectByNo(no) != null) {
            throw exception(CONTRACT_NO_EXISTS);
        }

        // 2.1 插入合同
        CrmContractDO contract = BeanUtils.toBean(createReqVO, CrmContractDO.class).setNo(no);
        // 设置负责人为当前用户（如果未指定）
        if (contract.getOwnerUserId() == null) {
            contract.setOwnerUserId(userId);
        }
        // 设置审批状态为草稿
        contract.setAuditStatus(CrmAuditStatusEnum.DRAFT.getStatus());
        // 处理分派部门 - 初始化每个部门的领取状态
        if (createReqVO.getAssignDeptIds() != null && !createReqVO.getAssignDeptIds().isEmpty()) {
            // 获取部门名称
            List<DeptRespDTO> depts = deptApi.getDeptList(createReqVO.getAssignDeptIds());
            java.util.Map<Long, String> deptNameMap = depts.stream()
                    .collect(java.util.stream.Collectors.toMap(DeptRespDTO::getId, DeptRespDTO::getName));

            // 构建分派部门信息列表（包含领取状态）
            List<ContractAssignDeptInfo> assignDeptInfoList = createReqVO.getAssignDeptIds().stream()
                    .map(deptId -> ContractAssignDeptInfo.builder()
                            .deptId(deptId)
                            .deptName(deptNameMap.getOrDefault(deptId, ""))
                            .claimed(false)
                            .claimUserId(null)
                            .claimUserName(null)
                            .claimTime(null)
                            .build())
                    .toList();
            contract.setAssignDeptIds(cn.hutool.json.JSONUtil.toJsonStr(assignDeptInfoList));
            contract.setClaimStatus(0); // 待领取
        } else {
            contract.setClaimStatus(1); // 无分派则直接已领取
        }
        // 计算总价（处理产品为空的情况）
        calculateTotalPrice(contract, contractProducts);
        contractMapper.insert(contract);
        // 2.2 插入合同关联商品
        if (CollUtil.isNotEmpty(contractProducts)) {
            contractProducts.forEach(item -> item.setContractId(contract.getId()));
            contractProductMapper.insertBatch(contractProducts);
        }

        // 3. 创建数据权限
        crmPermissionService.createPermission(new CrmPermissionCreateReqBO().setUserId(contract.getOwnerUserId())
                .setBizType(CrmBizTypeEnum.CRM_CONTRACT.getType()).setBizId(contract.getId())
                .setLevel(CrmPermissionLevelEnum.OWNER.getLevel()));

        // 4. 发送钉钉通知给分派部门的人员
        if (createReqVO.getAssignDeptIds() != null && !createReqVO.getAssignDeptIds().isEmpty()) {
            sendDingtalkNotifyToAssignedDepts(contract, createReqVO.getAssignDeptIds());
        }

        // 5. 自动创建对应的项目
        createProjectForContract(contract, createReqVO, userId);

        // 6. 记录操作日志上下文
        LogRecordContext.putVariable("contract", contract);
        return contract.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = CRM_CONTRACT_TYPE, subType = CRM_CONTRACT_UPDATE_SUB_TYPE, bizNo = "{{#updateReqVO.id}}", success = CRM_CONTRACT_UPDATE_SUCCESS)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CONTRACT, bizId = "#updateReqVO.id", level = CrmPermissionLevelEnum.WRITE)
    public void updateContract(CrmContractSaveReqVO updateReqVO) {
        Assert.notNull(updateReqVO.getId(), "合同编号不能为空");
        updateReqVO.setOwnerUserId(null); // 不允许更新的字段
        // 1.1 校验存在
        CrmContractDO oldContract = validateContractExists(updateReqVO.getId());
        // 1.2 只有草稿、审批中，可以编辑；
        if (!ObjectUtils.equalsAny(oldContract.getAuditStatus(), CrmAuditStatusEnum.DRAFT.getStatus(),
                CrmAuditStatusEnum.PROCESS.getStatus())) {
            throw exception(CONTRACT_UPDATE_FAIL_NOT_DRAFT);
        }
        // 1.3 校验产品项的有效性
        List<CrmContractProductDO> contractProducts = validateContractProducts(updateReqVO.getProducts());
        // 1.4 校验关联字段
        validateRelationDataExists(updateReqVO);

        // 2.1 更新合同
        CrmContractDO updateObj = BeanUtils.toBean(updateReqVO, CrmContractDO.class);
        calculateTotalPrice(updateObj, contractProducts);
        contractMapper.updateById(updateObj);
        // 2.2 更新合同关联商品
        updateContractProduct(updateReqVO.getId(), contractProducts);

        // 3. 记录操作日志上下文
        updateReqVO.setOwnerUserId(oldContract.getOwnerUserId()); // 避免操作日志出现“删除负责人”的情况
        LogRecordContext.putVariable(DiffParseFunction.OLD_OBJECT,
                BeanUtils.toBean(oldContract, CrmContractSaveReqVO.class));
        LogRecordContext.putVariable("contractName", oldContract.getName());
    }

    private void updateContractProduct(Long id, List<CrmContractProductDO> newList) {
        List<CrmContractProductDO> oldList = contractProductMapper.selectListByContractId(id);
        List<List<CrmContractProductDO>> diffList = diffList(oldList, newList, // id 不同，就认为是不同的记录
                (oldVal, newVal) -> oldVal.getId().equals(newVal.getId()));
        if (CollUtil.isNotEmpty(diffList.get(0))) {
            diffList.get(0).forEach(o -> o.setContractId(id));
            contractProductMapper.insertBatch(diffList.get(0));
        }
        if (CollUtil.isNotEmpty(diffList.get(1))) {
            contractProductMapper.updateBatch(diffList.get(1));
        }
        if (CollUtil.isNotEmpty(diffList.get(2))) {
            contractProductMapper.deleteByIds(convertSet(diffList.get(2), CrmContractProductDO::getId));
        }
    }

    /**
     * 校验关联数据是否存在
     *
     * @param reqVO 请求
     */
    private void validateRelationDataExists(CrmContractSaveReqVO reqVO) {
        // 1. 校验客户
        if (reqVO.getCustomerId() != null) {
            customerService.validateCustomer(reqVO.getCustomerId());
        }
        // 2. 校验负责人
        if (reqVO.getOwnerUserId() != null) {
            adminUserApi.validateUser(reqVO.getOwnerUserId());
        }
        // 3. 如果有关联商机，则需要校验存在
        if (reqVO.getBusinessId() != null) {
            businessService.validateBusiness(reqVO.getBusinessId());
        }
        // 4. 校验签约相关字段
        if (reqVO.getSignContactId() != null) {
            contactService.validateContact(reqVO.getSignContactId());
        }
        if (reqVO.getSignUserId() != null) {
            adminUserApi.validateUser(reqVO.getSignUserId());
        }
    }

    private List<CrmContractProductDO> validateContractProducts(List<CrmContractSaveReqVO.Product> list) {
        // 1. 校验产品存在
        productService.validProductList(convertSet(list, CrmContractSaveReqVO.Product::getProductId));
        // 2. 转化为 CrmContractProductDO 列表
        return convertList(list, o -> BeanUtils.toBean(o, CrmContractProductDO.class,
                item -> item.setTotalPrice(MoneyUtils.priceMultiply(item.getContractPrice(), item.getCount()))));
    }

    private void calculateTotalPrice(CrmContractDO contract, List<CrmContractProductDO> contractProducts) {
        // 计算产品总价（如果有产品的话）
        BigDecimal totalProductPrice = getSumValue(contractProducts, CrmContractProductDO::getTotalPrice,
                BigDecimal::add, BigDecimal.ZERO);
        contract.setTotalProductPrice(totalProductPrice);

        // 如果已经设置了合同总价（手动输入），则使用该值；否则根据产品计算
        if (contract.getTotalPrice() != null && contract.getTotalPrice().compareTo(BigDecimal.ZERO) > 0) {
            // 使用手动输入的合同总价
            return;
        }

        // 根据产品计算总价
        BigDecimal discountPercent = contract.getDiscountPercent() != null ? contract.getDiscountPercent()
                : BigDecimal.ZERO;
        BigDecimal discountPrice = MoneyUtils.priceMultiplyPercent(totalProductPrice, discountPercent);
        contract.setTotalPrice(totalProductPrice.subtract(discountPrice != null ? discountPrice : BigDecimal.ZERO));
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

        // 2.1 删除合同
        contractMapper.deleteById(id);
        // 2.2 删除数据权限
        crmPermissionService.deletePermission(CrmBizTypeEnum.CRM_CONTRACT.getType(), id);

        // 3. 记录操作日志上下文
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
        // 1. 校验合同是否存在
        CrmContractDO contract = validateContractExists(reqVO.getId());

        // 2.1 数据权限转移
        crmPermissionService
                .transferPermission(new CrmPermissionTransferReqBO(userId, CrmBizTypeEnum.CRM_CONTRACT.getType(),
                        reqVO.getId(), reqVO.getNewOwnerUserId(), reqVO.getOldOwnerPermissionLevel()));
        // 2.2 设置负责人
        contractMapper.updateById(new CrmContractDO().setId(reqVO.getId()).setOwnerUserId(reqVO.getNewOwnerUserId()));

        // 3. 记录转移日志
        LogRecordContext.putVariable("contract", contract);
    }

    @Override
    @LogRecord(type = CRM_CONTRACT_TYPE, subType = CRM_CONTRACT_FOLLOW_UP_SUB_TYPE, bizNo = "{{#id}}", success = CRM_CONTRACT_FOLLOW_UP_SUCCESS)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CONTRACT, bizId = "#id", level = CrmPermissionLevelEnum.WRITE)
    public void updateContractFollowUp(Long id, LocalDateTime contactNextTime, String contactLastContent) {
        // 1. 校验存在
        CrmContractDO contract = validateContractExists(id);

        // 2. 更新联系人的跟进信息
        contractMapper.updateById(new CrmContractDO().setId(id).setContactLastTime(LocalDateTime.now()));

        // 3. 记录操作日志上下文
        LogRecordContext.putVariable("contractName", contract.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = CRM_CONTRACT_TYPE, subType = CRM_CONTRACT_SUBMIT_SUB_TYPE, bizNo = "{{#id}}", success = CRM_CONTRACT_SUBMIT_SUCCESS)
    public void submitContract(Long id, Long userId) {
        // 1. 校验合同是否在审批
        CrmContractDO contract = validateContractExists(id);
        if (ObjUtil.notEqual(contract.getAuditStatus(), CrmAuditStatusEnum.DRAFT.getStatus())) {
            throw exception(CONTRACT_SUBMIT_FAIL_NOT_DRAFT);
        }

        // 2. 创建合同审批流程实例
        String processInstanceId = bpmProcessInstanceApi.createProcessInstance(userId,
                new BpmProcessInstanceCreateReqDTO()
                        .setProcessDefinitionKey(BPM_PROCESS_DEFINITION_KEY).setBusinessKey(String.valueOf(id)));

        // 3. 更新合同工作流编号
        contractMapper.updateById(new CrmContractDO().setId(id).setProcessInstanceId(processInstanceId)
                .setAuditStatus(CrmAuditStatusEnum.PROCESS.getStatus()));

        // 3. 记录日志
        LogRecordContext.putVariable("contractName", contract.getName());
    }

    @Override
    public void updateContractAuditStatus(Long id, Integer bpmResult) {
        // 1.1 校验合同是否存在
        CrmContractDO contract = validateContractExists(id);
        // 1.2 只有审批中，可以更新审批结果
        if (ObjUtil.notEqual(contract.getAuditStatus(), CrmAuditStatusEnum.PROCESS.getStatus())) {
            log.error("[updateContractAuditStatus][contract({}) 不处于审批中，无法更新审批结果({})]",
                    contract.getId(), bpmResult);
            throw exception(CONTRACT_UPDATE_AUDIT_STATUS_FAIL_NOT_PROCESS);
        }

        // 2. 更新合同审批结果
        Integer auditStatus = convertBpmResultToAuditStatus(bpmResult);
        contractMapper.updateById(new CrmContractDO().setId(id).setAuditStatus(auditStatus));
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
        // 1. 即将到期，需要查询合同配置
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
        // 2. 查询分页
        return contractMapper.selectPage(pageReqVO, userId, config);
    }

    @Override
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CUSTOMER, bizId = "#pageReqVO.customerId", level = CrmPermissionLevelEnum.READ)
    public PageResult<CrmContractDO> getContractPageByCustomerId(CrmContractPageReqVO pageReqVO) {
        return contractMapper.selectPageByCustomerId(pageReqVO);
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
    public List<CrmContractProductDO> getContractProductListByContractId(Long contactId) {
        return contractProductMapper.selectListByContractId(contactId);
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void claimContract(Long id, Long deptId, Long userId) {
        // 1. 校验合同是否存在
        CrmContractDO contract = validateContractExists(id);

        // 2. 解析分派部门信息
        if (cn.hutool.core.util.StrUtil.isEmpty(contract.getAssignDeptIds())) {
            throw exception(CONTRACT_ALREADY_CLAIMED);
        }
        List<ContractAssignDeptInfo> assignDeptInfoList = cn.hutool.json.JSONUtil.toList(
                contract.getAssignDeptIds(), ContractAssignDeptInfo.class);

        // 3. 查找要领取的部门
        ContractAssignDeptInfo targetDept = assignDeptInfoList.stream()
                .filter(info -> info.getDeptId().equals(deptId))
                .findFirst()
                .orElseThrow(() -> exception(CONTRACT_NOT_EXISTS));

        // 4. 校验该部门是否已被领取
        if (Boolean.TRUE.equals(targetDept.getClaimed())) {
            throw exception(CONTRACT_ALREADY_CLAIMED);
        }

        // 5. 校验当前用户是否是该部门的负责人
        List<DeptRespDTO> leaderDepts = deptApi.getDeptListByLeaderUserId(userId);
        boolean isLeader = leaderDepts != null && leaderDepts.stream()
                .anyMatch(dept -> dept.getId().equals(deptId));
        if (!isLeader) {
            log.warn("【合同领取】用户 {} 不是部门 {} 的负责人，无法领取", userId, deptId);
            throw exception(CONTRACT_NOT_EXISTS); // 没有权限
        }

        // 6. 获取用户信息
        AdminUserRespDTO user = adminUserApi.getUser(userId);
        String userName = user != null ? user.getNickname() : "";

        // 7. 更新该部门的领取状态
        targetDept.setClaimed(true);
        targetDept.setClaimUserId(userId);
        targetDept.setClaimUserName(userName);
        targetDept.setClaimTime(LocalDateTime.now());

        // 8. 检查是否所有部门都已领取
        boolean allClaimed = assignDeptInfoList.stream().allMatch(info -> Boolean.TRUE.equals(info.getClaimed()));

        // 9. 更新合同
        CrmContractDO updateContract = new CrmContractDO()
                .setId(id)
                .setAssignDeptIds(cn.hutool.json.JSONUtil.toJsonStr(assignDeptInfoList))
                .setClaimStatus(allClaimed ? 1 : 0);
        contractMapper.updateById(updateContract);

        // 10. 为领取人创建 WRITE 权限（让其成为参与者）
        // 先检查用户是否已有该合同的权限
        boolean hasPermission = crmPermissionService.hasPermission(
                CrmBizTypeEnum.CRM_CONTRACT.getType(), id, userId, CrmPermissionLevelEnum.OWNER)
                || crmPermissionService.hasPermission(
                        CrmBizTypeEnum.CRM_CONTRACT.getType(), id, userId, CrmPermissionLevelEnum.WRITE)
                || crmPermissionService.hasPermission(
                        CrmBizTypeEnum.CRM_CONTRACT.getType(), id, userId, CrmPermissionLevelEnum.READ);
        if (!hasPermission) {
            crmPermissionService.createPermission(new CrmPermissionCreateReqBO()
                    .setUserId(userId)
                    .setBizType(CrmBizTypeEnum.CRM_CONTRACT.getType())
                    .setBizId(id)
                    .setLevel(CrmPermissionLevelEnum.WRITE.getLevel()));
        } else {
            log.info("【合同领取】用户 {} 已有合同 {} 的权限，跳过创建权限", userId, id);
        }

        // 11. 将领取人添加为项目成员
        addClaimUserToProject(contract, userId, userName);

        log.info("【合同领取】用户 {} 成功领取合同 {} 的部门 {} 份额", userId, id, deptId);
    }

    @Override
    public PageResult<CrmContractDO> getPendingClaimContractPage(CrmContractPageReqVO pageReqVO, Long userId) {
        // 获取当前用户作为负责人的部门列表
        List<DeptRespDTO> leaderDepts = deptApi.getDeptListByLeaderUserId(userId);
        if (leaderDepts == null || leaderDepts.isEmpty()) {
            log.debug("【待领取合同】用户 {} 不是任何部门的负责人", userId);
            return PageResult.empty();
        }
        List<Long> leaderDeptIds = leaderDepts.stream().map(DeptRespDTO::getId).toList();
        log.debug("【待领取合同】用户 {} 是以下部门的负责人: {}", userId, leaderDeptIds);
        return contractMapper.selectPageByClaimStatusAndLeaderDeptIds(pageReqVO, leaderDeptIds);
    }

    /**
     * 发送钉钉通知给分派部门的负责人
     */
    private void sendDingtalkNotifyToAssignedDepts(CrmContractDO contract, List<Long> deptIds) {
        log.info("【合同通知】开始发送钉钉通知给部门负责人，合同编号={}, 分派部门={}", contract.getNo(), deptIds);

        // 获取钉钉配置
        List<DingtalkConfigDO> configs = dingtalkConfigService.getEnabledDingtalkConfigList();
        if (configs.isEmpty()) {
            log.warn("【合同通知】没有可用的钉钉配置，跳过通知");
            return;
        }
        DingtalkConfigDO config = configs.get(0);

        if (cn.hutool.core.util.StrUtil.isEmpty(config.getAgentId())) {
            log.warn("【合同通知】钉钉配置缺少agentId，跳过通知");
            return;
        }

        // 获取 access_token
        String accessToken = dingtalkApiService.getAccessToken(config);
        if (cn.hutool.core.util.StrUtil.isEmpty(accessToken)) {
            log.warn("【合同通知】获取accessToken失败，跳过通知");
            return;
        }

        // 获取分派部门信息（包含负责人）
        List<DeptRespDTO> depts = deptApi.getDeptList(deptIds);
        String deptNames = depts.stream().map(DeptRespDTO::getName).reduce((a, b) -> a + "、" + b).orElse("");

        // 收集所有部门负责人的钉钉ID
        List<String> dingtalkUserIds = new java.util.ArrayList<>();
        for (DeptRespDTO dept : depts) {
            Long leaderUserId = dept.getLeaderUserId();
            if (leaderUserId == null) {
                log.warn("【合同通知】部门 {} ({}) 没有设置负责人，跳过", dept.getName(), dept.getId());
                continue;
            }

            // 获取负责人的钉钉ID
            DingtalkMappingDO mapping = dingtalkMappingMapper.selectByLocalId(leaderUserId, "USER");
            if (mapping != null && cn.hutool.core.util.StrUtil.isNotEmpty(mapping.getDingtalkId())) {
                dingtalkUserIds.add(mapping.getDingtalkId());
                log.debug("【合同通知】部门 {} 负责人 userId={}, dingtalkId={}",
                        dept.getName(), leaderUserId, mapping.getDingtalkId());
            } else {
                log.warn("【合同通知】部门 {} 负责人 userId={} 没有钉钉映射", dept.getName(), leaderUserId);
            }
        }

        if (dingtalkUserIds.isEmpty()) {
            log.warn("【合同通知】没有可通知的部门负责人，跳过通知");
            return;
        }

        // 构建消息内容
        String title = "📋 您有新的合同待领取";
        String content = String.format(
                "### %s\n\n" +
                        "**合同编号：** %s\n\n" +
                        "**合同名称：** %s\n\n" +
                        "**分派部门：** %s\n\n" +
                        "---\n" +
                        "请登录系统领取合同",
                title,
                contract.getNo(),
                contract.getName(),
                deptNames);

        // 发送钉钉工作通知给所有部门负责人
        boolean success = dingtalkApiService.sendWorkNotice(
                accessToken,
                config.getAgentId(),
                dingtalkUserIds,
                title,
                content);

        if (success) {
            log.info("【合同通知】发送成功：contractNo={}, 负责人数量={}",
                    contract.getNo(), dingtalkUserIds.size());
        } else {
            log.error("【合同通知】发送失败：contractNo={}", contract.getNo());
        }
    }

    /**
     * 为合同创建对应的项目
     */
    private void createProjectForContract(CrmContractDO contract, CrmContractSaveReqVO createReqVO, Long userId) {
        log.info("【合同-项目】开始为合同 {} 创建项目，contractId={}, userId={}", contract.getNo(), contract.getId(), userId);
        try {
            // 确定部门类型（根据分派部门的第一个来确定，默认为1-安全服务）
            Integer deptType = 1;
            if (createReqVO.getAssignDeptIds() != null && !createReqVO.getAssignDeptIds().isEmpty()) {
                // 可以根据部门信息来确定 deptType，这里简化处理使用默认值
                // TODO: 可以根据部门的实际类型来设置
            }

            // 获取客户名称（直接使用 Mapper 绕过权限检查）
            String customerName = "";
            if (contract.getCustomerId() != null) {
                var customer = customerMapper.selectById(contract.getCustomerId());
                if (customer != null) {
                    customerName = customer.getName();
                }
            }

            // 创建项目
            ProjectSaveReqVO projectReqVO = new ProjectSaveReqVO();
            projectReqVO.setName(contract.getName()); // 使用合同名称作为项目名称
            projectReqVO.setDeptType(deptType);
            projectReqVO.setCustomerId(contract.getCustomerId());
            projectReqVO.setCustomerName(customerName);
            projectReqVO.setContractId(contract.getId());
            projectReqVO.setContractNo(contract.getNo());
            projectReqVO.setStatus(0); // 草稿状态
            projectReqVO.setDescription("由合同 " + contract.getNo() + " 自动创建");

            Long projectId = projectService.createProject(projectReqVO);

            // 将合同创建者添加为项目成员（项目经理角色）
            AdminUserRespDTO user = adminUserApi.getUser(userId);
            String userName = user != null ? user.getNickname() : "";
            projectService.addProjectMember(projectId, userId, userName, 1); // 1=项目经理

            log.info("【合同-项目】为合同 {} 创建了项目 {}，并添加用户 {} 为项目经理",
                    contract.getNo(), projectId, userId);

        } catch (Exception e) {
            log.error("【合同-项目】为合同 {} 创建项目失败: {}", contract.getNo(), e.getMessage(), e);
            // 不抛出异常，避免影响合同创建流程
        }
    }

    /**
     * 将领取人添加为项目成员
     */
    private void addClaimUserToProject(CrmContractDO contract, Long userId, String userName) {
        try {
            // 查找合同对应的项目
            ProjectDO project = projectService.getProjectByContractId(contract.getId());
            if (project == null) {
                log.warn("【合同-项目】合同 {} 没有对应的项目，跳过添加成员", contract.getId());
                return;
            }

            // 添加领取人为项目成员（执行人员角色）
            projectService.addProjectMember(project.getId(), userId, userName, 2); // 2=执行人员

            log.info("【合同-项目】已将用户 {} ({}) 添加为项目 {} 的执行人员",
                    userId, userName, project.getId());

        } catch (Exception e) {
            log.error("【合同-项目】添加项目成员失败: {}", e.getMessage(), e);
            // 不抛出异常，避免影响合同领取流程
        }
    }

}
