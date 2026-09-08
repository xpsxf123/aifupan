package com.jiuyu.replay.common.vo;


import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;

/**
 * 文章信息项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-08-23 20:39:45
 */
@Data
@Schema(description = "文章信息项")
public class ArticleInfoVo extends ArticleVo implements Serializable {
	private static final long serialVersionUID = 1L;



}
