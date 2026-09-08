package com.jiuyu.replay.words.vo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 视频标记列表项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-10-10 18:09:05
 */
@Data
@Schema(description = "视频标记列表项")
public class VideoMarkListVo extends VideoMarkVo implements Serializable {
	private static final long serialVersionUID = 1L;



}
