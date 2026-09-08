package com.jiuyu.replay.common.vo;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 操作日志表列表项
 *
 * @author lyw
 * @email 1798883178@qq.com
 * @date 2025-06-26 13:59:24
 */
@Data
@Schema(description = "操作日志表列表项")
public class OperationLogListVo extends OperationLogVo implements Serializable {
	private static final long serialVersionUID = 1L;

	@Schema(description = "操作前数据")
	private Map<String, Object>  beforeObjData;
	/**
	 * 操作后数据
	 */
	@Schema(description = "操作后数据")
	private Map<String, Object> afterObjData;

}
