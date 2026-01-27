package com.tencent.supersonic.advice;

import com.tencent.supersonic.common.pojo.ResultData;
import com.tencent.supersonic.common.pojo.enums.ReturnCode;
import com.tencent.supersonic.common.pojo.exception.AccessException;
import com.tencent.supersonic.common.pojo.exception.CommonException;
import com.tencent.supersonic.common.pojo.exception.InvalidArgumentException;
import com.tencent.supersonic.common.pojo.exception.InvalidPermissionException;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class RestExceptionHandler {

    /**
     * Handle client abort exceptions (connection reset, broken pipe). These are normal when clients
     * disconnect before response completes.
     */
    @ExceptionHandler(ClientAbortException.class)
    @ResponseStatus(HttpStatus.OK)
    public void clientAbortException(ClientAbortException e) {
        // Log at debug level only - this is normal client behavior
        if (log.isDebugEnabled()) {
            log.debug("Client disconnected: {}", e.getMessage());
        }
    }

    /** default global exception handler */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public ResultData<String> exception(Exception e) {
        // Check for client abort exceptions that may be wrapped
        if (isClientAbortException(e)) {
            if (log.isDebugEnabled()) {
                log.debug("Client disconnected: {}", e.getMessage());
            }
            return null;
        }
        log.error("default global exception", e);
        return ResultData.fail(ReturnCode.SYSTEM_ERROR.getCode(), e.getMessage());
    }

    /**
     * Check if the exception is caused by client disconnection.
     */
    private boolean isClientAbortException(Throwable e) {
        if (e == null) {
            return false;
        }
        if (e instanceof ClientAbortException) {
            return true;
        }
        String message = e.getMessage();
        if (message != null && (message.contains("Connection reset")
                || message.contains("Broken pipe") || message.contains("connection was aborted"))) {
            return true;
        }
        return isClientAbortException(e.getCause());
    }

    @ExceptionHandler(AccessException.class)
    @ResponseStatus(HttpStatus.OK)
    public ResultData<String> accessException(Exception e) {
        return ResultData.fail(ReturnCode.ACCESS_ERROR.getCode(), e.getMessage());
    }

    @ExceptionHandler(InvalidPermissionException.class)
    @ResponseStatus(HttpStatus.OK)
    public ResultData<String> invalidPermissionException(Exception e) {
        log.error("default global exception", e);
        return ResultData.fail(ReturnCode.INVALID_PERMISSION.getCode(), e.getMessage());
    }

    @ExceptionHandler(InvalidArgumentException.class)
    @ResponseStatus(HttpStatus.OK)
    public ResultData<String> invalidArgumentException(Exception e) {
        log.error("default global exception", e);
        return ResultData.fail(ReturnCode.INVALID_REQUEST.getCode(), e.getMessage());
    }

    @ExceptionHandler(CommonException.class)
    @ResponseStatus(HttpStatus.OK)
    public ResultData<String> commonException(CommonException e) {
        log.error("default global exception", e);
        return ResultData.fail(e.getCode(), e.getMessage());
    }
}
