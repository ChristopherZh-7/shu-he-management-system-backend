package cn.shuhe.system.module.system.dal.mysql.dingtalkrobot;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.shuhe.system.framework.mybatis.core.mapper.BaseMapperX;
import cn.shuhe.system.module.system.dal.dataobject.dingtalkrobot.DingtalkNotificationLogDO;
import cn.shuhe.system.module.system.controller.admin.dingtalkrobot.vo.*;

/**
 * 钉钉通知发送日志 Mapper
 *
 * @author shuhe
 */
@Mapper
public interface DingtalkNotificationLogMapper extends BaseMapperX<DingtalkNotificationLogDO> {

    default PageResult<DingtalkNotificationLogDO> selectPage(DingtalkNotificationLogPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<DingtalkNotificationLogDO>()
                .eqIfPresent(DingtalkNotificationLogDO::getConfigId, reqVO.getConfigId())
                .eqIfPresent(DingtalkNotificationLogDO::getRobotId, reqVO.getRobotId())
                .eqIfPresent(DingtalkNotificationLogDO::getEventType, reqVO.getEventType())
                .eqIfPresent(DingtalkNotificationLogDO::getEventModule, reqVO.getEventModule())
                .eqIfPresent(DingtalkNotificationLogDO::getBusinessId, reqVO.getBusinessId())
                .eqIfPresent(DingtalkNotificationLogDO::getSendStatus, reqVO.getSendStatus())
                .betweenIfPresent(DingtalkNotificationLogDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(DingtalkNotificationLogDO::getId));
    }

    /**
     * 根据配置ID查询日志列表
     */
    default List<DingtalkNotificationLogDO> selectListByConfigId(Long configId) {
        return selectList(DingtalkNotificationLogDO::getConfigId, configId);
    }

    /**
     * 根据业务ID查询日志列表
     */
    default List<DingtalkNotificationLogDO> selectListByBusinessId(Long businessId) {
        return selectList(DingtalkNotificationLogDO::getBusinessId, businessId);
    }

}
