package com.jiuyu.replay.words.producer.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.common.utils.BeanConvertUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.enums.words.VideoSliceTypeEnum;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.video.VideoSliceVo;
import com.jiuyu.replay.generic.vo.words.AiAnalysisSensitiveRelaInfoVo;
import com.jiuyu.replay.words.bo.ClientAiFavListBo;
import com.jiuyu.replay.words.bo.UploadFileAnalysisSaveBo;
import com.jiuyu.replay.words.bo.UploadFileBo;
import com.jiuyu.replay.words.bo.file.ClientFileListBo;
import com.jiuyu.replay.words.bo.file.UpdateFileAnalysisStatusBo;
import com.jiuyu.replay.words.bo.file.UpdateFileTradeBo;
import com.jiuyu.replay.words.bo.file.UploadFileInfoBo;
import com.jiuyu.replay.words.entity.*;
import com.jiuyu.replay.words.producer.UploadFileProducer;
import com.jiuyu.replay.words.repository.service.*;
import com.jiuyu.replay.words.vo.UploadFileVO;
import com.jiuyu.replay.words.vo.file.UploadFileInfoVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author tisheng
 * @date 2024/9/5
 * @apinNote
 */
@Service
public class UploadFileProducerImpl implements UploadFileProducer {

    @Resource
    private UploadFileAnalysisRecordService uploadFileAnalysisRecordService;

    @Resource
    UploadFileService uploadFileService;
    @Resource
    UploadFileAnalysisService uploadFileAnalysisService;

    @Resource
    private SyncContrastService syncContrastService;
    @Resource
    TradeService tradeService;
    @Resource
    private AiAnalysisSensitiveRelaService aiAnalysisSensitiveRelaService;
//    @Resource
//    private UserFeign userFeign;
    @Resource
    private VideoSliceService videoSliceService;


    @Override
    public R<PageUtils<UploadFileVO>> queryPage(UploadFileBo uploadFileBo) {

        QueryWrapper<UploadFileEntity> wrapper = new QueryWrapper<>();
        if (uploadFileBo.getFileName() != null) {
            wrapper.like("file_name", uploadFileBo.getFileName());
        }
        if (uploadFileBo.getStartTime() != null) {
            wrapper.between("upload_time", uploadFileBo.getStartTime(), uploadFileBo.getEndTime());
        }
        //根据用户Id
        if (uploadFileBo.getUserIds() != null && uploadFileBo.getUserIds().size() > 0) {
            wrapper.in("user_id", uploadFileBo.getUserIds());
        }

        IPage<UploadFileEntity> iPage = uploadFileService.page(new Query<UploadFileEntity>().getPage(uploadFileBo.getPage(), uploadFileBo.getLimit()), wrapper);

        PageUtils<UploadFileVO> pageUtils = new PageUtils<>(uploadFileBo.getPage(), uploadFileBo.getLimit(), iPage);
        List<UploadFileEntity> records = iPage.getRecords();
        if (records != null && records.size() > 0) {
            //行业
            List<Long> tradeIds = records.stream().map(UploadFileEntity::getTradeId).toList();
            List<TradeEntity> tradelist = tradeService.list(new QueryWrapper<TradeEntity>().in("id", tradeIds));

            // 获取未在词库的词语列表
            List<String> fileIds = records.stream().map(UploadFileEntity::getFileId).toList();
            List<AiAnalysisSensitiveRelaEntity> analysisSensitiveRelaEntities = this.aiAnalysisSensitiveRelaService.list(new QueryWrapper<AiAnalysisSensitiveRelaEntity>().in("uuid", fileIds).eq("record_type", 1));


            List<UploadFileVO> list = records.stream().map(item -> {
                UploadFileVO vo = new UploadFileVO();
                BeanUtils.copyProperties(item, vo);
                //行业名称
                List<TradeEntity> list1 = tradelist.stream().filter(entity -> entity.getId().equals(item.getTradeId())).toList();
                if (list1 != null && list1.size() > 0) {
                    vo.setTradeName(list1.get(0).getName());
                }
                // 未在词库的词语
                List<AiAnalysisSensitiveRelaInfoVo> notMarkWordList = new LinkedList<>();
                if(analysisSensitiveRelaEntities != null && analysisSensitiveRelaEntities.size() > 0) {
                    for (AiAnalysisSensitiveRelaEntity analysisSensitiveRelaEntity : analysisSensitiveRelaEntities) {
                        if(analysisSensitiveRelaEntity.getUuid().equals(vo.getFileId())) {
                            AiAnalysisSensitiveRelaInfoVo aiAnalysisSensitiveRelaInfoVo = new AiAnalysisSensitiveRelaInfoVo();
                            BeanUtils.copyProperties(analysisSensitiveRelaEntity, aiAnalysisSensitiveRelaInfoVo);
                            notMarkWordList.add(aiAnalysisSensitiveRelaInfoVo);
                        }
                    }
                }
                vo.setNotMarkWordList(notMarkWordList);
                return vo;
            }).toList();
            pageUtils.setList(list);
        }
        return R.ok(pageUtils);
    }

    @Override
    public R<String> saveUploadFile(UploadFileBo uploadFileBo) {

        QueryWrapper<UploadFileEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("file_id", uploadFileBo.getFileId());
        wrapper.eq("user_id", uploadFileBo.getUserId());
        wrapper.eq("tenant_id", uploadFileBo.getTenantId());
        UploadFileEntity one = uploadFileService.getOne(wrapper);
        if(one != null) {
            Long id = one.getId();
            BeanUtils.copyProperties(uploadFileBo, one);
            one.setId(id);
            uploadFileService.updateById(one);
        }else {
            UploadFileEntity uploadFileEntity = new UploadFileEntity();
            BeanUtils.copyProperties(uploadFileBo, uploadFileEntity);
            uploadFileEntity.setId(SnowflakeManager.nextValue());
            this.uploadFileService.save(uploadFileEntity);
        }

        return R.ok();

//        UploadFileEntity uploadFileEntity = new UploadFileEntity();
//        BeanUtils.copyProperties(uploadFileBo, uploadFileEntity);
//        uploadFileEntity.setId(SnowflakeManager.nextValue());
//        uploadFileService.save(uploadFileEntity);
//        return R.ok();
    }

    /**
     * 客户端获取复盘上传文件列表
     *
     * @param id
     * @return
     */
    @Override
    public R<List<UploadFileVO>> seletList(Long id) {
        QueryWrapper<UploadFileEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", id);
        wrapper.eq("upload_status", 1);
        List<UploadFileEntity> list = uploadFileService.list(wrapper);
        List<UploadFileVO> objeUploadFileVOcts = new ArrayList<>();
        if (list != null && list.size() > 0) {
            List<UploadFileVO> list1 = list.stream().map(item -> {
                UploadFileVO vo = new UploadFileVO();
                BeanUtils.copyProperties(item, vo);
                return vo;
            }).toList();
            objeUploadFileVOcts.addAll(list1);
        }
        return R.ok(objeUploadFileVOcts);
    }

    /***
     *
     * @param uploadFileBo
     * @return
     */
    @Override
    @Transactional
    public R<String> updateUploadFile(UploadFileBo uploadFileBo) {
        QueryWrapper<UploadFileEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("file_id", uploadFileBo.getFileId());
        wrapper.eq("user_id", uploadFileBo.getUserId());
        wrapper.eq("tenant_id", uploadFileBo.getTenantId());
        UploadFileEntity one = uploadFileService.getOne(wrapper);
        if(one != null) {
            Long id = one.getId();
            BeanUtils.copyProperties(uploadFileBo, one);
            one.setId(id);
            uploadFileService.updateById(one);
        }else {
            UploadFileEntity uploadFileEntity = new UploadFileEntity();
            BeanUtils.copyProperties(uploadFileBo, uploadFileEntity);
            uploadFileEntity.setId(SnowflakeManager.nextValue());
            this.uploadFileService.save(uploadFileEntity);
        }

        return R.ok();
    }

    /**
     * 根据fileId删除
     *
     * @param fileId
     */
    @Override
    public void deleltByFileId(String fileId) {
        uploadFileService.remove(new QueryWrapper<UploadFileEntity>().eq("file_id", fileId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<String> removeByFileId(List<String> fileIds) {
        // 删除视频文件
        uploadFileService.remove(new QueryWrapper<UploadFileEntity>().in("file_id", fileIds));
        // 删除视频分析数据（旧）
        uploadFileAnalysisService.remove(new QueryWrapper<UploadFileAnalysisEntity>().in("file_id", fileIds));
        // 删除分析记录
        uploadFileAnalysisRecordService.remove(new QueryWrapper<UploadFileAnalysisRecordEntity>().in("file_id", fileIds));
        // 删除关联的对比数据
        QueryWrapper<SyncContrastEntity> wrapper = new QueryWrapper<>();
        wrapper.and(w -> {
            w.in("file_one_id", fileIds).or().in("file_two_id", fileIds);
        });
        syncContrastService.remove(wrapper);
        return R.ok();
    }

    @Override
    public void saveBatch(List<UploadFileAnalysisSaveBo> uploadFileAnalysisSaveBos) {
        if(uploadFileAnalysisSaveBos != null && uploadFileAnalysisSaveBos.size() > 0) {

            // 获取最新的版本号
            String videoId = uploadFileAnalysisSaveBos.get(0).getFileId();
            QueryWrapper<UploadFileAnalysisEntity> wrapper = new QueryWrapper<>();
            wrapper.eq("file_id", videoId);
            wrapper.orderByDesc("version");
            wrapper.last(" limit 1");
            UploadFileAnalysisEntity lastAnalysis = this.uploadFileAnalysisService.getOne(wrapper);

            int version = 0;
            if(lastAnalysis != null) {
                version = lastAnalysis.getVersion() + 1;
            }

            int finalVersion = version;
            List<UploadFileAnalysisEntity> uploadFileAnalysisEntities = uploadFileAnalysisSaveBos.stream().map(item -> {
                UploadFileAnalysisEntity uploadFileAnalysisEntity = new UploadFileAnalysisEntity();
                BeanUtils.copyProperties(item, uploadFileAnalysisEntity);
                uploadFileAnalysisEntity.setId(SnowflakeManager.nextValue());
                uploadFileAnalysisEntity.setCreateDate(new Date());
                uploadFileAnalysisEntity.setVersion(finalVersion);
                return uploadFileAnalysisEntity;
            }).toList();

            this.uploadFileAnalysisService.saveBatch(uploadFileAnalysisEntities);
        }
    }

    @Override
    public UploadFileInfoVo getByFileId(String fileId) {

        UploadFileEntity uploadFileEntity = this.uploadFileService.getOne(new QueryWrapper<UploadFileEntity>().eq("file_id", fileId));
        if(uploadFileEntity != null) {
            UploadFileInfoVo uploadFileInfoVo = new UploadFileInfoVo();
            BeanUtils.copyProperties(uploadFileEntity, uploadFileInfoVo);
            return uploadFileInfoVo;
        }

        return null;
    }

    /**
     * 根据user_id查询文件上传分析
     * @param uploadFileBo
     * @return
     */
    @Override
    public R<PageUtils<UploadFileVO>> fileAnalysisByUserId(UploadFileBo uploadFileBo) {
        QueryWrapper<UploadFileEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", uploadFileBo.getUserId());

        IPage<UploadFileEntity> page = this.uploadFileService.page(new Query<UploadFileEntity>().getPage(uploadFileBo.getPage(), uploadFileBo.getLimit()), wrapper);
        PageUtils<UploadFileVO> pageUtils = new PageUtils<>(uploadFileBo.getPage(), uploadFileBo.getLimit(),page);

        List<UploadFileEntity> records = page.getRecords();
        if (records != null && records.size() > 0) {
            List<UploadFileVO> voList = records.stream().map(item ->{
                UploadFileVO vo = new UploadFileVO();
                BeanUtils.copyProperties(item,vo);

                TradeEntity trade = this.tradeService.getById(item.getTradeId());
                if (trade != null){
                    vo.setTradeName(trade.getName());
                }
                return vo;
            }).toList();
            pageUtils.setList(voList);
        }
        return R.ok("",pageUtils);
    }


    @Override
    public PageUtils<UploadFileInfoVo> clientFileList(ClientFileListBo clientFileListBo) {

        if(clientFileListBo.getFileSliceType() == null) {
            clientFileListBo.setFileSliceType(0);
        }

        QueryWrapper<UploadFileEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", clientFileListBo.getUserId());
        wrapper.eq("tenant_id", clientFileListBo.getTenantId());

        if(clientFileListBo.getFileSliceType() != null) {
            wrapper.eq("file_slice_type", clientFileListBo.getFileSliceType());
        }
        if(!StringUtils.isEmpty(clientFileListBo.getTradeId())) {
            wrapper.eq("trade_id", clientFileListBo.getTradeId());
        }
        if(!StringUtils.isEmpty(clientFileListBo.getFileName())) {
            wrapper.like("file_name", clientFileListBo.getFileName());
        }
        if(!StringUtils.isEmpty(clientFileListBo.getUploadStatus())) {
            wrapper.eq("upload_status", clientFileListBo.getUploadStatus());
        }
        if(!StringUtils.isEmpty(clientFileListBo.getAnalysisStatus())) {
            wrapper.eq("analysis_status", clientFileListBo.getAnalysisStatus());
        }
        if(!StringUtils.isEmpty(clientFileListBo.getFileType())) {
            wrapper.eq("file_type", clientFileListBo.getFileType());
        }
        if(clientFileListBo.getFileTypeArr() != null && !clientFileListBo.getFileTypeArr().isEmpty()) {
            wrapper.in("file_type", clientFileListBo.getFileTypeArr());
        }
        if(!StringUtils.isEmpty(clientFileListBo.getAnalysisStartDate())) {
            wrapper.ge("analysis_time", clientFileListBo.getAnalysisStartDate() + " 00:00:00");
        }
        if(!StringUtils.isEmpty(clientFileListBo.getAnalysisEndDate())) {
            wrapper.le("analysis_time", clientFileListBo.getAnalysisEndDate() + " 23:59:59");
        }
        if(!StringUtils.isEmpty(clientFileListBo.getUploadStartDate())) {
            wrapper.ge("upload_time", clientFileListBo.getUploadStartDate() + " 00:00:00");
        }
        if(!StringUtils.isEmpty(clientFileListBo.getUploadEndDate())) {
            wrapper.le("upload_time", clientFileListBo.getUploadEndDate() + " 23:59:59");
        }
//        wrapper.orderByDesc("upload_time");

        IPage<UploadFileEntity> iPage = uploadFileService.page(new Query<UploadFileEntity>().getPage(clientFileListBo.getPage(), clientFileListBo.getLimit()), wrapper);

        PageUtils<UploadFileInfoVo> pageUtils = new PageUtils<>(clientFileListBo.getPage(), clientFileListBo.getLimit(), iPage);

        List<UploadFileEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<UploadFileInfoVo> vos = records.stream().map(item -> {
                UploadFileInfoVo uploadFileInfoVo = new UploadFileInfoVo();
                BeanUtils.copyProperties(item, uploadFileInfoVo);
                return uploadFileInfoVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public List<String> listByAiFav(ClientAiFavListBo clientAiFavListBo) {

        QueryWrapper<UploadFileEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(clientAiFavListBo.getTradeId())) {
            wrapper.eq("trade_id", clientAiFavListBo.getTradeId());
        }
        if(!StringUtils.isEmpty(clientAiFavListBo.getFileName())) {
            wrapper.like("file_name", clientAiFavListBo.getFileName());
        }
        wrapper.eq("user_id", clientAiFavListBo.getUserId());
        wrapper.eq("tenant_id", clientAiFavListBo.getTenantId());

        List<UploadFileEntity> uploadFileEntities = this.uploadFileService.list(wrapper);
        if(uploadFileEntities != null && uploadFileEntities.size() > 0) {
            return uploadFileEntities.stream().map(UploadFileEntity::getFileId).toList();
        }
        return null;
    }

    @Override
    public List<UploadFileInfoVo> listByFileIds(Collection<String> fileIds) {
        if(fileIds != null && fileIds.size() > 0) {
            QueryWrapper<UploadFileEntity> wrapper = new QueryWrapper<>();
            wrapper.in("file_id", fileIds);
            List<UploadFileEntity> uploadFileEntities = this.uploadFileService.list(wrapper);
            if(uploadFileEntities != null && uploadFileEntities.size() > 0) {
                List<UploadFileInfoVo> uploadFileInfoVos = uploadFileEntities.stream().map(item -> {
                    UploadFileInfoVo uploadFileInfoVo = new UploadFileInfoVo();
                    BeanUtils.copyProperties(item, uploadFileInfoVo);
                    return uploadFileInfoVo;
                }).toList();

                return uploadFileInfoVos;
            }
        }
        return null;
    }

    @Override
    public List<String> getAllowDeleteFile(List<String> ids, Long tenantId, Long userId) {
        QueryWrapper<UploadFileEntity> wrapper = new QueryWrapper<>();
        wrapper.in("file_id", ids);
        wrapper.eq("tenant_id", tenantId);
        if(!StringUtils.isEmpty(userId)) {
            wrapper.eq("user_id", userId);
        }
        wrapper.select("file_id");
        List<UploadFileEntity> uploadFileEntities = this.uploadFileService.list(wrapper);
        if(uploadFileEntities != null && uploadFileEntities.size() > 0) {
            return uploadFileEntities.stream().map(UploadFileEntity::getFileId).collect(Collectors.toList());
        }
        return null;
    }

    @Override
    public void initFileAnalysisStatus(Long userId, Long tenantId) {

        QueryWrapper<UploadFileEntity> wrapper = new QueryWrapper<>();
        wrapper.in("analysis_status", 1);
        wrapper.eq("user_id", userId);
        wrapper.eq("tenant_id", tenantId);

        List<UploadFileEntity> uploadFileEntities = this.uploadFileService.list(wrapper);

        if(uploadFileEntities != null && uploadFileEntities.size() > 0) {
            for (UploadFileEntity uploadFileEntity : uploadFileEntities) {
                uploadFileEntity.setAnalysisStatus(3);
                uploadFileEntity.setErrorReason("系统被退出");
            }

            this.uploadFileService.updateBatchById(uploadFileEntities);
        }
    }

    @Override
    public UploadFileInfoVo clientGetFileByFileId(String fileId, Long userId, Long tenantId) {

        QueryWrapper<UploadFileEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("file_id", fileId);
        wrapper.eq("user_id", userId);
        wrapper.eq("tenant_id", tenantId);
        UploadFileEntity uploadFileEntity = this.uploadFileService.getOne(wrapper);

        if(uploadFileEntity != null) {
            UploadFileInfoVo uploadFileInfoVo = new UploadFileInfoVo();
            BeanUtils.copyProperties(uploadFileEntity, uploadFileInfoVo);

            if(Objects.equals(uploadFileInfoVo.getFileSliceType(), VideoSliceTypeEnum.VIDEO_SLICE.getCode())
                    || Objects.equals(uploadFileInfoVo.getFileSliceType(), VideoSliceTypeEnum.SHORT_VIDEO_SLICE.getCode())) {
                // 封装切片信息
                VideoSliceEntity videoSliceEntity = videoSliceService.getOne(new LambdaQueryWrapper<VideoSliceEntity>().eq(VideoSliceEntity::getSourceId, uploadFileInfoVo.getFileId()));
                if(videoSliceEntity != null) {
                    VideoSliceVo videoSliceVo = BeanConvertUtils.convert(videoSliceEntity, VideoSliceVo.class);
                    uploadFileInfoVo.setVideoSliceInfo(videoSliceVo);

                    // 封装切片视频所属原视频信息
                    UploadFileEntity parentVideo = this.uploadFileService.getOne(new LambdaQueryWrapper<UploadFileEntity>().eq(UploadFileEntity::getFileId, videoSliceVo.getSourceParentId()));
                    if(parentVideo != null) {
                        uploadFileInfoVo.setParentFileInfo(BeanConvertUtils.convert(parentVideo, UploadFileInfoVo.class));
                    }
                }
            }else if(Objects.equals(uploadFileInfoVo.getFileSliceType(), VideoSliceTypeEnum.VIDEO.getCode())) {
                // 封装原视频下的所有切片信息
                List<VideoSliceEntity> videoSliceEntities = videoSliceService.list(new LambdaQueryWrapper<VideoSliceEntity>().eq(VideoSliceEntity::getSourceParentId, uploadFileInfoVo.getFileId()));
                if(videoSliceEntities != null && !videoSliceEntities.isEmpty()) {
                    List<VideoSliceVo> videoSliceVos = BeanConvertUtils.convertList(videoSliceEntities, VideoSliceVo.class);
                    uploadFileInfoVo.setSliceList(videoSliceVos);
                }
            }

            return uploadFileInfoVo;
        }

        return null;
    }

    @Override
    public void saveOrUpdateFile(UploadFileInfoBo uploadFileInfoBo) {

        UploadFileEntity oldFile = this.uploadFileService.getOne(new QueryWrapper<UploadFileEntity>().eq("file_id", uploadFileInfoBo.getFileId()));

        UploadFileEntity uploadFileEntity = new UploadFileEntity();
        BeanUtils.copyProperties(uploadFileInfoBo, uploadFileEntity);

        if(oldFile != null) {
            // 修改
            uploadFileEntity.setId(oldFile.getId());
            this.uploadFileService.updateById(uploadFileEntity);
        }else {
            // 添加
            uploadFileEntity.setId(SnowflakeManager.nextValue());

            this.uploadFileService.save(uploadFileEntity);
        }
    }

    @Override
    public void updateFileAnalysisStatus(UpdateFileAnalysisStatusBo updateFileAnalysisStatusBo) {

        QueryWrapper<UploadFileEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("file_id", updateFileAnalysisStatusBo.getFileId());
        wrapper.eq("user_id", updateFileAnalysisStatusBo.getUserId());
        wrapper.eq("tenant_id", updateFileAnalysisStatusBo.getTenantId());
        UploadFileEntity uploadFileEntity = this.uploadFileService.getOne(wrapper);

        if(uploadFileEntity != null) {
            uploadFileEntity.setErrorReason(updateFileAnalysisStatusBo.getErrorReason());
            uploadFileEntity.setAnalysisStatus(updateFileAnalysisStatusBo.getAnalysisStatus());
            if(updateFileAnalysisStatusBo.getAnalysisStatus() == 2) {
                LocalDateTime now = LocalDateTime.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                uploadFileEntity.setAnalysisTime(now.format(formatter));
                uploadFileEntity.setIsMark(1);
            }

            this.uploadFileService.updateById(uploadFileEntity);
        }
    }

    @Override
    public void updateFileTrade(UpdateFileTradeBo updateFileTradeBo) {

        QueryWrapper<UploadFileEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("file_id", updateFileTradeBo.getFileId());
        wrapper.eq("user_id", updateFileTradeBo.getUserId());
        wrapper.eq("tenant_id", updateFileTradeBo.getTenantId());
        UploadFileEntity uploadFileEntity = this.uploadFileService.getOne(wrapper);

        if(uploadFileEntity != null) {
            uploadFileEntity.setTradeId(updateFileTradeBo.getTradeId());

            this.uploadFileService.updateById(uploadFileEntity);
        }
    }

    @Override
    public List<UploadFileInfoVo> clientListFileByFileIds(List<String> ids, Long userId, Long tenantId) {

        QueryWrapper<UploadFileEntity> wrapper = new QueryWrapper<>();
        wrapper.in("file_id", ids);
        wrapper.eq("user_id", userId);
        wrapper.eq("tenant_id", tenantId);

        List<UploadFileEntity> uploadFileEntities = this.uploadFileService.list(wrapper);

        if(uploadFileEntities != null && uploadFileEntities.size() > 0) {
            List<UploadFileInfoVo> uploadFileInfoVos = uploadFileEntities.stream().map(item -> {
                UploadFileInfoVo uploadFileInfoVo = new UploadFileInfoVo();
                BeanUtils.copyProperties(item, uploadFileInfoVo);
                return uploadFileInfoVo;
            }).collect(Collectors.toList());

            return uploadFileInfoVos;
        }

        return null;
    }



    @Override
    public UploadFileInfoVo getfileByRourceId(String sourceId) {
        List<UploadFileEntity> list = uploadFileService.lambdaQuery().eq(UploadFileEntity::getFileId, sourceId).list();
        if (!list.isEmpty()){
            UploadFileEntity uploadFileEntity = list.get(0);
            return BeanUtil.toBean(uploadFileEntity,UploadFileInfoVo.class );
        }
        return null;
    }

}
