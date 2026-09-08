package com.jiuyu.replay.words.producer;

import com.jiuyu.replay.generic.utils.PageUtils;

import com.jiuyu.replay.words.vo.OnlineNumListVo;
import com.jiuyu.replay.words.vo.OnlineNumInfoVo;
import com.jiuyu.replay.words.bo.OnlineNumBo;
import com.jiuyu.replay.words.bo.OnlineNumListBo;

import java.util.List;


/**
 * 直播实时在线人数
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-11-14 10:05:41
 */
public interface OnlineNumProducer {


    /**
     * 直播实时在线人数列表
     * @param onlineNumListBo 直播实时在线人数列表查询参数
     * @return
     */
    PageUtils<OnlineNumListVo> queryPage(OnlineNumListBo onlineNumListBo);

    /**
    * 直播实时在线人数信息
    * @param id 直播实时在线人数id
    * @return
    */
    OnlineNumInfoVo info(Long id);

    /**
     * 新增直播实时在线人数
     * @param onlineNumBo 直播实时在线人数对象
     * @return
     */
     OnlineNumInfoVo save(OnlineNumBo onlineNumBo);

    /**
     * 修改直播实时在线人数
     * @param onlineNumBo 直播实时在线人数对象
     * @return
     */
    void update(OnlineNumBo onlineNumBo);

    /**
     * 删除直播实时在线人数
     * @param id 直播实时在线人数id
     * @return
     */
    void deleteById(Long id);


    /**
     * 根据视频id获取在线人数列表
     * @param videoId 视频id
     * @return
     */
    List<OnlineNumInfoVo> listByVideoId(String videoId);

    /**
     * 根据用户id和场次号查找
     * @param userId 用户id
     * @param batchNumber 场次号
     * @return
     */
    OnlineNumInfoVo infoByUserIdAndBatchNumber(Long userId, String batchNumber);

    /**
     * 根据视频id获取在线人数列表
     * @param userId 用户id
     * @param batchNumber 场次号
     * @return
     */
    List<OnlineNumInfoVo> listByUserIdAndBatchNumber(Long userId, String batchNumber);

}

