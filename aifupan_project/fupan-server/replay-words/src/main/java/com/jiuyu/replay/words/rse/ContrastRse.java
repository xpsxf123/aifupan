package com.jiuyu.replay.words.rse;

import com.jiuyu.replay.generic.dto.words.ShareContrastCloudDto;
import com.jiuyu.replay.words.vo.SyncContrastInfoVo;

public interface ContrastRse {

    /**
     * 根据对比记录id和用户id获取对比记录
     * @param contrastId 对比记录id
     * @param userId 用户id
     * @return
     */
    SyncContrastInfoVo infoByContrastIdAndUserId(String contrastId, Long userId);

    /**
     * 分析对比记录到云空间
     * @param shareContrastCloudDto 对比记录数据
     * @return 分享地址
     */
    String shareContrastToCloud(ShareContrastCloudDto shareContrastCloudDto);

}
