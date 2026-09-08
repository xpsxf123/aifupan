package com.jiuyu.replay.words.producer.impl;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.words.bo.UploadFileAnalysisBo;
import com.jiuyu.replay.words.bo.UploadFileAnalysisListBo;
import com.jiuyu.replay.words.entity.UploadFileAnalysisEntity;
import com.jiuyu.replay.words.entity.UploadFileAnalysisRecordEntity;
import com.jiuyu.replay.words.entity.UploadFileEntity;
import com.jiuyu.replay.words.entity.UploadFileRecodEntity;
import com.jiuyu.replay.words.producer.UploadFileAnalysisProducer;
import com.jiuyu.replay.words.repository.service.UploadFileAnalysisRecordService;
import com.jiuyu.replay.words.repository.service.UploadFileAnalysisService;
import com.jiuyu.replay.words.repository.service.UploadFileRecodService;
import com.jiuyu.replay.words.repository.service.UploadFileService;
import com.jiuyu.replay.words.vo.OnlineAnalysisItemVo;
import com.jiuyu.replay.words.vo.UploadFileAnalysisVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * @author tisheng
 * @date 2024/9/5
 * @apinNote
 */
@Service
public class UploadFileAnalysisProducerImpl implements UploadFileAnalysisProducer {


    @Resource
    private UploadFileAnalysisRecordService uploadFileAnalysisRecordService;

    @Resource
    UploadFileAnalysisService uploadFileAnalysisService;
    @Resource
    UploadFileRecodService uploadFileRecodService;
    @Resource
    UploadFileService uploadFileService;


    /**
     * 保存文件分析内容
     * @param uploadFileAnalysisVo
     * @return
     */
    @Override
    public R<String> saveuploadFileAnalysis(List<UploadFileAnalysisVo> uploadFileAnalysisVo) {
        //查询是否分析过
        QueryWrapper<UploadFileAnalysisEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("file_id", uploadFileAnalysisVo.get(0).getFileId());
        List<UploadFileAnalysisEntity> list1 = uploadFileAnalysisService.list(queryWrapper);
        if(list1!=null&&list1.size()>0){
            //将上传分析的内容逻辑删除
            List<UploadFileAnalysisEntity> list = list1.stream().map(item -> {
                item.setIsDeleted(1);
                return item;
            }).toList();
            uploadFileAnalysisService.saveBatch(list);
        }
        List<UploadFileAnalysisEntity> list = uploadFileAnalysisVo.stream().map(item -> {
            UploadFileAnalysisEntity uploadFileAnalysisEntity = new UploadFileAnalysisEntity();
            BeanUtils.copyProperties(item, uploadFileAnalysisEntity);
            uploadFileAnalysisEntity.setId(SnowflakeManager.nextValue());
            return uploadFileAnalysisEntity;
        }).toList();
        uploadFileAnalysisService.saveBatch(list);
        return R.ok();
    }

    /**
     * 根据文件id 查询分析内容
     * @param fileId
     * @return
     */
    @Override
    public R<List<UploadFileAnalysisVo>> seletByFileId(String fileId) {
        QueryWrapper<UploadFileAnalysisEntity> wrapper =new QueryWrapper<>();
        wrapper.eq("file_id", fileId);
        wrapper.eq("is_deleted", 0);
        List<UploadFileAnalysisEntity> list = uploadFileAnalysisService.list(wrapper);
        List<UploadFileAnalysisVo> lis =   new ArrayList<>();
        if (list != null && list.size() > 0) {
            lis = list.stream().map(item -> {
                UploadFileAnalysisVo uploadFileAnalysisVo = new UploadFileAnalysisVo();
                BeanUtils.copyProperties(item, uploadFileAnalysisVo);
                String dataJson = item.getDataJson();
                JSONObject jsonObject = JSONObject.parseObject(dataJson);
                String content = jsonObject.getString("content");
                uploadFileAnalysisVo.setDataJson(content);
                return uploadFileAnalysisVo;
            }).toList();
        }
        return R.ok(lis);
    }

    /**
     * 客户获取复盘分析内容
     * @param uploadFileAnalysisListBo
     * @return
     */
    @Override
    public R<List<UploadFileAnalysisVo>> seletContent(UploadFileAnalysisListBo uploadFileAnalysisListBo) {
        QueryWrapper<UploadFileAnalysisEntity> queryWrapper = new QueryWrapper<>();

        queryWrapper.eq("file_id", uploadFileAnalysisListBo.getFileId());
        queryWrapper.eq("trade_id", uploadFileAnalysisListBo.getTradeId());

        List<UploadFileAnalysisEntity> list = uploadFileAnalysisService.list(queryWrapper);

        if(list != null && list.size() > 0) {
            Optional<UploadFileAnalysisEntity> maxVersionEntity = list.stream()
                    .max(Comparator.comparingInt(UploadFileAnalysisEntity::getVersion));

            List<UploadFileAnalysisEntity> collect = list.stream()
                    .filter(entity -> entity.getVersion().equals(maxVersionEntity.get().getVersion()))
                    .toList();


            List<UploadFileAnalysisVo> lis =   new ArrayList<>();
            if (collect != null && collect.size() > 0) {
                collect.stream().forEach(item -> {
                    UploadFileAnalysisVo uploadFileAnalysisVo = new UploadFileAnalysisVo();
                    BeanUtils.copyProperties(item, uploadFileAnalysisVo);
                    lis.add(uploadFileAnalysisVo);
                });
            }
            return R.ok(lis);
        }

        return R.ok();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<String> saveFileAnalysis(List<UploadFileAnalysisBo> list) {
        List<UploadFileAnalysisEntity> listAn = new ArrayList<>();
        QueryWrapper<UploadFileAnalysisEntity> queryWrapp = new QueryWrapper<>();
        queryWrapp.eq("file_id", list.get(0).getFileId());
        queryWrapp.eq("trade_id",list.get(0).getTradeId())
                .orderByDesc("version")
                .last("limit 1");
        UploadFileAnalysisEntity one1 = uploadFileAnalysisService.getOne(queryWrapp);
        int version=0;
        if(one1!=null){
            version = one1.getVersion()+1;
        }

        if(list.size()>0){
            for(UploadFileAnalysisBo item : list ){
                UploadFileAnalysisEntity uploadFileAnalysisEntity = new UploadFileAnalysisEntity();
                BeanUtils.copyProperties(item, uploadFileAnalysisEntity);
                uploadFileAnalysisEntity.setId(SnowflakeManager.nextValue());
                uploadFileAnalysisEntity.setVersion(version);
                listAn.add(uploadFileAnalysisEntity);
            }
            uploadFileAnalysisService.saveBatch(listAn);

            QueryWrapper<UploadFileEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("file_id", list.get(0).getFileId());
            UploadFileEntity one = uploadFileService.getOne(queryWrapper);
            UploadFileRecodEntity uploadFileRecodEntity = new UploadFileRecodEntity();
            BeanUtils.copyProperties(one, uploadFileRecodEntity);
            uploadFileRecodEntity.setId(SnowflakeManager.nextValue());
            uploadFileRecodEntity.setUploadTime(new Date());
            uploadFileRecodService.save(uploadFileRecodEntity);
        }
        return R.ok();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearAnalysis(String fileId) {
        // 删除分析数据（旧）
        this.uploadFileAnalysisService.remove(new QueryWrapper<UploadFileAnalysisEntity>().eq("file_id", fileId));
        // 删除分析记录
        this.uploadFileAnalysisRecordService.remove(new QueryWrapper<UploadFileAnalysisRecordEntity>().eq("file_id", fileId));
    }

    @Override
    public List<OnlineAnalysisItemVo> listByFileIdAndTradeId(String fileId, Long tradeId) {
        QueryWrapper<UploadFileAnalysisEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("file_id", fileId);
        wrapper.eq("trade_id", tradeId);
        List<UploadFileAnalysisEntity> uploadFileAnalysisEntities = this.uploadFileAnalysisService.list(wrapper);

        if(uploadFileAnalysisEntities != null && uploadFileAnalysisEntities.size() > 0) {
            Optional<UploadFileAnalysisEntity> maxVersionEntity = uploadFileAnalysisEntities.stream().max(Comparator.comparingInt(UploadFileAnalysisEntity::getVersion));

            List<UploadFileAnalysisEntity> analysisList = uploadFileAnalysisEntities.stream()
                    .filter(item -> item.getVersion().equals(maxVersionEntity.get().getVersion()))
                    .sorted(Comparator.comparingInt(UploadFileAnalysisEntity::getParagraph)).toList();

            List<OnlineAnalysisItemVo> analysisItemVoList = analysisList.stream().map(item -> {
                OnlineAnalysisItemVo onlineAnalysisItemVo = new OnlineAnalysisItemVo();
                onlineAnalysisItemVo.setId(item.getId());
                onlineAnalysisItemVo.setFileUuid(item.getFileId());
                onlineAnalysisItemVo.setTradeId(Long.valueOf(item.getTradeId()));
                onlineAnalysisItemVo.setParagraph(item.getParagraph());
                onlineAnalysisItemVo.setStatus(item.getStatus());
                onlineAnalysisItemVo.setDataJson(item.getDataJson());
                return onlineAnalysisItemVo;
            }).toList();

            return analysisItemVoList;
        }

        return null;
    }

    @Override
    public List<UploadFileAnalysisVo> listAnalysisOneByFileId(String fileId) {
        List<UploadFileAnalysisEntity> uploadFileAnalysisEntities = this.uploadFileAnalysisService.list(
                new QueryWrapper<UploadFileAnalysisEntity>().eq("file_id", fileId).eq("version", 0));
        if(uploadFileAnalysisEntities != null && uploadFileAnalysisEntities.size() > 0) {
            List<UploadFileAnalysisVo> uploadFileAnalysisVos = uploadFileAnalysisEntities.stream().map(item -> {
                UploadFileAnalysisVo audioAnalysisVo = new UploadFileAnalysisVo();
                BeanUtils.copyProperties(item, audioAnalysisVo);
                return audioAnalysisVo;
            }).sorted(Comparator.comparingInt(UploadFileAnalysisVo::getParagraph)).toList();

            return uploadFileAnalysisVos;
        }
        return null;
    }
}
