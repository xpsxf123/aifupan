package com.jiuyu.replay.power.vo.crm;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * CRM AI画像持久化结果
 */
@Data
public class CrmAiProfileInfoVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String profileId;
    private String source;
    private Date updatedAt;
    private String profileJson;
    private String summary;
    private Integer isDeleted;
    private Date createDate;
    private Date updateDate;
}
