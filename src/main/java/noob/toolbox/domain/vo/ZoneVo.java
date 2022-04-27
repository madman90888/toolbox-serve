package noob.toolbox.domain.vo;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import noob.toolbox.domain.pojo.ResultRow;

@Schema(hidden = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ZoneVo {
    private String name;
    private boolean success;
    private String message;

    public ZoneVo(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public static ZoneVo of(String name, ResultRow resultRow) {
        return new ZoneVo(name, resultRow.isSuccess(), resultRow.getMessage());
    }

    public static ZoneVo success(String name) {
        return new ZoneVo(name, true, "ok");
    }

    public static ZoneVo fail(String name, String msg) {
        return new ZoneVo(name, false, msg);
    }
}
