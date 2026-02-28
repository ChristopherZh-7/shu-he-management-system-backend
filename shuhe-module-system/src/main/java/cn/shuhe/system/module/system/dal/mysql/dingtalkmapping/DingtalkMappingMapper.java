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
 * @author ShuHe
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

    /**
     * 根据本地ID和类型查询钉钉映射
     *
     * @param localId 本地ID
     * @param type 类型（USER/DEPT）
     * @return 钉钉映射
     */
    default DingtalkMappingDO selectByLocalId(Long localId, String type) {
        return selectOne(new LambdaQueryWrapperX<DingtalkMappingDO>()
                .eq(DingtalkMappingDO::getLocalId, localId)
                .eq(DingtalkMappingDO::getType, type));
    }

}