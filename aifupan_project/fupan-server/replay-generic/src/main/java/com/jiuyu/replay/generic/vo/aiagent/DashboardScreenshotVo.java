package com.jiuyu.replay.generic.vo.aiagent;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 数据看板-截图识别内容项。
 *
 * <p>无巨量/蝉妈妈看板数据时的兜底：取 tb_data_screenshot 中该视频已识别完成（screenshotStatus=3）的截图，
 * 按「截图 code + AI 识别内容」组装。</p>
 *
 * @author fupan-server
 */
@Data
public class DashboardScreenshotVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 数据截图 code
     */
    @Schema(description = "数据截图 code")
    private String screenshotCode;

    /**
     * AI 识别内容
     */
    @Schema(description = "AI 识别内容")
    private String content;
}
