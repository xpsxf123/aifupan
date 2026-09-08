package com.jiuyu.replay.generic.vo.words;

import java.io.Serializable;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 视频的分析记录列表项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-12-03 11:26:50
 */
@Data
@Schema(description = "视频的分析记录列表项")
public class VideoAnalysisRecordListVo extends VideoAnalysisRecordVo implements Serializable {
	private static final long serialVersionUID = 1L;



}
