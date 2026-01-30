package cn.shuhe.system.module.project.service;

import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.module.project.controller.admin.vo.SecurityOperationContractPageReqVO;
import cn.shuhe.system.module.project.controller.admin.vo.SecurityOperationContractRespVO;
import cn.shuhe.system.module.project.controller.admin.vo.SecurityOperationContractSaveReqVO;
import cn.shuhe.system.module.project.dal.dataobject.SecurityOperationContractDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 安全运营合同 Service 接口
 */
public interface SecurityOperationContractService {

    /**
     * 创建安全运营合同
     *
     * @param createReqVO 创建信息
     * @return 安全运营合同ID
     */
    Long createSecurityOperationContract(@Valid SecurityOperationContractSaveReqVO createReqVO);

    /**
     * 更新安全运营合同
     *
     * @param updateReqVO 更新信息
     */
    void updateSecurityOperationContract(@Valid SecurityOperationContractSaveReqVO updateReqVO);

    /**
     * 删除安全运营合同
     *
     * @param id 安全运营合同ID
     */
    void deleteSecurityOperationContract(Long id);

    /**
     * 获得安全运营合同
     *
     * @param id 安全运营合同ID
     * @return 安全运营合同
     */
    SecurityOperationContractDO getSecurityOperationContract(Long id);

    /**
     * 获得安全运营合同详情（包含人员和服务项）
     *
     * @param id 安全运营合同ID
     * @return 安全运营合同详情
     */
    SecurityOperationContractRespVO getSecurityOperationContractDetail(Long id);

    /**
     * 获得安全运营合同分页
     *
     * @param pageReqVO 分页查询
     * @return 安全运营合同分页
     */
    PageResult<SecurityOperationContractDO> getSecurityOperationContractPage(SecurityOperationContractPageReqVO pageReqVO);

    /**
     * 根据合同ID获取安全运营合同
     *
     * @param contractId 合同ID
     * @return 安全运营合同
     */
    SecurityOperationContractDO getByContractId(Long contractId);

    /**
     * 根据客户ID获取安全运营合同列表
     *
     * @param customerId 客户ID
     * @return 安全运营合同列表
     */
    List<SecurityOperationContractDO> getListByCustomerId(Long customerId);

    /**
     * 更新安全运营合同状态
     *
     * @param id     安全运营合同ID
     * @param status 状态
     */
    void updateStatus(Long id, Integer status);

    /**
     * 更新人员统计
     *
     * @param id 安全运营合同ID
     */
    void updateMemberCount(Long id);

}
