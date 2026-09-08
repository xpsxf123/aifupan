package com.jiuyu.replay.order.bo;

import com.jiuyu.replay.generic.dto.activity.UserInviteMqDto;
import com.jiuyu.replay.order.vo.CommodityPriceInfoVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 创建订单bo
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-10 11:42:33
 */
@Data
@Schema(description = "创建订单bo")
public class CreateOrderBo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "商品id(购买的是套餐：packageId，购买的是增量包：incrementId)")
    private Long commodityId;

    @Schema(description = "订单标题")
    private String title;

    @Schema(description = "商品价格id")
    private Long commodityPriceId;

    @Schema(description = "商品价格-如果版本下单时没有商品价额id，就用单前的价格")
    private CommodityPriceInfoVo priceVo;

    @Schema(description = "活动商品信息-邀请活动订单独有参数")
    private ActivityCommodityBo activityCommodityBo;

    @Schema(description = "用户id")
    private Long userId;

    @Schema(description = "用户名称")
    private String userName;

    @Schema(description = "订单类型 0免费订单，1升级订单，2免费版换收费版，3订单续费，4增量包订单，5版本活动订单，6编辑订单，7商品活动订单")
    private Integer orderType;

    @Schema(description = "商品类型 0增量包，1正常版本(月底资源重置)，2活动版本(月底资源不重置)，3邀请活动订单")
    private Integer commodityType;

    @Schema(description = "优惠价格（主要用于套餐升级产生的差价，和折扣没有关系）(单个商品)")
    private Integer discountRate;

    @Schema(description = "来源 0正常下单，1手动添加，2邀请码赠送，3活动赠送")
    private Integer source;

    @Schema(description = "升级前的订单id(用于套餐升级)")
    private Long beforeUpgrading;

    @Schema(description = "支付方式 0微信，1支付宝")
    private Integer payType;

    @Schema(description = "是否是试用订单 0否，1是")
    private Integer trialOrder;

    @Schema(description = "支付凭证图片, 多个图片用逗号分开")
    private String payPictures;

    /**
     * 用户邀请相关实体 在注册首次下单后处理该逻辑
     */
    private UserInviteMqDto userInviteMqDto;
}
