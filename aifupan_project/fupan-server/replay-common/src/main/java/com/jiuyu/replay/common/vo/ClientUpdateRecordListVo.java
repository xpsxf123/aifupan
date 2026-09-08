package com.jiuyu.replay.common.vo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 客户端更新记录列表项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-10-08 15:03:35
 */
@Data
@Schema(description = "客户端更新记录列表项")
public class ClientUpdateRecordListVo extends ClientUpdateRecordVo implements Serializable {
	private static final long serialVersionUID = 1L;



}
