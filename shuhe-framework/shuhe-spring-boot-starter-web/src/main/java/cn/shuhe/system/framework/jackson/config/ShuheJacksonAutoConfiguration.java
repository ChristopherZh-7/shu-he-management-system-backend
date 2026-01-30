package cn.shuhe.system.framework.jackson.config;

import cn.shuhe.system.framework.common.util.json.JsonUtils;
import cn.shuhe.system.framework.common.util.json.databind.NumberSerializer;
import cn.shuhe.system.framework.common.util.json.databind.TimestampLocalDateTimeDeserializer;
import cn.shuhe.system.framework.common.util.json.databind.TimestampLocalDateTimeSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.context.annotation.Bean;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@AutoConfiguration(after = JacksonAutoConfiguration.class)
@Slf4j
public class ShuheJacksonAutoConfiguration {

    /**
     * 从 Builder 源头定制（关键：使用 *ByType，避免 handledType 要求）
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer ldtEpochMillisCustomizer() {
        return builder -> builder
                // 禁用日期序列化为数组格式，使用 ISO 字符串格式
                .featuresToDisable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                // Long -> Number
                .serializerByType(Long.class, NumberSerializer.INSTANCE)
                .serializerByType(Long.TYPE, NumberSerializer.INSTANCE)
                // LocalDate / LocalTime - 使用 ISO 字符串格式 (如 "2026-01-01")
                .serializerByType(LocalDate.class, LocalDateSerializer.INSTANCE)
                .deserializerByType(LocalDate.class, LocalDateDeserializer.INSTANCE)
                .serializerByType(LocalTime.class, LocalTimeSerializer.INSTANCE)
                .deserializerByType(LocalTime.class, LocalTimeDeserializer.INSTANCE)
                // LocalDateTime < - > EpochMillis
                .serializerByType(LocalDateTime.class, TimestampLocalDateTimeSerializer.INSTANCE)
                .deserializerByType(LocalDateTime.class, TimestampLocalDateTimeDeserializer.INSTANCE);
    }

    /**
     * 以 Bean 形式暴露 Module（Boot 会自动注册到所有 ObjectMapper）
     * 
     * 注意：LocalDate/LocalTime 使用 ISO 字符串格式，需要在 ObjectMapper 中禁用 WRITE_DATES_AS_TIMESTAMPS
     * 这已在 ldtEpochMillisCustomizer 中通过 featuresToDisable 设置
     */
    @Bean
    public Module timestampSupportModuleBean() {
        SimpleModule m = new SimpleModule("TimestampSupportModule");
        // Long -> Number，避免前端精度丢失
        m.addSerializer(Long.class, NumberSerializer.INSTANCE);
        m.addSerializer(Long.TYPE, NumberSerializer.INSTANCE);
        // LocalDate / LocalTime - 序列化器行为由 ObjectMapper 的 WRITE_DATES_AS_TIMESTAMPS 决定
        // 已在 ldtEpochMillisCustomizer 中禁用，所以会输出 ISO 字符串格式 "2026-01-01"
        m.addSerializer(LocalDate.class, LocalDateSerializer.INSTANCE);
        m.addDeserializer(LocalDate.class, LocalDateDeserializer.INSTANCE);
        m.addSerializer(LocalTime.class, LocalTimeSerializer.INSTANCE);
        m.addDeserializer(LocalTime.class, LocalTimeDeserializer.INSTANCE);
        // LocalDateTime < - > EpochMillis（自定义序列化器，直接输出时间戳）
        m.addSerializer(LocalDateTime.class, TimestampLocalDateTimeSerializer.INSTANCE);
        m.addDeserializer(LocalDateTime.class, TimestampLocalDateTimeDeserializer.INSTANCE);
        return m;
    }

    /**
     * 初始化全局 JsonUtils，直接使用主 ObjectMapper
     */
    @Bean
    @SuppressWarnings("InstantiationOfUtilityClass")
    public JsonUtils jsonUtils(ObjectMapper objectMapper) {
        JsonUtils.init(objectMapper);
        log.debug("[init][初始化 JsonUtils 成功]");
        return new JsonUtils();
    }

}
