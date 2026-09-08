package com.jiuyu.replay.generic.feign.words;

import com.jiuyu.replay.generic.dto.words.UserVideoCountDto;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.video.VideoSliceVo;
import com.jiuyu.replay.generic.vo.words.AnchorVideoInfoVo;

import java.util.List;

/**
 * 视频的接口
 */
public interface AnchorVideoFeign {

    R<AnchorVideoInfoVo> GetByVideoId(String videoId);

    /**
     * 根据用户ID列表获取每个用户本月的录制视频数量
     *
     * @param userIds 用户ID列表
     * @param tenantId 租户id
     * @return 用户视频数量列表
     */
    R<List<UserVideoCountDto>> getMonthlyVideoCountByUserIdsAndTenantId(List<Long> userIds, Long tenantId);

    /**
     * 根据用户ID列表获取每个用户昨日的录制视频数量
     *
     * @param userIds 用户ID列表、
     * @param tenantId 租户id
     * @return 用户昨日视频数量列表
     */
    R<List<UserVideoCountDto>> getYesterdayVideoCountByUserIdsAndTenantId(List<Long> userIds, Long tenantId);

    /**
     * 根据源id和源类型获取切片
     *
     * @param sourceId   据源id
     * @param sourceType 源类型
     * @return 切片
     */
    VideoSliceVo getVideoSliceBySourceId(String sourceId, Integer sourceType);

    /**
     * 根据视频id获取父视频信息
     *
     * @param videoId 视频id
     * @return 父视频信息
     */
    AnchorVideoInfoVo getParentVideoByVideoId(String videoId);

    /**
     * 根据视频id集合批量获取视频信息
     *
     * @param videoIds 视频id集合
     * @return 视频信息列表
     */
    R<List<AnchorVideoInfoVo>> listByVideoIds(List<String> videoIds);

    /**
     * 判断指定视频是否采集到弹幕（依据 tb_socket_collect_message.total_barrage_num &gt; 0）
     *
     * <p>说明：existBarrage 不是 tb_anchor_video 的物理列，而是运行时按 socket 采集结果计算。
     * 单视频查询场景（GetByVideoId）不会填充该字段，需通过本方法显式判定。</p>
     *
     * @param videoId 视频id
     * @return true 有弹幕；false 无弹幕或未采集到 socket 数据
     */
    R<Boolean> hasBarrage(String videoId);


    /**
     * 判断指定用户是否录制过视频(仅限已分析)
     *
     * @param userId    用户id
     * @param tenantId  租户id
     * @param minDuration 最短录制时长
     * @return true 录制过视频；false 未录制过视频
     */
    boolean hasRecord(long userId, long tenantId, int minDuration);
}
