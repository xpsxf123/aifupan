package com.jiuyu.replay.words.repository.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.vo.words.VideoContentVo;
import com.jiuyu.replay.words.entity.VideoContentEntity;
import com.jiuyu.replay.words.repository.mongo.VideoContentRepository;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.jiuyu.replay.words.repository.dao.UploadFileDetailDao;
import com.jiuyu.replay.words.entity.UploadFileDetailEntity;
import com.jiuyu.replay.words.repository.service.UploadFileDetailService;

import java.util.Date;
import java.util.List;


@Service("uploadFileDetailService")
public class UploadFileDetailServiceImpl extends ServiceImpl<UploadFileDetailDao, UploadFileDetailEntity> implements UploadFileDetailService {

    @Resource
    private VideoContentRepository videoContentRepository;


    @Override
    public List<VideoContentVo> selFileContents(String sourceId, Integer type, Integer sourceType, Long userId, Long tenantId) {
        List<VideoContentEntity> entities = selectData(userId, tenantId, sourceId, sourceType, type);
        if (!entities.isEmpty()){
            List<VideoContentVo> videoContentVos = BeanUtil.copyToList(entities, VideoContentVo.class);
            videoContentVos.forEach(x->{
                x.setSourceId(sourceId);
                x.setSourceType(sourceType);
            });
            return videoContentVos;
        }
        return List.of();
    }

    @Override
    public void inserto(String sourceId, Integer type, Integer sourceType, Long userId, Long tenantId) {
        UploadFileDetailEntity uploadFileDetailEntity = new UploadFileDetailEntity();
        uploadFileDetailEntity.setId(SnowflakeManager.nextValue());
        uploadFileDetailEntity.setCreateDate(new Date());
        uploadFileDetailEntity.setUpdateDate(new Date());
        uploadFileDetailEntity.setTenantId(tenantId);
        uploadFileDetailEntity.setFileId(sourceId);
        uploadFileDetailEntity.setUserId(userId);
        uploadFileDetailEntity.setHasDiagnosisReport(0);
        uploadFileDetailEntity.setNatureContentStatus(0);
        uploadFileDetailEntity.setOptimizeContentStatus(0);
        baseMapper.inserto(uploadFileDetailEntity);
    }

    private List<VideoContentEntity> selectData(Long userId, Long tenantId, String sourceId, Integer sourceType, Integer type) {
        VideoContentEntity exampleEntity = new VideoContentEntity();
        exampleEntity.setSourceId(sourceId);
        exampleEntity.setType(type);
        exampleEntity.setUserId(userId);
        exampleEntity.setTenantId(tenantId);
        exampleEntity.setSourceType(sourceType);
        exampleEntity.setIsDeleted(0);

        // 创建匹配器，忽略 null 值和空字符串
        ExampleMatcher matcher = ExampleMatcher.matching()
                .withIgnoreNullValues()
                .withStringMatcher(ExampleMatcher.StringMatcher.EXACT);

        Example<VideoContentEntity> example = Example.of(exampleEntity, matcher);
        // 添加排序
        Sort sort = Sort.by(Sort.Direction.ASC, "paragraph");
        return videoContentRepository.findAll(example,sort);
    }

    @Override
    public List<UploadFileDetailEntity> selectFileDetailData(Integer limit) {
        return baseMapper.selectFileDetailData(limit);
    }
}