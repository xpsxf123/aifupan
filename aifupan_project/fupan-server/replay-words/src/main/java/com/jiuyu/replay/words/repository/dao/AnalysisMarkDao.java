package com.jiuyu.replay.words.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuyu.replay.words.entity.AnalysisMarkEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Dao
 *
 * @author liaoxin
 * @date 2025-06-07
 */
@Mapper
public interface AnalysisMarkDao extends BaseMapper<AnalysisMarkEntity> {

    @Select("SELECT COALESCE(MAX(mark_no), 0) FROM tb_analysis_mark " +
        "WHERE source_id = #{sourceId} AND source_type = #{sourceType} ")
    Integer getMaxMarkNo(@Param("sourceId") String sourceId, @Param("sourceType") Integer sourceType);


    @Select("SELECT COUNT(*) FROM tb_analysis_mark " +
        "WHERE source_id = #{sourceId} " +
        "AND source_type = #{sourceType} " +
        "AND is_deleted = 0 " +
        "AND (#{paraphStartNo} <= paraph_end_no AND #{paraphEndNo} >= paraph_start_no) " +   // 段落范围重叠
        "AND (#{startIndex} <= mark_end_index AND #{endIndex} >= mark_start_index)")
        // 索引范围重叠
    Integer checkIndexOverlap(@Param("sourceId") String sourceId,
                              @Param("sourceType") Integer sourceType,
                              @Param("paraphStartNo") Integer paraphStartNo,
                              @Param("paraphEndNo") Integer paraphEndNo,
                              @Param("startIndex") Integer startIndex,
                              @Param("endIndex") Integer endIndex);


    @Select("<script>" +
        "SELECT distinct source_id AS sourceId FROM tb_analysis_mark " +
        "WHERE source_type = #{sourceType} " +
        "AND is_deleted = 0 " +
        "AND source_id IN " +
        "<foreach item='id' collection='videoIds' open='(' separator=',' close=')'>" +
        "#{id}" +
        "</foreach>" +
        "</script>")
    List<String> videoExistsMark(@Param("videoIds") List<String> videoIds, @Param("sourceType") int sourceType);

}