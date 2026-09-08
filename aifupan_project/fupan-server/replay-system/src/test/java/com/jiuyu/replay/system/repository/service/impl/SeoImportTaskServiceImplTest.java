package com.jiuyu.replay.system.repository.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.system.bo.SeoArticleBo;
import com.jiuyu.replay.system.bo.SeoImportFileBo;
import com.jiuyu.replay.system.constant.SeoConstant;
import com.jiuyu.replay.system.repository.service.SeoArticleService;
import com.jiuyu.replay.system.repository.service.SeoCategoryService;
import com.jiuyu.replay.system.repository.service.SeoTagService;
import com.jiuyu.replay.system.util.SeoImageFetcher;
import com.jiuyu.replay.system.vo.SeoImportItemVo;
import com.jiuyu.replay.system.vo.SeoSaveResultVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SeoImportTaskServiceImpl 单测，对应 openspec AC-7
 * （导入的失败原因必须具体、一篇失败不影响其他篇）。
 *
 * <p>被测类继承 MP {@code ServiceImpl}，用 {@link Mockito#spy} 部分模拟，
 * 四个协作者全部 mock。
 *
 * <p><b>为什么用反射调私有方法</b>：{@code importOne} 与 {@code replaceBodyImages}
 * 装着这个类的全部业务判断，而唯一的公开入口 {@code submit} 会把执行体扔进静态线程池
 * 异步执行——单测里既拿不到返回值也无法确定时序。把它们改成 public 只是为了可测而放宽可见性，
 * 反射调用把「这是内部实现」的信号保留在了原处。方法签名一旦变化，
 * 下面的 helper 会抛出明确的失败信息而不是静默跳过。
 *
 * @author claude
 * @date 2026-08-13
 */
@DisplayName("SEO 文章批量导入")
class SeoImportTaskServiceImplTest {

    private SeoArticleService seoArticleService;

    private SeoCategoryService seoCategoryService;

    private SeoTagService seoTagService;

    private SeoImageFetcher seoImageFetcher;

    private SeoImportTaskServiceImpl service;

    private static final String VALID_MD = """
            ---
            title: 直播复盘怎么做
            category: 直播运营
            cover: https://cdn.example.com/cover.jpg
            tags: [直播复盘, 数据分析]
            seoDescription: 一套可复用的直播复盘方法论
            ---

            ## 为什么复盘常常无效

            多数团队停在念数据。
            """;

    @BeforeEach
    void setUp() {
        seoArticleService = Mockito.mock(SeoArticleService.class);
        seoCategoryService = Mockito.mock(SeoCategoryService.class);
        seoTagService = Mockito.mock(SeoTagService.class);
        seoImageFetcher = Mockito.mock(SeoImageFetcher.class);

        service = Mockito.spy(new SeoImportTaskServiceImpl(
                seoArticleService, seoCategoryService, seoTagService, seoImageFetcher));

        // 默认放行路径：分类存在、标签已存在、图片抓取成功、文章保存成功、标题不重复
        when(seoCategoryService.findIdByName("直播运营")).thenReturn(100L);
        when(seoTagService.findIdByName(anyString())).thenReturn(200L);
        when(seoTagService.findOrCreateByName(anyString())).thenReturn(200L);
        when(seoImageFetcher.fetchAndUpload(anyString())).thenAnswer(invocation ->
                "https://img.example.com/seo/" + Math.abs(invocation.getArgument(0).hashCode()) + ".jpg");
        SeoSaveResultVo saved = new SeoSaveResultVo();
        saved.setId(1L);
        saved.setSlug("zhibo-fupan");
        saved.setSlugAppended(false);
        when(seoArticleService.saveArticle(any())).thenReturn(saved);
        doReturn(0L).when(seoArticleService).count(any(Wrapper.class));
    }

    private SeoImportItemVo importOne(SeoImportFileBo file, boolean autoCreateCategory) {
        try {
            Method method = SeoImportTaskServiceImpl.class.getDeclaredMethod(
                    "importOne", SeoImportFileBo.class, boolean.class);
            method.setAccessible(true);
            return (SeoImportItemVo) method.invoke(service, file, autoCreateCategory);
        } catch (InvocationTargetException e) {
            // 被测方法自身抛出的异常原样透出，否则会被包成 InvocationTargetException 看不出原因
            throw e.getCause() instanceof RuntimeException runtime
                    ? runtime : new IllegalStateException(e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("importOne 签名变了，测试需同步更新", e);
        }
    }

    private String replaceBodyImages(String html, List<String> notes) {
        try {
            Method method = SeoImportTaskServiceImpl.class.getDeclaredMethod(
                    "replaceBodyImages", String.class, List.class);
            method.setAccessible(true);
            return (String) method.invoke(service, html, notes);
        } catch (InvocationTargetException e) {
            throw e.getCause() instanceof RuntimeException runtime
                    ? runtime : new IllegalStateException(e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("replaceBodyImages 签名变了，测试需同步更新", e);
        }
    }

    private SeoArticleBo captureSavedBo() {
        ArgumentCaptor<SeoArticleBo> captor = ArgumentCaptor.forClass(SeoArticleBo.class);
        verify(seoArticleService).saveArticle(captor.capture());
        return captor.getValue();
    }

    private static String notesOf(SeoImportItemVo item) {
        return String.join(" | ", item.getNotes());
    }

    @Nested
    @DisplayName("正文图片转存")
    class BodyImages {

        @Test
        @DisplayName("带查询参数的图片地址先解 HTML 实体再请求——否则签名图床一律 403")
        void replaceBodyImages_urlWithEntities_decodedBeforeFetch() {
            // commonmark 渲染 src 时会把 & 转成 &amp;，属性里就是这个样子
            String html = "<p><img src=\"https://cdn.example.com/a.png?w=100&amp;h=200&amp;sign=abc\" /></p>";

            replaceBodyImages(html, new ArrayList<>());

            ArgumentCaptor<String> requested = ArgumentCaptor.forClass(String.class);
            verify(seoImageFetcher).fetchAndUpload(requested.capture());
            assertEquals("https://cdn.example.com/a.png?w=100&h=200&sign=abc", requested.getValue(),
                    "请求的是转义后的字面量，签名参数对不上，图床返回 403，"
                            + "功能不报错只是静默退化成「保留原链接」——而外链会烂掉正是转存存在的理由");
        }

        @Test
        @DisplayName("写回属性时重新转义，保留原链接的分支也不例外")
        void replaceBodyImages_writeBack_reEscapesEntities() {
            when(seoImageFetcher.fetchAndUpload(anyString()))
                    .thenThrow(new SeoImageFetcher.ImageFetchException("图片地址不可用"));
            String html = "<p><img src=\"https://cdn.example.com/a.png?w=1&amp;h=2\" /></p>";

            String result = replaceBodyImages(html, new ArrayList<>());

            assertTrue(result.contains("?w=1&amp;h=2"),
                    "解码后的裸 & 被写回属性，本来合法的 HTML 变成未转义的。实际：" + result);
        }

        @Test
        @DisplayName("同一张图出现多次只抓一次，不在图床存多份")
        void replaceBodyImages_duplicateUrl_fetchedOnce() {
            String url = "https://cdn.example.com/same.png";
            String html = "<img src=\"" + url + "\"><p>x</p><img src=\"" + url + "\">";

            String result = replaceBodyImages(html, new ArrayList<>());

            verify(seoImageFetcher, times(1)).fetchAndUpload(url);
            assertFalse(result.contains(url), "两处都应替换成转存地址。实际：" + result);
        }

        @Test
        @DisplayName("超过单篇 30 张的部分保留原链接并提示，不让整篇失败")
        void replaceBodyImages_beyondLimit_keptAndReported() {
            StringBuilder html = new StringBuilder();
            int overflow = SeoConstant.MAX_IMAGES_PER_ARTICLE + 3;
            for (int i = 0; i < overflow; i++) {
                html.append("<img src=\"https://cdn.example.com/").append(i).append(".png\">");
            }
            List<String> notes = new ArrayList<>();

            String result = replaceBodyImages(html.toString(), notes);

            verify(seoImageFetcher, times(SeoConstant.MAX_IMAGES_PER_ARTICLE)).fetchAndUpload(anyString());
            assertTrue(result.contains("https://cdn.example.com/" + (overflow - 1) + ".png"),
                    "超限的应原样保留");
            assertTrue(String.join(" ", notes).contains("超出单篇"), notes.toString());
        }

        @Test
        @DisplayName("单张抓取失败只记一条提示，其余图片照常转存")
        void replaceBodyImages_oneFailure_othersStillUploaded() {
            when(seoImageFetcher.fetchAndUpload("https://cdn.example.com/bad.png"))
                    .thenThrow(new SeoImageFetcher.ImageFetchException("图片地址不可用"));
            String html = "<img src=\"https://cdn.example.com/bad.png\">"
                    + "<img src=\"https://cdn.example.com/good.png\">";
            List<String> notes = new ArrayList<>();

            String result = replaceBodyImages(html, notes);

            assertTrue(result.contains("https://cdn.example.com/bad.png"), "失败的应保留原链接");
            assertFalse(result.contains("https://cdn.example.com/good.png"), "成功的应被替换");
            assertTrue(String.join(" ", notes).contains("1 张失败"), notes.toString());
        }

        @Test
        @DisplayName("URL 里的 $ 与 \\ 不会破坏正则替换")
        void replaceBodyImages_urlWithRegexSpecialChars_replacedSafely() {
            String url = "https://cdn.example.com/a$1b.png";
            when(seoImageFetcher.fetchAndUpload(url)).thenReturn("https://img.example.com/x$2y.png");

            String result = replaceBodyImages("<img src=\"" + url + "\">", new ArrayList<>());

            assertTrue(result.contains("https://img.example.com/x$2y.png"), result);
        }

        @Test
        @DisplayName("失败提示里的原始 URL 被截断——它完全由文件内容控制且长度不受限")
        void replaceBodyImages_veryLongUrl_truncatedInNotes() {
            String longUrl = "https://cdn.example.com/" + "a".repeat(5000) + ".png";
            when(seoImageFetcher.fetchAndUpload(anyString()))
                    .thenThrow(new SeoImageFetcher.ImageFetchException("图片地址不可用"));
            List<String> notes = new ArrayList<>();

            replaceBodyImages("<img src=\"" + longUrl + "\">", notes);

            assertTrue(notes.get(0).length() < 500,
                    "URL 未截断，会被逐篇累积进 result_json 并整体回给前端。长度：" + notes.get(0).length());
        }
    }

    @Nested
    @DisplayName("单篇导入的失败原因")
    class FailureReasons {

        @Test
        @DisplayName("提交阶段被拒的文件（非 .md / 超限 / 非 UTF-8）产出逐篇失败，不否决整批")
        void importOne_rejectedAtSubmit_producesFailItem() {
            SeoImportItemVo item = importOne(
                    SeoImportFileBo.rejected("a.zip", "仅支持 .md 文件，不支持压缩包等其他格式"), false);

            assertFalse(item.getSuccess());
            assertEquals("a.zip", item.getFile());
            assertTrue(notesOf(item).contains("仅支持 .md"), notesOf(item));
        }

        @Test
        @DisplayName("缺 front matter：提示该写什么，而不是只说「导入失败」")
        void importOne_withoutFrontMatter_reportsSpecificReason() {
            SeoImportItemVo item = importOne(SeoImportFileBo.of("a.md", "## 只有正文"), false);

            assertFalse(item.getSuccess());
            assertTrue(notesOf(item).contains("front matter"), notesOf(item));
        }

        @Test
        @DisplayName("缺 title / category / cover 各自点名是哪个字段")
        void importOne_missingRequiredField_namesTheField() {
            SeoImportItemVo noTitle = importOne(SeoImportFileBo.of("a.md",
                    "---\ncategory: 直播运营\ncover: https://cdn.example.com/a.jpg\n---\n\n正文\n"), false);
            SeoImportItemVo noCategory = importOne(SeoImportFileBo.of("b.md",
                    "---\ntitle: 标题\ncover: https://cdn.example.com/a.jpg\n---\n\n正文\n"), false);
            SeoImportItemVo noCover = importOne(SeoImportFileBo.of("c.md",
                    "---\ntitle: 标题\ncategory: 直播运营\n---\n\n正文\n"), false);

            assertTrue(notesOf(noTitle).contains("title"), notesOf(noTitle));
            assertTrue(notesOf(noCategory).contains("category"), notesOf(noCategory));
            assertTrue(notesOf(noCover).contains("cover"), notesOf(noCover));
        }

        @Test
        @DisplayName("分类不存在且未勾选自动创建：提示去哪里创建、可以勾什么")
        void importOne_categoryMissing_tellsHowToFix() {
            when(seoCategoryService.findIdByName("投放优化")).thenReturn(null);
            String md = VALID_MD.replace("category: 直播运营", "category: 投放优化");

            SeoImportItemVo item = importOne(SeoImportFileBo.of("a.md", md), false);

            assertFalse(item.getSuccess());
            assertTrue(notesOf(item).contains("投放优化"), notesOf(item));
            assertTrue(notesOf(item).contains("自动创建"), notesOf(item));
            verify(seoCategoryService, never()).findOrCreateByName(anyString());
        }

        @Test
        @DisplayName("勾选自动创建时新建分类，并在结果里点名——运营需知道系统里多了什么")
        void importOne_autoCreateCategory_createsAndReports() {
            when(seoCategoryService.findIdByName("投放优化")).thenReturn(null);
            when(seoCategoryService.findOrCreateByName("投放优化")).thenReturn(300L);
            String md = VALID_MD.replace("category: 直播运营", "category: 投放优化");

            SeoImportItemVo item = importOne(SeoImportFileBo.of("a.md", md), true);

            assertTrue(item.getSuccess(), notesOf(item));
            assertTrue(notesOf(item).contains("已自动创建分类"), notesOf(item));
        }

        @Test
        @DisplayName("封面抓取失败让整篇失败——封面必填，且是列表页与分享卡片的唯一图源")
        void importOne_coverFetchFailed_failsWholeArticle() {
            when(seoImageFetcher.fetchAndUpload("https://cdn.example.com/cover.jpg"))
                    .thenThrow(new SeoImageFetcher.ImageFetchException("图片地址不可用"));

            SeoImportItemVo item = importOne(SeoImportFileBo.of("a.md", VALID_MD), false);

            assertFalse(item.getSuccess());
            assertTrue(notesOf(item).contains("封面下载失败"), notesOf(item));
            verify(seoArticleService, never()).saveArticle(any());
        }

        @Test
        @DisplayName("标题已存在时跳过，避免同一批 md 被重复导入两遍")
        void importOne_duplicateTitle_skipped() {
            doReturn(1L).when(seoArticleService).count(any(Wrapper.class));

            SeoImportItemVo item = importOne(SeoImportFileBo.of("a.md", VALID_MD), false);

            assertFalse(item.getSuccess());
            assertTrue(notesOf(item).contains("已存在同名文章"), notesOf(item));
            verify(seoArticleService, never()).saveArticle(any());
        }

        @Test
        @DisplayName("正文为空时失败，并说明是 front matter 之后没内容")
        void importOne_emptyBody_reportsEmptyContent() {
            String md = """
                    ---
                    title: 标题
                    category: 直播运营
                    cover: https://cdn.example.com/cover.jpg
                    ---
                    """;

            SeoImportItemVo item = importOne(SeoImportFileBo.of("a.md", md), false);

            assertFalse(item.getSuccess());
            assertTrue(notesOf(item).contains("正文为空"), notesOf(item));
        }

        @Test
        @DisplayName("saveArticle 的业务校验失败原样透出，并带上此前已产生的提示")
        void importOne_saveArticleRejected_keepsReasonAndEarlierNotes() {
            when(seoCategoryService.findIdByName("投放优化")).thenReturn(null);
            when(seoCategoryService.findOrCreateByName("投放优化")).thenReturn(300L);
            when(seoArticleService.saveArticle(any()))
                    .thenThrow(new BusinessException("文章标题不能超过 100 字，当前 120 字"));
            String md = VALID_MD.replace("category: 直播运营", "category: 投放优化");

            SeoImportItemVo item = importOne(SeoImportFileBo.of("a.md", md), true);

            assertFalse(item.getSuccess());
            assertTrue(notesOf(item).contains("不能超过 100 字"), notesOf(item));
            // 分类是在独立事务里提交的，这篇失败它也留下了，必须告知
            assertTrue(notesOf(item).contains("已自动创建分类"),
                    "分类已被创建却没告诉运营。实际：" + notesOf(item));
        }
    }

    @Nested
    @DisplayName("字段处理")
    class FieldHandling {

        @Test
        @DisplayName("导入一律落「已下架」，front matter 里的 status 被忽略")
        void importOne_alwaysDisabled_ignoringFrontMatterStatus() {
            String md = VALID_MD.replace("seoDescription: 一套可复用的直播复盘方法论",
                    "seoDescription: 一套可复用的直播复盘方法论\nstatus: 1\npublishTime: 2026-01-01");

            SeoImportItemVo item = importOne(SeoImportFileBo.of("a.md", md), false);

            assertTrue(item.getSuccess(), notesOf(item));
            assertEquals(SeoConstant.STATUS_DISABLED, captureSavedBo().getArticleStatus(),
                    "AI 产出的内容必须经人工过目才能进公开站点，"
                            + "否则一份带 status: 1 的模板文件就能直接发布");
        }

        @Test
        @DisplayName("未填 seoDescription 时取 summary，并说明来源")
        void importOne_missingSeoDescription_fallsBackToSummary() {
            String withSummary = VALID_MD.replace(
                    "seoDescription: 一套可复用的直播复盘方法论", "summary: 这是摘要内容");

            SeoImportItemVo item = importOne(SeoImportFileBo.of("a.md", withSummary), false);

            assertTrue(item.getSuccess(), notesOf(item));
            assertEquals("这是摘要内容", captureSavedBo().getSeoDescription());
            assertTrue(notesOf(item).contains("已取 summary"), notesOf(item));
        }

        @Test
        @DisplayName("既无 seoDescription 也无 summary 时截取正文，且不带标签与实体")
        void importOne_noSummaryEither_usesPlainBodyText() {
            String md = """
                    ---
                    title: 标题
                    category: 直播运营
                    cover: https://cdn.example.com/cover.jpg
                    ---

                    ## 小标题

                    复盘要看 GMV & 转化率。
                    """;

            SeoImportItemVo item = importOne(SeoImportFileBo.of("a.md", md), false);

            assertTrue(item.getSuccess(), notesOf(item));
            String description = captureSavedBo().getSeoDescription();
            assertFalse(description.contains("<"), "SEO 描述里混进了标签：" + description);
            assertFalse(description.contains("&amp;"),
                    "实体未解码，搜索结果里会显示成 &amp;。实际：" + description);
            assertTrue(description.contains("GMV & 转化率"), description);
        }

        @Test
        @DisplayName("标签超过 10 个时截断并列出未使用的，不让整篇失败")
        void importOne_tooManyTags_truncatedWithNote() {
            StringBuilder tags = new StringBuilder("tags: [");
            int overflow = SeoConstant.MAX_TAGS_PER_ARTICLE + 2;
            for (int i = 0; i < overflow; i++) {
                tags.append("标签").append(i).append(i == overflow - 1 ? "]" : ", ");
            }
            String md = VALID_MD.replace("tags: [直播复盘, 数据分析]", tags.toString());

            SeoImportItemVo item = importOne(SeoImportFileBo.of("a.md", md), false);

            assertTrue(item.getSuccess(), notesOf(item));
            assertTrue(notesOf(item).contains("标签超过"), notesOf(item));
            assertTrue(notesOf(item).contains("标签" + (overflow - 1)),
                    "未使用的标签应当点名。实际：" + notesOf(item));
        }

        @Test
        @DisplayName("新建的标签在结果里点名，已存在的不报")
        void importOne_newTags_reportedOnlyWhenCreated() {
            when(seoTagService.findIdByName("直播复盘")).thenReturn(200L);
            when(seoTagService.findIdByName("数据分析")).thenReturn(null);
            when(seoTagService.findOrCreateByName("数据分析")).thenReturn(201L);

            SeoImportItemVo item = importOne(SeoImportFileBo.of("a.md", VALID_MD), false);

            assertTrue(item.getSuccess(), notesOf(item));
            assertTrue(notesOf(item).contains("新建标签：数据分析"), notesOf(item));
            assertFalse(notesOf(item).contains("新建标签：直播复盘"),
                    "已存在的标签被误报成新建。实际：" + notesOf(item));
        }

        @Test
        @DisplayName("解析到同一 ID 的标签会去重——重复 ID 会撞关联表的联合唯一索引")
        void importOne_duplicateTagIds_deduplicated() {
            when(seoTagService.findOrCreateByName(anyString())).thenReturn(200L);
            String md = VALID_MD.replace("tags: [直播复盘, 数据分析]", "tags: [标签甲, 标签乙]");

            importOne(SeoImportFileBo.of("a.md", md), false);

            assertEquals(1, captureSavedBo().getTagIds().size(),
                    "两个不同名的标签解析到同一 ID 时必须去重");
        }

        @Test
        @DisplayName("slug 被追加随机数时告知运营——这是他们唯一一次得知最终 URL 的机会")
        void importOne_slugAppended_reported() {
            SeoSaveResultVo saved = new SeoSaveResultVo();
            saved.setId(1L);
            saved.setSlug("zhibo-fupan-8412");
            saved.setSlugAppended(true);
            when(seoArticleService.saveArticle(any())).thenReturn(saved);

            SeoImportItemVo item = importOne(SeoImportFileBo.of("a.md", VALID_MD), false);

            assertTrue(notesOf(item).contains("zhibo-fupan-8412"), notesOf(item));
        }

        @Test
        @DisplayName("正文入库前必经 XSS 过滤，且过滤发生在图片抓取之前")
        void importOne_scriptInBody_sanitizedBeforeFetching() {
            String md = """
                    ---
                    title: 标题
                    category: 直播运营
                    cover: https://cdn.example.com/cover.jpg
                    seoDescription: 描述
                    ---

                    正文开始

                    <!-- <img src="http://10.0.0.5/probe.png"> -->

                    <script>alert(1)</script>
                    """;

            SeoImportItemVo item = importOne(SeoImportFileBo.of("a.md", md), false);

            assertTrue(item.getSuccess(), notesOf(item));
            assertFalse(captureSavedBo().getContent().contains("<script>"), captureSavedBo().getContent());
            // 注释里的 img 会被 sanitizer 剥掉，因此不该被抓取。顺序反了的话，
            // 一份预览起来完全干净的 md 可以静默发起几十次内网请求
            verify(seoImageFetcher, never()).fetchAndUpload("http://10.0.0.5/probe.png");
        }

        @Test
        @DisplayName("front matter 里的 slug 传给保存层，由它决定是否追加随机数")
        void importOne_declaredSlug_passedThrough() {
            String md = VALID_MD.replace("title: 直播复盘怎么做",
                    "title: 直播复盘怎么做\nslug: my-custom-slug");

            importOne(SeoImportFileBo.of("a.md", md), false);

            assertEquals("my-custom-slug", captureSavedBo().getSlug());
        }

        @Test
        @DisplayName("seoKeywords 列表被拼成逗号分隔字符串")
        void importOne_seoKeywordsList_joinedWithComma() {
            String md = VALID_MD.replace("tags: [直播复盘, 数据分析]",
                    "tags: [直播复盘]\nseoKeywords: [直播复盘, 复盘方法]");

            importOne(SeoImportFileBo.of("a.md", md), false);

            assertEquals("直播复盘,复盘方法", captureSavedBo().getSeoKeywords());
        }

        @Test
        @DisplayName("封面写入的是转存后的图床地址，不是原始外链")
        void importOne_cover_replacedWithHostedUrl() {
            when(seoImageFetcher.fetchAndUpload("https://cdn.example.com/cover.jpg"))
                    .thenReturn("https://img.example.com/img/seo/abc.jpg");

            importOne(SeoImportFileBo.of("a.md", VALID_MD), false);

            assertEquals("https://img.example.com/img/seo/abc.jpg", captureSavedBo().getCoverUrl());
        }
    }

    @Nested
    @DisplayName("提交入口")
    class Submit {

        @Test
        @DisplayName("空文件列表被拒，且不建任务行")
        void submit_emptyList_rejectedWithoutTaskRow() {
            assertThrows(BusinessException.class, () -> service.submit(List.of(), false));

            verify(service, never()).save(any());
        }

        @Test
        @DisplayName("超过 50 个文件时提示当前数量，便于运营知道要分几批")
        void submit_tooManyFiles_reportsCount() {
            List<SeoImportFileBo> tooMany = new ArrayList<>();
            for (int i = 0; i <= SeoConstant.MAX_IMPORT_FILES; i++) {
                tooMany.add(SeoImportFileBo.of(i + ".md", VALID_MD));
            }

            BusinessException e = assertThrows(BusinessException.class,
                    () -> service.submit(tooMany, false));

            assertTrue(e.getMessage().contains(String.valueOf(SeoConstant.MAX_IMPORT_FILES)), e.getMessage());
            assertTrue(e.getMessage().contains(String.valueOf(tooMany.size())), e.getMessage());
            verify(service, never()).save(any());
        }
    }
}
