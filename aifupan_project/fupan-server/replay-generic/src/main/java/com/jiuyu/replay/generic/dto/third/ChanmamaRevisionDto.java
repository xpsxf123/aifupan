package com.jiuyu.replay.generic.dto.third;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

import java.io.Serializable;

@Data
public class ChanmamaRevisionDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 	请求ID
     */
    private String requestId;
    /**
     * 查询结束时间的日期，如果查询的时间是：2028-08-10 15:33:11格式如：20250810,示例值(20250810)
     */
    private String date;
    /**
     * 	平台类型0蝉妈妈，1考古加,示例值(1)
     */
    private Integer platformType;
    /**
     * 	直播间Id
     */
    @JSONField(name = "room_id")
    private String roomId;
    /**
     * 开始时间,精确到秒，否则报错 时间格式必须是：yyyy-MM-dd HH:mm:ss 不能出现：2025-4-2 15:33:11 月和日必须补0,示例值(2025-04-02 15:16:33)
     */
    @JSONField(name = "begin_date")
    private String beginDate;
    /**
     * 	结束时间 精确到秒，否则报错 时间格式必须是：yyyy-MM-dd HH:mm:ss 不能出现：2025-4-2 15:33:11 月和日必须补0,示例值(2025-04-02 15:16:33)
     */
    @JSONField(name = "end_date")
    private String endDate;
}
