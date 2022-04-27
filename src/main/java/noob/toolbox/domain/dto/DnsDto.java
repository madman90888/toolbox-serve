package noob.toolbox.domain.dto;

import lombok.Data;

@Data
public class DnsDto {
    private String type;
    private String name;
    private String content;
    private Integer ttl;
    private Boolean proxied;
}
