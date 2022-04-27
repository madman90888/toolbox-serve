package noob.toolbox.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class ZoneExcel {
    @ExcelProperty
    @ExcelIgnore
    private String name;
    private String status;
    private String original_registrar;
    private String created_on;
    private String modified_on;
    private String activated_on;
}
