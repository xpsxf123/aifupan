package com.jiuyu.replay.api.config;

import cn.hutool.core.collection.CollectionUtil;
import com.jiuyu.replay.api.utils.GetIPUtils;
import com.jiuyu.replay.api.utils.ProfileUtil;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.common.code.StatusCode;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.power.utils.GlobalObject;
import com.jiuyu.replay.power.vo.UserCacheVo;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.apm.toolkit.trace.ActiveSpan;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 全局异常处理
 *
 * @author RayChou
 * @date 2025/6/26 10:35
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @Resource
    private ProfileUtil profileUtil;

    private void before(Throwable throwable, HttpServletRequest request) {
        try {
            ActiveSpan.error(throwable);
            ActiveSpan.tag("clientIp", GetIPUtils.getIpAddr(request));
            UserCacheVo localUser = GlobalObject.getLocalUser();
            if (localUser != null) {
                ActiveSpan.tag("userId", localUser.getId().toString());
                ActiveSpan.tag("tenantId", localUser.getActiveTenantId() == null ? "" : localUser.getActiveTenantId().toString());
            }
        } catch (Exception ignored) {
        }
    }

    // 自定义异常
    @ExceptionHandler(RRException.class)
    public ResponseEntity<R<Object>> handleCustomException(RRException ex, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        String stackInfo = getCoreStackTraceInfo(ex);
        log.warn("[业务异常] BusinessException: URI=[{}], 异常码=[{}], 异常信息=[{}], 关键位置={}", requestURI, ex.getCode(), ex.getMessage(), stackInfo);
        return new ResponseEntity<>(R.error(ex.getCode(), ex.getMessage()), HttpStatus.OK);
    }

    // 系统未知异常
    @ExceptionHandler(Exception.class)
    public Object handleException(Exception ex, HttpServletRequest request, HandlerMethod handlerMethod) {
        this.before(ex, request);
        // 获取请求信息
        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        String ip = GetIPUtils.getIpAddr(request);
        // 检查是否是SSE端点
        boolean isSseEndpoint = isSseEndpoint(requestURI, handlerMethod);
        // SSE端点的异常处理
        if (isSseEndpoint) {
            log.error("[SSE异常] 流处理异常: URI=[{}], 方法=[{}], IP=[{}], 异常类型=[{}], 异常信息=[{}]", requestURI, method, ip, ex.getClass().getName(), ex.getMessage(), ex);
            // 为SSE端点返回适当的错误响应
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).contentType(MediaType.TEXT_EVENT_STREAM).body("data: {\"error\":\"" + (profileUtil.isProd() ? "SSE异常！！！" : ex.getMessage()) + "\"}\n\n");
        }
        // 构建详细的日志信息
        log.error("[系统异常] 系统未知异常汇总: URI=[{}], 方法=[{}], IP=[{}], 异常类型=[{}], 异常信息=[{}]", requestURI, method, ip, ex.getClass().getName(), ex.getMessage(), ex);

        // 检查请求的Accept头
        String acceptHeader = request.getHeader("Accept");
        boolean isBinaryRequest = acceptHeader != null &&
                (acceptHeader.contains("application/octet-stream") ||
                        acceptHeader.contains("application/pdf") ||
                        acceptHeader.contains("image/") ||
                        acceptHeader.contains("audio/") ||
                        acceptHeader.contains("video/"));
        // 二进制数据响应
        if (isBinaryRequest) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(new byte[0]); // 返回空的二进制数组
        }
        // 普通JSON数据响应
        String message = profileUtil.isProd() ? "当前网络不佳，请稍后重试或联系管理员。" : ex.getMessage();
        return new ResponseEntity<>(R.error(StatusCode.INTERNAL_SERVER_ERROR.getCode(), message), HttpStatus.OK);
    }

    // 业务异常
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<R<Object>> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        String stackInfo = getCoreStackTraceInfo(ex);
        log.warn("[业务异常] BusinessException自定义异常: URI=[{}], 异常码=[{}], 异常信息=[{}], 关键位置={}", requestURI, ex.getCode(), ex.getMessage(), stackInfo);
        return new ResponseEntity<>(R.error(ex.getCode(), ex.getMessage()), HttpStatus.OK);
    }

    // 请求资源错误异常
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<R<Object>> handleResourceNotFoundException(NoResourceFoundException ex, HttpServletRequest request) {
        this.before(ex, request);
        log.info("[请求路径异常] 资源不存在: {}", ex.getMessage());
        return new ResponseEntity<>(R.error(StatusCode.BASE_VALID_PARAM.getCode(), "资源不存在"), HttpStatus.OK);
    }

    // IO异常，包括客户端连接中断和SSE处理
    @ExceptionHandler(IOException.class)
    public Object handleIOException(IOException ex, HttpServletRequest request, HandlerMethod handlerMethod) {
        this.before(ex, request);
        String message = ex.getMessage();
        String requestURI = request.getRequestURI();
        // 检查是否是客户端中断连接的异常
        boolean isConnectionReset = message != null && (message.equals("Broken pipe") || message.contains("Connection reset"));
        if (isConnectionReset) {
            // 检查是否是SSE端点
            boolean isSseEndpoint = isSseEndpoint(requestURI, handlerMethod);
            if (isSseEndpoint) {
                // SSE流式调用的客户端中断，只记录日志，不需要返回响应
                log.warn("[SSE连接异常] 流式调用客户端中断: URI=[{}], 异常信息=[{}]", requestURI, message);
                // 返回空响应，避免内容类型转换问题
                return ResponseEntity.ok().build();
            }
            // 普通HTTP请求的连接中断
            log.warn("[连接异常] 客户端连接中断: URI=[{}], 异常信息=[{}]", requestURI, message);
            return new ResponseEntity<>(R.error(StatusCode.BASE_VALID_PARAM.getCode(), "客户端连接中断"), HttpStatus.OK);
        }
        // 其他IO异常仍按原逻辑处理
        return handleException(ex, request, handlerMethod);
    }

    // AsyncRequestNotUsableException异常客户端连接中断
    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public Object handleIOException(AsyncRequestNotUsableException ex, HttpServletRequest request, HandlerMethod handlerMethod) {
        this.before(ex, request);
        String message = ex.getMessage();
        String requestURI = request.getRequestURI();
        // 检查是否是客户端中断连接的异常
        boolean isConnectionReset = message != null && (message.contains("Broken pipe") || message.contains("Connection reset"));
        if (isConnectionReset) {
            // 普通HTTP请求的连接中断
            log.warn("[连接异常] 客户端连接中断AsyncRequestNotUsableException: URI=[{}], 异常信息=[{}]", requestURI, message);
            return new ResponseEntity<>(R.error(StatusCode.BASE_VALID_PARAM.getCode(), "客户端连接中断"), HttpStatus.OK);
        }
        // 其他IO异常仍按原逻辑处理
        return handleException(ex, request, handlerMethod);
    }

    // 参数校验异常
    @ExceptionHandler({
            // Bean校验异常
            MethodArgumentNotValidException.class,
            // 绑定异常
            BindException.class,
            // 基础校验异常
            ValidationException.class,
            // 约束违反异常
            ConstraintViolationException.class,
            HandlerMethodValidationException.class,
            // 缺少请求参数异常
            MissingServletRequestParameterException.class,
            // 参数类型不匹配异常
            MethodArgumentTypeMismatchException.class, TypeMismatchException.class,
            // 非法参数异常
            IllegalArgumentException.class,
            // 请求体不可读异常
            HttpMessageNotReadableException.class,
            // 缺少路径变量异常
            MissingPathVariableException.class,
            // 请求绑定异常
            ServletRequestBindingException.class,
            // 缺少请求部分异常
            MissingServletRequestPartException.class,
            // 不支持的HTTP方法异常
            HttpRequestMethodNotSupportedException.class,
            // 不支持的媒体类型异常
            HttpMediaTypeNotSupportedException.class,
            // 文件上传大小超限异常
            MaxUploadSizeExceededException.class})
    public ResponseEntity<R<Object>> handleValidException(Exception ex, HttpServletRequest request) {
        this.before(ex, request);
        String requestURI = request.getRequestURI();
        String errorMsg;
        String prodErrorMsg;
        String stackInfo = getCoreStackTraceInfo(ex);

        // 根据不同异常类型构建错误消息
        if (ex instanceof MethodArgumentNotValidException) {
            // 处理@Valid注解校验不通过产生的异常
            MethodArgumentNotValidException e = (MethodArgumentNotValidException) ex;
            List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();
            errorMsg = buildFieldErrorMessage(fieldErrors);
            log.warn("[参数异常] 方法参数校验失败: URI=[{}], 错误字段=[{}], 错误信息=[{}], 关键位置={}",
                    requestURI, getFieldNames(fieldErrors), errorMsg, stackInfo);
            prodErrorMsg = buildProdFieldErrorMessage(fieldErrors);
        } else if (ex instanceof BindException) {
            // 处理表单绑定异常
            BindException e = (BindException) ex;
            List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();
            errorMsg = buildFieldErrorMessage(fieldErrors);
            log.warn("[参数异常] 表单绑定失败: URI=[{}], 错误字段=[{}], 错误信息=[{}], 关键位置={}",
                    requestURI, getFieldNames(fieldErrors), errorMsg, stackInfo);
            prodErrorMsg = buildProdFieldErrorMessage(fieldErrors);
        } else if (ex instanceof ConstraintViolationException) {
            // 处理@Validated注解校验不通过产生的异常
            ConstraintViolationException e = (ConstraintViolationException) ex;
            // 字段名仅打日志（便于排错），不拼到 errorMsg 里——避免参数名直接给用户看
            String fieldPaths = e.getConstraintViolations().stream().map(violation -> violation.getPropertyPath().toString()).collect(Collectors.joining(", "));
            errorMsg = e.getConstraintViolations().stream().map(ConstraintViolation::getMessage).collect(Collectors.joining(", "));
            log.warn("[参数异常] 约束校验失败: URI=[{}], 错误字段=[{}], 错误信息=[{}], 关键位置={}",
                    requestURI, fieldPaths, errorMsg, stackInfo);
            prodErrorMsg = "请求参数不符合约束条件，请检查输入";
        } else if (ex instanceof HandlerMethodValidationException) {
            // 处理Spring Boot 3.3中的方法参数验证异常
            HandlerMethodValidationException e = (HandlerMethodValidationException) ex;
            errorMsg = e.getAllValidationResults().stream()
                    .flatMap(result -> result.getResolvableErrors().stream())
                    .map(MessageSourceResolvable::getDefaultMessage)
                    .collect(Collectors.joining(", "));
            log.warn("[参数异常] 方法参数验证失败: URI=[{}], 错误信息=[{}], 关键位置={}",
                    requestURI, errorMsg, stackInfo);
            prodErrorMsg = e.getAllValidationResults().stream()
                    .flatMap(result -> result.getResolvableErrors().stream())
                    .map(MessageSourceResolvable::getDefaultMessage)
                    .findFirst()
                    .orElse("请求参数格式不正确，请检查输入");
        } else if (ex instanceof MissingServletRequestParameterException) {
            // 处理缺少必要参数的异常
            MissingServletRequestParameterException e = (MissingServletRequestParameterException) ex;
            errorMsg = "缺少必要参数: " + e.getParameterName();
            log.warn("[参数异常] 缺少必要参数: URI=[{}], 参数名=[{}], 关键位置={}",
                    requestURI, e.getParameterName(), stackInfo);
            prodErrorMsg = "缺少必要的请求参数，请完善请求内容";
        } else if (ex instanceof MethodArgumentTypeMismatchException) {
            // 处理参数类型不匹配的异常
            MethodArgumentTypeMismatchException e = (MethodArgumentTypeMismatchException) ex;
            errorMsg = "参数类型错误: " + e.getName() + "应为" + e.getRequiredType().getSimpleName() + "类型";
            log.warn("[参数异常] 参数类型不匹配: URI=[{}], 参数名=[{}], 提供值=[{}], 期望类型=[{}], 关键位置={}",
                    requestURI, e.getName(), e.getValue(), e.getRequiredType().getSimpleName(), stackInfo);
            prodErrorMsg = "参数类型不正确，请检查输入格式";
        } else if (ex instanceof HttpMessageNotReadableException) {
            // 处理请求体不可读的异常
            errorMsg = "请求体格式错误或不完整";
            log.warn("[参数异常] 请求体不可读: URI=[{}], 异常信息=[{}], 关键位置={}",
                    requestURI, ex.getMessage(), stackInfo);
            prodErrorMsg = "请求内容格式错误，请检查JSON格式是否正确";
        } else if (ex instanceof MissingPathVariableException) {
            // 处理缺少路径变量的异常
            MissingPathVariableException e = (MissingPathVariableException) ex;
            errorMsg = "缺少路径变量: " + e.getVariableName();
            log.warn("[参数异常] 缺少路径变量: URI=[{}], 变量名=[{}], 关键位置={}",
                    requestURI, e.getVariableName(), stackInfo);
            prodErrorMsg = "请求路径不完整，缺少必要参数";
        } else if (ex instanceof HttpRequestMethodNotSupportedException) {
            // 处理HTTP方法不支持的异常
            HttpRequestMethodNotSupportedException e = (HttpRequestMethodNotSupportedException) ex;
            errorMsg = "不支持的HTTP方法: " + e.getMethod();
            log.warn("[参数异常] 不支持的HTTP方法: URI=[{}], 方法=[{}], 支持的方法=[{}], 关键位置={}",
                    requestURI, e.getMethod(), String.join(", ", e.getSupportedMethods()), stackInfo);
            prodErrorMsg = "不支持当前请求方式";
        } else if (ex instanceof HttpMediaTypeNotSupportedException) {
            // 处理媒体类型不支持的异常
            HttpMediaTypeNotSupportedException e = (HttpMediaTypeNotSupportedException) ex;
            errorMsg = "不支持的媒体类型: " + e.getContentType();
            log.warn("[参数异常] 不支持的媒体类型: URI=[{}], 类型=[{}], 关键位置={}",
                    requestURI, e.getContentType(), stackInfo);
            prodErrorMsg = "不支持的内容类型";
        } else if (ex instanceof MaxUploadSizeExceededException) {
            // 处理上传文件大小超限的异常
            MaxUploadSizeExceededException e = (MaxUploadSizeExceededException) ex;
            errorMsg = "上传文件大小超过限制: " + e.getMaxUploadSize() / 1024 / 1024 + "MB";
            log.warn("[参数异常] 文件上传大小超限: URI=[{}], 最大限制=[{}]MB, 关键位置={}",
                    requestURI, e.getMaxUploadSize() / 1024 / 1024, stackInfo);
            prodErrorMsg = "上传文件过大，请压缩后重试";
        } else if (ex instanceof IllegalArgumentException) {
            // 处理非法参数异常
            errorMsg = "非法参数: " + ex.getMessage();
            log.warn("[参数异常] 非法参数: URI=[{}], 异常信息=[{}], 关键位置={}",
                    requestURI, ex.getMessage(), stackInfo);
            prodErrorMsg = "请求参数不合法，请检查输入";
        } else if (ex instanceof ServletRequestBindingException) {
            // 处理请求绑定异常
            errorMsg = "请求绑定异常: " + ex.getMessage();
            log.warn("[参数异常] 请求绑定异常: URI=[{}], 异常信息=[{}], 关键位置={}",
                    requestURI, ex.getMessage(), stackInfo);
            prodErrorMsg = "请求参数绑定失败，请检查参数格式";
        } else if (ex instanceof MissingServletRequestPartException) {
            // 处理缺少请求部分异常
            MissingServletRequestPartException e = (MissingServletRequestPartException) ex;
            errorMsg = "缺少请求部分: " + e.getRequestPartName();
            log.warn("[参数异常] 缺少请求部分: URI=[{}], 部分名=[{}], 关键位置={}",
                    requestURI, e.getRequestPartName(), stackInfo);
            prodErrorMsg = "请求不完整，缺少必要的文件或表单部分";
        } else if (ex instanceof ValidationException) {
            // 处理基础校验异常
            errorMsg = "参数校验失败: " + ex.getMessage();
            log.warn("[参数异常] 基础校验异常: URI=[{}], 异常信息=[{}], 关键位置={}",
                    requestURI, ex.getMessage(), stackInfo);
            prodErrorMsg = "参数校验失败，请检查输入内容";
        } else if (ex instanceof TypeMismatchException) {
            // 处理类型不匹配异常
            TypeMismatchException e = (TypeMismatchException) ex;
            errorMsg = "类型不匹配: " + e.getPropertyName() + "值类型错误";
            log.warn("[参数异常] 类型不匹配: URI=[{}], 属性名=[{}], 提供值=[{}], 关键位置={}",
                    requestURI, e.getPropertyName(), e.getValue(), stackInfo);
            prodErrorMsg = "参数类型不匹配，请检查输入格式";
        } else {
            // 处理其他参数校验异常
            errorMsg = "参数验证失败: " + ex.getMessage();
            log.warn("[参数异常] 其他参数校验异常: URI=[{}], 异常类型=[{}], 异常信息=[{}], 关键位置={}",
                    requestURI, ex.getClass().getName(), ex.getMessage(), stackInfo);
            prodErrorMsg = "参数验证失败，请检查输入";
        }
        // 生产环境使用友好提示，非生产环境展示详细错误
        if (profileUtil.isProd()) {
            return new ResponseEntity<>(R.error(StatusCode.BASE_VALID_PARAM.getCode(), prodErrorMsg), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(R.error(StatusCode.BASE_VALID_PARAM.getCode(), errorMsg), HttpStatus.OK);
        }
    }

    /**
     * 构建字段错误消息（仅拼 message，不拼字段名——字段名走 getFieldNames 进日志）
     */
    private String buildFieldErrorMessage(List<FieldError> fieldErrors) {
        if (CollectionUtil.isNotEmpty(fieldErrors)) {
            return fieldErrors.stream().map(FieldError::getDefaultMessage).collect(Collectors.joining(", "));
        }
        return "请求参数格式不正确，请检查输入内容";
    }

    /**
     * 构建生产环境字段错误消息
     */
    private String buildProdFieldErrorMessage(List<FieldError> fieldErrors) {
        if (CollectionUtil.isNotEmpty(fieldErrors)) {
            return fieldErrors.stream().findFirst().get().getDefaultMessage();
        }
        return "请求参数格式不正确，请检查输入内容";
    }

    /**
     * 获取所有错误字段名称
     */
    private String getFieldNames(List<FieldError> fieldErrors) {
        return fieldErrors.stream().map(FieldError::getField).collect(Collectors.joining(", "));
    }

    /**
     * 判断是否为SSE端点
     *
     * @param requestURI    请求URI
     * @param handlerMethod 处理方法
     * @return 是否为SSE端点
     */
    private boolean isSseEndpoint(String requestURI, HandlerMethod handlerMethod) {
        // 通过返回类型判断
        if (handlerMethod != null) {
            Class<?> returnType = handlerMethod.getMethod().getReturnType();
            if (SseEmitter.class.isAssignableFrom(returnType)) {
                return true;
            }
        }

        // 通过URI路径判断
        return requestURI.contains("/ask") || requestURI.contains("/screenshotAnalysis");
    }

    /**
     * 获取关键堆栈信息
     * 优先返回与项目相关的堆栈，如果找不到则返回前几个可能有用的堆栈
     *
     * @param ex 异常
     * @return 格式化的堆栈信息
     */
    private String getCoreStackTraceInfo(Exception ex) {
        StackTraceElement[] stackTrace = ex.getStackTrace();
        if (stackTrace == null || stackTrace.length == 0) {
            return "";
        }

        StringBuilder stackInfo = new StringBuilder();
        int projectCount = 0;
        int otherCount = 0;

        // 第一遍：优先查找项目相关堆栈
        for (StackTraceElement element : stackTrace) {
            if (element != null && element.getClassName() != null &&
                    element.getClassName().contains("com.jiuyu.replay") && projectCount < 3) {
                stackInfo.append("\n  at ").append(element);
                projectCount++;
            }
        }

        // 如果没有找到项目相关堆栈，则添加前几个非框架堆栈
        if (projectCount == 0) {
            for (StackTraceElement element : stackTrace) {
                if (element != null && element.getClassName() != null &&
                        !isFrameworkClass(element.getClassName()) && otherCount < 3) {
                    stackInfo.append("\n  at ").append(element);
                    otherCount++;
                }
            }

            // 如果还是没有找到有用的堆栈，则添加前3个堆栈
            if (otherCount == 0) {
                for (int i = 0; i < Math.min(3, stackTrace.length); i++) {
                    stackInfo.append("\n  at ").append(stackTrace[i]);
                }
            }
        }
        
        return stackInfo.toString();
    }

    /**
     * 判断是否为常见框架类
     *
     * @param className 类名
     * @return 是否为框架类
     */
    private boolean isFrameworkClass(String className) {
        return className.startsWith("java.") ||
                className.startsWith("javax.") ||
                className.startsWith("sun.") ||
                className.startsWith("com.sun.") ||
                className.startsWith("org.springframework.") ||
                className.startsWith("org.apache.") ||
                className.startsWith("org.hibernate.");
    }
}
