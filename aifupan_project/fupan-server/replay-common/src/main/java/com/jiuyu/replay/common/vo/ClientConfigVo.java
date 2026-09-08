package com.jiuyu.replay.common.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author ：lujie
 * @description：客户端的配置信息
 * @date ：2025/2/8 下午3:25
 */
@Data
public class ClientConfigVo {

    @Schema(description = "websocket方式 获取方式 0:js计算，1浏览器获取")
    private Integer websocketWay;

    @Schema(description = "弹幕保存最大数量")
    private Integer maxBarrageNum;

    @Schema(description = "是否显示由腾讯云AI技术支持图标  0不显示  1显示")
    private Integer showTencentAiIcon;
}
