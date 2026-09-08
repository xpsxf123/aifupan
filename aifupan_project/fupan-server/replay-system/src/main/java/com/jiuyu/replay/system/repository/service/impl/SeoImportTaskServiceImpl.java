package com.jiuyu.replay.system.repository.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.replay.common.context.RequestContext;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.system.bo.SeoArticleBo;
import com.jiuyu.replay.system.bo.SeoImportFileBo;
import com.jiuyu.replay.system.config.SeoImportExecutor;
import com.jiuyu.replay.system.constant.SeoConstant;
import com.jiuyu.replay.system.entity.SeoArticleEntity;
import com.jiuyu.replay.system.entity.SeoImportTaskEntity;
import com.jiuyu.replay.system.repository.dao.SeoImportTaskDao;
import com.jiuyu.replay.system.repository.service.SeoArticleService;
import com.jiuyu.replay.system.repository.service.SeoCategoryService;
import com.jiuyu.replay.system.repository.service.SeoImportTaskService;
import com.jiuyu.replay.system.repository.service.SeoTagService;
import com.jiuyu.replay.system.util.SeoImageFetcher;
import com.jiuyu.replay.system.util.SeoHtmlSanitizer;
import com.jiuyu.replay.system.util.SeoMarkdownParser;
import com.jiuyu.replay.system.vo.SeoImportItemVo;
import com.jiuyu.replay.system.vo.SeoImportProgressVo;
import com.jiuyu.replay.system.vo.SeoImportTaskVo;
import com.jiuyu.replay.system.vo.SeoSaveResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SEO 文章批量导入实现。
 *
 * <p>结构：{@link #submit} 只落任务行并把执行体交给线程池，{@link #runImport} 在后台逐篇处理。
 * <b>逐篇独立事务</b>——每篇通过 {@code seoArticleService.saveArticle}（自身 @Transactional）
 * 提交，一篇失败不影响其他篇。整批回滚是错的：批量导入的常态就是部分成功，
 * 为一个拼错的分类名把 49 篇成功的也丢掉，运营会被迫全部重来。
 *
 * @author claude
 * @date 2026-08-13
 */
@Service
public class SeoImportTaskServiceImpl extends ServiceImpl<SeoImportTaskDao, SeoImportTaskEntity>
        implements SeoImportTaskService {

    private static final Logger log = LoggerFactory.getLogger(SeoImportTaskServiceImpl.class);

    /** 抽取正文里的 img src。宽松匹配即可——真正的安全过滤在 SeoHtmlSanitizer */
    private static final Pattern IMG_SRC = Pattern.compile(
            "(<img\\b[^>]*?\\bsrc\\s*=\\s*)([\"'])(.*?)\\2", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    /** 从 HTML 里剥标签，用于 SEO 描述兜底 */
    private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");

    /** SEO 描述兜底截取长度，与 SeoArticleServiceImpl 的 MAX_SEO_DESC_LEN 一致 */
    private static final int SEO_DESC_FALLBACK_LEN = 160;

    /** 任务级失败原因字段是 varchar(500) */
    private static final int MAX_ERROR_MSG_LEN = 500;

    /** result_json 总长度上限。LONGTEXT 本身无约束，但它要整体回给前端 */
    private static final int MAX_RESULT_JSON_LEN = 256 * 1024;

    /** notes 里回显的图片 URL 截断长度 */
    private static final int MAX_NOTE_URL_LEN = 200;

    private final SeoArticleService seoArticleService;

    private final SeoCategoryService seoCategoryService;

    private final SeoTagService seoTagService;

    private final SeoImageFetcher seoImageFetcher;

    public SeoImportTaskServiceImpl(SeoArticleService seoArticleService,
                                    SeoCategoryService seoCategoryService,
                                    SeoTagService seoTagService,
                                    SeoImageFetcher seoImageFetcher) {
        this.seoArticleService = seoArticleService;
        this.seoCategoryService = seoCategoryService;
        this.seoTagService = seoTagService;
        this.seoImageFetcher = seoImageFetcher;
    }

    /**
     * {@inheritDoc}
     *
     * <p><b>刻意不加 {@code @Transactional}</b>（CLAUDE.md §5 要求写方法加事务，此处是有意的例外）：
     * 任务行必须在提交线程池之前<b>真正提交</b>。包在事务里的话，后台线程会去 update 一行
     * 尚未可见的记录，进度永远停在 0，{@link #progress} 还会抛「任务不存在」。
     * 本方法只有一次 insert，没有需要原子性的多步写入。
     */
    @Override
    public SeoImportTaskVo submit(List<SeoImportFileBo> files, boolean autoCreateCategory) {
        if (files == null || files.isEmpty()) {
            throw new BusinessException("请选择要导入的 .md 文件");
        }
        if (files.size() > SeoConstant.MAX_IMPORT_FILES) {
            throw new BusinessException("单次最多导入 " + SeoConstant.MAX_IMPORT_FILES
                    + " 篇，当前 " + files.size() + " 篇");
        }

        String taskId = UUID.randomUUID().toString().replace("-", "");
        SeoImportTaskEntity task = new SeoImportTaskEntity();
        task.setId(SnowflakeManager.nextValue());
        task.setTaskId(taskId);
        task.setTaskStatus(SeoConstant.IMPORT_STATUS_RUNNING);
        task.setTotal(files.size());
        task.setProcessed(0);
        task.setSuccessCount(0);
        task.setFailCount(0);
        task.setResultJson("[]");
        task.setErrorMsg("");
        // 取当前登录用户。RequestContext 在 replay-common（本模块的依赖链内），
        // 由 LoginInterceptor → UserTokenProducerImpl.updateTokenExpire 在请求线程里填好。
        // 注意本方法必须在 HTTP 线程执行——ThreadLocal 不会传递到后台导入线程
        Long currentUserId = RequestContext.getUserId();
        task.setCreateUserId(currentUserId == null ? 0L : currentUserId);
        task.setCreateDate(LocalDateTime.now());
        // 任务行必须先落库再提交线程池：反过来的话后台线程可能先跑完并去 update
        // 一行还不存在的记录，进度永远停在 0
        save(task);

        List<SeoImportFileBo> snapshot = List.copyOf(files);
        try {
            SeoImportExecutor.submit(() -> runImport(taskId, snapshot, autoCreateCategory));
        } catch (RejectedExecutionException e) {
            markTaskFailed(taskId, "导入队列已满，请等待正在进行的导入完成后重试");
            throw new BusinessException("导入队列已满，请等待正在进行的导入完成后重试");
        }

        SeoImportTaskVo vo = new SeoImportTaskVo();
        vo.setTaskId(taskId);
        vo.setTotal(files.size());
        return vo;
    }

    @Override
    public SeoImportProgressVo progress(String taskId) {
        if (taskId == null || taskId.isBlank()) {
            throw new BusinessException("任务 ID 不能为空");
        }
        SeoImportTaskEntity task = getOne(new LambdaQueryWrapper<SeoImportTaskEntity>()
                .eq(SeoImportTaskEntity::getTaskId, taskId)
                .last("limit 1"));
        if (task == null) {
            throw new BusinessException("导入任务不存在或已过期");
        }
        SeoImportProgressVo vo = new SeoImportProgressVo();
        vo.setTaskId(task.getTaskId());
        vo.setTaskStatus(task.getTaskStatus());
        // 僵死兜底：线程池是静态守护线程池，发版重启会让在跑和排队的任务无声消失，
        // 对应的行永远停在「处理中」。查询时按创建时间判超期，比引入调度扫表便宜
        if (Objects.equals(task.getTaskStatus(), SeoConstant.IMPORT_STATUS_RUNNING) && isStale(task)) {
            vo.setTaskStatus(SeoConstant.IMPORT_STATUS_FAILED);
            vo.setErrorMsg("导入任务超时或服务已重启，请重新导入");
            vo.setResults(parseResults(task.getResultJson()));
            vo.setTotal(task.getTotal());
            vo.setProcessed(task.getProcessed());
            vo.setSuccessCount(task.getSuccessCount());
            vo.setFailCount(task.getFailCount());
            return vo;
        }
        vo.setTotal(task.getTotal());
        vo.setProcessed(task.getProcessed());
        vo.setSuccessCount(task.getSuccessCount());
        vo.setFailCount(task.getFailCount());
        vo.setErrorMsg(task.getErrorMsg());
        vo.setResults(parseResults(task.getResultJson()));
        return vo;
    }

    /**
     * 后台执行体。
     *
     * <p>整个方法不加事务：它跨越 N 篇文章、可能跑几十秒，包在一个事务里会长时间占住连接，
     * 而且任何一篇失败都会连累其余。事务边界在每篇的 {@code saveArticle} 上。
     */
    private void runImport(String taskId, List<SeoImportFileBo> files, boolean autoCreateCategory) {
        List<SeoImportItemVo> results = new ArrayList<>(files.size());
        long deadline = System.currentTimeMillis() + SeoConstant.MAX_IMPORT_DURATION_MS;
        int successCount = 0;
        int failCount = 0;
        try {
            for (SeoImportFileBo file : files) {
                SeoImportItemVo item;
                if (System.currentTimeMillis() > deadline) {
                    // 剩余文件全部标为超时。不这么做的话，一批慢速图片能让任务跑几十小时，
                    // 前端就一直转圈——运营宁可看到「这几篇没处理」也不要无限等待
                    item = failItem(file.fileName(), null, "导入任务已超过 "
                            + (SeoConstant.MAX_IMPORT_DURATION_MS / 60000) + " 分钟上限，该文件未处理，请重新导入");
                } else {
                    try {
                        item = importOne(file, autoCreateCategory);
                    } catch (Exception e) {
                        // 兜底：任何未预料的异常只能让这一篇失败，不能中断整批
                        log.warn("[SEO导入] 单篇导入失败 taskId={} file={}", taskId, file.fileName(), e);
                        item = failItem(file.fileName(), null, describeUnexpected(e));
                    }
                }
                results.add(item);
                if (Boolean.TRUE.equals(item.getSuccess())) {
                    successCount++;
                } else {
                    failCount++;
                }
                updateProgress(taskId, results, successCount, failCount, SeoConstant.IMPORT_STATUS_RUNNING);
            }
            updateProgress(taskId, results, successCount, failCount, SeoConstant.IMPORT_STATUS_FINISHED);
        } catch (Throwable t) {
            // 必须 catch Throwable 而不是 Exception：OutOfMemoryError 逃逸的话
            // 任务行永远停在「处理中」，前端无限轮询。而这个功能一次要在堆里
            // 放几十 MB 文件内容，OOM 不是理论情况
            log.error("[SEO导入] 任务执行异常 taskId={}", taskId, t);
            try {
                markTaskFailed(taskId, "导入任务执行异常：" + t.getClass().getSimpleName());
            } catch (Exception ignored) {
                // 连写库都失败时无能为力，交给 progress() 的超期兜底
                log.error("[SEO导入] 标记任务失败也失败了 taskId={}", taskId);
            }
            if (t instanceof Error error) {
                throw error;
            }
        }
    }

    /**
     * 导入单篇。
     *
     * <p>返回值一律是 {@link SeoImportItemVo}，失败也不抛异常——调用方需要「这一篇失败了、
     * 原因是什么」而不是一个中断整批的异常。
     */
    private SeoImportItemVo importOne(SeoImportFileBo file, boolean autoCreateCategory) {
        String fileName = file.fileName();
        List<String> notes = new ArrayList<>();

        // 提交阶段就判定失败的文件（扩展名不对、超限、编码不是 UTF-8）在这里产出逐篇结果，
        // 而不是在 Controller 里让整批失败
        if (file.rejectReason() != null) {
            return failItem(fileName, null, file.rejectReason());
        }

        SeoMarkdownParser.ParsedMarkdown parsed;
        try {
            parsed = SeoMarkdownParser.parse(file.content());
        } catch (SeoMarkdownParser.MarkdownParseException e) {
            return failItem(fileName, null, e.getMessage());
        }
        Map<String, Object> frontMatter = parsed.frontMatter();

        String title = SeoMarkdownParser.getString(frontMatter, "title").orElse(null);
        if (title == null) {
            return failItem(fileName, null, "front matter 缺少必填字段 title");
        }
        if (existsByTitle(title)) {
            return failItem(fileName, title, "系统中已存在同名文章「" + title + "」，未重复导入");
        }

        String categoryName = SeoMarkdownParser.getString(frontMatter, "category").orElse(null);
        if (categoryName == null) {
            return failItem(fileName, title, "front matter 缺少必填字段 category");
        }
        Long categoryId = seoCategoryService.findIdByName(categoryName);
        if (categoryId == null && autoCreateCategory) {
            categoryId = seoCategoryService.findOrCreateByName(categoryName);
            // 先查后建才能分辨「本次新建」与「本来就有」——直接调 findOrCreateByName
            // 拿不到这个区别，而运营需要知道系统里多了个分类
            if (categoryId != null) {
                notes.add("已自动创建分类「" + categoryName + "」");
            }
        }
        if (categoryId == null) {
            return failItem(fileName, title, "分类「" + categoryName
                    + "」不存在，请先在分类管理中创建，或勾选「分类不存在时自动创建」");
        }

        String cover = SeoMarkdownParser.getString(frontMatter, "cover").orElse(null);
        if (cover == null) {
            return failItem(fileName, title, "front matter 缺少必填字段 cover");
        }
        String coverUrl;
        try {
            coverUrl = seoImageFetcher.fetchAndUpload(cover);
        } catch (SeoImageFetcher.ImageFetchException e) {
            // 封面必填且是列表页与分享卡片的唯一图源，抓不到只能让这一篇失败
            return failItem(fileName, title, "封面下载失败：" + e.getMessage());
        }

        String bodyHtml = parsed.bodyHtml();
        if (bodyHtml == null || bodyHtml.isBlank()) {
            return failItem(fileName, title, "正文为空，front matter 之后没有任何内容");
        }
        // 先过滤再抓图，顺序不能反：在未过滤的 HTML 上抓图意味着写在 HTML 注释或
        // <script> 块里的 <img> 也会被真实请求——它们随后会被 sanitizer 剥掉，
        // 于是一份预览起来完全干净的 md 可以静默发起几十次内网请求。
        // 只有最终真会展示的图片才值得抓
        bodyHtml = SeoHtmlSanitizer.sanitize(bodyHtml);
        if (bodyHtml.isBlank()) {
            return failItem(fileName, title, "正文经安全过滤后为空，请检查是否只写了脚本或样式");
        }
        bodyHtml = replaceBodyImages(bodyHtml, notes);

        SeoArticleBo bo = new SeoArticleBo();
        bo.setTitle(title);
        bo.setCategoryId(categoryId);
        bo.setContent(bodyHtml);
        bo.setCoverUrl(coverUrl);
        bo.setSlug(SeoMarkdownParser.getString(frontMatter, "slug").orElse(null));
        bo.setSummary(SeoMarkdownParser.getString(frontMatter, "summary").orElse(""));
        bo.setSeoTitle(SeoMarkdownParser.getString(frontMatter, "seoTitle").orElse(null));
        bo.setSeoDescription(resolveSeoDescription(frontMatter, bodyHtml, notes));
        bo.setSeoKeywords(String.join(",", SeoMarkdownParser.getStringList(frontMatter, "seoKeywords")));
        // 导入一律落「已下架」：AI 产出的内容必须经人工过目才能进公开站点。
        // front matter 里写的 status / publishTime 一律忽略——否则一份模板文件就能直接发布
        bo.setArticleStatus(SeoConstant.STATUS_DISABLED);
        bo.setTagIds(resolveTagIds(frontMatter, notes));

        SeoSaveResultVo saved;
        try {
            saved = seoArticleService.saveArticle(bo);
        } catch (BusinessException e) {
            // saveArticle 的校验失败（标题超长、SEO 描述超长等），消息本身就是给运营看的。
            // notes 要一起带上——分类/标签是在各自独立事务里提交的，这篇失败它们也留下了，
            // 不告知的话运营根本不知道系统里多了个分类
            return failItem(fileName, title, e.getMessage(), notes);
        }
        if (Boolean.TRUE.equals(saved.getSlugAppended())) {
            notes.add("别名已存在，实际使用「" + saved.getSlug() + "」");
        }

        SeoImportItemVo item = new SeoImportItemVo();
        item.setFile(fileName);
        item.setTitle(title);
        item.setSuccess(true);
        item.setNotes(notes);
        return item;
    }

    /**
     * 正文图片转存。
     *
     * <p>失败不让整篇失败——正文可能有十几张图，一张挂了就整篇作废，运营要为一张配图重导全部。
     * 保留原始外链 + 在结果里点名，运营可以事后单独处理。
     */
    private String replaceBodyImages(String html, List<String> notes) {
        Matcher matcher = IMG_SRC.matcher(html);
        StringBuilder rewritten = new StringBuilder();
        // 同一张图在正文里出现多次时只抓一次，避免重复下载并在图床存多份
        Map<String, String> fetched = new HashMap<>();
        int total = 0;
        int uploaded = 0;
        int failed = 0;
        int skipped = 0;
        while (matcher.find()) {
            total++;
            // 必须先做 HTML 实体解码：commonmark 渲染 src 时会转义 &，
            // 于是 ?w=100&h=200&sign=abc 在属性里是 ?w=100&amp;h=200&amp;sign=abc。
            // 直接拿去请求会打到错误 URL，签名图床一律 403——功能不报错，只是静默退化成
            // 「保留原链接」，而外链会烂掉正是这个转存功能存在的理由
            String original = unescapeHtml(matcher.group(3));
            String replacement = original;
            if (total > SeoConstant.MAX_IMAGES_PER_ARTICLE) {
                skipped++;
            } else if (fetched.containsKey(original)) {
                replacement = fetched.get(original);
            } else {
                try {
                    replacement = seoImageFetcher.fetchAndUpload(original);
                    fetched.put(original, replacement);
                    if (!replacement.equals(original)) {
                        uploaded++;
                    }
                } catch (SeoImageFetcher.ImageFetchException e) {
                    failed++;
                    fetched.put(original, original);
                    // URL 要截断：它完全由文件内容控制、长度不受限，直接拼进 notes
                    // 会被逐篇累积进 result_json 并整体回给前端
                    notes.add("正文图片抓取失败，已保留原链接：" + truncate(original, MAX_NOTE_URL_LEN)
                            + "（" + e.getMessage() + "）");
                }
            }
            // 写回同样要转义：保留原链接的分支若把解码后的 & 直接写进属性，
            // 会把本来合法的 HTML 变成未转义的，属于新引入的问题。
            // quoteReplacement 则是另一回事——它防的是 URL 里的 $ 和 \ 在替换串里被当作特殊字符
            matcher.appendReplacement(rewritten, Matcher.quoteReplacement(
                    matcher.group(1) + matcher.group(2) + escapeHtmlAttr(replacement) + matcher.group(2)));
        }
        matcher.appendTail(rewritten);

        if (total > 0) {
            notes.add("正文 " + total + " 张图片，" + uploaded + " 张已转存图床"
                    + (failed > 0 ? "，" + failed + " 张失败" : "")
                    + (skipped > 0 ? "，" + skipped + " 张超出单篇 " + SeoConstant.MAX_IMAGES_PER_ARTICLE
                            + " 张上限未处理" : ""));
        }
        return rewritten.toString();
    }

    /**
     * 标签解析：按名称匹配，匹配不到自动创建；超过上限截断。
     *
     * <p>截断而非报错——标签是弱关联，为多写两个标签让整篇失败不划算，提示到位即可。
     */
    private List<Long> resolveTagIds(Map<String, Object> frontMatter, List<String> notes) {
        List<String> names = SeoMarkdownParser.getStringList(frontMatter, "tags");
        if (names.isEmpty()) {
            return List.of();
        }
        List<String> effective = names;
        if (names.size() > SeoConstant.MAX_TAGS_PER_ARTICLE) {
            effective = names.subList(0, SeoConstant.MAX_TAGS_PER_ARTICLE);
            notes.add("标签超过 " + SeoConstant.MAX_TAGS_PER_ARTICLE + " 个上限，已保留前 "
                    + SeoConstant.MAX_TAGS_PER_ARTICLE + " 个，未使用：" + String.join("、",
                    names.subList(SeoConstant.MAX_TAGS_PER_ARTICLE, names.size())));
        }
        // LinkedHashSet 去重：findOrCreateByName 对同名标签返回同一 ID，
        // 重复 ID 会撞文章-标签关联表的联合唯一索引
        Set<Long> tagIds = new LinkedHashSet<>();
        List<String> created = new ArrayList<>();
        for (String name : effective) {
            // 先查后建：区分「本次新建」与「本来就有」。
            // 不能用 listEnabled() 去比对——它查不到已禁用的同名标签，
            // 会把复用已禁用标签误报成新建，而且每个标签全表查一次
            boolean existed = seoTagService.findIdByName(name) != null;
            Long tagId = seoTagService.findOrCreateByName(name);
            if (tagId != null) {
                tagIds.add(tagId);
                if (!existed) {
                    created.add(name);
                }
            }
        }
        if (!created.isEmpty()) {
            notes.add("新建标签：" + String.join("、", created));
        }
        return new ArrayList<>(tagIds);
    }

    /**
     * SEO 描述兜底。
     *
     * <p>{@code saveArticle} 要求 seoDescription 非空，而 front matter 里它是选填的——
     * 不兜底的话，一份没写 seoDescription 的合法模板会导入失败，而运营看到的原因是
     * 「SEO 描述不能为空」，完全不知道该改哪里。依次取 seoDescription → summary → 正文纯文本。
     */
    private String resolveSeoDescription(Map<String, Object> frontMatter, String bodyHtml, List<String> notes) {
        Optional<String> declared = SeoMarkdownParser.getString(frontMatter, "seoDescription");
        if (declared.isPresent()) {
            return truncate(declared.get(), SEO_DESC_FALLBACK_LEN);
        }
        Optional<String> summary = SeoMarkdownParser.getString(frontMatter, "summary");
        if (summary.isPresent()) {
            notes.add("未填写 seoDescription，已取 summary 作为 SEO 描述");
            return truncate(summary.get(), SEO_DESC_FALLBACK_LEN);
        }
        // 剥标签后还要解实体：否则正文里的 &amp; / &quot; 会原样进 seo_description，
        // 最终在搜索结果里显示成 &amp;。传进来的 bodyHtml 已过 sanitize，标签结构是规范的
        String plain = unescapeHtml(HTML_TAG.matcher(bodyHtml).replaceAll(" "))
                .replaceAll("\\s+", " ").trim();
        notes.add("未填写 seoDescription，已截取正文开头作为 SEO 描述，建议人工调整");
        return truncate(plain, SEO_DESC_FALLBACK_LEN);
    }

    /** 标题查重。同名文章直接跳过，避免同一批 md 被重复导入两遍 */
    private boolean existsByTitle(String title) {
        return seoArticleService.count(new LambdaQueryWrapper<SeoArticleEntity>()
                .eq(SeoArticleEntity::getTitle, title.trim())
                .eq(SeoArticleEntity::getIsDeleted, SeoConstant.NOT_DELETED)) > 0;
    }

    /** 进度落库。用 LambdaUpdateWrapper 逐字段 set，不依赖 MP 的 update-strategy */
    private void updateProgress(String taskId, List<SeoImportItemVo> results,
                                int successCount, int failCount, int status) {
        LambdaUpdateWrapper<SeoImportTaskEntity> wrapper = new LambdaUpdateWrapper<SeoImportTaskEntity>()
                .set(SeoImportTaskEntity::getProcessed, results.size())
                .set(SeoImportTaskEntity::getSuccessCount, successCount)
                .set(SeoImportTaskEntity::getFailCount, failCount)
                .set(SeoImportTaskEntity::getResultJson, serializeResults(results))
                .set(SeoImportTaskEntity::getTaskStatus, status)
                .eq(SeoImportTaskEntity::getTaskId, taskId);
        if (status != SeoConstant.IMPORT_STATUS_RUNNING) {
            wrapper.set(SeoImportTaskEntity::getFinishDate, LocalDateTime.now());
        }
        update(wrapper);
    }

    private void markTaskFailed(String taskId, String errorMsg) {
        update(new LambdaUpdateWrapper<SeoImportTaskEntity>()
                .set(SeoImportTaskEntity::getTaskStatus, SeoConstant.IMPORT_STATUS_FAILED)
                .set(SeoImportTaskEntity::getErrorMsg, truncate(errorMsg, MAX_ERROR_MSG_LEN))
                .set(SeoImportTaskEntity::getFinishDate, LocalDateTime.now())
                .eq(SeoImportTaskEntity::getTaskId, taskId));
    }

    /**
     * 序列化逐篇结果，并对总长度封顶。
     *
     * <p>result_json 是 LONGTEXT，没有 DDL 层的长度约束。notes 里会拼进图片 URL，
     * 而 URL 长度只受 2MB 文件本身限制——30 个 60KB 的失效 URL × 50 篇能撑出近百 MB，
     * 且每篇都要全量重写一次。封顶后超出部分只留计数，够运营判断问题出在哪。
     */
    private String serializeResults(List<SeoImportItemVo> results) {
        String json = JSON.toJSONString(results);
        if (json.length() <= MAX_RESULT_JSON_LEN) {
            return json;
        }
        List<SeoImportItemVo> trimmed = new ArrayList<>();
        int length = 0;
        for (SeoImportItemVo item : results) {
            String itemJson = JSON.toJSONString(item);
            if (length + itemJson.length() > MAX_RESULT_JSON_LEN) {
                SeoImportItemVo omitted = new SeoImportItemVo();
                omitted.setFile("(其余 " + (results.size() - trimmed.size()) + " 个文件)");
                omitted.setSuccess(false);
                List<String> notes = new ArrayList<>();
                notes.add("结果内容过长已省略，请缩短文件中的图片地址后重试，或分批导入");
                omitted.setNotes(notes);
                trimmed.add(omitted);
                break;
            }
            trimmed.add(item);
            length += itemJson.length();
        }
        return JSON.toJSONString(trimmed);
    }

    private boolean isStale(SeoImportTaskEntity task) {
        return task.getCreateDate() != null && task.getCreateDate().isBefore(
                LocalDateTime.now().minusNanos(SeoConstant.IMPORT_STALE_THRESHOLD_MS * 1_000_000L));
    }

    private List<SeoImportItemVo> parseResults(String resultJson) {
        if (resultJson == null || resultJson.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return JSON.parseArray(resultJson, SeoImportItemVo.class);
        } catch (Exception e) {
            log.warn("[SEO导入] 结果 JSON 解析失败，返回空列表", e);
            return new ArrayList<>();
        }
    }

    private SeoImportItemVo failItem(String fileName, String title, String reason) {
        return failItem(fileName, title, reason, List.of());
    }

    /** 失败原因排在最前，之后附上此前已产生的提示（如已自动创建的分类/标签） */
    private SeoImportItemVo failItem(String fileName, String title, String reason, List<String> extraNotes) {
        SeoImportItemVo item = new SeoImportItemVo();
        item.setFile(fileName);
        item.setTitle(title);
        item.setSuccess(false);
        List<String> notes = new ArrayList<>();
        notes.add(reason);
        notes.addAll(extraNotes);
        item.setNotes(notes);
        return item;
    }

    /**
     * 未预料异常的文案。不回显 {@code e.getMessage()}——那里可能带 SQL 片段、
     * 内网主机名等实现细节，而这段文案会原样显示给运营。
     */
    private String describeUnexpected(Exception e) {
        return "导入失败（" + e.getClass().getSimpleName() + "），请联系技术人员排查";
    }

    /**
     * HTML 属性值解码。
     *
     * <p>顺序关键：{@code &amp;} 必须<b>最后</b>解，否则 {@code &amp;amp;lt;} 会被先还原成
     * {@code &amp;lt;} 再被当作 {@code <} 二次解码，凭空多出一个尖括号。
     */
    private static String unescapeHtml(String value) {
        return value.replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&amp;", "&");
    }

    /**
     * HTML 属性值转义。
     *
     * <p>顺序同样关键，方向相反：{@code &} 必须<b>最先</b>转，否则后面转出来的
     * {@code &lt;} 里的 {@code &} 会被再转一次，变成 {@code &amp;lt;}。
     */
    private static String escapeHtmlAttr(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String truncate(String value, int maxLen) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLen ? value : value.substring(0, maxLen);
    }
}
