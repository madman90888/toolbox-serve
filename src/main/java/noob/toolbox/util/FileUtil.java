package noob.toolbox.util;

import org.springframework.util.Assert;

import java.io.File;
import java.util.StringJoiner;

public class FileUtil {
    // win系统的分隔符需要转义
    public static final String SEPARATOR = File.separator.equals("\\") ? "\\\\" : "/";
    /**
     * 文件路径拼接，并将 / 或 \ 分隔符统一替换为当前系统分隔符
     * @param paths 要拼接的路径数组
     * <p>String[]</p>
     * @return 拼接好的路径
     */
    public static String filePathJoin(String... paths) {
        Assert.notEmpty(paths, "路径参数不能为空");
        final StringJoiner joiner = new StringJoiner(SEPARATOR);
        for (String path : paths) {
            joiner.add(path);
        }
        final String path = joiner.toString().replaceAll("[/\\\\]+", SEPARATOR);
        // 防止分隔符结尾
        if (path.endsWith(File.separator)) {
            return path.substring(0, path.length() - 1);
        }
        return path;
    }

    /**
     * 删除文件
     * @param filePath
     * @return
     */
    public static boolean delFile(String filePath) {
        final File file = new File(filePath);
        if (file.exists())
            return file.delete();
        return false;
    }
}
