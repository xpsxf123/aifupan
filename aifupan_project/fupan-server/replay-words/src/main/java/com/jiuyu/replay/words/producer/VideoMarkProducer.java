package com.jiuyu.replay.words.producer;

import com.jiuyu.replay.generic.utils.PageUtils;

import com.jiuyu.replay.words.vo.VideoMarkListVo;
import com.jiuyu.replay.words.vo.VideoMarkInfoVo;
import com.jiuyu.replay.words.bo.VideoMarkBo;
import com.jiuyu.replay.words.bo.VideoMarkListBo;

import java.util.List;


/**
 * 视频标记
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-10-10 18:09:05
 */
public interface VideoMarkProducer {


    /**
     * 视频标记列表
     * @param videoMarkListBo 视频标记列表查询参数
     * @return
     */
    PageUtils<VideoMarkListVo> queryPage(VideoMarkListBo videoMarkListBo);

    /**
    * 视频标记信息
    * @param id 视频标记id
    * @return
    */
    VideoMarkInfoVo info(Long id);

    /**
     * 新增视频标记
     * @param videoMarkBo 视频标记对象
     * @return
     */
     VideoMarkInfoVo save(VideoMarkBo videoMarkBo);

    /**
     * 修改视频标记
     * @param videoMarkBo 视频标记对象
     * @return
     */
    void update(VideoMarkBo videoMarkBo);

    /**
     * 删除视频标记
     * @param id 视频标记id
     * @return
     */
    void deleteById(Long id);


    /**
     * 根据文件uuid唯一标识获取标记列表
     * @param uuid 视频标记唯一标识uuid
     * @return
     */
    List<VideoMarkListVo> listByUUID(String uuid);
}

