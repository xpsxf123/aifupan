package com.jiuyu.replay.common.bo;


import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import com.jiuyu.replay.common.bo.PageBo;

/**
 * 系统配置的键值对列表查询参数
 *
 * @author lj
 * @email 
 * @date 2025-05-13 16:59:53
 */
@Data
@Schema(description = "系统配置的键值对列表查询参数")
public class SystemKvListBo extends PageBo implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 模糊搜索查询条件
	 */
	@Schema(description = "模糊搜索查询条件")
	private String keyword;

}
