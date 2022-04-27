package noob.toolbox.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfiguration {

    /**
     * 配置消息监听器的容器
     */
    @Bean
    public RedisMessageListenerContainer container(RedisConnectionFactory redisConnectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        return container;
    }

    /**
     * 自定义redis序列化的机制,重新定义一个ObjectMapper.防止和MVC的冲突
     */
    @Bean
    public RedisSerializer<Object> redisSerializer() {
        ObjectMapper mapper = new ObjectMapper();
        /**************** 时间处理 ****************/
        // 使用JSR310提供的序列化类,里面包含了大量的JDK8时间序列化类
        mapper.registerModule(new JavaTimeModule());

        /**************** 序列化配置 ****************/
        // 属性值是 new Object() 时，不抛异常，序列化为 {}
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        // 不使用默认的dateTime进行序列化,
        mapper.disable(SerializationFeature.WRITE_DATE_KEYS_AS_TIMESTAMPS);

        // 指定要序列化的域，field,get和set,以及修饰符范围
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);

        /**************** 反序列化配置 ****************/
        // 反序列化时候遇到不匹配的属性 是否抛异常
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        // 反序列化的时候如果是无效子类型,是否抛出异常
        mapper.disable(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE);
        // 反序列化时，遇到忽略属性 是否抛出异常
        mapper.disable(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES);

        // 启用反序列化所需的类型信息,在属性中添加@class
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);
        // 配置null值的序列化器
        GenericJackson2JsonRedisSerializer.registerNullValueSerializer(mapper, null);
        return new GenericJackson2JsonRedisSerializer(mapper);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory, RedisSerializer<Object> redisSerializer) {
        // 创建RedisTemplate<String, Object>对象
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        // 配置连接工厂
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        // 设置默认序列化器
        redisTemplate.setDefaultSerializer(redisSerializer);
        // key使用 StringRedisSerializer 序列化
        RedisSerializer<String> stringSerial = new StringRedisSerializer();
        redisTemplate.setKeySerializer(stringSerial);
        redisTemplate.setHashKeySerializer(stringSerial);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }
}