package com.example.itodo.ai;

import com.example.itodo.common.error.ErrorCode;
import com.example.itodo.common.error.BusinessException;

public class AiException extends BusinessException {
    
    public AiException(ErrorCode code, String message) {
        super(code, message);
    }
    
    public AiException(ErrorCode code, String message, Throwable cause) {
        super(code, message);
        initCause(cause);
    }
}
