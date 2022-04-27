package noob.toolbox.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import noob.toolbox.domain.pojo.ResultData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Validated
@Tag(name = "消息模块", description = "临时存放信息")
@RestController
@RequestMapping("/msg")
public class MessageController {
    public static final String MESSAGE = "Message:";
    public static final String MSG_KEY = "msg";
    public static final String TIME_KEY = "time";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 设置文本，过期时间
     * @param key
     * @param list
     * @param time
     */
    private void setMes(String key, List<String> list, Integer time) {
        final HashOperations<String, Object, Object> opsForHash = redisTemplate.opsForHash();
        opsForHash.put(MESSAGE + key, MSG_KEY, list);
        if (time != null) {
            opsForHash.put(MESSAGE + key, TIME_KEY, time);
        }else {
            time = (Integer) opsForHash.get(MESSAGE + key, TIME_KEY);
        }
        redisTemplate.expire(MESSAGE + key, time, TimeUnit.HOURS);
    }

    @Operation(summary = "获取消息列表", description = "根据标识，获取消息列表")
    @Parameter(name = "m", description = "标识符", required = true, in = ParameterIn.PATH)
    @GetMapping("{m}")
    public ResultData<List> get(
            @PathVariable
            @NotBlank(message = "标识不能为空") String m) {
        final List<String> list = (List<String>) redisTemplate.opsForHash().get(MESSAGE + m, MSG_KEY);
        if (list == null) {
            return ResultData.error("标识有误");
        }
        return ResultData.success(list);
    }

    @Operation(summary = "新增标识", description = "新增一个标识")
    @Parameter(name = "m", description = "标识符，由字母数字组成", required = true, in = ParameterIn.PATH)
    @Parameter(name = "time", description = "消息有效时间，每次更新小时，将重置有效时长", in = ParameterIn.QUERY)
    @PostMapping("{m}")
    public ResultData<String> create(
            @PathVariable
            @NotBlank(message = "标识不能为空") String m, Integer time) {
        if (redisTemplate.opsForHash().get(MESSAGE + m, MSG_KEY) != null) {
            return ResultData.error("标识有误");
        }
        if (time == null || time == 0) {
            time = 24 * 30;
        }
        final ArrayList<String> list = new ArrayList<>();
        setMes(m, list, time);
        return ResultData.success("ok");
    }

    @Operation(summary = "添加消息", description = "根据标识，将消息添加到列表中")
    @Parameter(name = "m", description = "标识符", required = true, in = ParameterIn.QUERY)
    @Parameter(name = "text", description = "消息", required = true, in = ParameterIn.QUERY)
    @PutMapping("{m}")
    public ResultData<List> put(
            @PathVariable
            @NotBlank(message = "标识不能为空") String m,
            @NotBlank(message = "消息不能为空") String text) {
        final List<String> list = (List<String>) redisTemplate.opsForHash().get(MESSAGE + m, MSG_KEY);
        if (list == null) {
            return ResultData.error("标识有误");
        }
        list.add(text);
        setMes(m, list, null);
        return ResultData.success(list);
    }

    @Operation(summary = "删除消息", description = "根据标识删除消息列表")
    @Parameter(name = "m", description = "标识符", required = true, in = ParameterIn.PATH)
    @DeleteMapping("{m}")
    public ResultData<String> delete(
            @PathVariable
            @NotBlank(message = "标识不能为空") String m) {
        final List<String> list = (List<String>) redisTemplate.opsForHash().get(MESSAGE + m, MSG_KEY);
        if (list == null) {
            return ResultData.error("标识有误");
        }
        redisTemplate.delete(MESSAGE + m);
        return ResultData.success("ok");
    }

}
