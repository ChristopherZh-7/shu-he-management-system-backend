package cn.shuhe.system.module.system.dal.mysql.dingtalkrobot;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.shuhe.system.framework.mybatis.core.mapper.BaseMapperX;
import cn.shuhe.system.module.system.dal.dataobject.dingtalkrobot.DingtalkNotificationConfigDO;
import cn.shuhe.system.module.system.controller.admin.dingtalkrobot.vo.*;

/**
 * 钉钉通知场景配置 Mapper
 *
 * @author shuhe
 */
@Mapper
public interface DingtalkNotificationConfigMapper extends BaseMapperX<DingtalkNotificationConfigDO> {

    default PageResult<DingtalkNotificationConfigDO> selectPage(DingtalkNotificationConfigPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<DingtalkNotificationConfigDO>()
                .likeIfPresent(DingtalkNotificationConfigDO::getName, reqVO.getName())
                .eqIfPresent(DingtalkNotificationConfigDO::getRobotId, reqVO.getRobotId())
                .eqIfPresent(DingtalkNotificationConfigDO::getEventType, reqVO.getEventType())
                .eqIfPresent(DingtalkNotificationConfigDO::getEventModule, reqVO.getEventModule())
                .eqIfPresent(DingtalkNotificationConfigDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(DingtalkNotificationConfigDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(DingtalkNotificationConfigDO::getId));
    }

    /**
     * 根据事件类型和模块查询启用的配置列表
     */
    default List<DingtalkNotificationConfigDO> selectListByEventTypeAndModule(String eventType, String eventModule) {
        return selectList(new LambdaQueryWrapperX<DingtalkNotificationConfigDO>()
                .eq(DingtalkNotificationConfigDO::getEventType, eventType)
                .eq(DingtalkNotificationConfigDO::getEventModule, eventModule)
                .eq(DingtalkNotificationConfigDO::getStatus, 0) // 只查启用的
                .orderByAsc(DingtalkNotificationConfigDO::getId));
    }

    /**
     * 根据事件类型查询启用的配置列表
     */
    default List<DingtalkNotificationConfigDO> selectListByEventType(String eventType) {
        return selectList(new LambdaQueryWrapperX<DingtalkNotificationConfigDO>()
                .eq(DingtalkNotificationConfigDO::getEventType, eventType)
                .eq(DingtalkNotificationConfigDO::getStatus, 0)
                .orderByAsc(DingtalkNotificationConfigDO::getId));
    }

    /**
     * 根据机器人ID查询配置列表
     */
    default List<DingtalkNotificationConfigDO> selectListByRobotId(Long robotId) {
        return selectList(DingtalkNotificationConfigDO::getRobotId, robotId);
    }

}
