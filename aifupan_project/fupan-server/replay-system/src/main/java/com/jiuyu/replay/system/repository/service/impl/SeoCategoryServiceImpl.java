package com.jiuyu.replay.system.repository.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.system.bo.SeoCategoryBo;
import com.jiuyu.replay.system.bo.SeoCategoryListBo;
import com.jiuyu.replay.system.constant.SeoConstant;
import com.jiuyu.replay.system.entity.SeoCategoryEntity;
import com.jiuyu.replay.system.repository.dao.SeoCategoryDao;
import com.jiuyu.replay.system.repository.service.SeoArticleService;
import com.jiuyu.replay.system.repository.service.SeoCategoryService;
import com.jiuyu.replay.system.repository.service.SeoSiteService;
import com.jiuyu.replay.system.repository.service.SeoSlugHistoryService;
import com.jiuyu.replay.system.repository.service.SeoSlugService;
import com.jiuyu.replay.system.util.SeoSlugUtil;
import com.jiuyu.replay.system.vo.SeoCategoryListVo;
import com.jiuyu.replay.system.vo.SeoCategoryOptionVo;
import com.jiuyu.replay.system.vo.SeoSaveResultVo;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * SEO 分类实现。
 *
 * @author claude
 * @date 2026-08-12
 */
@Service
public class SeoCategoryServiceImpl extends ServiceImpl<SeoCategoryDao, SeoCategoryEntity>
        implements SeoCategoryService {

    private final SeoSlugService seoSlugService;

    private final SeoArticleService seoArticleService;

    private final SeoSlugHistoryService seoSlugHistoryService;

    /** 内容变更后让官网接口缓存立即失效 */
    private final SeoSiteService seoSiteService;

    public SeoCategoryServiceImpl(SeoSlugService seoSlugService,
                                  SeoArticleService seoArticleService,
                                  SeoSlugHistoryService seoSlugHistoryService,
                                  SeoSiteService seoSiteService) {
        this.seoSlugService = seoSlugService;
        this.seoArticleService = seoArticleService;
        this.seoSlugHistoryService = seoSlugHistoryService;
        this.seoSiteService = seoSiteService;
    }

    @Override
    public PageUtils<SeoCategoryListVo> queryPage(SeoCategoryListBo bo) {
        LambdaQueryWrapper<SeoCategoryEntity> wrapper = new LambdaQueryWrapper<SeoCategoryEntity>()
                .eq(SeoCategoryEntity::getIsDeleted, SeoConstant.NOT_DELETED)
                .like(bo.getCategoryName() != null && !bo.getCategoryName().isBlank(), SeoCategoryEntity::getCategoryName, bo.getCategoryName())
                .orderByAsc(SeoCategoryEntity::getSort)
                .orderByDesc(SeoCategoryEntity::getCreateDate)
                // id 兜底排序（雪花 ID 单调递增，等价于按创建先后）。
                // 没有它时排序键相同的行在 MySQL 里顺序不确定，翻页会重复或漏行（同一 sort 值的分类很常见，
                // 初始三个分类若都用默认 sort=0 就会撞上）
                .orderByDesc(SeoCategoryEntity::getId);

        IPage<SeoCategoryEntity> page = page(
                new Query<SeoCategoryEntity>().getPageNoSort(bo.getPage(), bo.getLimit()), wrapper);

        PageUtils<SeoCategoryListVo> result = new PageUtils<>(bo.getPage(), bo.getLimit(), page);
        List<SeoCategoryEntity> records = page.getRecords();
        if (records == null || records.isEmpty()) {
            result.setList(new ArrayList<>());
            return result;
        }
        // 一次聚合查出全部分类的文章数，避免逐条 count 造成 N+1
        Map<Long, Integer> countMap = countArticlesByCategoryIds(
                records.stream().map(SeoCategoryEntity::getId).toList());

        List<SeoCategoryListVo> list = new ArrayList<>(records.size());
        for (SeoCategoryEntity entity : records) {
            SeoCategoryListVo vo = new SeoCategoryListVo();
            BeanUtils.copyProperties(entity, vo);
            vo.setArticleCount(countMap.getOrDefault(entity.getId(), 0));
            list.add(vo);
        }
        result.setList(list);
        return result;
    }

    @Override
    public List<SeoCategoryOptionVo> listAll() {
        List<SeoCategoryEntity> entities = list(new LambdaQueryWrapper<SeoCategoryEntity>()
                .eq(SeoCategoryEntity::getIsDeleted, SeoConstant.NOT_DELETED)
                .orderByAsc(SeoCategoryEntity::getSort)
                .orderByDesc(SeoCategoryEntity::getCreateDate)
                .orderByDesc(SeoCategoryEntity::getId));
        List<SeoCategoryOptionVo> options = new ArrayList<>(entities.size());
        for (SeoCategoryEntity entity : entities) {
            SeoCategoryOptionVo vo = new SeoCategoryOptionVo();
            BeanUtils.copyProperties(entity, vo);
            options.add(vo);
        }
        return options;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SeoSaveResultVo saveCategory(SeoCategoryBo bo) {
        checkNameAvailable(bo.getCategoryName(), null);
        String finalSlug = seoSlugService.resolveSlug(
                SeoConstant.SLUG_TYPE_CATEGORY, bo.getCategoryName(), bo.getSlug(), null);

        SeoCategoryEntity entity = new SeoCategoryEntity();
        BeanUtils.copyProperties(bo, entity);
        entity.setId(SnowflakeManager.nextValue());
        entity.setSlug(finalSlug);
        entity.setSort(bo.getSort() == null ? 0 : bo.getSort());
        entity.setDescription(bo.getDescription() == null ? "" : bo.getDescription());
        entity.setIsDeleted(SeoConstant.NOT_DELETED);
        entity.setCreateDate(LocalDateTime.now());
        entity.setUpdateDate(LocalDateTime.now());
        save(entity);

        seoSiteService.evictAll();

        return buildResult(entity.getId(), finalSlug, bo.getCategoryName(), bo.getSlug());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SeoSaveResultVo updateCategory(SeoCategoryBo bo) {
        SeoCategoryEntity existing = getById(bo.getId());
        if (existing == null || Objects.equals(existing.getIsDeleted(), SeoConstant.DELETED)) {
            throw new BusinessException("该分类不存在或已被删除");
        }
        checkNameAvailable(bo.getCategoryName(), bo.getId());
        String finalSlug = seoSlugService.resolveSlug(
                SeoConstant.SLUG_TYPE_CATEGORY, bo.getCategoryName(), bo.getSlug(), bo.getId());

        SeoCategoryEntity entity = new SeoCategoryEntity();
        BeanUtils.copyProperties(bo, entity);
        entity.setSlug(finalSlug);
        entity.setUpdateDate(LocalDateTime.now());
        updateById(entity);

        // slug 变了就记一条历史，官网据此把旧栏目地址 301 到新地址。
        // 放在 updateById 之后：更新失败时不该留下一条指向未生效新值的历史。
        // 只在真的变化时记——每次保存都记会让历史表堆满 old_slug == 当前 slug 的噪音行
        if (!Objects.equals(existing.getSlug(), finalSlug)) {
            seoSlugHistoryService.record(
                    SeoConstant.SLUG_ENTITY_CATEGORY, bo.getId(), existing.getSlug());
        }

        seoSiteService.evictAll();

        return buildResult(bo.getId(), finalSlug, bo.getCategoryName(), bo.getSlug());
    }

    @Override
    public SeoCategoryListVo info(Long id) {
        if (id == null) {
            throw new BusinessException("请选择要查看的分类");
        }
        SeoCategoryEntity entity = getById(id);
        if (entity == null || Objects.equals(entity.getIsDeleted(), SeoConstant.DELETED)) {
            throw new BusinessException("该分类不存在或已被删除");
        }
        SeoCategoryListVo vo = new SeoCategoryListVo();
        BeanUtils.copyProperties(entity, vo);
        vo.setArticleCount(countArticlesByCategoryIds(List.of(id)).getOrDefault(id, 0));
        return vo;
    }

    @Override
    public Long findIdByName(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        SeoCategoryEntity existing = getOne(new LambdaQueryWrapper<SeoCategoryEntity>()
                .select(SeoCategoryEntity::getId)
                .eq(SeoCategoryEntity::getCategoryName, trimmed)
                .eq(SeoCategoryEntity::getIsDeleted, SeoConstant.NOT_DELETED)
                .last("limit 1"));
        return existing == null ? null : existing.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long findOrCreateByName(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        Long existingId = findIdByName(trimmed);
        if (existingId != null) {
            return existingId;
        }
        SeoCategoryBo bo = new SeoCategoryBo();
        bo.setCategoryName(trimmed);
        try {
            return saveCategory(bo).getId();
        } catch (DuplicateKeyException e) {
            // 并发下对手已先插入同名分类，重查取回它的 ID。
            //
            // 这里能安全地在 catch 之后继续用事务，靠的是 saveCategory 是 this 自调用：
            // 没有内层 TransactionInterceptor 看到这个异常，因此没有任何东西把事务标记为
            // rollback-only（InnoDB 的唯一键冲突只做语句级回滚，事务本身仍可用）。
            //
            // 【勿改成走代理调用】通常「Service 内部自调用绕过了 @Transactional」是要修的 bug，
            // 但这里一旦改成代理调用，内层拦截器就会把外层事务标记为 rollback-only，
            // 下面这次查询虽能执行，提交时却会抛 UnexpectedRollbackException。
            // SeoTagServiceImpl.findOrCreateByName 里注释描述的正是那种场景。
            return findIdByName(trimmed);
        }
        // 刻意不 catch BusinessException：saveCategory 的业务校验失败（分类名超长、
        // slug 无法生成）与「并发冲突」是两回事，吞掉它会让调用方拿到 null，
        // 进而报出「分类不存在，请先创建」——与真实原因毫无关系
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Long id) {
        // for update 锁住分类行：count 与 update 之间若有别的事务新插入文章，
        // 会产生指向已删分类的孤儿文章（官网面包屑/栏目页会渲染异常）。
        //
        // 注意：只在本侧加锁是不够的。SeoArticleServiceImpl.validate 里的分类校验
        // 也必须是当前读（同样带 for update）——普通查询走一致性非锁定读，
        // 不会被这里的 X 锁阻塞，竞态依旧成立。两侧同取当前读才闭环
        SeoCategoryEntity entity = getOne(new LambdaQueryWrapper<SeoCategoryEntity>()
                .eq(SeoCategoryEntity::getId, id)
                .last("for update"));
        if (entity == null || Objects.equals(entity.getIsDeleted(), SeoConstant.DELETED)) {
            throw new BusinessException("该分类不存在或已被删除");
        }
        int articleCount = countArticlesByCategoryIds(List.of(id)).getOrDefault(id, 0);
        if (articleCount > 0) {
            throw new BusinessException(
                    "该分类下有 " + articleCount + " 篇文章，请先将这些文章调整到其他分类后再删除");
        }
        // 必须用 LambdaUpdateWrapper.set 而非 updateById：
        // MP 全局配了 logic-delete-field: isDeleted，updateById 会把逻辑删除字段从 SET 中剔除，
        // 手动 setIsDeleted 会被框架静默丢弃 —— 结果是名字改成了墓碑值但记录仍然可见。
        // ew.sqlSet 不受该过滤影响。
        update(new LambdaUpdateWrapper<SeoCategoryEntity>()
                .set(SeoCategoryEntity::getCategoryName, SeoSlugUtil.toTombstone(entity.getCategoryName(), id))
                .set(SeoCategoryEntity::getSlug, SeoSlugUtil.toTombstone(entity.getSlug(), id))
                .set(SeoCategoryEntity::getIsDeleted, SeoConstant.DELETED)
                .set(SeoCategoryEntity::getUpdateDate, LocalDateTime.now())
                .eq(SeoCategoryEntity::getId, id));
        seoSiteService.evictAll();
    }

    /**
     * 统计各分类下未删除的文章数。
     *
     * <p>统计口径<b>含已下架</b>文章——这个数字用于判断内容存量，下架只是暂时不对外，
     * 内容仍然存在。若只统计已发布，运营会误判某分类下没有内容而重复选题。
     *
     * <p>走 SeoArticleService 的 SQL group by 聚合，而不是把明细行拉回内存计数——
     * 单个分类下的文章数可能上万。
     */
    private Map<Long, Integer> countArticlesByCategoryIds(List<Long> categoryIds) {
        return seoArticleService.countByCategoryIds(categoryIds);
    }

    /**
     * 组装保存结果。
     *
     * <p>slugAppended 必须与「实际使用的 base」比较，不能与入参 slug 比较：运营留空 slug 时
     * 入参为空串，与最终值一比永远判定为「未追加」——而留空 + 拼音撞车恰恰是最常见的追加场景，
     * 那样 AC-6 要求的「运营需知情」就失效了。
     */
    private SeoSaveResultVo buildResult(Long id, String finalSlug, String name, String requestedSlug) {
        SeoSaveResultVo result = new SeoSaveResultVo();
        result.setId(id);
        result.setSlug(finalSlug);
        String base = requestedSlug != null && !requestedSlug.isBlank()
                ? SeoSlugUtil.normalize(requestedSlug)
                : SeoSlugUtil.toPinyinSlug(name);
        base = SeoSlugUtil.truncate(base, SeoConstant.MAX_SLUG_LEN_CATEGORY_TAG - 5);
        result.setSlugAppended(!base.isEmpty() && !base.equals(finalSlug));
        return result;
    }

    /** 名称唯一校验。DDL 有 uk_seo_category_name，不前置校验会让重复名称直接 500 */
    private void checkNameAvailable(String name, Long excludeId) {
        if (name == null || name.isBlank()) {
            throw new BusinessException("分类名称不能为空");
        }
        long count = count(new LambdaQueryWrapper<SeoCategoryEntity>()
                .eq(SeoCategoryEntity::getCategoryName, name.trim())
                .eq(SeoCategoryEntity::getIsDeleted, SeoConstant.NOT_DELETED)
                .ne(Objects.nonNull(excludeId), SeoCategoryEntity::getId, excludeId));
        if (count > 0) {
            throw new BusinessException("分类名称「" + name.trim() + "」已存在");
        }
    }
}
