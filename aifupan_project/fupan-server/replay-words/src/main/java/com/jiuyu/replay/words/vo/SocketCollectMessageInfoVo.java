package com.jiuyu.replay.words.vo;


import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;

/**
 * websocket采集的信息信息项
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-12-03 15:37:56
 */
@Data
@Schema(description = "websocket采集的信息信息项")
public class SocketCollectMessageInfoVo extends SocketCollectMessageVo implements Serializable {
	private static final long serialVersionUID = 1L;

	@Schema(description = "websocket的json数据")
	private String fileStr;

}
