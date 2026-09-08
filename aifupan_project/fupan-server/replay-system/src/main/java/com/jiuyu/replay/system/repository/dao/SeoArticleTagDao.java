package com.jiuyu.replay.system.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuyu.replay.system.entity.SeoArticleTagEntity;
import com.jiuyu.replay.system.vo.SeoIdCountVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * SEO 文章标签关联。
 *
 * @author claude
 * @date 2026-08-12
 */
@Mapper
public interface SeoArticleTagDao extends BaseMapper<SeoArticleTagEntity> {

    /**
     * 按标签统计文章数，SQL 见 {@code resources/mapper/SeoArticleTagDao.xml}。
     *
     * <p>口径：未删除、<b>含已下架</b>（openspec §6.4）。必须 JOIN 文章表——
     * 关联表不带 is_deleted，文章逻辑删除后关联行仍在，只数关联行会把已删文章算进去。
     *
     * @param tagIds     标签 ID 集合，调用方需保证非空且已分批
     * @param notDeleted 未删除标记
     * @return 每个标签的文章数，<b>只含有文章的标签</b>——0 篇的标签不在结果里
     */
    List<SeoIdCountVo> countArticlesByTagIds(@Param("tagIds") List<Long> tagIds,
                                             @Param("notDeleted") Integer notDeleted);
}
