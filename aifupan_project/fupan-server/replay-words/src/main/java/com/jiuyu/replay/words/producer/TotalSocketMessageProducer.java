package com.jiuyu.replay.words.producer;

import com.jiuyu.replay.words.vo.TotalSocketMessageInfoVo;
import com.jiuyu.replay.words.bo.TotalSocketMessageBo;


/**
 * 直播场次的websocket记录统计
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2025-01-04 15:15:15
 */
public interface TotalSocketMessageProducer {


    /**
    * 直播场次的websocket记录统计信息
    * @param id 直播场次的websocket记录统计id
    * @return
    */
    TotalSocketMessageInfoVo info(Long id);

    /**
     * 新增直播场次的websocket记录统计
     * @param totalSocketMessageBo 直播场次的websocket记录统计对象
     * @return
     */
     TotalSocketMessageInfoVo save(TotalSocketMessageBo totalSocketMessageBo);

    /**
     * 修改直播场次的websocket记录统计
     * @param totalSocketMessageBo 直播场次的websocket记录统计对象
     * @return
     */
    void update(TotalSocketMessageBo totalSocketMessageBo);

    /**
     * 删除直播场次的websocket记录统计
     * @param id 直播场次的websocket记录统计id
     * @return
     */
    void deleteById(Long id);


}

