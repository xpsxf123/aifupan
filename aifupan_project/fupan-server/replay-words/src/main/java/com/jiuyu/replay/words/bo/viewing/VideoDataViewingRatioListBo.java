package com.jiuyu.replay.words.bo.viewing;


import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import com.jiuyu.replay.common.bo.PageBo;

/**
 * 数据看盘比例列表查询参数
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-04-17 17:45:33
 */
@Data
@Schema(description = "数据看盘比例列表查询参数")
public class VideoDataViewingRatioListBo extends PageBo implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 模糊搜索查询条件
	 */
	@Schema(description = "模糊搜索查询条件")
	private String keyword;

}
