package com.jiuyu.replay.words.bo;

import com.jiuyu.replay.words.vo.UploadFileAnalysisVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-14 09:15:19
 */
@Data
@Schema(description = "信息")
public class UploadFileRecodBo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 文件id
	 */
	@Schema(description = "文件id")
	private String fileId;

	/**
	 * 上传分析内容
	 */
	@Schema(description = "上传分析内容")
	List<UploadFileAnalysisVo> list;


	/**
	 * userid
	 */
	@Schema(description = "userid")
	private Long userId;
}
