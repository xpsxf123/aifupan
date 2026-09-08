package com.jiuyu.replay.generic.vo.order;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 商品类型信息项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-19 20:52:26
 */
@Data
@Schema(description = "商品类型信息项")
public class CommodityTypeInfoVo extends CommodityTypeVo implements Serializable {
	private static final long serialVersionUID = 1L;



}
