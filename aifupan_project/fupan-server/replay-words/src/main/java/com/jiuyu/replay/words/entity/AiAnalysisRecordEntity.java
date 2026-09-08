package com.jiuyu.replay.words.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * AI分析出来的关键词总数和未匹配上词库的关键词个数
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-02-21 17:06:53
 */
@Data
@TableName("tb_ai_analysis_record")
public class AiAnalysisRecordEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * 视频或文件唯一标识
	 */
	private String uuid;
	/**
	 * 类型  0：录制视频 1：上传文件
	 */
	private Integer recordType;
	/**
	 * 分析出来的关键词总数
	 */
	private Integer sensitiveWordTotal;
	/**
	 * 未匹配上词库的关键词个数
	 */
	private Integer sensitiveWordMark;
	/**
	 * 创建时间
	 */
	private Date createDate;
	/**
	 * 最后修改时间
	 */
	private Date updateDate;
	/**
	 * 是否已删除
	 */
	private Integer isDeleted;


}
