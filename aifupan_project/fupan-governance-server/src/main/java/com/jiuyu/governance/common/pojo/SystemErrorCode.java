package com.jiuyu.governance.common.pojo;

import lombok.Getter;

/**
 * 底层系统错误码（保留原有 HTTP 状态码和系统底层错误）
 *
 * @author HeHui
 * @date 2026-03-31
 */
@Getter
public enum SystemErrorCode implements ErrorCode {

    SUCCESS(0, "成功"),
    FAIL(500, "失败"),
    BAD_REQUEST(400, "状态非法"),
    UNAUTHORIZED(401, "未授权访问"), // Previously 4001, but we should map correctly. Wait, old was 4001. I'll make it 401 here and 4001 in InteractionErrorCode, but let's keep 401 for HTTP UNAUTHORIZED if needed, or stick to old codes. Let's look at the old ErrorCode.
    REQUIRED(402, "必填项缺失"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "未找到"),
    NOT_SUPPORTED(405, "不支持"),
    NOT_ACCEPTABLE(406, "不支持"),
    REQUEST_TIMEOUT(408, "请求超时"),
    CONFLICT(409, "冲突"),
    GONE(410, "已删除"),
    UNSUPPORTED_MEDIA_TYPE(415, "不支持的媒体类型"),
    UNPROCESSABLE_ENTITY(422, "无法处理"),
    TOO_MANY_REQUESTS(429, "请求过多");

    private final Integer code;
    private final String message;

    SystemErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
