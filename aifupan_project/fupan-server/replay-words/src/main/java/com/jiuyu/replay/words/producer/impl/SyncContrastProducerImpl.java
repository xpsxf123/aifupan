package com.jiuyu.replay.words.producer.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jiuyu.replay.common.utils.BeanConvertUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.words.AnchorUrlInfoVo;
import com.jiuyu.replay.generic.vo.words.AnchorVideoInfoVo;
import com.jiuyu.replay.words.bo.*;
import com.jiuyu.replay.words.constant.WordsProperties;
import com.jiuyu.replay.words.entity.*;
import com.jiuyu.replay.words.producer.SyncContrastProducer;
import com.jiuyu.replay.words.repository.service.*;
import com.jiuyu.replay.words.repository.service.impl.TradeServiceImpl;
import com.jiuyu.replay.words.vo.SyncContrastInfoVo;
import com.jiuyu.replay.words.vo.SyncContrastListVo;
import com.jiuyu.replay.words.vo.file.UploadFileInfoVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * 客户端对比数据
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-26 14:51:59
 */
@Service
public class SyncContrastProducerImpl implements SyncContrastProducer {

    @Resource
    private SyncContrastService syncContrastService;
    @Resource
    private AnchorVideoService anchorVideoService;
    @Resource
    private UploadFileService uploadFileService;
    @Resource
    private AnchorUrlService anchorUrlService;
    @Resource
    private AnchorUrlUserService anchorUrlUserService;
    @Resource
    private WordsProperties wordsProperties;
    @Autowired
    private TradeServiceImpl tradeService;


    @Override
    public PageUtils<SyncContrastListVo> queryPage(SyncContrastListBo syncContrastListBo) {
        QueryWrapper<SyncContrastEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(syncContrastListBo.getKeyword())){
            wrapper.like("name", syncContrastListBo.getKeyword());
        }
        if (!StringUtils.isEmpty(syncContrastListBo.getUserID())){
            wrapper.eq("user_id", syncContrastListBo.getUserID());
        }
        if (!StringUtils.isEmpty(syncContrastListBo.getUserIds())){
            wrapper.in("user_id", syncContrastListBo.getUserIds());
        }
        if (syncContrastListBo.getStartTime() != null){
            wrapper.ge("create_date", syncContrastListBo.getStartTime());
        }
        if (syncContrastListBo.getEndTime() != null){
            wrapper.le("create_date", syncContrastListBo.getEndTime());
        }

        IPage<SyncContrastEntity> iPage = syncContrastService.page(new Query<SyncContrastEntity>().getPage(syncContrastListBo.getPage(), syncContrastListBo.getLimit()), wrapper);

        PageUtils<SyncContrastListVo> pageUtils = new PageUtils<>(syncContrastListBo.getPage(), syncContrastListBo.getLimit(), iPage);

        List<SyncContrastEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<SyncContrastListVo> vos = records.stream().map(item -> {
                SyncContrastListVo syncContrastVo = new SyncContrastListVo();
                BeanUtils.copyProperties(item, syncContrastVo);
                return syncContrastVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public SyncContrastInfoVo info(Long id) {

        SyncContrastEntity syncContrastEntity = syncContrastService.getById(id);
        if(syncContrastEntity != null) {
            SyncContrastInfoVo syncContrastInfoVo = new SyncContrastInfoVo();
            BeanUtils.copyProperties(syncContrastEntity, syncContrastInfoVo);
            return syncContrastInfoVo;
        }

        return null;
    }

    /**
     * 新增客户端对比数据
     * @param syncContrastBo 客户端对比数据对象
     * @return
     */
     public SyncContrastInfoVo save(SyncContrastBo syncContrastBo) {

         SyncContrastEntity syncContrastEntity = new SyncContrastEntity();
         BeanUtils.copyProperties(syncContrastBo, syncContrastEntity);
         syncContrastEntity.setId(SnowflakeManager.nextValue());
         syncContrastEntity.setCreateDate(new Date());
         syncContrastEntity.setUpdateDate(new Date());
         SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
         syncContrastEntity.setContrastTime(sdf.format(new Date()));
         syncContrastEntity.setContrastId(UUID.randomUUID().toString());
         if(syncContrastEntity.getDeleteStatus() == null) {
             syncContrastEntity.setDeleteStatus(0);
         }else {
             syncContrastEntity.setDeleteStatus(syncContrastBo.getDeleteStatus());
         }
         if(syncContrastEntity.getIsShard() != null && syncContrastEntity.getIsShard() == 1) {
             String shareUrl = wordsProperties.getCloudSpaceUrl() + "contrastOnlineAnalysis/" + syncContrastEntity.getContrastId();
             syncContrastEntity.setShareUrl(shareUrl);
         }

         syncContrastService.save(syncContrastEntity);

         SyncContrastInfoVo syncContrastInfoVo = new SyncContrastInfoVo();
         BeanUtils.copyProperties(syncContrastEntity, syncContrastInfoVo);

         return syncContrastInfoVo;
     }

    /**
     * 修改客户端对比数据
     * @param syncContrastBo 客户端对比数据对象
     * @return
     */
    public void update(SyncContrastBo syncContrastBo) {

        SyncContrastEntity oldEntity = this.syncContrastService.getOne(new QueryWrapper<SyncContrastEntity>().eq("contrast_id", syncContrastBo.getContrastId()));
        if (oldEntity != null) {
            SyncContrastEntity syncContrastEntity = new SyncContrastEntity();
            BeanUtils.copyProperties(syncContrastBo, syncContrastEntity);
            syncContrastEntity.setId(oldEntity.getId());
            syncContrastEntity.setUpdateDate(new Date());
            syncContrastService.updateById(syncContrastEntity);
        }else {
            SyncContrastEntity syncContrastEntity = new SyncContrastEntity();
            BeanUtils.copyProperties(syncContrastBo, syncContrastEntity);
            syncContrastEntity.setId(SnowflakeManager.nextValue());
            syncContrastEntity.setCreateDate(new Date());
            syncContrastEntity.setUpdateDate(new Date());
            syncContrastService.save(syncContrastEntity);
        }

    }

    /**
     * 删除客户端对比数据
     * @param id 客户端对比数据id
     * @return
     */
    public void deleteById(Long id) {

        syncContrastService.removeById(id);
    }

    @Override
    public List<SyncContrastInfoVo> listByUserId(Long userId) {

        List<SyncContrastEntity> syncContrastEntities = this.syncContrastService.list(
                new QueryWrapper<SyncContrastEntity>().eq("user_id", userId).eq("is_shard", 1));

        if(syncContrastEntities != null && syncContrastEntities.size() > 0) {
            List<SyncContrastInfoVo> syncContrastInfoVos = syncContrastEntities.stream().map(item -> {
                SyncContrastInfoVo syncContrastInfoVo = new SyncContrastInfoVo();
                BeanUtils.copyProperties(item, syncContrastInfoVo);
                return syncContrastInfoVo;
            }).toList();

            return syncContrastInfoVos;
        }

        return null;
    }

    @Override
    public SyncContrastInfoVo infoByContrastId(String contrastId) {
        SyncContrastEntity syncContrastEntity = this.syncContrastService.getOne(new QueryWrapper<SyncContrastEntity>().eq("contrast_id", contrastId));
        if(syncContrastEntity != null) {
            SyncContrastInfoVo syncContrastInfoVo = new SyncContrastInfoVo();
            BeanUtils.copyProperties(syncContrastEntity, syncContrastInfoVo);
            return syncContrastInfoVo;
        }
        return null;
    }

    @Override
    public void switchContrastPosition(String contrastId) {
        SyncContrastEntity entity = this.syncContrastService.getOne(new QueryWrapper<SyncContrastEntity>().eq("contrast_id", contrastId));
        if (entity == null) {
            return;
        }
        // 交换 video_one_id 和 video_two_id
        String tempVideoId = entity.getVideoOneId();
        entity.setVideoOneId(entity.getVideoTwoId());
        entity.setVideoTwoId(tempVideoId);
        // 交换 anchor_one_id 和 anchor_two_id
        String tempAnchorId = entity.getAnchorOneId();
        entity.setAnchorOneId(entity.getAnchorTwoId());
        entity.setAnchorTwoId(tempAnchorId);

        this.syncContrastService.updateById(entity);
    }

    @Override
    public SyncContrastInfoVo infoDetailsByContrastId(String contrastId) {

        SyncContrastInfoVo result = infoByContrastId(contrastId);

        if (result == null) {
            return null;
        }
        // 设置视频或文件数据
        // 设置视频1数据
        setVideoOrFileData(result, result.getVideoOneId(), "video", 1);
        // 设置视频2数据
        setVideoOrFileData(result, result.getVideoTwoId(), "video", 2);
        // 设置文件1数据
        setVideoOrFileData(result, result.getFileOneId(), "file", 1);
        // 设置文件2数据
        setVideoOrFileData(result, result.getFileTwoId(), "file", 2);

        // 设置主播1数据
        setAnchorUrlData(result, result.getAnchorOneId(), 1);
        // 设置主播2数据
        setAnchorUrlData(result, result.getAnchorTwoId(), 2);

        return result;
    }

    /**
     * 设置主播数据
     *
     * @param result      结果
     * @param anchorOneId 主播1id
     * @param i           数字
     */
    private void setAnchorUrlData(SyncContrastInfoVo result, String anchorOneId, int i) {
        if (anchorOneId == null) return;
        AnchorUrlEntity anchorUrlEntity = anchorUrlService.lambdaQuery().eq(AnchorUrlEntity::getSecUid, anchorOneId).last("limit 1").one();
        if (anchorUrlEntity != null) {
            AnchorUrlInfoVo anchorUrlInfoVo = BeanUtil.copyProperties(anchorUrlEntity, AnchorUrlInfoVo.class);
            if (i == 1) {
                result.setAnchorOneInfo(anchorUrlInfoVo);
            } else if (i == 2) {
                result.setAnchorTwoInfo(anchorUrlInfoVo);
            }
        }
    }

    /**
     * 设置视频或文件数据
     *
     * @param res        结果
     * @param sourceId   源id
     * @param sourceType 源类型
     * @param num        数字
     */
    private void setVideoOrFileData(SyncContrastInfoVo res, String sourceId, String sourceType, int num) {
        if (sourceId == null || sourceType == null) return;

        if (sourceType.equals("video")) {
            AnchorVideoEntity video = anchorVideoService.lambdaQuery()
                    .eq(AnchorVideoEntity::getVideoId, sourceId)
                    .last("limit 1")
                    .one();
            if (video != null) {
                if (num == 1) {
                    res.setVideoOneInfo(BeanUtil.copyProperties(video, AnchorVideoInfoVo.class));
                } else if (num == 2) {
                    res.setVideoTwoInfo(BeanUtil.copyProperties(video, AnchorVideoInfoVo.class));
                }
            }
        } else if (sourceType.equals("file")) {
            UploadFileEntity file = uploadFileService.lambdaQuery()
                    .eq(UploadFileEntity::getFileId, sourceId)
                    .last("limit 1")
                    .one();
            if (file != null) {
                if (num == 1) {
                    res.setFileOneInfo(BeanUtil.copyProperties(file, UploadFileInfoVo.class));
                } else if (num == 2) {
                    res.setFileTwoInfo(BeanUtil.copyProperties(file, UploadFileInfoVo.class));
                }
            }
        }

    }

    @Override
    public PageUtils<SyncContrastListVo> listCloudContrast(CloudContrastListBo cloudContrastListBo) {

        QueryWrapper<SyncContrastEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("tenant_id", cloudContrastListBo.getTenantId());
        wrapper.eq("is_shard", 1);
        wrapper.isNotNull("video_one_id");
        wrapper.isNotNull("video_two_id");
        if(!StringUtils.isEmpty(cloudContrastListBo.getSecUid())) {
            wrapper.and(w -> {
                w.eq("anchor_one_id", cloudContrastListBo.getSecUid()).or().eq("anchor_two_id", cloudContrastListBo.getSecUid());
            });
        }
        if(!StringUtils.isEmpty(cloudContrastListBo.getContrastStartDate())) {
            wrapper.ge("contrast_time", cloudContrastListBo.getContrastStartDate() + " 00:00:00");
        }
        if(!StringUtils.isEmpty(cloudContrastListBo.getContrastEndDate())) {
            wrapper.le("contrast_time", cloudContrastListBo.getContrastEndDate() + " 23:59:59");
        }

        IPage<SyncContrastEntity> iPage = this.syncContrastService.page(new Query<SyncContrastEntity>().getPage(cloudContrastListBo.getPage(), cloudContrastListBo.getLimit()), wrapper);

        PageUtils<SyncContrastListVo> pageUtils = new PageUtils<>(cloudContrastListBo.getPage(), cloudContrastListBo.getLimit(), iPage);

        List<SyncContrastEntity> records = iPage.getRecords();

        if(records != null && records.size() > 0) {

            List<SyncContrastListVo> vos = records.stream().map(item -> {
                SyncContrastListVo syncContrastListVo = new SyncContrastListVo();
                BeanUtils.copyProperties(item, syncContrastListVo);

                return syncContrastListVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }


        return pageUtils;
    }

    @Override
    public void deleteCloudByVideoId(String videoId) {
        QueryWrapper<SyncContrastEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("is_shard", 1);
        wrapper.and(w -> {
            w.eq("video_one_id", videoId).or().eq("video_two_id", videoId);
        });
        List<SyncContrastEntity> syncContrastEntities = this.syncContrastService.list(wrapper);

        if(syncContrastEntities != null && syncContrastEntities.size() > 0) {
            for (SyncContrastEntity syncContrastEntity : syncContrastEntities) {
                syncContrastEntity.setShareUrl("");
                syncContrastEntity.setIsShard(0);
                syncContrastEntity.setUpdateDate(new Date());
                if(syncContrastEntity.getDeleteStatus() == 1) {
                    syncContrastEntity.setDeleteStatus(2);
                }
            }
            this.syncContrastService.updateBatchById(syncContrastEntities);
        }
    }

    @Override
    public void deleteCloudByVideoIds(Collection<String> videoIds) {
        if (videoIds == null || videoIds.isEmpty()) {
            return;
        }

        // 一次 SELECT IN：命中分享中（is_shard=1）且 video_one_id ∈ ids ∪ video_two_id ∈ ids 的对比记录
        QueryWrapper<SyncContrastEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("is_shard", 1);
        wrapper.and(w -> w.in("video_one_id", videoIds).or().in("video_two_id", videoIds));
        List<SyncContrastEntity> syncContrastEntities = this.syncContrastService.list(wrapper);

        if (syncContrastEntities == null || syncContrastEntities.isEmpty()) {
            return;
        }

        // 内存里统一修改字段，跟单条版本完全对齐：清分享 URL、撤销分享标记、更新时间、deleteStatus 1->2
        Date now = new Date();
        for (SyncContrastEntity entity : syncContrastEntities) {
            entity.setShareUrl("");
            entity.setIsShard(0);
            entity.setUpdateDate(now);
            if (entity.getDeleteStatus() == 1) {
                entity.setDeleteStatus(2);
            }
        }

        // 一次 updateBatchById 写回（MyBatis-Plus 默认按 1000 一批，远高于本接口入参上限 100）
        this.syncContrastService.updateBatchById(syncContrastEntities);
    }

    /**
     * 查询所有对比分析记录
     * @return
     */
    @Override
    public List<SyncContrastEntity> listAllContrast() {
        List<SyncContrastEntity> syncContrastEntities = this.syncContrastService.list();
        if(syncContrastEntities != null && syncContrastEntities.size() > 0) {
            return syncContrastEntities;
        }
        return null;
    }

    @Override
    public R<PageUtils<SyncContrastListVo>> listAllSyncContrast(SyncContrastListBo syncContrastListBo) {

        return null;
    }

    @Override
    public PageUtils<SyncContrastInfoVo> clientContrastList(ClientContrastListBo clientContrastListBo) {

        if(clientContrastListBo.getSliceContrastType() == null) {
            clientContrastListBo.setSliceContrastType(0);
        }

        QueryWrapper<SyncContrastEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(clientContrastListBo.getUserId())) {
            wrapper.eq("user_id", clientContrastListBo.getUserId());
        }
        if(!StringUtils.isEmpty(clientContrastListBo.getTenantId())) {
            wrapper.eq("tenant_id", clientContrastListBo.getTenantId());
        }
        if(clientContrastListBo.getUserIdList() != null && clientContrastListBo.getUserIdList().size() > 0) {
            wrapper.in("user_id", clientContrastListBo.getUserIdList());
        }

        if(!StringUtils.isEmpty(clientContrastListBo.getTradeId())) {
            if(!StringUtils.isEmpty(clientContrastListBo.getContrastType()) && clientContrastListBo.getContrastType() == 0) {
                // 获取该行业的主播
                QueryWrapper<AnchorUrlUserEntity> anchorWrapper = new QueryWrapper<>();
                if(!StringUtils.isEmpty(clientContrastListBo.getUserId())) {
                    anchorWrapper.eq("user_id", clientContrastListBo.getUserId());
                }
                if(!StringUtils.isEmpty(clientContrastListBo.getTenantId())) {
                    anchorWrapper.eq("tenant_id", clientContrastListBo.getTenantId());
                }
                anchorWrapper.eq("trade_id", clientContrastListBo.getTradeId());
                anchorWrapper.select("anchor_url_sec_uid");
                List<AnchorUrlUserEntity> anchorUrlUserEntities = this.anchorUrlUserService.list(anchorWrapper);

                if(anchorUrlUserEntities != null && anchorUrlUserEntities.size() > 0) {
                    List<String> secUids = anchorUrlUserEntities.stream().map(AnchorUrlUserEntity::getAnchorUrlSecUid).collect(Collectors.toList());
                    wrapper.and(w -> {
                        w.in("anchor_one_id", secUids).or().in("anchor_two_id", secUids);
                    });
                }else {
                    wrapper.eq("id", "0");
                }
            }else {
                wrapper.and(w -> {
                    w.eq("trade_one_id", clientContrastListBo.getTradeId()).or().eq("trade_two_id", clientContrastListBo.getTradeId());
                });
            }
        }

        if(clientContrastListBo.getSliceContrastType() != null) {
            wrapper.eq("slice_contrast_type", clientContrastListBo.getSliceContrastType());
        }
        if(!StringUtils.isEmpty(clientContrastListBo.getContrastType())) {
            wrapper.eq("contrast_type", clientContrastListBo.getContrastType());
        }
        if(!StringUtils.isEmpty(clientContrastListBo.getDeleteStatus())) {
            wrapper.eq("delete_status", clientContrastListBo.getDeleteStatus());
        }
        if(!StringUtils.isEmpty(clientContrastListBo.getIsShard())) {
            wrapper.eq("is_shard", clientContrastListBo.getIsShard());
        }
//        if(!StringUtils.isEmpty(clientContrastListBo.getTradeId())) {
//            wrapper.and(w -> {
//                w.eq("trade_one_id", clientContrastListBo.getTradeId()).or().eq("trade_two_id", clientContrastListBo.getTradeId());
//            });
//        }
        if(!StringUtils.isEmpty(clientContrastListBo.getSecUid())) {
            wrapper.and(w -> {
                w.eq("anchor_one_id", clientContrastListBo.getSecUid()).or().eq("anchor_two_id", clientContrastListBo.getSecUid());
            });
        }
        if(clientContrastListBo.getSecUidArr() != null && clientContrastListBo.getSecUidArr().size() > 0) {
            wrapper.and(w -> {
                w.in("anchor_one_id", clientContrastListBo.getSecUidArr()).or().in("anchor_two_id", clientContrastListBo.getSecUidArr());
            });
        }
        if(!StringUtils.isEmpty(clientContrastListBo.getFileName())) {
            QueryWrapper<UploadFileEntity> fileWrapper = new QueryWrapper<>();
            if(!StringUtils.isEmpty(clientContrastListBo.getUserId())) {
                fileWrapper.eq("user_id", clientContrastListBo.getUserId());
            }
            if(!StringUtils.isEmpty(clientContrastListBo.getTenantId())) {
                fileWrapper.eq("tenant_id", clientContrastListBo.getTenantId());
            }
            fileWrapper.like("file_name", clientContrastListBo.getFileName());
            fileWrapper.select("file_id");
            List<UploadFileEntity> uploadFileEntities = this.uploadFileService.list(fileWrapper);
            if(uploadFileEntities != null && uploadFileEntities.size() > 0) {
                Set<String> fileIds = uploadFileEntities.stream().map(UploadFileEntity::getFileId).collect(Collectors.toSet());
                wrapper.and(w -> {
                    w.in("file_one_id", fileIds).or().in("file_two_id", fileIds);
                });
            }else {
                wrapper.eq("id", 0);
            }
        }
        if(!StringUtils.isEmpty(clientContrastListBo.getContrastStartDate())) {
            wrapper.ge("update_date", clientContrastListBo.getContrastStartDate() + " 00:00:00");
        }
        if(!StringUtils.isEmpty(clientContrastListBo.getContrastEndDate())) {
            wrapper.le("update_date", clientContrastListBo.getContrastEndDate() + " 23:59:59");
        }
        wrapper.orderByDesc("update_date");

        IPage<SyncContrastEntity> iPage = syncContrastService.page(new Query<SyncContrastEntity>().getPageNoSort(clientContrastListBo.getPage(), clientContrastListBo.getLimit()), wrapper);

        PageUtils<SyncContrastInfoVo> pageUtils = new PageUtils<>(clientContrastListBo.getPage(), clientContrastListBo.getLimit(), iPage);

        List<SyncContrastEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<SyncContrastInfoVo> vos = records.stream().map(item -> {
                SyncContrastInfoVo syncContrastInfoVo = new SyncContrastInfoVo();
                BeanUtils.copyProperties(item, syncContrastInfoVo);
                return syncContrastInfoVo;
            }).collect(Collectors.toList());

            // 封装对比详情
            packageContrast(vos, clientContrastListBo.getTenantId());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    /**
     * 封装对比详情
     * @param list 对比列表
     * @param tenantId 租户id
     */
    private void packageContrast(List<SyncContrastInfoVo> list, Long tenantId) {
        if(list != null && list.size() > 0) {

            // 封装视频和文件信息
            Set<String> videoIds = new HashSet<>();
            Set<String> fileIds = new HashSet<>();
            Set<Long> userIds = new HashSet<>();
            for (SyncContrastInfoVo syncContrastInfoVo : list) {
                if(syncContrastInfoVo.getContrastType() == 0) {
                    videoIds.add(syncContrastInfoVo.getVideoOneId());
                    videoIds.add(syncContrastInfoVo.getVideoTwoId());
                }else {
                    fileIds.add(syncContrastInfoVo.getFileOneId());
                    fileIds.add(syncContrastInfoVo.getFileTwoId());
                }
            }
            List<AnchorVideoInfoVo> anchorVideoInfoVoList = null;
            List<UploadFileInfoVo> uploadFileInfoVoList = null;
            if(videoIds.size() > 0) {
                // 获取视频信息
                List<AnchorVideoEntity> anchorVideoEntities = this.anchorVideoService.list(new QueryWrapper<AnchorVideoEntity>().in("video_id", videoIds));
                if(anchorVideoEntities != null && anchorVideoEntities.size() > 0) {
                    anchorVideoInfoVoList = anchorVideoEntities.stream().map(item -> {
                        AnchorVideoInfoVo anchorVideoInfoVo = new AnchorVideoInfoVo();
                        BeanUtils.copyProperties(item, anchorVideoInfoVo);
                        userIds.add(anchorVideoInfoVo.getUserId());
                        return anchorVideoInfoVo;
                    }).toList();
                }
            }
            if(fileIds.size() > 0) {
                List<UploadFileEntity> uploadFileEntities = this.uploadFileService.list(new QueryWrapper<UploadFileEntity>().in("file_id", fileIds));
                if(uploadFileEntities != null && uploadFileEntities.size() > 0) {
                    uploadFileInfoVoList = uploadFileEntities.stream().map(item -> {
                        UploadFileInfoVo uploadFileInfoVo = new UploadFileInfoVo();
                        BeanUtils.copyProperties(item, uploadFileInfoVo);
                        return uploadFileInfoVo;
                    }).toList();
                }
            }

            Set<String> anchorIds = new HashSet<>();

            for (SyncContrastInfoVo syncContrastInfoVo : list) {
                if(syncContrastInfoVo.getContrastType() == 0) {
                    // 设置视频信息
                    if(anchorVideoInfoVoList != null && anchorVideoInfoVoList.size() > 0) {
                        for (AnchorVideoInfoVo anchorVideoInfoVo : anchorVideoInfoVoList) {
                            if(anchorVideoInfoVo.getVideoId().equals(syncContrastInfoVo.getVideoOneId())) {
                                syncContrastInfoVo.setVideoOneInfo(anchorVideoInfoVo);
                            }
                            if(anchorVideoInfoVo.getVideoId().equals(syncContrastInfoVo.getVideoTwoId())) {
                                syncContrastInfoVo.setVideoTwoInfo(anchorVideoInfoVo);
                            }
                        }
                    }
                    anchorIds.add(syncContrastInfoVo.getAnchorOneId());
                    anchorIds.add(syncContrastInfoVo.getAnchorTwoId());
                }else {
                    // 设置文件信息
                    if(uploadFileInfoVoList != null && uploadFileInfoVoList.size() > 0) {
                        for (UploadFileInfoVo uploadFileInfoVo : uploadFileInfoVoList) {
                            if(uploadFileInfoVo.getFileId().equals(syncContrastInfoVo.getFileOneId())) {
                                syncContrastInfoVo.setFileOneInfo(uploadFileInfoVo);
                            }
                            if(uploadFileInfoVo.getFileId().equals(syncContrastInfoVo.getFileTwoId())) {
                                syncContrastInfoVo.setFileTwoInfo(uploadFileInfoVo);
                            }
                        }
                    }
                }
            }

            // 封装主播信息
            if(anchorIds.size() > 0 && userIds.size() > 0) {

                QueryWrapper<AnchorUrlUserEntity> anchorUrlWrapper = new QueryWrapper<>();
                anchorUrlWrapper.in("user_id", userIds);
                anchorUrlWrapper.in("anchor_url_sec_uid", anchorIds);
                if(tenantId != null) {
                    anchorUrlWrapper.eq("tenant_id", tenantId);
                }
                List<AnchorUrlUserEntity> anchorUrlUserEntities = this.anchorUrlUserService.list(anchorUrlWrapper);
                if(anchorUrlUserEntities != null && anchorUrlUserEntities.size() > 0) {
                    Set<String> secUids = anchorUrlUserEntities.stream().map(AnchorUrlUserEntity::getAnchorUrlSecUid).collect(Collectors.toSet());
                    List<AnchorUrlEntity> anchorUrlEntities = anchorUrlService.list(new QueryWrapper<AnchorUrlEntity>().in("sec_uid", secUids));

                    if(anchorUrlEntities != null && anchorUrlEntities.size() > 0) {

                        Map<String, AnchorUrlEntity> anchorMap = anchorUrlEntities.stream().collect(Collectors.toMap(AnchorUrlEntity::getSecUid, item -> item, (o1, o2) -> o1));

                        for (SyncContrastInfoVo syncContrastInfoVo : list) {
                            if(syncContrastInfoVo.getVideoOneInfo() != null && syncContrastInfoVo.getVideoTwoInfo() != null) {
                                for (AnchorUrlUserEntity anchorUrlUserEntity : anchorUrlUserEntities) {
                                    if(anchorUrlUserEntity.getAnchorUrlSecUid().equals(syncContrastInfoVo.getVideoOneInfo().getSecUid()) &&
                                            anchorUrlUserEntity.getUserId().equals(syncContrastInfoVo.getVideoOneInfo().getUserId())) {
                                        AnchorUrlEntity anchorUrlEntity = anchorMap.get(anchorUrlUserEntity.getAnchorUrlSecUid());
                                        if(anchorUrlEntity != null) {
                                            AnchorUrlInfoVo anchorUrlInfoVo = BeanConvertUtils.convert(anchorUrlEntity, AnchorUrlInfoVo.class);
                                            anchorUrlInfoVo.setAnchorName(anchorUrlUserEntity.getRemarksName());
                                            syncContrastInfoVo.setAnchorOneInfo(anchorUrlInfoVo);
                                        }
                                    }
                                    if(anchorUrlUserEntity.getAnchorUrlSecUid().equals(syncContrastInfoVo.getVideoTwoInfo().getSecUid()) &&
                                            anchorUrlUserEntity.getUserId().equals(syncContrastInfoVo.getVideoTwoInfo().getUserId())) {
                                        AnchorUrlEntity anchorUrlEntity = anchorMap.get(anchorUrlUserEntity.getAnchorUrlSecUid());
                                        if(anchorUrlEntity != null) {
                                            AnchorUrlInfoVo anchorUrlInfoVo = BeanConvertUtils.convert(anchorUrlEntity, AnchorUrlInfoVo.class);
                                            anchorUrlInfoVo.setAnchorName(anchorUrlUserEntity.getRemarksName());
                                            syncContrastInfoVo.setAnchorTwoInfo(anchorUrlInfoVo);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public List<String> listByAiFav(ClientAiFavListBo clientAiFavListBo) {

        QueryWrapper<SyncContrastEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(clientAiFavListBo.getTradeId())) {
            // 获取该行业的主播
            QueryWrapper<AnchorUrlUserEntity> anchorWrapper = new QueryWrapper<>();
            anchorWrapper.eq("user_id", clientAiFavListBo.getUserId());
            anchorWrapper.eq("trade_id", clientAiFavListBo.getTradeId());
            anchorWrapper.select("anchor_url_sec_uid");
            List<AnchorUrlUserEntity> anchorUrlUserEntities = this.anchorUrlUserService.list(anchorWrapper);

            if(anchorUrlUserEntities != null && anchorUrlUserEntities.size() > 0) {
                List<String> secUids = anchorUrlUserEntities.stream().map(AnchorUrlUserEntity::getAnchorUrlSecUid).collect(Collectors.toList());
                wrapper.and(w -> {
                    w.in("anchor_one_id", secUids).or().in("anchor_two_id", secUids);
                });
            }else {
                wrapper.eq("id", "0");
            }
//            wrapper.and(w -> {
//                w.eq("trade_one_id", clientAiFavListBo.getTradeId()).or().eq("trade_two_id", clientAiFavListBo.getTradeId());
//            });
        }
        if((clientAiFavListBo.getSecUidArr() != null && clientAiFavListBo.getSecUidArr().size() > 0)) {
            wrapper.and(w -> {
                w.in("anchor_one_id", clientAiFavListBo.getSecUidArr()).or().eq("anchor_two_id", clientAiFavListBo.getSecUidArr());
            });
        }
        if(!StringUtils.isEmpty(clientAiFavListBo.getFileName())) {
            QueryWrapper<UploadFileEntity> fileWrapper = new QueryWrapper<>();
            if(!StringUtils.isEmpty(clientAiFavListBo.getUserId())) {
                fileWrapper.eq("user_id", clientAiFavListBo.getUserId());
            }
            if(!StringUtils.isEmpty(clientAiFavListBo.getTenantId())) {
                fileWrapper.eq("tenant_id", clientAiFavListBo.getTenantId());
            }
            fileWrapper.like("file_name", clientAiFavListBo.getFileName());
            fileWrapper.select("file_id");
            List<UploadFileEntity> uploadFileEntities = this.uploadFileService.list(fileWrapper);
            if(uploadFileEntities != null && uploadFileEntities.size() > 0) {
                Set<String> fileIds = uploadFileEntities.stream().map(UploadFileEntity::getFileId).collect(Collectors.toSet());
                wrapper.and(w -> {
                    w.in("file_one_id", fileIds).or().in("file_two_id", fileIds);
                });
            }else {
                wrapper.eq("id", 0);
            }
        }
        if(!StringUtils.isEmpty(clientAiFavListBo.getContrastType())) {
            wrapper.eq("contrast_type", clientAiFavListBo.getContrastType());
        }
        wrapper.eq("user_id", clientAiFavListBo.getUserId());
        wrapper.eq("tenant_id", clientAiFavListBo.getTenantId());
        wrapper.eq("delete_status", 0);

        List<SyncContrastEntity> syncContrastEntities = this.syncContrastService.list(wrapper);
        if(syncContrastEntities != null && syncContrastEntities.size() > 0) {
            return syncContrastEntities.stream().map(SyncContrastEntity::getContrastId).toList();
        }
        return null;
    }

    @Override
    public List<SyncContrastInfoVo> listByContrastIds(List<String> contrastIds, Long tenant) {
        List<SyncContrastEntity> syncContrastEntities = this.syncContrastService.list(new QueryWrapper<SyncContrastEntity>().in("contrast_id", contrastIds));
        if(syncContrastEntities != null && syncContrastEntities.size() > 0) {
            List<SyncContrastInfoVo> syncContrastInfoVos = syncContrastEntities.stream().map(item -> {
                SyncContrastInfoVo syncContrastInfoVo = new SyncContrastInfoVo();
                BeanUtils.copyProperties(item, syncContrastInfoVo);
                return syncContrastInfoVo;
            }).collect(Collectors.toList());

            // 封装视频、文件、主播等信息
            packageContrast(syncContrastInfoVos, tenant);

            return syncContrastInfoVos;
        }
        return null;
    }

    @Override
    public List<String> getAllowDeleteContrast(List<String> ids, Long tenantId, Long userId) {

        QueryWrapper<SyncContrastEntity> wrapper = new QueryWrapper<>();
        wrapper.in("contrast_id", ids);
        wrapper.eq("tenant_id", tenantId);
        if(!StringUtils.isEmpty(userId)) {
            wrapper.eq("user_id", userId);
        }
        wrapper.select("contrast_id");
        List<SyncContrastEntity> syncContrastEntities = this.syncContrastService.list(wrapper);
        if(syncContrastEntities != null && syncContrastEntities.size() > 0) {
            return syncContrastEntities.stream().map(SyncContrastEntity::getContrastId).collect(Collectors.toList());
        }
        return null;
    }

    @Override
    public void removeByContrastIds(List<String> delIds) {
        QueryWrapper<SyncContrastEntity> wrapper = new QueryWrapper<>();
        wrapper.in("contrast_id", delIds);

        this.syncContrastService.remove(wrapper);
    }

    @Override
    public void batchUpdateContrastDelStatus(List<String> contrastIds, Integer deleteStatus) {

        UpdateWrapper<SyncContrastEntity> wrapper = new UpdateWrapper<>();
        wrapper.in("contrast_id", contrastIds);

        wrapper.set("delete_status", deleteStatus);

        this.syncContrastService.update(wrapper);
    }

    @Override
    public void batchUpdateContrastIsShare(List<String> contrastIds, Integer isShare) {

        UpdateWrapper<SyncContrastEntity> wrapper = new UpdateWrapper<>();
        wrapper.in("contrast_id", contrastIds);

        wrapper.set("is_shard", isShare);
        if(isShare == 0) {
            wrapper.set("share_url", "");
        }

        this.syncContrastService.update(wrapper);
    }

    @Override
    public void batchUpdateContrastDelStatusByVideoIds(List<String> videoIds, Integer deleteStatus) {

        UpdateWrapper<SyncContrastEntity> wrapper = new UpdateWrapper<>();
        wrapper.and(w -> {
            w.in("video_one_id", videoIds).or().in("video_two_id", videoIds);
        });

        wrapper.set("delete_status", deleteStatus);

        this.syncContrastService.update(wrapper);

    }

    @Override
    public SyncContrastInfoVo checkExistUpdate(Integer contrastType, String uuid1, String uuid2, Long userId, Integer isCloud, Integer syncScene) {

        QueryWrapper<SyncContrastEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("contrast_type", contrastType);
        if(contrastType == 0) {
            wrapper.in("video_one_id", uuid1, uuid2);
            wrapper.in("video_two_id", uuid1, uuid2);
        }else if(contrastType == 1) {
            wrapper.in("file_one_id", uuid1, uuid2);
            wrapper.in("file_two_id", uuid1, uuid2);
        }
        if(userId != null) {
            wrapper.eq("user_id", userId);
        }
        wrapper.in("delete_status", 0, 1);
        wrapper.orderByDesc("update_date");

        List<SyncContrastEntity> syncContrastEntities = this.syncContrastService.list(wrapper);

        if(syncContrastEntities != null && syncContrastEntities.size() > 0) {
            SyncContrastEntity syncContrastEntity = syncContrastEntities.get(0);
            syncContrastEntity.setUpdateDate(new Date());
            // 如果不是云空间的对比,修改删除状态为未删除
            if(isCloud == 0) {
                syncContrastEntity.setDeleteStatus(0);
            }else {
                syncContrastEntity.setIsShard(1);
                syncContrastEntity.setShareUrl(wordsProperties.getCloudSpaceUrl() + "contrastOnlineAnalysis/" + syncContrastEntity.getContrastId());
            }

            if(syncScene != null) {
                syncContrastEntity.setSyncScene(syncScene);
            }

            this.syncContrastService.updateById(syncContrastEntity);

            SyncContrastInfoVo syncContrastInfoVo = new SyncContrastInfoVo();
            BeanUtils.copyProperties(syncContrastEntity, syncContrastInfoVo);
            return syncContrastInfoVo;
        }

        return null;
    }

    @Override
    public PageUtils<SyncContrastListVo> pageSyncContrastNew(SyncContrastListBo syncContrastListBo) {
        Page<SyncContrastListVo> iPage = syncContrastService.pageSyncContrastNew(syncContrastListBo);

        if (iPage != null && ObjectUtil.isNotEmpty(iPage.getRecords())) {

            // 封装视频、文件、行业信息
            setVideoNickName(iPage.getRecords());
        }
        PageUtils<SyncContrastListVo> pageUtils = new PageUtils<>(syncContrastListBo.getPage(), syncContrastListBo.getLimit(), iPage);
        List<SyncContrastListVo> records = iPage.getRecords();
        if (records != null && !records.isEmpty()) {
            pageUtils.setList(iPage.getRecords());
        }
        return pageUtils;
    }

    private void setVideoNickName(List<SyncContrastListVo> list) {
        if (ObjectUtil.isEmpty(list)) {
            return;
        }
        Set<Long> tradeIds = new HashSet<>();

        // 获取视频ID
        List<String> videoIds = list.stream()
                .flatMap(item -> Stream.of(item.getVideoOneId(), item.getVideoTwoId()))
                .filter(ObjectUtil::isNotEmpty)
                .distinct()
                .toList();
        Map<String, AnchorVideoEntity> videoMap = videoIds.isEmpty() ? new HashMap<>() : anchorVideoService.lambdaQuery()
                .in(AnchorVideoEntity::getVideoId, videoIds)
                .select(AnchorVideoEntity::getVideoName, AnchorVideoEntity::getTradeId, AnchorVideoEntity::getVideoId)
                .list().stream()
                .peek(item -> tradeIds.add(item.getTradeId()))
                .collect(Collectors.toMap(AnchorVideoEntity::getVideoId, Function.identity(), (a, b) -> a));

        // 获取文件ID
        List<String> fileIds = list.stream()
                .flatMap(item -> Stream.of(item.getFileOneId(), item.getFileTwoId()))
                .filter(ObjectUtil::isNotEmpty)
                .distinct()
                .toList();
        Map<String, UploadFileEntity> fileMap = fileIds.isEmpty() ? new HashMap<>() : uploadFileService.lambdaQuery()
                .in(UploadFileEntity::getFileId, fileIds)
                .select(UploadFileEntity::getFileName, UploadFileEntity::getTradeId, UploadFileEntity::getFileId)
                .list().stream()
                .peek(item -> tradeIds.add(item.getTradeId()))
                .collect(Collectors.toMap(UploadFileEntity::getFileId, Function.identity(), (a, b) -> a));

        // 行业
        Map<Long, String> tradeMap = tradeIds.isEmpty() ? new HashMap<>() : tradeService.lambdaQuery()
                .in(TradeEntity::getId, tradeIds)
                .select(TradeEntity::getName, TradeEntity::getId)
                .list()
                .stream()
                .collect(Collectors.toMap(TradeEntity::getId, TradeEntity::getName, (a, b) -> a));

        list.forEach(item -> {
            if (ObjectUtil.isNotEmpty(item.getVideoOneId())) {
                AnchorVideoEntity video = videoMap.get(item.getVideoOneId());
                item.setVideoNickNameOne(video == null ? null : video.getVideoName());
                item.setTradeNickNameOne(video == null ? null : tradeMap.getOrDefault(video.getTradeId(), null));
            }
            if (ObjectUtil.isNotEmpty(item.getVideoTwoId())) {
                AnchorVideoEntity video = videoMap.get(item.getVideoTwoId());
                item.setVideoNickNameTwo(video == null ? null : video.getVideoName());
                item.setTradeNickNameTwo(video == null ? null : tradeMap.getOrDefault(video.getTradeId(), null));
            }

            if (ObjectUtil.isNotEmpty(item.getFileOneId())) {
                UploadFileEntity file = fileMap.get(item.getFileOneId());
                item.setVideoNickNameOne(file == null ? null : file.getFileName());
                item.setTradeNickNameOne(file == null ? null : tradeMap.getOrDefault(file.getTradeId(), null));
            }
            if (ObjectUtil.isNotEmpty(item.getFileTwoId())) {
                UploadFileEntity file = fileMap.get(item.getFileTwoId());
                item.setVideoNickNameTwo(file == null ? null : file.getFileName());
                item.setTradeNickNameTwo(file == null ? null : tradeMap.getOrDefault(file.getTradeId(), null));
            }
        });

    }

    @Override
    public Boolean existsByTenantId(Long tenantId) {
        if (ObjectUtil.isEmpty(tenantId)) {
            return Boolean.FALSE;
        }
        Long count = syncContrastService.lambdaQuery()
                .eq(SyncContrastEntity::getTenantId, tenantId)
                .eq(SyncContrastEntity::getIsDeleted, 0)
                .last("limit 1")
                .count();
        return count != null && count > 0;
    }
}

