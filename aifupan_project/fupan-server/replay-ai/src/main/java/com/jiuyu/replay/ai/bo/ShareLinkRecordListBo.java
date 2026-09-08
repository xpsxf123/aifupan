package com.jiuyu.replay.ai.bo;


import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import com.jiuyu.replay.common.bo.PageBo;

/**
 * 分享链接记录列表查询参数
 *
 * @author lyw
 * @email 1798883178@qq.com
 * @date 2025-05-21 18:39:53
 */
@Data
@Schema(description = "分享链接记录列表查询参数")
public class ShareLinkRecordListBo extends PageBo implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 模糊搜索查询条件
	 */
	@Schema(description = "模糊搜索查询条件")
	private String keyword;

}
