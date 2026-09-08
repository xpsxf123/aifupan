package com.jiuyu.replay.common.vo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 系统配置的键值对列表项
 *
 * @author lj
 * @email 
 * @date 2025-05-13 16:59:53
 */
@Data
@Schema(description = "系统配置的键值对列表项")
public class SystemKvListVo extends SystemKvVo implements Serializable {
	private static final long serialVersionUID = 1L;



}
