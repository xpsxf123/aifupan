package com.jiuyu.replay.api.bo.audio;

import lombok.Getter;
import lombok.Setter;

/**
 *  asr引擎语言请求
 * @author HeHui
 * @date 2026-05-18 19:31
 */
@Getter
@Setter
public class AsrEngineLanguage {

    /**
     * 语言
     */
    private String language;


    /**
     * 业务
     *  anchor_replay: 主播录制
     *  user_upload： 用户上传
     *  short_video： 短视频
     */
    private String business;
}
