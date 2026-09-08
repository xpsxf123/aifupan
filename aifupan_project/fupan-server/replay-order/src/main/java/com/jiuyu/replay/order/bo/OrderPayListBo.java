package com.jiuyu.replay.order.bo;


import com.jiuyu.replay.common.bo.PageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 订单-支付信息列表查询参数
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-10 11:42:33
 */
@Data
@Schema(description = "订单-支付信息列表查询参数")
public class OrderPayListBo extends PageBo implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(description = "支付状态 0：未支付 1：已支付 2：支付失败 3：已退款 4：取消支付")
	private Integer payStatus;

}
