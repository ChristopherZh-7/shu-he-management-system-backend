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

    default List<CrmContractDO> selectListByCustomerIdOwnerUserId(Long customerId, Long ownerUserId) {
        return selectList(new LambdaQueryWrapperX<CrmContractDO>()
                .eq(CrmContractDO::getCustomerId, customerId)
                .eq(CrmContractDO::getOwnerUserId, ownerUserId));
    }

    /**
     * 查询待领取合同分页（基于部门）
     */
    default PageResult<CrmContractDO> selectPageByClaimStatusAndDeptId(CrmContractPageReqVO pageReqVO, Long deptId) {
        return selectPage(pageReqVO, new LambdaQueryWrapperX<CrmContractDO>()
                .eq(CrmContractDO::getClaimStatus, 0) // 待领取
                .likeIfPresent(CrmContractDO::getNo, pageReqVO.getNo())
                .likeIfPresent(CrmContractDO::getName, pageReqVO.getName())
                .apply("JSON_CONTAINS(assign_dept_ids, CAST({0} AS JSON))", deptId) // 分派部门包含当前用户部门
                .orderByDesc(CrmContractDO::getId));
    }

    /**
     * 查询待领取合同分页（基于负责人的部门ID列表）
     * 查询条件：claimStatus=0（全局待领取）且分派部门包含用户负责的部门
     * 
     * 注意：此方法只做初步筛选，具体的"该部门是否已领取"检查在 Service 层进行二次过滤
     * 
     * JSON 结构: [{"deptId":101,"claimed":false,...},
     * {"deptId":102,"claimed":true,...}]
     */
    default PageResult<CrmContractDO> selectPageByClaimStatusAndLeaderDeptIds(CrmContractPageReqVO pageReqVO,
            List<Long> leaderDeptIds) {
        LambdaQueryWrapperX<CrmContractDO> query = new LambdaQueryWrapperX<CrmContractDO>()
                .eq(CrmContractDO::getClaimStatus, 0) // 待领取（全局状态，表示还有部门未领取）
                .likeIfPresent(CrmContractDO::getNo, pageReqVO.getNo())
                .likeIfPresent(CrmContractDO::getName, pageReqVO.getName())
                .orderByDesc(CrmContractDO::getId);

        // 构建 OR 条件：assign_dept_ids 包含任意一个负责人的部门
        // 具体的 claimed 状态检查在 Service 层进行
        query.and(wrapper -> {
            for (int i = 0; i < leaderDeptIds.size(); i++) {
                Long deptId = leaderDeptIds.get(i);
                // 匹配: "deptId":101, 格式
                if (i == 0) {
                    wrapper.like(CrmContractDO::getAssignDeptIds, "\"deptId\":" + deptId + ",");
                } else {
                    wrapper.or().like(CrmContractDO::getAssignDeptIds, "\"deptId\":" + deptId + ",");
                }
            }
        });

        return selectPage(pageReqVO, query);
    }

}
