package com.jiuyu.replay.words.vo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 直播场次的websocket记录统计列表项
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2025-01-04 15:15:15
 */
@Data
@Schema(description = "直播场次的websocket记录统计列表项")
public class TotalSocketMessageListVo extends TotalSocketMessageVo implements Serializable {
	private static final long serialVersionUID = 1L;



}
