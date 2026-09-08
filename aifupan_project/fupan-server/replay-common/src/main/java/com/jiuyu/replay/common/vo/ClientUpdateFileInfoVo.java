package com.jiuyu.replay.common.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class ClientUpdateFileInfoVo implements Serializable {
    private static final long serialVersionUID = 1L;

    private String cosKey;
    private String md5;
    private String zipPath;
    private Long size;
}
