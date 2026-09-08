package com.jiuyu.replay.third.vo;


import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;

/**
 * qps调用记录表信息项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-11-08 16:27:43
 */
@Data
@Schema(description = "qps调用记录表信息项")
public class LogAudioInfoVo extends LogAudioVo implements Serializable {
	private static final long serialVersionUID = 1L;



}
