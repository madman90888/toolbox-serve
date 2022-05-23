package noob.toolbox.domain.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DomainRow {
    private String name;
    private boolean success;
    private String message;

    public static DomainRow success(String name) {
        return new DomainRow(name,true, "ok");
    }

    public static DomainRow fail(String name, String msg) {
        return new DomainRow(name,false, msg);
    }
}
