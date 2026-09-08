package com.jiuyu.replay.words.bo;


import com.jiuyu.replay.common.bo.PageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 数据截图记录列表查询参数
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2025-03-11 17:30:33
 */
@Data
@Schema(description = "数据截图记录列表查询参数")
public class DataScreenshotListBo extends PageBo implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(description = "数据类型 0视频，1文件，2对比分析")
	private Integer sourceType;

	@Schema(description = "来源id")
	private String sourceId;

	@Schema(description = "用户id")
	private Long userId;

	@Schema(description = "租户id")
	private Long tenantId;

	@Schema(description = "状态；0:图片未上传，1：图片已上传，2：ai识别中，3：ai识别完成，4：ai识别失败")
	private Integer screenshotStatus;
}
