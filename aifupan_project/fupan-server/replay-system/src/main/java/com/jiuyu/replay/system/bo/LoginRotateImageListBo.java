package com.jiuyu.replay.system.bo;


import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import com.jiuyu.replay.common.bo.PageBo;

/**
 * 客户端登录页轮播图列表查询参数
 *
 * @author lyw
 * @email 1798883178@qq.com
 * @date 2025-05-22 15:27:24
 */
@Data
@Schema(description = "客户端登录页轮播图列表查询参数")
public class LoginRotateImageListBo extends PageBo implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 模糊搜索查询条件
	 */
	@Schema(description = "模糊搜索查询条件")
	private String keyword;

}
