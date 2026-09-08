package com.jiuyu.replay.order.vo.datahub;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Data Hub 订单对账数据项（模块内传输对象）
 * 承载 tb_order 增量对账所需的核心事实字段
 */
@Data
@Schema(description = "Data Hub 订单对账数据项")
public class DataHubOrderItemVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 订单号
     */
    @Schema(description = "订单号")
    private Long id;

    /**
     * 下单用户ID
     */
    @Schema(description = "下单用户ID")
    private Long userId;

    /**
     * 归属租户ID（时点快照，0 表示无法归属）
     */
    @Schema(description = "归属租户ID")
    private Long tenantId;

    /**
     * 商品名称
     */
    @Schema(description = "商品名称")
    private String commodityName;

    /**
     * 订单标题
     */
    @Schema(description = "订单标题")
    private String title;

    /**
     * 状态 0未支付 1未开始 2生效中 3已过期 4已退款 5已结束 6超时未支付关闭 7冻结 8手动取消
     */
    @Schema(description = "状态")
    private Integer status;

    /**
     * 订单类型 0免费 1升级 2免费版换收费版 3续费 4增量包 5版本活动 6编辑 7商品活动
     */
    @Schema(description = "订单类型")
    private Integer orderType;

    /**
     * 商品类型 0增量包 1正常版本 2活动版本 3邀请活动
     */
    @Schema(description = "商品类型")
    private Integer commodityType;

    /**
     * 来源 0正常下单 1手动添加 2邀请用户成功 3邀请码赠送
     */
    @Schema(description = "来源")
    private Integer source;

    /**
     * 是否试用订单 0否 1是
     */
    @Schema(description = "是否试用订单")
    private Integer trialOrder;

    /**
     * 订单总价（实付金额，分）
     */
    @Schema(description = "订单总价（分）")
    private Integer totalPrice;

    /**
     * 支付时间
     */
    @Schema(description = "支付时间")
    private Date payDate;

    /**
     * 服务生效时间
     */
    @Schema(description = "服务生效时间")
    private Date startDate;

    /**
     * 服务到期时间
     */
    @Schema(description = "服务到期时间")
    private Date endDate;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private Date createDate;

    /**
     * 最后更新时间
     */
    @Schema(description = "最后更新时间")
    private Date updateDate;
}
