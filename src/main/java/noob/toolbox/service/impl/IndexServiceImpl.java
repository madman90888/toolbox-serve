package noob.toolbox.service.impl;

import lombok.extern.slf4j.Slf4j;
import noob.toolbox.exception.CustomerException;
import noob.toolbox.service.IndexService;
import noob.toolbox.util.Assert;
import noob.toolbox.util.FileUtil;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class IndexServiceImpl implements IndexService {
    public static final String CACHE_KEY = "Static_Page_List";

    @Value("${customer.page-path}")
    private String pagePath;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public List<String> list() {
        final List<String> pageList = (List<String>) redisTemplate.opsForValue().get(CACHE_KEY);
        if (!ObjectUtils.isEmpty(pageList)) {
            return pageList;
        }
        final List<String> list = listAll();
        return list;
    }

    @Override
    public List<String> listAll() {
        final File baseDir = new File(pagePath);
        final File[] files = baseDir.listFiles();
        final List<String> list = new ArrayList<>();
        Arrays.stream(files).forEach(file -> {
            if (file.isDirectory()) list.add(file.getName());
        });
        if (!list.isEmpty()) {
            redisTemplate.opsForValue().set(CACHE_KEY, list, 1, TimeUnit.DAYS);
            log.debug("缓存静态页");
        }
        return list;
    }

    @Override
    public boolean exists(String name) {
        return list().stream().anyMatch(s -> s.equals(name));
    }

    @Override
    public void create(MultipartFile file, String pageName) throws IOException {
        // 判断站点是否存在
        final File dir = new File(FileUtil.filePathJoin(pagePath, pageName));
        Assert.isFalse(dir.exists(), "站点名错误");
        // 判断文件是否合法
        final String filename = file.getOriginalFilename();
        if (filename.endsWith(".html")) {
            dir.mkdir();
            final File newFile = new File(FileUtil.filePathJoin(pagePath, pageName, "index.html"));
            file.transferTo(newFile);
        }else {
            throw new CustomerException("目前只支持单页面.html 或 压缩包 .zip上传");
        }
    }

    @Override
    public void update(String oldName, String newName) {
        final File file = new File(FileUtil.filePathJoin(pagePath, oldName));
        Assert.isTrue(file.exists(), "站点名错误");
        final File newFile = new File(FileUtil.filePathJoin(pagePath, newName));
        Assert.isFalse(newFile.exists(), "已存在");
        // 删除域名缓存
        redisTemplate.delete(DomainServiceImpl.DOMAIN_CACHE);
        file.renameTo(newFile);
    }

    @Override
    public void delete(String name) throws IOException {
        final File file = new File(FileUtil.filePathJoin(pagePath, name));
        Assert.isTrue(file.exists(), "站点名错误");
        FileUtils.deleteDirectory(file);
    }
}
