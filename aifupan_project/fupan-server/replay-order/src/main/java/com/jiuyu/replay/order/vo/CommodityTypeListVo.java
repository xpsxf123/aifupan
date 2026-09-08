package com.jiuyu.replay.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 商品类型列表项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-19 20:52:26
 */
@Data
@Schema(description = "商品类型列表项")
public class CommodityTypeListVo extends CommodityTypeVo implements Serializable {
	private static final long serialVersionUID = 1L;



}
