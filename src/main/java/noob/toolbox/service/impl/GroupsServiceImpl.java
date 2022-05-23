package noob.toolbox.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import noob.toolbox.domain.entity.Domain;
import noob.toolbox.domain.entity.Groups;
import noob.toolbox.domain.vo.GroupsVo;
import noob.toolbox.mapper.DomainMapper;
import noob.toolbox.mapper.GroupsMapper;
import noob.toolbox.service.GroupsService;
import noob.toolbox.util.Assert;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.stream.Collectors;

@Lazy
@Service
public class GroupsServiceImpl extends ServiceImpl<GroupsMapper, Groups>
    implements GroupsService{
    public static final int DEFAULT_GROUP_ID = 1;

    @Autowired
    private DomainMapper domainMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public Groups getGroupsByName(String name) {
        final LambdaQueryWrapper<Groups> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Groups::getName, name);
        return getOne(queryWrapper);
    }

    @Override
    public List<GroupsVo> queryGroupsSelective(Groups groups) {
        // 查询条件
        final LambdaQueryWrapper<Groups> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(!ObjectUtils.isEmpty(groups.getName()), Groups::getName, groups.getName());
        queryWrapper.ge(!ObjectUtils.isEmpty(groups.getCreateTime()), Groups::getCreateTime, groups.getCreateTime());
        queryWrapper.le(!ObjectUtils.isEmpty(groups.getUpdateTime()), Groups::getUpdateTime, groups.getUpdateTime());
        queryWrapper.like(!ObjectUtils.isEmpty(groups.getRemark()), Groups::getRemark, groups.getRemark());
        final List<Groups> groupsList = list(queryWrapper);
        // 获取分组id列表，查询域名表
        final List<Integer> groupIds = groupsList.stream().map(groups1 -> groups1.getId()).distinct().collect(Collectors.toList());
        final LambdaQueryWrapper<Domain> queryWrapper1 = new LambdaQueryWrapper<>();
        queryWrapper1.in(Domain::getGroupId, groupIds);
        final List<Domain> list = domainMapper.selectList(queryWrapper1);
        // 遍历统计
        return groupsList.stream().map(groups1 -> {
            final GroupsVo groupsVo = new GroupsVo();
            BeanUtils.copyProperties(groups1, groupsVo);
            final long count1 = list.stream()
                    .filter(domain -> domain.getGroupId().equals(groups1.getId()))
                    .map(domain -> domain.getName()).distinct().count();
            groupsVo.setDomainCount((int) count1);
            final long count2 = list.stream()
                    .filter(domain -> domain.getGroupId().equals(groups1.getId()))
                    .map(domain -> domain.getCode()).distinct().count();
            groupsVo.setInvitationCount((int) count2);
            return groupsVo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Integer> queryGroups(String nameKey) {
        final LambdaQueryWrapper<Groups> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(Groups::getName, nameKey);
        final List<Groups> list = list(queryWrapper);
        return list.stream().map(groups -> groups.getId()).collect(Collectors.toList());
    }

    @Override
    public boolean create(Groups groups) {
        groups.setId(null);
        Assert.isNull(getGroupsByName(groups.getName()), "小组名已存在");
        return save(groups);
    }

    @Override
    public boolean updateByPrimaryKeySelective(Groups groups) {
        Assert.isFalse(DEFAULT_GROUP_ID == groups.getId(), "默认分组不能修改");
        // 判断小组名是否存在
        if (!ObjectUtils.isEmpty(groups.getName())) {
            final Groups groups1 = getGroupsByName(groups.getName());
            if (groups1 != null) {
                Assert.isTrue(groups1.getId().equals(groups.getId()), "该小组名已存在");
            }
        }
        groups.setCreateTime(null);
        // 删除域名缓存
        redisTemplate.delete(DomainServiceImpl.DOMAIN_CACHE);
        return updateById(groups);
    }

    @Override
    public void deleteById(Integer groupId) {
        Assert.isFalse(DEFAULT_GROUP_ID == groupId, "默认分组不能删除");
        removeById(groupId);
    }
}




