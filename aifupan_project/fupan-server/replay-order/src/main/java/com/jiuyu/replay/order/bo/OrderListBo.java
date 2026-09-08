package com.jiuyu.replay.order.bo;


import com.jiuyu.replay.common.bo.PageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 订单列表查询参数
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-10 11:42:33
 */
@Data
@Schema(description = "订单列表查询参数")
public class OrderListBo extends PageBo implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 订单标题
	 */
	@Schema(description = "订单标题")
	private String title;

	@Schema(description = "用户id")
	private Long userId;

	@Schema(description = "用户名称")
	private String userName;

	@Schema(description = "商品id")
	private Long commodityId;

	@Schema(description = "订单id")
	private String id;

	@Schema(description = "状态")
	private Integer status;

	@Schema(description = "状态列表")
	private List<Integer> statusList;

	@Schema(description = "订单类型 0免费订单，1升级订单，2免费版换收费版，3订单续费")
	private Integer orderType;

	@Schema(description = "来源 0正常下单，1手动添加，2邀请用户成功，3邀请码赠送")
	private Integer source;

	@Schema(description = "开始时间")
	private Date startDate;

	@Schema(description = "结束时间")
	private Date endDate;

	@Schema(description = "订单创建者id")
	private Long createId;

	@Schema(description = "是否付费订单 0付费订单，1试用订单")
	private Integer trialOrder;
}
