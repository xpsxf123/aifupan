package com.jiuyu.governance.business.performance.service;

import com.jiuyu.governance.business.performance.pojo.entity.Product;

import java.util.List;
import java.util.Map;

/**
 * 商品服务接口
 *
 * @author lj
 * @date 2026-03-24
 */
public interface ProductService {



    /**
     * 按商品主键ID（product.id）批量取商品映射
     *
     * @param productIds 商品主键ID集合
     *
     * @return id -> 商品
     */
    Map<Long, Product> getProductMap(List<Long> productIds);

    /**
     * 按商品主键ID（product.id）批量取「商品名称」映射
     *
     * @param productIds 商品主键ID集合
     *
     * @return id -> 商品名称
     */
    Map<Long, String> getProductNameMap(List<Long> productIds);

    /**
     * 按商品主键ID（product.id）批量取「商品图片URL」映射
     *
     * @param productIds 商品主键ID集合
     *
     * @return id -> 商品图片URL
     */
    Map<Long, String> getProductImageMap(List<Long> productIds);

    /**
     * 按租户搜索商品列表，可按名称模糊匹配，按主键ID倒序，最多返回 limit 条
     *
     * @param tenantId   租户ID
     * @param productIds
     * @param name       商品名称关键字，为空则不过滤
     * @param limit      返回条数上限
     *
     * @return {@link List }<{@link Product }> 商品列表，按 id 倒序
     */
    List<Product> searchByTenantAndName(Long tenantId, List<String> productIds, String name, int limit);
}
