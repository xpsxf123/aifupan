package com.jiuyu.replay.words.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * ai问答的历史分析段落记录信息
 *
 * @author lj
 * @email 
 * @date 2025-03-22 15:36:36
 */
@Data
@Schema(description = "ai问答的历史分析段落记录信息")
public class HistoryParagraphVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * id
	 */
	@Schema(description = "id")
	private Long id;
	/**
	 * 租户id
	 */
	@Schema(description = "租户id")
	private Long tenantId;
	/**
	 * 用户id
	 */
	@Schema(description = "用户id")
	private Long userId;
	/**
	 * 别名
	 */
	@Schema(description = "别名")
	private String alias;
	/**
	 * 资产类型 0视频，1文件，2对比分析
	 */
	@Schema(description = "资产类型 0视频，1文件，2对比分析")
	private Integer sourceType;
	/**
	 * 资产id
	 */
	@Schema(description = "资产id")
	private String sourceId;
	/**
	 * 助手类型 0运营助手 1违规助手
	 */
	@Schema(description = "助手类型 0运营助手 1违规助手")
	private Integer type;
	/**
	 * 历史段落的code
	 */
	@Schema(description = "历史段落的code")
	private String code;
	/**
	 * 段落内容
	 */
	@Schema(description = "段落内容")
	private String content;
	/**
	 * 最后修改时间
	 */
	@Schema(description = "最后修改时间")
	private Date updateDate;
	/**
	 * 创建时间
	 */
	@Schema(description = "创建时间")
	private Date createDate;


}
