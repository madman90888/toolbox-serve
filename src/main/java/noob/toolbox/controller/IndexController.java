package noob.toolbox.controller;

import lombok.extern.slf4j.Slf4j;
import noob.toolbox.domain.entity.Domain;
import noob.toolbox.domain.pojo.Denied;
import noob.toolbox.domain.pojo.ResultData;
import noob.toolbox.service.DomainService;
import noob.toolbox.service.impl.DomainServiceImpl;
import noob.toolbox.util.WebUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Objects;

@Slf4j
@Controller
public class IndexController {

    @Autowired
    private DomainService domainService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @GetMapping
    public String index(HttpServletRequest request) {
        final String domainName = WebUtils.topLevelDomain(request.getHeader("host"));
        final Domain domain = domainService.getDomainByName(domainName);
        String name;
        if (ObjectUtils.isEmpty(domain) || ObjectUtils.isEmpty(domain.getPageName())) {
            log.debug("{} 该域名未绑定静态页，使用默认页", domainName);
            name = DomainServiceImpl.DEFAULT_PAGE_NAME;
        }else {
            name = domain.getPageName();
        }
        return "forward:/html/" + name + "/index.html";
    }

    @GetMapping("admin")
    public String admin() {
        return "forward:/index.html";
    }

    @CrossOrigin
    @GetMapping("code")
    @ResponseBody
    public String code(HttpServletRequest request, String m) {
        Domain domain;
        // 是否要查询指定域名
        if (!ObjectUtils.isEmpty(m)) {
            domain= domainService.getDomainByName(WebUtils.topLevelDomain(m));
        }else {
            // 根据请求域名查询
            m = WebUtils.topLevelDomain(request.getHeader("referer"));
            domain = domainService.getDomainByName(m);
            if (Objects.isNull(domain)) {
                m = WebUtils.topLevelDomain(request.getHeader("host"));
                domain = domainService.getDomainByName(m);
            }
        }
        if (Objects.nonNull(domain) && !ObjectUtils.isEmpty(domain.getCode())) {
            return domain.getCode();
        }
        return "";
    }

    public static final String INDEX_ACCESS = "Index_Access";

    @GetMapping("indexAccess")
    @ResponseBody
    public Denied indexAccess() {
        return (Denied) redisTemplate.opsForValue().get(INDEX_ACCESS);
    }

    @PostMapping("indexAccess")
    @ResponseBody
    public ResultData indexAccess(@RequestBody Denied denied) {
        if (Objects.isNull(denied)) {
            return ResultData.error("参数不能为空");
        }
        redisTemplate.opsForValue().set(INDEX_ACCESS, denied);
        return ResultData.success();
    }
}
