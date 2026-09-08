package com.jiuyu.replay.words.vo.viewing;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 视频看盘混淆后的数据列表项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-04-12 16:31:15
 */
@Data
@Schema(description = "视频看盘混淆后的数据列表项")
public class VideoDataViewingConfuseListVo extends VideoDataViewingConfuseVo implements Serializable {
	private static final long serialVersionUID = 1L;



}
