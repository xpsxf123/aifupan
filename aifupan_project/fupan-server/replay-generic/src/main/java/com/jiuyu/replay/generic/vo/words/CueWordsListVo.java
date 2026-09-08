package com.jiuyu.replay.generic.vo.words;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 提示词列表项
 *
 * @author DearSil
 * @email 2437495924@qq.com
 * @date 2025-02-20 15:23:28
 */
@Data
@Schema(description = "提示词列表项")
public class CueWordsListVo extends CueWordsVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 是否为租户自定义提示词
	 */
	private Boolean customize;


	/**
	 * 是否为租户自定义提示词
	 */
	public Boolean getCustomize() {
		if (customize == null) {
			this.customize = super.getTenantId() != null && super.getTenantId() != 0L;
		}
		return customize;
	}
}
