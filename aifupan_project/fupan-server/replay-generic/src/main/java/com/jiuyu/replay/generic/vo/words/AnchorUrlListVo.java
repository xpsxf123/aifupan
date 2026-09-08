package com.jiuyu.replay.generic.vo.words;

import java.io.Serializable;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 主播url列表项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-08-10 16:47:37
 */
@Data
@Schema(description = "主播url列表项")
public class AnchorUrlListVo extends AnchorUrlVo implements Serializable {
	private static final long serialVersionUID = 1L;



}
