package com.jiuyu.governance.business.performance.service;

import com.jiuyu.framework.oauth.AccessUser;
import com.jiuyu.framework.shandard.PageData;
import com.jiuyu.governance.business.performance.pojo.request.ProductCompanyRequest;
import com.jiuyu.governance.business.performance.pojo.request.ProductRankingRequest;
import com.jiuyu.governance.business.performance.pojo.request.ProductSessionRequest;
import com.jiuyu.governance.business.performance.pojo.response.ProductCompanyResponse;
import com.jiuyu.governance.business.performance.pojo.response.ProductRankingResponse;
import com.jiuyu.governance.business.performance.pojo.response.ProductSessionResponse;

import java.util.List;

/**
 * 商品排行服务接口
 *
 * @author lj
 * @date 2026-03-27
 */
public interface ProductRankingService {

    /**
     * 商品排行分页查询
     * <p>
     * 按商品维度统计销售数据，支持时间范围筛选、组织筛选和排序
     * 租户间数据隔离
     * </p>
     *
     * @param request 查询请求
     * @return 分页数据，包含商品销售统计和关联场次数、子公司数
     */
    PageData<ProductRankingResponse> pageQueryProductRanking(ProductRankingRequest request, AccessUser accessUser);

    /**
     * 商品关联直播场次分页查询
     * <p>
     * 查询指定商品在时间范围内关联的直播场次列表，用于弹窗展示
     * 返回主播信息、场次信息、商品销售数据
     * </p>
     *
     * @param request 查询请求
     * @return 分页数据，包含场次详情
     */
    PageData<ProductSessionResponse> pageQueryProductSessions(ProductSessionRequest request);

    /**
     * 商品关联分公司列表查询
     * <p>
     * 查询指定商品在时间范围内关联的分公司列表及其汇总数据，用于弹窗展示
     * 支持按销量、销售额、场观排序
     * </p>
     *
     * @param request 查询请求
     * @return 分公司列表
     */
    List<ProductCompanyResponse> queryProductCompanies(ProductCompanyRequest request);
}
