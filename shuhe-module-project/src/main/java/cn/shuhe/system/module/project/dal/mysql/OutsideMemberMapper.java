package cn.shuhe.system.module.project.dal.mysql;

import cn.shuhe.system.framework.mybatis.core.mapper.BaseMapperX;
import cn.shuhe.system.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.shuhe.system.module.project.dal.dataobject.OutsideMemberDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 外出人员 Mapper
 */
@Mapper
public interface OutsideMemberMapper extends BaseMapperX<OutsideMemberDO> {

    /**
     * 根据外出请求ID查询人员列表
     */
    default List<OutsideMemberDO> selectListByRequestId(Long requestId) {
        return selectList(new LambdaQueryWrapperX<OutsideMemberDO>()
                .eq(OutsideMemberDO::getRequestId, requestId)
                .orderByAsc(OutsideMemberDO::getId));
    }

    /**
     * 根据外出请求ID删除人员
     */
    default void deleteByRequestId(Long requestId) {
        delete(new LambdaQueryWrapperX<OutsideMemberDO>()
                .eq(OutsideMemberDO::getRequestId, requestId));
    }

    /**
     * 根据用户ID查询该用户参与的外出记录
     */
    default List<OutsideMemberDO> selectListByUserId(Long userId) {
        return selectList(new LambdaQueryWrapperX<OutsideMemberDO>()
                .eq(OutsideMemberDO::getUserId, userId)
                .orderByDesc(OutsideMemberDO::getId));
    }

    /**
     * 统计用户参与外出的次数
     */
    default Long selectCountByUserId(Long userId) {
        return selectCount(new LambdaQueryWrapperX<OutsideMemberDO>()
                .eq(OutsideMemberDO::getUserId, userId));
    }

}
