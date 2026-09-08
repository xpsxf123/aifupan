package com.jiuyu.replay.video.project.service;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jiuyu.replay.video.project.bo.hotsearch.VideoHotSearchListQueryBo;
import com.jiuyu.replay.video.project.entity.VideoHotSearchRelationEntity;
import com.jiuyu.replay.video.project.vo.hotsearch.VideoHotSearchResultVo;

/**
 * <p>
 * 爆款搜索关联表 服务类
 * </p>
 *
 * @author RayChou
 * @since 2025-08-29
 */
public interface VideoHotSearchRelationService extends IService<VideoHotSearchRelationEntity> {

    /**
     * 连表查询爆款搜索结果
     *
     * @param queryBo  查询条件
     * @param userId   用户ID
     * @param tenantId 租户ID
     * @return 分页结果
     */
    Page<VideoHotSearchResultVo> queryHotSearchResultsWithJoin(VideoHotSearchListQueryBo queryBo, Long userId, Long tenantId);

    /**
     * 查询爆款搜索记录（支持动态排序）
     *
     * @param page         分页对象
     * @param snapshotId   快照ID
     * @param tenantId     租户ID
     * @param sortCode     排序类型标识 1：默认排序 2：点赞数排序 3：评论数排序 4：转发数排序 5：收藏数排序
     * @param sortSequence 排序顺序 0：降序 1：升序
     * @return 分页结果
     */
    Page<VideoHotSearchResultVo> queryHotSearchRecord(Page<VideoHotSearchResultVo> page, Long snapshotId, Long tenantId, Byte sortCode, Byte sortSequence);
}
