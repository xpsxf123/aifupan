package com.jiuyu.replay.words.bll;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.viewing.VideoDataViewingRatioBo;
import com.jiuyu.replay.words.bo.viewing.VideoDataViewingRatioListBo;
import com.jiuyu.replay.words.producer.VideoDataViewingRatioProducer;
import com.jiuyu.replay.words.vo.viewing.VideoDataViewingRatioInfoVo;
import com.jiuyu.replay.words.vo.viewing.VideoDataViewingRatioListVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;


/**
 * 数据看盘比例
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-04-17 17:45:33
 */
@Component
public class VideoDataViewingRatioBll {

    @Resource
    private VideoDataViewingRatioProducer videoDataViewingRatioProducer;


    /**
     * 数据看盘比例列表
     * @param videoDataViewingRatioListBo 数据看盘比例列表查询参数
     * @return
     */
    public R<PageUtils<VideoDataViewingRatioListVo>> queryPage(VideoDataViewingRatioListBo videoDataViewingRatioListBo) {

        return R.ok("获取成功", videoDataViewingRatioProducer.queryPage(videoDataViewingRatioListBo));
    }

    /**
    * 数据看盘比例信息
    * @param id 数据看盘比例id
    * @return
    */
    public R<VideoDataViewingRatioInfoVo> info(Long id) {

        VideoDataViewingRatioInfoVo videoDataViewingRatioInfoVo = videoDataViewingRatioProducer.info(id);
        return R.ok("获取成功", videoDataViewingRatioInfoVo);
    }

    /**
     * 新增数据看盘比例
     * @param videoDataViewingRatioBo 数据看盘比例对象
     * @return
     */
    public R<String> save(VideoDataViewingRatioBo videoDataViewingRatioBo) {

        VideoDataViewingRatioInfoVo videoDataViewingRatioInfoVo = videoDataViewingRatioProducer.save(videoDataViewingRatioBo);
        return R.ok("添加成功");
    }

    /**
     * 修改数据看盘比例
     * @param videoDataViewingRatioBo 数据看盘比例对象
     * @return
     */
    public R<String> update(VideoDataViewingRatioBo videoDataViewingRatioBo) {

        videoDataViewingRatioProducer.update(videoDataViewingRatioBo);
        return R.ok("修改成功");
    }

    /**
     * 删除数据看盘比例
     * @param id 数据看盘比例id
     * @return
     */
    public R<String> delete(Long id) {

        videoDataViewingRatioProducer.deleteById(id);
        return R.ok("删除成功");
    }


}

