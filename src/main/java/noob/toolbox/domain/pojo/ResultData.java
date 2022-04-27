package noob.toolbox.domain.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "统一返回结果", description = "后端统一响应结果格式")
public class ResultData<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "状态码", example = "200")
    private int code;
    @Schema(description = "提示文本，一般有错误情况才会使用")
    private String message = "ok";
    @Schema(description = "响应数据")
    private T data;

    public ResultData(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public ResultData(int code, T data) {
        this.code = code;
        this.data = data;
    }

    public static ResultData message(String message) {
        return new ResultData(BusinessCode.SUCCESS.getCode(), message);
    }

    public static ResultData success() {
        return new ResultData(BusinessCode.SUCCESS.getCode(), "ok");
    }

    public static <T> ResultData<T> success(T data) {
        return new ResultData(BusinessCode.SUCCESS.getCode(), data);
    }

    public static ResultData error(String message) {
        return new ResultData(BusinessCode.FAILED.getCode(), message);
    }

    public static ResultData error(int code, String message) {
        return new ResultData(code, message);
    }

    public static <T> ResultData error(T data) {
        return new ResultData(400, data);
    }
}