package com.jiuyu.replay.words.vo;


import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;

/**
 * 视频标记信息项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-10-10 18:09:05
 */
@Data
@Schema(description = "视频标记信息项")
public class VideoMarkInfoVo extends VideoMarkVo implements Serializable {
	private static final long serialVersionUID = 1L;



}
