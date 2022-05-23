package noob.toolbox.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import noob.toolbox.validated.Create;
import noob.toolbox.validated.Update;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;

@Schema(name = "域名邀请码实体", description = "域名邀请码详细信息")
@TableName(value ="domain")
@Data
public class Domain implements Serializable {
    /**
     * 域名表ID
     */
    @NotNull(message = "ID不能为空", groups = Update.class)
    @Schema(description = "域名ID")
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 域名
     */
    @NotBlank(message = "域名不能为空")
    @Schema(description = "域名")
    private String name;

    /**
     * 邀请码
     */
    @Schema(description = "邀请码")
    private String code;

    /**
     * 备注
     */
    @Schema(description = "备注")
    private String remark;

    /**
     * 添加时间
     */
    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 修改时间
     */
    @Schema(description = "修改时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /**
     * 小组
     */
    private Integer groupId;

    @Schema(description = "分组名")
    @TableField(exist = false)
    private String groupName;

    @Schema(description = "绑定的站点名")
    private String pageName;
}