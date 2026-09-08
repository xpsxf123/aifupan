package com.jiuyu.replay.words.producer;

import com.jiuyu.replay.generic.utils.PageUtils;

import com.jiuyu.replay.words.bo.SocketDataBo;
import com.jiuyu.replay.words.bo.UploadSocketDataBo;
import com.jiuyu.replay.words.vo.*;
import com.jiuyu.replay.words.bo.SocketCollectMessageBo;
import com.jiuyu.replay.words.bo.SocketCollectMessageListBo;

import java.util.Collection;
import java.util.List;
import java.util.Map;


/**
 * websocket采集的信息
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-12-03 15:37:56
 */
public interface SocketCollectMessageProducer {


    /**
     * websocket采集的信息列表
     * @param socketCollectMessageListBo websocket采集的信息列表查询参数
     * @return
     */
    PageUtils<SocketCollectMessageListVo> queryPage(SocketCollectMessageListBo socketCollectMessageListBo);

    /**
    * websocket采集的信息信息
    * @param id websocket采集的信息id
    * @return
    */
    SocketCollectMessageInfoVo info(Long id);

    /**
     * 新增websocket采集的信息
     * @param socketCollectMessageBo websocket采集的信息对象
     * @return
     */
     SocketCollectMessageInfoVo save(SocketCollectMessageBo socketCollectMessageBo);

    /**
     * 修改websocket采集的信息
     *
     * @param socketCollectMessageBo websocket采集的信息对象
     * @return
     */
    SocketCollectMessageInfoVo update(SocketCollectMessageBo socketCollectMessageBo);

    /**
     * 删除websocket采集的信息
     * @param id websocket采集的信息id
     * @return
     */
    void deleteById(Long id);

    /**
     * 获取当前批次的采集信息
     * @return
     */
    SocketCollectMessageInfoVo getByBatch(String batchNumber, Long userId, String videoId);
    /**
     * 获取当前批次的采集信息
     * @return
     */
    SocketCollectMessageInfoVo getByBatch(String batchNumber, String videoId);

    /**
     * 获取当前视频的采集信息
     * @param videoId
     * @return
     */
    SocketCollectMessageInfoVo getByVideoId(String videoId);

    /**
     * 获取当前批次的在线人数
     *
     * @param userId
     * @param batchNumber
     * @param videoId
     * @return
     */
    List<OnlineNumInfoVo> onlineNumByUserIdAndBatchNumber(Long userId, String batchNumber, String videoId);

    /**
     * 判断当前批次是否已经存在采集数据
     * @param batchNumber
     * @param userId
     * @param videoId
     * @return
     */
    Boolean socketDataExist(String batchNumber, Long userId, String videoId);

    /**
     * 上传websocket采集的数据
     * @param bo
     */
    void uploadSocketData(UploadSocketDataBo bo);

    /**
     * 获取前两天的累计观看人数和场观
     * @param userId
     * @return
     */
    SynchronizeTwoDayVideoVo synchronizeTwoDayVideo(Long userId);

    /**
     * 根据批次号查询时间
     *
     * @param batchNumberList
     * @return
     */
    List<TotalSocketMessageVo> queryTimeByBatchNumber(List<String> batchNumberList);

    /**
     * 获取websocket数据
     * @param videoId
     * @param cosKey
     * @return
     * @throws Exception
     */
    SocketDataBo getWebsocketData(String videoId, String cosKey);

    /**
     * 设置websocket数据到redis
     * @param videoId
     * @param value
     */
    void setWebsocketRedisData(String videoId, String value);

    /**
     * 根据视频ids查询websocket数据
     * @param videoIds
     * @return
     */
    List<SocketCollectMessageInfoVo> listByVideoIds(List<String> videoIds);

    /**
     * 更新视频的弹幕数量
     * @param batchNumber
     * @param userId
     * @param videoId
     * @param totalBarrageNum
     */
    void updateTotalBarrageNumByVideoId(String batchNumber, Long userId, String videoId, Integer totalBarrageNum);

    /**
     * 拷贝视频socket数据到切片视频
     * @param sourceVideoId 原视频id
     * @param sliceVideoId 切片视频id
     * @param sliceStartTime 切片开始时间戳
     * @param sliceEndTime 切片结束时间戳
     * @param tenantId 租户id
     */
    void copyVideoDataToSlice(String sourceVideoId, String sliceVideoId, Long sliceStartTime, Long sliceEndTime, Long tenantId);

    /**
     * 获取视频是否有弹幕
     * @param videoIds
     * @return
     */
    Map<String, Boolean> getVideoHasBarragesMap(Collection<String> videoIds);


    Map<String, SocketCollectMessageVo> getVideoHasChartDataMap(Collection<String> videoIds);
}

