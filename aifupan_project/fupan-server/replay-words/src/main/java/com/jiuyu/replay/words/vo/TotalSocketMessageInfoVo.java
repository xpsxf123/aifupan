package com.jiuyu.replay.words.vo;


import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;

/**
 * 直播场次的websocket记录统计信息项
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2025-01-04 15:15:15
 */
@Data
@Schema(description = "直播场次的websocket记录统计信息项")
public class TotalSocketMessageInfoVo extends TotalSocketMessageVo implements Serializable {
	private static final long serialVersionUID = 1L;



}
