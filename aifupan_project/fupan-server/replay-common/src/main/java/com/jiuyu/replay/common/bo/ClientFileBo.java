package com.jiuyu.replay.common.bo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 客户端文件信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-10-08 15:03:35
 */
@Data
@Schema(description = "客户端文件信息")
public class ClientFileBo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 主键
	 */
	@Schema(description = "主键")
	private Long id;
	/**
	 * 文件名称
	 */
	@Schema(description = "文件名称")
	private String fileName;
	/**
	 * 文件类型 0exe,1dll,2sql文件
	 */
	@Schema(description = "文件类型 0exe,1dll,2sql文件")
	private Integer fileType;
	/**
	 * 更新表主键
	 */
	@Schema(description = "更新表主键")
	private Long updateId;


}
