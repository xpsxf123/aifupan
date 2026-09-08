package com.jiuyu.replay.words.vo.file;

import java.io.Serializable;
import java.util.Date;


import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 文件的详情列表项
 *
 * @author lyw
 * @email 1798883178@qq.com
 * @date 2025-06-03 14:14:53
 */
@Data
@Schema(description = "文件的详情列表项")
public class UploadFileDetailListVo extends UploadFileDetailVo implements Serializable {
	private static final long serialVersionUID = 1L;



}
