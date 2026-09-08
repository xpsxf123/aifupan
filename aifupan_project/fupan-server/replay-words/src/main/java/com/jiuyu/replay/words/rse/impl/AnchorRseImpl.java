package com.jiuyu.replay.words.rse.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jiuyu.replay.common.utils.BeanConvertUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.generic.bo.words.anchor.SetAnchorTradeBo;
import com.jiuyu.replay.generic.bo.words.anchor.SubAnchorListBo;
import com.jiuyu.replay.generic.bo.words.anchor.UpdateAnchorBaseInfoBo;
import com.jiuyu.replay.generic.bo.words.anchor.UpdateAnchorUserBo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.words.AnchorUrlInfoVo;
import com.jiuyu.replay.generic.vo.words.AnchorUrlUserVo;
import com.jiuyu.replay.words.entity.AnchorUrlEntity;
import com.jiuyu.replay.words.entity.AnchorUrlUserEntity;
import com.jiuyu.replay.words.entity.AnchorVideoEntity;
import com.jiuyu.replay.words.repository.service.AnchorUrlService;
import com.jiuyu.replay.words.repository.service.AnchorUrlUserService;
import com.jiuyu.replay.words.repository.service.AnchorVideoService;
import com.jiuyu.replay.words.rse.AnchorRse;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 主播相关的RSE实现类
 *
 * @author AI Assistant
 */
@Service
public class AnchorRseImpl implements AnchorRse {

    @Resource
    private AnchorUrlUserService anchorUrlUserService;

    @Resource
    private AnchorUrlService anchorUrlService;
    @Resource
    private AnchorVideoService anchorVideoService;

    @Override
    public PageUtils<AnchorUrlUserVo> getSubUserAnchorList(SubAnchorListBo subAnchorListBo) {

        LambdaQueryWrapper<AnchorUrlUserEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AnchorUrlUserEntity::getUserId, subAnchorListBo.getUserId());
        wrapper.eq(AnchorUrlUserEntity::getTenantId, subAnchorListBo.getTenantId());
        wrapper.eq(AnchorUrlUserEntity::getIsRemoveRecord, 0);
        wrapper.orderByDesc(AnchorUrlUserEntity::getLastRecordTime);
        if (subAnchorListBo.getAccountType() != null) {
            wrapper.eq(AnchorUrlUserEntity::getAccountType, subAnchorListBo.getAccountType());
        }

        IPage<AnchorUrlUserEntity> iPage = anchorUrlUserService.page(new Query<AnchorUrlUserEntity>().getPageNoSort(subAnchorListBo.getPage(), subAnchorListBo.getLimit()), wrapper);

        PageUtils<AnchorUrlUserVo> pageUtils = new PageUtils<>(subAnchorListBo.getPage(), subAnchorListBo.getLimit(), iPage);

        List<AnchorUrlUserEntity> anchorUrlUserEntities = iPage.getRecords();
        if(anchorUrlUserEntities == null || anchorUrlUserEntities.isEmpty()) {
            return pageUtils;
        }

        // 获取所有的secUid
        List<String> secUids = anchorUrlUserEntities.stream().map(AnchorUrlUserEntity::getAnchorUrlSecUid).collect(Collectors.toList());

        // 根据secUid查询主播信息
        List<AnchorUrlEntity> anchorUrlEntities = anchorUrlService.list(new QueryWrapper<AnchorUrlEntity>().in("sec_uid", secUids));
        final Map<String, AnchorUrlEntity> anchorUrlMap;
        if (anchorUrlEntities != null && !anchorUrlEntities.isEmpty()) {
            anchorUrlMap = anchorUrlEntities.stream().collect(Collectors.toMap(AnchorUrlEntity::getSecUid, Function.identity(), (o1, o2) -> o1));
        } else {
            anchorUrlMap = Map.of();
        }

        // 获取主播录制的视频数量
        LambdaQueryWrapper<AnchorVideoEntity> videoWrapper = new LambdaQueryWrapper<>();
        videoWrapper.in(AnchorVideoEntity::getSecUid, secUids);
        videoWrapper.eq(AnchorVideoEntity::getUserId, subAnchorListBo.getUserId());
        videoWrapper.eq(AnchorVideoEntity::getTenantId, subAnchorListBo.getTenantId());
        videoWrapper.ge(AnchorVideoEntity::getDuration, 60);
        videoWrapper.eq(AnchorVideoEntity::getIsRecording, 0);
        videoWrapper.select(AnchorVideoEntity::getSecUid);
        List<AnchorVideoEntity> anchorVideoEntities = anchorVideoService.list(videoWrapper);
        final Map<String, Long> anchorVideoMap;
        if (anchorVideoEntities != null && !anchorVideoEntities.isEmpty()) {
            anchorVideoMap = anchorVideoEntities.stream().collect(Collectors.groupingBy(AnchorVideoEntity::getSecUid, Collectors.counting()));
        } else {
            anchorVideoMap = Map.of();
        }

        List<AnchorUrlUserVo> vos = anchorUrlUserEntities.stream().map(entity -> {
            AnchorUrlUserVo vo = new AnchorUrlUserVo();
            BeanUtils.copyProperties(entity, vo);

            if(StringUtils.isEmpty(vo.getLastRecordTime()) || "2000-01-01 00:00:00".equals(vo.getLastRecordTime())) {
                vo.setLastRecordTime("-");
            }

            // 封装主播信息
            AnchorUrlEntity anchorUrlEntity = anchorUrlMap.get(entity.getAnchorUrlSecUid());
            if (anchorUrlEntity != null) {
                AnchorUrlInfoVo anchorUrlInfoVo = new AnchorUrlInfoVo();
                BeanUtils.copyProperties(anchorUrlEntity, anchorUrlInfoVo);
                // 如果有备注名称，使用备注名称作为主播名称
                if (entity.getRemarksName() != null && !entity.getRemarksName().isEmpty()) {
                    anchorUrlInfoVo.setAnchorName(entity.getRemarksName());
                }
                vo.setAnchorInfo(anchorUrlInfoVo);
            }

            // 封装录制的视频数量
            Long videoNum = anchorVideoMap.getOrDefault(entity.getAnchorUrlSecUid(), 0L);
            vo.setVideoCount(videoNum.intValue());

            return vo;
        }).collect(Collectors.toList());

        pageUtils.setList(vos);

        return pageUtils;
    }

    @Override
    public List<AnchorUrlInfoVo> listBySecUids(Collection<String> secUidList) {

        if(secUidList == null || secUidList.isEmpty()) {
            return null;
        }

        List<AnchorUrlEntity> anchorUrlEntities = this.anchorUrlService.list(new LambdaQueryWrapper<AnchorUrlEntity>().in(AnchorUrlEntity::getSecUid, secUidList));
        if(anchorUrlEntities != null && !anchorUrlEntities.isEmpty()) {
            return BeanConvertUtils.convertList(anchorUrlEntities, AnchorUrlInfoVo.class);
        }

        return null;
    }

    @Override
    public void updateAnchorBaseInfo(UpdateAnchorBaseInfoBo updateAnchorBaseInfoBo) {
        AnchorUrlEntity anchorUrlEntity = this.anchorUrlService.getOne(new LambdaQueryWrapper<AnchorUrlEntity>().eq(AnchorUrlEntity::getSecUid, updateAnchorBaseInfoBo.getSecUid()));
        if(anchorUrlEntity != null) {
            if(StringUtils.hasLength(updateAnchorBaseInfoBo.getAnchorName())) {
                anchorUrlEntity.setAnchorName(updateAnchorBaseInfoBo.getAnchorName());
            }
            if(StringUtils.hasLength(updateAnchorBaseInfoBo.getAnchorNumber())) {
                anchorUrlEntity.setAnchorNumber(updateAnchorBaseInfoBo.getAnchorNumber());
            }
            if(StringUtils.hasLength(updateAnchorBaseInfoBo.getAnchorAvatar())) {
                anchorUrlEntity.setAnchorAvatar(updateAnchorBaseInfoBo.getAnchorAvatar());
            }
            if(StringUtils.hasLength(updateAnchorBaseInfoBo.getAnchorUserId())) {
                anchorUrlEntity.setAnchorUserId(updateAnchorBaseInfoBo.getAnchorUserId());
            }
            anchorUrlEntity.setUpdateDate(new Date());
            this.anchorUrlService.updateById(anchorUrlEntity);
        }
    }

    @Override
    public void updateUserAnchorInfo(UpdateAnchorUserBo updateAnchorUserBo) {
        LambdaQueryWrapper<AnchorUrlUserEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AnchorUrlUserEntity::getAnchorUrlSecUid, updateAnchorUserBo.getSecUid());
        wrapper.eq(AnchorUrlUserEntity::getUserId, updateAnchorUserBo.getUserId());
        wrapper.eq(AnchorUrlUserEntity::getTenantId, updateAnchorUserBo.getTenantId());
        AnchorUrlUserEntity anchorUrlUserEntity = this.anchorUrlUserService.getOne(wrapper);

        if(anchorUrlUserEntity != null) {
            BeanUtils.copyProperties(updateAnchorUserBo, anchorUrlUserEntity);
            anchorUrlUserEntity.setUpdateDate(new Date());

            // 当修改授权巨量百应状态时，自动更新状态修改时间
            if(updateAnchorUserBo.getAuthJlbyStatus() != null) {
                anchorUrlUserEntity.setAuthJlbyStatusTime(new Date());
            }

            // 当修改千川授权状态时，自动更新状态修改时间
            if(updateAnchorUserBo.getAuthQcStatus() != null) {
                anchorUrlUserEntity.setAuthQcStatusTime(new Date());
            }

            // 当修改授权来客状态时，自动更新状态修改时间
            if(updateAnchorUserBo.getAuthLifeStatus() != null) {
                anchorUrlUserEntity.setAuthLifeStatusTime(new Date());
            }

            this.anchorUrlUserService.updateById(anchorUrlUserEntity);
        }

    }

    @Override
    public List<String> listRecentAnchorSecUids(int pageNum, int pageSize) {
        LambdaQueryWrapper<AnchorUrlEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(AnchorUrlEntity::getId)
                .select(AnchorUrlEntity::getSecUid);

        Page<AnchorUrlEntity> page = anchorUrlService.page(new Page<>(pageNum, pageSize), wrapper);
        List<AnchorUrlEntity> records = page.getRecords();

        if (records == null || records.isEmpty()) {
            return new ArrayList<>();
        }

        return records.stream()
                .map(AnchorUrlEntity::getSecUid)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public void setAnchorTrade(SetAnchorTradeBo setAnchorTradeBo) {
        AnchorUrlEntity anchorUrlEntity = this.anchorUrlService.getOne(
                new LambdaQueryWrapper<AnchorUrlEntity>().eq(AnchorUrlEntity::getSecUid, setAnchorTradeBo.getSecUid()));
        if (anchorUrlEntity != null) {
            Date now = new Date();
            
            // 设置系统行业时，自动更新修改时间
            if (setAnchorTradeBo.getSystemTradeId() != null) {
                anchorUrlEntity.setSystemTradeId(setAnchorTradeBo.getSystemTradeId());
                anchorUrlEntity.setUpdateSystemTradeDate(now);
            }
            
            // 设置AI纠正行业时，自动更新添加时间
            if (setAnchorTradeBo.getAiCorrectTradeId() != null) {
                anchorUrlEntity.setAiCorrectTradeId(setAnchorTradeBo.getAiCorrectTradeId());
                anchorUrlEntity.setAddAiTradeDate(now);
            }
            
            anchorUrlEntity.setUpdateDate(now);
            this.anchorUrlService.updateById(anchorUrlEntity);
        }
    }
}
