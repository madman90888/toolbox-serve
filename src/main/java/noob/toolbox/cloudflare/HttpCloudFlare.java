package noob.toolbox.cloudflare;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import noob.toolbox.domain.dto.FlareData;
import noob.toolbox.util.JsonUtil;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * cloudFlare网站api请求工具类
 * 统一返回FlareJson对象
 * 失败：根据错误信息封装成Map
 */
@Slf4j
public class HttpCloudFlare {
    // 默认的请求地址
    public static final String BASE_URL = "https://api.cloudflare.com/client/v4/";

    /**
     * API请求头 包含令牌
     * @param token 令牌
     * @return
     */
    public static Map<String, String> authHeader(String token) {
        HashMap<String, String> heads = new HashMap<>();
        heads.put("Authorization", "Bearer " + token);
        heads.put("Content-Type", "application/json");
        return heads;
    }

    /**
     * url拼接
     * @param url
     * @return
     */
    private static String urlBuild(String url) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(BASE_URL);
        return builder.path(url).toUriString();
    }

    /**
     * GET请求
     * @param url       api路径
     * @param token     身份令牌
     * @param params    请求参数
     * @return
     */
    public static FlareData get(String url, String token, Map<String, String> params) {
        try {
            String s = HttpClientUtils.doGet(urlBuild(url), authHeader(token), params);
            return parseResponse(s);
        }catch (Exception e) {
            return exceptionHandler(e);
        }
    }

    /**
     * POST请求
     * @param url       api路径
     * @param token     身份令牌
     * @param params    请求参数
     * @return
     */
    public static <T> FlareData post(String url, String token, T params) {
        try {
            String s = HttpClientUtils.doPost(urlBuild(url), authHeader(token), params);
            return parseResponse(s);
        }catch (Exception e) {
            return exceptionHandler(e);
        }
    }

    /**
     * PUT请求
     * @param url       api路径
     * @param token     身份令牌
     * @param params    请求参数
     * @return
     */
    public static <T> FlareData put(String url, String token, T params) {
        try {
            String s = HttpClientUtils.doPut(urlBuild(url), authHeader(token), params);
            return parseResponse(s);
        }catch (Exception e) {
            return exceptionHandler(e);
        }
    }

    /**
     * PATCH请求
     * @param url       api路径
     * @param token     身份令牌
     * @param params    请求参数
     * @return
     */
    public static <T> FlareData patch(String url, String token, T params) {
        try {
            String s = HttpClientUtils.doPatch(urlBuild(url), authHeader(token), params);
            return parseResponse(s);
        }catch (Exception e) {
            return exceptionHandler(e);
        }
    }

    /**
     * DELETE请求
     * @param url       api路径
     * @param token     身份令牌
     * @return
     */
    public static FlareData delete(String url, String token) {
        try {
            String s = HttpClientUtils.doDelete(urlBuild(url), authHeader(token), null);
            return parseResponse(s);
        }catch (Exception e) {
            return exceptionHandler(e);
        }
    }

    /**
     * 解析响应数据
     * @param str   响应字符串
     * @return
     * @throws JsonProcessingException
     */
    public static FlareData parseResponse(String str) {
        FlareData flareResult = JsonUtil.toBean(str, FlareData.class);
        return flareResult;
    }

    /**
     * 根据错误类型，返回对应的错误提示
     * @param e     错误类
     * @return
     */
    public static FlareData exceptionHandler(Exception e) {
        log.error(e.getMessage(), e);
        String msg = "未知错误";
        if (e instanceof ClientProtocolException) {
            msg = "协议错误";
        }else if (e instanceof ParseException) {
            msg = "解析错误";
        }else if (e instanceof JacksonException) {
            msg = "JSON解析异常";
        }else if (e instanceof RuntimeException) {
            msg = e.getMessage();
        }
        return FlareData.newError(0, msg);
    }
}
