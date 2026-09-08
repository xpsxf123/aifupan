package com.jiuyu.replay.common.vo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 文章列表项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-08-23 20:39:45
 */
@Data
@Schema(description = "文章列表项")
public class ArticleListVo extends ArticleVo implements Serializable {
	private static final long serialVersionUID = 1L;



}
