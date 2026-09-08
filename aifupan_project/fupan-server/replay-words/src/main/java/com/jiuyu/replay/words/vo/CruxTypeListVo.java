package com.jiuyu.replay.words.vo;

import java.io.Serializable;

import com.jiuyu.replay.generic.vo.words.CruxTypeVo;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 关键词类型列表项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-12-26 10:44:06
 */
@Data
@Schema(description = "关键词类型列表项")
public class CruxTypeListVo extends CruxTypeVo implements Serializable {
	private static final long serialVersionUID = 1L;



}
