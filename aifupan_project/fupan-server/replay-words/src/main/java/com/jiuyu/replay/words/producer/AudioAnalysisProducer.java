package com.jiuyu.replay.words.producer;

import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.AudioAnalysisBo;
import com.jiuyu.replay.words.entity.AudioAnalysisEntity;
import com.jiuyu.replay.words.vo.AudioAnalysisVo;
import com.jiuyu.replay.words.vo.AudioAnalysissVO;
import com.jiuyu.replay.words.vo.OnlineAnalysisItemVo;

import java.util.List;

public interface AudioAnalysisProducer {
    /**
     * 新增分享视频添加内容
     */
    void  saveBatchs(List<AudioAnalysisEntity> audioAnalysis);

    /**
     * 根据视频id查询分享内容
     */

    List<AudioAnalysisEntity> listByVideoiId(Long VideoiId);

    /**
     * 根据视频id查询该视频分析
     */
    R<List<AudioAnalysisEntity>> selectByVideoId(AudioAnalysisBo id);

    /**
     * 新增直播视频分的内容
     * @param udioAnalysisVo
     * @return
     */
    R saveAudioAnalysis(List<AudioAnalysisVo> udioAnalysisVo);

    /**
     * 客户端根据视频Id和行业ID查询该视频的分析内容
     * @param videoId
     * @return
     */
    R<List<AudioAnalysissVO>> selectByVideoIdOrTradeId(AudioAnalysisBo videoId);

    /**
     * 清除视频的分析数据
     * @param videoId 视频唯一标识
     * @return
     */
    void clearAnalysis(String videoId);

    /**
     * 根据视频唯一标识和行业id获取分析数据列表
     * @param videoId 视频唯一标识uuid
     * @param tradeId 行业id
     * @return
     */
    List<OnlineAnalysisItemVo> listByVideoIdAndTradeId(String videoId, Long tradeId);

    /**
     * 获取视频的第一批段落列表
     * @param videoId 视频唯一标识
     * @return
     */
    List<AudioAnalysisVo> listAnalysisOneByVideoId(String videoId);
}
