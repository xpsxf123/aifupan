package com.jiuyu.replay.api.logic.words;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

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
public interface AiTrainLogic {


    /**
     * AI训练列表
     * @param aiTrainListBo AI训练列表查询参数
     * @return
     */
    R<PageUtils<AiTrainListVo>> queryPage(AiTrainListBo aiTrainListBo);

    /**
    * AI训练信息
    * @param id AI训练id
    * @return
    */
    R<AiTrainInfoVo> info(Long id);

    /**
     * 新增AI训练
     * @param aiTrainBo AI训练对象
     * @return
     */
    R<String> save(AiTrainBo aiTrainBo);

    /**
     * 修改AI训练
     * @param aiTrainBo AI训练对象
     * @return
     */
    R<String> update(AiTrainBo aiTrainBo);

    /**
     * 删除AI训练
     * @param id AI训练id
     * @return
     */
    R<String> delete(Long id);


    /**
     * 根据视频id获取AI训练信息
     * @param videoId 视频id
     * @return
     */
    R<AiTrainInfoVo> infoByVideoId(String videoId);

    /**
     * 后台完成训练
     * @param id AI训练id
     * @return
     */
    R<String> completeTrain(Long id);
}

