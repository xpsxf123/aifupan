package com.jiuyu.governance.common.pojo;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 抽象业务错误码
 *
 * 1xxx: 通用类
 * 2xxx: 规则类
 * 3xxx: 业务类
 * 4xxx: 交互类
 *
 * @author HeHui
 * @date 2026-03-31
 */
@Getter
public enum BizErrorCode implements ErrorCode {

    // --- 1xxx 通用类 (系统运行时的通用技术类异常) ---
    ALLOW_SKIP(1000, "可容忍的未知异常"),
    EXTERNAL_SERVICE_FAILED(1001, "第三方服务调用失败"),
    DATA_ACCESS_FAILED(1002, "内部数据访问异常"),
    IO_FAILED(1003, "文件或资源IO异常"),

    // --- 2xxx 规则类 (通用的业务硬性约束与前置校验拦截) ---
    PARAM_INVALID(2001, "参数校验未通过"),
    DATA_DUPLICATED(2002, "数据已存在"),
    RATE_LIMIT(2003, "操作频率超限"),
    FORMAT_ERROR(2004, "数据格式错误"),

    // --- 3xxx 业务类 (系统核心业务逻辑流转失败) ---
    GENERAL_FAILED(3000, "通用业务处理失败"),
    QUOTA_EXCEEDED(3001, "资源配额不足"),
    INVALID_STATE(3002, "当前状态不支持该操作"),
    HAS_DEPENDENCY(3003, "存在关联数据，拒绝操作"),
    TIME_CONFLICT(3004, "业务时效性冲突"),

    // --- 4xxx 交互类 (前端强感知，必须触发特定UI行为) ---
    UNAUTHORIZED(4001, "未授权访问或登录已过期"),
    HTTP_UNAUTHORIZED(401, "未授权访问或登录已过期"),
    NO_POWER(4002, "没有权限"), // Previously 4002
    PACKAGE_INVALID(4003, "套餐或版本已失效"),
    ACCOUNT_DISABLED(4004, "账户已被禁用"),
    REQUIRE_CONFIRM(4005, "高危操作，需要二次确认");

    private final Integer code;
    private final String message;

    BizErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
