package cn.shuhe.system.module.project.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.common.util.object.BeanUtils;
import cn.shuhe.system.module.project.controller.admin.vo.*;
import cn.shuhe.system.module.project.dal.dataobject.SecurityOperationContractDO;
import cn.shuhe.system.module.project.dal.dataobject.SecurityOperationMemberDO;
import cn.shuhe.system.module.project.dal.dataobject.ServiceItemDO;
import cn.shuhe.system.module.project.dal.mysql.SecurityOperationContractMapper;
import cn.shuhe.system.module.project.dal.mysql.SecurityOperationMemberMapper;
import cn.shuhe.system.module.project.dal.mysql.ServiceItemMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static cn.shuhe.system.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.shuhe.system.module.project.enums.ErrorCodeConstants.*;

/**
 * 安全运营合同 Service 实现类
 */
@Service
@Validated
@Slf4j
public class SecurityOperationContractServiceImpl implements SecurityOperationContractService {

    @Resource
    private SecurityOperationContractMapper securityOperationContractMapper;

    @Resource
    private SecurityOperationMemberMapper securityOperationMemberMapper;

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

        // 3. 创建管理人员
        if (CollUtil.isNotEmpty(createReqVO.getManagementMembers())) {
            for (SecurityOperationMemberSaveReqVO memberVO : createReqVO.getManagementMembers()) {
                // 校验用户ID必填
                if (memberVO.getUserId() == null) {
                    log.warn("[createSecurityOperationContract] 管理人员 userId 为空，跳过: {}", memberVO);
                    continue;
                }
                SecurityOperationMemberDO member = BeanUtils.toBean(memberVO, SecurityOperationMemberDO.class);
                member.setSoContractId(contract.getId());
                member.setMemberType(SecurityOperationMemberDO.MEMBER_TYPE_MANAGEMENT);
                if (member.getStatus() == null) {
                    member.setStatus(SecurityOperationMemberDO.STATUS_ACTIVE);
                }
                log.info("[createSecurityOperationContract] 创建管理人员: contractId={}, userId={}, userName={}", 
                        contract.getId(), member.getUserId(), member.getUserName());
                securityOperationMemberMapper.insert(member);
            }
        }

        // 4. 创建驻场人员
        if (CollUtil.isNotEmpty(createReqVO.getOnsiteMembers())) {
            for (SecurityOperationMemberSaveReqVO memberVO : createReqVO.getOnsiteMembers()) {
                // 校验用户ID必填
                if (memberVO.getUserId() == null) {
                    log.warn("[createSecurityOperationContract] 驻场人员 userId 为空，跳过: {}", memberVO);
                    continue;
                }
                SecurityOperationMemberDO member = BeanUtils.toBean(memberVO, SecurityOperationMemberDO.class);
                member.setSoContractId(contract.getId());
                member.setMemberType(SecurityOperationMemberDO.MEMBER_TYPE_ONSITE);
                if (member.getStatus() == null) {
                    member.setStatus(SecurityOperationMemberDO.STATUS_ACTIVE);
                }
                log.info("[createSecurityOperationContract] 创建驻场人员: contractId={}, userId={}, userName={}", 
                        contract.getId(), member.getUserId(), member.getUserName());
                securityOperationMemberMapper.insert(member);
            }
        }

        // 5. 更新人员统计
        updateMemberCount(contract.getId());

        return contract.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSecurityOperationContract(SecurityOperationContractSaveReqVO updateReqVO) {
        // 1. 校验存在
        SecurityOperationContractDO existContract = validateSecurityOperationContractExists(updateReqVO.getId());

        // 2. 更新安全运营合同基本信息
        SecurityOperationContractDO updateObj = BeanUtils.toBean(updateReqVO, SecurityOperationContractDO.class);
        securityOperationContractMapper.updateById(updateObj);

        // 3. 更新管理人员（先删后增）
        log.info("[updateSecurityOperationContract] 开始更新管理人员, contractId={}", updateReqVO.getId());
        securityOperationMemberMapper.delete(new cn.shuhe.system.framework.mybatis.core.query.LambdaQueryWrapperX<SecurityOperationMemberDO>()
                .eq(SecurityOperationMemberDO::getSoContractId, updateReqVO.getId())
                .eq(SecurityOperationMemberDO::getMemberType, SecurityOperationMemberDO.MEMBER_TYPE_MANAGEMENT));
        if (CollUtil.isNotEmpty(updateReqVO.getManagementMembers())) {
            for (SecurityOperationMemberSaveReqVO memberVO : updateReqVO.getManagementMembers()) {
                // 校验用户ID必填
                if (memberVO.getUserId() == null) {
                    log.warn("[updateSecurityOperationContract] 管理人员 userId 为空，跳过: {}", memberVO);
                    continue;
                }
                SecurityOperationMemberDO member = BeanUtils.toBean(memberVO, SecurityOperationMemberDO.class);
                member.setId(null); // 新建
                member.setSoContractId(updateReqVO.getId());
                member.setMemberType(SecurityOperationMemberDO.MEMBER_TYPE_MANAGEMENT);
                if (member.getStatus() == null) {
                    member.setStatus(SecurityOperationMemberDO.STATUS_ACTIVE);
                }
                log.info("[updateSecurityOperationContract] 新增管理人员: contractId={}, userId={}, userName={}", 
                        updateReqVO.getId(), member.getUserId(), member.getUserName());
                securityOperationMemberMapper.insert(member);
            }
        }

        // 4. 更新驻场人员（先删后增）
        log.info("[updateSecurityOperationContract] 开始更新驻场人员, contractId={}", updateReqVO.getId());
        securityOperationMemberMapper.delete(new cn.shuhe.system.framework.mybatis.core.query.LambdaQueryWrapperX<SecurityOperationMemberDO>()
                .eq(SecurityOperationMemberDO::getSoContractId, updateReqVO.getId())
                .eq(SecurityOperationMemberDO::getMemberType, SecurityOperationMemberDO.MEMBER_TYPE_ONSITE));
        if (CollUtil.isNotEmpty(updateReqVO.getOnsiteMembers())) {
            for (SecurityOperationMemberSaveReqVO memberVO : updateReqVO.getOnsiteMembers()) {
                // 校验用户ID必填
                if (memberVO.getUserId() == null) {
                    log.warn("[updateSecurityOperationContract] 驻场人员 userId 为空，跳过: {}", memberVO);
                    continue;
                }
                SecurityOperationMemberDO member = BeanUtils.toBean(memberVO, SecurityOperationMemberDO.class);
                member.setId(null); // 新建
                member.setSoContractId(updateReqVO.getId());
                member.setMemberType(SecurityOperationMemberDO.MEMBER_TYPE_ONSITE);
                if (member.getStatus() == null) {
                    member.setStatus(SecurityOperationMemberDO.STATUS_ACTIVE);
                }
                log.info("[updateSecurityOperationContract] 新增驻场人员: contractId={}, userId={}, userName={}", 
                        updateReqVO.getId(), member.getUserId(), member.getUserName());
                securityOperationMemberMapper.insert(member);
            }
        }

        // 5. 更新人员统计
        updateMemberCount(updateReqVO.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSecurityOperationContract(Long id) {
        // 1. 校验存在
        validateSecurityOperationContractExists(id);

        // 2. 删除人员
        securityOperationMemberMapper.deleteBySoContractId(id);

        // 3. 解除服务项关联
        // TODO: 需要将关联服务项的 soContractId 置空

        // 4. 删除安全运营合同
        securityOperationContractMapper.deleteById(id);
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

        // 3. 获取管理人员
        List<SecurityOperationMemberDO> managementMembers = securityOperationMemberMapper
                .selectListBySoContractIdAndType(id, SecurityOperationMemberDO.MEMBER_TYPE_MANAGEMENT);
        respVO.setManagementMembers(convertMemberList(managementMembers));

        // 4. 获取驻场人员
        List<SecurityOperationMemberDO> onsiteMembers = securityOperationMemberMapper
                .selectListBySoContractIdAndType(id, SecurityOperationMemberDO.MEMBER_TYPE_ONSITE);
        respVO.setOnsiteMembers(convertMemberList(onsiteMembers));

        // 5. 获取服务项
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
        Long managementCount = securityOperationMemberMapper.countManagementBySoContractId(id);
        Long onsiteCount = securityOperationMemberMapper.countOnsiteBySoContractId(id);

        SecurityOperationContractDO updateObj = new SecurityOperationContractDO();
        updateObj.setId(id);
        updateObj.setManagementCount(managementCount.intValue());
        updateObj.setOnsiteCount(onsiteCount.intValue());
        securityOperationContractMapper.updateById(updateObj);
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

    /**
     * 转换人员列表
     */
    private List<SecurityOperationMemberRespVO> convertMemberList(List<SecurityOperationMemberDO> members) {
        if (CollUtil.isEmpty(members)) {
            return new ArrayList<>();
        }
        return members.stream().map(member -> {
            SecurityOperationMemberRespVO vo = BeanUtils.toBean(member, SecurityOperationMemberRespVO.class);
            // 设置类型名称
            vo.setMemberTypeName(member.getMemberType() == SecurityOperationMemberDO.MEMBER_TYPE_MANAGEMENT ? "管理人员" : "驻场人员");
            // 设置状态名称
            switch (member.getStatus()) {
                case 0:
                    vo.setStatusName("待入场");
                    break;
                case 1:
                    vo.setStatusName("在岗");
                    break;
                case 2:
                    vo.setStatusName("已离开");
                    break;
                default:
                    vo.setStatusName("未知");
            }
            return vo;
        }).collect(Collectors.toList());
    }

}
