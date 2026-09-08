package com.jiuyu.governance.openfeign.collect.pojo.response;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * 抖音开放API响应基础类
 *
 * @author HeHui
 * @date 2026-02-06 10:41
 */
@Getter
@Setter
public class DouyinOpenResult<T> implements Serializable {


    @Serial
    private static final long serialVersionUID = -3830201715577648302L;

    /** 响应状态吗 */
    private Integer code;

    /** 错误消息 */
    private String message;

    /** 数据 */
    private T data;


    /**
     * Instantiates a new Api response.
     */
    public DouyinOpenResult() {
    }

    /**
     * Instantiates a new Api response.
     *
     * @param code the code
     * @param msg  the msg
     * @param data the data
     */
    public DouyinOpenResult(Integer code, String msg, T data) {
        this.code = code;
        this.message = msg;
        this.data = data;
    }


    /**
     * Ok boolean.
     *
     * @return the boolean
     */
    public boolean ok() {
        return Objects.equals(code, 0);
    }

    /**
     * Failed boolean.
     *
     * @return the boolean
     */
    public boolean failed() {
        return !ok();
    }

}



