package com.jiuyu.governance.common.pojo;

/**
 * 全局错误码基类接口
 *
 * @author HeHui
 * @date 2026-03-17 14:24
 */
public interface ErrorCode {

    /**
     * 获取错误码
     *
     * @return 错误码
     */
    Integer getCode();

    /**
     * 获取错误信息
     *
     * @return 错误信息
     */
    String getMessage();
}
