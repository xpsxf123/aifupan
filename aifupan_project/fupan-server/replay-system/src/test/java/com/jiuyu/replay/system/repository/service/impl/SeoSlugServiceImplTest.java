package com.jiuyu.replay.system.repository.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.jiuyu.replay.system.bo.SeoSlugSuggestBo;
import com.jiuyu.replay.system.constant.SeoConstant;
import com.jiuyu.replay.system.entity.SeoArticleEntity;
import com.jiuyu.replay.system.entity.SeoCategoryEntity;
import com.jiuyu.replay.system.entity.SeoTagEntity;
import com.jiuyu.replay.system.repository.dao.SeoArticleDao;
import com.jiuyu.replay.system.repository.dao.SeoCategoryDao;
import com.jiuyu.replay.system.repository.dao.SeoTagDao;
import com.jiuyu.replay.system.util.SeoSlugUtil;
import com.jiuyu.replay.system.vo.SeoSlugSuggestVo;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * SeoSlugServiceImpl 单测，对应 openspec AC-5 与 §5.1 第 1 条易错点。
 *
 * <p><b>为什么这一层必须有测试</b>：SeoSlugUtil 只接一个 {@code Predicate<String>}，
 * 「查重排除自身」的 excludeId 语义完全落在本类的 LambdaQueryWrapper 里，工具类测不到。
 * 上一轮 code review 明确要求把该场景挪到本层，用真实 excludeId 验证
 * {@code .ne(id, excludeId)} 条件生效。
 *
 * <p><b>被测类不继承 MP ServiceImpl</b>，三个 Dao 直接 mock 即可，无需处理 baseMapper。
 * 但 LambdaQueryWrapper 的方法引用列名解析依赖 TableInfo 缓存，故 {@link #initLambdaColumnCache()}
 * 中先把三个实体注册进缓存——否则连 wrapper 都构造不出来。
 *
 * @author claude
 * @date 2026-08-13
 */
@DisplayName("SEO slug 查重与建议")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SeoSlugServiceImplTest {

    /** 假表里那条已存在的标签：name=直播复盘 slug=zhibofupan */
    private static final long EXISTING_TAG_ID = 1001L;

    /** 另一条记录的 ID，用于「编辑别人」场景 */
    private static final long OTHER_TAG_ID = 2002L;

    /** 已被占用的 slug */
    private static final String TAKEN_SLUG = "zhibofupan";

    /** 占用者名称 */
    private static final String OWNER_NAME = "直播复盘";

    @Mock
    private SeoArticleDao seoArticleDao;

    @Mock
    private SeoCategoryDao seoCategoryDao;

    @Mock
    private SeoTagDao seoTagDao;

    private SeoSlugServiceImpl seoSlugService;

    /**
     * 注册实体到 MP 的 lambda 列名缓存。
     *
     * <p>不注册的话 {@code new LambdaQueryWrapper<SeoTagEntity>().eq(SeoTagEntity::getSlug, x)}
     * 会直接抛「can not find lambda cache for this entity」，被测代码根本走不到断言。
     */
    @BeforeAll
    static void initLambdaColumnCache() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        assistant.setCurrentNamespace("seoSlugServiceImplTest");
        TableInfoHelper.initTableInfo(assistant, SeoArticleEntity.class);
        TableInfoHelper.initTableInfo(assistant, SeoCategoryEntity.class);
        TableInfoHelper.initTableInfo(assistant, SeoTagEntity.class);
    }

    @BeforeEach
    void setUp() {
        seoSlugService = new SeoSlugServiceImpl(seoArticleDao, seoCategoryDao, seoTagDao);
    }

    @Nested
    @DisplayName("编辑时查重排除自身")
    class ExcludeSelfOnDuplicateCheck {

        @Test
        @DisplayName("传入 excludeId 时查询条件带 id <> excludeId")
        void isSlugTaken_withExcludeId_appendsNotEqualIdCondition() {
            AtomicReference<String> sqlRef = stubTagTableAsEmptyAndCaptureSql();

            seoSlugService.isSlugTaken(SeoConstant.SLUG_TYPE_TAG, TAKEN_SLUG, EXISTING_TAG_ID);

            assertTrue(sqlRef.get().contains("id <>"),
                    "查询未带排除自身条件，编辑时会把自己判成占用者。实际 SQL：" + sqlRef.get());
        }

        @Test
        @DisplayName("excludeId 的实际值进入查询参数，而非被条件开关吞掉")
        void isSlugTaken_withExcludeId_bindsExcludeIdAsQueryParameter() {
            AtomicReference<Collection<Object>> paramsRef = new AtomicReference<>();
            when(seoTagDao.selectOne(any())).thenAnswer(invocation -> {
                LambdaQueryWrapper<SeoTagEntity> wrapper = invocation.getArgument(0);
                // 参数值是渲染 SQL 时才登记的，必须先取一次 SQL
                wrapper.getTargetSql();
                paramsRef.set(wrapper.getParamNameValuePairs().values());
                return null;
            });

            seoSlugService.isSlugTaken(SeoConstant.SLUG_TYPE_TAG, TAKEN_SLUG, EXISTING_TAG_ID);

            assertTrue(paramsRef.get().contains(EXISTING_TAG_ID),
                    "excludeId 未绑定进查询参数，实际参数：" + paramsRef.get());
        }

        @Test
        @DisplayName("不传 excludeId 时不追加 id <> 条件——新建场景不应排除任何记录")
        void isSlugTaken_withoutExcludeId_omitsNotEqualIdCondition() {
            AtomicReference<String> sqlRef = stubTagTableAsEmptyAndCaptureSql();

            seoSlugService.isSlugTaken(SeoConstant.SLUG_TYPE_TAG, TAKEN_SLUG, null);

            assertFalse(sqlRef.get().contains("id <>"),
                    "新建场景不应排除记录，实际 SQL：" + sqlRef.get());
        }

        @Test
        @DisplayName("查询恒带 is_deleted 过滤——已删除记录不占用 slug")
        void isSlugTaken_anyType_filtersDeletedRecords() {
            AtomicReference<String> sqlRef = stubTagTableAsEmptyAndCaptureSql();

            seoSlugService.isSlugTaken(SeoConstant.SLUG_TYPE_TAG, TAKEN_SLUG, null);

            assertTrue(sqlRef.get().contains("is_deleted ="),
                    "缺少 is_deleted 过滤，实际 SQL：" + sqlRef.get());
        }

        @Test
        @DisplayName("slug 的占用者就是自己时判为未占用")
        void isSlugTaken_slugOwnedBySelf_returnsFalse() {
            stubSingleRowTagTable();

            boolean taken = seoSlugService.isSlugTaken(
                    SeoConstant.SLUG_TYPE_TAG, TAKEN_SLUG, EXISTING_TAG_ID);

            assertFalse(taken, "把自己判成了占用者");
        }

        @Test
        @DisplayName("slug 的占用者是别人时判为已占用")
        void isSlugTaken_slugOwnedByAnother_returnsTrue() {
            stubSingleRowTagTable();

            boolean taken = seoSlugService.isSlugTaken(
                    SeoConstant.SLUG_TYPE_TAG, TAKEN_SLUG, OTHER_TAG_ID);

            assertTrue(taken, "别人占用的 slug 必须判为已占用");
        }

        @Test
        @DisplayName("编辑自身且 slug 未变更时原样返回，不追加随机数")
        void resolveSlug_editingSelfWithUnchangedSlug_keepsSlugUnchanged() {
            stubSingleRowTagTable();

            String resolved = seoSlugService.resolveSlug(
                    SeoConstant.SLUG_TYPE_TAG, OWNER_NAME, null, EXISTING_TAG_ID);

            assertEquals(TAKEN_SLUG, resolved, "编辑时给自己追加了后缀");
        }

        @Test
        @DisplayName("反复保存同一条记录不会叠加后缀")
        void resolveSlug_repeatedSaveOfSameRecord_doesNotStackSuffixes() {
            stubSingleRowTagTable();

            for (int i = 0; i < 5; i++) {
                String resolved = seoSlugService.resolveSlug(
                        SeoConstant.SLUG_TYPE_TAG, OWNER_NAME, TAKEN_SLUG, EXISTING_TAG_ID);
                assertEquals(TAKEN_SLUG, resolved, "第 " + (i + 1) + " 次保存被追加了后缀");
            }
        }

        @Test
        @DisplayName("编辑另一条记录撞上别人的 slug 时追加 4 位随机数")
        void resolveSlug_slugTakenByAnotherRecord_appendsRandomSuffix() {
            stubSingleRowTagTable();

            String resolved = seoSlugService.resolveSlug(
                    SeoConstant.SLUG_TYPE_TAG, OWNER_NAME, null, OTHER_TAG_ID);

            assertTrue(resolved.matches("^zhibofupan-\\d{4}$"), "实际：" + resolved);
        }

        @Test
        @DisplayName("新建时撞上已有 slug 同样追加随机数")
        void resolveSlug_newRecordWithTakenSlug_appendsRandomSuffix() {
            stubSingleRowTagTable();

            String resolved = seoSlugService.resolveSlug(
                    SeoConstant.SLUG_TYPE_TAG, OWNER_NAME, null, null);

            assertTrue(resolved.matches("^zhibofupan-\\d{4}$"), "实际：" + resolved);
        }

        @Test
        @DisplayName("名称转不出拼音时用类型前缀兜底，不落空 slug")
        void resolveSlug_nameWithoutPinyin_fallsBackToTypePrefix() {
            when(seoTagDao.selectOne(any())).thenReturn(null);

            String resolved = seoSlugService.resolveSlug(
                    SeoConstant.SLUG_TYPE_TAG, "こんにちは", null, null);

            assertTrue(resolved.startsWith("tag-"), "实际：" + resolved);
            assertTrue(resolved.matches("^tag-\\d{6}$"), "实际：" + resolved);
        }
    }

    @Nested
    @DisplayName("slug 建议（AC-5）")
    class SlugSuggestion {

        @Test
        @DisplayName("未被占用时返回拼音、available=true、无占用者与备选")
        void suggest_slugAvailable_returnsPinyinWithAvailableTrue() {
            when(seoTagDao.selectOne(any())).thenReturn(null);

            SeoSlugSuggestVo vo = seoSlugService.suggest(
                    buildBo(SeoConstant.SLUG_TYPE_TAG, "话术技巧", null, null));

            assertEquals("huashujiqiao", vo.getSlug());
            assertTrue(vo.getAvailable());
            assertNull(vo.getOwnerName());
            assertTrue(vo.getSuggestions().isEmpty(), "实际：" + vo.getSuggestions());
        }

        @Test
        @DisplayName("被他人占用时返回占用者名与语义化备选")
        void suggest_slugTakenByAnother_returnsOwnerNameAndSemanticSuggestion() {
            stubSingleRowTagTable();

            // 直播复判 与 直播复盘 同拼音，是 slug 冲突的典型来源
            SeoSlugSuggestVo vo = seoSlugService.suggest(
                    buildBo(SeoConstant.SLUG_TYPE_TAG, "直播复判", null, null));

            assertEquals(TAKEN_SLUG, vo.getSlug());
            assertFalse(vo.getAvailable());
            assertEquals(OWNER_NAME, vo.getOwnerName());
            assertEquals(List.of("zhibo-fupan"), vo.getSuggestions());
        }

        @Test
        @DisplayName("占用者就是自己时 available=true——excludeId 生效")
        void suggest_slugOwnedBySelf_returnsAvailableTrue() {
            stubSingleRowTagTable();

            SeoSlugSuggestVo vo = seoSlugService.suggest(
                    buildBo(SeoConstant.SLUG_TYPE_TAG, OWNER_NAME, null, EXISTING_TAG_ID));

            assertEquals(TAKEN_SLUG, vo.getSlug());
            assertTrue(vo.getAvailable(), "编辑自身时应判为可用");
            assertNull(vo.getOwnerName());
            assertTrue(vo.getSuggestions().isEmpty(), "实际：" + vo.getSuggestions());
        }

        @Test
        @DisplayName("名称转不出拼音时返回空 slug + available=false，且不查库")
        void suggest_nameWithoutPinyin_returnsEmptySlugAndUnavailable() {
            for (String name : new String[]{"こんにちは", "안녕하세요", "🔥🔥", "！！！"}) {
                SeoSlugSuggestVo vo = seoSlugService.suggest(
                        buildBo(SeoConstant.SLUG_TYPE_TAG, name, null, null));

                assertEquals("", vo.getSlug(), "输入 [" + name + "] 应返回空 slug");
                assertFalse(vo.getAvailable(), "输入 [" + name + "] 不应判为可用");
                assertNull(vo.getOwnerName(), "输入 [" + name + "] 不应有占用者");
                assertTrue(vo.getSuggestions().isEmpty(), "输入 [" + name + "] 不应有备选");
            }
            // slug 为空时查库没有意义，且空 slug 一定不能落库
            verifyNoInteractions(seoTagDao, seoCategoryDao, seoArticleDao);
        }

        @Test
        @DisplayName("name 与 slug 都为空时返回空 slug，不查库")
        void suggest_blankNameAndSlug_returnsEmptySlugWithoutQuery() {
            SeoSlugSuggestVo blankBoth = seoSlugService.suggest(
                    buildBo(SeoConstant.SLUG_TYPE_TAG, "   ", "  ", null));
            SeoSlugSuggestVo nullBoth = seoSlugService.suggest(
                    buildBo(SeoConstant.SLUG_TYPE_TAG, null, null, null));

            assertEquals("", blankBoth.getSlug());
            assertFalse(blankBoth.getAvailable());
            assertEquals("", nullBoth.getSlug());
            assertFalse(nullBoth.getAvailable());
            verifyNoInteractions(seoTagDao, seoCategoryDao, seoArticleDao);
        }

        @Test
        @DisplayName("运营手填 slug 时做规范化而非取拼音")
        void suggest_manualSlugProvided_normalizesInsteadOfPinyin() {
            when(seoTagDao.selectOne(any())).thenReturn(null);

            SeoSlugSuggestVo vo = seoSlugService.suggest(
                    buildBo(SeoConstant.SLUG_TYPE_TAG, OWNER_NAME, "  ZhiBo Fupan ", null));

            assertEquals("zhibo-fupan", vo.getSlug());
            assertTrue(vo.getAvailable());
        }

        @Test
        @DisplayName("名称不足 4 个汉字时不给语义备选——两字一组切不出更好的值")
        void suggest_shortChineseName_returnsNoSemanticSuggestion() {
            SeoTagEntity owner = new SeoTagEntity();
            owner.setId(EXISTING_TAG_ID);
            owner.setTagName("复盘");
            when(seoTagDao.selectOne(any())).thenReturn(owner);

            SeoSlugSuggestVo vo = seoSlugService.suggest(
                    buildBo(SeoConstant.SLUG_TYPE_TAG, "复盘", null, null));

            assertEquals("fupan", vo.getSlug());
            assertFalse(vo.getAvailable());
            assertEquals("复盘", vo.getOwnerName());
            assertTrue(vo.getSuggestions().isEmpty(), "实际：" + vo.getSuggestions());
        }

        @Test
        @DisplayName("语义备选也被占用时返回空数组，不硬凑劣质变体")
        void suggest_semanticVariantAlsoTaken_returnsEmptySuggestions() {
            SeoTagEntity owner = new SeoTagEntity();
            owner.setId(EXISTING_TAG_ID);
            owner.setTagName(OWNER_NAME);
            // 任何 slug 都被占用：base 与 zhibo-fupan 变体都撞
            when(seoTagDao.selectOne(any())).thenReturn(owner);

            SeoSlugSuggestVo vo = seoSlugService.suggest(
                    buildBo(SeoConstant.SLUG_TYPE_TAG, "直播复判", null, null));

            assertFalse(vo.getAvailable());
            assertEquals(OWNER_NAME, vo.getOwnerName());
            assertTrue(vo.getSuggestions().isEmpty(), "实际：" + vo.getSuggestions());
        }

        @Test
        @DisplayName("语义备选与 base 相同时不重复给出")
        void suggest_semanticVariantEqualsBase_returnsEmptySuggestions() {
            SeoCategoryEntity owner = new SeoCategoryEntity();
            owner.setId(EXISTING_TAG_ID);
            owner.setCategoryName("直播复盘");
            when(seoCategoryDao.selectOne(any())).thenReturn(owner);

            // 手填 slug 已经是 zhibo-fupan，两字一组切分结果与之相同
            SeoSlugSuggestVo vo = seoSlugService.suggest(
                    buildBo(SeoConstant.SLUG_TYPE_CATEGORY, "直播复盘", "zhibo-fupan", null));

            assertEquals("zhibo-fupan", vo.getSlug());
            assertTrue(vo.getSuggestions().isEmpty(), "实际：" + vo.getSuggestions());
        }
    }

    @Nested
    @DisplayName("按类型路由到对应表")
    class TypeRouting {

        @Test
        @DisplayName("category 只查分类表，返回分类名作为占用者")
        void suggest_categoryType_queriesCategoryTableOnly() {
            SeoCategoryEntity owner = new SeoCategoryEntity();
            owner.setId(EXISTING_TAG_ID);
            owner.setCategoryName("直播运营");
            when(seoCategoryDao.selectOne(any())).thenReturn(owner);

            SeoSlugSuggestVo vo = seoSlugService.suggest(
                    buildBo(SeoConstant.SLUG_TYPE_CATEGORY, "直播运营", null, null));

            assertEquals("直播运营", vo.getOwnerName());
            verifyNoInteractions(seoTagDao, seoArticleDao);
        }

        @Test
        @DisplayName("article 只查文章表，返回文章标题作为占用者")
        void suggest_articleType_returnsArticleTitleAsOwnerName() {
            SeoArticleEntity owner = new SeoArticleEntity();
            owner.setId(EXISTING_TAG_ID);
            owner.setTitle("直播复盘进阶");
            when(seoArticleDao.selectOne(any())).thenReturn(owner);

            SeoSlugSuggestVo vo = seoSlugService.suggest(
                    buildBo(SeoConstant.SLUG_TYPE_ARTICLE, "直播复盘进阶", null, null));

            assertEquals("直播复盘进阶", vo.getOwnerName());
            verifyNoInteractions(seoTagDao, seoCategoryDao);
        }

        @Test
        @DisplayName("未知类型退化为按文章表查重，而不是静默放行")
        void suggest_unknownType_fallsBackToArticleTable() {
            SeoArticleEntity owner = new SeoArticleEntity();
            owner.setId(EXISTING_TAG_ID);
            owner.setTitle("直播复盘进阶");
            when(seoArticleDao.selectOne(any())).thenReturn(owner);

            SeoSlugSuggestVo vo = seoSlugService.suggest(
                    buildBo("unknownType", "直播复盘进阶", null, null));

            assertFalse(vo.getAvailable(), "未知类型不得跳过查重");
            assertEquals("直播复盘进阶", vo.getOwnerName());
            verifyNoInteractions(seoTagDao, seoCategoryDao);
        }

        @Test
        @DisplayName("兜底前缀按类型取值，不会给分类发 article 前缀")
        void resolveSlug_nameWithoutPinyin_usesTypeSpecificFallbackPrefix() {
            when(seoCategoryDao.selectOne(any())).thenReturn(null);
            when(seoArticleDao.selectOne(any())).thenReturn(null);

            String categorySlug = seoSlugService.resolveSlug(
                    SeoConstant.SLUG_TYPE_CATEGORY, "🔥🔥", null, null);
            String articleSlug = seoSlugService.resolveSlug(
                    SeoConstant.SLUG_TYPE_ARTICLE, "🔥🔥", null, null);

            assertTrue(categorySlug.startsWith("category-"), "实际：" + categorySlug);
            assertTrue(articleSlug.startsWith("article-"), "实际：" + articleSlug);
        }
    }

    @Nested
    @DisplayName("入库长度截断")
    class LengthTruncation {

        /** 60 个汉字，转全拼后远超字段长度 */
        private static final String LONG_NAME = "直播复盘话术技巧场控憋单连麦选品转化留存涨粉投流".repeat(3);

        @Test
        @DisplayName("超长标题的拼音按类型上限截断，避免新增时撞 Data too long")
        void resolveSlug_veryLongName_truncatesToTypeLimit() {
            when(seoTagDao.selectOne(any())).thenReturn(null);
            when(seoArticleDao.selectOne(any())).thenReturn(null);

            String tagSlug = seoSlugService.resolveSlug(SeoConstant.SLUG_TYPE_TAG, LONG_NAME, null, null);
            String articleSlug = seoSlugService.resolveSlug(SeoConstant.SLUG_TYPE_ARTICLE, LONG_NAME, null, null);

            assertTrue(tagSlug.length() <= SeoConstant.MAX_SLUG_LEN_CATEGORY_TAG - 5,
                    "标签 slug 超长（" + tagSlug.length() + "）：" + tagSlug);
            assertTrue(articleSlug.length() <= SeoConstant.MAX_SLUG_LEN_ARTICLE - 5,
                    "文章 slug 超长（" + articleSlug.length() + "）：" + articleSlug);
        }

        @Test
        @DisplayName("截断后仍是合法 slug，不留尾部连字符")
        void resolveSlug_veryLongName_stillProducesValidSlug() {
            when(seoTagDao.selectOne(any())).thenReturn(null);

            String slug = seoSlugService.resolveSlug(SeoConstant.SLUG_TYPE_TAG, LONG_NAME, null, null);

            assertTrue(SeoSlugUtil.isValid(slug), "实际：" + slug);
            assertFalse(slug.endsWith("-"), "截断处留下了尾部连字符：" + slug);
        }

        @Test
        @DisplayName("建议接口与入库用同一套截断规则，避免预览值与实际值不一致")
        void suggest_veryLongName_appliesSameTruncationAsResolveSlug() {
            when(seoTagDao.selectOne(any())).thenReturn(null);

            SeoSlugSuggestVo vo = seoSlugService.suggest(
                    buildBo(SeoConstant.SLUG_TYPE_TAG, LONG_NAME, null, null));
            String resolved = seoSlugService.resolveSlug(SeoConstant.SLUG_TYPE_TAG, LONG_NAME, null, null);

            assertEquals(resolved, vo.getSlug(),
                    "预览值与入库值不一致，运营会看到一个值、存成另一个值");
        }
    }

    private SeoSlugSuggestBo buildBo(String type, String name, String slug, Long excludeId) {
        SeoSlugSuggestBo bo = new SeoSlugSuggestBo();
        bo.setType(type);
        bo.setName(name);
        bo.setSlug(slug);
        bo.setExcludeId(excludeId);
        return bo;
    }

    /** 标签表恒为空，同时把渲染后的 SQL 抓出来做结构断言 */
    private AtomicReference<String> stubTagTableAsEmptyAndCaptureSql() {
        AtomicReference<String> sqlRef = new AtomicReference<>("");
        when(seoTagDao.selectOne(any())).thenAnswer(invocation -> {
            LambdaQueryWrapper<SeoTagEntity> wrapper = invocation.getArgument(0);
            sqlRef.set(wrapper.getTargetSql());
            return null;
        });
        return sqlRef;
    }

    /**
     * 单行「假表」：表里只有 id=1001 / slug=zhibofupan / name=直播复盘 一条标签。
     *
     * <p>判定只依据 wrapper <b>自己渲染出的 SQL 与登记的参数值</b>，不重写查询语义：
     * {@code ne(condition, ...)} 在 condition=false 时既不出现在 SQL 里也不登记参数，
     * 因此「SQL 含 id &lt;&gt; 且参数里出现了 1001」恰好等价于「该行被 excludeId 排除」。
     */
    private void stubSingleRowTagTable() {
        when(seoTagDao.selectOne(any())).thenAnswer(invocation -> {
            LambdaQueryWrapper<SeoTagEntity> wrapper = invocation.getArgument(0);
            // 参数值是渲染 SQL 时才登记的，必须先取一次 SQL
            String sql = wrapper.getTargetSql();
            Collection<Object> params = wrapper.getParamNameValuePairs().values();
            boolean slugMatches = params.contains(TAKEN_SLUG);
            boolean rowExcluded = sql.contains("id <>") && params.contains(EXISTING_TAG_ID);
            if (!slugMatches || rowExcluded) {
                return null;
            }
            SeoTagEntity existing = new SeoTagEntity();
            existing.setId(EXISTING_TAG_ID);
            existing.setTagName(OWNER_NAME);
            existing.setSlug(TAKEN_SLUG);
            existing.setIsDeleted(SeoConstant.NOT_DELETED);
            return existing;
        });
    }
}
