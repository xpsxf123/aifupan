package com.jiuyu.replay.words.rse;

import com.jiuyu.replay.generic.bo.words.video.VideoSliceBo;
import com.jiuyu.replay.generic.vo.video.VideoSliceVo;

import java.util.List;

public interface VideoSliceRse {

    /**
     * 保存视频切片
     * @param videoSliceBo 视频切片参数
     * @return
     */
    void saveVideoSlice(VideoSliceBo videoSliceBo);

    /**
     * 根据源id集合删除切片
     * @param sourceIds 源id集合
     */
    void removeBySourceIds(List<String> sourceIds);

    /**
     * 修改视频切片
     *
     * @param videoSliceBo 视频切片参数
     *
     * @return
     */
    void updateVideoSlice(VideoSliceBo videoSliceBo);

    /**
     * 根据源id和源类型获取切片
     *
     * @param sourceId   来源id
     * @param sourceType 来源类型
     * @return 切片信息
     */
    VideoSliceVo getVideoSliceBySourceId(String sourceId, Integer sourceType);

    /**
     * 根据来源id获取切片信息
     * @param sourceId 来源id
     * @return
     */
    VideoSliceVo getByVideoId(String sourceId);
}
