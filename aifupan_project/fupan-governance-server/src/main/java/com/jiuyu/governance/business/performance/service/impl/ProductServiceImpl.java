package com.jiuyu.governance.business.performance.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.governance.business.performance.mapper.ProductMapper;
import com.jiuyu.governance.business.performance.pojo.entity.Product;
import com.jiuyu.governance.business.performance.service.ProductService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商品服务实现类
 *
 * @author lj
 * @date 2026-03-24
 */
@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {


    /**
     * 按商品主键ID（product.id）批量取商品映射
     *
     * @param productIds 商品主键ID集合
     *
     * @return id -> 商品
     */
    @Override
    public Map<Long, Product> getProductMap(List<Long> productIds) {
        if (EmptyUtil.isEmpty(productIds)) {
            return Map.of();
        }
        return lambdaQuery()
            .select(Product::getId, Product::getName, Product::getImageUri)
            .in(Product::getId, productIds)
            .list()
            .stream()
            .filter(p -> p.getId() != null && p.getName() != null && p.getImageUri() != null)
            .collect(Collectors.toMap(Product::getId, p -> p));
    }

    @Override
    public Map<Long, String> getProductNameMap(List<Long> productIds) {
        if (EmptyUtil.isEmpty(productIds)) {
            return Map.of();
        }
        // @TableLogic 自动追加 is_deleted 过滤
        return lambdaQuery()
            .select(Product::getId, Product::getName)
            .in(Product::getId, productIds)
            .list()
            .stream()
            .filter(p -> p.getId() != null && p.getName() != null)
            .collect(Collectors.toMap(Product::getId, Product::getName, (a, b) -> a));
    }

    @Override
    public Map<Long, String> getProductImageMap(List<Long> productIds) {
        if (EmptyUtil.isEmpty(productIds)) {
            return Map.of();
        }
        return lambdaQuery()
            .select(Product::getId, Product::getImageUri)
            .in(Product::getId, productIds)
            .list()
            .stream()
            .filter(p -> p.getId() != null && p.getImageUri() != null)
            .collect(Collectors.toMap(Product::getId, Product::getImageUri, (a, b) -> a));
    }

    @Override
    public List<Product> searchByTenantAndName(Long tenantId, List<String> productIds, String name, int limit) {
        if (tenantId == null || limit <= 0) {
            return List.of();
        }
        return lambdaQuery()
            .select(Product::getId, Product::getProductId, Product::getName, Product::getImageUri, Product::getUpdateDate)
            .eq(Product::getTenantId, tenantId)
            .in(EmptyUtil.isNotEmpty(productIds), Product::getProductId, productIds)
            .like(EmptyUtil.isNotEmpty(name), Product::getName, name)
            .orderByDesc(Product::getId)
            // 只要前 limit 条，关闭 count 查询
            .page(new Page<>(1, limit, false))
            .getRecords();
    }
}
