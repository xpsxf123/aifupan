package com.jiuyu.replay.api.logic.words.impl;

import com.jiuyu.replay.api.logic.words.VideoMarkLogic;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bll.VideoMarkBll;
import com.jiuyu.replay.words.bo.VideoMarkBo;
import com.jiuyu.replay.words.bo.VideoMarkListBo;
import com.jiuyu.replay.words.vo.VideoMarkInfoVo;
import com.jiuyu.replay.words.vo.VideoMarkListVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 * 视频标记
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-10-10 18:09:05
 */
@Service
public class VideoMarkLogicImpl implements VideoMarkLogic {

    @Resource
    private VideoMarkBll videoMarkBll;


    @Override
    public R<PageUtils<VideoMarkListVo>> queryPage(VideoMarkListBo videoMarkListBo) {

        return videoMarkBll.queryPage(videoMarkListBo);
    }

    @Override
    public R<VideoMarkInfoVo> info(Long id) {

        return videoMarkBll.info(id);
    }

    @Override
    public R<String> save(VideoMarkBo videoMarkBo) {

        return videoMarkBll.save(videoMarkBo);
    }

    @Override
    public R<String> update(VideoMarkBo videoMarkBo) {

        return videoMarkBll.update(videoMarkBo);
    }

    @Override
    public R<String> delete(Long id) {

        return videoMarkBll.delete(id);
    }

    @Override
    public R<List<VideoMarkListVo>> listByUUID(String uuid) {

        return videoMarkBll.listByUUID(uuid);
    }


}

