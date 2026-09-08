package com.jiuyu.replay.words.rse;

import com.jiuyu.replay.generic.bo.words.video.SaveSliceCorrelationDataBo;
import com.jiuyu.replay.words.bo.oceanEngine.OceanEngineProcessBo;
import com.jiuyu.replay.words.vo.viewing.VideoDataViewingConfuseInfoVo;

import java.util.List;

public interface VideoDataViewingParagraphRse {

    /**
     * 保存或更新本段录制的看板数据
     * @param dataViewingConfuseInfoVo 原数据看板数据
     * @param realTimeDataList 实时过程数据
     */
    void saveOrUpdateParagraphData(VideoDataViewingConfuseInfoVo dataViewingConfuseInfoVo, List<OceanEngineProcessBo> realTimeDataList);

    /**
     * 根据视频id获取本段视频的数据看板数据
     * @param videoId 视频id
     * @return
     */
    VideoDataViewingConfuseInfoVo infoByVideoId(String videoId);

    /**
     * 保存切片视频本段录制的看板数据
     * @param saveSliceCorrelationDataBo 切片信息
     * @param realTimeDataList 实时数据列表
     * @param dataViewingConfuseInfoVo 数据看板数据
     */
    void saveSliceVideoParagraphData(SaveSliceCorrelationDataBo saveSliceCorrelationDataBo, List<OceanEngineProcessBo> realTimeDataList, VideoDataViewingConfuseInfoVo dataViewingConfuseInfoVo);

    /**
     * 根据视频id列表批量获取本段视频的数据看板数据
     * @param videoIds 视频id列表
     * @return 数据看板数据列表
     */
    List<VideoDataViewingConfuseInfoVo> listByVideoIds(List<String> videoIds);
}
