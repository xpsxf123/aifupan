package com.jiuyu.replay.words.producer;

import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.AnchorUrlWhiteBo;
import com.jiuyu.replay.words.entity.AnchorUrlWhiteEntity;

import java.util.List;

/**
 * 主播白名单表
 *
 * @author hts
 * @email 1776764427@qq.com
 * @date 2024-09-15 09:50:55
 */
public interface AnchorUrlWhiteProducer {

    /**
     * 客户查询用户是否有录制该主播
     * @param secUid
     * @param id
     * @return
     */
    R<List<String>> seletBysecUidAnchorUrlWhite( List<String> secUid, Long id);

    /**
     * 主播保存用户白名单
     * @param anchorUrlWhiteBo
     * @return
     */
    R<String> saveAnchorUrlWhite(AnchorUrlWhiteBo anchorUrlWhiteBo);

    /**
     * 服务端主播列表删除用户白名单
     * @param anchorUrlWhiteBo
     * @return
     */
    R<String> removeAnchorUrlWhite(AnchorUrlWhiteBo anchorUrlWhiteBo);

    /**
     * 服务端查询查询主播的白名单
     * @param secUid
     * @return
     */
    List<Long> seletUidAnchorUrlWhite(String secUid);

    /**
     * 客户根据ssecUid查询用户是否有录制该主播
     * @param secUidS
     * @return
     */
    R<Boolean> seletBySerId(String secUidS,Long userId);

    /**
     * 添加主播至用户白名单
     * @param userId
     * @param secUid
     * @return
     */
    R<String> saveAnchorInUserWhite(Long userId,String secUid);

    /**
     * 查询该主播的所属用户信息（白名单）
     *
     * @param secUid
     * @return
     */
    R<List<AnchorUrlWhiteEntity>> selectUserByAnchorWhite(String secUid);

    /**
     * 删除主播白名单
     * @param anchorUrlWhiteBos
     * @return
     */
    R<String> removeAnchorWhite(List<AnchorUrlWhiteBo> anchorUrlWhiteBos);
}

