package com.jiuyu.replay.order.vo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 商品类型用量关联表列表项
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-08 10:49:32
 */
@Data
@Schema(description = "商品类型用量关联表列表项")
public class TypeConsumptionListVo extends TypeConsumptionVo implements Serializable {
	private static final long serialVersionUID = 1L;



}
