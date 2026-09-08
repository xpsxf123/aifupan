package com.jiuyu.replay.words.producer.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONException;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.common.constant.CustomizeConstant;
import com.jiuyu.replay.common.constant.WordsEnum;
import com.jiuyu.replay.common.utils.DataUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.bo.words.GenerateVideoContentBo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.generic.vo.words.*;
import com.jiuyu.replay.words.bo.video.AnchorVideoDetailBo;
import com.jiuyu.replay.words.bo.video.AnchorVideoDetailListBo;
import com.jiuyu.replay.words.entity.AnchorVideoDetailEntity;
import com.jiuyu.replay.words.entity.AnchorVideoEntity;
import com.jiuyu.replay.words.producer.AnchorVideoDetailProducer;
import com.jiuyu.replay.words.repository.service.AnchorVideoDetailService;
import com.jiuyu.replay.words.repository.service.AnchorVideoService;
import com.jiuyu.replay.words.vo.video.AnchorVideoFileAllVo;
import jakarta.annotation.Resource;
import jodd.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;


/**
 * 视频的详情
 *
 * @author lyw
 * @email 1798883178@qq.com
 * @date 2025-05-27 15:30:33
 */
@Service
@Slf4j
public class AnchorVideoDetailProducerImpl implements AnchorVideoDetailProducer {

    @Resource
    private AnchorVideoDetailService anchorVideoDetailService;
    @Resource
    private AnchorVideoService anchorVideoService;
    @Resource
    private RedissonClient redissonClient;


    @Override
    public PageUtils<AnchorVideoDetailListVo> queryPage(AnchorVideoDetailListBo anchorVideoDetailListBo) {
        QueryWrapper<AnchorVideoDetailEntity> wrapper = new QueryWrapper<>();
        if (ObjectUtil.isNotEmpty(anchorVideoDetailListBo.getKeyword())) {
            wrapper.like("name", anchorVideoDetailListBo.getKeyword());
        }

        IPage<AnchorVideoDetailEntity> iPage = anchorVideoDetailService.page(new Query<AnchorVideoDetailEntity>().getPage(anchorVideoDetailListBo.getPage(), anchorVideoDetailListBo.getLimit()), wrapper);

        PageUtils<AnchorVideoDetailListVo> pageUtils = new PageUtils<>(anchorVideoDetailListBo.getPage(), anchorVideoDetailListBo.getLimit(), iPage);

        List<AnchorVideoDetailEntity> records = iPage.getRecords();
        if (records != null && !records.isEmpty()) {
            List<AnchorVideoDetailListVo> vos = records.stream().map(item -> {
                AnchorVideoDetailListVo anchorVideoDetailVo = new AnchorVideoDetailListVo();
                BeanUtils.copyProperties(item, anchorVideoDetailVo);
                return anchorVideoDetailVo;
            }).toList();

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public AnchorVideoDetailInfoVo info(Long id) {

        AnchorVideoDetailEntity anchorVideoDetailEntity = anchorVideoDetailService.getById(id);
        if(anchorVideoDetailEntity != null) {
            AnchorVideoDetailInfoVo anchorVideoDetailInfoVo = new AnchorVideoDetailInfoVo();
            BeanUtils.copyProperties(anchorVideoDetailEntity, anchorVideoDetailInfoVo);
            return anchorVideoDetailInfoVo;
        }

        return null;
    }

    @Override
    public Boolean update(AnchorVideoDetailBo anchorVideoDetailBo) {

        AnchorVideoDetailEntity anchorVideoDetailEntity = new AnchorVideoDetailEntity();
        BeanUtils.copyProperties(anchorVideoDetailBo, anchorVideoDetailEntity);
        anchorVideoDetailEntity.setUpdateDate(new Date());

        return anchorVideoDetailService.updateById(anchorVideoDetailEntity);
    }

    @Override
    public void deleteById(Long id) {

        anchorVideoDetailService.removeById(id);
    }

    @Override
    public AnchorVideoDetailVo getVideoContent(String videoId, Integer type, AnchorVideoFileAllVo videoContentVo) {
        AnchorVideoDetailVo vo = new AnchorVideoDetailVo();
        List<AnchorVideoDetailEntity> list = anchorVideoDetailService.lambdaQuery()
                .eq(AnchorVideoDetailEntity::getVideoId, videoId)
                .list();
        if (!list.isEmpty()){
            AnchorVideoDetailEntity anchorVideoDetailEntity = list.get(0);
            BeanUtil.copyProperties(anchorVideoDetailEntity, vo);
            videoContentVo.setSourceId(videoId);
            if (type == 1){   //1是自然原文
                videoContentVo.setContentStatus(anchorVideoDetailEntity.getNatureContentStatus());
                videoContentVo.setType(1);
            }else {           //2是优化原文
                videoContentVo.setContentStatus(anchorVideoDetailEntity.getOptimizeContentStatus());
                videoContentVo.setType(2);
            }
            videoContentVo.setVideoFileContentList(new ArrayList<>());
        }
        return vo;
    }


    /**
     * 获取视频内容
     * @param videoId
     * @param type
     * @param sourceType
     * @param userId
     * @param tenantId
     * @return
     */
    @Override
    public List<VideoContentVo> selVideoContents(String videoId, Integer type,Integer sourceType,Long userId,Long tenantId) {
        List<VideoContentVo> vos = anchorVideoDetailService.selVideoContents(videoId, type, sourceType, userId, tenantId);
        if (ObjectUtil.isEmpty(vos)) {
            return new ArrayList<>();
        }
        List<VideoContentVo> list = vos.stream()
                .filter(x -> ObjectUtil.isNotEmpty(x.getContent()))
                .toList();
        if (ObjectUtil.isEmpty(list)) {
            return new ArrayList<>();
        }
        list.forEach(x -> {
            if (StringUtil.isNotBlank(x.getContent()) && x.getContent().startsWith("[")) {
                try {
                    x.setContentList(JSONUtil.toList(x.getContent(), String.class));
                } catch (JSONException e) {
                    log.error("JSON转换出错：{},videoId:{},sourceType:{},type:{}", x.getContent(),videoId,sourceType,type, e);
                    return ;
                }
            }
            x.setCueWord(null);
        });
        return list;

    }

    @Override
    public void saveVideoContents(List<VideoContentVo> voList,Long userId,Long tenantId,String videoId,Integer sourceType, Integer type) {

        anchorVideoDetailService.saveVideoContents(voList,userId,tenantId,videoId,sourceType,type);
    }


    @Override
    public void inserto(GenerateVideoContentBo bo) {
        AnchorVideoDetailBo videoDetailBo = new AnchorVideoDetailBo();
        videoDetailBo.setVideoId(bo.getSourceId());
        videoDetailBo.setUserId(bo.getUserId());
        videoDetailBo.setTenantId(bo.getTenantId());
        getAndSave(videoDetailBo);
    }

    @Override
    public AnchorVideoDetailInfoVo getAndSave(AnchorVideoDetailBo anchorVideoDetailBo) {

        AnchorVideoDetailEntity one = anchorVideoDetailService.getOne(new LambdaQueryWrapper<AnchorVideoDetailEntity>()
                .eq(AnchorVideoDetailEntity::getVideoId, anchorVideoDetailBo.getVideoId())
                .last(CustomizeConstant.SELECT_ONE_LAST_SQL.getValue()));

        if (one != null) {
            return BeanUtil.copyProperties(one, AnchorVideoDetailInfoVo.class);
        }

        AnchorVideoEntity video = anchorVideoService.getOne(new LambdaQueryWrapper<AnchorVideoEntity>()
                .eq(AnchorVideoEntity::getVideoId, anchorVideoDetailBo.getVideoId())
                .last(CustomizeConstant.SELECT_ONE_LAST_SQL.getValue())
        );
        BusinessException.requireNonEmpty(video, "视频不存在");

        String lockKey = "saveVideoDetail_" + anchorVideoDetailBo.getVideoId();
        RLock lock = redissonClient.getLock(lockKey);
        try {
            // 3. 尝试获取锁
            boolean isLocked = lock.tryLock(
                    10, // 等待时间
                    10, // 锁自动释放时间
                    TimeUnit.SECONDS);

            if (isLocked) {

                one = anchorVideoDetailService.getOne(new LambdaQueryWrapper<AnchorVideoDetailEntity>()
                        .eq(AnchorVideoDetailEntity::getVideoId, anchorVideoDetailBo.getVideoId())
                        .last(CustomizeConstant.SELECT_ONE_LAST_SQL.getValue()));

                if (one == null) {
                    AnchorVideoDetailEntity anchorVideoDetailEntity = new AnchorVideoDetailEntity();
                    BeanUtils.copyProperties(anchorVideoDetailBo, anchorVideoDetailEntity);
                    anchorVideoDetailEntity.setId(SnowflakeManager.nextValue());
                    anchorVideoDetailEntity.setUserId(video.getUserId());
                    anchorVideoDetailEntity.setTenantId(video.getTenantId());
                    anchorVideoDetailEntity.setCreateDate(new Date());
                    anchorVideoDetailEntity.setUpdateDate(new Date());
                    anchorVideoDetailService.save(anchorVideoDetailEntity);
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
            one = anchorVideoDetailService.getOne(new LambdaQueryWrapper<AnchorVideoDetailEntity>()
                    .eq(AnchorVideoDetailEntity::getVideoId, anchorVideoDetailBo.getVideoId())
                    .last(CustomizeConstant.SELECT_ONE_LAST_SQL.getValue()));
        }

        if (one != null){
            return BeanUtil.copyProperties(one, AnchorVideoDetailInfoVo.class);
        }
        return null;
    }

    @Override
    public AnchorVideoDetailVo updateContentStatusIng(GenerateVideoContentBo bo) {
        List<AnchorVideoDetailEntity> list = anchorVideoDetailService.lambdaQuery().eq(AnchorVideoDetailEntity::getVideoId, bo.getSourceId()).list();
        if (!list.isEmpty()){
            AnchorVideoDetailEntity anchorVideoDetailEntity = list.get(0);
            long now = System.currentTimeMillis();
            if (bo.getType() == WordsEnum.contentType.NATURE.getCode()) {
                anchorVideoDetailEntity.setNatureContentStatus(WordsEnum.contentStatus.GENERATING.getCode());
                anchorVideoDetailEntity.setNatureSourceType(WordsEnum.contentSourceType.SERVER.getCode());
                anchorVideoDetailEntity.setNatSetTime(now);
            }
            if (bo.getType() == WordsEnum.contentType.OPTIMIZE.getCode()) {
                anchorVideoDetailEntity.setOptimizeContentStatus(WordsEnum.contentStatus.GENERATING.getCode());
                anchorVideoDetailEntity.setOptimizeSourceType(WordsEnum.contentSourceType.SERVER.getCode());
                anchorVideoDetailEntity.setOptSetTime(now);
            }
            anchorVideoDetailEntity.setUpdateDate(new Date());
            boolean updated = anchorVideoDetailService.updateById(anchorVideoDetailEntity);
            if (updated){
                return BeanUtil.toBean(anchorVideoDetailEntity, AnchorVideoDetailVo.class);
            }
            return null;
        }
        return null;
    }

    @Override
    public AnchorVideoDetailVo getByVideoIdSet(GenerateVideoContentBo bo) {
        List<AnchorVideoDetailEntity> list = anchorVideoDetailService.lambdaQuery().eq(AnchorVideoDetailEntity::getVideoId, bo.getSourceId()).list();
        Date date = new Date();
        long time = date.getTime();
        if (!list.isEmpty()){
            AnchorVideoDetailEntity anchorVideoDetailEntity = list.get(0);
            if (bo.getType()==1){
                anchorVideoDetailEntity.setNatSetJob(1);
                anchorVideoDetailEntity.setNatSetTime( time);
            }
            if (bo.getType()==2){
                anchorVideoDetailEntity.setOptSetJob(1);
                anchorVideoDetailEntity.setOptSetTime( time);
            }
            anchorVideoDetailEntity.setUpdateDate(new Date());
            boolean updated = anchorVideoDetailService.updateById(anchorVideoDetailEntity);
            if (updated){
                return BeanUtil.toBean(anchorVideoDetailEntity, AnchorVideoDetailVo.class);
            }
            return null;
        }
        return null;
    }

    @Override
    public AnchorVideoDetailVo getByVideo(Long userId, Long tenantId, String videoId,Integer type) {
        List<AnchorVideoDetailEntity> list = anchorVideoDetailService.lambdaQuery()
                .eq(AnchorVideoDetailEntity::getVideoId, videoId)
//                .eq(AnchorVideoDetailEntity::getTenantId, tenantId)
//                .eq(AnchorVideoDetailEntity::getUserId, userId)
                .list();
        if (!list.isEmpty()){
            AnchorVideoDetailEntity anchorVideoDetailEntity = list.get(0);
            AnchorVideoDetailVo anchorVideoDetailVo = new AnchorVideoDetailVo();
            BeanUtils.copyProperties(anchorVideoDetailEntity,anchorVideoDetailVo);
            return anchorVideoDetailVo;
        }
        return null;
    }

    @Override
    public void updateVideoDetailById(AnchorVideoDetailVo vo) {
        if (ObjectUtil.isNotNull(vo)){
            AnchorVideoDetailEntity bean = BeanUtil.toBean(vo, AnchorVideoDetailEntity.class);
            anchorVideoDetailService.updateById(bean);
        }
    }

    @Override
    public List<AnchorVideoDetailInfoVo> listByVideoIds(List<String> videoIds) {
        List<AnchorVideoDetailEntity> list = anchorVideoDetailService.list(new LambdaQueryWrapper<AnchorVideoDetailEntity>()
                .in(AnchorVideoDetailEntity::getVideoId, videoIds)
        );
        if (ObjectUtil.isNotEmpty(list)){
            return BeanUtil.copyToList(list, AnchorVideoDetailInfoVo.class);
        }
        return new ArrayList<>();
    }

    @Override
    public AnchorVideoDetailVo getEmtyByVideoId(String videoId) {
        List<AnchorVideoDetailEntity> list = anchorVideoDetailService.list(new LambdaQueryWrapper<AnchorVideoDetailEntity>()
                .eq(AnchorVideoDetailEntity::getVideoId, videoId)
        );
        if (ObjectUtil.isNotEmpty(list)){
            AnchorVideoDetailEntity anchorVideoDetailEntity = list.get(0);
            return BeanUtil.toBean(anchorVideoDetailEntity, AnchorVideoDetailVo.class);
        }
        return null;
    }

    /**
     * 获取视频已分析完成的需要生成的自然/优化原文的数据
     * @param limit
     * @return
     */
    @Override
    public List<AnchorVideoDetailVo> selectByQuery(Integer limit) {

        return anchorVideoDetailService.selectVideoDetailData(limit);
    }



    @Override
    public void toUpdateStatus(String sourceId, Integer type) {

        // 构造要更新的数据
        AnchorVideoDetailEntity updateEntity = new AnchorVideoDetailEntity();
        if (type == 1){
            updateEntity.setNatureContentStatus(2);
        }else {
            updateEntity.setOptimizeContentStatus(2);
        }

        // 构造更新条件
        LambdaUpdateWrapper<AnchorVideoDetailEntity> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AnchorVideoDetailEntity::getVideoId, sourceId);
        anchorVideoDetailService.update(updateEntity,wrapper);
    }

    @Override
    public List<AnchorVideoDetailInfoVo> contentByToGenerated(List<String> videoIds, Long userId, Long tenantId) {

        List<AnchorVideoDetailEntity> list = anchorVideoDetailService.list(new LambdaQueryWrapper<AnchorVideoDetailEntity>()
                .in(ObjectUtil.isNotEmpty(videoIds), AnchorVideoDetailEntity::getVideoId, videoIds)
                .eq(AnchorVideoDetailEntity::getUserId, userId)
                .eq(AnchorVideoDetailEntity::getTenantId, tenantId)
                .and(e -> e
                        .eq(AnchorVideoDetailEntity::getNatureContentStatus, 1)
                        .eq(AnchorVideoDetailEntity::getNatureSourceType, WordsEnum.contentSourceType.CLIENT.getCode())
                        .or()
                        .eq(AnchorVideoDetailEntity::getOptimizeContentStatus, 1)
                        .eq(AnchorVideoDetailEntity::getOptimizeSourceType, WordsEnum.contentSourceType.CLIENT.getCode())
                )
        );
        if (ObjectUtil.isNotEmpty(list)) {
            List<AnchorVideoDetailInfoVo> anchorVideoDetailInfoVos = BeanUtil.copyToList(list, AnchorVideoDetailInfoVo.class);

            List<AnchorVideoInfoVo> list1 = anchorVideoService.list(new LambdaQueryWrapper<AnchorVideoEntity>()
                            .in(AnchorVideoEntity::getVideoId, list.stream().map(AnchorVideoDetailEntity::getVideoId).distinct().toList())
                    )
                    .stream()
                    .map(item -> {
                        AnchorVideoInfoVo anchorVideoInfoVo = BeanUtil.copyProperties(item, AnchorVideoInfoVo.class);
                        anchorVideoInfoVo.setId(null);
                        return anchorVideoInfoVo;
                    }).toList();

            // 设置值
            DataUtils.setFieldObject(anchorVideoDetailInfoVos, "videoId", "video", list1, "videoId");

            return anchorVideoDetailInfoVos;
        }
        return new ArrayList<>();
    }

    @Override
    public boolean updateContentStatus(AnchorVideoDetailBo detailBo) {

        Date now = new Date();
        boolean update = this.anchorVideoDetailService.update(new LambdaUpdateWrapper<AnchorVideoDetailEntity>()
                .eq(AnchorVideoDetailEntity::getVideoId, detailBo.getVideoId())
                .set(ObjectUtil.isNotEmpty(detailBo.getOptimizeContentStatus()), AnchorVideoDetailEntity::getOptimizeContentStatus, detailBo.getOptimizeContentStatus())
                .set(ObjectUtil.isNotEmpty(detailBo.getOptimizeSourceType()), AnchorVideoDetailEntity::getOptimizeSourceType, detailBo.getOptimizeSourceType())
                .set(ObjectUtil.isNotEmpty(detailBo.getNatureContentStatus()), AnchorVideoDetailEntity::getNatureContentStatus, detailBo.getNatureContentStatus())
                .set(ObjectUtil.isNotEmpty(detailBo.getNatureSourceType()), AnchorVideoDetailEntity::getNatureSourceType, detailBo.getNatureSourceType())
                .set(ObjectUtil.isNotEmpty(detailBo.getOptimizeContentStatus()) && detailBo.getOptimizeContentStatus() == 1, AnchorVideoDetailEntity::getOptSetTime, now.getTime())
                .set(ObjectUtil.isNotEmpty(detailBo.getNatureContentStatus()) && detailBo.getNatureContentStatus() == 1, AnchorVideoDetailEntity::getNatSetTime, now.getTime())
                .set(AnchorVideoDetailEntity::getUpdateDate, now)
        );
        if (!update) {
            log.error("anchorVideoDetailService.update失败,data = {}", JSONUtil.toJsonStr(detailBo));
        }
        return update;

    }

    @Override
    public boolean updateContentStatus(Long id, Integer type, int contentStatus) {
        Date now = new Date();
        boolean update = this.anchorVideoDetailService.update(new LambdaUpdateWrapper<AnchorVideoDetailEntity>()
                .eq(AnchorVideoDetailEntity::getId, id)
                .set(type == WordsEnum.contentType.NATURE.getCode(), AnchorVideoDetailEntity::getNatureContentStatus, contentStatus)
                .set(type == WordsEnum.contentType.OPTIMIZE.getCode(), AnchorVideoDetailEntity::getOptimizeContentStatus, contentStatus)
                .set(type == WordsEnum.contentType.NATURE.getCode() && contentStatus == WordsEnum.contentStatus.GENERATING.getCode(), AnchorVideoDetailEntity::getNatSetTime, now.getTime())
                .set(type == WordsEnum.contentType.OPTIMIZE.getCode() && contentStatus == WordsEnum.contentStatus.GENERATING.getCode(), AnchorVideoDetailEntity::getOptSetTime, now.getTime())
                .set(AnchorVideoDetailEntity::getUpdateDate, now)
        );
        if (!update) {
            log.error("anchorVideoDetailService.update失败,id = {}, type = {}, status = {}", id, type, contentStatus);
        }
        return update;
    }


    /**
     * 更新视频详情状态
     *
     * @param sourceId
     * @param type
     *
     */
    @Override
    public boolean updateDetailStatus(String sourceId, Integer type) {
        LambdaUpdateWrapper<AnchorVideoDetailEntity> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper
                .eq(AnchorVideoDetailEntity::getVideoId, sourceId)
                .set(type == WordsEnum.contentType.NATURE.getCode(), AnchorVideoDetailEntity::getNatSetJob, 1)
                .set(type == WordsEnum.contentType.OPTIMIZE.getCode(), AnchorVideoDetailEntity::getOptSetJob, 1)
                .set(AnchorVideoDetailEntity::getUpdateDate, new Date());
        boolean updated = anchorVideoDetailService.update(updateWrapper);
        if (!updated) {
            log.error("anchorVideoDetailService.update失败,sourceId = {}, type = {}", sourceId, type);
        }
        return updated;
    }

    @Override
    public Boolean updateByVideoId(AnchorVideoDetailBo anchorVideoDetailBo) {
        AnchorVideoDetailEntity one = anchorVideoDetailService.getOne(new LambdaQueryWrapper<AnchorVideoDetailEntity>()
                .eq(AnchorVideoDetailEntity::getVideoId, anchorVideoDetailBo.getVideoId())
                .last("limit 1")
        );
        if (one == null) {
            return false;
        }
        anchorVideoDetailBo.setId(one.getId());
        return update(anchorVideoDetailBo);
    }

    @Override
    public void initUpdateSuggestTrade(String videoId) {
        AnchorVideoEntity video = anchorVideoService.getOne(new LambdaQueryWrapper<AnchorVideoEntity>()
                .eq(AnchorVideoEntity::getVideoId, videoId)
                .last("limit 1")
        );
        if (video == null) {
            return;
        }

        long count = anchorVideoService.count(new LambdaQueryWrapper<AnchorVideoEntity>()
                .eq(AnchorVideoEntity::getUserId, video.getUserId())
                .eq(AnchorVideoEntity::getTenantId, video.getTenantId())
                .eq(AnchorVideoEntity::getSecUid, video.getSecUid())
                .eq(AnchorVideoEntity::getAnalysisStatus, 2)
        );
        if (count > 1) {
            AnchorVideoDetailBo bo = new AnchorVideoDetailBo();
            bo.setVideoId(videoId);
            AnchorVideoDetailInfoVo detailInfoVo = this.getAndSave(bo);
            AnchorVideoDetailBo detailBo = new AnchorVideoDetailBo();
            detailBo.setVideoId(detailInfoVo.getVideoId());
            detailBo.setSuggestTrade(1);
            this.updateByVideoId(detailBo);
        }
    }

    /**
     * 根据 sourceId（videoId）更新内容生成状态。
     *
     * <h3>作用</h3>
     * 供异步生成流程（{@code VideoContentGenerator}）在批次完成后更新最终状态，
     * 以及 Logic 层异常处理时重置状态。
     *
     * <h3>resetJob 参数</h3>
     * <ul>
     *   <li>true — 同时重置 set_job=0，允许定时器重新拾取（全部失败场景）</li>
     *   <li>false — 只更新 status，不重置 set_job（部分成功或配置错误场景）</li>
     * </ul>
     *
     * @param sourceId 视频 id（videoId）
     * @param type     内容类型：1=自然原文, 2=优化原文
     * @param status   目标状态：2=SUCCESS, 3=FAILED
     * @param resetJob 是否同时重置 set_job=0
     * @return true=更新成功
     */
    @Override
    public boolean updateContentStatusBySourceId(String sourceId, Integer type, int status, boolean resetJob) {
        Date now = new Date();
        LambdaUpdateWrapper<AnchorVideoDetailEntity> wrapper = new LambdaUpdateWrapper<AnchorVideoDetailEntity>()
                .eq(AnchorVideoDetailEntity::getVideoId, sourceId)
                .set(type == WordsEnum.contentType.NATURE.getCode(), AnchorVideoDetailEntity::getNatureContentStatus, status)
                .set(type == WordsEnum.contentType.OPTIMIZE.getCode(), AnchorVideoDetailEntity::getOptimizeContentStatus, status)
                .set(AnchorVideoDetailEntity::getUpdateDate, now);
        if (resetJob) {
            wrapper.set(type == WordsEnum.contentType.NATURE.getCode(), AnchorVideoDetailEntity::getNatSetJob, 0)
                   .set(type == WordsEnum.contentType.OPTIMIZE.getCode(), AnchorVideoDetailEntity::getOptSetJob, 0);
        }
        return anchorVideoDetailService.update(wrapper);
    }

    /**
     * 乐观锁标记任务为"已拾取"。
     *
     * <h3>并发安全</h3>
     * UPDATE ... WHERE video_id=? AND set_job=0 AND content_status=1
     * 如果多个实例同时抢同一条记录，只有第一个执行的 UPDATE 能匹配到行（affected rows=1），
     * 后续的 UPDATE 匹配不到（affected rows=0），从而实现无锁的互斥拾取。
     *
     * <h3>拾取时间戳</h3>
     * set_time 记录的是 {@link System#currentTimeMillis()}（毫秒时间戳），
     * 用于后续排查和监控（如发现某条记录拾取时间过长未完成则可能存在异常）。
     *
     * @param sourceId 视频 id（videoId）
     * @param type     内容类型：1=自然原文, 2=优化原文
     * @return true=拾取成功, false=已被其他实例抢先拾取
     */
    @Override
    public boolean markPickedUp(String sourceId, Integer type) {
        long now = System.currentTimeMillis();
        LambdaUpdateWrapper<AnchorVideoDetailEntity> wrapper = new LambdaUpdateWrapper<AnchorVideoDetailEntity>()
                .eq(AnchorVideoDetailEntity::getVideoId, sourceId)
                .set(AnchorVideoDetailEntity::getUpdateDate, new Date());
        if (type == WordsEnum.contentType.NATURE.getCode()) {
            wrapper.eq(AnchorVideoDetailEntity::getNatSetJob, 0)
                   .eq(AnchorVideoDetailEntity::getNatureContentStatus, 1)
                   .eq(AnchorVideoDetailEntity::getNatureSourceType, 0)
                   .set(AnchorVideoDetailEntity::getNatSetJob, 1)
                   .set(AnchorVideoDetailEntity::getNatSetTime, now);
        } else {
            wrapper.eq(AnchorVideoDetailEntity::getOptSetJob, 0)
                   .eq(AnchorVideoDetailEntity::getOptimizeContentStatus, 1)
                   .eq(AnchorVideoDetailEntity::getOptimizeSourceType, 0)
                   .set(AnchorVideoDetailEntity::getOptSetJob, 1)
                   .set(AnchorVideoDetailEntity::getOptSetTime, now);
        }
        return anchorVideoDetailService.update(wrapper);
    }
}

