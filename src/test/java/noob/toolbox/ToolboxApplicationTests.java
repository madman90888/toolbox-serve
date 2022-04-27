package noob.toolbox;

import noob.toolbox.controller.CloudFlareController;
import noob.toolbox.domain.dto.ZoneSearch;
import noob.toolbox.domain.entity.Zone;
import noob.toolbox.domain.vo.PageInfo;
import noob.toolbox.service.CloudFlareService;
import noob.toolbox.util.AESCrypt;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

@SpringBootTest
class ToolboxApplicationTests {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    void contextLoads() {
        redisTemplate.opsForValue().set(CloudFlareController.CLOUD_FLARE_TOKEN, "1", -1, TimeUnit.HOURS);
    }

    @Test
    void test() {
        final CloudFlareService service = new CloudFlareService("sE9KJklAx5mAHG0XDu766mClRmnyO98SqjzMhuf3");
        final ZoneSearch zoneSearch = new ZoneSearch();
        zoneSearch.setPage(1);
        zoneSearch.setLimit(20);
        final PageInfo<Zone> zones = service.queryZonesByKeySelective(zoneSearch);
    }

}
