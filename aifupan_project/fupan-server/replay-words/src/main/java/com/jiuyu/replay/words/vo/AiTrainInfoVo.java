package com.jiuyu.replay.words.vo;


import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;

/**
 * AI训练信息项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-02-20 18:47:39
 */
@Data
@Schema(description = "AI训练信息项")
public class AiTrainInfoVo extends AiTrainVo implements Serializable {
	private static final long serialVersionUID = 1L;



}
