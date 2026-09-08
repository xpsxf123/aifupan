package com.jiuyu.replay.words.bll;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.words.vo.VideoMarkListVo;
import com.jiuyu.replay.words.vo.VideoMarkInfoVo;
import com.jiuyu.replay.words.bo.VideoMarkBo;
import com.jiuyu.replay.words.bo.VideoMarkListBo;
import com.jiuyu.replay.words.producer.VideoMarkProducer;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;


/**
 * 视频标记
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-10-10 18:09:05
 */
@Component
public class VideoMarkBll {

    @Resource
    private VideoMarkProducer videoMarkProducer;


    /**
     * 视频标记列表
     * @param videoMarkListBo 视频标记列表查询参数
     * @return
     */
    public R<PageUtils<VideoMarkListVo>> queryPage(VideoMarkListBo videoMarkListBo) {

        return R.ok("获取成功", videoMarkProducer.queryPage(videoMarkListBo));
    }

    /**
    * 视频标记信息
    * @param id 视频标记id
    * @return
    */
    public R<VideoMarkInfoVo> info(Long id) {

        VideoMarkInfoVo videoMarkInfoVo = videoMarkProducer.info(id);
        return R.ok("获取成功", videoMarkInfoVo);
    }

    /**
     * 新增视频标记
     * @param videoMarkBo 视频标记对象
     * @return
     */
    public R<String> save(VideoMarkBo videoMarkBo) {

        VideoMarkInfoVo videoMarkInfoVo = videoMarkProducer.save(videoMarkBo);
        return R.ok("添加成功");
    }

    /**
     * 修改视频标记
     * @param videoMarkBo 视频标记对象
     * @return
     */
    public R<String> update(VideoMarkBo videoMarkBo) {

        videoMarkProducer.update(videoMarkBo);
        return R.ok("修改成功");
    }

    /**
     * 删除视频标记
     * @param id 视频标记id
     * @return
     */
    public R<String> delete(Long id) {

        videoMarkProducer.deleteById(id);
        return R.ok("删除成功");
    }

    /**
     * 根据文件uuid唯一标识获取标记列表
     * @param uuid 视频标记唯一标识uuid
     * @return
     */
    public R<List<VideoMarkListVo>> listByUUID(String uuid) {

        return R.ok("获取成功", videoMarkProducer.listByUUID(uuid));
    }
}

