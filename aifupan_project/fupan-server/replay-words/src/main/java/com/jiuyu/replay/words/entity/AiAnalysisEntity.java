package com.jiuyu.replay.words.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * AI分析表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-01-09 15:54:12
 */
@Data
@TableName("tb_ai_analysis")
public class AiAnalysisEntity implements Serializable {
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
	 * 会话的上下文缓存ID
	 */
	private String contextId;
	/**
	 * 类型  0：录制视频 1：上传文件
	 */
	private Integer type;
	/**
	 * 同一个对话中的类型 0：提示词 1：答案
	 */
	private Integer textType;
	/**
	 * 排序
	 */
	private Integer sort;
	/**
	 * json数据文件存储路径
	 */
	private String storeFileName;
	/**
	 * ossKey
	 */
	private String ossKey;
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
