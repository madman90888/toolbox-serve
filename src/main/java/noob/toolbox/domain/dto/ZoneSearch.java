package noob.toolbox.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "查询域名表单", description = "批量添加域名的对象格式<br>域名状态：<br>active, pending, initializing, moved, deleted, deactivated<br>活动、挂起、初始化、移动、删除、停用")
public class ZoneSearch {
    @Schema(description = "域名", example = "name.com")
    private String name;
    @Schema(description = "要搜索的域名状态", example = "active", allowableValues = {"active", "pending", "initializing", "moved", "deleted", "deactivated"})
    private String status;
    @Schema(description = "需要排序的列名", example = "name", allowableValues = {"id", "name", "status", "paused", "type", "created_on"} )
    private String order;
    @Schema(description = "降序还是升序", example = "desc", allowableValues = {"desc", "asc"})
    private String direction;
    @Schema(description = "当前页码", defaultValue = "1")
    private Integer page;
    @Schema(description = "每页大小", defaultValue = "20", maximum = "1000")
    private Integer limit;
}
