package noob.toolbox.config;

import lombok.extern.slf4j.Slf4j;
import noob.toolbox.controller.IndexController;
import noob.toolbox.domain.pojo.Denied;
import noob.toolbox.service.impl.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
public class AppStartupRunner implements ApplicationRunner {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${logging.file.path}")
    private String logFile;

    @Value("${customer.file-save-path}")
    private String fileSavePath;

    @Value("${customer.fake-page}")
    private String fakePagePath;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 登录账户
        final String admin = (String) redisTemplate.opsForValue().get(UserService.USER_KEY + "admin");
        if (admin == null) {
            // qwe123
            redisTemplate.opsForValue().set(UserService.USER_KEY + "admin", "2aeb0fd37a8c1ba10336bc1fdcd6dc16");
            log.error("Redis没有初始用户，自动添加admin用户");
        }
        // 文件夹是否存在
        if (logFile != null) {
            final File file = new File(logFile);
            if (!file.exists()) {
                file.mkdirs();
                log.error("存放日志的文件夹不存在，自动创建：" + logFile);
            }
        }
        if (fileSavePath != null) {
            final File file = new File(fileSavePath);
            if (!file.exists()) {
                file.mkdirs();
                log.error("上传文件存放目录不存在，自动创建：" + fileSavePath);
            }
        }
        final Denied denied = (Denied) redisTemplate.opsForValue().get(IndexController.INDEX_ACCESS);
        if (Objects.isNull(denied)) {
            final Denied d = new Denied();
            d.setUse(false);
            final List<String> list = new ArrayList<>();
            list.add("Java");
            list.add("BIZCO");
            list.add("wget");
            list.add("curl");
            list.add("libcurl");
            list.add("python");
            list.add("CheckMarkNetwork");
            list.add("facebookexternalhit");
            d.setFilter(list);
            d.setType(1);
            redisTemplate.opsForValue().set(IndexController.INDEX_ACCESS, d);
        }

        if (fakePagePath != null) {
            final File file = new File(fakePagePath);
            if (!file.exists()) {
                file.mkdirs();
                log.error("存放虚假的html文件夹不存在，自动创建：" + fakePagePath);
            }
        }
    }
}
