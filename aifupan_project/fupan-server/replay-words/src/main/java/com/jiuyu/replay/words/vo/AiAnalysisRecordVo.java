package com.jiuyu.replay.words.vo;

import com.jiuyu.replay.words.bo.AiAnalysisSensitiveRelaBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * AI分析出来的关键词总数和未匹配上词库的关键词个数信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-02-21 17:06:53
 */
@Data
@Schema(description = "AI分析出来的关键词总数和未匹配上词库的关键词个数信息")
public class AiAnalysisRecordVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Schema(description = "ID")
	private Long id;
	/**
	 * 视频或文件唯一标识
	 */
	@Schema(description = "视频或文件唯一标识")
	private String uuid;
	/**
	 * 类型  0：录制视频 1：上传文件
	 */
	@Schema(description = "类型  0：录制视频 1：上传文件")
	private Integer recordType;
	/**
	 * 分析出来的关键词总数
	 */
	@Schema(description = "分析出来的关键词总数")
	private Integer sensitiveWordTotal;
	/**
	 * 未匹配上词库的关键词个数
	 */
	@Schema(description = "未匹配上词库的关键词个数")
	private Integer sensitiveWordMark;
	/**
	 * 未匹配上词库的关键词列表
	 */
	@Schema(description = "未匹配上词库的关键词列表")
	private List<AiAnalysisSensitiveRelaBo> notMarkWordList;
	/**
	 * 创建时间
	 */
	@Schema(description = "创建时间")
	private Date createDate;
	/**
	 * 最后修改时间
	 */
	@Schema(description = "最后修改时间")
	private Date updateDate;
	/**
	 * 是否已删除
	 */
	@Schema(description = "是否已删除")
	private Integer isDeleted;

	/**
	 * Ai分析后的结果
	 */
	@Schema(description = "Ai分析后的结果")
	private List<String> aiAnswerList;
}
