package com.jiuyu.replay.generic.vo.order;

import java.io.Serializable;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 订单-支付信息列表项
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-10 11:42:33
 */
@Data
@Schema(description = "订单-支付信息列表项")
public class OrderPayListVo extends OrderPayVo implements Serializable {
	private static final long serialVersionUID = 1L;



}
