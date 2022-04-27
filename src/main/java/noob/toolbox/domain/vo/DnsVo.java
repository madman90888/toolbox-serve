package noob.toolbox.domain.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import noob.toolbox.domain.dto.DnsDto;

@Schema(hidden = true)
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DnsVo extends DnsDto {
    private boolean success;
    private String message;
    private String zone_name;

    public DnsVo() {
        this.success = true;
        this.message = "ok";
    }
}
