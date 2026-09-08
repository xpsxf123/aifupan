package com.jiuyu.replay.words.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @author tisheng
 * @date 2024/9/15
 * @apinNote
 */
@Data
public class UploadFileAnalysisListBo  implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 视频id
     */
    @Schema(description ="upload_file的id")
    private String fileId;

    /**
     * 行业id
     */
    @Schema(description ="行业id")
    private Long tradeId;


}
