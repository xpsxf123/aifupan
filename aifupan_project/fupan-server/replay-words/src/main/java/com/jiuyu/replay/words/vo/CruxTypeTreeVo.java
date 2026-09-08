package com.jiuyu.replay.words.vo;

import com.jiuyu.replay.generic.vo.words.CruxTypeVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 关键词类型信息-树形
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-12-26 10:44:06
 */
@Data
@Schema(description = "关键词类型信息-树形")
public class CruxTypeTreeVo extends CruxTypeVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 子关键词类型
	 */
	@Schema(description = "子关键词类型")
	private List<CruxTypeTreeVo> children;
	/**
	 * 父级id数组
	 */
	@Schema(description = "父级id数组")
	private List<Long> parentIdArr;
	/**
	 * id层级数组
	 */
	@Schema(description = "id层级数组")
	private List<Long> idArr;
	/**
	 * 关键词数量
	 */
	@Schema(description = "关键词数量")
	private Integer cruxNum;


}
