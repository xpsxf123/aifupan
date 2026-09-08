package com.jiuyu.replay.words.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * ai问答的历史分析段落记录
 *
 * @author lj
 * @email 
 * @date 2025-03-22 15:36:36
 */
@Data
@TableName("tb_history_paragraph")
public class HistoryParagraphEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * id
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * 租户id
	 */
	private Long tenantId;
	/**
	 * 用户id
	 */
	private Long userId;
	/**
	 * 别名
	 */
	private String alias;
	/**
	 * 资产类型 0视频，1文件，2对比分析
	 */
	private Integer sourceType;
	/**
	 * 资产id
	 */
	private String sourceId;
	/**
	 * 助手类型 0运营助手 1违规助手
	 */
	@TableField(value = "analysis_type")
	private Integer type;
	/**
	 * 历史段落的code
	 */
	@TableField(value = "analysis_code")
	private String code;
	/**
	 * 段落内容
	 */
	private String content;
	/**
	 * 最后修改时间
	 */
	private Date updateDate;
	/**
	 * 创建时间
	 */
	private Date createDate;


}
