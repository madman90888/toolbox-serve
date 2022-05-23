package noob.toolbox.domain.vo;

import lombok.Data;
import noob.toolbox.domain.entity.Groups;

@Data
public class GroupsVo extends Groups {
    private Integer domainCount;
    private Integer invitationCount;
}
