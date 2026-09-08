package com.jiuyu.replay.third.bo;

import lombok.Data;

import java.io.Serializable;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/1/14 下午3:02
 */
@Data
public class DanMuBO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 弹幕发送者
     */
    private String nickName;
    /**
     * 用户等级
     */
    private Integer level;
    /**
     * 粉丝等级
     */
    private Integer fansLevel;
    /**
     * 弹幕内容
     */
    private String content;
    /**
     * 消息id
     */
    private String msgId;
}
