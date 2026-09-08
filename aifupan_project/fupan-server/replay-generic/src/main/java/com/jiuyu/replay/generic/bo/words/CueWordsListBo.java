package com.jiuyu.replay.generic.bo.words;


import com.jiuyu.replay.generic.bo.common.PageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 提示词列表查询参数
 *
 * @author DearSil
 * @email 2437495924@qq.com
 * @date 2025-02-20 15:23:28
 */
@Data
@Schema(description = "提示词列表查询参数")
public class CueWordsListBo extends PageBo implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 模糊搜索查询条件
	 */
	@Schema(description = "模糊搜索查询条件")
	private String keyword;


	/**
	 * 行业id，1表示全行业
	 */
	@Schema(description = "行业id，1表示全行业")
	private Long tradeId;

	/**
	 * 客户端行业id，1表示全行业
	 */
	@Schema(description = "客户端资源id")
	private String sourceId;

	/**
	 * 客户端行业id，1表示全行业
	 */
    @Schema(description = "客户端资源类型：0: 视频, 1: 文件")
	private Integer sourceType;

	/**
	 * 类型id，0:运营提示词，1:违规提示词 2：对比复盘提示词
	 */
	@Schema(description = "类型id，0:运营提示词，1:违规提示词")
	private Integer cueType;

	/**
	 * 提示词用于：0：单个分析，1：对比分析
	 */
	@Schema(description = "提示词用于：0：单个分析，1：对比分析")
	private Integer applyTo;

	/**
	 * 当前范围：范围 0:全文，1:段落
	 */
	@Schema(description = "当前范围：范围 0:全文，1:段落")
	private Integer scope;

	/**
	 * 行业id数组，1表示全行业
	 */
	@Schema(description = "行业id数组，1表示全行业")
	private List<Long> tradeIds;

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
	 * 租户id
	 */
	private Long tenantId;


	/**
	 * 查询类型 0: 平台级，1: 租户级，2: 租户+平台组合型
	 */
	private Integer queryType = 0;

    /**
     * 分析类型 0-普通分析，1-综合分析
     */
    @Schema(description = "分析类型 0-普通分析，1-综合分析")
    private Integer analysisType;

	public Integer getQueryType() {
		if (queryType == null) {
			return 0;
		}
		return queryType;
	}
}
