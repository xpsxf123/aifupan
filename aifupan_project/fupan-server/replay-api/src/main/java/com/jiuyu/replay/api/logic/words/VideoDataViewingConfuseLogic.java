package com.jiuyu.replay.api.logic.words;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.viewing.VideoDataViewingConfuseBo;
import com.jiuyu.replay.words.bo.viewing.VideoDataViewingConfuseListBo;
import com.jiuyu.replay.words.vo.viewing.VideoDataViewingConfuseInfoVo;
import com.jiuyu.replay.words.vo.viewing.VideoDataViewingConfuseListVo;


/**
 * 视频看盘混淆后的数据
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-04-12 16:31:15
 */
public interface VideoDataViewingConfuseLogic {


    /**
     * 视频看盘混淆后的数据列表
     * @param videoDataViewingConfuseListBo 视频看盘混淆后的数据列表查询参数
     * @return
     */
    R<PageUtils<VideoDataViewingConfuseListVo>> queryPage(VideoDataViewingConfuseListBo videoDataViewingConfuseListBo);

    /**
    * 视频看盘混淆后的数据信息
    * @param id 视频看盘混淆后的数据id
    * @return
    */
    R<VideoDataViewingConfuseInfoVo> info(Long id);

    /**
     * 新增视频看盘混淆后的数据
     * @param videoDataViewingConfuseBo 视频看盘混淆后的数据对象
     * @return
     */
    R<String> save(VideoDataViewingConfuseBo videoDataViewingConfuseBo);

    /**
     * 修改视频看盘混淆后的数据
     * @param videoDataViewingConfuseBo 视频看盘混淆后的数据对象
     * @return
     */
    R<String> update(VideoDataViewingConfuseBo videoDataViewingConfuseBo);

    /**
     * 删除视频看盘混淆后的数据
     * @param id 视频看盘混淆后的数据id
     * @return
     */
    R<String> delete(Long id);

    /**
     * 根据视频id查询视频看盘混淆后的数据
     * @param videoId
     * @return
     */
    R<VideoDataViewingConfuseInfoVo> getByVideoId(String videoId);
}

