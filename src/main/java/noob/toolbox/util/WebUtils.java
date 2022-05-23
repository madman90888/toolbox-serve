package noob.toolbox.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import noob.toolbox.domain.pojo.ResultData;
import org.springframework.util.ObjectUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class WebUtils {
    public static final String CONTENT_TYPE = "application/json; charset=UTF-8";
    public static final ObjectMapper mapper = new ObjectMapper();

    public static void writeString(Integer code, String msg, HttpServletResponse response) {
        response.setContentType(CONTENT_TYPE);
//        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        try(PrintWriter out = response.getWriter()) {
            ResultData data = ResultData.error(code, msg);
            out.write(mapper.writeValueAsString(data));
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取客户IP
     *
     * @return
     */
    public static String getIpAddr() {
        return getIpAddr(((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
    }

    /**
     * 获取客户IP地址
     *
     * @param request 请求
     * @return IP地址
     */
    public static String getIpAddr(HttpServletRequest request) {
        String ipAddress;
        // 大部分的代理都会加上这个请求头
        ipAddress = request.getHeader("x-forwarded-for");
        if (ObjectUtils.isEmpty(ipAddress) || "unknown".equalsIgnoreCase(ipAddress)) {
            // 用apache http做代理时一般会加上Proxy-Client-IP请求头
            ipAddress = request.getHeader("Proxy-Client-IP");
            if (ObjectUtils.isEmpty(ipAddress) || "unknown".equalsIgnoreCase(ipAddress)) {
                // 用apache http 做代理 weblogic 插件加上的头
                ipAddress = request.getHeader("WL-Proxy-Client-IP");
                if (ObjectUtils.isEmpty(ipAddress) || "unknown".equalsIgnoreCase(ipAddress)) {
                    ipAddress = request.getRemoteAddr();
                }
            }
        }
        // 通过多个代理情况
        if (!ObjectUtils.isEmpty(ipAddress) && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0];
        }
        // 是否为本地IP
        if ("0:0:0:0:0:0:0:1".equals(ipAddress) || "127.0.0.1".equals(ipAddress)) {
            try {
                final InetAddress localHost = InetAddress.getLocalHost();
                return localHost.getHostAddress();
            } catch (UnknownHostException e) {
                return "";
            }
        }
        return ipAddress;
    }

    /**
     * 获取顶级域名
     * @param domain
     * @return
     */
    public static String topLevelDomain(String domain) {
        if (ObjectUtils.isEmpty(domain)) {
            return "";
        }
        domain = domain.replaceAll("^(https?://)?(.+)(:\\d+/?)$", "$2");
        String m = domain;
        if (domain.length() - m.replaceAll(".", "").length() > 1) {
            domain = domain.replaceAll("(^.+\\.)(?=\\w+\\.\\w+$)", "");
        }
        return domain;
    }
}