package com.jiuyu.replay.third.bo.chanmama;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

@Data
public class ChanmamaQueryBo {

    /**
     * 抖音号
     */
    @JSONField(name = "live_name")
    private String liveName;

    /**
     * 查询的开始时间
     */
    @JSONField(name = "begin_date")
    private String beginDate;

    /**
     * 查询的结束时间
     */
    @JSONField(name = "end_date")
    private String endDate;

    /**
     * 回调地址
     */
    @JSONField(name = "call_back")
    private String callBack;

    /**
     * 请求ID
     */
    private String requestId;

    /**
     * 主播secUid
     */
    private String secUid;

}
