package noob.toolbox.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface IndexService {
    /**
     * 获取所有站点，经过缓存
     * @return
     */
    List<String> list();

    /**
     * 获取所有站点
     * @return
     */
    List<String> listAll();

    /**
     * 判断站点吗是否存在
     * @param name
     * @return
     */
    boolean exists(String name);

    /**
     * 创建站点
     * @param file
     * @param pageName
     * @throws IOException
     */
    void create(MultipartFile file, String pageName) throws IOException;

    /**
     * 更新站点名
     * @param oldName
     * @param newName
     */
    void update(String oldName, String newName);

    /**
     * 删除站点
     * @param name
     * @throws IOException
     */
    void delete(String name) throws IOException;
}
