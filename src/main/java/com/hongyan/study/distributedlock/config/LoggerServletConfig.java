package com.hongyan.study.distributedlock.config;

        import com.hongyan.study.distributedlock.filter.LoggerServletFilter;
        import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
        import org.springframework.context.annotation.Bean;
        import org.springframework.context.annotation.Configuration;

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
@Configuration
public class LoggerServletConfig {
    @Bean
    @ConditionalOnMissingBean(
            name = {"loggerServletFilter"}
    )
    public LoggerServletFilter loggerServletFilter() {
        return new LoggerServletFilter();
    }
}
