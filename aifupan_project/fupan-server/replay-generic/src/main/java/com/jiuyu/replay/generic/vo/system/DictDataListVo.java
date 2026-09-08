package com.jiuyu.replay.generic.vo.system;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 字典列表项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-07-08 11:46:20
 */
@Data
@Schema(description = "字典列表项")
public class DictDataListVo extends DictDataVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 字典类型
	 */
	@Schema(description = "字典类型")
	private String typeName;

    @Schema(description = "子列表")
    private List<DictDataListVo> children;
}
