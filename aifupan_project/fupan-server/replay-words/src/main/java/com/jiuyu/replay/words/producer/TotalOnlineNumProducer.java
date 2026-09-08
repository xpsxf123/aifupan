package com.jiuyu.replay.words.producer;

import com.jiuyu.replay.generic.utils.PageUtils;

import com.jiuyu.replay.words.entity.TotalOnlineNumEntity;
import com.jiuyu.replay.words.vo.TotalOnlineNumListVo;
import com.jiuyu.replay.words.vo.TotalOnlineNumInfoVo;
import com.jiuyu.replay.words.bo.TotalOnlineNumBo;
import com.jiuyu.replay.words.bo.TotalOnlineNumListBo;

import java.util.List;


/**
 * 直播总观看人次
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-11-14 10:05:41
 */
public interface TotalOnlineNumProducer {


    /**
     * 直播总观看人次列表
     * @param totalOnlineNumListBo 直播总观看人次列表查询参数
     * @return
     */
    PageUtils<TotalOnlineNumListVo> queryPage(TotalOnlineNumListBo totalOnlineNumListBo);

    /**
    * 直播总观看人次信息
    * @param id 直播总观看人次id
    * @return
    */
    TotalOnlineNumInfoVo info(Long id);

    /**
     * 新增直播总观看人次
     * @param totalOnlineNumBo 直播总观看人次对象
     * @return
     */
     TotalOnlineNumInfoVo save(TotalOnlineNumBo totalOnlineNumBo);

    /**
     * 修改直播总观看人次
     * @param totalOnlineNumBo 直播总观看人次对象
     * @return
     */
    void update(TotalOnlineNumBo totalOnlineNumBo);

    /**
     * 删除直播总观看人次
     * @param id 直播总观看人次id
     * @return
     */
    void deleteById(Long id);


    /**
     * 根据直播场次号获取信息
     * @param batchNumber 直播场次号
     * @return
     */
    TotalOnlineNumInfoVo infoByBatchNumber(String batchNumber);

    /**
     * 根据用户id和场次号获取信息
     * @param userId 用户id
     * @param batchNumber 场次号
     * @return
     */
    TotalOnlineNumInfoVo infoByUserIdAndBatchNumber(Long userId, String batchNumber);

    List<TotalOnlineNumEntity> listAllNum();
}

