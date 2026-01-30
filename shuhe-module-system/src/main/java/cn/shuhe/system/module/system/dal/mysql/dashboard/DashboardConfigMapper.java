package cn.shuhe.system.module.system.dal.mysql.dashboard;

import cn.shuhe.system.framework.mybatis.core.mapper.BaseMapperX;
import cn.shuhe.system.module.system.dal.dataobject.dashboard.DashboardConfigDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 仪表板配置 Mapper
 */
@Mapper
public interface DashboardConfigMapper extends BaseMapperX<DashboardConfigDO> {

    /**
     * 根据用户ID和页面类型查询配置
     */
    default DashboardConfigDO selectByUserIdAndPageType(Long userId, String pageType) {
        return selectOne(DashboardConfigDO::getUserId, userId,
                DashboardConfigDO::getPageType, pageType);
    }

    /**
     * 根据用户ID和页面类型物理删除配置（避免唯一键冲突）
     */
    @Delete("DELETE FROM system_dashboard_config WHERE user_id = #{userId} AND page_type = #{pageType}")
    int physicalDeleteByUserIdAndPageType(@Param("userId") Long userId, @Param("pageType") String pageType);

}
