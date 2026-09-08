package com.jiuyu.replay.words.rse;

import com.jiuyu.replay.generic.dto.words.ShareVideoCloudDto;
import com.jiuyu.replay.generic.dto.words.UserVideoCountDto;
import com.jiuyu.replay.generic.vo.words.AnchorVideoInfoVo;
import com.jiuyu.replay.generic.vo.words.video.SubUserVideoListVo;

import java.util.List;
import java.util.Map;

public interface AnchorVideoRse {

    /**
     * 获取用户的视频
     * 
     * @param videoId 视频id
     * @param userId  用户id
     * @return
     */
    AnchorVideoInfoVo infoUserVideoByVideoId(String videoId, Long userId);

    /**
     * 分析视频到云空间
     *
     * @param shareVideoCloudDto 分享的视频信息
     * @return 分享地址
     */
    String shareVideoToCloud(ShareVideoCloudDto shareVideoCloudDto);

    /**
     * 根据用户ID列表获取每个用户本月的录制视频数量
     *
     * @param userIds 用户ID列表
     * @param tenantId 租户id
     * @return 用户视频数量列表
     */
    List<UserVideoCountDto> getMonthlyVideoCountByUserIdsAndTenantId(List<Long> userIds, Long tenantId);

    /**
     * 根据用户ID列表获取每个用户昨日的录制视频数量
     *
     * @param userIds 用户ID列表
     * @param tenantId 租户id
     * @return 用户昨日视频数量列表
     */
    List<UserVideoCountDto> getYesterdayVideoCountByUserIdsAndTenantId(List<Long> userIds, Long tenantId);

    /**
     * 根据用户id获取录制视频列表
     *
     * @param userId 用户id
     * @param tenantId 租户id
     * @return 录制视频列表
     */
    List<SubUserVideoListVo> getVideoListByUserIdAndTenantId(Long userId, Long tenantId);

    /**
     * 批量将视频的本地视频删除标识改为已删除
     *
     * @param videoIds 视频videoId集合
     * @return 录制视频列表
     */
    void deleteLocalVideoByIds(List<String> videoIds);

    /**
     * 统计用户录制的视频场次
     * @param userId 用户id
     * @param tenantId 租户id
     */
    Long countUserVideo(Long userId, Long tenantId);

    /**
     * 根据主播secUid集合和时长要求获取每个主播最新的一条视频
     *
     * @param secUids  主播secUid集合
     * @param duration 视频时长要求（秒）
     * @return 主撫secUid和视频videoId的映射
     */
    Map<String, String> listLatestVideoBySecUidsAndDuration(List<String> secUids, Integer duration);

    /**
     * 更新视频的云空间重命名
     *
     * @param videoId     视频id
     * @param cloudRename 云空间重命名
     */
    void updateCloudRename(String videoId, String cloudRename);
}
