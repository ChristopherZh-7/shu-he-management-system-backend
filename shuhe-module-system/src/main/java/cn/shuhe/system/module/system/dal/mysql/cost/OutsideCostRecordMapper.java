package cn.shuhe.system.module.system.dal.mysql.cost;

import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.mybatis.core.mapper.BaseMapperX;
import cn.shuhe.system.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.shuhe.system.module.system.controller.admin.cost.vo.OutsideCostRecordPageReqVO;
import cn.shuhe.system.module.system.dal.dataobject.cost.OutsideCostRecordDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

/**
 * 外出费用记录 Mapper
 */
@Mapper
public interface OutsideCostRecordMapper extends BaseMapperX<OutsideCostRecordDO> {

    default PageResult<OutsideCostRecordDO> selectPage(OutsideCostRecordPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<OutsideCostRecordDO>()
                .eqIfPresent(OutsideCostRecordDO::getContractId, reqVO.getContractId())
                .eqIfPresent(OutsideCostRecordDO::getTargetDeptId, reqVO.getTargetDeptId())
                .eqIfPresent(OutsideCostRecordDO::getStatus, reqVO.getStatus())
                .likeIfPresent(OutsideCostRecordDO::getContractNo, reqVO.getContractNo())
                .orderByDesc(OutsideCostRecordDO::getId));
    }

    default List<OutsideCostRecordDO> selectByContractId(Long contractId) {
        return selectList(new LambdaQueryWrapperX<OutsideCostRecordDO>()
                .eq(OutsideCostRecordDO::getContractId, contractId)
                .orderByDesc(OutsideCostRecordDO::getId));
    }

    default OutsideCostRecordDO selectByOutsideRequestId(Long outsideRequestId) {
        return selectOne(new LambdaQueryWrapperX<OutsideCostRecordDO>()
                .eq(OutsideCostRecordDO::getOutsideRequestId, outsideRequestId));
    }

    /**
     * 根据服务发起ID查询费用记录
     */
    default OutsideCostRecordDO selectByServiceLaunchId(Long serviceLaunchId) {
        return selectOne(new LambdaQueryWrapperX<OutsideCostRecordDO>()
                .eq(OutsideCostRecordDO::getServiceLaunchId, serviceLaunchId));
    }

    /**
     * 统计合同的外出费用总额
     */
    @Select("SELECT COALESCE(SUM(amount), 0) FROM outside_cost_record WHERE contract_id = #{contractId} AND status = 1 AND deleted = 0")
    BigDecimal sumAmountByContractId(@Param("contractId") Long contractId);

    /**
     * 统计合同的外出费用笔数
     */
    @Select("SELECT COUNT(*) FROM outside_cost_record WHERE contract_id = #{contractId} AND status = 1 AND deleted = 0")
    Integer countByContractId(@Param("contractId") Long contractId);

    /**
     * 统计某部门在某合同下的跨部门费用支出（作为发起方）
     * 只统计已完成状态(status=2)的记录
     */
    @Select("SELECT COALESCE(SUM(amount), 0) FROM outside_cost_record " +
            "WHERE contract_id = #{contractId} AND request_dept_id = #{deptId} AND status = 2 AND deleted = 0")
    BigDecimal sumExpenseByContractIdAndDeptId(@Param("contractId") Long contractId, @Param("deptId") Long deptId);

    /**
     * 统计某部门在某合同下的跨部门费用收入（作为目标方）
     * 只统计已完成状态(status=2)的记录
     */
    @Select("SELECT COALESCE(SUM(amount), 0) FROM outside_cost_record " +
            "WHERE contract_id = #{contractId} AND target_dept_id = #{deptId} AND status = 2 AND deleted = 0")
    BigDecimal sumIncomeByContractIdAndDeptId(@Param("contractId") Long contractId, @Param("deptId") Long deptId);

    /**
     * 统计某部门在某合同下的跨部门费用支出笔数（作为发起方）
     */
    @Select("SELECT COUNT(*) FROM outside_cost_record " +
            "WHERE contract_id = #{contractId} AND request_dept_id = #{deptId} AND status = 2 AND deleted = 0")
    Integer countExpenseByContractIdAndDeptId(@Param("contractId") Long contractId, @Param("deptId") Long deptId);

    /**
     * 统计某部门在某合同下的跨部门费用收入笔数（作为目标方）
     */
    @Select("SELECT COUNT(*) FROM outside_cost_record " +
            "WHERE contract_id = #{contractId} AND target_dept_id = #{deptId} AND status = 2 AND deleted = 0")
    Integer countIncomeByContractIdAndDeptId(@Param("contractId") Long contractId, @Param("deptId") Long deptId);

    // ========== 部门维度汇总（不限制合同） ==========

    /**
     * 统计某部门的跨部门费用总支出（作为发起方），截止到指定日期
     */
    @Select("SELECT COALESCE(SUM(amount), 0) FROM outside_cost_record " +
            "WHERE request_dept_id = #{deptId} AND status = 2 AND deleted = 0 " +
            "AND fill_time <= #{cutoffDate}")
    BigDecimal sumExpenseByDeptId(@Param("deptId") Long deptId, @Param("cutoffDate") java.time.LocalDateTime cutoffDate);

    /**
     * 统计某部门的跨部门费用总收入（作为目标方），截止到指定日期
     */
    @Select("SELECT COALESCE(SUM(amount), 0) FROM outside_cost_record " +
            "WHERE target_dept_id = #{deptId} AND status = 2 AND deleted = 0 " +
            "AND fill_time <= #{cutoffDate}")
    BigDecimal sumIncomeByDeptId(@Param("deptId") Long deptId, @Param("cutoffDate") java.time.LocalDateTime cutoffDate);

    /**
     * 统计某部门的跨部门费用支出笔数（作为发起方），截止到指定日期
     */
    @Select("SELECT COUNT(*) FROM outside_cost_record " +
            "WHERE request_dept_id = #{deptId} AND status = 2 AND deleted = 0 " +
            "AND fill_time <= #{cutoffDate}")
    Integer countExpenseByDeptId(@Param("deptId") Long deptId, @Param("cutoffDate") java.time.LocalDateTime cutoffDate);

    /**
     * 统计某部门的跨部门费用收入笔数（作为目标方），截止到指定日期
     */
    @Select("SELECT COUNT(*) FROM outside_cost_record " +
            "WHERE target_dept_id = #{deptId} AND status = 2 AND deleted = 0 " +
            "AND fill_time <= #{cutoffDate}")
    Integer countIncomeByDeptId(@Param("deptId") Long deptId, @Param("cutoffDate") java.time.LocalDateTime cutoffDate);

    /**
     * 查询合同下有跨部门费用记录的所有部门ID（包括支出方和收入方）
     * 用于合同分配页面展示跨部门支出方部门
     */
    @Select("SELECT DISTINCT dept_id FROM (" +
            "  SELECT request_dept_id AS dept_id FROM outside_cost_record " +
            "  WHERE contract_id = #{contractId} AND deleted = 0 AND request_dept_id IS NOT NULL " +
            "  UNION " +
            "  SELECT target_dept_id AS dept_id FROM outside_cost_record " +
            "  WHERE contract_id = #{contractId} AND deleted = 0 AND target_dept_id IS NOT NULL " +
            ") t")
    List<Long> selectDeptIdsByContractIdFromOutsideCost(@Param("contractId") Long contractId);

    // ========== 按年度统计（用于经营分析） ==========

    /**
     * 统计某部门在指定年度的跨部门费用总支出（作为发起方）
     * 使用 fill_time 作为时间判断依据
     */
    @Select("SELECT COALESCE(SUM(amount), 0) FROM outside_cost_record " +
            "WHERE request_dept_id = #{deptId} AND status = 2 AND deleted = 0 " +
            "AND YEAR(fill_time) = #{year} AND fill_time <= #{cutoffDate}")
    BigDecimal sumExpenseByDeptIdAndYear(@Param("deptId") Long deptId, @Param("year") int year,
                                          @Param("cutoffDate") java.time.LocalDateTime cutoffDate);

    /**
     * 统计某部门在指定年度的跨部门费用总收入（作为目标方）
     * 使用 fill_time 作为时间判断依据
     */
    @Select("SELECT COALESCE(SUM(amount), 0) FROM outside_cost_record " +
            "WHERE target_dept_id = #{deptId} AND status = 2 AND deleted = 0 " +
            "AND YEAR(fill_time) = #{year} AND fill_time <= #{cutoffDate}")
    BigDecimal sumIncomeByDeptIdAndYear(@Param("deptId") Long deptId, @Param("year") int year,
                                         @Param("cutoffDate") java.time.LocalDateTime cutoffDate);

    /**
     * 批量统计多个部门在指定年度的跨部门费用支出
     */
    @Select("<script>" +
            "SELECT request_dept_id as deptId, COALESCE(SUM(amount), 0) as amount " +
            "FROM outside_cost_record " +
            "WHERE request_dept_id IN " +
            "<foreach item='deptId' collection='deptIds' open='(' separator=',' close=')'>" +
            "#{deptId}" +
            "</foreach> " +
            "AND status = 2 AND deleted = 0 " +
            "AND YEAR(fill_time) = #{year} AND fill_time &lt;= #{cutoffDate} " +
            "GROUP BY request_dept_id" +
            "</script>")
    List<java.util.Map<String, Object>> batchSumExpenseByDeptIds(@Param("deptIds") List<Long> deptIds,
                                                                   @Param("year") int year,
                                                                   @Param("cutoffDate") java.time.LocalDateTime cutoffDate);

    /**
     * 批量统计多个部门在指定年度的跨部门费用收入
     */
    @Select("<script>" +
            "SELECT target_dept_id as deptId, COALESCE(SUM(amount), 0) as amount " +
            "FROM outside_cost_record " +
            "WHERE target_dept_id IN " +
            "<foreach item='deptId' collection='deptIds' open='(' separator=',' close=')'>" +
            "#{deptId}" +
            "</foreach> " +
            "AND status = 2 AND deleted = 0 " +
            "AND YEAR(fill_time) = #{year} AND fill_time &lt;= #{cutoffDate} " +
            "GROUP BY target_dept_id" +
            "</script>")
    List<java.util.Map<String, Object>> batchSumIncomeByDeptIds(@Param("deptIds") List<Long> deptIds,
                                                                  @Param("year") int year,
                                                                  @Param("cutoffDate") java.time.LocalDateTime cutoffDate);

}
