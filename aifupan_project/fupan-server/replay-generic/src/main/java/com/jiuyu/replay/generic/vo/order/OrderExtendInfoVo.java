package com.jiuyu.replay.generic.vo.order;



import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;

/**
 * 订单的扩展表信息项
 *
 * @author lyw
 * @email 1798883178@qq.com
 * @date 2025-07-05 09:56:24
 */
@Data
@Schema(description = "订单的扩展表信息项")
public class OrderExtendInfoVo extends OrderExtendVo implements Serializable {
	private static final long serialVersionUID = 1L;



}
