package noob.toolbox.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.io.IOException;

@Slf4j
@Component
public class JsonUtil {
    public static ObjectMapper mapper;

    public static ObjectMapper getMapper() {
        return mapper;
    }

    @Autowired
    public void setMapper(ObjectMapper mapper) {
        JsonUtil.mapper = mapper;
    }

    /**
     * 将对象解析为JSON字符串
     * @param obj
     * @param <T>
     * @return
     */
    public static <T> String toJson(T obj) {
        try {
            if (obj == null)
                return null;
            else if (obj instanceof String)
                return (String) obj;
            else
                return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Parse object to String error", e);
            return null;
        }
    }

    /**
     * Object转json字符串并格式化美化
     */
    public static <T> String toJsonPretty(T obj) {
        try {
            if (obj == null)
                return null;
            else if (obj instanceof String)
                return (String) obj;
            else
                return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Parse object to String Pretty error", e);
            return null;
        }
    }


    /**
     * json转object
     */
    public static <T> T toBean(String json, Class<T> clazz) {
        try {
            if (ObjectUtils.isEmpty(json) || clazz == null)
                return null;
            else if(clazz.equals(String.class))
                return (T)json;
            else
                return mapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("Parse String to bean error", e);
            return null;
        }
    }

    /**
     * json转集合
     * @param <T>
     * @param json
     * @param typeReference
     * <li>new TypeReference<List<User>>() {}</li>
     * @return
     */
    public static <T> T toBean(String json, TypeReference<T> typeReference) {
        try {
            if (ObjectUtils.isEmpty(json) || typeReference == null)
                return null;
            else if (typeReference.getType().equals(String.class))
                return (T) json;
            else
                return mapper.readValue(json, typeReference);
        } catch (IOException e) {
            log.error("Parse String to Bean error", e);
            return null;
        }
    }


    /**
     * string转object 用于转为集合对象
     * @param json Json字符串
     * @param collectionClass 被转集合的类
     * <p>List.class</p>
     * @param elementClasses 被转集合中对象类型的类
     * <p>User.class</p>
     */
    public static <T> T toBean(String json, Class<?> collectionClass, Class<?>... elementClasses) {
        try {
            JavaType javaType = mapper.getTypeFactory().constructParametricType(collectionClass, elementClasses);
            return mapper.readValue(json, javaType);
        } catch (IOException e) {
            log.error("Parse String to Bean error", e);
            return null;
        }
    }
}
