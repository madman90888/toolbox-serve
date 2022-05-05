package noob.toolbox.resolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import noob.toolbox.resolver.annotation.JsonParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;

@Component
public class JsonParamResolver implements HandlerMethodArgumentResolver {

    @Autowired
    private ObjectMapper mapper;

    // 参数有使用 RequestSingleParam 注解
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(JsonParam.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        // 获取注解信息
        JsonParam requestSingleParam = parameter.getParameterAnnotation(JsonParam.class);
        // 获取形参名
        String paramName = requestSingleParam.value();
        if (ObjectUtils.isEmpty(paramName)) {
            paramName = parameter.getParameterName();
        }
        // 获取请求，读取请求体内容
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        BufferedReader reader = request.getReader();
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[1024];
        int rd;
        while ((rd = reader.read(buf)) != -1) {
            sb.append(buf, 0, rd);
        }
        final JsonNode readTree = mapper.readTree(sb.toString());
        final JsonNode jsonNode = readTree.get(paramName);
        if (ObjectUtils.isEmpty(jsonNode)) return null;
        return jsonNode.asText();
    }
}
