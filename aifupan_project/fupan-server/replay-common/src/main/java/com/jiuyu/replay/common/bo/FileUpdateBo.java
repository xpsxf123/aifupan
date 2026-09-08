package com.jiuyu.replay.common.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 文件信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-07-10 11:13:51
 */
@Data
@Schema(description = "文件信息")
public class FileUpdateBo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Schema(description = "ID")
	private Long id;
	/**
	 * 排序
	 */
	@Schema(description = "排序")
	private Integer sort;


}
