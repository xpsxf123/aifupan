package com.jiuyu.governance.business.performance.service;

import com.jiuyu.governance.business.performance.pojo.bo.VideoProcessContext;
import com.jiuyu.governance.business.room.pojo.entity.LiveRoom;

/**
 * 场次服务接口
 *
 * @author lj
 * @date 2026-03-24
 */
public interface LiveSessionService {

    /**
     * 根据租户ID和secUid查找直播间
     *
     * @param tenantId 租户ID
     * @param secUid   secUid
     * @return LiveRoom
     */
    LiveRoom findLiveRoom(Long tenantId, String secUid);

    /**
     * 加载视频OSS过程数据，结果填充至 ctx.processData 和 ctx.ossUrl
     * <p>
     * 一个场次仅一条 live_video 记录，直接使用其 videoOssUrl，不再合并上传。
     * 耗时操作，应在事务外调用。
     * </p>
     *
     * @param ctx 视频处理上下文
     */
    void loadVideoOssData(VideoProcessContext ctx);

    void saveInTransaction(VideoProcessContext ctx);

    void calculatePerformanceInTransaction(VideoProcessContext ctx);
}
