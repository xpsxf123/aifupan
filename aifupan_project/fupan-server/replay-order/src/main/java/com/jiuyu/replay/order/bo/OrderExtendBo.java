package com.jiuyu.replay.order.bo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 订单的扩展表信息
 *
 * @author lyw
 * @email 1798883178@qq.com
 * @date 2025-07-05 09:56:24
 */
@Data
@Schema(description = "订单的扩展表信息")
public class OrderExtendBo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Schema(description = "ID")
	private Long id;
	/**
	 * 订单id
	 */
	@Schema(description = "订单id")
	private Long orderId;
	/**
	 * 付费截图，最多25张图片，图片id之间使用逗号隔开
	 */
	@Schema(description = "付费截图，最多25张图片，图片id之间使用逗号隔开")
	private String payPictures;
	/**
	 * 备注
	 */
	@Schema(description = "备注")
	private String remarks;
	/**
	 * 最后修改时间
	 */
	@Schema(description = "最后修改时间")
	private Date updateDate;
	/**
	 * 创建时间
	 */
	@Schema(description = "创建时间")
	private Date createDate;
	/**
	 * 是否已删除
	 */
	@Schema(description = "是否已删除")
	private Integer isDeleted;


}
