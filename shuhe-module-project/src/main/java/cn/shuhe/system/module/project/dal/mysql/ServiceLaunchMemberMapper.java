package cn.shuhe.system.module.project.dal.mysql;

import cn.shuhe.system.framework.mybatis.core.mapper.BaseMapperX;
import cn.shuhe.system.module.project.dal.dataobject.ServiceLaunchMemberDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 服务发起执行人 Mapper
 */
@Mapper
public interface ServiceLaunchMemberMapper extends BaseMapperX<ServiceLaunchMemberDO> {

    /**
     * 根据服务发起ID查询执行人列表
     */
    default List<ServiceLaunchMemberDO> selectListByLaunchId(Long launchId) {
        return selectList(ServiceLaunchMemberDO::getLaunchId, launchId);
    }

    /**
     * 根据用户ID查询执行人列表
     */
    default List<ServiceLaunchMemberDO> selectListByUserId(Long userId) {
        return selectList(ServiceLaunchMemberDO::getUserId, userId);
    }

    /**
     * 根据服务发起ID和用户ID查询
     */
    default ServiceLaunchMemberDO selectByLaunchIdAndUserId(Long launchId, Long userId) {
        return selectOne(ServiceLaunchMemberDO::getLaunchId, launchId,
                ServiceLaunchMemberDO::getUserId, userId);
    }

    /**
     * 根据服务发起ID删除执行人
     */
    default int deleteByLaunchId(Long launchId) {
        return delete(ServiceLaunchMemberDO::getLaunchId, launchId);
    }

    /**
     * 统计服务发起的执行人数量
     */
    default Long selectCountByLaunchId(Long launchId) {
        return selectCount(ServiceLaunchMemberDO::getLaunchId, launchId);
    }

    /**
     * 统计服务发起的已完成执行人数量（finishStatus > 0 表示已完成）
     */
    default Long selectFinishedCountByLaunchId(Long launchId) {
        return selectCount(new LambdaQueryWrapper<ServiceLaunchMemberDO>()
                .eq(ServiceLaunchMemberDO::getLaunchId, launchId)
                .gt(ServiceLaunchMemberDO::getFinishStatus, 0));
    }

}
