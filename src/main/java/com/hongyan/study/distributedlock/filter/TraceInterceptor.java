//package com.hongyan.study.distributedlock.filter;
//
//
//import com.hongyan.study.distributedlock.constant.Constants;
//import org.slf4j.MDC;
//import org.springframework.stereotype.Component;
//import org.springframework.web.servlet.HandlerInterceptor;
//import org.springframework.web.servlet.ModelAndView;
//
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//import java.util.UUID;
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
//@Component
//public class TraceInterceptor implements HandlerInterceptor {
//
//    @Override
//    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
//        String traceid = UUID.randomUUID().toString().replace("-", "").toUpperCase();
//        //如果有上层调用就用上层的ID
//        String traceId = request.getHeader(Constants.LOG_TRACE_ID);
//        if (traceId == null) {
//            traceId = traceid;
//        }
//
//        MDC.put(Constants.LOG_TRACE_ID, traceId);
//        return true;
//    }
//
//    @Override
//    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView)
//            throws Exception {
//    }
//
//    @Override
//    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
//            throws Exception {
//        //调用结束后删除
//        MDC.remove(Constants.LOG_TRACE_ID);
//    }
//}
//
