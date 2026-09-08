package com.jiuyu.replay.words.vo;


import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;

/**
 * 规则关联词信息项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-09 18:16:01
 */
@Data
@Schema(description = "规则关联词信息项")
public class WordRuleRelevanceInfoVo extends WordRuleRelevanceVo implements Serializable {
	private static final long serialVersionUID = 1L;



}
