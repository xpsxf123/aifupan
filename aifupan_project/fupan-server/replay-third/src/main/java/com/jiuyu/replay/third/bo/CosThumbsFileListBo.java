package com.jiuyu.replay.third.bo;


import com.jiuyu.replay.common.bo.PageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 点赞问答文件上传cos记录表列表查询参数
 *
 * @author DearSil
 * @email 2437495924@qq.com
 * @date 2025-02-21 16:50:31
 */
@Data
@Schema(description = "点赞问答文件上传cos记录表列表查询参数")
public class CosThumbsFileListBo extends PageBo implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 模糊搜索查询条件
	 */
	@Schema(description = "模糊搜索查询条件")
	private String keyword;

}
