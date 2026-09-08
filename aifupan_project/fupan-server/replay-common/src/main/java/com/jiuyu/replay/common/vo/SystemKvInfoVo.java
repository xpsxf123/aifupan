package com.jiuyu.replay.common.vo;


import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;

/**
 * 系统配置的键值对信息项
 *
 * @author lj
 * @email 
 * @date 2025-05-13 16:59:53
 */
@Data
@Schema(description = "系统配置的键值对信息项")
public class SystemKvInfoVo extends SystemKvVo implements Serializable {
	private static final long serialVersionUID = 1L;



}
