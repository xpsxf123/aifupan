package com.jiuyu.replay.words.vo.viewing;


import com.jiuyu.replay.words.bo.oceanEngine.OceanEngineProcessBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 视频看盘混淆后的数据信息项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-04-12 16:31:15
 */
@Data
@Schema(description = "视频看盘混淆后的数据信息项")
public class VideoDataViewingConfuseInfoVo extends VideoDataViewingConfuseVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 性别分布数据
	 */
	@Schema(description = "性别分布数据")
	private List<DataViewingDetailVo> sexDistributionList;
	/**
	 * 年龄分布数据
	 */
	@Schema(description = "年龄分布数据")
	private List<DataViewingDetailVo> ageDistributionList;
	/**
	 * 流量结构分布数据
	 */
	@Schema(description = "流量结构分布数据")
	private List<DataViewingDetailVo> trafficStructureDistributionList;

    @Schema(description = "巨量的实时数据")
    private List<OceanEngineProcessBo> oceanEngineProcessList;

	/**
	 * 开始时间
	 */
	@Schema(description = "开始时间")
	private String dataStartTime;
	/**
	 * 截止时间
	 */
	@Schema(description = "截止时间")
	private String dataEndTime;
}
