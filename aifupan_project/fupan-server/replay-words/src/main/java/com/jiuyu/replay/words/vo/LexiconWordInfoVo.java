package com.jiuyu.replay.words.vo;


import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;

/**
 * 词库-词语关联信息项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-10-08 10:41:43
 */
@Data
@Schema(description = "词库-词语关联信息项")
public class LexiconWordInfoVo extends LexiconWordVo implements Serializable {
	private static final long serialVersionUID = 1L;



}
