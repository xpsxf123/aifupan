package com.jiuyu.replay.words.vo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 词库-词语关联列表项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-10-08 10:41:43
 */
@Data
@Schema(description = "词库-词语关联列表项")
public class LexiconWordListVo extends LexiconWordVo implements Serializable {
	private static final long serialVersionUID = 1L;



}
