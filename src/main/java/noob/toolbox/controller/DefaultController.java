package noob.toolbox.controller;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import noob.toolbox.domain.pojo.ResultData;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@RestController
public class DefaultController implements ErrorController {
    @RequestMapping("/error")
    public ResultData error(HttpServletRequest request) {
        Integer statusCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
        log.debug("错误资源异常 => ", statusCode, new NoHandlerFoundException(
                request.getMethod(),request.getRequestURI(),new HttpHeaders()));
        return ResultData.error(404, request.getMethod());
    }
}