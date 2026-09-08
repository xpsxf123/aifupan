package com.jiuyu.replay.words.bll;

import cn.hutool.core.lang.tree.Tree;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.bo.words.CruxTypeScaleBo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.words.*;
import com.jiuyu.replay.words.bo.DataModelSaveBo;
import com.jiuyu.replay.words.bo.TradeBo;
import com.jiuyu.replay.words.bo.TradeListBo;
import com.jiuyu.replay.words.entity.AnchorUrlEntity;
import com.jiuyu.replay.words.entity.AnchorUrlUserEntity;
import com.jiuyu.replay.words.entity.DataModelEntity;
import com.jiuyu.replay.words.producer.*;
import com.jiuyu.replay.words.repository.service.AnchorUrlService;
import com.jiuyu.replay.words.rse.TradeRse;
import com.jiuyu.replay.words.repository.service.AnchorUrlUserService;
import com.jiuyu.replay.words.vo.AnchorTradeListVo;
import com.jiuyu.replay.words.vo.TradeSimpleTreeVo;
import com.jiuyu.replay.words.vo.TradeTreeVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


/**
 * 行业
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-06-20 16:22:18
 */
@Component
public class TradeBll {

    @Resource
    private TradeProducer tradeProducer;
    @Resource
    private ModelCruxProducer modelCruxProducer;
    @Resource
    private DataModelProducer dataModelProducer;
    @Resource
    private AnchorUrlUserProducer anchorUrlUserProducer;
    @Resource
    private AnchorVideoProducer anchorVideoProducer;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private AnchorUrlUserService anchorUrlUserService;
    @Resource
    private AnchorUrlService anchorUrlService;
    @Resource
    private TradeRse tradeRse;


    /**
     * 行业列表
     * @param tradeListBo 行业列表查询参数
     * @return
     */
    public R<PageUtils<TradeListVo>> queryPage(TradeListBo tradeListBo) {

        return R.ok("获取成功", tradeProducer.queryPage(tradeListBo));
    }

    /**
    * 行业信息
    * @param id 行业id
    * @return
    */
    public R<TradeInfoVo> info(Long id) {

        TradeInfoVo tradeInfoVo = tradeProducer.info(id);
        DataModelSaveBo modelSaveBo =  dataModelProducer.infoModel(id);
        if (modelSaveBo != null){
            tradeInfoVo.setModelName(modelSaveBo.getName());
            tradeInfoVo.setModelType(modelSaveBo.getType());
            tradeInfoVo.setCruxTypeScaleBos(modelSaveBo.getCruxTypeScaleBos());
        }

        return R.ok("获取成功", tradeInfoVo);
    }

    /**
     * 新增行业
     * @param tradeBo 行业对象
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public R<String> save(TradeBo tradeBo) {

        // 判断是否拥有行业模型
        if (tradeBo.getModelName() == null || tradeBo.getModelName().equals("")){
            TradeInfoVo tradeInfoVo = tradeProducer.save(tradeBo);
        }else{
            List<CruxTypeScaleBo> cruxTypeScaleBos = tradeBo.getCruxTypeScaleBos();
            if (cruxTypeScaleBos != null && !cruxTypeScaleBos.isEmpty()){
                double sum = 0;
                BigDecimal bigDecimal = new BigDecimal("0");
                for (int i = 0; i < cruxTypeScaleBos.size(); i++) {
                    BigDecimal bigDecimalItem = new BigDecimal("" + cruxTypeScaleBos.get(i).getScale());
                    bigDecimal = bigDecimal.add(bigDecimalItem);

                }
                sum = bigDecimal.doubleValue();
                if (sum != 1){
                    return R.error(40001,"关键词占比总和需要等于100%");
                }

                Long dataModelId = SnowflakeManager.nextValue();
                tradeBo.setTradeModelId(dataModelId);
                TradeInfoVo tradeInfoVo = tradeProducer.save(tradeBo);

                DataModelEntity dataModelEntity =  dataModelProducer.saveModel(tradeInfoVo);

                modelCruxProducer.saveBatchModel(cruxTypeScaleBos,dataModelEntity.getId());

            }
        }

        // 删除缓存
        this.redisTemplate.delete(List.of("replay:trade:tree:null", "replay:trade:tree:0", "replay:trade:tree:1", "replay:trade:simple:tree:null", "replay:trade:simple:tree:0", "replay:trade:simple:tree:1"));

        return R.ok("添加成功");
    }

    /**
     * 修改行业
     * @param tradeBo 行业对象
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public R<String> update(TradeBo tradeBo) {

        if (tradeBo.getModelName() == null || tradeBo.getModelName().equals("")){
            tradeProducer.update(tradeBo);
        }else{
            List<CruxTypeScaleBo> cruxTypeScaleBos = tradeBo.getCruxTypeScaleBos();
            if (cruxTypeScaleBos != null && !cruxTypeScaleBos.isEmpty()){
                double sum = 0;
                BigDecimal bigDecimal = new BigDecimal("0");
                for (int i = 0; i < cruxTypeScaleBos.size(); i++) {
                    BigDecimal bigDecimalItem = new BigDecimal("" + cruxTypeScaleBos.get(i).getScale());
                    bigDecimal = bigDecimal.add(bigDecimalItem);

                }
                sum = bigDecimal.doubleValue();
                if (sum != 1){
                    return R.error(40001,"关键词占比总和需要等于100%");
                }
                if (tradeBo.getTradeModelId() == 0){
                    Long dataModelId = SnowflakeManager.nextValue();
                    tradeBo.setTradeModelId(dataModelId);
                }
                tradeProducer.update(tradeBo);

                dataModelProducer.updateModel(tradeBo);
            }
        }

        // 删除缓存
        this.redisTemplate.delete(List.of("replay:trade:tree:null", "replay:trade:tree:0", "replay:trade:tree:1", "replay:trade:simple:tree:null", "replay:trade:simple:tree:0", "replay:trade:simple:tree:1"));

        return R.ok("修改成功");
    }

    /**
     * 删除行业
     * @param id 行业id
     * @return
     */
    public R<String> delete(Long id) {

        tradeProducer.deleteById(id);

        // 删除缓存
        this.redisTemplate.delete(List.of("replay:trade:tree:null", "replay:trade:tree:0", "replay:trade:tree:1", "replay:trade:simple:tree:null", "replay:trade:simple:tree:0", "replay:trade:simple:tree:1"));

        return R.ok("删除成功");
    }


    /**
     * 获取行业列表（树形结构）
     * @param childrenNotNull 当没有子行业时，子行业列表是否返回空集合 0：直接返回null 1：返回空集合
     * @return
     */
    public R<List<TradeTreeVo>> listTree(Integer childrenNotNull) {

        // 如果缓存有数据，直接返回
        String redisKey = "replay:trade:tree:" + childrenNotNull;
        Object obj = this.redisTemplate.opsForValue().get(redisKey);
        if(obj != null) {
            List<TradeTreeVo> cachedList = JSON.parseArray((String) obj, TradeTreeVo.class);
            // 缓存有数据，还需要加上主播数量统计
            addAnchorCountsToTradeTree(cachedList);
            return R.ok(cachedList);
        }

        List<TradeTreeVo> tradeTreeVos = tradeProducer.listTree(childrenNotNull);

        // 添加主播数量统计
        addAnchorCountsToTradeTree(tradeTreeVos);

        // 存到缓存
        if(tradeTreeVos != null && tradeTreeVos.size() > 0) {
            this.redisTemplate.opsForValue().set(redisKey, JSON.toJSONString(tradeTreeVos), Duration.ofDays(5));
        }

        return R.ok("获取成功", tradeTreeVos);
    }

    /**
     * 给行业树添加主播数量统计
     * @param tradeTreeVos 行业树列表
     */
    private void addAnchorCountsToTradeTree(List<TradeTreeVo> tradeTreeVos) {
        if(tradeTreeVos == null || tradeTreeVos.isEmpty()) {
            return;
        }

        // 先查询Redis缓存
        String anchorCountCacheKey = "replay:trade:anchor:count:all";
        Object cachedCountObj = this.redisTemplate.opsForValue().get(anchorCountCacheKey);

        Map<Long, Map<String, Integer>> tradeAnchorCounts;
        if(cachedCountObj != null) {
            tradeAnchorCounts = JSON.parseObject((String) cachedCountObj,
                new com.alibaba.fastjson2.TypeReference<Map<Long, Map<String, Integer>>>(){});
        } else {
            // 分批查询所有用户添加的主播（未删除的），每次查询1万条
            int pageSize = 10000;
            int pageNum = 1;
            boolean hasMore = true;

            // 统计每个行业的各平台主播数量
            tradeAnchorCounts = new HashMap<>();

            while(hasMore) {
                // 分页查询用户主播数据
                Page<AnchorUrlUserEntity> page = new Page<>(pageNum, pageSize);
                Page<AnchorUrlUserEntity> userAnchorPage = anchorUrlUserService.page(
                    page,
                    new LambdaQueryWrapper<AnchorUrlUserEntity>()
                        .eq(AnchorUrlUserEntity::getIsRemoveRecord, 0)
                        .select(AnchorUrlUserEntity::getAnchorUrlSecUid, AnchorUrlUserEntity::getTradeId)
                );

                List<AnchorUrlUserEntity> userAnchors = userAnchorPage.getRecords();
                if(userAnchors == null || userAnchors.isEmpty()) {
                    break;
                }

                // 获取当前批次的所有secUid
                List<String> secUids = userAnchors.stream()
                    .map(AnchorUrlUserEntity::getAnchorUrlSecUid)
                    .distinct()
                    .collect(Collectors.toList());

                // 分批查询主播信息（IN查询每批不超过1000个，避免SQL过长和性能问题）
                Map<String, Integer> secUidToPlatform = new HashMap<>();
                int inBatchSize = 1000;
                for(int i = 0; i < secUids.size(); i += inBatchSize) {
                    int endIndex = Math.min(i + inBatchSize, secUids.size());
                    List<String> batchSecUids = secUids.subList(i, endIndex);

                    List<AnchorUrlEntity> anchors = anchorUrlService.list(
                        new LambdaQueryWrapper<AnchorUrlEntity>()
                            .in(AnchorUrlEntity::getSecUid, batchSecUids)
                            .select(AnchorUrlEntity::getSecUid, AnchorUrlEntity::getPlatform)
                    );

                    // 构建secUid到platform的映射
                    anchors.forEach(anchor ->
                        secUidToPlatform.put(anchor.getSecUid(), anchor.getPlatform())
                    );
                }

                // 统计当前批次的主播数量
                for(AnchorUrlUserEntity userAnchor : userAnchors) {
                    String secUid = userAnchor.getAnchorUrlSecUid();

                    Long tradeId = userAnchor.getTradeId();
                    if(tradeId == null) {
                        continue;
                    }

                    Integer platform = secUidToPlatform.get(secUid);
                    if(platform == null) {
                        continue;
                    }

                    tradeAnchorCounts.putIfAbsent(tradeId, new HashMap<>());
                    Map<String, Integer> counts = tradeAnchorCounts.get(tradeId);

                    // 0:抖音 1:快手 2:视频号
                    String platformKey = platform == 0 ? "douyin" : (platform == 1 ? "kuaishou" : "shipinhao");
                    counts.put(platformKey, counts.getOrDefault(platformKey, 0) + 1);
                }

                // 判断是否还有下一页
                hasMore = userAnchorPage.getCurrent() < userAnchorPage.getPages();
                pageNum++;
            }

            // 存入Redis，每日最后1秒过期
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime endOfDay = now.toLocalDate().plusDays(1).atTime(0, 0, 0).minusSeconds(1);
            Duration duration = Duration.between(now, endOfDay);
            this.redisTemplate.opsForValue().set(anchorCountCacheKey,
                JSON.toJSONString(tradeAnchorCounts), duration);
        }

        // 递归设置每个行业节点的主播数量
        for(TradeTreeVo trade : tradeTreeVos) {
            setAnchorCountsRecursive(trade, tradeAnchorCounts);
        }
    }

    /**
     * 递归设置行业树节点的主播数量
     * @param trade 行业树节点
     * @param tradeAnchorCounts 行业主播数量映射
     */
    private void setAnchorCountsRecursive(TradeTreeVo trade, Map<Long, Map<String, Integer>> tradeAnchorCounts) {
        Map<String, Integer> counts = tradeAnchorCounts.get(trade.getId());
        if(counts != null) {
            trade.setDouyinAnchorCount(counts.getOrDefault("douyin", 0));
            trade.setKuaishouAnchorCount(counts.getOrDefault("kuaishou", 0));
            trade.setShipinhaoAnchorCount(counts.getOrDefault("shipinhao", 0));
        } else {
            trade.setDouyinAnchorCount(0);
            trade.setKuaishouAnchorCount(0);
            trade.setShipinhaoAnchorCount(0);
        }

        // 递归处理子行业
        if(trade.getChildren() != null && !trade.getChildren().isEmpty()) {
            for(TradeTreeVo child : trade.getChildren()) {
                setAnchorCountsRecursive(child, tradeAnchorCounts);
            }
        }
    }

    /**
     * 获取行业列表（树形结构，简化版-客户端用）
     * @param childrenNotNull 当没有子行业时，子行业列表是否返回空集合 0：直接返回null 1：返回空集合
     * @return
     */
    public R<List<TradeSimpleTreeVo>> listSimpleTree(Integer childrenNotNull) {

        // 如果缓存有数据，直接返回
        String redisKey = "replay:trade:simple:tree:" + childrenNotNull;
        Object obj = this.redisTemplate.opsForValue().get(redisKey);
        if(obj != null) {
            return R.ok(JSON.parseArray((String) obj, TradeSimpleTreeVo.class));
        }

        List<TradeSimpleTreeVo> tradeTreeVos = tradeProducer.listSimpleTree(childrenNotNull);

        // 存到缓存
        if(tradeTreeVos != null && tradeTreeVos.size() > 0) {
            this.redisTemplate.opsForValue().set(redisKey, JSON.toJSONString(tradeTreeVos), Duration.ofDays(5));
        }

        return R.ok("获取成功", tradeTreeVos);
    }

    /**
     * 根据行业id集合获取行业集合
     * @param tradeIds 行业id集合
     * @return
     */
    public R<List<TradeListVo>> listByIds(Collection<Long> tradeIds) {

        List<TradeListVo> tradeVos = tradeProducer.listByIds(tradeIds);

        return R.ok("获取成功", tradeVos);
    }

    /**
     * 根据行业名称查询
     * @param tradeName
     * @return
     */
    public List<Long> selectByName(String tradeName) {
      return   tradeProducer.selectByName(tradeName);
    }

    /**
     * 获取所有行业列表
     * @return
     */
    public R<List<TradeVo>> listAll() {

        List<TradeVo> tradeVos = tradeProducer.listAll();

        return R.ok("获取成功", tradeVos);
    }


    /**
     * 删除行业模型
     * @param tradeId
     * @param modelId
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public R<String> deleteTradeModel(Long tradeId, Long modelId) {
        if (tradeId != 0){
            tradeProducer.deleteTradeModelId(tradeId);
        }else{
            TradeBo tradeByModelId = tradeProducer.getTradeByModelId(modelId);
            if (tradeByModelId != null){
                tradeByModelId.setTradeModelId(0L);
                tradeProducer.update(tradeByModelId);
            }
        }
        dataModelProducer.deleteTradeModel(modelId);
        return R.ok("删除成功");
    }

    /**
     * 根据当前Id获取所有的父级Id
     * @param tradeIds
     * @param withGeneral 是否带有通用信息
     */
    public R<List<TradeInfoVo>> listTradeIdsByTradeIds(List<Long> tradeIds, Integer withGeneral) {
        List<TradeInfoVo> tradeVos = tradeProducer.listParentsByTradeIds(tradeIds, withGeneral);
        return R.ok("获取成功", tradeVos);
    }

    /**
     * 根据当前Id获取所有的父级Id
     * @param tradeId
     * @param withGeneral 是否带有通用信息
     */
    public R<List<TradeInfoVo>> listTradeIdsByTradeId(Long tradeId, Integer withGeneral) {
        List<TradeInfoVo> tradeVos = tradeProducer.listParentsByTradeId(tradeId, withGeneral);
        return R.ok("获取成功", tradeVos);
    }

    /**
     * 根据用户添加的主播返回行业列表
     * @param userId 用户id
     * @return
     */
    public R<List<AnchorTradeListVo>> listByAnchor(Long userId, Long tenantId, Integer isCloud) {
        // 获取用户的主播
        List<AnchorUrlUserVo> anchorUrlUserVos = this.anchorUrlUserProducer.listByUser(userId, tenantId);
        if(anchorUrlUserVos != null && anchorUrlUserVos.size() > 0) {

            List<String> secUids = anchorUrlUserVos.stream().map(AnchorUrlUserVo::getAnchorUrlSecUid).collect(Collectors.toList());
            // 获取视频信息
            List<AnchorVideoInfoVo> videoInfoVos = this.anchorVideoProducer.listByAnchorRecordList(secUids, userId, tenantId, isCloud, 0);
            // 剔除已从录制列表删除，并且视频数量为0的主播
            List<AnchorUrlUserVo> anchorResultList = anchorUrlUserVos.stream().filter(anchorUrlUserVo -> {
                int recordTotal = 0;
                if(videoInfoVos != null && videoInfoVos.size() > 0) {
                    for (AnchorVideoInfoVo videoInfoVo : videoInfoVos) {
                        if (videoInfoVo.getSecUid().equals(anchorUrlUserVo.getAnchorUrlSecUid())) {
                            recordTotal++;
                            break;
                        }
                    }
                }
                if (anchorUrlUserVo.getIsRemoveRecord() == 0 || recordTotal > 0) {
                    return true;
                }
                return false;
            }).toList();


            // 获取行业列表
            if(anchorResultList.size() > 0) {
                Set<Long> tradeIds = anchorResultList.stream().map(AnchorUrlUserVo::getTradeId).collect(Collectors.toSet());
                List<TradeListVo> tradeListVos = this.tradeProducer.listByIds(tradeIds);

                if(tradeListVos != null && tradeListVos.size() > 0) {
                    // 封装行业的主播数量
                    List<AnchorTradeListVo> anchorTradeListVos = tradeListVos.stream().map(item -> {
                        AnchorTradeListVo anchorTradeListVo = new AnchorTradeListVo();
                        BeanUtils.copyProperties(item, anchorTradeListVo);
                        anchorTradeListVo.setAnchorNum(0);
                        for (AnchorUrlUserVo anchorUrlUserVo : anchorResultList) {
                            if (anchorUrlUserVo.getTradeId().equals(anchorTradeListVo.getId())) {
                                anchorTradeListVo.setAnchorNum(anchorTradeListVo.getAnchorNum() + 1);
                            }
                        }
                        return anchorTradeListVo;
                    }).sorted(Comparator.comparingInt(AnchorTradeListVo::getAnchorNum).reversed()).collect(Collectors.toList());
                    return R.ok(anchorTradeListVos);
                }
            }

        }
        return R.ok();
    }

    /**
     * 根据id批量查询行业
     * @param list
     */
    public List<TradeListVo> selectByTradeIds(List<Long> list) {
        List<TradeListVo> tradeListVos = this.tradeProducer.listByIds(list);
        if (tradeListVos == null){
            return new ArrayList<>();
        }
        return tradeListVos;
    }

    public List<TradeVo> listTradeByIds(List<Long> ids) {
        return tradeProducer.listTradeByIds(ids);
    }

    /**
     * 根据行业ID获取行业信息
     * @param tradeId 行业ID
     * @return 行业信息
     */
    public R<TradeVo> getTradeById(Long tradeId) {
        TradeVo tradeVo = tradeRse.getById(tradeId);
        return R.ok("获取成功", tradeVo);
    }

    /**
     * 根据行业ID获取行业信息
     *
     * @return 行业列表
     */
    public List<Tree<Long>> listTreeTrade() {
        return tradeProducer.listTreeTrade();
    }

    public Map<Long, String> getTradeNames(List<Long> tradeIds) {
        if (EmptyUtil.isEmpty(tradeIds)) {
            return Map.of();
        }
        return tradeProducer.getTradeNames(tradeIds);
    }
}
