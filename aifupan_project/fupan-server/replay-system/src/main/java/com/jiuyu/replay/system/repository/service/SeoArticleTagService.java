package com.jiuyu.replay.system.repository.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jiuyu.replay.system.entity.SeoArticleTagEntity;

import java.util.List;
import java.util.Map;

/**
 * SEO 文章标签关联。
 *
 * @author claude
 * @date 2026-08-12
 */
public interface SeoArticleTagService extends IService<SeoArticleTagEntity> {

    /**
     * 覆盖式重建某篇文章的标签关联。
     *
     * @param articleId 文章 ID
     * @param tagIds    目标标签 ID 集合，可为空表示清空
     */
    void replaceArticleTags(Long articleId, List<Long> tagIds);

    /**
     * 按文章 ID 批量查关联的标签 ID。
     *
     * @param articleIds 文章 ID 集合
     * @return articleId -> tagId 列表
     */
    Map<Long, List<Long>> mapTagIdsByArticleIds(List<Long> articleIds);

    /**
     * 统计每个标签关联的未删除文章数（含已下架）。
     *
     * @param tagIds 标签 ID 集合
     * @return tagId -> 文章数
     */
    Map<Long, Integer> countArticlesByTagIds(List<Long> tagIds);

    /**
     * 清理某篇文章的全部标签关联。
     *
     * @param articleIds 文章 ID 集合
     */
    void removeByArticleIds(List<Long> articleIds);

    /**
     * 清理某个标签的全部文章关联。
     *
     * @param tagIds 标签 ID 集合
     */
    void removeByTagIds(List<Long> tagIds);
}
