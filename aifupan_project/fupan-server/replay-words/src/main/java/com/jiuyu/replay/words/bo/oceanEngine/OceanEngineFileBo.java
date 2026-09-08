package com.jiuyu.replay.words.bo.oceanEngine;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 巨量引擎文件里的实时格式
 *
 * @author liaoxin
 * @date 2025-06-13
 */
@Data
@Schema(description = "巨量引擎文件里的实时格式")
public class OceanEngineFileBo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "视频id")
    private String videoId;

    @Schema(description = "直播标识")
    private String secUid;

    @Schema(description = "直播场次号")
    private String batchNumber;

    @Schema(description = "记录过程")
    private List<OceanEngineProcessBo> juliangRealTimeData;


}