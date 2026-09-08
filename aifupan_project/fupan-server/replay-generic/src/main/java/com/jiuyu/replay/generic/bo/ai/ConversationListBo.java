package com.jiuyu.replay.generic.bo.ai;


import com.jiuyu.replay.generic.bo.common.PageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 分享链接记录列表查询参数
 *
 * @author lyw
 * @email 1798883178@qq.com
 * @date 2025-05-21 18:39:53
 */
@Data
@Schema(description = "分享链接记录列表查询参数")
public class ConversationListBo extends PageBo implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(description = "来源id")
	private String sourceId;

	@Schema(description = "来源类型 0视频，1文件，2对比分析")
	private Integer sourceType;

	@Schema(description = "用户id")
	private Long userId;

	@Schema(description = "租户id")
	private Long tenantId;

	@Schema(description = "助手类型")
	private Integer type;
}
