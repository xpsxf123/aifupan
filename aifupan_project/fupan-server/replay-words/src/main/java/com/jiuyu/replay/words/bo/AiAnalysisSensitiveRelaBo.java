package com.jiuyu.replay.words.bo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI分析关键词与记录关联关系表信息
 *
 * @author DearSil
 * @email 2437495924@qq.com
 * @date 2025-03-03 18:49:53
 */
@Data
@Schema(description = "AI分析关键词与记录关联关系表信息")
public class AiAnalysisSensitiveRelaBo implements Serializable {
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
	 * 行业Id
	 */
	@Schema(description = "行业Id")
	private Long tradeId;
	/**
	 * 行业Id数组Json
	 */
	@Schema(description = "行业Id数组Json")
	private String tradeIdArr;
	/**
	 * 关键词
	 */
	@Schema(description = "关键词")
	private String sensitiveWord;
	/**
	 * 词语类型 0：敏感词 1：关键词 2：白名单(只有客户自定义有)
	 */
	@Schema(description = "词语类型 0：敏感词 1：关键词 2：白名单(只有客户自定义有)")
	private Integer sensitiveType;
	/**
	 * 排序
	 */
	@Schema(description = "排序")
	private Integer sort;
	/**
	 * 描述
	 */
	@Schema(description = "描述")
	private String remarks;
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


}
