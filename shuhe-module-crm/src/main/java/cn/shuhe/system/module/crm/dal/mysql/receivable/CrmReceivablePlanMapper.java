package cn.shuhe.system.module.crm.dal.mysql.receivable;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.mybatis.core.mapper.BaseMapperX;
import cn.shuhe.system.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.shuhe.system.module.crm.controller.admin.receivable.vo.plan.CrmReceivablePlanPageReqVO;
import cn.shuhe.system.module.crm.dal.dataobject.contract.CrmContractDO;
import cn.shuhe.system.module.crm.dal.dataobject.receivable.CrmReceivablePlanDO;
import cn.shuhe.system.module.crm.enums.common.CrmBizTypeEnum;
import cn.shuhe.system.module.crm.enums.common.CrmSceneTypeEnum;
import cn.shuhe.system.module.crm.util.CrmPermissionUtils;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 回款计划 Mapper
 *
 * @author ShuHe
 */
@Mapper
public interface CrmReceivablePlanMapper extends BaseMapperX<CrmReceivablePlanDO> {

    default CrmReceivablePlanDO selectMaxPeriodByContractId(Long contractId) {
        return selectOne(new MPJLambdaWrapperX<CrmReceivablePlanDO>()
                .eq(CrmReceivablePlanDO::getContractId, contractId)
                .orderByDesc(CrmReceivablePlanDO::getPeriod)
                .last("LIMIT 1"));
    }

    default PageResult<CrmReceivablePlanDO> selectPageByCustomerId(CrmReceivablePlanPageReqVO reqVO) {
        MPJLambdaWrapperX<CrmReceivablePlanDO> query = new MPJLambdaWrapperX<>();
        if (Objects.nonNull(reqVO.getContractNo())) { // 根据合同编号检索
            query.innerJoin(CrmContractDO.class, on -> on.like(CrmContractDO::getNo, reqVO.getContractNo())
                    .eq(CrmContractDO::getId, CrmReceivablePlanDO::getContractId));
        }
        query.eq(CrmReceivablePlanDO::getCustomerId, reqVO.getCustomerId()) // 必须传递
                .eqIfPresent(CrmReceivablePlanDO::getContractId, reqVO.getContractId())
                .orderByDesc(CrmReceivablePlanDO::getPeriod);
        return selectJoinPage(reqVO, CrmReceivablePlanDO.class, query);
    }

    default PageResult<CrmReceivablePlanDO> selectPage(CrmReceivablePlanPageReqVO pageReqVO, Long userId) {
        MPJLambdaWrapperX<CrmReceivablePlanDO> query = new MPJLambdaWrapperX<>();
        // 拼接数据权限的查询条件
        CrmPermissionUtils.appendPermissionCondition(query, CrmBizTypeEnum.CRM_RECEIVABLE_PLAN.getType(),
                CrmReceivablePlanDO::getId, userId, pageReqVO.getSceneType());
        // 拼接自身的查询条件
        query.selectAll(CrmReceivablePlanDO.class)
                .eqIfPresent(CrmReceivablePlanDO::getCustomerId, pageReqVO.getCustomerId())
                .eqIfPresent(CrmReceivablePlanDO::getContractId, pageReqVO.getContractId())
                .orderByDesc(CrmReceivablePlanDO::getPeriod);
        if (Objects.nonNull(pageReqVO.getContractNo())) { // 根据合同编号检索
            query.innerJoin(CrmContractDO.class, on -> on.like(CrmContractDO::getNo, pageReqVO.getContractNo())
                    .eq(CrmContractDO::getId, CrmReceivablePlanDO::getContractId));
        }

        // Backlog: 回款提醒类型
        LocalDateTime beginOfToday = LocalDateTimeUtil.beginOfDay(LocalDateTime.now());
        if (CrmReceivablePlanPageReqVO.REMIND_TYPE_NEEDED.equals(pageReqVO.getRemindType())) { // 待回款
            // 查询条件：未回款 + 提醒时间 <= 当前时间（反过来即当前时间 >= 提醒时间，已经到达提醒的时间点）
            query.isNull(CrmReceivablePlanDO::getReceivableId) // 未回款
                    .le(CrmReceivablePlanDO::getRemindTime, beginOfToday); // 今天开始提醒
        } else if (CrmReceivablePlanPageReqVO.REMIND_TYPE_EXPIRED.equals(pageReqVO.getRemindType())) { // 已逾期
            // 查询条件：未回款 + 回款时间 < 当前时间（反过来即当前时间 > 回款时间，已经过了回款时间点）
            query.isNull(CrmReceivablePlanDO::getReceivableId) // 未回款
                    .lt(CrmReceivablePlanDO::getReturnTime, beginOfToday); // 已逾期
        } else if (CrmReceivablePlanPageReqVO.REMIND_TYPE_RECEIVED.equals(pageReqVO.getRemindType())) { // 已回款
            query.isNotNull(CrmReceivablePlanDO::getReceivableId);
        }
        return selectJoinPage(pageReqVO, CrmReceivablePlanDO.class, query);
    }

    default Long selectReceivablePlanCountByRemind(Long userId) {
        MPJLambdaWrapperX<CrmReceivablePlanDO> query = new MPJLambdaWrapperX<>();
        // 我负责的 + 非公海
        CrmPermissionUtils.appendPermissionCondition(query, CrmBizTypeEnum.CRM_RECEIVABLE_PLAN.getType(),
                CrmReceivablePlanDO::getId, userId, CrmSceneTypeEnum.OWNER.getType());
        // 未回款 + 已逾期 + 今天开始提醒
        LocalDateTime beginOfToday = LocalDateTimeUtil.beginOfDay(LocalDateTime.now());
        query.isNull(CrmReceivablePlanDO::getReceivableId) // 未回款
                .lt(CrmReceivablePlanDO::getReturnTime, beginOfToday) // 已逾期
                .lt(CrmReceivablePlanDO::getRemindTime, beginOfToday); // 今天开始提醒
        return selectCount(query);
    }

    /**
     * 仪表板：待回款数量（未回款的计划数）
     * sceneType 为 null 表示全部，OWNER 表示仅本人负责
     */
    default Long selectCountPendingForDashboard(Long userId, Integer sceneType) {
        MPJLambdaWrapperX<CrmReceivablePlanDO> query = new MPJLambdaWrapperX<>();
        if (sceneType != null) {
            CrmPermissionUtils.appendPermissionCondition(query, CrmBizTypeEnum.CRM_RECEIVABLE_PLAN.getType(),
                    CrmReceivablePlanDO::getId, userId, sceneType);
        }
        query.isNull(CrmReceivablePlanDO::getReceivableId); // 未回款
        return selectCount(query);
    }

    /**
     * 仪表板：已逾期回款数量
     * sceneType 为 null 表示全部，OWNER 表示仅本人负责
     */
    default Long selectCountOverdueForDashboard(Long userId, Integer sceneType) {
        MPJLambdaWrapperX<CrmReceivablePlanDO> query = new MPJLambdaWrapperX<>();
        if (sceneType != null) {
            CrmPermissionUtils.appendPermissionCondition(query, CrmBizTypeEnum.CRM_RECEIVABLE_PLAN.getType(),
                    CrmReceivablePlanDO::getId, userId, sceneType);
        }
        LocalDateTime beginOfToday = LocalDateTimeUtil.beginOfDay(LocalDateTime.now());
        query.isNull(CrmReceivablePlanDO::getReceivableId) // 未回款
                .lt(CrmReceivablePlanDO::getReturnTime, beginOfToday); // 已逾期
        return selectCount(query);
    }

    /**
     * 仪表板：待回款总金额（未回款计划的金额合计，单位：元）
     * sceneType 为 null 表示全部，OWNER 表示仅本人负责
     */
    default java.math.BigDecimal selectSumPendingAmountForDashboard(Long userId, Integer sceneType) {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<CrmReceivablePlanDO> q = 
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        q.select("COALESCE(SUM(price),0) as total")
                .isNull("receivable_id"); // 未回款
        if (sceneType != null && CrmSceneTypeEnum.OWNER.getType().equals(sceneType)) {
            q.eq("owner_user_id", userId);
        }
        java.util.List<java.util.Map<String, Object>> list = selectMaps(q);
        if (list == null || list.isEmpty()) {
            return java.math.BigDecimal.ZERO;
        }
        Object total = list.get(0).get("total");
        if (total == null) {
            return java.math.BigDecimal.ZERO;
        }
        if (total instanceof java.math.BigDecimal) {
            return (java.math.BigDecimal) total;
        }
        return new java.math.BigDecimal(total.toString());
    }

}
