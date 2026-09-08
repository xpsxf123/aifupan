package com.jiuyu.replay.third.vo;


import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;

/**
 * 点赞问答文件上传cos记录表信息项
 *
 * @author DearSil
 * @email 2437495924@qq.com
 * @date 2025-02-21 16:50:31
 */
@Data
@Schema(description = "点赞问答文件上传cos记录表信息项")
public class CosThumbsFileInfoVo extends CosThumbsFileVo implements Serializable {
	private static final long serialVersionUID = 1L;



}
