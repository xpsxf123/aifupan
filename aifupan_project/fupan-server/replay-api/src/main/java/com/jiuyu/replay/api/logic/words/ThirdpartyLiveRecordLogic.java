package com.jiuyu.replay.api.logic.words;

import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.words.ThirdpartyLiveRecordVo;

import java.util.List;

/**
 * 第三方直播录制记录Logic
 *
 * @author System
 * @date 2026-04-09
 */
public interface ThirdpartyLiveRecordLogic {

    /**
     * 按行业随机获取直播录制记录
     *
     * @param tradeId 行业ID
     * @return 3-5条记录
     */
    R<List<ThirdpartyLiveRecordVo>> randomLiveRecords(Long tradeId);
}
