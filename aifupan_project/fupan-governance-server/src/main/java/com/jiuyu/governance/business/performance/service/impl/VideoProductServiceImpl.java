package com.jiuyu.governance.business.performance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.governance.business.performance.mapper.VideoProductMapper;
import com.jiuyu.governance.business.performance.pojo.bo.CommodityProcessDataBo;
import com.jiuyu.governance.business.performance.pojo.entity.VideoProduct;
import com.jiuyu.governance.business.performance.pojo.request.VideoProductPageRequest;
import com.jiuyu.governance.business.performance.pojo.response.client.VideoProductPageResponse;
import com.jiuyu.governance.business.performance.service.VideoProductService;
import com.jiuyu.governance.plugins.oss.storage.impl.ProcessDataStorageService;
import com.jiuyu.framework.util.EmptyUtil;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 视频商品服务实现类
 *
 * @author lj
 * @date 2026-03-19
 */
@Service
@RequiredArgsConstructor
public class VideoProductServiceImpl extends ServiceImpl<VideoProductMapper, VideoProduct> implements VideoProductService {

    private final ProcessDataStorageService processDataStorageService;

    @Override
    public VideoProductPageResponse pageByBatchNumber(VideoProductPageRequest request) {
        // 全量查询该批次下所有商品
        LambdaQueryWrapper<VideoProduct> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VideoProduct::getTenantId, request.getTenantId())
                .eq(VideoProduct::getBatchNumber, request.getBatchNumber())
                .like(StringUtils.isNotBlank(request.getTitle()), VideoProduct::getTitle, request.getTitle());

        List<VideoProduct> products = list(wrapper);
        LocalDateTime startTime = request.getStartTime();
        LocalDateTime endTime = request.getEndTime();
        boolean needOss = startTime != null && endTime != null;

        // 遍历商品，有过程数据的计算时间段增量，没有的用原始值
        List<VideoProductPageResponse.ProductItem> records = products.stream()
                .map(p -> computeProductItem(p, needOss, startTime, endTime))
                .collect(Collectors.toList());

        // 内存排序（默认按 sort 字段）
        String sortBy = request.getSortBy();
        boolean asc = "asc".equalsIgnoreCase(request.getSortOrder());
        records.sort(buildComparator(sortBy, asc));

        // 内存分页
        int pageNum = request.getPage();
        int pageSize = request.getLimit();
        int total = records.size();
        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);

        List<VideoProductPageResponse.ProductItem> paged;
        if (fromIndex >= total) {
            paged = Collections.emptyList();
        } else {
            paged = new ArrayList<>(records.subList(fromIndex, toIndex));
        }

        long pages = (total + pageSize - 1) / pageSize;
        return new VideoProductPageResponse(paged, total, pageSize, pageNum, pages, true);
    }

    @Override
    public List<VideoProduct> listByTenantAndBatchNumbers(Long tenantId, Collection<String> batchNumbers) {
        if (tenantId == null || EmptyUtil.isEmpty(batchNumbers)) {
            return List.of();
        }
        return lambdaQuery()
                .eq(VideoProduct::getTenantId, tenantId)
                .in(VideoProduct::getBatchNumber, batchNumbers)
                .list();
    }

    @Override
    public Set<String> listExistBatchNumbers(Long tenantId, Collection<String> batchNumbers) {
        if (tenantId == null || EmptyUtil.isEmpty(batchNumbers)) {
            return Set.of();
        }
        // 只取 batch_number 一列，避免把整行商品数据捞进内存
        return lambdaQuery()
                .select(VideoProduct::getBatchNumber)
                .eq(VideoProduct::getTenantId, tenantId)
                .in(VideoProduct::getBatchNumber, batchNumbers)
                .list()
                .stream()
                .map(VideoProduct::getBatchNumber)
                .collect(Collectors.toSet());
    }

    @Override
    public Map<String, List<String>> getProductBatchNumberMap(Long tenantId, Collection<String> productIds,
                                                              LocalDateTime startTime, LocalDateTime endTime) {
        if (tenantId == null || EmptyUtil.isEmpty(productIds)) {
            return Map.of();
        }
        // 只取 product_id + batch_number 两列；走 idx_tenant_product 索引
        List<VideoProduct> rows = lambdaQuery()
                .select(VideoProduct::getProductId, VideoProduct::getBatchNumber)
                .eq(VideoProduct::getTenantId, tenantId)
                .in(VideoProduct::getProductId, productIds)
                .ge(startTime != null, VideoProduct::getCreateDate, startTime)
                .le(endTime != null, VideoProduct::getCreateDate, endTime)
                .isNotNull(VideoProduct::getBatchNumber)
                .list();

        // 同一商品可能出现在多个批次，且同一批次内可能有多段视频，先用 Set 去重
        Map<String, Set<String>> grouped = new LinkedHashMap<>();
        for (VideoProduct row : rows) {
            grouped.computeIfAbsent(row.getProductId(), k -> new LinkedHashSet<>()).add(row.getBatchNumber());
        }

        Map<String, List<String>> batchNumberMap = new HashMap<>(grouped.size());
        grouped.forEach((productId, batchNumbers) -> batchNumberMap.put(productId, new ArrayList<>(batchNumbers)));
        return batchNumberMap;
    }

    @Override
    public Map<Long, List<CommodityProcessDataBo>> getProcessDataMap(Collection<VideoProduct> products, int step) {
        if (EmptyUtil.isEmpty(products)) {
            return Map.of();
        }
        int normalizedStep = Math.max(1, step);

        Map<Long, List<CommodityProcessDataBo>> processDataMap = new LinkedHashMap<>();
        for (VideoProduct product : products) {
            if (StringUtils.isBlank(product.getProductOssKey())) {
                continue;
            }
            // 下载失败时内部已降级为空列表
            List<CommodityProcessDataBo> processData = processDataStorageService.downloadProcessDataFromOss(
                    product.getProductOssKey(), CommodityProcessDataBo.class);
            List<CommodityProcessDataBo> sampled = sampleByStep(processData, normalizedStep);
            if (EmptyUtil.isNotEmpty(sampled)) {
                processDataMap.put(product.getId(), sampled);
            }
        }
        return processDataMap;
    }

    /**
     * 按步长（分钟）对累计快照序列降采样：分窗后每个窗口只留窗口内最后一条。
     * <p>
     * 窗口起点对齐到首条数据所在的分钟。step=1 时等价于「每分钟保留最晚一条」，
     * 正好抹平写入侧 dedupByMinute 留下的同分钟两条记录。
     * </p>
     */
    private List<CommodityProcessDataBo> sampleByStep(List<CommodityProcessDataBo> processData, int step) {
        if (EmptyUtil.isEmpty(processData)) {
            return List.of();
        }

        List<CommodityProcessDataBo> sorted = processData.stream()
                .filter(d -> d.getDateTime() != null)
                .sorted(Comparator.comparing(CommodityProcessDataBo::getDateTime))
                .toList();
        if (sorted.isEmpty()) {
            return List.of();
        }

        LocalDateTime base = sorted.get(0).getDateTime().truncatedTo(ChronoUnit.MINUTES);
        // 已按时间升序，同窗口后写入的自然覆盖前一条，最终留下的就是窗口末值
        Map<Long, CommodityProcessDataBo> lastPerWindow = new LinkedHashMap<>();
        for (CommodityProcessDataBo item : sorted) {
            long window = ChronoUnit.MINUTES.between(base, item.getDateTime()) / step;
            lastPerWindow.put(window, item);
        }
        return new ArrayList<>(lastPerWindow.values());
    }

    /**
     * 计算单个商品的指标：有过程数据且时间段内≥2个时间点 → delta计算；否则 → 原始值
     */
    private VideoProductPageResponse.ProductItem computeProductItem(
            VideoProduct product, boolean needOss, LocalDateTime startTime, LocalDateTime endTime) {
        if (!needOss || StringUtils.isBlank(product.getProductOssKey())) {
            return toProductItemFromOriginal(product);
        }

        // 从OSS下载过程数据，失败时自动降级返回空列表
        List<CommodityProcessDataBo> processData =
                processDataStorageService.downloadProcessDataFromOss(
                        product.getProductOssKey(), CommodityProcessDataBo.class);

        // 过滤时间范围，按采集时间升序排列
        LocalDateTime tempEndTime = endTime.plusSeconds(1);
        List<CommodityProcessDataBo> filtered = processData.stream()
                .filter(d -> d.getDateTime() != null
                        && !d.getDateTime().isBefore(startTime)
                        && !d.getDateTime().isAfter(tempEndTime))
                .sorted(Comparator.comparing(CommodityProcessDataBo::getDateTime))
                .toList();

        // 不足2个时间点无法计算增量，回退到原始值
        if (filtered.size() < 2) {
            return toProductItemFromOriginal(product);
        }

        // 取最早和最晚两条，差值即为时间段内的增量
        CommodityProcessDataBo first = filtered.get(0);
        CommodityProcessDataBo last = filtered.get(filtered.size() - 1);

        // 仅当商品在查询开始前已上架时，first才包含期初累计，才需要做减法。
        // 商品在开始时间之后上架（或无上架时间），期初累计为零，直接用last即可。
        if (product.getProductBindTime() == null
                || !product.getProductBindTime().isBefore(startTime)) {
            first = new CommodityProcessDataBo();
        }

        return toProductItemFromDelta(first, last, product);
    }

    /**
     * 直接使用video_product表中的原始值（无过程数据或过程数据不足时回退）
     */
    private VideoProductPageResponse.ProductItem toProductItemFromOriginal(VideoProduct p) {
        VideoProductPageResponse.ProductItem item = new VideoProductPageResponse.ProductItem();
        item.setTitle(p.getTitle());
        item.setImageUri(p.getImageUri());
        item.setProductShowUcnt(p.getProductShowUcnt());
        item.setProductViewShowRatio(p.getProductViewShowRatio());
        item.setProductClickUcnt(p.getProductClickUcnt());
        item.setProductShowClickUcntRatio(p.getProductShowClickUcntRatio());
        item.setProductClickPayUcntRatio(p.getProductClickPayUcntRatio());
        item.setAvgPayAmtPerOrder(p.getAvgPayAmtPerOrder());
        item.setPayCnt(p.getPayCnt());
        item.setPayAmt(p.getPayAmt());
        item.setExplainCnt(p.getExplainCnt());
        item.setSort(p.getSort());
        return item;
    }

    /**
     * 用过程数据的最早和最晚两条记录计算时间段增量。
     * 累计字段：最后 − 最早 = 时间段内增量
     * 比率字段：用增量重新计算（曝光观看率 = 曝光人数增量 ÷ 观看人数增量 × 100）
     * avgPayAmtPerOrder 用video_product原始值，不通过过程数据计算
     */
    private VideoProductPageResponse.ProductItem toProductItemFromDelta(
            CommodityProcessDataBo first, CommodityProcessDataBo last, VideoProduct p) {
        // 累计字段：最后 − 最早
        long deltaShowUcnt = subLong(last.getProductShowUcnt(), first.getProductShowUcnt());
        long deltaClickUcnt = subLong(last.getProductClickUcnt(), first.getProductClickUcnt());
        int deltaViewCount = subInt(last.getViewCount(), first.getViewCount());
        int deltaOrderCnt = subInt(last.getPayComboCnt(), first.getPayComboCnt());
        BigDecimal deltaPayAmt = subDecimal(last.getPayAmt(), first.getPayAmt());
        int deltaExplainCnt = subInt(last.getExplainCnt(), first.getExplainCnt());

        VideoProductPageResponse.ProductItem item = new VideoProductPageResponse.ProductItem();
        item.setTitle(p.getTitle());
        item.setImageUri(p.getImageUri());
        item.setProductShowUcnt(deltaShowUcnt);
        // 比率用增量重新计算
        item.setProductViewShowRatio(calcRatio(deltaShowUcnt, deltaViewCount));
        item.setProductClickUcnt(deltaClickUcnt);
        item.setProductShowClickUcntRatio(calcRatio(deltaClickUcnt, deltaShowUcnt));
        // 点击-成交率：用订单数增量代替成交人数（过程数据没有成交人数字段）
        item.setProductClickPayUcntRatio(calcRatio(deltaOrderCnt, deltaClickUcnt));
        item.setAvgPayAmtPerOrder(p.getAvgPayAmtPerOrder());
        item.setPayCnt((long) deltaOrderCnt);
        item.setPayAmt(deltaPayAmt);
        item.setExplainCnt(deltaExplainCnt);
        item.setSort(p.getSort());
        return item;
    }

    /** null 安全比较器：nulls 恒排最后，asc 只作用于非 null 值 */
    private <U extends Comparable<? super U>> Comparator<VideoProductPageResponse.ProductItem> nullSafe(
            Function<VideoProductPageResponse.ProductItem, U> keyExtractor, boolean asc) {
        Comparator<U> keyCmp = asc ? Comparator.naturalOrder() : Comparator.reverseOrder();
        return Comparator.comparing(keyExtractor, Comparator.nullsLast(keyCmp));
    }

    private Comparator<VideoProductPageResponse.ProductItem> buildComparator(String sortBy, boolean asc) {
        return switch (sortBy != null ? sortBy : "") {
            case "productShowUcnt" -> nullSafe(VideoProductPageResponse.ProductItem::getProductShowUcnt, asc);
            case "productViewShowRatio" -> nullSafe(VideoProductPageResponse.ProductItem::getProductViewShowRatio, asc);
            case "productClickUcnt" -> nullSafe(VideoProductPageResponse.ProductItem::getProductClickUcnt, asc);
            case "productShowClickUcntRatio" -> nullSafe(VideoProductPageResponse.ProductItem::getProductShowClickUcntRatio, asc);
            case "productClickPayUcntRatio" -> nullSafe(VideoProductPageResponse.ProductItem::getProductClickPayUcntRatio, asc);
            case "avgPayAmtPerOrder" -> nullSafe(VideoProductPageResponse.ProductItem::getAvgPayAmtPerOrder, asc);
            case "explainCnt" -> nullSafe(VideoProductPageResponse.ProductItem::getExplainCnt, asc);
            case "payCnt" -> nullSafe(VideoProductPageResponse.ProductItem::getPayCnt, asc);
            case "payAmt" -> nullSafe(VideoProductPageResponse.ProductItem::getPayAmt, asc);
            case "sort" -> nullSafe(VideoProductPageResponse.ProductItem::getSort, asc);
            default -> nullSafe(VideoProductPageResponse.ProductItem::getSort, asc);
        };
    }

    /** null安全的Long减法，null当0处理，负数取0 */
    private long subLong(Long a, Long b) {
        long va = a != null ? a : 0L;
        long vb = b != null ? b : 0L;
        return Math.max(0, va - vb);
    }

    /** null安全的Integer减法，null当0处理，负数取0 */
    private int subInt(Integer a, Integer b) {
        int va = a != null ? a : 0;
        int vb = b != null ? b : 0;
        return Math.max(0, va - vb);
    }

    /** null安全的BigDecimal减法，null当0处理，负数取0 */
    private BigDecimal subDecimal(BigDecimal a, BigDecimal b) {
        BigDecimal va = a != null ? a : BigDecimal.ZERO;
        BigDecimal vb = b != null ? b : BigDecimal.ZERO;
        BigDecimal result = va.subtract(vb);
        return result.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : result;
    }

    /** 比率计算：分子 ÷ 分母 × 100，保留2位小数，分母为0返回0 */
    private BigDecimal calcRatio(long numerator, long denominator) {
        if (denominator == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }
}
