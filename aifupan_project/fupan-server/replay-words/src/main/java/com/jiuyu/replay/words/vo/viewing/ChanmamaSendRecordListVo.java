package com.jiuyu.replay.words.vo.viewing;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 第三方数据平台发送记录列表项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-04-22 14:21:09
 */
@Data
@Schema(description = "第三方数据平台发送记录列表项")
public class ChanmamaSendRecordListVo extends ChanmamaSendRecordVo implements Serializable {
	private static final long serialVersionUID = 1L;



}
