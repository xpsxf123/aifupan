package com.jiuyu.replay.words.vo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 用户分析汇总表列表项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-01-19 16:56:41
 */
@Data
@Schema(description = "用户分析汇总表列表项")
public class UserAnalysisRollupListVo extends UserAnalysisRollupVo implements Serializable {
	private static final long serialVersionUID = 1L;



}
