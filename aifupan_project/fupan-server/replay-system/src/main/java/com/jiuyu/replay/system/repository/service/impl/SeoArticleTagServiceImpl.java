package com.jiuyu.replay.system.repository.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.system.constant.SeoConstant;
import com.jiuyu.replay.system.vo.SeoIdCountVo;
import com.jiuyu.replay.system.entity.SeoArticleEntity;
import com.jiuyu.replay.system.entity.SeoArticleTagEntity;
import com.jiuyu.replay.system.repository.dao.SeoArticleDao;
import com.jiuyu.replay.system.repository.dao.SeoArticleTagDao;
import com.jiuyu.replay.system.repository.service.SeoArticleTagService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * SEO 文章标签关联实现。
 *
 * @author claude
 * @date 2026-08-12
 */
@Service
public class SeoArticleTagServiceImpl extends ServiceImpl<SeoArticleTagDao, SeoArticleTagEntity>
        implements SeoArticleTagService {

    private final SeoArticleDao seoArticleDao;

    public SeoArticleTagServiceImpl(SeoArticleDao seoArticleDao) {
        this.seoArticleDao = seoArticleDao;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceArticleTags(Long articleId, List<Long> tagIds) {
        removeByArticleIds(Collections.singletonList(articleId));
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        // 去重后写入，避免入参重复导致撞 uk_article_tag
        Set<Long> distinctTagIds = new HashSet<>(tagIds);
        LocalDateTime now = LocalDateTime.now();
        List<SeoArticleTagEntity> relations = new ArrayList<>(distinctTagIds.size());
        for (Long tagId : distinctTagIds) {
            SeoArticleTagEntity relation = new SeoArticleTagEntity();
            relation.setId(SnowflakeManager.nextValue());
            relation.setArticleId(articleId);
            relation.setTagId(tagId);
            relation.setCreateDate(now);
            relations.add(relation);
        }
        saveBatch(relations);
    }

    @Override
    public Map<Long, List<Long>> mapTagIdsByArticleIds(List<Long> articleIds) {
        Map<Long, List<Long>> result = new HashMap<>();
        if (articleIds == null || articleIds.isEmpty()) {
            return result;
        }
        for (List<Long> batch : Lists.partition(articleIds, SeoConstant.IN_BATCH_SIZE)) {
            List<SeoArticleTagEntity> relations = list(new LambdaQueryWrapper<SeoArticleTagEntity>()
                    .in(SeoArticleTagEntity::getArticleId, batch));
            for (SeoArticleTagEntity relation : relations) {
                result.computeIfAbsent(relation.getArticleId(), key -> new ArrayList<>()).add(relation.getTagId());
            }
        }
        return result;
    }

    @Override
    public Map<Long, Integer> countArticlesByTagIds(List<Long> tagIds) {
        Map<Long, Integer> result = new HashMap<>();
        if (tagIds == null || tagIds.isEmpty()) {
            return result;
        }
        for (List<Long> batch : Lists.partition(tagIds, SeoConstant.IN_BATCH_SIZE)) {
            // SQL 见 mapper/SeoArticleTagDao.xml，走 JOIN + GROUP BY 一次聚合。
            //
            // 原先是把关联行全查回内存、再回查文章表剔除已删除、在 Java 里 merge 累加——
            // 一个热门标签关联上万篇文章就会把上万行拉回来，
            // 直接违反 openspec §6「articleCount 由 SQL join 聚合返回，禁止 N+1」
            for (SeoIdCountVo row : getBaseMapper().countArticlesByTagIds(batch, SeoConstant.NOT_DELETED)) {
                result.put(row.getId(), row.getTotal());
            }
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeByArticleIds(List<Long> articleIds) {
        if (articleIds == null || articleIds.isEmpty()) {
            return;
        }
        for (List<Long> batch : Lists.partition(articleIds, SeoConstant.IN_BATCH_SIZE)) {
            remove(new LambdaQueryWrapper<SeoArticleTagEntity>().in(SeoArticleTagEntity::getArticleId, batch));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeByTagIds(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        for (List<Long> batch : Lists.partition(tagIds, SeoConstant.IN_BATCH_SIZE)) {
            remove(new LambdaQueryWrapper<SeoArticleTagEntity>().in(SeoArticleTagEntity::getTagId, batch));
        }
    }

    /** 查出未逻辑删除的文章 ID 集合 */
    private Set<Long> findAliveArticleIds(List<Long> articleIds) {
        Set<Long> alive = new HashSet<>();
        for (List<Long> batch : Lists.partition(articleIds, SeoConstant.IN_BATCH_SIZE)) {
            List<SeoArticleEntity> articles = seoArticleDao.selectList(
                    new LambdaQueryWrapper<SeoArticleEntity>()
                            .select(SeoArticleEntity::getId)
                            .in(SeoArticleEntity::getId, batch)
                            .eq(SeoArticleEntity::getIsDeleted, SeoConstant.NOT_DELETED));
            alive.addAll(articles.stream().map(SeoArticleEntity::getId).collect(Collectors.toSet()));
        }
        return alive;
    }
}
