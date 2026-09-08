package com.jiuyu.replay.generic.vo.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 订单列表项
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-10 11:42:33
 */
@Data
@Schema(description = "订单列表项")
public class OrderListVo extends OrderVo implements Serializable {
	private static final long serialVersionUID = 1L;

	@Schema(description = "佣金比例")
	private Double commission;

	@Schema(description = "佣金，单位：分")
	private Integer commissionMoney;

	@Schema(description = "分佣类型 0：新签佣金 1：续费佣金")
	private Integer commissionType;

}
