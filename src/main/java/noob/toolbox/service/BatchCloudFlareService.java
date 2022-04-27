package noob.toolbox.service;

import noob.toolbox.domain.dto.FlareData;
import noob.toolbox.domain.dto.ZoneDto;
import noob.toolbox.domain.dto.ZoneSearch;
import noob.toolbox.domain.entity.Dns;
import noob.toolbox.domain.entity.Zone;
import noob.toolbox.domain.pojo.ResultRow;
import noob.toolbox.domain.vo.DnsVo;
import noob.toolbox.domain.vo.PageInfo;
import noob.toolbox.domain.vo.ZoneBatchVo;
import noob.toolbox.domain.vo.ZoneVo;
import org.springframework.beans.BeanUtils;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.stream.Collectors;

public class BatchCloudFlareService {
    private CloudFlareService service;

    public BatchCloudFlareService(String token) {
        service = new CloudFlareService(token);
    }

    /**
     * 验证令牌是否有效
     * @return  成功返回令牌信息对象，失败返回错误信息
     */
    public FlareData verifyToken() {
        return service.verifyToken();
    }

    /**
     * 根据区域名关键字、状态查询
     * @return
     */
    public PageInfo<Zone> queryZone(ZoneSearch zoneSearch) {
        return service.queryZonesByKeySelective(zoneSearch);
    }

    /**
     * 根据域名批量添加
     * @param zoneDto
     * @return
     */
    public List<ZoneVo> createZones(ZoneDto zoneDto) {
        final List<ZoneVo> zoneVos = new ArrayList<>();
        zoneDto.getZoneNames().forEach(name -> {
            final Zone zone = new Zone(name, zoneDto.getJump(), zoneDto.getType());
            final ResultRow resultRow = service.createZone(zone);
            final ZoneVo zoneVo = new ZoneVo();
            zoneVo.setName(name);
            zoneVo.setSuccess(resultRow.isSuccess());
            zoneVo.setMessage(resultRow.getMessage());
            zoneVos.add(zoneVo);
        });
        return zoneVos;
    }

    /**
     * 批量更新区域HTTPS SSL级别
     */
    public List<ZoneBatchVo> updateHttpsAndSSL(ZoneDto zoneDto) {
        final List<Zone> zones = service.getZoneByName(zoneDto.getZoneNames());
        final List<ZoneBatchVo> list = zones.stream().map(zone -> service.updateHttpsAndSSL(zone, zoneDto.getAuto(), zoneDto.getAlways(), zoneDto.getSsl()))
                .collect(Collectors.toList());
        zoneDto.getZoneNames().forEach(name -> {
            final boolean b = zones.stream().anyMatch(zone -> zone.getName().equalsIgnoreCase(name));
            if (!b) {
                final ZoneBatchVo vo = new ZoneBatchVo();
                vo.setZoneName(name);
                vo.setAuto(ResultRow.fail("fail"));
                vo.setAlways(ResultRow.fail("fail"));
                vo.setSsl(ResultRow.fail("fail"));
                list.add(vo);
            }
        });
        return list;
    }

    /**
     * 根据ID删除区域
     * @param ids   id集合
     * @return
     */
    public List<ZoneVo> deleteZonesById(List<String> ids) {
        final ArrayList<ZoneVo> list = new ArrayList<>();
        for (String id : ids) {
            final ResultRow resultRow = service.deleteZoneById(id);
            list.add(ZoneVo.of(id, resultRow));
        }
        return list;
    }

    /**
     * 根据域名删除
     * @param names 域名集合
     * @return
     */
    public List<ZoneVo> deleteZonesByName(List<String> names) {
        // 根据域名获取 区域信息
        final List<Zone> zoneList = service.getZoneByName(names);
        final List<ZoneVo> list = new ArrayList<>();
        // 遍历删除
        zoneList.forEach(zone -> {
            final ResultRow resultRow = service.deleteZoneById(zone.getId());
            list.add(ZoneVo.of(zone.getName(), resultRow));
        });
        // 比较 要操作列表和实际查询到列表，是否有未绑定的域名，添加错误提示
        if (names.size() != list.size()) {
            names.forEach(name -> {
                final boolean b = zoneList.stream().anyMatch(zone -> name.equalsIgnoreCase(zone.getName()));
                if (!b) {
                    list.add(ZoneVo.fail(name, "未找到域名信息"));
                }
            });
        }
        return list;
    }

    /**
     * 按区域ID根据key查询DNS记录
     * @return
     */
    public List<Dns> queryDnsListByKeySelective(Dns dns) {
        return service.queryDnsListByKeySelective(dns);
    }

    /**
     * 批量添加DNS记录
     */
    public List<DnsVo> createDns(List<Dns> dnsList) {
        final List<String> zoneNames = dnsList.stream().map(dns -> dns.getZone_name()).distinct().collect(Collectors.toList());
        final List<Zone> zoneList = service.getZoneByName(zoneNames);
        // 匹配对应的ID
        dnsList.forEach(dns -> zoneList.stream().anyMatch(zone -> {
            if (zone.getName().equalsIgnoreCase(dns.getZone_name())) {
                dns.setZone_id(zone.getId());
                return true;
            }
            return false;
        }));
        // 遍历添加
        final ArrayList<DnsVo> list = new ArrayList<>();
        dnsList.forEach(dns -> {
            final DnsVo dnsVo = new DnsVo();
            BeanUtils.copyProperties(dns, dnsVo);
            if (ObjectUtils.isEmpty(dns.getZone_id())) {
                dnsVo.setSuccess(false);
                dnsVo.setMessage("未查到域名信息，无法添加DNS");
                list.add(dnsVo);
                return;
            }
            final ResultRow row = service.createDns(dns);
            dnsVo.setSuccess(row.isSuccess());
            dnsVo.setMessage(row.getMessage());
            list.add(dnsVo);
        });
        return list;
    }

    /**
     * 删除DNS记录
     * @param zoneNameList    域名名称集合
     * @return
     */
    public List<DnsVo> deleteDns(List<String> zoneNameList) {
        final List<Zone> zones = service.getZoneByName(zoneNameList);
        final ArrayList<DnsVo> list = new ArrayList<>();
        // 遍历域名，获取所有dns记录，再次遍历删除
        zones.forEach(zone -> {
            final List<Dns> dnsList = service.getDnsList(zone.getId());
            final DnsVo dnsVo = new DnsVo();
            dnsVo.setZone_name(zone.getName());
            if (dnsList.isEmpty()) {
                dnsVo.setSuccess(false);
                dnsVo.setMessage("该域名没有 DNS 记录，无需操作");
                list.add(dnsVo);
                return;
            }
            dnsList.forEach(dns -> service.deleteDns(zone.getId(), dns.getId()));
            list.add(dnsVo);
        });
        // 比较 要操作列表和实际查询到列表，是否有未绑定的域名，添加错误提示
        if (zoneNameList.size() != list.size()) {
            zoneNameList.forEach(name -> {
                final boolean b = zones.stream().anyMatch(zone -> zone.getName().equalsIgnoreCase(name));
                if (!b) {
                    final DnsVo dnsVo = new DnsVo();
                    dnsVo.setSuccess(false);
                    dnsVo.setZone_name(name);
                    dnsVo.setMessage("未查到域名信息，无法删除DNS记录");
                    list.add(dnsVo);
                }
            });
        }
        return list;
    }
}
