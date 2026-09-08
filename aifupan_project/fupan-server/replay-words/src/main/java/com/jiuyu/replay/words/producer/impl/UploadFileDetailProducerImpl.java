package com.jiuyu.replay.words.producer.impl;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.common.constant.CustomizeConstant;
import com.jiuyu.replay.common.constant.WordsEnum;
import com.jiuyu.replay.common.utils.DataUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.bo.words.GenerateVideoContentBo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.words.VideoContentVo;
import com.jiuyu.replay.words.bo.file.UploadFileDetailBo;
import com.jiuyu.replay.words.bo.file.UploadFileDetailListBo;
import com.jiuyu.replay.words.entity.UploadFileDetailEntity;
import com.jiuyu.replay.words.entity.UploadFileEntity;
import com.jiuyu.replay.words.producer.UploadFileDetailProducer;
import com.jiuyu.replay.words.repository.service.UploadFileDetailService;
import com.jiuyu.replay.words.repository.service.impl.UploadFileServiceImpl;
import com.jiuyu.replay.words.vo.file.UploadFileDetailInfoVo;
import com.jiuyu.replay.words.vo.file.UploadFileDetailListVo;
import com.jiuyu.replay.words.vo.file.UploadFileDetailVo;
import com.jiuyu.replay.words.vo.file.UploadFileInfoVo;
import com.jiuyu.replay.words.vo.video.AnchorVideoFileAllVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


/**
 * 文件的详情
 *
 * @author lyw
 * @email 1798883178@qq.com
 * @date 2025-06-03 14:14:53
 */
@Service
@Slf4j
public class UploadFileDetailProducerImpl implements UploadFileDetailProducer {

    @Resource
    private UploadFileDetailService uploadFileDetailService;
    @Autowired
    private UploadFileServiceImpl uploadFileService;
    @Resource
    private RedissonClient redissonClient;


    @Override
    public PageUtils<UploadFileDetailListVo> queryPage(UploadFileDetailListBo uploadFileDetailListBo) {
        QueryWrapper<UploadFileDetailEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(uploadFileDetailListBo.getKeyword())){
            wrapper.like("name", uploadFileDetailListBo.getKeyword());
        }

        IPage<UploadFileDetailEntity> iPage = uploadFileDetailService.page(new Query<UploadFileDetailEntity>().getPage(uploadFileDetailListBo.getPage(), uploadFileDetailListBo.getLimit()), wrapper);

        PageUtils<UploadFileDetailListVo> pageUtils = new PageUtils<>(uploadFileDetailListBo.getPage(), uploadFileDetailListBo.getLimit(), iPage);

        List<UploadFileDetailEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<UploadFileDetailListVo> vos = records.stream().map(item -> {
                UploadFileDetailListVo uploadFileDetailVo = new UploadFileDetailListVo();
                BeanUtils.copyProperties(item, uploadFileDetailVo);
                return uploadFileDetailVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public UploadFileDetailInfoVo info(Long id) {

        UploadFileDetailEntity uploadFileDetailEntity = uploadFileDetailService.getById(id);
        if(uploadFileDetailEntity != null) {
            UploadFileDetailInfoVo uploadFileDetailInfoVo = new UploadFileDetailInfoVo();
            BeanUtils.copyProperties(uploadFileDetailEntity, uploadFileDetailInfoVo);
            return uploadFileDetailInfoVo;
        }

        return null;
    }

    @Override
    public UploadFileDetailInfoVo save(UploadFileDetailBo uploadFileDetailBo) {

         UploadFileDetailEntity uploadFileDetailEntity = new UploadFileDetailEntity();
         BeanUtils.copyProperties(uploadFileDetailBo, uploadFileDetailEntity);
         uploadFileDetailEntity.setId(SnowflakeManager.nextValue());
         uploadFileDetailEntity.setCreateDate(new Date());
         uploadFileDetailEntity.setUpdateDate(new Date());

         uploadFileDetailService.save(uploadFileDetailEntity);

         UploadFileDetailInfoVo uploadFileDetailInfoVo = new UploadFileDetailInfoVo();
         BeanUtils.copyProperties(uploadFileDetailEntity, uploadFileDetailInfoVo);

         return uploadFileDetailInfoVo;
     }

    @Override
    public Boolean update(UploadFileDetailBo uploadFileDetailBo) {

        UploadFileDetailEntity uploadFileDetailEntity = new UploadFileDetailEntity();
        BeanUtils.copyProperties(uploadFileDetailBo, uploadFileDetailEntity);
        uploadFileDetailEntity.setUpdateDate(new Date());

        return uploadFileDetailService.updateById(uploadFileDetailEntity);
    }

    @Override
    public void deleteById(Long id) {

        uploadFileDetailService.removeById(id);
    }

    @Override
    public UploadFileDetailVo getFileContent(String sourceId, Integer type, AnchorVideoFileAllVo videoFileAllVo) {
        UploadFileDetailVo vo = new UploadFileDetailVo();
        List<UploadFileDetailEntity> list = uploadFileDetailService.lambdaQuery()
                .eq(UploadFileDetailEntity::getFileId, sourceId)
                .list();
        if (!list.isEmpty()){
            UploadFileDetailEntity uploadFileDetailEntity = list.get(0);
            BeanUtil.copyProperties(uploadFileDetailEntity,vo);
            videoFileAllVo.setSourceId(uploadFileDetailEntity.getFileId());
            if (type == 1){   //1是自然原文
                videoFileAllVo.setContentStatus(uploadFileDetailEntity.getNatureContentStatus());
                videoFileAllVo.setType(1);
            }else {           //2是优化原文
                videoFileAllVo.setContentStatus(uploadFileDetailEntity.getOptimizeContentStatus());
                videoFileAllVo.setType(2);
            }
            videoFileAllVo.setVideoFileContentList(new ArrayList<>());
        }
        return vo;
    }


    @Override
    public List<VideoContentVo> selFileContents(String sourceId, Integer type, Integer sourceType, Long userId, Long tenantId) {
        List<VideoContentVo> vos = uploadFileDetailService.selFileContents(sourceId, type, sourceType, userId, tenantId);
        if (vos!=null&&!vos.isEmpty()){
            vos.forEach(x->{
                if (!x.getContent().isBlank()){
                    x.setContentList(JSONUtil.toList(x.getContent(),String.class));
                }
            });
        }
        return vos;
    }

    @Override
    public void inserto(GenerateVideoContentBo bo) {
        uploadFileDetailService.inserto(bo.getSourceId(),bo.getType(),bo.getSourceType(),bo.getUserId(),bo.getTenantId());
    }

    @Override
    public UploadFileDetailVo getByFileId(GenerateVideoContentBo bo,Long setTime) {
        List<UploadFileDetailEntity> list = uploadFileDetailService.lambdaQuery().eq(UploadFileDetailEntity::getFileId, bo.getSourceId()).list();
        if (!list.isEmpty()){
            UploadFileDetailEntity uploadFileDetailEntity = list.get(0);
            if (bo.getType()==1){
                uploadFileDetailEntity.setNatureContentStatus(1);
                uploadFileDetailEntity.setNatureSourceType(WordsEnum.contentSourceType.SERVER.getCode());
                uploadFileDetailEntity.setNatSetTime(setTime);
            }
            if (bo.getType()==2){
                uploadFileDetailEntity.setOptimizeContentStatus(1);
                uploadFileDetailEntity.setOptimizeSourceType(WordsEnum.contentSourceType.SERVER.getCode());
                uploadFileDetailEntity.setOptSetTime(setTime);
            }
            uploadFileDetailEntity.setUpdateDate(new Date());
            boolean updated = uploadFileDetailService.updateById(uploadFileDetailEntity);
            if (updated){
                return BeanUtil.toBean(uploadFileDetailEntity, UploadFileDetailVo.class);
            }
            return null;
        }
        return null;
    }

    @Override
    public void updateFileDetailById(UploadFileDetailVo uploadFileDetailVo) {
        if (ObjectUtil.isNotNull(uploadFileDetailVo)){
            UploadFileDetailEntity bean = BeanUtil.toBean(uploadFileDetailVo, UploadFileDetailEntity.class);
            uploadFileDetailService.updateById(bean);
        }
    }

    @Override
    public UploadFileDetailVo getByFile(String sourceId) {
        List<UploadFileDetailEntity> list = uploadFileDetailService.lambdaQuery()
                .eq(UploadFileDetailEntity::getFileId, sourceId)
//                .eq(UploadFileDetailEntity::getTenantId, tenantId)
//                .eq(UploadFileDetailEntity::getUserId, userId)
                .list();
        if (!list.isEmpty()){
            UploadFileDetailEntity uploadFileDetailEntity = list.get(0);
            UploadFileDetailVo videoDetailVo = new UploadFileDetailVo();
            BeanUtils.copyProperties(uploadFileDetailEntity,videoDetailVo);
            return videoDetailVo;
        }
        return null;
    }

    @Override
    public UploadFileDetailVo getEmtyByFileId(String videoId) {
        List<UploadFileDetailEntity> list = uploadFileDetailService.lambdaQuery()
                .eq(UploadFileDetailEntity::getFileId, videoId)
                .list();
        if (!list.isEmpty()){
            UploadFileDetailEntity uploadFileDetailEntity = list.get(0);
            UploadFileDetailVo videoDetailVo = new UploadFileDetailVo();
            BeanUtils.copyProperties(uploadFileDetailEntity,videoDetailVo);
            return videoDetailVo;
        }
        return null;
    }

    @Override
    public List<UploadFileDetailInfoVo> contentByToGenerated(List<String> fileIds, Long userId, Long tenantId) {
        List<UploadFileDetailEntity> list = uploadFileDetailService.list(new LambdaQueryWrapper<UploadFileDetailEntity>()
                .in(ObjectUtil.isNotEmpty(fileIds), UploadFileDetailEntity::getFileId, fileIds)
                .eq(UploadFileDetailEntity::getUserId, userId)
                .eq(UploadFileDetailEntity::getTenantId, tenantId)
                .and(e -> e
                        .eq(UploadFileDetailEntity::getNatureContentStatus, 1)
                        .or()
                        .eq(UploadFileDetailEntity::getOptimizeContentStatus, 1)
                )
        );
        if (ObjectUtil.isNotEmpty(list)) {
            List<UploadFileDetailInfoVo> uploadFileDetailInfoVos = BeanUtil.copyToList(list, UploadFileDetailInfoVo.class);

            List<UploadFileInfoVo> list1 = uploadFileService.list(new LambdaQueryWrapper<UploadFileEntity>()
                            .in(UploadFileEntity::getFileId, list.stream().map(UploadFileDetailEntity::getFileId).distinct().toList())
                    )
                    .stream()
                    .map(item -> {
                        UploadFileInfoVo uploadFileInfoVo = BeanUtil.copyProperties(item, UploadFileInfoVo.class);
                        uploadFileInfoVo.setId(null);
                        return uploadFileInfoVo;
                    }).toList();

            // 设置值
            DataUtils.setFieldObject(uploadFileDetailInfoVos, "fileId", "uploadFile", list1, "fileId");

            return uploadFileDetailInfoVos;
        }
        return new ArrayList<>();
    }

    @Override
    public boolean updateContentStatus(UploadFileDetailBo detailBo) {

        Date now = new Date();
        return this.uploadFileDetailService.update(new LambdaUpdateWrapper<UploadFileDetailEntity>()
                .eq(UploadFileDetailEntity::getFileId, detailBo.getFileId())
                .set(ObjectUtil.isNotEmpty(detailBo.getOptimizeContentStatus()), UploadFileDetailEntity::getOptimizeContentStatus, detailBo.getOptimizeContentStatus())
                .set(ObjectUtil.isNotEmpty(detailBo.getNatureContentStatus()), UploadFileDetailEntity::getNatureContentStatus, detailBo.getNatureContentStatus())
                .set(ObjectUtil.isNotEmpty(detailBo.getOptimizeContentStatus()) && detailBo.getOptimizeContentStatus() == 1, UploadFileDetailEntity::getOptSetTime, now.getTime())
                .set(ObjectUtil.isNotEmpty(detailBo.getNatureContentStatus()) && detailBo.getNatureContentStatus() == 1, UploadFileDetailEntity::getNatSetTime, now.getTime())
                .set(UploadFileDetailEntity::getUpdateDate, now)
        );

    }

    @Override
    public UploadFileDetailInfoVo getAndSave(UploadFileDetailBo uploadFileDetailBo) {
        UploadFileDetailEntity one = uploadFileDetailService.getOne(new LambdaQueryWrapper<UploadFileDetailEntity>()
                .eq(UploadFileDetailEntity::getFileId, uploadFileDetailBo.getFileId())
                .last(CustomizeConstant.SELECT_ONE_LAST_SQL.getValue()));

        if (one != null) {
            return BeanUtil.copyProperties(one, UploadFileDetailInfoVo.class);
        }

        UploadFileEntity video = uploadFileService.getOne(new LambdaQueryWrapper<UploadFileEntity>()
                .eq(UploadFileEntity::getFileId, uploadFileDetailBo.getFileId())
                .last(CustomizeConstant.SELECT_ONE_LAST_SQL.getValue())
        );
        RRException.isNotEmpty(video, "上传的文件不存在");
        String lockKey = "saveUploadFIleDetail_" + uploadFileDetailBo.getFileId();
        RLock lock = redissonClient.getLock(lockKey);
        try {
            // 3. 尝试获取锁
            boolean isLocked = lock.tryLock(
                    10, // 等待时间
                    10, // 锁自动释放时间
                    TimeUnit.SECONDS
            );

            if (isLocked) {

                one = uploadFileDetailService.getOne(new LambdaQueryWrapper<UploadFileDetailEntity>()
                        .eq(UploadFileDetailEntity::getFileId, uploadFileDetailBo.getFileId())
                        .last(CustomizeConstant.SELECT_ONE_LAST_SQL.getValue()));
                if (one == null) {
                    UploadFileDetailEntity uploadFileEntity = new UploadFileDetailEntity();
                    BeanUtils.copyProperties(uploadFileDetailBo, uploadFileEntity);
                    uploadFileEntity.setId(SnowflakeManager.nextValue());
                    uploadFileEntity.setUserId(video.getUserId());
                    uploadFileEntity.setTenantId(video.getTenantId());
                    uploadFileEntity.setCreateDate(new Date());
                    uploadFileEntity.setUpdateDate(new Date());
                    uploadFileDetailService.save(uploadFileEntity);
                }
            }

        } catch (InterruptedException e) {
            log.error("获取分布式锁失败，Key: {}", lockKey);
        } finally {
            // 5. 释放锁（确保当前线程持有锁）
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        if (one == null) {
            one = uploadFileDetailService.getOne(new LambdaQueryWrapper<UploadFileDetailEntity>()
                    .eq(UploadFileDetailEntity::getFileId, uploadFileDetailBo.getFileId())
                    .last(CustomizeConstant.SELECT_ONE_LAST_SQL.getValue()));
        }

        if (one != null) {
            return BeanUtil.copyProperties(one, UploadFileDetailInfoVo.class);
        }
        return null;
    }

    @Override
    public boolean updateContentStatus(Long id, int type, int contentStatus) {
        Date now = new Date();
        boolean update = this.uploadFileDetailService.update(new LambdaUpdateWrapper<UploadFileDetailEntity>()
                .eq(UploadFileDetailEntity::getId, id)
                .set(type == WordsEnum.contentType.NATURE.getCode(), UploadFileDetailEntity::getNatureContentStatus, contentStatus)
                .set(type == WordsEnum.contentType.OPTIMIZE.getCode(), UploadFileDetailEntity::getOptimizeContentStatus, contentStatus)
                .set(type == WordsEnum.contentType.NATURE.getCode() && contentStatus == WordsEnum.contentStatus.GENERATING.getCode(), UploadFileDetailEntity::getNatSetTime, now.getTime())
                .set(type == WordsEnum.contentType.OPTIMIZE.getCode() && contentStatus == WordsEnum.contentStatus.GENERATING.getCode(), UploadFileDetailEntity::getOptSetTime, now.getTime())
                .set(UploadFileDetailEntity::getUpdateDate, now)
        );
        if (!update) {
            log.error("anchorVideoDetailService.update失败,id = {}, type = {}, status = {}", id, type, contentStatus);
        }
        return update;
    }

    /**
     * 根据 sourceId（fileId）更新文件内容生成状态。
     * 与 {@code AnchorVideoDetailProducerImpl#updateContentStatusBySourceId} 逻辑对称，
     * 操作的是文件表（{@code tb_upload_file_detail}）。
     *
     * @param sourceId 文件 id（fileId）
     * @param type     内容类型：1=自然原文, 2=优化原文
     * @param status   目标状态：2=SUCCESS, 3=FAILED
     * @param resetJob 是否同时重置 set_job=0
     * @return true=更新成功
     */
    @Override
    public boolean updateContentStatusBySourceId(String sourceId, Integer type, int status, boolean resetJob) {
        Date now = new Date();
        LambdaUpdateWrapper<UploadFileDetailEntity> wrapper = new LambdaUpdateWrapper<UploadFileDetailEntity>()
                .eq(UploadFileDetailEntity::getFileId, sourceId)
                .set(type == WordsEnum.contentType.NATURE.getCode(), UploadFileDetailEntity::getNatureContentStatus, status)
                .set(type == WordsEnum.contentType.OPTIMIZE.getCode(), UploadFileDetailEntity::getOptimizeContentStatus, status)
                .set(UploadFileDetailEntity::getUpdateDate, now);
        if (resetJob) {
            wrapper.set(type == WordsEnum.contentType.NATURE.getCode(), UploadFileDetailEntity::getNatSetJob, 0)
                   .set(type == WordsEnum.contentType.OPTIMIZE.getCode(), UploadFileDetailEntity::getOptSetJob, 0);
        }
        return uploadFileDetailService.update(wrapper);
    }

    /**
     * 查询文件表中待生成自然/优化原文的记录列表。
     *
     * <h3>查询条件</h3>
     * <ul>
     *   <li>文件分析状态 = 2（分析完成）</li>
     *   <li>content_status = 1（待生成）</li>
     *   <li>set_job = 0（未被拾取）</li>
     *   <li>source_type = 0（服务端来源，排除客户端手动触发的）</li>
     *   <li>is_deleted = 0（未删除）</li>
     * </ul>
     *
     * @param limit 最大返回条数
     * @return 待处理文件列表，无数据时返回空列表
     */
    @Override
    public List<UploadFileDetailVo> selectFileDetailData(Integer limit) {
        List<UploadFileDetailEntity> list = uploadFileDetailService.selectFileDetailData(limit);
        if (ObjectUtil.isEmpty(list)) {
            return List.of();
        }
        return BeanUtil.copyToList(list, UploadFileDetailVo.class);
    }

    /**
     * 乐观锁标记文件任务为"已拾取"，与 {@code AnchorVideoDetailProducerImpl#markPickedUp} 逻辑对称。
     *
     * @param sourceId 文件 id（fileId）
     * @param type     内容类型：1=自然原文, 2=优化原文
     * @return true=拾取成功, false=已被其他实例抢先拾取
     */
    @Override
    public boolean markPickedUp(String sourceId, Integer type) {
        long now = System.currentTimeMillis();
        LambdaUpdateWrapper<UploadFileDetailEntity> wrapper = new LambdaUpdateWrapper<UploadFileDetailEntity>()
                .eq(UploadFileDetailEntity::getFileId, sourceId)
                .set(UploadFileDetailEntity::getUpdateDate, new Date());
        if (type == WordsEnum.contentType.NATURE.getCode()) {
            wrapper.eq(UploadFileDetailEntity::getNatSetJob, 0)
                   .eq(UploadFileDetailEntity::getNatureContentStatus, 1)
                   .eq(UploadFileDetailEntity::getNatureSourceType, 0)
                   .set(UploadFileDetailEntity::getNatSetJob, 1)
                   .set(UploadFileDetailEntity::getNatSetTime, now);
        } else {
            wrapper.eq(UploadFileDetailEntity::getOptSetJob, 0)
                   .eq(UploadFileDetailEntity::getOptimizeContentStatus, 1)
                   .eq(UploadFileDetailEntity::getOptimizeSourceType, 0)
                   .set(UploadFileDetailEntity::getOptSetJob, 1)
                   .set(UploadFileDetailEntity::getOptSetTime, now);
        }
        return uploadFileDetailService.update(wrapper);
    }
}

