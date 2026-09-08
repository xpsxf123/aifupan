package com.jiuyu.replay.order.vo;


import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;

/**
 * 商品类型用量关联表信息项
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-08 10:49:32
 */
@Data
@Schema(description = "商品类型用量关联表信息项")
public class TypeConsumptionInfoVo extends TypeConsumptionVo implements Serializable {
	private static final long serialVersionUID = 1L;


}
