package com.jiuyu.replay.api.logic.words;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.viewing.VideoDataViewingBo;
import com.jiuyu.replay.words.bo.viewing.VideoDataViewingCallbackBo;
import com.jiuyu.replay.words.bo.viewing.VideoDataViewingListBo;
import com.jiuyu.replay.words.vo.viewing.VideoDataViewingConfuseInfoVo;
import com.jiuyu.replay.words.vo.viewing.VideoDataViewingContrastVo;
import com.jiuyu.replay.words.vo.viewing.VideoDataViewingInfoVo;
import com.jiuyu.replay.words.vo.viewing.VideoDataViewingListVo;


/**
 * 视频看盘数据
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-04-12 16:31:15
 */
public interface VideoDataViewingLogic {


    /**
     * 视频看盘数据列表
     * @param videoDataViewingListBo 视频看盘数据列表查询参数
     * @return
     */
    R<PageUtils<VideoDataViewingListVo>> queryPage(VideoDataViewingListBo videoDataViewingListBo);

    /**
    * 视频看盘数据信息
    * @param id 视频看盘数据id
    * @return
    */
    R<VideoDataViewingInfoVo> info(Long id);

    /**
     * 新增视频看盘数据
     * @param videoDataViewingBo 视频看盘数据对象
     * @return
     */
    R<String> save(VideoDataViewingBo videoDataViewingBo);

    /**
     * 修改视频看盘数据
     * @param videoDataViewingBo 视频看盘数据对象
     * @return
     */
    R<String> update(VideoDataViewingBo videoDataViewingBo);

    /**
     * 删除视频看盘数据
     * @param id 视频看盘数据id
     * @return
     */
    R<String> delete(Long id);


    /**
     * 生成视频场次的看盘数据
     * @param videoId 视频id
     * @param isAuto 是否自动生成的 0：否 1：是
     * @param anchorOnlineStatus 主播是否已下播 0：否 1：是
     * @return
     */
    R<String> createDataViewing(String videoId, Integer isAuto, Integer anchorOnlineStatus);

    /**
     * 处理第三方数据平台回调数据
     * @param videoDataViewingCallbackBo 第三方数据平台回调数据
     */
    R<String> handleDataViewingCallback(VideoDataViewingCallbackBo videoDataViewingCallbackBo);

    /**
     * 根据视频id获取看盘数据
     * @param videoId 视频id
     * @return
     */
    R<VideoDataViewingConfuseInfoVo> infoByVideoId(String videoId);

    /**
     * 根据对比id获取看盘数据
     * @param contrastId 对比id
     * @return
     */
    R<VideoDataViewingContrastVo> infoByContrastId(String contrastId);
}

