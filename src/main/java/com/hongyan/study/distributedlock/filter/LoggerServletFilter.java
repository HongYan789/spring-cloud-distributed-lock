package com.hongyan.study.distributedlock.filter;

import com.hongyan.study.distributedlock.constant.Constants;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.UUID;

/**
 * @author zy
 * @version 1.0
 * @date Created in 2021-02-22 21:25
 * @description InterceptorConfig
 *
 * 2种实现MDC全链路调用跟踪实现方式：（就是一个请求，尽管经过多次逻辑操作，但是其traceId是唯一且相同的，只有不同请求的traceId才会不同）
 * InterceptorConfig配合TraceInterceptor实现MDC打印traceId来个全链路调用跟踪
 * 或者使用
 * LoggerServletConfig配合LoggerServletFilter实现MDC打印traceId来个全链路调用跟踪
 * 但是这2种MDC打印traceId的全链路调用跟踪是有一定缺陷的，例如：
 * 1、子线程中打印日志丢失traceId
 * 2、HTTP调用丢失traceId
 *
 *
 */
@Slf4j
public class LoggerServletFilter implements Filter {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        //
        String traceid = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        if (servletRequest instanceof HttpServletRequest) {
            String header = ((HttpServletRequest) servletRequest).getHeader(Constants.LOG_TRACE_ID);
            if (!StringUtils.isEmpty(header)) {
                traceid = header;
            }
        }
        MDC.put(Constants.LOG_TRACE_ID, traceid);
        try {
            filterChain.doFilter(servletRequest, servletResponse);
        } catch (Exception e) {
            log.error("过滤异常！", e);
            throw e;
        } finally {
            MDC.remove(Constants.LOG_TRACE_ID);
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {


    }

    @Override
    public void destroy() {

    }
}
