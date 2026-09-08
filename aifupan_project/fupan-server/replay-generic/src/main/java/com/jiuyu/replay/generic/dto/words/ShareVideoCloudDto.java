package com.jiuyu.replay.generic.dto.words;

import lombok.Data;

import java.io.Serializable;

@Data
public class ShareVideoCloudDto  implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 视频id
     */
    public Long id;
    /**
     * 视频id
     */
    public String videoId;
    /**
     * 视频在线播放地址
     */
    public String onlineFileUrl;
    /**
     * 云空间备注
     */
    private String cloudRemarks;
    /**
     * 云空间重命名
     */
    private String cloudRename;

}
