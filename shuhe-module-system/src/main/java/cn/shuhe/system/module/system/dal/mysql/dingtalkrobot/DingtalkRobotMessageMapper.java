package cn.shuhe.system.module.system.dal.mysql.dingtalkrobot;

import java.util.List;

import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.shuhe.system.framework.mybatis.core.mapper.BaseMapperX;
import cn.shuhe.system.module.system.dal.dataobject.dingtalkrobot.DingtalkRobotMessageDO;
import org.apache.ibatis.annotations.Mapper;
import cn.shuhe.system.module.system.controller.admin.dingtalkrobot.vo.*;

/**
 * 钉钉群机器人消息记录 Mapper
 *
 * @author shuhe
 */
@Mapper
public interface DingtalkRobotMessageMapper extends BaseMapperX<DingtalkRobotMessageDO> {

    default PageResult<DingtalkRobotMessageDO> selectPage(DingtalkRobotMessagePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<DingtalkRobotMessageDO>()
                .eqIfPresent(DingtalkRobotMessageDO::getRobotId, reqVO.getRobotId())
                .eqIfPresent(DingtalkRobotMessageDO::getMsgType, reqVO.getMsgType())
                .eqIfPresent(DingtalkRobotMessageDO::getSendStatus, reqVO.getSendStatus())
                .betweenIfPresent(DingtalkRobotMessageDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(DingtalkRobotMessageDO::getId));
    }

    /**
     * 根据机器人ID查询消息记录列表
     *
     * @param robotId 机器人编号
     * @return 消息记录列表
     */
    default List<DingtalkRobotMessageDO> selectListByRobotId(Long robotId) {
        return selectList(DingtalkRobotMessageDO::getRobotId, robotId);
    }

}
