package noob.toolbox.service;

import noob.toolbox.cloudflare.HttpCloudFlare;
import noob.toolbox.domain.dto.DnsDto;
import noob.toolbox.domain.dto.FlareData;
import noob.toolbox.domain.dto.FlarePage;
import noob.toolbox.domain.dto.ZoneSearch;
import noob.toolbox.domain.entity.Dns;
import noob.toolbox.domain.entity.Zone;
import noob.toolbox.domain.pojo.ResultRow;
import noob.toolbox.domain.vo.PageInfo;
import noob.toolbox.domain.vo.ZoneBatchVo;
import noob.toolbox.exception.CustomerException;
import org.springframework.beans.BeanUtils;
import org.springframework.cglib.beans.BeanMap;
import org.springframework.util.ObjectUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

public class CloudFlareService {
    public static final String ZONE_PATH = "zones";
    // 身份令牌
    private String token;

    public CloudFlareService(String token) {
        this.token = token;
    }

    public String uriSplicing(String... path) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(ZONE_PATH);
        return builder.pathSegment(path).toUriString();
    }

    /**
     * 验证令牌是否有效
     * @return  成功返回令牌信息对象，失败返回错误信息
     */
    public FlareData verifyToken() {
        final FlareData flareData = HttpCloudFlare.get("user/tokens/verify", token, null);
        return flareData;
    }

    /**
     * 查询分页设置
     * @param map   参数的map对象
     * @param page  页码
     * @param limit 每页大小
     */
    public static void pageInfo(Map<String, String> map, Integer page, Integer limit) {
        if (page == null || page == 0) {
            page = 1;
        }
        if (limit == null || limit == 0 || limit > 1000) {
            limit = 999;
        }
        map.put("page", String.valueOf(page));
        map.put("per_page", String.valueOf(limit));
    }

    /**
     * 查询域名，多个通过“,”分隔
     * @param zoneNames 域名信息集合
     * @return
     */
    public List<Zone> getZoneByName(List<String> zoneNames) {
        // 拼接域名
        final StringJoiner sj = new StringJoiner(",");
        zoneNames.stream().forEach(name -> sj.add(name));
        if (sj.length() == 0) {
            return null;
        }
        // 设置参数
        HashMap<String, String> map = new HashMap<>();
        map.put("name", sj.toString());
        pageInfo(map, null, null);
        FlareData result = HttpCloudFlare.get(ZONE_PATH, token, map);
        if (!result.isSuccess()) {
            throw new CustomerException(result.errorToString());
        }
        final List<Zone> list = result.getResultList(Zone.class);
        if (ObjectUtils.isEmpty(list)) {
            throw new CustomerException("域名信息查询错误：" + sj);
        }
        return list;
    }

    /**
     * @return
     */
    public PageInfo<Zone> queryZonesByKeySelective(ZoneSearch zoneSearch) {
        // 封装参数
        HashMap<String, String> map = new HashMap<>();
        if (!ObjectUtils.isEmpty(zoneSearch.getName())) {
            map.put("name", "contains:" + zoneSearch.getName());
        }
        if (!ObjectUtils.isEmpty(zoneSearch.getStatus())) {
            map.put("status", zoneSearch.getStatus());
        }
        map.put("order", ObjectUtils.isEmpty(zoneSearch.getOrder()) ? "created_on" : zoneSearch.getOrder());
        map.put("direction", "desc".equalsIgnoreCase(zoneSearch.getDirection()) ? "desc" : "asc");
        pageInfo(map, zoneSearch.getPage(), zoneSearch.getLimit());

        //定义数组存放
        FlareData result;
        PageInfo<Zone> pageInfo = null;
        List<Zone> list = new ArrayList<>();
        while (true) {
            result = HttpCloudFlare.get(ZONE_PATH, token, map);
            // 查询失败
            if (!result.isSuccess()) {
                throw new CustomerException(result.errorToString());
            }
            // 查询结果不是null
            final List<Zone> resultList = result.getResultList(Zone.class);
            if (resultList != null){
                list.addAll(resultList);
            }
            final FlarePage info = result.getResultInfo();
            if (pageInfo == null && info != null) {
                pageInfo = new PageInfo<>();
                pageInfo.setPage(1);
                pageInfo.setLimit(info.getPer_page());
                pageInfo.setTotal(info.getTotal_count());
                pageInfo.setPages(info.getTotal_pages());
            }
            // 请求指定页，退出循环
            if (zoneSearch.getPage() != null && zoneSearch.getPage() > 0) {
                pageInfo.setPage(zoneSearch.getPage());
                pageInfo.setLimit(zoneSearch.getLimit());
                break;
            }
            Integer p = info.getPage();
            if (p == null || p >= info.getTotal_pages()) {
                break;
            }
            pageInfo(map, p + 1, zoneSearch.getLimit());
        }
        pageInfo.setRecord(list);
        return pageInfo;
    }

    /**
     *  创建域名
     * @return 失败返回错误信息，成功返回null，主键回填
     */
    public ResultRow createZone(Zone zone) {
        //封装参数
        HashMap<String, Object> map = new HashMap<>();
        map.put("name", zone.getName());
        if (zone.getJumpStart() != null) {
            map.put("jump_start", zone.getJumpStart());
        }
        if (ObjectUtils.isEmpty(zone.getType())) {
            map.put("type", zone.getType());
        }
        final FlareData flareData = HttpCloudFlare.post(ZONE_PATH, token, map);
        // 失败返回错误信息
        if (!flareData.isSuccess()) {
            return ResultRow.fail(flareData.errorToString());
        }
        // 成功将结果map回填给参数传来的zone对象
        final BeanMap beanMap = BeanMap.create(zone);
        beanMap.putAll(flareData.getResultMap());
        return ResultRow.success();
    }

    /**
     * 根据ID删除区域
     * @param id    区域ID
     * @return
     */
    public ResultRow deleteZoneById(String id) {
        final FlareData flareData = HttpCloudFlare.delete(uriSplicing(id), token);
        return flareData.getResult();
    }

    /**
     * 更新操作
     * @param zone      域名信息
     * @param auto      是否启用自动跳转HTTPS
     * @param always    始终使用HTTPS
     * @param ssl       SSL级别    off、flexible、full、strict
     * @return
     */
    public ZoneBatchVo updateHttpsAndSSL(Zone zone, Boolean auto, Boolean always, String ssl) {
        final ZoneBatchVo vo = new ZoneBatchVo();
        vo.setZoneName(zone.getName());
        // 启用https
        if (auto != null) {
            vo.setAuto(automaticRewritesHttps(zone.getId(), auto ? "on" : "off"));
        }
        if (always != null) {
            vo.setAlways(alwaysUseHttps(zone.getId(), always ? "on" : "off"));
        }
        if (!ObjectUtils.isEmpty(ssl)) {
            vo.setSsl(changeSSL(zone.getId(), ssl));
        }
        return vo;
    }

    /**
     * 是否始终使用HTTPS
     * @param zoneId        区域ID
     * @param status    是否使用 on/off
     */
    public ResultRow alwaysUseHttps(String zoneId, String status) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("value", "off".equals(status) ? "off" : "on");
        final FlareData flareData = HttpCloudFlare.patch(uriSplicing(zoneId, "settings", "always_use_https"), token, map);
        return flareData.getResult();
    }

    /**
     * 获取始终使用HTTPS 状态
     * @param zoneId    区域ID
     * @return
     */
    public Map getAlwaysUseHttps(String zoneId) {
        final FlareData flareData = HttpCloudFlare.get(uriSplicing(zoneId, "settings", "always_use_https"), token, null);
        return flareData.getResultMap();
    }

    /**
     * 自动重写HTTPS
     * @param zoneId        区域ID
     * @param status    是否使用 on/off
     */
    public ResultRow automaticRewritesHttps(String zoneId, String status) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("value", "off".equals(status) ? "off" : "on");
        final FlareData flareData = HttpCloudFlare.patch(uriSplicing(zoneId, "settings", "automatic_https_rewrites"), token, map);
        return flareData.getResult();
    }

    /**
     * 获取自动重写HTTPS 状态
     * @param zoneId    区域ID
     * @return
     */
    public Map getAutomaticRewritesHttps(String zoneId) {
        return HttpCloudFlare.get(uriSplicing(zoneId, "settings", "automatic_https_rewrites"), token,null).getResultMap();
    }

    /**
     * 更改SSL设置
     * @param zoneId    区域ID
     * @param value 设置off、flexible、full、strict
     *              关闭，灵活，充分，严格
     */
    public ResultRow changeSSL(String zoneId, String value) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("value", ObjectUtils.isEmpty(value) ? "flexible" : value);
        final FlareData flareData = HttpCloudFlare.patch(uriSplicing(zoneId, "settings", "ssl"), token, map);
        return flareData.getResult();
    }

    /**
     * 获取SSL设置
     * @param zoneId    区域ID
     * @return
     */
    public Map getSSL(String zoneId) {
        return HttpCloudFlare.get(uriSplicing(zoneId, "settings", "ssl"), token, null).getResultMap();
    }

    /**************** DNS操作 *********************/

    /**
     * 根据区域ID查询所有DNS记录
     * @param zoneId    区域ID
     * @return
     */
    public List<Dns> getDnsList(String zoneId) {
        final Dns dns = new Dns();
        dns.setZone_id(zoneId);
        return queryDnsListByKeySelective(dns);
    }

    /**
     * 按区域ID根据key查询DNS记录
     * @param dns    dns信息
     */
    public List<Dns> queryDnsListByKeySelective(Dns dns) {
        HashMap<String, String> map = new HashMap<>();
        //参数设置
        if (!ObjectUtils.isEmpty(dns.getName())) {
            map.put("name", "contains:" + dns.getName());
        }
        if (!ObjectUtils.isEmpty(dns.getType())) {
            map.put("type", "contains:" + dns.getType());
        }
        if (!ObjectUtils.isEmpty(dns.getContent())) {
            map.put("content", "contains:" + dns.getContent());
        }
        if (!ObjectUtils.isEmpty(dns.getProxied())) {
            map.put("proxied", String.valueOf(dns.getProxied()));
        }
        // 若没有查询条件
        if (map.isEmpty()) {
            map.put("match", "any");
        }
        pageInfo(map, null, null);
        //定义数组存放
        FlareData result;
        List<Dns> list = new ArrayList<>();
        while (true) {
            result = HttpCloudFlare.get(uriSplicing(dns.getZone_id(), "dns_records"), token, map);
            // 失败直接返回
            if (!result.isSuccess()) {
                throw new CustomerException(result.errorToString());
            }
            final List<Dns> dnsList = result.getResultList(Dns.class);
            if (dnsList != null){
                list.addAll(dnsList);
            }
            //判断是否查询完所有数据
            final FlarePage info = result.getResultInfo();
            Integer page = info.getPage();
            if (page == null || page >= info.getTotal_pages()) {
                break;
            }
            pageInfo(map, page + 1, null);
        }
        return list;
    }

    /**
     * 添加DNS记录
     * @param dns    Dns信息
     * @return
     */
    public ResultRow createDns(Dns dns) {
        final DnsDto dnsDto = new DnsDto();
        BeanUtils.copyProperties(dns, dnsDto);
        if (ObjectUtils.isEmpty(dnsDto.getTtl())) {
            dnsDto.setTtl(1);
        }
        if (ObjectUtils.isEmpty(dnsDto.getProxied())) {
            dnsDto.setProxied(true);
        }
        final FlareData flareData = HttpCloudFlare.post(uriSplicing(dns.getZone_id(), "dns_records"), token, dnsDto);
        // 成功将数据回填给参数传来的对象
        if (flareData.isSuccess()) {
            final BeanMap beanMap = BeanMap.create(dns);
            beanMap.putAll(flareData.getResultMap());
            return ResultRow.success();
        }
        return ResultRow.fail(flareData.errorToString());
    }

    /**
     * 更新DNS记录
     * @param dns    dns详细信息
     * @return
     */
    public ResultRow updateDns(Dns dns) {
        final DnsDto dnsDto = new DnsDto();
        BeanUtils.copyProperties(dns, dnsDto);
        final FlareData flareData = HttpCloudFlare.put(uriSplicing(dns.getZone_id(), "dns_records", dns.getId()), token, dnsDto);
        return flareData.getResult();
    }

    /**
     * 删除DNS记录
     * @param zoneId    区域ID
     * @param dnsId     DnsId
     * @return
     */
    public ResultRow deleteDns(String zoneId, String dnsId) {
        final FlareData flareData = HttpCloudFlare.delete(uriSplicing(zoneId, "dns_records", dnsId), token);
        return flareData.getResult();
    }

}
