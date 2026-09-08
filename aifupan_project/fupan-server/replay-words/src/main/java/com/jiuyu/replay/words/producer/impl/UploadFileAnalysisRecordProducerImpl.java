package com.jiuyu.replay.words.producer.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.common.utils.ReplayFileUtils;
import com.jiuyu.replay.words.bo.UploadFileAnalysisRecordBo;
import com.jiuyu.replay.words.bo.UploadFileAnalysisRecordListBo;
import com.jiuyu.replay.words.constant.WordsProperties;
import com.jiuyu.replay.words.entity.UploadFileAnalysisRecordEntity;
import com.jiuyu.replay.words.producer.UploadFileAnalysisRecordProducer;
import com.jiuyu.replay.words.repository.service.UploadFileAnalysisRecordService;
import com.jiuyu.replay.generic.vo.words.SentenceMarkVo;
import com.jiuyu.replay.words.vo.UploadFileAnalysisRecordInfoVo;
import com.jiuyu.replay.words.vo.UploadFileAnalysisRecordListVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 文件的分析记录
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-12-03 11:26:50
 */
@Service
public class UploadFileAnalysisRecordProducerImpl implements UploadFileAnalysisRecordProducer {

    @Resource
    private UploadFileAnalysisRecordService uploadFileAnalysisRecordService;
    @Resource
    private WordsProperties wordsProperties;


    @Override
    public PageUtils<UploadFileAnalysisRecordListVo> queryPage(UploadFileAnalysisRecordListBo uploadFileAnalysisRecordListBo) {
        QueryWrapper<UploadFileAnalysisRecordEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(uploadFileAnalysisRecordListBo.getKeyword())){
            wrapper.like("name", uploadFileAnalysisRecordListBo.getKeyword());
        }

        IPage<UploadFileAnalysisRecordEntity> iPage = uploadFileAnalysisRecordService.page(new Query<UploadFileAnalysisRecordEntity>().getPage(uploadFileAnalysisRecordListBo.getPage(), uploadFileAnalysisRecordListBo.getLimit()), wrapper);

        PageUtils<UploadFileAnalysisRecordListVo> pageUtils = new PageUtils<>(uploadFileAnalysisRecordListBo.getPage(), uploadFileAnalysisRecordListBo.getLimit(), iPage);

        List<UploadFileAnalysisRecordEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<UploadFileAnalysisRecordListVo> vos = records.stream().map(item -> {
                UploadFileAnalysisRecordListVo uploadFileAnalysisRecordVo = new UploadFileAnalysisRecordListVo();
                BeanUtils.copyProperties(item, uploadFileAnalysisRecordVo);
                return uploadFileAnalysisRecordVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public UploadFileAnalysisRecordInfoVo info(Long id) {

        UploadFileAnalysisRecordEntity uploadFileAnalysisRecordEntity = uploadFileAnalysisRecordService.getById(id);
        if(uploadFileAnalysisRecordEntity != null) {
            UploadFileAnalysisRecordInfoVo uploadFileAnalysisRecordInfoVo = new UploadFileAnalysisRecordInfoVo();
            BeanUtils.copyProperties(uploadFileAnalysisRecordEntity, uploadFileAnalysisRecordInfoVo);
            return uploadFileAnalysisRecordInfoVo;
        }

        return null;
    }

    /**
     * 新增文件的分析记录
     * @param uploadFileAnalysisRecordBo 文件的分析记录对象
     * @return
     */
     public UploadFileAnalysisRecordInfoVo save(UploadFileAnalysisRecordBo uploadFileAnalysisRecordBo) {

         UploadFileAnalysisRecordEntity uploadFileAnalysisRecordEntity = new UploadFileAnalysisRecordEntity();
         BeanUtils.copyProperties(uploadFileAnalysisRecordBo, uploadFileAnalysisRecordEntity);
//         uploadFileAnalysisRecordEntity.setId(SnowflakeManager.nextValue());
         uploadFileAnalysisRecordEntity.setCreateDate(new Date());
         uploadFileAnalysisRecordEntity.setUpdateDate(new Date());

         uploadFileAnalysisRecordService.save(uploadFileAnalysisRecordEntity);

         UploadFileAnalysisRecordInfoVo uploadFileAnalysisRecordInfoVo = new UploadFileAnalysisRecordInfoVo();
         BeanUtils.copyProperties(uploadFileAnalysisRecordEntity, uploadFileAnalysisRecordInfoVo);

         return uploadFileAnalysisRecordInfoVo;
     }

    /**
     * 修改文件的分析记录
     * @param uploadFileAnalysisRecordBo 文件的分析记录对象
     * @return
     */
    public void update(UploadFileAnalysisRecordBo uploadFileAnalysisRecordBo) {

        UploadFileAnalysisRecordEntity uploadFileAnalysisRecordEntity = new UploadFileAnalysisRecordEntity();
        BeanUtils.copyProperties(uploadFileAnalysisRecordBo, uploadFileAnalysisRecordEntity);
        uploadFileAnalysisRecordEntity.setUpdateDate(new Date());

        uploadFileAnalysisRecordService.updateById(uploadFileAnalysisRecordEntity);
    }

    /**
     * 删除文件的分析记录
     * @param id 文件的分析记录id
     * @return
     */
    public void deleteById(Long id) {

        uploadFileAnalysisRecordService.removeById(id);
    }

    @Override
    public int getLastVersion(String fileId) {

        QueryWrapper<UploadFileAnalysisRecordEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("file_id", fileId);
        wrapper.orderByDesc("version");
        wrapper.last(" limit 1");
        UploadFileAnalysisRecordEntity uploadFileAnalysisRecordEntity = this.uploadFileAnalysisRecordService.getOne(wrapper);
        if(uploadFileAnalysisRecordEntity != null) {
            return uploadFileAnalysisRecordEntity.getVersion() + 1;
        }
        return 0;
    }

    @Override
    public UploadFileAnalysisRecordInfoVo getLastInfo(String fileId) {

        QueryWrapper<UploadFileAnalysisRecordEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("file_id", fileId);
        wrapper.orderByDesc("version");
        wrapper.last(" limit 1");
        UploadFileAnalysisRecordEntity uploadFileAnalysisRecordEntity = this.uploadFileAnalysisRecordService.getOne(wrapper);
        if(uploadFileAnalysisRecordEntity != null) {
            UploadFileAnalysisRecordInfoVo uploadFileAnalysisRecordInfoVo = new UploadFileAnalysisRecordInfoVo();
            BeanUtils.copyProperties(uploadFileAnalysisRecordEntity, uploadFileAnalysisRecordInfoVo);
            return uploadFileAnalysisRecordInfoVo;
        }

        return null;
    }

    @Override
    public UploadFileAnalysisRecordInfoVo infoLastByFileIdAndTradeId(String fileId, Long tradeId) {

        QueryWrapper<UploadFileAnalysisRecordEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("file_id", fileId);
        wrapper.eq("trade_id", tradeId);
        wrapper.orderByDesc("version");
        wrapper.last(" limit 1");
        UploadFileAnalysisRecordEntity uploadFileAnalysisRecordEntity = this.uploadFileAnalysisRecordService.getOne(wrapper);
        if(uploadFileAnalysisRecordEntity != null) {

            UploadFileAnalysisRecordInfoVo uploadFileAnalysisRecordInfoVo = new UploadFileAnalysisRecordInfoVo();
            BeanUtils.copyProperties(uploadFileAnalysisRecordEntity, uploadFileAnalysisRecordInfoVo);
            return uploadFileAnalysisRecordInfoVo;

        }

        return null;
    }

    @Override
    public UploadFileAnalysisRecordInfoVo infoByVideoIdAndVersion(String fileId, int version) {

        QueryWrapper<UploadFileAnalysisRecordEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("file_id", fileId);
        wrapper.eq("version", version);
        wrapper.last(" limit 1");
        UploadFileAnalysisRecordEntity uploadFileAnalysisRecordEntity = this.uploadFileAnalysisRecordService.getOne(wrapper);
        if(uploadFileAnalysisRecordEntity != null) {

            UploadFileAnalysisRecordInfoVo uploadFileAnalysisRecordInfoVo = new UploadFileAnalysisRecordInfoVo();
            BeanUtils.copyProperties(uploadFileAnalysisRecordEntity, uploadFileAnalysisRecordInfoVo);
            return uploadFileAnalysisRecordInfoVo;

        }

        return null;
    }

    @Override
    public R<List<SentenceMarkVo>> selectAnalysisByFileId(String fileId, Long tradeId) {
        if (fileId != null && tradeId != null) {
            UploadFileAnalysisRecordInfoVo uploadFileAnalysisRecordInfoVo = this.infoLastByFileIdAndTradeId(fileId, tradeId);
            String fileContent = ReplayFileUtils.getFileContent(wordsProperties.getFileAnalysisStorePath() + uploadFileAnalysisRecordInfoVo.getStoreFileName());
            List<SentenceMarkVo> sentenceMarkVos = new ArrayList<>();
            if (!StringUtils.isEmpty(fileContent)){
                sentenceMarkVos = JSON.parseArray(fileContent, SentenceMarkVo.class);
                sentenceMarkVos.sort((o1,o2) -> Integer.compare(o1.getCurrentSort(),o2.getCurrentSort()));
            }
            return R.ok(sentenceMarkVos);
        }
        return null;
    }

    @Override
    public void saveNewFilePath(Long id, String storeFileNameNew) {
        UploadFileAnalysisRecordEntity uploadFileAnalysisRecordEntity = new UploadFileAnalysisRecordEntity();
        uploadFileAnalysisRecordEntity.setId(id);
        uploadFileAnalysisRecordEntity.setStoreFileNameNew(storeFileNameNew);
        uploadFileAnalysisRecordEntity.setUpdateDate(new Date());
        this.uploadFileAnalysisRecordService.updateById(uploadFileAnalysisRecordEntity);
    }

    @Override
    public UploadFileAnalysisRecordInfoVo listByFileIdAndTradeId(String fileId, Long tradeId) {
        QueryWrapper<UploadFileAnalysisRecordEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("file_id", fileId);
        wrapper.eq("trade_id", tradeId);
        UploadFileAnalysisRecordEntity analysisRecordServiceOne = this.uploadFileAnalysisRecordService.getOne(wrapper);
        if(analysisRecordServiceOne != null) {
            UploadFileAnalysisRecordInfoVo fileAnalysisRecordInfoVo = new UploadFileAnalysisRecordInfoVo();
            BeanUtils.copyProperties(analysisRecordServiceOne, fileAnalysisRecordInfoVo);
            return fileAnalysisRecordInfoVo;
        }

        return null;
    }

    /**
     * 获取昨天的文件分析记录
     * @return
     */
    @Override
    public List<UploadFileAnalysisRecordEntity> yesterdayDateRecord() {
        // 获取当前时间
        LocalDateTime now = LocalDateTime.now();
        // 获取昨天的日期
        LocalDateTime yesterday = now.minusDays(1);
        // 获取昨天的开始时间（即 00:00:00）
        LocalDateTime startOfYesterday = yesterday.withHour(0).withMinute(0).withSecond(0).withNano(0);
        // 设置时间为昨天的最后一秒（即 23:59:59）
        LocalDateTime endOfYesterday = yesterday.withHour(23).withMinute(59).withSecond(59).withNano(0);
        QueryWrapper<UploadFileAnalysisRecordEntity> wrapper = new QueryWrapper<>();
        wrapper.between("create_date", startOfYesterday, endOfYesterday);
        List<UploadFileAnalysisRecordEntity> recordEntities = this.uploadFileAnalysisRecordService.list(wrapper);
        if (recordEntities != null && recordEntities.size() > 0) {
            return recordEntities;
        }
        return null;
    }

    @Override
    public void saveCosKey(Long id, String cosSaveKey) {
        UploadFileAnalysisRecordEntity uploadFileAnalysisRecordEntity = new UploadFileAnalysisRecordEntity();
        uploadFileAnalysisRecordEntity.setId(id);
        uploadFileAnalysisRecordEntity.setStoreFileOssKey(cosSaveKey);
        uploadFileAnalysisRecordEntity.setUpdateDate(new Date());
        this.uploadFileAnalysisRecordService.updateById(uploadFileAnalysisRecordEntity);
    }

    @Override
    public List<UploadFileAnalysisRecordInfoVo> listByFileIds(List<String> fileIds) {

        QueryWrapper<UploadFileAnalysisRecordEntity> wrapper = new QueryWrapper<>();
        wrapper.in("file_id", fileIds);

        List<UploadFileAnalysisRecordEntity> uploadFileAnalysisRecordEntities = this.uploadFileAnalysisRecordService.list(wrapper);

        if(uploadFileAnalysisRecordEntities != null && uploadFileAnalysisRecordEntities.size() > 0) {
            List<UploadFileAnalysisRecordInfoVo> fileAnalysisRecordInfoVos = uploadFileAnalysisRecordEntities.stream().map(item -> {
                UploadFileAnalysisRecordInfoVo fileAnalysisRecordInfoVo = new UploadFileAnalysisRecordInfoVo();
                BeanUtils.copyProperties(item, fileAnalysisRecordInfoVo);
                return fileAnalysisRecordInfoVo;
            }).collect(Collectors.toList());

            return fileAnalysisRecordInfoVos;
        }

        return null;
    }

}