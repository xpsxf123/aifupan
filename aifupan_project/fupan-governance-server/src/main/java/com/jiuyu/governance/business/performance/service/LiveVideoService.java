package com.jiuyu.governance.business.performance.service;

import com.jiuyu.governance.business.performance.pojo.bo.PendingVideoGroup;
import com.jiuyu.governance.business.performance.pojo.entity.LiveVideo;
import com.jiuyu.governance.business.performance.pojo.request.ClientPushVideoRequest;

import java.util.List;

/**
 * 视频服务接口
 *
 * @author lj
 * @date 2026-03-19
 */
public interface LiveVideoService {

    /**
     * 重置超时的处理中视频为待处理状态
     *
     * @param timeoutMinutes 超时时间（分钟）
     * @return 是否执行成功
     */
    boolean resetTimeoutProcessingVideos(int timeoutMinutes);

    /**
     * 获取待处理视频分组列表（按 tenantId + batchNumber 分组）
     *
     * @param limit 限制数量
     * @return 待处理视频分组列表
     */
    List<PendingVideoGroup> getPendingVideoGroups(int limit);

    /**
     * 根据租户ID和直播批次号获取待处理视频列表，并将状态更新为处理中
     *
     * @param tenantId    租户ID
     * @param batchNumber 直播批次号
     * @return 待处理视频列表
     */
    List<LiveVideo> fetchAndLockPendingVideos(Long tenantId, String batchNumber);

    /**
     * 更新视频处理结果为成功
     *
     * @param videoId 视频ID
     * @return 是否更新成功
     */
    boolean updateProcessSuccess(Long videoId);

    /**
     * 更新视频处理结果为失败
     *
     * @param videoId    视频ID
     * @param failReason 失败原因
     * @return 是否更新成功
     */
    boolean updateProcessFailed(Long videoId, String failReason);

    /**
     * 批量更新视频处理结果为成功
     *
     * @param videoIds 视频ID列表
     * @return 是否更新成功
     */
    boolean batchUpdateProcessSuccess(List<Long> videoIds);

    /**
     * 批量更新视频处理结果为失败
     *
     * @param videoIds   视频ID列表
     * @param failReason 失败原因
     * @return 是否更新成功
     */
    boolean batchUpdateProcessFailed(List<Long> videoIds, String failReason);

    /**
     * 客户端推送视频数据
     *
     * @param request 请求数据
     */
    void clientPushVideo(ClientPushVideoRequest request);

    /**
     * 判断指定租户和批次号是否已存在 live_video 记录（即是否已拉取过数据）
     *
     * @param tenantId    租户ID
     * @param batchNumber 直播批次号
     * @return true=已拉取
     */
    boolean hasBatchNumber(Long tenantId, String batchNumber);
}
