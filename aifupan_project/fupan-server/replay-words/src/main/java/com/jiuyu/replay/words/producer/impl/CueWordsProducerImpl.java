package com.jiuyu.replay.words.producer.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.replay.common.constant.AiEnums;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.bo.words.CueWordsBo;
import com.jiuyu.replay.generic.bo.words.CueWordsListBo;
import com.jiuyu.replay.generic.bo.words.cue.CueWordsPageBo;
import com.jiuyu.replay.generic.bo.words.cue.CueWordsQueryBo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.words.*;
import com.jiuyu.replay.words.entity.CueWordsEntity;
import com.jiuyu.replay.words.producer.CueWordsProducer;
import com.jiuyu.replay.words.repository.service.CueWordsService;
import lombok.AllArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;


/**
 * 提示词
 *
 * @author DearSil
 * @email 2437495924@qq.com
 * @date 2025-02-20 15:23:28
 */
@Service
@AllArgsConstructor
public class CueWordsProducerImpl implements CueWordsProducer {

    private final CueWordsService cueWordsService;

    @Override
    public PageUtils<CueWordsListVo> queryPage(CueWordsListBo cueWordsListBo) {

        // 通用查询条件数据
        QueryWrapper<CueWordsEntity> wrapper = getWrapper(cueWordsListBo);

        // 客户端数据数据
        wrapper = getClientWrapper(cueWordsListBo, wrapper);


        wrapper.orderByAsc("sort");

        IPage<CueWordsEntity> iPage = cueWordsService.page(new Query<CueWordsEntity>().getPageNoSort(cueWordsListBo.getPage(), cueWordsListBo.getLimit()), wrapper);

        //分页数据转换为
        PageUtils<CueWordsListVo> pageUtils = new PageUtils<>(cueWordsListBo.getPage(), cueWordsListBo.getLimit(), iPage);

        List<CueWordsEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<CueWordsListVo> vos = records.stream().map(item -> {
                CueWordsListVo cueWordsVo = new CueWordsListVo();
                BeanUtils.copyProperties(item, cueWordsVo);
                return cueWordsVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public PageUtils<CueWordsListVo> pageCueWords(CueWordsPageBo pageBo) {
        LambdaQueryWrapper<CueWordsEntity> qw = new QueryWrapper<CueWordsEntity>()
                .lambda()
            .select(CueWordsEntity::getId,
                CueWordsEntity::getTradeId,
                CueWordsEntity::getCueType,
                CueWordsEntity::getScope,
                CueWordsEntity::getScene,
                CueWordsEntity::getOutline,
                CueWordsEntity::getResourceType,
                CueWordsEntity::getApplyTo,
                CueWordsEntity::getAccountType,
                CueWordsEntity::getSyncScene,
                CueWordsEntity::getCueWord,
                CueWordsEntity::getSort,
                CueWordsEntity::getRemarks,
                CueWordsEntity::getCreateDate,
                CueWordsEntity::getUpdateDate,
                CueWordsEntity::getTenantId)
                .eq(ObjectUtil.isNotEmpty(pageBo.getTradeId()), CueWordsEntity::getTradeId, pageBo.getTradeId())
                .eq(ObjectUtil.isNotEmpty(pageBo.getCueType()), CueWordsEntity::getCueType, pageBo.getCueType())
                .eq(ObjectUtil.isNotEmpty(pageBo.getScope()), CueWordsEntity::getScope, pageBo.getScope())
                .eq(ObjectUtil.isNotEmpty(pageBo.getApplyTo()), CueWordsEntity::getApplyTo, pageBo.getApplyTo())
                .eq(ObjectUtil.isNotEmpty(pageBo.getAccountType()), CueWordsEntity::getAccountType, pageBo.getAccountType())
                .eq(ObjectUtil.isNotEmpty(pageBo.getSyncScene()), CueWordsEntity::getSyncScene, pageBo.getSyncScene())
                .in(pageBo.getTenantId() != null, CueWordsEntity::getTenantId, Arrays.asList(pageBo.getTenantId(), 0L))
                .eq(pageBo.getTenantId() == null, CueWordsEntity::getTenantId, 0L)
                .orderByDesc(pageBo.getTenantId() != null, CueWordsEntity::getTenantId)
                .orderByAsc(CueWordsEntity::getSort);
        IPage<CueWordsEntity> iPage = cueWordsService.page(new Query<CueWordsEntity>().getPageNoSort(pageBo.getPage(), pageBo.getLimit()), qw);

        //分页数据转换为
        PageUtils<CueWordsListVo> pageUtils = new PageUtils<>(pageBo.getPage(), pageBo.getLimit(), iPage);

        List<CueWordsEntity> records = iPage.getRecords();
        if (records != null && !records.isEmpty()) {
            List<CueWordsListVo> list = BeanUtil.copyToList(records, CueWordsListVo.class);
            list.forEach(item -> item.setProblem(null));
            pageUtils.setList(list);
        } else {
            pageUtils.setList(new ArrayList<>());
        }
        return pageUtils;
    }


    /**
     * 自定义获取提示词
     *
     * @param pageBo 获取参数
     *
     * @return {@link CustomizeCueWordsResponse }
     */
    @Override
    public CustomizeCueWordsResponse customizeCueWordsList(CueWordsPageBo pageBo) {
        List<CueWordsEntity> universal = buildQuery(pageBo, false);
        List<CueWordsEntity> tenantCustomize = buildQuery(pageBo, true);
        CustomizeCueWordsResponse cueWordsResponse = new CustomizeCueWordsResponse();
        cueWordsResponse.setTenantCustomize(BeanUtil.copyToList(tenantCustomize, CueWordsListVo.class));
        cueWordsResponse.setUniversal(BeanUtil.copyToList(universal, CueWordsListVo.class));
        return cueWordsResponse;
    }

    /**
     * 生成查询
     *
     * @param pageBo    bo页
     * @param customize 是否租户自定义
     *
     * @return {@link QueryWrapper<CueWordsEntity>}
     */
    private List<CueWordsEntity> buildQuery(CueWordsPageBo pageBo, boolean customize) {
        if (customize && pageBo.getTenantId() == null) {
            return List.of();
        }
        return cueWordsService.lambdaQuery()
            .select(CueWordsEntity::getId,
                CueWordsEntity::getTradeId,
                CueWordsEntity::getCueType,
                CueWordsEntity::getScope,
                CueWordsEntity::getScene,
                CueWordsEntity::getOutline,
                CueWordsEntity::getResourceType,
                CueWordsEntity::getApplyTo,
                CueWordsEntity::getAccountType,
                CueWordsEntity::getSyncScene,
                CueWordsEntity::getCueWord,
                CueWordsEntity::getSort,
                CueWordsEntity::getRemarks,
                CueWordsEntity::getCreateDate,
                CueWordsEntity::getUpdateDate,
                CueWordsEntity::getTenantId)
            .eq(!customize && ObjectUtil.isNotEmpty(pageBo.getTradeId()), CueWordsEntity::getTradeId, pageBo.getTradeId())
            .eq(customize && ObjectUtil.isNotEmpty(pageBo.getCustomizeTradeId()), CueWordsEntity::getTradeId, pageBo.getCustomizeTradeId())
            .eq(ObjectUtil.isNotEmpty(pageBo.getCueType()), CueWordsEntity::getCueType, pageBo.getCueType())
            .eq(ObjectUtil.isNotEmpty(pageBo.getScope()), CueWordsEntity::getScope, pageBo.getScope())
            .eq(ObjectUtil.isNotEmpty(pageBo.getApplyTo()), CueWordsEntity::getApplyTo, pageBo.getApplyTo())
            .eq(!customize && ObjectUtil.isNotEmpty(pageBo.getAccountType()), CueWordsEntity::getAccountType, pageBo.getAccountType())
            .eq(customize && ObjectUtil.isNotEmpty(pageBo.getCustomizeAccountType()), CueWordsEntity::getAccountType, pageBo.getCustomizeAccountType())
            .eq(ObjectUtil.isNotEmpty(pageBo.getSyncScene()), CueWordsEntity::getSyncScene, pageBo.getSyncScene())
            .eq(customize, CueWordsEntity::getTenantId, pageBo.getTenantId())
            .eq(!customize, CueWordsEntity::getTenantId, 0L)
            .orderByAsc(CueWordsEntity::getSort)
            .last("limit " + pageBo.getLimit())
            .list();
    }


    /**
     * 客户端数据获取wrapper
     * @param cueWordsListBo
     * @return
     */
    private QueryWrapper<CueWordsEntity> getClientWrapper(CueWordsListBo cueWordsListBo, QueryWrapper<CueWordsEntity> wrapper) {

        if (ObjectUtil.isEmpty(cueWordsListBo.getSourceId()) && ObjectUtil.isEmpty(cueWordsListBo.getTradeIds())) {
            return wrapper;
        }
        //构建默认排序数据
        wrapper.orderByAsc("sort");

        //当list没有数据默认使用通用行业数据
        if (ObjectUtil.isEmpty(cueWordsListBo.getTradeIds())) {
            wrapper.eq("trade_id", 1);
            return wrapper;
        }

        //当list有数据的时候，批量查询各 tradeId 的提示词数量，避免 N+1（M7 修补）
        Long tradeId = 1L;
        // 批量查询：一次 IN 查询取各 tradeId 的 count，再按原顺序找第一个有数据的行业
        List<CueWordsEntity> batchCounts = cueWordsService.list(
                new LambdaQueryWrapper<CueWordsEntity>()
                        .select(CueWordsEntity::getTradeId)
                        .in(CueWordsEntity::getTradeId, cueWordsListBo.getTradeIds())
                        .eq(CueWordsEntity::getCueType, cueWordsListBo.getCueType())
                        .eq(ObjectUtil.isNotEmpty(cueWordsListBo.getScope()), CueWordsEntity::getScope, cueWordsListBo.getScope())
                        .eq(ObjectUtil.isNotEmpty(cueWordsListBo.getApplyTo()), CueWordsEntity::getApplyTo, cueWordsListBo.getApplyTo())
        );
        Set<Long> tradeIdsWithData = batchCounts.stream()
                .map(CueWordsEntity::getTradeId)
                .collect(Collectors.toSet());
        // 按原有 tradeIds 顺序找第一个有数据的行业（保持原逻辑语义）
        for (Long id : cueWordsListBo.getTradeIds()) {
            if (tradeIdsWithData.contains(id)) {
                tradeId = id;
                break;
            }
        }

        //如果都为空，则也是调用通用的行业数据
        wrapper.eq("trade_id", tradeId);
        return wrapper;
    }

    /**
     * 构建通用查询条件方法
     *
     * @param cueWordsListBo 提示词列表对象
     * @return 查询条件
     */
    private QueryWrapper<CueWordsEntity> getWrapper(CueWordsListBo cueWordsListBo) {

        QueryWrapper<CueWordsEntity> wrapper = new QueryWrapper<>();
        if (ObjectUtil.isNotEmpty(cueWordsListBo.getKeyword())) {
            wrapper.like("cue_word", cueWordsListBo.getKeyword());
        }

        //行业id筛选
        if (ObjectUtil.isNotEmpty(cueWordsListBo.getTradeId())) {
            wrapper.eq("trade_id", cueWordsListBo.getTradeId());
        }

        //范围筛选
        if (ObjectUtil.isNotEmpty(cueWordsListBo.getScope())) {
            wrapper.eq("scope", cueWordsListBo.getScope());
        }

        //类型筛选
        if (ObjectUtil.isNotEmpty(cueWordsListBo.getCueType())) {
            wrapper.eq("cue_type", cueWordsListBo.getCueType());
        }

        //提示词用于：0：单个分析，1：对比分析
        if (ObjectUtil.isNotEmpty(cueWordsListBo.getApplyTo())) {
            wrapper.eq("apply_to", cueWordsListBo.getApplyTo());
        }
        //同步场景筛选
        if (ObjectUtil.isNotEmpty(cueWordsListBo.getSyncScene())) {
            wrapper.and(w ->
                    w.eq("sync_scene", cueWordsListBo.getSyncScene())
                            .eq("apply_to", 1)
            );
        }
        //账户类型筛选
        if (ObjectUtil.isNotEmpty(cueWordsListBo.getAccountType())) {
            wrapper.and(w ->
                    w.eq("account_type", cueWordsListBo.getAccountType())
                            .eq("apply_to", 0)
            );
        }

        // 查询类型 过滤租户
        switch (cueWordsListBo.getQueryType()) {
            case 0:
                wrapper.eq("tenant_id",0);
                break;
            case 1:
                // 无租户
                if (cueWordsListBo.getTenantId() == null) {
                    wrapper.gt("tenant_id", 0);
                } else {
                    wrapper.eq("tenant_id", cueWordsListBo.getTenantId());
                }
                break;
            case 2:
                if (cueWordsListBo.getTenantId() == null) {
                    cueWordsListBo.setTenantId(0L);
                }
                wrapper.in("tenant_id", List.of(cueWordsListBo.getTenantId(), 0L));
                break;
        }

        return wrapper;
    }

    @Override
    public CueWordsInfoVo info(Long id) {

        CueWordsEntity cueWordsEntity = cueWordsService.getById(id);
        if(cueWordsEntity != null) {
            CueWordsInfoVo cueWordsInfoVo = new CueWordsInfoVo();
            BeanUtils.copyProperties(cueWordsEntity, cueWordsInfoVo);
            return cueWordsInfoVo;
        }

        return null;
    }

    /**
     * 新增提示词
     * @param cueWordsBo 提示词对象
     * @return
     */
     public CueWordsInfoVo save(CueWordsBo cueWordsBo) {

         CueWordsEntity cueWordsEntity = new CueWordsEntity();
         BeanUtils.copyProperties(cueWordsBo, cueWordsEntity);
         cueWordsEntity.setId(SnowflakeManager.nextValue());
         cueWordsEntity.setCreateDate(new Date());
         cueWordsEntity.setUpdateDate(new Date());
         if (cueWordsEntity.getTenantId() == null) {
             cueWordsEntity.setTenantId(0L);
         }
         if (cueWordsEntity.getAnalysisType() == null) {
             cueWordsEntity.setAnalysisType(0);
         }

         cueWordsService.save(cueWordsEntity);

         CueWordsInfoVo cueWordsInfoVo = new CueWordsInfoVo();
         BeanUtils.copyProperties(cueWordsEntity, cueWordsInfoVo);

         return cueWordsInfoVo;
     }

    /**
     * 修改提示词
     * @param cueWordsBo 提示词对象
     * @return
     */
    public void update(CueWordsBo cueWordsBo) {

        CueWordsEntity cueWordsEntity = new CueWordsEntity();
        BeanUtils.copyProperties(cueWordsBo, cueWordsEntity);
        cueWordsEntity.setUpdateDate(new Date());

        cueWordsService.updateById(cueWordsEntity);
    }

    /**
     * 删除提示词
     * @param id 提示词id
     * @return
     */
    public void deleteById(Long id) {

        cueWordsService.removeById(id);
    }

    @Override
    public CueWordsInfoVo importantCueWordsByTradeId(List<TradeInfoVo> tradeList) {

        if (ObjectUtil.isNotEmpty(tradeList)){

            for (TradeInfoVo tradeInfoVo : tradeList) {

                CueWordsEntity words = cueWordsService.getOne(new LambdaUpdateWrapper<CueWordsEntity>()
                        .eq(CueWordsEntity::getTradeId, tradeInfoVo.getId())
                        .eq(CueWordsEntity::getCueType, AiEnums.askType.IMPORTANT_SCREENSHOT.getCode())
                        .last("limit 1")
                );

                if (words != null){
                    return BeanUtil.copyProperties(words, CueWordsInfoVo.class);
                }
            }

        }

        return null;
    }

    @Override
    public List<CueWordsInfoVo> listByIds(List<Long> ids) {
        List<CueWordsEntity> cueWordsEntities = cueWordsService.listByIds(ids);
        if (ObjectUtil.isNotEmpty(cueWordsEntities)){
            return BeanUtil.copyToList(cueWordsEntities, CueWordsInfoVo.class);
        }
        return new ArrayList<>();
    }

    @Override
    public long countByParams(Long tradeId, CueWordsPageBo pageBo) {
        return cueWordsService.lambdaQuery()
                .eq(CueWordsEntity::getTradeId, tradeId)
                .eq(CueWordsEntity::getCueType, pageBo.getCueType())
                .eq(CueWordsEntity::getScope, pageBo.getScope())
                .eq(CueWordsEntity::getApplyTo, pageBo.getApplyTo())
                .eq(ObjectUtil.isNotEmpty(pageBo.getAccountType()), CueWordsEntity::getAccountType, pageBo.getAccountType())
                .eq(ObjectUtil.isNotEmpty(pageBo.getSyncScene()), CueWordsEntity::getSyncScene, pageBo.getSyncScene())
                .count();
    }

    @Override
    public Map<String, Integer> countByParamsBatch(List<Long> tradeIds, CueWordsPageBo pageBo) {
        // 构建查询条件
        return cueWordsService.list(new QueryWrapper<CueWordsEntity>()
                        .select("trade_id", "account_type", "count(1) as sort")
                        .lambda()
                        .in(CueWordsEntity::getTradeId, tradeIds)
                        .eq(CueWordsEntity::getCueType, pageBo.getCueType())
                        .eq(CueWordsEntity::getScope, pageBo.getScope())
                        .eq(CueWordsEntity::getApplyTo, pageBo.getApplyTo())
                        .eq(ObjectUtil.isNotEmpty(pageBo.getSyncScene()), CueWordsEntity::getSyncScene, pageBo.getSyncScene())
                        .groupBy(CueWordsEntity::getTradeId, CueWordsEntity::getAccountType)
                )
                .stream()
                .collect(Collectors.toMap(item -> item.getTradeId() + "_" + item.getAccountType(), item -> item.getSort()));
    }

    /**
     * 批量获取多个行业提示词的条数
     *
     * @param tradeIds 行业ID列表
     * @param pageBo   获取参数
     *
     * @return 行业ID到计数的映射
     */
    @Override
    public Map<Long, Map<String, Integer>> countByTenantParamsBatch(List<Long> tradeIds, CueWordsPageBo pageBo) {
        // 构建查询条件
        return cueWordsService.list(new QueryWrapper<CueWordsEntity>()
                .select("tenant_id", "trade_id", "account_type", "count(1) as sort")
                .lambda()
                .in(CueWordsEntity::getTradeId, tradeIds)
                .eq(CueWordsEntity::getCueType, pageBo.getCueType())
                .eq(CueWordsEntity::getScope, pageBo.getScope())
                .in(CueWordsEntity::getTenantId, List.of(0, pageBo.getTenantId() == null ? 0 : pageBo.getTenantId()))
                .eq(CueWordsEntity::getApplyTo, pageBo.getApplyTo())
                .eq(ObjectUtil.isNotEmpty(pageBo.getSyncScene()), CueWordsEntity::getSyncScene, pageBo.getSyncScene())
                .groupBy(CueWordsEntity::getTenantId, CueWordsEntity::getTradeId, CueWordsEntity::getAccountType)
            )
            .stream().collect(Collectors.groupingBy(CueWordsEntity::getTenantId, Collectors.toMap(item -> item.getTradeId() + "_" + item.getAccountType(), item -> item.getSort())));
    }

    /**
     * 按行业 id 集合 + {@link CueWordsQueryBo} 取有效提示词列表（含 problem 字段 + isDeleted=0 过滤）。
     *
     * <p>命中策略由 API 层在内存中决定（按 tradeIds 顺序找第一个有数据的行业）。
     * 本方法仅做一次 IN 查询；SQL 已按 tenant_id DESC, sort ASC 排序，
     * 同行业层级内租户定制（tenant_id = bo.tenantId）天然排在通用（tenant_id = 0）之前，
     * sort 最小一条天然排首位。</p>
     *
     * @param tradeIds 候选行业 id 集合（已含父链 + 兜底 1L）
     * @param bo       查询入参，cueType 必填
     * @return 含 problem 字段的提示词列表
     */
    @Override
    public List<CueWordsInfoVo> listByTradeIdsAndQuery(List<Long> tradeIds, CueWordsQueryBo bo) {
        if (tradeIds == null || tradeIds.isEmpty()) {
            return Collections.emptyList();
        }
        // tenantId 非 null 且非 0 时才查租户定制 + 通用；否则只查通用，行为同重构前
        boolean hasTenantCustomize = bo.getTenantId() != null && bo.getTenantId() != 0L;
        List<CueWordsEntity> list = cueWordsService.lambdaQuery()
                .in(CueWordsEntity::getTradeId, tradeIds)
                .eq(CueWordsEntity::getCueType, bo.getCueType())
                .eq(ObjectUtil.isNotEmpty(bo.getScope()), CueWordsEntity::getScope, bo.getScope())
                .eq(ObjectUtil.isNotEmpty(bo.getApplyTo()), CueWordsEntity::getApplyTo, bo.getApplyTo())
                .eq(ObjectUtil.isNotEmpty(bo.getAccountType()), CueWordsEntity::getAccountType, bo.getAccountType())
                .eq(ObjectUtil.isNotEmpty(bo.getSyncScene()), CueWordsEntity::getSyncScene, bo.getSyncScene())
                .in(hasTenantCustomize, CueWordsEntity::getTenantId, Arrays.asList(bo.getTenantId(), 0L))
                .eq(!hasTenantCustomize, CueWordsEntity::getTenantId, 0L)
                .eq(CueWordsEntity::getIsDeleted, 0)
                .orderByDesc(hasTenantCustomize, CueWordsEntity::getTenantId)
                .orderByAsc(CueWordsEntity::getSort)
                .list();
        return BeanUtil.copyToList(list, CueWordsInfoVo.class);
    }

    /**
     * 查询行业预设提示词
     *
     * @param tradeIds    行业ID
     * @param cueType     ai助手类型（场景）
     * @param applyTo     提示词用于 0:单个分析，1:对比分析
     * @param accountType 账号归属类型 0:自有账号，1:同行账号
     * @param limit       返回条数
     *
     * @return {@link List }<{@link CueWordsNameVo }>
     */
    @Override
    public List<CueWordsNameVo> listTradeWordNames(Collection<Long> tradeIds, Integer cueType, Integer applyTo, Integer accountType, Integer syncScene, int limit) {
        if (EmptyUtil.isEmpty(tradeIds) || cueType == null) {
            return List.of();
        }
        List<Long> trade = new ArrayList<>(tradeIds);
        trade.add(0L);
        trade.add(1L);
        return cueWordsService.lambdaQuery()
            .select(CueWordsEntity::getId, CueWordsEntity::getOutline, CueWordsEntity::getSort)
            .in(CueWordsEntity::getTradeId, trade)
            .eq(CueWordsEntity::getCueType, cueType)
            .eq(syncScene != null, CueWordsEntity::getSyncScene, syncScene)
            .eq(applyTo != null, CueWordsEntity::getApplyTo, applyTo)
            .eq(accountType != null && (!Objects.equals(applyTo, 1)), CueWordsEntity::getAccountType, accountType)
            .eq(CueWordsEntity::getIsDeleted, 0)
            .eq(CueWordsEntity::getTenantId, 0L)
            .orderByDesc(CueWordsEntity::getTradeId)
            .last("limit " + limit)
            .list()
            .stream()
            .sorted(Comparator.comparingInt(CueWordsEntity::getSort))
            // 把 outline 当 cueWord 用 因为之前讨论时 老板说反了
            .map(item -> new CueWordsNameVo(item.getId(), item.getOutline()))
            .collect(Collectors.toList());
    }


    /**
     * 查询提示词摘要
     *
     * @param id 提示词ID
     *
     * @return {@link CueWordsDescVo }
     */
    @Override
    public CueWordsDescVo getWordDesc(long id) {
        CueWordsEntity cueWords = cueWordsService.lambdaQuery()
            .select(CueWordsEntity::getId, CueWordsEntity::getCueWord, CueWordsEntity::getOutline)
            .eq(CueWordsEntity::getId, id)
            .last("limit 1")
            .one();
        if (cueWords != null) {
            // 把 outline 当 cueWord 用 因为之前讨论时 老板说反了
            return new CueWordsDescVo(cueWords.getId(), cueWords.getOutline(), cueWords.getCueWord());
        }
        return null;
    }
}

