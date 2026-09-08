package com.jiuyu.replay.words.repository.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import com.jiuyu.replay.common.constant.WordsEnum;
import com.jiuyu.replay.generic.vo.words.AnchorVideoDetailVo;
import com.jiuyu.replay.generic.vo.words.anchor.AnchorUrlTradeVo;
import com.jiuyu.replay.words.entity.VideoContentEntity;
import com.jiuyu.replay.words.repository.mongo.VideoContentRepository;
import com.jiuyu.replay.generic.vo.words.VideoContentVo;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.jiuyu.replay.words.repository.dao.AnchorVideoDetailDao;
import com.jiuyu.replay.words.entity.AnchorVideoDetailEntity;
import com.jiuyu.replay.words.repository.service.AnchorVideoDetailService;

import java.util.List;


@Service("anchorVideoDetailService")
public class AnchorVideoDetailServiceImpl extends ServiceImpl<AnchorVideoDetailDao, AnchorVideoDetailEntity> implements AnchorVideoDetailService {

    @Resource
    private VideoContentRepository videoContentRepository;
    @Resource
    private AnchorVideoDetailDao albumVideoDetailDao;


    @Override
    public List<VideoContentVo> selVideoContents(String videoId, Integer type,Integer sourceType,Long userId,Long tenantId) {

        List<VideoContentEntity> entities = selectData(userId, tenantId, videoId, sourceType, type);
        if (!entities.isEmpty()){
            List<VideoContentVo> videoContentVos = BeanUtil.copyToList(entities, VideoContentVo.class);
            videoContentVos.forEach(x->{
                x.setSourceId(videoId);
                x.setSourceType(sourceType);
            });
            return videoContentVos;
        }
        return List.of();
    }


    @Override
    public void saveVideoContents(List<VideoContentVo> voList,Long userId,Long tenantId,String videoId,Integer sourceType, Integer type){
        List<VideoContentEntity> entities = selectData(userId, tenantId, videoId, sourceType, type);
        if (!entities.isEmpty()){
            entities.forEach(x->{
                x.setIsDeleted(1);
            });
            videoContentRepository.saveAll(entities);
        }
        if (!voList.isEmpty()){
            for (VideoContentVo item : voList) {
                item.setGenerateStatus(WordsEnum.contentGenerateStatus.SUCCESS.getCode());
                item.setCueWord(null);
            }
            List<VideoContentEntity> list = Convert.toList(VideoContentEntity.class, voList);
            videoContentRepository.saveAll(list);
        }

    }


    /**
     * 获取视频已分析完成的需要生成的自然/优化原文的数据
     * @param limit
     * @return
     */
    @Override
    public List<AnchorVideoDetailVo> selectVideoDetailData( Integer limit) {
        List<AnchorVideoDetailEntity> anchorVideoDetailEntities = albumVideoDetailDao.selectVideoDetailData(limit);
        if (ObjectUtil.isNotEmpty(anchorVideoDetailEntities)){
            return BeanUtil.copyToList(anchorVideoDetailEntities, AnchorVideoDetailVo.class);
        }
        return List.of();
    }

    private List<VideoContentEntity> selectData(Long userId, Long tenantId, String videoId, Integer sourceType, Integer type) {
        VideoContentEntity exampleEntity = new VideoContentEntity();
        exampleEntity.setSourceId(videoId);
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
    public void inserto(AnchorVideoDetailEntity anchorVideoDetailEntity) {
        baseMapper.inserto(anchorVideoDetailEntity);
    }

    @Override
    public List<AnchorUrlTradeVo> tradeListBySecUids(List<String> secUids) {
        return baseMapper.tradeListBySecUids(secUids);
    }
}