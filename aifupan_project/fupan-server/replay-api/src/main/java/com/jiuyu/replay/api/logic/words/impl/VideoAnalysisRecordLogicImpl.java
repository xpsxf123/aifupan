package com.jiuyu.replay.api.logic.words.impl;

import com.jiuyu.replay.api.logic.words.VideoAnalysisRecordLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bll.VideoAnalysisRecordBll;
import com.jiuyu.replay.words.bo.VideoAnalysisRecordBo;
import com.jiuyu.replay.words.bo.VideoAnalysisRecordListBo;
import com.jiuyu.replay.generic.vo.words.SentenceMarkVo;
import com.jiuyu.replay.generic.vo.words.VideoAnalysisRecordInfoVo;
import com.jiuyu.replay.generic.vo.words.VideoAnalysisRecordListVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 * 视频的分析记录
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-12-03 11:26:50
 */
@Service
public class VideoAnalysisRecordLogicImpl implements VideoAnalysisRecordLogic {

    @Resource
    private VideoAnalysisRecordBll videoAnalysisRecordBll;


    @Override
    public R<PageUtils<VideoAnalysisRecordListVo>> queryPage(VideoAnalysisRecordListBo videoAnalysisRecordListBo) {

        return videoAnalysisRecordBll.queryPage(videoAnalysisRecordListBo);
    }

    @Override
    public R<VideoAnalysisRecordInfoVo> info(Long id) {

        return videoAnalysisRecordBll.info(id);
    }

    @Override
    public R<String> save(VideoAnalysisRecordBo videoAnalysisRecordBo) {

        return videoAnalysisRecordBll.save(videoAnalysisRecordBo);
    }

    @Override
    public R<String> update(VideoAnalysisRecordBo videoAnalysisRecordBo) {

        return videoAnalysisRecordBll.update(videoAnalysisRecordBo);
    }

    @Override
    public R<String> delete(Long id) {

        return videoAnalysisRecordBll.delete(id);
    }

    @Override
    public R<List<SentenceMarkVo>> selectAnalysisByVideoId(String videoId, Long tradeId) {
        return videoAnalysisRecordBll.selectAnalysisByVideoId(videoId,tradeId);
    }

}

