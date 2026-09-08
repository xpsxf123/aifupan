package com.jiuyu.replay.generic.vo.order;

import java.io.Serializable;



import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 订单的扩展表列表项
 *
 * @author lyw
 * @email 1798883178@qq.com
 * @date 2025-07-05 09:56:24
 */
@Data
@Schema(description = "订单的扩展表列表项")
public class OrderExtendListVo extends OrderExtendVo implements Serializable {
	private static final long serialVersionUID = 1L;



}
