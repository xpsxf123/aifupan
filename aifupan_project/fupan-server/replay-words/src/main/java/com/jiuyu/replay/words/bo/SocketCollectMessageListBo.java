package com.jiuyu.replay.words.bo;


import com.jiuyu.replay.common.bo.PageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * websocket采集的信息列表查询参数
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-12-03 15:37:56
 */
@Data
@Schema(description = "websocket采集的信息列表查询参数")
public class SocketCollectMessageListBo extends PageBo implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(description = "直播场次号")
	private String batchNumber;

	@Schema(description = "直播标识")
	private String secUid;

	@Schema(description = "用户id")
	private Long userId;

	@Schema(description = "id")
	private Long id;

	@Schema(description = "视频唯一标识")
	private String videoId;

}
