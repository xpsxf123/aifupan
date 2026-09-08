package com.jiuyu.replay.words.bll;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.jiuyu.replay.common.repository.service.SystemKvService;
import com.jiuyu.replay.words.entity.DouyinPeerAvgEntity;
import com.jiuyu.replay.words.producer.DouyinPeerAvgProducer;
import com.jiuyu.replay.words.repository.dao.AnchorUrlUserDao;
import com.jiuyu.replay.words.repository.dao.DouyinPeerAvgDao;
import com.jiuyu.replay.words.vo.peer.AnchorUrlUserTradeVo;
import com.jiuyu.replay.words.vo.peer.DouyinPeerAvgRawVo;
import com.jiuyu.replay.words.vo.peer.DouyinPeerAvgVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 抖音同行直播基础数据平均值 BLL — 预计算 + 查询 + AI 格式化
 *
 * @author jy
 * @date 2026-06-24
 */
@Component
@Slf4j
public class DouyinPeerAvgBll {

    @Resource
    private DouyinPeerAvgDao douyinPeerAvgDao;
    @Resource
    private DouyinPeerAvgProducer douyinPeerAvgProducer;
    @Resource
    private SystemKvService systemKvService;
    @Resource
    private AnchorUrlUserDao anchorUrlUserDao;

    // ===== 字段提取器：RawVo → Number（用于平均值计算）=====

    private static final List<FieldDef> FIELD_DEFS = Arrays.asList(
            // 核心数值（Integer 字段）
            new FieldDef("avgTotalWatchNum", DouyinPeerAvgRawVo::getTotalWatchNum, true),
            new FieldDef("avgAverageOnlineNum", DouyinPeerAvgRawVo::getAverageOnlineNum, true),
            new FieldDef("avgAverageResidenceTime", DouyinPeerAvgRawVo::getAverageResidenceTime, true),
            new FieldDef("avgIncrementFollowerCount", DouyinPeerAvgRawVo::getIncrementFollowerCount, true),
            // Double 字段
            new FieldDef("avgConvertFanRate", DouyinPeerAvgRawVo::getConvertFanRate, false),
            new FieldDef("avgInteractionPercent", DouyinPeerAvgRawVo::getInteractionPercent, false),
            // 区间字段（Integer）
            new FieldDef("avgVolumeStart", DouyinPeerAvgRawVo::getVolumeStart, true),
            new FieldDef("avgVolumeEnd", DouyinPeerAvgRawVo::getVolumeEnd, true),
            new FieldDef("avgPurchaseCountStart", DouyinPeerAvgRawVo::getPurchaseCountStart, true),
            new FieldDef("avgPurchaseCountEnd", DouyinPeerAvgRawVo::getPurchaseCountEnd, true),
            // 区间字段（Double）
            new FieldDef("avgCustomerUnitPriceStart", DouyinPeerAvgRawVo::getCustomerUnitPriceStart, false),
            new FieldDef("avgCustomerUnitPriceEnd", DouyinPeerAvgRawVo::getCustomerUnitPriceEnd, false),
            new FieldDef("avgUvValueStart", DouyinPeerAvgRawVo::getUvValueStart, false),
            new FieldDef("avgUvValueEnd", DouyinPeerAvgRawVo::getUvValueEnd, false),
            new FieldDef("avgGoodsConvertRateStart", DouyinPeerAvgRawVo::getGoodsConvertRateStart, false),
            new FieldDef("avgGoodsConvertRateEnd", DouyinPeerAvgRawVo::getGoodsConvertRateEnd, false),
            new FieldDef("avgGpmStart", DouyinPeerAvgRawVo::getGpmStart, false),
            new FieldDef("avgGpmEnd", DouyinPeerAvgRawVo::getGpmEnd, false)
    );

    /**
     * 按天预计算并存储同行平均值。
     *
     * @param days 回溯完整天数（不含今天）
     */
    @Transactional(rollbackFor = Exception.class)
    public void computeAndSavePeerAvg(int days) {
        int minNum = systemKvService.getValueByKey("douyin_peer_avg_days_min_num", 3);
        Date today = DateUtil.beginOfDay(new Date());

        for (int i = 1; i <= days; i++) {
            DateTime dayStart = DateUtil.offsetDay(today, -i);
            DateTime dayEnd = DateUtil.offsetDay(dayStart, 1);
            List<DouyinPeerAvgRawVo> rawList = douyinPeerAvgDao.selectRawDataByDay(dayStart.toJdkDate(), dayEnd.toJdkDate());
            if (CollUtil.isEmpty(rawList)) {
                log.info("DouyinPeerAvg: day={} 无原始数据，跳过", dayStart.toDateStr());
                continue;
            }

            // trade_id 为 NULL 的，拿 sec_uid 去 tb_anchor_url_user 查行业
            List<DouyinPeerAvgRawVo> noTradeRows = rawList.stream()
                    .filter(r -> r.getTradeId() == null)
                    .collect(Collectors.toList());
            if (!noTradeRows.isEmpty()) {
                Set<String> secUids = noTradeRows.stream()
                        .map(DouyinPeerAvgRawVo::getSecUid)
                        .collect(Collectors.toSet());
                List<AnchorUrlUserTradeVo> userTrades = anchorUrlUserDao.selectTradeBySecUids(secUids);
                Map<String, List<Long>> tradeMap = userTrades.stream()
                        .collect(Collectors.groupingBy(
                                AnchorUrlUserTradeVo::getSecUid,
                                Collectors.mapping(AnchorUrlUserTradeVo::getTradeId, Collectors.toList())
                        ));
                List<DouyinPeerAvgRawVo> expanded = new ArrayList<>();
                for (DouyinPeerAvgRawVo row : rawList) {
                    if (row.getTradeId() != null) {
                        expanded.add(row);
                    } else {
                        List<Long> tids = tradeMap.get(row.getSecUid());
                        if (CollUtil.isEmpty(tids)) continue;
                        for (Long tid : tids) {
                            DouyinPeerAvgRawVo copy = BeanUtil.copyProperties(row, DouyinPeerAvgRawVo.class);
                            copy.setTradeId(tid);
                            expanded.add(copy);
                        }
                    }
                }
                rawList = expanded;
            }

            // 按 tradeId 分组
            Map<Long, List<DouyinPeerAvgRawVo>> groupMap = rawList.stream()
                    .collect(Collectors.groupingBy(DouyinPeerAvgRawVo::getTradeId));

            int savedCount = 0;
            for (Map.Entry<Long, List<DouyinPeerAvgRawVo>> entry : groupMap.entrySet()) {
                Long tradeId = entry.getKey();
                List<DouyinPeerAvgRawVo> rows = entry.getValue();

                // 过滤掉销售额为 -1 或 null 的行
                List<DouyinPeerAvgRawVo> validRows = rows.stream()
                        .filter(r -> isValidSales(r))
                        .collect(Collectors.toList());

                // 按销售额排序
                validRows.sort(Comparator.comparingDouble(this::calcSales));

                // 去头去尾
                if (validRows.size() < minNum + 2) {
                    continue;
                }
                List<DouyinPeerAvgRawVo> coreRows = validRows.subList(1, validRows.size() - 1);
                if (coreRows.size() < minNum) {
                    continue;
                }

                DouyinPeerAvgEntity entity = new DouyinPeerAvgEntity();
                entity.setTradeId(tradeId);
                entity.setStatDate(dayStart.toSqlDate());
                entity.setVideoCount(coreRows.size());

                // 逐字段计算平均值
                for (FieldDef fd : FIELD_DEFS) {
                    BigDecimal avg = computeAvg(coreRows, fd.extractor, fd.isInt);
                    fd.setter.accept(entity, avg);
                }

                douyinPeerAvgProducer.upsert(entity);
                savedCount++;
            }
            log.info("DouyinPeerAvg: day={} 完成，保存 {} 个行业", dayStart.toDateStr(), savedCount);
        }
    }

    /**
     * 按行业和天数查询同行平均值。
     *
     * @param tradeId 行业ID
     * @param days    回溯完整天数
     * @return 聚合结果列表（按天倒序）
     */
    public List<DouyinPeerAvgVo> getPeerAvgByTradeAndDays(Long tradeId, int days) {
        DateTime today = DateUtil.beginOfDay(new Date());
        Date startDate = DateUtil.offsetDay(today, -days).toJdkDate();
        return douyinPeerAvgProducer.listByTradeIdAndDateRange(tradeId, startDate, today.toJdkDate());
    }

    /**
     * 格式化为 AI 提示词文本。
     *
     * @param list      聚合结果列表
     * @param tradeName 行业名称
     * @param days      回溯天数
     * @return 格式化文本，无数据返回 null
     */
    public String formatPeerAvgForAi(List<DouyinPeerAvgVo> list, String tradeName, int days) {
        if (CollUtil.isEmpty(list)) return null;

        StringBuilder sb = new StringBuilder();
        sb.append("【").append(tradeName).append("】行业同行近").append(days).append("天直播数据平均值：\n");

        for (DouyinPeerAvgVo vo : list) {
            sb.append(DateUtil.formatDate(vo.getStatDate())).append("（").append(vo.getVideoCount()).append("个样本）：");
            appendIfNotNull(sb, "总观看人次", vo.getAvgTotalWatchNum(), false);
            appendIfNotNull(sb, "在线人数", vo.getAvgAverageOnlineNum(), false);
            appendIfNotNull(sb, "停留时长(秒)", vo.getAvgAverageResidenceTime(), false);
            appendIfNotNull(sb, "新增粉丝", vo.getAvgIncrementFollowerCount(), false);
            appendIfNotNull(sb, "粉丝转化率", vo.getAvgConvertFanRate(), true);
            appendIfNotNull(sb, "互动率", vo.getAvgInteractionPercent(), true);
            appendRangeField(sb, "销售额", vo.getAvgVolumeStart(), vo.getAvgVolumeEnd(), false);
            appendRangeField(sb, "销量", vo.getAvgPurchaseCountStart(), vo.getAvgPurchaseCountEnd(), false);
            appendRangeField(sb, "客单价", vo.getAvgCustomerUnitPriceStart(), vo.getAvgCustomerUnitPriceEnd(), true);
            appendRangeField(sb, "UV价值", vo.getAvgUvValueStart(), vo.getAvgUvValueEnd(), true);
            appendRangeField(sb, "带货转换率", vo.getAvgGoodsConvertRateStart(), vo.getAvgGoodsConvertRateEnd(), true);
            appendRangeField(sb, "GPM", vo.getAvgGpmStart(), vo.getAvgGpmEnd(), true);
            sb.append("\n");
        }
        return sb.toString();
    }

    // ===== 内部方法 =====

    private boolean isValidSales(DouyinPeerAvgRawVo r) {
        Integer vs = r.getVolumeStart();
        Integer ve = r.getVolumeEnd();
        if (vs == null || ve == null || vs == -1 || ve == -1) return false;
        return true;
    }

    private double calcSales(DouyinPeerAvgRawVo r) {
        return (r.getVolumeStart() + r.getVolumeEnd()) / 2.0;
    }

    /**
     * 计算字段平均值，跳过 -1 和 null 的值。
     */
    private BigDecimal computeAvg(List<DouyinPeerAvgRawVo> rows, Function<DouyinPeerAvgRawVo, Number> extractor, boolean isInt) {
        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        for (DouyinPeerAvgRawVo row : rows) {
            Number val = extractor.apply(row);
            if (val == null) continue;
            double dv = val.doubleValue();
            if (dv == -1) continue;
            sum = sum.add(BigDecimal.valueOf(dv));
            count++;
        }
        if (count == 0) return null;
        return sum.divide(BigDecimal.valueOf(count), isInt ? 2 : 4, RoundingMode.HALF_UP);
    }

    private void appendIfNotNull(StringBuilder sb, String label, BigDecimal val, boolean isRate) {
        if (val == null) return;
        sb.append(label).append("=");
        if (isRate) {
            sb.append(val.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP)).append("%");
        } else {
            sb.append(val.stripTrailingZeros().toPlainString());
        }
        sb.append("，");
    }

    private void appendRangeField(StringBuilder sb, String label, BigDecimal start, BigDecimal end, boolean isRate) {
        if (start == null && end == null) return;
        sb.append(label).append("=");
        String s = start != null ? (isRate ? toPercentStr(start) : start.stripTrailingZeros().toPlainString()) : "?";
        String e = end != null ? (isRate ? toPercentStr(end) : end.stripTrailingZeros().toPlainString()) : "?";
        sb.append(s).append("~").append(e);
        sb.append("，");
    }

    private String toPercentStr(BigDecimal val) {
        return val.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP).toPlainString() + "%";
    }

    // ===== 字段定义 =====

    private static class FieldDef {
        final String name;
        final Function<DouyinPeerAvgRawVo, Number> extractor;
        final boolean isInt;
        final java.util.function.BiConsumer<DouyinPeerAvgEntity, BigDecimal> setter;

        FieldDef(String name, Function<DouyinPeerAvgRawVo, Number> extractor, boolean isInt) {
            this.name = name;
            this.extractor = extractor;
            this.isInt = isInt;
            this.setter = buildSetter(name);
        }

        private static java.util.function.BiConsumer<DouyinPeerAvgEntity, BigDecimal> buildSetter(String name) {
            switch (name) {
                case "avgTotalWatchNum":
                    return DouyinPeerAvgEntity::setAvgTotalWatchNum;
                case "avgAverageOnlineNum":
                    return DouyinPeerAvgEntity::setAvgAverageOnlineNum;
                case "avgAverageResidenceTime":
                    return DouyinPeerAvgEntity::setAvgAverageResidenceTime;
                case "avgIncrementFollowerCount":
                    return DouyinPeerAvgEntity::setAvgIncrementFollowerCount;
                case "avgConvertFanRate":
                    return DouyinPeerAvgEntity::setAvgConvertFanRate;
                case "avgInteractionPercent":
                    return DouyinPeerAvgEntity::setAvgInteractionPercent;
                case "avgVolumeStart":
                    return DouyinPeerAvgEntity::setAvgVolumeStart;
                case "avgVolumeEnd":
                    return DouyinPeerAvgEntity::setAvgVolumeEnd;
                case "avgPurchaseCountStart":
                    return DouyinPeerAvgEntity::setAvgPurchaseCountStart;
                case "avgPurchaseCountEnd":
                    return DouyinPeerAvgEntity::setAvgPurchaseCountEnd;
                case "avgCustomerUnitPriceStart":
                    return DouyinPeerAvgEntity::setAvgCustomerUnitPriceStart;
                case "avgCustomerUnitPriceEnd":
                    return DouyinPeerAvgEntity::setAvgCustomerUnitPriceEnd;
                case "avgUvValueStart":
                    return DouyinPeerAvgEntity::setAvgUvValueStart;
                case "avgUvValueEnd":
                    return DouyinPeerAvgEntity::setAvgUvValueEnd;
                case "avgGoodsConvertRateStart":
                    return DouyinPeerAvgEntity::setAvgGoodsConvertRateStart;
                case "avgGoodsConvertRateEnd":
                    return DouyinPeerAvgEntity::setAvgGoodsConvertRateEnd;
                case "avgGpmStart":
                    return DouyinPeerAvgEntity::setAvgGpmStart;
                case "avgGpmEnd":
                    return DouyinPeerAvgEntity::setAvgGpmEnd;
                default:
                    throw new IllegalArgumentException("Unknown field: " + name);
            }
        }
    }
}
