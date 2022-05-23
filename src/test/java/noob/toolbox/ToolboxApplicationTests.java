package noob.toolbox;

import noob.toolbox.service.IndexService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class ToolboxApplicationTests {

    @Autowired
    private IndexService indexService;
    @Test
    void contextLoads() {
        final List<String> list = indexService.list();

    }
}
