package com.jiuyu.replay.video.project.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuyu.replay.video.project.entity.VideoHotSearchVideoEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * <p>
 * 爆款搜索视频关联表 Mapper 接口
 * </p>
 *
 * @author RayChou
 * @since 2025-09-02
 */
@Mapper
public interface VideoHotSearchVideoDao extends BaseMapper<VideoHotSearchVideoEntity> {

    /**
     * 查询爆款视频关联数据表数据根据平台类型和关键字
     *
     * @param platformType
     * @param searchKeyword
     * @return
     */
    List<VideoHotSearchVideoEntity> listVideoHotSearchVideoByPlatformTypeAndKeyWord(Byte platformType, String searchKeyword, Integer limit);
}
