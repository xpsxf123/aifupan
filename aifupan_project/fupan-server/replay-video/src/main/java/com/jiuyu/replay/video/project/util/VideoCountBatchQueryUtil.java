package com.jiuyu.replay.video.project.util;

import cn.hutool.core.collection.CollectionUtil;
import com.jiuyu.replay.video.project.dao.VideoInfoDao;
import com.jiuyu.replay.video.project.entity.VideoHotSearchEntity;
import com.jiuyu.replay.video.project.entity.VideoUserHotSubscriptionEntity;
import com.jiuyu.replay.video.project.service.VideoInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;

/**
 * 视频数量批量查询工具类
 * 用于避免N+1查询问题，提供统一的批量查询接口
 *
 * @author RayChou
 * @since 2025-01-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VideoCountBatchQueryUtil {

    private final VideoInfoService videoInfoService;

    /**
     * 批量查询视频数量（通过searchId映射）
     *
     * @param subscriptions 订阅列表
     * @param searchIdMap   searchId映射，key为"platformType:keyword"，value为searchId
     * @return 视频数量缓存，key为"searchId:likeCount"，value为视频数量
     */
    public Map<String, Integer> batchQueryVideoCountsBySearchIdMap(
            List<VideoUserHotSubscriptionEntity> subscriptions,
            Map<String, Long> searchIdMap) {

        return batchQueryVideoCounts(subscriptions, subscription -> {
            String key = subscription.getPlatformType() + ":" + subscription.getKeyword();
            return searchIdMap.get(key);
        });
    }

    /**
     * 批量查询视频数量（通过VideoHotSearchEntity映射）
     *
     * @param subscriptions           订阅列表
     * @param videoHotSearchEntityMap VideoHotSearchEntity映射，key为"platformType:keyword"，value为VideoHotSearchEntity
     * @return 视频数量缓存，key为"searchId:likeCount"，value为视频数量
     */
    public Map<String, Integer> batchQueryVideoCountsByEntityMap(
            List<VideoUserHotSubscriptionEntity> subscriptions,
            Map<String, VideoHotSearchEntity> videoHotSearchEntityMap) {

        return batchQueryVideoCounts(subscriptions, subscription -> {
            String hotSearchKey = buildHotSearchKey(subscription.getPlatformType(), subscription.getKeyword());
            VideoHotSearchEntity entity = videoHotSearchEntityMap.get(hotSearchKey);
            return entity != null ? entity.getId() : null;
        });
    }

    /**
     * 批量查询视频数量的核心方法
     *
     * @param subscriptions     订阅列表
     * @param searchIdExtractor searchId提取器函数
     * @return 视频数量缓存，key为"searchId:likeCount"，value为视频数量
     */
    private Map<String, Integer> batchQueryVideoCounts(
            List<VideoUserHotSubscriptionEntity> subscriptions,
            Function<VideoUserHotSubscriptionEntity, Long> searchIdExtractor) {

        // 1. 收集所有需要查询的参数
        List<VideoInfoDao.VideoCountQuery> queryParams = new ArrayList<>();
        Set<String> uniqueQueries = new HashSet<>(); // 用于去重

        for (VideoUserHotSubscriptionEntity subscription : subscriptions) {
            Long searchId = searchIdExtractor.apply(subscription);

            if (searchId != null && subscription.getSubscriptionLikeCountThreshold() != null) {
                String queryKey = searchId + ":" + subscription.getSubscriptionLikeCountThreshold();

                // 去重：避免相同的searchId和likeCount重复查询
                if (uniqueQueries.add(queryKey)) {
                    queryParams.add(new VideoInfoDao.VideoCountQuery(searchId, subscription.getSubscriptionLikeCountThreshold()));
                }
            }
        }

        // 2. 批量查询视频数量
        if (CollectionUtil.isEmpty(queryParams)) {
            return new HashMap<>();
        }

        return videoInfoService.batchCountVideoNumberBySearchIdAndLikeCount(queryParams);
    }

    /**
     * 构建爆款搜索的key
     *
     * @param platformType 平台类型
     * @param keyword      关键词
     * @return 搜索key
     */
    private String buildHotSearchKey(Byte platformType, String keyword) {
        return platformType + "_" + keyword;
    }

    /**
     * 从视频数量缓存中获取指定的视频数量
     *
     * @param videoCountCache 视频数量缓存
     * @param searchId        搜索ID
     * @param likeCount       点赞数阈值
     * @return 视频数量，如果不存在则返回0
     */
    public Integer getVideoCount(Map<String, Integer> videoCountCache, Long searchId, Integer likeCount) {
        if (searchId == null || likeCount == null) {
            return 0;
        }
        String cacheKey = searchId + ":" + likeCount;
        return videoCountCache.getOrDefault(cacheKey, 0);
    }
}
