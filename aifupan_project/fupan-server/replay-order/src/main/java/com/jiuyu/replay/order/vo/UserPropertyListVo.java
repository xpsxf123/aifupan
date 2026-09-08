package com.jiuyu.replay.order.vo;

import com.jiuyu.replay.generic.vo.order.UserPropertyTypeInfoVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 用户资产列表项
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-11 15:04:48
 */
@Data
@Schema(description = "用户资产列表项")
public class UserPropertyListVo extends UserPropertyVo implements Serializable {
	private static final long serialVersionUID = 1L;

	@Schema(description = "用户名称")
	private String userName;

	@Schema(description = "用户昵称")
	private String nickName;

	@Schema(description = "用户手机号")
	private String phone;

	@Schema(description = "用户类型")
	private Integer userType;

	@Schema(description = "用户版本名称")
	private String packageName;

	@Schema(description = "用户版本id")
	private Long packageId;

	@Schema(description = "用户版本等级")
	private Integer packageLevel;

	@Schema(description = "用户版本过期时间")
	private Date expirationTime;


	@Schema(description = "用户资产明细列表")
	private List<UserPropertyTypeInfoVo> userPropertyTypeList;

}
