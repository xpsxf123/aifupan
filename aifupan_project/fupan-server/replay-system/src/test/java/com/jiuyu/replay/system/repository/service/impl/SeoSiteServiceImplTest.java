package com.jiuyu.replay.system.repository.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.system.bo.SeoSiteArticleListBo;
import com.jiuyu.replay.system.constant.SeoConstant;
import com.jiuyu.replay.system.entity.SeoArticleEntity;
import com.jiuyu.replay.system.repository.dao.SeoArticleDao;
import com.jiuyu.replay.system.repository.dao.SeoSiteDao;
import com.jiuyu.replay.system.repository.service.SeoSlugHistoryService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import com.jiuyu.replay.system.vo.site.SeoSiteArticleDetailVo;
import com.jiuyu.replay.system.vo.site.SeoSiteArticleListResultVo;
import com.jiuyu.replay.system.vo.site.SeoSiteArticleListVo;
import com.jiuyu.replay.system.vo.site.SeoSiteCategoryVo;
import com.jiuyu.replay.system.vo.site.SeoSiteTagVo;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * 官网公开接口的行为校验。
 *
 * <p><b>这组测试守的是「什么不该出现在公网上」</b>——本模块的每一条约束都没有
 * 运行时报错可依赖：下架文章泄漏、内部字段外传、禁用标签渲染成死链，
 * 全都是接口 200、页面正常，只有事后才会发现。
 *
 * @author claude
 * @date 2026-08-18
 */
@DisplayName("SeoSiteServiceImpl")
class SeoSiteServiceImplTest {

    private SeoSiteDao seoSiteDao;

    private SeoArticleDao seoArticleDao;

    private SeoSlugHistoryService seoSlugHistoryService;

    private RedisTemplate<String, Object> redisTemplate;

    private SeoSiteServiceImpl service;

    @BeforeAll
    static void initLambdaColumnCache() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        assistant.setCurrentNamespace("seoSiteServiceImplTest");
        TableInfoHelper.initTableInfo(assistant, SeoArticleEntity.class);
    }

    @BeforeEach
    void setUp() {
        seoSiteDao = Mockito.mock(SeoSiteDao.class);
        seoArticleDao = Mockito.mock(SeoArticleDao.class);
        seoSlugHistoryService = Mockito.mock(SeoSlugHistoryService.class);

        // Redis 全程返回 null（未命中），让测试始终走真实查询路径。
        // 缓存命中与否不是这组测试的关注点，这里只需保证它不干扰断言
        redisTemplate = Mockito.mock(RedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, Object> ops = Mockito.mock(ValueOperations.class);
        Mockito.when(redisTemplate.opsForValue()).thenReturn(ops);
        Mockito.when(ops.get(anyString())).thenReturn(null);

        service = new SeoSiteServiceImpl(
                seoSiteDao, seoArticleDao, seoSlugHistoryService, redisTemplate);
    }

    private static SeoSiteArticleListVo listVo(String slug) {
        SeoSiteArticleListVo vo = new SeoSiteArticleListVo();
        vo.setSlug(slug);
        vo.setTitle("标题-" + slug);
        return vo;
    }

    private void stubEmptyPage() {
        IPage<SeoSiteArticleListVo> page = new Page<>(1, 10);
        page.setRecords(new ArrayList<>());
        when(seoSiteDao.selectSitePage(any(), any(), anyInt(), anyInt(), anyInt())).thenReturn(page);
    }

    @Nested
    @DisplayName("列表参数校验")
    class ListParams {

        @Test
        @DisplayName("categorySlug 与 tagSlug 同传时报错——没有「某分类下某标签」这个页面")
        void rejectsBothSlugs() {
            SeoSiteArticleListBo bo = new SeoSiteArticleListBo();
            bo.setCategorySlug("zhibo-yunying");
            bo.setTagSlug("fupan");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.queryArticleList(bo));
            assertTrue(ex.getMessage().contains("不能同时传"));
        }

        @Test
        @DisplayName("limit 超上限时封顶，防止一次性拉走全站内容")
        void capsLimit() {
            SeoSiteArticleListBo bo = new SeoSiteArticleListBo();
            bo.setLimit(999999);
            stubEmptyPage();

            service.queryArticleList(bo);

            assertEquals(SeoConstant.SITE_MAX_PAGE_SIZE, bo.getLimit(),
                    "limit 必须封顶。这是匿名公开接口，不限制的话一个 limit=999999 "
                            + "就能把全站文章一次性拉走");
        }

        @Test
        @DisplayName("page/limit 缺省时用默认值，不因 null 抛异常")
        void appliesDefaults() {
            SeoSiteArticleListBo bo = new SeoSiteArticleListBo();
            stubEmptyPage();

            service.queryArticleList(bo);

            assertEquals(1, bo.getPage());
            assertEquals(SeoConstant.SITE_DEFAULT_PAGE_SIZE, bo.getLimit());
        }

        @Test
        @DisplayName("分类不存在时抛错而不是返回空列表——官网据此 404")
        void throwsWhenCategoryMissing() {
            SeoSiteArticleListBo bo = new SeoSiteArticleListBo();
            bo.setCategorySlug("not-exist");
            when(seoSiteDao.selectSiteCategoryBySlug(anyString(), anyInt(), anyInt())).thenReturn(null);

            assertThrows(BusinessException.class, () -> service.queryArticleList(bo),
                    "slug 不存在必须抛错。返回 200 空列表会让官网渲染出一个空壳页，"
                            + "而空页面会被搜索引擎当作有效页收录");
        }

        @Test
        @DisplayName("标签不存在（或已禁用）时同样抛错")
        void throwsWhenTagMissing() {
            SeoSiteArticleListBo bo = new SeoSiteArticleListBo();
            bo.setTagSlug("disabled-tag");
            when(seoSiteDao.selectSiteTagBySlug(anyString(), anyInt(), anyInt(), anyInt()))
                    .thenReturn(null);

            assertThrows(BusinessException.class, () -> service.queryArticleList(bo));
        }

        @Test
        @DisplayName("分类页把分类信息一起返回，供页面渲染 h1 与 meta")
        void returnsCategoryAlongsideList() {
            SeoSiteArticleListBo bo = new SeoSiteArticleListBo();
            bo.setCategorySlug("zhibo-yunying");
            SeoSiteCategoryVo category = new SeoSiteCategoryVo();
            category.setCategoryName("直播运营");
            category.setArticleCount(6);
            when(seoSiteDao.selectSiteCategoryBySlug(anyString(), anyInt(), anyInt()))
                    .thenReturn(category);
            stubEmptyPage();

            SeoSiteArticleListResultVo result = service.queryArticleList(bo);

            assertNotNull(result.getCategory());
            assertEquals("直播运营", result.getCategory().getCategoryName());
            assertNull(result.getTag(), "非标签页时 tag 应为 null");
        }
    }

    @Nested
    @DisplayName("详情与 301")
    class Detail {

        @Test
        @DisplayName("命中已发布文章时正常返回，redirectSlug 为空")
        void returnsPublishedArticle() {
            SeoSiteArticleDetailVo detail = new SeoSiteArticleDetailVo();
            detail.setSlug("hello");
            detail.setTitle("标题");
            when(seoSiteDao.selectSiteDetail(eq("hello"), anyInt(), anyInt())).thenReturn(detail);
            when(seoSiteDao.selectSiteTagsByArticleSlugs(anyList(), anyInt(), anyInt()))
                    .thenReturn(new ArrayList<>());

            SeoSiteArticleDetailVo result = service.getArticleDetail("hello");

            assertEquals("标题", result.getTitle());
            assertNull(result.getRedirectSlug());
        }

        @Test
        @DisplayName("下架/不存在的 slug 抛错，绝不返回内容")
        void throwsForUnpublished() {
            when(seoSiteDao.selectSiteDetail(anyString(), anyInt(), anyInt())).thenReturn(null);
            when(seoSlugHistoryService.resolveEntityId(anyInt(), anyString())).thenReturn(null);

            assertThrows(BusinessException.class, () -> service.getArticleDetail("gone"));
        }

        @Test
        @DisplayName("空 slug 直接抛错，不打数据库")
        void throwsForBlankSlug() {
            assertThrows(BusinessException.class, () -> service.getArticleDetail("  "));
        }

        @Test
        @DisplayName("旧 slug 命中历史表且目标仍已发布 → 只回 redirectSlug")
        void returnsRedirectForMovedSlug() {
            when(seoSiteDao.selectSiteDetail(anyString(), anyInt(), anyInt())).thenReturn(null);
            when(seoSlugHistoryService.resolveEntityId(
                    eq(SeoConstant.SLUG_ENTITY_ARTICLE), eq("old-slug"))).thenReturn(1001L);

            SeoArticleEntity current = new SeoArticleEntity();
            current.setSlug("new-slug");
            current.setArticleStatus(SeoConstant.STATUS_ENABLED);
            when(seoArticleDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(current);

            SeoSiteArticleDetailVo result = service.getArticleDetail("old-slug");

            assertEquals("new-slug", result.getRedirectSlug());
            assertNull(result.getTitle(), "命中历史表时不查正文，其余字段应为空");
        }

        @Test
        @DisplayName("旧 slug 的目标文章已被删除 → 404 而不是 301 到死链")
        void throwsWhenRedirectTargetDeleted() {
            when(seoSiteDao.selectSiteDetail(anyString(), anyInt(), anyInt())).thenReturn(null);
            when(seoSlugHistoryService.resolveEntityId(anyInt(), anyString())).thenReturn(1001L);
            // 实体已被逻辑删除，MP 的逻辑删除过滤会让这次查询返回 null
            when(seoArticleDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            assertThrows(BusinessException.class, () -> service.getArticleDetail("old-slug"),
                    "301 到一个 404 页面比直接 404 更糟——搜索引擎会多跟一跳才发现是死链");
        }

        @Test
        @DisplayName("旧 slug 的目标文章已下架 → 同样 404")
        void throwsWhenRedirectTargetUnpublished() {
            when(seoSiteDao.selectSiteDetail(anyString(), anyInt(), anyInt())).thenReturn(null);
            when(seoSlugHistoryService.resolveEntityId(anyInt(), anyString())).thenReturn(1001L);

            SeoArticleEntity current = new SeoArticleEntity();
            current.setSlug("new-slug");
            current.setArticleStatus(SeoConstant.STATUS_DISABLED);
            when(seoArticleDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(current);

            assertThrows(BusinessException.class, () -> service.getArticleDetail("old-slug"));
        }
    }

    @Nested
    @DisplayName("相关文章")
    class Related {

        @Test
        @DisplayName("共享标签够数时不再走同分类兜底")
        void skipsFallbackWhenEnough() {
            when(seoSiteDao.selectRelatedByTags(anyString(), anyInt(), anyInt(), anyInt(), anyInt()))
                    .thenReturn(new ArrayList<>(Arrays.asList(
                            listVo("a"), listVo("b"), listVo("c"), listVo("d"))));
            when(seoSiteDao.selectSiteTagsByArticleSlugs(anyList(), anyInt(), anyInt()))
                    .thenReturn(new ArrayList<>());

            List<SeoSiteArticleListVo> result = service.listRelated("current", 4);

            assertEquals(4, result.size());
            Mockito.verify(seoSiteDao, Mockito.never())
                    .selectRelatedByCategory(anyString(), anyList(), anyInt(), anyInt(), anyInt());
        }

        @Test
        @DisplayName("不足时用同分类补齐，且把已选中的排除掉")
        void fallsBackToCategory() {
            when(seoSiteDao.selectRelatedByTags(anyString(), anyInt(), anyInt(), anyInt(), anyInt()))
                    .thenReturn(new ArrayList<>(List.of(listVo("a"))));
            when(seoSiteDao.selectRelatedByCategory(anyString(), anyList(), eq(3), anyInt(), anyInt()))
                    .thenReturn(new ArrayList<>(Arrays.asList(listVo("x"), listVo("y"), listVo("z"))));
            when(seoSiteDao.selectSiteTagsByArticleSlugs(anyList(), anyInt(), anyInt()))
                    .thenReturn(new ArrayList<>());

            List<SeoSiteArticleListVo> result = service.listRelated("current", 4);

            assertEquals(4, result.size());
            @SuppressWarnings("unchecked")
            org.mockito.ArgumentCaptor<List<String>> captor =
                    org.mockito.ArgumentCaptor.forClass(List.class);
            Mockito.verify(seoSiteDao).selectRelatedByCategory(
                    anyString(), captor.capture(), eq(3), anyInt(), anyInt());
            assertTrue(captor.getValue().contains("a"),
                    "第一段已选中的 slug 必须传给兜底查询排除，否则同一篇会出现两次");
        }

        @Test
        @DisplayName("无标签的文章不抛异常，直接走兜底")
        void handlesArticleWithoutTags() {
            // SQL 内的子查询在文章无标签时返回空集，本段自然是 0 行——
            // 不需要 Java 侧判空，也就不会拼出 IN () 语法错误
            when(seoSiteDao.selectRelatedByTags(anyString(), anyInt(), anyInt(), anyInt(), anyInt()))
                    .thenReturn(new ArrayList<>());
            when(seoSiteDao.selectRelatedByCategory(anyString(), anyList(), anyInt(), anyInt(), anyInt()))
                    .thenReturn(new ArrayList<>(List.of(listVo("x"))));
            when(seoSiteDao.selectSiteTagsByArticleSlugs(anyList(), anyInt(), anyInt()))
                    .thenReturn(new ArrayList<>());

            List<SeoSiteArticleListVo> result = service.listRelated("no-tags", 4);

            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("slug 为空时返回空列表——推荐区缺失不该让整个详情页失败")
        void returnsEmptyForBlankSlug() {
            assertTrue(service.listRelated("  ", 4).isEmpty());
        }
    }

    @Nested
    @DisplayName("标签填充")
    class TagFilling {

        @Test
        @DisplayName("按 slug 分组填充，互不串味")
        void groupsTagsByArticleSlug() {
            IPage<SeoSiteArticleListVo> page = new Page<>(1, 10);
            page.setRecords(new ArrayList<>(Arrays.asList(listVo("a"), listVo("b"))));
            when(seoSiteDao.selectSitePage(any(), any(), anyInt(), anyInt(), anyInt())).thenReturn(page);
            when(seoSiteDao.selectSiteTagsByArticleSlugs(anyList(), anyInt(), anyInt()))
                    .thenReturn(new ArrayList<>(Arrays.asList(
                            Map.of("articleSlug", "a", "tagName", "标签1", "tagSlug", "t1"),
                            Map.of("articleSlug", "a", "tagName", "标签2", "tagSlug", "t2"),
                            Map.of("articleSlug", "b", "tagName", "标签3", "tagSlug", "t3"))));

            SeoSiteArticleListResultVo result = service.queryArticleList(new SeoSiteArticleListBo());
            List<SeoSiteArticleListVo> list = result.getPage().getList();

            assertEquals(2, list.get(0).getTags().size());
            assertEquals(1, list.get(1).getTags().size());
            assertEquals("标签3", list.get(1).getTags().get(0).getTagName());
        }

        @Test
        @DisplayName("没有标签的文章得到空列表而不是 null，避免前端渲染时报错")
        void emptyListRatherThanNull() {
            IPage<SeoSiteArticleListVo> page = new Page<>(1, 10);
            page.setRecords(new ArrayList<>(List.of(listVo("a"))));
            when(seoSiteDao.selectSitePage(any(), any(), anyInt(), anyInt(), anyInt())).thenReturn(page);
            when(seoSiteDao.selectSiteTagsByArticleSlugs(anyList(), anyInt(), anyInt()))
                    .thenReturn(new ArrayList<>());

            SeoSiteArticleListResultVo result = service.queryArticleList(new SeoSiteArticleListBo());

            assertNotNull(result.getPage().getList().get(0).getTags());
            assertTrue(result.getPage().getList().get(0).getTags().isEmpty());
        }
    }

    @Nested
    @DisplayName("出参不含内部字段")
    class NoInternalFields {

        @Test
        @DisplayName("列表 VO 没有 id / categoryId / articleStatus / viewCount 这些字段")
        void listVoHasNoInternalFields() {
            assertNoFields(SeoSiteArticleListVo.class,
                    "id", "categoryId", "articleStatus", "viewCount", "isDeleted");
        }

        @Test
        @DisplayName("详情 VO 同样不含内部字段")
        void detailVoHasNoInternalFields() {
            assertNoFields(SeoSiteArticleDetailVo.class,
                    "id", "categoryId", "articleStatus", "viewCount", "isDeleted", "createDate");
        }

        @Test
        @DisplayName("标签项只有名称与 slug，不含 id 与 tagStatus")
        void tagItemHasNoInternalFields() {
            assertNoFields(com.jiuyu.replay.system.vo.site.SeoSiteTagItemVo.class, "id", "tagStatus");
        }

        @Test
        @DisplayName("分类/标签 VO 不含 id")
        void aggregateVosHaveNoId() {
            assertNoFields(SeoSiteCategoryVo.class, "id", "sort", "isDeleted");
            assertNoFields(SeoSiteTagVo.class, "id", "tagStatus", "isDeleted");
        }

        /**
         * 这组断言存在的原因：出参裁剪是纯约定，没有任何机制阻止有人日后
         * 顺手加一个字段。一旦加上，内部数据就静默流向匿名公网，
         * 而接口照常 200、页面照常渲染，没有任何迹象。
         */
        private void assertNoFields(Class<?> type, String... forbidden) {
            List<String> actual = Arrays.stream(type.getDeclaredFields())
                    .map(Field::getName)
                    .toList();
            for (String name : forbidden) {
                assertTrue(!actual.contains(name),
                        type.getSimpleName() + " 不得含字段 " + name
                                + "——这是对匿名公网输出的 VO，内部字段一旦加上就会直接泄漏，"
                                + "且不会有任何报错");
            }
        }
    }
}
