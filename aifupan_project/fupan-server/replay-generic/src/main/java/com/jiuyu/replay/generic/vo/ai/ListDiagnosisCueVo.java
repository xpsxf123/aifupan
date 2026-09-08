package com.jiuyu.replay.generic.vo.ai;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * ai诊断提示词配置信息项
 *
 * @author lj
 * @email 
 * @date 2025-05-19 14:02:59
 */
@Data
@Schema(description = "ai诊断提示词配置信息项")
public class ListDiagnosisCueVo implements Serializable {
	private static final long serialVersionUID = 1L;

	@Schema(description = "提示词类型")
	private Integer cueType;

	@Schema(description = "标签名称")
	private String tagName;

	@Schema(description = "id")
	private List<CueWords> cueWordsList;

	@Schema(description = "ai诊断提示词配置信息项-提示词")
	@Data
	public static class CueWords{
		@Schema(description = "提示词id")
		private Long cueWordsId;

		@Schema(description = "行业id，0表示全行业")
		private Long tradeId;

		@Schema(description = "提示词")
		private String cueWord;

		@Schema(description = "提示词在当前行业排序")
		private Integer sort;

		@Schema(description = "主播的是否选中")
		private Integer anchorSelect;

		@Schema(description = "视频的是否选中")
		private Integer videoSelect;

		@Schema(description = "状态 0未处理 1处理中 2处理完成 3处理失败")
		private Integer qaStatus;

		@Schema(description = "处理失败的错误信息")
		private String errorContent;
	}

}
