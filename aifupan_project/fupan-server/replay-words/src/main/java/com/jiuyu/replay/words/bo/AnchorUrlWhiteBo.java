package com.jiuyu.replay.words.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 主播白名单表信息
 *
 * @author hts
 * @email 1776764427@qq.com
 * @date 2024-09-15 09:50:55
 */
@Data
@Schema(description = "主播白名单表信息")
public class AnchorUrlWhiteBo implements Serializable {
	private static final long serialVersionUID = 1L;


	/**
	 * 主播唯一标识
	 */
	@Schema(description = "主播唯一标识")
	private String secUid;
	/**
	 * 用户ID
	 */
	@Schema(description = "用户ID")
	private List<Long> userId;


}
