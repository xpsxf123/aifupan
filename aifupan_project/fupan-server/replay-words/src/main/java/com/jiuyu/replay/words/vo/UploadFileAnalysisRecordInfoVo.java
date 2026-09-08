package com.jiuyu.replay.words.vo;


import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;

/**
 * 文件的分析记录信息项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-12-03 11:26:50
 */
@Data
@Schema(description = "文件的分析记录信息项")
public class UploadFileAnalysisRecordInfoVo extends UploadFileAnalysisRecordVo implements Serializable {
	private static final long serialVersionUID = 1L;



}
