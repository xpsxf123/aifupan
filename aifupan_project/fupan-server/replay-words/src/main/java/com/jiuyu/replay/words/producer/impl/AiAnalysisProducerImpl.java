package com.jiuyu.replay.words.producer.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.common.alibaba.AiOssUtils;
import com.jiuyu.replay.common.constant.RedisCacheKey;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.AiAnalysisBo;
import com.jiuyu.replay.words.bo.AiAnalysisListBo;
import com.jiuyu.replay.words.entity.AiAnalysisEntity;
import com.jiuyu.replay.words.producer.AiAnalysisProducer;
import com.jiuyu.replay.words.repository.service.AiAnalysisService;
import com.jiuyu.replay.generic.vo.words.AiAnalysisInfoVo;
import com.jiuyu.replay.generic.vo.words.AiAnalysisListVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


/**
 * AI分析表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-01-09 15:54:12
 */
@Service
public class AiAnalysisProducerImpl implements AiAnalysisProducer {

    @Resource
    private AiAnalysisService aiAnalysisService;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private AiOssUtils aiOssUtils;

    @Override
    public PageUtils<AiAnalysisListVo> queryPage(AiAnalysisListBo aiAnalysisListBo) {
        QueryWrapper<AiAnalysisEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(aiAnalysisListBo.getKeyword())){
            wrapper.like("name", aiAnalysisListBo.getKeyword());
        }

        IPage<AiAnalysisEntity> iPage = aiAnalysisService.page(new Query<AiAnalysisEntity>().getPage(aiAnalysisListBo.getPage(), aiAnalysisListBo.getLimit()), wrapper);

        PageUtils<AiAnalysisListVo> pageUtils = new PageUtils<>(aiAnalysisListBo.getPage(), aiAnalysisListBo.getLimit(), iPage);

        List<AiAnalysisEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<AiAnalysisListVo> vos = records.stream().map(item -> {
                AiAnalysisListVo aiAnalysisVo = new AiAnalysisListVo();
                BeanUtils.copyProperties(item, aiAnalysisVo);
                return aiAnalysisVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public AiAnalysisInfoVo info(Long id) {

        AiAnalysisEntity aiAnalysisEntity = aiAnalysisService.getById(id);
        if(aiAnalysisEntity != null) {
            AiAnalysisInfoVo aiAnalysisInfoVo = new AiAnalysisInfoVo();
            BeanUtils.copyProperties(aiAnalysisEntity, aiAnalysisInfoVo);
            return aiAnalysisInfoVo;
        }

        return null;
    }

    @Override
    public AiAnalysisInfoVo save(AiAnalysisBo aiAnalysisBo) {

        AiAnalysisEntity aiAnalysisEntity = new AiAnalysisEntity();
        BeanUtils.copyProperties(aiAnalysisBo, aiAnalysisEntity);

        aiAnalysisEntity.setCreateDate(new Date());
        aiAnalysisEntity.setUpdateDate(new Date());
        AiAnalysisInfoVo aiAnalysisInfoVo = new AiAnalysisInfoVo();
        BeanUtils.copyProperties(aiAnalysisEntity, aiAnalysisInfoVo);
        aiAnalysisEntity.setStoreFileName(ObjectUtil.defaultIfNull(aiAnalysisEntity.getStoreFileName(), ""));
        aiAnalysisService.save(aiAnalysisEntity);
        return aiAnalysisInfoVo;
     }

    @Override
    public void update(AiAnalysisBo aiAnalysisBo) {

        AiAnalysisEntity aiAnalysisEntity = new AiAnalysisEntity();
        BeanUtils.copyProperties(aiAnalysisBo, aiAnalysisEntity);
        aiAnalysisEntity.setUpdateDate(new Date());

        aiAnalysisService.updateById(aiAnalysisEntity);
    }

    @Override
    public void deleteById(Long id) {

        aiAnalysisService.removeById(id);
    }

    /**
     * 根据唯一标识获取AI分析最后一条记录
     * @param uuid 唯一标识
     * @return
     */
    @Override
    public int LastSort(String uuid) {
        QueryWrapper<AiAnalysisEntity> wrapper =new QueryWrapper<>();
        wrapper.eq("uuid",uuid);
        List<AiAnalysisEntity> aiAnalysisList = aiAnalysisService.list(wrapper);
        if (!aiAnalysisList.isEmpty()) {
            aiAnalysisList.sort(Comparator.comparingInt(AiAnalysisEntity::getSort));
            AiAnalysisEntity aiAnalysisEntity = aiAnalysisList.get(aiAnalysisList.size() - 1);
            Integer aiSort = aiAnalysisEntity.getSort();
            return ++aiSort;
        }

        return 1;
    }

    /**
     * 根据唯一标识查询所有AI分析记录
     * @param uuid 唯一标识
     * @return
     */
    @Override
    public R<List<List<String>>> listAiAnalysisByUuid(String uuid) {
        List<AiAnalysisEntity> fileAiAnalysisList = aiAnalysisService.list(new QueryWrapper<AiAnalysisEntity>().eq("uuid", uuid));
        List<List<String>> aiAnalysisList = new ArrayList<>();
        fileAiAnalysisList.forEach(item ->{
            if (ObjectUtil.isNotEmpty(item.getOssKey())){
                List<String> list = aiOssUtils.getAiAnalysisData(item.getOssKey());
                if (ObjectUtil.isNotEmpty(list)){
                    aiAnalysisList.add(list);
                }
            }
//            String storeFileName = item.getStoreFileName();
//            File file = new File(storeFileName);
//            try(Scanner scanner = new Scanner(file)) {
//                while (scanner.hasNextLine()){
//                    String line = scanner.nextLine();
//                    List<String> strings = JSON.parseObject(line, new TypeReference<List<String>>() {});
//                    aiAnalysisList.add(strings);
//                }
//            }catch (FileNotFoundException e) {
//                e.printStackTrace();
//            }
        });
        return R.ok(aiAnalysisList);
    }

    /**
     * 根据唯一标识去判断是否有正在进行AI分析的文本;返回true代表正在分析,false则没有
     * @param uuid 唯一标识
     * @return
     */
    @Override
    public String analysisStatusByUuid(String uuid, String modelId) {
        String sessionIDKey = uuid + "sessionIdKey";
        String analysisStatus = (String)redisTemplate.opsForValue().get(RedisCacheKey.getRedisKey(RedisCacheKey.aiSessionIdCacheKey, sessionIDKey, modelId));
        return analysisStatus;
    }
}