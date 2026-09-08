package com.jiuyu.replay.words.vo.file;



import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;

/**
 * 文件的详情信息项
 *
 * @author lyw
 * @email 1798883178@qq.com
 * @date 2025-06-03 14:14:53
 */
@Data
@Schema(description = "文件的详情信息项")
public class UploadFileDetailInfoVo extends UploadFileDetailVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 文件信息
	 */
	@Schema(description = "文件信息")
	private UploadFileInfoVo uploadFile;


}
