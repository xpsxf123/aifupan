package com.jiuyu.governance.business.performance.controller;

import cn.hutool.core.bean.BeanUtil;
import com.jiuyu.framework.oauth.client.annotation.APIKey;
import com.jiuyu.framework.shandard.ApiResponse;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.framework.function.complete.Complete;
import com.jiuyu.governance.business.performance.pojo.bo.CommodityProcessDataBo;
import com.jiuyu.governance.business.performance.pojo.entity.Product;
import com.jiuyu.governance.business.performance.pojo.entity.SessionProduct;
import com.jiuyu.governance.business.performance.pojo.entity.VideoProduct;
import com.jiuyu.governance.business.performance.pojo.request.OpenBatchProductExistsRequest;
import com.jiuyu.governance.business.performance.pojo.request.OpenBatchProductExistsRequest.PlatformBatchItem;
import com.jiuyu.governance.business.performance.pojo.request.OpenProductSearchRequest;
import com.jiuyu.governance.business.performance.pojo.request.OpenSessionProductQueryRequest;
import com.jiuyu.governance.business.performance.pojo.request.OpenVideoProductQueryRequest;
import com.jiuyu.governance.business.performance.pojo.response.OpenBatchProductExistsResponse;
import com.jiuyu.governance.business.performance.pojo.response.OpenProductSearchResponse;
import com.jiuyu.governance.business.performance.pojo.response.OpenSessionProductResponse;
import com.jiuyu.governance.business.performance.pojo.response.OpenSessionProductResponse.ProductMetric;
import com.jiuyu.governance.business.performance.pojo.response.OpenVideoProductResponse;
import com.jiuyu.governance.business.performance.pojo.response.OpenVideoProductResponse.VideoProductMetric;
import com.jiuyu.governance.business.performance.service.ProductService;
import com.jiuyu.governance.business.performance.service.SessionProductService;
import com.jiuyu.governance.business.performance.service.VideoProductService;
import com.jiuyu.governance.common.pojo.BizErrorCode;
import com.jiuyu.governance.openfeign.replay.AnchorVideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 开放接口 - 场次商品（含指标）查询
 * <p>
 * 供外部应用（非同一应用、不同 IP）通过 {@code @APIKey} 调用。
 * 先按场次ID集合从 {@code session_product} 取指标行，再用 {@link Complete} 经 productId
 * 反查 {@code product} 回填商品名称与图片URL，最后按场次ID聚合输出。
 * </p>
 *
 * @author HeHui
 * @date 2026-06-22
 */
@Slf4j
@RestController
@RequestMapping("/api/governance/session-product-open")
@RequiredArgsConstructor
@APIKey(clientName = "x-jiuyu-client-id")
public class OpenSessionProductController {

    /**
     * 商品搜索默认返回条数
     */
    private static final int DEFAULT_PRODUCT_LIMIT = 100;

    /**
     * 过程数据默认步长（分钟）
     */
    private static final int DEFAULT_PROCESS_STEP = 1;

    private final SessionProductService sessionProductService;
    private final ProductService productService;
    private final VideoProductService videoProductService;
    private final AnchorVideoService anchorVideoService;

    /**
     * 按场次ID集合批量查询场次商品指标（按场次ID聚合）
     *
     * @param request 查询请求（含身份字段 + 场次ID集合）
     *
     * @return 每个场次ID对应的商品指标集合
     */
    @PostMapping("/batch-query")
    public ApiResponse<List<OpenSessionProductResponse>> batchQuery(@RequestBody @Validated OpenSessionProductQueryRequest request) {
        log.info("[场次商品开放接口 - 批量查询] tenantId={}, userId={}, userType={}, sessionIds={}",
            request.getTenantId(), request.getUserId(), request.getUserType(),
            EmptyUtil.isEmpty(request.getSessionIds()) ? 0 : request.getSessionIds().size());

        // 1. 先用 SessionProduct 按租户 + 场次ID集合查指标行
        List<SessionProduct> rows = sessionProductService.listByTenantAndSessionIds(request.getTenantId(), request.getSessionIds());
        if (EmptyUtil.isEmpty(rows)) {
            return ApiResponse.success(List.of());
        }

        // 2. 转指标对象，并按场次ID聚合（保持场次首次出现顺序）。
        //    同一批 ProductMetric 既挂在分组结构里、又收进 flat 列表，供 Complete 原地回填。
        Map<Long, OpenSessionProductResponse> bySession = new LinkedHashMap<>();
        List<ProductMetric> flat = new ArrayList<>();
        for (SessionProduct sp : rows) {
            ProductMetric metric = toMetric(sp);
            OpenSessionProductResponse group = bySession.computeIfAbsent(sp.getSessionId(), sid -> {
                OpenSessionProductResponse r = new OpenSessionProductResponse();
                r.setSessionId(sid);
                r.setProducts(new ArrayList<>());
                return r;
            });
            group.getProducts().add(metric);
            flat.add(metric);
        }

        // 3. 经 productId 反查 product，回填商品名称与图片URL（Complete 原地写回 flat 中的对象，
        //    分组结构里的同一引用同步生效）。
        Complete.start(flat)
            .build(ProductMetric::getProductId, (row, data) -> {
                row.setProductName(data.getName());
                row.setProductImageUrl(data.getImageUri());
            }, productService::getProductMap)
            .then()
            .over();

        return ApiResponse.success(new ArrayList<>(bySession.values()));
    }

    /**
     * 按视频ID集合批量查询视频商品指标（按视频ID聚合）
     * <p>
     * {@code video_product.video_id} 已废弃、恒为空，商品实际按 batchNumber 归属。
     * 因此先经爱复盘 simple 查询把 videoId 换成 batchNumber，再按批次号查商品。
     * 每个商品额外附带 OSS 过程数据（分钟级累计快照），按入参 step 分钟降采样。
     * </p>
     *
     * @param request 查询请求（含身份字段 + 视频ID集合 + 过程数据步长）
     *
     * @return 每个视频ID对应的商品指标集合（无批次号或批次下无商品的视频ID不返回）
     */
    @PostMapping("/batch-query-video-product")
    public ApiResponse<List<OpenVideoProductResponse>> batchQueryVideoProduct(@RequestBody @Validated OpenVideoProductQueryRequest request) {
        log.info("[视频商品开放接口 - 批量查询] tenantId={}, userId={}, userType={}, videoIds={}, step={}",
            request.getTenantId(), request.getUserId(), request.getUserType(), request.getVideoIds().size(), request.getStep());

        // 1. videoId -> batchNumber（爱复盘 simple 查询，只返回视频本身信息）
        Map<String, String> batchNumberMap = anchorVideoService.getVideoBatchNumberMap(request.getVideoIds(), request.getUserId(), request.getUserType(),request.getTenantId());
        if (EmptyUtil.isEmpty(batchNumberMap)) {
            log.error("[视频商品开放接口 - 批量查询] tenantId={}, userId={}, userType={}， videoIds: {}, 查询视频批次号为空", request.getTenantId(), request.getUserId(), request.getUserType(), request.getVideoIds());
            return ApiResponse.success(List.of());
        }

        // 2. 按租户 + 批次号集合查商品（多个视频可能同属一个批次，先去重）
        List<VideoProduct> rows = videoProductService.listByTenantAndBatchNumbers(
            request.getTenantId(), new LinkedHashSet<>(batchNumberMap.values()));
        if (EmptyUtil.isEmpty(rows)) {
            return ApiResponse.success(List.of());
        }

        // 3. 拉取各商品的过程数据（OSS，带Redis缓存），按步长降采样
        int step = request.getStep() != null ? request.getStep() : DEFAULT_PROCESS_STEP;
        Map<Long, List<CommodityProcessDataBo>> processDataMap = videoProductService.getProcessDataMap(rows, step);

        // 4. 商品按批次号分组后，回挂到各自的视频ID上（保持入参视频ID顺序）
        Map<String, List<VideoProductMetric>> byBatchNumber = rows.stream()
            .collect(Collectors.groupingBy(VideoProduct::getBatchNumber,
                Collectors.mapping(product -> toVideoMetric(product, processDataMap), Collectors.toList())));

        List<OpenVideoProductResponse> responses = new ArrayList<>();
        for (String videoId : request.getVideoIds()) {
            String batchNumber = batchNumberMap.get(videoId);
            if (EmptyUtil.isEmpty(batchNumber)) {
                continue;
            }
            List<VideoProductMetric> products = byBatchNumber.get(batchNumber);
            if (EmptyUtil.isEmpty(products)) {
                continue;
            }
            OpenVideoProductResponse response = new OpenVideoProductResponse();
            response.setVideoId(videoId);
            response.setBatchNumber(batchNumber);
            response.setProducts(products);
            responses.add(response);
        }

        return ApiResponse.success(responses);
    }

    /**
     * 按「平台 + 直播批次号集合」批量查询批次下是否存在商品数据（支持多平台）
     * <p>
     * {@code video_product} 按 tenantId + batchNumber 归属，批次号本身已全局唯一，
     * 平台仅作为调用方的分组维度原样回显。
     * </p>
     *
     * @param request 查询请求（含身份字段 + 平台分组集合）
     *
     * @return 每一对「平台 + 批次号」的商品存在性，顺序与入参一致
     */
    @PostMapping("/batch-query-product-exists")
    public ApiResponse<List<OpenBatchProductExistsResponse>> batchQueryProductExists(@RequestBody @Validated OpenBatchProductExistsRequest request) {
        log.info("[批次商品存在性开放接口 - 批量查询] tenantId={}, userId={}, userType={}, items={}",
            request.getTenantId(), request.getUserId(), request.getUserType(), request.getItems().size());

        // 1. 汇总所有平台下的批次号，一次查库
        Set<String> allBatchNumbers = request.getItems().stream()
            .flatMap(item -> item.getBatchNumbers().stream())
            .filter(batchNumber -> EmptyUtil.isNotEmpty(batchNumber))
            .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<String> existBatchNumbers = videoProductService.listExistBatchNumbers(request.getTenantId(), allBatchNumbers);

        // 2. 按入参顺序展开成「平台 + 批次号 + 是否存在」
        List<OpenBatchProductExistsResponse> responses = new ArrayList<>();
        for (PlatformBatchItem item : request.getItems()) {
            for (String batchNumber : item.getBatchNumbers()) {
                responses.add(new OpenBatchProductExistsResponse(
                    item.getPlatform(), batchNumber, existBatchNumbers.contains(batchNumber)));
            }
        }

        return ApiResponse.success(responses);
    }

    /**
     * 搜索租户下的商品列表，并回填每个商品出现过的直播批次号
     * <p>
     * 商品按 {@code product.id} 倒序，最多返回 limit 条（默认100，最大3000）。
     * 批次号取自 {@code video_product}，按 {@code create_date} 落在时间范围内的行统计；
     * 时间范围默认近一年，显式传入时跨度不得超过一年。
     * </p>
     *
     * @param request 查询请求（含身份字段 + 名称关键字 + 条数 + 时间范围）
     *
     * @return 商品列表（含批次号集合）
     */
    @PostMapping("/search-product")
    public ApiResponse<List<OpenProductSearchResponse>> searchProduct(@RequestBody @Validated OpenProductSearchRequest request) {
        log.info("[商品搜索开放接口] tenantId={}, userId={}, userType={}, name={}, limit={}, startTime={}, endTime={}",
            request.getTenantId(), request.getUserId(), request.getUserType(), request.getName(),
            request.getLimit(), request.getStartTime(), request.getEndTime());

        // 1. 归一化批次号的时间范围：默认近一年，跨度不得超过一年
        LocalDateTime endTime = request.getEndTime() != null ? request.getEndTime() : LocalDateTime.now();
        LocalDateTime startTime = request.getStartTime() != null ? request.getStartTime() : endTime.minusYears(1);
        if (startTime.isAfter(endTime)) {
            return ApiResponse.failed(BizErrorCode.PARAM_INVALID.getCode(), "开始时间不能大于结束时间");
        }
        if (startTime.isBefore(endTime.minusYears(1))) {
            return ApiResponse.failed(BizErrorCode.PARAM_INVALID.getCode(), "时间范围不能超过一年");
        }

        // 2. 按租户 + 名称搜索商品（id 倒序，取前 limit 条）
        int limit = request.getLimit() != null ? request.getLimit() : DEFAULT_PRODUCT_LIMIT;
        List<Product> products = productService.searchByTenantAndName(request.getTenantId(), request.getProductIds(), request.getName(), limit);
        if (EmptyUtil.isEmpty(products)) {
            return ApiResponse.success(List.of());
        }

        // 3. 用商品ID反查 video_product，取时间范围内的批次号
        Set<String> productIds = products.stream()
            .map(Product::getProductId)
            .filter(productId -> EmptyUtil.isNotEmpty(productId))
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, List<String>> batchNumberMap =
            videoProductService.getProductBatchNumberMap(request.getTenantId(), productIds, startTime, endTime);

        // 4. 组装出参（保持商品的 id 倒序）
        List<OpenProductSearchResponse> responses = new ArrayList<>(products.size());
        for (Product product : products) {
            OpenProductSearchResponse response = new OpenProductSearchResponse();
            response.setId(product.getId());
            response.setProductId(product.getProductId());
            response.setName(product.getName());
            response.setImageUri(product.getImageUri());
            response.setUpdateDate(product.getUpdateDate());
            response.setBatchNumbers(batchNumberMap.getOrDefault(product.getProductId(), List.of()));
            responses.add(response);
        }

        return ApiResponse.success(responses);
    }

    /**
     * VideoProduct -> VideoProductMetric（同名字段直接搬运，剔除主键/租户/OSS路径/审计字段），
     * 并挂上该商品降采样后的过程数据序列。
     */
    private VideoProductMetric toVideoMetric(VideoProduct product, Map<Long, List<CommodityProcessDataBo>> processDataMap) {
        VideoProductMetric metric = BeanUtil.copyProperties(product, VideoProductMetric.class);
        metric.setProcessList(processDataMap.getOrDefault(product.getId(), List.of()));
        return metric;
    }

    /**
     * SessionProduct -> ProductMetric（仅搬运指标，商品名称/图片留待 Complete 回填）。
     */
    private ProductMetric toMetric(SessionProduct sp) {
        ProductMetric metric = new ProductMetric();
        metric.setSessionId(sp.getSessionId());
        metric.setProductId(sp.getProductId());
        metric.setQuantity(sp.getQuantity());
        metric.setPrice(sp.getPrice());
        metric.setSalesAmount(sp.getSalesAmount());
        metric.setExposureClickRate(sp.getExposureClickRate());
        metric.setExposureConversionRate(sp.getExposureConversionRate());
        metric.setGpm(sp.getGpm());
        metric.setRefundQuantity(sp.getRefundQuantity());
        metric.setRefundAmount(sp.getRefundAmount());
        metric.setRefundRate(sp.getRefundRate());
        metric.setClickPaymentRate(sp.getClickPaymentRate());
        return metric;
    }
}
