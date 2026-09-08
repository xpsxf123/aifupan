package com.jiuyu.governance.plugins.webmvc;

import com.jiuyu.governance.common.pojo.BizErrorCode;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import com.jiuyu.framework.lock.TryLockException;
import com.jiuyu.framework.oauth.AccessUser;
import com.jiuyu.framework.oauth.AuthenticationConstant;
import com.jiuyu.framework.oauth.exceptions.AuthenticationException;
import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.governance.common.exceptions.BusinessException;
import com.jiuyu.governance.common.pojo.SystemErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.exceptions.PersistenceException;
import org.mybatis.spring.MyBatisSystemException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MultipartException;

import java.nio.charset.StandardCharsets;
import java.security.SignatureException;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * 全局异常处理程序
 *
 * @author HeHui
 * @date 2019/8/3 17:43
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private String getContext(boolean loadParam) {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes) {
            HttpServletRequest httpServletRequest = ((ServletRequestAttributes) requestAttributes).getRequest();
            Object attribute = httpServletRequest.getAttribute(AuthenticationConstant.ACCESS_USER_ATTRIBUTE_NAME);
            StringBuilder sb = new StringBuilder();
            if (attribute instanceof AccessUser accessUser) {
                sb.append("[").append(accessUser.userId()).append("@").append(accessUser.username()).append("]");
            }
            sb.append(httpServletRequest.getRequestURI());
            if (loadParam) {
                String params = httpServletRequest.getQueryString();
                if (StrUtil.isNotBlank(params)) {
                    sb.append("?").append(params);
                }
                try {
                    String contentType = httpServletRequest.getHeader(HttpHeaders.CONTENT_TYPE);
                    MediaType mediaType = (StringUtils.hasLength(contentType) ? MediaType.parseMediaType(contentType) : null);
                    String body = null;

                    // 仅当请求体为JSON类型时，读取请求体内容
                    if (mediaType != null && mediaType.includes(MediaType.APPLICATION_JSON)) {
                        body = IoUtil.read(httpServletRequest.getInputStream(), StandardCharsets.UTF_8).replaceAll("\\s+", "");
                    }
                    if (StrUtil.isNotBlank(body)) {
                        sb.append("   ").append(body);
                    }
                } catch (Throwable ignored) {
                }
            }
            return sb.toString();
        }
        return "";
    }


    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> businessException(BusinessException e) {
        log.error("[业务异常] client visit resource error {} [{}]", e.getMessage(), getContext(true), e);
        return ApiResponse.failed(e.getErrorCode().getCode(), e.getMessage());
    }


    @ExceptionHandler(TryLockException.class)
    public ApiResponse<Void> tryLockException(TryLockException e) {
        log.error("[锁异常] client visit resource  [{}]", getContext(false), e);
        return ApiResponse.failed(SystemErrorCode.UNPROCESSABLE_ENTITY.getCode(), e.getMessage());
    }




    /**
     * 非登录异常
     *
     * @param e e
     *
     * @return {@link ApiResponse }<{@link Void }>
     */
    @ExceptionHandler(AuthenticationException.class)
    public ApiResponse<Void> notLoginException(AuthenticationException e) {
        log.error("[访问权限] client visit resource not auth {} [{}]", e.getMessage(), getContext(false));
        return ApiResponse.failed(BizErrorCode.HTTP_UNAUTHORIZED.getCode(), "登陆已过期,请重新登陆!");
    }


    /**
     * http消息不可读例外
     *
     * @param ex 前女友
     *
     * @return {@link ApiResponse}<{@link Void}>
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResponse<Void> httpMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.error("[HTTP Request] client body request error [{}]", getContext(true), ex);
        String message = ex.getMessage();
        if (StrUtil.containsAny(message, "Could not read document:")) {
            String msg = String.format("无法正确的解析json类型的参数：%s", StrUtil.subBetween(message, "Could not read document:", " at "));
            return ApiResponse.failed(msg);
        } else if (StrUtil.containsAny(message, "Required request body is missing:")) {
            return ApiResponse.failed(BizErrorCode.PARAM_INVALID.getCode(), "JSON格式非法");
        } else if (StrUtil.containsAny(message, "JSON parse error:")) {
            String[] split = message.split("\"");
            if (split.length == 3) {
                return ApiResponse.failed("请求参数格式错误, Param: " + split[1]);
            }
        }
        return ApiResponse.failed(BizErrorCode.GENERAL_FAILED.getCode(), "数据格式异常");
    }

    /**
     * 缺少servlet请求参数异常
     *
     * @param missingServletRequestParameterException 缺少servlet请求参数异常
     *
     * @return {@link ApiResponse }<{@link Void }>
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ApiResponse<Void> missingServletRequestParameterException(MissingServletRequestParameterException missingServletRequestParameterException) {
        log.error("[HTTP Request] missing request parameter {}", getContext(true), missingServletRequestParameterException);
        return ApiResponse.failed(SystemErrorCode.REQUIRED.getCode(), missingServletRequestParameterException.getMessage());
    }

    /**
     * 不支持http请求方法异常
     *
     * @param e e
     *
     * @return {@link ApiResponse }<{@link Void }>
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ApiResponse<Void> httpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.error("[HTTP Request] client request method error [{}]", getContext(true), e);
        return ApiResponse.failed(SystemErrorCode.NOT_SUPPORTED.getCode(), e.getMessage());
    }

    /**
     * 绑定异常
     *
     * @param ex 前女友
     *
     * @return {@link ApiResponse}<{@link Void}>
     */
    @ExceptionHandler(BindException.class)
    public ApiResponse<Void> bindException(BindException ex) {
        log.error("[HTTP Request] client request param bind error [{}]", getContext(true), ex);
        try {
            String msg = Objects.requireNonNull(ex.getBindingResult().getFieldError()).getDefaultMessage();
            if (StrUtil.isNotEmpty(msg)) {
                return ApiResponse.failed(BizErrorCode.PARAM_INVALID.getCode(), "参数格式错误");
            }
        } catch (Exception ee) {
            log.debug("获取异常描述失败", ee);
        }
        StringBuilder msg = new StringBuilder();
        List<FieldError> fieldErrors = ex.getFieldErrors();
        fieldErrors.forEach((oe) ->
            msg.append("参数:[").append(oe.getObjectName())
                .append(".").append(oe.getField())
                .append("]的传入值:[").append(oe.getRejectedValue()).append("]与预期的字段类型不匹配.")
        );
        return ApiResponse.failed(BizErrorCode.PARAM_INVALID.getCode(), msg.toString());
    }


    /**
     * 方法参数类型不匹配例外
     *
     * @param ex 前女友
     *
     * @return {@link ApiResponse}<{@link Void}>
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ApiResponse<Void> methodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        log.error("[HTTP Request] client request param type error [{}]", getContext(true), ex);
        String msg = "参数：[" + ex.getName() + "]的传入值：[" + ex.getValue() +
            "]与预期的字段类型：[" + Objects.requireNonNull(ex.getRequiredType()).getName() + "]不匹配";
        return ApiResponse.failed(BizErrorCode.PARAM_INVALID.getCode(), msg);
    }

    /**
     * 非法状态异常
     *
     * @param ex 前女友
     *
     * @return {@link ApiResponse}<{@link Void}>
     */
    @ExceptionHandler(IllegalStateException.class)
    public ApiResponse<Void> illegalStateException(IllegalStateException ex) {
        log.error("[HTTP Request] client request state illegal error [{}]", getContext(true), ex);
        return ApiResponse.failed(BizErrorCode.GENERAL_FAILED.getCode(), "系统出小差啦");
    }


    /**
     * 空指针异常
     *
     * @param ex 前女友
     *
     * @return {@link ApiResponse}<{@link Void}>
     */
    @ExceptionHandler(NullPointerException.class)
    public ApiResponse<Void> nullPointerException(NullPointerException ex) {
        log.error("[业务异常] system code litter resultIn error [{}]", getContext(true), ex);
        return ApiResponse.failed(BizErrorCode.GENERAL_FAILED.getCode(), "系统出小差啦");
    }


    /**
     * 文件上传例外
     *
     * @param ex 前女友
     *
     * @return {@link ApiResponse}<{@link Void}>
     */
    @ExceptionHandler(MultipartException.class)
    public ApiResponse<Void> multipartException(MultipartException ex) {
        log.error("[HTTP Request] client file request error [{}]", getContext(false), ex);
        return ApiResponse.failed(BizErrorCode.PARAM_INVALID.getCode(), "文件上传错误");
    }

    /**
     * jsr 规范中的验证异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResponse<Void> constraintViolationException(ConstraintViolationException ex) {
        log.error("[系统异常] system code constraint violation [{}]", getContext(true), ex);
        Set<ConstraintViolation<?>> violations = ex.getConstraintViolations();
        String message = violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.joining(";"));
        return ApiResponse.failed(BizErrorCode.PARAM_INVALID.getCode(), message);
    }

    /**
     * 非法参数异常
     *
     * @param ex 异常
     *
     * @return {@link ApiResponse}<{@link Void}>
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Void> illegalArgumentException(IllegalArgumentException ex) {
        log.error("[系统异常] 非法参数异常 [{}]", getContext(true), ex);
        return ApiResponse.failed(BizErrorCode.PARAM_INVALID.getCode(), ex.getMessage());
    }

    /**
     * spring 封装的参数验证异常， 在controller中没有写result参数时，会进入
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> methodArgumentNotValidException(MethodArgumentNotValidException ex) {
        log.error("[HTTP Request] client request param valid error [{}]", getContext(true), ex);
        return ApiResponse.failed(BizErrorCode.PARAM_INVALID.getCode(), Objects.requireNonNull(ex.getBindingResult().getFieldError()).getDefaultMessage());
    }


    /**
     * 持久性例外
     *
     * @param ex 前女友
     *
     * @return {@link ApiResponse}<{@link Void}>
     */
    @ExceptionHandler(PersistenceException.class)
    public ApiResponse<Object> persistenceException(PersistenceException ex) {
        log.error("[SQL异常] mybatis row persistence error [{}]", getContext(true), ex);
        return ApiResponse.failed(SystemErrorCode.FAIL.getCode(), "系统出小差啦");
    }

    /**
     * mybatis系统异常
     *
     * @param ex 前女友
     *
     * @return {@link ApiResponse}<{@link Void}>
     */
    @ExceptionHandler(MyBatisSystemException.class)
    public ApiResponse<Object> myBatisSystemException(MyBatisSystemException ex) {
        if (ex.getCause() instanceof PersistenceException) {
            return this.persistenceException((PersistenceException) ex.getCause());
        }
        log.error("[SQL异常] mybatis system error [{}]", getContext(true), ex);
        return ApiResponse.failed(BizErrorCode.GENERAL_FAILED.getCode(), "系统出小差啦");
    }

    /**
     * sql异常
     *
     * @param ex 前女友
     *
     * @return {@link ApiResponse}<{@link Void}>
     */
    @ExceptionHandler(SQLException.class)
    public ApiResponse<Void> sqlException(SQLException ex) {
        log.error("[SQL异常] mybatis sql analysis error [{}]", getContext(true), ex);
        return ApiResponse.failed(BizErrorCode.GENERAL_FAILED.getCode(), "系统出小差啦");
    }

    /**
     * 违反数据完整性异常
     *
     * @param ex 前女友
     *
     * @return {@link ApiResponse}<{@link Void}>
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ApiResponse<Void> dataIntegrityViolationException(DataIntegrityViolationException ex) {
        log.error("[SQL异常] mybatis row data integrity violation error [{}]", getContext(true), ex);
        return ApiResponse.failed(BizErrorCode.GENERAL_FAILED.getCode(), "系统出小差啦");
    }


    /**
     * 签名异常
     *
     * @param signatureException 签名异常
     *
     * @return {@link ApiResponse}<{@link Void}>
     */
    @ExceptionHandler(SignatureException.class)
    public ApiResponse<Void> signatureException(SignatureException signatureException) {
        log.error("[HTTP Request] request sign error, {} [{}]", signatureException.getMessage(), getContext(true), signatureException.getCause());
        return ApiResponse.failed(SystemErrorCode.FORBIDDEN.getCode(), signatureException.getMessage());
    }

    /**
     * 默认异常处理
     *
     * @param e 前女友
     *
     * @return {@link ApiResponse}<{@link Void}>
     */
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception e) {
        log.error("[系统异常] system error [{}]", getContext(true), e);
        return ApiResponse.failed(SystemErrorCode.FAIL.getCode(), "系统出小差啦");
    }

}
