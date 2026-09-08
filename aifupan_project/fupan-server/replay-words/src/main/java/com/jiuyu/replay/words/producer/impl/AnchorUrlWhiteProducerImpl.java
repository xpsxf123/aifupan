package com.jiuyu.replay.words.producer.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.AnchorUrlWhiteBo;
import com.jiuyu.replay.words.entity.AnchorUrlWhiteEntity;
import com.jiuyu.replay.words.producer.AnchorUrlWhiteProducer;
import com.jiuyu.replay.words.repository.service.AnchorUrlWhiteService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * 主播白名单表
 *
 * @author hts
 * @email 1776764427@qq.com
 * @date 2024-09-15 09:50:55
 */
@Service
public class AnchorUrlWhiteProducerImpl implements AnchorUrlWhiteProducer {

    @Resource
    private AnchorUrlWhiteService anchorUrlWhiteService;

    /**
     * 客户查询用户是否有录制该主播
     * @param secUid
     * @param userId
     * @return
     */
    @Override
    public R<List<String>> seletBysecUidAnchorUrlWhite( List<String> secUid, Long userId) {
        QueryWrapper<AnchorUrlWhiteEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("sec_uid", secUid);
        List<AnchorUrlWhiteEntity> list = anchorUrlWhiteService.list(queryWrapper);
         List<String>white= new ArrayList<>();

        if(list != null && list.size() > 0 ) {
            List<String> lis = list.stream()
                    .filter(entity -> entity.getUserId().equals(userId))
                    .map(AnchorUrlWhiteEntity::getSecUid).toList();
            white.addAll(lis);
        }

        if(white!=null && white.size() > 0) {
            List<String> list1 = list.stream().map(AnchorUrlWhiteEntity::getSecUid).toList();
            secUid.removeAll(list1);
            white.addAll(list1);
            return R.ok(white);
        }

        return R.ok(secUid);

    }


    /**
     * 主播保存用户白名单
     * @param anchorUrlWhiteBo
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<String> saveAnchorUrlWhite(AnchorUrlWhiteBo anchorUrlWhiteBo) {

        QueryWrapper<AnchorUrlWhiteEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("sec_uid", anchorUrlWhiteBo.getSecUid());
        queryWrapper.in("user_id", anchorUrlWhiteBo.getUserId());
        anchorUrlWhiteService.remove(queryWrapper);
        List<Long> userId = anchorUrlWhiteBo.getUserId();
        List<AnchorUrlWhiteEntity> list = userId.stream().map(item -> {
            AnchorUrlWhiteEntity anchorUrlWhiteEntity = new AnchorUrlWhiteEntity();
            anchorUrlWhiteEntity.setSecUid(anchorUrlWhiteBo.getSecUid());
            anchorUrlWhiteEntity.setUserId(item);
            anchorUrlWhiteEntity.setId(SnowflakeManager.nextValue());
            anchorUrlWhiteEntity.setCreateDate(new Date());
            return anchorUrlWhiteEntity;
        }).toList();
        anchorUrlWhiteService.saveBatch(list);
        return R.ok("添加成功");
    }

    /**
     * 服务端主播列表删除用户白名单
     * @param anchorUrlWhiteBo
     * @return
     */
    @Override
    public R<String> removeAnchorUrlWhite(AnchorUrlWhiteBo anchorUrlWhiteBo) {
        QueryWrapper<AnchorUrlWhiteEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("sec_uid", anchorUrlWhiteBo.getSecUid());
        queryWrapper.in("user_id", anchorUrlWhiteBo.getUserId());
        anchorUrlWhiteService.remove(queryWrapper);
        return R.ok();
    }

    /**
     * 服务端查询查询主播的白名单
     * @param secUid
     * @return
     */
    @Override
    public List<Long> seletUidAnchorUrlWhite(String secUid) {
        QueryWrapper<AnchorUrlWhiteEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("sec_uid",secUid);
        List<AnchorUrlWhiteEntity> list = anchorUrlWhiteService.list(queryWrapper);
        if(list!=null && list.size() > 0 ) {
            return list.stream().map(AnchorUrlWhiteEntity::getUserId).toList();
        }
        return null;
    }

    /**
     *客户根据ssecUid查询用户是否有录制该主播
     * @param secUidS
     * @param userId
     * @return
     */
    @Override
    public R<Boolean> seletBySerId(String secUidS, Long userId) {
        QueryWrapper<AnchorUrlWhiteEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("sec_uid",secUidS);
        List<AnchorUrlWhiteEntity> list = anchorUrlWhiteService.list(queryWrapper);
        if(list != null && list.size() > 0 ) {
            List<AnchorUrlWhiteEntity> list1 = list.stream().filter(entity -> entity.getUserId().equals(userId)).toList();
            if(list1 !=null && list1.size() > 0 ) {
                return R.ok(true);
            }else {
                return R.ok(false);
            }
        }
        return R.ok(true);

    }

    /**
     * 添加主播至用户白名单
     * @param userId
     * @param secUid
     * @return
     */
    @Override
    public R<String> saveAnchorInUserWhite(Long userId,String secUid) {

        AnchorUrlWhiteEntity anchorUrlWhiteEntity = new AnchorUrlWhiteEntity();

            anchorUrlWhiteEntity.setUserId(userId);
            anchorUrlWhiteEntity.setSecUid(secUid);
            anchorUrlWhiteEntity.setId(SnowflakeManager.nextValue());
            anchorUrlWhiteEntity.setCreateDate(new Date());
            this.anchorUrlWhiteService.save(anchorUrlWhiteEntity);


        return R.ok("添加成功");

    }


    /**
     * 查询该主播的所属用户信息（白名单）
     *
     * @param secUid
     * @return
     */
    @Override
    public R<List<AnchorUrlWhiteEntity>> selectUserByAnchorWhite(String secUid) {

        List<AnchorUrlWhiteEntity> whiteEntityList = this.anchorUrlWhiteService.list(new QueryWrapper<AnchorUrlWhiteEntity>().eq("sec_uid", secUid));
        return R.ok(whiteEntityList);
    }

    /**
     * 删除主播白名单
     * @param anchorUrlWhiteBos
     * @return
     */
    @Override
    public R<String> removeAnchorWhite(List<AnchorUrlWhiteBo> anchorUrlWhiteBos) {

        QueryWrapper<AnchorUrlWhiteEntity> wrapper = new QueryWrapper<>();
        for (AnchorUrlWhiteBo bo : anchorUrlWhiteBos) {
            wrapper.eq("sec_uid",bo.getSecUid());
            wrapper.eq("user_id",bo.getUserId());
            this.anchorUrlWhiteService.remove(wrapper);
        }

        return R.ok("删除成功");

    }

}

