package com.jiuyu.governance.business.performance.pojo.response.client;

import com.jiuyu.framework.shandard.PageData;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.math.BigDecimal;
import java.util.List;

/**
 * 视频商品分页查询响应
 *
 * @author AI Assistant
 * @date 2026-07-28
 */
@Getter
@Setter
public class VideoProductPageResponse extends PageData<VideoProductPageResponse.ProductItem> {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 是否已拉取商品数据 */
    private Boolean pullStatus;

    public VideoProductPageResponse(List<ProductItem> records, long total, int pageSize, int pageNum, long pages, Boolean pullStatus) {
        super(records, total, pageSize, pageNum, pages);
        this.pullStatus = pullStatus;
    }

    @Getter
    @Setter
    public static class ProductItem implements java.io.Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /** 商品标题 */
        private String title;

        /** 商品封面图 */
        private String imageUri;

        /** 曝光人数 */
        private Long productShowUcnt;

        /** 曝光观看率 */
        private BigDecimal productViewShowRatio;

        /** 点击人数 */
        private Long productClickUcnt;

        /** 曝光点击率 */
        private BigDecimal productShowClickUcntRatio;

        /** 点击成交转化率 */
        private BigDecimal productClickPayUcntRatio;

        /** 成交单价 */
        private BigDecimal avgPayAmtPerOrder;

        /** 订单量 */
        private Long payCnt;

        /** 成交金额 */
        private BigDecimal payAmt;

        /** 讲解次数 */
        private Integer explainCnt;

        /** 排序 */
        private Integer sort;
    }
}
