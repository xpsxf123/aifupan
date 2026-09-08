package com.jiuyu.replay.words.producer.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.validation.BeanValidationResult;
import cn.hutool.extra.validation.ValidationUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import com.jiuyu.framework.function.BatchQuery;
import com.jiuyu.framework.function.complete.Complete;
import com.jiuyu.framework.shandard.Entry;
import com.jiuyu.framework.util.CollUtil;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.framework.util.FunctionUtil;
import com.jiuyu.replay.common.entity.SystemKvEntity;
import com.jiuyu.replay.common.repository.service.SystemKvService;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.common.utils.excel.ExcelUtils;
import com.jiuyu.replay.generic.dto.third.ThirdAnchorBo;
import com.jiuyu.replay.generic.enums.words.AnchorPlatformEnum;
import com.jiuyu.replay.generic.feign.order.OrderFeign;
import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.common.code.StatusCode;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.generic.vo.order.OrderInfoVo;
import com.jiuyu.replay.generic.vo.power.UserCacheVo;
import com.jiuyu.replay.words.bo.TradeRankAdminQueryBo;
import com.jiuyu.replay.words.bo.anchor.ExcelUploadSimilarAnchor;
import com.jiuyu.replay.words.dto.SimilarAnchorCallbackDto;
import com.jiuyu.replay.words.dto.TradeThirdRankingDto;
import com.jiuyu.replay.words.dto.UpdateSimilarAnchorDto;
import com.jiuyu.replay.words.entity.*;
import com.jiuyu.replay.words.repository.dao.SimilarAnchorDao;
import com.jiuyu.replay.words.repository.dao.TbHotSearchTradeRankingListMapper;
import com.jiuyu.replay.words.repository.dao.TradeThirdRankingMapper;
import com.jiuyu.replay.words.repository.dao.TradeThirdRankingRelationMapper;
import com.jiuyu.replay.words.repository.service.*;
import com.jiuyu.replay.words.vo.LeafTradeVo;
import com.jiuyu.replay.words.vo.SimilarAnchorVo;
import com.jiuyu.replay.words.vo.TradeRankAdminVo;
import com.jiuyu.replay.words.vo.TradeRankVo;
import com.jiuyu.replay.words.vo.anchor.TradeRankInfo;
import com.jiuyu.replay.words.vo.anchor.TradeThirdRankingVO;
import com.jiuyu.replay.words.vo.anchor.TreeTradeThirdRank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 行业排名主播生产者
 * 处理行业主播相似达人查询和回调逻辑
 *
 * @author RayChou, HeHui
 * @date 2025/10/27 10:24
 * @since 2026/1/27 15:05
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TradeRankAnchorProducer {

    private final AnchorUrlService anchorUrlService;
    private final AnchorUrlUserService anchorUrlUserService;
    private final SimilarAnchorService similarAnchorService;
    private final AnchorCruxWordsRelaService cruxWordsRelaService;
    private final SimilarAnchorDao similarAnchorDao;
    private final SimilarSendRecordService similarSendRecordService;
    private final SystemKvService systemKvService;
    private final OrderFeign orderFeign;
    private final UserFeign userFeign;
    private final TradeService tradeService;
    private final RedisTemplate<String, Object> redisTemplate;

    private final TbHotSearchTradeRankingListMapper hotSearchTradeRankingListMapper;
    private final TradeThirdRankingRelationMapper thirdRankingRelationMapper;
    private final TradeThirdRankingMapper tradeThirdRankingMapper;


    /**
     * 相似达人回调接口（优化版：批量查询，避免N+1问题）
     *
     * @param callbackDto 回调数据
     *
     * @return 处理结果（成功返回SUCCESS，失败返回FAIL）
     *
     * @author RayChou
     * @date 2025-10-28
     */
    @Transactional(rollbackFor = Exception.class)
    public R<String> handleSimilarAnchorCallback(SimilarAnchorCallbackDto callbackDto) {
        String currentAnchorNumber = callbackDto.getCurrentAnchorId();
        String requestId = callbackDto.getRequestId();
        List<SimilarAnchorVo> similarAnchorList = callbackDto.getSimilarAnchorList();
        int similarCount = CollectionUtil.isEmpty(similarAnchorList) ? 0 : similarAnchorList.size();

        log.info("========== [回调接口] 收到相似达人回调数据 ==========");
        log.info("[回调数据] currentAnchorId(抖音号): {}, requestId: {}, 相似主播数量: {}",
            currentAnchorNumber, requestId, similarCount);

        // 用于finally块中保存回调信息
        boolean isSuccess = false;
        String errorMessage = null;
        String successMessage = null;

        try {
            // 1. 验证数据
            if (CollectionUtil.isEmpty(similarAnchorList)) {
                log.info("[步骤1] 相似主播列表为空，无需保存数据");
                // 标记处理成功，并设置成功消息
                isSuccess = true;
                successMessage = "相似主播列表为空，无需保存数据";
                log.info("========== [回调接口] 处理完成(相似主播列表为空) ==========");
                return R.ok("成功", "SUCCESS");
            }
            // 2. 验证requestId是否存在
            if (StrUtil.isBlank(requestId)) {
                errorMessage = "回调数据中缺少requestId";
                log.warn("[步骤2] ❌ {}，回调处理结束", errorMessage);
                log.info("========== [回调接口] 处理完成(缺少requestId) ==========");
                return R.error(500, errorMessage, "FAIL");
            }

            // 3. 通过requestId查询发送记录
            log.info("[步骤3] 通过requestId查询发送记录，requestId: {}", requestId);
            SimilarSendRecordEntity sendRecord = similarSendRecordService.getOne(
                new LambdaQueryWrapper<SimilarSendRecordEntity>()
                    .eq(SimilarSendRecordEntity::getRequestId, requestId)
                    .last("LIMIT 1")
            );

            if (ObjectUtil.isNull(sendRecord)) {
                errorMessage = "未找到对应的发送记录";
                log.warn("[步骤3] ❌ {}，requestId: {}，回调处理结束", errorMessage, requestId);
                log.info("========== [回调接口] 处理完成(未找到发送记录) ==========");
                return R.error(500, errorMessage, "FAIL");
            }

            log.info("[步骤3] ✓ 找到发送记录，记录ID: {}, 主播抖音号: {}, secUid: {}",
                sendRecord.getId(), sendRecord.getAnchorNumber(), sendRecord.getSecUid());

            String secUid = sendRecord.getSecUid();

            Optional<AnchorUrlEntity> optionalAnchor = anchorUrlService.lambdaQuery().eq(AnchorUrlEntity::getSecUid, secUid)
                .eq(AnchorUrlEntity::getPlatform, 0)
                .last("LIMIT 1").oneOpt();

            if (optionalAnchor.isEmpty()) {
                errorMessage = "未找到主播关联信息";
                log.warn("[步骤4] ❌ {}，secUid: {}，回调处理结束", errorMessage, secUid);
                log.info("========== [回调接口] 处理完成(未找到关联信息) ==========");
                return R.error(500, errorMessage, "FAIL");
            }
            AnchorUrlEntity anchorUrlEntity = optionalAnchor.get();
            Optional<TbHotSearchTradeRankingList> optionalTradeRanking = ChainWrappers.lambdaQueryChain(hotSearchTradeRankingListMapper)
                .eq(TbHotSearchTradeRankingList::getPreAnchorId, anchorUrlEntity.getId())
                .last("LIMIT 1")
                .oneOpt();
            if (optionalTradeRanking.isEmpty()) {
                errorMessage = "未找到系统主播榜单数据";
                log.warn("[步骤4] ❌ {}，secUid: {}，回调处理结束", errorMessage, secUid);
                log.info("========== [回调接口] 处理完成(未找到系统主播榜单数据) ==========");
                return R.error(500, errorMessage, "FAIL");
            }
            TbHotSearchTradeRankingList searchTradeRankingList = optionalTradeRanking.get();

            log.info("[步骤5] 开始处理{}个相似主播数据", similarAnchorList.size());

            // 优化：批量查询已存在的相似主播（使用authorId作为唯一标识，避免N+1查询）
            List<String> authorIdList = similarAnchorList.stream()
                .map(SimilarAnchorVo::getAuthorId)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.toList());

            // 验证：如果所有相似主播的authorId都为空，记录错误
            if (CollectionUtil.isEmpty(authorIdList)) {
                errorMessage = "相似主播列表中所有authorId都为空，数据异常";
                log.warn("[步骤5] ❌ {}，相似主播数量: {}，回调处理结束", errorMessage, similarAnchorList.size());
                log.info("========== [回调接口] 处理完成(authorId都为空) ==========");
                return R.error(500, errorMessage, "FAIL");
            }

            // 批量查询已存在的相似主播 key 婵妈妈账户ID value 相似主播
            Map<String, SimilarAnchorEntity> existingAnchorMap = similarAnchorService.lambdaQuery()
                .in(SimilarAnchorEntity::getAuthorId, authorIdList)
                .list().stream().collect(Collectors.toMap(SimilarAnchorEntity::getAuthorId, Function.identity(), FunctionUtil::mergeFirst));
            log.info("[步骤5] ✓ 批量查询已存在的相似主播，查询数量: {}, 已存在数量: {}", authorIdList.size(), existingAnchorMap.size());
            List<String> anchorNumberList = similarAnchorList.stream()
                .map(SimilarAnchorVo::getUniqueId)
                .filter(StrUtil::isNotBlank)
                .distinct().toList();
            Map<String, SimilarAnchorEntity> existingAnchorNumberMap = similarAnchorService.lambdaQuery()
                .in(SimilarAnchorEntity::getUniqueId, anchorNumberList)
                .eq(SimilarAnchorEntity::getIsDeleted, 0)
                .orderByDesc(SimilarAnchorEntity::getId)
                .list().stream().collect(Collectors.toMap(SimilarAnchorEntity::getUniqueId, Function.identity(), FunctionUtil::mergeFirst));
            Map<Long, TbHotSearchTradeRankingList> similarAnchorRankingMap = this.getSimilarAnchorRankingListMap(Stream.concat(existingAnchorNumberMap.values().stream().map(SimilarAnchorEntity::getId), existingAnchorMap.values().stream().map(SimilarAnchorEntity::getId)).distinct().toList());
            Map<String, AnchorUrlEntity> anchorNumberMap = anchorUrlService.getAnchorMapByAnchorNumber(anchorNumberList, AnchorPlatformEnum.DOU_YIN.getCode());

            // 分开处理新增和更新（避免saveOrUpdateBatch的N+1问题）
            List<SimilarAnchorEntity> newAnchorEntities = new ArrayList<>();
            List<SimilarAnchorEntity> updateAnchorEntities = new ArrayList<>();
            List<TbHotSearchTradeRankingList> newSimilarAnchorRankingList = new ArrayList<>();
            List<TbHotSearchTradeRankingList> updateSimilarAnchorRankingList = new ArrayList<>();
            int newAnchorCount = 0;
            int existingAnchorCount = 0;
            int newRelationCount = 0;
            int existingRelationCount = 0;
            int skippedCount = 0;  // 跳过的数量（authorId为空）
            // 找到跟系统主播完全匹配到的相似主播
            Optional<SimilarAnchorVo> eqOptionalAnchorNumber = similarAnchorList.stream().filter(similarAnchorVo -> Objects.equals(similarAnchorVo.getUniqueId(), anchorUrlEntity.getAnchorNumber())).findFirst();
            for (int i = 0; i < similarAnchorList.size(); i++) {
                SimilarAnchorVo vo = similarAnchorList.get(i);
                if (eqOptionalAnchorNumber.isPresent() && Objects.equals(vo.getAuthorId(), eqOptionalAnchorNumber.get().getAuthorId())) {
                    log.debug("[步骤5] ✓ 找到跟系统主播完全匹配到的相似主播，authorId:{} 昵称: {}", vo.getAuthorId(), vo.getNickName());
                    continue;
                }
                log.debug("[步骤5] 处理第{}/{}个相似主播，authorId: {}, 昵称: {}",
                    i + 1, similarAnchorList.size(), vo.getAuthorId(), vo.getNickName());

                // 验证：如果单个相似主播的authorId为空，跳过（理论上不应该发生，因为前面已经过滤了）
                if (StrUtil.isBlank(vo.getAuthorId())) {
                    skippedCount++;
                    log.warn("[步骤5] ⚠ 跳过authorId为空的相似主播，昵称: {}", vo.getNickName());
                    continue;
                }

                // 从批量查询结果中获取已存在的主播（使用authorId作为唯一标识）
                SimilarAnchorEntity existingAnchor = existingAnchorMap.get(vo.getAuthorId());
                if (existingAnchor == null) {
                    existingAnchor = existingAnchorNumberMap.get(vo.getUniqueId());
                }
                Long similarAnchorId;
                if (ObjectUtil.isNull(existingAnchor)) {
                    // 新增相似主播
                    SimilarAnchorEntity anchorEntity = new SimilarAnchorEntity();
                    BeanUtils.copyProperties(vo, anchorEntity);
                    anchorEntity.setAnchorName(vo.getNickName());
                    anchorEntity.setAnchorAvatar(vo.getAvatar());
                    similarAnchorId = SnowflakeManager.nextValue();
                    anchorEntity.setId(similarAnchorId);
                    anchorEntity.setCollectDate(new Date());
                    anchorEntity.setCreateDate(new Date());
                    anchorEntity.setUpdateDate(new Date());
                    anchorEntity.setIsDeleted(0);
                    // 解析并设置总销售额数值（用于查询过滤和排序）
                    anchorEntity.calcTotalAmountToValue();
                    // 计算并设置账号热度
                    anchorEntity.calcAccountHeat();
                    Integer accountHeat = anchorEntity.getAccountHeat();


                    newAnchorEntities.add(anchorEntity);
                    newAnchorCount++;
                    existingAnchor = anchorEntity;
                    log.debug("[步骤5] 相似主播不存在，准备新增，authorId: {}, 主播名称: {}, 热度: {}",
                        vo.getAuthorId(), vo.getNickName(), accountHeat);
                } else {
                    // 更新已存在的相似主播数据
                    BeanUtils.copyProperties(vo, existingAnchor);
                    existingAnchor.setAnchorName(vo.getNickName());
                    existingAnchor.setAnchorAvatar(vo.getAvatar());
                    existingAnchor.setUpdateDate(new Date());

                    // 解析并设置总销售额数值（用于查询过滤和排序）
                    existingAnchor.calcTotalAmountToValue();

                    // 重新计算并更新账号热度
                    existingAnchor.calcAccountHeat();
                    Integer accountHeat = existingAnchor.getAccountHeat();


                    // 添加到更新列表
                    updateAnchorEntities.add(existingAnchor);

                    similarAnchorId = existingAnchor.getId();
                    existingAnchorCount++;
                    log.debug("[步骤5] 相似主播已存在，准备更新，authorId: {}, 主播名称: {}, 热度: {}",
                        vo.getAuthorId(), vo.getNickName(), accountHeat);
                }

                // 4. 创建关联关系
                TbHotSearchTradeRankingList rankingList = similarAnchorRankingMap.get(similarAnchorId);
                if (EmptyUtil.isEmpty(rankingList)) {
                    AnchorUrlEntity anchorUrl = anchorNumberMap.get(vo.getUniqueId());
                    TbHotSearchTradeRankingList tradeRankingList = createNewRankingList(similarAnchorId, anchorUrl, searchTradeRankingList.getTradeId());
                    tradeRankingList.setSystemAnchorId(0L);
                    tradeRankingList.setPreAnchorId(anchorUrlEntity.getId());
                    tradeRankingList.calcWeightScore(existingAnchor);
                    tradeRankingList.setCollectStatus(2);
                    newSimilarAnchorRankingList.add(tradeRankingList);
                    newRelationCount++;
                    log.debug("[步骤5] 准备新增关联关系，行业ID: {}, 相似主播ID: {}, 得分: {}", tradeRankingList.getTradeId(), similarAnchorId, tradeRankingList.getWeightScore());
                } else {
                    rankingList.calcWeightScore(existingAnchor);
                    rankingList.setCollectStatus(2);
                    rankingList.setUpdateDate(LocalDateTime.now());
                    updateSimilarAnchorRankingList.add(rankingList);
                    existingRelationCount++;
                    log.debug("[步骤5] 找到已存在的关联关系，行业ID: {}, 相似主播ID: {}, 得分: {}", rankingList.getTradeId(), similarAnchorId, rankingList.getWeightScore());
                }
            }
            if (eqOptionalAnchorNumber.isPresent()) {
                SimilarAnchorVo vo = eqOptionalAnchorNumber.get();
                SimilarAnchorEntity existingAnchor = similarAnchorDao.selectById(searchTradeRankingList.getSimilarAnchorId());
                // 更新已存在的相似主播数据
                BeanUtils.copyProperties(vo, existingAnchor);
                existingAnchor.setAnchorName(vo.getNickName());
                existingAnchor.setAnchorAvatar(vo.getAvatar());
                existingAnchor.setUpdateDate(new Date());
                // 解析并设置总销售额数值（用于查询过滤和排序）
                existingAnchor.calcTotalAmountToValue();

                // 重新计算并更新账号热度
                existingAnchor.calcAccountHeat();
                Integer accountHeat = existingAnchor.getAccountHeat();


                // 添加到更新列表
                updateAnchorEntities.add(existingAnchor);
                existingAnchorCount++;
                log.debug("[步骤5] 相似主播已存在，准备更新，authorId: {}, 主播名称: {}, 热度: {}",
                    vo.getAuthorId(), vo.getNickName(), accountHeat);
                // 更新关联关系中的得分
                searchTradeRankingList.calcWeightScore(existingAnchor);
                if (anchorUrlEntity.getSystemTradeId() != null) {
                    searchTradeRankingList.setSystemTradeId(anchorUrlEntity.getSystemTradeId());
                }
                searchTradeRankingList.setCollectStatus(2);
                searchTradeRankingList.setSystemAnchorId(anchorUrlEntity.getId());
                searchTradeRankingList.setUpdateDate(LocalDateTime.now());
                updateSimilarAnchorRankingList.add(searchTradeRankingList);
                existingRelationCount++;
            }

            log.info("[步骤5] 相似主播数据处理完成，新增: {}, 更新: {}, 跳过: {}", newAnchorCount, existingAnchorCount, skippedCount);
            log.info("[步骤5] 关联关系处理完成，新增: {}, 已存在: {}", newRelationCount, existingRelationCount);

            // 批量保存新增的主播（真正的批量INSERT，避免N+1）
            if (CollectionUtil.isNotEmpty(newAnchorEntities)) {
                similarAnchorService.saveBatch(newAnchorEntities);
                log.info("[步骤5] ✓ 批量新增相似主播数据成功，数量: {}", newAnchorEntities.size());
            } else {
                log.info("[步骤5] 无新增相似主播数据需要保存");
            }

            // 批量更新已存在的主播（真正的批量UPDATE，避免N+1）
            if (CollectionUtil.isNotEmpty(updateAnchorEntities)) {
                similarAnchorDao.batchUpdateSelective(updateAnchorEntities);
                log.info("[步骤6] ✓ 批量更新相似主播数据成功，数量: {}", updateAnchorEntities.size());
            } else {
                log.info("[步骤6] 无相似主播数据需要更新");
            }

            // 批量保存关联关系
            if (CollectionUtil.isNotEmpty(newSimilarAnchorRankingList)) {
                hotSearchTradeRankingListMapper.batchInsert(newSimilarAnchorRankingList);
                log.info("[步骤7] ✓ 批量保存相似主播关联关系成功，数量: {}", newSimilarAnchorRankingList.size());
            } else {
                log.info("[步骤7] 无新增关联关系需要保存");
            }
            if (CollectionUtil.isNotEmpty(updateSimilarAnchorRankingList)) {
                hotSearchTradeRankingListMapper.updateBatchSelective(updateSimilarAnchorRankingList);
                log.info("[步骤7] ✓ 批量更新相似主播关联关系成功，数量: {}", updateSimilarAnchorRankingList.size());
            } else {
                log.info("[步骤7] 无相似主播关联关系需要更新");
            }

            // 标记处理成功
            isSuccess = true;
            if (skippedCount > 0) {
                successMessage = String.format("成功处理%d个相似主播，新增%d个，更新%d个，跳过%d个(authorId为空)，新增关联关系%d个",
                    similarAnchorList.size(), newAnchorCount, existingAnchorCount, skippedCount, newRelationCount);
            } else {
                successMessage = String.format("成功处理%d个相似主播，新增%d个，更新%d个，新增关联关系%d个",
                    similarAnchorList.size(), newAnchorCount, existingAnchorCount, newRelationCount);
            }

            // 删除缓存
            this.clearCache();
            log.info("========== [回调接口] 处理完成(成功) ==========");
            return R.ok("成功", "SUCCESS");

        } catch (Exception e) {
            errorMessage = "处理回调数据失败: " + e.getMessage();
            log.error("========== [回调接口] 处理失败(发生异常) ==========", e);
            throw e;
        } finally {
            // 【关键优化】无论成功、失败、异常，都要更新发送记录的回调状态
            // 这样可以追踪所有回调情况，避免回调状态丢失
            if (StrUtil.isNotBlank(requestId)) {
                try {
                    log.info("[Finally块] 更新发送记录的回调状态，requestId: {}, 处理结果: {}",
                        requestId, isSuccess ? "成功" : "失败");
                    updateSendRecordCallback(callbackDto, requestId, isSuccess, errorMessage, successMessage);
                } catch (Exception e) {
                    // 更新回调状态失败不影响主流程，只记录日志
                    log.error("[Finally块] 更新发送记录的回调状态失败，requestId: {}", requestId, e);
                }
            } else {
                log.warn("[Finally块] requestId为空，无法更新发送记录的回调状态，anchorNumber: {}", currentAnchorNumber);
            }
        }
    }

    /**
     * 更新发送记录的回调状态（优化版：使用requestId查询，记录成功和失败状态）
     *
     * @param callbackDto    回调数据
     * @param requestId      请求ID
     * @param isSuccess      处理是否成功
     * @param errorMessage   错误信息（失败时）
     * @param successMessage 成功信息（成功时）
     *
     * @author RayChou
     * @date 2025-10-30
     */
    private void updateSendRecordCallback(SimilarAnchorCallbackDto callbackDto, String requestId,
                                          boolean isSuccess, String errorMessage, String successMessage) {
        try {
            log.debug("[更新记录] 查询发送记录，requestId: {}", requestId);

            SimilarSendRecordEntity sendRecord = similarSendRecordService.getOne(
                new LambdaQueryWrapper<SimilarSendRecordEntity>()
                    .eq(SimilarSendRecordEntity::getRequestId, requestId)
                    .last("LIMIT 1")
            );

            if (ObjectUtil.isNotNull(sendRecord)) {
                // 保存回调数据
                sendRecord.setCallbackBody(JSON.toJSONString(callbackDto));

                // 根据处理结果设置回调状态和备注
                if (isSuccess) {
                    sendRecord.setCallbackStatus("SUCCESS");
                    // 优先使用详细的成功信息，否则使用默认信息
                    if (StrUtil.isNotBlank(successMessage)) {
                        sendRecord.setRemark(successMessage);
                        log.info("[更新记录] ✓ 更新发送记录回调状态为成功，记录ID: {}, requestId: {}, 备注: {}",
                            sendRecord.getId(), requestId, successMessage);
                    } else {
                        sendRecord.setRemark("回调处理成功");
                        log.info("[更新记录] ✓ 更新发送记录回调状态为成功，记录ID: {}, requestId: {}",
                            sendRecord.getId(), requestId);
                    }
                } else {
                    sendRecord.setCallbackStatus("FAIL");
                    // 保存错误信息到备注字段
                    if (StrUtil.isNotBlank(errorMessage)) {
                        sendRecord.setRemark(errorMessage);
                        log.warn("[更新记录] ⚠ 更新发送记录回调状态为失败，记录ID: {}, requestId: {}, 错误: {}",
                            sendRecord.getId(), requestId, errorMessage);
                    } else {
                        sendRecord.setRemark("回调处理失败，原因未知");
                        log.warn("[更新记录] ⚠ 更新发送记录回调状态为失败，记录ID: {}, requestId: {}",
                            sendRecord.getId(), requestId);
                    }
                }

                sendRecord.setUpdateDate(new Date());
                similarSendRecordService.updateById(sendRecord);
            } else {
                log.warn("[更新记录] ❌ 未找到对应的发送记录，requestId: {}", requestId);
            }
        } catch (Exception e) {
            log.error("[更新记录] ❌ 更新发送记录回调状态失败，requestId: {}", requestId, e);
            throw e;
        }
    }

    /**
     * 分页查询行业热榜数据（使用XML映射优化性能）
     *
     * @param page        页码
     * @param limit       每页条数
     * @param tradeId     行业ID
     * @param anchorName
     * @param liveKeyword
     *
     * @return 分页数据
     *
     * @author RayChou
     * @date 2025-10-28
     */
    public PageUtils<TradeRankVo> pageTradeRank(Integer page, Integer limit, Long tradeId, String anchorName, String liveKeyword) {
        // 获取用户信息和租户信息
        UserCacheVo userCacheVo = userFeign.getLocalUser().getData();
        OrderInfoVo orderInfoVo = orderFeign.currentOrderByUserId(userCacheVo.getId());
        if (Objects.isNull(orderInfoVo) || orderInfoVo.getLevel() < 20) {
            throw new BusinessException(StatusCode.USER_PACKAGE_VERSION_UPGRADE);
        }
        // 1. 使用XML映射的连接查询（INNER JOIN），性能更优，可以利用索引
        Page<SimilarAnchorEntity> pageParam = new Page<>(page, limit);
        IPage<SimilarAnchorEntity> anchorPage = similarAnchorDao.pageTradeRank(pageParam, tradeId, anchorName, liveKeyword);
        // 2. 转换为TradeRankVo（直接使用数据库中的热度字段，不重新计算）
        List<TradeRankVo> tradeRankVoList = anchorPage.getRecords().stream()
            .map(this::convertToTradeRankVo)
            .collect(Collectors.toList());
        return new PageUtils<>(anchorPage, tradeRankVoList);
    }

    /**
     * 后台管理系统-分页查询行业热榜数据（返回所有字段，支持多条件筛选）
     *
     * @param queryBo 查询条件BO
     *
     * @return 分页数据
     *
     * @author RayChou
     * @date 2025-10-28
     */
    public PageUtils<TradeRankAdminVo> pageTradeRankAdmin(TradeRankAdminQueryBo queryBo) {
        // 1. 使用XML映射的查询（支持行业ID可选、主播账号精确筛选、主播名称模糊搜索、热度范围筛选、更新时间范围筛选、排序）
        Page<SimilarAnchorEntity> pageParam = new Page<>(queryBo.getPage(), queryBo.getLimit());
        IPage<TradeRankAdminVo> anchorPage = similarAnchorDao.pageTradeRankAdmin(
            pageParam, queryBo.getTradeId(), queryBo.getUniqueId(), queryBo.getAnchorName(),
            queryBo.getHeatStart(), queryBo.getHeatEnd(),
            queryBo.getUpdateTimeStart(), queryBo.getUpdateTimeEnd(),
            queryBo.getOrderBy(), queryBo.getOrderDirection(), queryBo.getInStock(), queryBo.getUpRanking(),
            queryBo.getSourceTypes(), queryBo.getLiveKeyword(), queryBo.getEffective(), queryBo.getCollectStatus());
        Complete.start(anchorPage.getRecords())
            // 前置账户
            .build(anchorUrlService::getAnchorMap)
            .filter(row -> row.getPreAnchorId() != null && row.getPreAnchorId() > 0)
            .add(TradeRankAdminVo::getPreAnchorId, (row, anchor) -> {
                // row.setSystemAnchorTradeId(anchor.getSystemTradeId() == null ? anchor.getAiCorrectTradeId() : anchor.getSystemTradeId());
                row.setSystemAnchorName(anchor.getAnchorName());
                row.setSystemAnchorAvatar(anchor.getAnchorAvatar());
                row.setSystemSecUid(anchor.getSecUid());
                row.setSystemAnchorNumber(anchor.getAnchorNumber());
            })
            .doThen() // 继续一次循环补充行业名称 和 获取主播关键词
            .build(anchorUrlUserService::getAnchorTopTradeMap) // 从用户所选行业中计算一个出来
            .filter(row -> row.getSystemAnchorTradeId() == null && EmptyUtil.isNotEmpty(row.getSystemSecUid()))
            .add(TradeRankAdminVo::getSystemSecUid, TradeRankAdminVo::setSystemAnchorTradeId)
            .then()
            .build(cruxWordsRelaService::getAnchorWordsMap)
            .filter(row -> EmptyUtil.isNotEmpty(row.getSystemSecUid()))
            .add(TradeRankAdminVo::getSystemSecUid, TradeRankAdminVo::setSystemAnchorKeywords)
            .doThen()
            .build(tradeService::getTradeNameMap)
            .add(TradeRankAdminVo::getSystemTradeId, TradeRankAdminVo::setSystemTradeName)
            .add(TradeRankAdminVo::getSystemAnchorTradeId, TradeRankAdminVo::setSystemAnchorTradeName)
            .then().over(); // 结束补充 释放资源
        return new PageUtils<>(anchorPage);
    }

    /**
     * 将SimilarAnchorEntity转换为TradeRankVo（直接使用数据库热度字段）
     *
     * @param entity 相似主播实体
     *
     * @return 行业热榜视图对象
     *
     * @author RayChou
     * @date 2025-10-28
     */
    private TradeRankVo convertToTradeRankVo(SimilarAnchorEntity entity) {
        TradeRankVo vo = new TradeRankVo();

        vo.setId(entity.getId());
        vo.setAnchorId(entity.getAuthorId());
        vo.setAnchorPlatformType((byte) 1); // 抖音平台
        vo.setAnchorName(entity.getAnchorName());
        vo.setAnchorAvatar(entity.getAnchorAvatar());
        vo.setAnchorPlatformAccount(entity.getUniqueId());
        vo.setAccountHeat(entity.getAccountHeat().toString());
        vo.setFollowerCount(SimilarAnchorEntity.parseLong(entity.getFollowerCount()));
        vo.setTotalAmount(entity.getTotalAmount());
        vo.setLiveAverageUser(entity.getLiveAverageUser());
        // 转换更新时间
        if (entity.getUpdateDate() != null) {
            vo.setUpdateTime(entity.getUpdateDate().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime());
        }
        return vo;
    }


    /**
     * 批量修正相似达人行业
     *
     * @param similarRankingIds 批量相似主播榜单ID
     * @param tradeId           新的行业ID
     */
    @Transactional(rollbackFor = Exception.class)
    public R<Void> batchCorrectTrade(List<Long> similarRankingIds, long tradeId) {
        if (EmptyUtil.isEmpty(similarRankingIds)) {
            return R.error(StatusCode.ILLEGAL_ARGUMENT_EX.getCode(), "请选择需要修正的主播");
        }
        long operationUserId = userFeign.getLocalUser().getData().getId();
        // 1. 检查关系是否存在
        List<TbHotSearchTradeRankingList> tradeRankingLists = hotSearchTradeRankingListMapper.selectBatchIds(similarRankingIds);

        if (EmptyUtil.isEmpty(tradeRankingLists) || tradeRankingLists.size() != similarRankingIds.size()) {
            log.warn("[行业修正] ❌ 收藏关系不存在");
            throw new BusinessException(StatusCode.ILLEGAL_ARGUMENT_EX.getCode(), "收藏关系不存在");
        }
        List<TbHotSearchTradeRankingList> allowUpdateList = tradeRankingLists.stream().filter(rank -> {
            return !Objects.equals(rank.getSystemTradeId(), tradeId);
        }).toList();
        // 没有需要更改的
        if (EmptyUtil.isEmpty(allowUpdateList)) {
            return R.ok();
        }

        allowUpdateList.forEach(ranking -> {
            Long systemTradeId = ranking.getSystemTradeId();
            // 3. 直接修改行业ID
            ranking.setTradeId(tradeId);
            ranking.setSystemTradeId(tradeId);
            ranking.setLastUpdateSystemTradeTime(LocalDateTime.now());
            ranking.setUpdateDate(ranking.getLastUpdateSystemTradeTime());
            ranking.setUpdatedBy(operationUserId);
            log.info("[行业热榜] 修正行业 ，similarRankingId: {}, 原行业: {} tradeId: {}", ranking.getId(), systemTradeId, tradeId);
        });
        hotSearchTradeRankingListMapper.updateBatchSelective(allowUpdateList);
        // 关联系统主播的榜单
        List<Long> systemAnchorIds = allowUpdateList.stream().filter(ranking -> ranking.getSystemAnchorId() != null && ranking.getSystemAnchorId() > 0)
            .map(TbHotSearchTradeRankingList::getSystemAnchorId).toList();
        if (EmptyUtil.isNotEmpty(systemAnchorIds)) {
            anchorUrlService.batchUpdateSystemTradeId(systemAnchorIds, tradeId);
            log.info("[行业热榜] 批量关联系统主播的榜单，systemAnchorIds: {}, tradeId: {}", systemAnchorIds, tradeId);
        }
        // 删除缓存
        this.clearCache();
        return R.ok();
    }

    /**
     * 后台管理-删除相似达人关系
     *
     * @param similarRankingId 相似主播榜单ID
     *
     * @author RayChou
     * @date 2025-11-05
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteSimilarCollect(Long similarRankingId) {
        long operationUserId = userFeign.getLocalUser().getData().getId();
        TbHotSearchTradeRankingList rankingList = hotSearchTradeRankingListMapper.selectById(similarRankingId);
        // 1. 检查收藏关系是否存在
        if (rankingList == null) {
            log.warn("[删除关系] ❌ 收藏关系不存在，similarCollectId: {}", similarRankingId);
            throw new BusinessException(StatusCode.ILLEGAL_ARGUMENT_EX.getCode(), "收藏关系不存在");
        }
        // 2. 逻辑删除（设置 is_deleted = 1）
        boolean remove = hotSearchTradeRankingListMapper.batchDelete(List.of(rankingList.getId()), operationUserId) > 0;
        if (remove) {
            // 删除缓存
            this.clearCache();
        }
        return remove;
    }

    /**
     * 后台管理-刷新历史相似达人行业关联数据
     *
     * @param cutoffTime 截止时间
     *
     * @return 操作结果
     *
     * @author RayChou
     * @date 2025-11-06
     */
    public R<String> refreshHistoryData(LocalDateTime cutoffTime) {
        BatchQuery<Long, SimilarAnchorEntity> batchQuery = new BatchQuery<>((limit, idx) -> {
            return ChainWrappers.lambdaQueryChain(similarAnchorDao)
                .lt(idx != null, SimilarAnchorEntity::getId, idx)
                .lt(cutoffTime != null, SimilarAnchorEntity::getCreateDate, cutoffTime)
                .orderByDesc(SimilarAnchorEntity::getId)
                .last("limit " + limit)
                .list();
        }, SimilarAnchorEntity::getId);

        batchQuery.consumer(rows -> {
            List<Long> similarAnchorIds = rows.stream().map(SimilarAnchorEntity::getId).toList();
            Map<Long, TbHotSearchTradeRankingList> rankingMap = getSimilarAnchorRankingListMap(similarAnchorIds);
            rows.forEach(row -> {
                // 解析总金额
                row.calcTotalAmountToValue();
                if (EmptyUtil.isEmpty(row.getLiveAverageAmount())) {

                    if (row.getTotalAmountNumeric() != null && row.getTotalAmountNumeric() > 0 && row.getLiveCount() != null && row.getLiveCount() > 0) {
                        row.setLiveAverageAmount(BigDecimal.valueOf(row.getTotalAmountNumeric()).divide(BigDecimal.valueOf(row.getLiveCount()), 2, RoundingMode.HALF_UP).toString());
                        log.info("[行业热榜] 刷新历史数据，重新计算-场均销售额 主播: {}, 抖音号: {},计算结果: {}", row.getAnchorName(), row.getUniqueId(), row.getLiveAverageAmount());
                    } else {
                        row.setLiveAverageAmount("0");
                        log.warn("[行业热榜] 刷新历史数据，重新计算-场均销售额 主播: {}, 抖音号: {}, 销售额或直播场次数据为空或<=0. 计算结果: 0.00", row.getAnchorName(), row.getUniqueId());
                    }
                }
                row.calcAccountHeat();
                // 重新计算排行榜权重
                TbHotSearchTradeRankingList rankingList = rankingMap.get(row.getId());
                if (rankingList != null) {
                    rankingList.calcWeightScore(row);
                }
            });
            similarAnchorDao.batchUpdateSelective(rows);
            if (EmptyUtil.isNotEmpty(rankingMap)) {
                hotSearchTradeRankingListMapper.updateBatchSelective(rankingMap.values());
            }
        });
        batchQuery.run();
        this.clearCache();
        return R.ok();
    }


    /**
     * 后台管理-批量删除行业相似达人
     *
     * @param similarRankingIds 相似主播榜单ID列表
     *
     * @return 操作结果
     *
     * @author RayChou
     * @date 2025-11-12
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean batchDeleteSimilarCollect(List<Long> similarRankingIds) {
        // 1. 参数校验
        if (CollectionUtil.isEmpty(similarRankingIds)) {
            log.warn("[批量删除相似达人] ❌ 参数为空");
            throw new BusinessException(StatusCode.ILLEGAL_ARGUMENT_EX.getCode(), "删除ID列表不能为空");
        }

        long userId = userFeign.getLocalUser().getData().getId();


        // 2. 批量查询收藏关系是否存在
        List<TbHotSearchTradeRankingList> tradeRankingLists = hotSearchTradeRankingListMapper.selectBatchIds(similarRankingIds);
        if (CollectionUtil.isEmpty(tradeRankingLists)) {
            log.warn("[批量删除相似达人] ❌ 未找到任何收藏关系，similarCollectIds: {}", similarRankingIds);
            throw new BusinessException(StatusCode.ILLEGAL_ARGUMENT_EX.getCode(), "未找到任何收藏关系");
        }

        // 3. 检查是否有不存在的ID
        if (tradeRankingLists.size() != similarRankingIds.size()) {
            Set<Long> existingIds = tradeRankingLists.stream()
                .map(TbHotSearchTradeRankingList::getId)
                .collect(Collectors.toSet());
            List<Long> notFoundIds = similarRankingIds.stream()
                .filter(id -> !existingIds.contains(id))
                .collect(Collectors.toList());
            log.warn("[批量删除相似达人] ⚠️ 部分ID不存在，不存在的ID: {}", notFoundIds);
        }
        // 4. 批量逻辑删除
        boolean remove = hotSearchTradeRankingListMapper.batchDelete(similarRankingIds, userId) > 0;
        if (remove) {
            // 删除缓存
            this.clearCache();
        }
        return remove;
    }

    /**
     * 后台管理-更改行业热榜生效状态
     *
     * @param tradeId     行业ID
     * @param rankEnabled 热榜生效状态（0:不生效, 1:生效）
     *
     * @return 操作结果
     *
     * @author RayChou
     * @date 2025-11-12
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTradeRankEnabled(Long tradeId, Integer rankEnabled) {

        // 1. 参数校验
        if (tradeId == null) {
            log.warn("[更改热榜状态] ❌ 行业ID为空");
            throw new BusinessException(StatusCode.ILLEGAL_ARGUMENT_EX.getCode(), "行业ID不能为空");
        }

        if (rankEnabled == null || (rankEnabled != 0 && rankEnabled != 1)) {
            log.warn("[更改热榜状态] ❌ 热榜状态参数错误，rankEnabled: {}", rankEnabled);
            throw new BusinessException(StatusCode.ILLEGAL_ARGUMENT_EX.getCode(), "热榜状态参数错误，只能为0或1");
        }

        // 2. 检查行业是否存在
        TradeEntity trade = tradeService.getById(tradeId);
        if (trade == null) {
            log.warn("[更改热榜状态] ❌ 行业不存在，tradeId: {}", tradeId);
            throw new BusinessException(StatusCode.ILLEGAL_ARGUMENT_EX.getCode(), "行业不存在");
        }

        // 3. 如果是开启状态，检查行业是否在有效的叶子节点列表中
        if (rankEnabled == 1) {
            List<Long> validLeafTradeIds = getLeafTradeIds();
            if (!validLeafTradeIds.contains(tradeId)) {
                log.warn("[更改热榜状态] ❌ 行业ID: {} 不在有效的叶子节点行业列表中，不能开启热榜", tradeId);
                throw new BusinessException(StatusCode.ILLEGAL_ARGUMENT_EX.getCode(),
                    "该行业不是有效的叶子节点行业，不能开启热榜。可能原因：1.该行业已被配置排除 2.该行业不是最底级行业");
            }
            log.info("[更改热榜状态] ✓ 行业ID: {} 在有效的叶子节点行业列表中，可以开启热榜", tradeId);
        }

        // 4. 更新状态
        trade.setRankEnabled(rankEnabled);
        trade.setUpdateDate(new Date());

        // 5. 删除缓存
        boolean result = tradeService.updateById(trade);
        if (result) {
            this.clearCache();
        }
        return result;
    }


    /**
     * 获取所有关闭的行业ID（不生效）
     *
     * @return 关闭的行业ID列表
     */
    public List<Long> getCloseRankTradeIds() {
        return FunctionUtil.batchQuery((limit, idx) -> {
            return tradeService.lambdaQuery().select(TradeEntity::getId)
                .gt(idx != null, TradeEntity::getId, idx)
                .eq(TradeEntity::getRankEnabled, 0)
                .eq(TradeEntity::getIsDeleted, 0)
                .last(" limit " + limit)
                .list();
        }, TradeEntity::getId).stream().map(TradeEntity::getId).toList();
    }

    /**
     * 获取所有开启的行业ID（生效）
     *
     * @param tradeIds 行业ID列表
     *
     * @return 开启的行业ID列表
     *
     * @author RayChou
     * @date 2025-11-13
     */
    public List<Long> getOpenRankTradeIds(Collection<Long> tradeIds) {
        if (EmptyUtil.isEmpty(tradeIds)) {
            return List.of();
        }
        return tradeService.lambdaQuery().select(TradeEntity::getId)
            .in(TradeEntity::getId, tradeIds)
            .eq(TradeEntity::getRankEnabled, 1)
            .eq(TradeEntity::getIsDeleted, 0)
            .select(TradeEntity::getId)
            .list().stream().map(TradeEntity::getId).toList();
    }

    /**
     * 获取所有叶子行业ID
     *
     * @return 叶子行业ID列表
     *
     * @author RayChou
     * @date 2025-11-13
     */
    public List<Long> calcLeafTrade(List<TradeEntity> allTrades) {
        // 2. 获取排除的行业ID列表（从数据库键值表中读取）
        List<Long> excludeTradeIds = new ArrayList<>();
        SystemKvEntity tradeRankExcludeIdsKv = systemKvService.getByKey("trade_rank_exclude_ids");
        if (tradeRankExcludeIdsKv != null && StrUtil.isNotBlank(tradeRankExcludeIdsKv.getKvValue())) {
            try {
                // 解析逗号分隔的配置值，例如：1,4402547711715311616
                String[] idArray = tradeRankExcludeIdsKv.getKvValue().split(",");
                for (String idStr : idArray) {
                    String trimmedId = idStr.trim();
                    if (StrUtil.isNotBlank(trimmedId)) {
                        long tradeId = Long.parseLong(trimmedId);
                        // 过滤掉 0（0 是顶级行业的 parent_id，不是有效的行业ID）
                        if (tradeId != 0) {
                            excludeTradeIds.add(tradeId);
                        } else {
                            log.warn("[获取最底级行业ID] ⚠️ trade_rank_exclude_ids 配置中包含无效的行业ID: 0（0是顶级行业的parent_id，已自动过滤）");
                        }
                    }
                }
            } catch (Exception e) {
                log.error("[获取最底级行业ID] ❌ 解析排除行业ID配置失败，配置值: {}", tradeRankExcludeIdsKv.getKvValue(), e);
                excludeTradeIds = new ArrayList<>();
            }
        }

        // 3. 获取额外包含的行业ID列表（从数据库键值表中读取）
        List<Long> includeTradeIds = new ArrayList<>();
        SystemKvEntity tradeRankIncludeIdsKv = systemKvService.getByKey("trade_rank_include_ids");
        if (tradeRankIncludeIdsKv != null && StrUtil.isNotBlank(tradeRankIncludeIdsKv.getKvValue())) {
            try {
                // 解析逗号分隔的配置值，例如：5,6
                String[] idArray = tradeRankIncludeIdsKv.getKvValue().split(",");
                for (String idStr : idArray) {
                    String trimmedId = idStr.trim();
                    if (StrUtil.isNotBlank(trimmedId)) {
                        long tradeId = Long.parseLong(trimmedId);
                        // 过滤掉 0（0 是顶级行业的 parent_id，不是有效的行业ID）
                        if (tradeId != 0) {
                            includeTradeIds.add(tradeId);
                        } else {
                            log.warn("[获取最底级行业ID] ⚠️ trade_rank_include_ids 配置中包含无效的行业ID: 0（0是顶级行业的parent_id，已自动过滤）");
                        }
                    }
                }
            } catch (Exception e) {
                log.error("[获取最底级行业ID] ❌ 解析包含行业ID配置失败，配置值: {}", tradeRankIncludeIdsKv.getKvValue(), e);
                includeTradeIds = new ArrayList<>();
            }
        }

        // 4. 构建排除的行业ID集合（递归包含所有子行业）
        // 注意：如果某个ID同时在 exclude 和 include 中，优先使用 include（即不排除）
        Set<Long> excludeTradeIdSet = new HashSet<>();
        if (!excludeTradeIds.isEmpty()) {
            // 添加配置的排除行业ID
            excludeTradeIdSet.addAll(excludeTradeIds);

            // 构建父子关系映射
            Map<Long, List<TradeEntity>> parentIdMap = allTrades.stream()
                .collect(Collectors.groupingBy(TradeEntity::getParentId));

            // 递归查找所有需要排除的子行业
            for (Long excludeId : excludeTradeIds) {
                addAllChildrenToSet(excludeId, parentIdMap, excludeTradeIdSet);
            }

            // 从排除集合中移除 include 配置的ID（include 优先级更高）
            if (!includeTradeIds.isEmpty()) {
                Set<Long> includeSet = new HashSet<>(includeTradeIds);
                excludeTradeIdSet.removeAll(includeSet);
            }
        }

        // 5. 构建父子关系映射（用于判断是否为叶子节点）
        Map<Long, List<TradeEntity>> parentIdMap = allTrades.stream()
            .filter(trade -> !excludeTradeIdSet.contains(trade.getId())) // 过滤掉排除的行业
            .collect(Collectors.groupingBy(TradeEntity::getParentId));

        // 6. 查找所有叶子节点（没有子节点的行业）
        List<Long> leafTradeIds = allTrades.stream()
            .map(TradeEntity::getId) // 过滤掉排除的行业
            .filter(id -> !excludeTradeIdSet.contains(id))
            .filter(id -> {
                // 判断是否为叶子节点：该行业ID不在父ID映射中，或者映射的子节点列表为空
                List<TradeEntity> children = parentIdMap.get(id);
                return CollectionUtil.isEmpty(children);
            })
            .collect(Collectors.toList());

        // 7. 添加额外包含的行业ID（验证这些ID是否存在于行业表中）
        if (!includeTradeIds.isEmpty()) {
            Set<Long> allTradeIdSet = allTrades.stream()
                .map(TradeEntity::getId)
                .collect(Collectors.toSet());

            Set<Long> leafTradeIdSet = new HashSet<>(leafTradeIds);

            for (Long includeId : includeTradeIds) {
                // 验证ID是否存在
                if (!allTradeIdSet.contains(includeId)) {
                    log.warn("[获取最底级行业ID] ⚠️ trade_rank_include_ids 配置的行业ID不存在: {}", includeId);
                    continue;
                }

                // 如果不在叶子节点列表中，添加进去
                if (!leafTradeIdSet.contains(includeId)) {
                    leafTradeIds.add(includeId);
                }
            }
        }

        return leafTradeIds;
    }

    /**
     * 获取所有最底级的行业ID（叶子节点）
     * 支持两种配置：
     * 1. trade_rank_exclude_ids：排除指定的行业及其所有子行业
     * 2. trade_rank_include_ids：额外添加的有效行业ID列表（优先级高于 exclude）
     *
     * @return 最底级的行业ID列表
     *
     * @author RayChou
     * @date 2025-11-13
     */
    public List<Long> getLeafTradeIds() {
        // 1. 获取所有行业（未删除的）
//        LambdaQueryWrapper<TradeEntity> queryWrapper = new LambdaQueryWrapper<>();
//        queryWrapper.orderByAsc(TradeEntity::getSort);
//        List<TradeEntity> allTrades = tradeService.list(queryWrapper);
        List<TradeEntity> allTrades = new BatchQuery<>((limit, idx) -> {
            return tradeService.lambdaQuery()
                .gt(idx != null, TradeEntity::getId, idx)
                .eq(TradeEntity::getIsDeleted, 0)
                .last("limit " + limit).list();
        }, TradeEntity::getId).get().stream().sorted(Comparator.comparing(TradeEntity::getSort)).toList();

        if (CollectionUtil.isEmpty(allTrades)) {
            log.warn("[获取最底级行业ID] ❌ 未找到任何行业数据");
            return new ArrayList<>();
        }

       return this.calcLeafTrade(allTrades);
    }

    /**
     * 递归添加所有子行业ID到集合中
     *
     * @param parentId     父行业ID
     * @param parentIdMap  父子关系映射
     * @param excludeIdSet 排除的行业ID集合
     *
     * @author RayChou
     * @date 2025-11-13
     */
    private void addAllChildrenToSet(Long parentId, Map<Long, List<TradeEntity>> parentIdMap, Set<Long> excludeIdSet) {
        List<TradeEntity> children = parentIdMap.get(parentId);
        if (CollectionUtil.isEmpty(children)) {
            return;
        }

        for (TradeEntity child : children) {
            excludeIdSet.add(child.getId());
            // 递归添加子行业的子行业
            addAllChildrenToSet(child.getId(), parentIdMap, excludeIdSet);
        }
    }

    /**
     * 获取所有有效的叶子节点行业信息（包含ID和名称）
     * 用于测试验证排除逻辑是否正确
     *
     * @return 有效的叶子节点行业信息列表
     *
     * @author RayChou
     * @date 2025-11-13
     */
    public List<LeafTradeVo> getLeafTradeInfoList() {
        log.info("[获取有效叶子节点行业信息] 开始查询");

        // 1. 获取所有最底级行业ID
        List<Long> leafTradeIds = getLeafTradeIds();

        if (CollectionUtil.isEmpty(leafTradeIds)) {
            log.warn("[获取有效叶子节点行业信息] ❌ 未找到任何有效的叶子节点行业");
            return new ArrayList<>();
        }

        // 2. 查询这些行业的详细信息
        LambdaQueryWrapper<TradeEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(TradeEntity::getId, leafTradeIds);
        queryWrapper.orderByAsc(TradeEntity::getSort);
        List<TradeEntity> tradeEntities = tradeService.list(queryWrapper);

        // 3. 转换为VO
        List<LeafTradeVo> result = tradeEntities.stream()
            .map(entity -> new LeafTradeVo(
                entity.getId(),
                entity.getName(),
                entity.getParentId(),
                entity.getSort()
            ))
            .collect(Collectors.toList());

        log.info("[获取有效叶子节点行业信息] ✓ 查询完成，有效叶子节点数量: {}", result.size());

        return result;
    }

    /**
     * 统计行业指定渠道类型的主播数量
     *
     * @param isUp          是否为上架主播
     * @param lastUpdate    最后更新时间
     * @param channelTypes  渠道类型列表
     * @param collectStatus 采集状态
     *
     * @return 行业对应的主播数量
     */
    public Map<Long, Long> countTradeAnchor(Boolean isUp, LocalDateTime lastUpdate, List<Integer> channelTypes, Integer collectStatus, Collection<Long> tradeIds) {
        if (EmptyUtil.isEmpty(tradeIds)) {
            return Map.of();
        }
        List<Entry<Long, Long>> entryList = hotSearchTradeRankingListMapper.countTradeAnchor(isUp, lastUpdate, channelTypes, collectStatus, tradeIds);
        if (EmptyUtil.isEmpty(entryList)) {
            return Map.of();
        }
        return entryList.stream()
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }

    /**
     * 批量保存系统行业主播数据
     *
     * @param saveMap 保存的数据映射 key 系统主播 value 行业ID
     */
    @Transactional(rollbackFor = Throwable.class)
    public void batchSaveSystemTradeAnchor(Map<AnchorUrlEntity, Long> saveMap) {
        if (EmptyUtil.isEmpty(saveMap)) {
            log.warn("[批量保存系统行业主播数据] ⚠️ 无需保存数据(空集合)");
            return;
        }

        log.info("[批量保存系统行业主播数据] 开始处理，待处理主播数量: {}", saveMap.size());

        // 1. 获取已存在的系统主播榜单映射
        Set<Long> anchorIds = saveMap.keySet().stream().map(AnchorUrlEntity::getId).collect(Collectors.toSet());
        Map<Long, TbHotSearchTradeRankingList> systemAnchorRankingMap = this.getSystemAnchorRankingListMap(anchorIds);
        log.info("[批量保存系统行业主播数据] 查询到已存在关联关系的主播数量: {}", systemAnchorRankingMap.size());

        // 2. 分离已存在和不存在关联的主播
        Map<AnchorUrlEntity, Long> existsAnchorMap = saveMap.entrySet().stream()
            .filter(entry -> systemAnchorRankingMap.containsKey(entry.getKey().getId()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Map<AnchorUrlEntity, Long> notExistsAnchorMap = saveMap.entrySet().stream()
            .filter(entry -> !systemAnchorRankingMap.containsKey(entry.getKey().getId()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        log.info("[批量保存系统行业主播数据] 已存在关联的主播数量: {}, 新增关联的主播数量: {}",
            existsAnchorMap.size(), notExistsAnchorMap.size());

        // 3. 处理已存在的主播关联（更新相似达人信息）
        if (EmptyUtil.isNotEmpty(existsAnchorMap)) {
            processExistingAnchors(existsAnchorMap, systemAnchorRankingMap);
        }

        // 4. 处理新主播关联（创建相似达人和关联关系）
        if (EmptyUtil.isNotEmpty(notExistsAnchorMap)) {
            processNewAnchors(notExistsAnchorMap);
        }

        log.info("[批量保存系统行业主播数据] 处理完成");
    }

    /**
     * 处理已存在的主播关联（更新相似达人信息）
     *
     * @param existsAnchorMap        已存在关联的主播映射
     * @param systemAnchorRankingMap 系统主播对应的榜单映射
     */
    private void processExistingAnchors(Map<AnchorUrlEntity, Long> existsAnchorMap,
                                        Map<Long, TbHotSearchTradeRankingList> systemAnchorRankingMap) {
        log.info("[处理已存在主播] 开始处理已存在关联的主播数量: {}", existsAnchorMap.size());

        // 获取已存在相似达人的信息
        Map<AnchorUrlEntity, Long> existsSimilarAnchorMap = existsAnchorMap.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> systemAnchorRankingMap.get(entry.getKey().getId()).getSimilarAnchorId()));

        Map<Long, SimilarAnchorEntity> similarAnchorMap = ChainWrappers.lambdaQueryChain(similarAnchorDao)
            .in(SimilarAnchorEntity::getId, existsSimilarAnchorMap.values())
            .list().stream()
            .collect(Collectors.toMap(SimilarAnchorEntity::getId, Function.identity()));

        List<SimilarAnchorEntity> updateList = new ArrayList<>();
        for (Map.Entry<AnchorUrlEntity, Long> entry : existsSimilarAnchorMap.entrySet()) {
            SimilarAnchorEntity similarAnchor = similarAnchorMap.get(entry.getValue());
            if (similarAnchor != null) {
                AnchorUrlEntity anchor = entry.getKey();

                // 记录更新前的信息
                log.debug("[更新相似达人信息] ID: {}, 原名称: {}, 原头像: {}, 原抖音号: {}, 原SecUid: {}",
                    similarAnchor.getId(), similarAnchor.getAnchorName(), similarAnchor.getAnchorAvatar(),
                    similarAnchor.getUniqueId(), similarAnchor.getSecUid());

                // 更新相似达人基本信息
                similarAnchor.setAnchorName(anchor.getAnchorName());
                similarAnchor.setAnchorAvatar(anchor.getAnchorAvatar());
                similarAnchor.setUniqueId(anchor.getAnchorNumber());
                similarAnchor.setSecUid(anchor.getSecUid());

                // 记录更新后的信息
                log.debug("[更新相似达人信息] ID: {}, 新名称: {}, 新头像: {}, 新抖音号: {}, 新SecUid: {}",
                    similarAnchor.getId(), similarAnchor.getAnchorName(), similarAnchor.getAnchorAvatar(),
                    similarAnchor.getUniqueId(), similarAnchor.getSecUid());

                updateList.add(similarAnchor);
            }
        }

        if (EmptyUtil.isNotEmpty(updateList)) {
            similarAnchorDao.batchUpdateBaseInfo(updateList);
            log.info("[处理已存在主播] 批量更新相似达人基础信息完成，更新数量: {}", updateList.size());
        } else {
            log.info("[处理已存在主播] 无需更新相似达人基础信息");
        }
    }

    /**
     * 处理新主播关联（创建相似达人和关联关系）
     *
     * @param notExistsAnchorMap 不存在关联的主播映射
     */
    private void processNewAnchors(Map<AnchorUrlEntity, Long> notExistsAnchorMap) {
        log.info("[处理新主播] 开始处理新主播关联数量: {}", notExistsAnchorMap.size());

        List<String> anchorNumbers = notExistsAnchorMap.keySet().stream()
            .map(AnchorUrlEntity::getAnchorNumber)
            .toList();

        // 获取已存在主播数据(根据抖音账户)
        Map<String, SimilarAnchorEntity> numberAnchorMap = ChainWrappers.lambdaQueryChain(similarAnchorDao)
            .in(SimilarAnchorEntity::getUniqueId, anchorNumbers)
            .eq(SimilarAnchorEntity::getIsDeleted, 0)
            .orderByDesc(SimilarAnchorEntity::getId)
            .list().stream()
            .collect(Collectors.toMap(SimilarAnchorEntity::getUniqueId, Function.identity(), FunctionUtil::mergeFirst));

        log.info("[处理新主播] 根据抖音号查询到已存在的相似达人数量: {}", numberAnchorMap.size());

        // 相似主播对应的榜单
        Map<Long, TbHotSearchTradeRankingList> similarAnchorRankingMap = new HashMap<>();
        if (EmptyUtil.isNotEmpty(numberAnchorMap)) {
            List<Long> existingSimilarAnchorIds = numberAnchorMap.values().stream()
                .map(SimilarAnchorEntity::getId)
                .toList();
            Map<Long, TbHotSearchTradeRankingList> rankingListMap = this.getSimilarAnchorRankingListMap(existingSimilarAnchorIds);
            if (EmptyUtil.isNotEmpty(rankingListMap)) {
                similarAnchorRankingMap.putAll(rankingListMap);
                log.info("[处理新主播] 查询到已存在的相似达人关联榜单数量: {}", rankingListMap.size());
            }
        }

        List<SimilarAnchorEntity> updateList = new ArrayList<>();
        List<SimilarAnchorEntity> insertList = new ArrayList<>();
        List<TbHotSearchTradeRankingList> updateRankingList = new ArrayList<>();
        List<TbHotSearchTradeRankingList> insertRankingList = new ArrayList<>();

        for (Map.Entry<AnchorUrlEntity, Long> entry : notExistsAnchorMap.entrySet()) {
            AnchorUrlEntity anchor = entry.getKey();
            Long tradeId = entry.getValue();

            SimilarAnchorEntity similarAnchor = numberAnchorMap.get(anchor.getAnchorNumber());

            if (similarAnchor != null) {
                // 更新已存在的相似达人信息
                log.debug("[更新相似达人信息] 发现已存在相似达人，ID: {}, 抖音号: {}",
                    similarAnchor.getId(), similarAnchor.getUniqueId());

                // 记录更新前的信息
                log.debug("[更新相似达人信息] ID: {}, 原名称: {}, 原头像: {}, 原SecUid: {}",
                    similarAnchor.getId(), similarAnchor.getAnchorName(), similarAnchor.getAnchorAvatar(),
                    similarAnchor.getSecUid());

                similarAnchor.setAnchorName(anchor.getAnchorName());
                similarAnchor.setAnchorAvatar(anchor.getAnchorAvatar());
                similarAnchor.setSecUid(anchor.getSecUid());

                // 记录更新后的信息
                log.debug("[更新相似达人信息] ID: {}, 新名称: {}, 新头像: {}, 新SecUid: {}",
                    similarAnchor.getId(), similarAnchor.getAnchorName(), similarAnchor.getAnchorAvatar(),
                    similarAnchor.getSecUid());

                updateList.add(similarAnchor);

                TbHotSearchTradeRankingList rankingList = similarAnchorRankingMap.get(similarAnchor.getId());
                if (rankingList != null) {
                    // 更新现有榜单记录的系统主播ID
                    if (rankingList.getSystemAnchorId() == null || rankingList.getSystemAnchorId() <= 0) {
                        log.debug("[更新榜单记录] 更新现有榜单记录，相似达人ID: {}, 系统主播ID: {}",
                            similarAnchor.getId(), anchor.getId());

                        Long oldSystemAnchorId = rankingList.getSystemAnchorId();
                        rankingList.setSystemAnchorId(anchor.getId());
                        if (anchor.getSystemTradeId() != null && anchor.getSystemTradeId() > 0) {
                            rankingList.setSystemTradeId(anchor.getSystemTradeId());
                            rankingList.setTradeId(anchor.getSystemTradeId());
                        }

                        log.debug("[更新榜单记录] 榜单记录更新完成，原系统主播ID: {}, 新系统主播ID: {}",
                            oldSystemAnchorId, rankingList.getSystemAnchorId());

                        updateRankingList.add(rankingList);
                    } else {
                        log.debug("[更新榜单记录] 榜单记录已有系统主播ID，跳过更新，相似达人ID: {}, 系统主播ID: {}",
                            similarAnchor.getId(), rankingList.getSystemAnchorId());
                    }
                } else {
                    // 创建新的榜单记录
                    log.debug("[创建榜单记录] 为相似达人创建新的榜单记录，相似达人ID: {}, 系统主播ID: {}",
                        similarAnchor.getId(), anchor.getId());

                    rankingList = createNewRankingList(similarAnchor.getId(), anchor, tradeId);
                    rankingList.setSystemAnchorId(0L);
                    rankingList.setPreAnchorId(anchor.getId());
                    insertRankingList.add(rankingList);

                    log.debug("[创建榜单记录] 新榜单记录创建完成，ID: {}, 相似达人ID: {}, 系统主播ID: {}",
                        rankingList.getId(), rankingList.getSimilarAnchorId(), rankingList.getSystemAnchorId());
                }
            } else {
                // 创建新的相似达人记录
                log.debug("[创建相似达人] 未找到匹配的相似达人，为抖音号: {} 创建新的相似达人记录", anchor.getAnchorNumber());

                similarAnchor = createNewSimilarAnchor(anchor);
                insertList.add(similarAnchor);

                log.debug("[创建相似达人] 新相似达人创建完成，ID: {}, 名称: {}, 抖音号: {}",
                    similarAnchor.getId(), similarAnchor.getAnchorName(), similarAnchor.getUniqueId());

                // 创建新的榜单记录
                TbHotSearchTradeRankingList rankingList = createNewRankingList(similarAnchor.getId(), anchor, tradeId);
                rankingList.setPreAnchorId(anchor.getId());
                insertRankingList.add(rankingList);

                log.debug("[创建榜单记录] 新榜单记录创建完成，ID: {}, 相似达人ID: {}, 系统主播ID: {}",
                    rankingList.getId(), rankingList.getSimilarAnchorId(), rankingList.getSystemAnchorId());
            }
        }

        // 执行批量更新和插入操作
        if (EmptyUtil.isNotEmpty(updateList)) {
            similarAnchorDao.batchUpdateBaseInfo(updateList);
            log.info("[处理新主播] 批量更新相似达人基础信息完成，更新数量: {}", updateList.size());
        }

        if (EmptyUtil.isNotEmpty(insertList)) {
            similarAnchorDao.batchInsertBaseInfo(insertList);
            log.info("[处理新主播] 批量插入相似达人基础信息完成，插入数量: {}", insertList.size());
        }

        if (EmptyUtil.isNotEmpty(updateRankingList)) {
            hotSearchTradeRankingListMapper.updateBatchSelective(updateRankingList);
            log.info("[处理新主播] 批量更新榜单记录完成，更新数量: {}", updateRankingList.size());
        }

        if (EmptyUtil.isNotEmpty(insertRankingList)) {
            hotSearchTradeRankingListMapper.batchInsert(insertRankingList);
            log.info("[处理新主播] 批量插入榜单记录完成，插入数量: {}", insertRankingList.size());
        }
    }

    /**
     * 创建新的相似达人记录
     *
     * @param anchor 系统主播实体
     *
     * @return 新的相似达人实体
     */
    private SimilarAnchorEntity createNewSimilarAnchor(AnchorUrlEntity anchor) {
        SimilarAnchorEntity similarAnchor = new SimilarAnchorEntity();
        similarAnchor.setAnchorName(anchor.getAnchorName());
        similarAnchor.setAnchorAvatar(anchor.getAnchorAvatar());
        similarAnchor.setUniqueId(anchor.getAnchorNumber());
        similarAnchor.setSecUid(anchor.getSecUid());
        similarAnchor.setAuthorId(""); // 新建时设置为空字符串
        similarAnchor.setCreateDate(new Date());
        similarAnchor.setUpdateDate(new Date());
        similarAnchor.setId(SnowflakeManager.nextValue());
        return similarAnchor;
    }

    /**
     * 创建新的榜单记录
     *
     * @param similarAnchorId 相似达人ID
     * @param anchor          系统主播实体
     * @param tradeId         行业ID
     *
     * @return 新的榜单记录实体
     */
    private TbHotSearchTradeRankingList createNewRankingList(Long similarAnchorId, AnchorUrlEntity anchor, Long tradeId) {
        TbHotSearchTradeRankingList rankingList = new TbHotSearchTradeRankingList();
        rankingList.setCreatedBy(0L);
        rankingList.setUpdatedBy(0L);
        rankingList.setSimilarAnchorId(similarAnchorId);
        rankingList.setSystemAnchorId(0L);
        rankingList.setSystemTradeId(0L);
        rankingList.setTradeId(tradeId);
        rankingList.setPreAnchorId(0L);
        // 确定最终的行业ID
        if (anchor != null) {
            rankingList.setSystemAnchorId(anchor.getId());
            Long finalTradeId = determineFinalTradeId(anchor, tradeId);
            rankingList.setTradeId(finalTradeId);
            rankingList.setSystemTradeId(anchor.getSystemTradeId() != null && anchor.getSystemTradeId() > 0 ? anchor.getSystemTradeId() : 0L);
        }

        rankingList.setLastUpdateSystemTradeTime(null);
        rankingList.setSourceType(1);
        rankingList.setCollectStatus(0);
        rankingList.setUpRanking(false);
        rankingList.setCreateDate(LocalDateTime.now());
        rankingList.setUpdateDate(rankingList.getCreateDate());
        rankingList.setIsDeleted(false);
        rankingList.setWeightScore(0L);

        return rankingList;
    }

    /**
     * 确定最终的行业ID
     *
     * @param anchor  系统主播实体
     * @param tradeId 原始行业ID
     *
     * @return 最终行业ID
     */
    private Long determineFinalTradeId(AnchorUrlEntity anchor, Long tradeId) {
        Long anchorTradeId = tradeId;
        long systemTradeId;

        if (anchor.getAiCorrectTradeId() != null && anchor.getAiCorrectTradeId() > 0) {
            anchorTradeId = anchor.getAiCorrectTradeId();
        }

        if (anchor.getSystemTradeId() != null && anchor.getSystemTradeId() > 0) {
            systemTradeId = anchor.getSystemTradeId();
            anchorTradeId = systemTradeId;
        }

        return anchorTradeId;
    }

    /**
     * 获取系统主播的榜单
     */
    public Map<Long, TbHotSearchTradeRankingList> getSystemAnchorRankingListMap(Collection<Long> systemAnchorIds) {
        if (EmptyUtil.isEmpty(systemAnchorIds)) {
            return Map.of();
        }
        return ChainWrappers.lambdaQueryChain(hotSearchTradeRankingListMapper)
            .in(TbHotSearchTradeRankingList::getSystemAnchorId, systemAnchorIds)
            .eq(TbHotSearchTradeRankingList::getIsDeleted, false)
            .list()
            .stream()
            .collect(Collectors.toMap(TbHotSearchTradeRankingList::getSystemAnchorId, Function.identity(), FunctionUtil::mergeFirst));
    }


    /**
     * 获取相似主播的榜单
     *
     * @param similarAnchorIds 相似主播ID列表
     *
     * @return 相似主播的榜单记录
     */
    public Map<Long, TbHotSearchTradeRankingList> getSimilarAnchorRankingListMap(Collection<Long> similarAnchorIds) {
        if (EmptyUtil.isEmpty(similarAnchorIds)) {
            return Map.of();
        }
        return ChainWrappers.lambdaQueryChain(hotSearchTradeRankingListMapper)
            .in(TbHotSearchTradeRankingList::getSimilarAnchorId, similarAnchorIds)
            .eq(TbHotSearchTradeRankingList::getIsDeleted, false)
            .list()
            .stream()
            .collect(Collectors.toMap(TbHotSearchTradeRankingList::getSimilarAnchorId, Function.identity()));
    }


    /**
     * 获取行业第三方榜单列表
     *
     * @param tradeId 行业ID
     *
     * @return 行业第三方榜单列表
     */
    public List<TradeThirdRankingVO> getThirdRankingList(long tradeId) {
        return ChainWrappers.lambdaQueryChain(thirdRankingRelationMapper)
            .eq(TradeThirdRankingRelation::getTradeId, tradeId)
            .list()
            .stream()
            .map(row -> {
                TradeThirdRankingVO tradeThirdRankingVO = new TradeThirdRankingVO();
                BeanUtils.copyProperties(row, tradeThirdRankingVO);
                return tradeThirdRankingVO;
            })
            .collect(Collectors.toList());
    }


    /**
     * 添加行业第三方榜单关联
     *
     * @param dto 添加参数
     *
     * @return 操作结果
     */
    public R<Void> addTradeThirdRanking(TradeThirdRankingDto dto) {
        Long userId = userFeign.getLocalUser().getData().getId();
        boolean exists = ChainWrappers.lambdaQueryChain(thirdRankingRelationMapper)
            .eq(TradeThirdRankingRelation::getThirdRankingId, dto.getThirdRankingId())
            .exists();
        if (exists) {
            log.info("[行业热榜] 添加第三方榜单关联ID: {} user: {} 已存在", dto.getThirdRankingId(), userId);
            return R.error(StatusCode.BASE_VALID_PARAM.getCode(), "第三方榜单已存在");
        }
        TradeThirdRankingRelation relation = new TradeThirdRankingRelation();
        relation.setTradeId(dto.getTradeId());
        relation.setThirdRankingId(dto.getThirdRankingId());
        relation.setThirdRankingName(dto.getThirdRankingName());
        relation.setDayOfWeek(dto.getDayOfWeek());
        relation.setCreateDate(LocalDateTime.now());
        relation.setUpdateDate(relation.getCreateDate());
        relation.setCreatedBy(userId);
        relation.setUpdatedBy(userId);
        relation.setEnableCollect(dto.getEnableCollect() == null || dto.getEnableCollect());
        int i = thirdRankingRelationMapper.insert(relation);
        log.info("[行业热榜] 添加第三方榜单关联ID: {} {} user: {} 添加结果: {}", dto.getThirdRankingId(), dto.getThirdRankingName(), userId, i > 0 ? "success" : "error");
        return i > 0 ? R.ok() : R.error(500, "添加失败");
    }

    /**
     * 修改行业第三方榜单关联
     */
    public R<Void> updateTradeThirdRanking(TradeThirdRankingDto dto) {
        if (dto.getId() == null) {
            return R.error(StatusCode.BASE_VALID_PARAM.getCode(), "ID不能为空");
        }
        TradeThirdRankingRelation rankingRelation = thirdRankingRelationMapper.selectById(dto.getId());
        if (rankingRelation == null) {
            return R.error(StatusCode.BASE_VALID_PARAM.getCode(), "第三方榜单关联不存在");
        }
        if (!Objects.equals(rankingRelation.getTradeId(), dto.getTradeId())) {
            return R.error(StatusCode.BASE_VALID_PARAM.getCode(), "第三方榜单关联行业ID不一致");
        }
        Long userId = userFeign.getLocalUser().getData().getId();
        boolean exists = ChainWrappers.lambdaQueryChain(thirdRankingRelationMapper)
            .eq(TradeThirdRankingRelation::getThirdRankingId, dto.getThirdRankingId())
            .ne(TradeThirdRankingRelation::getTradeId, dto.getTradeId())
            .exists();
        if (exists) {
            log.info("[行业热榜] 修改第三方榜单关联ID: {} user: {} 已存在", dto.getThirdRankingId(), userId);
            return R.error(StatusCode.BASE_VALID_PARAM.getCode(), "第三方榜单已存在");
        }
        rankingRelation.setThirdRankingId(dto.getThirdRankingId());
        rankingRelation.setThirdRankingName(dto.getThirdRankingName());
        rankingRelation.setDayOfWeek(dto.getDayOfWeek());
        rankingRelation.setUpdateDate(LocalDateTime.now());
        rankingRelation.setUpdatedBy(userId);
        rankingRelation.setEnableCollect(dto.getEnableCollect());
        int i = thirdRankingRelationMapper.updateById(rankingRelation);
        log.info("[行业热榜] 修改第三方榜单关联ID: {} {} user: {} 修改结果: {}", dto.getThirdRankingId(), dto.getThirdRankingName(), userId, i > 0 ? "success" : "error");
        return i > 0 ? R.ok() : R.error(500, "修改失败");
    }


    /**
     * 删除行业第三方榜单关联
     *
     * @param thirdRelationId 第三方榜单关联ID
     *
     * @return 操作结果
     */
    public R<Void> deleteTradeThirdRanking(long thirdRelationId) {
        Long userId = userFeign.getLocalUser().getData().getId();
        int i = thirdRankingRelationMapper.deleteById(thirdRelationId);
        log.info("[行业热榜] 删除第三方榜单关联ID: {} user: {} 删除结果: {}", thirdRelationId, userId, i > 0 ? "success" : "error");
        return i > 0 ? R.ok() : R.error(500, "删除失败");
    }

    /**
     * 上榜
     *
     * @param similarAnchorId 相似达人ID
     *
     * @return 操作结果
     */
    public R<Void> upRank(long similarAnchorId) {
        Long userId = userFeign.getLocalUser().getData().getId();
        Optional<TbHotSearchTradeRankingList> optional = ChainWrappers.lambdaQueryChain(hotSearchTradeRankingListMapper)
            .eq(TbHotSearchTradeRankingList::getSimilarAnchorId, similarAnchorId)
            .eq(TbHotSearchTradeRankingList::getIsDeleted, false).last("limit 1")
            .oneOpt();
        if (optional.isEmpty()) {
            return R.error(StatusCode.BASE_VALID_PARAM.getCode(), "热榜主播不存在");
        }
        TbHotSearchTradeRankingList rankingList = optional.get();
        if (rankingList.getWeightScore() <= 0) {
            return R.error(StatusCode.BASE_VALID_PARAM.getCode(), "销售额/平均场观不达标,无法上榜");
        }
        if (rankingList.getCollectStatus() != 2) {
            return R.error(StatusCode.BASE_VALID_PARAM.getCode(), "未完成数据采集,无法上榜");
        }
        boolean update = ChainWrappers.lambdaUpdateChain(hotSearchTradeRankingListMapper)
            .eq(TbHotSearchTradeRankingList::getId, rankingList.getId())
            .eq(TbHotSearchTradeRankingList::getIsDeleted, false)
            .eq(TbHotSearchTradeRankingList::getUpRanking, false)
            .set(TbHotSearchTradeRankingList::getUpRanking, true)
            .set(TbHotSearchTradeRankingList::getUpdatedBy, userId)
            .set(TbHotSearchTradeRankingList::getUpdateDate, LocalDateTime.now())
            .update();
        log.info("[行业热榜] 上榜 similarAnchorId: {} 上榜结果: {}", similarAnchorId, update ? "success" : "error");
        if (update) {
            clearCache();
            return R.ok();
        }
        return R.error("上榜失败");
    }

    /**
     * 下榜
     *
     * @param similarAnchorId 相似达人ID
     *
     * @return 操作结果
     */
    public R<Void> downRank(long similarAnchorId) {
        Long userId = userFeign.getLocalUser().getData().getId();
        boolean update = ChainWrappers.lambdaUpdateChain(hotSearchTradeRankingListMapper)
            .eq(TbHotSearchTradeRankingList::getSimilarAnchorId, similarAnchorId)
            .eq(TbHotSearchTradeRankingList::getIsDeleted, false)
            .eq(TbHotSearchTradeRankingList::getUpRanking, true)
            .set(TbHotSearchTradeRankingList::getUpRanking, false)
            .set(TbHotSearchTradeRankingList::getUpdatedBy, userId)
            .set(TbHotSearchTradeRankingList::getUpdateDate, LocalDateTime.now())
            .update();
        log.info("[行业热榜] 下榜 similarAnchorId: {} 下榜结果: {}", similarAnchorId, update ? "success" : "error");
        if (update) {
            clearCache();
            return R.ok();
        }
        return R.error("下榜失败");
    }


    /**
     * 清除缓存
     */
    private void clearCache() {
        this.redisTemplate.delete(List.of("replay:trade:tree:null", "replay:trade:tree:0", "replay:trade:tree:1", "replay:trade:simple:tree:null", "replay:trade:simple:tree:0", "replay:trade:simple:tree:1"));
    }


    /**
     * 导入热榜主播
     *
     * @param inputStream 导入文件输入流
     * @param tradeId     行业ID
     *
     * @return 操作结果
     */
    public R<Void> importAnchor(InputStream inputStream, long tradeId) {
        // 错误信息
        Map<Integer, StringBuilder> rowErrorMap = new HashMap<>();
        Map<Integer, String> rowAnchorNameMap = new HashMap<>();
        // 行号计算器
        AtomicInteger rowNumCalculator = new AtomicInteger(0);
        // 添加错误信息
        BiConsumer<Integer, String> appendError = (rowNum, errorMsg) -> {
            StringBuilder sb = rowErrorMap.computeIfAbsent(rowNum, k -> new StringBuilder());
            if (!sb.isEmpty()) {
                sb.append(";");
            }
            sb.append(errorMsg);
        };
        Set<String> uniqueIdSet = new HashSet<>();
        long userId = userFeign.getLocalUser().getData().getId();
        AtomicInteger successCount = new AtomicInteger(0);
        ExcelUtils.readExcel(inputStream, ExcelUploadSimilarAnchor.class, rows -> {
            // 行号对应的数据
            Map<Integer, ExcelUploadSimilarAnchor> rowMap = rows.stream().collect(Collectors.toMap(r -> {
                int num = rowNumCalculator.incrementAndGet();
                rowAnchorNameMap.put(num, r.getAnchorName() == null ? "" : r.getAnchorName());
                return num;
            }, Function.identity()));
            // 先使用通用的验证器
            rowMap = rowMap.entrySet().stream().filter(entry -> {
                ExcelUploadSimilarAnchor anchorDto = entry.getValue();
                BeanValidationResult warpValidate = ValidationUtil.warpValidate(anchorDto);
                if (!warpValidate.isSuccess()) {
                    appendError.accept(entry.getKey(), warpValidate.getErrorMessages().stream().map(BeanValidationResult.ErrorMessage::getMessage).collect(Collectors.joining(",")));
                    return false;
                }
                if (!SimilarAnchorEntity.validateValueFormat(anchorDto.getTotalAmount(), anchorDto.getAnchorName(), "直播总销售额", true)) {
                    appendError.accept(entry.getKey(), "直播总销售额格式错误");
                    return false;
                }
                if (!SimilarAnchorEntity.validateValueFormat(anchorDto.getFollowerCount(), anchorDto.getAnchorName(), "粉丝量", true)) {
                    appendError.accept(entry.getKey(), "粉丝量格式错误");
                    return false;
                }
                if (!SimilarAnchorEntity.validateValueFormat(anchorDto.getLiveAverageUser(), anchorDto.getAnchorName(), "场均观", true)) {
                    appendError.accept(entry.getKey(), "场均观格式错误");
                    return false;
                }
                if (uniqueIdSet.contains(anchorDto.getUniqueId())) {
                    appendError.accept(entry.getKey(), "抖音号重复");
                    return false;
                }
                if (!NumberUtil.isInteger(anchorDto.getLiveCount())) {
                    appendError.accept(entry.getKey(), "直播场次格式错误请填入纯数字");
                    return false;
                }
                uniqueIdSet.add(anchorDto.getUniqueId());
                return true;
            }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            // 没有数据通过 那么全部数据都验证失败
            if (EmptyUtil.isEmpty(rowMap)) {
                return true;
            }
            // 抖音号
            List<String> existsAnchorNumbers = ChainWrappers.lambdaQueryChain(similarAnchorDao).select(SimilarAnchorEntity::getUniqueId)
                .in(SimilarAnchorEntity::getUniqueId, rowMap.values().stream().map(ExcelUploadSimilarAnchor::getUniqueId).toList())
                .eq(SimilarAnchorEntity::getIsDeleted, 0)
                .orderByDesc(SimilarAnchorEntity::getId)
                .list().stream().map(SimilarAnchorEntity::getUniqueId).toList();
            if (EmptyUtil.isNotEmpty(existsAnchorNumbers)) {
                // 验证抖音号是否已经在数据库中
                rowMap = rowMap.entrySet().stream().filter(entry -> {
                    boolean contains = existsAnchorNumbers.contains(entry.getValue().getUniqueId());
                    if (contains) {
                        appendError.accept(entry.getKey(), "抖音号已在库");
                        return false;
                    }
                    return true;
                }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                // 提前结束当前批次函数
                if (EmptyUtil.isEmpty(rowMap)) {
                    return true;
                }
            }
            // 根据抖音号查询系统主播
            Map<String, AnchorUrlEntity> anchorMap = anchorUrlService.getAnchorMapByAnchorNumber(rowMap.values().stream().map(ExcelUploadSimilarAnchor::getUniqueId).toList(), AnchorPlatformEnum.DOU_YIN.getCode());
            if (EmptyUtil.isNotEmpty(anchorMap)) {
                // 防御性编程，判断所关联的系统主播是否已经存在榜单
                List<Long> existsRankAnchorIds = ChainWrappers.lambdaQueryChain(hotSearchTradeRankingListMapper)
                    .select(TbHotSearchTradeRankingList::getSystemAnchorId)
                    .in(TbHotSearchTradeRankingList::getSystemAnchorId, anchorMap.values().stream().map(AnchorUrlEntity::getId).toList())
                    .eq(TbHotSearchTradeRankingList::getIsDeleted, false)
                    .list().stream().map(TbHotSearchTradeRankingList::getSystemAnchorId).toList();
                if (EmptyUtil.isNotEmpty(existsRankAnchorIds)) {
                    // 验证系统主播是否已经存在榜单
                    rowMap = rowMap.entrySet().stream().filter(entry -> {
                        AnchorUrlEntity anchor = anchorMap.get(entry.getValue().getUniqueId());
                        if (anchor == null) {
                            return true;
                        }
                        boolean contains = existsRankAnchorIds.contains(anchor.getId());
                        if (contains) {
                            appendError.accept(entry.getKey(), "系统主播已存在热榜中");
                            return false;
                        }
                        return true;
                    }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                    // 提前结束当前批次函数
                    if (EmptyUtil.isEmpty(rowMap)) {
                        return true;
                    }
                }
            }

            List<TbHotSearchTradeRankingList> rankingLists = new ArrayList<>(rowMap.size());
            List<SimilarAnchorEntity> similarAnchorEntityList = rowMap.values().stream().map(anchorDto -> {
                SimilarAnchorEntity similarAnchor = new SimilarAnchorEntity();
                similarAnchor.setId(SnowflakeManager.nextValue());
                similarAnchor.setAnchorName(anchorDto.getAnchorName());
                similarAnchor.setAnchorAvatar(anchorDto.getAnchorAvatar());
                similarAnchor.setUniqueId(anchorDto.getUniqueId());
                similarAnchor.setFollowerCount(anchorDto.getFollowerCount());
                similarAnchor.setLiveAverageUser(anchorDto.getLiveAverageUser());
                similarAnchor.setLiveAverageAmount(anchorDto.getLiveAverageAmount());
                similarAnchor.setTotalAmount(anchorDto.getTotalAmount());
                similarAnchor.setLiveCount(Integer.parseInt(anchorDto.getLiveCount()));
                // 计算
                similarAnchor.calcTotalAmountToValue();
                similarAnchor.calcAccountHeat();
                similarAnchor.setAuthorId("");
                similarAnchor.setCreateDate(new Date());
                similarAnchor.setUpdateDate(similarAnchor.getCreateDate());
                similarAnchor.setIsDeleted(0);
                similarAnchor.setLiveAverageOnline("");
                similarAnchor.setLiveAverageUv("");
                similarAnchor.setLiveTotalAmountCmmInd("");
                similarAnchor.setSecUid("");
                AnchorUrlEntity anchor = anchorMap.get(anchorDto.getUniqueId());
                if (EmptyUtil.isNotEmpty(anchor)) {
                    similarAnchor.setSecUid(anchor.getSecUid());
                    similarAnchor.setSimilarScore("1.0");
                }
                log.info("[行业热榜] 添加新主播: 抖音号: {}， 昵称:{}, tradeId: {}, 主播ID: {}", anchorDto.getUniqueId(), anchorDto.getAnchorName(), anchorDto.getTradeId(), similarAnchor.getId());
                TbHotSearchTradeRankingList rankingList = new TbHotSearchTradeRankingList();
                rankingList.setCreatedBy(userId);
                rankingList.setUpdatedBy(userId);
                rankingList.setSimilarAnchorId(similarAnchor.getId());
                rankingList.setSystemAnchorId(0L);
                rankingList.setTradeId(tradeId);
                rankingList.setSystemTradeId(0L);
                rankingList.setLastUpdateSystemTradeTime(null);
                rankingList.setSourceType(2);
                rankingList.setCollectStatus(2);
                rankingList.setUpRanking(false);
                rankingList.setCreateDate(LocalDateTime.now());
                rankingList.setUpdateDate(rankingList.getCreateDate());
                rankingList.setIsDeleted(false);
                rankingList.setWeightScore(0L);
                rankingList.calcWeightScore(similarAnchor);
                rankingList.setPreAnchorId(0L);
                if (EmptyUtil.isNotEmpty(anchor)) {
                    rankingList.setSystemAnchorId(anchor.getId());
                    rankingList.setSystemTradeId(anchor.getSystemTradeId());
                    if (rankingList.getSystemTradeId() != null && rankingList.getSystemTradeId() > 0) {
                        rankingList.setTradeId(rankingList.getSystemTradeId());
                    }
                    log.info("[行业热榜] 系统主播关联新主播: 抖音号: {}， 昵称:{}, tradeId: {}, 系统主播ID: {}, 系统行业ID: {}", anchorDto.getUniqueId(), anchorDto.getAnchorName(), anchorDto.getTradeId(), anchor.getId(), anchor.getSystemTradeId());
                }
                if (rankingList.getWeightScore() > 0) {
                    rankingList.setUpRanking(true);
                }
                rankingLists.add(rankingList);
                successCount.incrementAndGet();
                return similarAnchor;
            }).toList();
            similarAnchorDao.batchInsert(similarAnchorEntityList);
            hotSearchTradeRankingListMapper.batchInsert(rankingLists);
            return true;
        });
        log.info("[行业热榜] 批量导入主播: {}条, 失败: {}", successCount.get(), rowErrorMap.size());
        if (EmptyUtil.isNotEmpty(rowErrorMap)) {
            return R.error(StatusCode.BASE_VALID_PARAM.getCode(), rowErrorMap.entrySet().stream().map(entry -> "第【" + entry.getKey() + "】行" + rowAnchorNameMap.get(entry.getKey()) + ",添加失败原因：" + entry.getValue().toString()).collect(Collectors.joining("\r\n")));
        }
        return R.ok();
    }


    /**
     * 添加新主播
     *
     * @param anchorDto 新主播信息
     *
     * @return 操作结果
     */
    @Transactional(rollbackFor = Exception.class)
    public R<Void> addNewAnchor(ExcelUploadSimilarAnchor anchorDto) {
        if (!SimilarAnchorEntity.validateValueFormat(anchorDto.getTotalAmount(), anchorDto.getAnchorName(), "直播总销售额", true)) {
            return R.error(StatusCode.BASE_VALID_PARAM.getCode(), "直播总销售额格式错误");
        }
        if (!SimilarAnchorEntity.validateValueFormat(anchorDto.getFollowerCount(), anchorDto.getAnchorName(), "粉丝量", true)) {
            return R.error(StatusCode.BASE_VALID_PARAM.getCode(), "粉丝量格式错误");
        }
        if (!SimilarAnchorEntity.validateValueFormat(anchorDto.getLiveAverageUser(), anchorDto.getAnchorName(), "场均观", true)) {
            return R.error(StatusCode.BASE_VALID_PARAM.getCode(), "场均观格式错误");
        }
        log.info("[行业热榜] 添加新主播: 抖音号: {}， 昵称:{}, tradeId: {}", anchorDto.getUniqueId(), anchorDto.getAnchorName(), anchorDto.getTradeId());
        boolean exists = ChainWrappers.lambdaQueryChain(similarAnchorDao)
            .eq(SimilarAnchorEntity::getUniqueId, anchorDto.getUniqueId())
            .eq(SimilarAnchorEntity::getIsDeleted, false)
            .exists();
        if (exists) {
            log.info("[行业热榜] 添加新主播: 抖音号: {}， 昵称:{} 已存在", anchorDto.getUniqueId(), anchorDto.getAnchorName());
            return R.error(StatusCode.BASE_VALID_PARAM.getCode(), "主播已存在");
        }
        long userId = userFeign.getLocalUser().getData().getId();
        // 通过抖音号查询主播
        Optional<AnchorUrlEntity> anchorOptional = anchorUrlService.findByAnchorNumber(anchorDto.getUniqueId(), AnchorPlatformEnum.DOU_YIN.getCode());
        if (anchorOptional.isPresent()) {
            // 防御性检测
            boolean rankExists = ChainWrappers.lambdaQueryChain(hotSearchTradeRankingListMapper)
                .eq(TbHotSearchTradeRankingList::getSystemAnchorId, anchorOptional.get().getId())
                .eq(TbHotSearchTradeRankingList::getIsDeleted, false)
                .exists();
            if (rankExists) {
                log.info("[行业热榜] 添加新主播: 抖音号: {}， 昵称:{} 已在行业热榜中", anchorDto.getUniqueId(), anchorOptional.get().getAnchorName());
                return R.error(StatusCode.BASE_VALID_PARAM.getCode(), "此抖音号的系统主播已存在热榜中, 系统主播名称：" + anchorOptional.get().getAnchorName());
            }
        }
        SimilarAnchorEntity similarAnchor = new SimilarAnchorEntity();
        similarAnchor.setId(SnowflakeManager.nextValue());
        similarAnchor.setAnchorName(anchorDto.getAnchorName());
        similarAnchor.setAnchorAvatar(anchorDto.getAnchorAvatar());
        similarAnchor.setUniqueId(anchorDto.getUniqueId());
        similarAnchor.setFollowerCount(anchorDto.getFollowerCount());
        similarAnchor.setLiveAverageUser(anchorDto.getLiveAverageUser());
        similarAnchor.setLiveAverageAmount(anchorDto.getLiveAverageAmount());
        similarAnchor.setTotalAmount(anchorDto.getTotalAmount());
        similarAnchor.setLiveCount(NumberUtil.isInteger(anchorDto.getLiveCount()) ? Integer.parseInt(anchorDto.getLiveCount()) : 0);
        similarAnchor.calcTotalAmountToValue();
        similarAnchor.calcAccountHeat();
        similarAnchor.setAuthorId("");
        similarAnchor.setCreateDate(new Date());
        similarAnchor.setUpdateDate(similarAnchor.getCreateDate());
        similarAnchor.setIsDeleted(0);
        similarAnchor.setLiveAverageOnline("");
        similarAnchor.setLiveAverageUv("");
        similarAnchor.setLiveTotalAmountCmmInd("");
        similarAnchor.setSecUid("");
        anchorOptional.ifPresent(anchor -> {
            similarAnchor.setSecUid(anchor.getSecUid());
            similarAnchor.setSimilarScore("1.0");
        });
        similarAnchorDao.insert(similarAnchor);
        log.info("[行业热榜] 添加新主播: 抖音号: {}， 昵称:{}, tradeId: {}, 主播ID: {}", anchorDto.getUniqueId(), anchorDto.getAnchorName(), anchorDto.getTradeId(), similarAnchor.getId());
        TbHotSearchTradeRankingList rankingList = new TbHotSearchTradeRankingList();
        rankingList.setCreatedBy(userId);
        rankingList.setUpdatedBy(userId);
        rankingList.setSimilarAnchorId(similarAnchor.getId());
        rankingList.setSystemAnchorId(0L);
        rankingList.setTradeId(anchorDto.getTradeId());
        rankingList.setSystemTradeId(0L);
        rankingList.setLastUpdateSystemTradeTime(null);
        rankingList.setSourceType(2);
        rankingList.setCollectStatus(2);
        rankingList.setUpRanking(true);
        rankingList.setCreateDate(LocalDateTime.now());
        rankingList.setUpdateDate(rankingList.getCreateDate());
        rankingList.setIsDeleted(false);
        rankingList.setWeightScore(0L);
        rankingList.calcWeightScore(similarAnchor);
        anchorOptional.ifPresent(anchor -> {
            rankingList.setSystemAnchorId(anchor.getId());
            rankingList.setSystemTradeId(anchor.getSystemTradeId());
            if (anchor.getSystemTradeId() != null && anchor.getSystemTradeId() > 0) {
                rankingList.setTradeId(anchor.getSystemTradeId());
            }
            log.info("[行业热榜] 系统主播关联新主播: 抖音号: {}， 昵称:{}, tradeId: {}, 系统主播ID: {}, 系统行业ID: {}", anchorDto.getUniqueId(), anchorDto.getAnchorName(), anchorDto.getTradeId(), anchor.getId(), anchor.getSystemTradeId());
        });
        hotSearchTradeRankingListMapper.insert(rankingList);
        this.clearCache();
        return R.ok();
    }


    /**
     * 修改主播信息
     *
     * @param anchorDto 主播信息
     *
     * @return 操作结果
     */
    @Transactional(rollbackFor = Exception.class)
    public R<Void> updateAnchor(UpdateSimilarAnchorDto anchorDto) {
        if (anchorDto.getId() == null) {
            return R.error(StatusCode.BASE_VALID_PARAM.getCode(), "ID不能为空");
        }
        SimilarAnchorEntity similarAnchor = similarAnchorDao.selectById(anchorDto.getId());
        if (similarAnchor == null) {
            return R.error(StatusCode.BASE_VALID_PARAM.getCode(), "主播不存在");
        }
        if (!Objects.equals(similarAnchor.getIsDeleted(), 0)) {
            return R.error(StatusCode.BASE_VALID_PARAM.getCode(), "主播已删除");
        }
        if (!SimilarAnchorEntity.validateValueFormat(anchorDto.getTotalAmount(), anchorDto.getAnchorName(), "直播总销售额", true)) {
            return R.error(StatusCode.BASE_VALID_PARAM.getCode(), "直播总销售额格式错误");
        }
        if (!SimilarAnchorEntity.validateValueFormat(anchorDto.getFollowerCount(), anchorDto.getAnchorName(), "粉丝量", true)) {
            return R.error(StatusCode.BASE_VALID_PARAM.getCode(), "粉丝量格式错误");
        }
        if (!SimilarAnchorEntity.validateValueFormat(anchorDto.getLiveAverageUser(), anchorDto.getAnchorName(), "场均观", true)) {
            return R.error(StatusCode.BASE_VALID_PARAM.getCode(), "场均观格式错误");
        }
        log.info("[行业热榜] 修改主播: 抖音号: {}， 昵称:{},  主播ID: {}", similarAnchor.getUniqueId(), anchorDto.getAnchorName(), similarAnchor.getId());
        similarAnchor.setAnchorName(anchorDto.getAnchorName());
        similarAnchor.setAnchorAvatar(anchorDto.getAnchorAvatar());
        similarAnchor.setFollowerCount(anchorDto.getFollowerCount());
        similarAnchor.setLiveAverageUser(anchorDto.getLiveAverageUser());
        similarAnchor.setLiveAverageAmount(anchorDto.getLiveAverageAmount());
        similarAnchor.setLiveAverageOnline(anchorDto.getLiveAverageOnline());
        similarAnchor.setLiveAverageUv(anchorDto.getLiveAverageUv());
        similarAnchor.setLiveTotalAmountCmmInd(anchorDto.getLiveTotalAmountCmmInd());
        similarAnchor.setTotalAmount(anchorDto.getTotalAmount());
        similarAnchor.calcTotalAmountToValue();
        similarAnchor.calcAccountHeat();
        similarAnchor.setUpdateDate(new Date());
        similarAnchorDao.updateById(similarAnchor);
        TbHotSearchTradeRankingList rankingList = this.getSimilarAnchorRankingListMap(List.of(similarAnchor.getId())).get(similarAnchor.getId());
        if (rankingList != null) {
            Long userId = userFeign.getLocalUser().getData().getId();
            if (rankingList.getSourceType() == 1) {
                rankingList.setSourceType(2);
            }
            rankingList.calcWeightScore(similarAnchor);
            rankingList.setUpdatedBy(userId);
            rankingList.setUpdateDate(LocalDateTime.now());
            hotSearchTradeRankingListMapper.updateById(rankingList);
        }
        return R.ok();
    }

    /**
     * 获取树型第三方榜单
     *
     * @param parentId 父级ID
     * @param keyword  关键词
     * @param limit    限制数量
     *
     * @return 树型第三方榜单
     */
    public List<TreeTradeThirdRank> getTreeThirdRank(Long parentId, String keyword, Integer limit) {
        int maxSize = 1000;
        if (limit != null && limit < maxSize) {
            maxSize = limit;
        }
        // 没有关键词 获取第一层数据
        if (EmptyUtil.isEmpty(keyword)) {
            BatchQuery<Long, TradeThirdRanking> batchQuery = new BatchQuery<>((queryLimit, idx) -> {
                return ChainWrappers.lambdaQueryChain(tradeThirdRankingMapper)
                    .gt(idx != null, TradeThirdRanking::getId, idx)
                    .eq(TradeThirdRanking::getParentId, parentId == null ? 0 : parentId)
                    .last("limit " + queryLimit)
                    .list();
            }, TradeThirdRanking::getId);
            return batchQuery.get().stream().map(TradeThirdRanking::toTree).toList();
        }

        // 搜索
        List<TradeThirdRanking> searchResult = ChainWrappers.lambdaQueryChain(tradeThirdRankingMapper)
            .eq(parentId != null, TradeThirdRanking::getParentId, parentId)
            .like(TradeThirdRanking::getCategoryName, keyword)
            .last("limit " + maxSize)
            .list();
        if (EmptyUtil.isEmpty(searchResult)) {
            return List.of();
        }
        List<List<TradeThirdRanking>> all = new ArrayList<>(3000);
        all.add(searchResult);


        // 获取父级
        List<Long> queryFatherIds = searchResult.stream().map(TradeThirdRanking::getParentPath).filter(EmptyUtil::isNotEmpty)
            .flatMap(ids -> {
                return Arrays.stream(ids.split("-")).map(Long::parseLong);
            }).filter(id -> id > 0).distinct().toList();
        if (EmptyUtil.isNotEmpty(queryFatherIds)) {
            List<TradeThirdRanking> list = ChainWrappers.lambdaQueryChain(tradeThirdRankingMapper)
                .in(TradeThirdRanking::getId, queryFatherIds)
                .notIn(TradeThirdRanking::getId, searchResult.stream().map(TradeThirdRanking::getId).distinct().toList())
                .list();
            if (EmptyUtil.isNotEmpty(list)) {
                all.add(list);
            }
        }
        return CollUtil.tree(all.stream().flatMap(Collection::stream).distinct().map(TradeThirdRanking::toTree).toList(), 0L);
    }

    /**
     * 切换行业第三方采集
     *
     * @param tradeId 行业id
     * @param collect 是否采集
     *
     * @return 响应
     */
    public R<Void> switchThirdCollect(long tradeId, boolean collect) {
        tradeService.lambdaUpdate()
            .eq(TradeEntity::getId, tradeId)
            .set(TradeEntity::getThirdCollect, collect)
            .set(TradeEntity::getUpdateDate, LocalDateTime.now())
            .update();
        log.info("[行业热榜] 切换行业第三方采集: 行业ID: {}, 是否采集: {}", tradeId, collect);
        return R.ok();
    }

    /**
     * 行业信息
     *
     * @param tradeId 行业id
     *
     * @return 行业信息
     */
    public TradeRankInfo getInfo(long tradeId) {
        TradeEntity trade = tradeService.getById(tradeId);
        if (trade == null) {
            return null;
        }
        TradeRankInfo tradeRankInfo = new TradeRankInfo();
        tradeRankInfo.setId(trade.getId());
        tradeRankInfo.setName(trade.getName());
        tradeRankInfo.setRankEnabled(trade.getRankEnabled());
        tradeRankInfo.setThirdCollect(trade.getThirdCollect());
        Long count = ChainWrappers.lambdaQueryChain(thirdRankingRelationMapper)
            .eq(TradeThirdRankingRelation::getTradeId, tradeId)
            .count();
        tradeRankInfo.setThirdRelationSize(count.intValue());
        return tradeRankInfo;
    }


    /**
     * 从相似达人待采集状态中取出前30条数据
     */
    public Map<Long, SimilarAnchorVo> pollTop30() {
        List<TbHotSearchTradeRankingList> rankingLists = ChainWrappers.lambdaQueryChain(hotSearchTradeRankingListMapper)
            .select(TbHotSearchTradeRankingList::getId, TbHotSearchTradeRankingList::getSimilarAnchorId)
            .eq(TbHotSearchTradeRankingList::getCollectStatus, 0)
            .eq(TbHotSearchTradeRankingList::getSourceType, 1)
            .last("limit 30")
            .list();
        if (EmptyUtil.isEmpty(rankingLists)) {
            return Map.of();
        }
        Map<Long, Long> map = rankingLists.stream().collect(Collectors.toMap(TbHotSearchTradeRankingList::getId, TbHotSearchTradeRankingList::getSimilarAnchorId));
        List<SimilarAnchorEntity> entities = ChainWrappers.lambdaQueryChain(similarAnchorDao)
            .in(SimilarAnchorEntity::getId, map.values())
            .list();
        if (EmptyUtil.isEmpty(entities)) {
            return Map.of();
        }
        Map<Long, SimilarAnchorVo> anchorMap = entities.stream().collect(Collectors.toMap(SimilarAnchorEntity::getId, item -> {
            SimilarAnchorVo vo = new SimilarAnchorVo();
            BeanUtils.copyProperties(item, vo);
            return vo;
        }));
        return map.entrySet().stream().filter(item -> anchorMap.containsKey(item.getValue()))
            .collect(Collectors.toMap(Map.Entry::getKey, item -> anchorMap.get(item.getValue())));
    }

    /**
     * 发送第三方采集成功
     *
     * @param rankIds 成功的id
     */
    public void sendThirdPartySuccess(List<Long> rankIds) {
        if (EmptyUtil.isEmpty(rankIds)) {
            return;
        }
        ChainWrappers.lambdaUpdateChain(hotSearchTradeRankingListMapper)
            .in(TbHotSearchTradeRankingList::getId, rankIds)
            .set(TbHotSearchTradeRankingList::getCollectStatus, 1)
            .update();
    }

    /**
     * 获取周几的第三方榜单采集行业
     *
     * @param dayOfWeek 周几
     *
     * @return 行业id
     */
    public List<TradeThirdRankingRelation> getDayOfWeekThirdTrades(int dayOfWeek) {
        return thirdRankingRelationMapper.getDayOfWeekThirdTrades(dayOfWeek);
    }

    /**
     * 处理第三方榜单数据
     *
     * @param callbackTradeId 回调行业id
     * @param thirdRankingId  第三方榜单id
     * @param salesRankingVos 第三方榜单数据
     *
     * @return {@link R }<{@link Void }>
     */
    @Transactional(rollbackFor = Exception.class)
    public R<Void> processThirdRankAnchor(Long callbackTradeId, String thirdRankingId, List<ThirdAnchorBo> salesRankingVos) {
        log.info("========== [回调接口] 收到第三方榜单回调数据 ==========");
        if (EmptyUtil.isEmpty(thirdRankingId)) {
            log.error("[第三方榜单] 回调数据 第三方榜单id不能为空");
            return R.error("第三方榜单id不能为空");
        }
        if (EmptyUtil.isEmpty(salesRankingVos)) {
            log.error("[第三方榜单] 回调数据 第三方榜单数据不能为空");
            return R.error("第三方榜单数据不能为空");
        }
        Long tradeId = callbackTradeId;
        if (tradeId == null) {
            Optional<TradeThirdRankingRelation> thirdRankingRelation = ChainWrappers.lambdaQueryChain(thirdRankingRelationMapper)
                .eq(TradeThirdRankingRelation::getThirdRankingId, thirdRankingId)
                .select(TradeThirdRankingRelation::getTradeId)
                .last("limit 1")
                .oneOpt();
            if (thirdRankingRelation.isPresent()) {
                TradeThirdRankingRelation rankingRelation = thirdRankingRelation.get();
                ChainWrappers.lambdaUpdateChain(thirdRankingRelationMapper)
                    .eq(TradeThirdRankingRelation::getId, rankingRelation.getId())
                    .set(TradeThirdRankingRelation::getLastCollectTime, LocalDateTime.now())
                    .update();
                tradeId = rankingRelation.getTradeId();
            } else {
                log.error("[第三方榜单] 回调数据 行业不存在");
                return R.error("行业不存在");
            }
        } else {
            ChainWrappers.lambdaUpdateChain(thirdRankingRelationMapper)
                .eq(TradeThirdRankingRelation::getTradeId, tradeId)
                .eq(TradeThirdRankingRelation::getThirdRankingId, thirdRankingId)
                .set(TradeThirdRankingRelation::getLastCollectTime, LocalDateTime.now())
                .update();
        }

        log.info("[第三方榜单] 回调数据 系统行业ID: {}, 第三方榜单ID: {}, 主播数据:{}条", tradeId, thirdRankingId, salesRankingVos.size());

        // 根据第三方ID分组，重复的丢弃
        Map<String, ThirdAnchorBo> authorMap = salesRankingVos.stream().filter(item -> EmptyUtil.isNotEmpty(item.getAuthorId()) && EmptyUtil.isNotEmpty(item.getUniqueId())).collect(Collectors.toMap(ThirdAnchorBo::getAuthorId, Function.identity(), FunctionUtil::mergeFirst));
        if (EmptyUtil.isEmpty(authorMap)) {
            log.error("[第三方榜单] 回调数据 第三方榜单主播ID和抖音号不为空的数据为0");
            return R.error("主播数据字段缺少, 缺少主播ID或抖音号");
        }
        log.info("[第三方榜单] 回调数据 原始数据{}条, 过滤主播数据{}条", salesRankingVos.size(), authorMap.size());
        // 批量查询已存在的相似主播 key 婵妈妈账户ID value 相似主播
        Map<String, SimilarAnchorEntity> existingAnchorMap = similarAnchorService.lambdaQuery()
            .in(SimilarAnchorEntity::getAuthorId, authorMap.keySet())
            .list().stream().collect(Collectors.toMap(SimilarAnchorEntity::getAuthorId, Function.identity(), FunctionUtil::mergeFirst));
        // 批量查询已存在的相似主播 key 抖音号 value 相似主播
        List<String> anchorNumberList = authorMap.values().stream().map(ThirdAnchorBo::getUniqueId).distinct().toList();
        Map<String, SimilarAnchorEntity> existingAnchorNumberMap = similarAnchorService.lambdaQuery()
            .in(SimilarAnchorEntity::getUniqueId, anchorNumberList)
            .eq(SimilarAnchorEntity::getIsDeleted, 0)
            .orderByDesc(SimilarAnchorEntity::getId)
            .list().stream().collect(Collectors.toMap(SimilarAnchorEntity::getUniqueId, Function.identity(), FunctionUtil::mergeFirst));
        log.info("[第三方榜单] ✓ 批量查询已存在的相似主播，查询数量: {}, 已存在数量: {}", authorMap.size(), existingAnchorMap.size());

        Map<String, AnchorUrlEntity> anchorMap = anchorUrlService.getAnchorMapByAnchorNumber(anchorNumberList, AnchorPlatformEnum.DOU_YIN.getCode());

        Map<Long, TbHotSearchTradeRankingList> similarAnchorRankingMap = this.getSimilarAnchorRankingListMap(Stream.concat(existingAnchorNumberMap.values().stream().map(SimilarAnchorEntity::getId), existingAnchorMap.values().stream().map(SimilarAnchorEntity::getId)).distinct().toList());
        List<ThirdAnchorBo> thirdAnchorBos = new ArrayList<>(authorMap.values());
        authorMap = null;
        // 分开处理新增和更新（避免saveOrUpdateBatch的N+1问题）
        List<SimilarAnchorEntity> newAnchorEntities = new ArrayList<>();
        List<SimilarAnchorEntity> updateAnchorEntities = new ArrayList<>();
        List<TbHotSearchTradeRankingList> newSimilarAnchorRankingList = new ArrayList<>();
        List<TbHotSearchTradeRankingList> updateSimilarAnchorRankingList = new ArrayList<>();
        int newAnchorCount = 0;
        int existingAnchorCount = 0;
        int newRelationCount = 0;
        int existingRelationCount = 0;
        // 找到跟系统主播完全匹配到的相似主播
        for (int i = 0; i < thirdAnchorBos.size(); i++) {
            ThirdAnchorBo vo = thirdAnchorBos.get(i);
            log.debug("[第三方榜单] 处理第{}/{}个相似主播，authorId: {}, 昵称: {}",
                i + 1, thirdAnchorBos.size(), vo.getAuthorId(), vo.getNickname());
            // 从批量查询结果中获取已存在的主播（使用authorId作为唯一标识）
            SimilarAnchorEntity existingAnchor = existingAnchorMap.get(vo.getAuthorId());
            if (existingAnchor == null) {
                existingAnchor = existingAnchorNumberMap.get(vo.getUniqueId());
            }
            Consumer<SimilarAnchorEntity> setValue = anchorEntity -> {
                if (EmptyUtil.isEmpty(anchorEntity.getSimilarScore())) {
                    anchorEntity.setSimilarScore("0");
                }
                if (EmptyUtil.isEmpty(anchorEntity.getLiveAverageUv())) {
                    anchorEntity.setLiveAverageUv("");
                }
                if (EmptyUtil.isEmpty(anchorEntity.getLiveTotalAmountCmmInd())) {
                    anchorEntity.setLiveTotalAmountCmmInd("");
                }
                anchorEntity.setFollowerCount(vo.getFollowerCount() == null ? "0" : vo.getFollowerCount().toString());
                anchorEntity.setLiveAverageUser(vo.getAvgDailyUserCount());
                anchorEntity.setLiveAverageAmount("");
                anchorEntity.setTotalAmount(vo.getSalesText());

                anchorEntity.setLiveCount(vo.getLiveShowCount() == null ? 0 : vo.getLiveShowCount());
                anchorEntity.setLiveAverageOnline("");
                anchorEntity.setAuthorId(vo.getAuthorId());
                anchorEntity.setUniqueId(vo.getUniqueId());
                anchorEntity.setSecUid(anchorMap.get(vo.getUniqueId()) == null ? "" : anchorMap.get(vo.getUniqueId()).getSecUid());
                anchorEntity.setCollectDate(EmptyUtil.isEmpty(vo.getCollectionTime()) ? null : DateUtil.parse(vo.getCollectionTime()));
                anchorEntity.setAnchorName(vo.getNickname());
                anchorEntity.setAnchorAvatar(vo.getAvatar());
                // 解析并设置总销售额数值（用于查询过滤和排序）
                anchorEntity.calcTotalAmountToValue();
                // 计算并设置账号热度
                anchorEntity.calcAccountHeat();
                if (anchorEntity.getTotalAmountNumeric() != null && anchorEntity.getTotalAmountNumeric() > 0 && anchorEntity.getLiveCount() != null && anchorEntity.getLiveCount() > 0) {
                    anchorEntity.setLiveAverageAmount(BigDecimal.valueOf(anchorEntity.getTotalAmountNumeric()).divide(BigDecimal.valueOf(anchorEntity.getLiveCount()), 2, RoundingMode.HALF_UP).toString());
                }
            };
            Long similarAnchorId;
            if (ObjectUtil.isNull(existingAnchor)) {
                // 新增相似主播
                SimilarAnchorEntity anchorEntity = new SimilarAnchorEntity();
                setValue.accept(anchorEntity);
                anchorEntity.setCreateDate(new Date());
                anchorEntity.setUpdateDate(new Date());
                anchorEntity.setIsDeleted(0);
                similarAnchorId = SnowflakeManager.nextValue();
                anchorEntity.setId(similarAnchorId);
                Integer accountHeat = anchorEntity.getAccountHeat();
                newAnchorEntities.add(anchorEntity);
                newAnchorCount++;
                existingAnchor = anchorEntity;
                log.debug("[第三方榜单] 相似主播不存在，准备新增，authorId: {}, 主播名称: {}, 热度: {}",
                    vo.getAuthorId(), vo.getNickname(), accountHeat);
            } else {
                // 更新已存在的相似主播数据
                setValue.accept(existingAnchor);
                existingAnchor.setUpdateDate(new Date());
                Integer accountHeat = existingAnchor.getAccountHeat();
                // 添加到更新列表
                updateAnchorEntities.add(existingAnchor);

                similarAnchorId = existingAnchor.getId();
                existingAnchorCount++;
                log.debug("[第三方榜单] 相似主播已存在，准备更新，authorId: {}, 主播名称: {}, 热度: {}",
                    vo.getAuthorId(), vo.getNickname(), accountHeat);
            }

            // 4. 创建关联关系
            TbHotSearchTradeRankingList rankingList = similarAnchorRankingMap.get(similarAnchorId);
            if (EmptyUtil.isEmpty(rankingList)) {
                TbHotSearchTradeRankingList tradeRankingList = createNewRankingList(similarAnchorId, anchorMap.get(vo.getUniqueId()), tradeId);
                tradeRankingList.calcWeightScore(existingAnchor);
                if (tradeRankingList.getWeightScore() > 0) {
                    // 上榜
                    tradeRankingList.setUpRanking(true);
                }
                tradeRankingList.setCollectStatus(2);
                tradeRankingList.setSourceType(3);
                newSimilarAnchorRankingList.add(tradeRankingList);
                newRelationCount++;
                log.debug("[第三方榜单] 准备新增关联关系，行业ID: {}, 相似主播ID: {}, 得分: {}", tradeRankingList.getTradeId(), similarAnchorId, tradeRankingList.getWeightScore());
            } else {
                rankingList.calcWeightScore(existingAnchor);
                if (rankingList.getWeightScore() > 0) {
                    // 上榜
                    rankingList.setUpRanking(true);
                }
                rankingList.setCollectStatus(2);
                rankingList.setSourceType(3);
                rankingList.setUpdateDate(LocalDateTime.now());
                updateSimilarAnchorRankingList.add(rankingList);
                existingRelationCount++;
                log.debug("[第三方榜单] 找到已存在的关联关系，行业ID: {}, 相似主播ID: {}, 得分: {}", rankingList.getTradeId(), similarAnchorId, rankingList.getWeightScore());
            }
        }

        // 批量保存新增的主播（真正的批量INSERT，避免N+1）
        if (CollectionUtil.isNotEmpty(newAnchorEntities)) {
            similarAnchorService.saveBatch(newAnchorEntities);
            log.info("[第三方榜单] ✓ 批量新增相似主播数据成功，数量: {}", newAnchorEntities.size());
        } else {
            log.info("[第三方榜单] 无新增相似主播数据需要保存");
        }

        // 批量更新已存在的主播（真正的批量UPDATE，避免N+1）
        if (CollectionUtil.isNotEmpty(updateAnchorEntities)) {
            similarAnchorDao.batchUpdateSelective(updateAnchorEntities);
            log.info("[第三方榜单] ✓ 批量更新相似主播数据成功，数量: {}", updateAnchorEntities.size());
        } else {
            log.info("[第三方榜单] 无相似主播数据需要更新");
        }

        // 批量保存关联关系
        if (CollectionUtil.isNotEmpty(newSimilarAnchorRankingList)) {
            hotSearchTradeRankingListMapper.batchInsert(newSimilarAnchorRankingList);
            log.info("[第三方榜单] ✓ 批量保存相似主播关联关系成功，数量: {}", newSimilarAnchorRankingList.size());
        } else {
            log.info("[第三方榜单] 无新增关联关系需要保存");
        }
        if (CollectionUtil.isNotEmpty(updateSimilarAnchorRankingList)) {
            hotSearchTradeRankingListMapper.updateBatchSelective(updateSimilarAnchorRankingList);
            log.info("[第三方榜单] ✓ 批量更新相似主播关联关系成功，数量: {}", updateSimilarAnchorRankingList.size());
        } else {
            log.info("[第三方榜单] 无相似主播关联关系需要更新");
        }
        log.info("[第三方榜单] 回调处理 成功处理{}个相似主播，新增{}个，更新{}个，跳过{}个(authorId为空或重复)，新增关联关系{}个", thirdAnchorBos.size(), newAnchorCount, existingAnchorCount, salesRankingVos.size() - thirdAnchorBos.size(), newRelationCount);
        log.info("========== [回调接口] 批量处理完成 ==========");
        return R.ok();

    }

    /**
     * 同步主播行业
     *
     * @param anchorSystemTradeMap 主播系统行业映射
     */
    @Transactional(rollbackFor = Exception.class)
    public void syncAnchorTrade(Map<Long, Long> anchorSystemTradeMap) {
        if (EmptyUtil.isEmpty(anchorSystemTradeMap)) {
            return;
        }
        List<TbHotSearchTradeRankingList> rankingLists = ChainWrappers.lambdaQueryChain(hotSearchTradeRankingListMapper)
            .in(TbHotSearchTradeRankingList::getSystemAnchorId, anchorSystemTradeMap.keySet())
            .list();
        if (EmptyUtil.isEmpty(rankingLists)) {
            log.info("[第三方榜单] 相似达人 无关联热榜 无需同步主播行业");
            return;
        }
        List<TbHotSearchTradeRankingList> updateList = rankingLists.stream().filter(rank -> {
            Long tradeId = anchorSystemTradeMap.get(rank.getSystemAnchorId());
            if (EmptyUtil.isEmpty(tradeId)) {
                return false;
            }
            if (Objects.equals(tradeId, rank.getSystemTradeId())) {
                return false;
            }
            rank.setSystemTradeId(tradeId);
            log.info("[第三方榜单] 相似达人 榜单同步系统主播行业,  榜单ID: {},  systemTradeId: {}", rank.getId(), rank.getSystemTradeId());
            return true;
        }).toList();
        if (EmptyUtil.isEmpty(updateList)) {
            log.info("[第三方榜单] 相似达人 行业一致 无需同步主播行业");
            return;
        }
        hotSearchTradeRankingListMapper.updateBatchSelective(updateList);
        log.info("[第三方榜单]  similarityAnchorTrade 批量更新成功，数量: {}", updateList.size());
    }

    /**
     * 批量保存系统主播关联
     *
     * @param anchorList 主播列表
     */
    @Transactional(rollbackFor = Exception.class)
    public void batchCorrelationSystemAnchor(Collection<AnchorUrlEntity> anchorList) {
        if (EmptyUtil.isEmpty(anchorList)) {
            return;
        }
        Map<String, AnchorUrlEntity> anchorMap = anchorList.stream().collect(Collectors.toMap(AnchorUrlEntity::getAnchorNumber, Function.identity(), (v1, v2) -> v2));
        List<TbHotSearchTradeRankingList> tradeRankingList = hotSearchTradeRankingListMapper.queryAnchorNumberNotCorrelation(anchorMap.keySet());
        if (EmptyUtil.isEmpty(tradeRankingList)) {
            return;
        }
        List<Long> similarAnchorIds = tradeRankingList.stream().map(TbHotSearchTradeRankingList::getSimilarAnchorId).toList();
        Map<Long, SimilarAnchorEntity> similarAnchorMap = ChainWrappers.lambdaQueryChain(similarAnchorDao)
            .in(SimilarAnchorEntity::getId, similarAnchorIds)
            .eq(SimilarAnchorEntity::getIsDeleted, 0)
            .select(SimilarAnchorEntity::getId, SimilarAnchorEntity::getUniqueId, SimilarAnchorEntity::getSecUid)
            .list().stream().collect(Collectors.toMap(SimilarAnchorEntity::getId, Function.identity()));

        List<TbHotSearchTradeRankingList> updateList = new ArrayList<>();
        List<SimilarAnchorEntity> updateSimilarAnchorList = new ArrayList<>();
        tradeRankingList.forEach(rank -> {
            SimilarAnchorEntity similarAnchor = similarAnchorMap.get(rank.getSimilarAnchorId());
            if (similarAnchor == null) {
                return;
            }
            AnchorUrlEntity anchorUrl = anchorMap.get(similarAnchor.getUniqueId());
            if (anchorUrl == null) {
                return;
            }
            similarAnchor.setSecUid(anchorUrl.getSecUid());
            rank.setSystemAnchorId(anchorUrl.getId());
            if (anchorUrl.getSystemTradeId() != null && rank.getSystemTradeId() > 0 && rank.getSystemTradeId() != anchorUrl.getSystemTradeId()) {
                rank.setSystemTradeId(anchorUrl.getSystemTradeId());
                rank.setTradeId(anchorUrl.getSystemTradeId());
            }
            updateList.add(rank);
            updateSimilarAnchorList.add(similarAnchor);
        });
        if (EmptyUtil.isNotEmpty(updateList)) {
            hotSearchTradeRankingListMapper.updateBatchSelective(updateList);
        }
        if (EmptyUtil.isNotEmpty(updateSimilarAnchorList)) {
            similarAnchorDao.batchUpdateSelective(updateSimilarAnchorList);
        }
    }
}
