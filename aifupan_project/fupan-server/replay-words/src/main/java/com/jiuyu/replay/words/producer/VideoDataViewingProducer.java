package com.jiuyu.replay.words.producer;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.words.bo.viewing.LiveRoomBo;
import com.jiuyu.replay.words.bo.viewing.UseDataViewingPropertyBo;
import com.jiuyu.replay.words.bo.viewing.VideoDataViewingBo;
import com.jiuyu.replay.words.bo.viewing.VideoDataViewingListBo;
import com.jiuyu.replay.words.entity.VideoDataViewingEntity;
import com.jiuyu.replay.words.vo.viewing.VideoDataViewingInfoVo;
import com.jiuyu.replay.words.vo.viewing.VideoDataViewingListVo;

import java.util.List;


/**
 * 视频看盘数据
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-04-12 16:31:15
 */
public interface VideoDataViewingProducer {


    /**
     * 视频看盘数据列表
     * @param videoDataViewingListBo 视频看盘数据列表查询参数
     * @return
     */
    PageUtils<VideoDataViewingListVo> queryPage(VideoDataViewingListBo videoDataViewingListBo);

    /**
    * 视频看盘数据信息
    * @param id 视频看盘数据id
    * @return
    */
    VideoDataViewingInfoVo info(Long id);

    /**
     * 新增视频看盘数据
     * @param videoDataViewingBo 视频看盘数据对象
     * @return
     */
     VideoDataViewingInfoVo save(VideoDataViewingBo videoDataViewingBo);

    /**
     * 修改视频看盘数据
     * @param videoDataViewingBo 视频看盘数据对象
     * @return
     */
    void update(VideoDataViewingBo videoDataViewingBo);

    /**
     * 删除视频看盘数据
     * @param id 视频看盘数据id
     * @return
     */
    void deleteById(Long id);

    /**
     * 查询本地数据库有没有对应的数据，如果有就直接创建数据看盘记录
     * @param videoId 视频id
     * @param anchorNumber 主播抖音号
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param batchNumber 直播场次号
     * @param userId 用户id
     * @param tenantId 租户id
     * @return
     */
    Boolean checkLocalDBExistCreate(String videoId, String anchorNumber, long startTime, long endTime, String batchNumber, Long userId, Long tenantId);

    /**
     * 处理第三方数据平台回调数据
     * @param liveRoomBo 第三方数据平台回调数据
     * @param batchNumber 直播场次号
     * @param requestId 请求id
     */
    VideoDataViewingEntity saveDataViewingCallbackData(LiveRoomBo liveRoomBo, String batchNumber, String requestId);

    /**
     * 查询正在拉取状态的数据，匹配上则修改状态
     * @param videoDataViewingEntity 数据看盘数据
     * @param anchorNumber 主播抖音号
     * @param batchNumber 直播场次号
     * @param dataStatus 数据状态 0：正常数据 1：异常数据但已校验完成 2：异常数据，未校验 3：未校验
     */
    List<UseDataViewingPropertyBo> checkLocalDbExistUpdateStatus(VideoDataViewingEntity videoDataViewingEntity, String anchorNumber, String batchNumber, Integer dataStatus);
}

