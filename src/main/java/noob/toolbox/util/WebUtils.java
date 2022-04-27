package noob.toolbox.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import noob.toolbox.domain.pojo.ResultData;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class WebUtils {
    public static final String CONTENT_TYPE = "application/json; charset=UTF-8";
    public static final ObjectMapper mapper = new ObjectMapper();

    public static void writeString(Integer code, String msg, HttpServletResponse response) {
        response.setContentType(CONTENT_TYPE);
//        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        try(PrintWriter out = response.getWriter()) {
            ResultData data = ResultData.error(code, msg);
            out.write(mapper.writeValueAsString(data));
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}