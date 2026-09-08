package com.jiuyu.replay.order.bo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class CrmOrderChangedBo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String eventType;
    private Date occurredAt;
    private Long userId;
    private Long salesId;
    private String salesPhone;
    private Long orderId;
}
