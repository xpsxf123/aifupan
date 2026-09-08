package com.jiuyu.replay.order.vo;


import com.jiuyu.replay.generic.vo.order.UserPropertyTypeInfoVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 用户资产信息项
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-11 15:04:48
 */
@Data
@Schema(description = "用户资产信息项")
public class UserPropertyInfoVo extends UserPropertyVo implements Serializable {
	private static final long serialVersionUID = 1L;


	@Schema(description = "用户资产类型明细列表")
	private List<UserPropertyTypeInfoVo> userPropertyTypeList;

}
