package com.jiuyu.replay.common.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author ：liwj
 * @description：
 * @date ：2025/1/2 下午6:56
 */
@Data
@Schema(description = "websocket采集数据错误记录")
public class SendSocketErrorData implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "电脑cpuId")
    private String cpuId;

    @Schema(description = "用户id")
    private Long userId;

    @Schema(description = "主播错误个数")
    private Integer anchorErrorNum;

    @Schema(description = "类型 jsSocketAddress：js计算的websocket地址链接失败，" +
            "browserSocketAddress：浏览器获取的websocket地址连接失败，" +
            "browserGetSocketAddressError：websocket地址获取超时")
    private String type;

    @Schema(description = "当前时间")
    private Date currentDate;
}
