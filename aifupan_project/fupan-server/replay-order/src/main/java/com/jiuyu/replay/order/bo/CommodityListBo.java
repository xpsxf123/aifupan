package com.jiuyu.replay.order.bo;


import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import com.jiuyu.replay.common.bo.PageBo;

/**
 * 商品列表查询参数
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-08 10:49:32
 */
@Data
@Schema(description = "商品列表查询参数")
public class CommodityListBo extends PageBo implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 模糊搜索查询条件
	 */
	@Schema(description = "模糊搜索查询条件")
	private String name;

	@Schema(description = "商品类型id")
	private Long commodityTypeId;

	@Schema(description = "状态 0未上架，1已上架")
	private Integer status;

}
