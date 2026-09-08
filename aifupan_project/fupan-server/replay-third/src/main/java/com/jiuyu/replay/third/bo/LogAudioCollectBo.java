package com.jiuyu.replay.third.bo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 语音每秒的统计记录信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-11-08 16:27:43
 */
@Data
@Schema(description = "语音每秒的统计记录信息")
public class LogAudioCollectBo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Schema(description = "ID")
	private Long id;
	/**
	 * 调用时间
	 */
	@Schema(description = "调用时间")
	private Date callDate;
	/**
	 * qps总数
	 */
	@Schema(description = "qps总数")
	private Integer qpsNumCount;
	/**
	 * 用户量总数
	 */
	@Schema(description = "用户量总数")
	private Integer userNumCount;
	/**
	 * 创建时间
	 */
	@Schema(description = "创建时间")
	private Date createDate;
	/**
	 * 最后修改时间
	 */
	@Schema(description = "最后修改时间")
	private Date updateDate;
	/**
	 * 是否已删除
	 */
	@Schema(description = "是否已删除")
	private Integer isDeleted;


}
