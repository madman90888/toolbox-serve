package noob.toolbox.domain.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import noob.toolbox.validated.Create;
import noob.toolbox.validated.Update;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Schema(name = "小组", description = "小组信息")
@TableName(value ="groups")
@Data
public class Groups implements Serializable {
    /**
     * ID
     */
    @NotNull(message = "ID不能为空", groups = Update.class)
    @Schema(description = "主键")
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 小组名
     */
    @Schema(description = "小组名")
    @NotBlank(message = "分组名不能为空")
    private String name;

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
}