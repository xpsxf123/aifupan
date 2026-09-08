package com.jiuyu.replay.words.producer;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.words.bo.viewing.VideoDataViewingRatioBo;
import com.jiuyu.replay.words.bo.viewing.VideoDataViewingRatioListBo;
import com.jiuyu.replay.words.vo.viewing.VideoDataViewingRatioInfoVo;
import com.jiuyu.replay.words.vo.viewing.VideoDataViewingRatioListVo;


/**
 * 数据看盘比例
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-04-17 17:45:33
 */
public interface VideoDataViewingRatioProducer {


    /**
     * 数据看盘比例列表
     * @param videoDataViewingRatioListBo 数据看盘比例列表查询参数
     * @return
     */
    PageUtils<VideoDataViewingRatioListVo> queryPage(VideoDataViewingRatioListBo videoDataViewingRatioListBo);

    /**
    * 数据看盘比例信息
    * @param id 数据看盘比例id
    * @return
    */
    VideoDataViewingRatioInfoVo info(Long id);

    /**
     * 新增数据看盘比例
     * @param videoDataViewingRatioBo 数据看盘比例对象
     * @return
     */
     VideoDataViewingRatioInfoVo save(VideoDataViewingRatioBo videoDataViewingRatioBo);

    /**
     * 修改数据看盘比例
     * @param videoDataViewingRatioBo 数据看盘比例对象
     * @return
     */
    void update(VideoDataViewingRatioBo videoDataViewingRatioBo);

    /**
     * 删除数据看盘比例
     * @param id 数据看盘比例id
     * @return
     */
    void deleteById(Long id);


}

