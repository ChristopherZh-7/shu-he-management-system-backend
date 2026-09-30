package cn.shuhe.system.module.project.dal.mysql;

import cn.shuhe.system.framework.mybatis.core.mapper.BaseMapperX;
import cn.shuhe.system.module.project.dal.dataobject.GolishJobDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;

@Mapper
public interface GolishJobMapper extends BaseMapperX<GolishJobDO> {
    @Select("SELECT * FROM project_golish_job WHERE ticket_id = #{id} AND deleted = 0 ORDER BY id")
    List<GolishJobDO> forTicket(@Param("id") Long id);

    @Select("SELECT * FROM project_golish_job WHERE deleted = 0 AND imported = 0 AND next_poll <= NOW() AND lease_until < NOW() ORDER BY next_poll LIMIT 10")
    List<GolishJobDO> pending();

    @Update("UPDATE project_golish_job SET lease_token = #{token}, lease_until = DATE_ADD(NOW(), INTERVAL 180 SECOND) WHERE id = #{id} AND deleted = 0 AND imported = 0 AND lease_until < NOW()")
    int claim(@Param("id") Long id, @Param("token") String token);

    @Update("UPDATE project_golish_job SET lease_token = '', lease_until = '1970-01-01', next_poll = DATE_ADD(NOW(), INTERVAL #{delay} SECOND) WHERE id = #{id} AND lease_token = #{token}")
    int release(@Param("id") Long id, @Param("token") String token, @Param("delay") int delay);
}
