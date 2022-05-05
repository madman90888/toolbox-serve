package noob.toolbox.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class DnsDto {

    @NotBlank(message = "域名不能为空")
    private String zone_name;

    @NotBlank(message = "类型不能为空")
    private String type;

    @NotBlank(message = "前缀不能为空")
    private String name;

    @NotBlank(message = "解析值不能为空")
    private String content;

    private Integer ttl;
    private Boolean proxied;
}
