package com.jiuyu.replay.system.repository.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.system.bo.SeoTagListBo;
import com.jiuyu.replay.system.constant.SeoConstant;
import com.jiuyu.replay.system.entity.SeoTagEntity;
import com.jiuyu.replay.system.repository.dao.SeoTagDao;
import com.jiuyu.replay.system.repository.service.SeoArticleTagService;
import com.jiuyu.replay.system.repository.service.SeoSiteService;
import com.jiuyu.replay.system.repository.service.SeoSlugHistoryService;
import com.jiuyu.replay.system.repository.service.SeoSlugService;
import com.jiuyu.replay.system.vo.SeoTagListVo;
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
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * SeoTagServiceImpl 单测，对应 openspec AC-3 与 §3 标签列表排序契约。
 *
 * <p>同 SeoCategoryServiceImplTest：被测类继承 MP {@code ServiceImpl}，
 * 用 {@link Mockito#spy} 部分模拟，继承来的 CRUD 方法逐个打桩。
 * spy 会拦截内部自调用，所以 findOrCreateByName 内部对 saveTag 的调用走真实逻辑，
 * 只有它触达的 {@code getOne / count / save} 落在桩上。
 *
 * <p>删除断言盯着 {@code update(Wrapper)} 的 SET 片段而非 {@code updateBatchById}：
 * MP 全局配了 {@code logic-delete-field: isDeleted}，批量更新会把该字段从 SET 里剔除，
 * 结果是名字改成墓碑值但记录仍可见、关联却已被物理清掉。
 *
 * @author claude
 * @date 2026-08-13
 */
@DisplayName("SEO 标签管理")
class SeoTagServiceImplTest {

    private static final long EXISTING_TAG_ID = 1001L;

    private static final String TAG_NAME = "直播复盘";

    private static final String TAG_SLUG = "zhibofupan";

    private SeoSlugService seoSlugService;

    private SeoArticleTagService seoArticleTagService;

    private SeoTagServiceImpl seoTagService;

    /** 注册实体到 MP 的 lambda 列名缓存，否则 LambdaQueryWrapper 构造即抛异常 */
    @BeforeAll
    static void initLambdaColumnCache() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        assistant.setCurrentNamespace("seoTagServiceImplTest");
        TableInfoHelper.initTableInfo(assistant, SeoTagEntity.class);
    }

    /** 内容变更后必须调它让官网缓存失效，测试里断言这一点 */
    private SeoSiteService seoSiteService;

    @BeforeEach
    void setUp() {
        seoSiteService = Mockito.mock(SeoSiteService.class);
        seoSlugService = Mockito.mock(SeoSlugService.class);
        seoArticleTagService = Mockito.mock(SeoArticleTagService.class);
        seoTagService = Mockito.spy(new SeoTagServiceImpl(
                seoSlugService, seoArticleTagService, Mockito.mock(SeoSlugHistoryService.class),
                seoSiteService));
    }

    @Nested
    @DisplayName("按名称查找或创建（导入链路复用）")
    class FindOrCreateByName {

        @Test
        @DisplayName("标签已存在时返回既有 ID，不新建也不再算 slug")
        void findOrCreateByName_existingTag_returnsExistingIdWithoutInsert() {
            doReturn(existingTag()).when(seoTagService).getOne(any());

            Long id = seoTagService.findOrCreateByName(TAG_NAME);

            assertEquals(EXISTING_TAG_ID, id);
            verify(seoTagService, never()).save(any(SeoTagEntity.class));
            verifyNoInteractions(seoSlugService);
        }

        @Test
        @DisplayName("标签不存在时新建，状态默认启用")
        void findOrCreateByName_newTag_createsTagWithStatusEnabled() {
            doReturn(null).when(seoTagService).getOne(any());
            stubNameAvailable();
            when(seoSlugService.resolveSlug(eq(SeoConstant.SLUG_TYPE_TAG), eq(TAG_NAME), isNull(), isNull()))
                    .thenReturn(TAG_SLUG);
            doReturn(true).when(seoTagService).save(any(SeoTagEntity.class));

            Long id = seoTagService.findOrCreateByName(TAG_NAME);

            SeoTagEntity saved = captureSavedTag();
            // 新建标签必然 0 篇文章，一律落关闭态（后台反馈第 3 条）。
            // 原先这里是启用——改规则后若不同步，批量导入会整批失败
            assertEquals(SeoConstant.STATUS_DISABLED, saved.getTagStatus(),
                    "导入自动创建的标签须为关闭态：此刻它还没有任何关联文章");
            assertEquals(TAG_NAME, saved.getTagName());
            assertEquals(TAG_SLUG, saved.getSlug());
            assertEquals(SeoConstant.NOT_DELETED, saved.getIsDeleted());
            assertNotNull(saved.getCreateDate());
            assertNotNull(saved.getUpdateDate());
            assertNotNull(id, "须返回新建标签的 ID");
            assertEquals(saved.getId(), id);
        }

        @Test
        @DisplayName("名称为空时返回 null，不查库也不新建")
        void findOrCreateByName_blankName_returnsNullWithoutQuery() {
            for (String name : new String[]{null, "", "   "}) {
                assertNull(seoTagService.findOrCreateByName(name), "输入 [" + name + "] 应返回 null");
            }

            verify(seoTagService, never()).getOne(any());
            verify(seoTagService, never()).save(any(SeoTagEntity.class));
        }

        @Test
        @DisplayName("名称首尾空白先去除，避免「直播复盘」与「直播复盘 」建成两个标签")
        void findOrCreateByName_nameWithSurroundingWhitespace_usesTrimmedName() {
            AtomicReference<Collection<Object>> paramsRef = new AtomicReference<>(new ArrayList<>());
            doAnswer(invocation -> {
                LambdaQueryWrapper<SeoTagEntity> wrapper = invocation.getArgument(0);
                // 参数值是渲染 SQL 时才登记的，必须先取一次 SQL
                wrapper.getTargetSql();
                paramsRef.set(wrapper.getParamNameValuePairs().values());
                return null;
            }).when(seoTagService).getOne(any());
            stubNameAvailable();
            when(seoSlugService.resolveSlug(any(), any(), any(), any())).thenReturn(TAG_SLUG);
            doReturn(true).when(seoTagService).save(any(SeoTagEntity.class));

            seoTagService.findOrCreateByName("  " + TAG_NAME + "  ");

            assertTrue(paramsRef.get().contains(TAG_NAME),
                    "查重应使用去空白后的名称，实际参数：" + paramsRef.get());
            assertEquals(TAG_NAME, captureSavedTag().getTagName(), "入库名称须已去空白");
        }

        @Test
        @DisplayName("并发下对手已插入同名标签时重查取回其 ID，而非在已标记回滚的事务里重试插入")
        void findOrCreateByName_concurrentInsertConflict_returnsIdFromRequery() {
            SeoTagEntity concurrent = existingTag();
            doReturn(null, concurrent).when(seoTagService).getOne(any());
            stubNameAvailable();
            when(seoSlugService.resolveSlug(any(), any(), any(), any())).thenReturn(TAG_SLUG);
            doThrow(new DuplicateKeyException("uk_seo_tag_name"))
                    .when(seoTagService).save(any(SeoTagEntity.class));

            Long id = seoTagService.findOrCreateByName(TAG_NAME);

            assertEquals(EXISTING_TAG_ID, id, "应返回对手插入的那条标签 ID");
        }

        @Test
        @DisplayName("插入冲突后重查仍查不到时返回 null，而不是把异常抛给导入流程")
        void findOrCreateByName_conflictAndRequeryEmpty_returnsNull() {
            doReturn(null, (SeoTagEntity) null).when(seoTagService).getOne(any());
            stubNameAvailable();
            when(seoSlugService.resolveSlug(any(), any(), any(), any())).thenReturn(TAG_SLUG);
            doThrow(new DuplicateKeyException("uk_seo_tag_name"))
                    .when(seoTagService).save(any(SeoTagEntity.class));

            assertNull(seoTagService.findOrCreateByName(TAG_NAME));
        }
    }

    @Nested
    @DisplayName("标签列表排序与分页")
    class TagListing {

        @Test
        @DisplayName("按关联文章数倒序排列")
        void queryPage_orderByCount_sortsByArticleCountDescending() {
            stubTagList(fourTags());
            stubArticleCounts(1, 5, 3, 5);

            PageUtils<SeoTagListVo> result = seoTagService.queryPage(buildListBo("count", 1, 10));

            assertEquals(List.of(5, 5, 3, 1), result.getList().stream()
                    .map(SeoTagListVo::getArticleCount).toList());
        }

        @Test
        @DisplayName("文章数相同时按创建时间倒序，保证排序稳定可预期")
        void queryPage_tiedArticleCount_fallsBackToCreateDateDescending() {
            stubTagList(fourTags());
            stubArticleCounts(1, 5, 3, 5);

            PageUtils<SeoTagListVo> result = seoTagService.queryPage(buildListBo("count", 1, 10));

            // tag2 与 tag4 都是 5 篇，tag4 建得更晚，应排在前
            assertEquals("tag4", result.getList().get(0).getTagName());
            assertEquals("tag2", result.getList().get(1).getTagName());
        }

        @Test
        @DisplayName("按文章数排序时分页元数据基于全量结果计算")
        void queryPage_orderByCount_returnsPageMetadataForWholeResultSet() {
            stubTagList(fourTags());
            stubArticleCounts(1, 5, 3, 5);

            PageUtils<SeoTagListVo> result = seoTagService.queryPage(buildListBo("count", 1, 2));

            assertEquals(4, result.getTotalCount(), "总数须为全量而非当前页大小");
            assertEquals(2, result.getPageSize());
            assertEquals(1, result.getCurrPage());
            assertEquals(2, result.getTotalPage());
            assertEquals(2, result.getList().size());
        }

        @Test
        @DisplayName("第二页返回剩余数据且不与第一页重叠")
        void queryPage_orderByCountSecondPage_returnsRemainingRecords() {
            stubTagList(fourTags());
            stubArticleCounts(1, 5, 3, 5);

            PageUtils<SeoTagListVo> firstPage = seoTagService.queryPage(buildListBo("count", 1, 2));
            PageUtils<SeoTagListVo> secondPage = seoTagService.queryPage(buildListBo("count", 2, 2));

            assertEquals(List.of("tag4", "tag2"), firstPage.getList().stream()
                    .map(SeoTagListVo::getTagName).toList());
            assertEquals(List.of("tag3", "tag1"), secondPage.getList().stream()
                    .map(SeoTagListVo::getTagName).toList());
            assertEquals(2, secondPage.getCurrPage());
        }

        @Test
        @DisplayName("页码越界时返回空列表而非抛下标异常")
        void queryPage_pageBeyondTotal_returnsEmptyList() {
            stubTagList(fourTags());
            stubArticleCounts(1, 5, 3, 5);

            PageUtils<SeoTagListVo> result = seoTagService.queryPage(buildListBo("count", 99, 2));

            assertTrue(result.getList().isEmpty());
            assertEquals(4, result.getTotalCount());
        }

        @Test
        @DisplayName("page / limit 为空或非法时套用默认值")
        void queryPage_invalidPageAndLimit_appliesDefaults() {
            stubTagList(fourTags());
            stubArticleCounts(1, 5, 3, 5);

            PageUtils<SeoTagListVo> nullParams = seoTagService.queryPage(buildListBo("count", null, null));
            PageUtils<SeoTagListVo> zeroParams = seoTagService.queryPage(buildListBo("count", 0, 0));

            assertEquals(1, nullParams.getCurrPage());
            assertEquals(10, nullParams.getPageSize());
            assertEquals(4, nullParams.getList().size());
            assertEquals(1, zeroParams.getCurrPage());
            assertEquals(10, zeroParams.getPageSize());
        }

        @Test
        @DisplayName("orderBy 缺省等同于按文章数排序，走内存排序而非数据库分页")
        void queryPage_orderByOmitted_defaultsToArticleCountOrdering() {
            stubTagList(fourTags());
            stubArticleCounts(1, 5, 3, 5);

            PageUtils<SeoTagListVo> result = seoTagService.queryPage(buildListBo(null, 1, 10));

            assertEquals("tag4", result.getList().get(0).getTagName());
            verify(seoTagService, never()).page(any(), any());
        }

        @Test
        @DisplayName("orderBy=new 时走数据库分页，不再按文章数重排")
        void queryPage_orderByNew_usesDatabasePaginationWithoutResorting() {
            List<SeoTagEntity> records = List.of(tag(1L, "tag1"), tag(2L, "tag2"));
            Page<SeoTagEntity> page = new Page<>(1, 10, 25L);
            page.setRecords(records);
            doReturn(page).when(seoTagService).page(any(), any());
            Map<Long, Integer> counts = new HashMap<>();
            counts.put(1L, 1);
            counts.put(2L, 9);
            when(seoArticleTagService.countArticlesByTagIds(anyList())).thenReturn(counts);

            PageUtils<SeoTagListVo> result = seoTagService.queryPage(buildListBo("new", 1, 10));

            assertEquals(List.of("tag1", "tag2"), result.getList().stream()
                    .map(SeoTagListVo::getTagName).toList());
            assertEquals(25, result.getTotalCount());
            assertEquals(3, result.getTotalPage());
            verify(seoTagService, never()).list(ArgumentMatchers.<Wrapper<SeoTagEntity>>any());
        }

        @Test
        @DisplayName("结果为空时不再发起文章数聚合查询")
        void queryPage_noMatchingTags_returnsEmptyListWithoutCountQuery() {
            stubTagList(new ArrayList<>());

            PageUtils<SeoTagListVo> result = seoTagService.queryPage(buildListBo("count", 1, 10));

            assertTrue(result.getList().isEmpty());
            assertEquals(0, result.getTotalCount());
            verifyNoInteractions(seoArticleTagService);
        }

        @Test
        @DisplayName("查询恒带 is_deleted 过滤，并按状态与名称筛选")
        void queryPage_statusAndNameGiven_appendsBothConditions() {
            AtomicReference<String> sqlRef = stubTagListCapturingSql();

            SeoTagListBo bo = buildListBo("count", 1, 10);
            bo.setTagName("复盘");
            bo.setTagStatus(SeoConstant.STATUS_ENABLED);
            seoTagService.queryPage(bo);

            assertTrue(sqlRef.get().contains("is_deleted ="), "实际 SQL：" + sqlRef.get());
            assertTrue(sqlRef.get().contains("status ="), "实际 SQL：" + sqlRef.get());
            assertTrue(sqlRef.get().contains("LIKE"), "实际 SQL：" + sqlRef.get());
        }

        @Test
        @DisplayName("状态未传时不拼 status 条件——列表默认返回全部状态")
        void queryPage_statusOmitted_omitsStatusCondition() {
            AtomicReference<String> sqlRef = stubTagListCapturingSql();

            seoTagService.queryPage(buildListBo("count", 1, 10));

            assertTrue(sqlRef.get().contains("is_deleted ="), "实际 SQL：" + sqlRef.get());
            assertFalse(sqlRef.get().contains("status ="), "实际 SQL：" + sqlRef.get());
        }

        @Test
        @DisplayName("下拉选项只取启用且未删除的标签")
        void listEnabled_anyCall_filtersEnabledAndNotDeleted() {
            AtomicReference<String> sqlRef = stubTagListCapturingSql();

            List<SeoTagOptionVo> options = seoTagService.listEnabled();

            assertTrue(options.isEmpty());
            assertTrue(sqlRef.get().contains("status ="), "实际 SQL：" + sqlRef.get());
            assertTrue(sqlRef.get().contains("is_deleted ="), "实际 SQL：" + sqlRef.get());
        }

        @Test
        @DisplayName("按 ID 批量取标签时空集合直接返回，不查库")
        void listByIds_emptyIds_returnsEmptyListWithoutQuery() {
            assertTrue(seoTagService.listByIds(null).isEmpty());
            assertTrue(seoTagService.listByIds(new ArrayList<>()).isEmpty());

            verify(seoTagService, never()).list(ArgumentMatchers.<Wrapper<SeoTagEntity>>any());
        }
    }

    @Nested
    @DisplayName("标签删除与墓碑值（AC-3）")
    class TagDeletion {

        @Test
        @DisplayName("name / slug 都用 CONCAT 就地改写为 {原值}__del_{id}，使同名同 slug 可重建")
        void deleteTags_existingTags_writesTombstoneForNameAndSlug() {
            List<LambdaUpdateWrapper<SeoTagEntity>> wrappers = stubBaseMapperUpdate(2);

            seoTagService.deleteTags(List.of(1001L, 1002L));

            String sqlSet = wrappers.get(0).getSqlSet();
            // 两列都要改：name 有唯一索引，只改 slug 的话删完就无法重建同名标签。
            // 用 CONCAT 让数据库就地拼，省掉「先 SELECT 再逐条 UPDATE」的往返与中间窗口
            assertTrue(sqlSet.contains("name = CONCAT(name, '__del_', id)"), "实际 SET：" + sqlSet);
            assertTrue(sqlSet.contains("slug = CONCAT(slug, '__del_', id)"), "实际 SET：" + sqlSet);
            // 后缀取各自主键，同名记录先后删除也不会互撞唯一索引
            assertTrue(sqlSet.contains("id"), "墓碑后缀未包含主键，实际 SET：" + sqlSet);
            verify(seoTagService, never()).list(ArgumentMatchers.<Wrapper<SeoTagEntity>>any());
        }

        @Test
        @DisplayName("is_deleted 必须进 SET 片段，否则被 MP 逻辑删除过滤静默丢弃")
        void deleteTags_existingTags_putsIsDeletedIntoSetClause() {
            List<LambdaUpdateWrapper<SeoTagEntity>> wrappers = stubBaseMapperUpdate(1);

            seoTagService.deleteTags(List.of(1001L));

            LambdaUpdateWrapper<SeoTagEntity> wrapper = wrappers.get(0);
            assertTrue(wrapper.getSqlSet().contains("is_deleted="),
                    "实际 SET：" + wrapper.getSqlSet());
            assertTrue(wrapper.getSqlSet().contains("update_date="),
                    "删除须刷新 updateDate，实际 SET：" + wrapper.getSqlSet());
            assertTrue(paramsOf(wrapper).contains(SeoConstant.DELETED),
                    "实际参数：" + paramsOf(wrapper));
            assertFalse(wrapper.getSqlSet().contains("status="),
                    "删除不应顺带改状态，实际 SET：" + wrapper.getSqlSet());
        }

        @Test
        @DisplayName("整批走一条 UPDATE，WHERE 同时限定 IN 与未删除")
        void deleteTags_multipleTags_singleUpdateScopedToIds() {
            List<LambdaUpdateWrapper<SeoTagEntity>> wrappers = stubBaseMapperUpdate(2);

            seoTagService.deleteTags(List.of(1001L, 1002L));

            assertEquals(1, wrappers.size(), "一批应只发一条 update，实际发了 " + wrappers.size() + " 条");
            String sql = wrappers.get(0).getTargetSql();
            assertTrue(sql.contains("IN"), "缺少 IN 条件会全表更新，实际 WHERE：" + sql);
            // 必须带 is_deleted=0：否则重复删同一批会把墓碑后缀叠加成 xxx__del_1__del_1
            assertTrue(sql.contains("is_deleted ="), "实际 WHERE：" + sql);
            Collection<Object> params = paramsOf(wrappers.get(0));
            assertTrue(params.contains(1001L) && params.contains(1002L), "实际参数：" + params);
        }

        @Test
        @DisplayName("删除标签同时清理文章关联，文章本身不受影响")
        void deleteTags_existingTags_clearsArticleRelations() {
            List<Long> ids = List.of(1001L, 1002L);
            stubBaseMapperUpdate(2);

            seoTagService.deleteTags(ids);

            verify(seoArticleTagService).removeByTagIds(ids);
        }

        @Test
        @DisplayName("一条都没删到时抛业务异常，不报「删除成功」")
        void deleteTags_nothingAffected_throwsInsteadOfSilentSuccess() {
            stubBaseMapperUpdate(0);

            BusinessException thrown = assertThrows(BusinessException.class,
                    () -> seoTagService.deleteTags(List.of(9999L)));

            assertTrue(thrown.getMessage().contains("不存在或已被删除"), "实际：" + thrown.getMessage());
            verifyNoInteractions(seoArticleTagService);
        }

        @Test
        @DisplayName("空集合直接返回，不发更新也不清关联")
        void deleteTags_emptyIds_doesNothing() {
            seoTagService.deleteTags(null);
            seoTagService.deleteTags(new ArrayList<>());

            verify(seoTagService, never()).update(ArgumentMatchers.<Wrapper<SeoTagEntity>>any());
            verifyNoInteractions(seoArticleTagService);
        }

    }

    @Nested
    @DisplayName("标签状态批量变更")
    class TagStatusChange {

        @Test
        @DisplayName("批量启用时逐条写入状态与 updateDate，不动其他字段")
        void changeStatus_multipleIds_updatesStatusAndUpdateDateOnly() {
            AtomicReference<List<SeoTagEntity>> updatesRef = stubUpdateBatchCapturingArgument();
            // 启用前会校验文章数（后台反馈第 3 条），这里让两个标签都有文章
            when(seoArticleTagService.countArticlesByTagIds(any()))
                    .thenReturn(java.util.Map.of(1001L, 5, 1002L, 8));

            seoTagService.changeStatus(List.of(1001L, 1002L), SeoConstant.STATUS_ENABLED);

            List<SeoTagEntity> updates = updatesRef.get();
            assertEquals(2, updates.size());
            assertEquals(1001L, updates.get(0).getId());
            assertEquals(SeoConstant.STATUS_ENABLED, updates.get(0).getTagStatus());
            assertNotNull(updates.get(0).getUpdateDate());
            assertNull(updates.get(0).getTagName(), "状态变更不应改写 name");
            assertNull(updates.get(0).getSlug(), "状态变更不应改写 slug");
            assertNull(updates.get(0).getIsDeleted(), "状态变更不应改写 isDeleted");
        }

        @Test
        @DisplayName("批量禁用时写入传入的状态值")
        void changeStatus_disableRequested_writesGivenStatus() {
            AtomicReference<List<SeoTagEntity>> updatesRef = stubUpdateBatchCapturingArgument();

            seoTagService.changeStatus(List.of(1001L), SeoConstant.STATUS_DISABLED);

            assertEquals(SeoConstant.STATUS_DISABLED, updatesRef.get().get(0).getTagStatus());
        }

        @Test
        @DisplayName("空集合抛业务异常——静默成功会让前端显示「操作成功」而实际没做事")
        void changeStatus_emptyIds_throwsBusinessException() {
            assertThrows(BusinessException.class,
                    () -> seoTagService.changeStatus(null, SeoConstant.STATUS_ENABLED));
            assertThrows(BusinessException.class,
                    () -> seoTagService.changeStatus(new ArrayList<>(), SeoConstant.STATUS_ENABLED));

            verify(seoTagService, never()).updateBatchById(anyList());
        }

        @Test
        @DisplayName("ID 全为 null 时抛业务异常，不发出一条空更新")
        void changeStatus_allIdsNull_throwsBusinessException() {
            List<Long> nullIds = new ArrayList<>();
            nullIds.add(null);

            assertThrows(BusinessException.class,
                    () -> seoTagService.changeStatus(nullIds, SeoConstant.STATUS_ENABLED));

            verify(seoTagService, never()).updateBatchById(anyList());
        }

        @Test
        @DisplayName("状态为空或超出枚举范围时抛业务异常，避免把 status 写成脏值")
        void changeStatus_invalidStatus_throwsBusinessException() {
            assertThrows(BusinessException.class, () -> seoTagService.changeStatus(List.of(1001L), null));
            assertThrows(BusinessException.class, () -> seoTagService.changeStatus(List.of(1001L), 5));
            assertThrows(BusinessException.class, () -> seoTagService.changeStatus(List.of(1001L), -1));

            verify(seoTagService, never()).updateBatchById(anyList());
        }
    }

    private SeoTagListBo buildListBo(String orderBy, Integer page, Integer limit) {
        SeoTagListBo bo = new SeoTagListBo();
        bo.setOrderBy(orderBy);
        bo.setPage(page);
        bo.setLimit(limit);
        return bo;
    }

    private SeoTagEntity existingTag() {
        SeoTagEntity entity = tag(EXISTING_TAG_ID, TAG_NAME);
        entity.setSlug(TAG_SLUG);
        return entity;
    }

    /** 创建时间按 id 递增，便于断言「文章数相同时按创建时间倒序」 */
    private SeoTagEntity tag(Long id, String name) {
        SeoTagEntity entity = new SeoTagEntity();
        entity.setId(id);
        entity.setTagName(name);
        entity.setSlug(name + "-slug");
        entity.setTagStatus(SeoConstant.STATUS_ENABLED);
        entity.setIsDeleted(SeoConstant.NOT_DELETED);
        entity.setCreateDate(LocalDateTime.of(2026, 8, 1, 0, 0).plusDays(id));
        entity.setUpdateDate(LocalDateTime.of(2026, 8, 1, 0, 0).plusDays(id));
        return entity;
    }

    private List<SeoTagEntity> fourTags() {
        return List.of(tag(1L, "tag1"), tag(2L, "tag2"), tag(3L, "tag3"), tag(4L, "tag4"));
    }

    /** 名称唯一校验放行 */
    private void stubNameAvailable() {
        doReturn(0L).when(seoTagService).count(ArgumentMatchers.<Wrapper<SeoTagEntity>>any());
    }

    /** 打桩继承自 ServiceImpl 的 list(Wrapper) */
    private void stubTagList(List<SeoTagEntity> tags) {
        doReturn(tags).when(seoTagService).list(ArgumentMatchers.<Wrapper<SeoTagEntity>>any());
    }

    /** 打桩 list(Wrapper) 返回空集合，同时抓取渲染后的 SQL */
    private AtomicReference<String> stubTagListCapturingSql() {
        AtomicReference<String> sqlRef = new AtomicReference<>("");
        doAnswer(invocation -> {
            LambdaQueryWrapper<SeoTagEntity> wrapper = invocation.getArgument(0);
            sqlRef.set(wrapper.getTargetSql());
            return new ArrayList<SeoTagEntity>();
        }).when(seoTagService).list(ArgumentMatchers.<Wrapper<SeoTagEntity>>any());
        return sqlRef;
    }

    /** 按 tag1..tag4 的顺序指定各自的关联文章数 */
    private void stubArticleCounts(int first, int second, int third, int fourth) {
        Map<Long, Integer> counts = new HashMap<>();
        counts.put(1L, first);
        counts.put(2L, second);
        counts.put(3L, third);
        counts.put(4L, fourth);
        when(seoArticleTagService.countArticlesByTagIds(anyList())).thenReturn(counts);
    }

    /** 打桩 update(Wrapper) 并按调用顺序收集 LambdaUpdateWrapper */
    /**
     * 桩住 baseMapper.update 并捕获 wrapper。
     *
     * <p>批量删除走 {@code getBaseMapper().update(null, wrapper)}——
     * {@code ServiceImpl.update(wrapper)} 只返回 boolean，拿不到影响行数，
     * 而「一条都没删到」需要它才能判断。
     *
     * @param affectedRows 每次调用返回的影响行数
     */
    private List<LambdaUpdateWrapper<SeoTagEntity>> stubBaseMapperUpdate(int affectedRows) {
        List<LambdaUpdateWrapper<SeoTagEntity>> wrappers = new ArrayList<>();
        SeoTagDao dao = Mockito.mock(SeoTagDao.class);
        doReturn(dao).when(seoTagService).getBaseMapper();
        doAnswer(invocation -> {
            wrappers.add(invocation.getArgument(1));
            return affectedRows;
        }).when(dao).update(isNull(), ArgumentMatchers.<Wrapper<SeoTagEntity>>any());
        return wrappers;
    }

    private List<LambdaUpdateWrapper<SeoTagEntity>> stubUpdateByWrapper() {
        List<LambdaUpdateWrapper<SeoTagEntity>> wrappers = new ArrayList<>();
        doAnswer(invocation -> {
            wrappers.add(invocation.getArgument(0));
            return true;
        }).when(seoTagService).update(ArgumentMatchers.<Wrapper<SeoTagEntity>>any());
        return wrappers;
    }

    /**
     * 取出 wrapper 登记的全部参数值。
     *
     * <p>WHERE 片段的参数是渲染 SQL 时才登记的，故先取一次 SQL 再读参数。
     */
    private Collection<Object> paramsOf(LambdaUpdateWrapper<SeoTagEntity> wrapper) {
        wrapper.getSqlSet();
        wrapper.getTargetSql();
        return new ArrayList<>(wrapper.getParamNameValuePairs().values());
    }

    private AtomicReference<List<SeoTagEntity>> stubUpdateBatchCapturingArgument() {
        AtomicReference<List<SeoTagEntity>> ref = new AtomicReference<>();
        doAnswer(invocation -> {
            ref.set(new ArrayList<>(invocation.<Collection<SeoTagEntity>>getArgument(0)));
            return true;
        }).when(seoTagService).updateBatchById(anyList());
        return ref;
    }

    private SeoTagEntity captureSavedTag() {
        ArgumentCaptor<SeoTagEntity> captor = ArgumentCaptor.forClass(SeoTagEntity.class);
        verify(seoTagService).save(captor.capture());
        return captor.getValue();
    }
}
