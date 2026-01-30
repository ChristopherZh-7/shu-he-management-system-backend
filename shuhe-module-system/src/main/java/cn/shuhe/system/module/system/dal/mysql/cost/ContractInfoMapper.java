package cn.shuhe.system.module.system.dal.mysql.cost;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 合同信息查询 Mapper
 * 
 * 用于跨模块查询 CRM 合同信息（避免循环依赖）
 */
@Mapper
public interface ContractInfoMapper {

    /**
     * 根据合同ID查询合同基本信息
     * 
     * @param contractId 合同ID
     * @return 包含合同信息的 Map（id, no, name, customerName, totalPrice）
     */
    @Select("SELECT c.id, c.no as contractNo, c.name as contractName, cu.name as customerName, c.total_price as totalPrice " +
            "FROM crm_contract c " +
            "LEFT JOIN crm_customer cu ON c.customer_id = cu.id AND cu.deleted = 0 " +
            "WHERE c.id = #{contractId} AND c.deleted = 0")
    Map<String, Object> selectContractInfo(@Param("contractId") Long contractId);

    /**
     * 查询所有可用的合同列表（用于下拉选择）
     * 
     * @return 合同列表
     */
    @Select("SELECT c.id, c.no as contractNo, c.name as contractName, cu.name as customerName, c.total_price as totalPrice " +
            "FROM crm_contract c " +
            "LEFT JOIN crm_customer cu ON c.customer_id = cu.id AND cu.deleted = 0 " +
            "WHERE c.deleted = 0 AND c.audit_status = 20 " +  // 20 = 已审批通过
            "ORDER BY c.create_time DESC")
    List<Map<String, Object>> selectContractList();

    /**
     * 根据合同编号模糊查询合同列表
     * 
     * @param contractNo 合同编号（模糊）
     * @param customerName 客户名称（模糊）
     * @return 合同列表
     */
    @Select("<script>" +
            "SELECT c.id, c.no as contractNo, c.name as contractName, cu.name as customerName, c.total_price as totalPrice " +
            "FROM crm_contract c " +
            "LEFT JOIN crm_customer cu ON c.customer_id = cu.id AND cu.deleted = 0 " +
            "WHERE c.deleted = 0 AND c.audit_status = 20 " +
            "<if test='contractNo != null and contractNo != \"\"'> AND c.no LIKE CONCAT('%', #{contractNo}, '%') </if>" +
            "<if test='customerName != null and customerName != \"\"'> AND cu.name LIKE CONCAT('%', #{customerName}, '%') </if>" +
            "ORDER BY c.create_time DESC " +
            "LIMIT 50" +
            "</script>")
    List<Map<String, Object>> searchContracts(@Param("contractNo") String contractNo, @Param("customerName") String customerName);

}
