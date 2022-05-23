package noob.toolbox.domain.pojo;

import lombok.Data;

import java.util.List;

@Data
public class Denied {
    private Boolean use;
    private List<String> filter;
    private Integer type;
}
