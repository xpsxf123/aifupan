package com.jiuyu.replay.api.logic.words.impl;

import com.jiuyu.replay.api.logic.words.ThirdpartyLiveRecordLogic;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.words.ThirdpartyLiveRecordVo;
import com.jiuyu.replay.words.bll.ThirdpartyLiveRecordBll;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 第三方直播录制记录LogicImpl
 *
 * @author System
 * @date 2026-04-09
 */
@Service
@AllArgsConstructor
public class ThirdpartyLiveRecordLogicImpl implements ThirdpartyLiveRecordLogic {

    private final ThirdpartyLiveRecordBll thirdpartyLiveRecordBll;

    @Override
    public R<List<ThirdpartyLiveRecordVo>> randomLiveRecords(Long tradeId) {
        return thirdpartyLiveRecordBll.randomLiveRecords(tradeId);
    }
}
