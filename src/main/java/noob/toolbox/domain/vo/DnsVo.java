package noob.toolbox.domain.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import noob.toolbox.domain.dto.DnsDto;

@Schema(hidden = true)
@Data
public class DnsVo extends DnsDto {
    private boolean success;
    private String message;
}
