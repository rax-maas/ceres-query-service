package com.rackspacecloud.metrics.queryservice.controllers;

import com.rackspacecloud.metrics.queryservice.exceptions.ErroredQueryResultException;
import com.rackspacecloud.metrics.queryservice.exceptions.InvalidQueryException;
import com.rackspacecloud.metrics.queryservice.exceptions.RouteNotFoundException;
import com.rackspacecloud.metrics.queryservice.models.ErrorInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.naming.ConfigurationException;

@ControllerAdvice
public class GlobalExceptionHandler {
    private static Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handle RouteNotFoundException
     * @param e
     * @return
     */
    @ExceptionHandler(RouteNotFoundException.class)
    public ResponseEntity<ErrorInfo> handle(final RouteNotFoundException e) {
        LOGGER.error(e.getMessage(), e);
        return new ResponseEntity<>(
                new ErrorInfo(e.getMessage(), getRootCause(e).getMessage()),
                HttpStatus.NOT_FOUND
        );
    }

    /**
     * Handle IllegalArgumentException, MethodArgumentNotValidException, ConfigurationException,
     * InvalidQueryException, and ErroredQueryResultException
     * @param e
     * @return
     */
    @ExceptionHandler({
            IllegalArgumentException.class,
            MethodArgumentNotValidException.class,
            ConfigurationException.class,
            InvalidQueryException.class,
            ErroredQueryResultException.class
    })
    public ResponseEntity<ErrorInfo> handleBadRequest(Exception e) {
        LOGGER.error(e.getMessage(), e);
        return new ResponseEntity<>(
                new ErrorInfo(e.getMessage(), getRootCause(e).getMessage()),
                HttpStatus.BAD_REQUEST
        );
    }

    /**
     * Handle any Exception
     * @param e
     * @return
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorInfo> handle(final Exception e) {
        LOGGER.error(e.getMessage(), e);
        return new ResponseEntity<>(
                new ErrorInfo(e.getMessage(), getRootCause(e).getMessage()),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    /**
     * Dig root cause in the exception
     * @param e
     * @return
     */
    private Throwable getRootCause(Throwable e) {
        if(e == null) return e;

        Throwable cause = e.getCause();
        if(cause == null) {
            return e;
        }
        else {
            return getRootCause(cause);
        }
    }
}
