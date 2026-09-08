package com.jiuyu.governance.business.performance.pojo.bo;

import com.jiuyu.governance.business.performance.pojo.entity.LiveSession;
import com.jiuyu.governance.business.performance.pojo.entity.LiveVideo;
import com.jiuyu.governance.business.room.pojo.entity.LiveRoom;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 视频处理上下文
 * 用于事务拆分后各阶段之间传递数据
 *
 * @author lj
 * @date 2026-03-27
 */
@Getter
@Setter
public class VideoProcessContext {

    /**
     * 待处理的视频列表
     */
    private List<LiveVideo> videos;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 批次号
     */
    private String batchNumber;

    /**
     * 直播间信息
     */
    private LiveRoom liveRoom;

    /**
     * 场次ID
     */
    private Long sessionId;

    /**
     * 场次信息
     */
    private LiveSession session;

    /**
     * OSS实时数据（合并后）
     */
    private List<OceanEngineProcessBo> processData;

    /**
     * 合并后上传到OSS的URL（由 mergeAndUploadVideoOssData 填充）
     */
    private String ossUrl;

    private VideoProcessContext() {
    }

    /**
     * 创建上下文
     */
    public static VideoProcessContext of(List<LiveVideo> videos) {
        VideoProcessContext ctx = new VideoProcessContext();
        ctx.setVideos(videos);
        if (videos != null && !videos.isEmpty()) {
            LiveVideo firstVideo = videos.get(0);
            ctx.setTenantId(firstVideo.getTenantId());
            ctx.setBatchNumber(firstVideo.getBatchNumber());
        }
        return ctx;
    }
}
