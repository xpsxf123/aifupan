package com.jiuyu.replay.generic.vo.aiagent;

import lombok.Data;

import java.io.Serializable;

/**
 * 爆款关键词项。
 *
 * @author fupan-server
 */
@Data
public class HotSearchKeywordVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 爆款搜索 id（爆款视频列表接口用它定位）
     */
    private Long searchId;

    /**
     * 平台类型：1-抖音 2-快手 3-视频号
     */
    private Byte platformType;

    /**
     * 搜索关键词
     */
    private String searchKeyword;

    /**
     * 视频总数
     */
    private Integer videoCount;

    /**
     * 最后同步时间（yyyy-MM-dd HH:mm:ss）
     */
    private String lastSyncTime;
}
