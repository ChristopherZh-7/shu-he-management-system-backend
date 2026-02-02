package cn.shuhe.system.module.project.service;

import cn.hutool.core.collection.CollUtil;
import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.common.util.object.BeanUtils;
import cn.shuhe.system.module.project.controller.admin.vo.*;
import cn.shuhe.system.module.project.dal.dataobject.ProjectSiteMemberDO;
import cn.shuhe.system.module.project.dal.dataobject.SecurityOperationContractDO;
import cn.shuhe.system.module.project.dal.dataobject.ServiceItemDO;
import cn.shuhe.system.module.project.dal.mysql.ProjectSiteMemberMapper;
import cn.shuhe.system.module.project.dal.mysql.SecurityOperationContractMapper;
import cn.shuhe.system.module.project.dal.mysql.ServiceItemMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.List;

import static cn.shuhe.system.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.shuhe.system.module.project.enums.ErrorCodeConstants.*;

/**
 * 安全运营合同 Service 实现类
 * 
 * 注意：人员管理已迁移到 ProjectSiteService 和 ProjectSiteMemberService，
 * 此 Service 只管理费用相关信息
 */
@Service
@Validated
@Slf4j
public class SecurityOperationContractServiceImpl implements SecurityOperationContractService {

    @Resource
    private SecurityOperationContractMapper securityOperationContractMapper;

    @Resource
    private ProjectSiteMemberMapper projectSiteMemberMapper;

    @Resource
    private ServiceItemMapper serviceItemMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSecurityOperationContract(SecurityOperationContractSaveReqVO createReqVO) {
        // 1. 校验合同是否已存在安全运营记录
        SecurityOperationContractDO existContract = securityOperationContractMapper.selectByContractId(createReqVO.getContractId());
        if (existContract != null) {
            throw exception(SECURITY_OPERATION_CONTRACT_EXISTS);
        }

        // 2. 创建安全运营合同
        SecurityOperationContractDO contract = BeanUtils.toBean(createReqVO, SecurityOperationContractDO.class);
        contract.setManagementCount(0);
        contract.setOnsiteCount(0);
        contract.setStatus(0); // 待启动
        securityOperationContractMapper.insert(contract);

        log.info("[createSecurityOperationContract] 创建安全运营合同成功，id={}, contractId={}", 
                contract.getId(), contract.getContractId());

        return contract.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSecurityOperationContract(SecurityOperationContractSaveReqVO updateReqVO) {
        // 1. 校验存在
        validateSecurityOperationContractExists(updateReqVO.getId());

        // 2. 更新安全运营合同基本信息
        SecurityOperationContractDO updateObj = BeanUtils.toBean(updateReqVO, SecurityOperationContractDO.class);
        securityOperationContractMapper.updateById(updateObj);

        log.info("[updateSecurityOperationContract] 更新安全运营合同成功，id={}", updateReqVO.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSecurityOperationContract(Long id) {
        // 1. 校验存在
        validateSecurityOperationContractExists(id);

        // 2. 删除安全运营合同
        securityOperationContractMapper.deleteById(id);

        log.info("[deleteSecurityOperationContract] 删除安全运营合同成功，id={}", id);
    }

    @Override
    public SecurityOperationContractDO getSecurityOperationContract(Long id) {
        return securityOperationContractMapper.selectById(id);
    }

    @Override
    public SecurityOperationContractRespVO getSecurityOperationContractDetail(Long id) {
        // 1. 获取安全运营合同
        SecurityOperationContractDO contract = validateSecurityOperationContractExists(id);
        SecurityOperationContractRespVO respVO = BeanUtils.toBean(contract, SecurityOperationContractRespVO.class);

        // 2. 计算总费用
        BigDecimal managementFee = contract.getManagementFee() != null ? contract.getManagementFee() : BigDecimal.ZERO;
        BigDecimal onsiteFee = contract.getOnsiteFee() != null ? contract.getOnsiteFee() : BigDecimal.ZERO;
        respVO.setTotalFee(managementFee.add(onsiteFee));

        // 3. 获取服务项
        List<ServiceItemDO> serviceItems = serviceItemMapper.selectListBySoContractId(id);
        respVO.setServiceItemCount(serviceItems.size());
        respVO.setServiceItems(BeanUtils.toBean(serviceItems, ServiceItemRespVO.class));

        return respVO;
    }

    @Override
    public PageResult<SecurityOperationContractDO> getSecurityOperationContractPage(SecurityOperationContractPageReqVO pageReqVO) {
        return securityOperationContractMapper.selectPage(pageReqVO);
    }

    @Override
    public SecurityOperationContractDO getByContractId(Long contractId) {
        return securityOperationContractMapper.selectByContractId(contractId);
    }

    @Override
    public List<SecurityOperationContractDO> getListByCustomerId(Long customerId) {
        return securityOperationContractMapper.selectListByCustomerId(customerId);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        // 校验存在
        validateSecurityOperationContractExists(id);
        // 更新状态
        SecurityOperationContractDO updateObj = new SecurityOperationContractDO();
        updateObj.setId(id);
        updateObj.setStatus(status);
        securityOperationContractMapper.updateById(updateObj);
    }

    @Override
    public void updateMemberCount(Long id) {
        // 注意：人员统计现在从 project_site_member 表获取
        // 但由于关联关系变化，这个方法暂时保留空实现
        // 后续可以根据项目ID统计驻场人员
        log.debug("[updateMemberCount] 人员统计已迁移到 ProjectSite，此方法暂不执行，id={}", id);
    }

    /**
     * 校验安全运营合同是否存在
     */
    private SecurityOperationContractDO validateSecurityOperationContractExists(Long id) {
        SecurityOperationContractDO contract = securityOperationContractMapper.selectById(id);
        if (contract == null) {
            throw exception(SECURITY_OPERATION_CONTRACT_NOT_EXISTS);
        }
        return contract;
    }

}
