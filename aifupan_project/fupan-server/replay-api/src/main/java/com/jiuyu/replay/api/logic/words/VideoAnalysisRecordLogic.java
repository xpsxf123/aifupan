package com.jiuyu.replay.api.logic.words;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.generic.vo.words.SentenceMarkVo;
import com.jiuyu.replay.generic.vo.words.VideoAnalysisRecordListVo;
import com.jiuyu.replay.generic.vo.words.VideoAnalysisRecordInfoVo;
import com.jiuyu.replay.words.bo.VideoAnalysisRecordBo;
import com.jiuyu.replay.words.bo.VideoAnalysisRecordListBo;

import java.util.List;


/**
 * 视频的分析记录
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-12-03 11:26:50
 */
public interface VideoAnalysisRecordLogic {


    /**
     * 视频的分析记录列表
     * @param videoAnalysisRecordListBo 视频的分析记录列表查询参数
     * @return
     */
    R<PageUtils<VideoAnalysisRecordListVo>> queryPage(VideoAnalysisRecordListBo videoAnalysisRecordListBo);

    /**
    * 视频的分析记录信息
    * @param id 视频的分析记录id
    * @return
    */
    R<VideoAnalysisRecordInfoVo> info(Long id);

    /**
     * 新增视频的分析记录
     * @param videoAnalysisRecordBo 视频的分析记录对象
     * @return
     */
    R<String> save(VideoAnalysisRecordBo videoAnalysisRecordBo);

    /**
     * 修改视频的分析记录
     * @param videoAnalysisRecordBo 视频的分析记录对象
     * @return
     */
    R<String> update(VideoAnalysisRecordBo videoAnalysisRecordBo);

    /**
     * 删除视频的分析记录
     * @param id 视频的分析记录id
     * @return
     */
    R<String> delete(Long id);


    R<List<SentenceMarkVo>> selectAnalysisByVideoId(String videoId, Long tradeId);

}

