package noob.toolbox.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import noob.toolbox.validated.HTTPS;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;

@Data
@Schema(name = "批量添加域名", description = "除了域名列表，其他参数都有默认值")
public class ZoneDto {

    @NotEmpty(message = "域名禁止为空")
    @Schema(name = "name", description = "域名", example = "name1.com", required = true)
    private String name;

    @Pattern(regexp = "^(off|flexible|full|strict)$", message = "无效级别")
    @Pattern(regexp = "^(off|on)$", message = "状态无效", groups = HTTPS.class)
    private String value;
}
