package com.jiuyu.replay.generic.dto.words;

import lombok.Data;

import java.io.Serializable;

@Data
public class ShareContrastCloudDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 对比id
     */
    public Long id;
    /**
     * 对比唯一标识
     */
    public String contrastId;
    /**
     * 云空间备注
     */
    private String cloudRemarks;

}
