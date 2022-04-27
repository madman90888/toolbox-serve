package noob.toolbox.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 配置资源映射
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Value("${customer.file-save-path}")
    private String fileSavePath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/p/**")
                .addResourceLocations("file:" + fileSavePath);
    }
}
