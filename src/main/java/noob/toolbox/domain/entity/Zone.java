package noob.toolbox.domain.entity;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import noob.toolbox.converter.DateTimeConverter;
import noob.toolbox.converter.StatusConverter;

import javax.validation.constraints.NotBlank;

@Schema(name = "域名实体", description = "域名相对完整信息")
@Data
@NoArgsConstructor
public class Zone {
    @ExcelIgnore
    @Schema(description = "域名ID")
    private String id;

    @NotBlank(message = "域名不能为空")
    @ExcelProperty("域名")
    @ColumnWidth(25)
    @Schema(description = "名称")
    private String name;

    @ExcelProperty(value = "状态", converter = StatusConverter.class)
    @Schema(description = "状态")
    private String status;

    @ExcelIgnore
    @Schema(description = "域名是否处于暂停状态")
    private Boolean paused;

    @ExcelProperty("托管")
    @Schema(description = "域名托管状态")
    private String type;

    @ExcelProperty("注册商")
    @ColumnWidth(25)
    @Schema(description = "原始注册商")
    private String original_registrar;

    @ExcelProperty(value = "注册时间", converter = DateTimeConverter.class)
    @ColumnWidth(25)
    @Schema(description = "创建时间")
    private String created_on;

    @ExcelProperty(value = "修改时间", converter = DateTimeConverter.class)
    @ColumnWidth(25)
    @Schema(description = "修改时间")
    private String modified_on;

    @ExcelProperty(value = "激活时间", converter = DateTimeConverter.class)
    @ColumnWidth(25)
    @Schema(description = "激活时间")
    private String activated_on;

    @ExcelIgnore
    @Schema(description = "获取现有的 DNS 记录")
    private Boolean jumpStart;

    public Zone(String name, Boolean jumpStart, String type) {
        this.name = name;
        this.jumpStart = jumpStart != null ? jumpStart : true;
        this.type = (type != null && type.equals("partial")) ? "partial" : "full";
    }
}
