package com.jiuyu.replay.generic.feign.words;

import com.jiuyu.replay.generic.vo.words.BlessBagInfoVo;

import java.util.List;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/6/11 下午6:34
 */
public interface BlessBagFeign {

    /**
     * 根据视频id查询福袋信息
     * @param videoId
     * @return
     */
    List<BlessBagInfoVo> listByVideoId(String videoId);

}
