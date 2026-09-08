package com.jiuyu.replay.words.producer.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.lang.tree.TreeUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.framework.function.BatchQuery;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.replay.common.constant.Constant;
import com.jiuyu.replay.common.utils.BeanConvertUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.words.TradeInfoVo;
import com.jiuyu.replay.generic.vo.words.TradeListVo;
import com.jiuyu.replay.generic.vo.words.TradeVo;
import com.jiuyu.replay.words.bo.TradeBo;
import com.jiuyu.replay.words.bo.TradeListBo;
import com.jiuyu.replay.words.entity.CueWordsEntity;
import com.jiuyu.replay.words.entity.SensitiveWordsEntity;
import com.jiuyu.replay.words.entity.TradeEntity;
import com.jiuyu.replay.words.enums.TradeEnum;
import com.jiuyu.replay.words.producer.TradeProducer;
import com.jiuyu.replay.words.repository.service.CruxWordsService;
import com.jiuyu.replay.words.repository.service.CueWordsService;
import com.jiuyu.replay.words.repository.service.SensitiveWordsService;
import com.jiuyu.replay.words.repository.service.TradeService;
import com.jiuyu.replay.words.vo.TradeSimpleTreeVo;
import com.jiuyu.replay.words.vo.TradeTreeVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


/**
 * 行业
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-06-20 16:20:06
 */
@Service
public class TradeProducerImpl implements TradeProducer {

    @Resource
    private TradeService tradeService;
    @Resource
    private CruxWordsService cruxWordsService;
    @Resource
    private SensitiveWordsService sensitiveWordsService;

    @Resource
    private CueWordsService cueWordsService;

    @Resource
    private TradeRankAnchorProducer tradeRankAnchorProducer;



    @Override
    public PageUtils<TradeListVo> queryPage(TradeListBo tradeListBo) {
        QueryWrapper<TradeEntity> wrapper = new QueryWrapper<>();
        if (!StringUtils.isEmpty(tradeListBo.getKeyword())) {
            wrapper.like("name", tradeListBo.getKeyword());
        }

        IPage<TradeEntity> iPage = tradeService.page(new Query<TradeEntity>().getPage(tradeListBo.getPage(), tradeListBo.getLimit()), wrapper);

        PageUtils<TradeListVo> pageUtils = new PageUtils<>(tradeListBo.getPage(), tradeListBo.getLimit(), iPage);

        List<TradeEntity> records = iPage.getRecords();
        if (records != null && records.size() > 0) {
            List<TradeListVo> vos = records.stream().map(item -> {
                TradeListVo tradeVo = new TradeListVo();
                BeanUtils.copyProperties(item, tradeVo);
                return tradeVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public TradeInfoVo info(Long id) {

        TradeEntity tradeEntity = tradeService.getById(id);
        if (tradeEntity != null) {
            TradeInfoVo tradeInfoVo = new TradeInfoVo();
            BeanUtils.copyProperties(tradeEntity, tradeInfoVo);
            return tradeInfoVo;
        }

        return null;
    }

    /**
     * 新增行业
     *
     * @param tradeBo 行业对象
     * @return
     */
    public TradeInfoVo save(TradeBo tradeBo) {

        TradeEntity tradeEntity = new TradeEntity();
        BeanUtils.copyProperties(tradeBo, tradeEntity);
        tradeEntity.setId(SnowflakeManager.nextValue());
        tradeEntity.setCreateDate(new Date());
        tradeEntity.setUpdateDate(new Date());

        tradeService.save(tradeEntity);

        TradeInfoVo tradeInfoVo = new TradeInfoVo();
        BeanUtils.copyProperties(tradeEntity, tradeInfoVo);
        tradeInfoVo.setModelName(tradeBo.getModelName());
        tradeInfoVo.setModelType(tradeBo.getModelType());
        return tradeInfoVo;
    }

    /**
     * 修改行业
     *
     * @param tradeBo 行业对象
     * @return
     */
    public void update(TradeBo tradeBo) {

        TradeEntity tradeEntity = new TradeEntity();
        BeanUtils.copyProperties(tradeBo, tradeEntity);
        tradeEntity.setUpdateDate(new Date());
        tradeService.updateById(tradeEntity);
    }

    /**
     * 删除行业
     *
     * @param id 行业id
     * @return
     */
    public void deleteById(Long id) {

        tradeService.removeById(id);
    }

    @Override
    public List<TradeTreeVo> listTree(Integer childrenNotNull) {
        BatchQuery<Long, TradeEntity> batchQuery = new BatchQuery<>((limit, idx) -> {
            return tradeService.lambdaQuery()
                .gt(idx != null, TradeEntity::getId, idx)
                .last("limit " + limit)
                .list();
        }, TradeEntity::getId);
        // 获取所有行业
        // List<TradeEntity> tradeEntities = this.tradeService.list(new QueryWrapper<TradeEntity>().orderByAsc("sort"));
        List<TradeEntity> tradeEntities = batchQuery.get().stream().sorted(Comparator.comparing(TradeEntity::getSort)).toList();
        // 获取所有关键词(用于统计每个行业的关键词数量)
//        List<CruxWordsEntity> cruxWordsEntities = this.cruxWordsService.list(new QueryWrapper<CruxWordsEntity>().select("id", "trade_id"));
        // 获取所有敏感词(用于统计每个行业的敏感词数量)
        List<SensitiveWordsEntity> sensitiveWordsEntities = this.sensitiveWordsService.list(new QueryWrapper<SensitiveWordsEntity>().select("id", "trade_id", "words_type"));
        //获取所有的提示词
        List<CueWordsEntity> cueWordsEntities = this.cueWordsService.list(new QueryWrapper<CueWordsEntity>().select("id", "trade_id", "cue_type", "scope"));

        // 获取所有有效的叶子节点行业ID（用于判断是否是行业热榜有效行业）
        List<Long> leafTradeIds = tradeRankAnchorProducer.calcLeafTrade(tradeEntities);
        Set<Long> validLeafTradeIdSet = new HashSet<>(leafTradeIds);

        // 去查询所有有效行业热榜的数量（时间范围：最近30天，SQL中使用 DATE_SUB(NOW(), INTERVAL 30 DAY)）
        // checkRankEnabled = false：不检查 rank_enabled（后台管理系统调用）
        Map<Long, Long> tradeCountMap = tradeRankAnchorProducer.countTradeAnchor(true, LocalDateTime.now().minusDays(30), null, null, validLeafTradeIdSet);

        if (tradeEntities != null && tradeEntities.size() > 0) {
            List<TradeTreeVo> tradeTreeVos = tradeEntities.stream().filter((item) -> item.getParentId() == 0)
                    .map(item -> {
                        TradeTreeVo tradeTreeVo = new TradeTreeVo();
                        BeanUtils.copyProperties(item, tradeTreeVo);
                        // 封装子菜单
                        tradeTreeVo.setChildren(getChildrenMenu(tradeTreeVo, tradeEntities, sensitiveWordsEntities, cueWordsEntities, childrenNotNull, validLeafTradeIdSet, tradeCountMap));
                        // 封装关键词和敏感词数量
                        tradeTreeVo.setCruxNum(0);
                        tradeTreeVo.setSensitiveNum(0);
                        tradeTreeVo.setCueNum(0);
                        tradeTreeVo.setOpeFullcueNum(0);
                        tradeTreeVo.setOpeParCueNum(0);
                        tradeTreeVo.setVioFullCueNum(0);
                        tradeTreeVo.setVioParCueNum(0);
                        // 设置是否是行业热榜有效行业
                        tradeTreeVo.setIsValidRankTrade(leafTradeIds.contains(item.getId()) ? 1 : 0);
                        // 设置行业热榜数量
                        tradeTreeVo.setTradeRankCount(tradeCountMap.getOrDefault(item.getId(), 0L));
                        if (sensitiveWordsEntities != null && sensitiveWordsEntities.size() > 0) {
                            for (SensitiveWordsEntity sensitiveWordsEntity : sensitiveWordsEntities) {
                                if (sensitiveWordsEntity.getTradeId().equals(tradeTreeVo.getId())) {
                                    if (sensitiveWordsEntity.getWordsType() == 0) {
                                        tradeTreeVo.setSensitiveNum(tradeTreeVo.getSensitiveNum() + 1);
                                    } else if (sensitiveWordsEntity.getWordsType() == 1) {
                                        tradeTreeVo.setCruxNum(tradeTreeVo.getCruxNum() + 1);
                                    }
                                }
                            }
                        }

                        //提示词数量处理
                        if (cueWordsEntities != null && cueWordsEntities.size() > 0) {
                            for (CueWordsEntity cueWordsEntity : cueWordsEntities) {
                                //当前行业的提示词
                                if (cueWordsEntity.getTradeId().equals(tradeTreeVo.getId())) {
                                    tradeTreeVo.setCueNum(tradeTreeVo.getCueNum() + 1);
                                    //判断提示词类型 这个地方暂时用魔术值 提示词类型 0: 运营提示词，1:违规提示词
                                    //判断提示词的范围 范围 0:全文，1:段落
                                    //运营全文提示词
                                    if (cueWordsEntity.getCueType() == 0 && cueWordsEntity.getScope() == 0)
                                        tradeTreeVo.setOpeFullcueNum(tradeTreeVo.getOpeFullcueNum() + 1);
                                    //运营段落提示词
                                    if (cueWordsEntity.getCueType() == 0 && cueWordsEntity.getScope() == 1)
                                        tradeTreeVo.setOpeParCueNum(tradeTreeVo.getOpeParCueNum() + 1);
                                    //违规全文提示词
                                    if (cueWordsEntity.getCueType() == 1 && cueWordsEntity.getScope() == 0)
                                        tradeTreeVo.setVioFullCueNum(tradeTreeVo.getVioFullCueNum() + 1);
                                    //违规段落提示词
                                    if (cueWordsEntity.getCueType() == 1 && cueWordsEntity.getScope() == 1)
                                        tradeTreeVo.setVioParCueNum(tradeTreeVo.getVioParCueNum() + 1);
                                }
                            }
                        }

                        return tradeTreeVo;
                    }).toList();

            return tradeTreeVos;
        }
        return null;
    }

    @Override
    public List<Tree<Long>> listTreeTrade() {
        // 获取所有行业
        List<TradeEntity> tradeEntities = this.tradeService.list(new QueryWrapper<TradeEntity>().orderByAsc("sort"));
        return TreeUtil.build(tradeEntities, 0L, (t, c) -> {
            c.setId(t.getId());
            c.setParentId(t.getParentId());
            c.setWeight(t.getSort());
            c.setName(t.getName());
        });
    }

    @Override
    public List<TradeSimpleTreeVo> listSimpleTree(Integer childrenNotNull) {
        // 获取所有行业
        List<TradeEntity> tradeEntities = this.tradeService.list(new QueryWrapper<TradeEntity>().orderByAsc("sort"));

        if (tradeEntities != null && tradeEntities.size() > 0) {
            // 先获取父行业
            List<TradeSimpleTreeVo> tradeTreeVos = tradeEntities.stream().filter(item -> item.getParentId() == 0).map(item -> {
                TradeSimpleTreeVo tradeTreeVo = new TradeSimpleTreeVo();
                BeanUtils.copyProperties(item, tradeTreeVo);
                tradeTreeVo.setChildren(getSimpleChildrenMenu(tradeTreeVo, tradeEntities, childrenNotNull));
                return tradeTreeVo;
            }).collect(Collectors.toList());

            return tradeTreeVos;
        }
        return null;
    }

    /**
     * 返回当前行业的子行业（简化版）
     *
     * @param tradeTreeVo         行业
     * @param tradeEntities       全部行业列表
     * @param childrenNotNull     当没有子行业时，子行业列表是否返回空集合 0：直接返回null 1：返回空集合
     * @return
     */
    private List<TradeSimpleTreeVo> getSimpleChildrenMenu(TradeSimpleTreeVo tradeTreeVo, List<TradeEntity> tradeEntities, Integer childrenNotNull) {
        List<TradeSimpleTreeVo> childrenTradeTreeVos = tradeEntities.stream().filter(item -> item.getParentId().equals(tradeTreeVo.getId())).map(item -> {
            TradeSimpleTreeVo childrenTradeTreeVo = new TradeSimpleTreeVo();
            BeanUtils.copyProperties(item, childrenTradeTreeVo);
            childrenTradeTreeVo.setChildren(getSimpleChildrenMenu(childrenTradeTreeVo, tradeEntities, childrenNotNull));
            return childrenTradeTreeVo;
        }).toList();

        if (childrenTradeTreeVos.size() > 0) {
            return childrenTradeTreeVos;
        } else {
            if (childrenNotNull != null && childrenNotNull == 1) {
                return new ArrayList<>();
            }
            return null;
        }
    }

    @Override
    public List<TradeListVo> listByIds(Collection<Long> tradeIds) {

        if (tradeIds != null && tradeIds.size() > 0) {
            List<TradeEntity> tradeEntities = this.tradeService.listByIds(tradeIds);
            if (tradeEntities != null && tradeEntities.size() > 0) {
                List<TradeListVo> tradeListVos = tradeEntities.stream().map(item -> {
                    TradeListVo tradeListVo = new TradeListVo();
                    BeanUtils.copyProperties(item, tradeListVo);
                    return tradeListVo;
                }).toList();
                return tradeListVos;
            }
        }

        return new ArrayList<>();
    }

    /**
     * 根据行业名称查询
     *
     * @param tradeName
     * @return
     */
    @Override
    public List<Long> selectByName(String tradeName) {
        QueryWrapper<TradeEntity> qw = new QueryWrapper<>();
        qw.like("name", tradeName);
        List<TradeEntity> list = tradeService.list(qw);
        if (list != null && list.size() > 0) {
            return list.stream().map(TradeEntity::getId).collect(Collectors.toList());
        }
        return null;
    }

    @Override
    public List<TradeVo> listAll() {

        List<TradeEntity> tradeEntities = this.tradeService.list();

        if (tradeEntities != null && tradeEntities.size() > 0) {

            List<TradeVo> tradeVos = tradeEntities.stream().map(item -> {
                TradeVo tradeVo = new TradeVo();
                BeanUtils.copyProperties(item, tradeVo);
                return tradeVo;
            }).toList();

            return tradeVos;
        }

        return null;
    }

    @Override
    public void deleteTradeModelId(Long tradeId) {
        TradeEntity byId = tradeService.getById(tradeId);
        byId.setTradeModelId(0L);
        tradeService.updateById(byId);
    }

    @Override
    public TradeBo getTradeByModelId(Long modelId) {
        TradeEntity tradeModelId = tradeService.getOne(new QueryWrapper<TradeEntity>().eq("trade_model_id", modelId));
        TradeBo tradeBo = new TradeBo();
        if (tradeModelId != null) {
            BeanUtils.copyProperties(tradeModelId, tradeBo);
        }
        return tradeBo;
    }

    @Override
    public List<TradeInfoVo> listByDefaultGeneralModelId(Long modelId) {

        List<TradeEntity> tradeEntities = this.tradeService.list(new QueryWrapper<TradeEntity>().eq("default_general_model_id", modelId));
        return generalEntitiesToVos(tradeEntities);
    }

    /**
     * 通用行业集合转换成vo集合
     *
     * @param tradeEntities
     * @return
     */
    private List<TradeInfoVo> generalEntitiesToVos(List<TradeEntity> tradeEntities) {
        if (tradeEntities != null && tradeEntities.size() > 0) {
            List<TradeInfoVo> vos = tradeEntities.stream().map(item -> {
                TradeInfoVo tradeInfoVo = new TradeInfoVo();
                BeanUtils.copyProperties(item, tradeInfoVo);
                return tradeInfoVo;
            }).collect(Collectors.toList());
            return vos;
        }
        return null;
    }

    /**
     * 根据当前行业id来获取父行业集合
     *
     * @param tradeId
     * @param withGeneral
     * @return
     */
    @Override
    public List<TradeInfoVo> listParentsByTradeId(Long tradeId, Integer withGeneral) {

        List<TradeEntity> tradeEntities = getParents(tradeId, new ArrayList<>());

        //如果需要通用数据，将通用数据展示出来
        if (withGeneral.equals(Constant.GeneralEnum.GENERAL_YES.getCode())) {
            // 判断是否已经有通用行业数据
            if (ObjectUtil.isEmpty(tradeEntities) || tradeEntities.stream().noneMatch(tradeEntity -> tradeEntity.getId().equals(1L))) {
                tradeEntities.add(this.tradeService.getById(1));
            }
        }

        return generalEntitiesToVos(tradeEntities);
    }

    /**
     * 根据行业id集合来获取父行业集合
     *
     * @param tradeIds
     * @param withGeneral
     * @return
     */
    @Override
    public List<TradeInfoVo> listParentsByTradeIds(List<Long> tradeIds, Integer withGeneral) {

        // 创建一个空的结果列表
        List<TradeEntity> tradeEntities = new ArrayList<>();

        // 遍历 tradeIds 列表
        for (Long tradeId : tradeIds) {
            // 调用 listParentsByTradeId 方法获取父行业列表
            List<TradeEntity> parents = getParents(tradeId, new ArrayList<>());
            // 如果结果不为空，则将其添加到结果列表中
            if (parents != null) {
                tradeEntities.addAll(parents);
            }
        }

        // 去重
        tradeEntities = tradeEntities.stream().distinct().collect(Collectors.toList());

        //如果需要通用数据，将通用数据展示出来
        if (withGeneral.equals(Constant.GeneralEnum.GENERAL_YES)) {
            tradeEntities.add(this.tradeService.getById(1));
        }

        return generalEntitiesToVos(tradeEntities);
    }

    @Override
    public String getById(Long tradeId) {
        TradeEntity byId = this.tradeService.getById(tradeId);
        if (byId != null) {
            return byId.getName();
        }
        return null;
    }

    /**
     * 获取当前行业id以及子行业
     *
     * @param aLong
     * @return
     */
    @Override
    public List<Long> getChildById(Long aLong) {
        Set<Long> tradeIds = new ConcurrentSkipListSet<>();
        tradeIds.add(aLong);
        //设置允许递归次数
        selectChild(Collections.singletonList(aLong), tradeIds, new AtomicInteger(8));
        // 最终返回List
        return new ArrayList<>(tradeIds);
    }

    @Override
    public List<TradeInfoVo> listChildrenByTradeId(Long tradeId, boolean containerSelf) {

        List<TradeEntity> tradeEntities = this.tradeService.list();

        if (tradeEntities != null && tradeEntities.size() > 0) {
            List<TradeEntity> resultList = new ArrayList<>();

            // 添加当前行业进集合
            if (containerSelf) {
                for (TradeEntity tradeEntity : tradeEntities) {
                    if (tradeEntity.getId().equals(tradeId)) {
                        resultList.add(tradeEntity);
                        break;
                    }
                }
            }

            if (tradeId.equals(TradeEnum.GENERAL_TRADE_ID.getTradeId())) {
                // 当前是通用行业，添加所有行业
                for (TradeEntity tradeEntity : tradeEntities) {
                    if (!tradeEntity.getId().equals(TradeEnum.GENERAL_TRADE_ID.getTradeId())) {
                        resultList.add(tradeEntity);
                    }
                }
                return BeanConvertUtils.convertList(resultList, TradeInfoVo.class);
            }

            Map<Long, List<TradeEntity>> parentIdMap = new HashMap<>();
            for (TradeEntity trade : tradeEntities) {
                parentIdMap.computeIfAbsent(trade.getParentId(), k -> new ArrayList<>()).add(trade);
            }

            // 递归添加所有子行业
            addAllChildren(tradeId, parentIdMap, resultList);

            if (resultList.size() > 0) {
                return BeanConvertUtils.convertList(resultList, TradeInfoVo.class);
            }
        }

        return new ArrayList<>();
    }

    @Override
    public List<TradeVo> listTradeByIds(List<Long> ids) {
        if (CollectionUtil.isEmpty(ids)) {
            return List.of();
        }
        List<TradeEntity> tradeEntityList = tradeService.listByIds(ids);
        if (CollectionUtil.isEmpty(tradeEntityList)) {
            return List.of();
        }
        return BeanConvertUtils.convertList(tradeEntityList, TradeVo.class);
    }

    /**
     * 根据行业id获取名称
     *
     * @param tradeIds ID
     *
     * @return {@link Map }<{@link Long }, {@link String }>
     */
    @Override
    public Map<Long, String> getTradeNames(List<Long> tradeIds) {
        if (EmptyUtil.isEmpty(tradeIds)) {
            return Map.of();
        }
        return tradeService.lambdaQuery()
            .select(TradeEntity::getId, TradeEntity::getName)
            .in(TradeEntity::getId, tradeIds)
            .list().stream()
            .collect(Collectors.toMap(TradeEntity::getId, TradeEntity::getName));
    }

    /**
     * 递归添加子行业到集合
     *
     * @param tradeId     当前行业id
     * @param parentIdMap 行业map
     * @param resultList  结果集合
     */
    private void addAllChildren(Long tradeId, Map<Long, List<TradeEntity>> parentIdMap, List<TradeEntity> resultList) {
        List<TradeEntity> childrenList = parentIdMap.get(tradeId);
        if (childrenList != null && childrenList.size() > 0) {
            for (TradeEntity tradeEntity : childrenList) {
                resultList.add(tradeEntity);
                addAllChildren(tradeEntity.getId(), parentIdMap, resultList);
            }
        }
    }


    /**
     * 查询子行业
     *
     * @param parentIds
     * @param results
     */
    private void selectChild(List<Long> parentIds, Set<Long> results, AtomicInteger depth) {
        if (parentIds == null || parentIds.isEmpty() || depth.decrementAndGet() <= 0) {
            return;
        }
        List<Long> childIds = new ArrayList<>(tradeService.lambdaQuery()
                .in(TradeEntity::getParentId, parentIds)
                .select(TradeEntity::getId)
                .list()
                .stream()
                .map(TradeEntity::getId)
                .toList());
        // 只添加未出现过的ID，防止重复和死循环
        childIds.removeAll(results);
        if (childIds.isEmpty()) {
            return;
        }
        results.addAll(childIds);
        selectChild(childIds, results, depth);
    }


    /**
     * 循环获取父级信息
     *
     * @param list
     * @return
     */
    private List<TradeEntity> getParents(Long tradeId, List<TradeEntity> list) {
        TradeEntity trade = this.tradeService.getById(tradeId);
        //有数据
        if (ObjectUtil.isNotEmpty(trade)) {
            list.add(trade);
        } else {
            return list;
        }
        //有上级
        if (trade.getParentId() != 0) {
            getParents(trade.getParentId(), list);
        }

        return list;
    }

    /**
     * 返回当前行业的子行业
     *
     * @param tradeTreeVo         行业
     * @param tradeEntities       全部行业列表
     * @param childrenNotNull     当没有子行业时，子行业列表是否返回空集合 0：直接返回null 1：返回空集合
     * @param validLeafTradeIdSet 有效的叶子节点行业ID集合
     * @return
     */
    private List<TradeTreeVo> getChildrenMenu(TradeTreeVo tradeTreeVo, List<TradeEntity> tradeEntities, List<SensitiveWordsEntity> sensitiveWordsEntities, List<CueWordsEntity> cueWordsEntities, Integer childrenNotNull, Set<Long> validLeafTradeIdSet, Map<Long, Long> tradeCountMap) {
        List<TradeTreeVo> childrenTradeTreeVos = tradeEntities.stream().filter(item -> item.getParentId().equals(tradeTreeVo.getId())).map(item -> {
            TradeTreeVo childrenTradeTreeVo = new TradeTreeVo();
            BeanUtils.copyProperties(item, childrenTradeTreeVo);
            childrenTradeTreeVo.setChildren(getChildrenMenu(childrenTradeTreeVo, tradeEntities, sensitiveWordsEntities, cueWordsEntities, childrenNotNull, validLeafTradeIdSet, tradeCountMap));
            // 封装关键词和敏感词数量
            childrenTradeTreeVo.setCruxNum(0);
            childrenTradeTreeVo.setSensitiveNum(0);
            childrenTradeTreeVo.setCueNum(0);
            childrenTradeTreeVo.setOpeFullcueNum(0);
            childrenTradeTreeVo.setOpeParCueNum(0);
            childrenTradeTreeVo.setVioFullCueNum(0);
            childrenTradeTreeVo.setVioParCueNum(0);
            // 设置是否是行业热榜有效行业
            childrenTradeTreeVo.setIsValidRankTrade(validLeafTradeIdSet.contains(item.getId()) ? 1 : 0);
            // 设置行业热榜数量
            childrenTradeTreeVo.setTradeRankCount(tradeCountMap.getOrDefault(item.getId(), 0L));

            if (sensitiveWordsEntities != null && sensitiveWordsEntities.size() > 0) {
                for (SensitiveWordsEntity sensitiveWordsEntity : sensitiveWordsEntities) {
                    if (sensitiveWordsEntity.getTradeId().equals(childrenTradeTreeVo.getId())) {
                        if (sensitiveWordsEntity.getWordsType() == 0) {
                            childrenTradeTreeVo.setSensitiveNum(childrenTradeTreeVo.getSensitiveNum() + 1);
                        } else if (sensitiveWordsEntity.getWordsType() == 1) {
                            childrenTradeTreeVo.setCruxNum(childrenTradeTreeVo.getCruxNum() + 1);
                        }
                    }
                }
            }

            //提示词数量处理
            if (cueWordsEntities != null && cueWordsEntities.size() > 0) {
                for (CueWordsEntity cueWordsEntity : cueWordsEntities) {
                    if (cueWordsEntity.getTradeId().equals(childrenTradeTreeVo.getId())) {
                        childrenTradeTreeVo.setCueNum(childrenTradeTreeVo.getCueNum() + 1);
                        //判断提示词类型 这个地方暂时用魔术值 提示词类型 0: 运营提示词，1:违规提示词
                        //判断提示词的范围 范围 0:全文，1:段落
                        //运营全文提示词
                        if (cueWordsEntity.getCueType() == 0 && cueWordsEntity.getScope() == 0)
                            childrenTradeTreeVo.setOpeFullcueNum(childrenTradeTreeVo.getOpeFullcueNum() + 1);
                        //运营段落提示词
                        if (cueWordsEntity.getCueType() == 0 && cueWordsEntity.getScope() == 1)
                            childrenTradeTreeVo.setOpeParCueNum(childrenTradeTreeVo.getOpeParCueNum() + 1);
                        //违规全文提示词
                        if (cueWordsEntity.getCueType() == 1 && cueWordsEntity.getScope() == 0)
                            childrenTradeTreeVo.setVioFullCueNum(childrenTradeTreeVo.getVioFullCueNum() + 1);
                        //违规段落提示词
                        if (cueWordsEntity.getCueType() == 1 && cueWordsEntity.getScope() == 1)
                            childrenTradeTreeVo.setVioParCueNum(childrenTradeTreeVo.getVioParCueNum() + 1);
                    }
                }
            }

            return childrenTradeTreeVo;
        }).toList();

        if (childrenTradeTreeVos.size() == 0) {
            if (!StringUtils.isEmpty(childrenNotNull) && childrenNotNull == 1) {
                return childrenTradeTreeVos;
            } else {
                return null;
            }

        }

        return childrenTradeTreeVos;
    }

}

