package com.jiuyu.replay.generic.vo.order;


import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;

/**
 * 订单-支付信息信息项
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-10 11:42:33
 */
@Data
@Schema(description = "订单-支付信息信息项")
public class OrderPayInfoVo extends OrderPayVo implements Serializable {
	private static final long serialVersionUID = 1L;

	@Schema(description = "支付二维码")
	private String urlCode;

	@Schema(description = "商品名称")
	private String title;

}
