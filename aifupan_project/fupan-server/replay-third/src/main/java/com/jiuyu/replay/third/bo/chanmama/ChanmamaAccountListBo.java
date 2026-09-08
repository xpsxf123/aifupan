package com.jiuyu.replay.third.bo.chanmama;


import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import com.jiuyu.replay.common.bo.PageBo;

/**
 * 第三方数据平台账号列表查询参数
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-04-14 15:59:03
 */
@Data
@Schema(description = "第三方数据平台账号列表查询参数")
public class ChanmamaAccountListBo extends PageBo implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 模糊搜索查询条件
	 */
	@Schema(description = "模糊搜索查询条件")
	private String keyword;

}
