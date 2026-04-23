package com.mylife.config;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.filter.SimplePropertyPreFilter;
import com.mylife.common.BaseResult;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.lang.reflect.Method;
import java.util.Set;

/**
 * 全局 API 日志切面。
 * 用 @Around 同时打印请求参数和返回值，替代 Filter 方案。
 * 跳过 SseEmitter 返回类型，避免干扰流式推送。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ApiLogAspect {

    private static final Set<String> SENSITIVE_FIELDS = Set.of("password", "token", "accessToken");

    private static final SimplePropertyPreFilter SENSITIVE_FILTER = new SimplePropertyPreFilter();
    static {
        SENSITIVE_FIELDS.forEach(SENSITIVE_FILTER.getExcludes()::add);
    }

    private final HttpServletRequest request;

    @Around("execution(public * com.mylife.controller..*.*(..))")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 跳过 SseEmitter 返回类型
        Method signatureMethod = ((MethodSignature) joinPoint.getSignature()).getMethod();
        if (SseEmitter.class.isAssignableFrom(signatureMethod.getReturnType())) {
            return joinPoint.proceed();
        }

        String httpMethod = request.getMethod();
        String uri = request.getRequestURI();

        // 打印请求参数
        logRequest(httpMethod, uri, joinPoint.getArgs());

        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long cost = System.currentTimeMillis() - start;
            logResponse(httpMethod, uri, result, cost);
            return result;
        } catch (Throwable ex) {
            long cost = System.currentTimeMillis() - start;
            log.error("[Response] {} {} → EXCEPTION {}ms", httpMethod, uri, cost, ex);
            throw ex;
        }
    }

    private void logRequest(String method, String uri, Object[] args) {
        if (args == null || args.length == 0) {
            log.info("[Request] {} {}", method, uri);
            return;
        }
        String paramJson = JSON.toJSONString(args, SENSITIVE_FILTER);
        log.info("[Request] {} {} param: {}", method, uri, paramJson);
    }

    private void logResponse(String method, String uri, Object result, long cost) {
        if (result instanceof BaseResult<?> baseResult) {
            log.info("[Response] {} {} → {} {}ms", method, uri, baseResult.getCode(), cost);
            if (log.isDebugEnabled() && baseResult.getData() != null) {
                log.debug("[Response] {} {} data: {}", method, uri,
                        JSON.toJSONString(baseResult.getData(), SENSITIVE_FILTER));
            }
        } else {
            log.info("[Response] {} {} → {}ms", method, uri, cost);
        }
    }
}
