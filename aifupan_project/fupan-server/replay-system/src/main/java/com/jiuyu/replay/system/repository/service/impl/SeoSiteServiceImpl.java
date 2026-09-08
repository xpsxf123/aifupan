package com.jiuyu.replay.system.repository.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.google.common.collect.Lists;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.code.StatusCode;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.system.bo.SeoSiteArticleListBo;
import com.jiuyu.replay.system.constant.SeoConstant;
import com.jiuyu.replay.system.entity.SeoArticleEntity;
import com.jiuyu.replay.system.repository.dao.SeoArticleDao;
import com.jiuyu.replay.system.repository.dao.SeoSiteDao;
import com.jiuyu.replay.system.repository.service.SeoSiteService;
import com.jiuyu.replay.system.repository.service.SeoSlugHistoryService;
import com.jiuyu.replay.system.vo.site.SeoSiteArticleDetailVo;
import com.jiuyu.replay.system.vo.site.SeoSiteArticleListResultVo;
import com.jiuyu.replay.system.vo.site.SeoSiteArticleListVo;
import com.jiuyu.replay.system.vo.site.SeoSiteCategoryVo;
import com.jiuyu.replay.system.vo.site.SeoSiteSitemapVo;
import com.jiuyu.replay.system.vo.site.SeoSiteTagItemVo;
import com.jiuyu.replay.system.vo.site.SeoSiteTagVo;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 官网公开只读查询。
 *
 * @author claude
 * @date 2026-08-18
 */
@Service
public class SeoSiteServiceImpl implements SeoSiteService {

    private final SeoSiteDao seoSiteDao;

    private final SeoArticleDao seoArticleDao;

    private final SeoSlugHistoryService seoSlugHistoryService;

    private final RedisTemplate<String, Object> redisTemplate;

    /** 缓存版本号所在的 key，自增即让全部旧缓存失效 */
    private static final String CACHE_VERSION_KEY = "seo:site:version";

    /** 缓存 key 前缀 */
    private static final String CACHE_PREFIX = "seo:site:v";

    /**
     * 缓存有效期 10 分钟。
     *
     * <p>官网详情页不做页面级缓存（原因见 {@link SeoSiteService#evictAll()}），
     * 压力全部落到这一层，因此这里必须有缓存。
     * 10 分钟只是兜底——正常情况下运营一改内容就会 evictAll，不必等它到期。
     */
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    public SeoSiteServiceImpl(SeoSiteDao seoSiteDao,
                              SeoArticleDao seoArticleDao,
                              SeoSlugHistoryService seoSlugHistoryService,
                              RedisTemplate<String, Object> redisTemplate) {
        this.seoSiteDao = seoSiteDao;
        this.seoArticleDao = seoArticleDao;
        this.seoSlugHistoryService = seoSlugHistoryService;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void evictAll() {
        redisTemplate.opsForValue().increment(CACHE_VERSION_KEY);
    }

    /**
     * 取缓存，未命中则执行 loader 并回写。
     *
     * <p>不缓存 null：官网对「查不到」是抛 404 而非返回 null，
     * 走到这里的 null 只可能是异常路径，缓存它会把一次偶发故障固化 10 分钟。
     */
    @SuppressWarnings("unchecked")
    private <T> T cached(String suffix, java.util.function.Supplier<T> loader) {
        String key = CACHE_PREFIX + currentVersion() + ":" + suffix;
        try {
            Object hit = redisTemplate.opsForValue().get(key);
            if (hit != null) {
                return (T) hit;
            }
        } catch (Exception e) {
            // Redis 不可用时降级为直接查库，不能让缓存故障演变成官网整站 500
            return loader.get();
        }
        T value = loader.get();
        if (value != null) {
            try {
                redisTemplate.opsForValue().set(key, value, CACHE_TTL);
            } catch (Exception ignored) {
                // 回写失败不影响本次返回
            }
        }
        return value;
    }

    private long currentVersion() {
        try {
            Object v = redisTemplate.opsForValue().get(CACHE_VERSION_KEY);
            return v == null ? 1L : Long.parseLong(String.valueOf(v));
        } catch (Exception e) {
            return 1L;
        }
    }

    @Override
    public SeoSiteArticleListResultVo queryArticleList(SeoSiteArticleListBo bo) {
        String categorySlug = trimToNull(bo.getCategorySlug());
        String tagSlug = trimToNull(bo.getTagSlug());

        // 互斥校验：没有「某分类下的某标签」这个页面。放任两者同传，
        // 等于凭空多出一批可被抓取、却没有任何入口的 URL 组合
        if (categorySlug != null && tagSlug != null) {
            throw new BusinessException("categorySlug 与 tagSlug 不能同时传");
        }

        bo.setCategorySlug(categorySlug);
        bo.setTagSlug(tagSlug);
        bo.setPage(bo.getPage() == null || bo.getPage() < 1 ? 1 : bo.getPage());
        bo.setLimit(normalizeLimit(bo.getLimit()));

        SeoSiteArticleListResultVo result = new SeoSiteArticleListResultVo();

        // 先确认聚合页本身存在，再查列表：slug 不存在要返回错误让官网 404，
        // 而不是给一个空列表——空列表在官网侧是 200 空壳页，会被搜索引擎收录
        if (categorySlug != null) {
            SeoSiteCategoryVo category = cached("category:" + categorySlug,
                    () -> seoSiteDao.selectSiteCategoryBySlug(
                            categorySlug, SeoConstant.NOT_DELETED, SeoConstant.STATUS_ENABLED));
            if (category == null) {
                throw notFound("分类不存在");
            }
            result.setCategory(category);
        }
        if (tagSlug != null) {
            SeoSiteTagVo tag = cached("tag:" + tagSlug,
                    () -> seoSiteDao.selectSiteTagBySlug(
                            tagSlug, SeoConstant.NOT_DELETED, SeoConstant.STATUS_ENABLED,
                            SeoConstant.STATUS_ENABLED));
            if (tag == null) {
                throw notFound("标签不存在");
            }
            result.setTag(tag);
        }

        // 缓存 key 必须覆盖全部查询参数，漏一个就会让不同页/不同分类读到同一份结果
        String cacheKey = "list:" + bo.getPage() + ":" + bo.getLimit()
                + ":" + (categorySlug == null ? "" : categorySlug)
                + ":" + (tagSlug == null ? "" : tagSlug);
        PageUtils<SeoSiteArticleListVo> pageResult = cached(cacheKey, () -> loadPage(bo));
        result.setPage(pageResult);
        return result;
    }

    private PageUtils<SeoSiteArticleListVo> loadPage(SeoSiteArticleListBo bo) {
        IPage<SeoSiteArticleListVo> page = seoSiteDao.selectSitePage(
                new Query<SeoArticleEntity>().getPageNoSort(bo.getPage(), bo.getLimit()),
                bo, SeoConstant.NOT_DELETED, SeoConstant.STATUS_ENABLED, SeoConstant.STATUS_ENABLED);

        PageUtils<SeoSiteArticleListVo> pageResult = new PageUtils<>(bo.getPage(), bo.getLimit(), page);
        List<SeoSiteArticleListVo> records = page.getRecords();
        fillTags(records);
        pageResult.setList(records == null ? new ArrayList<>() : records);
        return pageResult;
    }

    @Override
    public SeoSiteArticleDetailVo getArticleDetail(String slug) {
        String normalized = trimToNull(slug);
        if (normalized == null) {
            throw notFound("文章不存在");
        }

        // 【顺序不能反】先查当前表、查不到再查历史表。该顺序自动处理三个边界：
        //   1. slug 改回原值（x→y→x）：命中当前表，历史表那条永不生效
        //   2. A 改 x→y 后新文章 B 占用了 x：命中当前表的 B，不会把读者错误 301 到 A
        //   3. A 改 x→y 后 A 被删除：走到下面的历史表分支，回查实体已删 → 404
        SeoSiteArticleDetailVo detail = cached("detail:" + normalized,
                () -> seoSiteDao.selectSiteDetail(
                        normalized, SeoConstant.NOT_DELETED, SeoConstant.STATUS_ENABLED));
        if (detail != null) {
            fillTags(Collections.singletonList(toListVo(detail)), detail);
            return detail;
        }

        String redirectSlug = resolveRedirectSlug(normalized);
        if (redirectSlug != null) {
            // 只回 redirectSlug，不查正文——省一次大字段读取
            SeoSiteArticleDetailVo moved = new SeoSiteArticleDetailVo();
            moved.setRedirectSlug(redirectSlug);
            return moved;
        }

        throw notFound("文章不存在");
    }

    @Override
    public List<SeoSiteArticleListVo> listRelated(String slug, Integer limit) {
        String normalized = trimToNull(slug);
        if (normalized == null) {
            return new ArrayList<>();
        }
        int size = limit == null || limit < 1
                ? SeoConstant.SITE_RELATED_DEFAULT_LIMIT
                : Math.min(limit, SeoConstant.SITE_MAX_PAGE_SIZE);

        // 第一段：共享标签数降序。当前文章无标签时 SQL 内的子查询返回空集，
        // 本段自然得到 0 行，不需要在这里判空
        List<SeoSiteArticleListVo> cachedResult = cached("related:" + normalized + ":" + size,
                () -> loadRelated(normalized, size));
        return cachedResult == null ? new ArrayList<>() : cachedResult;
    }

    private List<SeoSiteArticleListVo> loadRelated(String normalized, int size) {
        List<SeoSiteArticleListVo> picked = new ArrayList<>(seoSiteDao.selectRelatedByTags(
                normalized, size, SeoConstant.NOT_DELETED,
                SeoConstant.STATUS_ENABLED, SeoConstant.STATUS_ENABLED));

        // 第二段：同分类最新补齐
        if (picked.size() < size) {
            List<String> excludeSlugs = picked.stream().map(SeoSiteArticleListVo::getSlug).toList();
            picked.addAll(seoSiteDao.selectRelatedByCategory(
                    normalized, excludeSlugs, size - picked.size(),
                    SeoConstant.NOT_DELETED, SeoConstant.STATUS_ENABLED));
        }

        fillTags(picked);
        return picked;
    }

    @Override
    public List<SeoSiteCategoryVo> listCategories() {
        return cached("categories", () ->
                seoSiteDao.selectSiteCategories(SeoConstant.NOT_DELETED, SeoConstant.STATUS_ENABLED));
    }

    @Override
    public List<SeoSiteTagVo> listTags() {
        return cached("tags", () -> seoSiteDao.selectSiteTags(
                SeoConstant.NOT_DELETED, SeoConstant.STATUS_ENABLED, SeoConstant.STATUS_ENABLED));
    }

    @Override
    public SeoSiteSitemapVo getSitemap() {
        return cached("sitemap", this::loadSitemap);
    }

    private SeoSiteSitemapVo loadSitemap() {
        SeoSiteSitemapVo vo = new SeoSiteSitemapVo();
        vo.setArticles(seoSiteDao.selectSitemapArticles(
                SeoConstant.NOT_DELETED, SeoConstant.STATUS_ENABLED));
        vo.setCategories(seoSiteDao.selectSitemapCategories(
                SeoConstant.NOT_DELETED, SeoConstant.STATUS_ENABLED));
        vo.setTags(seoSiteDao.selectSitemapTags(
                SeoConstant.SITE_TAG_INDEX_MIN_COUNT, SeoConstant.NOT_DELETED,
                SeoConstant.STATUS_ENABLED, SeoConstant.STATUS_ENABLED));
        return vo;
    }

    // ---------------------------------------------------------------- private

    /**
     * 按旧 slug 解析出仍然有效的新 slug。
     *
     * <p>命中历史表只说明「这个地址曾经属于某篇文章」，不代表那篇文章现在还在：
     * 它可能已被删除或下架。因此必须回查实体并确认仍是已发布状态，
     * 否则会 301 到一个 404 页面——对搜索引擎而言比直接 404 更糟。
     */
    private String resolveRedirectSlug(String oldSlug) {
        Long articleId = seoSlugHistoryService.resolveEntityId(
                SeoConstant.SLUG_ENTITY_ARTICLE, oldSlug);
        if (articleId == null) {
            return null;
        }
        SeoArticleEntity article = seoArticleDao.selectOne(
                new LambdaQueryWrapper<SeoArticleEntity>()
                        .select(SeoArticleEntity::getSlug, SeoArticleEntity::getArticleStatus)
                        .eq(SeoArticleEntity::getId, articleId));
        if (article == null
                || !Objects.equals(article.getArticleStatus(), SeoConstant.STATUS_ENABLED)) {
            return null;
        }
        return article.getSlug();
    }

    /** 详情复用列表的标签填充逻辑：先包一个临时列表项，填完再回写 */
    private void fillTags(List<SeoSiteArticleListVo> holder, SeoSiteArticleDetailVo detail) {
        fillTags(holder);
        detail.setTags(holder.get(0).getTags());
    }

    /**
     * 批量填充启用中的标签，按 slug 分组，避免 N+1。
     *
     * <p>只查启用中的标签——禁用标签的聚合页在官网不可访问，展示出来就是必然 404 的链接。
     */
    private void fillTags(List<SeoSiteArticleListVo> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<String> slugs = records.stream()
                .map(SeoSiteArticleListVo::getSlug)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (slugs.isEmpty()) {
            return;
        }

        Map<String, List<SeoSiteTagItemVo>> tagMap = new HashMap<>();
        // 沿用项目处理大 IN 的惯例：超过阈值分批，避免拼出超长 SQL
        for (List<String> batch : Lists.partition(slugs, SeoConstant.IN_BATCH_SIZE)) {
            List<Map<String, Object>> rows = seoSiteDao.selectSiteTagsByArticleSlugs(
                    batch, SeoConstant.NOT_DELETED, SeoConstant.STATUS_ENABLED);
            for (Map<String, Object> row : rows) {
                String articleSlug = asString(row.get("articleSlug"));
                if (articleSlug == null) {
                    continue;
                }
                SeoSiteTagItemVo tag = new SeoSiteTagItemVo();
                tag.setTagName(asString(row.get("tagName")));
                tag.setSlug(asString(row.get("tagSlug")));
                tagMap.computeIfAbsent(articleSlug, k -> new ArrayList<>()).add(tag);
            }
        }

        for (SeoSiteArticleListVo record : records) {
            record.setTags(tagMap.getOrDefault(record.getSlug(), new ArrayList<>()));
        }
    }

    /** 详情转成列表项，仅用于复用标签填充 */
    private SeoSiteArticleListVo toListVo(SeoSiteArticleDetailVo detail) {
        SeoSiteArticleListVo vo = new SeoSiteArticleListVo();
        vo.setSlug(detail.getSlug());
        return vo;
    }

    /**
     * 「资源不存在」专用异常，码为 {@link StatusCode#NOT_FOUND}（404）。
     *
     * <p><b>必须与其他业务异常区分开。</b>本项目的全局约定是所有错误都返回
     * HTTP 200 + 非 0 的 code，因此官网侧无法靠 HTTP 状态码判断失败原因。
     * 若这里沿用默认的 {@code BusinessException(msg)}（码 -9），官网就只能按
     * 「code != 0 即内容不存在」来处理——那样数据库抖动、空指针、SQL 异常
     * 全都会被渲染成 404，搜索引擎会据此判定一批正常文章已被删除，
     * 而收录与排名的损失是不可逆的。
     *
     * <p>官网侧的约定：只有 code == 404 才渲染 404 页，其余非 0 一律当作
     * 后端故障抛 500——500 只是暂时不可用，不会让已收录页面掉出索引。
     */
    private BusinessException notFound(String message) {
        return new BusinessException(StatusCode.NOT_FOUND.getCode(), message);
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return SeoConstant.SITE_DEFAULT_PAGE_SIZE;
        }
        // 封顶：匿名公开接口，不限制的话一个 limit=999999 就能把全站文章一次性拉走
        return Math.min(limit, SeoConstant.SITE_MAX_PAGE_SIZE);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }
}
