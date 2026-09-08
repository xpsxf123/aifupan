package com.jiuyu.replay.api.logic.words;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.SocketCollectMessageBo;
import com.jiuyu.replay.words.bo.SocketCollectMessageListBo;
import com.jiuyu.replay.words.bo.UploadSocketDataBo;
import com.jiuyu.replay.words.vo.*;

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
public interface SocketCollectMessageLogic {


    /**
     * websocket采集的信息列表
     * @param socketCollectMessageListBo websocket采集的信息列表查询参数
     * @return
     */
    R<PageUtils<SocketCollectMessageListVo>> queryPage(SocketCollectMessageListBo socketCollectMessageListBo);

    /**
    * websocket采集的信息信息
    * @param id websocket采集的信息id
    * @return
    */
    R<SocketCollectMessageInfoVo> info(Long id);

    /**
     * 新增websocket采集的信息
     * @param socketCollectMessageBo websocket采集的信息对象
     * @return
     */
    R<String> save(SocketCollectMessageBo socketCollectMessageBo);

    /**
     * 修改websocket采集的信息
     * @param socketCollectMessageBo websocket采集的信息对象
     * @return
     */
    R<String> update(SocketCollectMessageBo socketCollectMessageBo);

    /**
     * 删除websocket采集的信息
     * @param id websocket采集的信息id
     * @return
     */
    R<String> delete(Long id);


    /**
     * 采集信息是否存在
     * @param batchNumber
     * @param userId
     * @param videoId
     * @return
     */
    R<Boolean> socketDataExist(String batchNumber, Long userId, String videoId);

    /**
     * 获取在线人数列表
     * @param batchNumber 场次号
     * @param userId 用户id
     * @param videoId 视频唯一标识
     * @return
     */
    R<List<OnlineNumInfoVo>> getOnlineNumList(String batchNumber, Long userId, String videoId) throws Exception;

    /**
     * 上传采集信息
     * @param bo
     * @return
     */
    R<String> uploadSocketData(UploadSocketDataBo bo);

    /**
     * 获取前两天的累计观看人数和场观
     * @param userId
     * @return
     */
    R<SynchronizeTwoDayVideoVo> synchronizeTwoDayVideo(Long userId);

    /**
     * 采集信息详情-不带json
     *
     * @param userId
     * @param videoId
     * @param batchNumber
     * @return
     */
    R<SocketCollectMessageInfoVo> socketMessageInfoNotJson(Long userId, String videoId, String batchNumber);

    /**
     * 获取云空间的弹幕数据曲线
     * @param videoId 视频id
     * @return 数据统计
     */
    R<OnlineChartVo> onlineChartData(String videoId);

    /**
     * 获取云空间的弹幕数据曲线
     *
     * @param videoId 视频id
     * @param step    间隔
     * @return 数据统计
     */
    R<OnlineChartVo> onlineChartData(String videoId, Integer step);

    /**
     * 获取视频是否包含弹幕
     *
     * @param videoIds 视频id
     * @return 视频是否包含弹幕
     */
    Map<String, Boolean> getVideoHasBarragesMap(Collection<String> videoIds);

    /**
     * 获取视频在线曲线关联的采集统计数据（累计场观/场观/弹幕总数/最高在线）。
     *
     * @param videoIds 视频id
     * @return 视频id与采集统计数据映射，能查出即代表存在在线曲线数据
     */
    Map<String, SocketCollectMessageVo> getVideoHasChartDataMap(Collection<String> videoIds);
}


