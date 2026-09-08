package com.jiuyu.replay.video.project.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuyu.replay.video.project.entity.VideoHotSearchEmailUsageLogEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 热搜视频邮箱账号使用记录 DAO
 *
 * @author RayChou
 * @date 2025-11-28
 * @description 热搜邮箱账号使用日志数据访问层，提供使用记录的增删改查操作
 */
@Mapper
public interface VideoHotSearchEmailUsageLogDao extends BaseMapper<VideoHotSearchEmailUsageLogEntity> {

    /**
     * 加载超时使用记录
     *
     * @param idx   起始索引
     * @param limit 获取数量
     *
     * @return 超时使用记录列表
     */
    List<VideoHotSearchEmailUsageLogEntity> loadTimeoutUse(@Param("idx") long idx, @Param("limit") int limit);

    /**
     * 批量释放
     *
     * @param ids 待释放的ID列表
     */
    void batchRelease(@Param("ids") List<Long> ids);
}

