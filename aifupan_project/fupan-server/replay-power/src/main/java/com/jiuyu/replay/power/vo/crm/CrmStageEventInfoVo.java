package com.jiuyu.replay.power.vo.crm;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * CRM 阶段事件持久化结果
 */
@Data
public class CrmStageEventInfoVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String eventId;
    private String source;
    private Date occurredAt;
    private String stageCode;
    private String stageLabel;
    private BigDecimal confidence;
    private String summary;
    private String factsJson;
    private String rawJson;
    private Date createDate;
}
