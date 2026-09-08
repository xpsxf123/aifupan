package com.jiuyu.replay.words.bo.video;


import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import com.jiuyu.replay.common.bo.PageBo;

/**
 * 视频的详情列表查询参数
 *
 * @author lj
 * @email 
 * @date 2025-05-26 16:34:59
 */
@Data
@Schema(description = "视频的详情列表查询参数")
public class AnchorVideoDetailListBo extends PageBo implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 模糊搜索查询条件
	 */
	@Schema(description = "模糊搜索查询条件")
	private String keyword;

}
