package com.jiuyu.replay.words.bll;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.VideoAnalysisRecordBo;
import com.jiuyu.replay.words.bo.VideoAnalysisRecordListBo;
import com.jiuyu.replay.words.entity.VideoAnalysisRecordEntity;
import com.jiuyu.replay.words.producer.VideoAnalysisRecordProducer;
import com.jiuyu.replay.generic.vo.words.SentenceMarkVo;
import com.jiuyu.replay.generic.vo.words.VideoAnalysisRecordInfoVo;
import com.jiuyu.replay.generic.vo.words.VideoAnalysisRecordListVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;


/**
 * 视频的分析记录
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-12-03 11:26:50
 */
@Component
public class VideoAnalysisRecordBll {

    @Resource
    private VideoAnalysisRecordProducer videoAnalysisRecordProducer;


    /**
     * 视频的分析记录列表
     * @param videoAnalysisRecordListBo 视频的分析记录列表查询参数
     * @return
     */
    public R<PageUtils<VideoAnalysisRecordListVo>> queryPage(VideoAnalysisRecordListBo videoAnalysisRecordListBo) {

        return R.ok("获取成功", videoAnalysisRecordProducer.queryPage(videoAnalysisRecordListBo));
    }

    /**
    * 视频的分析记录信息
    * @param id 视频的分析记录id
    * @return
    */
    public R<VideoAnalysisRecordInfoVo> info(Long id) {

        VideoAnalysisRecordInfoVo videoAnalysisRecordInfoVo = videoAnalysisRecordProducer.info(id);
        return R.ok("获取成功", videoAnalysisRecordInfoVo);
    }

    /**
     * 新增视频的分析记录
     * @param videoAnalysisRecordBo 视频的分析记录对象
     * @return
     */
    public R<String> save(VideoAnalysisRecordBo videoAnalysisRecordBo) {

        VideoAnalysisRecordInfoVo videoAnalysisRecordInfoVo = videoAnalysisRecordProducer.save(videoAnalysisRecordBo);
        return R.ok("添加成功");
    }

    /**
     * 修改视频的分析记录
     * @param videoAnalysisRecordBo 视频的分析记录对象
     * @return
     */
    public R<String> update(VideoAnalysisRecordBo videoAnalysisRecordBo) {

        videoAnalysisRecordProducer.update(videoAnalysisRecordBo);
        return R.ok("修改成功");
    }

    /**
     * 删除视频的分析记录
     * @param id 视频的分析记录id
     * @return
     */
    public R<String> delete(Long id) {

        videoAnalysisRecordProducer.deleteById(id);
        return R.ok("删除成功");
    }


    /**
     * 根据视频id和行业id获取最后一条分析记录
     * @param videoId 视频id
     * @param tradeId 行业id
     * @return
     */
    public R<VideoAnalysisRecordInfoVo> infoLastByVideoIdAndTradeId(String videoId, Long tradeId) {

        return R.ok(this.videoAnalysisRecordProducer.infoLastByVideoIdAndTradeId(videoId, tradeId));

    }

    public R<List<SentenceMarkVo>> selectAnalysisByVideoId(String videoId, Long tradeId) {
        return videoAnalysisRecordProducer.selectAnalysisByVideoId(videoId,tradeId);
    }

    /**
     * 获取昨天的全部分析记录
     * @return
     */
    public List<VideoAnalysisRecordEntity> yesterdayDateRecord() {
        return videoAnalysisRecordProducer.yesterdayDateRecord();
    }
}

