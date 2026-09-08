package com.jiuyu.replay.words.producer;

import com.jiuyu.replay.generic.utils.PageUtils;

import com.jiuyu.replay.words.vo.AiTrainListVo;
import com.jiuyu.replay.words.vo.AiTrainInfoVo;
import com.jiuyu.replay.words.bo.AiTrainBo;
import com.jiuyu.replay.words.bo.AiTrainListBo;


/**
 * AI训练
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-02-20 18:47:39
 */
public interface AiTrainProducer {


    /**
     * AI训练列表
     * @param aiTrainListBo AI训练列表查询参数
     * @return
     */
    PageUtils<AiTrainListVo> queryPage(AiTrainListBo aiTrainListBo);

    /**
    * AI训练信息
    * @param id AI训练id
    * @return
    */
    AiTrainInfoVo info(Long id);

    /**
     * 新增AI训练
     * @param aiTrainBo AI训练对象
     * @return
     */
     AiTrainInfoVo save(AiTrainBo aiTrainBo);

    /**
     * 修改AI训练
     * @param aiTrainBo AI训练对象
     * @return
     */
    void update(AiTrainBo aiTrainBo);

    /**
     * 删除AI训练
     * @param id AI训练id
     * @return
     */
    void deleteById(Long id);


    /**
     * 根据视频id获取ai训练信息
     * @param videoId 视频id
     * @return
     */
    AiTrainInfoVo getByVideoId(String videoId);

    /**
     * 将训练状态改为后台完成
     * @param id AI训练id
     * @return
     */
    void completeTrain(Long id);
}

