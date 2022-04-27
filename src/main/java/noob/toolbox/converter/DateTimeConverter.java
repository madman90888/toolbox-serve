package noob.toolbox.converter;

import com.alibaba.excel.converters.Converter;
import com.alibaba.excel.metadata.GlobalConfiguration;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.metadata.property.ExcelContentProperty;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class DateTimeConverter implements Converter<String> {
    private static final String DEFAULT_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern(DEFAULT_FORMAT);

    @Override
    public WriteCellData<?> convertToExcelData(String value, ExcelContentProperty contentProperty, GlobalConfiguration globalConfiguration) throws Exception {
        final ZonedDateTime parse = ZonedDateTime.parse(value);
        final ZoneId zoneId = ZoneId.of("UTC+8");
        return new WriteCellData<>(dtf.format( parse.withZoneSameInstant(zoneId) ));
    }
}
