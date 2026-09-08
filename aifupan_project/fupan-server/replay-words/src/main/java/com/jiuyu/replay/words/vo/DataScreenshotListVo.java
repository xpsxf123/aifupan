package com.jiuyu.replay.words.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 数据截图记录列表项
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2025-03-11 17:30:33
 */
@Data
@Schema(description = "数据截图记录列表项")
public class DataScreenshotListVo extends DataScreenshotVo implements Serializable {
	private static final long serialVersionUID = 1L;

	@Schema(description = "示例图片地址")
	private String exampleImgUrl;

	@Schema(description = "标题")
	private String title;

}
