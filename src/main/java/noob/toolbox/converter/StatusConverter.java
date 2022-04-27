package noob.toolbox.converter;

import com.alibaba.excel.converters.Converter;
import com.alibaba.excel.metadata.GlobalConfiguration;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.metadata.property.ExcelContentProperty;

public class StatusConverter implements Converter<String> {
    @Override
    public WriteCellData<?> convertToExcelData(String value, ExcelContentProperty contentProperty, GlobalConfiguration globalConfiguration) throws Exception {
        String val = value;
        switch (value) {
            case "active":
                val = "激活";
                break;
            case "pending":
                val = "挂起";
                break;
            case "initializing":
                val = "初始化";
                break;
            case "moved":
                val = "移动";
                break;
            case "deleted":
                val = "删除";
                break;
            case "deactivated":
                val = "停用";
                break;
        }
        return new WriteCellData<>(val);
    }
}
