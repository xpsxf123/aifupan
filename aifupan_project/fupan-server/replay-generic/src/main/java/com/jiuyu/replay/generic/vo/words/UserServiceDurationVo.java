package com.jiuyu.replay.generic.vo.words;

import lombok.Data;

@Data
public class UserServiceDurationVo {

    /**
     * 服务时长（小时）
     */
    private Long duration;
    /**
     * 视频总场次
     */
    private Long videoCount;
}
