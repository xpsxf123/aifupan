package com.jiuyu.replay.generic.dto.third;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * 短信发送结果
 *
 * @author jiuyu
 */
@Data
@Accessors(chain = true)
public class SmsResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 状态码
     */
    private String code;

    /**
     * 消息
     */
    private String message;

    /**
     * 请求ID/任务ID
     */
    private String requestId;

    /**
     * 发送数量
     */
    private Integer count;

    /**
     * 非法手机号列表
     */
    private List<String> illegalMobiles;

    /**
     * 创建成功结果
     *
     * @return 成功结果
     */
    public static SmsResult success() {
        return new SmsResult().setSuccess(true).setCode("200").setMessage("发送成功");
    }

    /**
     * 创建失败结果
     *
     * @param code    状态码
     * @param message 消息
     * @return 失败结果
     */
    public static SmsResult fail(String code, String message) {
        return new SmsResult().setSuccess(false).setCode(code).setMessage(message);
    }
} 