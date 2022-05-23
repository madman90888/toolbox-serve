package noob.toolbox.domain.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class DomainDto implements Serializable {

    @Schema(description = "域名")
    private String name;

    @Schema(description = "邀请码")
    private String code;

    @Schema(description = "分组名")
    private String groupName;

    @Schema(description = "绑定的站点名")
    private String pageName;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "修改时间")
    private LocalDateTime updateTime;

    @Schema(description = "页码")
    private Integer current;

    @Schema(description = "每页限制大小")
    private Integer limit;

    @Schema(description = "排序列名")
    private String sort;

    @Schema(description = "排序方向")
    private String direction;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
