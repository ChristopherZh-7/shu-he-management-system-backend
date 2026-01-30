package cn.shuhe.system.module.system.dal.mysql.cost;

import cn.shuhe.system.framework.mybatis.core.mapper.BaseMapperX;
import cn.shuhe.system.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.shuhe.system.module.system.dal.dataobject.cost.ServiceItemAllocationDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 服务项金额分配 Mapper
 */
@Mapper
public interface ServiceItemAllocationMapper extends BaseMapperX<ServiceItemAllocationDO> {

    /**
     * 根据合同部门分配ID查询服务项分配列表
     */
    default List<ServiceItemAllocationDO> selectByContractDeptAllocationId(Long contractDeptAllocationId) {
        return selectList(new LambdaQueryWrapperX<ServiceItemAllocationDO>()
                .eq(ServiceItemAllocationDO::getContractDeptAllocationId, contractDeptAllocationId)
                .orderByAsc(ServiceItemAllocationDO::getId));
    }

    /**
     * 根据服务项ID查询分配列表
     */
    default List<ServiceItemAllocationDO> selectByServiceItemId(Long serviceItemId) {
        return selectList(new LambdaQueryWrapperX<ServiceItemAllocationDO>()
                .eq(ServiceItemAllocationDO::getServiceItemId, serviceItemId)
                .orderByDesc(ServiceItemAllocationDO::getCreateTime));
    }

    /**
     * 根据合同部门分配ID和服务项ID查询
     */
    default ServiceItemAllocationDO selectByAllocationIdAndServiceItemId(Long contractDeptAllocationId, Long serviceItemId) {
        return selectOne(new LambdaQueryWrapperX<ServiceItemAllocationDO>()
                .eq(ServiceItemAllocationDO::getContractDeptAllocationId, contractDeptAllocationId)
                .eq(ServiceItemAllocationDO::getServiceItemId, serviceItemId));
    }

    /**
     * 根据合同部门分配ID和分配类型查询（用于安全运营费用分配）
     */
    default ServiceItemAllocationDO selectByAllocationIdAndType(Long contractDeptAllocationId, String allocationType) {
        return selectOne(new LambdaQueryWrapperX<ServiceItemAllocationDO>()
                .eq(ServiceItemAllocationDO::getContractDeptAllocationId, contractDeptAllocationId)
                .eq(ServiceItemAllocationDO::getAllocationType, allocationType));
    }

    /**
     * 根据合同部门分配ID和分配类型列表查询
     */
    default List<ServiceItemAllocationDO> selectByAllocationIdAndTypes(Long contractDeptAllocationId, List<String> allocationTypes) {
        return selectList(new LambdaQueryWrapperX<ServiceItemAllocationDO>()
                .eq(ServiceItemAllocationDO::getContractDeptAllocationId, contractDeptAllocationId)
                .in(ServiceItemAllocationDO::getAllocationType, allocationTypes)
                .orderByAsc(ServiceItemAllocationDO::getId));
    }

    /**
     * 批量删除：根据合同部门分配ID删除所有关联的服务项分配
     */
    default int deleteByContractDeptAllocationId(Long contractDeptAllocationId) {
        return delete(new LambdaQueryWrapperX<ServiceItemAllocationDO>()
                .eq(ServiceItemAllocationDO::getContractDeptAllocationId, contractDeptAllocationId));
    }

}
