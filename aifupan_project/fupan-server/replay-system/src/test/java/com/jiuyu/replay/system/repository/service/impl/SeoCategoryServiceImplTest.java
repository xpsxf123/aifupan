package com.jiuyu.replay.system.repository.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.system.bo.SeoCategoryBo;
import com.jiuyu.replay.system.bo.SeoCategoryListBo;
import com.jiuyu.replay.system.constant.SeoConstant;
import com.jiuyu.replay.system.entity.SeoArticleEntity;
import com.jiuyu.replay.system.entity.SeoCategoryEntity;
import com.jiuyu.replay.system.repository.service.SeoArticleService;
import com.jiuyu.replay.system.repository.service.SeoSiteService;
import com.jiuyu.replay.system.repository.service.SeoSlugHistoryService;
import com.jiuyu.replay.system.repository.service.SeoSlugService;
import com.jiuyu.replay.system.vo.SeoCategoryListVo;
import com.jiuyu.replay.system.vo.SeoCategoryOptionVo;
import com.jiuyu.replay.system.vo.SeoSaveResultVo;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
 * SeoCategoryServiceImpl 单测，对应 openspec AC-3 / AC-4 / AC-6 / AC-9。
 *
 * <p><b>为什么用 spy</b>：被测类继承 MyBatis-Plus 的 {@code ServiceImpl}，
 * 直接 new 出来 baseMapper 为 null，一调 {@code getById / save / page / count} 就 NPE。
 * 这里用 {@link Mockito#spy} 部分模拟：业务方法跑真实逻辑，继承来的 CRUD 方法逐个打桩。
 * spy 会拦截对象内部的自调用，因此 deleteCategory 内部的 getById / update 也走桩。
 *
 * <p><b>删除断言为什么盯着 {@code update(Wrapper)} 而不是 {@code updateById}</b>：
 * MP 全局配了 {@code logic-delete-field: isDeleted}，updateById 会把逻辑删除字段从 SET 里剔除，
 * 手动 setIsDeleted 被静默丢弃——名字改成了墓碑值但记录仍可见。因此断言必须落在
 * LambdaUpdateWrapper 的 SET 片段上，确认 is_deleted 真的进了 SET。
 *
 * <p>被打桩的继承方法即「与数据库的边界」——真正的 SQL 执行、唯一索引冲突、事务回滚
 * 不在单测覆盖范围内（见报告中的集成测试清单）。
 *
 * @author claude
 * @date 2026-08-13
 */
@DisplayName("SEO 分类管理")
class SeoCategoryServiceImplTest {

    private static final long CATEGORY_ID = 1955000000000000001L;

    private static final String CATEGORY_NAME = "直播复盘";

    private static final String CATEGORY_SLUG = "zhibofupan";

    private SeoSlugService seoSlugService;

    private SeoArticleService seoArticleService;

    private SeoCategoryServiceImpl seoCategoryService;

    /** 注册实体到 MP 的 lambda 列名缓存，否则 LambdaQueryWrapper 构造即抛异常 */
    @BeforeAll
    static void initLambdaColumnCache() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        assistant.setCurrentNamespace("seoCategoryServiceImplTest");
        TableInfoHelper.initTableInfo(assistant, SeoCategoryEntity.class);
        TableInfoHelper.initTableInfo(assistant, SeoArticleEntity.class);
    }

    /** 内容变更后必须调它让官网缓存失效，测试里断言这一点 */
    private SeoSiteService seoSiteService;

    @BeforeEach
    void setUp() {
        seoSiteService = Mockito.mock(SeoSiteService.class);
        seoSlugService = Mockito.mock(SeoSlugService.class);
        seoArticleService = Mockito.mock(SeoArticleService.class);
        seoCategoryService = Mockito.spy(new SeoCategoryServiceImpl(
                seoSlugService, seoArticleService, Mockito.mock(SeoSlugHistoryService.class),
                seoSiteService));
    }

    @Nested
    @DisplayName("分类删除（AC-4）")
    class CategoryDeletion {

        @Test
        @DisplayName("分类下有文章时抛业务异常，异常消息含文章数量")
        void deleteCategory_categoryHasArticles_throwsBusinessExceptionWithArticleCount() {
            doReturn(existingCategory()).when(seoCategoryService).getOne(any());
            when(seoArticleService.countByCategoryIds(any())).thenReturn(Map.of(CATEGORY_ID, 3));

            BusinessException thrown = assertThrows(BusinessException.class,
                    () -> seoCategoryService.deleteCategory(CATEGORY_ID));

            assertTrue(thrown.getMessage().contains("3"),
                    "拒绝原因须含文章数量，否则运营不知道要挪几篇。实际：" + thrown.getMessage());
            assertTrue(thrown.getMessage().contains("篇文章"),
                    "拒绝原因须说明是文章占用，实际：" + thrown.getMessage());
        }

        @Test
        @DisplayName("分类下有文章时不执行删除")
        void deleteCategory_categoryHasArticles_doesNotUpdateRecord() {
            doReturn(existingCategory()).when(seoCategoryService).getOne(any());
            when(seoArticleService.countByCategoryIds(any())).thenReturn(Map.of(CATEGORY_ID, 1));

            assertThrows(BusinessException.class, () -> seoCategoryService.deleteCategory(CATEGORY_ID));

            verify(seoCategoryService, never()).update(ArgumentMatchers.<Wrapper<SeoCategoryEntity>>any());
            verify(seoCategoryService, never()).updateById(any());
        }

        @Test
        @DisplayName("分类下无文章时删除成功，并置 isDeleted 与 updateDate")
        void deleteCategory_categoryWithoutArticles_marksDeletedAndRefreshesUpdateDate() {
            doReturn(existingCategory()).when(seoCategoryService).getOne(any());
            when(seoArticleService.countByCategoryIds(any())).thenReturn(Map.of());
            AtomicReference<LambdaUpdateWrapper<SeoCategoryEntity>> wrapperRef = stubUpdateByWrapper();

            seoCategoryService.deleteCategory(CATEGORY_ID);

            LambdaUpdateWrapper<SeoCategoryEntity> wrapper = wrapperRef.get();
            assertNotNull(wrapper, "未发出删除更新");
            assertTrue(wrapper.getSqlSet().contains("is_deleted="),
                    "is_deleted 必须出现在 SET 片段里，否则会被 MP 逻辑删除过滤静默丢弃。实际 SET："
                            + wrapper.getSqlSet());
            assertTrue(wrapper.getSqlSet().contains("update_date="),
                    "软删除须刷新 updateDate，实际 SET：" + wrapper.getSqlSet());
            assertTrue(paramsOf(wrapper).contains(SeoConstant.DELETED),
                    "isDeleted 未写成已删除，实际参数：" + paramsOf(wrapper));
        }

        @Test
        @DisplayName("删除只作用于目标 ID，不会误伤同表其他分类")
        void deleteCategory_categoryWithoutArticles_scopesUpdateToTargetId() {
            doReturn(existingCategory()).when(seoCategoryService).getOne(any());
            when(seoArticleService.countByCategoryIds(any())).thenReturn(Map.of());
            AtomicReference<LambdaUpdateWrapper<SeoCategoryEntity>> wrapperRef = stubUpdateByWrapper();

            seoCategoryService.deleteCategory(CATEGORY_ID);

            LambdaUpdateWrapper<SeoCategoryEntity> wrapper = wrapperRef.get();
            assertTrue(wrapper.getTargetSql().contains("id ="),
                    "缺少 id 条件会全表更新，实际 WHERE：" + wrapper.getTargetSql());
            assertTrue(paramsOf(wrapper).contains(CATEGORY_ID),
                    "实际参数：" + paramsOf(wrapper));
        }

        @Test
        @DisplayName("墓碑值形态为 {原值}__del_{id}，使同名同 slug 可立即重建（AC-3）")
        void deleteCategory_categoryWithoutArticles_writesTombstoneForNameAndSlug() {
            doReturn(existingCategory()).when(seoCategoryService).getOne(any());
            when(seoArticleService.countByCategoryIds(any())).thenReturn(Map.of());
            AtomicReference<LambdaUpdateWrapper<SeoCategoryEntity>> wrapperRef = stubUpdateByWrapper();

            seoCategoryService.deleteCategory(CATEGORY_ID);

            LambdaUpdateWrapper<SeoCategoryEntity> wrapper = wrapperRef.get();
            Collection<Object> params = paramsOf(wrapper);
            assertTrue(params.contains(CATEGORY_NAME + "__del_" + CATEGORY_ID),
                    "name 未改写为墓碑值，删除后无法重建同名分类。实际参数：" + params);
            assertTrue(params.contains(CATEGORY_SLUG + "__del_" + CATEGORY_ID),
                    "slug 未改写为墓碑值，删除后无法重建同 slug 分类。实际参数：" + params);
            // 原值已被释放：墓碑值与原值不同，唯一索引不再被这条已删除记录占用
            assertFalse(params.contains(CATEGORY_SLUG), "slug 仍写回原值，唯一索引未被释放");
            assertFalse(params.contains(CATEGORY_NAME), "name 仍写回原值，唯一索引未被释放");
        }

        @Test
        @DisplayName("删除后重建同名同 slug 分类时 slug 不被追加后缀（AC-3）")
        void saveCategory_afterTombstonedDeletion_reusesOriginalSlugWithoutSuffix() {
            // slug 查重只看未删除记录，墓碑化后原 slug 重新可用，resolveSlug 原样返回
            stubNameAvailable();
            when(seoSlugService.resolveSlug(eq(SeoConstant.SLUG_TYPE_CATEGORY), eq(CATEGORY_NAME),
                    eq(CATEGORY_SLUG), isNull())).thenReturn(CATEGORY_SLUG);
            doReturn(true).when(seoCategoryService).save(any(SeoCategoryEntity.class));

            SeoSaveResultVo result = seoCategoryService.saveCategory(buildBo(null, CATEGORY_NAME, CATEGORY_SLUG));

            assertEquals(CATEGORY_SLUG, result.getSlug());
            assertFalse(result.getSlugAppended(), "重建时不应被追加随机后缀");
            assertEquals(CATEGORY_SLUG, captureSavedCategory().getSlug());
        }

        @Test
        @DisplayName("分类不存在时抛业务异常，不误发一条 update")
        void deleteCategory_categoryNotFound_throwsWithoutUpdate() {
            doReturn(null).when(seoCategoryService).getOne(any());

            assertThrows(BusinessException.class, () -> seoCategoryService.deleteCategory(CATEGORY_ID));

            verify(seoCategoryService, never()).update(ArgumentMatchers.<Wrapper<SeoCategoryEntity>>any());
            verifyNoInteractions(seoArticleService);
        }

        @Test
        @DisplayName("重复删除已删除的分类时抛业务异常，避免墓碑值二次叠加")
        void deleteCategory_alreadyDeleted_throwsWithoutUpdate() {
            SeoCategoryEntity deleted = existingCategory();
            deleted.setIsDeleted(SeoConstant.DELETED);
            doReturn(deleted).when(seoCategoryService).getOne(any());

            assertThrows(BusinessException.class, () -> seoCategoryService.deleteCategory(CATEGORY_ID));

            verify(seoCategoryService, never()).update(ArgumentMatchers.<Wrapper<SeoCategoryEntity>>any());
        }

        @Test
        @DisplayName("删除前用 for update 锁住分类行——否则 count 与 update 之间会插进新文章")
        void deleteCategory_beforeCounting_locksCategoryRowForUpdate() {
            AtomicReference<String> sqlRef = new AtomicReference<>("");
            doAnswer(invocation -> {
                LambdaQueryWrapper<SeoCategoryEntity> wrapper = invocation.getArgument(0);
                sqlRef.set(wrapper.getSqlSegment());
                return existingCategory();
            }).when(seoCategoryService).getOne(any());
            when(seoArticleService.countByCategoryIds(any())).thenReturn(Map.of());
            stubUpdateByWrapper();

            seoCategoryService.deleteCategory(CATEGORY_ID);

            assertTrue(sqlRef.get().toLowerCase().contains("for update"),
                    "未加行锁：另一个事务可在 count 之后插入文章，产生指向已删分类的孤儿文章。实际：" + sqlRef.get());
        }

        @Test
        @DisplayName("文章数统计委托给文章服务的聚合查询，不自行拉明细计数")
        void deleteCategory_articleCount_delegatesToArticleServiceAggregate() {
            // 统计口径本身（含已下架、排除已删除、单条 group by 而非拉明细）
            // 由 SeoArticleServiceImplTest 验证——聚合逻辑在 SeoArticleServiceImpl.countByCategoryIds。
            // 这里只锁住「分类服务不自己数」这层委托关系
            doReturn(existingCategory()).when(seoCategoryService).getOne(any());
            when(seoArticleService.countByCategoryIds(any())).thenReturn(Map.of(CATEGORY_ID, 2));

            assertThrows(BusinessException.class, () -> seoCategoryService.deleteCategory(CATEGORY_ID));

            verify(seoArticleService, times(1)).countByCategoryIds(any());
        }
    }

    @Nested
    @DisplayName("分类列表")
    class CategoryListing {

        @Test
        @DisplayName("文章数由单条聚合查询填充，不逐条 count（AC-9）")
        void queryPage_multipleCategories_fillsArticleCountWithSingleQuery() {
            SeoCategoryEntity first = existingCategory();
            SeoCategoryEntity second = category(2002L, "话术技巧", "huashujiqiao");
            stubDatabasePage(List.of(first, second), 2L);
            when(seoArticleService.countByCategoryIds(any())).thenReturn(Map.of(CATEGORY_ID, 2, 2002L, 1));

            PageUtils<SeoCategoryListVo> result = seoCategoryService.queryPage(buildListBo(null, 1, 10));

            assertEquals(2, result.getList().size());
            assertEquals(2, result.getList().get(0).getArticleCount());
            assertEquals(1, result.getList().get(1).getArticleCount());
            verify(seoArticleService, times(1)).countByCategoryIds(any());
        }

        @Test
        @DisplayName("无关联文章的分类文章数为 0 而非 null")
        void queryPage_categoryWithoutArticles_articleCountIsZero() {
            stubDatabasePage(List.of(existingCategory()), 1L);
            when(seoArticleService.countByCategoryIds(any())).thenReturn(Map.of());

            PageUtils<SeoCategoryListVo> result = seoCategoryService.queryPage(buildListBo(null, 1, 10));

            assertEquals(0, result.getList().get(0).getArticleCount());
        }

        @Test
        @DisplayName("空页不再发起文章聚合查询")
        void queryPage_emptyPage_returnsEmptyListWithoutArticleQuery() {
            stubDatabasePage(new ArrayList<>(), 0L);

            PageUtils<SeoCategoryListVo> result = seoCategoryService.queryPage(buildListBo(null, 1, 10));

            assertTrue(result.getList().isEmpty());
            verifyNoInteractions(seoArticleService);
        }

        @Test
        @DisplayName("分页元数据取自数据库分页结果")
        void queryPage_databasePage_copiesPageMetadata() {
            stubDatabasePage(List.of(existingCategory()), 25L);
            when(seoArticleService.countByCategoryIds(any())).thenReturn(Map.of());

            PageUtils<SeoCategoryListVo> result = seoCategoryService.queryPage(buildListBo(null, 2, 10));

            assertEquals(25, result.getTotalCount());
            assertEquals(10, result.getPageSize());
            assertEquals(2, result.getCurrPage());
            assertEquals(3, result.getTotalPage());
        }

        @Test
        @DisplayName("查询恒带 is_deleted 过滤，name 为空时不拼 LIKE")
        void queryPage_blankName_filtersDeletedWithoutLikeCondition() {
            AtomicReference<String> sqlRef = stubDatabasePageCapturingSql();

            seoCategoryService.queryPage(buildListBo("   ", 1, 10));

            assertTrue(sqlRef.get().contains("is_deleted ="), "实际 SQL：" + sqlRef.get());
            assertFalse(sqlRef.get().contains("LIKE"), "空关键词不应拼 LIKE，实际 SQL：" + sqlRef.get());
        }

        @Test
        @DisplayName("name 非空时按名称模糊匹配")
        void queryPage_nameGiven_appendsLikeCondition() {
            AtomicReference<String> sqlRef = stubDatabasePageCapturingSql();

            seoCategoryService.queryPage(buildListBo("复盘", 1, 10));

            assertTrue(sqlRef.get().contains("LIKE"), "实际 SQL：" + sqlRef.get());
        }

        @Test
        @DisplayName("下拉选项只返回 id / name / slug 三个字段")
        void listAll_existingCategories_mapsOptionFields() {
            // list(Wrapper) 与 list(IPage) 同名，需显式指明类型参数消除歧义
            doReturn(List.of(existingCategory())).when(seoCategoryService)
                    .list(ArgumentMatchers.<Wrapper<SeoCategoryEntity>>any());

            List<SeoCategoryOptionVo> options = seoCategoryService.listAll();

            assertEquals(1, options.size());
            assertEquals(CATEGORY_ID, options.get(0).getId());
            assertEquals(CATEGORY_NAME, options.get(0).getCategoryName());
            assertEquals(CATEGORY_SLUG, options.get(0).getSlug());
        }
    }

    @Nested
    @DisplayName("分类保存与更新（AC-6 slugAppended）")
    class CategorySaveAndUpdate {

        @Test
        @DisplayName("运营手填的 slug 被追加随机数时回传 slugAppended=true")
        void saveCategory_requestedSlugGotSuffix_marksSlugAppended() {
            stubNameAvailable();
            when(seoSlugService.resolveSlug(any(), any(), any(), isNull())).thenReturn("zhibofupan-4827");
            doReturn(true).when(seoCategoryService).save(any(SeoCategoryEntity.class));

            SeoSaveResultVo result = seoCategoryService.saveCategory(buildBo(null, CATEGORY_NAME, CATEGORY_SLUG));

            assertEquals("zhibofupan-4827", result.getSlug());
            assertTrue(result.getSlugAppended(), "运营需知情 slug 被改写");
        }

        @Test
        @DisplayName("运营留空 slug 且拼音撞车时同样回传 slugAppended=true")
        void saveCategory_blankSlugWithPinyinCollision_marksSlugAppended() {
            stubNameAvailable();
            when(seoSlugService.resolveSlug(any(), any(), any(), isNull())).thenReturn("zhibofupan-4827");
            doReturn(true).when(seoCategoryService).save(any(SeoCategoryEntity.class));

            SeoSaveResultVo result = seoCategoryService.saveCategory(buildBo(null, CATEGORY_NAME, ""));

            // 留空 slug + 拼音撞车是最常见的追加场景；若拿空串与最终值比较会永远判「未追加」
            assertTrue(result.getSlugAppended(), "留空 slug 时也必须提示被追加，否则 AC-6 失效");
        }

        @Test
        @DisplayName("最终 slug 与拼音一致时不提示被追加")
        void saveCategory_blankSlugWithoutCollision_doesNotMarkSlugAppended() {
            stubNameAvailable();
            when(seoSlugService.resolveSlug(any(), any(), any(), isNull())).thenReturn(CATEGORY_SLUG);
            doReturn(true).when(seoCategoryService).save(any(SeoCategoryEntity.class));

            SeoSaveResultVo result = seoCategoryService.saveCategory(buildBo(null, CATEGORY_NAME, ""));

            assertFalse(result.getSlugAppended(), "未发生冲突时不应提示被追加");
        }

        @Test
        @DisplayName("名称转不出拼音时兜底值不算被追加——无 base 可比")
        void saveCategory_nameWithoutPinyin_doesNotMarkSlugAppended() {
            stubNameAvailable();
            when(seoSlugService.resolveSlug(any(), any(), any(), isNull())).thenReturn("category-123456");
            doReturn(true).when(seoCategoryService).save(any(SeoCategoryEntity.class));

            SeoSaveResultVo result = seoCategoryService.saveCategory(buildBo(null, "こんにちは", ""));

            assertEquals("category-123456", result.getSlug());
            assertFalse(result.getSlugAppended(), "base 为空时不存在「被追加」这回事");
        }

        @Test
        @DisplayName("新增时补齐雪花 ID、软删除标记、时间戳与默认排序")
        void saveCategory_newCategory_fillsIdAuditFieldsAndDefaults() {
            stubNameAvailable();
            when(seoSlugService.resolveSlug(any(), any(), any(), isNull())).thenReturn(CATEGORY_SLUG);
            doReturn(true).when(seoCategoryService).save(any(SeoCategoryEntity.class));

            SeoSaveResultVo result = seoCategoryService.saveCategory(buildBo(null, CATEGORY_NAME, null));

            SeoCategoryEntity saved = captureSavedCategory();
            assertNotNull(saved.getId(), "ID 须由 SnowflakeManager 生成");
            assertEquals(saved.getId(), result.getId());
            assertEquals(SeoConstant.NOT_DELETED, saved.getIsDeleted());
            assertNotNull(saved.getCreateDate());
            assertNotNull(saved.getUpdateDate());
            assertEquals(0, saved.getSort(), "sort 为空时默认 0");
            assertEquals("", saved.getDescription(), "description 为空时默认空串而非 null");
        }

        @Test
        @DisplayName("新增时 excludeId 传 null——新记录没有自身可排除")
        void saveCategory_newCategory_passesNullExcludeId() {
            stubNameAvailable();
            when(seoSlugService.resolveSlug(any(), any(), any(), isNull())).thenReturn(CATEGORY_SLUG);
            doReturn(true).when(seoCategoryService).save(any(SeoCategoryEntity.class));

            seoCategoryService.saveCategory(buildBo(null, CATEGORY_NAME, CATEGORY_SLUG));

            verify(seoSlugService).resolveSlug(
                    eq(SeoConstant.SLUG_TYPE_CATEGORY), eq(CATEGORY_NAME), eq(CATEGORY_SLUG), isNull());
        }

        @Test
        @DisplayName("编辑时把自身 ID 作为 excludeId 传给查重，避免给自己追加后缀")
        void updateCategory_existingCategory_passesOwnIdAsExcludeId() {
            doReturn(existingCategory()).when(seoCategoryService).getById(CATEGORY_ID);
            stubNameAvailable();
            when(seoSlugService.resolveSlug(any(), any(), any(), eq(CATEGORY_ID))).thenReturn(CATEGORY_SLUG);
            doReturn(true).when(seoCategoryService).updateById(any());

            SeoSaveResultVo result = seoCategoryService.updateCategory(
                    buildBo(CATEGORY_ID, CATEGORY_NAME, CATEGORY_SLUG));

            verify(seoSlugService).resolveSlug(
                    eq(SeoConstant.SLUG_TYPE_CATEGORY), eq(CATEGORY_NAME), eq(CATEGORY_SLUG), eq(CATEGORY_ID));
            assertEquals(CATEGORY_SLUG, result.getSlug());
            assertFalse(result.getSlugAppended(), "slug 未变更时不应提示被追加");
        }

        @Test
        @DisplayName("编辑时刷新 updateDate 但不覆盖 createDate")
        void updateCategory_existingCategory_refreshesUpdateDateOnly() {
            doReturn(existingCategory()).when(seoCategoryService).getById(CATEGORY_ID);
            stubNameAvailable();
            when(seoSlugService.resolveSlug(any(), any(), any(), eq(CATEGORY_ID))).thenReturn(CATEGORY_SLUG);
            doReturn(true).when(seoCategoryService).updateById(any());

            seoCategoryService.updateCategory(buildBo(CATEGORY_ID, CATEGORY_NAME, CATEGORY_SLUG));

            SeoCategoryEntity update = captureUpdatedCategory();
            assertNotNull(update.getUpdateDate());
            assertNull(update.getCreateDate(), "createDate 不应在编辑时被写入");
        }

        @Test
        @DisplayName("编辑不存在或已删除的分类时抛业务异常，不落 update")
        void updateCategory_categoryNotFound_throwsWithoutUpdate() {
            doReturn(null).when(seoCategoryService).getById(CATEGORY_ID);

            assertThrows(BusinessException.class,
                    () -> seoCategoryService.updateCategory(buildBo(CATEGORY_ID, CATEGORY_NAME, CATEGORY_SLUG)));

            verify(seoCategoryService, never()).updateById(any());
            verifyNoInteractions(seoSlugService);
        }

        @Test
        @DisplayName("名称重复时抛业务异常并带出名称，不等数据库唯一索引报 500")
        void saveCategory_duplicateName_throwsBusinessExceptionWithName() {
            doReturn(1L).when(seoCategoryService).count(ArgumentMatchers.<Wrapper<SeoCategoryEntity>>any());

            BusinessException thrown = assertThrows(BusinessException.class,
                    () -> seoCategoryService.saveCategory(buildBo(null, CATEGORY_NAME, CATEGORY_SLUG)));

            assertTrue(thrown.getMessage().contains(CATEGORY_NAME), "实际：" + thrown.getMessage());
            verify(seoCategoryService, never()).save(any(SeoCategoryEntity.class));
            verifyNoInteractions(seoSlugService);
        }

        @Test
        @DisplayName("名称查重排除自身，编辑时不会把自己判成重名")
        void updateCategory_existingCategory_excludesSelfFromNameCheck() {
            doReturn(existingCategory()).when(seoCategoryService).getById(CATEGORY_ID);
            AtomicReference<String> sqlRef = new AtomicReference<>("");
            AtomicReference<Collection<Object>> paramsRef = new AtomicReference<>(new ArrayList<>());
            doAnswer(invocation -> {
                LambdaQueryWrapper<SeoCategoryEntity> wrapper = invocation.getArgument(0);
                sqlRef.set(wrapper.getTargetSql());
                paramsRef.set(wrapper.getParamNameValuePairs().values());
                return 0L;
            }).when(seoCategoryService).count(ArgumentMatchers.<Wrapper<SeoCategoryEntity>>any());
            when(seoSlugService.resolveSlug(any(), any(), any(), eq(CATEGORY_ID))).thenReturn(CATEGORY_SLUG);
            doReturn(true).when(seoCategoryService).updateById(any());

            seoCategoryService.updateCategory(buildBo(CATEGORY_ID, CATEGORY_NAME, CATEGORY_SLUG));

            assertTrue(sqlRef.get().contains("id <>"),
                    "名称查重未排除自身，改个描述都会报「名称已存在」。实际 SQL：" + sqlRef.get());
            assertTrue(paramsRef.get().contains(CATEGORY_ID), "实际参数：" + paramsRef.get());
        }

        @Test
        @DisplayName("名称为空时抛业务异常")
        void saveCategory_blankName_throwsBusinessException() {
            assertThrows(BusinessException.class,
                    () -> seoCategoryService.saveCategory(buildBo(null, "   ", CATEGORY_SLUG)));
            assertThrows(BusinessException.class,
                    () -> seoCategoryService.saveCategory(buildBo(null, null, CATEGORY_SLUG)));

            verify(seoCategoryService, never()).save(any(SeoCategoryEntity.class));
        }
    }

    private SeoCategoryBo buildBo(Long id, String name, String slug) {
        SeoCategoryBo bo = new SeoCategoryBo();
        bo.setId(id);
        bo.setCategoryName(name);
        bo.setSlug(slug);
        return bo;
    }

    private SeoCategoryListBo buildListBo(String name, Integer page, Integer limit) {
        SeoCategoryListBo bo = new SeoCategoryListBo();
        bo.setCategoryName(name);
        bo.setPage(page);
        bo.setLimit(limit);
        return bo;
    }

    private SeoCategoryEntity existingCategory() {
        return category(CATEGORY_ID, CATEGORY_NAME, CATEGORY_SLUG);
    }

    private SeoCategoryEntity category(Long id, String name, String slug) {
        SeoCategoryEntity entity = new SeoCategoryEntity();
        entity.setId(id);
        entity.setCategoryName(name);
        entity.setSlug(slug);
        entity.setSort(0);
        entity.setDescription("");
        entity.setIsDeleted(SeoConstant.NOT_DELETED);
        entity.setCreateDate(LocalDateTime.now());
        entity.setUpdateDate(LocalDateTime.now());
        return entity;
    }

    private SeoArticleEntity article(Long categoryId) {
        SeoArticleEntity entity = new SeoArticleEntity();
        entity.setCategoryId(categoryId);
        return entity;
    }

    private List<SeoArticleEntity> articlesOfCategory(int count) {
        List<SeoArticleEntity> articles = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            articles.add(article(CATEGORY_ID));
        }
        return articles;
    }

    /** 名称唯一校验放行 */
    private void stubNameAvailable() {
        doReturn(0L).when(seoCategoryService).count(ArgumentMatchers.<Wrapper<SeoCategoryEntity>>any());
    }

    /** 打桩 update(Wrapper) 并把 LambdaUpdateWrapper 抓出来断言 SET / WHERE */
    private AtomicReference<LambdaUpdateWrapper<SeoCategoryEntity>> stubUpdateByWrapper() {
        AtomicReference<LambdaUpdateWrapper<SeoCategoryEntity>> ref = new AtomicReference<>();
        doAnswer(invocation -> {
            ref.set(invocation.getArgument(0));
            return true;
        }).when(seoCategoryService).update(ArgumentMatchers.<Wrapper<SeoCategoryEntity>>any());
        return ref;
    }

    /**
     * 取出 wrapper 登记的全部参数值。
     *
     * <p>WHERE 片段的参数是渲染 SQL 时才登记的，故先取一次 SQL 再读参数。
     */
    private Collection<Object> paramsOf(LambdaUpdateWrapper<SeoCategoryEntity> wrapper) {
        wrapper.getSqlSet();
        wrapper.getTargetSql();
        return wrapper.getParamNameValuePairs().values();
    }

    /** 打桩继承自 ServiceImpl 的 page 方法，模拟数据库分页结果 */
    private void stubDatabasePage(List<SeoCategoryEntity> records, long total) {
        Page<SeoCategoryEntity> page = new Page<>(1, 10, total);
        page.setRecords(records);
        doReturn(page).when(seoCategoryService).page(any(), any());
    }

    /** 打桩 page 并抓取渲染后的 SQL，用于断言查询条件 */
    private AtomicReference<String> stubDatabasePageCapturingSql() {
        AtomicReference<String> sqlRef = new AtomicReference<>("");
        Page<SeoCategoryEntity> page = new Page<>(1, 10, 0L);
        page.setRecords(new ArrayList<>());
        doAnswer(invocation -> {
            LambdaQueryWrapper<SeoCategoryEntity> wrapper = invocation.getArgument(1);
            sqlRef.set(wrapper.getTargetSql());
            return page;
        }).when(seoCategoryService).page(any(), any());
        return sqlRef;
    }

    private SeoCategoryEntity captureSavedCategory() {
        ArgumentCaptor<SeoCategoryEntity> captor = ArgumentCaptor.forClass(SeoCategoryEntity.class);
        verify(seoCategoryService).save(captor.capture());
        return captor.getValue();
    }

    private SeoCategoryEntity captureUpdatedCategory() {
        ArgumentCaptor<SeoCategoryEntity> captor = ArgumentCaptor.forClass(SeoCategoryEntity.class);
        verify(seoCategoryService).updateById(captor.capture());
        return captor.getValue();
    }
}
