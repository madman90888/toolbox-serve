package noob.toolbox.controller;

import io.swagger.v3.oas.annotations.Hidden;
import noob.toolbox.domain.pojo.ResultData;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@Hidden
@RestController
public class DefaultController implements ErrorController {
    @RequestMapping("/error")
    public ResultData error(HttpServletRequest request) {
//        Integer statusCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
//        throw new NoHandlerFoundException(request.getMethod(),request.getRequestURI(),new HttpHeaders());
        return ResultData.error(404, request.getMethod());
    }
}