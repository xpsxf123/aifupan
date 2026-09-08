package com.jiuyu.replay.words.bo.viewing;

import lombok.Data;

import java.util.List;

@Data
public class VideoDataViewingCallbackBo {

    /**
     * code
     */
    private Integer code;
    /**
     * 消息
     */
    private String msg;
    /**
     * 数据
     */
    private List<LiveRoomBo> data;
    /**
     * 请求id
     */
    private String requestId;
}
