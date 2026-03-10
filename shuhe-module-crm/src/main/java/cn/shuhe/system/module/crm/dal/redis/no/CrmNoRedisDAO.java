package cn.shuhe.system.module.crm.dal.redis.no;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.shuhe.system.module.crm.dal.redis.RedisKeyConstants;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.LocalDateTime;


/**
 * Crm 订单序号的 Redis DAO
 *
 * @author HUIHUI
 */
@Repository
public class CrmNoRedisDAO {

    /**
     * A类合同前缀：有合作商的合同
     * 格式：A + yyyyMMdd + 6位序号，如 A20250101000001
     */
    public static final String CONTRACT_NO_PREFIX_A = "A";

    /**
     * B类合同前缀：无合作商的合同
     * 格式：B + yyyyMMdd + 6位序号，如 B20250101000001
     */
    public static final String CONTRACT_NO_PREFIX_B = "B";

    /**
     * 回款 {@link cn.shuhe.system.module.crm.dal.dataobject.receivable.CrmReceivablePlanDO}
     */
    public static final String RECEIVABLE_PREFIX = "HK";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 生成序号，使用当前日期，格式为 {PREFIX} + yyyyMMdd + 6 位自增
     * 例如说：QTRK 202109 000001 （没有中间空格）
     *
     * @param prefix 前缀
     * @return 序号
     */
    public String generate(String prefix) {
        // 递增序号
        String noPrefix = prefix + DateUtil.format(LocalDateTime.now(), DatePattern.PURE_DATE_PATTERN);
        String key = RedisKeyConstants.NO + noPrefix;
        Long no = stringRedisTemplate.opsForValue().increment(key);
        // 设置过期时间
        stringRedisTemplate.expire(key, Duration.ofDays(1L));
        return noPrefix + String.format("%06d", no);
    }

}
