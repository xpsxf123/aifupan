package com.jiuyu.replay.power.bo.crm;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * CRM 阶段事件命中受控映射后的销售快照更新参数
 */
@Data
public class CrmBusinessSnapshotUpdateBo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;

    private Integer accordingStatus;

    private String accordingContent;

    private Date accordingDate;
}
