package noob.toolbox.domain.dto;

import lombok.Data;

@Data
public class FlarePage {
    private Integer page;
    private Integer per_page;
    private Integer count;
    private Integer total_pages;
    private Integer total_count;
}
