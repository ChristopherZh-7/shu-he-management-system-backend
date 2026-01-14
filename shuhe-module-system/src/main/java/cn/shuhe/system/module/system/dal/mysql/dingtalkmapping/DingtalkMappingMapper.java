package cn.shuhe.system.module.system.dal.mysql.dingtalkmapping;

import java.util.*;

import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.shuhe.system.framework.mybatis.core.mapper.BaseMapperX;
import cn.shuhe.system.module.system.dal.dataobject.dingtalkmapping.DingtalkMappingDO;
import org.apache.ibatis.annotations.Mapper;
import cn.shuhe.system.module.system.controller.admin.dingtalkmapping.vo.*;

/**
 * 钉钉数据映射 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface DingtalkMappingMapper extends BaseMapperX<DingtalkMappingDO> {

    default PageResult<DingtalkMappingDO> selectPage(DingtalkMappingPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<DingtalkMappingDO>()
                .eqIfPresent(DingtalkMappingDO::getConfigId, reqVO.getConfigId())
                .eqIfPresent(DingtalkMappingDO::getType, reqVO.getType())
                .eqIfPresent(DingtalkMappingDO::getLocalId, reqVO.getLocalId())
                .eqIfPresent(DingtalkMappingDO::getDingtalkId, reqVO.getDingtalkId())
                .betweenIfPresent(DingtalkMappingDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(DingtalkMappingDO::getId));
    }

}