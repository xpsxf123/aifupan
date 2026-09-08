package com.jiuyu.replay.generic.vo.common.exception;

import com.jiuyu.replay.generic.vo.common.code.BaseStatusCode;
import com.jiuyu.replay.generic.vo.common.code.StatusCode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.ObjectUtils;

/**
 * 业务异常
 *
 * @author RayChou
 * @date 2025/6/25 11:23
 */
public class BusinessException extends ApplicationException {

    private static final long serialVersionUID = 1L;

    public BusinessException(BaseStatusCode baseExceptionCode) {
        super(baseExceptionCode.getCode(), baseExceptionCode.getMsg());
    }

    public BusinessException(int code, String message) {
        super(code, message);
    }


    public BusinessException(String message) {
        // 业务异常
        super(StatusCode.BASE_VALID_PARAM.getCode(), message);
    }

    public BusinessException(int code, String message, Object data) {
        super(code, message, data);
    }

    /**
     * 参数校验工具类
     *
     * <p><b>设计原则：</b>
     * <ol>
     *   <li>避免重复校验（ {@code ObjectUtils.isEmpty(value)} ）</li>
     *   <li>支持动态错误码（扩展性）</li>
     *   <li>默认消息国际化兼容（预留接口）</li>
     * </ol>
     *
     * @param value   待校验参数（支持对象、集合、Map、数组等）
     * @param message 自定义错误信息（可选）
     * @throws BusinessException 当参数为空时抛出，含标准化错误码
     */
    public static void requireNonEmpty(Object value, String message) {
        if (ObjectUtils.isEmpty(value)) {
            throw StringUtils.isBlank(message)
                    ? new BusinessException(StatusCode.ILLEGAL_ARGUMENT_EX)
                    : new BusinessException(StatusCode.ILLEGAL_ARGUMENT_EX.getCode(), message);
        }
    }

}
