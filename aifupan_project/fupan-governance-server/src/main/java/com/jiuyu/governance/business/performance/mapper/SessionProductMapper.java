package com.jiuyu.governance.business.performance.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jiuyu.governance.business.performance.pojo.bo.ProductCompanyBO;
import com.jiuyu.governance.business.performance.pojo.bo.ProductRankingBO;
import com.jiuyu.governance.business.performance.pojo.bo.ProductSessionBO;
import com.jiuyu.governance.business.performance.pojo.entity.SessionProduct;
import com.jiuyu.governance.business.performance.pojo.request.ProductRankingRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 场次商品关联表Mapper
 *
 * @author lj
 * @date 2026-03-24
 */
@Mapper
public interface SessionProductMapper extends BaseMapper<SessionProduct> {

    /**
     * 商品排行聚合查询（分页）
     *
     * @param page      分页对象
     * @param tenantId  租户ID
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @param companyId 分公司ID（可选）
     * @param deptId    部门ID（可选）
     * @param teamId    小组ID（可选）
     * @param sortBy    排序字段
     * @param sortOrder 排序方向
     * @return 商品排行分页结果
     */
    IPage<ProductRankingBO> queryProductRanking(
            Page<ProductRankingBO> page,
            @Param("req") ProductRankingRequest req,
            @Param("sortBy") String sortBy,
            @Param("sortOrder") String sortOrder
    );

    /**
     * 商品关联直播场次分页查询
     *
     * @param page      分页对象
     * @param tenantId  租户ID
     * @param productId 商品ID
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @param sortBy    排序字段
     * @param sortOrder 排序方向
     * @return 直播场次分页结果
     */
    IPage<ProductSessionBO> queryProductSessions(
            Page<ProductSessionBO> page,
            @Param("tenantId") Long tenantId,
            @Param("productId") Long productId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("sortBy") String sortBy,
            @Param("sortOrder") String sortOrder
    );

    /**
     * 商品关联分公司列表查询
     *
     * @param tenantId  租户ID
     * @param productId 商品ID
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @param sortBy    排序字段
     * @param sortOrder 排序方向
     * @return 分公司列表
     */
    List<ProductCompanyBO> queryProductCompanies(
            @Param("tenantId") Long tenantId,
            @Param("productId") Long productId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("sortBy") String sortBy,
            @Param("sortOrder") String sortOrder
    );
}
