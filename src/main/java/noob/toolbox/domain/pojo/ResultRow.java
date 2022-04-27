package noob.toolbox.domain.pojo;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Hidden
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResultRow {

    private boolean success;
    private String message;

    public static ResultRow success() {
        return new ResultRow(true, "ok");
    }

    public static ResultRow fail(String msg) {
        return new ResultRow(false, msg);
    }
}
