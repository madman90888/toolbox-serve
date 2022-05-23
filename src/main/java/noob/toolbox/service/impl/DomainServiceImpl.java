package noob.toolbox.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import noob.toolbox.domain.dto.DomainDto;
import noob.toolbox.domain.entity.Domain;
import noob.toolbox.domain.entity.Groups;
import noob.toolbox.domain.pojo.ResultRow;
import noob.toolbox.mapper.DomainMapper;
import noob.toolbox.service.DomainService;
import noob.toolbox.service.GroupsService;
import noob.toolbox.service.IndexService;
import noob.toolbox.util.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DomainServiceImpl extends ServiceImpl<DomainMapper, Domain>
    implements DomainService{
    public static final String DEFAULT_PAGE_NAME = "default";
    public static final String DOMAIN_CACHE = "Domain_Cache";
    public static final int CACHE_TIME = 24 * 60 * 60;

    @Autowired
    private GroupsService groupsService;

    @Autowired
    private IndexService indexService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public Domain getDomainByName(String name) {
        if (ObjectUtils.isEmpty(name)) return null;
        // 先从缓存中查询
        final Domain d = (Domain) redisTemplate.opsForHash().get(DOMAIN_CACHE, name);
        if (Objects.nonNull(d)) {
            return d;
        }
        // 缓存中没有，在从数据库中查询
        final LambdaQueryWrapper<Domain> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Domain::getName, name);
        final Domain domain = getOne(queryWrapper);
        if (Objects.nonNull(domain)) {
            redisTemplate.opsForHash().put(DOMAIN_CACHE, name, domain);
            redisTemplate.expire(DOMAIN_CACHE, CACHE_TIME, TimeUnit.SECONDS);
            log.debug("将域名信息写入缓存");
        }
        return domain;
    }

    @Override
    public Page<Domain> query(DomainDto domainDto) {
        // 排序
        final QueryWrapper<Domain> wrapper = new QueryWrapper<>();
        if (!ObjectUtils.isEmpty(domainDto.getDirection()) && "asc".equals(domainDto.getDirection())) {
            wrapper.orderByAsc(getColumnName(domainDto.getSort()));
        }else {
            wrapper.orderByDesc(getColumnName(domainDto.getSort()));
        }
        // 查询条件
        final LambdaQueryWrapper<Domain> queryWrapper = wrapper.lambda();
        queryWrapper.like(!ObjectUtils.isEmpty(domainDto.getName()), Domain::getName, domainDto.getName());
        queryWrapper.like(!ObjectUtils.isEmpty(domainDto.getCode()), Domain::getCode, domainDto.getCode());
        if (!ObjectUtils.isEmpty(domainDto.getGroupName())) {
            // 查询分组关键字
            final List<Integer> groups = groupsService.queryGroups(domainDto.getGroupName());
            queryWrapper.in(Domain::getGroupId, groups);
        }
        queryWrapper.like(!ObjectUtils.isEmpty(domainDto.getPageName()), Domain::getPageName, domainDto.getPageName());
        queryWrapper.like(!ObjectUtils.isEmpty(domainDto.getRemark()), Domain::getRemark, domainDto.getRemark());
        queryWrapper.ge(!ObjectUtils.isEmpty(domainDto.getCreateTime()), Domain::getCreateTime, domainDto.getCreateTime());
        queryWrapper.le(!ObjectUtils.isEmpty(domainDto.getUpdateTime()), Domain::getUpdateTime, domainDto.getUpdateTime());
        // 分页
        if (ObjectUtils.isEmpty(domainDto.getCurrent())) {
            domainDto.setCurrent(1);
        }
        if (ObjectUtils.isEmpty(domainDto.getLimit())) {
            domainDto.setLimit(10);
        }
        final Page<Domain> page = new Page<>(domainDto.getCurrent(), domainDto.getLimit());
        page(page, queryWrapper);
        // 查询分组名
        final List<Integer> groupIds = page.getRecords().stream().map(domain -> domain.getGroupId()).distinct().collect(Collectors.toList());
        if (!ObjectUtils.isEmpty(groupIds)) {
            final List<Groups> groups = groupsService.listByIds(groupIds);
            page.getRecords().stream().forEach(domain -> {
                groups.stream().anyMatch(group -> {
                    if (group.getId().equals(domain.getGroupId())) {
                        domain.setGroupName(group.getName());
                        return true;
                    }
                    return false;
                });
            });
        }
        return page;
    }

    private String getColumnName(String column) {
        String name = "create_time";
        if (ObjectUtils.isEmpty(column)) {
            return name;
        }else if ("name".equalsIgnoreCase(column)) {
            name = "name";
        }else if ("code".equalsIgnoreCase(column)) {
            name = "code";
        }else if ("groupName".equalsIgnoreCase(column)) {
            name = "group_id";
        }else if ("pageName".equalsIgnoreCase(column)) {
            name = "page_name";
        }else if ("remark".equalsIgnoreCase(column)) {
            name = "remark";
        }else if ("updateTime".equalsIgnoreCase(column)) {
            name = "update_time";
        }
        return name;
    }

    @Override
    public ResultRow create(Domain domain) {
        // 将ID置空，防止用户手动设置ID
        domain.setId(null);
        // 判断域名是否存在
        if (Objects.nonNull(getDomainByName(domain.getName()))) {
            return ResultRow.fail("域名已存在");
        }

        // 判断设置小组ID
        if (!ObjectUtils.isEmpty(domain.getGroupName())) {
            final Groups groups = groupsService.getGroupsByName(domain.getGroupName());
            if (Objects.isNull(groups)) {
                return ResultRow.fail("该小组不存在");
            }else {
                domain.setGroupId(groups.getId());
            }
        }else {
            domain.setGroupId(GroupsServiceImpl.DEFAULT_GROUP_ID);
        }

        // 判断站点是否存在
        if (!ObjectUtils.isEmpty(domain.getPageName()) ) {
            if (!indexService.exists(domain.getPageName())) {
                return ResultRow.fail("该站点不存在");
            }
        }else {
            domain.setPageName(DEFAULT_PAGE_NAME);
        }

        final boolean save = save(domain);
        if (save == false) {
            return ResultRow.fail("添加失败");
        }
        return ResultRow.success();
    }

    @Override
    public boolean updateByPrimaryKeySelective(Domain domain) {
        // 域名验证
        if (domain.getName() != null) {
            Assert.isFalse(domain.getName().length() == 0, "域名不能为空字符串");
            final Domain domain1 = getDomainByName(domain.getName());
            /**
             * 查询到的域名与当前ID不一致，则抛出异常
             */
            if (domain1 != null) {
                Assert.isTrue(domain1.getId().equals(domain.getId()), "该域名已存在");
            }
        }
        // 分组是否合法
        if (!ObjectUtils.isEmpty(domain.getGroupId())) {
            Assert.notNull(groupsService.getById(domain.getGroupId()), "该分组不存在");
        }

        // 判断站点是否存在
        if (!ObjectUtils.isEmpty(domain.getPageName())) {
            Assert.isTrue(indexService.exists(domain.getPageName()), "站点不存在");
        }
        domain.setCreateTime(null);

        // 删除缓存
        redisTemplate.delete(DOMAIN_CACHE);
        return updateById(domain);
    }

    @Override
    public boolean defaultGroupId(Integer groupId) {
        if (Objects.isNull(groupId)) {
            return false;
        }
        final LambdaUpdateWrapper<Domain> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.set(Domain::getGroupId, GroupsServiceImpl.DEFAULT_GROUP_ID);
        updateWrapper.eq(Domain::getGroupId, groupId);
        return update(updateWrapper);
    }

    @Override
    public boolean deleteByIds(List<Integer> ids) {
        // 删除缓存
        redisTemplate.delete(DOMAIN_CACHE);
        return removeByIds(ids);
    }

    @Override
    public List<String> listPage() {
        final List<Domain> domains = list(new QueryWrapper<Domain>().select("distinct page_name"));
        return domains.stream().map(domain -> domain.getPageName()).collect(Collectors.toList());
    }

    @Override
    public void defaultPage(String pageName) {
        // 删除缓存
        redisTemplate.delete(DOMAIN_CACHE);
        final LambdaUpdateWrapper<Domain> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.set(Domain::getPageName, DEFAULT_PAGE_NAME);
        updateWrapper.eq(Domain::getPageName, pageName);
        update(updateWrapper);
    }

    @Transactional
    @Override
    public void deleteGroup(Integer groupId) {
        // 删除缓存
        redisTemplate.delete(DOMAIN_CACHE);
        groupsService.deleteById(groupId);
        defaultGroupId(groupId);
    }

    @Transactional
    @Override
    public void deletePage(String pageName) throws IOException {
        // 删除缓存
        redisTemplate.delete(DOMAIN_CACHE);
        indexService.delete(pageName);
        defaultPage(pageName);
    }
}




