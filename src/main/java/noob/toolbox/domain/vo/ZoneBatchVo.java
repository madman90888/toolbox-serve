package noob.toolbox.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import noob.toolbox.domain.pojo.ResultRow;

@Schema(hidden = true)
@Data
public class ZoneBatchVo {
    private String zoneName;
    private ResultRow auto;
    private ResultRow always;
    private ResultRow ssl;
}
