package com.jiuyu.replay.words.bo;


import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import com.jiuyu.replay.common.bo.PageBo;

/**
 * 福袋信息列表查询参数
 *
 * @author lyw
 * @email 1798883178@qq.com
 * @date 2025-05-22 20:02:21
 */
@Data
@Schema(description = "福袋信息列表查询参数")
public class BlessBagListBo extends PageBo implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 模糊搜索查询条件
	 */
	@Schema(description = "模糊搜索查询条件")
	private String keyword;

}
