package noob.toolbox.service;

import noob.toolbox.domain.entity.Groups;
import com.baomidou.mybatisplus.extension.service.IService;
import noob.toolbox.domain.vo.GroupsVo;

import java.util.List;

public interface GroupsService extends IService<Groups> {

    /**
     * 根据名字获取信息
     * @param name
     * @return
     */
    Groups getGroupsByName(String name);

    List<GroupsVo> queryGroupsSelective(Groups groups);

    /**
     * 根据关键字查询分组
     * @param nameKey
     * @return
     */
    List<Integer> queryGroups(String nameKey);

    /**
     * 创建小组
     * @param groups
     * @return
     */
    boolean create(Groups groups);

    /**
     * 修改小组信息
     * @param groups
     * @return
     */
    boolean updateByPrimaryKeySelective(Groups groups);

    /**
     * 删除小组
     * @param groupId
     */
    void deleteById(Integer groupId);
}
