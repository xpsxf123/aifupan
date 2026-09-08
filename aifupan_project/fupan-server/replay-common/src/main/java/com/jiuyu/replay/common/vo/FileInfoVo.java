package com.jiuyu.replay.common.vo;


import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;

/**
 * 文件信息项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-07-10 11:13:51
 */
@Data
@Schema(description = "文件信息项")
public class FileInfoVo extends FileVo implements Serializable {
	private static final long serialVersionUID = 1L;



}
