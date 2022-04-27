package noob.toolbox.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class MessageInfo {
    private Integer code;
    private String message;
    private Object type;        // 验证令牌时返回
    @JsonProperty("error_chain")
    private List<MessageInfo> errorChain;

    public MessageInfo(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
