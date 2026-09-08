package com.jiuyu.replay.generic.vo.aiagent;

import lombok.Data;

import java.io.Serializable;

/**
 * 折线数据点。
 *
 * @author fupan-server
 */
@Data
public class CurvePointVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 时间戳
     */
    private Long dateTime;

    /**
     * 数值
     */
    private Integer valueNum;
}
