package noob.toolbox.service;

import lombok.extern.slf4j.Slf4j;
import noob.toolbox.cloudflare.HttpCloudFlare;
import noob.toolbox.controller.CloudFlareController;
import noob.toolbox.domain.dto.DnsDto;
import noob.toolbox.domain.dto.FlareData;
import noob.toolbox.domain.dto.FlarePage;
import noob.toolbox.domain.dto.ZoneSearch;
import noob.toolbox.domain.entity.Dns;
import noob.toolbox.domain.entity.Zone;
import noob.toolbox.domain.pojo.ResultData;
import noob.toolbox.domain.pojo.ResultRow;
import noob.toolbox.domain.vo.PageInfo;
import noob.toolbox.exception.CustomerException;
import noob.toolbox.util.AESCrypt;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.beans.BeanMap;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class CloudFlareService {
    public static final String FLARE_CACHE = "Cloud_Flare_Cache:";
    public static final String CLOUD_FLARE_TOKEN = "CloudFlare:token";
    public static final int TIME_OUT = 30;

    // 请求限制 5分钟 1200次请求
    private static final int REQUEST_INTERVAL_TIME = 5;
    private static final int REQUEST_COUNT_MAX = 1200;
    // 记录请求时间 次数
    private static LocalDateTime requestStart = LocalDateTime.now();
    private static int requestCount = 0;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 所有请求都调用这个方法，进行请求限制
     * @return
     */
    public HttpCloudFlare sendHttp() {
        final Duration duration = Duration.between(requestStart, LocalDateTime.now());
        if (duration.toMinutes() < REQUEST_INTERVAL_TIME) {
            requestCount++;
            log.debug("请求限制，时间：{}\t次数：{}", requestStart, requestCount);
            // 超出上限，抛出异常，终止请求
            if (requestCount > REQUEST_COUNT_MAX) {
                throw new CustomerException("请求达到上限，请5分钟后在使用！");
            }
        }else {
            log.info("上一次请求超过5分钟，重置时间和次数\n时间：{}\n次数：{}", requestStart, requestCount);
            requestStart = LocalDateTime.now();
            requestCount = 0;
        }
        return HttpCloudFlare.newInstance;
    }

    /**
     * 获取令牌
     */
    public String getToken() {
        final String token = (String) redisTemplate.opsForValue().get(CLOUD_FLARE_TOKEN);
        if (ObjectUtils.isEmpty(token)) {
            throw new AccessDeniedException("缺少 CloudFlare 令牌");
        }
        return token;
    }

    /**
     * 获取令牌 和 有效期
     * @return
     */
    public ResultData<Map> getTokenAndExpire() {
        String token = (String) redisTemplate.opsForValue().get(CLOUD_FLARE_TOKEN);
        if (ObjectUtils.isEmpty(token)) {
            throw new CustomerException("未设置令牌");
        }
        final HashMap<String, String> map = new HashMap<>();
        map.put("token", AESCrypt.encryptECB(token, CloudFlareController.key));
        map.put("expire", String.valueOf(redisTemplate.getExpire(CLOUD_FLARE_TOKEN, TimeUnit.HOURS)));
        map.put("request_start", requestStart.toString());
        map.put("count", String.valueOf(requestCount));
        return ResultData.success(map);
    }

    /**
     * 设置令牌
     * @param token     Cloudflare 令牌
     * @param expire    过期时间 小时
     * @return  成功返回令牌信息对象，失败返回错误信息
     */
    public ResultData<Map> setToken(String token, Integer expire) {
        final FlareData flareData = sendHttp().get("user/tokens/verify", token, null);
        if (flareData.isSuccess()) {
            if (expire != null && expire > 0) {
                redisTemplate.opsForValue().set(CLOUD_FLARE_TOKEN, token, expire, TimeUnit.HOURS);
            }else {
                redisTemplate.opsForValue().set(CLOUD_FLARE_TOKEN, token);
            }
            return ResultData.success(flareData.getResultMap());
        }
        return ResultData.error(flareData.errorToString());
    }

    /**
     * 清除令牌
     */
    public void deleteToken() {
        redisTemplate.delete(CLOUD_FLARE_TOKEN);
    }

    /**
     * 根据域名获取信息
     * @param name  域名信息
     * @return
     */
    public Zone getZone(String name) {
        return getZoneByName(Arrays.asList(name)).get(0);
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
        final String nameStr = sj.toString();
        // 先从缓存中查询
        final List<Zone> zoneList = (List<Zone>) redisTemplate.opsForValue().get(FLARE_CACHE + nameStr);
        if (!ObjectUtils.isEmpty(zoneList)) {
            log.debug("获取域名信息 -> 使用缓存数据");
            return zoneList;
        }

        // 设置参数
        HashMap<String, String> map = new HashMap<>();
        map.put("name", nameStr);
        pageInfo(map, null, null);
        FlareData result = sendHttp().get("/zones", getToken(), map);
        // 根据域名获取信息失败，直接抛出异常
        if (!result.isSuccess()) {
            throw new CustomerException(result.errorToString());
        }
        final List<Zone> list = result.getResultList(Zone.class);
        if (ObjectUtils.isEmpty(list)) {
            throw new CustomerException("未找到域名：" + sj);
        }

        // 缓存查询结果
        redisTemplate.opsForValue().set(FLARE_CACHE + nameStr, list, TIME_OUT, TimeUnit.MINUTES);
        return list;
    }

    /**
     * 查询域名
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
            result = sendHttp().get("/zones", getToken(), map);
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
        final FlareData flareData = sendHttp().post("/zones", getToken(), map);
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
        final FlareData flareData = sendHttp().delete("/zones/" + id, getToken());
        return flareData.getResult();
    }

    /**
     * 根据域名删除区域
     * @param zoneName    域名
     * @return
     */
    public ResultRow deleteZoneByName(String zoneName) {
        final List<Zone> zones = getZoneByName(Arrays.asList(zoneName));
        if (ObjectUtils.isEmpty(zones)) {
            return ResultRow.fail(zoneName + " 该域名未绑定");
        }
        redisTemplate.delete(FLARE_CACHE + zoneName);
        final FlareData flareData = sendHttp().delete("/zones/" + zones.get(0).getId(), getToken());
        return flareData.getResult();
    }

    /**
     * 是否始终使用HTTPS
     * @param zoneId        区域ID
     * @param status    是否使用 on/off
     */
    public ResultRow alwaysUseHttps(String zoneId, String status) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("value", "off".equals(status) ? "off" : "on");
        final FlareData flareData = sendHttp().patch( "/zones/" + zoneId + "/settings/always_use_https", getToken(), map);
        final ResultRow result = flareData.getResult();
        if (result.isSuccess()) {
            result.setMessage("off".equals(status) ? "关闭" : "开启");
        }
        return result;
    }

    /**
     * 获取始终使用HTTPS 状态
     * @param zoneId    区域ID
     * @return
     */
    public ResultRow getAlwaysUseHttps(String zoneId) {
        final FlareData flareData = sendHttp().get("/zones/" + zoneId + "/settings/always_use_https", getToken(), null);
        final ResultRow result = flareData.getResult();
        if (result.isSuccess()) {
            final Map map = flareData.getResultMap();
            result.setMessage("on".equals(map.get("value")) ? "开启" : "关闭");
        }
        return result;
    }

    /**
     * 自动重写HTTPS
     * @param zoneId        区域ID
     * @param status    是否使用 on/off
     */
    public ResultRow automaticRewritesHttps(String zoneId, String status) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("value", "off".equals(status) ? "off" : "on");
        final FlareData flareData = sendHttp().patch("/zones/" + zoneId + "/settings/automatic_https_rewrites", getToken(), map);
        final ResultRow result = flareData.getResult();
        if (result.isSuccess()) {
            result.setMessage("off".equals(status) ? "关闭" : "开启");
        }
        return result;
    }

    /**
     * 获取自动重写HTTPS 状态
     * @param zoneId    区域ID
     * @return
     */
    public ResultRow getAutomaticRewritesHttps(String zoneId) {
        final FlareData flareData = sendHttp().get("/zones/" + zoneId + "/settings/automatic_https_rewrites", getToken(), null);
        final ResultRow result = flareData.getResult();
        if (result.isSuccess()) {
            final Map map = flareData.getResultMap();
            result.setMessage("on".equals(map.get("value")) ? "开启" : "关闭");
        }
        return result;
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
        final FlareData flareData = sendHttp().patch("/zones/" + zoneId + "/settings/ssl", getToken(), map);
        final ResultRow result = flareData.getResult();
        if (result.isSuccess()) {
            result.setMessage((String) map.get("value"));
        }
        return result;
    }

    /**
     * 获取SSL设置
     * @param zoneId    区域ID
     * @return
     */
    public ResultRow getSSL(String zoneId) {
        final FlareData flareData = sendHttp().get("/zones/" + zoneId + "/settings/ssl", getToken(), null);
        final ResultRow result = flareData.getResult();
        if (result.isSuccess()) {
            final Map map = flareData.getResultMap();
            result.setMessage((String) map.get("value"));
        }
        return result;
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
            result = sendHttp().get("/zones/" + dns.getZone_id() + "/dns_records", getToken(), map);
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
     * @param zoneId    区域ID
     * @param dnsDto    Dns信息
     * @return
     */
    public ResultRow createDns(String zoneId, DnsDto dnsDto) {
        if (ObjectUtils.isEmpty(dnsDto.getTtl())) {
            dnsDto.setTtl(1);
        }
        if (ObjectUtils.isEmpty(dnsDto.getProxied())) {
            dnsDto.setProxied(true);
        }
        final FlareData flareData = sendHttp().post("/zones/" + zoneId + "/dns_records", getToken(), dnsDto);
        return flareData.getResult();
    }

    /**
     * 更新DNS记录
     * @param dns    dns详细信息
     * @return
     */
    public ResultRow updateDns(Dns dns) {
        final DnsDto dnsDto = new DnsDto();
        BeanUtils.copyProperties(dns, dnsDto);
        final FlareData flareData = sendHttp().put("/zones/" + dns.getZone_id() + "/dns_records/" + dns.getId(), getToken(), dnsDto);
        return flareData.getResult();
    }

    /**
     * 删除DNS记录
     * @param zoneId    区域ID
     * @param dnsId     DnsId
     * @return
     */
    public ResultRow deleteDns(String zoneId, String dnsId) {
        if (ObjectUtils.isEmpty(zoneId) || ObjectUtils.isEmpty(dnsId)) {
            return ResultRow.fail("域名ID 或 DNS ID 不能为空");
        }
        final FlareData flareData = sendHttp().delete("/zones/" + zoneId + "/dns_records/" + dnsId, getToken());
        return flareData.getResult();
    }

    /**
     * 根据域名清空 DNS 记录
     * @param zoneName
     * @return
     */
    public ResultRow deleteDnsByZoneName(String zoneName) {
        final Zone zone = getZoneByName(Arrays.asList(zoneName)).get(0);
        final List<Dns> dnsList = getDnsList(zone.getId());
        if (dnsList.isEmpty()) {
            return ResultRow.fail("该域名没有 DNS 记录，无需操作");
        }
        // 遍历删除Dns记录，遇到一个错误，则中断执行
        ResultRow resultRow = ResultRow.success();
        resultRow.setMessage("DNS记录清空完毕");
        dnsList.stream().anyMatch(dns -> {
            final ResultRow row = deleteDns(zone.getId(), dns.getId());
            if (!row.isSuccess()) {
                resultRow.setSuccess(false);
                resultRow.setMessage(row.getMessage());
                return true;
            }
            return false;
        });
        return resultRow;
    }

    /**
     * 查询分页设置
     * @param map   参数的map对象
     * @param page  页码
     * @param limit 每页大小
     */
    private static void pageInfo(Map<String, String> map, Integer page, Integer limit) {
        if (page == null || page == 0) {
            page = 1;
        }
        if (limit == null || limit == 0 || limit > 1000) {
            limit = 999;
        }
        map.put("page", String.valueOf(page));
        map.put("per_page", String.valueOf(limit));
    }
}
