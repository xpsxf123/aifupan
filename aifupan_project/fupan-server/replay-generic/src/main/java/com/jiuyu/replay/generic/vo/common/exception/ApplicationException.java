package com.jiuyu.replay.generic.vo.common.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 系统异常父类，其他业务异常必须继承此类
 *
 * @author RayChou
 * @date 2025/6/25 11:23
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApplicationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 业务异常状态码
     */
    private int code;

    /**
     * 异常说明
     */
    private String message;

    /**
     * 携带的数据
     */
    private Object data;

    public ApplicationException(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
