package com.jiuyu.replay.common.bo;


import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import com.jiuyu.replay.common.bo.PageBo;

/**
 * 操作日志表列表查询参数
 *
 * @author lyw
 * @email 1798883178@qq.com
 * @date 2025-06-26 13:59:24
 */
@Data
@Schema(description = "操作日志表列表查询参数")
public class OperationLogListBo extends PageBo implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 模糊搜索查询条件
	 */
	@Schema(description = "模糊搜索查询条件")
	private String keyword;

	/**
	 * 用户userId
	 */
	@Schema(description = "用户userId")
	private Long userId;


	/**
	 * 业务类型(USER_DETAILS/USER/ORDER/OTHER)
	 */
	@Schema(description = "业务类型(USER_DETAILS/USER/ORDER/OTHER)")
	private String businessType ;

}
