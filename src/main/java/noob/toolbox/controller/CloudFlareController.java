package noob.toolbox.controller;

import com.alibaba.excel.EasyExcel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import noob.toolbox.domain.dto.DnsDto;
import noob.toolbox.domain.dto.ZoneDto;
import noob.toolbox.domain.dto.ZoneSearch;
import noob.toolbox.domain.entity.Dns;
import noob.toolbox.domain.entity.Zone;
import noob.toolbox.domain.pojo.ResultData;
import noob.toolbox.domain.pojo.ResultRow;
import noob.toolbox.domain.vo.DnsVo;
import noob.toolbox.domain.vo.PageInfo;
import noob.toolbox.domain.vo.ZoneVo;
import noob.toolbox.resolver.annotation.JsonParam;
import noob.toolbox.service.impl.CloudFlareService;
import noob.toolbox.util.AESCrypt;
import noob.toolbox.validated.HTTPS;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.io.IOException;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Validated
@Tag(name = "CloudFlare模块", description = "用于管理域名的批量查询、添加、删除<br>DNS的添加、删除")
@RestController
@RequestMapping("flare")
public class CloudFlareController {
    private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    public static final String key = "11f6fa0a98f58bhk";

    @Autowired
    private CloudFlareService service;

    @Operation(summary = "设置令牌", description = "验证 CloudFlare 令牌是否有效，若有效则将令牌保存在服务端")
    @Parameter(name = "token", description = "身份令牌", required = true, in = ParameterIn.QUERY)
    @Parameter(name = "expire", description = "令牌保存有效期(小时)")
    @PostMapping("token")
    public ResultData<Map> setToken(@NotBlank(message = "token 不能为空") String token,
                                       Integer expire) {
        return service.setToken(AESCrypt.decryptECB(token, key), expire);
    }

    @Operation(summary = "获取当前令牌", description = "获取设置的身份令牌，没有返回空")
    @GetMapping("token")
    public ResultData<Map> getToken() {
        return service.getTokenAndExpire();
    }

    @Operation(summary = "清空令牌")
    @DeleteMapping("token")
    public ResultData deleteToken() {
        service.deleteToken();
        return ResultData.success("清除成功");
    }

    @Operation(summary = "查询域名", description = "根据条件查询账户下的域名列表信息")
    @Parameter(in = ParameterIn.QUERY)
    @GetMapping("zones")
    public PageInfo<Zone> getZones(ZoneSearch zoneSearch) {
        return service.queryZonesByKeySelective(zoneSearch);
    }

    @Operation(summary = "添加域名")
    @Parameter(name = "name", description = "域名", required = true, example = "name.com")
    @Parameter(name = "type", description = "域名托管状态", example = "full")
    @Parameter(name = "jumpStart", description = "获取现有的 DNS 记录", example = "true")
    @PostMapping("zones")
    public ZoneVo createZone(@RequestBody @Valid Zone zone) {
        return ZoneVo.of(zone.getName(), service.createZone(zone));
    }

    @Operation(summary = "删除域名")
    @DeleteMapping("zones")
    public ZoneVo updateZone(
            @JsonParam
            @NotBlank(message = "域名禁止为空") String name) {
        return ZoneVo.of(name, service.deleteZoneByName(name)) ;
    }

    @Operation(summary = "自动 HTTPS 重写状态")
    @Parameter(name = "name", description = "获取当前状态", example = "name.com")
    @GetMapping("zones/auto_https")
    public ZoneVo getAutoHttps(@NotBlank(message = "域名不能为空") String name) {
        final ResultRow resultRow = service.getAutomaticRewritesHttps(service.getZone(name).getId());
        return ZoneVo.of(name, resultRow);
    }

    @Operation(summary = "设置自动 HTTPS 重写")
    @Parameter(name = "value", description = "是否启用该功能该功能", example = "on|off")
    @PatchMapping("zones/auto_https")
    public ZoneVo updateAutoHttps(@RequestBody @Validated(HTTPS.class) ZoneDto zoneDto) {
        final ResultRow resultRow = service.automaticRewritesHttps(service.getZone(zoneDto.getName()).getId(), zoneDto.getValue());
        return ZoneVo.of(zoneDto.getName(), resultRow);
    }

    @Operation(summary = "始终使用 HTTPS状态")
    @Parameter(name = "name", description = "获取当前状态", example = "name.com")
    @GetMapping("zones/always_use_https")
    public ZoneVo getAlwaysUseHttps(@NotBlank(message = "域名不能为空") String name) {
        final ResultRow resultRow = service.getAlwaysUseHttps(service.getZone(name).getId());
        return ZoneVo.of(name, resultRow);
    }

    @Operation(summary = "设置始终使用 HTTPS")
    @Parameter(name = "value", description = "是否启用该功能该功能", example = "on|off")
    @PatchMapping("zones/always_use_https")
    public ZoneVo updateAlwaysUseHttps(@RequestBody @Validated(HTTPS.class) ZoneDto zoneDto) {
        final ResultRow resultRow = service.alwaysUseHttps(service.getZone(zoneDto.getName()).getId(), zoneDto.getValue());
        return ZoneVo.of(zoneDto.getName(), resultRow);
    }

    @Operation(summary = "获取当前 SSL/TLS 模式")
    @Parameter(name = "name", description = "获取当前模式", example = "name.com")
    @GetMapping("zones/ssl")
    public ZoneVo getSSL(@NotBlank(message = "域名不能为空") String name) {
        final ResultRow resultRow = service.getSSL(service.getZone(name).getId());
        return ZoneVo.of(name, resultRow);
    }

    @Operation(summary = "更改 SSL/TLS 模式")
    @Parameter(name = "value", description = "SSL/TLS 模式", example = "off|flexible|full|strict")
    @PatchMapping("zones/ssl")
    public ZoneVo updateSSL(@RequestBody @Validated ZoneDto zoneDto) {
        final ResultRow resultRow = service.changeSSL(service.getZone(zoneDto.getName()).getId(), zoneDto.getValue());
        return ZoneVo.of(zoneDto.getName(), resultRow);
    }

    @Operation(summary = "查询DNS记录", description = "根据参数查询指定域名下的所有DNS记录")
    @GetMapping("dns")
    public List<Dns> getDnsList(Dns dns) {
        return service.queryDnsListByKeySelective(dns);
    }

    @Operation(summary = "添加DNS")
    @PostMapping("dns")
    public DnsVo createDns(@RequestBody @Validated DnsDto dnsDto) {
        final Zone zone = service.getZoneByName(Arrays.asList(dnsDto.getZone_name())).get(0);
        final ResultRow res = service.createDns(zone.getId(), dnsDto);
        final DnsVo dnsVo = new DnsVo();
        BeanUtils.copyProperties(dnsDto, dnsVo);
        BeanUtils.copyProperties(res, dnsVo);
        return dnsVo;
    }

    @Operation(summary = "删除DNS")
    @DeleteMapping("dns")
    public ZoneVo deleteDns(
            @JsonParam
            @NotBlank(message = "域名禁止为空") String name) {
        return ZoneVo.of(name, service.deleteDnsByZoneName(name)) ;
    }

    @Operation(summary = "删除指定DNS")
    @DeleteMapping("dns/{zoneId}/{dnsId}")
    public ZoneVo deleteDnsById(
            @PathVariable @NotBlank(message = "域名ID禁止为空") String zoneId,
            @PathVariable @NotBlank(message = "DNS ID禁止为空") String dnsId ) {
        return ZoneVo.of(dnsId, service.deleteDns(zoneId, dnsId)) ;
    }

    @Operation(summary = "域名列表下载", description = "根据条件查询账户下的域名列表信息")
    @Parameter(in = ParameterIn.QUERY)
    @GetMapping("zones/down")
    public void downZoneList(ZoneSearch zoneSearch, HttpServletResponse response) throws IOException {
        final PageInfo<Zone> pageInfo = service.queryZonesByKeySelective(zoneSearch);
        final List<Zone> list = pageInfo.getRecord();
        final String format = DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT).format(LocalDate.now());
        String fileName = URLEncoder.encode("域名列表_" + format, "UTF-8");
        response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");
        EasyExcel.write(response.getOutputStream(), Zone.class)
                .autoCloseStream(Boolean.TRUE)
                .sheet("zone")
                .doWrite(list);
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

    @Operation(summary = "文件增加DNS", description = "通过文件添加DNS记录")
    @PostMapping("dns/file")
    public List<Dns> batchUploadFile(MultipartFile file) throws IOException {
        final List<Dns> list = EasyExcel.read(file.getInputStream()).head(Dns.class).sheet().doReadSync();
        return list;
    }
}
