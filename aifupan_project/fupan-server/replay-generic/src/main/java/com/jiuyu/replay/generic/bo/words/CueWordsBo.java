package com.jiuyu.replay.generic.bo.words;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 提示词信息
 *
 * @author DearSil
 * @email 2437495924@qq.com
 * @date 2025-02-20 15:23:28
 */
@Data
@Schema(description = "提示词信息")
public class CueWordsBo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Schema(description = "ID")
	private Long id;
	/**
	 * 行业id，0表示全行业
	 */
	@Schema(description = "行业id，0表示全行业")
	private Long tradeId;
	/**
	 * 来源类型 0：系统
	 */
	@Schema(description = "来源类型 0：系统")
	private Integer resourceType;
    /**
     * 提示词概要
     */
    @Schema(description = "提示词概要")
    private String outline;
	/**
	 * 提示词
	 */
	@Schema(description = "提示词")
	private String cueWord;
	/**
	 * 实际提示词：具体问题
	 */
	@Schema(description = "实际提示词：具体问题")
	private String problem;
	/**
	 * 提示词用于：0：单个分析，1：对比分析
	 */
	@Schema(description = "提示词用于：0：单个分析，1：对比分析")
	private Integer applyTo;
	/**
	 * 提示词类型(预留)
	 */
	@Schema(description = "提示词类型(预留)")
	private Integer cueType;
	/**
	 * 范围 0:全文，1:段落
	 */
	@Schema(description = "范围 0:全文，1:段落")
	private Integer scope;
	/**
	 * 场景 0:直接提示，1:弹框操作
	 */
	@Schema(description = "场景 0:直接提示，1:弹框操作")
	private Integer scene;
	/**
	 * 提示词在当前行业排序
	 */
	@Schema(description = "提示词在当前行业排序")
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

    /**
     * 账号归属类型(单个分析才有) 0：自由账号 1：同行账号
     */
    @Schema(description = "账号归属类型(单个分析才有) 0：自由账号 1：同行账号")
    private Integer accountType;

    /**
     * 对比使用场景(对比分析才有) 1：对比上一次场、2：不同直播间对比、3：同直播间对比
     */
    @Schema(description = "对比使用场景(对比分析才有) 1：对比上一次场、2：不同直播间对比、3：同直播间对比")
    private Integer syncScene;


	/**
	 * 租户ID 新增时默认0，修改时为空则忽略
	 */
	private Long tenantId;

    /**
     * 分析类型 0-普通分析，1-综合分析
     */
    @Schema(description = "分析类型 0-普通分析，1-综合分析")
    private Integer analysisType;
}
