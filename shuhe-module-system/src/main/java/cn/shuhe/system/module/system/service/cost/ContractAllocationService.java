package cn.shuhe.system.module.system.service.cost;

import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.module.system.controller.admin.cost.vo.*;
import cn.shuhe.system.module.system.dal.dataobject.cost.ContractDeptAllocationDO;
import cn.shuhe.system.module.system.dal.dataobject.cost.ServiceItemAllocationDO;

import java.util.List;

/**
 * 合同收入分配 Service
 */
public interface ContractAllocationService {

    // ========== 合同部门分配 ==========

    /**
     * 创建合同部门分配
     *
     * @param reqVO 创建信息
     * @return 分配ID
     */
    Long createContractDeptAllocation(ContractDeptAllocationSaveReqVO reqVO);

    /**
     * 更新合同部门分配
     *
     * @param reqVO 更新信息
     */
    void updateContractDeptAllocation(ContractDeptAllocationSaveReqVO reqVO);

    /**
     * 删除合同部门分配
     *
     * @param id 分配ID
     */
    void deleteContractDeptAllocation(Long id);

    /**
     * 获取合同部门分配
     *
     * @param id 分配ID
     * @return 分配信息
     */
    ContractDeptAllocationRespVO getContractDeptAllocation(Long id);

    /**
     * 分页查询合同部门分配
     *
     * @param reqVO 查询条件
     * @return 分页结果
     */
    PageResult<ContractDeptAllocationRespVO> getContractDeptAllocationPage(ContractDeptAllocationPageReqVO reqVO);

    /**
     * 获取合同的分配详情（包含所有部门分配）
     *
     * @param contractId 合同ID
     * @return 分配详情
     */
    ContractAllocationDetailRespVO getContractAllocationDetail(Long contractId);

    /**
     * 根据合同ID获取部门分配列表
     *
     * @param contractId 合同ID
     * @return 部门分配列表
     */
    List<ContractDeptAllocationDO> getContractDeptAllocationsByContractId(Long contractId);

    /**
     * 根据部门ID获取分配列表（用于部门视图）
     *
     * @param deptId 部门ID
     * @return 分配列表
     */
    List<ContractDeptAllocationRespVO> getContractDeptAllocationsByDeptId(Long deptId);

    // ========== 服务项分配 ==========

    /**
     * 创建服务项金额分配
     *
     * @param reqVO 创建信息
     * @return 分配ID
     */
    Long createServiceItemAllocation(ServiceItemAllocationSaveReqVO reqVO);

    /**
     * 更新服务项金额分配
     *
     * @param reqVO 更新信息
     */
    void updateServiceItemAllocation(ServiceItemAllocationSaveReqVO reqVO);

    /**
     * 删除服务项金额分配
     *
     * @param id 分配ID
     */
    void deleteServiceItemAllocation(Long id);

    /**
     * 获取服务项金额分配
     *
     * @param id 分配ID
     * @return 分配信息
     */
    ServiceItemAllocationRespVO getServiceItemAllocation(Long id);

    /**
     * 根据合同部门分配ID获取服务项分配列表
     *
     * @param contractDeptAllocationId 合同部门分配ID
     * @return 服务项分配列表
     */
    List<ServiceItemAllocationRespVO> getServiceItemAllocationsByDeptAllocationId(Long contractDeptAllocationId);

    /**
     * 根据服务项ID获取其所有金额分配
     *
     * @param serviceItemId 服务项ID
     * @return 分配列表
     */
    List<ServiceItemAllocationDO> getServiceItemAllocationsByServiceItemId(Long serviceItemId);

}
