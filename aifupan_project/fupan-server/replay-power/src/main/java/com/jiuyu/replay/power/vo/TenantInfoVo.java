package com.jiuyu.replay.power.vo;


import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;

/**
 * 租户信息项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-12-31 11:52:17
 */
@Data
@Schema(description = "租户信息项")
public class TenantInfoVo extends TenantVo implements Serializable {
	private static final long serialVersionUID = 1L;



}
