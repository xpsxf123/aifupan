package com.jiuyu.replay.api.logic.words.impl;

import com.jiuyu.replay.api.logic.words.VideoDataViewingConfuseLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bll.VideoDataViewingConfuseBll;
import com.jiuyu.replay.words.bo.viewing.VideoDataViewingConfuseBo;
import com.jiuyu.replay.words.bo.viewing.VideoDataViewingConfuseListBo;
import com.jiuyu.replay.words.vo.viewing.VideoDataViewingConfuseInfoVo;
import com.jiuyu.replay.words.vo.viewing.VideoDataViewingConfuseListVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;


/**
 * 视频看盘混淆后的数据
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-04-12 16:31:15
 */
@Service
public class VideoDataViewingConfuseLogicImpl implements VideoDataViewingConfuseLogic {

    @Resource
    private VideoDataViewingConfuseBll videoDataViewingConfuseBll;


    @Override
    public R<PageUtils<VideoDataViewingConfuseListVo>> queryPage(VideoDataViewingConfuseListBo videoDataViewingConfuseListBo) {

        return videoDataViewingConfuseBll.queryPage(videoDataViewingConfuseListBo);
    }

    @Override
    public R<VideoDataViewingConfuseInfoVo> info(Long id) {

        return videoDataViewingConfuseBll.info(id);
    }

    @Override
    public R<String> save(VideoDataViewingConfuseBo videoDataViewingConfuseBo) {

        return videoDataViewingConfuseBll.save(videoDataViewingConfuseBo);
    }

    @Override
    public R<String> update(VideoDataViewingConfuseBo videoDataViewingConfuseBo) {

        return videoDataViewingConfuseBll.update(videoDataViewingConfuseBo);
    }

    @Override
    public R<String> delete(Long id) {

        return videoDataViewingConfuseBll.delete(id);
    }

    @Override
    public R<VideoDataViewingConfuseInfoVo> getByVideoId(String videoId) {
        return videoDataViewingConfuseBll.getByVideoId(videoId);
    }
}

