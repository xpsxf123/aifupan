package com.jiuyu.replay.generic.bo.power;


import com.jiuyu.replay.generic.bo.common.PageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户跟进销售人员表列表查询参数
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-01-21 10:16:13
 */
@Data
@Schema(description = "用户跟进销售人员表列表查询参数")
public class SalesListBo extends PageBo implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 模糊搜索查询条件
	 */
	@Schema(description = "模糊搜索查询条件")
	private String keyword;

    @Schema(description = "销售类型 0平台销售，1代理商销售")
    private Integer salesType;

    @Schema(description = "代理商id")
    private Long agentId;

    @Schema(description = "员工状态 0离职  1在职")
    private Integer employeeStatus;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "用户id")
    private Long userId;

}
