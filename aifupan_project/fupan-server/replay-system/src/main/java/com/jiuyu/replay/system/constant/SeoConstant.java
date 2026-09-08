package com.jiuyu.replay.system.constant;

/**
 * SEO 内容管理常量。
 *
 * @author claude
 * @date 2026-08-12
 */
public final class SeoConstant {

    /** 未删除 */
    public static final int NOT_DELETED = 0;

    /** 已删除 */
    public static final int DELETED = 1;

    /** 文章已发布 / 标签启用 */
    public static final int STATUS_ENABLED = 1;

    /** 文章已下架 / 标签禁用 */
    public static final int STATUS_DISABLED = 0;

    /** slug 对象类型：文章 */
    public static final String SLUG_TYPE_ARTICLE = "article";

    /** slug 对象类型：分类 */
    public static final String SLUG_TYPE_CATEGORY = "category";

    /** slug 对象类型：标签 */
    public static final String SLUG_TYPE_TAG = "tag";

    /** 单篇文章标签上限。超量标签会稀释权重，且前台展示空间有限 */
    public static final int MAX_TAGS_PER_ARTICLE = 10;

    /** IN 查询分批大小，超过需 Lists.partition 分批 */
    public static final int IN_BATCH_SIZE = 500;

    // ---------------------------------------------------------------- 官网公开接口（/replay/site/**）

    /** slug 历史表 entity_type：文章 */
    public static final int SLUG_ENTITY_ARTICLE = 1;

    /** slug 历史表 entity_type：分类 */
    public static final int SLUG_ENTITY_CATEGORY = 2;

    /** slug 历史表 entity_type：标签 */
    public static final int SLUG_ENTITY_TAG = 3;

    /** 官网列表接口默认每页条数 */
    public static final int SITE_DEFAULT_PAGE_SIZE = 10;

    /**
     * 官网列表接口每页条数上限。
     *
     * <p>必须封顶：这是匿名公开接口，不限制的话一个 {@code limit=999999} 就能把全站文章
     * 一次性拉走——既是内容被批量抓取的口子，也能拖垮数据库。
     */
    public static final int SITE_MAX_PAGE_SIZE = 50;

    /** 官网文末相关文章默认条数（决策 5） */
    public static final int SITE_RELATED_DEFAULT_LIMIT = 4;

    /**
     * 标签页进 sitemap 的最低已发布文章数（决策 7）。
     *
     * <p>低于此值的标签页属薄内容，官网会加 {@code noindex, follow} 且不进 sitemap——
     * 大量「只有 1-2 篇文章」的聚合页会拉低站点整体质量评分，连带影响文章页排名。
     *
     * <p>阈值放后端统一把控，官网只负责渲染，避免同一个数字散落两处、日后改一处忘另一处。
     */
    public static final int SITE_TAG_INDEX_MIN_COUNT = 3;

    /**
     * slug 入库长度上限，须为「字段长度 - 墓碑后缀 25 字符」。
     *
     * <p>不截断会在新增时就撞 1406 Data too long：100 字标题转全拼可达 300+ 字符。
     * 分类/标签 slug 字段 varchar(140)，文章 varchar(180)。
     */
    public static final int MAX_SLUG_LEN_CATEGORY_TAG = 115;

    /** 文章 slug 入库长度上限，见 {@link #MAX_SLUG_LEN_CATEGORY_TAG} */
    public static final int MAX_SLUG_LEN_ARTICLE = 155;

    /** 导入任务状态：处理中 */
    public static final int IMPORT_STATUS_RUNNING = 0;

    /** 导入任务状态：已完成（含部分失败——逐篇独立事务，整批不回滚） */
    public static final int IMPORT_STATUS_FINISHED = 1;

    /** 导入任务状态：任务级失败（线程池拒绝、执行器崩溃等，与单篇失败无关） */
    public static final int IMPORT_STATUS_FAILED = 2;

    /** 单次导入文件数上限，与前端 import-drawer 的 MAX_FILES 一致 */
    public static final int MAX_IMPORT_FILES = 50;

    /** 单个 .md 大小上限（2MB），与前端一致 */
    public static final long MAX_IMPORT_FILE_SIZE = 2L * 1024 * 1024;

    /**
     * 单次导入总大小上限（20MB）。
     *
     * <p>按总字节而不是按文件数限流：文件内容要在堆里常驻到导入结束，
     * 只卡「50 个文件」而不卡总量的话，并发几次满额导入就能把进程压垮
     * （multipart 全局配的是 1024MB，请求侧没有别的闸门）。
     */
    public static final long MAX_IMPORT_TOTAL_SIZE = 20L * 1024 * 1024;

    /**
     * 单个导入任务的总时长上限（毫秒）。
     *
     * <p>没有它的最坏情况：50 篇 × 31 张图 × 4 跳 × 20s 超时 ≈ 33 小时，
     * 期间独占一个池线程（core 只有 2 个），前端转圈一整天。
     */
    public static final long MAX_IMPORT_DURATION_MS = 10L * 60 * 1000;

    /**
     * 导入任务判定为「僵死」的时长（毫秒）。
     *
     * <p>线程池是静态守护线程池，发版重启会让正在跑和排队中的任务无声消失，
     * 对应的 DB 行永远停在「处理中」，前端无限轮询。查询时按创建时间兜底判超期，
     * 比引入调度扫表便宜。取值须大于 {@link #MAX_IMPORT_DURATION_MS}。
     */
    public static final long IMPORT_STALE_THRESHOLD_MS = 30L * 60 * 1000;

    /** 单篇正文图片抓取上限。超出的保留原始外链，不让整篇失败 */
    public static final int MAX_IMAGES_PER_ARTICLE = 30;

    /** 单张图片大小上限（5MB） */
    public static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;

    /** 图片抓取超时（毫秒），连接与「两次 read 之间」各自适用 */
    public static final int IMAGE_FETCH_TIMEOUT_MS = 10_000;

    /**
     * 单张图片抓取的整体预算（毫秒），跳转与读取共用。
     *
     * <p>{@link #IMAGE_FETCH_TIMEOUT_MS} 管不住慢速投喂：对方每 9 秒回 1 个字节，
     * 每次 read 都不超时，读满 5MB 要十几个小时，期间独占一个导入线程。
     */
    public static final long IMAGE_FETCH_DEADLINE_MS = 20_000L;

    /** 图片抓取允许的最大跳转次数。每一跳都要重新做内网校验，见 SeoImageFetcher */
    public static final int MAX_IMAGE_REDIRECTS = 3;

    /**
     * MyBatis-Plus 全局配了 logic-delete-field: isDeleted（见 application-*.yml），
     * 因此 updateById 会把 is_deleted 从 SET 中剔除——手动 setIsDeleted 会被静默丢弃。
     *
     * <p>逻辑删除必须走 LambdaUpdateWrapper.set(...)（ew.sqlSet 不受该过滤影响）
     * 或 removeById。同理，查询里的 is_deleted=0 条件由 MP 自动追加，
     * 代码里再写一次是重复条件（无害，保留是为了让意图显式）。
     */
    public static final String LOGIC_DELETE_NOTE = "see application-*.yml logic-delete-field";

    private SeoConstant() {
    }
}
