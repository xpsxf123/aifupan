package com.jiuyu.replay.system.repository.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.system.bo.SeoTagBo;
import com.jiuyu.replay.system.bo.SeoTagListBo;
import com.jiuyu.replay.system.constant.SeoConstant;
import com.jiuyu.replay.system.entity.SeoTagEntity;
import com.jiuyu.replay.system.repository.dao.SeoTagDao;
import com.jiuyu.replay.system.repository.service.SeoArticleTagService;
import com.jiuyu.replay.system.repository.service.SeoSiteService;
import com.jiuyu.replay.system.repository.service.SeoSlugHistoryService;
import com.jiuyu.replay.system.repository.service.SeoSlugService;
import com.jiuyu.replay.system.repository.service.SeoTagService;
import com.jiuyu.replay.system.util.SeoSlugUtil;
import com.jiuyu.replay.system.vo.SeoSaveResultVo;
import com.jiuyu.replay.system.vo.SeoTagListVo;
import com.jiuyu.replay.system.vo.SeoTagOptionVo;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * SEO 标签实现。
 *
 * @author claude
 * @date 2026-08-12
 */
@Service
public class SeoTagServiceImpl extends ServiceImpl<SeoTagDao, SeoTagEntity> implements SeoTagService {

    /** 排序方式：按关联文章数倒序（默认）。原定按浏览量倒序，浏览量本期不做故改用此项 */
    private static final String ORDER_BY_COUNT = "count";

    private final SeoSlugService seoSlugService;

    private final SeoArticleTagService seoArticleTagService;

    private final SeoSlugHistoryService seoSlugHistoryService;

    /** 内容变更后让官网接口缓存立即失效 */
    private final SeoSiteService seoSiteService;

    public SeoTagServiceImpl(SeoSlugService seoSlugService,
                             SeoArticleTagService seoArticleTagService,
                             SeoSlugHistoryService seoSlugHistoryService,
                             SeoSiteService seoSiteService) {
        this.seoSlugService = seoSlugService;
        this.seoArticleTagService = seoArticleTagService;
        this.seoSlugHistoryService = seoSlugHistoryService;
        this.seoSiteService = seoSiteService;
    }

    @Override
    public PageUtils<SeoTagListVo> queryPage(SeoTagListBo bo) {
        LambdaQueryWrapper<SeoTagEntity> wrapper = new LambdaQueryWrapper<SeoTagEntity>()
                .eq(SeoTagEntity::getIsDeleted, SeoConstant.NOT_DELETED)
                .like(bo.getTagName() != null && !bo.getTagName().isBlank(), SeoTagEntity::getTagName, bo.getTagName())
                .eq(Objects.nonNull(bo.getTagStatus()), SeoTagEntity::getTagStatus, bo.getTagStatus())
                .orderByDesc(SeoTagEntity::getCreateDate)
                // id 兜底排序（雪花 ID 单调递增，等价于按创建先后）。
                // 没有它时排序键相同的行在 MySQL 里顺序不确定，翻页会重复或漏行（导入一篇多标签的文章时，
                // 那几个标签的 create_date 会落在同一秒）
                .orderByDesc(SeoTagEntity::getId);

        // 关联文章数是聚合值、不落库，无法在 SQL 层排序 + 分页。标签量级预计几十到几百，
        // 因此按 count 排序时先全量取回、聚合后内存排序再切页；按 new 排序走正常 DB 分页。
        boolean orderByCount = bo.getOrderBy() == null || ORDER_BY_COUNT.equals(bo.getOrderBy());
        if (!orderByCount) {
            return pageByDatabase(bo, wrapper);
        }
        return pageByArticleCount(bo, wrapper);
    }

    @Override
    public List<SeoTagOptionVo> listEnabled() {
        List<SeoTagEntity> entities = list(new LambdaQueryWrapper<SeoTagEntity>()
                .eq(SeoTagEntity::getIsDeleted, SeoConstant.NOT_DELETED)
                .eq(SeoTagEntity::getTagStatus, SeoConstant.STATUS_ENABLED)
                .orderByDesc(SeoTagEntity::getCreateDate)
                .orderByDesc(SeoTagEntity::getId));
        return toOptions(entities);
    }

    @Override
    public List<SeoTagOptionVo> listByIds(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<SeoTagEntity> entities = new ArrayList<>();
        for (List<Long> batch : Lists.partition(tagIds, SeoConstant.IN_BATCH_SIZE)) {
            entities.addAll(list(new LambdaQueryWrapper<SeoTagEntity>()
                    .in(SeoTagEntity::getId, batch)
                    .eq(SeoTagEntity::getIsDeleted, SeoConstant.NOT_DELETED)));
        }
        return toOptions(entities);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SeoSaveResultVo saveTag(SeoTagBo bo) {
        checkNameAvailable(bo.getTagName(), null);
        String finalSlug = seoSlugService.resolveSlug(SeoConstant.SLUG_TYPE_TAG, bo.getTagName(), bo.getSlug(), null);

        SeoTagEntity entity = new SeoTagEntity();
        BeanUtils.copyProperties(bo, entity);
        entity.setId(SnowflakeManager.nextValue());
        entity.setSlug(finalSlug);
        // 新建标签必然是 0 篇文章，因此只能是关闭态（后台反馈第 3 条）：
        // 启用一个空标签会让官网多出一个必然 404 的聚合页入口
        if (Objects.equals(bo.getTagStatus(), SeoConstant.STATUS_ENABLED)) {
            throw new BusinessException("新建标签暂无关联文章，不能直接启用；请先给文章打上该标签");
        }
        entity.setTagStatus(SeoConstant.STATUS_DISABLED);
        entity.setIsDeleted(SeoConstant.NOT_DELETED);
        entity.setCreateDate(LocalDateTime.now());
        entity.setUpdateDate(LocalDateTime.now());
        save(entity);

        seoSiteService.evictAll();

        return buildResult(entity.getId(), finalSlug, bo.getTagName(), bo.getSlug());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SeoSaveResultVo updateTag(SeoTagBo bo) {
        SeoTagEntity existing = getById(bo.getId());
        if (existing == null || Objects.equals(existing.getIsDeleted(), SeoConstant.DELETED)) {
            throw new BusinessException("该标签不存在或已被删除");
        }
        checkNameAvailable(bo.getTagName(), bo.getId());
        String finalSlug = seoSlugService.resolveSlug(SeoConstant.SLUG_TYPE_TAG, bo.getTagName(), bo.getSlug(), bo.getId());

        SeoTagEntity entity = new SeoTagEntity();
        BeanUtils.copyProperties(bo, entity);
        entity.setSlug(finalSlug);
        entity.setUpdateDate(LocalDateTime.now());
        updateById(entity);

        // slug 变了就记一条历史，官网据此把旧标签地址 301 到新地址。理由同分类
        if (!Objects.equals(existing.getSlug(), finalSlug)) {
            seoSlugHistoryService.record(SeoConstant.SLUG_ENTITY_TAG, bo.getId(), existing.getSlug());
        }

        seoSiteService.evictAll();

        return buildResult(bo.getId(), finalSlug, bo.getTagName(), bo.getSlug());
    }

    @Override
    public SeoTagListVo info(Long id) {
        if (id == null) {
            throw new BusinessException("请选择要查看的标签");
        }
        SeoTagEntity entity = getById(id);
        if (entity == null || Objects.equals(entity.getIsDeleted(), SeoConstant.DELETED)) {
            throw new BusinessException("该标签不存在或已被删除");
        }
        SeoTagListVo vo = new SeoTagListVo();
        BeanUtils.copyProperties(entity, vo);
        vo.setArticleCount(seoArticleTagService.countArticlesByTagIds(List.of(id)).getOrDefault(id, 0));
        return vo;
    }

    @Override
    public Long findIdByName(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        SeoTagEntity existing = getOne(new LambdaQueryWrapper<SeoTagEntity>()
                .select(SeoTagEntity::getId)
                .eq(SeoTagEntity::getTagName, trimmed)
                .eq(SeoTagEntity::getIsDeleted, SeoConstant.NOT_DELETED)
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
        SeoTagEntity existing = getOne(new LambdaQueryWrapper<SeoTagEntity>()
                .eq(SeoTagEntity::getTagName, trimmed)
                .eq(SeoTagEntity::getIsDeleted, SeoConstant.NOT_DELETED)
                .last("limit 1"));
        if (existing != null) {
            return existing.getId();
        }
        SeoTagBo bo = new SeoTagBo();
        bo.setTagName(trimmed);
        // 不设 tagStatus：新建标签一律落关闭态（后台反馈第 3 条）。
        // 这里原先显式设为启用，改规则后会直接撞上 saveTag 的校验、让整个批量导入失败——
        // 导入是一次几十篇的批处理，崩在这里等于整批白跑
        bo.setTagStatus(SeoConstant.STATUS_DISABLED);
        try {
            return saveTag(bo).getId();
        } catch (BusinessException | DuplicateKeyException e) {
            // 并发下对手已先插入同名标签：重查取回它的 ID 而非重试插入（幂等）。
            // 注意不能在当前事务里重试 INSERT——DuplicateKeyException 已把事务标记为
            // rollback-only，提交时会抛 UnexpectedRollbackException
            SeoTagEntity concurrent = getOne(new LambdaQueryWrapper<SeoTagEntity>()
                    .eq(SeoTagEntity::getTagName, trimmed)
                    .eq(SeoTagEntity::getIsDeleted, SeoConstant.NOT_DELETED)
                    .last("limit 1"));
            return concurrent == null ? null : concurrent.getId();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTags(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        int affected = 0;
        for (List<Long> batch : Lists.partition(ids, SeoConstant.IN_BATCH_SIZE)) {
            // name 与 slug 的墓碑值都用 SQL 的 CONCAT 就地拼，一条 UPDATE 处理整批。
            //
            // 原先是「先 SELECT 出 name/slug，再逐条 UPDATE」：删 50 个标签要 51 次往返，
            // 且两步之间有窗口——期间别人改了名字，写回的墓碑值就基于旧值了。
            //
            // 必须用 setSql / LambdaUpdateWrapper 而不是 updateBatchById：MP 全局配了
            // logic-delete-field: isDeleted，updateById/updateBatchById 会把逻辑删除字段从 SET
            // 中剔除，手动 setIsDeleted 被静默丢弃 —— 结果是名字改成墓碑值但记录仍可见，
            // 而关联已被物理清掉。ew.sqlSet 不受该过滤影响。
            affected += getBaseMapper().update(null, new LambdaUpdateWrapper<SeoTagEntity>()
                    .setSql(SeoSlugUtil.tombstoneSetSql("name"))
                    .setSql(SeoSlugUtil.tombstoneSetSql("slug"))
                    .set(SeoTagEntity::getIsDeleted, SeoConstant.DELETED)
                    .set(SeoTagEntity::getUpdateDate, now)
                    .in(SeoTagEntity::getId, batch)
                    .eq(SeoTagEntity::getIsDeleted, SeoConstant.NOT_DELETED));
        }
        if (affected == 0) {
            throw new BusinessException("所选标签不存在或已被删除，请刷新后重试");
        }
        // 标签是弱关联：删除时清理文章关联，文章本身不受影响
        seoArticleTagService.removeByTagIds(ids);
        seoSiteService.evictAll();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(List<Long> ids, Integer status) {
        // 空入参不能静默返回成功——那会让前端显示「操作成功」而实际什么都没做
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("请选择要操作的标签");
        }
        if (status == null || (status != SeoConstant.STATUS_ENABLED && status != SeoConstant.STATUS_DISABLED)) {
            throw new BusinessException("状态值不合法，仅支持 1 启用 / 0 禁用");
        }
        // 0 篇文章的标签不允许启用（后台反馈第 3 条）。
        // 校验放在后端而不是只靠前端置灰：批量启用是一次调用多个 id，
        // 前端只按当前列表页的数据判断，别人刚把文章挪走它是不知道的
        if (status == SeoConstant.STATUS_ENABLED) {
            Map<Long, Integer> counts = seoArticleTagService.countArticlesByTagIds(ids);
            List<Long> empty = ids.stream()
                    .filter(id -> counts.getOrDefault(id, 0) == 0)
                    .toList();
            if (!empty.isEmpty()) {
                throw new BusinessException(
                        "有 " + empty.size() + " 个标签暂无关联文章，不能启用；请先给文章打上标签");
            }
        }
        List<Long> validIds = ids.stream().filter(Objects::nonNull).toList();
        if (validIds.isEmpty()) {
            throw new BusinessException("请选择要操作的标签");
        }
        ids = validIds;
        LocalDateTime now = LocalDateTime.now();
        for (List<Long> batch : Lists.partition(ids, SeoConstant.IN_BATCH_SIZE)) {
            List<SeoTagEntity> updates = new ArrayList<>(batch.size());
            for (Long id : batch) {
                SeoTagEntity update = new SeoTagEntity();
                update.setId(id);
                update.setTagStatus(status);
                update.setUpdateDate(now);
                updates.add(update);
            }
            updateBatchById(updates);
        }
        seoSiteService.evictAll();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void autoDisableEmptyTags(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        List<Long> distinct = tagIds.stream().filter(Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return;
        }

        Map<Long, Integer> counts = seoArticleTagService.countArticlesByTagIds(distinct);
        List<Long> empty = distinct.stream()
                .filter(id -> counts.getOrDefault(id, 0) == 0)
                .toList();
        if (empty.isEmpty()) {
            return;
        }

        int affected = 0;
        LocalDateTime now = LocalDateTime.now();
        for (List<Long> batch : Lists.partition(empty, SeoConstant.IN_BATCH_SIZE)) {
            // 带上 tagStatus = 启用 的条件：已经是关闭态的不必再写一次，
            // 免得平白刷新 updateDate、也少一次无谓的缓存失效
            affected += getBaseMapper().update(null, new LambdaUpdateWrapper<SeoTagEntity>()
                    .set(SeoTagEntity::getTagStatus, SeoConstant.STATUS_DISABLED)
                    .set(SeoTagEntity::getUpdateDate, now)
                    .in(SeoTagEntity::getId, batch)
                    .eq(SeoTagEntity::getTagStatus, SeoConstant.STATUS_ENABLED)
                    .eq(SeoTagEntity::getIsDeleted, SeoConstant.NOT_DELETED));
        }
        if (affected > 0) {
            // 标签状态影响官网标签云与文章底部标签，必须让缓存失效
            seoSiteService.evictAll();
        }
    }

    /** 按创建时间倒序：走数据库分页 */
    private PageUtils<SeoTagListVo> pageByDatabase(SeoTagListBo bo, LambdaQueryWrapper<SeoTagEntity> wrapper) {
        com.baomidou.mybatisplus.core.metadata.IPage<SeoTagEntity> page = page(
                new com.jiuyu.replay.common.utils.Query<SeoTagEntity>().getPageNoSort(bo.getPage(), bo.getLimit()),
                wrapper);
        PageUtils<SeoTagListVo> result = new PageUtils<>(bo.getPage(), bo.getLimit(), page);
        result.setList(toListVos(page.getRecords()));
        return result;
    }

    /** 按关联文章数倒序：聚合值无法在 SQL 层排序，全量取回后内存排序再切页 */
    private PageUtils<SeoTagListVo> pageByArticleCount(SeoTagListBo bo, LambdaQueryWrapper<SeoTagEntity> wrapper) {
        List<SeoTagEntity> all = list(wrapper);
        List<SeoTagListVo> vos = toListVos(all);
        vos.sort(Comparator.comparing(SeoTagListVo::getArticleCount, Comparator.reverseOrder())
                .thenComparing(SeoTagListVo::getCreateDate, Comparator.reverseOrder()));

        int page = bo.getPage() == null || bo.getPage() < 1 ? 1 : bo.getPage();
        int limit = bo.getLimit() == null || bo.getLimit() < 1 ? 10 : bo.getLimit();
        int from = Math.min((page - 1) * limit, vos.size());
        int to = Math.min(from + limit, vos.size());

        PageUtils<SeoTagListVo> result = new PageUtils<>(vos.subList(from, to), vos.size(), limit, page);
        return result;
    }

    private List<SeoTagListVo> toListVos(List<SeoTagEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, Integer> countMap = seoArticleTagService.countArticlesByTagIds(
                entities.stream().map(SeoTagEntity::getId).toList());
        List<SeoTagListVo> vos = new ArrayList<>(entities.size());
        for (SeoTagEntity entity : entities) {
            SeoTagListVo vo = new SeoTagListVo();
            BeanUtils.copyProperties(entity, vo);
            vo.setArticleCount(countMap.getOrDefault(entity.getId(), 0));
            vos.add(vo);
        }
        return vos;
    }

    private List<SeoTagOptionVo> toOptions(List<SeoTagEntity> entities) {
        List<SeoTagOptionVo> options = new ArrayList<>(entities.size());
        for (SeoTagEntity entity : entities) {
            SeoTagOptionVo vo = new SeoTagOptionVo();
            BeanUtils.copyProperties(entity, vo);
            options.add(vo);
        }
        return options;
    }

    /**
     * 组装保存结果。
     *
     * <p>slugAppended 与「实际使用的 base」比较而非入参 slug：运营留空 slug 时入参为空串，
     * 与最终值一比永远判定为「未追加」——而留空 + 拼音撞车恰恰是最常见的追加场景。
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

    /** 名称唯一校验。DDL 有 uk_seo_tag_name，不前置校验会让重复名称直接 500 */
    private void checkNameAvailable(String name, Long excludeId) {
        if (name == null || name.isBlank()) {
            throw new BusinessException("标签名称不能为空");
        }
        long count = count(new LambdaQueryWrapper<SeoTagEntity>()
                .eq(SeoTagEntity::getTagName, name.trim())
                .eq(SeoTagEntity::getIsDeleted, SeoConstant.NOT_DELETED)
                .ne(Objects.nonNull(excludeId), SeoTagEntity::getId, excludeId));
        if (count > 0) {
            throw new BusinessException("标签名称「" + name.trim() + "」已存在");
        }
    }
}
