package noob.toolbox.interceptor;

import lombok.extern.slf4j.Slf4j;
import noob.toolbox.controller.IndexController;
import noob.toolbox.domain.pojo.Denied;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class LogInterceptor implements HandlerInterceptor {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${customer.fake-page}")
    private String fakePagePath;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        final String url = request.getRequestURL().toString();
        // 只对首页进行拦截
        if (!url.endsWith("/")) return true;

        // 是否需要拦截
        final Denied denied = (Denied) redisTemplate.opsForValue().get(IndexController.INDEX_ACCESS);
        if (Objects.isNull(denied) || denied.getUse() == null || denied.getUse() == false) {
            return true;
        }

        // 进行 user-agent 关键字判断
        final String userAgent = request.getHeader("user-agent").toLowerCase(Locale.ROOT);
        boolean headOff = false;
        if (ObjectUtils.isEmpty(userAgent)) {
            headOff = true;
        }else {
            if (!ObjectUtils.isEmpty(denied.getFilter())) {
                headOff = denied.getFilter().stream().anyMatch(s -> userAgent.contains(s.toLowerCase(Locale.ROOT)));
            }
        }

        if (!headOff) {
            // 进行 user-agent 关键字判断
            final String accept = request.getHeader("accept").toLowerCase(Locale.ROOT);
            if (ObjectUtils.isEmpty(accept)) {
                headOff = true;
            }else {
                if (!accept.startsWith("text/html")) headOff = true;
            }
        }

        // 记录日志
        if (log.isDebugEnabled()) {
            logAll(request);
        }else {
            log(request);
        }

        if (!headOff) return true;

        // 拦截处理方式
        final Integer type = denied.getType();
        if (type == 1) {
            sendHtml(response);
        }
        return false;
    }

    private void log(HttpServletRequest request) {
        final StringJoiner joiner = new StringJoiner("\n");
        joiner.add("==================== Request Header ====================");
        joiner.add("user-agent ==> " + request.getHeader("user-agent"));
        joiner.add("accept ==> " + request.getHeader("accept"));
        joiner.add("x-forwarded-for ==> " + request.getHeader("x-forwarded-for"));
        joiner.add("referer ==> " + request.getHeader("referer"));
        joiner.add("==================== End ====================\n");
        log.info(joiner.toString());
    }

    private void logAll(HttpServletRequest request) {
        final StringJoiner joiner = new StringJoiner("\n");
        joiner.add("==================== Request Header ====================");
        joiner.add("Request URL: " + request.getRequestURL());
        final Enumeration<String> names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            final String s = names.nextElement();
            joiner.add(s + " ==> " + request.getHeader(s));
        }
        joiner.add("==================== End ====================\n");
        log.debug(joiner.toString());
    }

    private void sendHtml(HttpServletResponse response) {
        final File dir = new File(fakePagePath);
        if (!dir.exists()) {
            log.error("未提供 HTML 文件，无法返回");
            return;
        }
        final File[] files = dir.listFiles();
        final List<File> list = Arrays.stream(files).filter(f -> f.isFile() && f.getName().endsWith("html")).collect(Collectors.toList());
        if (ObjectUtils.isEmpty(list)) {
            return;
        }
        final File file = list.get((int) (Math.random() * list.size()));

        response.setContentType("text/html; charset=UTF-8");
        try (final FileInputStream fis = new FileInputStream(file);
            final ServletOutputStream out = response.getOutputStream()) {
            final byte[] buf = new byte[1024];
            int len;
            while ((len = fis.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
            out.flush();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
}
