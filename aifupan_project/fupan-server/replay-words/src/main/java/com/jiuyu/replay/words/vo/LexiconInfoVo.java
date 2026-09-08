package com.jiuyu.replay.words.vo;


import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;

/**
 * 词库信息项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-10-06 16:34:13
 */
@Data
@Schema(description = "词库信息项")
public class LexiconInfoVo extends LexiconVo implements Serializable {
	private static final long serialVersionUID = 1L;



}
