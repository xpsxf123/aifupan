package com.jiuyu.replay.order.bo;


import com.jiuyu.replay.common.bo.PageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 列表查询参数
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-10 11:42:33
 */
@Data
@Schema(description = "列表查询参数")
public class OrderDetailListBo extends PageBo implements Serializable {

	private static final long serialVersionUID = 1L;


	@Schema(description = "订单id")
	private Long orderId;

}
