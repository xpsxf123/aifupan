package com.jiuyu.replay.words.vo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * websocket采集的信息列表项
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-12-03 15:37:56
 */
@Data
@Schema(description = "websocket采集的信息列表项")
public class SocketCollectMessageListVo extends SocketCollectMessageVo implements Serializable {
	private static final long serialVersionUID = 1L;



}
