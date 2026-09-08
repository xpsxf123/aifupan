package com.jiuyu.replay.video.project.dao;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jiuyu.replay.video.project.bo.hotsearch.VideoHotSearchListQueryBo;
import com.jiuyu.replay.video.project.entity.VideoHotSearchRelationEntity;
import com.jiuyu.replay.video.project.vo.hotsearch.VideoHotSearchResultVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 爆款搜索关联表 Mapper 接口
 * </p>
 *
 * @author RayChou
 * @since 2025-08-29
 */
@Mapper
public interface VideoHotSearchRelationDao extends BaseMapper<VideoHotSearchRelationEntity> {

    /**
     * 连表查询爆款搜索结果
     *
     * @param page     分页对象
     * @param queryBo  查询条件
     * @param userId   用户ID
     * @param tenantId 租户ID
     * @return 分页结果
     */

    Page<VideoHotSearchResultVo> queryHotSearchResultsWithJoin(Page<VideoHotSearchResultVo> page,
                                                               @Param("queryBo") VideoHotSearchListQueryBo queryBo,
                                                               @Param("userId") Long userId,
                                                               @Param("tenantId") Long tenantId);

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
    Page<VideoHotSearchResultVo> queryHotSearchRecord(Page<VideoHotSearchResultVo> page,
                                                      @Param("snapshotId") Long snapshotId,
                                                      @Param("tenantId") Long tenantId,
                                                      @Param("sortCode") Byte sortCode,
                                                      @Param("sortSequence") Byte sortSequence);

}
