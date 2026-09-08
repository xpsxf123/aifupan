package com.jiuyu.replay.generic.vo.aiagent;

import lombok.Data;

import java.io.Serializable;

/**
 * 主播行业聚合项（ID + 名称去重）。
 *
 * @author fupan-server
 */
@Data
public class TradeOptionVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 行业 ID
     */
    private Long tradeId;

    /**
     * 行业名称
     */
    private String tradeName;
}
