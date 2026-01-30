package cn.shuhe.system.module.system.dal.mysql.cost;

import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.mybatis.core.mapper.BaseMapperX;
import cn.shuhe.system.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.shuhe.system.module.system.dal.dataobject.cost.ContractDeptAllocationDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 合同部门分配 Mapper
 */
@Mapper
public interface ContractDeptAllocationMapper extends BaseMapperX<ContractDeptAllocationDO> {

    /**
     * 根据合同ID查询部门分配列表
     */
    default List<ContractDeptAllocationDO> selectByContractId(Long contractId) {
        return selectList(new LambdaQueryWrapperX<ContractDeptAllocationDO>()
                .eq(ContractDeptAllocationDO::getContractId, contractId)
                .orderByAsc(ContractDeptAllocationDO::getId));
    }

    /**
     * 根据部门ID查询分配列表
     */
    default List<ContractDeptAllocationDO> selectByDeptId(Long deptId) {
        return selectList(new LambdaQueryWrapperX<ContractDeptAllocationDO>()
                .eq(ContractDeptAllocationDO::getDeptId, deptId)
                .orderByDesc(ContractDeptAllocationDO::getCreateTime));
    }

    /**
     * 根据合同ID和部门ID查询
     */
    default ContractDeptAllocationDO selectByContractIdAndDeptId(Long contractId, Long deptId) {
        return selectOne(new LambdaQueryWrapperX<ContractDeptAllocationDO>()
                .eq(ContractDeptAllocationDO::getContractId, contractId)
                .eq(ContractDeptAllocationDO::getDeptId, deptId));
    }

    /**
     * 分页查询
     */
    default PageResult<ContractDeptAllocationDO> selectPage(Long contractId, Long deptId, String contractNo,
                                                             String customerName, Integer pageNo, Integer pageSize) {
        return selectPage(new cn.shuhe.system.framework.common.pojo.PageParam()
                        .setPageNo(pageNo).setPageSize(pageSize),
                new LambdaQueryWrapperX<ContractDeptAllocationDO>()
                        .eqIfPresent(ContractDeptAllocationDO::getContractId, contractId)
                        .eqIfPresent(ContractDeptAllocationDO::getDeptId, deptId)
                        .likeIfPresent(ContractDeptAllocationDO::getContractNo, contractNo)
                        .likeIfPresent(ContractDeptAllocationDO::getCustomerName, customerName)
                        .orderByDesc(ContractDeptAllocationDO::getCreateTime));
    }

    /**
     * 查询合同分配的部门数量
     */
    default Long selectCountByContractId(Long contractId) {
        return selectCount(new LambdaQueryWrapperX<ContractDeptAllocationDO>()
                .eq(ContractDeptAllocationDO::getContractId, contractId));
    }

}
