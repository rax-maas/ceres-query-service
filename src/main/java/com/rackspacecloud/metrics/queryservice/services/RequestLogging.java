package com.rackspacecloud.metrics.queryservice.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * An interceptor that can be registered by a {@link WebMvcConfigurer} config bean via
 * {@link WebMvcConfigurer#addInterceptors(InterceptorRegistry)}. It will log a summary of
 * a request and its response to allow for easily locating within logs along with any trace IDs.
 */
@Slf4j
public class RequestLogging extends HandlerInterceptorAdapter {

  @Override
  public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                              Object handler, Exception ex) throws Exception {
    log.info("Request completed: method={} path={}{} parameters={} status={}",
        request.getMethod(), request.getContextPath(), request.getServletPath(), request.getParameterMap(),
        response.getStatus());
  }

}
