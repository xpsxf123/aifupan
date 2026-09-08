package com.jiuyu.replay.words.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * AI分析关键词与记录关联关系表
 *
 * @author DearSil
 * @email 2437495924@qq.com
 * @date 2025-03-03 18:49:53
 */
@Data
@TableName("tb_ai_analysis_sensitive_rela")
public class AiAnalysisSensitiveRelaEntity implements Serializable {
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
	 * 行业Id
	 */
	private Long tradeId;
	/**
	 * 行业Id数组Json
	 */
	private String tradeIdArr;
	/**
	 * 关键词
	 */
	private String sensitiveWord;
	/**
	 * 词语类型 0：敏感词 1：关键词 2：白名单(只有客户自定义有)
	 */
	private Integer sensitiveType;
	/**
	 * 排序
	 */
	private Integer sort;
	/**
	 * 描述
	 */
	private String remarks;
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
