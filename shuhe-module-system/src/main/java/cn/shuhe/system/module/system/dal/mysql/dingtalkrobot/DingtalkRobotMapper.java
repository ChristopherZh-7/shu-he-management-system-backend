package cn.shuhe.system.module.system.dal.mysql.dingtalkrobot;

import java.util.List;

import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.shuhe.system.framework.mybatis.core.mapper.BaseMapperX;
import cn.shuhe.system.module.system.dal.dataobject.dingtalkrobot.DingtalkRobotDO;
import org.apache.ibatis.annotations.Mapper;
import cn.shuhe.system.module.system.controller.admin.dingtalkrobot.vo.*;

/**
 * 钉钉群机器人配置 Mapper
 *
 * @author shuhe
 */
@Mapper
public interface DingtalkRobotMapper extends BaseMapperX<DingtalkRobotDO> {

    default PageResult<DingtalkRobotDO> selectPage(DingtalkRobotPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<DingtalkRobotDO>()
                .likeIfPresent(DingtalkRobotDO::getName, reqVO.getName())
                .eqIfPresent(DingtalkRobotDO::getStatus, reqVO.getStatus())
                .eqIfPresent(DingtalkRobotDO::getSecurityType, reqVO.getSecurityType())
                .betweenIfPresent(DingtalkRobotDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(DingtalkRobotDO::getId));
    }

    /**
     * 根据状态查询机器人配置列表
     *
     * @param status 状态（0-启用）
     * @return 配置列表
     */
    default List<DingtalkRobotDO> selectListByStatus(Integer status) {
        return selectList(DingtalkRobotDO::getStatus, status);
    }

}
