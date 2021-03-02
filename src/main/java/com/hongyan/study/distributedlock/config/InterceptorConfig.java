//package com.hongyan.study.distributedlock.config;
//
//import com.hongyan.study.distributedlock.filter.TraceInterceptor;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.servlet.HandlerInterceptor;
//import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
//import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
//import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
//
///**
// * @author zy
// * @version 1.0
// * @date Created in 2021-02-22 21:25
// * @description InterceptorConfig
// *
// * 2种实现MDC全链路调用跟踪实现方式：（就是一个请求，尽管经过多次逻辑操作，但是其traceId是唯一且相同的，只有不同请求的traceId才会不同）
// * InterceptorConfig配合TraceInterceptor实现MDC打印traceId来个全链路调用跟踪
// * 或者使用
// * LoggerServletConfig配合LoggerServletFilter实现MDC打印traceId来个全链路调用跟踪
// * 但是这2种MDC打印traceId的全链路调用跟踪是有一定缺陷的，例如：
// * 1、子线程中打印日志丢失traceId
// * 2、HTTP调用丢失traceId
// *
// *
// */
//@Configuration
//public class InterceptorConfig extends WebMvcConfigurerAdapter {
//
//    @Bean
//    public HandlerInterceptor getMyInterceptor() {
//        return new TraceInterceptor();
//    }
//
//    @Override
//    public void addInterceptors(InterceptorRegistry registry) {
//        InterceptorRegistration interceptor = registry.addInterceptor(getMyInterceptor());
//        // 拦截所有、排除
//        interceptor.addPathPatterns("/**")
//                .excludePathPatterns("/aa/bb");
//    }
//}