package com.jiuyu.replay.words.api;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jiuyu.replay.generic.dto.words.UserVideoCountDto;
import com.jiuyu.replay.generic.feign.words.AnchorVideoFeign;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.video.VideoSliceVo;
import com.jiuyu.replay.generic.vo.words.AnchorVideoInfoVo;
import com.jiuyu.replay.words.entity.SocketCollectMessageEntity;
import com.jiuyu.replay.words.producer.AnchorVideoProducer;
import com.jiuyu.replay.words.repository.service.SocketCollectMessageService;
import com.jiuyu.replay.words.rse.AnchorVideoRse;
import com.jiuyu.replay.words.rse.VideoSliceRse;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/5/29 下午3:23
 */
@Component
@AllArgsConstructor
public class AnchorVideoApi implements AnchorVideoFeign {

    private final AnchorVideoProducer anchorVideoProducer;
    private final AnchorVideoRse anchorVideoRse;
    private final VideoSliceRse videoSliceRse;
    /**
     * 跳过 SocketCollectMessageProducer 直注 Service：Producer 依赖 TableStoreFeign，
     * 而 TableStoreBll 反向依赖 AnchorVideoFeign（本 Api），会形成循环依赖。
     * Service 是纯 IService(Entity)，无跨域依赖，安全。
     */
    private final SocketCollectMessageService socketCollectMessageService;

    @Override
    public R<AnchorVideoInfoVo> GetByVideoId(String videoId) {
        return R.ok(anchorVideoProducer.getByVideoId(videoId));
    }

    @Override
    public R<List<UserVideoCountDto>> getMonthlyVideoCountByUserIdsAndTenantId(List<Long> userIds, Long tenantId) {
        return R.ok(anchorVideoRse.getMonthlyVideoCountByUserIdsAndTenantId(userIds, tenantId));
    }

    @Override
    public R<List<UserVideoCountDto>> getYesterdayVideoCountByUserIdsAndTenantId(List<Long> userIds, Long tenantId) {
        return R.ok(anchorVideoRse.getYesterdayVideoCountByUserIdsAndTenantId(userIds, tenantId));
    }

    @Override
    public VideoSliceVo getVideoSliceBySourceId(String sourceId, Integer sourceType) {
        return videoSliceRse.getVideoSliceBySourceId(sourceId, sourceType);
    }

    @Override
    public AnchorVideoInfoVo getParentVideoByVideoId(String videoId) {
        return anchorVideoProducer.getParentVideoByVideoId(videoId);
    }

    @Override
    public R<List<AnchorVideoInfoVo>> listByVideoIds(List<String> videoIds) {
        return R.ok(anchorVideoProducer.listByVideoIds(videoIds));
    }

    @Override
    public R<Boolean> hasBarrage(String videoId) {
        // existBarrage 是运行时聚合字段（tb_anchor_video 表无此列），按 socket 采集到的弹幕总数判定
        SocketCollectMessageEntity one = socketCollectMessageService.getOne(
                new LambdaQueryWrapper<SocketCollectMessageEntity>()
                        .eq(SocketCollectMessageEntity::getVideoId, videoId)
                        .last("limit 1"));
        boolean exist = one != null
                && one.getTotalBarrageNum() != null
                && one.getTotalBarrageNum() > 0;
        return R.ok(exist);
    }

    /**
     * 判断指定用户是否录制过视频(仅限已分析)
     *
     * @param userId      用户id
     * @param tenantId    租户id
     * @param minDuration 最短录制时长
     *
     * @return true 录制过视频；false 未录制过视频
     */
    @Override
    public boolean hasRecord(long userId, long tenantId, int minDuration) {
        return anchorVideoProducer.hasRecord(userId, tenantId, minDuration);
    }
}
