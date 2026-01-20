package cn.shuhe.system.framework.common.util.json.databind;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 基于时间戳的 LocalDateTime 反序列化器
 * 支持以下格式：
 * 1. 时间戳（毫秒数）
 * 2. 字符串格式 yyyy-MM-dd HH:mm:ss
 * 3. ISO 8601 格式 yyyy-MM-ddTHH:mm:ss.SSSZ
 *
 * @author 老五
 */
public class TimestampLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    public static final TimestampLocalDateTimeDeserializer INSTANCE = new TimestampLocalDateTimeDeserializer();

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        // 如果是数字类型，按时间戳处理
        if (p.currentToken() == JsonToken.VALUE_NUMBER_INT) {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(p.getValueAsLong()), ZoneId.systemDefault());
        }
        // 如果是字符串类型，尝试多种格式解析
        if (p.currentToken() == JsonToken.VALUE_STRING) {
            String text = p.getText();
            if (text == null || text.isEmpty()) {
                return null;
            }
            
            // 1. 尝试按 "yyyy-MM-dd HH:mm:ss" 格式解析
            try {
                return LocalDateTime.parse(text, FORMATTER);
            } catch (DateTimeParseException ignored) {
            }
            
            // 2. 尝试按 ISO 8601 格式解析（如 2026-01-21T12:37:38.000Z）
            try {
                // 如果包含 'Z' 或时区信息，使用 ZonedDateTime 解析后转换为本地时间
                if (text.contains("Z") || text.contains("+") || text.matches(".*[+-]\\d{2}:\\d{2}$")) {
                    ZonedDateTime zdt = ZonedDateTime.parse(text, ISO_FORMATTER);
                    return zdt.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
                }
                // 否则直接作为 LocalDateTime 解析
                return LocalDateTime.parse(text, ISO_FORMATTER);
            } catch (DateTimeParseException ignored) {
            }
            
            // 3. 尝试按 ISO 本地日期时间格式解析（如 2026-01-21T12:37:38）
            try {
                return LocalDateTime.parse(text);
            } catch (DateTimeParseException ignored) {
            }
            
            // 4. 尝试作为时间戳解析
            try {
                long timestamp = Long.parseLong(text);
                return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
            } catch (NumberFormatException ignored) {
            }
            
            throw new IOException("无法解析时间: " + text);
        }
        return null;
    }

}
