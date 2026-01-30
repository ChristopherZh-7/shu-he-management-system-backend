package cn.shuhe.system.module.project.dal.mysql;

import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.mybatis.core.mapper.BaseMapperX;
import cn.shuhe.system.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.shuhe.system.module.project.controller.admin.vo.ServiceLaunchPageReqVO;
import cn.shuhe.system.module.project.dal.dataobject.ServiceLaunchDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ServiceLaunchMapper extends BaseMapperX<ServiceLaunchDO> {

    default PageResult<ServiceLaunchDO> selectPage(ServiceLaunchPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ServiceLaunchDO>()
                .eqIfPresent(ServiceLaunchDO::getContractId, reqVO.getContractId())
                .eqIfPresent(ServiceLaunchDO::getProjectId, reqVO.getProjectId())
                .eqIfPresent(ServiceLaunchDO::getServiceItemId, reqVO.getServiceItemId())
                .eqIfPresent(ServiceLaunchDO::getStatus, reqVO.getStatus())
                .eqIfPresent(ServiceLaunchDO::getIsOutside, reqVO.getIsOutside())
                .eqIfPresent(ServiceLaunchDO::getIsCrossDept, reqVO.getIsCrossDept())
                .orderByDesc(ServiceLaunchDO::getId));
    }

    default PageResult<ServiceLaunchDO> selectPageByUserId(ServiceLaunchPageReqVO reqVO, Long userId) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ServiceLaunchDO>()
                .eq(ServiceLaunchDO::getRequestUserId, userId)
                .eqIfPresent(ServiceLaunchDO::getContractId, reqVO.getContractId())
                .eqIfPresent(ServiceLaunchDO::getProjectId, reqVO.getProjectId())
                .eqIfPresent(ServiceLaunchDO::getServiceItemId, reqVO.getServiceItemId())
                .eqIfPresent(ServiceLaunchDO::getStatus, reqVO.getStatus())
                .eqIfPresent(ServiceLaunchDO::getIsOutside, reqVO.getIsOutside())
                .eqIfPresent(ServiceLaunchDO::getIsCrossDept, reqVO.getIsCrossDept())
                .orderByDesc(ServiceLaunchDO::getId));
    }

}
