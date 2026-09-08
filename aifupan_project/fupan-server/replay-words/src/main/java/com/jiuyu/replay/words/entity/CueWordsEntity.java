package com.jiuyu.replay.words.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 提示词
 *
 * @author DearSil
 * @email 2437495924@qq.com
 * @date 2025-02-20 15:23:28
 */
@Data
@TableName("tb_cue_words")
public class CueWordsEntity implements Serializable {
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
     * 提示词概要
     */
    private String outline;
	/**
	 * 提示词
	 */
	private String cueWord;
	/**
	 * 实际提示词：具体问题
	 */
	private String problem;
	/**
	 * 提示词用于：0：单个分析，1：对比分析
	 */
	private Integer applyTo;
	/**
	 * 提示词类型 0: 运营提示词，1:违规提示词 2：对比复盘提示词
	 */
	private Integer cueType;
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

    /**
     * 账号归属类型(单个分析才有) 0：自有账号 1：同行账号
     */
    private Integer accountType;

    /**
     * 对比使用场景(对比分析才有) 1：对比上一次场、2：不同直播间对比、3：同直播间对比
     */
    private Integer syncScene;

	/**
	 * 租户ID， 0表示全平台通用
	 */
	private Long tenantId;

    /**
     * 分析类型 0-普通分析，1-综合分析
     */
    private Integer analysisType;
}
