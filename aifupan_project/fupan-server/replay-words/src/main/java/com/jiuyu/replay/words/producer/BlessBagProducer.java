package com.jiuyu.replay.words.producer;

import com.jiuyu.replay.generic.utils.PageUtils;

import com.jiuyu.replay.generic.vo.words.BlessBagListVo;
import com.jiuyu.replay.generic.vo.words.BlessBagInfoVo;
import com.jiuyu.replay.words.bo.BlessBagBo;
import com.jiuyu.replay.words.bo.BlessBagListBo;

import java.util.List;


/**
 * 福袋信息
 *
 * @author lyw
 * @email 1798883178@qq.com
 * @date 2025-05-22 20:02:21
 */
public interface BlessBagProducer {


    /**
     * 福袋信息列表
     * @param blessBagListBo 福袋信息列表查询参数
     * @return
     */
    PageUtils<BlessBagListVo> queryPage(BlessBagListBo blessBagListBo);

    /**
    * 福袋信息信息
    * @param id 福袋信息id
    * @return
    */
    BlessBagInfoVo info(Long id);

    List<BlessBagInfoVo> infoByVideo(String videoId);

    /**
     * 新增福袋信息
     * @param blessBagBo 福袋信息对象
     * @return
     */
     BlessBagInfoVo save(BlessBagBo blessBagBo);

    /**
     * 修改福袋信息
     * @param blessBagBo 福袋信息对象
     * @return
     */
    void update(BlessBagBo blessBagBo);

    /**
     * 删除福袋信息
     * @param id 福袋信息id
     * @return
     */
    void deleteById(Long id);

    /**
     * 根据视频id查询福袋信息
     * @param videoId
     * @return
     */
    List<BlessBagInfoVo> listByVideoId(String videoId);
}

