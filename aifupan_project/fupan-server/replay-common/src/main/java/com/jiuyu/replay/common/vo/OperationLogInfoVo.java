package com.jiuyu.replay.common.vo;


import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;

/**
 * 操作日志表信息项
 *
 * @author lyw
 * @email 1798883178@qq.com
 * @date 2025-06-26 13:59:24
 */
@Data
@Schema(description = "操作日志表信息项")
public class OperationLogInfoVo extends OperationLogVo implements Serializable {
	private static final long serialVersionUID = 1L;



}
