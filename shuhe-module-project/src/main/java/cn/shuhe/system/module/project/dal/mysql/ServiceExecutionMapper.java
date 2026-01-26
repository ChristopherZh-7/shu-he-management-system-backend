package cn.shuhe.system.module.project.dal.mysql;

import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.mybatis.core.mapper.BaseMapperX;
import cn.shuhe.system.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.shuhe.system.module.project.controller.admin.vo.ServiceExecutionPageReqVO;
import cn.shuhe.system.module.project.dal.dataobject.ServiceExecutionDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 服务执行申请 Mapper
 */
@Mapper
public interface ServiceExecutionMapper extends BaseMapperX<ServiceExecutionDO> {

    default PageResult<ServiceExecutionDO> selectPage(ServiceExecutionPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ServiceExecutionDO>()
                .eqIfPresent(ServiceExecutionDO::getProjectId, reqVO.getProjectId())
                .eqIfPresent(ServiceExecutionDO::getServiceItemId, reqVO.getServiceItemId())
                .eqIfPresent(ServiceExecutionDO::getRequestUserId, reqVO.getRequestUserId())
                .eqIfPresent(ServiceExecutionDO::getStatus, reqVO.getStatus())
                .orderByDesc(ServiceExecutionDO::getId));
    }

    default PageResult<ServiceExecutionDO> selectPageByUserId(ServiceExecutionPageReqVO reqVO, Long userId) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ServiceExecutionDO>()
                .eq(ServiceExecutionDO::getRequestUserId, userId)
                .eqIfPresent(ServiceExecutionDO::getProjectId, reqVO.getProjectId())
                .eqIfPresent(ServiceExecutionDO::getServiceItemId, reqVO.getServiceItemId())
                .eqIfPresent(ServiceExecutionDO::getStatus, reqVO.getStatus())
                .orderByDesc(ServiceExecutionDO::getId));
    }

    /**
     * 根据轮次ID查询服务执行申请
     */
    default ServiceExecutionDO selectByRoundId(Long roundId) {
        return selectOne(new LambdaQueryWrapperX<ServiceExecutionDO>()
                .eq(ServiceExecutionDO::getRoundId, roundId));
    }

}
