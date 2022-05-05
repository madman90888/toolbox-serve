package noob.toolbox.config;

import noob.toolbox.resolver.JsonParamResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * 配置资源映射
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Value("${customer.file-save-path}")
    private String fileSavePath;

    @Autowired
    private JsonParamResolver jsonParamResolver;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/p/**")
                .addResourceLocations("file:" + fileSavePath);
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(jsonParamResolver);
        WebMvcConfigurer.super.addArgumentResolvers(resolvers);
    }
}
