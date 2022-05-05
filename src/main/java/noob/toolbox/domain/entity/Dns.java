package noob.toolbox.domain.entity;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(name = "DNS实体", description = "DNS完整信息")
@Data
public class Dns {
    @ExcelIgnore
    @Schema(description = "DNS ID")
    private String id;

    @ColumnWidth(8)
    @ExcelProperty(value = "类型", index = 2)
    @Schema(description = "类型")
    private String type;

    @ColumnWidth(8)
    @ExcelProperty(value = "名称", index = 1)
    @Schema(description = "前缀")
    private String name;

    @ColumnWidth(20)
    @ExcelProperty(value = "解析值", index = 3)
    @Schema(description = "解析值")
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
    @Schema(description = "域名ID")
    private String zone_id;

    @ColumnWidth(22)
    @ExcelProperty(value = "域名", index = 0)
    @Schema(description = "域名名称")
    private String zone_name;

    @ExcelIgnore
    @Schema(description = "创建时间")
    private String created_on;

    @ExcelIgnore
    @Schema(description = "修改时间")
    private String modified_on;
}
