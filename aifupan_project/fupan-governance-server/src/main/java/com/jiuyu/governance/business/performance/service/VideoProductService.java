package com.jiuyu.governance.business.performance.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jiuyu.governance.business.performance.pojo.bo.CommodityProcessDataBo;
import com.jiuyu.governance.business.performance.pojo.entity.VideoProduct;
import com.jiuyu.governance.business.performance.pojo.request.VideoProductPageRequest;
import com.jiuyu.governance.business.performance.pojo.response.client.VideoProductPageResponse;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 视频商品服务接口
 *
 * @author lj
 * @date 2026-03-19
 */
public interface VideoProductService extends IService<VideoProduct> {

    /**
     * 按直播批次号分页查询视频商品
     *
     * @param request 分页查询请求（含 batchNumber、title、分页参数、排序字段）
     * @return 分页结果
     */
    VideoProductPageResponse pageByBatchNumber(VideoProductPageRequest request);

    /**
     * 按租户 + 直播批次号集合批量查询视频商品
     *
     * @param tenantId     租户ID
     * @param batchNumbers 直播批次号集合
     * @return {@link List }<{@link VideoProduct }> 命中的商品行，入参为空时返回空集合
     */
    List<VideoProduct> listByTenantAndBatchNumbers(Long tenantId, Collection<String> batchNumbers);

    /**
     * 按租户 + 直播批次号集合筛出「存在商品数据」的批次号
     *
     * @param tenantId     租户ID
     * @param batchNumbers 直播批次号集合
     * @return {@link Set }<{@link String }> 存在商品的批次号集合，入参为空时返回空集合
     */
    Set<String> listExistBatchNumbers(Long tenantId, Collection<String> batchNumbers);

    /**
     * 按租户 + 商品ID集合 + 创建时间范围，取每个商品出现过的直播批次号
     *
     * @param tenantId   租户ID
     * @param productIds 商品ID集合（video_product.product_id）
     * @param startTime  创建时间起（含）
     * @param endTime    创建时间止（含）
     *
     * @return {@link Map }<{@link String }, {@link List }<{@link String }>> productId -> 去重后的批次号集合
     */
    Map<String, List<String>> getProductBatchNumberMap(Long tenantId, Collection<String> productIds,
                                                       LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 批量取商品过程数据，并按步长（分钟）降采样
     * <p>
     * OSS 里是分钟级累计快照（同一分钟可能有最早/最晚两条）。按 step 分钟分窗，
     * 窗口起点对齐到该商品第一条数据所在的分钟，每个窗口只保留窗口内最后一条快照，
     * 值仍是累计口径。无 {@code productOssKey} 或下载失败的商品不进入结果 Map。
     * </p>
     *
     * @param products 商品行集合
     * @param step     步长（分钟），小于1按1处理
     *
     * @return {@link Map }<{@link Long }, {@link List }<{@link CommodityProcessDataBo }>> video_product.id -> 降采样后的累计快照序列（时间升序）
     */
    Map<Long, List<CommodityProcessDataBo>> getProcessDataMap(Collection<VideoProduct> products, int step);
}
