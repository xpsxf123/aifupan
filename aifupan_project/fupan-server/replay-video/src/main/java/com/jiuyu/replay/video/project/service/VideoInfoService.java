package com.jiuyu.replay.video.project.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jiuyu.replay.video.project.bo.hotsearch.VideoHotSearchListSubscriptionQueryBo;
import com.jiuyu.replay.video.project.dao.VideoInfoDao;
import com.jiuyu.replay.video.project.entity.VideoInfoEntity;
import com.jiuyu.replay.video.project.vo.hotsearch.VideoHotSearchResultVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 视频基础信息表服务类
 *
 * @author RayChou
 * @description 视频信息的业务逻辑接口，提供原子性的保存或更新操作
 * @since 2025-08-13
 */
public interface VideoInfoService extends IService<VideoInfoEntity> {

    /**
     * 批量保存或更新视频信息（原子性操作）
     * 基于platformType和platformVideoId组合判断唯一性
     *
     * @param videoEntities 视频信息实体列表
     * @param isSyncDaily   是否同步每日数据
     */
    void batchSaveOrUpdate(List<VideoInfoEntity> videoEntities, Boolean isSyncDaily);

    /**
     * 单个保存或更新视频信息（原子性操作）
     * 基于platformType和platformVideoId组合判断唯一性
     *
     * @param videoEntity 视频信息实体
     * @return 保存或更新后的视频信息实体
     */
    VideoInfoEntity singleSaveOrUpdate(VideoInfoEntity videoEntity);

    /**
     * 查询视频信息列表根据视频ID和条件
     *
     * @param tenantId                       租户id
     * @param searchId                       搜索ID
     * @param queryBo                        查询条件
     * @param subscriptionLikeCountThreshold 订阅点赞阈值
     * @return
     */
    Page<VideoHotSearchResultVo> queryVideoListByIdsAndCondition(Long tenantId, Long searchId, VideoHotSearchListSubscriptionQueryBo queryBo, Integer subscriptionLikeCountThreshold);

    /**
     * 统计视频总数根据搜索Id和点赞量
     *
     * @param searchId  搜索ID
     * @param likeCount 点赞量
     * @return
     */
    Integer countVideoNumberBySearchIdAndLikeCount(@Param("searchId") Long searchId, @Param("likeCount") Integer likeCount);

    /**
     * 批量根据搜索ID和点赞数统计视频数量
     *
     * @param queryParams 查询参数列表
     * @return 查询结果Map，key为"searchId:likeCount"，value为视频数量
     */
    Map<String, Integer> batchCountVideoNumberBySearchIdAndLikeCount(List<VideoInfoDao.VideoCountQuery> queryParams);

    /**
     * 批量根据达人平台用户ID统计视频数量
     *
     * @param authorIds 达人平台用户ID列表
     * @return 查询结果Map，key为authorId，value为视频数量
     */
    Map<String, Integer> batchCountVideoNumberByAuthorIds(List<String> authorIds);
}
