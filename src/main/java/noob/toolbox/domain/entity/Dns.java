package noob.toolbox.domain.entity;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 *
 * @param dnsId    DnsID
 * @param name      记录名称
 * @param type      类型：A, AAAA, CNAME, HTTPS, TXT, SRV, LOC, MX, NS, CERT, DNSKEY, DS, NAPTR, SMIMEA, SSHFP, SVCB, TLSA, URI
 * @param content   DNS记录内容
 * @param ttl       DNS 记录的生存时间（以秒为单位）。必须介于 60 和 86400 之间，或 1 表示“自动”
 * @param proxied   记录是否正在获得 Cloudflare 的性能和安全优势
 */
@Schema(name = "DNS实体", description = "DNS完整信息")
@Data
public class Dns {
    @ExcelIgnore
    @Schema(description = "DNS ID")
    private String id;

    @ColumnWidth(8)
    @ExcelProperty(value = "类型", index = 2)
    @Schema(description = "类型")
    @NotBlank(message = "类型不能为空")
    private String type;

    @ColumnWidth(8)
    @ExcelProperty(value = "名称", index = 1)
    @Schema(description = "前缀")
    @NotBlank(message = "前缀不能为空")
    private String name;

    @ColumnWidth(20)
    @ExcelProperty(value = "解析值", index = 3)
    @Schema(description = "解析值")
    @NotBlank(message = "解析值不能为空")
    private String content;

    @ExcelIgnore
    @Schema(description = "MX、SRV 和 URI 记录需要")
    private Boolean proxiable;

    @ExcelIgnore
    @Schema(description = "记录是否正在获得 Cloudflare 的性能和安全优势")
    private Boolean proxied;

    @ColumnWidth(12)
    @ExcelProperty(value = "生效时间", index = 4)
    @Schema(description = "DNS 记录的生存时间（以秒为单位）。必须介于 60 和 86400 之间，或 1 表示“自动")
    private Integer ttl;

    @ExcelIgnore
    private Boolean locked;

    @ExcelIgnore
    @Schema(description = "域名ID")
    private String zone_id;

    @ColumnWidth(22)
    @ExcelProperty(value = "域名", index = 0)
    @Schema(description = "域名名称")
    @NotBlank(message = "域名不能为空")
    private String zone_name;

    @ExcelIgnore
    @Schema(description = "创建时间")
    private String created_on;

    @ExcelIgnore
    @Schema(description = "修改时间")
    private String modified_on;
}
