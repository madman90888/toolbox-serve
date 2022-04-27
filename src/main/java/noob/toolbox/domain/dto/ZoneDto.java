package noob.toolbox.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import java.util.List;

@Data
@Schema(name = "批量添加域名", description = "除了域名列表，其他参数都有默认值")
public class ZoneDto {

    @NotEmpty(message = "域名列表禁止为空")
    @Schema(name = "zoneNames", description = "域名列表数组", example = "[\"name1.com\", \"name2.com\"]", required = true)
    private List<String> zoneNames;

    @Schema(description = "添加域名时，是否尝试获取现有的 DNS 记录", defaultValue = "true")
    private Boolean jump;

    @Pattern(regexp = "^(full|partial)$", message = "参数只能是(full|partial)")
    @Schema(description = "DNS 是否由 Cloudflare 托管", defaultValue = "full", allowableValues = {"full", "partial"})
    private String type;

    @Schema(description = "是否要将 HTTP 自动跳转到 HTTPS", defaultValue = "true")
    private Boolean auto;

    @Schema(description = "是否始终使用 HTTPS", defaultValue = "true")
    private Boolean always;

    @Pattern(regexp = "^(off|flexible|full|strict)$", message = "无效级别")
    @Schema(description = "SSL级别", defaultValue = "flexible", allowableValues = {"off", "flexible", "full", "strict"})
    private String ssl;
}
