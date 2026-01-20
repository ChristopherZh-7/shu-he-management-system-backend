package cn.shuhe.system.module.project.dal.mysql;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 合同时间查询 Mapper
 * 
 * 用于跨模块查询 CRM 合同的时间信息（避免循环依赖）
 */
@Mapper
public interface ContractTimeMapper {

    /**
     * 根据合同ID查询合同时间
     * 
     * @param contractId 合同ID
     * @return 包含 startTime 和 endTime 的 Map
     */
    @Select("SELECT start_time as startTime, end_time as endTime FROM crm_contract WHERE id = #{contractId} AND deleted = 0")
    Map<String, LocalDateTime> selectContractTime(@Param("contractId") Long contractId);

}
