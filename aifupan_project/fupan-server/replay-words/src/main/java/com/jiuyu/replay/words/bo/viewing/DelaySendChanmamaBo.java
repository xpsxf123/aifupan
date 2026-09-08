package com.jiuyu.replay.words.bo.viewing;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "延迟发送第三方数据平台")
public class DelaySendChanmamaBo {

    /**
     * 查询请求数据JSON
     */
    @Schema(description = "查询请求数据JSON")
    private String chanmamaQueryBoJson;
    /**
     * 查询记录数据JSON
     */
    @Schema(description = "查询记录数据JSON")
    private String chanmamaSendRecordBoJson;
    /**
     * 创建任务时的时间戳
     */
    @Schema(description = "创建任务时的时间戳")
    private Long createTime;
}
