package com.jiuyu.replay.system.repository.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.system.bo.SeoArticleBo;
import com.jiuyu.replay.system.bo.SeoArticleListBo;
import com.jiuyu.replay.system.constant.SeoConstant;
import com.jiuyu.replay.system.entity.SeoArticleEntity;
import com.jiuyu.replay.system.entity.SeoCategoryEntity;
import com.jiuyu.replay.system.repository.dao.SeoArticleDao;
import com.jiuyu.replay.system.repository.dao.SeoCategoryDao;
import com.jiuyu.replay.system.repository.service.SeoArticleTagService;
import com.jiuyu.replay.system.repository.service.SeoSiteService;
import com.jiuyu.replay.system.repository.service.SeoSlugHistoryService;
import com.jiuyu.replay.system.repository.service.SeoSlugService;
import com.jiuyu.replay.system.repository.service.SeoTagService;
import com.jiuyu.replay.system.vo.SeoArticleInfoVo;
import com.jiuyu.replay.system.vo.SeoArticleListVo;
import com.jiuyu.replay.system.vo.SeoArticleTagVo;
import com.jiuyu.replay.system.vo.SeoIdCountVo;
import com.jiuyu.replay.system.vo.SeoSaveResultVo;
import com.jiuyu.replay.system.vo.SeoTagOptionVo;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * SeoArticleServiceImpl 单测，对应 openspec AC-6（publishTime 语义 + slugAppended）与 AC-9（聚合统计口径）。
 *
 * <p><b>为什么用 spy</b>：同 SeoCategoryServiceImplTest / SeoTagServiceImplTest——被测类继承
 * MyBatis-Plus 的 {@code ServiceImpl}，直接 new 出来 baseMapper 为 null，一调
 * {@code getById / save / updateById / page / list / listMaps / update} 就 NPE。
 * 用 {@link Mockito#spy} 部分模拟：业务方法跑真实逻辑，继承来的 CRUD 方法逐个打桩。
 *
 * <p><b>「不覆盖 publishTime」为什么断言 null 而不是断言旧值</b>：updateArticle 走
 * {@code updateById(entity)}，MP 未配置 update-strategy（见 application-*.yml，全局只配了
 * logic-delete-field），默认 NOT_NULL——实体上为 null 的字段不进 SET 片段，列值原样保留。
 * 因此「不得覆盖 / 不得清空」在这一层的可观测形态就是「实体的 publishTime 保持 null」。
 * changeStatus 走 LambdaUpdateWrapper，可观测形态则是 SET 片段里没有 publish_time。
 *
 * <p><b>删除断言为什么盯着 {@code update(Wrapper)} 的 SET 片段</b>：MP 全局配了
 * {@code logic-delete-field: isDeleted}，{@code updateById} 会把该字段从 SET 里剔除——
 * 手动 setIsDeleted 被静默丢弃，结果是 slug 改成了墓碑值但记录仍可见。
 *
 * <p><b>列表不返回正文断言在 {@code getSqlSelect()} 上</b>：选择的列在 sqlSelect 里，
 * 不在 {@code getTargetSql()} 渲染的 WHERE 片段里。若拿 targetSql 去断言「不含 content」，
 * 那是一条恒真的空断言。若 {@code .select(...)} 被删掉，sqlSelect 会变为空、MP 退化成
 * SELECT *（含正文），故这里同时断言 sqlSelect 非空并含预期列作为正向对照。
 *
 * <p>被打桩的继承方法即「与数据库的边界」：真正的 SQL 执行、唯一索引冲突、事务回滚
 * 不在单测覆盖范围内。
 *
 * @author claude
 * @date 2026-08-13
 */
@DisplayName("SEO 文章管理")
class SeoArticleServiceImplTest {

    private static final long ARTICLE_ID = 1955000000000000001L;

    private static final long CATEGORY_ID = 1955000000000000009L;

    private static final long ENABLED_TAG_ID = 2001L;

    private static final long DISABLED_TAG_ID = 2002L;

    private static final String ARTICLE_TITLE = "直播复盘";

    private static final String ARTICLE_SLUG = "zhibofupan";

    private static final String CATEGORY_NAME = "运营方法论";

    /** 首次发布时间基线：用于断言「重新发布 / 下架」不覆盖也不清空 */
    private static final LocalDateTime FIRST_PUBLISH_TIME = LocalDateTime.of(2026, 1, 1, 8, 30);

    private SeoSlugService seoSlugService;

    private SeoArticleTagService seoArticleTagService;

    private SeoTagService seoTagService;

    private SeoCategoryDao seoCategoryDao;

    /** slug 变更时记历史，供官网 301 */
    private SeoSlugHistoryService seoSlugHistoryService;

    private SeoArticleServiceImpl seoArticleService;

    /** 列表查询已下沉到 Dao + XML，需要单独 mock 它 */
    private SeoArticleDao seoArticleDao;

    /** 注册实体到 MP 的 lambda 列名缓存，否则 LambdaQueryWrapper 构造即抛异常 */
    @BeforeAll
    static void initLambdaColumnCache() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        assistant.setCurrentNamespace("seoArticleServiceImplTest");
        TableInfoHelper.initTableInfo(assistant, SeoArticleEntity.class);
        TableInfoHelper.initTableInfo(assistant, SeoCategoryEntity.class);
    }

    /** 内容变更后必须调它让官网缓存失效，测试里断言这一点 */
    private SeoSiteService seoSiteService;

    @BeforeEach
    void setUp() {
        seoSiteService = Mockito.mock(SeoSiteService.class);
        seoSlugService = Mockito.mock(SeoSlugService.class);
        seoArticleTagService = Mockito.mock(SeoArticleTagService.class);
        seoTagService = Mockito.mock(SeoTagService.class);
        seoCategoryDao = Mockito.mock(SeoCategoryDao.class);
        seoSlugHistoryService = Mockito.mock(SeoSlugHistoryService.class);
        seoArticleService = Mockito.spy(new SeoArticleServiceImpl(
                seoSlugService, seoArticleTagService, seoTagService, seoCategoryDao,
                seoSlugHistoryService, seoSiteService));
        seoArticleDao = Mockito.mock(SeoArticleDao.class);
        doReturn(seoArticleDao).when(seoArticleService).getBaseMapper();
    }

    @Nested
    @DisplayName("首次发布时间语义（AC-6）")
    class PublishTimeSemantics {

        @Test
        @DisplayName("新建即发布时写入 publishTime")
        void saveArticle_statusPublished_writesPublishTime() {
            stubSaveHappyPath();

            seoArticleService.saveArticle(buildBo(null, SeoConstant.STATUS_ENABLED));

            SeoArticleEntity saved = captureSavedArticle();
            assertEquals(SeoConstant.STATUS_ENABLED, saved.getArticleStatus());
            assertNotNull(saved.getPublishTime(), "新建即发布必须落首次发布时间，否则官网无法排序也无法出 sitemap");
        }

        @Test
        @DisplayName("新建为已下架时 publishTime 保持为 null")
        void saveArticle_statusUnpublished_leavesPublishTimeNull() {
            stubSaveHappyPath();

            seoArticleService.saveArticle(buildBo(null, SeoConstant.STATUS_DISABLED));

            SeoArticleEntity saved = captureSavedArticle();
            assertEquals(SeoConstant.STATUS_DISABLED, saved.getArticleStatus());
            assertNull(saved.getPublishTime(),
                    "未发布过的文章不该有发布时间，实际：" + saved.getPublishTime());
        }

        // 「status 未传时默认为已发布」的用例已刻意不写：该缺省值正在被移除
        // （改为 status 必填 + 取值校验），断言它等于把一个待删除的行为焊死在测试里。
        // 待 validate() 补上 status 校验后，应在 InputValidation 组补两条：
        // saveArticle_nullStatus_throwsBusinessException / saveArticle_statusOutOfRange_throwsBusinessException。

        @Test
        @DisplayName("编辑：此前从未发布过且本次转为已发布时写入 publishTime")
        void updateArticle_neverPublishedAndNowPublishing_writesPublishTime() {
            AtomicReference<LambdaUpdateWrapper<SeoArticleEntity>> ref = stubUpdateHappyPath(existingArticle(null));

            seoArticleService.updateArticle(buildBo(ARTICLE_ID, SeoConstant.STATUS_ENABLED));

            assertTrue(ref.get().getSqlSet().contains("publish_time"),
                    "首次由下架转发布时必须补上发布时间。实际 SET：" + ref.get().getSqlSet());
        }

        @Test
        @DisplayName("编辑：原记录已有 publishTime 时重新发布不覆盖首次发布时间")
        void updateArticle_alreadyPublished_doesNotOverwritePublishTime() {
            AtomicReference<LambdaUpdateWrapper<SeoArticleEntity>> ref =
                    stubUpdateHappyPath(existingArticle(FIRST_PUBLISH_TIME));

            seoArticleService.updateArticle(buildBo(ARTICLE_ID, SeoConstant.STATUS_ENABLED));

            String sqlSet = ref.get().getSqlSet();
            assertFalse(sqlSet.contains("publish_time"),
                    "publish_time 不得进 SET；一旦写入就会把首次发布时间刷成当前时间，"
                            + "搜索引擎会据此重新判定内容时效。实际 SET：" + sqlSet);
            assertTrue(sqlSet.contains("status"), "状态本身仍要写入。实际 SET：" + sqlSet);
        }

        @Test
        @DisplayName("编辑：原记录已有 publishTime 时下架不清空首次发布时间")
        void updateArticle_alreadyPublishedAndNowUnpublishing_doesNotClearPublishTime() {
            AtomicReference<LambdaUpdateWrapper<SeoArticleEntity>> ref =
                    stubUpdateHappyPath(existingArticle(FIRST_PUBLISH_TIME));

            seoArticleService.updateArticle(buildBo(ARTICLE_ID, SeoConstant.STATUS_DISABLED));

            String sqlSet = ref.get().getSqlSet();
            assertFalse(sqlSet.contains("publish_time"),
                    "下架只是不对外展示，重新上架后首次发布时间应还在。实际 SET：" + sqlSet);
            assertTrue(sqlSet.contains("status"), "实际 SET：" + sqlSet);
        }

        @Test
        @DisplayName("编辑：从未发布且本次仍为下架时不写 publishTime")
        void updateArticle_neverPublishedAndStaysUnpublished_leavesPublishTimeNull() {
            AtomicReference<LambdaUpdateWrapper<SeoArticleEntity>> ref = stubUpdateHappyPath(existingArticle(null));

            seoArticleService.updateArticle(buildBo(ARTICLE_ID, SeoConstant.STATUS_DISABLED));

            assertFalse(ref.get().getSqlSet().contains("publish_time"),
                    "实际 SET：" + ref.get().getSqlSet());
        }

        @Test
        @DisplayName("状态变更：首次发布把 publish_time 写进 SET 片段")
        void changeStatus_firstPublish_writesPublishTimeIntoSetClause() {
            doReturn(existingArticle(null)).when(seoArticleService).getById(ARTICLE_ID);
            AtomicReference<LambdaUpdateWrapper<SeoArticleEntity>> wrapperRef = stubUpdateByWrapper();

            seoArticleService.changeStatus(ARTICLE_ID, SeoConstant.STATUS_ENABLED);

            LambdaUpdateWrapper<SeoArticleEntity> wrapper = wrapperRef.get();
            assertNotNull(wrapper, "未发出状态更新");
            assertTrue(wrapper.getSqlSet().contains("publish_time="),
                    "首次发布必须落 publishTime，实际 SET：" + wrapper.getSqlSet());
            assertTrue(wrapper.getSqlSet().contains("status="),
                    "实际 SET：" + wrapper.getSqlSet());
            assertTrue(wrapper.getSqlSet().contains("update_date="),
                    "状态变更须刷新 updateDate，实际 SET：" + wrapper.getSqlSet());
            assertTrue(paramsOf(wrapper).contains(ARTICLE_ID),
                    "更新须限定到目标文章，实际参数：" + paramsOf(wrapper));
        }

        @Test
        @DisplayName("状态变更：已有 publishTime 时重新发布不把 publish_time 放进 SET")
        void changeStatus_republish_omitsPublishTimeFromSetClause() {
            doReturn(existingArticle(FIRST_PUBLISH_TIME)).when(seoArticleService).getById(ARTICLE_ID);
            AtomicReference<LambdaUpdateWrapper<SeoArticleEntity>> wrapperRef = stubUpdateByWrapper();

            seoArticleService.changeStatus(ARTICLE_ID, SeoConstant.STATUS_ENABLED);

            LambdaUpdateWrapper<SeoArticleEntity> wrapper = wrapperRef.get();
            assertFalse(wrapper.getSqlSet().contains("publish_time="),
                    "重新发布不得覆盖首次发布时间，实际 SET：" + wrapper.getSqlSet());
            assertTrue(wrapper.getSqlSet().contains("status="),
                    "状态本身仍要写入，实际 SET：" + wrapper.getSqlSet());
        }

        @Test
        @DisplayName("状态变更：下架不把 publish_time 放进 SET，首次发布时间不被清空")
        void changeStatus_unpublish_omitsPublishTimeFromSetClause() {
            doReturn(existingArticle(FIRST_PUBLISH_TIME)).when(seoArticleService).getById(ARTICLE_ID);
            AtomicReference<LambdaUpdateWrapper<SeoArticleEntity>> wrapperRef = stubUpdateByWrapper();

            seoArticleService.changeStatus(ARTICLE_ID, SeoConstant.STATUS_DISABLED);

            LambdaUpdateWrapper<SeoArticleEntity> wrapper = wrapperRef.get();
            assertFalse(wrapper.getSqlSet().contains("publish_time="),
                    "下架不得清空首次发布时间，实际 SET：" + wrapper.getSqlSet());
            assertTrue(paramsOf(wrapper).contains(SeoConstant.STATUS_DISABLED),
                    "实际参数：" + paramsOf(wrapper));
        }

        @Test
        @DisplayName("状态变更：从未发布过时下架同样不写 publish_time")
        void changeStatus_neverPublishedAndUnpublishing_omitsPublishTime() {
            doReturn(existingArticle(null)).when(seoArticleService).getById(ARTICLE_ID);
            AtomicReference<LambdaUpdateWrapper<SeoArticleEntity>> wrapperRef = stubUpdateByWrapper();

            seoArticleService.changeStatus(ARTICLE_ID, SeoConstant.STATUS_DISABLED);

            assertFalse(wrapperRef.get().getSqlSet().contains("publish_time="),
                    "下架不是发布动作，实际 SET：" + wrapperRef.get().getSqlSet());
        }
    }

    @Nested
    @DisplayName("分类文章数统计口径（AC-9，自分类测试移交）")
    class CategoryArticleCount {

        @Test
        @DisplayName("聚合结果按分类 ID 组装成 Map")
        void countByCategoryIds_daoRows_mappedById() {
            stubCountByCategoryIds(List.of(countRow(CATEGORY_ID, 3), countRow(999L, 1)));

            Map<Long, Integer> counts = seoArticleService.countByCategoryIds(List.of(CATEGORY_ID, 999L));

            assertEquals(3, counts.get(CATEGORY_ID));
            assertEquals(1, counts.get(999L));
        }

        @Test
        @DisplayName("没有文章的分类不在聚合结果里——调用方需自己兜底为 0")
        void countByCategoryIds_categoryWithoutArticles_isAbsentFromResult() {
            // GROUP BY 只会返回有行的分组，0 篇的分类根本不出现在结果集中
            stubCountByCategoryIds(List.of(countRow(CATEGORY_ID, 3)));

            Map<Long, Integer> counts = seoArticleService.countByCategoryIds(List.of(CATEGORY_ID, 999L));

            assertEquals(3, counts.get(CATEGORY_ID));
            assertNull(counts.get(999L), "0 篇的分类不该被凭空补进 Map，由展示层 getOrDefault(0)");
        }

        @Test
        @DisplayName("超过 500 个分类时分批查询——IN 列表过长会拖垮查询")
        void countByCategoryIds_idsBeyondBatchSize_splitsIntoMultipleQueries() {
            List<Long> manyIds = new ArrayList<>();
            for (long i = 0; i < SeoConstant.IN_BATCH_SIZE + 10; i++) {
                manyIds.add(i);
            }
            List<List<Long>> batches = stubCountByCategoryIds(new ArrayList<>());

            seoArticleService.countByCategoryIds(manyIds);

            assertEquals(2, batches.size(), "应按 IN_BATCH_SIZE 分成两批");
            assertEquals(SeoConstant.IN_BATCH_SIZE, batches.get(0).size());
            assertEquals(10, batches.get(1).size());
        }

        @Test
        @DisplayName("空入参直接返回空 Map，不查库")
        void countByCategoryIds_emptyInput_returnsEmptyWithoutQuery() {
            List<List<Long>> batches = stubCountByCategoryIds(new ArrayList<>());

            assertTrue(seoArticleService.countByCategoryIds(new ArrayList<>()).isEmpty());
            assertTrue(seoArticleService.countByCategoryIds(null).isEmpty());
            assertTrue(batches.isEmpty(), "空入参不该触发查询");
        }

        @Test
        @DisplayName("未删除标记按常量传入")
        void countByCategoryIds_anyCategories_passesNotDeletedFlag() {
            stubCountByCategoryIds(new ArrayList<>());

            seoArticleService.countByCategoryIds(List.of(CATEGORY_ID));

            verify(seoArticleDao).countByCategoryIds(anyList(), eq(SeoConstant.NOT_DELETED));
        }
    }

    @Nested
    @DisplayName("文章列表与详情")
    class ArticleListingAndInfo {

        @Test
        @DisplayName("查询条件原样传给 Dao——SQL 本身在 XML 里，Java 侧只负责传对参数")
        void queryPage_filtersGiven_passesThemToDao() {
            AtomicReference<SeoArticleListBo> boRef = stubArticlePage(new ArrayList<>(), 0L);
            SeoArticleListBo input = buildListBo("复盘", CATEGORY_ID, SeoConstant.STATUS_DISABLED, 1, 10);
            input.setTagId(555L);

            seoArticleService.queryPage(input);

            SeoArticleListBo passed = boRef.get();
            assertNotNull(passed, "Dao 没被调用");
            assertEquals("复盘", passed.getTitle());
            assertEquals(CATEGORY_ID, passed.getCategoryId());
            assertEquals(SeoConstant.STATUS_DISABLED, passed.getArticleStatus());
            assertEquals(555L, passed.getTagId());
        }

        @Test
        @DisplayName("未删除标记按常量传入，不在 XML 里写死 0")
        void queryPage_anyQuery_passesNotDeletedFlag() {
            stubArticlePage(new ArrayList<>(), 0L);

            seoArticleService.queryPage(buildListBo(null, null, null, 1, 10));

            // XML 用 #{notDeleted} 占位而非硬编码 0：逻辑删除的取值口径只该有一处定义
            verify(seoArticleDao).selectArticlePage(any(), any(), eq(SeoConstant.NOT_DELETED));
        }

        @Test
        @DisplayName("空条件原样传下去，由 XML 的 <if> 决定拼不拼")
        void queryPage_filtersOmitted_passesNullsToDao() {
            AtomicReference<SeoArticleListBo> boRef = stubArticlePage(new ArrayList<>(), 0L);

            seoArticleService.queryPage(buildListBo(null, null, null, 1, 10));

            SeoArticleListBo passed = boRef.get();
            assertNull(passed.getTitle());
            assertNull(passed.getCategoryId());
            assertNull(passed.getArticleStatus());
            assertNull(passed.getTagId());
        }

        @Test
        @DisplayName("列表的 tags 带 status，且已禁用的标签也返回")
        void queryPage_articleWithDisabledTag_returnsTagWithStatus() {
            stubArticlePage(List.of(listRecord()), 1L);
            stubCategoryNames();
            stubArticleTags();

            PageUtils<SeoArticleListVo> result = seoArticleService.queryPage(buildListBo(null, null, null, 1, 10));

            List<SeoArticleTagVo> tags = result.getList().get(0).getTags();
            assertEquals(2, tags.size(), "已禁用的标签也要返回，列表需据此置灰标注。实际：" + tags);
            assertEquals(ENABLED_TAG_ID, tags.get(0).getId());
            assertEquals(SeoConstant.STATUS_ENABLED, tags.get(0).getTagStatus());
            assertEquals(DISABLED_TAG_ID, tags.get(1).getId(),
                    "禁用标签被过滤掉了，运营在列表上看不出它已不对外展示");
            assertEquals(SeoConstant.STATUS_DISABLED, tags.get(1).getTagStatus(),
                    "tags 必须带 status，否则前端无从置灰");
        }

        @Test
        @DisplayName("列表回填分类名，并复用分页元数据")
        void queryPage_records_fillsCategoryNameAndPageMetadata() {
            stubArticlePage(List.of(listRecord()), 25L);
            stubCategoryNames();

            PageUtils<SeoArticleListVo> result = seoArticleService.queryPage(buildListBo(null, null, null, 2, 10));

            assertEquals(CATEGORY_NAME, result.getList().get(0).getCategoryName());
            assertEquals(ARTICLE_TITLE, result.getList().get(0).getTitle());
            assertEquals(25, result.getTotalCount());
            assertEquals(10, result.getPageSize());
            assertEquals(2, result.getCurrPage());
            assertEquals(3, result.getTotalPage());
        }

        @Test
        @DisplayName("空页返回空列表，不再发起分类名与标签查询")
        void queryPage_emptyPage_returnsEmptyListWithoutExtraQueries() {
            stubArticlePage(new ArrayList<>(), 0L);

            PageUtils<SeoArticleListVo> result = seoArticleService.queryPage(buildListBo(null, null, null, 1, 10));

            assertTrue(result.getList().isEmpty());
            verifyNoInteractions(seoCategoryDao);
            verifyNoInteractions(seoArticleTagService);
            verifyNoInteractions(seoTagService);
        }

        @Test
        @DisplayName("无关联标签的文章 tags 为空列表而非 null")
        void queryPage_articleWithoutTags_returnsEmptyTagList() {
            stubArticlePage(List.of(listRecord()), 1L);
            stubCategoryNames();

            List<SeoArticleTagVo> tags = seoArticleService
                    .queryPage(buildListBo(null, null, null, 1, 10)).getList().get(0).getTags();

            assertNotNull(tags, "tags 为 null 会让前端渲染报错");
            assertTrue(tags.isEmpty());
        }

        @Test
        @DisplayName("详情返回正文，且已禁用的标签也带 status 返回")
        void info_existingArticle_returnsContentAndDisabledTags() {
            SeoArticleEntity entity = existingArticle(FIRST_PUBLISH_TIME);
            entity.setContent("<h2>小节</h2><p>正文</p>");
            doReturn(entity).when(seoArticleService).getById(ARTICLE_ID);
            stubCategoryNames();
            stubArticleTags();

            SeoArticleInfoVo vo = seoArticleService.info(ARTICLE_ID);

            assertEquals("<h2>小节</h2><p>正文</p>", vo.getContent(), "详情必须返回正文");
            assertEquals(CATEGORY_NAME, vo.getCategoryName());
            assertEquals(2, vo.getTags().size(),
                    "编辑态若丢掉已禁用标签，保存时会被静默解绑。实际：" + vo.getTags());
            assertEquals(SeoConstant.STATUS_DISABLED, vo.getTags().get(1).getTagStatus());
        }

        @Test
        @DisplayName("详情查不到文章时抛业务异常")
        void info_articleNotFound_throwsBusinessException() {
            doReturn(null).when(seoArticleService).getById(ARTICLE_ID);

            BusinessException thrown = assertThrows(BusinessException.class,
                    () -> seoArticleService.info(ARTICLE_ID));

            assertTrue(thrown.getMessage().contains("不存在"), "实际：" + thrown.getMessage());
        }

        @Test
        @DisplayName("详情命中已删除文章时抛业务异常，不把墓碑数据回显给运营")
        void info_articleAlreadyDeleted_throwsBusinessException() {
            SeoArticleEntity deleted = existingArticle(FIRST_PUBLISH_TIME);
            deleted.setIsDeleted(SeoConstant.DELETED);
            doReturn(deleted).when(seoArticleService).getById(ARTICLE_ID);

            assertThrows(BusinessException.class, () -> seoArticleService.info(ARTICLE_ID));
        }
    }

    @Nested
    @DisplayName("正文 XSS 过滤与 h1 降级")
    class ContentSanitization {

        private static final String RAW_CONTENT =
                "<h1>小节标题</h1><script>alert(1)</script><p>正文</p>";

        @Test
        @DisplayName("新建时正文的 script 在入库前被剥离")
        void saveArticle_contentWithScriptTag_stripsScriptBeforePersist() {
            stubSaveHappyPath();
            SeoArticleBo bo = buildBo(null, SeoConstant.STATUS_ENABLED);
            bo.setContent(RAW_CONTENT);

            seoArticleService.saveArticle(bo);

            String content = captureSavedArticle().getContent();
            assertFalse(content.contains("script"), "落库正文仍含 script，实际：" + content);
            assertFalse(content.contains("alert"), "script 内容也应剥离，实际：" + content);
            assertTrue(content.contains("正文"), "正常文字不应被误删，实际：" + content);
        }

        @Test
        @DisplayName("新建时正文的 h1 降级为 h2 且标题文字不丢")
        void saveArticle_contentWithH1_downgradesH1ToH2KeepingText() {
            stubSaveHappyPath();
            SeoArticleBo bo = buildBo(null, SeoConstant.STATUS_ENABLED);
            bo.setContent(RAW_CONTENT);

            seoArticleService.saveArticle(bo);

            String content = captureSavedArticle().getContent();
            assertTrue(content.contains("<h2>小节标题</h2>"),
                    "官网用文章标题渲染唯一 H1，正文 h1 须降级为 h2 且保留层级。实际：" + content);
            assertFalse(content.contains("<h1"), "实际：" + content);
        }

        @Test
        @DisplayName("编辑时正文同样过滤——两条写入路径不能只堵一条")
        void updateArticle_contentWithScriptTag_stripsScriptBeforePersist() {
            AtomicReference<LambdaUpdateWrapper<SeoArticleEntity>> ref =
                    stubUpdateHappyPath(existingArticle(FIRST_PUBLISH_TIME));
            SeoArticleBo bo = buildBo(ARTICLE_ID, SeoConstant.STATUS_ENABLED);
            bo.setContent(RAW_CONTENT);

            seoArticleService.updateArticle(bo);

            // updateArticle 显式 set 字段，值落在 wrapper 的参数表里而非实体上
            String content = ref.get().getParamNameValuePairs().values().stream()
                    .filter(String.class::isInstance).map(String.class::cast)
                    .filter(value -> value.contains("小节标题"))
                    .findFirst().orElse("");
            assertFalse(content.isEmpty(), "未找到正文参数，实际参数：" + ref.get().getParamNameValuePairs());
            assertFalse(content.contains("script"), "实际：" + content);
            assertTrue(content.contains("<h2>小节标题</h2>"), "实际：" + content);
        }
    }

    @Nested
    @DisplayName("入参校验")
    class InputValidation {

        @Test
        @DisplayName("标题为空时抛业务异常，异常消息指明是标题")
        void saveArticle_blankTitle_throwsBusinessException() {
            for (String title : new String[]{null, "", "   "}) {
                SeoArticleBo bo = buildBo(null, SeoConstant.STATUS_ENABLED);
                bo.setTitle(title);

                BusinessException thrown = assertThrows(BusinessException.class,
                        () -> seoArticleService.saveArticle(bo), "输入 [" + title + "] 应被拒绝");
                assertTrue(thrown.getMessage().contains("标题"), "实际：" + thrown.getMessage());
            }
            verify(seoArticleService, never()).save(any(SeoArticleEntity.class));
        }

        @Test
        @DisplayName("status 缺失时抛业务异常——绝不能默认为「已发布」把草稿发上线")
        void saveArticle_nullStatus_throwsBusinessException() {
            SeoArticleBo bo = buildBo(null, SeoConstant.STATUS_ENABLED);
            bo.setArticleStatus(null);

            BusinessException thrown = assertThrows(BusinessException.class,
                    () -> seoArticleService.saveArticle(bo));

            assertTrue(thrown.getMessage().contains("状态"), "实际：" + thrown.getMessage());
            verify(seoArticleService, never()).save(any(SeoArticleEntity.class));
        }

        @Test
        @DisplayName("status 越界时抛业务异常，不把无法解释的状态写进库")
        void saveArticle_statusOutOfRange_throwsBusinessException() {
            for (Integer status : new Integer[]{2, 5, -1, 99}) {
                SeoArticleBo bo = buildBo(null, SeoConstant.STATUS_ENABLED);
                bo.setArticleStatus(status);

                BusinessException thrown = assertThrows(BusinessException.class,
                        () -> seoArticleService.saveArticle(bo), "status=" + status + " 应被拒绝");
                assertTrue(thrown.getMessage().contains("状态"), "实际：" + thrown.getMessage());
            }
            verify(seoArticleService, never()).save(any(SeoArticleEntity.class));
        }

        @Test
        @DisplayName("标题超长时抛业务异常且消息带实际字数，而非撞 MySQL 1406 报 500")
        void saveArticle_titleExceedsLimit_throwsBusinessExceptionWithActualLength() {
            SeoArticleBo bo = buildBo(null, SeoConstant.STATUS_ENABLED);
            bo.setTitle("x".repeat(101));

            BusinessException thrown = assertThrows(BusinessException.class,
                    () -> seoArticleService.saveArticle(bo));

            assertTrue(thrown.getMessage().contains("100"), "应说明上限，实际：" + thrown.getMessage());
            assertTrue(thrown.getMessage().contains("101"), "应说明当前字数，实际：" + thrown.getMessage());
        }

        @Test
        @DisplayName("标题正好 100 字放行——边界不能写成 >=")
        void saveArticle_titleAtLimit_passes() {
            stubSaveHappyPath();
            SeoArticleBo bo = buildBo(null, SeoConstant.STATUS_ENABLED);
            bo.setTitle("x".repeat(100));

            assertDoesNotThrow(() -> seoArticleService.saveArticle(bo));
        }

        @Test
        @DisplayName("tagIds 含 null 元素时被剔除——关联表 tag_id 是 NOT NULL")
        void saveArticle_tagIdsContainingNull_filtersNullBeforePersist() {
            stubSaveHappyPath();
            SeoArticleBo bo = buildBo(null, SeoConstant.STATUS_ENABLED);
            bo.setTagIds(java.util.Arrays.asList(1001L, null, 1002L));

            seoArticleService.saveArticle(bo);

            ArgumentCaptor<List<Long>> captor = ArgumentCaptor.forClass(List.class);
            verify(seoArticleTagService).replaceArticleTags(any(), captor.capture());
            assertFalse(captor.getValue().contains(null),
                    "含 null 的 tagId 会 INSERT 出 tag_id=NULL 撞 1048。实际：" + captor.getValue());
            assertEquals(2, captor.getValue().size(), "实际：" + captor.getValue());
        }

        @Test
        @DisplayName("封面图为空时抛业务异常，异常消息指明是封面")
        void saveArticle_blankCoverUrl_throwsBusinessException() {
            SeoArticleBo bo = buildBo(null, SeoConstant.STATUS_ENABLED);
            bo.setCoverUrl("  ");

            BusinessException thrown = assertThrows(BusinessException.class,
                    () -> seoArticleService.saveArticle(bo));

            assertTrue(thrown.getMessage().contains("封面"), "实际：" + thrown.getMessage());
            verify(seoArticleService, never()).save(any(SeoArticleEntity.class));
        }

        @Test
        @DisplayName("SEO 描述为空时抛业务异常，异常消息指明是 SEO 描述")
        void saveArticle_blankSeoDescription_throwsBusinessException() {
            SeoArticleBo bo = buildBo(null, SeoConstant.STATUS_ENABLED);
            bo.setSeoDescription("");

            BusinessException thrown = assertThrows(BusinessException.class,
                    () -> seoArticleService.saveArticle(bo));

            assertTrue(thrown.getMessage().contains("SEO"), "实际：" + thrown.getMessage());
            verify(seoArticleService, never()).save(any(SeoArticleEntity.class));
        }

        @Test
        @DisplayName("正文为空时抛业务异常，异常消息指明是正文")
        void saveArticle_blankContent_throwsBusinessException() {
            SeoArticleBo bo = buildBo(null, SeoConstant.STATUS_ENABLED);
            bo.setContent("   ");

            BusinessException thrown = assertThrows(BusinessException.class,
                    () -> seoArticleService.saveArticle(bo));

            assertTrue(thrown.getMessage().contains("正文"), "实际：" + thrown.getMessage());
            verify(seoArticleService, never()).save(any(SeoArticleEntity.class));
        }

        @Test
        @DisplayName("未选分类时抛业务异常，且不去查分类表")
        void saveArticle_nullCategoryId_throwsBusinessException() {
            SeoArticleBo bo = buildBo(null, SeoConstant.STATUS_ENABLED);
            bo.setCategoryId(null);

            BusinessException thrown = assertThrows(BusinessException.class,
                    () -> seoArticleService.saveArticle(bo));

            assertTrue(thrown.getMessage().contains("分类"), "实际：" + thrown.getMessage());
            verifyNoInteractions(seoCategoryDao);
            verify(seoArticleService, never()).save(any(SeoArticleEntity.class));
        }

        @Test
        @DisplayName("分类不存在或已删除时抛业务异常，提示重新选择")
        void saveArticle_categoryNotFoundOrDeleted_throwsBusinessException() {
            // 分类校验查询自带 isDeleted 过滤，已删除的分类同样查不到，返回 null
            when(seoCategoryDao.selectOne(ArgumentMatchers.<Wrapper<SeoCategoryEntity>>any())).thenReturn(null);

            BusinessException thrown = assertThrows(BusinessException.class,
                    () -> seoArticleService.saveArticle(buildBo(null, SeoConstant.STATUS_ENABLED)));

            assertTrue(thrown.getMessage().contains("分类"), "实际：" + thrown.getMessage());
            assertTrue(thrown.getMessage().contains("重新选择"),
                    "需告诉运营下一步怎么办，实际：" + thrown.getMessage());
            verify(seoArticleService, never()).save(any(SeoArticleEntity.class));
        }

        @Test
        @DisplayName("分类校验查询排除已删除分类，堵住「选中后分类被删」的空窗")
        void saveArticle_categoryCheck_filtersDeletedCategory() {
            AtomicReference<String> sqlRef = new AtomicReference<>("");
            when(seoCategoryDao.selectOne(ArgumentMatchers.<Wrapper<SeoCategoryEntity>>any()))
                    .thenAnswer(invocation -> {
                        LambdaQueryWrapper<SeoCategoryEntity> wrapper = invocation.getArgument(0);
                        sqlRef.set(wrapper.getTargetSql());
                        return null;
                    });

            assertThrows(BusinessException.class,
                    () -> seoArticleService.saveArticle(buildBo(null, SeoConstant.STATUS_ENABLED)));

            assertTrue(sqlRef.get().contains("is_deleted ="),
                    "分类存在性校验须排除已删除分类，实际 SQL：" + sqlRef.get());
            assertTrue(sqlRef.get().contains("id ="), "实际 SQL：" + sqlRef.get());
        }

        @Test
        @DisplayName("标签超过 10 个时抛业务异常，异常消息带出上限")
        void saveArticle_tagsBeyondLimit_throwsBusinessException() {
            SeoArticleBo bo = buildBo(null, SeoConstant.STATUS_ENABLED);
            bo.setTagIds(tagIds(SeoConstant.MAX_TAGS_PER_ARTICLE + 1));

            BusinessException thrown = assertThrows(BusinessException.class,
                    () -> seoArticleService.saveArticle(bo));

            assertTrue(thrown.getMessage().contains("标签"), "实际：" + thrown.getMessage());
            assertTrue(thrown.getMessage().contains(String.valueOf(SeoConstant.MAX_TAGS_PER_ARTICLE)),
                    "需带出上限数字，实际：" + thrown.getMessage());
            verify(seoArticleService, never()).save(any(SeoArticleEntity.class));
        }

        @Test
        @DisplayName("标签正好 10 个时放行——边界值不能被误拒")
        void saveArticle_tagsAtLimit_savesSuccessfully() {
            stubSaveHappyPath();
            SeoArticleBo bo = buildBo(null, SeoConstant.STATUS_ENABLED);
            List<Long> ids = tagIds(SeoConstant.MAX_TAGS_PER_ARTICLE);
            bo.setTagIds(ids);

            seoArticleService.saveArticle(bo);

            verify(seoArticleService).save(any(SeoArticleEntity.class));
            verify(seoArticleTagService).replaceArticleTags(any(), eq(ids));
        }

        @Test
        @DisplayName("编辑时同样走校验，标题为空不落 update")
        void updateArticle_blankTitle_throwsWithoutUpdate() {
            stubUpdateHappyPath(existingArticle(FIRST_PUBLISH_TIME));
            SeoArticleBo bo = buildBo(ARTICLE_ID, SeoConstant.STATUS_ENABLED);
            bo.setTitle("   ");

            assertThrows(BusinessException.class, () -> seoArticleService.updateArticle(bo));

            verify(seoArticleService, never()).updateById(any());
            verifyNoInteractions(seoSlugService);
        }
    }

    @Nested
    @DisplayName("文章删除")
    class ArticleDeletion {

        @Test
        @DisplayName("is_deleted 必须进 SET 片段，不能走 updateById")
        void deleteArticles_existingArticles_putsIsDeletedIntoSetClause() {
            List<LambdaUpdateWrapper<SeoArticleEntity>> wrappers = stubBaseMapperUpdate(1);

            seoArticleService.deleteArticles(List.of(ARTICLE_ID));

            LambdaUpdateWrapper<SeoArticleEntity> wrapper = wrappers.get(0);
            assertTrue(wrapper.getSqlSet().contains("is_deleted="),
                    "MP 全局逻辑删除会让 updateById 把 is_deleted 从 SET 中剔除，记录会「改了名却还在」。"
                            + "实际 SET：" + wrapper.getSqlSet());
            assertTrue(wrapper.getSqlSet().contains("update_date="),
                    "删除须刷新 updateDate，实际 SET：" + wrapper.getSqlSet());
            assertTrue(paramsOf(wrapper).contains(SeoConstant.DELETED),
                    "实际参数：" + paramsOf(wrapper));
            verify(seoArticleService, never()).updateById(any());
        }

        @Test
        @DisplayName("slug 用 CONCAT 就地改写为 {原值}__del_{id}，不先查再写")
        void deleteArticles_existingArticles_writesTombstoneSlug() {
            List<LambdaUpdateWrapper<SeoArticleEntity>> wrappers = stubBaseMapperUpdate(1);

            seoArticleService.deleteArticles(List.of(ARTICLE_ID));

            String sqlSet = wrappers.get(0).getSqlSet();
            // 墓碑值由数据库拼：不这么做就得先 SELECT 出 slug 再逐条 UPDATE，
            // 两步之间别人改了 slug，写回的墓碑值就基于旧值了
            assertTrue(sqlSet.contains("slug = CONCAT(slug, '__del_', id)"),
                    "slug 未用 CONCAT 就地改写，唯一索引可能仍被已删记录占用。实际 SET：" + sqlSet);
            // 不再需要先把行捞出来
            verify(seoArticleService, never()).list(ArgumentMatchers.<Wrapper<SeoArticleEntity>>any());
        }

        @Test
        @DisplayName("整批走一条 UPDATE，WHERE 同时限定 IN 与未删除")
        void deleteArticles_multipleArticles_singleUpdateScopedToIds() {
            List<LambdaUpdateWrapper<SeoArticleEntity>> wrappers = stubBaseMapperUpdate(2);

            seoArticleService.deleteArticles(List.of(1001L, 1002L));

            assertEquals(1, wrappers.size(), "一批应只发一条 update，实际发了 " + wrappers.size() + " 条");
            String sql = wrappers.get(0).getTargetSql();
            assertTrue(sql.contains("IN"), "缺少 IN 条件会全表更新，实际 WHERE：" + sql);
            // 必须带 is_deleted=0：否则重复删同一批会把墓碑后缀叠加成 xxx__del_1__del_1
            assertTrue(sql.contains("is_deleted ="), "实际 WHERE：" + sql);
            Collection<Object> params = paramsOf(wrappers.get(0));
            assertTrue(params.contains(1001L) && params.contains(1002L), "实际参数：" + params);
        }

        @Test
        @DisplayName("删除同时清理标签关联")
        void deleteArticles_existingArticles_clearsTagRelations() {
            List<Long> ids = List.of(ARTICLE_ID);
            stubBaseMapperUpdate(1);

            seoArticleService.deleteArticles(ids);

            verify(seoArticleTagService).removeByArticleIds(ids);
        }

        @Test
        @DisplayName("空集合或全 null 时抛业务异常，不静默成功")
        void deleteArticles_emptyIds_throwsBusinessException() {
            List<Long> nullIds = new ArrayList<>();
            nullIds.add(null);

            assertThrows(BusinessException.class, () -> seoArticleService.deleteArticles(null));
            assertThrows(BusinessException.class, () -> seoArticleService.deleteArticles(new ArrayList<>()));
            assertThrows(BusinessException.class, () -> seoArticleService.deleteArticles(nullIds));

            verifyNoInteractions(seoArticleTagService);
        }

        @Test
        @DisplayName("一条都没删到时抛业务异常，不报「删除成功」")
        void deleteArticles_nothingAffected_throwsInsteadOfSilentSuccess() {
            stubBaseMapperUpdate(0);

            BusinessException thrown = assertThrows(BusinessException.class,
                    () -> seoArticleService.deleteArticles(List.of(9999L)));

            // 静默成功会让运营以为生效了，刷新后发现文章还在
            assertTrue(thrown.getMessage().contains("不存在或已被删除"), "实际：" + thrown.getMessage());
            verifyNoInteractions(seoArticleTagService);
        }
    }

    @Nested
    @DisplayName("状态变更与编辑的边界输入")
    class BoundaryInputs {

        @Test
        @DisplayName("状态变更传 null id 时抛业务异常，不查库")
        void changeStatus_nullId_throwsBusinessException() {
            BusinessException thrown = assertThrows(BusinessException.class,
                    () -> seoArticleService.changeStatus(null, SeoConstant.STATUS_ENABLED));

            assertTrue(thrown.getMessage().contains("文章"), "实际：" + thrown.getMessage());
            verify(seoArticleService, never()).getById(any());
            verify(seoArticleService, never()).update(ArgumentMatchers.<Wrapper<SeoArticleEntity>>any());
        }

        @Test
        @DisplayName("状态为 null 或越界时抛业务异常，避免把 status 写成脏值")
        void changeStatus_invalidStatus_throwsBusinessException() {
            for (Integer status : new Integer[]{null, 2, 5, -1}) {
                BusinessException thrown = assertThrows(BusinessException.class,
                        () -> seoArticleService.changeStatus(ARTICLE_ID, status),
                        "状态 [" + status + "] 应被拒绝");
                assertTrue(thrown.getMessage().contains("状态"), "实际：" + thrown.getMessage());
            }
            verify(seoArticleService, never()).update(ArgumentMatchers.<Wrapper<SeoArticleEntity>>any());
        }

        @Test
        @DisplayName("状态变更命中不存在的文章时抛业务异常，不落 update")
        void changeStatus_articleNotFound_throwsWithoutUpdate() {
            doReturn(null).when(seoArticleService).getById(ARTICLE_ID);

            assertThrows(BusinessException.class,
                    () -> seoArticleService.changeStatus(ARTICLE_ID, SeoConstant.STATUS_ENABLED));

            verify(seoArticleService, never()).update(ArgumentMatchers.<Wrapper<SeoArticleEntity>>any());
        }

        @Test
        @DisplayName("状态变更命中已删除文章时抛业务异常，不让已删内容重新上线")
        void changeStatus_articleAlreadyDeleted_throwsWithoutUpdate() {
            SeoArticleEntity deleted = existingArticle(null);
            deleted.setIsDeleted(SeoConstant.DELETED);
            doReturn(deleted).when(seoArticleService).getById(ARTICLE_ID);

            assertThrows(BusinessException.class,
                    () -> seoArticleService.changeStatus(ARTICLE_ID, SeoConstant.STATUS_ENABLED));

            verify(seoArticleService, never()).update(ArgumentMatchers.<Wrapper<SeoArticleEntity>>any());
        }

        @Test
        @DisplayName("编辑不存在的文章时抛业务异常，不落 update 也不算 slug")
        void updateArticle_articleNotFound_throwsWithoutUpdate() {
            doReturn(null).when(seoArticleService).getById(ARTICLE_ID);

            BusinessException thrown = assertThrows(BusinessException.class,
                    () -> seoArticleService.updateArticle(buildBo(ARTICLE_ID, SeoConstant.STATUS_ENABLED)));

            assertTrue(thrown.getMessage().contains("不存在"), "实际：" + thrown.getMessage());
            verify(seoArticleService, never()).updateById(any());
            verifyNoInteractions(seoSlugService);
            verifyNoInteractions(seoArticleTagService);
        }

        @Test
        @DisplayName("编辑已删除的文章时抛业务异常")
        void updateArticle_articleAlreadyDeleted_throwsWithoutUpdate() {
            SeoArticleEntity deleted = existingArticle(FIRST_PUBLISH_TIME);
            deleted.setIsDeleted(SeoConstant.DELETED);
            doReturn(deleted).when(seoArticleService).getById(ARTICLE_ID);

            assertThrows(BusinessException.class,
                    () -> seoArticleService.updateArticle(buildBo(ARTICLE_ID, SeoConstant.STATUS_ENABLED)));

            verify(seoArticleService, never()).updateById(any());
        }
    }

    @Nested
    @DisplayName("slug 回传与查重排除自身（AC-6）")
    class SlugResult {

        @Test
        @DisplayName("新增时 excludeId 传 null——新记录没有自身可排除")
        void saveArticle_newArticle_passesNullExcludeId() {
            stubSaveHappyPath();

            seoArticleService.saveArticle(buildBo(null, SeoConstant.STATUS_ENABLED));

            verify(seoSlugService).resolveSlug(
                    eq(SeoConstant.SLUG_TYPE_ARTICLE), eq(ARTICLE_TITLE), eq(ARTICLE_SLUG), isNull());
        }

        @Test
        @DisplayName("编辑时把自身 ID 作为 excludeId 传给查重，避免给自己追加后缀")
        void updateArticle_existingArticle_passesOwnIdAsExcludeId() {
            stubUpdateHappyPath(existingArticle(FIRST_PUBLISH_TIME));

            SeoSaveResultVo result = seoArticleService.updateArticle(
                    buildBo(ARTICLE_ID, SeoConstant.STATUS_ENABLED));

            verify(seoSlugService).resolveSlug(
                    eq(SeoConstant.SLUG_TYPE_ARTICLE), eq(ARTICLE_TITLE), eq(ARTICLE_SLUG), eq(ARTICLE_ID));
            assertEquals(ARTICLE_SLUG, result.getSlug());
            assertFalse(result.getSlugAppended(), "slug 未变更时不应提示被追加");
        }

        @Test
        @DisplayName("slug 被追加随机数时回传 slugAppended=true，运营需知情")
        void saveArticle_requestedSlugGotSuffix_marksSlugAppended() {
            stubCategoryExists();
            when(seoSlugService.resolveSlug(any(), any(), any(), isNull())).thenReturn("zhibofupan-4827");
            doReturn(true).when(seoArticleService).save(any(SeoArticleEntity.class));

            SeoSaveResultVo result = seoArticleService.saveArticle(buildBo(null, SeoConstant.STATUS_ENABLED));

            assertEquals("zhibofupan-4827", result.getSlug());
            assertTrue(result.getSlugAppended(), "实际入库 slug 与运营填写的不同，必须提示");
            assertEquals("zhibofupan-4827", captureSavedArticle().getSlug(), "回传值须与入库值一致");
        }

        @Test
        @DisplayName("运营手填的别名撞车时回传 slugAppended=true")
        void saveArticle_manualSlugWithCollision_marksSlugAppended() {
            // 【本用例已随规则改写】原先测的是「留空 → 生成拼音 → 撞车 → 提示」，
            // 但别名留空后现在直接用文章雪花 ID（后台反馈第 1 条），
            // 不再走拼音、也不可能撞车，那个场景已经不存在。
            //
            // 仍然需要守住的是另一半：运营【手填】了别名而它撞了车时，
            // 后端会追加随机数，必须如实告诉运营——否则他以为地址是自己填的那个，
            // 对外发出去的链接会全部 404。
            stubCategoryExists();
            when(seoSlugService.resolveSlug(any(), any(), any(), isNull())).thenReturn("zhibofupan-4827");
            doReturn(true).when(seoArticleService).save(any(SeoArticleEntity.class));
            SeoArticleBo bo = buildBo(null, SeoConstant.STATUS_ENABLED);
            bo.setSlug("zhibofupan");

            SeoSaveResultVo result = seoArticleService.saveArticle(bo);

            assertTrue(result.getSlugAppended(), "手填别名被追加随机数时必须提示");
            assertEquals("zhibofupan-4827", result.getSlug());
        }

        @Test
        @DisplayName("最终 slug 与拼音一致时不提示被追加")
        void saveArticle_blankSlugWithoutCollision_doesNotMarkSlugAppended() {
            stubCategoryExists();
            when(seoSlugService.resolveSlug(any(), any(), any(), isNull())).thenReturn(ARTICLE_SLUG);
            doReturn(true).when(seoArticleService).save(any(SeoArticleEntity.class));
            SeoArticleBo bo = buildBo(null, SeoConstant.STATUS_ENABLED);
            bo.setSlug("");

            SeoSaveResultVo result = seoArticleService.saveArticle(bo);

            // 别名留空时用文章雪花 ID（后台反馈第 1 条），它天然唯一，
            // 不存在「被追加随机数」这回事
            assertFalse(result.getSlugAppended(), "用文章 ID 作别名时不应提示被追加");
            assertEquals(String.valueOf(result.getId()), result.getSlug(),
                    "别名留空时应回落到文章自身的雪花 ID");
        }
    }

    @Nested
    @DisplayName("新增时的审计字段与默认值")
    class SaveDefaults {

        @Test
        @DisplayName("新增补齐雪花 ID、软删除标记、时间戳与浏览量默认值")
        void saveArticle_newArticle_fillsIdAuditFieldsAndDefaults() {
            stubSaveHappyPath();

            SeoSaveResultVo result = seoArticleService.saveArticle(buildBo(null, SeoConstant.STATUS_ENABLED));

            SeoArticleEntity saved = captureSavedArticle();
            assertNotNull(saved.getId(), "ID 须由 SnowflakeManager 生成");
            assertEquals(saved.getId(), result.getId());
            assertEquals(SeoConstant.NOT_DELETED, saved.getIsDeleted());
            assertEquals(0, saved.getViewCount(), "本期不做浏览量，字段须落默认 0 而非 null");
            assertNotNull(saved.getCreateDate());
            assertNotNull(saved.getUpdateDate());
        }

        @Test
        @DisplayName("SEO 标题留空时取文章标题，与前端「只填空不覆盖」的自动填充一致")
        void saveArticle_blankSeoTitle_fallsBackToArticleTitle() {
            stubSaveHappyPath();
            SeoArticleBo bo = buildBo(null, SeoConstant.STATUS_ENABLED);
            bo.setSeoTitle("  ");

            seoArticleService.saveArticle(bo);

            assertEquals(ARTICLE_TITLE, captureSavedArticle().getSeoTitle());
        }

        @Test
        @DisplayName("摘要与关键词留空时落空串而非 null")
        void saveArticle_blankOptionalFields_persistEmptyStringInsteadOfNull() {
            stubSaveHappyPath();
            SeoArticleBo bo = buildBo(null, SeoConstant.STATUS_ENABLED);
            bo.setSummary(null);
            bo.setSeoKeywords(null);

            seoArticleService.saveArticle(bo);

            SeoArticleEntity saved = captureSavedArticle();
            assertEquals("", saved.getSummary());
            assertEquals("", saved.getSeoKeywords());
        }

        @Test
        @DisplayName("编辑刷新 updateDate 但不写 createDate / isDeleted / viewCount")
        void updateArticle_existingArticle_refreshesUpdateDateOnly() {
            AtomicReference<LambdaUpdateWrapper<SeoArticleEntity>> ref =
                    stubUpdateHappyPath(existingArticle(FIRST_PUBLISH_TIME));

            seoArticleService.updateArticle(buildBo(ARTICLE_ID, SeoConstant.STATUS_ENABLED));

            // 显式 set 的好处：这几个字段压根不进 SET，不再依赖 MP 的 update-strategy 把 null 排除
            String sqlSet = ref.get().getSqlSet();
            assertTrue(sqlSet.contains("update_date"), "实际 SET：" + sqlSet);
            assertFalse(sqlSet.contains("create_date"), "createDate 不应在编辑时被写入。实际 SET：" + sqlSet);
            assertFalse(sqlSet.contains("is_deleted"), "编辑不该碰软删除标记。实际 SET：" + sqlSet);
            assertFalse(sqlSet.contains("view_count"), "编辑不该重置浏览量。实际 SET：" + sqlSet);
        }

        @Test
        @DisplayName("编辑覆盖式重建标签关联")
        void updateArticle_existingArticle_replacesTagRelations() {
            stubUpdateHappyPath(existingArticle(FIRST_PUBLISH_TIME));
            SeoArticleBo bo = buildBo(ARTICLE_ID, SeoConstant.STATUS_ENABLED);
            bo.setTagIds(List.of(ENABLED_TAG_ID));

            seoArticleService.updateArticle(bo);

            verify(seoArticleTagService).replaceArticleTags(ARTICLE_ID, List.of(ENABLED_TAG_ID));
        }
    }

    /**
     * 上面若干条断言是「渲染出的 SQL 里有没有某个片段」，这类断言最容易在不知不觉间退化成恒真。
     *
     * <p>本组不测业务，只锁住它们所依赖的 MP 渲染行为：
     * 一旦 MP 升级后 {@code getSqlSelect()} 的空值语义变了、或 GROUP BY 不再进
     * {@code getSqlSegment()}，这里会先失败并指明「上面那些断言已经不再有判别力」，
     * 而不是让它们静悄悄地永远通过。参照 SeoLogicDeleteBehaviorTest 的做法。
     */
    @Nested
    @DisplayName("SQL 断言的判别力自检")
    class WrapperAssertionGuards {

        @Test
        @DisplayName("未调用 select 时 sqlSelect 为空——「列表不含 content」的断言据此才有判别力")
        void lambdaQueryWrapper_withoutSelect_hasBlankSqlSelect() {
            LambdaQueryWrapper<SeoArticleEntity> wrapper = new LambdaQueryWrapper<SeoArticleEntity>()
                    .eq(SeoArticleEntity::getIsDeleted, SeoConstant.NOT_DELETED);

            String select = wrapper.getSqlSelect();

            assertTrue(select == null || select.isBlank(),
                    "若未限定列时 sqlSelect 也非空，queryPage 的「不含 content」断言就失去意义。实际：" + select);
        }

        @Test
        @DisplayName("调用 select 后 sqlSelect 含被选列，且正文列会真的出现——断言并非恒假")
        void lambdaQueryWrapper_selectingContent_exposesContentColumn() {
            LambdaQueryWrapper<SeoArticleEntity> wrapper = new LambdaQueryWrapper<SeoArticleEntity>()
                    .select(SeoArticleEntity::getTitle, SeoArticleEntity::getContent);

            String select = wrapper.getSqlSelect().toLowerCase(Locale.ROOT);

            assertTrue(select.contains("content"),
                    "选了正文却检测不出来，说明「不含 content」的断言恒真。实际 SELECT：" + select);
            assertTrue(select.contains("title"), "实际 SELECT：" + select);
        }

        @Test
        @DisplayName("QueryWrapper 加了 status 条件就会渲染出 status——「口径不含 status」的断言据此才有判别力")
        void queryWrapper_withStatusCondition_rendersStatusIntoSql() {
            QueryWrapper<SeoArticleEntity> wrapper = new QueryWrapper<SeoArticleEntity>()
                    .eq("is_deleted", SeoConstant.NOT_DELETED)
                    .eq("status", SeoConstant.STATUS_ENABLED);

            String sql = wrapper.getTargetSql().toLowerCase(Locale.ROOT);

            assertTrue(sql.contains("status"),
                    "按 status 过滤却检测不出来，说明统计口径断言恒真。实际 SQL：" + sql);
        }

        @Test
        @DisplayName("未调用 groupBy 时 SQL 不含 group by——「必须聚合」的断言据此才有判别力")
        void queryWrapper_withoutGroupBy_rendersNoGroupByClause() {
            QueryWrapper<SeoArticleEntity> aggregated = new QueryWrapper<SeoArticleEntity>()
                    .eq("is_deleted", SeoConstant.NOT_DELETED)
                    .groupBy("category_id");
            QueryWrapper<SeoArticleEntity> plain = new QueryWrapper<SeoArticleEntity>()
                    .eq("is_deleted", SeoConstant.NOT_DELETED);

            assertTrue(aggregated.getTargetSql().toLowerCase(Locale.ROOT).contains("group by"),
                    "GROUP BY 未进 sqlSegment，聚合断言已失效。实际 SQL：" + aggregated.getTargetSql());
            assertFalse(plain.getTargetSql().toLowerCase(Locale.ROOT).contains("group by"),
                    "不聚合时也检测出 group by，说明聚合断言恒真。实际 SQL：" + plain.getTargetSql());
        }

        @Test
        @DisplayName("LambdaUpdateWrapper 未 set 某列时 SET 片段不含该列——publishTime 的三条断言据此才有判别力")
        void lambdaUpdateWrapper_setAbsentColumn_omitsItFromSetClause() {
            LambdaUpdateWrapper<SeoArticleEntity> withPublishTime = new LambdaUpdateWrapper<SeoArticleEntity>()
                    .set(SeoArticleEntity::getArticleStatus, SeoConstant.STATUS_ENABLED)
                    .set(SeoArticleEntity::getPublishTime, LocalDateTime.now());
            LambdaUpdateWrapper<SeoArticleEntity> withoutPublishTime = new LambdaUpdateWrapper<SeoArticleEntity>()
                    .set(SeoArticleEntity::getArticleStatus, SeoConstant.STATUS_ENABLED);

            assertTrue(withPublishTime.getSqlSet().contains("publish_time="),
                    "set 了却检测不出来，说明「首次发布写入」的断言恒假。实际 SET：" + withPublishTime.getSqlSet());
            assertFalse(withoutPublishTime.getSqlSet().contains("publish_time="),
                    "没 set 却检测出来，说明「重发不覆盖」的断言恒真。实际 SET：" + withoutPublishTime.getSqlSet());
        }
    }

    private SeoArticleBo buildBo(Long id, Integer status) {
        SeoArticleBo bo = new SeoArticleBo();
        bo.setId(id);
        bo.setTitle(ARTICLE_TITLE);
        bo.setSlug(ARTICLE_SLUG);
        bo.setCategoryId(CATEGORY_ID);
        bo.setContent("<p>正文</p>");
        bo.setSummary("摘要");
        bo.setCoverUrl("https://cos.example.com/cover.jpg");
        bo.setArticleStatus(status);
        bo.setSeoTitle("直播复盘完全指南");
        bo.setSeoDescription("直播复盘的完整方法论与实操清单");
        bo.setSeoKeywords("直播复盘,话术");
        return bo;
    }

    private SeoArticleListBo buildListBo(String title, Long categoryId, Integer status,
                                         Integer page, Integer limit) {
        SeoArticleListBo bo = new SeoArticleListBo();
        bo.setTitle(title);
        bo.setCategoryId(categoryId);
        bo.setArticleStatus(status);
        bo.setPage(page);
        bo.setLimit(limit);
        return bo;
    }

    /** 数据库里已存在的文章，publishTime 由调用方指定以区分「发过」与「没发过」 */
    private SeoArticleEntity existingArticle(LocalDateTime publishTime) {
        SeoArticleEntity entity = new SeoArticleEntity();
        entity.setId(ARTICLE_ID);
        entity.setTitle(ARTICLE_TITLE);
        entity.setSlug(ARTICLE_SLUG);
        entity.setCategoryId(CATEGORY_ID);
        entity.setContent("<p>旧正文</p>");
        entity.setSummary("旧摘要");
        entity.setCoverUrl("https://cos.example.com/old.jpg");
        entity.setArticleStatus(publishTime == null ? SeoConstant.STATUS_DISABLED : SeoConstant.STATUS_ENABLED);
        entity.setViewCount(0);
        entity.setPublishTime(publishTime);
        entity.setIsDeleted(SeoConstant.NOT_DELETED);
        entity.setCreateDate(LocalDateTime.of(2025, 12, 1, 10, 0));
        entity.setUpdateDate(LocalDateTime.of(2025, 12, 1, 10, 0));
        return entity;
    }

    /** 列表查询返回的精简行：只含被 select 的那几列 */
    private SeoArticleEntity listRecord() {
        SeoArticleEntity entity = new SeoArticleEntity();
        entity.setId(ARTICLE_ID);
        entity.setTitle(ARTICLE_TITLE);
        entity.setSlug(ARTICLE_SLUG);
        entity.setCategoryId(CATEGORY_ID);
        entity.setCoverUrl("https://cos.example.com/cover.jpg");
        entity.setArticleStatus(SeoConstant.STATUS_ENABLED);
        entity.setPublishTime(FIRST_PUBLISH_TIME);
        entity.setUpdateDate(LocalDateTime.of(2026, 2, 1, 9, 0));
        return entity;
    }

    /** 删除路径上只 select 了 id / slug 两列 */
    private SeoArticleEntity articleRow(Long id, String slug) {
        SeoArticleEntity entity = new SeoArticleEntity();
        entity.setId(id);
        entity.setSlug(slug);
        return entity;
    }

    private SeoCategoryEntity category() {
        SeoCategoryEntity entity = new SeoCategoryEntity();
        entity.setId(CATEGORY_ID);
        entity.setCategoryName(CATEGORY_NAME);
        entity.setSlug("yunyingfangfalun");
        entity.setIsDeleted(SeoConstant.NOT_DELETED);
        return entity;
    }

    private List<Long> tagIds(int count) {
        List<Long> ids = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ids.add(3000L + i);
        }
        return ids;
    }

    private Map<String, Object> row(Object categoryId, Object count) {
        Map<String, Object> row = new HashMap<>();
        row.put("categoryId", categoryId);
        row.put("articleCount", count);
        return row;
    }

    /** 分类存在性校验放行 */
    private void stubCategoryExists() {
        when(seoCategoryDao.selectOne(ArgumentMatchers.<Wrapper<SeoCategoryEntity>>any()))
                .thenReturn(category());
    }

    /** 新增路径的完整放行：分类存在 + slug 原样返回 + save 成功 */
    private void stubSaveHappyPath() {
        stubCategoryExists();
        when(seoSlugService.resolveSlug(any(), any(), any(), any())).thenReturn(ARTICLE_SLUG);
        doReturn(true).when(seoArticleService).save(any(SeoArticleEntity.class));
    }

    /**
     * 编辑路径的完整放行：指定原记录 + 分类存在 + slug 原样返回 + update(wrapper) 成功。
     *
     * <p>updateArticle 显式列出要更新的字段（不用 updateById），因此「不覆盖 publishTime」
     * 的可观测形态是 SET 片段里根本没有 publish_time，而不是实体字段为 null 后靠
     * MP 的 update-strategy 把它排除——后者依赖全局配置，是团队踩过的同一类陷阱。
     */
    private AtomicReference<LambdaUpdateWrapper<SeoArticleEntity>> stubUpdateHappyPath(SeoArticleEntity existing) {
        doReturn(existing).when(seoArticleService).getById(ARTICLE_ID);
        stubCategoryExists();
        when(seoSlugService.resolveSlug(any(), any(), any(), any())).thenReturn(ARTICLE_SLUG);
        return stubUpdateByWrapper();
    }

    /** 分类名回填放行 */
    private void stubCategoryNames() {
        when(seoCategoryDao.selectList(ArgumentMatchers.<Wrapper<SeoCategoryEntity>>any()))
                .thenReturn(List.of(category()));
    }

    /** 文章挂了一个启用标签与一个已禁用标签 */
    private void stubArticleTags() {
        Map<Long, List<Long>> relations = new HashMap<>();
        relations.put(ARTICLE_ID, List.of(ENABLED_TAG_ID, DISABLED_TAG_ID));
        when(seoArticleTagService.mapTagIdsByArticleIds(anyList())).thenReturn(relations);
        when(seoTagService.listByIds(anyList())).thenReturn(List.of(
                tagOption(ENABLED_TAG_ID, "话术", SeoConstant.STATUS_ENABLED),
                tagOption(DISABLED_TAG_ID, "憋单", SeoConstant.STATUS_DISABLED)));
    }

    private SeoTagOptionVo tagOption(Long id, String name, Integer status) {
        SeoTagOptionVo option = new SeoTagOptionVo();
        option.setId(id);
        option.setTagName(name);
        option.setSlug(name + "-slug");
        option.setTagStatus(status);
        return option;
    }

    /** 打桩继承自 ServiceImpl 的 page 方法，同时抓出 LambdaQueryWrapper 以断言 SELECT / WHERE */
    /**
     * 打桩 Dao 的列表查询，抓出传给它的入参。
     *
     * <p><b>抓的是入参不是 SQL</b>：查询本身已挪到 mapper/SeoArticleDao.xml，
     * Java 侧只能验证「条件是否被正确传下去」。SQL 文本的正确性由
     * {@code SeoArticleMapperXmlTest} 静态校验，真实执行结果需要集成测试（本项目暂无）。
     */
    private AtomicReference<SeoArticleListBo> stubArticlePage(
            List<SeoArticleEntity> records, long total) {
        AtomicReference<SeoArticleListBo> ref = new AtomicReference<>();
        Page<SeoArticleEntity> page = new Page<>(1, 10, total);
        page.setRecords(records);
        doAnswer(invocation -> {
            ref.set(invocation.getArgument(1));
            return page;
        }).when(seoArticleDao).selectArticlePage(any(), any(), any());
        return ref;
    }

    /** 打桩 list(Wrapper)，供删除路径使用 */
    private void stubArticleList(List<SeoArticleEntity> records) {
        doReturn(records).when(seoArticleService).list(ArgumentMatchers.<Wrapper<SeoArticleEntity>>any());
    }

    /** 打桩 listMaps(Wrapper) 并抓出聚合用的 QueryWrapper */
    /**
     * 打桩分类统计的 Dao 调用，记录每一批传进去的分类 ID。
     *
     * <p>聚合 SQL 已挪到 mapper/SeoArticleDao.xml，Java 侧只验「分批是否正确、结果是否正确组装」；
     * SQL 本身（GROUP BY、不按 status 过滤、带 is_deleted）由 SeoArticleMapperXmlTest 静态校验。
     */
    private List<List<Long>> stubCountByCategoryIds(List<SeoIdCountVo> rows) {
        List<List<Long>> batches = new ArrayList<>();
        doAnswer(invocation -> {
            batches.add(new ArrayList<>(invocation.getArgument(0)));
            return rows;
        }).when(seoArticleDao).countByCategoryIds(anyList(), any());
        return batches;
    }

    /** 构造一行聚合结果 */
    private static SeoIdCountVo countRow(Long id, int total) {
        SeoIdCountVo vo = new SeoIdCountVo();
        vo.setId(id);
        vo.setTotal(total);
        return vo;
    }

    /** 打桩 update(Wrapper) 并留下最后一次的 LambdaUpdateWrapper */
    private AtomicReference<LambdaUpdateWrapper<SeoArticleEntity>> stubUpdateByWrapper() {
        AtomicReference<LambdaUpdateWrapper<SeoArticleEntity>> ref = new AtomicReference<>();
        doAnswer(invocation -> {
            ref.set(invocation.getArgument(0));
            return true;
        }).when(seoArticleService).update(ArgumentMatchers.<Wrapper<SeoArticleEntity>>any());
        return ref;
    }

    /** 打桩 update(Wrapper) 并按调用顺序收集 LambdaUpdateWrapper */
    private List<LambdaUpdateWrapper<SeoArticleEntity>> stubUpdateByWrapperList() {
        List<LambdaUpdateWrapper<SeoArticleEntity>> wrappers = new ArrayList<>();
        doAnswer(invocation -> {
            wrappers.add(invocation.getArgument(0));
            return true;
        }).when(seoArticleService).update(ArgumentMatchers.<Wrapper<SeoArticleEntity>>any());
        return wrappers;
    }

    /**
     * 桩住 baseMapper.update 并捕获 wrapper。
     *
     * <p>批量删除走的是 {@code getBaseMapper().update(null, wrapper)} 而不是
     * {@code ServiceImpl.update(wrapper)}——后者只返回 boolean，拿不到影响行数，
     * 而「一条都没删到」需要影响行数才能判断。
     *
     * @param affectedRows 每次调用返回的影响行数
     */
    private List<LambdaUpdateWrapper<SeoArticleEntity>> stubBaseMapperUpdate(int affectedRows) {
        List<LambdaUpdateWrapper<SeoArticleEntity>> wrappers = new ArrayList<>();
        SeoArticleDao dao = Mockito.mock(SeoArticleDao.class);
        doReturn(dao).when(seoArticleService).getBaseMapper();
        doAnswer(invocation -> {
            wrappers.add(invocation.getArgument(1));
            return affectedRows;
        }).when(dao).update(isNull(), ArgumentMatchers.<Wrapper<SeoArticleEntity>>any());
        return wrappers;
    }

    /**
     * 取出 wrapper 登记的全部参数值。
     *
     * <p>WHERE 片段的参数是渲染 SQL 时才登记的，故先取一次 SQL 再读参数。
     */
    private Collection<Object> paramsOf(LambdaUpdateWrapper<SeoArticleEntity> wrapper) {
        wrapper.getSqlSet();
        wrapper.getTargetSql();
        return new ArrayList<>(wrapper.getParamNameValuePairs().values());
    }

    private SeoArticleEntity captureSavedArticle() {
        ArgumentCaptor<SeoArticleEntity> captor = ArgumentCaptor.forClass(SeoArticleEntity.class);
        verify(seoArticleService).save(captor.capture());
        return captor.getValue();
    }

    private SeoArticleEntity captureUpdatedArticle() {
        ArgumentCaptor<SeoArticleEntity> captor = ArgumentCaptor.forClass(SeoArticleEntity.class);
        verify(seoArticleService).updateById(captor.capture());
        return captor.getValue();
    }
    @Nested
    @DisplayName("内容变更后必须让官网缓存失效")
    class CacheEviction {

        /**
         * 这组断言存在的原因：
         *
         * <p>官网详情页【不做页面级缓存】——Nitro 只缓存 2xx 响应，一个 URL 从 200 变成
         * 404 后，revalidate 产生的 404 不会写回缓存，旧值会【永久】返回 200 + 旧内容
         * （swr / cache / staleMaxAge:0 四种写法实测均如此，见官网技术设计 §4.7）。
         *
         * <p>因此压力改由后端接口缓存承担，而这一层的失效就成了「下架后多久从官网消失」
         * 的唯一闸门。哪个写方法漏调 evictAll，对应的内容就会继续对外可见到 TTL 到期，
         * 而这件事【没有任何报错】：接口 200、页面正常，只是内容是旧的。
         */
        @Test
        @DisplayName("changeStatus 下架后清缓存——漏了会让下架文章继续对外可见")
        void changeStatusEvictsCache() {
            SeoArticleEntity existing = new SeoArticleEntity();
            existing.setId(1L);
            existing.setIsDeleted(SeoConstant.NOT_DELETED);
            existing.setPublishTime(LocalDateTime.now());
            doReturn(existing).when(seoArticleService).getById(1L);
            doReturn(true).when(seoArticleService).update(any(LambdaUpdateWrapper.class));

            seoArticleService.changeStatus(1L, SeoConstant.STATUS_DISABLED);

            verify(seoSiteService).evictAll();
        }

        @Test
        @DisplayName("删除文章后清缓存")
        void deleteEvictsCache() {
            doReturn(1).when(seoArticleDao).update(any(), any(LambdaUpdateWrapper.class));
            SeoArticleEntity existing = new SeoArticleEntity();
            existing.setId(1L);
            existing.setIsDeleted(SeoConstant.NOT_DELETED);
            doReturn(java.util.List.of(existing)).when(seoArticleService).listByIds(any());

            try {
                seoArticleService.deleteArticles(java.util.List.of(1L));
            } catch (RuntimeException ignored) {
                // 该方法内部依赖较多，此处只关心「走到末尾时是否清了缓存」；
                // 若因 mock 不全提前抛错，下面的 verify 会失败并暴露出来
            }
            verify(seoSiteService).evictAll();
        }
    }

}
