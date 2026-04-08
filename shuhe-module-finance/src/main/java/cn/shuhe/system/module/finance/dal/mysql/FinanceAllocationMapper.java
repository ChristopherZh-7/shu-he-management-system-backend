package cn.shuhe.system.module.finance.dal.mysql;

import cn.shuhe.system.framework.mybatis.core.mapper.BaseMapperX;
import cn.shuhe.system.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.shuhe.system.module.finance.dal.dataobject.FinanceAllocationDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface FinanceAllocationMapper extends BaseMapperX<FinanceAllocationDO> {

    default List<FinanceAllocationDO> selectListByContractId(Long contractId) {
        return selectList(new LambdaQueryWrapperX<FinanceAllocationDO>()
                .eq(FinanceAllocationDO::getContractId, contractId)
                .orderByAsc(FinanceAllocationDO::getAllocationLevel)
                .orderByAsc(FinanceAllocationDO::getId));
    }

    default List<FinanceAllocationDO> selectListByContractIdAndLevel(Long contractId, Integer level) {
        return selectList(new LambdaQueryWrapperX<FinanceAllocationDO>()
                .eq(FinanceAllocationDO::getContractId, contractId)
                .eq(FinanceAllocationDO::getAllocationLevel, level));
    }

    default List<FinanceAllocationDO> selectListByParentId(Long parentId) {
        return selectList(new LambdaQueryWrapperX<FinanceAllocationDO>()
                .eq(FinanceAllocationDO::getParentId, parentId)
                .orderByAsc(FinanceAllocationDO::getId));
    }

    @Select("SELECT COALESCE(SUM(allocated_amount), 0) FROM finance_allocation WHERE parent_id = #{parentId} AND deleted = 0")
    BigDecimal sumChildAllocations(@Param("parentId") Long parentId);

    @Select("SELECT COALESCE(SUM(allocated_amount), 0) FROM finance_allocation WHERE contract_id = #{contractId} AND allocation_level = 1 AND deleted = 0")
    BigDecimal sumLevel1Allocations(@Param("contractId") Long contractId);

}
