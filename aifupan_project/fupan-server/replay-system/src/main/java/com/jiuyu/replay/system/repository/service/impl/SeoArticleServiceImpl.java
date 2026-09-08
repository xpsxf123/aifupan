package com.jiuyu.replay.system.repository.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.system.bo.SeoArticleBo;
import com.jiuyu.replay.system.bo.SeoArticleListBo;
import com.jiuyu.replay.system.constant.SeoConstant;
import com.jiuyu.replay.system.entity.SeoArticleEntity;
import com.jiuyu.replay.system.entity.SeoCategoryEntity;
import com.jiuyu.replay.system.repository.dao.SeoArticleDao;
import com.jiuyu.replay.system.repository.dao.SeoCategoryDao;
import com.jiuyu.replay.system.repository.service.SeoArticleService;
import com.jiuyu.replay.system.repository.service.SeoArticleTagService;
import com.jiuyu.replay.system.repository.service.SeoSiteService;
import com.jiuyu.replay.system.repository.service.SeoSlugHistoryService;
import com.jiuyu.replay.system.repository.service.SeoSlugService;
import com.jiuyu.replay.system.repository.service.SeoTagService;
import com.jiuyu.replay.system.util.SeoHtmlSanitizer;
import com.jiuyu.replay.system.util.SeoSlugUtil;
import com.jiuyu.replay.system.vo.SeoArticleInfoVo;
import com.jiuyu.replay.system.vo.SeoArticleListVo;
import com.jiuyu.replay.system.vo.SeoArticleTagVo;
import com.jiuyu.replay.system.vo.SeoIdCountVo;
import com.jiuyu.replay.system.vo.SeoSaveResultVo;
import com.jiuyu.replay.system.vo.SeoTagOptionVo;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * SEO 文章实现。
 *
 * @author claude
 * @date 2026-08-13
 */
@Service
public class SeoArticleServiceImpl extends ServiceImpl<SeoArticleDao, SeoArticleEntity>
        implements SeoArticleService {

    /** 标题业务上限，超出会撞 varchar(200) 的 1406 */
    private static final int MAX_TITLE_LEN = 100;

    /** 摘要业务上限 */
    private static final int MAX_SUMMARY_LEN = 200;

    /**
     * SEO 标题的<b>入库</b>上限，取 DDL 的 varchar(200)。
     *
     * <p>不要拿建议值 60 来校验：60 是「超出会被搜索结果截断」的经验值，不是数据约束。
     * PRD 5.2.3 明确要求超出时只提示、<b>不阻断提交</b>——运营有时确实需要更长的标题，
     * 截断发生在搜索引擎那边，与能否保存无关。前端按 60 计数变红，这里只挡真正会撞 1406 的长度。
     */
    private static final int MAX_SEO_TITLE_LEN = 200;

    /** SEO 描述入库上限，取 DDL 的 varchar(300)。建议值 160 同样只用于前端提示，见上 */
    private static final int MAX_SEO_DESC_LEN = 300;

    /** seo_keywords 字段是 varchar(300) */
    private static final int MAX_SEO_KEYWORDS_LEN = 300;

    /** cover_url 字段是 varchar(500) */
    private static final int MAX_COVER_URL_LEN = 500;

    private final SeoSlugService seoSlugService;

    private final SeoArticleTagService seoArticleTagService;

    private final SeoTagService seoTagService;

    private final SeoCategoryDao seoCategoryDao;

    private final SeoSlugHistoryService seoSlugHistoryService;

    /** 内容变更后让官网接口缓存立即失效 */
    private final SeoSiteService seoSiteService;

    public SeoArticleServiceImpl(SeoSlugService seoSlugService,
                                 SeoArticleTagService seoArticleTagService,
                                 SeoTagService seoTagService,
                                 SeoCategoryDao seoCategoryDao,
                                 SeoSlugHistoryService seoSlugHistoryService,
                                 SeoSiteService seoSiteService) {
        this.seoSlugService = seoSlugService;
        this.seoArticleTagService = seoArticleTagService;
        this.seoTagService = seoTagService;
        this.seoCategoryDao = seoCategoryDao;
        this.seoSlugHistoryService = seoSlugHistoryService;
        this.seoSiteService = seoSiteService;
    }

    @Override
    public PageUtils<SeoArticleListVo> queryPage(SeoArticleListBo bo) {
        // 查询整体在 mapper/SeoArticleDao.xml 里，不用 Wrapper 拼——
        // 标签筛选是多对多关联表上的条件，Wrapper 只能靠 apply() 塞原生 SQL 字符串
        IPage<SeoArticleEntity> page = getBaseMapper().selectArticlePage(
                new Query<SeoArticleEntity>().getPageNoSort(bo.getPage(), bo.getLimit()),
                bo, SeoConstant.NOT_DELETED);

        PageUtils<SeoArticleListVo> result = new PageUtils<>(bo.getPage(), bo.getLimit(), page);
        List<SeoArticleEntity> records = page.getRecords();
        if (records == null || records.isEmpty()) {
            result.setList(new ArrayList<>());
            return result;
        }

        Map<Long, String> categoryNames = mapCategoryNames(
                records.stream().map(SeoArticleEntity::getCategoryId).distinct().toList());
        Map<Long, List<SeoArticleTagVo>> tagMap = mapArticleTags(
                records.stream().map(SeoArticleEntity::getId).toList());

        List<SeoArticleListVo> list = new ArrayList<>(records.size());
        for (SeoArticleEntity entity : records) {
            SeoArticleListVo vo = new SeoArticleListVo();
            BeanUtils.copyProperties(entity, vo);
            vo.setCategoryName(categoryNames.get(entity.getCategoryId()));
            vo.setTags(tagMap.getOrDefault(entity.getId(), new ArrayList<>()));
            list.add(vo);
        }
        result.setList(list);
        return result;
    }

    @Override
    public SeoArticleInfoVo info(Long id) {
        SeoArticleEntity entity = getById(id);
        if (entity == null || Objects.equals(entity.getIsDeleted(), SeoConstant.DELETED)) {
            throw new BusinessException("该文章不存在或已被删除");
        }
        SeoArticleInfoVo vo = new SeoArticleInfoVo();
        BeanUtils.copyProperties(entity, vo);
        vo.setCategoryName(mapCategoryNames(List.of(entity.getCategoryId())).get(entity.getCategoryId()));
        vo.setTags(mapArticleTags(List.of(id)).getOrDefault(id, new ArrayList<>()));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SeoSaveResultVo saveArticle(SeoArticleBo bo) {
        validate(bo);

        // 先生成主键：运营未指定别名时要拿它当 slug，顺序不能反
        SeoArticleEntity entity = new SeoArticleEntity();
        entity.setId(SnowflakeManager.nextValue());
        String finalSlug = resolveArticleSlug(bo.getTitle(), bo.getSlug(), entity.getId(), null);
        applyBo(entity, bo, finalSlug);
        entity.setViewCount(0);
        entity.setIsDeleted(SeoConstant.NOT_DELETED);
        entity.setCreateDate(LocalDateTime.now());
        entity.setUpdateDate(LocalDateTime.now());
        // 新建即发布时写入首次发布时间
        if (Objects.equals(entity.getArticleStatus(), SeoConstant.STATUS_ENABLED)) {
            entity.setPublishTime(LocalDateTime.now());
        }
        saveWithSlugRetry(entity, bo.getTitle(), bo.getSlug());
        seoArticleTagService.replaceArticleTags(entity.getId(), sanitizeTagIds(bo.getTagIds()));

        // 内容变了就让官网缓存立即失效。不清的话，下架的文章会继续对外可见
        // 到 TTL 自然到期为止——官网详情页不做页面级缓存，这一层是唯一的闸门
        seoSiteService.evictAll();

        return buildResult(entity.getId(), finalSlug, bo.getTitle(), bo.getSlug());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SeoSaveResultVo updateArticle(SeoArticleBo bo) {
        SeoArticleEntity existing = getById(bo.getId());
        if (existing == null || Objects.equals(existing.getIsDeleted(), SeoConstant.DELETED)) {
            throw new BusinessException("该文章不存在或已被删除");
        }
        validate(bo);
        String finalSlug = resolveArticleSlug(bo.getTitle(), bo.getSlug(), bo.getId(), bo.getId());

        SeoArticleEntity entity = new SeoArticleEntity();
        applyBo(entity, bo, finalSlug);

        // 显式列出要更新的字段，不用 updateById。
        // updateById 保留 publishTime/createDate/viewCount 靠的是 MP 全局 update-strategy
        // 默认 NOT_NULL 把 null 字段排除出 SET——这与上一轮翻车的 logic-delete-field 是同一类
        // 「全局配置决定字段是否进 SET」的陷阱：只要有人配了 update-strategy: ignored，
        // 这三个字段会被一次性清零且不报任何错。显式 set 则完全不依赖该策略
        LambdaUpdateWrapper<SeoArticleEntity> wrapper = new LambdaUpdateWrapper<SeoArticleEntity>()
                .set(SeoArticleEntity::getTitle, entity.getTitle())
                .set(SeoArticleEntity::getSlug, entity.getSlug())
                .set(SeoArticleEntity::getCategoryId, entity.getCategoryId())
                .set(SeoArticleEntity::getContent, entity.getContent())
                .set(SeoArticleEntity::getSummary, entity.getSummary())
                .set(SeoArticleEntity::getCoverUrl, entity.getCoverUrl())
                .set(SeoArticleEntity::getArticleStatus, entity.getArticleStatus())
                .set(SeoArticleEntity::getSeoTitle, entity.getSeoTitle())
                .set(SeoArticleEntity::getSeoDescription, entity.getSeoDescription())
                .set(SeoArticleEntity::getSeoKeywords, entity.getSeoKeywords())
                .set(SeoArticleEntity::getUpdateDate, LocalDateTime.now())
                .eq(SeoArticleEntity::getId, bo.getId());
        // publishTime 只在「此前从未发布过、本次转为已发布」时写入。
        // 下架不清空、重新发布不覆盖——保持首次发布时间，避免搜索引擎重新判定内容时效。
        // 其余情况根本不进 SET，因此不可能被覆盖或清空
        if (existing.getPublishTime() == null
                && Objects.equals(bo.getArticleStatus(), SeoConstant.STATUS_ENABLED)) {
            wrapper.set(SeoArticleEntity::getPublishTime, LocalDateTime.now());
        }
        update(wrapper);

        // 必须在 replaceArticleTags【之前】取旧标签：替换之后关联已经没了，
        // 就查不出「这次操作让哪些标签失去了最后一篇文章」
        List<Long> previousTagIds = seoArticleTagService
                .mapTagIdsByArticleIds(List.of(bo.getId()))
                .getOrDefault(bo.getId(), new ArrayList<>());
        seoArticleTagService.replaceArticleTags(bo.getId(), sanitizeTagIds(bo.getTagIds()));
        // 被摘掉的标签可能已归零，自动关闭（后台反馈第 2 条：只自动关、不自动开）
        seoTagService.autoDisableEmptyTags(previousTagIds);

        // slug 变了就记一条历史，官网据此把旧文章地址 301 到新地址。
        // 这是三处里最要紧的一处：文章页是唯一真正承接搜索流量的页面，
        // 旧地址一旦直接 404，累积的排名与外链会一起丢掉，且没有任何报错。
        // 只在真的变化时记，避免历史表堆满 old_slug == 当前 slug 的噪音行
        if (!Objects.equals(existing.getSlug(), finalSlug)) {
            seoSlugHistoryService.record(
                    SeoConstant.SLUG_ENTITY_ARTICLE, bo.getId(), existing.getSlug());
        }

        seoSiteService.evictAll();

        return buildResult(bo.getId(), finalSlug, bo.getTitle(), bo.getSlug());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteArticles(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("请选择要删除的文章");
        }
        List<Long> validIds = ids.stream().filter(Objects::nonNull).toList();
        if (validIds.isEmpty()) {
            throw new BusinessException("请选择要删除的文章");
        }
        LocalDateTime now = LocalDateTime.now();
        int affected = 0;
        for (List<Long> batch : Lists.partition(validIds, SeoConstant.IN_BATCH_SIZE)) {
            // 墓碑值用 SQL 的 CONCAT 就地拼，一条 UPDATE 处理整批。
            //
            // 原先是「先 SELECT 出 slug，再逐条 UPDATE」：删 100 篇要 101 次往返，
            // 且 SELECT 与 UPDATE 之间有窗口——期间别人改了 slug，写回的墓碑值就基于旧值了。
            //
            // 必须用 setSql / LambdaUpdateWrapper 而不是 updateBatchById：MP 全局
            // logic-delete-field 会把 is_deleted 从 SET 中剔除，详见 SeoConstant.LOGIC_DELETE_NOTE
            affected += getBaseMapper().update(null, new LambdaUpdateWrapper<SeoArticleEntity>()
                    .setSql(SeoSlugUtil.tombstoneSetSql("slug"))
                    .set(SeoArticleEntity::getIsDeleted, SeoConstant.DELETED)
                    .set(SeoArticleEntity::getUpdateDate, now)
                    .in(SeoArticleEntity::getId, batch)
                    .eq(SeoArticleEntity::getIsDeleted, SeoConstant.NOT_DELETED));
        }
        // 一条都没删掉说明这些 id 全都不存在或已被别人删了。静默返回「删除成功」
        // 会让运营以为生效了，刷新后发现文章还在（或早就不在了）
        if (affected == 0) {
            throw new BusinessException("所选文章不存在或已被删除，请刷新后重试");
        }
        // 同样要在解除关联之前取，否则查不到受影响的标签
        List<Long> affectedTagIds = seoArticleTagService.mapTagIdsByArticleIds(validIds)
                .values().stream().flatMap(List::stream).distinct().toList();
        seoArticleTagService.removeByArticleIds(validIds);
        seoTagService.autoDisableEmptyTags(affectedTagIds);
        seoSiteService.evictAll();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long id, Integer status) {
        if (id == null) {
            throw new BusinessException("请选择要操作的文章");
        }
        if (status == null
                || (status != SeoConstant.STATUS_ENABLED && status != SeoConstant.STATUS_DISABLED)) {
            throw new BusinessException("状态值不合法，仅支持 1 已发布 / 0 已下架");
        }
        SeoArticleEntity existing = getById(id);
        if (existing == null || Objects.equals(existing.getIsDeleted(), SeoConstant.DELETED)) {
            throw new BusinessException("该文章不存在或已被删除");
        }
        LambdaUpdateWrapper<SeoArticleEntity> wrapper = new LambdaUpdateWrapper<SeoArticleEntity>()
                .set(SeoArticleEntity::getArticleStatus, status)
                .set(SeoArticleEntity::getUpdateDate, LocalDateTime.now())
                .eq(SeoArticleEntity::getId, id);
        // 仅首次发布写入 publishTime
        if (existing.getPublishTime() == null && Objects.equals(status, SeoConstant.STATUS_ENABLED)) {
            wrapper.set(SeoArticleEntity::getPublishTime, LocalDateTime.now());
        }
        update(wrapper);
        seoSiteService.evictAll();
    }

    @Override
    public Map<Long, Integer> countByCategoryIds(List<Long> categoryIds) {
        Map<Long, Integer> countMap = new HashMap<>();
        if (categoryIds == null || categoryIds.isEmpty()) {
            return countMap;
        }
        for (List<Long> batch : Lists.partition(categoryIds, SeoConstant.IN_BATCH_SIZE)) {
            // SQL 见 mapper/SeoArticleDao.xml。GROUP BY 聚合而非拉明细行计数——
            // 一个分类下的文章可能上万
            for (SeoIdCountVo row : getBaseMapper().countByCategoryIds(batch, SeoConstant.NOT_DELETED)) {
                countMap.put(row.getId(), row.getTotal());
            }
        }
        return countMap;
    }

    /** 入参校验。分类存在性校验同时是分类删除 TOCTOU 闭环的一半 */
    private void validate(SeoArticleBo bo) {
        if (bo.getTitle() == null || bo.getTitle().isBlank()) {
            throw new BusinessException("文章标题不能为空");
        }
        if (bo.getCoverUrl() == null || bo.getCoverUrl().isBlank()) {
            throw new BusinessException("封面图不能为空");
        }
        if (bo.getSeoDescription() == null || bo.getSeoDescription().isBlank()) {
            throw new BusinessException("SEO 描述不能为空");
        }
        if (bo.getContent() == null || bo.getContent().isBlank()) {
            throw new BusinessException("正文内容不能为空");
        }
        if (bo.getTagIds() != null && bo.getTagIds().size() > SeoConstant.MAX_TAGS_PER_ARTICLE) {
            throw new BusinessException("单篇文章最多关联 " + SeoConstant.MAX_TAGS_PER_ARTICLE + " 个标签");
        }
        // status 必填且限 0/1。不能像原先那样「null 默认已发布」——那意味着一个漏填状态的
        // 请求会把草稿直接发到公开 SEO 站点，而 publishTime 一旦写入没有任何路径能清空
        if (bo.getArticleStatus() == null) {
            throw new BusinessException("请选择文章状态");
        }
        if (bo.getArticleStatus() != SeoConstant.STATUS_ENABLED && bo.getArticleStatus() != SeoConstant.STATUS_DISABLED) {
            throw new BusinessException("状态值不合法，仅支持 1 已发布 / 0 已下架");
        }
        checkLength("文章标题", bo.getTitle(), MAX_TITLE_LEN);
        checkLength("摘要", bo.getSummary(), MAX_SUMMARY_LEN);
        checkLength("SEO 标题", bo.getSeoTitle(), MAX_SEO_TITLE_LEN);
        checkLength("SEO 描述", bo.getSeoDescription(), MAX_SEO_DESC_LEN);
        // seo_keywords 是 varchar(300)、cover_url 是 varchar(500)。不在这里拦的话，
        // 超长会撞 MySQL 1406，被导入路径兜底成「导入失败（DataIntegrityViolationException）」——
        // 违反 openspec §5.4「失败原因必须具体」
        checkLength("SEO 关键词", bo.getSeoKeywords(), MAX_SEO_KEYWORDS_LEN);
        checkLength("封面图地址", bo.getCoverUrl(), MAX_COVER_URL_LEN);
        // 封面地址会进 og:image 与列表页，且它不经过正文的 HTML 过滤，
        // 只能在这里挡住 javascript: 之类的协议
        String cover = bo.getCoverUrl().trim().toLowerCase();
        if (!cover.startsWith("http://") && !cover.startsWith("https://")) {
            throw new BusinessException("封面图地址必须以 http:// 或 https:// 开头");
        }
        if (bo.getCategoryId() == null) {
            throw new BusinessException("请选择所属分类");
        }
        // 必须是「当前读」（for update），不能用普通查询：
        // InnoDB 的一致性非锁定读走 undo log，不会被 deleteCategory 持有的 X 锁阻塞——
        // 那样即便删除侧加了行锁，仍可能 T1 count=0 → T2 快照读看到分类存在 → T1 删除 → T2 插入，
        // 产生指向已删分类的孤儿文章。两侧同取当前读才真正闭环
        SeoCategoryEntity category = seoCategoryDao.selectOne(new LambdaQueryWrapper<SeoCategoryEntity>()
                .eq(SeoCategoryEntity::getId, bo.getCategoryId())
                .eq(SeoCategoryEntity::getIsDeleted, SeoConstant.NOT_DELETED)
                .last("limit 1 for update"));
        if (category == null) {
            throw new BusinessException("所选分类不存在或已被删除，请重新选择");
        }
    }

    /**
     * 插入文章，slug 撞唯一索引时重新解析一次再插。
     *
     * <p>为什么需要：{@code resolveSlug} 是「查重 → 返回」，与 INSERT 之间存在窗口。
     * 两个并发请求对同一标题都判定 base 可用，后插入的那个会撞 uk_seo_article_slug，
     * 运营看到的是 500 而不是自动追加后缀（openspec §5.1 第 3 条要求重试）。
     *
     * <p>为什么可以在事务内重试：Spring 只在异常<b>抛出</b>事务方法时才标记 rollback-only，
     * 这里 catch 的是 MP {@code save()} 直接抛出的异常，未跨越任何事务边界，事务仍然可用。
     * 反例见 {@code SeoTagServiceImpl.findOrCreateByName}——那里的异常来自另一个
     * {@code @Transactional} 方法，事务已被标记，所以只能重查而不能重试插入。
     *
     * <p>文章表除主键外只有 slug 一个唯一索引，因此该异常必定源于 slug 冲突。
     */
    /**
     * 决定文章的 URL 别名。
     *
     * <p><b>运营未填别名时，用文章自身的雪花 ID</b>，而不是从标题生成拼音
     * （2026-08-18 后台反馈第 1 条）。雪花 ID 天然唯一，因此这条路径不需要查重、
     * 也不会出现「追加 4 位随机数」的后缀。
     *
     * <p>运营手动填了值时仍走原有的拼音/查重链路——保留这个口子是因为
     * URL 里的关键词对中文 SEO 有实际权重，日后想给重点文章配一个可读别名时
     * 不必再改代码。
     *
     * @param title        标题，仅在手动填了别名时用于兜底生成
     * @param requestedSlug 运营填写的别名，可空
     * @param articleId    文章主键，未填别名时作为 slug
     * @param excludeId    查重时要排除的自身 ID，新增传 null
     */
    private String resolveArticleSlug(String title, String requestedSlug, Long articleId, Long excludeId) {
        if (requestedSlug != null && !requestedSlug.isBlank()) {
            return seoSlugService.resolveSlug(
                    SeoConstant.SLUG_TYPE_ARTICLE, title, requestedSlug, excludeId);
        }
        return String.valueOf(articleId);
    }

    private void saveWithSlugRetry(SeoArticleEntity entity, String title, String requestedSlug) {
        try {
            save(entity);
        } catch (DuplicateKeyException e) {
            // 对手已提交，此刻重新查重能看到它，必定解析出不同的值
            entity.setSlug(resolveArticleSlug(title, requestedSlug, entity.getId(), null));
            save(entity);
        }
    }

    /**
     * 长度校验。不做的话超长字段会撞 MySQL strict 模式的 1406，运营只看到 500——
     * 而导入路径复用本方法时，openspec §5.4 要求「失败原因必须具体」
     */
    private void checkLength(String label, String value, int maxLen) {
        if (value != null && value.length() > maxLen) {
            throw new BusinessException(label + "不能超过 " + maxLen + " 字，当前 " + value.length() + " 字");
        }
    }

    /** 把 BO 的字段搬到实体，并做正文过滤与 SEO 标题兜底 */
    private void applyBo(SeoArticleEntity entity, SeoArticleBo bo, String finalSlug) {
        entity.setTitle(bo.getTitle().trim());
        entity.setSlug(finalSlug);
        entity.setCategoryId(bo.getCategoryId());
        // 入库前统一过滤：手工新建与批量导入两条路都经过这里，h1 也在此降级为 h2
        entity.setContent(SeoHtmlSanitizer.sanitize(bo.getContent()));
        entity.setSummary(bo.getSummary() == null ? "" : bo.getSummary().trim());
        entity.setCoverUrl(bo.getCoverUrl().trim());
        entity.setArticleStatus(bo.getArticleStatus());
        // SEO 标题留空时取文章标题，与前端「只填空不覆盖」的自动填充规则一致
        entity.setSeoTitle(stripAngleBrackets(bo.getSeoTitle() == null || bo.getSeoTitle().isBlank()
                ? bo.getTitle().trim() : bo.getSeoTitle().trim()));
        entity.setSeoDescription(stripAngleBrackets(bo.getSeoDescription().trim()));
        entity.setSeoKeywords(stripAngleBrackets(
                bo.getSeoKeywords() == null ? "" : bo.getSeoKeywords().trim()));
    }

    /**
     * 剥掉 SEO 元数据里的尖括号。
     *
     * <p>这三个字段只用于渲染 {@code <meta>} 与 {@code <title>}，不经过正文的 HTML 白名单过滤。
     * 导入路径下它们直接来自不可信的 md 文件，写个
     * {@code seoDescription: '"><script>alert(1)</script>'} 就够了——160 字上限对 payload 绰绰有余。
     *
     * <p>这是<b>纵深防御</b>，不是替代品：官网渲染这些字段时仍必须做属性转义。
     * 尖括号在 meta 内容里没有任何正当用途，剥掉不会损失语义；
     * 引号则保留（描述里会正常出现），靠渲染层转义。
     */
    private String stripAngleBrackets(String value) {
        return value == null ? "" : value.replace("<", "").replace(">", "");
    }

    /** 剔除 null 元素：关联表 tag_id 是 NOT NULL，含 null 会撞 1048 */
    private List<Long> sanitizeTagIds(List<Long> tagIds) {
        if (tagIds == null) {
            return new ArrayList<>();
        }
        return tagIds.stream().filter(Objects::nonNull).toList();
    }

    private Map<Long, String> mapCategoryNames(List<Long> categoryIds) {
        Map<Long, String> names = new HashMap<>();
        if (categoryIds == null || categoryIds.isEmpty()) {
            return names;
        }
        for (List<Long> batch : Lists.partition(categoryIds, SeoConstant.IN_BATCH_SIZE)) {
            List<SeoCategoryEntity> categories = seoCategoryDao.selectList(
                    new LambdaQueryWrapper<SeoCategoryEntity>()
                            .select(SeoCategoryEntity::getId, SeoCategoryEntity::getCategoryName)
                            .in(SeoCategoryEntity::getId, batch));
            names.putAll(categories.stream().collect(
                    Collectors.toMap(SeoCategoryEntity::getId, SeoCategoryEntity::getCategoryName, (a, b) -> a)));
        }
        return names;
    }

    /** 查出文章的标签，含已禁用的——列表要置灰标注，编辑时也不能静默丢弃 */
    private Map<Long, List<SeoArticleTagVo>> mapArticleTags(List<Long> articleIds) {
        Map<Long, List<SeoArticleTagVo>> result = new HashMap<>();
        Map<Long, List<Long>> relations = seoArticleTagService.mapTagIdsByArticleIds(articleIds);
        if (relations.isEmpty()) {
            return result;
        }
        List<Long> allTagIds = relations.values().stream()
                .flatMap(List::stream).distinct().toList();
        Map<Long, SeoTagOptionVo> tagMap = seoTagService.listByIds(allTagIds).stream()
                .collect(Collectors.toMap(SeoTagOptionVo::getId, Function.identity(), (a, b) -> a));

        for (Map.Entry<Long, List<Long>> entry : relations.entrySet()) {
            List<SeoArticleTagVo> tags = new ArrayList<>();
            for (Long tagId : entry.getValue()) {
                SeoTagOptionVo option = tagMap.get(tagId);
                if (option == null) {
                    continue;
                }
                SeoArticleTagVo tagVo = new SeoArticleTagVo();
                tagVo.setId(option.getId());
                tagVo.setTagName(option.getTagName());
                tagVo.setTagStatus(option.getTagStatus());
                tags.add(tagVo);
            }
            result.put(entry.getKey(), tags);
        }
        return result;
    }

    private SeoSaveResultVo buildResult(Long id, String finalSlug, String title, String requestedSlug) {
        SeoSaveResultVo result = new SeoSaveResultVo();
        result.setId(id);
        result.setSlug(finalSlug);
        // 未填别名时用的是文章雪花 ID（见 resolveArticleSlug），它天然唯一、
        // 不存在「被追加随机数」这回事。若仍按拼音去比对，会永远判定为已追加，
        // 让前端弹出一个不存在的冲突提示
        if (requestedSlug == null || requestedSlug.isBlank()) {
            SeoSaveResultVo idResult = new SeoSaveResultVo();
            idResult.setId(id);
            idResult.setSlug(finalSlug);
            idResult.setSlugAppended(false);
            return idResult;
        }
        String base = SeoSlugUtil.normalize(requestedSlug);
        base = SeoSlugUtil.truncate(base, SeoConstant.MAX_SLUG_LEN_ARTICLE - 5);
        // base 为空说明标题转不出拼音（纯日文 / emoji / 纯符号），最终值是 resolveWithFallback
        // 兜底生成的 article-xxxxxx——与用户输入毫无关系，同样要让运营知情
        result.setSlugAppended(base.isEmpty() || !base.equals(finalSlug));
        return result;
    }
}
