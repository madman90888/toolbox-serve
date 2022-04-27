package noob.toolbox.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import noob.toolbox.listener.RedisKeyExpirationListener;
import noob.toolbox.util.FileUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Tag(name = "文件上传", description = "处理文件上传下载")
@RestController
public class FileController {
    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyMMdd");

    @Value("${customer.file-save-path}")
    private String fileSavePath;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Operation(summary = "文件上传", description = "根据条件，设置上传文件的位置、有效期")
    @Parameter(name = "file", description = "Spring封装的文件对象", required = true)
    @Parameter(name = "fileDir", description = "文件保存的目录名")
    @Parameter(name = "fileName", description = "用于单文件上传，指定文件名")
    @Parameter(name = "day", description = "文件有效期(天)")
    @PostMapping("/file")
    public Map uploadFile(MultipartFile file,
                          String fileDir,
                          String fileName,
                          Integer day,
                          HttpServletRequest request) {
        // 获取保存路径、后缀
        final String dirName = dirPath(fileDir);
        final String saveDir = FileUtil.filePathJoin(fileSavePath, dirName);
        final String suffix = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
        if (ObjectUtils.isEmpty(fileName)) {
            fileName = randomName();
        }
        String realPath = FileUtil.filePathJoin(saveDir, fileName + suffix);
        File newFile = new File(realPath);


        HashMap map = new HashMap();
        map.put("fileName", file.getOriginalFilename());
        try {
            file.transferTo(newFile);
            // 判断是否要删除文件
            if (day != null && day > 0) {
                // 存入redis
                redisTemplate.opsForValue().set(RedisKeyExpirationListener.EXPIRATION_DELETE_FILE + realPath, "", day, TimeUnit.DAYS);
            }
            // 协议 :// ip地址: 端口 文件目录
            String url = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort()
                    + "/p/" + dirName + "/" + fileName + suffix;
            map.put("success", true);
            map.put("url", url);
        } catch (IOException e) {
            e.printStackTrace();
            map.put("success", false);
            map.put("message", "上传失败");
        }
        return map;
    }

    /**
     * 判断文件夹是否存在
     * @param fileDir
     * @return
     */
    private String dirPath(String fileDir) {
        String path = null;
        // 判断文件夹名是否合法
        if (fileDir != null) {
            final boolean matches = fileDir.matches("^[0-9A-Za-z]{1,6}$");
            if (matches) {
                path = fileDir;
            }
        }
        // 若没有文件夹名，则使用当天日期作为文件夹名
        if (path == null) {
            path = FORMATTER.format(LocalDate.now());
        }
        String savePath = FileUtil.filePathJoin(fileSavePath, path);
        final File dir = new File(savePath);
        // 目录不存在，则创建
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return path;
    }

    /**
     * 根据时间戳后八位、转为32进制
     * @return
     */
    private String randomName() {
//        return Integer.toHexString(Integer.parseInt(String.valueOf(System.currentTimeMillis()).substring(5)));
        return new BigInteger(String.valueOf(System.currentTimeMillis()).substring(5), 10).toString(32);
    }
}
