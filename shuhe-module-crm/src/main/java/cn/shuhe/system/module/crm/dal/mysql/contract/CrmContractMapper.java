package cn.shuhe.system.module.crm.dal.mysql.contract;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.mybatis.core.mapper.BaseMapperX;
import cn.shuhe.system.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.shuhe.system.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.shuhe.system.module.crm.controller.admin.contract.vo.contract.CrmContractPageReqVO;
import cn.shuhe.system.module.crm.dal.dataobject.contract.CrmContractConfigDO;
import cn.shuhe.system.module.crm.dal.dataobject.contract.CrmContractDO;
import cn.shuhe.system.module.crm.enums.common.CrmAuditStatusEnum;
import cn.shuhe.system.module.crm.enums.common.CrmBizTypeEnum;
import cn.shuhe.system.module.crm.enums.common.CrmSceneTypeEnum;
import cn.shuhe.system.module.crm.util.CrmPermissionUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.lang.Nullable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * CRM 合同 Mapper
 *
 * @author dhb52
 */
@Mapper
public interface CrmContractMapper extends BaseMapperX<CrmContractDO> {

    default CrmContractDO selectByNo(String no) {
        return selectOne(CrmContractDO::getNo, no);
    }

    default PageResult<CrmContractDO> selectPageByCustomerId(CrmContractPageReqVO pageReqVO) {
        return selectPage(pageReqVO, new LambdaQueryWrapperX<CrmContractDO>()
                .eq(CrmContractDO::getCustomerId, pageReqVO.getCustomerId())
                .likeIfPresent(CrmContractDO::getNo, pageReqVO.getNo())
                .likeIfPresent(CrmContractDO::getName, pageReqVO.getName())
                .eqIfPresent(CrmContractDO::getCustomerId, pageReqVO.getCustomerId())
                .eqIfPresent(CrmContractDO::getBusinessId, pageReqVO.getBusinessId())
                .orderByDesc(CrmContractDO::getId));
    }

    default PageResult<CrmContractDO> selectPageByBusinessId(CrmContractPageReqVO pageReqVO) {
        return selectPage(pageReqVO, new LambdaQueryWrapperX<CrmContractDO>()
                .eq(CrmContractDO::getBusinessId, pageReqVO.getBusinessId())
                .likeIfPresent(CrmContractDO::getNo, pageReqVO.getNo())
                .likeIfPresent(CrmContractDO::getName, pageReqVO.getName())
                .eqIfPresent(CrmContractDO::getCustomerId, pageReqVO.getCustomerId())
                .eqIfPresent(CrmContractDO::getBusinessId, pageReqVO.getBusinessId())
                .orderByDesc(CrmContractDO::getId));
    }

    default PageResult<CrmContractDO> selectPage(CrmContractPageReqVO pageReqVO, Long userId,
            CrmContractConfigDO config) {
        MPJLambdaWrapperX<CrmContractDO> query = new MPJLambdaWrapperX<>();
        // 拼接数据权限的查询条件
        CrmPermissionUtils.appendPermissionCondition(query, CrmBizTypeEnum.CRM_CONTRACT.getType(),
                CrmContractDO::getId, userId, pageReqVO.getSceneType());
        // 拼接自身的查询条件
        query.selectAll(CrmContractDO.class)
                .likeIfPresent(CrmContractDO::getNo, pageReqVO.getNo())
                .likeIfPresent(CrmContractDO::getName, pageReqVO.getName())
                .eqIfPresent(CrmContractDO::getCustomerId, pageReqVO.getCustomerId())
                .eqIfPresent(CrmContractDO::getBusinessId, pageReqVO.getBusinessId())
                .eqIfPresent(CrmContractDO::getAuditStatus, pageReqVO.getAuditStatus())
                .orderByDesc(CrmContractDO::getId);

        // Backlog: 即将到期的合同
        LocalDateTime beginOfToday = LocalDateTimeUtil.beginOfDay(LocalDateTime.now());
        LocalDateTime endOfToday = LocalDateTimeUtil.endOfDay(LocalDateTime.now());
        if (CrmContractPageReqVO.EXPIRY_TYPE_ABOUT_TO_EXPIRE.equals(pageReqVO.getExpiryType())) { // 即将到期
            query.eq(CrmContractDO::getAuditStatus, CrmAuditStatusEnum.APPROVE.getStatus())
                    .between(CrmContractDO::getEndTime, beginOfToday, endOfToday.plusDays(config.getNotifyDays()));
        } else if (CrmContractPageReqVO.EXPIRY_TYPE_EXPIRED.equals(pageReqVO.getExpiryType())) { // 已到期
            query.eq(CrmContractDO::getAuditStatus, CrmAuditStatusEnum.APPROVE.getStatus())
                    .lt(CrmContractDO::getEndTime, endOfToday);
        }
        return selectJoinPage(pageReqVO, CrmContractDO.class, query);
    }

    default Long selectCountByContactId(Long contactId) {
        return selectCount(CrmContractDO::getSignContactId, contactId);
    }

    default Long selectCountByBusinessId(Long businessId) {
        return selectCount(CrmContractDO::getBusinessId, businessId);
    }

    default Long selectCountByAudit(Long userId) {
        return selectCountByAudit(userId, CrmSceneTypeEnum.OWNER.getType());
    }

    /**
     * 待审核合同数量（支持管理员查全部）
     */
    default Long selectCountByAudit(Long userId, @Nullable Integer sceneType) {
        MPJLambdaWrapperX<CrmContractDO> query = new MPJLambdaWrapperX<>();
        if (sceneType != null) {
            CrmPermissionUtils.appendPermissionCondition(query, CrmBizTypeEnum.CRM_CONTRACT.getType(),
                    CrmContractDO::getId, userId, sceneType);
        }
        query.eq(CrmContractDO::getAuditStatus, CrmAuditStatusEnum.PROCESS.getStatus());
        return selectCount(query);
    }

    default Long selectCountByRemind(Long userId, CrmContractConfigDO config) {
        return selectCountByRemind(userId, CrmSceneTypeEnum.OWNER.getType(), config);
    }

    /**
     * 即将到期合同数量（支持管理员查全部）
     */
    default Long selectCountByRemind(Long userId, @Nullable Integer sceneType, CrmContractConfigDO config) {
        MPJLambdaWrapperX<CrmContractDO> query = new MPJLambdaWrapperX<>();
        if (sceneType != null) {
            CrmPermissionUtils.appendPermissionCondition(query, CrmBizTypeEnum.CRM_CONTRACT.getType(),
                    CrmContractDO::getId, userId, sceneType);
        }
        LocalDateTime beginOfToday = LocalDateTimeUtil.beginOfDay(LocalDateTime.now());
        LocalDateTime endOfToday = LocalDateTimeUtil.endOfDay(LocalDateTime.now());
        query.eq(CrmContractDO::getAuditStatus, CrmAuditStatusEnum.APPROVE.getStatus())
                .between(CrmContractDO::getEndTime, beginOfToday, endOfToday.plusDays(config.getNotifyDays()));
        return selectCount(query);
    }

    /**
     * 仪表板：合同总数量（sceneType 为 null 表示全部）
     */
    default Long selectCountForDashboard(Long userId, @Nullable Integer sceneType) {
        MPJLambdaWrapperX<CrmContractDO> query = new MPJLambdaWrapperX<>();
        if (sceneType != null) {
            CrmPermissionUtils.appendPermissionCondition(query, CrmBizTypeEnum.CRM_CONTRACT.getType(),
                    CrmContractDO::getId, userId, sceneType);
        }
        return selectCount(query);
    }

    /**
     * 仪表板：进行中合同数量（审批通过且未过期）
     */
    default Long selectCountActiveForDashboard(Long userId, @Nullable Integer sceneType) {
        MPJLambdaWrapperX<CrmContractDO> query = new MPJLambdaWrapperX<>();
        if (sceneType != null) {
            CrmPermissionUtils.appendPermissionCondition(query, CrmBizTypeEnum.CRM_CONTRACT.getType(),
                    CrmContractDO::getId, userId, sceneType);
        }
        LocalDateTime endOfToday = LocalDateTimeUtil.endOfDay(LocalDateTime.now());
        query.eq(CrmContractDO::getAuditStatus, CrmAuditStatusEnum.APPROVE.getStatus())
                .and(w -> w.isNull(CrmContractDO::getEndTime).or().ge(CrmContractDO::getEndTime, endOfToday));
        return selectCount(query);
    }

    /**
     * 仪表板：合同总金额合计（单位：分），sceneType 为 null 表示全部，OWNER 表示仅本人负责
     */
    default BigDecimal selectSumTotalPriceForDashboard(Long userId, @Nullable Integer sceneType) {
        QueryWrapper<CrmContractDO> q = new QueryWrapper<>();
        q.select("COALESCE(SUM(total_price),0) as total");
        if (sceneType != null && CrmSceneTypeEnum.OWNER.getType().equals(sceneType)) {
            q.eq("owner_user_id", userId);
        }
        List<Map<String, Object>> list = selectMaps(q);
        if (list == null || list.isEmpty()) {
            return BigDecimal.ZERO;
        }
        Object total = list.get(0).get("total");
        if (total == null) {
            return BigDecimal.ZERO;
        }
        if (total instanceof BigDecimal) {
            return (BigDecimal) total;
        }
        return new BigDecimal(total.toString());
    }

    /**
     * 仪表板：指定时间范围内合同金额合计（单位：分）
     * 基于合同创建时间（create_time）进行过滤
     */
    default BigDecimal selectSumTotalPriceForDashboard(Long userId, @Nullable Integer sceneType,
                                                        LocalDateTime start, LocalDateTime end) {
        QueryWrapper<CrmContractDO> q = new QueryWrapper<>();
        q.select("COALESCE(SUM(total_price),0) as total");
        q.between("create_time", start, end);
        if (sceneType != null && CrmSceneTypeEnum.OWNER.getType().equals(sceneType)) {
            q.eq("owner_user_id", userId);
        }
        List<Map<String, Object>> list = selectMaps(q);
        if (list == null || list.isEmpty()) {
            return BigDecimal.ZERO;
        }
        Object total = list.get(0).get("total");
        if (total == null) {
            return BigDecimal.ZERO;
        }
        if (total instanceof BigDecimal) {
            return (BigDecimal) total;
        }
        return new BigDecimal(total.toString());
    }

    default List<CrmContractDO> selectListByCustomerIdOwnerUserId(Long customerId, Long ownerUserId) {
        return selectList(new LambdaQueryWrapperX<CrmContractDO>()
                .eq(CrmContractDO::getCustomerId, customerId)
                .eq(CrmContractDO::getOwnerUserId, ownerUserId));
    }

    /**
     * 根据负责人查询合同列表（用于离职交接）
     */
    default List<CrmContractDO> selectListByOwnerUserId(Long ownerUserId) {
        return selectList(CrmContractDO::getOwnerUserId, ownerUserId);
    }

}
