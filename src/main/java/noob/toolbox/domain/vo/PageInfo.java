package noob.toolbox.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "分页数据", description = "封装好的分页数据，包含数据和分页信息")
public class PageInfo<T> {
    @Schema(description = "查询结果数据")
    private List<T> record;
    @Schema(description = "当前页码")
    private int page;
    @Schema(description = "每页大小")
    private int limit;
    @Schema(description = "总条数")
    private int total;
    @Schema(description = "总共页码")
    private int pages;
}
