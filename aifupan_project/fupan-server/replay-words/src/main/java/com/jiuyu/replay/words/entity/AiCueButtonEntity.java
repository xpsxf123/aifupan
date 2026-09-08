package com.jiuyu.replay.words.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 固定提示按钮

 *
 * @author DearSil
 * @email 2437495924@qq.com
 * @date 2025-02-25 11:11:43
 */
@Data
@TableName("tb_ai_cue_button")
public class AiCueButtonEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * 行业id，0表示全行业
	 */
	private Long tradeId;
	/**
	 * 来源类型 0：系统
	 */
	private Integer resourceType;
	/**
	 * 按钮名称
	 */
	private String buttonName;
	/**
	 * 实际提示词：具体问题
	 */
	private String problem;
	/**
	 * 提示词类型 0: 运营提示词，1:违规提示词 2：对比复盘提示词
	 */
	private Integer buttonType;
	/**
	 * 范围 0:全文，1:段落
	 */
	private Integer scope;
	/**
	 * 场景 0:直接提示，1:弹框操作
	 */
	private Integer scene;
	/**
	 * 提示词在当前行业排序
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
