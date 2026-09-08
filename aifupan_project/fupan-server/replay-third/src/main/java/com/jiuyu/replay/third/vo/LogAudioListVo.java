package com.jiuyu.replay.third.vo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * qps调用记录表列表项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-11-08 16:27:43
 */
@Data
@Schema(description = "qps调用记录表列表项")
public class LogAudioListVo extends LogAudioVo implements Serializable {
	private static final long serialVersionUID = 1L;



}
