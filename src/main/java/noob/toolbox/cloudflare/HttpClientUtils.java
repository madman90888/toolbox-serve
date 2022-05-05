package noob.toolbox.cloudflare;

import com.fasterxml.jackson.core.JsonProcessingException;
import noob.toolbox.util.JsonUtil;
import org.apache.http.*;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.springframework.util.ObjectUtils;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

/**
 * HttpClient 请求工具类
 * 支持：get、delete、post、put、patch 请求
 * 接收参数支持Map、JSON格式
 * 返回响应体内容字符串
 */
public class HttpClientUtils {
    // 编码格式
    public static final String ENCODING = "UTF-8";
    // 使用连接池来管理连接,从连接池获取连接的超时时间
    public static final int REQUEST_TIMEOUT = 9000;
    // 连接超时,连接建立时间,三次握手完成时间，单位毫秒
    public static final int CONNECT_TIMEOUT = 9000;
    // 请求超时,数据传输过程中数据包之间间隔的最大时间
    public static final int SOCKET_TIMEOUT = 9000;
    // 全局连接池对象
    private static final PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();

    /**
     * 设置连接池的基本配置项
     */
    static {
        // 设置最大连接数
        connManager.setMaxTotal(200);
        /**
         * 设置每个连接的路由数
         * 路由是指 IP + Port 或指域名
         * 如果使用域名来访问，则每个域名有自己的连接池
         * 如果使用 IP + Port，则每个 IP + Port 有自己的连接池
         */
        connManager.setDefaultMaxPerRoute(20);
    }

    /**
     * 根据配置创建客户端对象
     * @return  请求客户端对象
     */
    private static CloseableHttpClient createHttpClient() {
        return HttpClients.custom()
                // 把请求相关的超时信息设置到连接客户端;
                .setDefaultRequestConfig(createConfig())
                // 把请求重试设置到连接客户端
                .setRetryHandler(HttpClientUtils::retry)
                // 配置连接池管理对象
                .setConnectionManager(connManager)
                .build();
    }

    /**
     * 创建客户端配置对象
     */
    private static RequestConfig createConfig() {
        // 创建HTTP客户端配置对象
        RequestConfig.Builder config = RequestConfig.custom()
                .setConnectionRequestTimeout(REQUEST_TIMEOUT)
                .setConnectTimeout(CONNECT_TIMEOUT)
                .setSocketTimeout(SOCKET_TIMEOUT)
                // 禁止Cookie
                .setCookieSpec(CookieSpecs.IGNORE_COOKIES);
        return config.build();
    }

    /**
     * 超时重试机制为了防止超时不生效而设置
     * 根据情况进行判断是否重试
     * 接口：HttpRequestRetryHandler;
     */
    private static boolean retry(IOException e, int i, HttpContext httpContext) {
        if (i > 3) {    //最多重试3次
            return false;
        }
        if (e instanceof NoHttpResponseException) {
            return true;    // 如果服务器丢掉了连接，那么就重试
        }
        if (e instanceof InterruptedIOException) {
            return true;    // 超时
        }
        if (e instanceof UnknownHostException) {
            return false;   // 目标服务器不可达
        }
        if (e instanceof ConnectTimeoutException) {
            return false;   // 连接被拒绝
        }
        if (e instanceof SSLHandshakeException) {
            return false;   // 不要重试SSL握手异常
        }
        if (e instanceof SSLException) {
            return false;   // ssl握手异常
        }
        HttpClientContext httpClientContext  = HttpClientContext.adapt(httpContext);
        HttpRequest request = httpClientContext.getRequest();
        // 如果请求是幂等的，就再次尝试
        if (!(request instanceof HttpEntityEnclosingRequest)) {
            return true;
        }
        return false;
    }

    /**
     * URL构建
     * @param url       访问地址
     * @param params    参数
     * @return
     */
    public static String uriBuilder(String url, Map<String, String> params) throws URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder(url);
        if (!ObjectUtils.isEmpty(params)) {
            Set<Map.Entry<String, String>> entrySet = params.entrySet();
            for (Map.Entry<String, String> entry : entrySet) {
                uriBuilder.addParameter(entry.getKey(), entry.getValue());
            }
        }
        return uriBuilder.toString();
    }

    /**
     * 封装请求头
     * @param request   http请求对象
     * @param headers   请求头参数对象
     */
    public static void packageHeader(HttpRequestBase request, Map<String, String> headers) {
        if (request == null || ObjectUtils.isEmpty(headers)) {
            return;
        }
        Set<Map.Entry<String, String>> entrySet = headers.entrySet();
        for (Map.Entry<String, String> entry : entrySet) {
            request.setHeader(entry.getKey(), entry.getValue());
        }
    }

    /**
     * 封装请求参数
     * @param request   http请求对象
     * @param params    请求参数参数
     * @param isForm      Map集合是否直接转为JSON格式
     * @throws UnsupportedEncodingException 不支持的编码异常
     */
    public static void packageParam(HttpEntityEnclosingRequestBase request, Object params, boolean isForm) throws UnsupportedEncodingException, JsonProcessingException {
        if (request == null || ObjectUtils.isEmpty(params)) {
            return;
        }
        HttpEntity entity;
        if (params instanceof String) {
            entity = new StringEntity((String) params);
        }else if (params instanceof Map && isForm) {
            // 类型是Map，且不是要直接转为JSON格式
            Map<String, Object> paramMap = (Map<String, Object>) params;
            // 定义一个list，该list的数据类型是NameValuePair（简单名称值对节点类型）
            ArrayList<NameValuePair> nvp = new ArrayList<>();
            Set<Map.Entry<String, Object>> entrySet = paramMap.entrySet();
            for (Map.Entry<String, Object> entry : entrySet) {
                nvp.add(new BasicNameValuePair(entry.getKey(), String.valueOf(entry.getValue())));
            }
            // 模拟表单
            entity = new UrlEncodedFormEntity(nvp, ENCODING);
        }else {
            entity = new StringEntity(JsonUtil.toJson(params));
        }
        request.setEntity(entity);
    }

    /**
     * 执行请求
     * @param requestMethod 请求方法对象
     * @return
     * @throws ClientProtocolException  协议错误
     * @throws ParseException   解析错误
     * @throws IOException      IO错误
     */
    public static String execute(HttpRequestBase requestMethod, Map<String, String> headers) throws IOException {
        CloseableHttpClient httpClient = createHttpClient();
        // 定义响应结果对象
        CloseableHttpResponse httpResponse = null;
        try {
            // 设置请求头
            packageHeader(requestMethod, headers);
            // 执行请求
            httpResponse = httpClient.execute(requestMethod);

            /**
             * 封装响应结果
             * 内部读取流结束后，会自动返还链接到连接池
             */
            // 根据指定编码，获取响应体内容
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            if (httpResponse.getEntity() != null) {
                return EntityUtils.toString(httpResponse.getEntity(), ENCODING);
            }else{
                // throw new RuntimeException(String.valueOf();=
                throw new RuntimeException(String.valueOf(statusCode));
            }
        }finally {
            /**
             * 当请求结束后做一个兜底链接归还（如果返回状态值不是200，则需要在这里处理链接归还）
             * 注意这里不能调用response.close();因为其不是归还链接到连接池，而是关闭链接。
             */
            if (httpResponse != null) {
                EntityUtils.consume(httpResponse.getEntity());
            }
        }
    }

    /**
     * 发送GET请求
     * @param url       请求地址
     * @param headers   请求头集合
     * @param params    请求参数集合
     * @return      返回响应体
     * @throws URISyntaxException   编码异常
     * @throws IOException  IO异常
     */
    public static String doGet(String url, Map<String, String> headers, Map<String, String> params) throws URISyntaxException, IOException {
        HttpGet http = new HttpGet(uriBuilder(url, params));
        return execute(http, headers);
    }

    /**
     * 发送Delete请求
     * @param url       请求地址
     * @param headers   请求头集合
     * @param params    请求参数集合
     * @return      返回响应体
     * @throws IOException  IO异常
     */
    public static String doDelete(String url, Map<String, String> headers, Map<String, String> params) throws IOException, URISyntaxException {
        HttpDelete http = new HttpDelete(uriBuilder(url, params));
        return execute(http, headers);
    }

    /**
     * 发送POST请求
     * @param url       请求地址
     * @param headers   请求头集合
     * @param params    请求体对象，支持对象、字符串
     * @param isForm    若请求参数是Map集合，是否使用Form表单方式
     * @return      返回响应体
     * @throws IOException  IO异常
     */
    public static String doPost(String url, Map<String, String> headers, Object params, boolean isForm) throws IOException {
        HttpPost http = new HttpPost(url);
        packageParam(http, params, isForm);
        return execute(http, headers);
    }

    public static String doPost(String url, Map<String, String> headers, Object params) throws IOException {
        return doPost(url, headers, params, false);
    }

    /**
     * 发送PUT请求
     * @param url       请求地址
     * @param headers   请求头集合
     * @param params    请求体对象，支持对象、字符串
     * @param isForm    若请求参数是Map集合，是否使用Form表单方式
     * @return      返回响应体
     * @throws IOException  IO异常
     */
    public static String doPut(String url, Map<String, String> headers, Object params, boolean isForm) throws IOException {
        HttpPut http = new HttpPut(url);
        packageParam(http, params, isForm);
        return execute(http, headers);
    }

    public static String doPut(String url, Map<String, String> headers, Object params) throws IOException {
        return doPut(url, headers, params, false);
    }

    /**
     * 发送PATCH请求
     * @param url       请求地址
     * @param headers   请求头集合
     * @param params    请求体对象，支持对象、字符串
     * @param isForm    若请求参数是Map集合，是否使用Form表单方式
     * @return      返回响应体
     * @throws IOException  IO异常
     */
    public static String doPatch(String url, Map<String, String> headers, Object params, boolean isForm) throws IOException {
        HttpPatch http = new HttpPatch(url);
        packageParam(http, params, isForm);
        return execute(http, headers);
    }

    public static String doPatch(String url, Map<String, String> headers, Object params) throws IOException {
        return doPatch(url, headers, params, false);
    }
}
