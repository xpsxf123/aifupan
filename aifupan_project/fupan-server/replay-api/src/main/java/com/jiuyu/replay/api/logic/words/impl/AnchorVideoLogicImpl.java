package com.jiuyu.replay.api.logic.words.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson2.JSON;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.replay.ai.bll.ConversationBll;
import com.jiuyu.replay.ai.bll.DiagnosisCueBll;
import com.jiuyu.replay.ai.bll.DiagnosisModelBll;
import com.jiuyu.replay.ai.vo.DataDiagnosisConfigVo;
import com.jiuyu.replay.api.logic.order.UserPropertyLogic;
import com.jiuyu.replay.api.logic.words.AnchorUrlLogic;
import com.jiuyu.replay.api.logic.words.AnchorVideoLogic;
import com.jiuyu.replay.common.bll.SystemKvBll;
import com.jiuyu.replay.common.constant.AiEnums;
import com.jiuyu.replay.common.constant.AnchorVideoEnums;
import com.jiuyu.replay.common.constant.CommonEnum;
import com.jiuyu.replay.common.constant.OrderEnums;
import com.jiuyu.replay.common.constant.WordsEnum;
import com.jiuyu.replay.common.tencent.TencentVodUtils;
import com.jiuyu.replay.common.utils.CommonUtils;
import com.jiuyu.replay.common.utils.ExecutorUtil;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.common.utils.ReplayFileUtils;
import com.jiuyu.replay.common.vo.SystemKvInfoVo;
import com.jiuyu.replay.generic.bo.ai.AiMessageBo;
import com.jiuyu.replay.generic.bo.ai.AiModelBo;
import com.jiuyu.replay.generic.bo.ai.SaveDiagnosisCueBo;
import com.jiuyu.replay.generic.bo.third.AiTokenUseRecordBo;

import com.jiuyu.replay.generic.bo.words.cue.CueWordsPageBo;
import com.jiuyu.replay.generic.enums.words.SliceSourceTypeEnum;
import com.jiuyu.replay.generic.enums.words.VideoSliceTypeEnum;
import com.jiuyu.replay.generic.feign.words.ScriptMonitorFeign;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.utils.ResultUtil;
import com.jiuyu.replay.generic.vo.ai.*;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.common.code.StatusCode;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.generic.vo.order.OrderInfoVo;
import com.jiuyu.replay.generic.vo.order.UserPropertyTypeInfoVo;
import com.jiuyu.replay.generic.vo.power.SalesInfoVo;
import com.jiuyu.replay.generic.vo.video.VideoSliceVo;
import com.jiuyu.replay.generic.vo.words.*;
import com.jiuyu.replay.order.bll.AiTokenUseRecordBll;
import com.jiuyu.replay.order.bll.OrderBll;
import com.jiuyu.replay.order.bll.UserPropertyBll;
import com.jiuyu.replay.power.bll.SalesBll;
import com.jiuyu.replay.power.bll.UserBll;
import com.jiuyu.replay.power.bll.UserDetailsBll;
import com.jiuyu.replay.power.bo.UserListBo;
import com.jiuyu.replay.power.utils.GlobalObject;
import com.jiuyu.replay.power.vo.UserCacheVo;
import com.jiuyu.replay.power.vo.UserDetailsVo;
import com.jiuyu.replay.power.vo.UserInfoVo;
import com.jiuyu.replay.power.vo.UserListVo;
import com.jiuyu.replay.ai.factory.ModelFactoryUtils;
import com.jiuyu.replay.ai.model.AiModel;
import com.jiuyu.replay.third.bll.AiModelBll;
import com.jiuyu.replay.third.bll.TableStoreBll;
import com.jiuyu.replay.third.tablestore.entity.BarrageBo;
import com.jiuyu.replay.third.zijie.ZiJieUtils;
import com.jiuyu.replay.words.api.CueWordsApi;
import com.jiuyu.replay.words.bll.*;
import com.jiuyu.replay.words.bo.*;
import com.jiuyu.replay.words.bo.video.*;
import com.jiuyu.replay.words.constant.Constant;
import com.jiuyu.replay.words.constant.SystemKeyConstant;
import com.jiuyu.replay.words.constant.WordsProperties;
import com.jiuyu.replay.words.entity.AiAnalysisRecordEntity;
import com.jiuyu.replay.words.entity.AudioAnalysisEntity;
import com.jiuyu.replay.words.enums.DataViewingStatusEnum;
import com.jiuyu.replay.words.enums.ImportantBarrageStatusEnum;
import com.jiuyu.replay.words.producer.VideoAnalysisRecordProducer;
import com.jiuyu.replay.words.vo.*;
import com.jiuyu.replay.words.vo.anchor.HistoryBatchNumberVideoListVo;
import com.jiuyu.replay.words.vo.file.UploadFileDetailVo;
import com.jiuyu.replay.words.vo.file.UploadFileInfoVo;
import com.jiuyu.replay.words.vo.video.GenerateTaskVo;
import com.jiuyu.replay.words.vo.video.ToGeneratedVo;
import com.jiuyu.replay.words.vo.viewing.VideoDataViewingConfuseInfoVo;
import com.jiuyu.replay.common.utils.SnowflakeManager;
import com.jiuyu.replay.words.entity.AnchorVideoRecycleEntity;
import com.jiuyu.replay.words.repository.dao.AnchorVideoRecycleDao;
import com.jiuyu.replay.words.vo.viewing.VideoDataViewingConfuseVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AnchorVideoLogicImpl implements AnchorVideoLogic {
    @Resource
    private AnchorVideoBll anchorVideoBll;
    @Resource
    TradeBll tradeBll;
    @Autowired
    private UserBll userBll;
    @Resource
    private TencentVodUtils tencentVodUtils;
    @Resource
    private UserPropertyLogic userPropertyLogic;
    @Resource
    private SensitiveWordsBll sensitiveWordsBll;
    @Resource
    private VideoAnalysisRecordBll videoAnalysisRecordBll;
    @Resource
    private WordsProperties wordsProperties;
    @Resource
    private VideoAnalysisRecordProducer videoAnalysisRecordProducer;
    @Resource
    private SyncContrastBll syncContrastBll;
    @Resource
    private TotalOnlineNumBll totalOnlineNumBll;
    @Resource
    private AiAnalysisRecordBll aiAnalysisRecordBll;
    @Resource
    private SalesBll salesBll;
    @Resource
    private UserDetailsBll userDetailsBll;

    @Resource
    private ZiJieUtils ziJieUtils;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private VideoDataViewingConfuseBll videoDataViewingConfuseBll;
    @Resource
    private SocketCollectMessageBll socketCollectMessageBll;
    @Resource
    private OrderBll orderBll;
    @Autowired
    private AnchorUrlBll anchorUrlBll;
    @Resource
    private CueWordsBll cueWordsBll;
    @Resource
    private AiTokenUseRecordBll aiTokenUseRecordBll;
    @Resource
    private AnchorVideoDetailBll anchorVideoDetailBll;
    @Resource
    private TableStoreBll tableStoreBll;
    @Resource
    private AiModelBll aiModelBll;
    @Resource
    private SystemKvBll systemKvBll;
    @Autowired
    private UserPropertyBll userPropertyBll;
    @Autowired
    private VideoSliceBll videoSliceBll;
    @Autowired
    private DiagnosisModelBll diagnosisModelBll;
    @Autowired
    private DiagnosisCueBll diagnosisCueBll;
    @Autowired
    private DataScreenshotBll dataScreenshotBll;
    @Resource
    private VideoDataViewingBll videoDataViewingBll;
    @Autowired
    private ConversationBll conversationBll;
    @Autowired
    private CueWordsApi cueWordsApi;
    @Autowired
    private BasicSettingsBll basicSettingsBll;
    @Autowired
    private AnchorCruxWordsBll anchorCruxWordsBll;
    @Autowired
    private AnchorUrlLogic anchorUrlLogic;
    @Resource
    private AnchorVideoRecycleDao anchorVideoRecycleDao;

    @Resource
    private com.jiuyu.replay.words.producer.AnchorVideoDetailProducer anchorVideoDetailProducer;
    @Resource
    private com.jiuyu.replay.words.producer.UploadFileDetailProducer uploadFileDetailProducer;
    @Resource
    private com.jiuyu.replay.words.producer.VideoContentProducer videoContentProducer;
    @Resource
    private com.jiuyu.replay.words.bll.VideoContentGenerator videoContentGenerator;
    @Resource
    private com.jiuyu.replay.generic.feign.third.AiFeign aiFeign;
    @Resource
    private com.jiuyu.replay.words.producer.TradeProducer tradeProducer;
    @Resource
    private com.jiuyu.replay.words.producer.AnchorVideoProducer anchorVideoProducer;
    @Resource
    private com.jiuyu.replay.words.producer.UploadFileProducer uploadFileProducer;
    @Resource
    private ScriptMonitorFeign scriptMonitorFeign;

    /**
     * 录制时上传视频信息
     *
     * @return
     */
    @Override
    public R<String> save(AnchorVideoInfoBo anchorVideoInfoBo) {
        UserCacheVo user = GlobalObject.getLocalUser();
        anchorVideoInfoBo.setUserId(user.getId());
        anchorVideoInfoBo.setTenantId(user.getActiveTenantId());
        return anchorVideoBll.save(anchorVideoInfoBo);
    }

    /**
     * 分页查询录制视频信息
     *
     * @param anchorVideoVO
     * @return
     */
    @Override
    public R<PageUtils<AnchorVideoVO>> pageLists(AnchorVideoBo anchorVideoVO) {

        //根据用户名称查询
        if (anchorVideoVO.getUserName() != null) {
            UserListBo userListBo = new UserListBo();
            userListBo.setKeyword(anchorVideoVO.getUserName());
            List<Long> longs = userBll.selectByNameOrNick(userListBo);
            if (longs != null && longs.size() > 0) {
                anchorVideoVO.setUserIdS(longs);
            } else {
                return R.ok(new PageUtils<>());
            }
        }

        R<PageUtils<AnchorVideoVO>> pageUtilsR = anchorVideoBll.pageLists(anchorVideoVO);
        List<AnchorVideoVO> list = pageUtilsR.getData().getList();
        //填充用户名
        if (list != null && list.size() > 0) {
            List<Long> userIds = list.stream().map(AnchorVideoVO::getUserId).toList();
            R<List<UserListVo>> listR = userBll.listByIds(userIds);
            if (listR.getData() != null && listR.getData().size() > 0) {
                list.stream().map(item -> {
                    List<UserListVo> list1 = listR.getData().stream().filter(vo -> vo.getId().equals(item.getUserId())).toList();
                    if (list1 != null && list1.size() > 0) {
                        item.setUserName(list1.get(0).getNickName());
                    }
                    return item;
                }).toList();
            }
        }

        return pageUtilsR;
    }

    /**
     * 根据视频id查询该视频分析
     *
     * @param id
     * @return
     */
    @Override
    public R<List<AudioAnalysisEntity>> selectByVideoId(AudioAnalysisBo id) {
        return anchorVideoBll.selectByVideoId(id);
    }

    /**
     * 新增直播视频分析的内容
     *
     * @param udioAnalysisVo
     * @return
     */
    @Override
    public R<String> saveAudioAnalysis(List<AudioAnalysisVo> udioAnalysisVo) {
        return anchorVideoBll.saveAudioAnalysis(udioAnalysisVo);
    }

    /**
     * 更新视频信息
     *
     * @param anchorVideoInfoBo
     * @return
     */
    @Override
    public R<String> updateVideo(AnchorVideoInfoBo anchorVideoInfoBo) {
        UserCacheVo user = GlobalObject.getLocalUser();
        anchorVideoInfoBo.setUserId(user.getId());
        anchorVideoInfoBo.setTenantId(user.getActiveTenantId());
        return anchorVideoBll.updateVideo(anchorVideoInfoBo);
    }

    /**
     * 客户端保存分析记录
     *
     * @param anchorVideoRecodVo
     */
    @Override
    public void saveAnchorVideoRecod(List<AudioAnalysisVo> anchorVideoRecodVo) {
        if (anchorVideoRecodVo != null && anchorVideoRecodVo.size() > 0) {
            UserCacheVo user = GlobalObject.getLocalUser();
            for (AudioAnalysisVo audioAnalysisVo : anchorVideoRecodVo) {
                audioAnalysisVo.setUserId(user.getId());
            }
            anchorVideoBll.save(anchorVideoRecodVo);
        }
    }

    /**
     * 分页查询分析记录
     *
     * @param anchorVideoBo
     * @return
     */
    @Override
    public R<PageUtils<AnchorVideoVO>> selectAnchorVideoRecod(AnchorVideoBo anchorVideoBo) {
        anchorVideoBo.setUserIdS(new ArrayList<>());
        // 版本搜索
        //根据版本查询
        if (ObjectUtil.isNotEmpty(anchorVideoBo.getPackageId())) {
            List<Long> userIdsByPackage = orderBll.getUserIdByPackageId(anchorVideoBo.getPackageId());
            if (userIdsByPackage == null || userIdsByPackage.isEmpty()) {
                return R.ok(new PageUtils<>());
            }
            List<Long> tenantIds = userBll.getTenantIdsByUserIds(userIdsByPackage);
            anchorVideoBo.setTenantIds(tenantIds);
        }

        PageUtils<AnchorVideoVO> data = anchorVideoBll.list(anchorVideoBo).getData();
        List<AnchorVideoVO> anchorVideolist = data.getList();

        // 填充信息
        if (ObjectUtil.isNotEmpty(anchorVideolist)) {
            List<Long> userIds = anchorVideolist.stream().map(AnchorVideoVO::getUserId).toList();
            List<String> videoIds = anchorVideolist.stream().map(AnchorVideoVO::getVideoId).toList();

            // 获取所有的AI分析关键词记录
            List<AiAnalysisRecordEntity> aiAnalysisRecordEntities = aiAnalysisRecordBll.allListByVideoIds(videoIds);
            Map<String, AiAnalysisRecordEntity> recordEntityMap = new HashMap<>();
            if (ObjectUtil.isNotEmpty(aiAnalysisRecordEntities)) {
                recordEntityMap = aiAnalysisRecordEntities.stream().collect(Collectors.toMap(AiAnalysisRecordEntity::getUuid, a -> a, (existing, replacement) -> replacement));
            }

            // 获取所有用户详情信息
            List<UserDetailsVo> userDetailsEntityList = userDetailsBll.listByUserIds(userIds);
            Map<Long, UserDetailsVo> detailsEntityMap = new HashMap<>();
            if (ObjectUtil.isNotEmpty(userDetailsEntityList)) {
                detailsEntityMap = userDetailsEntityList.stream().collect(Collectors.toMap(UserDetailsVo::getUserId, u -> u, (existing, replacement) -> replacement));
            }

            // 获取所有销售人员
            List<SalesInfoVo> salesEntityList = ResultUtil.getResult(salesBll.listByIds(ObjectUtil.isNotEmpty(userDetailsEntityList) ? userDetailsEntityList.stream().map(UserDetailsVo::getSaleId).distinct().toList() : null));
            Map<Long, SalesInfoVo> salesEntityMap = new HashMap<>();
            if (salesEntityList != null && !salesEntityList.isEmpty()) {
                salesEntityMap = salesEntityList.stream().collect(Collectors.toMap(SalesInfoVo::getId, s -> s, (a, b) -> b));
            }

            // 数据看板数据
            Map<String, VideoDataViewingConfuseInfoVo> dataataViewingConfuseMap = videoDataViewingConfuseBll
                    .listByVideoIdsAndStatus(videoIds, DataViewingStatusEnum.PULL_SUCCESS.getStatus())
                    .stream().collect(Collectors.toMap(VideoDataViewingConfuseVo::getVideoId, a -> a, (a, b) -> a));

            // 查询Websocket的数据
            Map<String, SocketCollectMessageInfoVo> socketCollectMessageMap = socketCollectMessageBll.listByVideoIds(videoIds)
                    .stream()
                    .collect(Collectors.toMap(SocketCollectMessageInfoVo::getVideoId, a -> a, (a, b) -> a));

            //填充用户名
            List<UserListVo> userList = ResultUtil.getResult(userBll.listByIds(userIds));
            Map<Long, UserListVo> userMap = new HashMap<>();
            if (ObjectUtil.isNotEmpty(userList)) {
                userMap = userList.stream().collect(Collectors.toMap(UserListVo::getId, a -> a, (a, b) -> a));
            }

            // 获取用户信息，以便查询用户版本
            List<Long> userOrderIds = new ArrayList<>();
            if (ObjectUtil.isNotEmpty(userList)) {
                userOrderIds = userList.stream().map(item -> {
                    if (item.getParentId() != null && item.getParentId() != 0) {
                        return item.getParentId();
                    }
                    return item.getId();
                }).toList();
            }
            // 查询用户版本信息

            Map<Long, OrderInfoVo> orderMap = orderBll.currentOrderByUserIds(userOrderIds)
                    .stream()
                    .collect(Collectors.toMap(OrderInfoVo::getUserId, Function.identity(), (o, n) -> o));
            Map<Long, Date> lastOrderDateMap = orderBll.userOrderExpireTimeByUserIds(userOrderIds);

            // 查询主播对应的主播类型
            List<String> secUidList = anchorVideolist.stream().map(AnchorVideoVO::getSecUid).toList();
            Map<String, AnchorUrlUserVo> secUidMap = anchorUrlBll.listBySecUidsAndUserIds(secUidList, userIds)
                    .stream()
                    .collect(Collectors.toMap(item -> item.getAnchorUrlSecUid() + "_" + item.getUserId(), item -> item, (item1, item2) -> item1));

            for (AnchorVideoVO item : anchorVideolist) {

                // 获取主播账户类型
                String keyName = item.getSecUid() + "_" + item.getUserId();
                AnchorUrlUserVo anchorUrlUserVo = secUidMap.get(keyName);
                if (anchorUrlUserVo != null) {
                    item.setAccountType(anchorUrlUserVo.getAccountType());
                }

                // 获取用户信息
                UserListVo userListVo = userMap.get(item.getUserId());
                if (userListVo != null) {
                    item.setUserName(userListVo.getNickName());

                    // 填充每条记录的用户销售人员名称
                    UserDetailsVo userDetailsEntity = detailsEntityMap.get(userListVo.getId());
                    if (userDetailsEntity != null) {
                        SalesInfoVo salesEntity = salesEntityMap.get(userDetailsEntity.getSaleId());
                        if (salesEntity != null) {
                            item.setUserSales(salesEntity.getSalesName());
                        }
                    }

                    // 填装版本名称和版本过期时间
                    Long currentUserId = userListVo.getParentId() != null && userListVo.getParentId() != 0 ? userListVo.getParentId() : userListVo.getId();
                    OrderInfoVo orderInfoVo = orderMap.get(currentUserId);
                    if (orderInfoVo != null) {
                        item.setPackageName(orderInfoVo.getCommodityName());
                    }

                    if (ObjectUtil.isNotEmpty(lastOrderDateMap)) {
                        item.setPackageExpiredTime(lastOrderDateMap.get(currentUserId));
                    }
                }

                // 填充每条记录进行AI分析后得到的关键词数量
                AiAnalysisRecordEntity aiAnalysisRecordEntity = recordEntityMap.get(item.getVideoId());
                if (aiAnalysisRecordEntity != null) {
                    item.setSensitiveWordTotal(aiAnalysisRecordEntity.getSensitiveWordTotal());
                    item.setSensitiveWordMark(aiAnalysisRecordEntity.getSensitiveWordMark());
                }

                // 填装场观和销售额
                VideoDataViewingConfuseInfoVo confuseInfoVo = dataataViewingConfuseMap.get(item.getVideoId());
                if (confuseInfoVo != null) {
                    item.setTotalWatchNum(confuseInfoVo.getTotalWatchNum());
                    item.setVolumeStart(confuseInfoVo.getVolumeStart());
                    item.setVolumeEnd(confuseInfoVo.getVolumeEnd());
                    item.setExistDataView(1);
                } else {
                    item.setExistDataView(0);
                    SocketCollectMessageInfoVo socketCollectMessageInfoVo = socketCollectMessageMap.get(item.getVideoId());
                    if (socketCollectMessageInfoVo != null && ObjectUtil.isNotEmpty(socketCollectMessageInfoVo.getObservationNum())) {
                        int totalWatchNum = CommonUtils.conversionObservationNum(NumberUtil.parseInt(socketCollectMessageInfoVo.getObservationNum(), 0), socketCollectMessageInfoVo.getOnlineMaxNum());
                        item.setTotalWatchNum(totalWatchNum == -1 ? null : totalWatchNum);
                    }
                }
            }
        }
        return R.ok(data);
    }

    /**
     * 取联合查询出来的用户ID集合的交集
     * @param collections 所有集合
     * @return 交集
     */
    public static <T> Collection<T> intersectionOrSingleSet(List<Collection<T>> collections) {
        if (collections == null || collections.isEmpty()) {
            return Collections.emptyList();
        }

        List<Set<T>> filteredSets = new ArrayList<>();
        for (Collection<T> collection : collections) {
            if (collection != null && !collection.isEmpty()) {
                // 确保所有的集合都是 Set 类型
                filteredSets.add(new HashSet<>(collection));
            }
        }

        int nonEmptyCount = filteredSets.size();
        if (nonEmptyCount == 0) {
            return Collections.emptyList(); // 所有集合都是 null 或空
        } else if (nonEmptyCount == 1) {
            // 如果只有一个非空集合，返回它的所有元素
            return filteredSets.get(0);
        } else {
            // 否则计算所有非空集合的交集
            Set<T> result = new HashSet<>(filteredSets.get(0));
            for (int i = 1; i < filteredSets.size(); i++) {
                result.retainAll(filteredSets.get(i));
            }
            return result;
        }
    }

    /**
     * 客户端分页查询录制视频信息
     *
     * @param anchorVideoBo
     * @return
     */
    @Override
    public R<PageUtils<AnchorVideoVO>> pageByUserId(AnchorVideoBo anchorVideoBo) {
        UserCacheVo user = GlobalObject.getLocalUser();
        anchorVideoBo.setUserId(user.getId());
        return anchorVideoBll.pageLists(anchorVideoBo);
    }

    /**
     * 客户端查询视频列表
     *
     * @return
     */
    @Override
    public R<List<AnchorVideoVO>> selectByuserId() {
        UserCacheVo user = GlobalObject.getLocalUser();
        return anchorVideoBll.selectByuserId(user.getId());
    }

    /**
     * 根据客户端上传的List<voidId>删除
     *
     * @param voidId
     * @return
     */
    @Override
    public R<String> removeByVoidId(List<String> voidId) {
        return anchorVideoBll.removeByVoidId(voidId);
    }

    /**
     * 客户端根据视频Id和行业ID查询该视频的分析内容
     * @param audioAnalysisBo
     * @return
     */
    @Override
    public R<List<AudioAnalysissVO>> selectByVideoIdOrTradeId(AudioAnalysisBo audioAnalysisBo) {

        // 根据视频id和行业id获取最后一条分析记录
        R<VideoAnalysisRecordInfoVo> recordInfoR = this.videoAnalysisRecordBll.infoLastByVideoIdAndTradeId(audioAnalysisBo.getVideoId(), audioAnalysisBo.getTradeId());
        if(recordInfoR.getCode() == 0 && recordInfoR.getData() != null) {
            VideoAnalysisRecordInfoVo videoAnalysisRecordInfoVo = recordInfoR.getData();

            // 获取分析数据
            String jsonDataStr = ReplayFileUtils.getFileContent(wordsProperties.getVideoAnalysisStorePath() + videoAnalysisRecordInfoVo.getStoreFileName());
            if(!StringUtils.isEmpty(jsonDataStr)) {
                List<SentenceMarkVo> sentenceMarkVos = JSON.parseArray(jsonDataStr, SentenceMarkVo.class);

                List<AudioAnalysissVO> audioAnalysissVOList = sentenceMarkVos.stream().map(item -> {
                    AudioAnalysissVO audioAnalysissVo = new AudioAnalysissVO();
                    audioAnalysissVo.setVideoId(videoAnalysisRecordInfoVo.getVideoId());
                    audioAnalysissVo.setUserId(videoAnalysisRecordInfoVo.getUserId());
                    audioAnalysissVo.setStatus(0);
                    audioAnalysissVo.setDataJson(JSON.toJSONString(item));
                    audioAnalysissVo.setTradeId(videoAnalysisRecordInfoVo.getTradeId() + "");
                    audioAnalysissVo.setParagraph(item.getCurrentSort());
                    return audioAnalysissVo;
                }).sorted(Comparator.comparingInt(AudioAnalysissVO::getParagraph)).collect(Collectors.toList());

                return R.ok(audioAnalysissVOList);
            }


        }

        return R.error(5001, "数据不存在");

//        return anchorVideoBll.selectByVideoIdOrTradeId(videoId);
    }

    /**
     * 服务端查询录制记录
     *
     * @return
     */
    @Override
    public R<PageUtils<AnchorVideoVO>> listByUserId(AnchorVideoBo anchorVideoBo) {
        R<PageUtils<AnchorVideoVO>> listR = anchorVideoBll.listByUserId(anchorVideoBo);
        List<AnchorVideoVO> list = listR.getData().getList();
        //填充行业信息
        if (list != null && list.size() > 0) {
            list.stream().map(item -> {
                if (item.getTradeId() != null) {
                    R<TradeInfoVo> info = tradeBll.info(item.getTradeId());
                    if (info != null && info.getData() != null) {
                        item.setTradeName(info.getData().getName());
                    }
                }
                return item;
            }).toList();
        }

        return listR;
    }

    /**
     * 服务端用户详情查询分析记录
     *
     * @return
     */
    @Override
    public R<PageUtils<AnchorVideoRecodListVo>> selectVideoRecod(AnchorVideoBo anchorVideoBo) {
        return anchorVideoBll.selectVideoRecod(anchorVideoBo);
    }

    @Override
    public void clearAnalysis(String videoId) {
        this.anchorVideoBll.clearAnalysis(videoId);
    }


    /**
     * 根据主播sec_uid获取已录制该主播的视频
     * @param anchorVideoBo
     * @return
     */
    @Override
    public R<PageUtils<AnchorVideoVO>> selectVideoBySecUid(AnchorVideoBo anchorVideoBo) {
        return anchorVideoBll.selectVideoBySecUid(anchorVideoBo);
    }

    /**
     * 根据视频ID获取分析记录内容
     * @param videoId
     * @return
     */
    @Override
    public R<AnchorVideoVO> videoAnalysisByVideoId(String videoId) {
        return anchorVideoBll.videoAnalysisByVideoId(videoId);
    }

    /**
     * 根据user_id获取录制分析
     * @param anchorVideoVO
     * @return
     */
    @Override
    public R<PageUtils<AnchorVideoVO>> videoAnalysisByUserId(AnchorVideoVO anchorVideoVO) {
        return anchorVideoBll.videoAnalysisByUserId(anchorVideoVO);
    }

    @Override
    public R<String> deleteOnlineVideo(String videoId) {

        // 获取视频信息
        R<AnchorVideoInfoVo> anchorVideoInfoVoR = this.anchorVideoBll.GetByVideoId(videoId);

        if(anchorVideoInfoVoR.getCode() == 0 && anchorVideoInfoVoR.getData() != null) {
            AnchorVideoInfoVo videoInfoVo = anchorVideoInfoVoR.getData();

            String playUrl = videoInfoVo.getPlayUrl();
            if(!StringUtils.isEmpty(playUrl)) {
                int index = playUrl.lastIndexOf("/");
                String vodFileId = playUrl.substring(index - 19, index);

                // 从vod删除视频
                String result = tencentVodUtils.deleteFile(vodFileId);

                if("success".equals(result)) {
                    UserCacheVo user = GlobalObject.getLocalUser();

                    // 校准用户的存储资产
                    updateStorageNum(user);
                    return R.ok();
                }

            }
        }

        return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "删除失败");
    }

    @Override
    public R<Boolean> checkVideoAnalysisExist(String videoId, Long tradeId) {
        VideoAnalysisRecordInfoVo videoAnalysisRecordInfoVo = this.videoAnalysisRecordProducer.infoLastByVideoIdAndTradeId(videoId, tradeId);
        return R.ok(videoAnalysisRecordInfoVo != null);
    }

    @Override
    public R<String> syncVideoAnalysisToServer(SyncVideoAnalysisBo syncVideoAnalysisBo) {

        UserCacheVo user = GlobalObject.getLocalUser();
        syncVideoAnalysisBo.setUserId(user.getId());
        return anchorVideoBll.syncVideoAnalysisToServer(syncVideoAnalysisBo);
    }

    @Override
    public R<List<AnchorVideoInfoVo>> getVideoListByTenantId() {
        UserCacheVo user = GlobalObject.getLocalUser();
        return anchorVideoBll.getVideoListByTenantId(user.getActiveTenantId());
    }

    @Override
    public R<AnchorVideoInfoVo> infoByVideoId(String videoId) {

        return anchorVideoBll.infoByVideoId(videoId);
    }

    @Override
    public R<PageUtils<AnchorVideoInfoVo>> listCloudVideo(CloudVideoListBo cloudVideoListBo) {

        UserCacheVo user = GlobalObject.getLocalUser();
        cloudVideoListBo.setTenantId(user.getActiveTenantId());

        R<PageUtils<AnchorVideoInfoVo>> pageUtilsR = anchorVideoBll.listCloudVideo(cloudVideoListBo);
        if(pageUtilsR.getCode() == 0 && pageUtilsR.getData() != null) {
            PageUtils<AnchorVideoInfoVo> pageUtils = pageUtilsR.getData();
            List<AnchorVideoInfoVo> list = pageUtils.getList();

            if(list != null && list.size() > 0) {
                // 封装用户名称
                Set<Long> userIds = list.stream().map(AnchorVideoInfoVo::getUserId).collect(Collectors.toSet());
                R<List<UserListVo>> userR = this.userBll.listByIds(userIds);
                List<UserListVo> userList = userR.getData();
                if(userList != null && userList.size() > 0) {
                    for (AnchorVideoInfoVo item : list) {
                        for (UserListVo userListVo : userList) {
                            if(userListVo.getId().equals(item.getUserId())) {
                                item.setUserNickName(userListVo.getNickName());
                                break;
                            }
                        }
                        item.setViewersNum(CommonUtils.conversionObservationNum(item.getViewersNum(), item.getOnlineMaxNum()));
                        item.setObservationNum(item.getViewersNum());
                    }
                }
            }
        }

        return pageUtilsR;
    }

    @Override
    public R<PageUtils<SyncContrastListVo>> listCloudContrast(CloudContrastListBo cloudContrastListBo) {

        UserCacheVo user = GlobalObject.getLocalUser();
        cloudContrastListBo.setTenantId(user.getActiveTenantId());

        R<PageUtils<SyncContrastListVo>> pageUtilsR = syncContrastBll.listCloudContrast(cloudContrastListBo);
        PageUtils<SyncContrastListVo> data = pageUtilsR.getData();
        List<SyncContrastListVo> list = data.getList();

        if(list != null && list.size() > 0) {
            // 封装用户名称
            Set<Long> userIds = list.stream().map(SyncContrastVo::getUserId).collect(Collectors.toSet());
            R<List<UserListVo>> userR = this.userBll.listByIds(userIds);
            List<UserListVo> userList = userR.getData();
            if(userList != null && userList.size() > 0) {
                for (SyncContrastListVo item : list) {
                    for (UserListVo userListVo : userList) {
                        if(userListVo.getId().equals(item.getUserId())) {
                            item.setUserNickName(userListVo.getNickName());
                            break;
                        }
                    }
                }
            }
        }

        return pageUtilsR;
    }

    /**
     * PC后端获取分析数据
     * @param fileId 文件ID
     * @param videoId 视频ID
     * @return
     */
    @Override
    public R<OnlineAnalysisInfoVo> getAnalysisInfo(String fileId, String videoId) {
        return sensitiveWordsBll.getAnalysisInfo(fileId,videoId);
    }

    @Override
    public R<PageUtils<AnchorVideoInfoVo>> clientVideoList(ClientVideoListBo clientVideoListBo) {

        UserCacheVo user = GlobalObject.getLocalUser();
        clientVideoListBo.setUserId(user.getId());
        clientVideoListBo.setTenantId(user.getActiveTenantId());
        clientVideoListBo.setDeleteStatus(0);
        clientVideoListBo.setIsCloud(false);

        return anchorVideoBll.clientVideoList(clientVideoListBo);
    }

    @Override
    public R<PageUtils<LocalSourceVideoVo>> listLocalSourceVideo(LocalSourceVideoBo bo) {

        UserCacheVo user = GlobalObject.getLocalUser();

        PageUtils<LocalSourceVideoVo> pageUtils = anchorVideoProducer.listLocalSourceVideo(
                user.getId(), user.getActiveTenantId(), bo.getPage(), bo.getLimit());

        return R.ok(pageUtils);
    }

    @Override
    public R<String> clientDeleteVideo(List<String> ids) {

        UserCacheVo user = GlobalObject.getLocalUser();
        // 子账号自能操作自己的
        Long userId = user.getUserType() == 2 ? user.getId() : null;

        return anchorVideoBll.clientDeleteVideo(ids, user.getActiveTenantId(), userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<String> clientDeleteCloudVideo(String videoId) {

        // 获取视频信息
        R<AnchorVideoInfoVo> videoInfoVoR = this.anchorVideoBll.GetByVideoId(videoId);
        if(videoInfoVoR.getCode() == 0 && videoInfoVoR.getData() != null) {

            AnchorVideoInfoVo videoInfoVo = videoInfoVoR.getData();

            UserCacheVo user = GlobalObject.getLocalUser();
            if(!(user.getId().equals(videoInfoVo.getUserId()) || (user.getActiveTenantId().equals(videoInfoVo.getTenantId()) && user.getUserType() == 0))) {
                return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "没有权限，删除失败");
            }

            if(videoInfoVo.getUploadStatus() == 1) {
                // 从vod删除视频
                String playUrl = videoInfoVo.getPlayUrl();
                if(!StringUtils.isEmpty(playUrl)) {
                    int index = playUrl.lastIndexOf("/");
                    String vodFileId = playUrl.substring(index - 19, index);
                    tencentVodUtils.deleteFile(vodFileId);
                }

                // 处理视频信息
                anchorVideoBll.clientDeleteCloudVideo(videoId);

                // 校准用户的存储资产
                updateStorageNum(user);
            }

            return R.ok("删除成功");
        }

        return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "删除失败");
    }

    /**
     * 批量上限：超出此值直接拒绝，避免 VOD 单条 SDK 串行调用过久阻塞接口。
     * 上限范围内 DB 操作恒为常数次（无 N+1）。
     */
    private static final int BATCH_DELETE_CLOUD_MAX_SIZE = 100;

    /**
     * 客户端批量删除云空间视频。
     * 将被删视频写入回收站表 {@code tb_anchor_video_recycle}，不再调用腾讯云 VOD 物理删除。
     * 原有软删逻辑保留，为将来恢复功能预留数据基础。
     *
     * @param videoIds 视频uuid集合，单批最多 100 条
     * @return 删除结果文案，含 成功 / 无权限 / 非云端 / 不存在 四类计数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<String> clientBatchDeleteCloudVideos(List<String> videoIds) {

        // 1) 入参校验：空集合直接返回；超过批量上限拒绝（提示前端分批）
        if (videoIds == null || videoIds.isEmpty()) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "输入的数据为空");
        }
        // 入参去重（保留顺序），避免重复 id 导致计数失真
        List<String> distinctIds = videoIds.stream().filter(StrUtil::isNotBlank).distinct().collect(Collectors.toList());
        if (distinctIds.isEmpty()) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "输入的数据为空");
        }
        if (distinctIds.size() > BATCH_DELETE_CLOUD_MAX_SIZE) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "单次最多删除 " + BATCH_DELETE_CLOUD_MAX_SIZE + " 条");
        }

        UserCacheVo user = GlobalObject.getLocalUser();

        // 2) 一次 SELECT IN：拿到所有视频；缺失的 id 由集合差集得出，不再回查 DB
        List<AnchorVideoInfoVo> videoInfoVos = ResultUtil.getResult(anchorVideoBll.listByVideoIds(distinctIds));
        if (videoInfoVos == null || videoInfoVos.isEmpty()) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "数据不存在");
        }

        // 3) 内存过滤：权限校验 + 仅处理已上传云空间(uploadStatus==1)的视频；其余按理由分类计数
        //    权限规则与单条版本对齐：本人 OR (同租户且主账号 userType==0)
        Long currentUserId = user.getId();
        Long currentTenantId = user.getActiveTenantId();
        boolean isMainAccount = user.getUserType() == 0;

        List<AnchorVideoInfoVo> candidates = new ArrayList<>(videoInfoVos.size());
        int noPermissionCount = 0;
        int notCloudCount = 0;
        for (AnchorVideoInfoVo vo : videoInfoVos) {
            boolean owned = currentUserId.equals(vo.getUserId());
            boolean sameTenantMain = isMainAccount && currentTenantId.equals(vo.getTenantId());
            if (!(owned || sameTenantMain)) {
                noPermissionCount++;
                continue;
            }
            if (vo.getUploadStatus() == null || vo.getUploadStatus() != 1) {
                notCloudCount++;
                continue;
            }
            candidates.add(vo);
        }

        // 4) 组装回收站记录并批量写入；同时收集软删所需 id 列表
        //    所有 candidates 均视为成功（不再依赖 VOD 结果决定）
        List<String> vodSuccessIds = new ArrayList<>(candidates.size());
        List<String> hardDeleteIds = new ArrayList<>();
        List<AnchorVideoRecycleEntity> recycleList = new ArrayList<>(candidates.size());
        LocalDateTime now = LocalDateTime.now();
        for (AnchorVideoInfoVo vo : candidates) {
            String playUrl = vo.getPlayUrl();
            String fileId;
            if (EmptyUtil.isEmpty(playUrl)) {
                fileId = "";
            } else {
                // playUrl 末尾 "/" 前的 19 位 = 腾讯云 VOD fileId
                int idx = playUrl.lastIndexOf("/");
                fileId = playUrl.substring(idx - 19, idx);
            }
            AnchorVideoRecycleEntity recycle = new AnchorVideoRecycleEntity();
            recycle.setId(SnowflakeManager.nextValue());
            recycle.setVideoId(vo.getVideoId());
            recycle.setVideoName(vo.getVideoName());
            recycle.setPlayUrl(playUrl == null ? "" : playUrl);
            recycle.setFileId(fileId);
            recycle.setVideoSize(vo.getVedioSizie());
            recycle.setUserId(user.getId());
            recycle.setUserName(user.getNickName());
            recycle.setTenantId(user.getActiveTenantId());
            recycle.setDeleteTime(now);
            recycle.setRestoreStatus(0);
            recycle.setVodDeleted(0);
            recycle.setIsDeleted(0);
            recycle.setCreateDate(now);
            recycle.setUpdateDate(now);
            recycleList.add(recycle);

            vodSuccessIds.add(vo.getVideoId());
            if (vo.getDeleteStatus() != null && vo.getDeleteStatus() == 1) {
                hardDeleteIds.add(vo.getVideoId());
            }
        }

        // 5) 批量写入回收站 + 调用原软删逻辑（均在同一事务边界内，任一异常均全量回滚）
        if (!vodSuccessIds.isEmpty()) {
            anchorVideoRecycleDao.batchInsert(recycleList);
            log.info("[clientBatchDeleteCloudVideos] 回收站落库完成，tenantId={}, count={}", currentTenantId, recycleList.size());

            anchorVideoBll.clientBatchDeleteCloudVideo(vodSuccessIds, hardDeleteIds);

            // 6) 校准用户存储资产（与单条版本一致：全量重算 + 写回父账号/本人）
            updateStorageNum(user);
        }

        // 7) 汇总返回：成功 / 无权限 / 非云端 / 不存在 四类计数
        int missingCount = distinctIds.size() - videoInfoVos.size();
        String msg = String.format("删除成功 %d 条；无权限 %d 条；非云端视频 %d 条；不存在 %d 条",
                vodSuccessIds.size(), noPermissionCount, notCloudCount, Math.max(missingCount, 0));
        return R.ok(msg);
    }

    private void updateStorageNum(UserCacheVo user) {
        R<Long> longR = anchorVideoBll.statisticsStoreByTenantId(user.getActiveTenantId());
        if (longR.getCode() == 0) {
            UserInfoVo currentUser = ResultUtil.getResult(userBll.info(user.getId(), false));
            if (currentUser != null) {
                userPropertyBll.updateByPropertyNum(ObjectUtil.defaultIfNull(currentUser.getParentId(), 0L) > 0 ? currentUser.getParentId() : user.getId(), "storageNum", longR.getData());
            }
        }
    }


    @Override
    public R<PageUtils<AnchorVideoInfoVo>> clientListCloudVideo(ClientVideoListBo clientVideoListBo) {

        UserCacheVo user = GlobalObject.getLocalUser();
        clientVideoListBo.setUploadStatus(1);
        clientVideoListBo.setTenantId(user.getActiveTenantId());
        clientVideoListBo.setIsCloud(true);

        if(!StringUtils.isEmpty(clientVideoListBo.getUserKeyword())) {
            // 查用户id集合
            R<List<Long>> userListR = this.userBll.listIdsByLikePhoneOrName(clientVideoListBo.getUserKeyword());
            List<Long> userIds = userListR.getData();
            if (userListR.getCode() != 0 || userIds == null || userIds.isEmpty()) {
                userIds = new LinkedList<>();
                userIds.add(0L);
            }

            clientVideoListBo.setUserIdList(userIds);
        }

        R<PageUtils<AnchorVideoInfoVo>> pageUtilsR = anchorVideoBll.clientVideoList(clientVideoListBo);
        if(pageUtilsR.getCode() == 0 && pageUtilsR.getData() != null) {
            PageUtils<AnchorVideoInfoVo> pageUtils = pageUtilsR.getData();
            List<AnchorVideoInfoVo> list = pageUtils.getList();

            if (list != null && !list.isEmpty()) {

                // 封装用户名称
                Set<Long> userIds = list.stream().map(AnchorVideoInfoVo::getUserId).collect(Collectors.toSet());
                Map<Long, UserListVo> userMap = ObjectUtil.defaultIfNull(ResultUtil.getResult(this.userBll.listByIds(userIds)), new ArrayList<UserListVo>())
                        .stream()
                        .collect(Collectors.toMap(UserListVo::getId, Function.identity(), (oldValue, newValue) -> newValue));


                for (AnchorVideoInfoVo item : list) {

                    // 从用户Map中获取用户昵称
                    if (!userMap.isEmpty() && userMap.containsKey(item.getUserId())) {
                        item.setUserNickName(userMap.get(item.getUserId()).getNickName());
                    }

                    // 判断内容诊断报告的状态 hasDiagnosisReport==1 and diagnosisOssName is not null ? 1 : 0
                    item.setHasDiagnosisReport(ObjectUtil.equal(item.getHasDiagnosisReport(), 1) && ObjectUtil.isNotEmpty(item.getDiagnosisOssName()) ? 1 : 0);
                    // 判断数据诊断报告的状态 hasDataDiagnosisReport==1 and dataDiagnosisOssName is not null ? 1 : 0
                    item.setHasDataDiagnosisReport(ObjectUtil.equal(item.getHasDataDiagnosisReport(), 1) && ObjectUtil.isNotEmpty(item.getDataDiagnosisOssName()) ? 1 : 0);

                }

            }
        }

        return pageUtilsR;
    }

    @Override
    public R<List<AnchorVideoInfoVo>> listUserVideo(ListUserVideoByConditionBo listUserVideoByConditionBo) {

        UserCacheVo user = GlobalObject.getLocalUser();
        listUserVideoByConditionBo.setUserId(user.getId());
        listUserVideoByConditionBo.setTenantId(user.getActiveTenantId());

        return anchorVideoBll.listUserVideo(listUserVideoByConditionBo);
    }

    @Override
    public R<List<AnchorVideoInfoVo>> listRecentLiveSessions(ListRecentLiveSessionBo listRecentLiveSessionBo) {

        UserCacheVo user = GlobalObject.getLocalUser();

        return anchorVideoBll.listRecentLiveSessions(user.getId(), user.getActiveTenantId(), listRecentLiveSessionBo.getStartTimeGe());
    }

    @Override
    public R<String> updateVideoAnalysisStatus(UpdateVideoAnalysisStatusBo updateVideoAnalysisStatusBo) {

        UserCacheVo user = GlobalObject.getLocalUser();
        updateVideoAnalysisStatusBo.setUserId(user.getId());
        updateVideoAnalysisStatusBo.setTenantId(user.getActiveTenantId());

        R<String> stringR = anchorVideoBll.updateVideoAnalysisStatus(updateVideoAnalysisStatusBo);

        // 判断是否需要推荐行业
        if (stringR.getCode() == 0 && updateVideoAnalysisStatusBo.getAnalysisStatus() == 2) {
            anchorVideoDetailBll.initUpdateSuggestTrade(updateVideoAnalysisStatusBo.getVideoId());
        }

        // status=2 时一次查 videoInfo，供主播关键词和 B7 自动触发两段共享
        if (stringR.getCode() == 0 && updateVideoAnalysisStatusBo.getAnalysisStatus() == 2) {
            AnchorVideoInfoVo videoInfo = null;
            try {
                videoInfo = ResultUtil.getResult(anchorVideoBll.infoByVideoId(updateVideoAnalysisStatusBo.getVideoId()));
            } catch (Exception e) {
                log.error("[updateVideoAnalysisStatus] infoByVideoId 失败 videoId={}", updateVideoAnalysisStatusBo.getVideoId(), e);
            }

            if (videoInfo != null) {
                // 判断主播是否有关键词，没有的话异步生成关键词
                try {
                    if (ObjectUtil.isNotEmpty(videoInfo.getSecUid())) {
                        // 获取视频时长要求
                        Integer videoDuration = systemKvBll.getValueByKey(SystemKeyConstant.generateAnchorKeywordVideoDuration, 1800);
                        if (videoInfo.getDuration() >= videoDuration) {
                            String secUid = videoInfo.getSecUid();
                            R<List<AnchorCruxWordsVo>> keywordsR = anchorCruxWordsBll.listBySecUid(secUid);
                            if (keywordsR.getCode() == 0 && (keywordsR.getData() == null || keywordsR.getData().isEmpty())) {
                                String videoId = updateVideoAnalysisStatusBo.getVideoId();
                                ExecutorUtil.getAnchorKeywordTaskExecutor().execute(() -> {
                                    try {
                                        List<String> keywords = anchorUrlLogic.startGetAnchorKeywords(secUid, videoId);
                                        if (ObjectUtil.isNotEmpty(keywords)) {
                                            anchorCruxWordsBll.saveAnchorKeywords(secUid, keywords, user.getId(), user.getActiveTenantId());
                                        }
                                    } catch (Exception e) {
                                        log.error("[生成主播关键词] 异步生成主播关键词失败 secUid={} videoId={}", secUid, videoId, e);
                                    }
                                });
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("[生成主播关键词] 判断主播关键词失败 videoId={}", updateVideoAnalysisStatusBo.getVideoId(), e);
                }

                // 录制完成时自动触发话术质检/互动巡检报告生成（B7）
                try {
                    scriptMonitorFeign.autoTriggerForVideo(
                            updateVideoAnalysisStatusBo.getVideoId(),
                            user.getId(),
                            user.getActiveTenantId(),
                            videoInfo.getSecUid());
                } catch (Exception e) {
                    log.error("[B7 自动触发] 失败 videoId={}", updateVideoAnalysisStatusBo.getVideoId(), e);
                }
            }
        }

        return stringR;
    }

    @Override
    public R<String> updateVideoUploadStatus(UpdateVideoUploadStatusBo updateVideoUploadStatusBo) {

        UserCacheVo user = GlobalObject.getLocalUser();
        updateVideoUploadStatusBo.setUserId(user.getId());
        updateVideoUploadStatusBo.setTenantId(user.getActiveTenantId());

        return anchorVideoBll.updateVideoUploadStatus(updateVideoUploadStatusBo);
    }

    @Override
    public R<AnchorVideoInfoVo> clientGetVideoByVideoId(String videoId) {

        UserCacheVo user = GlobalObject.getLocalUser();

        return anchorVideoBll.clientGetVideoByVideoId(videoId, null, user.getActiveTenantId());
    }

    @Override
    public R<AnchorVideoInfoVo> getLiveSessionByBatchNumber(String batchNumber) {

        UserCacheVo user = GlobalObject.getLocalUser();

        return anchorVideoBll.getLiveSessionByBatchNumber(batchNumber, user.getActiveTenantId());
    }

    @Override
    public R<String> updateVideoSizeDuration(List<UpdateVideoSizeDurationBo> updateVideoUploadStatusBoList) {

        if(updateVideoUploadStatusBoList == null || updateVideoUploadStatusBoList.size() < 1) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "数据不能为空");
        }

        UserCacheVo user = GlobalObject.getLocalUser();
        for (UpdateVideoSizeDurationBo updateVideoSizeDurationBo : updateVideoUploadStatusBoList) {
            updateVideoSizeDurationBo.setUserId(user.getId());
            updateVideoSizeDurationBo.setTenantId(user.getActiveTenantId());
        }

        return anchorVideoBll.updateVideoSizeDuration(updateVideoUploadStatusBoList);
    }

    @Override
    public R<String> initVideoAndFileAnalysisStatus() {

        UserCacheVo user = GlobalObject.getLocalUser();

        return anchorVideoBll.initVideoAndFileAnalysisStatus(user.getId(), user.getActiveTenantId());
    }

    @Override
    public R<String> saveOrUpdateVideo(AnchorVideoInfoBo anchorVideoInfoBo) {

        UserCacheVo user = GlobalObject.getLocalUser();
        anchorVideoInfoBo.setUserId(user.getId());
        anchorVideoInfoBo.setTenantId(user.getActiveTenantId());

        return anchorVideoBll.saveOrUpdateVideo(anchorVideoInfoBo);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public R<String> savePulledVideos(List<AnchorVideoInfoBo> videoList) {
        UserCacheVo user = GlobalObject.getLocalUser();

        R<List<AnchorVideoInfoVo>> bllResult = anchorVideoBll.savePulledVideos(
            videoList, user.getId(), user.getActiveTenantId());
        List<AnchorVideoInfoVo> replacedVideos = bllResult.getData();

        // 云端视频清理
        if (replacedVideos != null && !replacedVideos.isEmpty()) {
            List<AnchorVideoInfoVo> cloudVideos = replacedVideos.stream()
                .filter(v -> v.getUploadStatus() != null && v.getUploadStatus() == 1)
                .toList();

            if (!cloudVideos.isEmpty()) {
                List<String> cloudVideoIds = new ArrayList<>(cloudVideos.size());
                List<AnchorVideoRecycleEntity> recycleList = new ArrayList<>(cloudVideos.size());
                LocalDateTime now = LocalDateTime.now();

                for (AnchorVideoInfoVo vo : cloudVideos) {
                    String playUrl = vo.getPlayUrl();
                    String fileId = "";
                    if (StrUtil.isNotEmpty(playUrl)) {
                        int idx = playUrl.lastIndexOf("/");
                        fileId = playUrl.substring(idx - 19, idx);
                    }
                    AnchorVideoRecycleEntity recycle = new AnchorVideoRecycleEntity();
                    recycle.setId(SnowflakeManager.nextValue());
                    recycle.setVideoId(vo.getVideoId());
                    recycle.setVideoName(vo.getVideoName());
                    recycle.setPlayUrl(playUrl == null ? "" : playUrl);
                    recycle.setFileId(fileId);
                    recycle.setVideoSize(vo.getVedioSizie());
                    recycle.setUserId(user.getId());
                    recycle.setUserName(user.getNickName());
                    recycle.setTenantId(user.getActiveTenantId());
                    recycle.setDeleteTime(now);
                    recycle.setRestoreStatus(0);
                    recycle.setVodDeleted(0);
                    recycle.setIsDeleted(0);
                    recycle.setCreateDate(now);
                    recycle.setUpdateDate(now);
                    recycleList.add(recycle);
                    cloudVideoIds.add(vo.getVideoId());
                }

                anchorVideoRecycleDao.batchInsert(recycleList);
                anchorVideoBll.clientBatchDeleteCloudVideo(cloudVideoIds, cloudVideoIds);
            }
        }

        // 配额重算
        updateStorageNum(user);

        return R.ok();
    }

    @Override
    public R<List<AnchorVideoInfoVo>> clientListVideoByVideoIds(List<String> ids) {

        UserCacheVo user = GlobalObject.getLocalUser();

        return anchorVideoBll.clientListVideoByVideoIds(ids, user.getId(), user.getActiveTenantId());
    }

    @Override
    public R<Integer> checkUserSyncLocalData() {

        return R.ok(1);

//        UserCacheVo user = GlobalObject.getLocalUser();
//
//        Object o = this.redisTemplate.opsForValue().get("replay:temp:sync-local-data:" + user.getId() + user.getActiveTenantId());
//
//        if(o != null) {
//            return R.ok(1);
//        }
//
//        return R.ok(0);
    }

    @Override
    public R<String> syncLocalDataToServer(SyncLocalDataToServerBo syncLocalDataToServerBo) {

        UserCacheVo user = GlobalObject.getLocalUser();

        Object o = this.redisTemplate.opsForValue().get("replay:temp:sync-local-data:" + user.getId() + user.getActiveTenantId());

        if(o != null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "已经同步过，不需要再同步");
        }

        syncLocalDataToServerBo.setUserId(user.getId());
        syncLocalDataToServerBo.setTenantId(user.getActiveTenantId());

        return anchorVideoBll.syncLocalDataToServer(syncLocalDataToServerBo);
    }

    @Override
    public R<List<HistoryBatchNumberVideoListVo>> historyBatchNumberVideoList(String videoId, Integer dataType, Integer uploadStatus, Integer limit) {
        return anchorVideoBll.historyBatchNumberVideoList(videoId, dataType, uploadStatus, limit);
    }

    @Override
    @Transactional
    public R<Boolean> startImportantBarrage(String videoId) {
        AnchorVideoInfoVo video = ResultUtil.getResult(anchorVideoBll.infoByVideoId(videoId));
        if (video == null) {
            throw new BusinessException(StatusCode.DATA_NOT_EXIST.getCode(), "视频找不到");
        }
        if (!ObjectUtil.equals(video.getVideoSliceType(), VideoSliceTypeEnum.VIDEO.getCode())) {
            VideoSliceVo videoSlice = videoSliceBll.getVideoSliceBySourceId(video.getVideoId(), SliceSourceTypeEnum.VIDEO_SLICE.getCode());
            if (ObjectUtil.isNotEmpty(videoSlice.getSourceParentId())) {
                videoId = videoSlice.getSourceParentId();
            }
        }
        final String newVideoId = videoId;
        Boolean isYes = anchorVideoDetailBll.startImportantBarrage(newVideoId);
        if (isYes) {
            // 获取弹幕列表
            List<BarrageBo> list = ResultUtil.getResult(tableStoreBll.listVideoBarrageByVideoId(video.getUserId(), video.getTenantId(), video.getBatchNumber(), newVideoId));
            if (list == null || list.isEmpty()) {
                int status = ImportantBarrageStatusEnum.ACQUISITION_FAILED.getCode();
                String str = "当前视频没有弹幕数据，无法分析重要弹幕";
                // 设置重要弹幕的状态
                AnchorVideoDetailBo update = new AnchorVideoDetailBo();
                update.setVideoId(newVideoId);
                update.setImportantBarrageStatus(status);
                update.setImportantBarrageError(str);
                anchorVideoDetailBll.updateByVideoId(update);
                return R.error(30001, str);
            }
            ExecutorUtil.getImportantBarrageTaskExecutor().execute(() -> startImportantBarrageAsync(newVideoId));
        }
        return R.ok(true);
    }

    @Override
    public DataDiagnosisConfigVo getDataDiagnosisConfig(String sourceId, Integer sourceType, String webVersion) {
        if (!ObjectUtil.equals(sourceType, WordsEnum.sourceType.VIDEO.getCode())) {
            throw new BusinessException("只有视频有数据诊断功能");
        }

        AnchorVideoInfoVo video = ResultUtil.getResult(anchorVideoBll.infoByVideoId(sourceId));
        if (video == null) {
            throw new BusinessException("视频不存在");
        }
        DataDiagnosisConfigVo data = new DataDiagnosisConfigVo();
        data.setSourceId(sourceId);
        data.setSourceType(sourceType);
        data.setPlatform(NumberUtil.parseInt(video.getPlatformType(), 1) - 1);

        // 检查是否超时
        diagnosisCueBll.handleDiagnosis(sourceId, sourceType);

        // 基础配置-
        BasicSettingsVo basic = basicSettingsBll.getBySourceUser(video.getSecUid(), WordsEnum.basicSettingsType.ANCHOR.getCode(), video.getUserId(), video.getTenantId());
        if (basic != null) {
            data.setBasicSettingsVo(basic);
        }

        CueWordsPageBo listBo = new CueWordsPageBo();
        listBo.setSourceId(sourceId);
        listBo.setSourceType(sourceType);
        listBo.setCueType(AiEnums.askType.DATA_DIAGNOSIS.getCode());
        listBo.setPage(1);
        listBo.setLimit(systemKvBll.getValueByKey("content_diagnosis_prompt_limit", 10));
        // 设置参数
        cueWordsApi.setTradeCueWordsParams(listBo);
        PageUtils<CueWordsListVo> pageUtils = cueWordsBll.pageCueWords(listBo);
        if (ObjectUtil.isEmpty(pageUtils) || ObjectUtil.isEmpty(pageUtils.getList())) {
            throw new BusinessException("没有配置提示词，请联系管理员");
        }
        List<CueWordsListVo> list = pageUtils.getList();
        if (ObjectUtil.isNotEmpty(list)) {
            data.setCueWordsId(list.get(0).getId());
            data.setNewCueWordsId(list.get(0).getId());
        }

        List<DiagnosisCueInfoVo> diagnosisCueList = diagnosisCueBll.listBySource(sourceId, AiEnums.diagnosisSourceType.VIDEO.getCode(), AiEnums.diagnosisType.DATA_DIAGNOSIS.getCode(), video.getTenantId(), video.getUserId());
        // 时间降序排序
        diagnosisCueList.sort(Comparator.comparing(DiagnosisCueInfoVo::getCreateDate).reversed());
        // 取提示词id
        // 条件：1、状态为进行中的优先，2、状态为完成的优先
        for (DiagnosisCueInfoVo e : diagnosisCueList) {
            if (ObjectUtil.equals(e.getQaStatus(), AiEnums.qaStatus.GENERATING.getCode()) || ObjectUtil.equals(e.getQaStatus(), AiEnums.qaStatus.SUCCESS.getCode())) {
                data.setCueWordsId(e.getCueWordsId());
                break;
            }
        }

        Map<Long, DiagnosisCueInfoVo> diagnosisCueMap = diagnosisCueList
                .stream().collect(Collectors.toMap(DiagnosisCueVo::getCueWordsId, Function.identity(), (e1, e2) -> e2));
        DiagnosisCueInfoVo diagnosisCueInfoVo = diagnosisCueMap.get(data.getCueWordsId());
        data.setQaStatus(diagnosisCueInfoVo == null ? null : diagnosisCueInfoVo.getQaStatus());
        data.setErrorContent(diagnosisCueInfoVo == null ? null : diagnosisCueInfoVo.getErrorContent());


        // 数据截图 - 数据看板
        Boolean hasDataScreenshot = ResultUtil.getResult(dataScreenshotBll.existVideoDataScreenshot(sourceType, sourceId));
        data.setHasDataScreenshot(ObjectUtil.defaultIfNull(hasDataScreenshot, false) ? 1 : 0);
        data.setHasBoard(0);
        if (sourceType == 0) {
            VideoDataViewingConfuseInfoVo confuseInfoVo = ResultUtil.getResult(videoDataViewingBll.infoByVideoIdPriorityParagraph(sourceId));
            if (confuseInfoVo != null && confuseInfoVo.getDataStatus() != null) {
                int val = confuseInfoVo.getDataStatus() == AnchorVideoEnums.videoDataViewingStatus.SUCCESS.getCode()
                        || confuseInfoVo.getDataStatus() == AnchorVideoEnums.videoDataViewingStatus.SALES_DATA_IS_BEING_SUMMARIZED.getCode() ? 1 : 0;
                data.setHasBoard(val);
            }
        }

        // 是否已经选择 数据截图 - 数据看板
        data.setSelectDataScreenshot(0);
        data.setSelectBoard(0);
        if (diagnosisCueInfoVo != null && ObjectUtil.equals(data.getHasDataScreenshot(), 1)) {
            data.setSelectDataScreenshot(ObjectUtil.defaultIfNull(diagnosisCueInfoVo.getSelectDataScreenshot(), 0));
        }
        if (diagnosisCueInfoVo != null && ObjectUtil.equals(data.getHasBoard(), 1)) {
            data.setSelectBoard(ObjectUtil.defaultIfNull(diagnosisCueInfoVo.getSelectBoard(), 0));
        }

        // 行业
        data.setTradeId(video.getTradeId());

        // 获取模型配置
        AiModelInfoVo modelConfig = null;
        DiagnosisModelInfoVo modelInfo = diagnosisModelBll.getBySource(sourceId, AiEnums.diagnosisSourceType.VIDEO.getCode(), video.getTenantId(), video.getUserId(), AiEnums.diagnosisType.DATA_DIAGNOSIS.getCode());
        if (modelInfo == null) {
            R<AiModelInfoVo> byCode = aiModelBll.getByCode(diagnosisCueBll.resolveDefaultModelCode(webVersion));
            modelConfig = ResultUtil.getResult(byCode);
        } else {
            modelConfig = aiModelBll.getDiagnosis(modelInfo.getModelId());
        }
        RRException.isNotEmpty(modelConfig, "ai模型配置获取失败");
        if (ObjectUtil.isNotEmpty(modelConfig)) {
            data.setModelId(modelConfig.getId());
            data.setModelName(modelConfig.getModelName());
        }
        return data;
    }

    @Override
    @Transactional
    public void updateDataDiagnosisConfig(DataDiagnosisConfigVo dataDiagnosisConfigVo) {

        if (!ObjectUtil.equals(dataDiagnosisConfigVo.getSourceType(), WordsEnum.sourceType.VIDEO.getCode())) {
            throw new BusinessException("只有视频有数据诊断功能");
        }

        UserCacheVo user = GlobalObject.getLocalUser();

        // 资产判断
        UserPropertyTypeInfoVo userPropertyByCode = userPropertyBll.getUserPropertyByCode(user.getId(), OrderEnums.commodityTypeCode.AI_TOKEN_NUM.getCode());
        if (userPropertyByCode == null || userPropertyByCode.getUseQuantity() >= userPropertyByCode.getTotalQuantity()) {
            throw new BusinessException("AI助手分析余量不足，请联系产品顾问进行套餐外购买");
        }

        AnchorVideoInfoVo video = ResultUtil.getResult(anchorVideoBll.infoByVideoId(dataDiagnosisConfigVo.getSourceId()));
        if (video == null) {
            throw new BusinessException("视频不存在");
        }

        // 修改基础设置
        if (ObjectUtil.isNotEmpty(dataDiagnosisConfigVo.getBasicSettingsVo())) {
            BasicSettingsBo basicSettingsBo = BeanUtil.copyProperties(dataDiagnosisConfigVo.getBasicSettingsVo(), BasicSettingsBo.class);
            basicSettingsBo.setSourceType(WordsEnum.basicSettingsType.VIDEO.getCode());
            basicSettingsBo.setSourceId(dataDiagnosisConfigVo.getSourceId());
            basicSettingsBo.setUserId(video.getUserId());
            basicSettingsBo.setTenantId(video.getTenantId());
            basicSettingsBll.updateAiPartialNew(basicSettingsBo);
        }

        // 处理数据诊断的问题
        CueWordsPageBo listBo = new CueWordsPageBo();
        listBo.setSourceId(dataDiagnosisConfigVo.getSourceId());
        listBo.setSourceType(dataDiagnosisConfigVo.getSourceType());
        listBo.setCueType(AiEnums.askType.DATA_DIAGNOSIS.getCode());
        listBo.setPage(1);
        listBo.setLimit(systemKvBll.getValueByKey("content_diagnosis_prompt_limit", 10));
        // 设置参数
        cueWordsApi.setTradeCueWordsParams(listBo);
        PageUtils<CueWordsListVo> pageUtils = cueWordsBll.pageCueWords(listBo);
        if (ObjectUtil.isEmpty(pageUtils) || ObjectUtil.isEmpty(pageUtils.getList())) {
            throw new BusinessException("没有配置提示词，请联系管理员");
        }
        Long cueWordsId = null;
        List<CueWordsListVo> list = pageUtils.getList();
        if (ObjectUtil.isNotEmpty(list)) {
            cueWordsId = list.get(0).getId();
        }

        final Long cueWordsIdFinal = cueWordsId;
        List<DiagnosisCueInfoVo> diagnosisCueList = diagnosisCueBll.listBySource(dataDiagnosisConfigVo.getSourceId(), AiEnums.diagnosisSourceType.VIDEO.getCode(), AiEnums.diagnosisType.DATA_DIAGNOSIS.getCode(), video.getTenantId(), video.getUserId());
        List<DiagnosisCueInfoVo> oldiagnosisList = diagnosisCueList.stream()
                .filter(e -> !ObjectUtil.equals(e.getCueWordsId(), cueWordsIdFinal))
                .sorted(Comparator.comparing(DiagnosisCueInfoVo::getCreateDate).reversed())
                .toList();
        if (ObjectUtil.isNotEmpty(oldiagnosisList)) {
            diagnosisCueBll.deleteBatch(oldiagnosisList.stream().map(DiagnosisCueInfoVo::getId).toList());
        }

        SaveDiagnosisCueBo cueBo = new SaveDiagnosisCueBo();
        cueBo.setSourceId(dataDiagnosisConfigVo.getSourceId());
        cueBo.setSourceType(1);
        cueBo.setSelectCueWordsIdsList(cueWordsId == null ? List.of() : List.of(cueWordsId));
        cueBo.setDiagnosisType(AiEnums.diagnosisType.DATA_DIAGNOSIS.getCode());
        cueBo.setModelId(dataDiagnosisConfigVo.getModelId());
        cueBo.setSelectDataScreenshot(dataDiagnosisConfigVo.getSelectDataScreenshot());
        cueBo.setSelectBoard(dataDiagnosisConfigVo.getSelectBoard());
        diagnosisCueBll.saveDiagnosisCue(cueBo);
    }

    /**
     * 设置重要弹幕字段
     *
     * @param videoId 视频id
     */
    private void startImportantBarrageAsync(String videoId) {

        int status = ImportantBarrageStatusEnum.IN_PROGRESS.getCode();
        String errorStr = "";
        try {
            // 获取视频
            AnchorVideoInfoVo video = ResultUtil.getResult(anchorVideoBll.infoByVideoId(videoId));
            if (video == null) {
                throw new BusinessException(StatusCode.ILLEGAL_ARGUMENT_EX.getCode(), "视频找不到");
            }

            // 获取弹幕列表
            List<BarrageBo> list = ResultUtil.getResult(tableStoreBll.listVideoBarrageByVideoId(video.getUserId(), video.getTenantId(), video.getBatchNumber(), videoId));
            if (list == null || list.isEmpty()) {
                throw new BusinessException(StatusCode.ILLEGAL_ARGUMENT_EX.getCode(), "当前视频没有弹幕数据，无法分析重要弹幕");
            }

            // 获取行业
            TradeInfoVo trade = ResultUtil.getResult(tradeBll.info(video.getTradeId()));
            if (trade == null) {
                log.error("重要弹幕的行业获取失败 行业ID：{}", video.getTradeId());
                throw new BusinessException(StatusCode.ILLEGAL_ARGUMENT_EX.getCode(), "行业获取失败");
            }

            // 获取提示词
            CueWordsInfoVo data = ResultUtil.getResult(cueWordsBll.importantCueWordsByTradeId(video.getTradeId()));
            if (data == null) {
                log.error("行业ID：{}, 行业名称：{}，没有获取到重要弹幕提示词", trade.getId(), trade.getName());
                throw new BusinessException(StatusCode.ILLEGAL_ARGUMENT_EX.getCode(), "重要弹幕提示词没有");
            }

            // 处理占位符
            String problem = "提问：" + CommonUtils.placeholderHandle(data.getProblem(), Map.of("trade", trade.getName()));

            // 重要弹幕提示词的要求
            problem += "\n" + importantAdditionalStr();

            // 获取ai模型配置
            SystemKvInfoVo importantBarrageAiCode = ResultUtil.getResult(systemKvBll.getByKey("important_barrage_ai_code"));
            if (importantBarrageAiCode == null) {
                throw new BusinessException(StatusCode.ILLEGAL_ARGUMENT_EX.getCode(), "重要弹幕没有设置ai分析的模型 key = important_barrage_ai_code");
            }
            AiModelInfoVo aiModel = ResultUtil.getResult(aiModelBll.getByCode(importantBarrageAiCode.getKvValue()));
            if (aiModel == null) {
                log.error("没有找到对应的ai模型配置，code={}", importantBarrageAiCode.getKvValue());
                throw new BusinessException(StatusCode.ILLEGAL_ARGUMENT_EX.getCode(), "查询aiModel失败，code = " + importantBarrageAiCode.getKvValue());
            }
            AiModelBo modelConfig = BeanUtil.copyProperties(aiModel, AiModelBo.class);

            // 获取ai模型
            AiModel aiModel1 = ModelFactoryUtils.getAiModel(aiModel.getResourceType());
            if (aiModel1 == null) {
                log.error("在工厂模式中获取AI模型失败，resourceType = {}", aiModel.getResourceType());
                throw new BusinessException(StatusCode.ILLEGAL_ARGUMENT_EX.getCode(), "在工厂模式中获取AI模型失败，resourceType = " + aiModel.getResourceType());
            }

            AiMessageBo params = new AiMessageBo();
            params.setSystem(List.of(Map.of("text", "你是一个顶尖抖音弹幕分析专家")));

            // 获取弹幕提示词，有多条问多次
            List<String> problemList = separateBarrageList(problem, modelConfig.getWordsNum(), list);

            if (ObjectUtil.isEmpty(problemList)) {
                log.error("组装弹幕提示词错误， problem = {}, modelConfig.getWordsNum() = {}", problem, modelConfig.getWordsNum());
                throw new BusinessException(StatusCode.ILLEGAL_ARGUMENT_EX.getCode(), "组装弹幕提示词错误");
            }

            // 重要弹幕全部设置为0
            for (BarrageBo item : list) {
                item.setImportant(0L);
            }

            for (String problemStr : problemList) {
                params.setUser(List.of(Map.of("text", problemStr)));
                AiReturnDataVo aiReturnDataVo = aiModel1.chatCompletion(modelConfig, params, 0L);
                log.debug("获取的重要弹幕videoId = {} ,回答：{}", videoId, aiReturnDataVo.getContent());

                // 记录aiToken消耗量
                AiTokenUseRecordBo aiTokenUseRecordBo = AiTokenUseRecordBo.builder(aiReturnDataVo, modelConfig.getModelName());
                aiTokenUseRecordBo.setTenantId(video.getTenantId());
                aiTokenUseRecordBo.setUserId(video.getUserId());
                aiTokenUseRecordBo.setModelName(modelConfig.getModelName());
                aiTokenUseRecordBo.setUseSourceType(AiEnums.useSourceType.VIDEO.getCode());
                aiTokenUseRecordBo.setUseSourceId(video.getVideoId());
                aiTokenUseRecordBo.setAssistantType(AiEnums.askType.IMPORTANT_SCREENSHOT.getCode());
                aiTokenUseRecordBll.save(aiTokenUseRecordBo);

                // 设置重要弹幕的值
                setImportantValue(aiReturnDataVo.getContent(), list);
            }
            // 保存到库中
            List<BarrageBo> newList = list.stream().filter(item -> ObjectUtil.equals(item.getImportant(), 1L)).toList();
            if (!newList.isEmpty()) {
                tableStoreBll.updateBarrageBatch(newList);
            }
            status = ImportantBarrageStatusEnum.SUCCESS.getCode();
        } catch (BusinessException ex) {
            log.info("获取重要弹幕报错：{}", ex.getMessage());
            status = ImportantBarrageStatusEnum.ACQUISITION_FAILED.getCode();
            errorStr = ex.getMessage();
        } catch (Exception ex) {
            log.info("获取重要弹幕报错：{}", ex.getMessage());
            errorStr = ex.getMessage();
        } finally {
            // 设置重要弹幕的状态
            AnchorVideoDetailBo update = new AnchorVideoDetailBo();
            update.setVideoId(videoId);
            update.setImportantBarrageStatus(status);
            update.setImportantBarrageError(errorStr);
            anchorVideoDetailBll.updateByVideoId(update);
        }
    }

    /**
     * 设置重要弹幕字段的值
     *
     * @param value ai返回的回答
     * @param list  所有的弹幕数据
     */
    private void setImportantValue(String value, List<BarrageBo> list) {
        if (ObjectUtil.isNotEmpty(value)) {
            String[] split = value.split(",");
            if (ObjectUtil.isNotEmpty(split)) {
                for (String v : split) {
                    if (ObjectUtil.isNotEmpty(v)) {
                        Integer i = NumberUtil.parseInt(v.trim(), -1);
                        if (i != -1 && i < list.size()) {
                            list.get(i).setImportant(1L);
                        }
                    }
                }
            }
        }
    }

    /**
     * 重要弹幕提示词的要求
     *
     * @return 重要弹幕提示词
     */
    public String importantAdditionalStr() {
        List<String> list = new ArrayList<>();
        list.add("筛选出重要序号");
        list.add("只要序号，不要有额外的说明");
        list.add("序号和序号之间有逗号分隔");
        StringBuilder str = new StringBuilder("要求：");
        for (int i = 0; i < list.size(); i++) {
            str.append(StrUtil.format("{}.{}。", i + 1, list.get(i)));
        }
        return str.toString();
    }

    /**
     * 分割弹幕助手的提示词，以便于分多次去ai提问
     *
     * @param problem          开启提示词
     * @param maxProblemLength 提示词最大的字数
     * @param list             弹幕列表
     * @return 多个要问的提示词
     */
    private List<String> separateBarrageList(String problem, int maxProblemLength, List<BarrageBo> list) {
        ArrayList<String> result = new ArrayList<>();

        problem += "\n弹幕数据:\n";

        if (maxProblemLength == 0) {
            maxProblemLength = -1;
        }

        int maxOldLength = maxProblemLength == -1 ? -1 : maxProblemLength - problem.length();
        int surplusLength = maxOldLength;
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            BarrageBo barrageBo = list.get(i);
            String format = StrUtil.format("{}:{}\n", i, barrageBo.getContent());

            if (maxOldLength != -1) {
                int temp = surplusLength - format.length();
                if (temp > 0) {
                    surplusLength = temp;
                } else {
                    result.add(problem + str);
                    str = new StringBuilder();
                    surplusLength = maxOldLength - format.length();
                }
            }
            str.append(format);
            if (i == list.size() - 1 && ObjectUtil.isNotEmpty(str.toString())) {
                result.add(problem + str);
            }
        }
        return result;
    }

    /**
     * 新版自然/优化原文生成入口（由 XXL-Job 定时器调用）。
     *
     * <h3>整体流程</h3>
     * <ol>
     *   <li><b>查询待处理数据</b> — 分别查视频表（{@code tb_anchor_video_detail}）和文件表（{@code tb_upload_file_detail}），
     *       条件：content_status=1 AND set_job=0 AND source_type=0（服务端来源）</li>
     *   <li><b>合并为统一任务列表</b> — 一条 DB 记录如果 both=1（自然和优化两个 status 都为 1），拆成两条独立任务</li>
     *   <li><b>逐条处理</b> — 乐观锁标记拾取 → 查 MongoDB 已有分段 → 无则 OSS 下载 + AI 分段 → 提交线程池异步执行</li>
     * </ol>
     *
     * <h3>异常兜底</h3>
     * 单条任务处理异常时，外层 catch 会重置该条记录的 set_job=0 + status=FAILED，
     * 确保记录不会卡在"已拾取但未完成"的状态。
     *
     * <h3>快速返回</h3>
     * 本方法只负责"拾取 + 提交线程池"，不等待 AI 调用结果。
     * 最终的生成 status 由 {@link com.jiuyu.replay.words.bll.VideoContentGenerator} 在线程池内自行更新。
     *
     * @param limit 每次最多查询条数（视频和文件各查 limit 条），为 null 时默认 10
     */
    @Override
    public void generateVideoContentNew(Integer limit) {
        int queryLimit = ObjectUtil.defaultIfNull(limit, 10);

        // 步骤1：分别查询视频和文件待处理数据
        List<AnchorVideoDetailVo> videoList = anchorVideoDetailProducer.selectByQuery(queryLimit);
        List<UploadFileDetailVo> fileList = uploadFileDetailProducer.selectFileDetailData(queryLimit);

        // 步骤2：合并为统一任务列表（处理 both=1 拆分）
        List<GenerateTaskVo> tasks = mergeTasks(videoList, fileList);
        if (tasks.isEmpty()) {
            log.info("[生成任务] 无待处理数据");
            return;
        }
        log.info("[生成任务] 扫描到 {} 条待处理任务", tasks.size());

        // 步骤3：逐条处理
        for (GenerateTaskVo task : tasks) {
            try {
                processTask(task);
            } catch (Exception e) {
                log.error("[生成任务] 处理异常, sourceId={}, type={}", task.getSourceId(), task.getType(), e);
                // 异常后重置 set_job=0，避免记录卡在"已拾取"状态导致永远不会被重新处理
                updateTaskStatus(task, WordsEnum.contentStatus.FAILED.getCode(), true);
            }
        }
    }

    /**
     * 合并视频和文件任务列表为统一的生成任务。
     *
     * <h3>both=1 处理</h3>
     * 一条 DB 记录如果 nature_content_status=1 且 optimize_content_status=1，会拆成两条独立任务：
     * 一条 type=NATURE（自然原文），一条 type=OPTIMIZE（优化原文）。两条任务各自独立拾取和执行。
     *
     * @param videoList 视频待处理列表
     * @param fileList  文件待处理列表
     * @return 统一任务列表，按视频优先、文件在后的顺序
     */
    private List<GenerateTaskVo> mergeTasks(List<AnchorVideoDetailVo> videoList,
                                            List<UploadFileDetailVo> fileList) {
        List<GenerateTaskVo> tasks = new ArrayList<>();
        for (AnchorVideoDetailVo vo : videoList) {
            if (ObjectUtil.equals(vo.getNatureContentStatus(), 1) && ObjectUtil.equals(vo.getNatSetJob(), 0)) {
                tasks.add(buildTask(vo.getVideoId(), WordsEnum.sourceType.VIDEO.getCode(),
                        WordsEnum.contentType.NATURE.getCode(), vo.getUserId(), vo.getTenantId()));
            }
            if (ObjectUtil.equals(vo.getOptimizeContentStatus(), 1) && ObjectUtil.equals(vo.getOptSetJob(), 0)) {
                tasks.add(buildTask(vo.getVideoId(), WordsEnum.sourceType.VIDEO.getCode(),
                        WordsEnum.contentType.OPTIMIZE.getCode(), vo.getUserId(), vo.getTenantId()));
            }
        }
        for (UploadFileDetailVo vo : fileList) {
            if (ObjectUtil.equals(vo.getNatureContentStatus(), 1) && ObjectUtil.equals(vo.getNatSetJob(), 0)) {
                tasks.add(buildTask(vo.getFileId(), WordsEnum.sourceType.FILE.getCode(),
                        WordsEnum.contentType.NATURE.getCode(), vo.getUserId(), vo.getTenantId()));
            }
            if (ObjectUtil.equals(vo.getOptimizeContentStatus(), 1) && ObjectUtil.equals(vo.getOptSetJob(), 0)) {
                tasks.add(buildTask(vo.getFileId(), WordsEnum.sourceType.FILE.getCode(),
                        WordsEnum.contentType.OPTIMIZE.getCode(), vo.getUserId(), vo.getTenantId()));
            }
        }
        return tasks;
    }

    /**
     * 构造单个生成任务 VO。
     *
     * @param sourceId   来源 id（videoId 或 fileId）
     * @param sourceType 来源类型：0=视频, 1=文件
     * @param type       内容类型：1=自然原文, 2=优化原文
     * @param userId     用户 id
     * @param tenantId   租户 id
     * @return 生成任务 VO
     */
    private GenerateTaskVo buildTask(String sourceId, int sourceType, int type, Long userId, Long tenantId) {
        GenerateTaskVo task = new GenerateTaskVo();
        task.setSourceId(sourceId);
        task.setSourceType(sourceType);
        task.setType(type);
        task.setUserId(userId);
        task.setTenantId(tenantId);
        return task;
    }

    /**
     * 处理单条生成任务：标记拾取 → 获取分段 → 提交线程池。
     *
     * <h3>流程</h3>
     * <ol>
     *   <li><b>乐观锁标记拾取</b> — UPDATE ... WHERE set_job=0 AND content_status=1，
     *       affected_rows=0 说明被其他实例抢先拾取，跳过</li>
     *   <li><b>查 MongoDB 已有分段</b> — 如果有缓存的分段（generateStatus=0），直接复用，
     *       跳过 OSS 下载和 AI 分段步骤</li>
     *   <li><b>无分段则创建</b> — OSS 下载分析数据 → AI 分段 → 保存到 MongoDB</li>
     *   <li><b>提交线程池</b> — 异步执行，立即返回</li>
     * </ol>
     *
     * @param task 生成任务
     */
    private void processTask(GenerateTaskVo task) {
        // 步骤1：乐观锁标记拾取（多实例互斥）
        boolean picked;
        if (task.getSourceType() == WordsEnum.sourceType.VIDEO.getCode()) {
            picked = anchorVideoDetailProducer.markPickedUp(task.getSourceId(), task.getType());
        } else {
            picked = uploadFileDetailProducer.markPickedUp(task.getSourceId(), task.getType());
        }
        if (!picked) {
            log.info("[生成任务] 被其他实例拾取, sourceId={}, type={}", task.getSourceId(), task.getType());
            return;
        }

        // 步骤2：先查 MongoDB 是否已有分段（优化：避免重复下载 OSS）
        List<VideoContentVo> existingSegments = videoContentProducer.listBySourceIdAndStatus(
                task.getSourceId(), task.getType(), task.getUserId(), task.getTenantId(),
                WordsEnum.contentGenerateStatus.NO_GENERATED.getCode());

        ToGeneratedVo batch = new ToGeneratedVo();
        batch.setSourceId(task.getSourceId());
        batch.setSourceType(task.getSourceType());
        batch.setType(task.getType());

        if (ObjectUtil.isNotEmpty(existingSegments)) {
            // 有未完成分段，直接复用
            batch.setVideoContentList(existingSegments);
        } else {
            // 无未完成分段时，检查是否已全部生成完毕
            // 场景：上次生成的最终状态更新失败，分段全部 status=1 但 MySQL 仍为待处理
            List<VideoContentVo> completedSegments = videoContentProducer.listBySourceIdAndStatus(
                    task.getSourceId(), task.getType(), task.getUserId(), task.getTenantId(),
                    WordsEnum.contentGenerateStatus.SUCCESS.getCode());
            if (ObjectUtil.isNotEmpty(completedSegments)) {
                // 分段已全部生成完毕，直接更新 MySQL 状态为成功，跳过重复生成
                log.info("[生成任务] 分段已全部生成, 直接更新状态, sourceId={}, type={}, count={}",
                        task.getSourceId(), task.getType(), completedSegments.size());
                updateTaskStatus(task, WordsEnum.contentStatus.SUCCESS.getCode(), false);
                return;
            }
            // 无任何分段（未完成也没有已完成的），创建分段（OSS下载 + AI分段 + 保存MongoDB）
            List<VideoContentVo> segments = createSegments(task);
            if (segments == null) {
                return; // createSegments 内部已处理状态更新
            }
            batch.setVideoContentList(segments);
        }

        // 步骤4：提交线程池异步执行（不等结果）
        videoContentGenerator.executeBatch(batch);
        log.info("[生成任务] 已提交批次, sourceId={}, type={}, segments={}",
                task.getSourceId(), task.getType(), batch.getVideoContentList().size());
    }

    /**
     * 创建分段数据：OSS 下载分析数据 → 获取行业 → 获取提示词 → AI 分段 → 保存到 MongoDB。
     *
     * <h3>失败处理策略</h3>
     * <ul>
     *   <li><b>可重试错误（resetJob=true）</b>：分析数据为空、段落为空、AI 分段失败 —
     *       重置 set_job=0，下次定时器可重新拾取</li>
     *   <li><b>配置错误（resetJob=false）</b>：行业不存在、未配置提示词 —
     *       不重置 set_job，避免反复无效重试（需人工修复配置后手动重置）</li>
     * </ul>
     *
     * @param task 生成任务
     * @return 分段列表（已保存到 MongoDB 含 _id），失败时返回 null
     */
    private List<VideoContentVo> createSegments(GenerateTaskVo task) {
        // 获取段落数据（OSS下载+解压）
        R<AnalysisResultAllVo> result = sensitiveWordsBll.getParagraphContent(
                task.getSourceType(), task.getSourceId(), task.getType());
        AnalysisResultAllVo analysisData = result.getData();
        if (analysisData == null || ObjectUtil.isEmpty(analysisData.getAnalysisList())) {
            log.warn("[生成任务] 分析数据为空, sourceId={}, type={}", task.getSourceId(), task.getType());
            updateTaskStatus(task, WordsEnum.contentStatus.FAILED.getCode(), true);
            return null;
        }

        List<String> contentList = analysisData.getAnalysisList().stream()
                .sorted(Comparator.comparing(OnlineAnalysisItemVo::getParagraph))
                .map(item -> AnchorVideoDetailBll.formatParagraphFromDataJson(
                        item.getDataJson(), item.getParagraph()))
                .filter(ObjectUtil::isNotEmpty)
                .toList();

        if (contentList.isEmpty()) {
            log.warn("[生成任务] 段落内容为空, sourceId={}", task.getSourceId());
            updateTaskStatus(task, WordsEnum.contentStatus.FAILED.getCode(), true);
            return null;
        }

        Long tradeId = getTradeId(task);
        TradeInfoVo trade = tradeProducer.info(tradeId);
        if (trade == null) {
            log.warn("[生成任务] 行业不存在, tradeId={}", tradeId);
            // 配置问题，不重置 set_job 避免死循环
            updateTaskStatus(task, WordsEnum.contentStatus.FAILED.getCode(), false);
            return null;
        }

        List<CueWordsListVo> cueWords = anchorVideoDetailBll.getCueWordsByTrade(
                task.getSourceId(), tradeId, task.getType(), task.getSourceType());
        if (ObjectUtil.isEmpty(cueWords)) {
            log.warn("[生成任务] 未配置提示词, sourceId={}, tradeId={}", task.getSourceId(), tradeId);
            // 配置问题，不重置 set_job 避免死循环
            updateTaskStatus(task, WordsEnum.contentStatus.FAILED.getCode(), false);
            return null;
        }
        CueWordsListVo cueWord = cueWords.get(0);

        List<String> contentSection = aiFeign.getVideoContentSection(
                contentList, cueWord.getProblem(), Map.of("trade", trade.getName()));
        if (ObjectUtil.isEmpty(contentSection)) {
            log.warn("[生成任务] AI分段失败, sourceId={}", task.getSourceId());
            updateTaskStatus(task, WordsEnum.contentStatus.FAILED.getCode(), true);
            return null;
        }

        List<VideoContentVo> videoContentList = new ArrayList<>();
        for (int i = 0; i < contentSection.size(); i++) {
            VideoContentVo vo = new VideoContentVo();
            vo.setSourceId(task.getSourceId());
            vo.setSourceType(task.getSourceType());
            vo.setType(task.getType());
            vo.setCueWord(contentSection.get(i));
            vo.setGenerateStatus(0);
            vo.setUserId(task.getUserId());
            vo.setTenantId(task.getTenantId());
            vo.setParagraph(i);
            videoContentList.add(vo);
        }

        return videoContentProducer.saveAll(videoContentList);
    }

    /**
     * 从视频/文件信息中获取行业 id，取不到时返回默认行业（{@link CommonEnum#INIT_TRADE_ID}）。
     *
     * @param task 生成任务
     * @return 行业 id
     */
    private Long getTradeId(GenerateTaskVo task) {
        if (task.getSourceType() == WordsEnum.sourceType.VIDEO.getCode()) {
            AnchorVideoInfoVo video = anchorVideoProducer.getByVideoId(task.getSourceId());
            if (video != null && video.getTradeId() != null) {
                return video.getTradeId();
            }
        } else if (task.getSourceType() == WordsEnum.sourceType.FILE.getCode()) {
            UploadFileInfoVo file = uploadFileProducer.getByFileId(task.getSourceId());
            if (file != null && file.getTradeId() != null) {
                return file.getTradeId();
            }
        }
        return CommonEnum.INIT_TRADE_ID.getCodeLong();
    }

    /**
     * 更新任务最终状态。根据 sourceType 路由到对应的 Producer 执行 UPDATE。
     *
     * @param task     生成任务
     * @param status   目标状态：2=SUCCESS, 3=FAILED
     * @param resetJob 是否同时重置 set_job=0（true=允许定时器重试, false=保持已拾取状态）
     */
    private void updateTaskStatus(GenerateTaskVo task, int status, boolean resetJob) {
        if (task.getSourceType() == WordsEnum.sourceType.VIDEO.getCode()) {
            anchorVideoDetailProducer.updateContentStatusBySourceId(
                    task.getSourceId(), task.getType(), status, resetJob);
        } else {
            uploadFileDetailProducer.updateContentStatusBySourceId(
                    task.getSourceId(), task.getType(), status, resetJob);
        }
    }

    @Override
    public R<Integer> countBySecUid(String secUid) {
        UserCacheVo user = GlobalObject.getLocalUser();
        return anchorVideoBll.countBySecUid(secUid, user.getId(), user.getActiveTenantId());
    }
}
