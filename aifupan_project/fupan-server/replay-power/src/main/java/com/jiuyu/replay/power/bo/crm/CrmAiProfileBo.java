package com.jiuyu.replay.power.bo.crm;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * CRM AI画像持久化写入对象
 */
@Data
public class CrmAiProfileBo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private String profileId;
    private String source;
    private Date updatedAt;
    private String profileJson;
    private String summary;
}
