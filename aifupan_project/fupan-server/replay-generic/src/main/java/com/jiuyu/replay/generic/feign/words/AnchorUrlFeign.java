package com.jiuyu.replay.generic.feign.words;

import com.jiuyu.replay.generic.vo.words.AnchorUrlInfoVo;
import com.jiuyu.replay.generic.vo.words.AnchorUrlUserVo;

import java.util.List;

/**
 * @Description: 获取主播信息
 */
public interface AnchorUrlFeign {

    AnchorUrlUserVo getUserAnchorBySecUid(String secUid, Long userId, Long tenantId);

    /**
     * 减少数据诊断生成数
     *
     * @param secUid   主播secUid
     * @param videoId  视频id
     * @param userId   用户id
     * @param tenantId 租户id
     * @param num      减少次数
     */
    void minusDataDiagnosisGenerateNum(String secUid, String videoId, Long userId, Long tenantId, int num);

    /**
     * 根据secUid集合批量获取主播信息
     *
     * @param secUids 主播secUid集合
     * @return 主播信息列表
     */
    List<AnchorUrlInfoVo> listBySecUids(List<String> secUids);
}
