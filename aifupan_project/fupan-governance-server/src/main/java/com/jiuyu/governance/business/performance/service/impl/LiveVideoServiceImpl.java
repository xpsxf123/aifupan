package com.jiuyu.governance.business.performance.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.governance.business.performance.mapper.LiveVideoMapper;
import com.jiuyu.governance.business.performance.pojo.bo.CommodityProcessDataBo;
import com.jiuyu.governance.business.performance.pojo.bo.OceanEngineProcessBo;
import com.jiuyu.governance.business.performance.utils.MetricsUtil;
import com.jiuyu.governance.business.performance.pojo.bo.PendingVideoGroup;
import com.jiuyu.governance.business.performance.pojo.constants.ProcessStatus;
import com.jiuyu.governance.business.performance.pojo.entity.LiveVideo;
import com.jiuyu.governance.business.performance.pojo.entity.VideoProduct;
import com.jiuyu.governance.business.performance.pojo.request.ClientPushVideoRequest;
import com.jiuyu.governance.business.performance.pojo.request.VideoProductRequest;
import com.jiuyu.governance.business.performance.service.LiveVideoService;
import com.jiuyu.governance.business.performance.service.VideoProductService;
import com.jiuyu.governance.common.exceptions.BusinessException;
import com.jiuyu.governance.plugins.oss.storage.impl.ImagesStorageService;
import com.jiuyu.governance.plugins.oss.storage.impl.ProcessDataStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 视频服务实现类
 *
 * @author lj
 * @date 2026-03-19
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LiveVideoServiceImpl extends ServiceImpl<LiveVideoMapper, LiveVideo> implements LiveVideoService {

    private final VideoProductService videoProductService;
    private final ProcessDataStorageService processDataStorageService;
    private final ImagesStorageService imagesStorageService;
    private final ObjectMapper objectMapper;
    private final ThreadPoolTaskExecutor productUploadExecutor;

    /**
     * 重置超时的处理中视频为待处理状态
     *
     * @param timeoutMinutes 超时时间（分钟）
     * @return 是否执行成功
     */
    @Override
    public boolean resetTimeoutProcessingVideos(int timeoutMinutes) {
        LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(timeoutMinutes);

        // 直接更新处理中且超时的视频为待处理状态
        return this.lambdaUpdate()
                .set(LiveVideo::getProcessStatus, ProcessStatus.PENDING.getCode())
                .set(LiveVideo::getUpdateDate, LocalDateTime.now())
                .eq(LiveVideo::getProcessStatus, ProcessStatus.PROCESSING.getCode())
                .lt(LiveVideo::getProcessTime, timeoutThreshold)
                .update();
    }

    /**
     * 获取待处理视频分组列表
     *
     * @param limit 限制数量
     * @return 待处理视频分组列表
     */
    @Override
    public List<PendingVideoGroup> getPendingVideoGroups(int limit) {
        return this.lambdaQuery()
                .select(LiveVideo::getTenantId, LiveVideo::getBatchNumber)
                .eq(LiveVideo::getProcessStatus, ProcessStatus.PENDING.getCode())
                .groupBy(LiveVideo::getTenantId, LiveVideo::getBatchNumber)
                .orderByAsc(LiveVideo::getId)
                .list()
                .stream()
                .map(item -> new PendingVideoGroup(item.getTenantId(), item.getBatchNumber()))
                .collect(Collectors.toList());
    }

    /**
     * 根据租户ID和直播批次号获取待处理视频列表，并将状态更新为处理中
     *
     * @param tenantId    租户ID
     * @param batchNumber 直播批次号
     * @return 待处理视频列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<LiveVideo> fetchAndLockPendingVideos(Long tenantId, String batchNumber) {
        // 查询待处理的视频
        List<LiveVideo> pendingVideos = this.lambdaQuery()
                .eq(LiveVideo::getTenantId, tenantId)
                .eq(LiveVideo::getBatchNumber, batchNumber)
                .list();

        if (pendingVideos.isEmpty()) {
            return Collections.emptyList();
        }

        // 获取视频ID列表
        List<Long> videoIds = pendingVideos.stream()
                .map(LiveVideo::getId)
                .collect(Collectors.toList());

        // 将状态更新为处理中
        this.lambdaUpdate()
                .set(LiveVideo::getProcessStatus, ProcessStatus.PROCESSING.getCode())
                .set(LiveVideo::getProcessTime, LocalDateTime.now())
                .set(LiveVideo::getUpdateDate, LocalDateTime.now())
                .in(LiveVideo::getId, videoIds)
                .update();

        log.info("[视频任务] 锁定待处理视频, tenantId={}, batchNumber={}, count={}", tenantId, batchNumber, pendingVideos.size());
        return pendingVideos;
    }

    /**
     * 更新视频处理结果为成功
     *
     * @param videoId 视频ID
     * @return 是否更新成功
     */
    @Override
    public boolean updateProcessSuccess(Long videoId) {
        return this.lambdaUpdate()
                .set(LiveVideo::getProcessStatus, ProcessStatus.SUCCESS.getCode())
                .set(LiveVideo::getUpdateDate, LocalDateTime.now())
                .set(LiveVideo::getFailReason, null)
                .eq(LiveVideo::getId, videoId)
                .update();
    }

    /**
     * 更新视频处理结果为失败
     *
     * @param videoId    视频ID
     * @param failReason 失败原因
     * @return 是否更新成功
     */
    @Override
    public boolean updateProcessFailed(Long videoId, String failReason) {
        return this.lambdaUpdate()
                .set(LiveVideo::getProcessStatus, ProcessStatus.FAILED.getCode())
                .set(LiveVideo::getUpdateDate, LocalDateTime.now())
                .set(LiveVideo::getFailReason, failReason)
                .eq(LiveVideo::getId, videoId)
                .update();
    }

    /**
     * 批量更新视频处理结果为成功
     *
     * @param videoIds 视频ID列表
     * @return 是否更新成功
     */
    @Override
    public boolean batchUpdateProcessSuccess(List<Long> videoIds) {
        if (videoIds == null || videoIds.isEmpty()) {
            return true;
        }
        return this.lambdaUpdate()
                .set(LiveVideo::getProcessStatus, ProcessStatus.SUCCESS.getCode())
                .set(LiveVideo::getUpdateDate, LocalDateTime.now())
                .set(LiveVideo::getFailReason, null)
                .in(LiveVideo::getId, videoIds)
                .update();
    }

    /**
     * 批量更新视频处理结果为失败
     *
     * @param videoIds   视频ID列表
     * @param failReason 失败原因
     * @return 是否更新成功
     */
    @Override
    public boolean batchUpdateProcessFailed(List<Long> videoIds, String failReason) {
        if (videoIds == null || videoIds.isEmpty()) {
            return true;
        }
        return this.lambdaUpdate()
                .set(LiveVideo::getProcessStatus, ProcessStatus.FAILED.getCode())
                .set(LiveVideo::getUpdateDate, LocalDateTime.now())
                .set(LiveVideo::getFailReason, failReason)
                .in(LiveVideo::getId, videoIds)
                .update();
    }

    @Override
    public boolean hasBatchNumber(Long tenantId, String batchNumber) {
        Long liveVideo = this.lambdaQuery()
                .eq(LiveVideo::getTenantId, tenantId)
                .eq(LiveVideo::getBatchNumber, batchNumber)
                .select(LiveVideo::getBatchNumber)
                .count();
        return ObjUtil.defaultIfNull(liveVideo, 0L) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clientPushVideo(ClientPushVideoRequest request) {
        long startTime = System.currentTimeMillis();
        Long tenantId = request.getTenantId();
        Integer platform = request.getPlatform();
        String secUid = request.getSecUid();
        String batchNumber = request.getBatchNumber();

        if (EmptyUtil.isEmpty(secUid)) {
            throw new BusinessException("secUid 不能为空");
        }

        log.debug("[业绩] 客户端推送视频开始, tenantId={}, batchNumber={}", tenantId, batchNumber);

        // Step1：查询现有 live_video 记录
        long step1Start = System.currentTimeMillis();
        LiveVideo existVideo = this.lambdaQuery()
                .eq(LiveVideo::getTenantId, tenantId)
                .eq(LiveVideo::getBatchNumber, batchNumber)
                .eq(LiveVideo::getSecUid, secUid)
                .eq(LiveVideo::getPlatform, platform)
                .one();
        log.debug("[业绩] Step1 查询 live_video 耗时: {}ms", System.currentTimeMillis() - step1Start);

        // Step2：合并直播过程数据
        long step2Start = System.currentTimeMillis();
        String liveOssBizPath = buildLiveOssBizPath(tenantId, platform, secUid, batchNumber);
        List<OceanEngineProcessBo> mergedProcessList = mergeLiveProcessData(existVideo, request, liveOssBizPath);
        log.debug("[业绩] Step2 合并过程数据耗时: {}ms", System.currentTimeMillis() - step2Start);

        // Step3：计算最终业绩指标（直接写入 request）
        calculateMetricsFromProcess(mergedProcessList, request);

        // Step4：save/update live_video（使用实际 OSS Key）
        long step4Start = System.currentTimeMillis();
        String actualLiveOssKey = uploadProcessDataToOss(liveOssBizPath, mergedProcessList);
        LocalDateTime now = LocalDateTime.now();
        saveOrUpdateLiveVideo(existVideo, request, actualLiveOssKey, now);
        log.debug("[业绩] Step4 保存 live_video 耗时: {}ms", System.currentTimeMillis() - step4Start);

        // Step5：处理商品列表
        long step5Start = System.currentTimeMillis();
        String videoId = existVideo != null ? existVideo.getVideoId() : batchNumber;
        processProductsForClientPush(request, videoId, tenantId, platform, secUid, batchNumber, now);
        log.debug("[业绩] Step5 处理商品耗时: {}ms", System.currentTimeMillis() - step5Start);

        log.info("[业绩] 客户端推送视频完成, tenantId={}, batchNumber={}, videoId={}, 总耗时: {}ms",
                tenantId, batchNumber, videoId, System.currentTimeMillis() - startTime);
    }

    /**
     * 合并直播过程数据
     */
    private List<OceanEngineProcessBo> mergeLiveProcessData(LiveVideo existVideo,
                                                            ClientPushVideoRequest request,
                                                            String liveOssBizPath) {
        List<OceanEngineProcessBo> oldList = Collections.emptyList();
        if (existVideo != null && EmptyUtil.isNotEmpty(existVideo.getVideoOssUrl())) {
            oldList = processDataStorageService.downloadProcessDataFromOss(existVideo.getVideoOssUrl(), OceanEngineProcessBo.class);
        }

        List<OceanEngineProcessBo> newList = request.getOceanEngineProcessList();
        List<OceanEngineProcessBo> merged = new ArrayList<>(oldList);
        if (EmptyUtil.isNotEmpty(newList)) {
            merged.addAll(newList);
        }

        return dedupByMinute(merged, OceanEngineProcessBo::getGatherDateTime);
    }

    /**
     * 按分钟去重，保留每分钟时间最早和最晚的记录
     */
    private <T> List<T> dedupByMinute(List<T> list, java.util.function.Function<T, LocalDateTime> timeExtractor) {
        if (EmptyUtil.isEmpty(list)) {
            return Collections.emptyList();
        }

        Map<LocalDateTime, List<T>> grouped = list.stream()
                .collect(Collectors.groupingBy(
                        item -> timeExtractor.apply(item).truncatedTo(ChronoUnit.MINUTES)
                ));

        List<T> result = new ArrayList<>(list.size());
        for (List<T> items : grouped.values()) {
            if (items.size() == 1) {
                result.add(items.get(0));
            } else {
                // 单次遍历找最早和最晚
                T minItem = items.get(0);
                T maxItem = items.get(0);
                LocalDateTime minTime = timeExtractor.apply(minItem);
                LocalDateTime maxTime = timeExtractor.apply(maxItem);

                for (int i = 1; i < items.size(); i++) {
                    T item = items.get(i);
                    LocalDateTime t = timeExtractor.apply(item);
                    if (t.isBefore(minTime)) {
                        minItem = item;
                        minTime = t;
                    }
                    if (t.isAfter(maxTime)) {
                        maxItem = item;
                        maxTime = t;
                    }
                }
                result.add(minItem);
                result.add(maxItem);
            }
        }

        // 按时间升序
        result.sort(Comparator.comparing(timeExtractor));
        return result;
    }

    /**
     * 上传过程数据到 OSS
     *
     * @param bizPath 业务路径
     * @param data    数据列表
     * @return 实际 OSS Key（含环境前缀和路径前缀）
     */
    private <T> String uploadProcessDataToOss(String bizPath, List<T> data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            return processDataStorageService.upload(bizPath, bytes, "application/json");
        } catch (JsonProcessingException e) {
            log.error("[业绩] 序列化过程数据失败, bizPath={}", bizPath, e);
            throw new RuntimeException("序列化过程数据失败", e);
        }
    }

    /**
     * 从合并后的过程数据计算最终业绩指标
     */
    private void calculateMetricsFromProcess(List<OceanEngineProcessBo> mergedList,
                                             ClientPushVideoRequest request) {
        // 如果过程数据为空，直接使用请求中的 BasePerformanceMetrics 字段
        if (EmptyUtil.isEmpty(mergedList)) {
            return;
        }

        // 取 gatherDateTime 最大的一条作为快照
        OceanEngineProcessBo latest = mergedList.stream()
                .max(Comparator.comparing(OceanEngineProcessBo::getGatherDateTime))
                .orElseThrow(() -> new IllegalStateException("mergedList 不为空但无法获取最大值"));

        // 原始字段（从 latest 快照取值）
        Integer viewCount = latest.getViewCount() != null ? latest.getViewCount() : 0;
        BigDecimal salesRevenue = latest.getSalesRevenue() != null ? latest.getSalesRevenue() : BigDecimal.ZERO;
        BigDecimal refund = latest.getRefund() != null ? latest.getRefund() : BigDecimal.ZERO;
        BigDecimal investment = latest.getInvestment() != null ? latest.getInvestment() : BigDecimal.ZERO;
        Integer payComboCnt = latest.getPayComboCnt() != null ? latest.getPayComboCnt() : 0;
        Integer refundQuantity = latest.getRefundQuantity() != null ? latest.getRefundQuantity() : 0;
        Integer exposureCount = latest.getExposureCount() != null ? latest.getExposureCount() : 0;
        Integer followCount = latest.getFollowCount() != null ? latest.getFollowCount() : 0;
        BigDecimal clickPaymentRate = latest.getClickPaymentRate();
        BigDecimal interactionRate = latest.getInteractionRate();

        // maxOnline：遍历整个 mergedList 取 onlineCount 最大值
        Integer maxOnline = mergedList.stream()
                .map(OceanEngineProcessBo::getOnlineCount)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(null);

        // 直接更新 request 对象 —— 原始字段
        request.setViewCount(viewCount);
        request.setSalesRevenue(salesRevenue);
        request.setRefund(refund);
        request.setInvestment(investment);
        request.setRefundQuantity(refundQuantity);
        request.setPayComboCnt(payComboCnt);
        request.setExposureCount(exposureCount);
        request.setFollowCount(followCount);
        request.setClickPaymentRate(clickPaymentRate);
        request.setInteractionRate(interactionRate);
        request.setMaxOnline(maxOnline);

        // 衍生指标计算
        request.setPerformanceCalculate(request);

        // 处理开始结束时间：从过程数据中获取最小和最大采集时间
        LocalDateTime minGatherTime = mergedList.stream()
                .map(OceanEngineProcessBo::getGatherDateTime)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(null);
        LocalDateTime maxGatherTime = mergedList.stream()
                .map(OceanEngineProcessBo::getGatherDateTime)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        if (minGatherTime != null && (request.getStartTime() == null || minGatherTime.isBefore(request.getStartTime()))) {
            request.setStartTime(minGatherTime);
        }
        if (maxGatherTime != null && (request.getEndTime() == null || maxGatherTime.isAfter(request.getEndTime()))) {
            request.setEndTime(maxGatherTime);
        }
    }

    /**
     * 比较两个BigDecimal是否相等
     */
    private boolean compareBigDecimal(BigDecimal a, BigDecimal b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.compareTo(b) == 0;
    }

    /**
     * save/update live_video
     */
    private void saveOrUpdateLiveVideo(LiveVideo existVideo,
                                       ClientPushVideoRequest request,
                                       String liveOssBizPath,
                                       LocalDateTime now) {

        if (existVideo == null) {
            // 新增
            LiveVideo newVideo = new LiveVideo();
            newVideo.setTenantId(request.getTenantId());
            newVideo.setVideoId(request.getBatchNumber());
            newVideo.setBatchNumber(request.getBatchNumber());
            newVideo.setHasPerformance(1);
            newVideo.setStartTime(request.getStartTime());
            newVideo.setEndTime(request.getEndTime());
            newVideo.setSecUid(request.getSecUid());
            newVideo.setPlatform(request.getPlatform());
            newVideo.setProcessStatus(ProcessStatus.PENDING.getCode());
            newVideo.setVideoOssUrl(liveOssBizPath);
            newVideo.setDataUpdateTime(now);
            newVideo.setViewCount(request.getViewCount());
            newVideo.setSalesRevenue(request.getSalesRevenue());
            newVideo.setRefund(request.getRefund());
            newVideo.setInvestment(request.getInvestment());
            newVideo.setRefundQuantity(request.getRefundQuantity());
            newVideo.setPayComboCnt(request.getPayComboCnt());
            newVideo.setNetSales(request.getNetSales());
            newVideo.setRefundRate(request.getRefundRate());
            newVideo.setRoi(request.getRoi());
            newVideo.setThousandSales(request.getThousandSales());
            newVideo.setExposureCount(request.getExposureCount());
            newVideo.setFollowCount(request.getFollowCount());
            newVideo.setClickPaymentRate(request.getClickPaymentRate());
            newVideo.setInteractionRate(request.getInteractionRate());
            newVideo.setMaxOnline(request.getMaxOnline());
            newVideo.setConversionRate(request.getConversionRate());
            newVideo.setUvValue(request.getUvValue());
            newVideo.setFollowRate(request.getFollowRate());
            newVideo.setCreateDate(now);
            newVideo.setUpdateDate(now);
            newVideo.setIsDeleted(0);
            this.save(newVideo);
            log.info("[业绩] 新增客户端推送视频, tenantId={}, videoId={}", request.getTenantId(), newVideo.getVideoId());
            return;
        }

        // 已存在：对比业绩指标是否变化
        boolean changed = !Objects.equals(existVideo.getViewCount(), request.getViewCount())
                || !compareBigDecimal(existVideo.getSalesRevenue(), request.getSalesRevenue())
                || !compareBigDecimal(existVideo.getRefund(), request.getRefund())
                || !compareBigDecimal(existVideo.getInvestment(), request.getInvestment());

        existVideo.setStartTime(request.getStartTime());
        existVideo.setEndTime(request.getEndTime());
        existVideo.setVideoOssUrl(liveOssBizPath);
        existVideo.setDataUpdateTime(now);

        if (changed) {
            existVideo.setViewCount(request.getViewCount());
            existVideo.setSalesRevenue(request.getSalesRevenue());
            existVideo.setRefund(request.getRefund());
            existVideo.setInvestment(request.getInvestment());
            existVideo.setRefundQuantity(request.getRefundQuantity());
            existVideo.setPayComboCnt(request.getPayComboCnt());
            existVideo.setNetSales(request.getNetSales());
            existVideo.setRefundRate(request.getRefundRate());
            existVideo.setRoi(request.getRoi());
            existVideo.setThousandSales(request.getThousandSales());
            existVideo.setExposureCount(request.getExposureCount());
            existVideo.setFollowCount(request.getFollowCount());
            existVideo.setClickPaymentRate(request.getClickPaymentRate());
            existVideo.setInteractionRate(request.getInteractionRate());
            existVideo.setMaxOnline(request.getMaxOnline());
            existVideo.setConversionRate(request.getConversionRate());
            existVideo.setUvValue(request.getUvValue());
            existVideo.setFollowRate(request.getFollowRate());
            existVideo.setProcessStatus(ProcessStatus.PENDING.getCode());
            log.info("[业绩] 客户端推送数据有变化，重置为待处理, tenantId={}, videoId={}",
                    request.getTenantId(), existVideo.getVideoId());
        } else {
            log.info("[业绩] 客户端推送数据无变化，仅更新过程数据和 data_update_time, tenantId={}, videoId={}",
                    request.getTenantId(), existVideo.getVideoId());
        }

        existVideo.setUpdateDate(now);
        this.updateById(existVideo);
    }

    /**
     * 处理商品列表（客户端推送场景）
     */
    private void processProductsForClientPush(ClientPushVideoRequest request,
                                              String videoId,
                                              Long tenantId,
                                              Integer platform,
                                              String secUid,
                                              String batchNumber,
                                              LocalDateTime now) {
        List<VideoProductRequest> productList = request.getProductList();
        if (EmptyUtil.isEmpty(productList)) {
            return;
        }

        // 批量查询已有商品，避免 N+1 查询
        List<String> productIds = productList.stream()
                .map(VideoProductRequest::getProductId)
                .collect(Collectors.toList());

        Map<String, VideoProduct> existProductMap = videoProductService.lambdaQuery()
                .eq(VideoProduct::getTenantId, tenantId)
                .eq(VideoProduct::getBatchNumber, batchNumber)
                .in(VideoProduct::getProductId, productIds)
                .list()
                .stream()
                .collect(Collectors.toMap(VideoProduct::getProductId, p -> p));

        // 并行处理每个商品：合并 + 去重 + 上传 OSS
        List<CompletableFuture<VideoProduct>> futures = productList.stream()
                .map(dto -> CompletableFuture.supplyAsync(() -> {
                    VideoProduct existProduct = existProductMap.get(dto.getProductId());

                    // 合并商品过程数据
                    String productOssBizPath = buildProductOssBizPath(tenantId, platform, secUid, batchNumber, dto.getProductId());

                    List<CommodityProcessDataBo> oldList = Collections.emptyList();
                    if (existProduct != null && EmptyUtil.isNotEmpty(existProduct.getProductOssKey())) {
                        oldList = processDataStorageService.downloadProcessDataFromOss(existProduct.getProductOssKey(), CommodityProcessDataBo.class);
                    }

                    List<CommodityProcessDataBo> merged = new ArrayList<>(oldList);
                    if (EmptyUtil.isNotEmpty(dto.getCommodityProcessDataList())) {
                        merged.addAll(dto.getCommodityProcessDataList());
                    }

                    List<CommodityProcessDataBo> deduped = dedupByMinute(merged, CommodityProcessDataBo::getDateTime);
                    String actualProductOssKey = uploadProcessDataToOss(productOssBizPath, deduped);

                    // 构建商品实体
                    VideoProduct product = BeanUtil.copyProperties(dto, VideoProduct.class);
                    product.setId(existProduct != null ? existProduct.getId() : null);
                    product.setBatchNumber(batchNumber);
                    product.setTenantId(tenantId);
                    product.setVideoId(videoId);
                    product.setProductOssKey(actualProductOssKey);
                    product.setUpdateDate(now);
                    if (product.getCreateDate() == null) {
                        product.setCreateDate(now);
                    }
                    if (product.getIsDeleted() == null) {
                        product.setIsDeleted(0);
                    }

                    // 处理商品封面图：仅新增时下载平台CDN图片并上传到OSS
                    if (existProduct == null && EmptyUtil.isNotEmpty(dto.getImageUri())) {
                        String ossImageUrl = uploadProductCoverImage(dto.getProductId(), dto.getImageUri(),
                                tenantId);
                        if (ossImageUrl != null) {
                            product.setImageUri(ossImageUrl);
                        }
                    } else if (existProduct != null) {
                        // 更新时保留DB中原有的封面图，不覆盖
                        product.setImageUri(existProduct.getImageUri());
                    }

                    return product;
                }, productUploadExecutor))
                .collect(Collectors.toList());

        // 等待所有并行任务完成
        List<VideoProduct> saveList = futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());

        videoProductService.saveOrUpdateBatch(saveList);
        log.info("[业绩] 批量保存商品过程数据, tenantId={}, videoId={}, count={}", tenantId, videoId, saveList.size());
    }

    /**
     * 下载平台CDN商品封面图并上传到OSS
     * <p>
     * 仅新增商品时调用。下载失败时返回 null，调用方保留原始 URL。
     * </p>
     *
     * @param productId   商品ID
     * @param imageUri    平台CDN图片URL
     * @param tenantId    租户ID
     * @return OSS公网URL，失败返回null
     */
    private String uploadProductCoverImage(String productId, String imageUri, Long tenantId) {
        try {
            URL url = new URL(imageUri);
            URLConnection conn = url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);
            String contentType = conn.getContentType();
            if (contentType == null || contentType.isEmpty()) {
                contentType = "image/jpeg";
            }
            try (InputStream in = conn.getInputStream()) {
                String bizPath = String.format("product/%s/%s.jpg", tenantId, productId);
                bizPath = imagesStorageService.upload(bizPath, in, contentType);
                return imagesStorageService.getPublicUrl(bizPath);
            }
        } catch (Exception e) {
            log.warn("[业绩] 商品封面图下载/上传失败, productId={}, imageUri={}", productId, imageUri, e);
            return null;
        }
    }

    /**
     * 构建直播过程数据 OSS bizPath
     */
    private String buildLiveOssBizPath(Long tenantId, Integer platform, String secUid, String batchNumber) {
        return String.format("%s/%s/%s/%s/live.json", tenantId, platform, secUid, batchNumber);
    }

    /**
     * 构建商品过程数据 OSS bizPath
     */
    private String buildProductOssBizPath(Long tenantId, Integer platform, String secUid, String batchNumber, String productId) {
        return String.format("%s/%s/%s/%s/%s.json", tenantId, platform, secUid, batchNumber, productId);
    }
}
