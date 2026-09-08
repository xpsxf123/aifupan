package com.jiuyu.governance.openfeign.replay.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 短信发送结果
 *
 * @author jiuyu
 */
@Getter
@Setter
@NoArgsConstructor
@Accessors(chain = true)
public class SmsResponse implements Serializable {

    @Serial
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
    public static SmsResponse success() {
        return new SmsResponse().setSuccess(true).setCode("200").setMessage("发送成功");
    }

    /**
     * 创建失败结果
     *
     * @param code    状态码
     * @param message 消息
     *
     * @return 失败结果
     */
    public static SmsResponse fail(String code, String message) {
        return new SmsResponse().setSuccess(false).setCode(code).setMessage(message);
    }
}
