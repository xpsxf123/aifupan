package com.jiuyu.replay.api.logic.words.impl;

import com.jiuyu.replay.api.logic.words.VideoDataViewingRatioLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bll.VideoDataViewingRatioBll;
import com.jiuyu.replay.words.bo.viewing.VideoDataViewingRatioBo;
import com.jiuyu.replay.words.bo.viewing.VideoDataViewingRatioListBo;
import com.jiuyu.replay.words.vo.viewing.VideoDataViewingRatioInfoVo;
import com.jiuyu.replay.words.vo.viewing.VideoDataViewingRatioListVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;


/**
 * 数据看盘比例
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-04-17 17:45:33
 */
@Service
public class VideoDataViewingRatioLogicImpl implements VideoDataViewingRatioLogic {

    @Resource
    private VideoDataViewingRatioBll videoDataViewingRatioBll;


    @Override
    public R<PageUtils<VideoDataViewingRatioListVo>> queryPage(VideoDataViewingRatioListBo videoDataViewingRatioListBo) {

        return videoDataViewingRatioBll.queryPage(videoDataViewingRatioListBo);
    }

    @Override
    public R<VideoDataViewingRatioInfoVo> info(Long id) {

        return videoDataViewingRatioBll.info(id);
    }

    @Override
    public R<String> save(VideoDataViewingRatioBo videoDataViewingRatioBo) {

        return videoDataViewingRatioBll.save(videoDataViewingRatioBo);
    }

    @Override
    public R<String> update(VideoDataViewingRatioBo videoDataViewingRatioBo) {

        return videoDataViewingRatioBll.update(videoDataViewingRatioBo);
    }

    @Override
    public R<String> delete(Long id) {

        return videoDataViewingRatioBll.delete(id);
    }


}

