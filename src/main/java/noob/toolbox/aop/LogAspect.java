package noob.toolbox.aop;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import noob.toolbox.util.WebUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Collectors;

@Slf4j
@Aspect
@Component
public class LogAspect {
    @Autowired
    private ObjectMapper mapper;

    @Pointcut("execution(* noob.toolbox.controller.*.*(..))")
    public void pt() {}

    @Around("pt()")
    public Object log(ProceedingJoinPoint joinPoint) throws Throwable {
        final long beginTime = System.currentTimeMillis();
        try {
            final Object result = joinPoint.proceed(joinPoint.getArgs());
            recordLog(joinPoint, System.currentTimeMillis() - beginTime, null);
            return result;
        }catch (Exception e) {
            recordLog(joinPoint, System.currentTimeMillis() - beginTime, e);
            throw e;
        }
    }

    private void recordLog(ProceedingJoinPoint joinPoint, long time, Exception e) {
        final StringJoiner joiner = new StringJoiner("\n");
        final MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        joiner.add("控制器日志记录：");
        joiner.add("==================== log start ====================");
        joiner.add("class:" + joinPoint.getTarget().getClass().getName());
        joiner.add("method:" + signature.getName());
        try {
            joiner.add("params:" + mapper.writeValueAsString(joinPoint.getArgs()));
        } catch (JsonProcessingException ex) {
            joiner.add("params:" + Arrays.stream(joinPoint.getArgs()).map(item -> item == null ? "null" : item.getClass().getName()).collect(Collectors.toList()));
        }
        joiner.add("ip:" + WebUtils.getIpAddr());
        joiner.add("execute time : " + time + " ms");
        if (Objects.nonNull(e)) {
            joiner.add("Exception ==> " + e.getMessage());
        }
        joiner.add("==================== log end ====================\n");
        if (Objects.nonNull(e)) {
            log.error(joiner.toString());
        }else {
            log.debug(joiner.toString());
        }
    }
}
