package com.jiuyu.replay.third.producer.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.common.utils.SnowflakeManager;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jiuyu.replay.third.bo.CosThumbsFileBo;
import com.jiuyu.replay.third.bo.CosThumbsFileListBo;
import com.jiuyu.replay.third.entity.CosThumbsFileEntity;
import com.jiuyu.replay.third.producer.CosThumbsFileProducer;
import com.jiuyu.replay.third.repository.service.CosThumbsFileService;
import com.jiuyu.replay.third.vo.CosThumbsFileInfoVo;
import com.jiuyu.replay.third.vo.CosThumbsFileListVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.beans.BeanUtils;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 点赞问答文件上传cos记录表
 *
 * @author DearSil
 * @email 2437495924@qq.com
 * @date 2025-02-21 16:50:31
 */
@Service
public class CosThumbsFileProducerImpl implements CosThumbsFileProducer {

    @Resource
    private CosThumbsFileService cosThumbsFileService;


    @Override
    public PageUtils<CosThumbsFileListVo> queryPage(CosThumbsFileListBo cosThumbsFileListBo) {
        QueryWrapper<CosThumbsFileEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(cosThumbsFileListBo.getKeyword())){
            wrapper.like("name", cosThumbsFileListBo.getKeyword());
        }

        IPage<CosThumbsFileEntity> iPage = cosThumbsFileService.page(new Query<CosThumbsFileEntity>().getPage(cosThumbsFileListBo.getPage(), cosThumbsFileListBo.getLimit()), wrapper);

        PageUtils<CosThumbsFileListVo> pageUtils = new PageUtils<>(cosThumbsFileListBo.getPage(), cosThumbsFileListBo.getLimit(), iPage);

        List<CosThumbsFileEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<CosThumbsFileListVo> vos = records.stream().map(item -> {
                CosThumbsFileListVo cosThumbsFileVo = new CosThumbsFileListVo();
                BeanUtils.copyProperties(item, cosThumbsFileVo);
                return cosThumbsFileVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public CosThumbsFileInfoVo info(Long id) {

        CosThumbsFileEntity cosThumbsFileEntity = cosThumbsFileService.getById(id);
        if(cosThumbsFileEntity != null) {
            CosThumbsFileInfoVo cosThumbsFileInfoVo = new CosThumbsFileInfoVo();
            BeanUtils.copyProperties(cosThumbsFileEntity, cosThumbsFileInfoVo);
            return cosThumbsFileInfoVo;
        }

        return null;
    }

    @Override
    public CosThumbsFileInfoVo getCosThumbsFileByContextId(String contextId) {
        CosThumbsFileEntity cosThumbsFileEntity = cosThumbsFileService.getOne(new LambdaQueryWrapper<CosThumbsFileEntity>()
                .eq(CosThumbsFileEntity::getContextId, contextId)
        );
        if(cosThumbsFileEntity != null) {
            CosThumbsFileInfoVo cosThumbsFileInfoVo = new CosThumbsFileInfoVo();
            BeanUtils.copyProperties(cosThumbsFileEntity, cosThumbsFileInfoVo);
            return cosThumbsFileInfoVo;
        }

        return null;
    }

    /**
     * 新增点赞问答文件上传cos记录表
     * @param cosThumbsFileBo 点赞问答文件上传cos记录表对象
     * @return
     */
     public CosThumbsFileInfoVo save(CosThumbsFileBo cosThumbsFileBo) {

         CosThumbsFileEntity cosThumbsFileEntity = new CosThumbsFileEntity();
         BeanUtils.copyProperties(cosThumbsFileBo, cosThumbsFileEntity);
         cosThumbsFileEntity.setId(SnowflakeManager.nextValue());
         cosThumbsFileEntity.setCreateDate(new Date());
         cosThumbsFileEntity.setUpdateDate(new Date());

         cosThumbsFileService.save(cosThumbsFileEntity);

         CosThumbsFileInfoVo cosThumbsFileInfoVo = new CosThumbsFileInfoVo();
         BeanUtils.copyProperties(cosThumbsFileEntity, cosThumbsFileInfoVo);

         return cosThumbsFileInfoVo;
     }

    /**
     * 修改点赞问答文件上传cos记录表
     * @param cosThumbsFileBo 点赞问答文件上传cos记录表对象
     * @return
     */
    public void update(CosThumbsFileBo cosThumbsFileBo) {

        CosThumbsFileEntity cosThumbsFileEntity = new CosThumbsFileEntity();
        BeanUtils.copyProperties(cosThumbsFileBo, cosThumbsFileEntity);
        cosThumbsFileEntity.setUpdateDate(new Date());

        cosThumbsFileService.updateById(cosThumbsFileEntity);
    }

    /**
     * 删除点赞问答文件上传cos记录表
     * @param id 点赞问答文件上传cos记录表id
     * @return
     */
    public void deleteById(Long id) {

        cosThumbsFileService.removeById(id);
    }


    @Override
    public void saveOrUpdate(CosThumbsFileBo cosThumbsFileListBo) {
        RRException.isNotEmpty(cosThumbsFileListBo.getContextId(), "上下文Id不能为空");

        CosThumbsFileEntity serviceOne = cosThumbsFileService.getOne(new LambdaQueryWrapper<CosThumbsFileEntity>()
                .eq(CosThumbsFileEntity::getContextId, cosThumbsFileListBo.getContextId())
        );
        if (serviceOne != null){
            BeanUtil.copyProperties(cosThumbsFileListBo, serviceOne, CopyOptions.create().setIgnoreNullValue(true));
            serviceOne.setAskCount(serviceOne.getAskCount() + 1);
            update(BeanUtil.copyProperties(serviceOne, CosThumbsFileBo.class));
        }else{
            save(cosThumbsFileListBo);
        }
    }
}

