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
            recordLog(joinPoint, System.currentTimeMillis() - beginTime);
            return result;
        }catch (Exception e) {
            recordErrorLog(joinPoint, System.currentTimeMillis() - beginTime, e);
            throw e;
        }
    }

    private void recordLog(ProceedingJoinPoint joinPoint, long time) throws JsonProcessingException {
        final MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        log.debug("==================== log start ====================");
        log.debug("class:{}", joinPoint.getTarget().getClass().getName());
        log.debug("method:{}", signature.getName());
        log.debug("params:{}", mapper.writeValueAsString(joinPoint.getArgs()));
        log.debug("ip:{}", WebUtils.getIpAddr());
        log.debug("execute time : {} ms", time);
        log.debug("==================== log end ====================");
    }

    private void recordErrorLog(ProceedingJoinPoint joinPoint, long time, Exception e) throws JsonProcessingException {
        final MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        log.error("==================== log start ====================");
        log.error("class:{}", joinPoint.getTarget().getClass().getName());
        log.error("method:{}", signature.getName());
        log.error("params:{}", mapper.writeValueAsString(joinPoint.getArgs()));
        log.error("ip:{}", WebUtils.getIpAddr());
        log.error("execute time : {} ms", time);
        if (!ObjectUtils.isEmpty(e)) {
            log.error("Exception ==> {}", e.getMessage());
        }
        log.error("==================== log end ====================");
    }
}
