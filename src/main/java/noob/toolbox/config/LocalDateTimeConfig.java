package noob.toolbox.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.expression.ParseException;
import org.springframework.format.Formatter;
import org.springframework.util.ObjectUtils;

import javax.annotation.PostConstruct;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Slf4j
@Configuration
public class LocalDateTimeConfig {
    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    public static final String DEFAULT_TIME_FORMAT = "HH:mm:ss";
    public static final String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT);
    public static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern(DEFAULT_TIME_FORMAT);
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DEFAULT_DATE_TIME_FORMAT);

    /**
     * 类型转换接口  org.springframework.core.convert.converter.Converter<S,T>
     */
    @Bean
    public Converter<String, LocalDateTime> localDateTimeConverter() {
        return new Converter<String, LocalDateTime>() {
            @Override
            public LocalDateTime convert(String source) {
                if (ObjectUtils.isEmpty(source)) return null;
                return LocalDateTime.parse(source, DATE_TIME_FORMATTER);
            }
        };
    }
    @Bean
    public Converter<String, LocalDate> localDateConverter() {
        return new Converter<String, LocalDate>() {
            @Override
            public LocalDate convert(String source) {
                if (ObjectUtils.isEmpty(source)) return null;
                return LocalDate.parse(source, DATE_FORMAT);
            }
        };
    }
    @Bean
    public Converter<String, LocalTime> localTimeConverter() {
        return new Converter<String, LocalTime>() {
            @Override
            public LocalTime convert(String source) {
                if (ObjectUtils.isEmpty(source)) return null;
                return LocalTime.parse(source, TIME_FORMAT);
            }
        };
    }

    @Autowired
    private ObjectMapper mapper;
    /**
     * 设置容器中的 ObjectMapper JDK8的时间格式
     */
    @PostConstruct
    private void dateTimeFormat() {
        log.debug("设置容器中的 ObjectMapper JDK8的时间格式");
        /**************** 时间格式处理 ****************/
        final SimpleModule simpleModule = new SimpleModule()
                // long 序列化 转为字符串
                .addSerializer(Long.class, ToStringSerializer.instance)
                .addSerializer(BigInteger.class, ToStringSerializer.instance);
        // JDK8 日期格式处理
        simpleModule
                // 序列化
                .addSerializer(LocalDate.class, new LocalDateSerializer(DATE_FORMAT))
                .addSerializer(LocalTime.class, new LocalTimeSerializer(TIME_FORMAT))
                .addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DATE_TIME_FORMATTER))
                // 反序列化
                .addDeserializer(LocalDate.class, new LocalDateDeserializer(DATE_FORMAT))
                .addDeserializer(LocalTime.class, new LocalTimeDeserializer(TIME_FORMAT))
                .addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DATE_TIME_FORMATTER));
        mapper.registerModule(simpleModule);
    }
}
