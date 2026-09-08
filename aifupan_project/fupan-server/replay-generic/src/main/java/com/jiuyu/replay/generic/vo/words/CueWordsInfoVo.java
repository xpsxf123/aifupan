package com.jiuyu.replay.generic.vo.words;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 提示词信息项
 *
 * @author DearSil
 * @email 2437495924@qq.com
 * @date 2025-02-20 15:23:28
 */
@Data
@Schema(description = "提示词信息项")
public class CueWordsInfoVo extends CueWordsVo implements Serializable {
	private static final long serialVersionUID = 1L;

}
