package com.jiuyu.replay.words.vo;


import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;

/**
 * 数据截图记录信息项
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2025-03-11 17:30:33
 */
@Data
@Schema(description = "数据截图记录信息项")
public class DataScreenshotInfoVo extends DataScreenshotVo implements Serializable {
	private static final long serialVersionUID = 1L;



}
