package noob.toolbox.handler;

import lombok.extern.slf4j.Slf4j;
import noob.toolbox.domain.pojo.BusinessCode;
import noob.toolbox.domain.pojo.ResultData;
import noob.toolbox.exception.CustomerException;
import noob.toolbox.service.impl.CloudFlareService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @Autowired
    private CloudFlareService service;

    @ExceptionHandler(CustomerException.class)
    public ResultData exception(CustomerException e) {
        log.debug("自定义异常：", e);
        return ResultData.error(e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResultData exception(AccessDeniedException e) {
        log.debug("权限不足：", e);
        service.deleteToken();
        return ResultData.error(BusinessCode.NOT_AUTH_USER.getCode(), BusinessCode.NOT_AUTH_USER.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResultData exception(AuthenticationException e) {
        log.debug("校验失败：", e);
        return ResultData.error(BusinessCode.FAILED.getCode(), e.getMessage());
    }

    // Form、JSON 校验失败抛出的异常
    @ExceptionHandler(BindException.class)
    public ResultData exception(BindException e) {
        log.debug("Form、JSON参数错误：", e);
        List<FieldError> errors = e.getBindingResult().getFieldErrors();
        final ResultData resultData = ResultData.error(errors.get(0).getDefaultMessage());
        List<String> collect = errors.stream()
                .map(o -> o.getDefaultMessage())
                .collect(Collectors.toList());
        resultData.setData(collect);
        return resultData;
    }

    // 处理单个参数校验失败抛出的异常
    @ExceptionHandler(ConstraintViolationException.class)
    public ResultData exception(ConstraintViolationException e) {
        log.debug("单参数错误：", e);
        Set<ConstraintViolation<?>> errors = e.getConstraintViolations();
        final StringJoiner joiner = new StringJoiner(" ");
        errors.stream()
                .forEach(o -> joiner.add(o.getMessageTemplate()));
        return ResultData.error(joiner.toString());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResultData exception(RuntimeException e) {
        log.debug("手动抛出运行时异常：", e);
        return ResultData.error("请求出现异常");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResultData exception(HttpRequestMethodNotSupportedException e) {
        log.debug("不支持的请求：", e);
        return ResultData.error(e.getMessage());
    }

    /**
     * 拦截所有异常，并设置响应状态码
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResultData exception(Exception e) {
        log.error("异常 => {}", e.getMessage(), e);
        return ResultData.error("服务器异常，请稍后重试");
    }

}