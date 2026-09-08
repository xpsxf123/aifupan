package com.jiuyu.replay.order.bo;


import com.jiuyu.replay.common.bo.PageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 增量包表列表查询参数
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-09 16:13:50
 */
@Data
@Schema(description = "增量包表列表查询参数")
public class IncrementListBo extends PageBo implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(description = "商品id")
	private Long commodityId;

	@Schema(description = "套餐id")
	private Long packageId;

	@Schema(description = "商品价格id")
	private Long commodityPriceId;

	@Schema(description = "状态 0未上架，1已上架")
	private Integer status;
}
