package cn.shuhe.system.module.system.dal.mysql.dingtalkconfig;

import java.util.*;

import cn.shuhe.system.framework.common.pojo.PageResult;
import cn.shuhe.system.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.shuhe.system.framework.mybatis.core.mapper.BaseMapperX;
import cn.shuhe.system.module.system.dal.dataobject.dingtalkconfig.DingtalkConfigDO;
import org.apache.ibatis.annotations.Mapper;
import cn.shuhe.system.module.system.controller.admin.dingtalkconfig.vo.*;

/**
 * 钉钉配置 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface DingtalkConfigMapper extends BaseMapperX<DingtalkConfigDO> {

    default PageResult<DingtalkConfigDO> selectPage(DingtalkConfigPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<DingtalkConfigDO>()
                .likeIfPresent(DingtalkConfigDO::getName, reqVO.getName())
                .eqIfPresent(DingtalkConfigDO::getCorpId, reqVO.getCorpId())
                .eqIfPresent(DingtalkConfigDO::getAppId, reqVO.getAppId())
                .eqIfPresent(DingtalkConfigDO::getAgentId, reqVO.getAgentId())
                .eqIfPresent(DingtalkConfigDO::getClientId, reqVO.getClientId())
                .eqIfPresent(DingtalkConfigDO::getClientSecret, reqVO.getClientSecret())
                .eqIfPresent(DingtalkConfigDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(DingtalkConfigDO::getLastSyncTime, reqVO.getLastSyncTime())
                .eqIfPresent(DingtalkConfigDO::getLastSyncResult, reqVO.getLastSyncResult())
                .eqIfPresent(DingtalkConfigDO::getRemark, reqVO.getRemark())
                .betweenIfPresent(DingtalkConfigDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(DingtalkConfigDO::getId));
    }

    /**
     * 根据状态查询钉钉配置列表
     *
     * @param status 状态（0-启用）
     * @return 配置列表
     */
    default List<DingtalkConfigDO> selectListByStatus(Integer status) {
        return selectList(DingtalkConfigDO::getStatus, status);
    }

}