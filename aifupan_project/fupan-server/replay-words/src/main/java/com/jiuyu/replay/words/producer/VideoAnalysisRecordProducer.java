package com.jiuyu.replay.words.producer;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.VideoAnalysisRecordBo;
import com.jiuyu.replay.words.bo.VideoAnalysisRecordListBo;
import com.jiuyu.replay.words.entity.VideoAnalysisRecordEntity;
import com.jiuyu.replay.generic.vo.words.SentenceMarkVo;
import com.jiuyu.replay.generic.vo.words.VideoAnalysisRecordInfoVo;
import com.jiuyu.replay.generic.vo.words.VideoAnalysisRecordListVo;

import java.util.Collection;
import java.util.List;


/**
 * 视频的分析记录
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-12-03 11:26:50
 */
public interface VideoAnalysisRecordProducer {


    /**
     * 视频的分析记录列表
     * @param videoAnalysisRecordListBo 视频的分析记录列表查询参数
     * @return
     */
    PageUtils<VideoAnalysisRecordListVo> queryPage(VideoAnalysisRecordListBo videoAnalysisRecordListBo);

    /**
    * 视频的分析记录信息
    * @param id 视频的分析记录id
    * @return
    */
    VideoAnalysisRecordInfoVo info(Long id);

    /**
     * 新增视频的分析记录
     * @param videoAnalysisRecordBo 视频的分析记录对象
     * @return
     */
     VideoAnalysisRecordInfoVo save(VideoAnalysisRecordBo videoAnalysisRecordBo);

    /**
     * 修改视频的分析记录
     * @param videoAnalysisRecordBo 视频的分析记录对象
     * @return
     */
    void update(VideoAnalysisRecordBo videoAnalysisRecordBo);

    /**
     * 删除视频的分析记录
     * @param id 视频的分析记录id
     * @return
     */
    void deleteById(Long id);


    /**
     * 获取视频分析最新的版本号
     * @param videoId 视频id
     * @return
     */
    int getLastVersion(String videoId);

    /**
     * 获取视频分析最新的分析信息
     * @param videoId 视频id
     * @return
     */
    VideoAnalysisRecordInfoVo getLastInfo(String videoId);

    /**
     * 根据视频id和行业id获取最后一条分析记录
     * @param videoId 视频id
     * @param tradeId 行业id
     * @return
     */
    VideoAnalysisRecordInfoVo infoLastByVideoIdAndTradeId(String videoId, Long tradeId);

    /**
     * 根据视频id和版本号获取分析记录
     * @param videoId 视频id
     * @param version 版本号
     * @return
     */
    VideoAnalysisRecordInfoVo infoByVideoIdAndVersion(String videoId, int version);

    R<List<SentenceMarkVo>> selectAnalysisByVideoId(String videoId, Long tradeId);

    /**
     * 将新路径保存到数据库
     * @param id 记录id
     * @param storeFileNameNew 新路径
     */
    void saveNewFilePath(Long id, String storeFileNameNew);

    /**
     * 将cosKey保存到数据库
     * @param id 记录id
     * @param cosSaveKey cosKey
     */
    void saveCosKey(Long id, String cosSaveKey);

    /**
     * 获取昨天的全部分析记录
     * @return
     */
    List<VideoAnalysisRecordEntity> yesterdayDateRecord();

    /**
     * 根据视频id集合获取分析记录
     * @param videoIds 视频id集合
     * @return
     */
    List<VideoAnalysisRecordInfoVo> listByVideoIds(Collection<String> videoIds);
}

