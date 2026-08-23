package cn.shuhe.system.module.project.dal.mysql;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

/** 跨模块读取审批已通过的提前投入权威计划日期，避免 project 模块依赖 CRM 实现。 */
@Mapper
public interface BusinessTimeMapper {

    @Select("SELECT early_investment_plan_start AS startDate, "
            + "early_investment_plan_end AS endDate "
            + "FROM crm_business WHERE id = #{businessId} "
            + "AND early_investment_status = 20 AND deleted = 0")
    Map<String, Object> selectEarlyInvestmentTime(@Param("businessId") Long businessId);
}
