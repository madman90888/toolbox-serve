package noob.toolbox.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import noob.toolbox.domain.dto.DomainDto;
import noob.toolbox.domain.entity.Domain;
import com.baomidou.mybatisplus.extension.service.IService;
import noob.toolbox.domain.pojo.ResultRow;

import java.io.IOException;
import java.util.List;

public interface DomainService extends IService<Domain> {

    /**
     * 根据域名查询信息
     * @param name  域名
     * @return
     */
    Domain getDomainByName(String name);

    /**
     * 根据条件查询
     * @param domainDto
     * @return
     */
    Page<Domain> query(DomainDto domainDto);

    /**
     * 批量添加域名
     * @param domain
     * @return
     */
    ResultRow create(Domain domain);

    /**
     * 选择性更新，并清空缓存
     * @param domain
     * @return
     */
    boolean updateByPrimaryKeySelective(Domain domain);

    /**
     * 将组ID重置为默认
     * @param groupId
     * @return
     */
    boolean defaultGroupId(Integer groupId);

    /**
     * 根据ID数组删除
     * @param ids
     * @return
     */
    boolean deleteByIds(List<Integer> ids);

    /**
     * 获取当前所有落地页
     * @return
     */
    List<String> listPage();

    /**
     * 将页面设置为默认
     * @param pageName
     */
    void defaultPage(String pageName);

    /**
     * 删除分组，并将该分组域名重置为默认分组
     * @param groupId
     */
    void deleteGroup(Integer groupId);

    /**
     * 删除page，并将使用该page的域名重置到默认页
     * @param pageName
     */
    void deletePage(String pageName) throws IOException;
}
