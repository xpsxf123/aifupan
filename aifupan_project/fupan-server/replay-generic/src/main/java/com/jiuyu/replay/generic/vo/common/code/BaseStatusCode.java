package com.jiuyu.replay.generic.vo.common.code;

/**
 * 全局状态码
 *
 * @author RayChou
 * @date 2025/6/25 11:23
 */
public interface BaseStatusCode {

    /**
     * 状态码
     *
     * @return
     */
    int getCode();

    /**
     * 状态信息
     *
     * @return
     */
    String getMsg();
}
