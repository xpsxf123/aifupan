package com.jiuyu.replay.generic.vo.common;

import com.jiuyu.replay.generic.vo.common.code.BaseStatusCode;
import com.jiuyu.replay.generic.vo.common.code.StatusCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Objects;

@Schema(description = "公共返回数据对象")
@Data
public class R<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "响应码，为0表示成功")
    private Integer code;
    @Schema(description = "响应信息")
    private String msg;
    @Schema(description = "响应数据")
    private T data;

    // 添加无参构造函数
    public R() {
    }

    public R(Integer code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public R(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
        this.data = null;
    }

    public static <T> R<T> ok() {

        return new R<>(0, "", null);
    }

    public static <T> R<T> ok(String msg) {

        return new R<>(0, msg, null);
    }

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>(0, "", data);
        return r;
    }

    public static <T> R<T> ok(String msg, T data) {

        return new R<>(0, msg, data);
    }

    public static <T> R<T> error(String msg) {
        return new R<>(StatusCode.BASE_VALID_PARAM.getCode(), msg, null);
    }

    public static <T> R<T> error(Integer code, String msg) {
        return new R<>(code, msg, null);
    }

    public static <T> R<T> error(Integer code, String msg, T data) {
        return new R<>(code, msg, data);
    }

    public static <T> R<T> validFail(BaseStatusCode statusCode) {
        return new R(statusCode.getCode(), statusCode.getMsg() != null && !statusCode.getMsg().isEmpty() ? statusCode.getMsg() : "系统繁忙，请稍候再试");
    }

    public boolean success() {
        return Objects.equals(this.code, 0);
    }

    public boolean fail() {
        return !success();
    }
}
