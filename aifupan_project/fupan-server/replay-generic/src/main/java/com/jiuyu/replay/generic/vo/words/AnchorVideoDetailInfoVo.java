package com.jiuyu.replay.generic.vo.words;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 视频的详情信息项
 *
 * @author lj
 * @email 
 * @date 2025-05-26 16:34:59
 */
@Data
@Schema(description = "视频的详情信息项")
public class AnchorVideoDetailInfoVo extends AnchorVideoDetailVo implements Serializable {
	private static final long serialVersionUID = 1L;

    @Schema(description = "视频")
    private AnchorVideoInfoVo video;

}
