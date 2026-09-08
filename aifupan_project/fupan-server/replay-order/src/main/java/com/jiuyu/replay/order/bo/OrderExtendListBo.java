package com.jiuyu.replay.order.bo;


import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import com.jiuyu.replay.common.bo.PageBo;

/**
 * 订单的扩展表列表查询参数
 *
 * @author lyw
 * @email 1798883178@qq.com
 * @date 2025-07-05 09:56:24
 */
@Data
@Schema(description = "订单的扩展表列表查询参数")
public class OrderExtendListBo extends PageBo implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 模糊搜索查询条件
	 */
	@Schema(description = "模糊搜索查询条件")
	private String keyword;

	/**
	 * 根据订单id查询
	 */
	@Schema(description = "根据订单id查询")
	private Long orderId;

}
