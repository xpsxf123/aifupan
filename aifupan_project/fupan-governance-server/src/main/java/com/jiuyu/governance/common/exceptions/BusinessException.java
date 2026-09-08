package com.jiuyu.governance.common.exceptions;

import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.governance.common.pojo.ErrorCode;
import com.jiuyu.governance.common.pojo.BizErrorCode;
import lombok.Getter;

import java.io.Serial;

/**
 * 业务异常
 *
 * @author HeHui
 * @date 2026-03-17 14:26
 */
@Getter
public class BusinessException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = -967355532965537207L;

    private final ErrorCode errorCode;


    public BusinessException(ErrorCode errorCode) {
        this(errorCode, "");
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(EmptyUtil.isEmpty(message) ? errorCode.getMessage() : message);
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(EmptyUtil.isEmpty(message) ? errorCode.getMessage() : message, cause);
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, Throwable cause) {
        this(errorCode, null, cause);
    }

    public BusinessException(String message) {
        super(message);
        this.errorCode = com.jiuyu.governance.common.pojo.BizErrorCode.GENERAL_FAILED;
    }


}
