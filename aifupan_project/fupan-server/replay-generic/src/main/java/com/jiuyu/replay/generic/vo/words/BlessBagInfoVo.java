package com.jiuyu.replay.generic.vo.words;


import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;

/**
 * 福袋信息信息项
 *
 * @author lyw
 * @email 1798883178@qq.com
 * @date 2025-05-22 20:02:21
 */
@Data
@Schema(description = "福袋信息信息项")
public class BlessBagInfoVo extends BlessBagVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 发放福袋时间
	 */
	@Schema(description = "发放福袋时间")
	private String blessBagTime;

	/**
	 * 福袋奖励
	 */
	@Schema(description = "福袋奖励")
	private String blessBagReward;

	/**
	 * 领取条件
	 */
	@Schema(description = "领取条件")
	private String getCondition;


}
