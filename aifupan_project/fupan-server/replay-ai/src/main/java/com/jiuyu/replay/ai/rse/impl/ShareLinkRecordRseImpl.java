package com.jiuyu.replay.ai.rse.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.jiuyu.replay.ai.repository.service.ShareLinkRecordService;
import com.jiuyu.replay.common.alibaba.AiOssUtils;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;

import com.jiuyu.replay.ai.vo.ShareLinkRecordListVo;
import com.jiuyu.replay.ai.vo.ShareLinkRecordInfoVo;
import com.jiuyu.replay.ai.bo.ShareLinkRecordBo;
import com.jiuyu.replay.ai.bo.ShareLinkRecordListBo;

import com.jiuyu.replay.ai.entity.ShareLinkRecordEntity;
import com.jiuyu.replay.ai.rse.ShareLinkRecordRse;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.annotation.Resource;
import jodd.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.beans.BeanUtils;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


/**
 * 分享链接记录
 *
 * @author lyw
 * @email 1798883178@qq.com
 * @date 2025-05-21 18:39:53
 */
@Slf4j
@Service
public class ShareLinkRecordRseImpl implements ShareLinkRecordRse {

    @Resource
    private ShareLinkRecordService shareLinkRecordService;
    @Resource
    private AiOssUtils aiOssUtils;


    @Override
    public PageUtils<ShareLinkRecordListVo> queryPage(ShareLinkRecordListBo shareLinkRecordListBo) {
        QueryWrapper<ShareLinkRecordEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(shareLinkRecordListBo.getKeyword())){
            wrapper.like(StrUtil.isNotBlank(shareLinkRecordListBo.getKeyword()),"name", shareLinkRecordListBo.getKeyword());
            wrapper.eq("url_status",0);
        }

        IPage<ShareLinkRecordEntity> iPage = shareLinkRecordService.page(new Query<ShareLinkRecordEntity>().getPage(shareLinkRecordListBo.getPage(), shareLinkRecordListBo.getLimit()), wrapper);

        PageUtils<ShareLinkRecordListVo> pageUtils = new PageUtils<>(shareLinkRecordListBo.getPage(), shareLinkRecordListBo.getLimit(), iPage);

        List<ShareLinkRecordEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<ShareLinkRecordListVo> vos = records.stream().map(item -> {
                ShareLinkRecordListVo shareLinkRecordVo = new ShareLinkRecordListVo();
                BeanUtils.copyProperties(item, shareLinkRecordVo);
                return shareLinkRecordVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public ShareLinkRecordInfoVo info(Long id) {

        ShareLinkRecordEntity shareLinkRecordEntity = shareLinkRecordService.getById(id);
        if(shareLinkRecordEntity != null) {
            ShareLinkRecordInfoVo shareLinkRecordInfoVo = new ShareLinkRecordInfoVo();
            BeanUtils.copyProperties(shareLinkRecordEntity, shareLinkRecordInfoVo);
            return shareLinkRecordInfoVo;
        }

        return null;
    }





    @Override
    public ShareLinkRecordInfoVo save(ShareLinkRecordBo shareLinkRecordBo) {

         ShareLinkRecordEntity shareLinkRecordEntity = new ShareLinkRecordEntity();
         BeanUtils.copyProperties(shareLinkRecordBo, shareLinkRecordEntity);
         shareLinkRecordEntity.setId(SnowflakeManager.nextValue());
         shareLinkRecordEntity.setShareTime(new Date());
         shareLinkRecordEntity.setUrlStatus(0);
         shareLinkRecordEntity.setCreateDate(new Date());
         shareLinkRecordEntity.setUpdateDate(new Date());

         shareLinkRecordService.save(shareLinkRecordEntity);

         ShareLinkRecordInfoVo shareLinkRecordInfoVo = new ShareLinkRecordInfoVo();
         BeanUtils.copyProperties(shareLinkRecordEntity, shareLinkRecordInfoVo);
         shareLinkRecordInfoVo.setId(shareLinkRecordEntity.getId());
         return shareLinkRecordInfoVo;
     }

    @Override
    public void update(ShareLinkRecordBo shareLinkRecordBo) {

        ShareLinkRecordEntity shareLinkRecordEntity = new ShareLinkRecordEntity();
        BeanUtils.copyProperties(shareLinkRecordBo, shareLinkRecordEntity);
        shareLinkRecordEntity.setUpdateDate(new Date());

        shareLinkRecordService.updateById(shareLinkRecordEntity);
    }

    @Override
    public void deleteById(Long id) {

        shareLinkRecordService.removeById(id);
    }


    @Override
    public void updateUrlExpire(String value) {
        if (value!=null){
            Integer integer = Integer.valueOf(value);
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime targetTime = now.minusDays(integer);
            List<ShareLinkRecordEntity> list = shareLinkRecordService.lambdaQuery().lt(ShareLinkRecordEntity::getShareTime, targetTime)
                    .eq(ShareLinkRecordEntity::getUrlStatus, 0)
                    .list();
            if (Objects.nonNull(list)&& !list.isEmpty()){
                list.forEach(x -> {
                    x.setUrlStatus(1);
                    Date date = new Date();
                    x.setExpireTime(date);
                    x.setUpdateDate(new Date());
                });
                boolean updated = shareLinkRecordService.updateBatchById(list);
                log.info("批量修改url失效的记录：{}",updated);
            }
        }

    }



}

