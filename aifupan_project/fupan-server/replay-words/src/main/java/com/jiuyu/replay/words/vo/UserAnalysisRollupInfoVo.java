package com.jiuyu.replay.words.vo;


import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;

/**
 * 用户分析汇总表信息项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-01-19 16:56:41
 */
@Data
@Schema(description = "用户分析汇总表信息项")
public class UserAnalysisRollupInfoVo extends UserAnalysisRollupVo implements Serializable {
	private static final long serialVersionUID = 1L;



}
