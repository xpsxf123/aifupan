package com.jiuyu.governance.business.performance.pojo.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 开放接口 - 租户下商品搜索响应
 * <p>
 * 每个商品一行，附带该商品在指定时间范围内出现过的直播批次号集合。
 * </p>
 *
 * @author HeHui
 * @date 2026-08-06
 */
@Getter
@Setter
public class OpenProductSearchResponse {

    /**
     * 商品主键ID（product.id）
     */
    private Long id;

    /**
     * 商品ID（平台侧商品ID，product.product_id）
     */
    private String productId;

    /**
     * 商品名称
     */
    private String name;

    /**
     * 商品图片URL
     */
    private String imageUri;

    /**
     * 更新时间
     */
    private LocalDateTime updateDate;

    /**
     * 该商品在时间范围内出现过的直播批次号集合（去重，无命中时为空集合）
     */
    private List<String> batchNumbers;
}
