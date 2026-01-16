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

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = CRM_CONTRACT_TYPE, subType = CRM_CONTRACT_CREATE_SUB_TYPE, bizNo = "{{#contract.id}}",
            success = CRM_CONTRACT_CREATE_SUCCESS)
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
        // 处理分派部门IDs转JSON
        if (createReqVO.getAssignDeptIds() != null && !createReqVO.getAssignDeptIds().isEmpty()) {
            contract.setAssignDeptIds(cn.hutool.json.JSONUtil.toJsonStr(createReqVO.getAssignDeptIds()));
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

        // 5. 记录操作日志上下文
        LogRecordContext.putVariable("contract", contract);
        return contract.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = CRM_CONTRACT_TYPE, subType = CRM_CONTRACT_UPDATE_SUB_TYPE, bizNo = "{{#updateReqVO.id}}",
            success = CRM_CONTRACT_UPDATE_SUCCESS)
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
        LogRecordContext.putVariable(DiffParseFunction.OLD_OBJECT, BeanUtils.toBean(oldContract, CrmContractSaveReqVO.class));
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
        BigDecimal totalProductPrice = getSumValue(contractProducts, CrmContractProductDO::getTotalPrice, BigDecimal::add, BigDecimal.ZERO);
        contract.setTotalProductPrice(totalProductPrice);
        
        // 如果已经设置了合同总价（手动输入），则使用该值；否则根据产品计算
        if (contract.getTotalPrice() != null && contract.getTotalPrice().compareTo(BigDecimal.ZERO) > 0) {
            // 使用手动输入的合同总价
            return;
        }
        
        // 根据产品计算总价
        BigDecimal discountPercent = contract.getDiscountPercent() != null ? contract.getDiscountPercent() : BigDecimal.ZERO;
        BigDecimal discountPrice = MoneyUtils.priceMultiplyPercent(totalProductPrice, discountPercent);
        contract.setTotalPrice(totalProductPrice.subtract(discountPrice != null ? discountPrice : BigDecimal.ZERO));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = CRM_CONTRACT_TYPE, subType = CRM_CONTRACT_DELETE_SUB_TYPE, bizNo = "{{#id}}",
            success = CRM_CONTRACT_DELETE_SUCCESS)
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
    @LogRecord(type = CRM_CONTRACT_TYPE, subType = CRM_CONTRACT_TRANSFER_SUB_TYPE, bizNo = "{{#reqVO.id}}",
            success = CRM_CONTRACT_TRANSFER_SUCCESS)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CONTRACT, bizId = "#reqVO.id", level = CrmPermissionLevelEnum.OWNER)
    public void transferContract(CrmContractTransferReqVO reqVO, Long userId) {
        // 1. 校验合同是否存在
        CrmContractDO contract = validateContractExists(reqVO.getId());

        // 2.1 数据权限转移
        crmPermissionService.transferPermission(new CrmPermissionTransferReqBO(userId, CrmBizTypeEnum.CRM_CONTRACT.getType(),
                reqVO.getId(), reqVO.getNewOwnerUserId(), reqVO.getOldOwnerPermissionLevel()));
        // 2.2 设置负责人
        contractMapper.updateById(new CrmContractDO().setId(reqVO.getId()).setOwnerUserId(reqVO.getNewOwnerUserId()));

        // 3. 记录转移日志
        LogRecordContext.putVariable("contract", contract);
    }

    @Override
    @LogRecord(type = CRM_CONTRACT_TYPE, subType = CRM_CONTRACT_FOLLOW_UP_SUB_TYPE, bizNo = "{{#id}}",
            success = CRM_CONTRACT_FOLLOW_UP_SUCCESS)
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
    @LogRecord(type = CRM_CONTRACT_TYPE, subType = CRM_CONTRACT_SUBMIT_SUB_TYPE, bizNo = "{{#id}}",
            success = CRM_CONTRACT_SUBMIT_SUCCESS)
    public void submitContract(Long id, Long userId) {
        // 1. 校验合同是否在审批
        CrmContractDO contract = validateContractExists(id);
        if (ObjUtil.notEqual(contract.getAuditStatus(), CrmAuditStatusEnum.DRAFT.getStatus())) {
            throw exception(CONTRACT_SUBMIT_FAIL_NOT_DRAFT);
        }

        // 2. 创建合同审批流程实例
        String processInstanceId = bpmProcessInstanceApi.createProcessInstance(userId, new BpmProcessInstanceCreateReqDTO()
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
    public void claimContract(Long id, Long userId) {
        // 1. 校验合同是否存在
        CrmContractDO contract = validateContractExists(id);
        // 2. 校验是否已被领取
        if (contract.getClaimStatus() != null && contract.getClaimStatus() == 1) {
            throw exception(CONTRACT_ALREADY_CLAIMED);
        }
        // 3. 更新合同领取状态
        contractMapper.updateById(new CrmContractDO()
                .setId(id)
                .setClaimStatus(1)
                .setClaimUserId(userId)
                .setClaimTime(LocalDateTime.now())
                .setOwnerUserId(userId)); // 领取后负责人为领取人

        // 4. 创建数据权限（如果不存在）
        crmPermissionService.createPermission(new CrmPermissionCreateReqBO()
                .setUserId(userId)
                .setBizType(CrmBizTypeEnum.CRM_CONTRACT.getType())
                .setBizId(id)
                .setLevel(CrmPermissionLevelEnum.OWNER.getLevel()));
    }

    @Override
    public PageResult<CrmContractDO> getPendingClaimContractPage(CrmContractPageReqVO pageReqVO, Long userId) {
        // 获取用户所在部门
        Long deptId = adminUserApi.getUser(userId).getDeptId();
        if (deptId == null) {
            return PageResult.empty();
        }
        return contractMapper.selectPageByClaimStatusAndDeptId(pageReqVO, deptId);
    }

    /**
     * 发送钉钉通知给分派部门的人员
     */
    private void sendDingtalkNotifyToAssignedDepts(CrmContractDO contract, List<Long> deptIds) {
        log.info("【合同通知】开始发送钉钉通知，合同编号={}, 分派部门={}", contract.getNo(), deptIds);
        
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
        
        // 获取分派部门名称
        List<DeptRespDTO> depts = deptApi.getDeptList(deptIds);
        String deptNames = depts.stream().map(DeptRespDTO::getName).reduce((a, b) -> a + "、" + b).orElse("");
        
        // 获取部门下的所有用户
        for (Long deptId : deptIds) {
            List<AdminUserRespDTO> users = adminUserApi.getUserListByDeptIds(java.util.Collections.singletonList(deptId));
            if (users == null || users.isEmpty()) {
                log.debug("【合同通知】部门 {} 没有用户", deptId);
                continue;
            }
            
            // 获取用户的钉钉ID列表
            List<String> dingtalkUserIds = new java.util.ArrayList<>();
            for (AdminUserRespDTO user : users) {
                DingtalkMappingDO mapping = dingtalkMappingMapper.selectByLocalId(user.getId(), "USER");
                if (mapping != null && cn.hutool.core.util.StrUtil.isNotEmpty(mapping.getDingtalkId())) {
                    dingtalkUserIds.add(mapping.getDingtalkId());
                }
            }
            
            if (dingtalkUserIds.isEmpty()) {
                log.debug("【合同通知】部门 {} 的用户没有钉钉映射", deptId);
                continue;
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
                    deptNames
            );
            
            // 发送钉钉工作通知
            boolean success = dingtalkApiService.sendWorkNotice(
                    accessToken,
                    config.getAgentId(),
                    dingtalkUserIds,
                    title,
                    content
            );
            
            if (success) {
                log.info("【合同通知】发送成功：contractNo={}, deptId={}, userCount={}", 
                        contract.getNo(), deptId, dingtalkUserIds.size());
            } else {
                log.error("【合同通知】发送失败：contractNo={}, deptId={}", contract.getNo(), deptId);
            }
        }
    }

}
