package com.jiuyu.replay.generic.bo.third;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

import java.io.Serializable;

@Data
public class ChanmamaQueryBo implements Serializable {

    private static final long serialVersionUID = 1L;

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

}
