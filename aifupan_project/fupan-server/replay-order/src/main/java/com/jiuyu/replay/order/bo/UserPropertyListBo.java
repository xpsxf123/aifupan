package com.jiuyu.replay.order.bo;


import com.jiuyu.replay.common.bo.PageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 用户资产列表查询参数
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-11 15:04:48
 */
@Data
@Schema(description = "用户资产列表查询参数")
public class UserPropertyListBo extends PageBo implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(description = "搜索关键字-用户账号、昵称、电话")
	private String keywords;

	@Schema(description = "用户id")
	private Long userId;

	@Schema(description = "用户id集合")
	private List<Long> userIds;

	@Schema(description = "用户名")
	private String userName;

	@Schema(description = "昵称")
	private String nickName;

	@Schema(description = "手机号")
	private String phone;

	@Schema(description = "商品类型code,范围集合")
	private List<SelectScopeBo> commodityTypeCode;

	@Schema(description = "版本Id")
	private Long packageId;

}
