package com.jiuyu.replay.words.vo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 规则关联词列表项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-09 18:16:01
 */
@Data
@Schema(description = "规则关联词列表项")
public class WordRuleRelevanceListVo extends WordRuleRelevanceVo implements Serializable {
	private static final long serialVersionUID = 1L;



}
