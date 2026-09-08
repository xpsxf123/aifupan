package com.jiuyu.replay.third.vo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 语音每秒的统计记录列表项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-11-08 16:27:43
 */
@Data
@Schema(description = "语音每秒的统计记录列表项")
public class LogAudioCollectListVo extends LogAudioCollectVo implements Serializable {
	private static final long serialVersionUID = 1L;



}
