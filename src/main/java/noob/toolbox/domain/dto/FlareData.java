package noob.toolbox.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import noob.toolbox.cloudflare.FlareErrorCode;
import noob.toolbox.domain.pojo.ResultRow;
import noob.toolbox.exception.CustomerException;
import org.springframework.beans.BeanUtils;
import org.springframework.cglib.beans.BeanMap;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Data
public class FlareData {
    private boolean success;
    private Object result;
    private List<MessageInfo> errors;
    private List<MessageInfo> messages;
    @JsonProperty("result_info")
    private FlarePage resultInfo;

    /**
     * 根据状态码创建一个错误
     * @param code
     * @param msg
     * @return
     */
    public static FlareData newError(int code, String msg) {
        FlareData flareData = new FlareData();
        flareData.setSuccess(false);
        // 设置错误消息
        List<MessageInfo> errors = new ArrayList();
        final MessageInfo messageInfo = new MessageInfo(code, msg);
        errors.add(messageInfo);
        flareData.setErrors(errors);
        return flareData;
    }

    /**
     * 将错误集合转为字符串
     * @return
     */
    public String errorToString() {
        if (ObjectUtils.isEmpty(errors)) {
            return "错误消息为空";
        }
        log.debug("错误消息：" + errors);
        StringBuffer buffer = new StringBuffer();
        errors.stream().forEach(info -> {
            buffer.append(FlareErrorCode.getMsg(info.getCode(), info.getMessage()))
                .append(" ");
            if (!ObjectUtils.isEmpty(info.getErrorChain())) {
                info.getErrorChain().stream()
                        .forEach(info2 -> {
                            buffer.append(FlareErrorCode.getMsg(info2.getCode(), info2.getMessage()))
                                    .append(" ");
                        });
            }
        });
        if (buffer.length() == 0) {
            return "未获取到错误消息";
        }
        return buffer.toString();
    }

    /**
     * 返回结果map
     * @return
     */
    public Map getResultMap() {
        if (result instanceof Map) {
            return (Map) result;
        }
        if (result instanceof List) {
            List list = (List) result;
            if (!ObjectUtils.isEmpty(list)) {
                return (Map) list.get(0);
            }
        }
        return null;
    }

    public<T> List<T> getResultList(Class<T> clazz) {
        if (!(result instanceof List)) {
            return null;
        }
        List list = (List) result;
        return (List<T>) list.stream().map(item -> {
            try {
                final T t = clazz.newInstance();
                final BeanMap beanMap = BeanMap.create(t);
                beanMap.putAll((Map) item);
                return t;
            } catch (Exception e) {
                throw new CustomerException(e);
            }
        }).collect(Collectors.toList());
    }

    /**
     * 成功与失败
     * @return
     */
    public ResultRow getResult() {
        if (success) {
            return ResultRow.success();
        }else{
            return ResultRow.fail(errorToString());
        }
    }
}
