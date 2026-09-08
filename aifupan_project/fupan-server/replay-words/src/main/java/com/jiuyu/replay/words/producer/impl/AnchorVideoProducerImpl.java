package com.jiuyu.replay.words.producer.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.NumberChineseFormatter;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jiuyu.replay.common.constant.AnchorVideoEnums;
import com.jiuyu.replay.common.constant.WordsEnum;
import com.jiuyu.replay.common.utils.BeanConvertUtils;
import com.jiuyu.replay.common.utils.Query;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.generic.enums.words.SliceTypeEnum;
import com.jiuyu.replay.generic.enums.words.VideoSliceTypeEnum;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.video.VideoSliceVo;
import com.jiuyu.replay.generic.vo.words.AnchorVideoInfoVo;
import com.jiuyu.replay.generic.vo.words.AnchorVideoVO;
import com.jiuyu.replay.generic.vo.words.LocalSourceVideoVo;
import com.jiuyu.replay.words.bo.*;
import com.jiuyu.replay.words.bo.video.*;
import com.jiuyu.replay.words.constant.Constant;
import com.jiuyu.replay.words.entity.*;
import com.jiuyu.replay.words.producer.AnchorVideoProducer;
import com.jiuyu.replay.words.repository.service.*;
import com.jiuyu.replay.words.vo.anchor.HistoryBatchNumberVideoListVo;
import com.jiuyu.replay.words.vo.video.AnchorVideoFileAllVo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AnchorVideoProducerImpl implements AnchorVideoProducer {

    @Resource
    AnchorVideoService anchorVideoService;
    @Resource
    private com.jiuyu.replay.words.repository.dao.AnchorVideoDao anchorVideoDao;

    @Resource
    VideoAnalysisRecordService videoAnalysisRecordService;
    @Resource
    AudioAnalysisService audioAnalysisService;
    @Resource
    private SyncContrastService syncContrastService;
    @Resource
    TradeService tradeService;
    @Resource
    AnchorUrlService anchorUrlService;
    @Resource
    VideoAnalysisRecordService videoRecordService;
    @Resource
    private AnchorUrlUserService anchorUrlUserService;
    @Resource
    private UploadFileService uploadFileService;
    @Resource
    private AnchorVideoDetailService anchorVideoDetailService;
    @Resource
    private VideoSliceService videoSliceService;


    /**
     * 保存录制视频信息
     *
     * @return
     */
    @Override
    public R<String> save(AnchorVideoInfoBo anchorVideoInfoBo) {
        if (StringUtils.isEmpty(anchorVideoInfoBo.getVideoId())) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "保存录制视频信息失败");
        }

        List<AnchorVideoEntity> anchorVideoEntities = this.anchorVideoService.list(new QueryWrapper<AnchorVideoEntity>().eq("video_id", anchorVideoInfoBo.getVideoId()));
        if(anchorVideoEntities != null && anchorVideoEntities.size() > 0) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "视频已存在");
        }

        AnchorVideoEntity anchorVideoEntity = new AnchorVideoEntity();
        BeanUtils.copyProperties(anchorVideoInfoBo, anchorVideoEntity);
        anchorVideoEntity.setId(SnowflakeManager.nextValue());
        anchorVideoEntity.setType(1);
        anchorVideoEntity.setCreateDate(new Date());
        anchorVideoEntity.setUpdateDate(new Date());
        anchorVideoService.save(anchorVideoEntity);
        return R.ok();
    }


    /**
     * 分页查询
     *
     * @param anchorVideoVO
     * @return
     */
    @Override
    public PageUtils<AnchorVideoVO> queryPage(AnchorVideoVO anchorVideoVO) {
        QueryWrapper<AnchorVideoEntity> wrapper = new QueryWrapper<>();
        IPage<AnchorVideoEntity> iPage = anchorVideoService.page(
                new Query<AnchorVideoEntity>().getPage(anchorVideoVO.getPage(), anchorVideoVO.getLimit()), wrapper);

        PageUtils<AnchorVideoVO> pageUtils = new PageUtils<>(anchorVideoVO.getPage(), anchorVideoVO.getLimit(), iPage);

        List<AnchorVideoEntity> records = iPage.getRecords();
        if (records != null && records.size() > 0) {
            List<AnchorVideoVO> vos = records.stream().map(item -> {
                AnchorVideoVO anchorUrlVo = new AnchorVideoVO();
                BeanUtils.copyProperties(item, anchorUrlVo);
                return anchorUrlVo;
            }).collect(Collectors.toList());
            pageUtils.setList(vos);
        }
        return pageUtils;
    }

    /**
     * 跟查询用户Id分页查询录制视频信息
     *
     * @param anchorVideoVO
     * @return
     */
    @Override
    public R<PageUtils<AnchorVideoVO>> pageLists(AnchorVideoBo anchorVideoVO) {
        QueryWrapper<AnchorVideoEntity> wrapper = new QueryWrapper<>();

        //只要不在录制中的
        wrapper.eq("is_recording", 0);
        //主播名称
        if (anchorVideoVO.getAnchorName() != null) {
            QueryWrapper<AnchorUrlEntity> wrapper1 = new QueryWrapper<>();
            wrapper1.like("anchor_name", anchorVideoVO.getAnchorName());
            List<AnchorUrlEntity> list = anchorUrlService.list(wrapper1);
            if (list != null && list.size() > 0) {
                List<String> list1 = list.stream().map(AnchorUrlEntity::getSecUid).toList();
                wrapper.in("sec_uid", list1);
            } else {
                return R.ok(new PageUtils<>());
            }
        }
        //用户id集合
        if (anchorVideoVO.getUserIdS() != null && anchorVideoVO.getUserIdS().size() > 0) {
            wrapper.in("user_id", anchorVideoVO.getUserIdS());
        }

        if (anchorVideoVO.getSecUid() != null) {
            wrapper.eq("sec_uid", anchorVideoVO.getSecUid());
        }

        if (anchorVideoVO.getVideoName() != null) {
            wrapper.like("video_name", anchorVideoVO.getVideoName());
        }
        if (anchorVideoVO.getStartTime() != null) {
            wrapper.between("create_date", anchorVideoVO.getStartTime(), anchorVideoVO.getEndTime());
        }
        if (anchorVideoVO.getUserId() != null) {
            wrapper.eq("user_id", anchorVideoVO.getUserId());
        }
        if (anchorVideoVO.getAnalysisStatus() != null) {
            wrapper.eq("analysis_status", anchorVideoVO.getAnalysisStatus());
        }
        if (anchorVideoVO.getTradeList() != null && !anchorVideoVO.getTradeList().isEmpty()){
            wrapper.in("trade_id", anchorVideoVO.getTradeList());
        }
        if (anchorVideoVO.getVideoIdList() != null && !anchorVideoVO.getVideoIdList().isEmpty()){
            wrapper.in("video_id", anchorVideoVO.getVideoIdList());
        }

        wrapper.gt("duration",0);

        IPage<AnchorVideoEntity> page = anchorVideoService.page(new Query<AnchorVideoEntity>().getPage(anchorVideoVO.getPage(), anchorVideoVO.getLimit()), wrapper);

        PageUtils<AnchorVideoVO> pageUtils = new PageUtils<>(anchorVideoVO.getPage(), anchorVideoVO.getLimit(), page);

        if (page.getRecords() != null || page.getRecords().size() > 0) {
            //行业
            // List<Long> tradeIds = page.getRecords().stream().map(AnchorVideoEntity::getTradeId).toList();
            // List<TradeEntity> tradelist = tradeService.list(new QueryWrapper<TradeEntity>().in("id", tradeIds));

            List<AnchorVideoVO> collect = page.getRecords().stream().map(item -> {
                    AnchorUrlEntity one = anchorUrlService.getOne(new QueryWrapper<AnchorUrlEntity>().eq("sec_uid", item.getSecUid()));
                    AnchorVideoVO anchorUrlVo = new AnchorVideoVO();
                        BeanUtils.copyProperties(item, anchorUrlVo);
                        if (one != null){
                            anchorUrlVo.setAnchorUrlId(one.getId());
                            anchorUrlVo.setAnchorName(one.getAnchorName());
                        }

                        //行业名称
                        TradeEntity entity = tradeService.getById(item.getTradeId());
                        // List<TradeEntity> list1 = tradelist.stream().filter(entity -> entity.getId().equals(item.getTradeId())).toList();
                        if (entity != null) {
                            anchorUrlVo.setTradeName(entity.getName());
                        }
                    return anchorUrlVo;
            }).toList();

//            List<AnchorVideoVO> collectFinal = collect.stream().filter(item -> item.getVideoName() != null).collect(Collectors.toList());

            pageUtils.setList(collect);

            return R.ok(pageUtils);
        }

        return null;
    }

    @Override
    public AnchorVideoInfoVo updateVideo(AnchorVideoInfoBo anchorVideoInfoBo) {

        List<AnchorVideoEntity> anchorVideoEntities = this.anchorVideoService.list(new QueryWrapper<AnchorVideoEntity>().eq("video_id", anchorVideoInfoBo.getVideoId()));

        AnchorVideoEntity anchorVideoEntity;

        if(anchorVideoEntities != null && anchorVideoEntities.size() > 0) {
            anchorVideoEntities.sort((e1, e2) -> e2.getCreateDate().compareTo(e1.getCreateDate()));

            anchorVideoEntity = anchorVideoEntities.get(0);
            Long id = anchorVideoEntity.getId();
            BeanUtils.copyProperties(anchorVideoInfoBo, anchorVideoEntity);
            anchorVideoEntity.setId(id);
            anchorVideoEntity.setUpdateDate(new Date());
            anchorVideoService.updateById(anchorVideoEntity);

        } else {
            anchorVideoEntity = new AnchorVideoEntity();
            BeanUtils.copyProperties(anchorVideoInfoBo, anchorVideoEntity);
            anchorVideoEntity.setId(SnowflakeManager.nextValue());
            anchorVideoEntity.setCreateDate(new Date());
            anchorVideoEntity.setUpdateDate(new Date());
            anchorVideoService.save(anchorVideoEntity);
        }

        AnchorVideoInfoVo anchorVideoInfoVo = new AnchorVideoInfoVo();
        BeanUtils.copyProperties(anchorVideoEntity, anchorVideoInfoVo);
        return anchorVideoInfoVo;

    }

    /**
     * 客户端查询视频列表
     *
     * @param id
     * @return
     */
    @Override
    public R<List<AnchorVideoVO>> selectByuserId(Long id) {
        QueryWrapper<AnchorVideoEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", id);
        wrapper.eq("upload_status", 1);
        List<AnchorVideoEntity> list = anchorVideoService.list(wrapper);
        if (list != null && list.size() > 0) {
            List<AnchorVideoVO> list1 = list.stream().map(item -> {
                AnchorVideoVO anchorVideoVO = new AnchorVideoVO();
                BeanUtils.copyProperties(item, anchorVideoVO);
                return anchorVideoVO;
            }).toList();
            return R.ok(list1);
        }
        return R.ok(new ArrayList<>());
    }


    /**
     * 据客户端上传的List<voidId>删除
     *
     * @param voidIds
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<String> removeByVoidId(List<String> voidIds) {
        // 删除视频
        anchorVideoService.remove(new QueryWrapper<AnchorVideoEntity>().in("video_id", voidIds));
        // 删除视频分析数据（旧）
        audioAnalysisService.remove(new QueryWrapper<AudioAnalysisEntity>().in("video_id", voidIds));
        // 删除视频的分析记录
        videoAnalysisRecordService.remove(new QueryWrapper<VideoAnalysisRecordEntity>().in("video_id", voidIds));
        // 删除视频同步数据
        QueryWrapper<SyncContrastEntity> wrapper = new QueryWrapper<>();
        wrapper.and(w -> {
            w.in("video_one_id", voidIds).or().in("video_two_id", voidIds);
        });
        syncContrastService.remove(wrapper);
        return R.ok();
    }

    /**
     * 服务端用户详情查询录制记录
     *
     * @param anchorVideoBo
     * @return
     */
    @Override
    public R<PageUtils<AnchorVideoVO>> listByUserId(AnchorVideoBo anchorVideoBo) {
        QueryWrapper<AnchorVideoEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", anchorVideoBo.getUserId());

        IPage<AnchorVideoEntity> page = anchorVideoService.page(new Query<AnchorVideoEntity>().getPage(anchorVideoBo.getPage(), anchorVideoBo.getLimit()), wrapper);
        PageUtils<AnchorVideoVO> objectPageUtils = new PageUtils<>(anchorVideoBo.getPage(), anchorVideoBo.getLimit(), page);
        List<AnchorVideoEntity> records = page.getRecords();


        if (records != null && records.size() > 0) {
            List<String> list1 = records.stream().map(AnchorVideoEntity::getSecUid).toList();

            QueryWrapper<AnchorUrlEntity> wrapper2 = new QueryWrapper<>();
            wrapper2.in("sec_uid", list1);

            List<AnchorUrlEntity> list2 = anchorUrlService.list(wrapper2);
            List<AnchorVideoVO> list = records.stream().map(item -> {
                AnchorVideoVO anchorVideoVO = new AnchorVideoVO();
                BeanUtils.copyProperties(item, anchorVideoVO);
                List<AnchorUrlEntity> list3 = list2.stream().filter(entity -> entity.getSecUid().equals(item.getSecUid())).toList();
                anchorVideoVO.setAnchorName(list3.get(0).getAnchorName());
                return anchorVideoVO;
            }).toList();
            objectPageUtils.setList(list);
        }
        return R.ok(objectPageUtils);
    }

    @Override
    public void saveBatch(List<AudioAnalysisSaveBo> audioAnalysisSaveBos) {

        if(audioAnalysisSaveBos != null && audioAnalysisSaveBos.size() > 0) {

            // 获取最新的版本号
            String videoId = audioAnalysisSaveBos.get(0).getVideoId();
            QueryWrapper<AudioAnalysisEntity> wrapper = new QueryWrapper<>();
            wrapper.eq("video_id", videoId);
            wrapper.orderByDesc("version");
            wrapper.last(" limit 1");
            AudioAnalysisEntity lastAudioAnalysis = this.audioAnalysisService.getOne(wrapper);

            int version = 0;
            if(lastAudioAnalysis != null) {
                version = lastAudioAnalysis.getVersion() + 1;
            }

            int finalVersion = version;
            List<AudioAnalysisEntity> audioAnalysisEntities = audioAnalysisSaveBos.stream().map(item -> {
                AudioAnalysisEntity audioAnalysisEntity = new AudioAnalysisEntity();
                BeanUtils.copyProperties(item, audioAnalysisEntity);
                audioAnalysisEntity.setId(SnowflakeManager.nextValue());
                audioAnalysisEntity.setCreateDate(new Date());
                audioAnalysisEntity.setVersion(finalVersion);
                return audioAnalysisEntity;
            }).toList();

            this.audioAnalysisService.saveBatch(audioAnalysisEntities);
        }
    }


    /**
     * 根据主播sec_uid获取已录制该主播的视频
     * @param anchorVideoBo
     * @return
     */
    @Override
    public R<PageUtils<AnchorVideoVO>> selectVideoBySecUid(AnchorVideoBo anchorVideoBo) {
        QueryWrapper<AnchorVideoEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("sec_uid", anchorVideoBo.getSecUid());

        IPage<AnchorVideoEntity> page = this.anchorVideoService.page(new Query<AnchorVideoEntity>().getPage(anchorVideoBo.getPage(), anchorVideoBo.getLimit()), wrapper);

        PageUtils<AnchorVideoVO> pageUtils = new PageUtils<>(anchorVideoBo.getPage(), anchorVideoBo.getLimit(), page);

        List<AnchorVideoEntity> records = page.getRecords();
        if(records != null && records.size() > 0) {

            Set<Long> tradeIds = records.stream().map(AnchorVideoEntity::getTradeId).collect(Collectors.toSet());
            List<TradeEntity> tradeEntities = this.tradeService.listByIds(tradeIds);
            Map<Long, TradeEntity> tradeMap = new HashMap<>();
            if(tradeEntities != null && !tradeEntities.isEmpty()) {
                tradeMap = tradeEntities.stream().collect(Collectors.toMap(TradeEntity::getId, Function.identity()));
            }

            Map<Long, TradeEntity> finalTradeMap = tradeMap;
            List<AnchorVideoVO> vos = records.stream().map(item -> {
                AnchorVideoVO anchorVideoVO = new AnchorVideoVO();
                BeanUtils.copyProperties(item, anchorVideoVO);

                TradeEntity tradeEntity = finalTradeMap.get(item.getTradeId());
                if (tradeEntity != null){
                    anchorVideoVO.setTradeName(tradeEntity.getName());
                }

                return anchorVideoVO;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }
        return R.ok(pageUtils);

//        List<AnchorVideoVO> AnchorVideoVOList = page.getRecords().stream().map(item ->{
//            AnchorVideoVO anchorVideoVO = new AnchorVideoVO();
//            BeanUtils.copyProperties(item, anchorVideoVO);

//            TradeEntity tradeEntityById = this.tradeService.getById(item.getTradeId());
//            anchorVideoVO.setTradeName(tradeEntityById.getName());

//            return anchorVideoVO;
//
//        }).toList();

//        pageUtils.setList(AnchorVideoVOList);



    }

    @Override
    public AnchorVideoInfoVo getByVideoId(String videoId) {

        List<AnchorVideoEntity> anchorVideoEntities = this.anchorVideoService.list(new QueryWrapper<AnchorVideoEntity>().eq("video_id", videoId));

        if(anchorVideoEntities != null && anchorVideoEntities.size() > 0) {
            anchorVideoEntities.sort((e1, e2) -> e2.getCreateDate().compareTo(e1.getCreateDate()));

            for (AnchorVideoEntity anchorVideoEntity : anchorVideoEntities) {
                AnchorVideoInfoVo anchorVideoInfoVo = new AnchorVideoInfoVo();
                BeanUtils.copyProperties(anchorVideoEntity, anchorVideoInfoVo);

                packageSliceInfo(anchorVideoInfoVo);

                return anchorVideoInfoVo;
            }
        }

        return null;
    }

    /**
     * 根据视频ID获取分析记录内容
     * @param videoId
     * @return
     */
    @Override
    public R<AnchorVideoVO> videoAnalysisByVideoId(String videoId) {
        AnchorVideoEntity anchorVideoEntity = this.anchorVideoService.getOne(new QueryWrapper<AnchorVideoEntity>()
                .eq("video_id",videoId)
                .last("limit 1")
        );
        if (anchorVideoEntity != null){
            AnchorVideoVO anchorVideoVO = new AnchorVideoVO();
            BeanUtils.copyProperties(anchorVideoEntity, anchorVideoVO);

            // 主播名
            AnchorUrlEntity urlEntity = this.anchorUrlService.getOne(new QueryWrapper<AnchorUrlEntity>().eq("sec_uid", anchorVideoVO.getSecUid()));
            if (urlEntity != null){
                anchorVideoVO.setAnchorName(urlEntity.getAnchorName());
            }

            //录制行业
            TradeEntity trade = this.tradeService.getById(anchorVideoVO.getTradeId());
            if (trade != null){
                anchorVideoVO.setTradeName(trade.getName());
            }

            //分析次数
            List<VideoAnalysisRecordEntity> analysis = this.videoRecordService.list(new QueryWrapper<VideoAnalysisRecordEntity>().eq("video_id", anchorVideoVO.getVideoId()).select("version"));
            if (analysis != null && analysis.size() > 0) {
                anchorVideoVO.setAnalysisStatusVersion(analysis.size());
            }else{
                anchorVideoVO.setAnalysisStatusVersion(0);
            }
            return R.ok("",anchorVideoVO);
        }
        return null;
    }

    /**
     * 根据user_id获取录制分析
     * @param anchorVideoVO
     * @return
     */
    @Override
    public R<PageUtils<AnchorVideoVO>> videoAnalysisByUserId(AnchorVideoVO anchorVideoVO) {
        QueryWrapper<AnchorVideoEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", anchorVideoVO.getUserId());

        IPage<AnchorVideoEntity> page = this.anchorVideoService.page(new Query<AnchorVideoEntity>().getPage(anchorVideoVO.getPage(), anchorVideoVO.getLimit()), wrapper);
        PageUtils<AnchorVideoVO> pageUtils = new PageUtils<>(anchorVideoVO.getPage(),anchorVideoVO.getLimit(),page);

        List<AnchorVideoEntity> records = page.getRecords();
        if (records != null && records.size() > 0) {

            // 获取行业信息
            Set<Long> tradeIds = records.stream().map(AnchorVideoEntity::getTradeId).collect(Collectors.toSet());
            List<TradeEntity> tradeEntities = this.tradeService.listByIds(tradeIds);
            Map<Long, TradeEntity> tradeMap = new HashMap<>();
            if(tradeEntities != null && !tradeEntities.isEmpty()) {
                tradeMap = tradeEntities.stream().collect(Collectors.toMap(TradeEntity::getId, Function.identity()));
            }

            Map<Long, TradeEntity> finalTradeMap = tradeMap;
            List<AnchorVideoVO> voList = records.stream().map(item ->{
                AnchorVideoVO vo = new AnchorVideoVO();
                BeanUtils.copyProperties(item,vo);

                TradeEntity tradeEntity = finalTradeMap.get(item.getTradeId());
                if (tradeEntity != null){
                    vo.setTradeName(tradeEntity.getName());
                }

                if (item.getSecUid() != null){
                    QueryWrapper<AnchorUrlEntity> anchorWrapper = new QueryWrapper<>();
                    anchorWrapper.eq("sec_uid", item.getSecUid());

                    AnchorUrlEntity anchor = this.anchorUrlService.getOne(anchorWrapper);
                    if (anchor != null){
                        vo.setAnchorName(anchor.getAnchorName());
                    }
                }

                if (item.getVideoId() != null){
                    List<VideoAnalysisRecordEntity> analysis = this.videoRecordService.list(new QueryWrapper<VideoAnalysisRecordEntity>().eq("video_id", item.getVideoId()).select("version"));
                    if (analysis != null && analysis.size() > 0) {
                        vo.setAnalysisStatusVersion(analysis.size());
                    }else{
                        vo.setAnalysisStatusVersion(0);
                    }
                }
                return vo;

            }).toList();
            pageUtils.setList(voList);
        }

        return R.ok("",pageUtils);
    }

    @Override
    public List<AnchorVideoInfoVo> getVideoListByTenantId(Long activeTenantId, Integer uploadStatus) {

        QueryWrapper<AnchorVideoEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("tenant_id", activeTenantId);
        if(!StringUtils.isEmpty(uploadStatus)) {
            wrapper.eq("upload_status", uploadStatus);
        }

        List<AnchorVideoEntity> anchorVideoEntities = this.anchorVideoService.list(wrapper);
        if(anchorVideoEntities != null && anchorVideoEntities.size() > 0) {
            List<AnchorVideoInfoVo> anchorVideoVOS = anchorVideoEntities.stream().map(item -> {
                AnchorVideoInfoVo anchorVideoInfoVo = new AnchorVideoInfoVo();
                BeanUtils.copyProperties(item, anchorVideoInfoVo);
                return anchorVideoInfoVo;
            }).toList();

            return anchorVideoVOS;
        }

        return null;
    }

    @Override
    public PageUtils<AnchorVideoInfoVo> listCloudVideoPage(CloudVideoListBo cloudVideoListBo) {

        QueryWrapper<AnchorVideoEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("tenant_id", cloudVideoListBo.getTenantId());
        wrapper.eq("upload_status", 1);
        if(!StringUtils.isEmpty(cloudVideoListBo.getSecUid())) {
            wrapper.eq("sec_uid", cloudVideoListBo.getSecUid());
        }
        if(!StringUtils.isEmpty(cloudVideoListBo.getRecordStartDate())) {
            wrapper.ge("start_time", cloudVideoListBo.getRecordStartDate() + " 00:00:00");
        }
        if(!StringUtils.isEmpty(cloudVideoListBo.getRecordEndDate())) {
            wrapper.le("start_time", cloudVideoListBo.getRecordEndDate() + " 23:59:59");
        }
        if(!StringUtils.isEmpty(cloudVideoListBo.getAnalysisStartDate())) {
            wrapper.ge("analysis_time", cloudVideoListBo.getAnalysisStartDate() + " 00:00:00");
        }
        if(!StringUtils.isEmpty(cloudVideoListBo.getAnalysisEndDate())) {
            wrapper.le("analysis_time", cloudVideoListBo.getAnalysisEndDate() + " 23:59:59");
        }

        IPage<AnchorVideoEntity> iPage = anchorVideoService.page(new Query<AnchorVideoEntity>().getPage(cloudVideoListBo.getPage(), cloudVideoListBo.getLimit()), wrapper);

        PageUtils<AnchorVideoInfoVo> pageUtils = new PageUtils<>(cloudVideoListBo.getPage(), cloudVideoListBo.getLimit(), iPage);

        List<AnchorVideoEntity> records = iPage.getRecords();

        if(records != null && records.size() > 0) {

            List<AnchorVideoInfoVo> vos = records.stream().map(item -> {
                AnchorVideoInfoVo anchorVideoInfoVo = new AnchorVideoInfoVo();
                BeanUtils.copyProperties(item, anchorVideoInfoVo);

                return anchorVideoInfoVo;
            }).collect(Collectors.toList());

            pageUtils.setList(vos);
        }


        return pageUtils;
    }

    @Override
    public List<AnchorVideoInfoVo> listByVideoIds(Collection<String> videoIds) {

        if(videoIds != null && videoIds.size() > 0) {
            List<AnchorVideoEntity> anchorVideoEntities = this.anchorVideoService.list(new QueryWrapper<AnchorVideoEntity>().in("video_id", videoIds));
            if(anchorVideoEntities != null && anchorVideoEntities.size() > 0) {
                List<AnchorVideoInfoVo> anchorVideoInfoVoList = anchorVideoEntities.stream().map(item -> {
                    AnchorVideoInfoVo anchorVideoInfoVo = new AnchorVideoInfoVo();
                    BeanUtils.copyProperties(item, anchorVideoInfoVo);
                    return anchorVideoInfoVo;
                }).toList();
                return anchorVideoInfoVoList;
            }
        }

        return null;
    }

    @Override
    public Long statisticsStoreByTenantId(Long activeTenantId) {
        AnchorVideoEntity one = anchorVideoService.getOne(new QueryWrapper<AnchorVideoEntity>()
                .select("ifnull(sum(cloud_store), 0) as cloud_store")
                .lambda()
                .eq(AnchorVideoEntity::getTenantId, activeTenantId)
                .eq(AnchorVideoEntity::getUploadStatus, 1)
        );
        if (one != null){
            return BigDecimal.valueOf(one.getCloudStore()).multiply(BigDecimal.valueOf(1024)).longValue();
        }
        return null;
    }


    @Override
    public List<String> listByAiFav(ClientAiFavListBo clientAiFavListBo) {
        QueryWrapper<AnchorVideoEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(clientAiFavListBo.getTradeId())) {
            // 获取该行业的主播
            QueryWrapper<AnchorUrlUserEntity> anchorWrapper = new QueryWrapper<>();
            anchorWrapper.eq("user_id", clientAiFavListBo.getUserId());
            anchorWrapper.eq("trade_id", clientAiFavListBo.getTradeId());
            anchorWrapper.select("anchor_url_sec_uid");
            List<AnchorUrlUserEntity> anchorUrlUserEntities = this.anchorUrlUserService.list(anchorWrapper);

            if(anchorUrlUserEntities != null && anchorUrlUserEntities.size() > 0) {
                List<String> secUids = anchorUrlUserEntities.stream().map(AnchorUrlUserEntity::getAnchorUrlSecUid).collect(Collectors.toList());
                wrapper.in("sec_uid", secUids);
            }else {
                wrapper.eq("id", "0");
            }
//            wrapper.eq("trade_id", clientAiFavListBo.getTradeId());
        }
        if((clientAiFavListBo.getSecUidArr() != null && clientAiFavListBo.getSecUidArr().size() > 0)) {
            wrapper.in("sec_uid", clientAiFavListBo.getSecUidArr());
        }

        wrapper.eq("user_id", clientAiFavListBo.getUserId());
        wrapper.eq("tenant_id", clientAiFavListBo.getTenantId());
        wrapper.eq("delete_status", 0);
        wrapper.ge("duration", 60);

        List<AnchorVideoEntity> anchorVideoEntities = this.anchorVideoService.list(wrapper);
        if(anchorVideoEntities != null && anchorVideoEntities.size() > 0) {
            return anchorVideoEntities.stream().map(AnchorVideoEntity::getVideoId).toList();
        }
        return null;
    }

    @Override
    public PageUtils<AnchorVideoInfoVo> clientVideoList(ClientVideoListBo clientVideoListBo) {

        QueryWrapper<AnchorVideoEntity> wrapper = new QueryWrapper<>();
        if(!StringUtils.isEmpty(clientVideoListBo.getUserId())) {
            wrapper.eq("user_id", clientVideoListBo.getUserId());
        }
        if(!StringUtils.isEmpty(clientVideoListBo.getTenantId())) {
            wrapper.eq("tenant_id", clientVideoListBo.getTenantId());
        }
        if(clientVideoListBo.getUserIdList() != null && clientVideoListBo.getUserIdList().size() > 0) {
            wrapper.in("user_id", clientVideoListBo.getUserIdList());
        }
        wrapper.eq("is_recording", 0);

        if(clientVideoListBo.getVideoSliceType() == null || clientVideoListBo.getVideoSliceType().equals(VideoSliceTypeEnum.VIDEO.getCode())) {
            // 只有原视频才筛选大于60秒的视频
            wrapper.ge("duration", 60);
        }
        if(clientVideoListBo.getVideoSliceType() != null) {
            wrapper.eq("video_slice_type", clientVideoListBo.getVideoSliceType());
        }

        if (Objects.nonNull(clientVideoListBo.getTradeId()) || Objects.nonNull(clientVideoListBo.getAccountType())) {
            // 获取符合条件的主播
            QueryWrapper<AnchorUrlUserEntity> anchorWrapper = new QueryWrapper<>();
            if (Objects.nonNull(clientVideoListBo.getUserId())) {
                anchorWrapper.eq("user_id", clientVideoListBo.getUserId());
            }
            if (Objects.nonNull(clientVideoListBo.getTenantId())) {
                anchorWrapper.eq("tenant_id", clientVideoListBo.getTenantId());
            }
            if (Objects.nonNull(clientVideoListBo.getTradeId())) {
                anchorWrapper.eq("trade_id", clientVideoListBo.getTradeId());
            }
            if(clientVideoListBo.getAccountType() != null) {
                anchorWrapper.eq("account_type", clientVideoListBo.getAccountType());
            }
            anchorWrapper.select("anchor_url_sec_uid", "user_id");
            List<AnchorUrlUserEntity> anchorUrlUserEntities = this.anchorUrlUserService.list(anchorWrapper);
            if(anchorUrlUserEntities != null && anchorUrlUserEntities.size() > 0) {
                wrapper.and(w -> {
                    for (AnchorUrlUserEntity anchorUrlUserEntity : anchorUrlUserEntities) {
                        w.or(wp -> {
                            wp.eq("sec_uid", anchorUrlUserEntity.getAnchorUrlSecUid()).eq("user_id", anchorUrlUserEntity.getUserId());
                        });
                    }
                });
            }else {
                wrapper.eq("id", 0);
            }
//            wrapper.eq("trade_id", clientVideoListBo.getTradeId());
        }

        if(!StringUtils.isEmpty(clientVideoListBo.getAnchorName())) {
            QueryWrapper<AnchorUrlEntity> anchorWrapper = new QueryWrapper<>();
            anchorWrapper.like("anchor_name", clientVideoListBo.getAnchorName());
            anchorWrapper.select("sec_uid");
            List<AnchorUrlEntity> anchorUrlEntities = this.anchorUrlService.list(anchorWrapper);
            if(anchorUrlEntities != null && anchorUrlEntities.size() > 0) {
                List<String> secUids = anchorUrlEntities.stream().map(AnchorUrlEntity::getSecUid).collect(Collectors.toList());
                wrapper.in("sec_uid", secUids);
            }else {
                wrapper.eq("id", "0");
            }
        }

        if(clientVideoListBo.getSliceClass() != null) {
            // 按切片类型筛选
            LambdaQueryWrapper<VideoSliceEntity> sliceWrapper = new LambdaQueryWrapper<>();
            sliceWrapper.eq(VideoSliceEntity::getSliceClass, clientVideoListBo.getSliceClass());
            if(clientVideoListBo.getVideoSliceType() != null) {
                if(clientVideoListBo.getVideoSliceType().equals(VideoSliceTypeEnum.VIDEO_SLICE.getCode())) {
                    sliceWrapper.eq(VideoSliceEntity::getSliceType, SliceTypeEnum.VIDEO_SLICE.getCode());
                }else if(clientVideoListBo.getVideoSliceType().equals(VideoSliceTypeEnum.SHORT_VIDEO_SLICE.getCode())) {
                    sliceWrapper.eq(VideoSliceEntity::getSliceType, SliceTypeEnum.SHORT_VIDEO_SLICE.getCode());
                }
            }
            sliceWrapper.select(VideoSliceEntity::getSourceId);

            List<VideoSliceEntity> videoSliceEntities = this.videoSliceService.list(sliceWrapper);
            if(videoSliceEntities != null && !videoSliceEntities.isEmpty()) {
                List<String> videoIds = videoSliceEntities.stream().map(VideoSliceEntity::getSourceId).toList();
                wrapper.in("video_id", videoIds);
            }else {
                wrapper.eq("id", 0);
            }
        }

        if(!StringUtils.isEmpty(clientVideoListBo.getSecUid())) {
            wrapper.eq("sec_uid", clientVideoListBo.getSecUid());
        }
        if(clientVideoListBo.getSecUidArr() != null && clientVideoListBo.getSecUidArr().size() > 0) {
            wrapper.in("sec_uid", clientVideoListBo.getSecUidArr());
        }
        if(!StringUtils.isEmpty(clientVideoListBo.getUploadStatus())) {
            wrapper.eq("upload_status", clientVideoListBo.getUploadStatus());
        }
        if(!StringUtils.isEmpty(clientVideoListBo.getDeleteStatus())) {
            wrapper.eq("delete_status", clientVideoListBo.getDeleteStatus());
        }
        if(!StringUtils.isEmpty(clientVideoListBo.getAnalysisStatus())) {
            wrapper.eq("analysis_status", clientVideoListBo.getAnalysisStatus());
        }
        if(!StringUtils.isEmpty(clientVideoListBo.getAnalysisStartDate())) {
            wrapper.ge("analysis_time", clientVideoListBo.getAnalysisStartDate() + " 00:00:00");
        }
        if(!StringUtils.isEmpty(clientVideoListBo.getAnalysisEndDate())) {
            wrapper.le("analysis_time", clientVideoListBo.getAnalysisEndDate() + " 23:59:59");
        }
        if(!StringUtils.isEmpty(clientVideoListBo.getRecordStartDate())) {
            wrapper.ge("start_time", clientVideoListBo.getRecordStartDate() + " 00:00:00");
        }
        if(!StringUtils.isEmpty(clientVideoListBo.getRecordEndDate())) {
            wrapper.le("start_time", clientVideoListBo.getRecordEndDate() + " 23:59:59");
        }
        if(clientVideoListBo.getVideoIdList() != null && !clientVideoListBo.getVideoIdList().isEmpty()) {
            wrapper.in("video_id", clientVideoListBo.getVideoIdList());
        }

        // 是否有诊断报告
        if (clientVideoListBo.getHasDiagnosisReport() != null || clientVideoListBo.getHasDataDiagnosisReport() != null) {
            List<AnchorVideoDetailEntity> list = anchorVideoDetailService.list(new LambdaQueryWrapper<AnchorVideoDetailEntity>()
                    .eq(ObjectUtil.isNotEmpty(clientVideoListBo.getUserId()), AnchorVideoDetailEntity::getUserId, clientVideoListBo.getUserId())
                    .eq(ObjectUtil.isNotEmpty(clientVideoListBo.getTenantId()), AnchorVideoDetailEntity::getTenantId, clientVideoListBo.getTenantId())
                    .eq(clientVideoListBo.getHasDiagnosisReport() != null, AnchorVideoDetailEntity::getHasDiagnosisReport, clientVideoListBo.getHasDiagnosisReport())
                    .eq(clientVideoListBo.getHasDataDiagnosisReport() != null, AnchorVideoDetailEntity::getHasDataDiagnosisReport, clientVideoListBo.getHasDataDiagnosisReport())
            );
            if (ObjectUtil.isNotEmpty(list)){
                wrapper.in("video_id", list.stream().map(AnchorVideoDetailEntity::getVideoId).toList());
            }else{
                wrapper.eq("id", "0");
            }
        } else if (clientVideoListBo.getHasDiagnosis() != null) {
            List<AnchorVideoDetailEntity> list = anchorVideoDetailService.list(new LambdaQueryWrapper<AnchorVideoDetailEntity>()
                    .eq(ObjectUtil.isNotEmpty(clientVideoListBo.getUserId()), AnchorVideoDetailEntity::getUserId, clientVideoListBo.getUserId())
                    .eq(ObjectUtil.isNotEmpty(clientVideoListBo.getTenantId()), AnchorVideoDetailEntity::getTenantId, clientVideoListBo.getTenantId())
                    .and(w -> w
                            .eq(AnchorVideoDetailEntity::getHasDiagnosisReport, 1)
                            .eq(AnchorVideoDetailEntity::getHasDataDiagnosisReport, 1)
                    )
            );
            if (ObjectUtil.isNotEmpty(list)) {
                wrapper.in("video_id", list.stream().map(AnchorVideoDetailEntity::getVideoId).toList());
            } else {
                wrapper.eq("id", "0");
            }
        }

        if (VideoSliceTypeEnum.VIDEO.getCode().equals(clientVideoListBo.getVideoSliceType())) {
            wrapper.orderByDesc("start_time");
        }else {
            wrapper.orderByDesc("id");
        }


        IPage<AnchorVideoEntity> iPage = anchorVideoService.page(new Query<AnchorVideoEntity>().getPageNoSort(clientVideoListBo.getPage(), clientVideoListBo.getLimit()), wrapper);

        PageUtils<AnchorVideoInfoVo> pageUtils = new PageUtils<>(clientVideoListBo.getPage(), clientVideoListBo.getLimit(), iPage);

        List<AnchorVideoEntity> records = iPage.getRecords();
        if(records != null && records.size() > 0) {
            List<AnchorVideoInfoVo> vos = records.stream().map(item -> {
                AnchorVideoInfoVo anchorVideoInfoVo = new AnchorVideoInfoVo();
                BeanUtils.copyProperties(item, anchorVideoInfoVo);
                return anchorVideoInfoVo;
            }).collect(Collectors.toList());

            // 如果是切片视频，封装原视频信息
            if(clientVideoListBo.getVideoSliceType() != null && (clientVideoListBo.getVideoSliceType().equals(VideoSliceTypeEnum.VIDEO_SLICE.getCode()) ||
                    clientVideoListBo.getVideoSliceType().equals(VideoSliceTypeEnum.SHORT_VIDEO_SLICE.getCode()))) {
                List<String> videoIds = vos.stream().map(AnchorVideoInfoVo::getVideoId).collect(Collectors.toList());
                // 获取切片信息
                LambdaQueryWrapper<VideoSliceEntity> sliceWrapper = new LambdaQueryWrapper<>();
                sliceWrapper.in(VideoSliceEntity::getSourceId, videoIds);
                List<VideoSliceEntity> videoSliceEntities = this.videoSliceService.list(sliceWrapper);

                if(videoSliceEntities != null && !videoSliceEntities.isEmpty()) {
                    // 获取原视频信息列表
                    List<String> parentVideoIds = videoSliceEntities.stream().map(VideoSliceEntity::getSourceParentId).toList();
                    LambdaQueryWrapper<AnchorVideoEntity> parentWrapper = new LambdaQueryWrapper<>();
                    parentWrapper.in(AnchorVideoEntity::getVideoId, parentVideoIds);
                    List<AnchorVideoEntity> parentVideoList = this.anchorVideoService.list(parentWrapper);
                    if(parentVideoList != null && !parentVideoList.isEmpty()) {
                        // 封装原视频信息
                        Map<String, VideoSliceEntity> videoSliceMap = videoSliceEntities.stream().collect(Collectors.toMap(VideoSliceEntity::getSourceId, Function.identity()));
                        Map<String, AnchorVideoEntity> parentVideoMap = parentVideoList.stream().collect(Collectors.toMap(AnchorVideoEntity::getVideoId, Function.identity()));
                        for (AnchorVideoInfoVo videoInfoVo : vos) {
                            VideoSliceEntity videoSliceEntity = videoSliceMap.get(videoInfoVo.getVideoId());
                            if(videoSliceEntity != null) {
                                videoInfoVo.setVideoSliceInfo(BeanConvertUtils.convert(videoSliceEntity, VideoSliceVo.class));
                                AnchorVideoEntity anchorVideoEntity = parentVideoMap.get(videoSliceEntity.getSourceParentId());
                                AnchorVideoInfoVo anchorVideoInfoVo = BeanConvertUtils.convert(anchorVideoEntity, AnchorVideoInfoVo.class);
                                videoInfoVo.setParentVideoInfo(anchorVideoInfoVo);
                            }
                        }
                    }
                }
            }

            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    @Override
    public PageUtils<LocalSourceVideoVo> listLocalSourceVideo(Long userId, Long tenantId, Integer page, Integer limit) {

        LambdaQueryWrapper<AnchorVideoEntity> wrapper = new LambdaQueryWrapper<>();
        // 只查询删除逻辑需要的 5 列，避免整行大字段
        wrapper.select(AnchorVideoEntity::getVideoId, AnchorVideoEntity::getVideoName,
                AnchorVideoEntity::getStoragePath, AnchorVideoEntity::getSecUid, AnchorVideoEntity::getAnalysisStatus);
        // 租户隔离 + 用户隔离
        wrapper.eq(AnchorVideoEntity::getTenantId, tenantId);
        wrapper.eq(AnchorVideoEntity::getUserId, userId);
        // 固定过滤条件：未软删、不在录制、本地未删、分析完成、原视频、时长>=60
        wrapper.eq(AnchorVideoEntity::getIsDeleted, 0);
        wrapper.eq(AnchorVideoEntity::getIsRecording, 0);
        wrapper.eq(AnchorVideoEntity::getLocalVideoStatus, 0);
        wrapper.eq(AnchorVideoEntity::getAnalysisStatus, 2);
        wrapper.eq(AnchorVideoEntity::getVideoSliceType, 0);
        wrapper.ge(AnchorVideoEntity::getDuration, 60L);

        IPage<AnchorVideoEntity> iPage = anchorVideoService.page(new Query<AnchorVideoEntity>().getPageNoSort(page, limit), wrapper);

        PageUtils<LocalSourceVideoVo> pageUtils = new PageUtils<>(page, limit, iPage);

        List<AnchorVideoEntity> records = iPage.getRecords();
        if (records != null && !records.isEmpty()) {
            List<LocalSourceVideoVo> vos = records.stream().map(item -> {
                LocalSourceVideoVo vo = new LocalSourceVideoVo();
                BeanUtils.copyProperties(item, vo);
                return vo;
            }).collect(Collectors.toList());
            pageUtils.setList(vos);
        }

        return pageUtils;
    }

    private void packageSliceSourceVideo() {

    }

    @Override
    public List<String> getAllowDeleteVideo(List<String> ids, Long tenantId, Long userId) {

        QueryWrapper<AnchorVideoEntity> wrapper = new QueryWrapper<>();
        wrapper.in("video_id", ids);
        wrapper.eq("tenant_id", tenantId);
        if(!StringUtils.isEmpty(userId)) {
            wrapper.eq("user_id", userId);
        }
        wrapper.select("video_id");
        List<AnchorVideoEntity> videoEntities = this.anchorVideoService.list(wrapper);
        if(videoEntities != null && videoEntities.size() > 0) {
            return videoEntities.stream().map(AnchorVideoEntity::getVideoId).collect(Collectors.toList());
        }
        return null;
    }

    @Override
    public void batchUpdateVideoDelStatus(List<String> videoIds, Integer deleteStatus) {

        UpdateWrapper<AnchorVideoEntity> wrapper = new UpdateWrapper<>();
        wrapper.in("video_id", videoIds);

        wrapper.set("delete_status", deleteStatus);

        this.anchorVideoService.update(wrapper);
    }

    @Override
    public void batchUpdateVideoUploadStatus(List<String> videoIds, Integer uploadStatus) {
        UpdateWrapper<AnchorVideoEntity> wrapper = new UpdateWrapper<>();
        wrapper.in("video_id", videoIds);

        wrapper.set("upload_status", uploadStatus);
        if(uploadStatus == 0) {
            wrapper.set("play_url", "");
            wrapper.set("share_url", "");
            wrapper.set("cloud_store", 0);
        }

        this.anchorVideoService.update(wrapper);
    }


    @Override
    public List<AnchorVideoInfoVo> listByAnchorRecordList(List<String> secUids, Long userId, Long tenantId, Integer isCloud, Integer videoSliceType) {
        QueryWrapper<AnchorVideoEntity> wrapper = new QueryWrapper<>();
        wrapper.in("sec_uid", secUids);
        if(videoSliceType != null) {
            wrapper.eq("video_slice_type", videoSliceType);
        }
        if(userId != null) {
            wrapper.eq("user_id", userId);
        }
        if(tenantId != null) {
            wrapper.eq("tenant_id", tenantId);
        }
        if(videoSliceType == null || videoSliceType.equals(VideoSliceTypeEnum.VIDEO.getCode())) {
            wrapper.ge("duration", 60);
        }

        wrapper.eq("is_recording", 0);
        if(isCloud == 0) {
            wrapper.eq("delete_status", 0);
        }else {
            wrapper.in("delete_status", 0, 1);
            wrapper.eq("upload_status", 1);
        }

        wrapper.select("video_id", "sec_uid", "start_time", "create_date", "video_slice_type");

        List<AnchorVideoEntity> videoEntities = this.anchorVideoService.list(wrapper);
        if(videoEntities != null && videoEntities.size() > 0) {
            List<AnchorVideoInfoVo> anchorVideoInfoVos = videoEntities.stream().map(item -> {
                AnchorVideoInfoVo anchorVideoInfoVo = new AnchorVideoInfoVo();
                BeanUtils.copyProperties(item, anchorVideoInfoVo);
                return anchorVideoInfoVo;
            }).collect(Collectors.toList());

            return anchorVideoInfoVos;
        }

        return null;
    }

    @Override
    public List<AnchorVideoInfoVo> listUserVideo(ListUserVideoByConditionBo listUserVideoByConditionBo) {

        QueryWrapper<AnchorVideoEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", listUserVideoByConditionBo.getUserId());
        wrapper.eq("tenant_id", listUserVideoByConditionBo.getTenantId());
        if(!StringUtils.isEmpty(listUserVideoByConditionBo.getAnalysisStatus())) {
            wrapper.eq("analysis_status", listUserVideoByConditionBo.getAnalysisStatus());
        }
        if(!StringUtils.isEmpty(listUserVideoByConditionBo.getIsRecording())) {
            wrapper.eq("is_recording", listUserVideoByConditionBo.getIsRecording());
        }
        if(listUserVideoByConditionBo.getVideoSliceType() != null) {
            wrapper.eq("video_slice_type", listUserVideoByConditionBo.getVideoSliceType());
        }
        wrapper.eq("delete_status", 0);
        wrapper.ge("duration", 60);
        wrapper.orderByDesc("id");
        List<AnchorVideoEntity> videoEntities = this.anchorVideoService.list(wrapper);

        if(videoEntities != null && videoEntities.size() > 0) {
            List<AnchorVideoInfoVo> videoInfoVos = videoEntities.stream().map(item -> {
                AnchorVideoInfoVo videoInfoVo = new AnchorVideoInfoVo();
                BeanUtils.copyProperties(item, videoInfoVo);
                return videoInfoVo;
            }).toList();

            return videoInfoVos;
        }

        return null;
    }

    @Override
    public List<AnchorVideoInfoVo> listRecentLiveSessions(Long userId, Long tenantId, String startTimeGe) {

        LambdaQueryWrapper<AnchorVideoEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AnchorVideoEntity::getUserId, userId);
        wrapper.eq(AnchorVideoEntity::getTenantId, tenantId);
        wrapper.eq(AnchorVideoEntity::getDeleteStatus, 0);
        wrapper.ge(AnchorVideoEntity::getDuration, 60);
        wrapper.eq(AnchorVideoEntity::getVideoSliceType, 0);
        if (StrUtil.isNotBlank(startTimeGe)) {
            wrapper.ge(AnchorVideoEntity::getStartTime, startTimeGe);
        }
        List<AnchorVideoEntity> videoEntities = this.anchorVideoService.list(wrapper);

        if (videoEntities == null || videoEntities.isEmpty()) {
            return new ArrayList<>();
        }

        // 按 batchNumber 分组取代表：先按 paragraph（null 视为最小）、再按 id 兜底排序，
        // 取每组第一条作为该场次的代表视频。
        videoEntities.sort((a, b) -> {
            int pa = a.getParagraph() == null ? Integer.MIN_VALUE : a.getParagraph();
            int pb = b.getParagraph() == null ? Integer.MIN_VALUE : b.getParagraph();
            if (pa != pb) {
                return Integer.compare(pa, pb);
            }
            long ia = a.getId() == null ? Long.MAX_VALUE : a.getId();
            long ib = b.getId() == null ? Long.MAX_VALUE : b.getId();
            return Long.compare(ia, ib);
        });

        Map<String, AnchorVideoEntity> sessionMap = videoEntities.stream()
                .filter(item -> StrUtil.isNotBlank(item.getBatchNumber()))
                .collect(Collectors.toMap(AnchorVideoEntity::getBatchNumber, Function.identity(), (a, b) -> a));

        return sessionMap.values().stream().map(item -> {
            AnchorVideoInfoVo videoInfoVo = new AnchorVideoInfoVo();
            BeanUtils.copyProperties(item, videoInfoVo);
            return videoInfoVo;
        }).collect(Collectors.toList());
    }

    @Override
    public void updateVideoAnalysisStatus(UpdateVideoAnalysisStatusBo updateVideoAnalysisStatusBo) {

        QueryWrapper<AnchorVideoEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("video_id", updateVideoAnalysisStatusBo.getVideoId());
        wrapper.eq("user_id", updateVideoAnalysisStatusBo.getUserId());
        wrapper.eq("tenant_id", updateVideoAnalysisStatusBo.getTenantId());
        AnchorVideoEntity videoEntity = this.anchorVideoService.getOne(wrapper);

        if(videoEntity != null) {

            // 兼容旧版本
            if(!StringUtils.isEmpty(videoEntity.getPlatformType()) && videoEntity.getPlatformType().contains("DouYinLive")) {
                videoEntity.setPlatformType("1");
            }

            videoEntity.setErrorReason(updateVideoAnalysisStatusBo.getErrorReason());
            videoEntity.setAnalysisStatus(updateVideoAnalysisStatusBo.getAnalysisStatus());
            if(updateVideoAnalysisStatusBo.getAnalysisStatus() == 2) {
                LocalDateTime now = LocalDateTime.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                videoEntity.setAnalysisTime(now.format(formatter));
                videoEntity.setIsMark(1);
            }

            this.anchorVideoService.updateById(videoEntity);
        }

    }

    @Override
    public void updateVideoUploadStatus(UpdateVideoUploadStatusBo updateVideoUploadStatusBo) {

        QueryWrapper<AnchorVideoEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("video_id", updateVideoUploadStatusBo.getVideoId());
        wrapper.eq("user_id", updateVideoUploadStatusBo.getUserId());
        wrapper.eq("tenant_id", updateVideoUploadStatusBo.getTenantId());
        AnchorVideoEntity videoEntity = this.anchorVideoService.getOne(wrapper);

        if(videoEntity != null) {
            videoEntity.setUploadStatus(updateVideoUploadStatusBo.getUploadStatus());
            videoEntity.setPlayUrl(updateVideoUploadStatusBo.getVideoOnlinePayUrl());
            videoEntity.setShareUrl(updateVideoUploadStatusBo.getShareUrl());
            this.anchorVideoService.updateById(videoEntity);
        }
    }

    @Override
    public AnchorVideoInfoVo clientGetVideoByVideoId(String videoId, Long userId, Long tenantId) {

        QueryWrapper<AnchorVideoEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("video_id", videoId);
        if(!StringUtils.isEmpty(userId)) {
            wrapper.eq("user_id", userId);
        }
        if(!StringUtils.isEmpty(tenantId)) {
            wrapper.eq("tenant_id", tenantId);
        }
        AnchorVideoEntity videoEntity = this.anchorVideoService.getOne(wrapper);

        if(videoEntity != null) {
            AnchorVideoInfoVo videoInfoVo = new AnchorVideoInfoVo();
            BeanUtils.copyProperties(videoEntity, videoInfoVo);

            packageSliceInfo(videoInfoVo);

            return videoInfoVo;
        }

        return null;
    }

    /**
     * 封装视频的切片信息
     * @param videoInfoVo 视频信息
     */
    private void packageSliceInfo(AnchorVideoInfoVo videoInfoVo) {
        if(Objects.equals(videoInfoVo.getVideoSliceType(), VideoSliceTypeEnum.VIDEO_SLICE.getCode())
                || Objects.equals(videoInfoVo.getVideoSliceType(), VideoSliceTypeEnum.SHORT_VIDEO_SLICE.getCode())) {
            // 封装切片视频的切片信息
            VideoSliceEntity videoSliceEntity = videoSliceService.getOne(new LambdaQueryWrapper<VideoSliceEntity>().eq(VideoSliceEntity::getSourceId, videoInfoVo.getVideoId()));
            if(videoSliceEntity != null) {
                VideoSliceVo videoSliceVo = BeanConvertUtils.convert(videoSliceEntity, VideoSliceVo.class);
                videoInfoVo.setVideoSliceInfo(videoSliceVo);

                // 封装切片视频所属原视频信息
                AnchorVideoEntity parentVideo = this.anchorVideoService.getOne(new LambdaQueryWrapper<AnchorVideoEntity>().eq(AnchorVideoEntity::getVideoId, videoSliceVo.getSourceParentId()));
                if(parentVideo != null) {
                    videoInfoVo.setParentVideoInfo(BeanConvertUtils.convert(parentVideo, AnchorVideoInfoVo.class));
                }
            }
        }else if(Objects.equals(videoInfoVo.getVideoSliceType(), VideoSliceTypeEnum.VIDEO.getCode())) {
            // 封装原视频下的所有切片信息
            List<VideoSliceEntity> videoSliceEntities = videoSliceService.list(new LambdaQueryWrapper<VideoSliceEntity>().eq(VideoSliceEntity::getSourceParentId, videoInfoVo.getVideoId()));
            if(videoSliceEntities != null && !videoSliceEntities.isEmpty()) {
                List<VideoSliceVo> videoSliceVos = BeanConvertUtils.convertList(videoSliceEntities, VideoSliceVo.class);
                videoInfoVo.setSliceList(videoSliceVos);
            }
        }
    }

    @Override
    public void updateVideoSizeDuration(List<UpdateVideoSizeDurationBo> updateVideoUploadStatusBoList) {
        if(updateVideoUploadStatusBoList != null && updateVideoUploadStatusBoList.size() > 0) {

            List<String> videoIds = updateVideoUploadStatusBoList.stream().map(UpdateVideoSizeDurationBo::getVideoId).collect(Collectors.toList());
            Long userId = updateVideoUploadStatusBoList.get(0).getUserId();
            Long tenantId = updateVideoUploadStatusBoList.get(0).getTenantId();
            QueryWrapper<AnchorVideoEntity> wrapper = new QueryWrapper<>();
            wrapper.in("video_id", videoIds);
            wrapper.eq("user_id", userId);
            wrapper.eq("tenant_id", tenantId);
            List<AnchorVideoEntity> anchorVideoEntities = this.anchorVideoService.list(wrapper);

            if(anchorVideoEntities != null && anchorVideoEntities.size() > 0) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                for (AnchorVideoEntity anchorVideoEntity : anchorVideoEntities) {
                    for (UpdateVideoSizeDurationBo updateVideoSizeDurationBo : updateVideoUploadStatusBoList) {
                        if(anchorVideoEntity.getVideoId().equals(updateVideoSizeDurationBo.getVideoId())) {
                            anchorVideoEntity.setDuration(updateVideoSizeDurationBo.getDuration());
                            anchorVideoEntity.setVedioSizie(updateVideoSizeDurationBo.getFileSize());
                            if(!StringUtils.isEmpty(updateVideoSizeDurationBo.getEndTime())) {
                                LocalDateTime localDateTime = LocalDateTime.parse(updateVideoSizeDurationBo.getEndTime(), formatter);
                                anchorVideoEntity.setEndTime(Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant()));
                            }
                            if(!StringUtils.isEmpty(updateVideoSizeDurationBo.getIsRecording())) {
                                anchorVideoEntity.setIsRecording(updateVideoSizeDurationBo.getIsRecording());
                            }
                            anchorVideoEntity.setUpdateDate(new Date());
                            break;
                        }
                    }
                }

                this.anchorVideoService.updateBatchById(anchorVideoEntities);
            }
        }
    }

    @Override
    public void initVideoAnalysisStatus(Long userId, Long tenantId) {
        QueryWrapper<AnchorVideoEntity> wrapper = new QueryWrapper<>();
        wrapper.in("analysis_status", 1);
        wrapper.eq("user_id", userId);
        wrapper.eq("tenant_id", tenantId);

        List<AnchorVideoEntity> anchorVideoEntities = this.anchorVideoService.list(wrapper);

        if(anchorVideoEntities != null && anchorVideoEntities.size() > 0) {
            for (AnchorVideoEntity anchorVideoEntity : anchorVideoEntities) {
                anchorVideoEntity.setAnalysisStatus(3);
                anchorVideoEntity.setErrorReason("系统被退出");
                anchorVideoEntity.setUpdateDate(new Date());
            }

            this.anchorVideoService.updateBatchById(anchorVideoEntities);
        }
    }

    @Override
    public void saveOrUpdate(AnchorVideoInfoBo anchorVideoInfoBo) {

        AnchorVideoEntity oldVideo = this.anchorVideoService.getOne(new QueryWrapper<AnchorVideoEntity>().eq("video_id", anchorVideoInfoBo.getVideoId()));

        AnchorVideoEntity videoEntity = new AnchorVideoEntity();
        BeanUtils.copyProperties(anchorVideoInfoBo, videoEntity);

        // 兼容旧版本
        if(!StringUtils.isEmpty(videoEntity.getPlatformType()) && videoEntity.getPlatformType().contains("DouYinLive")) {
            videoEntity.setPlatformType("1");
        }

        if(oldVideo != null) {
            // 修改
            videoEntity.setId(oldVideo.getId());
            videoEntity.setUpdateDate(new Date());
            this.anchorVideoService.updateById(videoEntity);
        }else {
            // 添加
            videoEntity.setId(SnowflakeManager.nextValue());
            videoEntity.setCreateDate(new Date());
            videoEntity.setUpdateDate(new Date());
            this.anchorVideoService.save(videoEntity);
        }
    }

    /**
     * 批量保存巨量拉取的视频记录
     * 去重逻辑：同 batch_number + 同 userId 下，data_source=1 则跳过，否则软删除旧记录后插入
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<AnchorVideoInfoVo> savePulledVideos(List<AnchorVideoInfoBo> videoList, Long userId, Long tenantId) {
        List<AnchorVideoInfoVo> replacedVideoList = new ArrayList<>();
        if (CollUtil.isEmpty(videoList)) {
            return replacedVideoList;
        }

        // 批量查已存在的记录（按 batch_number + userId + tenantId + delete_status=0）
        List<String> batchNumbers = videoList.stream()
            .map(AnchorVideoInfoBo::getBatchNumber).filter(StrUtil::isNotBlank).distinct().toList();
        List<AnchorVideoEntity> existingList = new ArrayList<>();
        if (CollUtil.isNotEmpty(batchNumbers)) {
            existingList = this.anchorVideoService.list(
                new LambdaQueryWrapper<AnchorVideoEntity>()
                    .in(AnchorVideoEntity::getBatchNumber, batchNumbers)
                    .eq(AnchorVideoEntity::getUserId, userId)
                    .eq(AnchorVideoEntity::getTenantId, tenantId)
                    .eq(AnchorVideoEntity::getDeleteStatus, 0));
        }

        // 批量查主播信息
        List<String> secUids = videoList.stream()
            .map(AnchorVideoInfoBo::getSecUid).filter(StrUtil::isNotBlank).distinct().toList();
        Map<String, AnchorUrlEntity> anchorUrlMap = new HashMap<>();
        Map<String, AnchorUrlUserEntity> anchorUserMap = new HashMap<>();
        if (CollUtil.isNotEmpty(secUids)) {
            List<AnchorUrlEntity> anchorUrls = this.anchorUrlService.list(
                new QueryWrapper<AnchorUrlEntity>().in("sec_uid", secUids));
            if (CollUtil.isNotEmpty(anchorUrls)) {
                anchorUrlMap = anchorUrls.stream()
                    .collect(Collectors.toMap(AnchorUrlEntity::getSecUid, Function.identity(), (a, b) -> a));
            }
            List<AnchorUrlUserEntity> anchorUsers = this.anchorUrlUserService.list(
                new QueryWrapper<AnchorUrlUserEntity>()
                    .in("anchor_url_sec_uid", secUids)
                    .eq("user_id", userId));
            if (CollUtil.isNotEmpty(anchorUsers)) {
                anchorUserMap = anchorUsers.stream()
                    .collect(Collectors.toMap(AnchorUrlUserEntity::getAnchorUrlSecUid, Function.identity(), (a, b) -> a));
            }
        }

        Set<String> replacedBatchSet = new HashSet<>();
        for (AnchorVideoInfoBo videoBo : videoList) {
            if (!replacedBatchSet.contains(videoBo.getBatchNumber())) {
                List<AnchorVideoEntity> sameBatchExists = existingList.stream()
                    .filter(e -> videoBo.getBatchNumber().equals(e.getBatchNumber()))
                    .toList();
                boolean hasNonJuliang = sameBatchExists.stream()
                    .anyMatch(e -> Integer.valueOf(AnchorVideoEnums.dataSource.JULIANG.getCode()).equals(e.getDataSource()));
                if (hasNonJuliang) {
                    // 判断是否是巨量拉取的视频，如果是就跳过
                    continue;
                }else {
                    // 不是巨量拉取的视频，就删除
                    replacedBatchSet.add(videoBo.getBatchNumber());
                    // 同场次所有非巨量(data_source!=1)的记录都需要删除
                    replacedVideoList.addAll(sameBatchExists.stream()
                            .filter(e -> !Integer.valueOf(1).equals(e.getDataSource()))
                            .map(e -> BeanUtil.copyProperties(e, AnchorVideoInfoVo.class))
                            .toList());
                }
            }

            AnchorUrlEntity anchorUrl = anchorUrlMap.get(videoBo.getSecUid());
            AnchorUrlUserEntity anchorUser = anchorUserMap.get(videoBo.getSecUid());
            String anchorName = anchorUrl != null ? anchorUrl.getAnchorName() : "";

            AnchorVideoEntity videoEntity = new AnchorVideoEntity();
            BeanUtils.copyProperties(videoBo, videoEntity);
            videoEntity.setId(SnowflakeManager.nextValue());
            videoEntity.setUserId(userId);
            videoEntity.setTenantId(anchorUser != null ? anchorUser.getTenantId() : tenantId);
            // paragraph / subsectionType 由客户端分段计算后传入，仅在缺省时兜底为 0（不分段）
            videoEntity.setParagraph(videoBo.getParagraph() != null ? videoBo.getParagraph() : 0);
            videoEntity.setSubsectionType(videoBo.getSubsectionType() != null ? videoBo.getSubsectionType() : 0);
            videoEntity.setDefinition(0);
            videoEntity.setAnchorId(0);
            videoEntity.setTradeId(anchorUser != null ? anchorUser.getTradeId() : null);
            videoEntity.setVideoName((StrUtil.isNotBlank(anchorName) ? anchorName : "")
                + "_" + (videoBo.getStartTime() != null ? DateUtil.format(videoBo.getStartTime(), "yyyy年MM月dd日HH时mm分ss秒") : "")
                + "_第" + NumberChineseFormatter.format(videoEntity.getParagraph() + 1, false) + "段");
            videoEntity.setVideoSliceType(VideoSliceTypeEnum.VIDEO.getCode());
            videoEntity.setIsRecording(0);
            videoEntity.setSourceType(0);
            videoEntity.setAnalysisStatus(
                anchorUser != null && Integer.valueOf(1).equals(anchorUser.getIsAutoAnalysis())
                    ? AnchorVideoEnums.analysisStatus.NOT_ANALYZED.getCode()
                    : AnchorVideoEnums.analysisStatus.NOT_AUTO_ANALYSIS.getCode());
            videoEntity.setDataSource(AnchorVideoEnums.dataSource.JULIANG.getCode());
            videoEntity.setIsDownloaded(AnchorVideoEnums.downloadStatus.NOT_DOWNLOADED.getCode());
            videoEntity.setCreateDate(new Date());
            videoEntity.setUpdateDate(new Date());
            this.anchorVideoService.save(videoEntity);
        }

        // 批量软删除被替换的旧记录
        if (CollUtil.isNotEmpty(replacedVideoList)) {
            List<String> ids = replacedVideoList.stream()
                .map(AnchorVideoInfoVo::getVideoId).toList();
            batchUpdateVideoDelStatus(ids, 1);
        }

        return replacedVideoList;
    }

    @Override
    public List<AnchorVideoInfoVo> clientListVideoByVideoIds(List<String> ids, Long userId, Long tenantId) {

        if(ids == null || ids.isEmpty()) {
            return null;
        }

        QueryWrapper<AnchorVideoEntity> wrapper = new QueryWrapper<>();
        wrapper.in("video_id", ids);
        wrapper.eq("user_id", userId);
        wrapper.eq("tenant_id", tenantId);

        List<AnchorVideoEntity> anchorVideoEntities = this.anchorVideoService.list(wrapper);

        if(anchorVideoEntities != null && anchorVideoEntities.size() > 0) {
            List<AnchorVideoInfoVo> anchorVideoInfoVos = anchorVideoEntities.stream().map(item -> {
                AnchorVideoInfoVo videoInfoVo = new AnchorVideoInfoVo();
                BeanUtils.copyProperties(item, videoInfoVo);
                return videoInfoVo;
            }).collect(Collectors.toList());

            return anchorVideoInfoVos;
        }

        return null;
    }

    @Override
    public List<AnchorVideoInfoVo> listYesterdayBySecUids(List<String> secUidList, Long userId, Long tenantId) {
        QueryWrapper<AnchorVideoEntity> wrapper = new QueryWrapper<>();
        wrapper.in("sec_uid", secUidList);
        wrapper.eq("user_id", userId);
        wrapper.eq("tenant_id", tenantId);
        LocalDate now = LocalDate.now();
        LocalDate yesterday = now.minusDays(1);
        String yesterdayStr = yesterday.toString();
        wrapper.between("start_time", yesterdayStr + " 00:00:00", yesterdayStr + " 23:59:59");
        wrapper.ge("duration", 60);
        wrapper.eq("video_slice_type", 0);
        wrapper.orderByAsc("start_time");

        List<AnchorVideoEntity> anchorVideoEntities = this.anchorVideoService.list(wrapper);
        if(anchorVideoEntities != null && anchorVideoEntities.size() > 0) {
            List<AnchorVideoInfoVo> anchorVideoInfoVos = anchorVideoEntities.stream().map(item -> {
                AnchorVideoInfoVo videoInfoVo = new AnchorVideoInfoVo();
                BeanUtils.copyProperties(item, videoInfoVo);
                return videoInfoVo;
            }).collect(Collectors.toList());

            return anchorVideoInfoVos;
        }

        return null;
    }

    @Override
    public List<AnchorVideoInfoVo> listDayBeforeYesterdayBySecUids(List<String> secUidList, Long userId, Long tenantId) {
        QueryWrapper<AnchorVideoEntity> wrapper = new QueryWrapper<>();
        wrapper.in("sec_uid", secUidList);
        wrapper.eq("user_id", userId);
        wrapper.eq("tenant_id", tenantId);
        LocalDate now = LocalDate.now();
        LocalDate dayBeforeYesterday = now.minusDays(2);
        String dayBeforeYesterdayStr = dayBeforeYesterday.toString();
        wrapper.between("start_time", dayBeforeYesterdayStr + " 00:00:00", dayBeforeYesterdayStr + " 23:59:59");
        wrapper.ge("duration", 60);
        wrapper.eq("video_slice_type", 0);
        wrapper.orderByAsc("start_time");

        List<AnchorVideoEntity> anchorVideoEntities = this.anchorVideoService.list(wrapper);
        if (anchorVideoEntities != null && anchorVideoEntities.size() > 0) {
            List<AnchorVideoInfoVo> anchorVideoInfoVos = anchorVideoEntities.stream().map(item -> {
                AnchorVideoInfoVo videoInfoVo = new AnchorVideoInfoVo();
                BeanUtils.copyProperties(item, videoInfoVo);
                return videoInfoVo;
            }).collect(Collectors.toList());

            return anchorVideoInfoVos;
        }

        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncLocalDataToServer(SyncLocalDataToServerBo syncLocalDataToServerBo) {
        List<String> videoIds = syncLocalDataToServerBo.getVideoIds();
        // 处理视频
        QueryWrapper<AnchorVideoEntity> videoWrapper = new QueryWrapper<>();
        videoWrapper.eq("user_id", syncLocalDataToServerBo.getUserId());
        videoWrapper.eq("tenant_id", syncLocalDataToServerBo.getTenantId());
        videoWrapper.select("id", "video_id", "delete_status");
        List<AnchorVideoEntity> anchorVideoEntities = this.anchorVideoService.list(videoWrapper);
        if(anchorVideoEntities != null && anchorVideoEntities.size() > 0) {
            if(videoIds != null && videoIds.size() > 0) {
                for (AnchorVideoEntity anchorVideoEntity : anchorVideoEntities) {
                    anchorVideoEntity.setDeleteStatus(1);
                    for (String videoId : videoIds) {
                        if(anchorVideoEntity.getVideoId().equals(videoId)) {
                            anchorVideoEntity.setDeleteStatus(0);
                            break;
                        }
                    }
                }
                this.anchorVideoService.updateBatchById(anchorVideoEntities);
            }else {
                for (AnchorVideoEntity anchorVideoEntity : anchorVideoEntities) {
                    anchorVideoEntity.setDeleteStatus(1);
                }
                this.anchorVideoService.updateBatchById(anchorVideoEntities);
            }
        }


        List<String> fileIds = syncLocalDataToServerBo.getFileIds();
        if(fileIds != null && fileIds.size() > 0) {
            // 处理文件
            QueryWrapper<UploadFileEntity> wrapper = new QueryWrapper<>();
            wrapper.eq("user_id", syncLocalDataToServerBo.getUserId());
            wrapper.eq("tenant_id", syncLocalDataToServerBo.getTenantId());
            wrapper.select("id", "file_id");
            List<UploadFileEntity> uploadFileEntities = this.uploadFileService.list(wrapper);
            if(uploadFileEntities != null && uploadFileEntities.size() > 0) {
                List<String> delFileIds = new LinkedList<>();
                for (UploadFileEntity uploadFileEntity : uploadFileEntities) {
                    uploadFileEntity.setIsDeleted(1);
                    for (String fileId : fileIds) {
                        if(uploadFileEntity.getFileId().equals(fileId)) {
                            uploadFileEntity.setIsDeleted(0);
                            break;
                        }
                    }
                    if(uploadFileEntity.getIsDeleted() == 1) {
                        delFileIds.add(uploadFileEntity.getFileId());
                    }
                }
                if(delFileIds.size() > 0) {
                    this.uploadFileService.remove(new QueryWrapper<UploadFileEntity>().in("file_id", delFileIds));
                }
            }
        }else {
            this.uploadFileService.remove(new QueryWrapper<UploadFileEntity>()
                    .eq("user_id", syncLocalDataToServerBo.getUserId())
                    .eq("tenant_id", syncLocalDataToServerBo.getTenantId()));
        }

        List<String> contrastIds = syncLocalDataToServerBo.getContrastIds();
        QueryWrapper<SyncContrastEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", syncLocalDataToServerBo.getUserId());
        wrapper.eq("tenant_id", syncLocalDataToServerBo.getTenantId());
        wrapper.select("id", "contrast_id", "delete_status");
        List<SyncContrastEntity> syncContrastEntities = this.syncContrastService.list(wrapper);
        if(syncContrastEntities != null && syncContrastEntities.size() > 0) {
            if(contrastIds != null && contrastIds.size() > 0) {
                for (SyncContrastEntity syncContrastEntity : syncContrastEntities) {
                    syncContrastEntity.setDeleteStatus(1);
                    for (String contrastId : contrastIds) {
                        if(syncContrastEntity.getContrastId().equals(contrastId)) {
                            syncContrastEntity.setDeleteStatus(0);
                            break;
                        }
                    }
                }
                this.syncContrastService.updateBatchById(syncContrastEntities);
            }else {
                for (SyncContrastEntity syncContrastEntity : syncContrastEntities) {
                    syncContrastEntity.setDeleteStatus(1);
                }
                this.syncContrastService.updateBatchById(syncContrastEntities);
            }

        }


    }

    /**
     * 获取历史批次视频列表
     * @param videoId 视频ID
     * @param limit 限制返回数量,默认3条
     * @return 历史批次视频列表
     */
    @Override
    public List<HistoryBatchNumberVideoListVo> historyBatchNumberList(String videoId, Integer dataType, Integer uploadStatus, Integer limit) {
        // 设置默认limit
        limit = (limit == null || limit == 0) ? 3 : limit;
        List<HistoryBatchNumberVideoListVo> result = new ArrayList<>();

        // 1. 获取当前视频信息
        AnchorVideoEntity currentVideo = anchorVideoService.getOne(new LambdaQueryWrapper<AnchorVideoEntity>()
                .eq(AnchorVideoEntity::getVideoId, videoId)
                .last("limit 1")
        );
        if (currentVideo == null) {
            return result;
        }

        // 2. 获取同一主播的其他批次视频
        HistoryBatchNumberListBo bo = new HistoryBatchNumberListBo();
        bo.setDataType(dataType);
        bo.setAnalysisStatus(2);
        bo.setSecUid(currentVideo.getSecUid());
        bo.setUserId(currentVideo.getUserId());
        bo.setTenantId(currentVideo.getTenantId());
        bo.setVideoId(videoId);
        bo.setEndTime(DateUtil.formatDateTime(currentVideo.getStartTime()));
        bo.setUploadStatus(uploadStatus);
        bo.setLimit(limit);

        List<AnchorVideoEntity> batchList = anchorVideoService.historyBatchNumberList(bo);

//        List<AnchorVideoEntity> batchList = anchorVideoService.list(new QueryWrapper<AnchorVideoEntity>()
//                .lambda()
//                .eq(AnchorVideoEntity::getAnalysisStatus, 2) // 分析完成
//                .eq(AnchorVideoEntity::getSecUid, currentVideo.getSecUid())
//                .eq(AnchorVideoEntity::getUserId, currentVideo.getUserId())
//                .eq(AnchorVideoEntity::getTenantId, currentVideo.getTenantId())
//                .eq(uploadStatus == 0, AnchorVideoEntity::getDeleteStatus, 0) // 未删除
//                .eq(uploadStatus == 1, AnchorVideoEntity::getUploadStatus, uploadStatus)
//                .in(uploadStatus == 1, AnchorVideoEntity::getDeleteStatus, 0, 1) // 未删除
//                .ge(AnchorVideoEntity::getDuration, 60) // 时长大于60秒
//                .le(AnchorVideoEntity::getEndTime, DateUtil.formatDateTime(currentVideo.getStartTime()))
//                .ne(AnchorVideoEntity::getVideoId, videoId)
//                .exists(dataType == 0, "select 1 from tb_data_screenshot as ds where source_type = 0 and tb_anchor_video.video_id = ds.source_id and ds.screenshot_status = 3")
//                .exists(dataType == 1, "select 1 from tb_video_data_viewing_confuse as vdv where is_deleted = 0 and vdv.data_status = 1 and vdv.video_id = tb_anchor_video.video_id")
//                .orderByDesc(AnchorVideoEntity::getStartTime)
//                .last("limit " + limit)
//        );

        if (ObjectUtil.isNotEmpty(batchList)) {
            result = BeanUtil.copyToList(batchList, HistoryBatchNumberVideoListVo.class);
        }
        return result;
    }

    @Override
    public List<AnchorVideoInfoVo> listOneVideoByBatchNumber(List<Long> batchNumberIds) {
        List<AnchorVideoEntity> list = anchorVideoService.list(new LambdaQueryWrapper<AnchorVideoEntity>()
                .in(AnchorVideoEntity::getBatchNumber, batchNumberIds)
                .eq(AnchorVideoEntity::getParagraph, 0)
                .orderByAsc(AnchorVideoEntity::getParagraph)
        );
        if (ObjectUtil.isNotEmpty(list)){
            return BeanUtil.copyToList(list, AnchorVideoInfoVo.class);
        }
        return new ArrayList<>();
    }

    @Override
    public AnchorVideoInfoVo getLiveSessionByBatchNumber(String batchNumber, Long tenantId) {
        AnchorVideoEntity entity = anchorVideoService.getOne(new LambdaQueryWrapper<AnchorVideoEntity>()
                .eq(AnchorVideoEntity::getBatchNumber, batchNumber)
                .eq(AnchorVideoEntity::getParagraph, 0)
                .eq(AnchorVideoEntity::getTenantId, tenantId)
                .eq(AnchorVideoEntity::getDataSource, AnchorVideoEnums.dataSource.JULIANG.getCode())
                .orderByAsc(AnchorVideoEntity::getStartTime)
                .last("limit 1")
        );
        if (entity != null) {
            return BeanUtil.copyProperties(entity, AnchorVideoInfoVo.class);
        }
        return null;
    }


    @Override
    public Integer getVideoStatus(String videoId, AnchorVideoFileAllVo videoContentVo) {
        List<AnchorVideoEntity> list = anchorVideoService.lambdaQuery().eq(AnchorVideoEntity::getVideoId, videoId).list();
        if (!list.isEmpty()){
            AnchorVideoEntity anchorVideoEntity = list.get(0);
            AnchorVideoInfoVo infoVo = new AnchorVideoInfoVo();
            BeanUtil.copyProperties(anchorVideoEntity,infoVo);
            videoContentVo.setAnchorVideo(infoVo);
            return list.get(0).getAnalysisStatus();
        }
        return null;
    }

    @Override
    public boolean hasOneVideo(Long userId, Long tenantId) {
        Long count = anchorVideoService.lambdaQuery()
                .eq(AnchorVideoEntity::getUserId, userId)
                .eq(AnchorVideoEntity::getTenantId, tenantId)
                .eq(AnchorVideoEntity::getAnalysisStatus, 2)
                .gt(AnchorVideoEntity::getDuration, 60)
                .count();
        return count <= 0;
    }

    @Override
    public List<String> listIdsByTenantAndBatchNumberAndEndTime(Long tenantId, Long batchNumber, String startTime, String endTime) {

        LambdaQueryWrapper<AnchorVideoEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AnchorVideoEntity::getTenantId, tenantId);
        wrapper.eq(AnchorVideoEntity::getBatchNumber, batchNumber);
        wrapper.ge(AnchorVideoEntity::getEndTime, startTime);
        wrapper.le(AnchorVideoEntity::getEndTime, endTime);
        wrapper.select(AnchorVideoEntity::getVideoId);

        List<AnchorVideoEntity> anchorVideoEntities = this.anchorVideoService.list(wrapper);
        if(anchorVideoEntities != null && anchorVideoEntities.size() > 0) {
            return anchorVideoEntities.stream().map(AnchorVideoEntity::getVideoId).collect(Collectors.toList());
        }

        return null;
    }

    @Override
    public AnchorVideoInfoVo getBySecUidAndGeDuration(String secUids, int duration) {

        AnchorVideoEntity video = anchorVideoService.getOne(new LambdaQueryWrapper<AnchorVideoEntity>()
                .eq(AnchorVideoEntity::getSecUid, secUids)
                .eq(AnchorVideoEntity::getAnalysisStatus, 2)
                .ge(AnchorVideoEntity::getDuration, duration)
                .orderByDesc(AnchorVideoEntity::getAnalysisTime)
                .last("limit 1")
        );
        if (ObjectUtil.isEmpty(video)) {
            return null;
        }

        return BeanUtil.copyProperties(video, AnchorVideoInfoVo.class);
    }

    @Override
    public AnchorVideoInfoVo getPrevBySecUid(String secUid, Long userId, Long tenantId, Date beforeStartTime) {
        AnchorVideoEntity video = anchorVideoService.getOne(new LambdaQueryWrapper<AnchorVideoEntity>()
                .eq(AnchorVideoEntity::getSecUid, secUid)
                .eq(AnchorVideoEntity::getUserId, userId)
                .eq(AnchorVideoEntity::getTenantId, tenantId)
                .lt(AnchorVideoEntity::getStartTime, beforeStartTime)
                .eq(AnchorVideoEntity::getAnalysisStatus, 2)
                .orderByDesc(AnchorVideoEntity::getStartTime)
                .last("limit 1")
        );
        if (ObjectUtil.isEmpty(video)) {
            return null;
        }
        return BeanUtil.copyProperties(video, AnchorVideoInfoVo.class);
    }

    @Override
    public AnchorVideoInfoVo getParentVideoByVideoId(String videoId) {

        // 查询video
        AnchorVideoEntity video = anchorVideoService.getOne(new LambdaQueryWrapper<AnchorVideoEntity>()
                .eq(AnchorVideoEntity::getVideoId, videoId)
                .last("limit 1")
        );
        if (video == null) {
            return null;
        }

        // 判断类型
        if (ObjectUtil.equals(video.getVideoSliceType(), VideoSliceTypeEnum.VIDEO.getCode())) {
            return BeanUtil.copyProperties(video, AnchorVideoInfoVo.class);
        }

        // 查询切片表
        VideoSliceEntity slice = videoSliceService.lambdaQuery()
                .eq(VideoSliceEntity::getSourceId, videoId)
                .eq(VideoSliceEntity::getSourceType, VideoSliceTypeEnum.VIDEO.getCode())
                .last("limit 1")
                .one();

        if (slice == null) {
            return null;
        }

        // 查询父视频
        video = anchorVideoService.getOne(new LambdaQueryWrapper<AnchorVideoEntity>()
                .eq(AnchorVideoEntity::getVideoId, slice.getSourceParentId())
                .last("limit 1")
        );

        if (video == null) {
            return null;
        }
        return BeanUtil.copyProperties(video, AnchorVideoInfoVo.class);
    }

    @Override
    public AnchorVideoInfoVo getByVideoName(String videoName, Long userId, Long tenantId) {
        QueryWrapper<AnchorVideoEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("video_name", videoName);
        wrapper.eq("user_id", userId);
        wrapper.eq("tenant_id", tenantId);
        wrapper.last("limit 1");
        AnchorVideoEntity videoEntity = this.anchorVideoService.getOne(wrapper);
        if (videoEntity != null) {
            AnchorVideoInfoVo videoInfoVo = new AnchorVideoInfoVo();
            BeanUtils.copyProperties(videoEntity, videoInfoVo);
            return videoInfoVo;
        }
        return null;
    }

    /** 视频分析状态：2 = 分析完成 */
    private static final Integer ANALYSIS_STATUS_DONE = 2;

    @Override
    public Long sumAnalyzedDurationByTenantId(Long tenantId) {
        if (ObjectUtil.isEmpty(tenantId)) {
            return 0L;
        }
        QueryWrapper<AnchorVideoEntity> wrapper = new QueryWrapper<>();
        wrapper.select("COALESCE(SUM(duration), 0) AS total");
        wrapper.eq("tenant_id", tenantId);
        wrapper.eq("is_deleted", 0);
        wrapper.eq("analysis_status", ANALYSIS_STATUS_DONE);
        Map<String, Object> row = anchorVideoService.getMap(wrapper);
        if (row == null || row.get("total") == null) {
            return 0L;
        }
        Object total = row.get("total");
        if (total instanceof Number num) {
            return num.longValue();
        }
        return 0L;
    }

    /**
     * 判断指定用户是否录制过视频(仅限已分析)
     *
     * @param userId      用户id
     * @param tenantId    租户id
     * @param minDuration 最短录制时长
     *
     * @return true 录制过视频；false 未录制过视频
     */
    @Override
    public boolean hasRecord(long userId, long tenantId, int minDuration) {
        return anchorVideoService.lambdaQuery()
                .eq(AnchorVideoEntity::getUserId, userId)
                .eq(AnchorVideoEntity::getTenantId, tenantId)
                .eq(AnchorVideoEntity::getIsDeleted, 0)
                .eq(AnchorVideoEntity::getAnalysisStatus, 2)
                .ge(AnchorVideoEntity::getDuration, minDuration)
                .last("limit 1")
                .exists();
    }

    @Override
    public int countBySecUid(String secUid, Long userId, Long tenantId) {
        LambdaQueryWrapper<AnchorVideoEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AnchorVideoEntity::getSecUid, secUid);
        wrapper.eq(AnchorVideoEntity::getUserId, userId);
        wrapper.eq(AnchorVideoEntity::getTenantId, tenantId);
        wrapper.eq(AnchorVideoEntity::getIsRecording, 0);
        wrapper.eq(AnchorVideoEntity::getDeleteStatus, 0);
        wrapper.ge(AnchorVideoEntity::getDuration, 60);
        return (int) anchorVideoService.count(wrapper);
    }

    @Override
    public java.util.List<com.jiuyu.replay.words.vo.datahub.DataHubAnalysisStatsVo> aggregateDataHubAnalysisStats(
            java.util.Collection<Long> tenantIds, java.util.Date startDate, java.util.Date endDate) {
        if (tenantIds == null || tenantIds.isEmpty()) {
            return java.util.List.of();
        }
        java.util.List<com.jiuyu.replay.words.vo.datahub.DataHubAnalysisStatsVo> stats =
                anchorVideoDao.selectDataHubAnalysisStats(tenantIds, startDate, endDate);
        return stats == null ? java.util.List.of() : stats;
    }
}
