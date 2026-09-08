package com.jiuyu.replay.words.task;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.jiuyu.framework.function.BatchQuery;
import com.jiuyu.framework.json.JsonTemplate;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.framework.util.FunctionUtil;
import com.jiuyu.replay.common.constant.CommonProperties;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.dto.third.SendSimilarAnchorDto;
import com.jiuyu.replay.generic.dto.third.ThirdSalesRankingResult;
import com.jiuyu.replay.generic.enums.words.AnchorPlatformEnum;
import com.jiuyu.replay.generic.feign.third.ChanmamaFeign;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.entity.*;
import com.jiuyu.replay.words.producer.impl.TradeRankAnchorProducer;
import com.jiuyu.replay.words.repository.service.AnchorUrlService;
import com.jiuyu.replay.words.repository.service.AnchorUrlUserService;
import com.jiuyu.replay.words.repository.service.SimilarSendRecordService;
import com.jiuyu.replay.words.repository.service.VideoDataViewingConfuseService;
import com.jiuyu.replay.words.vo.SimilarAnchorVo;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 相似达人查询定时任务
 *
 * @author RayChou
 * @date 2025-10-27
 * @description 定时查询行业主播的相似达人数据
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SimilarAnchorQueryTask {

    private final TradeRankAnchorProducer tradeRankAnchorProducer;

    private final AnchorUrlUserService anchorUrlUserService;

    private final AnchorUrlService anchorUrlService;

    private final VideoDataViewingConfuseService videoDataViewingConfuseService;

    private final ChanmamaFeign chanmamaFeign;

    private final CommonProperties commonProperties;

    private final SimilarSendRecordService similarSendRecordService;



    /**
     * 系统主播与相似达人的自动关联
     */
    @XxlJob("authCorrelationAnchor")
    public void authCorrelationAnchor() {
        int shardTotal = XxlJobHelper.getShardTotal();
        int shardIndex = XxlJobHelper.getShardIndex();
        Tuple2<LocalDateTime, LocalDateTime> queryDate = extractQueryDate(XxlJobHelper.getJobParam(), shardIndex, shardTotal);
        // 分批查询
        BatchQuery<Long, AnchorUrlUserEntity> batchQuery = new BatchQuery<>((limit, idx) -> {
            log.info("authCorrelationAnchor 分批查询用户主播数据, 批次ID: {}, 批次大小: {}", idx, limit);
            return this.loadUserAnchor(idx, limit, queryDate.getT1(), queryDate.getT2(), null);
        }, AnchorUrlUserEntity::getId);


        batchQuery.consumer(userAnchors -> {
            // 查询主播信息
            Map<AnchorUrlUserEntity, AnchorUrlEntity> anchorInfoMap = this.getAnchorInfoMap(userAnchors);
            if (EmptyUtil.isEmpty(anchorInfoMap)) {
                return;
            }
            tradeRankAnchorProducer.batchCorrelationSystemAnchor(anchorInfoMap.values());
        });
        batchQuery.run(Long.MAX_VALUE);

    }



    /**
     * 自动同步主播系统行业id 每天一次
     */
    @XxlJob("autoSyncAnchorSystemTradeId")
    public void autoSyncAnchorSystemTradeId() {
        LocalDateTime lastUpdateSystemTradeTime = LocalDateTime.now().minusDays(1).minusHours(1);

        // 获取最近1天有更新的系统行业id的主播
        BatchQuery<Long, AnchorUrlEntity> lastUpdateSystemTradeBatchQuery = new BatchQuery<>((limit, idx) -> {
            log.info("[相似达人] query update system trade date time > {} the anchor. batch query idx: {}, limit: {}", lastUpdateSystemTradeTime, idx, limit);
            return anchorUrlService.lambdaQuery()
                .select(AnchorUrlEntity::getId, AnchorUrlEntity::getSystemTradeId)
                .lt(idx != null, AnchorUrlEntity::getId, idx)
                .isNotNull(AnchorUrlEntity::getSystemTradeId)
                .ge(AnchorUrlEntity::getUpdateSystemTradeDate, lastUpdateSystemTradeTime)
                .orderByDesc(AnchorUrlEntity::getId)
                .last("limit " + limit)
                .list();
        }, AnchorUrlEntity::getId);

        LocalDateTime lastUpdateAiTradeTime = LocalDateTime.now().minusDays(7).plusHours(1);
        LocalDateTime lastUpdateAiTradeStartTime = lastUpdateAiTradeTime.minusHours(1);
        // 七天前改过AI纠正行业同时系统行业为空的主播
        BatchQuery<Long, AnchorUrlEntity> aiTradeBatchQuery = new BatchQuery<>((limit, idx) -> {
            log.info("[相似达人] query add ai trade date time < {} the anchor. batch query idx: {}, limit: {}", lastUpdateAiTradeTime, idx, limit);
            return anchorUrlService.lambdaQuery()
                .select(AnchorUrlEntity::getId, AnchorUrlEntity::getAiCorrectTradeId)
                .lt(idx != null, AnchorUrlEntity::getId, idx)
                .isNotNull(AnchorUrlEntity::getAiCorrectTradeId)
                .isNull(AnchorUrlEntity::getSystemTradeId)
                .le(AnchorUrlEntity::getAddAiTradeDate, lastUpdateAiTradeTime)
                .ge(AnchorUrlEntity::getAddAiTradeDate, lastUpdateAiTradeStartTime)
                .orderByDesc(AnchorUrlEntity::getId)
                .last("limit " + limit)
                .list();
        }, AnchorUrlEntity::getId);

        lastUpdateSystemTradeBatchQuery.consumer((batch, rows) -> {
            log.info("[相似达人] update system trade date time > {} the anchor, 查询 batch: {}, result rows {} 条", lastUpdateSystemTradeTime, batch, rows.size());
            rows.forEach(row -> {
                log.info("[相似达人] 系统行业更新, 主播id: {}, 系统行业id: {}", row.getId(), row.getSystemTradeId());
            });
            Map<Long, Long> anchorSystemTradeMap = rows.stream().collect(Collectors.toMap(AnchorUrlEntity::getId, AnchorUrlEntity::getSystemTradeId));
            tradeRankAnchorProducer.syncAnchorTrade(anchorSystemTradeMap);
        });
        aiTradeBatchQuery.consumer((batch, rows) -> {
            log.info("[相似达人] add ai trade date time < {} the anchor, query batch: {}, result rows {} 条", lastUpdateAiTradeTime, batch, rows.size());
            Map<Long, Long> anchorSystemTradeMap = rows.stream().collect(Collectors.toMap(AnchorUrlEntity::getId, AnchorUrlEntity::getAiCorrectTradeId));
            // 同步到行业热榜中
            tradeRankAnchorProducer.syncAnchorTrade(anchorSystemTradeMap);
            rows.forEach(row -> {
                log.info("[相似达人] AI纠正行业更新以达7天， 同步主播行业, 主播id: {}, 行业id: {}", row.getId(), row.getAiCorrectTradeId());
                row.setSystemTradeId(row.getAiCorrectTradeId());
            });
            // 更新主播表里面的系统行业ID
            anchorUrlService.updateBatchById(rows);
        });
        long updateSystemTradeAnchorCount = lastUpdateSystemTradeBatchQuery.run();
        long updateAiTradeAnchorCount = aiTradeBatchQuery.run();
        log.info("[相似达人] 本次任务查询到, 最近一天系统行业更新 {} 条数据, 七天前更新AI纠正行业 {} 条数据", updateSystemTradeAnchorCount, updateAiTradeAnchorCount);
        XxlJobHelper.log("[相似达人] 本次任务查询到, 最近一天系统行业更新 {} 条数据, 七天前更新AI纠正行业 {} 条数据", updateSystemTradeAnchorCount, updateAiTradeAnchorCount);
    }

    /**
     * 向第三方榜单发送采集请求 每天一次
     */
    @XxlJob("thirdPartyRankings")
    public void thirdPartyRankings() {
        int dayOfWeek = LocalDateTime.now().getDayOfWeek().getValue();
        log.info("[第三方榜单] 开始查询行业主播的第三方榜单数据, 当天是星期: {}", dayOfWeek);
        List<TradeThirdRankingRelation> dayOfWeekThirdTradeList = tradeRankAnchorProducer.getDayOfWeekThirdTrades(dayOfWeek);
        if (EmptyUtil.isEmpty(dayOfWeekThirdTradeList)) {
            log.info("[第三方榜单] 没有需要采集的第三方榜单");
            XxlJobHelper.log("星期{}, 没有需要采集的第三方榜单", dayOfWeek);
            return;
        }
        // 构建回调地址
        String callBackUrl = commonProperties.getCurrentSystemAddress() + "replay/words/tradeRank/third-rank-callback";
        dayOfWeekThirdTradeList.forEach(thirdTrade -> {
            XxlJobHelper.log("[第三方榜单] 榜单-{}，开始查询第三方榜单数据, thirdId: {}", thirdTrade.getThirdRankingName(), thirdTrade.getThirdRankingId());
            R<ThirdSalesRankingResult> salesRankingResult = chanmamaFeign.getSalesRanking(thirdTrade.getThirdRankingId(), callBackUrl);
            if (salesRankingResult.fail()) {
                log.error("[第三方榜单] 榜单-{}，get sales ranking error, thirdId: {}, result: {}", thirdTrade.getThirdRankingName(), thirdTrade.getThirdRankingId(), salesRankingResult.getMsg());
                XxlJobHelper.log("[第三方榜单] 榜单-{}，get sales ranking error, thirdId: {}, result: {}", thirdTrade.getThirdRankingName(), thirdTrade.getThirdRankingId(), salesRankingResult.getMsg());
                return;
            }
            if (EmptyUtil.isEmpty(salesRankingResult.getData()) || EmptyUtil.isEmpty(salesRankingResult.getData().getSalesRankingVos())) {
                log.info("[第三方榜单] 榜单-{}，get sales ranking success, thirdId: {} Waiting for third-party processing callback", thirdTrade.getThirdRankingName(), thirdTrade.getThirdRankingId());
                XxlJobHelper.log("[第三方榜单] 榜单-{}，get sales ranking success, thirdId: {} Waiting for third-party processing callback", thirdTrade.getThirdRankingName(), thirdTrade.getThirdRankingId());
                return;
            }
            log.info("[第三方榜单] 榜单-{}，get sales ranking success, thirdId: {}, resultSize: {}", thirdTrade.getThirdRankingName(), thirdTrade.getThirdRankingId(), salesRankingResult.getData().getSalesRankingVos().size());
            XxlJobHelper.log("[第三方榜单] 榜单-{}，get sales ranking success, thirdId: {}, resultSize: {}", thirdTrade.getThirdRankingName(), thirdTrade.getThirdRankingId(), salesRankingResult.getData().getSalesRankingVos().size());
            tradeRankAnchorProducer.processThirdRankAnchor(thirdTrade.getTradeId(), thirdTrade.getThirdRankingId(), salesRankingResult.getData().getSalesRankingVos());
            XxlJobHelper.log("[第三方榜单] 榜单-{}，get sales ranking success, thirdId: {}, resultSize: {} savedSuccessfully", thirdTrade.getThirdRankingName(), thirdTrade.getThirdRankingId(), salesRankingResult.getData().getSalesRankingVos().size());
        });
        log.info("[第三方榜单] 榜单查询完成");
    }


    /**
     * 向第三方发送采集请求 相似达人 每10分钟一次
     */
    @XxlJob("probeSimilarAnchors")
    public void probeSimilarAnchors() {
        Map<Long, SimilarAnchorVo> anchorMap = tradeRankAnchorProducer.pollTop30();
        if (EmptyUtil.isEmpty(anchorMap)) {
            return;
        }
        log.info("[相似达人] 开始向三方发送采集任务, 达人数量: {}", anchorMap.size());
        // 构建回调地址
        String callBackUrl = commonProperties.getCurrentSystemAddress() + "replay/words/tradeRank/similarAnchorCallback";
        List<SimilarSendRecordEntity> records = new ArrayList<>();
        List<Long> successRankIds = new ArrayList<>(anchorMap.size());
        anchorMap.forEach((rankId, anchor) -> {
            log.info("[相似达人] 开始向三方发送采集任务, 达人抖音号: {}, 达人昵称: {}", anchor.getUniqueId(), anchor.getNickName());
            SendSimilarAnchorDto sendSimilarAnchorDto = new SendSimilarAnchorDto();
            sendSimilarAnchorDto.setAnchorId(anchor.getUniqueId());
            sendSimilarAnchorDto.setRequestId(IdUtil.fastSimpleUUID());
            sendSimilarAnchorDto.setSecUid(anchor.getSecUid());
            sendSimilarAnchorDto.setCallBackUrl(callBackUrl);
            try {
                R<Boolean> result = chanmamaFeign.sendSimilarAnchorQuery(sendSimilarAnchorDto);
                if (result.fail()) {
                    log.warn("[相似达人] 向三方发送采集任务成功, requestId: {}, 达人抖音号: {}, 达人昵称: {},  {}", sendSimilarAnchorDto.getRequestId(), anchor.getUniqueId(), anchor.getNickName(), result.getMsg());
                }
                SimilarSendRecordEntity sendRecord = new SimilarSendRecordEntity();
                sendRecord.setId(SnowflakeManager.nextValue());
                sendRecord.setUserId(0L);
                sendRecord.setTenantId(0L);
                sendRecord.setAnchorNumber(anchor.getUniqueId());
                sendRecord.setSecUid(anchor.getSecUid());
                sendRecord.setRequestId(sendSimilarAnchorDto.getRequestId());
                sendRecord.setRequestBody(JsonTemplate.toJson(sendSimilarAnchorDto));
                sendRecord.setResponseBody(JsonTemplate.toJson(result));
                sendRecord.setCreateDate(new Date());
                sendRecord.setUpdateDate(new Date());
                sendRecord.setIsDeleted(0);
                records.add(sendRecord);
                successRankIds.add(rankId);
                log.info("[相似达人] 向三方发送采集任务成功, requestId: {}, 达人抖音号: {}, 达人昵称: {}", sendSimilarAnchorDto.getRequestId(), anchor.getUniqueId(), anchor.getNickName());
            } catch (Exception e) {
                log.error("[相似达人] 向三方发送采集任务失败, requestId: {}, 达人抖音号: {}, 达人昵称: {}", sendSimilarAnchorDto.getRequestId(), anchor.getUniqueId(), anchor.getNickName(), e);
            }
        });
        if (EmptyUtil.isNotEmpty(records)) {
            similarSendRecordService.saveBatch(records);
        }
        if (EmptyUtil.isNotEmpty(successRankIds)) {
            tradeRankAnchorProducer.sendThirdPartySuccess(successRankIds);
        }
    }


    /**
     * 每天凌晨2点执行相似达人查询任务
     * 查询前一天新增的主播数据，并为不满足条件的行业发送相似达人查询请求
     *
     * @author RayChou
     * @date 2025-10-27
     */
    //@Scheduled(cron = "0 0 2 * * ?")
    @XxlJob("querySimilarAnchors")
    public void querySimilarAnchors() {
        int shardTotal = XxlJobHelper.getShardTotal();
        int shardIndex = XxlJobHelper.getShardIndex();
        log.info("开始执行相似达人查询定时任务, 集群{}/{}", shardIndex, shardTotal);
        try {
            Tuple2<LocalDateTime, LocalDateTime> queryDate = extractQueryDate(XxlJobHelper.getJobParam(), shardIndex, shardTotal);
            List<Long> closeRankTradeIds = tradeRankAnchorProducer.getCloseRankTradeIds();
            // 获取有效的行业
            List<Long> leafTradeIds = tradeRankAnchorProducer.getLeafTradeIds();
            LocalDateTime lastUpdate = LocalDateTime.now().minusDays(30);
            // 统计近30天的行业上榜主播数，排除大于1000的行业
            Map<Long, Long> tradeAnchorCountMap = tradeRankAnchorProducer.countTradeAnchor(true, lastUpdate, null, null, null);
            Stream<Long> excessTrade = tradeAnchorCountMap.entrySet().stream().filter(entry -> entry.getValue() >= 1000).map(Map.Entry::getKey);
            // 合并关闭采集的行业和超额的行业作为排除条件
            List<Long> excludeTradeIds = Stream.concat(closeRankTradeIds.stream(), excessTrade).distinct().toList();

            // 分批查询
            BatchQuery<Long, AnchorUrlUserEntity> batchQuery = new BatchQuery<>((limit, idx) -> {
                log.info("分批查询用户主播数据, 批次ID: {}, 批次大小: {}", idx, limit);
                return this.loadUserAnchor(idx, limit, queryDate.getT1(), queryDate.getT2(), excludeTradeIds);
            }, AnchorUrlUserEntity::getId);

            batchQuery.consumer(userAnchors -> {
                // 过滤有效行业
                List<AnchorUrlUserEntity> leafTradeAnchors = userAnchors.stream().filter(anchor -> leafTradeIds.contains(anchor.getTradeId())).toList();
                // 查询主播信息
                Map<AnchorUrlUserEntity, AnchorUrlEntity> anchorInfoMap = this.getAnchorInfoMap(leafTradeAnchors);
                if (EmptyUtil.isEmpty(anchorInfoMap)) {
                    return;
                }
                List<String> anchorNumbers = anchorInfoMap.values().stream().map(AnchorUrlEntity::getAnchorNumber).toList();
                // 查询主播数据看盘信息
                Set<String> validAnchorNumbers = videoDataViewingConfuseService.lambdaQuery()
                    .select(VideoDataViewingConfuseEntity::getAnchorNumber)
                    .in(VideoDataViewingConfuseEntity::getAnchorNumber, anchorNumbers)
                    .eq(VideoDataViewingConfuseEntity::getDataSourceType, 0)
                    .in(VideoDataViewingConfuseEntity::getDataStatus, 1, 8)
                    .list().stream()
                    .map(VideoDataViewingConfuseEntity::getAnchorNumber)
                    .collect(Collectors.toSet());
                if (EmptyUtil.isEmpty(validAnchorNumbers)) {
                    return;
                }
                Map<AnchorUrlEntity, Long> saveMap = anchorInfoMap.entrySet().stream().filter(entry -> validAnchorNumbers.contains(entry.getValue().getAnchorNumber())).collect(Collectors.toMap(Map.Entry::getValue, e -> e.getKey().getTradeId()));
                tradeRankAnchorProducer.batchSaveSystemTradeAnchor(saveMap);
            });
            batchQuery.run(Long.MAX_VALUE);
            // tradeRankAnchorProducer.querySimilarAnchorsForTrades(XxlJobHelper.getJobParam());
            log.info("相似达人查询定时任务执行完成");
        } catch (Exception e) {
            log.error("相似达人查询定时任务执行失败", e);
        }
    }


    /**
     * 加载指定时间范围内的用户主播数据
     *
     * @param idx             主播ID
     * @param limit           每次加载的行数
     * @param startTime       开始时间
     * @param endTime         结束时间
     * @param excludeTradeIds 排除的行业ID
     *
     * @return 主播数据列表
     */
    private List<AnchorUrlUserEntity> loadUserAnchor(long idx, int limit, LocalDateTime startTime, LocalDateTime endTime, List<Long> excludeTradeIds) {
        return anchorUrlUserService.lambdaQuery()
            .lt(AnchorUrlUserEntity::getId, idx)
            .between(AnchorUrlUserEntity::getCreateDate, startTime, endTime)
            .eq(AnchorUrlUserEntity::getIsDeleted, 0)
            .notIn(EmptyUtil.isNotEmpty(excludeTradeIds), AnchorUrlUserEntity::getTradeId, excludeTradeIds)
            .orderByDesc(AnchorUrlUserEntity::getId).last(" limit " + limit)
            .list();
    }


    /**
     * 获取主播信息
     *
     * @param userAnchors 用户主播数据列表
     *
     * @return 主播信息Map
     */
    private Map<AnchorUrlUserEntity, AnchorUrlEntity> getAnchorInfoMap(List<AnchorUrlUserEntity> userAnchors) {
        if (EmptyUtil.isEmpty(userAnchors)) {
            return Map.of();
        }
        Map<String, AnchorUrlUserEntity> secUidAnchorMap = userAnchors.stream().collect(Collectors.toMap(AnchorUrlUserEntity::getAnchorUrlSecUid, Function.identity(), FunctionUtil::mergeFirst));
        return anchorUrlService.lambdaQuery()
            .in(AnchorUrlEntity::getSecUid, secUidAnchorMap.keySet())
            .eq(AnchorUrlEntity::getPlatform, AnchorPlatformEnum.DOU_YIN.getCode()) // 只查询 0:抖音
            .list()
            .stream()
            .collect(Collectors.toMap(a -> secUidAnchorMap.get(a.getSecUid()), Function.identity(), FunctionUtil::mergeFirst));
    }


    /**
     * 提取查询日期
     */
    private Tuple2<LocalDateTime, LocalDateTime> extractQueryDate(String queryDate, int shardIndex, int shardTotal) {
        // 1. 计算查询时间范围
        int days = StrUtil.isBlank(queryDate) ? 1 : Integer.parseInt(queryDate);

        LocalDateTime startOfDate;
        LocalDateTime endOfDate;
        if (days == 0) {
            // 入参为0：采集今天0点到23:59:59
            LocalDateTime today = LocalDateTime.now();
            startOfDate = today.withHour(0).withMinute(0).withSecond(0).withNano(0);
            endOfDate = today.withHour(23).withMinute(59).withSecond(59).withNano(999999999);
            log.info("[步骤1] 查询今天的主播数据，时间范围: {} ~ {}", startOfDate, endOfDate);
        } else {
            // 入参大于0：查询最近N天的数据（截止到昨天23:59:59）
            LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
            endOfDate = yesterday.withHour(23).withMinute(59).withSecond(59).withNano(999999999);

            LocalDateTime startDate = LocalDateTime.now().minusDays(days);
            startOfDate = startDate.withHour(0).withMinute(0).withSecond(0).withNano(0);
            log.info("[步骤1] 查询最近{}天的主播数据，时间范围: {} ~ {}", days, startOfDate, endOfDate);
        }

        if (shardTotal <= 1 && shardIndex <= 0) {
            log.info("[步骤2] 集群分片总数为1，不进行分片");
            return Tuples.of(startOfDate, endOfDate);
        }

        // 根据分片索引计算当前节点的时间范围
        long minutesPerShard = Duration.between(startOfDate, endOfDate).toMinutes() / shardTotal;
        LocalDateTime actualStartOfDate = startOfDate.plusMinutes(shardIndex * minutesPerShard);
        LocalDateTime actualEndOfDate;

        // 最后一个分片应该到达原始结束时间，避免由于整数除法导致的时间遗漏
        if (shardIndex == shardTotal - 1) {
            actualEndOfDate = endOfDate;
        } else {
            actualEndOfDate = actualStartOfDate.plusMinutes(minutesPerShard);
            // 确保相邻分片之间不会出现时间重叠，最后一个分片除外
            if (shardIndex < shardTotal - 1) {
                actualEndOfDate = actualEndOfDate.minusNanos(1000000); // 减少1毫秒以避免重叠
            }
        }
        log.info("[步骤2] 集群分片索引为{}/{}，计算当前节点的时间范围: {} ~ {}", shardIndex, shardTotal, actualStartOfDate, actualEndOfDate);
        return Tuples.of(actualStartOfDate, actualEndOfDate);
    }
}

