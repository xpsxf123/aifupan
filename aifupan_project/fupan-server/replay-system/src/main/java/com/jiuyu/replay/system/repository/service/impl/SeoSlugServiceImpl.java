package com.jiuyu.replay.system.repository.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jiuyu.replay.system.bo.SeoSlugSuggestBo;
import com.jiuyu.replay.system.constant.SeoConstant;
import com.jiuyu.replay.system.entity.SeoArticleEntity;
import com.jiuyu.replay.system.entity.SeoCategoryEntity;
import com.jiuyu.replay.system.entity.SeoTagEntity;
import com.jiuyu.replay.system.repository.dao.SeoArticleDao;
import com.jiuyu.replay.system.repository.dao.SeoCategoryDao;
import com.jiuyu.replay.system.repository.dao.SeoTagDao;
import com.jiuyu.replay.system.repository.service.SeoSlugService;
import com.jiuyu.replay.system.util.SeoSlugUtil;
import com.jiuyu.replay.system.vo.SeoSlugSuggestVo;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * slug 查重与建议实现。
 *
 * @author claude
 * @date 2026-08-12
 */
@Service
public class SeoSlugServiceImpl implements SeoSlugService {

    private final SeoArticleDao seoArticleDao;

    private final SeoCategoryDao seoCategoryDao;

    private final SeoTagDao seoTagDao;

    public SeoSlugServiceImpl(SeoArticleDao seoArticleDao, SeoCategoryDao seoCategoryDao, SeoTagDao seoTagDao) {
        this.seoArticleDao = seoArticleDao;
        this.seoCategoryDao = seoCategoryDao;
        this.seoTagDao = seoTagDao;
    }

    @Override
    public SeoSlugSuggestVo suggest(SeoSlugSuggestBo bo) {
        String type = bo.getType();
        // 与 resolveSlug 用同一套截断规则，否则前端预览值与实际入库值会不一致
        String base = SeoSlugUtil.truncate(resolveBase(bo.getName(), bo.getSlug()), maxSlugLenOf(type) - 5);

        SeoSlugSuggestVo vo = new SeoSlugSuggestVo();
        vo.setSlug(base);
        vo.setSuggestions(new ArrayList<>());

        if (base.isEmpty()) {
            // 名称转不出拼音（纯日文 / emoji / 纯标点等），交给前端提示手填
            vo.setAvailable(false);
            return vo;
        }

        String ownerName = findOwnerName(type, base, bo.getExcludeId());
        if (ownerName == null) {
            vo.setAvailable(true);
            return vo;
        }
        vo.setAvailable(false);
        vo.setOwnerName(ownerName);
        vo.setSuggestions(buildSemanticSuggestions(type, bo.getName(), base, bo.getExcludeId()));
        return vo;
    }

    @Override
    public String resolveSlug(String type, String name, String slug, Long excludeId) {
        // 先截断再解析：100 字标题转全拼可达 300+ 字符，不截断会在新增时就撞 1406 Data too long。
        // 预留 5 字符给可能追加的随机后缀（"-" + 4 位）
        int maxLen = maxSlugLenOf(type) - 5;
        String base = SeoSlugUtil.truncate(resolveBase(name, slug), maxLen);
        Predicate<String> taken = candidate -> isSlugTaken(type, candidate, excludeId);
        return SeoSlugUtil.resolveWithFallback(base, fallbackPrefixOf(type), taken);
    }

    @Override
    public boolean isSlugTaken(String type, String slug, Long excludeId) {
        return findOwnerName(type, slug, excludeId) != null;
    }

    /** slug 为空时由 name 生成拼音，否则规范化入参 slug */
    private String resolveBase(String name, String slug) {
        if (slug != null && !slug.isBlank()) {
            return SeoSlugUtil.normalize(slug);
        }
        return SeoSlugUtil.toPinyinSlug(name);
    }

    /**
     * 查出占用者名称，未被占用返回 null。
     *
     * <p>查询一律带 isDeleted 过滤——已逻辑删除的记录不占用 slug（其 slug 已被改写为墓碑值，
     * 此处的过滤是双保险）。
     */
    private String findOwnerName(String type, String slug, Long excludeId) {
        if (slug == null || slug.isEmpty()) {
            return null;
        }
        if (SeoConstant.SLUG_TYPE_CATEGORY.equals(type)) {
            LambdaQueryWrapper<SeoCategoryEntity> wrapper = new LambdaQueryWrapper<SeoCategoryEntity>()
                    .eq(SeoCategoryEntity::getSlug, slug)
                    .eq(SeoCategoryEntity::getIsDeleted, SeoConstant.NOT_DELETED)
                    .ne(Objects.nonNull(excludeId), SeoCategoryEntity::getId, excludeId)
                    .last("limit 1");
            SeoCategoryEntity entity = seoCategoryDao.selectOne(wrapper);
            return entity == null ? null : entity.getCategoryName();
        }
        if (SeoConstant.SLUG_TYPE_TAG.equals(type)) {
            LambdaQueryWrapper<SeoTagEntity> wrapper = new LambdaQueryWrapper<SeoTagEntity>()
                    .eq(SeoTagEntity::getSlug, slug)
                    .eq(SeoTagEntity::getIsDeleted, SeoConstant.NOT_DELETED)
                    .ne(Objects.nonNull(excludeId), SeoTagEntity::getId, excludeId)
                    .last("limit 1");
            SeoTagEntity entity = seoTagDao.selectOne(wrapper);
            return entity == null ? null : entity.getTagName();
        }
        LambdaQueryWrapper<SeoArticleEntity> wrapper = new LambdaQueryWrapper<SeoArticleEntity>()
                .eq(SeoArticleEntity::getSlug, slug)
                .eq(SeoArticleEntity::getIsDeleted, SeoConstant.NOT_DELETED)
                .ne(Objects.nonNull(excludeId), SeoArticleEntity::getId, excludeId)
                .last("limit 1");
        SeoArticleEntity entity = seoArticleDao.selectOne(wrapper);
        return entity == null ? null : entity.getTitle();
    }

    /**
     * 语义化备选：用「两字一组」近似分词重新切分（zhibofupan -> zhibo-fupan）。
     *
     * <p>不给数字后缀备选——冲突已由自动追加随机数兜底，再列 -2/-3 只是噪音。
     * 切不出更好的值时返回空列表，不硬凑「看起来有语义」的劣质变体。
     */
    private List<String> buildSemanticSuggestions(String type, String name, String base, Long excludeId) {
        List<String> suggestions = new ArrayList<>();
        if (name == null || !name.matches("^[\\u4e00-\\u9fa5]{4,}$")) {
            return suggestions;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < name.length(); i += 2) {
            String segment = SeoSlugUtil.toPinyinSlug(name.substring(i, Math.min(i + 2, name.length())));
            if (segment.isEmpty()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append('-');
            }
            builder.append(segment);
        }
        String variant = builder.toString();
        if (!variant.isEmpty() && !variant.equals(base) && !isSlugTaken(type, variant, excludeId)) {
            suggestions.add(variant);
        }
        return suggestions;
    }

    /** slug 入库长度上限，须为字段长度减去墓碑后缀 */
    private int maxSlugLenOf(String type) {
        if (SeoConstant.SLUG_TYPE_ARTICLE.equals(type)) {
            return SeoConstant.MAX_SLUG_LEN_ARTICLE;
        }
        return SeoConstant.MAX_SLUG_LEN_CATEGORY_TAG;
    }

    /** 名称转不出拼音时的兜底前缀 */
    private String fallbackPrefixOf(String type) {
        if (SeoConstant.SLUG_TYPE_CATEGORY.equals(type)) {
            return SeoConstant.SLUG_TYPE_CATEGORY;
        }
        if (SeoConstant.SLUG_TYPE_TAG.equals(type)) {
            return SeoConstant.SLUG_TYPE_TAG;
        }
        return SeoConstant.SLUG_TYPE_ARTICLE;
    }
}
