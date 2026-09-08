package com.jiuyu.replay.order.bo;


import com.jiuyu.replay.common.bo.PageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户资产消费记录表列表查询参数
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-11 15:04:48
 */
@Data
@Schema(description = "用户资产消费记录表列表查询参数")
public class UserPropertyDetailsListBo extends PageBo implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(description = "用户id")
	private Long userId;

	@Schema(description = "用户名")
	private String userName;

	@Schema(description = "父级用户id")
	private Long parentUserId;

	@Schema(description = "资产id")
	private Long propertyId;

	@Schema(description = "商品类型id")
	private Long commodityTypeId;

	@Schema(description = "商品类型code")
	private String commodityTypeCode;

	@Schema(description = "开始时间")
	private Date startCreateDate;

	@Schema(description = "结束时间")
	private Date endCreateDate;
}
