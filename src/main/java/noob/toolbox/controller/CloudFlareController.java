package noob.toolbox.controller;

import com.alibaba.excel.EasyExcel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import noob.toolbox.domain.dto.FlareData;
import noob.toolbox.domain.dto.ZoneDto;
import noob.toolbox.domain.dto.ZoneSearch;
import noob.toolbox.domain.entity.Dns;
import noob.toolbox.domain.entity.Zone;
import noob.toolbox.domain.pojo.ResultData;
import noob.toolbox.domain.vo.DnsVo;
import noob.toolbox.domain.vo.PageInfo;
import noob.toolbox.domain.vo.ZoneBatchVo;
import noob.toolbox.domain.vo.ZoneVo;
import noob.toolbox.listener.RedisKeyExpirationListener;
import noob.toolbox.service.BatchCloudFlareService;
import noob.toolbox.util.AESCrypt;
import noob.toolbox.util.FileUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Validated
@Tag(name = "CloudFlare模块", description = "用于管理域名的批量查询、添加、删除<br>DNS的添加、删除")
@RestController
@RequestMapping("flare")
public class CloudFlareController {
    public static final String CLOUD_FLARE_TOKEN = "CloudFlare:token";
    public static final String key = "11f6fa0a98f58bhk";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private BatchCloudFlareService newInstance() {
        final String token = (String) redisTemplate.opsForValue().get(CLOUD_FLARE_TOKEN);
        if (ObjectUtils.isEmpty(token)) {
            throw new AccessDeniedException("缺少 CloudFlare 令牌");
        }
        return new BatchCloudFlareService(AESCrypt.decryptECB(token, key));
    }

    @Operation(summary = "验证令牌", description = "验证 CloudFlare 令牌是否有效，若有效则将令牌保存在服务端")
    @Parameter(name = "token", description = "身份令牌", required = true, in = ParameterIn.QUERY)
    @PostMapping("token")
    public ResultData<Map> verifyToken(@NotBlank(message = "token 不能为空") String token,
                                       Integer expire) {
        final BatchCloudFlareService service = new BatchCloudFlareService(AESCrypt.decryptECB(token, key));
        final FlareData flareData = service.verifyToken();
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

    @Operation(summary = "获取当前令牌", description = "获取设置的身份令牌，没有返回空")
    @GetMapping("token")
    public ResultData<Map> getToken() {
        final HashMap<String, String> map = new HashMap<>();
        String token = (String) redisTemplate.opsForValue().get(CLOUD_FLARE_TOKEN);
        if (ObjectUtils.isEmpty(token)) {
            return ResultData.error("令牌未设置");
        }
        map.put("token", token);
        map.put("expire", String.valueOf(redisTemplate.getExpire(CLOUD_FLARE_TOKEN, TimeUnit.HOURS)));
        return ResultData.success(map);
    }

    @Operation(summary = "清空令牌")
    @DeleteMapping("token")
    public ResultData deleteToken() {
        redisTemplate.delete(CLOUD_FLARE_TOKEN);
        return ResultData.success("清除成功");
    }

    @Operation(summary = "查询域名", description = "根据条件查询账户下的域名列表信息")
    @Parameter(in = ParameterIn.QUERY)
    @GetMapping("zones")
    public PageInfo<Zone> getZones(ZoneSearch zoneSearch) {
        return newInstance().queryZone(zoneSearch);
    }

    @Operation(summary = "批量添加域名", description = "根据数组批量添加域名到指定账户下")
    @PostMapping("zones")
    public List<ZoneVo> createZone(@Valid @RequestBody ZoneDto zoneDto) {
        return newInstance().createZones(zoneDto);
    }

    @Operation(summary = "批量设置HTTPS", description = "根据数组批量设置HTTPS、SSL级别")
    @PatchMapping("zones")
    public List<ZoneBatchVo> updateZone(@Valid @RequestBody ZoneDto zoneDto) {
        return newInstance().updateHttpsAndSSL(zoneDto);
    }

    @Operation(summary = "批量删除域名", description = "根据域名数组，删除账户下的域名")
    @Parameter(description = "域名集合", example = "[\"name1.com\", \"name2.com\"]")
    @DeleteMapping("zones")
    public List<ZoneVo> deleteZone(@NotEmpty(message = "域名列表不能为空") @RequestBody List<String> names) {
        return newInstance().deleteZonesByName(names);
    }

    @Operation(summary = "查询DNS记录", description = "根据参数查询指定域名下的所有DNS记录")
    @Parameter(in = ParameterIn.QUERY)
    @GetMapping("dns")
    public List<Dns> getDnsList(Dns dns) {
        return newInstance().queryDnsListByKeySelective(dns);
    }

    @Operation(summary = "批量添加DNS", description = "根据dns集合，批量添加DNS解析")
    @PostMapping("dns")
    public List<DnsVo> createDns(
            @NotEmpty(message = "dns列表不能为空")
            @Valid
            @RequestBody List<Dns> dnsList) {
        return newInstance().createDns(dnsList);
    }

    @Operation(summary = "批量删除DNS", description = "根据域名集合，清空域名下的所有DNS解析")
    @DeleteMapping("dns")
    public List<DnsVo> deleteDns(
            @NotEmpty(message = "域名列表不能为空")
            @RequestBody List<String> zoneNameList) {
        return newInstance().deleteDns(zoneNameList);
    }

    private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";

    @Operation(summary = "域名列表下载", description = "根据条件查询账户下的域名列表信息")
    @Parameter(in = ParameterIn.QUERY)
    @GetMapping("zones/down")
    public void downZoneList(ZoneSearch zoneSearch, HttpServletResponse response) throws IOException {
        final PageInfo<Zone> pageInfo = newInstance().queryZone(zoneSearch);
        final List<Zone> list = pageInfo.getRecord();
        final String format = DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT).format(LocalDate.now());
        String fileName = URLEncoder.encode("域名列表" + format, "UTF-8");
        response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");
        response.setHeader("Access-Control-Expose-Headers", "Content-disposition");
        EasyExcel.write(response.getOutputStream(), Zone.class)
                .autoCloseStream(Boolean.TRUE)
                .sheet("zone")
                .doWrite(list);
    }

    @Operation(summary = "文件增加DNS", description = "通过文件添加DNS记录")
    @PostMapping("dns/file")
    public List<DnsVo> uploadFile(MultipartFile file) throws IOException {
        final List<Dns> list = EasyExcel.read(file.getInputStream()).head(Dns.class).sheet().doReadSync();
        return newInstance().createDns(list);
    }

    @Operation(summary = "文件上传demo")
    @GetMapping("dns/demo")
    public void demoDown(HttpServletResponse response) throws IOException {
        final Dns dns = new Dns();
        dns.setZone_name("name.com");
        dns.setName("www");
        dns.setType("A");
        dns.setContent("1.1.1.1");
        dns.setTtl(1);
        final ArrayList<Dns> list = new ArrayList<>();
        list.add(dns);
        // 写出
        final String fileName = URLEncoder.encode("DNS模板", "UTF-8");
        response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");
        EasyExcel.write(response.getOutputStream(), Dns.class)
                .sheet("dns")
                .doWrite(list);
    }

}
