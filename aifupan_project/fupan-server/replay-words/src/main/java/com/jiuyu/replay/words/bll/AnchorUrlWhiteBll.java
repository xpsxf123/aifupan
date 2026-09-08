package com.jiuyu.replay.words.bll;

import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.AnchorUrlWhiteBo;
import com.jiuyu.replay.words.entity.AnchorUrlWhiteEntity;
import com.jiuyu.replay.words.producer.AnchorUrlWhiteProducer;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;


/**
 * 主播白名单表
 *
 * @author hts
 * @email 1776764427@qq.com
 * @date 2024-09-15 09:50:55
 */
@Component
public class AnchorUrlWhiteBll {

    @Resource
    private AnchorUrlWhiteProducer anchorUrlWhiteProducer;


    /**
     * 客户查询用户是否有录制该主播
     * @param secUid
     * @param id
     * @return
     */
    public R<List<String>> seletBysecUidAnchorUrlWhite( List<String> secUid, Long id) {
        return anchorUrlWhiteProducer.seletBysecUidAnchorUrlWhite(secUid,id);
    }

    /**
     * 主播保存用户白名单
     * @param anchorUrlWhiteBo
     * @return
     */
    public R<String> saveAnchorUrlWhite(AnchorUrlWhiteBo anchorUrlWhiteBo) {
        return  anchorUrlWhiteProducer.saveAnchorUrlWhite(anchorUrlWhiteBo);
    }

    /**
     * 服务端主播列表删除用户白名单
     * @param anchorUrlWhiteBo
     * @return
     */
    public R<String> removeAnchorUrlWhite(AnchorUrlWhiteBo anchorUrlWhiteBo) {
        return anchorUrlWhiteProducer.removeAnchorUrlWhite(anchorUrlWhiteBo);
    }

    /**
     * 服务端查询查询主播的白名单
     * @param secUid
     * @return
     */
    public List<Long> seletUidAnchorUrlWhite(String secUid) {
        return anchorUrlWhiteProducer.seletUidAnchorUrlWhite(secUid);
    }

    /***
     * 客户根据ssecUid查询用户是否有录制该主播
     * @param secUidS
     * @return
     */
    public R<Boolean> seletBySerId(String secUidS ,Long userId) {
        return  anchorUrlWhiteProducer.seletBySerId(secUidS,userId);
    }

    /**
     * 添加主播至用户白名单
     * @param userId
     * @param secUid
     * @return
     */
    public R<String> saveAnchorInUserWhite(Long userId,String secUid) {
        return anchorUrlWhiteProducer.saveAnchorInUserWhite(userId,secUid);
    }


    /**
     * 查询该主播的所属用户信息（白名单）
     * @param secUid
     * @return
     */
    public R<List<AnchorUrlWhiteEntity>> selectUserByAnchorWhite(String secUid) {
        return anchorUrlWhiteProducer.selectUserByAnchorWhite(secUid);
    }


    /**
     * 删除主播白名单
     * @param anchorUrlWhiteBos
     * @return
     */
    public R<String> removeAnchorWhite(List<AnchorUrlWhiteBo> anchorUrlWhiteBos) {
        return anchorUrlWhiteProducer.removeAnchorWhite(anchorUrlWhiteBos);
    }
}

