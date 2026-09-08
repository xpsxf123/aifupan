package com.jiuyu.replay.api.logic.words.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.jiuyu.replay.ai.bll.DiagnosisCueBll;
import com.jiuyu.replay.ai.bll.DiagnosisModelBll;
import com.jiuyu.replay.api.logic.words.AnchorUrlLogic;
import com.jiuyu.replay.common.bll.SystemKvBll;
import com.jiuyu.replay.common.constant.AiEnums;
import com.jiuyu.replay.common.constant.WordsEnum;
import com.jiuyu.replay.common.lock.DistributedLock;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.common.utils.TreePrinterWithMap;
import com.jiuyu.replay.common.vo.SystemKvInfoVo;
import com.jiuyu.replay.generic.bo.ai.AiMessageBo;
import com.jiuyu.replay.generic.bo.ai.AiModelBo;
import com.jiuyu.replay.generic.bo.ai.DiagnosisModelBo;
import com.jiuyu.replay.generic.bo.ai.SaveDiagnosisCueBo;
import com.jiuyu.replay.generic.bo.third.AiTokenUseRecordBo;
import com.jiuyu.replay.generic.enums.words.AnchorPlatformEnum;
import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.utils.ResultUtil;
import com.jiuyu.replay.generic.vo.ai.AiModelInfoVo;
import com.jiuyu.replay.generic.vo.ai.AiReturnDataVo;
import com.jiuyu.replay.generic.vo.ai.DiagnosisCueInfoVo;
import com.jiuyu.replay.generic.vo.ai.DiagnosisModelInfoVo;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.generic.vo.order.UserPropertyTypeInfoVo;
import com.jiuyu.replay.generic.vo.words.*;
import com.jiuyu.replay.order.bean.impl.UserPropertyImpl;
import com.jiuyu.replay.order.bll.AiTokenUseRecordBll;
import com.jiuyu.replay.order.bll.UserPropertyBll;
import com.jiuyu.replay.order.bo.AssetsMinusOrPlusBo;
import com.jiuyu.replay.order.bo.UserPropertyDetailsBo;
import com.jiuyu.replay.power.bll.UserBll;
import com.jiuyu.replay.power.bo.UserListBo;
import com.jiuyu.replay.power.utils.GlobalObject;
import com.jiuyu.replay.power.vo.UserCacheVo;
import com.jiuyu.replay.power.vo.UserInfoVo;
import com.jiuyu.replay.power.vo.UserListVo;
import com.jiuyu.replay.power.vo.UserVo;
import com.jiuyu.replay.ai.factory.ModelFactoryUtils;
import com.jiuyu.replay.ai.model.AiModel;
import com.jiuyu.replay.ai.model.impl.DoubaoAiModelImpl;
import com.jiuyu.replay.third.bll.AiModelBll;
import com.jiuyu.replay.third.bll.MsgBll;
import com.jiuyu.replay.generic.feign.third.GovernanceLiveRoomService;
import com.jiuyu.replay.generic.bo.governance.GovernanceAddAnchorBo;
import com.jiuyu.replay.words.bll.*;
import com.jiuyu.replay.words.bo.*;
import com.jiuyu.replay.words.bo.anchor.AddOrUpdateAnchorBo;
import com.jiuyu.replay.words.bo.anchor.ClientAnchorListBo;
import com.jiuyu.replay.words.bo.anchor.TopAnchorBo;
import com.jiuyu.replay.words.constant.Constant;
import com.jiuyu.replay.words.entity.AnchorUrlDetailsEntity;
import com.jiuyu.replay.words.entity.AnchorUrlWhiteEntity;
import com.jiuyu.replay.words.producer.AnchorUrlProducer;
import com.jiuyu.replay.words.producer.BasicSettingsProducer;
import com.jiuyu.replay.words.vo.AnchorClientVo;
import com.jiuyu.replay.words.vo.AnchorVideoSimpleVo;
import com.jiuyu.replay.words.vo.anchor.AnchorRecordListVo;
import com.jiuyu.replay.words.vo.anchor.AnchorYesterdayRecordVo;
import com.jiuyu.replay.words.vo.anchor.OpenMonitoringPositionVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
 * 主播url
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-08-10 16:47:37
 */
@Service
@Slf4j
public class AnchorUrlLogicImpl implements AnchorUrlLogic {

    @Resource
    private AnchorUrlBll anchorUrlBll;

    @Resource
    private UserBll userBll;

    @Resource
    AnchorUrlWhiteBll anchorUrlWhiteBll;
    @Resource
    private UserPropertyBll userPropertyBll;
    @Resource
    private AnchorUrlUserBll anchorUrlUserBll;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private TradeBll tradeBll;
    @Resource
    private AnchorVideoBll anchorVideoBll;
    @Resource
    private MsgBll msgBll;
    @Resource
    private UserFeign userApi;
    @Resource
    private DiagnosisCueBll diagnosisCueBll;
    @Autowired
    private DiagnosisModelBll diagnosisModelBll;
    @Resource
    private AnchorUrlProducer anchorUrlProducer;
    @Autowired
    private DistributedLock distributedLock;
    @Autowired
    private BasicSettingsProducer basicSettingsProducer;
    @Autowired
    private AnchorCruxWordsBll anchorCruxWordsBll;
    @Autowired
    private SensitiveWordsBll sensitiveWordsBll;
    @Autowired
    private SystemKvBll systemKvBll;
    @Autowired
    private AiModelBll aiModelBll;
    @Autowired
    private AiTokenUseRecordBll aiTokenUseRecordBll;
    @Autowired
    private GovernanceLiveRoomService governanceLiveRoomService;


    @Override
    public R<String> sendSwitchAnchorMsg(final String anchorUrlName, final Integer type) {
        // 获取当前用户
        final UserCacheVo user = GlobalObject.getLocalUser();
        if (user == null) {
            log.warn("当前用户未登录");
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "当前用户未登录");
        }

        if (ObjectUtil.isEmpty(anchorUrlName)) {
            log.warn("发送上下播主播消息-主播名称为空，不发送");
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "主播名称为空");
        }

        // 获取短信余量
        final UserPropertyTypeInfoVo property = userPropertyBll.getUserPropertyByCode(user.getId(), "smsMessageNum");
        if (property == null || property.getTotalQuantity() - property.getUseQuantity() <= 0) {
            log.info("用户短信余量不足，userId={}", user.getId());
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "短信余量不足");
        }

        // 发送短信
        boolean sendFlag = msgBll.sendSwitchAnchorMsg(user.getPhone(), type, anchorUrlName, "主播下播");
        if (!sendFlag) {
            log.error("主播上下播消息发送失败，phone={}，anchorUrlName={}", user.getPhone(), anchorUrlName);
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "短信发送失败");
        }

        // 扣减短信条数
        try {
            final AssetsMinusOrPlusBo assets = new AssetsMinusOrPlusBo();
            assets.setUserId(user.getId());
            assets.setUserName(user.getNickName());
            assets.setCode("smsMessageNum");
            assets.setNum(-1L);
            UserPropertyImpl.use(assets);
        } catch (Exception e) {
            log.error("扣减短信条数失败，userId={}，error={}", user.getId(), e.getMessage(), e);
        }

        return R.ok();
    }

    @Override
    public R<PageUtils<AnchorUrlListVo>> queryPage(AnchorUrlListBo anchorUrlListBo) {

        return anchorUrlBll.queryPage(anchorUrlListBo);
    }

    @Override
    public R<AnchorUrlInfoVo> info(Long id) {

        return anchorUrlBll.info(id);
    }

    @Override
    public R<String> save(AnchorUrlBo anchorUrlBo) {

        R<String> save = anchorUrlBll.save(anchorUrlBo);
        UserCacheVo user = GlobalObject.getLocalUser();
        // 统计主播数量
        this.updateUserAnchorNum(user.getId(), user.getActiveTenantId());
        return save;
    }

    @Override
    public R<String> update(AnchorUrlBo anchorUrlBo) {

        return anchorUrlBll.update(anchorUrlBo);
    }

    @Override
    public R<String> delete(Long id) {

        return anchorUrlBll.delete(id);
    }

    @Override
    public R<AnchorUrlInfoVo> infoByCondition(String secUid, String liveUrl, String homeUrl) {

        return anchorUrlBll.infoByCondition(secUid, liveUrl, homeUrl);
    }

    @Override
    public R<List<AnchorUrlInfoVo>> listBySecUids(List<String> secUidList) {

        return anchorUrlBll.listByUniques(secUidList);
    }

    @Override
    public R<String> saveBatch(List<AnchorUrlBo> anchorUrlBos) {

        return anchorUrlBll.saveBatch(anchorUrlBos);
    }

    @Override
    public R<List<AnchorUrlInfoVo>> listLiveBySecUids(List<String> secUidList) {

        return anchorUrlBll.listLiveBySecUids(secUidList);
    }

    @Override
    public R<List<AnchorClientVo>> listByUserToken() {


        UserCacheVo user = GlobalObject.getLocalUser();
        // 初始化已使用主播数量
        UserPropertyDetailsBo userPropertyDetailsBo = new UserPropertyDetailsBo();
        userPropertyDetailsBo.setUserId(user.getId());

        R<List<AnchorClientVo>> listR = anchorUrlBll.listByUserId(user.getId());

        UserCacheVo localUser = GlobalObject.getLocalUser();
        if (localUser != null){
            R<Long> longR = anchorVideoBll.statisticsStoreByTenantId(localUser.getActiveTenantId());
            if (longR.getCode() == 0){
                Long parentId = ResultUtil.getResult(userApi.getUserParentId(localUser.getId()));
                userPropertyBll.updateByPropertyNum(parentId != null && parentId != 0L ? parentId : user.getId(), "storageNum", longR.getData());
            }
        }

        List<AnchorClientVo> urlVos = listR.getData();
        if(urlVos != null && !urlVos.isEmpty()) {

            // 添加默认行业
            R<List<TradeListVo>> listedByIds = tradeBll.listByIds(urlVos.stream().map(AnchorClientVo::getTradeId).distinct().toList());
            if (listedByIds.getCode() == 0 && ObjectUtil.isNotEmpty(listedByIds.getData())){
                Map<Long, TradeListVo> map = listedByIds.getData().stream()
                        .collect(Collectors.toMap(TradeListVo::getId, Function.identity(), (o1, o2) -> o1));
                urlVos.forEach(item -> {
                    TradeListVo tradeListVo = map.get(item.getTradeId());
                    if (ObjectUtil.isEmpty(tradeListVo)){
                        item.setTradeId(1L);
                    }
                });
            }
            // 统计主播数量
//            this.updateUserAnchorNum(user.getId(), user.getActiveTenantId());

            // 获取用户可添加主播的数量
//            Long total = 0L;
//            List<UserPropertyTypeInfoVo> userProperty = userPropertyBll.getUserProperty(user.getId());
//            if (ObjectUtil.isNotEmpty(userProperty)) {
//                UserPropertyTypeInfoVo typeInfoVo = userProperty.stream().filter(item -> item.getCommodityTypeCode().equals("anchorNum")).findFirst().orElse(null);
//                if (ObjectUtil.isNotEmpty(typeInfoVo)) {
//                    total = typeInfoVo.getTotalQuantity();
//                }
//            }

//            long addAnchorNum = 0; // 已使用的添加主播数
//            for (int i = 0; i < urlVos.size(); i++) {
//                AnchorClientVo item = urlVos.get(i);
//
//                if(item.getIsRemoveRecord() == 0 && addAnchorNum < total) {
//                    item.setIsRemoveRecord(0);
//                    addAnchorNum++;
//                }else {
//                    item.setIsRemoveRecord(1);
//                }
//
//            }
//            userPropertyBll.updateByAnchorNum(user.getId(), "anchorNum", addAnchorNum);

//            // 删除已删除的主播
//            List<String> secUids = urlVos.stream().filter(item -> item.getIsRemoveRecord() == 1).map(AnchorClientVo::getSecUid).toList();
//            if (ObjectUtil.isNotEmpty(secUids)){
//                anchorUrlBll.updateIsRemoveRecord(secUids, user.getId(), 1);
//            }

            listR.setData(urlVos);
            return listR;
        }

        listR.setData(new LinkedList<>());
        return listR;
    }

    /**
     * 新增绑定用户与主播
     * @param secUidList
     * @return
     */
    @Override
    public R<String> savs(List<AnchorUrlUserBo> secUidList) {
        UserCacheVo user = GlobalObject.getLocalUser();
        if(secUidList == null || secUidList.size() == 0) {
            return  R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(),"绑定失败，用户为null或者secUidList为null");
        }
         anchorUrlBll.save(secUidList, user.getId());
        // 更新用户已绑定主播数量
        this.updateUserAnchorNum(user.getId(), user.getActiveTenantId());
        return R.ok();
    }

    /**
     * 删除用户与主播绑定关系
     * @param secUidList
     * @return
     */
    @Override
    public R<List<AnchorUrlInfoVo>> deletBySecUid(String secUidList) {
        UserCacheVo user = GlobalObject.getLocalUser();
        anchorUrlBll.deletBySecUid(secUidList,user.getId());
        return null;
    }

    @Override
    public R<PageUtils<AnchorUrlVo>> seletByUserId(AnchorUrlPegBo anchorUrlPegBo) {
        //根据用户名称模糊查询
        if(anchorUrlPegBo.getUserName()!=null) {
            UserListBo userListBo = new UserListBo();
            userListBo.setKeyword(anchorUrlPegBo.getUserName());
            List<Long> longs = userBll.selectByNameOrNick(userListBo);
            if(longs==null||longs.size()==0){
                return null;
            }
            anchorUrlPegBo.setUserIds(longs);
        }
        if(anchorUrlPegBo.getUserId()!=null) {
            List<Long> longs = new ArrayList<>();
            longs.add(anchorUrlPegBo.getUserId());
            anchorUrlPegBo.setUserIds(longs);
        }

        R<PageUtils<AnchorUrlVo>> pageUtilsR = anchorUrlBll.seletByUserId(anchorUrlPegBo);

        if( pageUtilsR.getData()==null){
            return pageUtilsR;
        }
        List<AnchorUrlVo> list = pageUtilsR.getData().getList();
        if(list!=null){
            for( AnchorUrlVo  anchorUrlVo :list){
                R<UserInfoVo> info = userBll.info(anchorUrlVo.getUserId(), false);
                anchorUrlVo.setUserName(info.getData().getUsername());
                anchorUrlVo.setNickName(info.getData().getNickName());
            }
        }

        return pageUtilsR;
    }

    /**
     * 服务端获取主播列表
     * @param anchorUrlPegBo
     * @return
     */
    @Override
    public R<PageUtils<AnchorUrlVo>> seletAnchorUrl(AnchorUrlPegBo anchorUrlPegBo) {
        return anchorUrlBll.seletAnchorUrl(anchorUrlPegBo);
    }

    /**
     * 客户查询用户是否有录制该主播
     * @param secUid
     * @return
     */
    @Override
    public R<List<String>> seletBysecUidAnchorUrlWhite( List<String> secUid) {
        UserCacheVo user = GlobalObject.getLocalUser();
        return anchorUrlWhiteBll.seletBysecUidAnchorUrlWhite(secUid,user.getId());
    }

    /**
     * 主播保存用户白名单
     * @param anchorUrlWhiteBo
     * @return
     */
    @Override
    public R<String> saveAnchorUrlWhite(AnchorUrlWhiteBo anchorUrlWhiteBo) {
        return anchorUrlWhiteBll.saveAnchorUrlWhite(anchorUrlWhiteBo);
    }

    /**
     * 服务端主播列表删除用户白名单
     * @param anchorUrlWhiteBo
     * @return
     */
    @Override
    public R<String> removeAnchorUrlWhite(AnchorUrlWhiteBo anchorUrlWhiteBo) {
        return anchorUrlWhiteBll.removeAnchorUrlWhite(anchorUrlWhiteBo);
    }

    /**
     * 服务端查询查询主播的白名单
     * @param anchorUrlWhiteListBo
     * @return
     */
    @Override
    public R<PageUtils<UserListVo>> seletUidAnchorUrlWhite(AnchorUrlWhiteListBo anchorUrlWhiteListBo) {
        List<Long> usrids = anchorUrlWhiteBll.seletUidAnchorUrlWhite(anchorUrlWhiteListBo.getSecUid());
        if(usrids !=null && usrids.size()>0){
            return userBll.selectByIds(anchorUrlWhiteListBo.getLimit(), anchorUrlWhiteListBo.getPage(),usrids);
        }
        return R.ok();
    }

    /**
     * 客户根据ssecUid查询用户是否有录制该主播
     * @param secUidS
     * @return
     */
    @Override
    public R<Boolean> seletBySerId(String secUidS) {
        UserCacheVo user = GlobalObject.getLocalUser();
        return anchorUrlWhiteBll.seletBySerId(secUidS,user.getId());
    }

    @Override
    public R<String> updateUserAnchor(AnchorUrlUserBo anchorUrlUserBo) {
        UserCacheVo user = GlobalObject.getLocalUser();
        anchorUrlUserBo.setUserId(user.getId());
        anchorUrlUserBo.setTenantId(user.getActiveTenantId());
        R<String> result = anchorUrlBll.updateUserAnchor(anchorUrlUserBo);

        // 统计主播数量
        this.updateUserAnchorNum(user.getId(), user.getActiveTenantId());

        return result;
    }

    private void updateUserAnchorNum(Long userId, Long tenantId) {
        // 获取数据
        R<List<AnchorUrlUserVo>> listR = anchorUrlBll.clientAnchorList(userId, tenantId);
        List<AnchorUrlUserVo> urlVos = Optional.ofNullable(listR)
                .map(R::getData)
                .orElse(Collections.emptyList());
        List<String> codes = List.of("anchorNum", "kuaishouMonitorNum", "anchorBarrageNum", "channelMonitorNum");

        // 没数据也照样更新 0
        if (CollUtil.isEmpty(urlVos)) {
            codes.forEach(code -> userPropertyBll.updateByPropertyNum(userId, code, 0L));
            return;
        }

        // 定义要统计的字段
        Map<String, Long> totalProperty = userPropertyBll.getTotalPropertyByUserIdCode(userId, codes);

        // 预过滤出有效数据（只保留未删除的）
        List<AnchorUrlUserVo> activeVos = urlVos.stream()
                .filter(vo -> vo != null && vo.getIsRemoveRecord() == 0)
                .toList();

        // 计算平台对应数量（只遍历一次）
        long douyinCount = countByPlatform(activeVos, AnchorPlatformEnum.DOU_YIN.getCode());
        long kuaishouCount = countByPlatform(activeVos, AnchorPlatformEnum.KUAI_SHOU.getCode());
        long shipinhaoCount = countByPlatform(activeVos, AnchorPlatformEnum.SHI_PING_HAO.getCode());
        long barrageCount = activeVos.stream().filter(vo -> vo.getIsBarrageMonitoring() == 1).count();

        // 组织结果
        Map<String, Long> result = Map.of(
                "anchorNum", Math.min(douyinCount, totalProperty.getOrDefault("anchorNum", 0L)),
                "kuaishouMonitorNum", Math.min(kuaishouCount, totalProperty.getOrDefault("kuaishouMonitorNum", 0L)),
                "anchorBarrageNum", barrageCount,
                "channelMonitorNum", Math.min(shipinhaoCount, totalProperty.getOrDefault("channelMonitorNum", 0L))
        );

        // 批量更新
        result.forEach((code, num) -> userPropertyBll.updateByPropertyNum(userId, code, num));
    }

    /**
     * 统计指定平台的主播数量
     */
    private long countByPlatform(List<AnchorUrlUserVo> vos, Integer platformCode) {
        return vos.stream()
                .filter(vo -> ObjectUtil.isNotEmpty(vo.getAnchorInfo())
                        && Objects.equals(vo.getAnchorInfo().getPlatform(), platformCode))
                .count();
    }

    /**
     * 添加主播至用户白名单
     * @param userId
     * @param secUid
     * @return
     */
    @Override
    public R<String> saveAnchorInUserWhite(Long userId,String secUid) {
        return anchorUrlWhiteBll.saveAnchorInUserWhite(userId,secUid);
    }

    /**
     * 只根据SecUid查询主播信息
     * @param secUid
     * @return
     */
    @Override
    public R<AnchorUrlInfoVo> infoBySecUidOne(String secUid) {

        return anchorUrlBll.infoBySecUidOne(secUid);

    }

    /**
     * 查询该主播的所属用户信息（白名单）
     * @param secUid
     * @return
     */
    @Override
    public R<List<UserVo>> selectUserByAnchorWhite(String secUid) {
        R<List<AnchorUrlWhiteEntity>> listR = anchorUrlWhiteBll.selectUserByAnchorWhite(secUid);
        if (listR.getCode() == 0 && ObjectUtil.isNotEmpty(listR.getData())) {
            List<AnchorUrlWhiteEntity> data = listR.getData();
            R<List<UserListVo>> userListR = userBll.listByIds(data.stream().map(AnchorUrlWhiteEntity::getUserId).distinct().toList());
            if (ObjectUtil.isNotEmpty(userListR.getData())) {
                return R.ok(userListR.getData().stream().map(item -> BeanUtil.copyProperties(item, UserVo.class)).toList());
            }
        }
        return R.ok(new ArrayList<>());
    }


    /**
     * 删除主播白名单
     * @param anchorUrlWhiteBos
     * @return
     */
    @Override
    public R<String> removeAnchorWhite(List<AnchorUrlWhiteBo> anchorUrlWhiteBos) {
        return anchorUrlWhiteBll.removeAnchorWhite(anchorUrlWhiteBos);
    }

    /**
     * 根据user_id从主播用户关联表中查出用户关联的所有主播sec_uid,在拿sec_uid去查询主播信息
     * @param anchorUrlUserBo
     * @return
     */
    @Override
    public R<PageUtils<AnchorUrlVo>> selectAnchorByUserId(AnchorUrlUserBo anchorUrlUserBo) {
        return anchorUrlBll.selectAnchorByUserId(anchorUrlUserBo);
    }


    /**
     * 根据userId获取当前用户添加的主播信息
     * @param anchorUrlUserBo
     * @return
     */
    @Override
    public R<PageUtils<AnchorUrlVo>> userAddAnchorRecord(AnchorUrlUserBo anchorUrlUserBo) {
        R<UserVo> userVoR = userBll.getById(anchorUrlUserBo.getUserId());
        if (userVoR.getData() != null && anchorUrlUserBo.getTenantId() == null){
            anchorUrlUserBo.setTenantId(userVoR.getData().getActiveTenantId());
        }
        return anchorUrlBll.userAddAnchorRecord(anchorUrlUserBo);
    }

    @Override
    public R<String> saveOrUpdateAnchor(AnchorUrlBo anchorUrlBo) {

        return anchorUrlBll.saveOrUpdateAnchor(anchorUrlBo);
    }

    @Override
    public R<String> bindUserAnchor(UserAnchorBo userAnchorBo) {

        UserCacheVo user = GlobalObject.getLocalUser();
        userAnchorBo.setUserId(user.getId());
        userAnchorBo.setTenantId(user.getActiveTenantId());

        R<String> stringR = anchorUrlBll.bindUserAnchor(userAnchorBo);

        // 统计主播数量
        this.updateUserAnchorNum(user.getId(), user.getActiveTenantId());

        return stringR;
    }

    @Override
    public R<String> updateAnchorTop(String secUid, Integer action, String addTopTime) {
        UserCacheVo user = GlobalObject.getLocalUser();

        return anchorUrlBll.updateAnchorTop(secUid, action, addTopTime, user.getId(), user.getActiveTenantId());
    }

    @Override
    public R<String> updateAnchorLastRecordTime(String secUid, String lastRecordTime) {

        UserCacheVo user = GlobalObject.getLocalUser();

        return anchorUrlBll.updateAnchorLastRecordTime(secUid, lastRecordTime, user.getId(), user.getActiveTenantId());
    }

    @Override
    public R<PageUtils<AnchorRecordListVo>> clientAnchorRecordList(ClientAnchorListBo clientAnchorListBo) {

        UserCacheVo user = GlobalObject.getLocalUser();
        clientAnchorListBo.setUserId(user.getId());
        clientAnchorListBo.setTenantId(user.getActiveTenantId());
        clientAnchorListBo.setLimit(99999);

        return anchorUrlBll.clientAnchorRecordList(clientAnchorListBo);
    }

    @Override
    public R<String> topAnchor(TopAnchorBo topAnchorBo) {

        UserCacheVo user = GlobalObject.getLocalUser();
        topAnchorBo.setUserId(user.getId());
        topAnchorBo.setTenantId(user.getActiveTenantId());

        return anchorUrlBll.topAnchor(topAnchorBo);
    }

    @Override
    public R<AnchorUrlUserVo> getUserAnchorBySecUid(String secUid) {

        UserCacheVo user = GlobalObject.getLocalUser();

        return anchorUrlBll.getUserAnchorBySecUid(secUid, null, user.getActiveTenantId());
    }

    @Override
    public R<List<AnchorUrlUserVo>> clientAnchorList() {

        UserCacheVo user = GlobalObject.getLocalUser();

        if (user == null){
            return R.ok(new ArrayList<>());
        }

        // 获取主播列表
        R<List<AnchorUrlUserVo>> listR = anchorUrlBll.clientAnchorList(user.getId(), user.getActiveTenantId());

        // 校准用户的存储资产
        R<Long> longR = anchorVideoBll.statisticsStoreByTenantId(user.getActiveTenantId());
        if (longR.getCode() == 0){
            Long parentId = ResultUtil.getResult(userApi.getUserParentId(user.getId()));
            userPropertyBll.updateByPropertyNum(parentId != null && parentId != 0 ? parentId : user.getId(), "storageNum", longR.getData());
        }

        // 统计主播数量
        this.updateUserAnchorNum(user.getId(), user.getActiveTenantId());
        List<AnchorUrlUserVo> anchorList = listR.getData();
        if (anchorList != null && !anchorList.isEmpty()) {

            //  设置基础设置
            setBasicSettings(anchorList, user.getId(), user.getActiveTenantId());

            // 主播不存在行业则添加默认行业
            Set<Long> tradeIds = anchorList.stream().map(AnchorUrlUserVo::getTradeId).collect(Collectors.toSet());
            R<List<TradeListVo>> listedByIds = tradeBll.listByIds(tradeIds);

            if (listedByIds.getCode() == 0 && ObjectUtil.isNotEmpty(listedByIds.getData())){
                Map<Long, TradeListVo> map = listedByIds.getData().stream()
                        .collect(Collectors.toMap(TradeListVo::getId, Function.identity(), (o1, o2) -> o1));

                anchorList.forEach(item -> {
                    TradeListVo tradeListVo = map.get(item.getTradeId());
                    if (ObjectUtil.isEmpty(tradeListVo)){
                        item.setTradeId(1L);
                    }
                });
            }

            // 获取诊断报告
            anchorList.forEach(item -> {
                item.setDiagnosisParams(new AnchorUrlUserVo.DiagnosisParams());
                item.setDataDiagnosisParams(new AnchorUrlUserVo.DiagnosisParams());
            });

            List<String> secUidList = anchorList.stream().map(AnchorUrlUserVo::getAnchorUrlSecUid).toList();
            List<DiagnosisCueInfoVo> diagnosisCueInfoVos = diagnosisCueBll.listBySourceIds(secUidList, 0, user.getId(), user.getActiveTenantId());
            List<DiagnosisModelInfoVo> diagnosisModelInfoVo = diagnosisModelBll.listBySourceIds(secUidList, 0, user.getId(), user.getActiveTenantId());

            Map<String, List<DiagnosisCueInfoVo>> cueMap = diagnosisCueInfoVos.stream()
                    .filter(item -> ObjectUtil.equals(item.getDiagnosisType(), AiEnums.diagnosisType.CONTENT_DIAGNOSIS.getCode()))
                    .collect(Collectors.groupingBy(DiagnosisCueInfoVo::getSourceId));
            Map<String, List<DiagnosisCueInfoVo>> cueDataMap = diagnosisCueInfoVos.stream()
                    .filter(item -> ObjectUtil.equals(item.getDiagnosisType(), AiEnums.diagnosisType.DATA_DIAGNOSIS.getCode()))
                    .collect(Collectors.groupingBy(DiagnosisCueInfoVo::getSourceId));


            Map<String, DiagnosisModelInfoVo> modelMap = diagnosisModelInfoVo.stream()
                    .filter(item -> ObjectUtil.equals(item.getDiagnosisType(), AiEnums.diagnosisType.CONTENT_DIAGNOSIS.getCode()))
                    .collect(Collectors.toMap(DiagnosisModelInfoVo::getSourceId, Function.identity(), (o1, o2) -> o1));
            Map<String, DiagnosisModelInfoVo> modelDataMap = diagnosisModelInfoVo.stream()
                    .filter(item -> ObjectUtil.equals(item.getDiagnosisType(), AiEnums.diagnosisType.DATA_DIAGNOSIS.getCode()))
                    .collect(Collectors.toMap(DiagnosisModelInfoVo::getSourceId, Function.identity(), (o1, o2) -> o1));


            anchorList.forEach(item -> {

                // 设置内容诊断的问题
                List<DiagnosisCueInfoVo> cueList = cueMap.get(item.getAnchorUrlSecUid());
                item.getDiagnosisParams().setCueWordsIds(cueList == null ? null : cueList.stream().map(DiagnosisCueInfoVo::getCueWordsId).toList());


//                List<DiagnosisCueInfoVo> cueDataList = cueDataMap.get(item.getAnchorUrlSecUid());
//                item.getDataDiagnosisParams().setCueWordsIds(cueDataList == null ? null : cueDataList.stream().map(DiagnosisCueInfoVo::getCueWordsId).toList());

                // 设置内容诊断的模型id
                DiagnosisModelInfoVo model = modelMap.get(item.getAnchorUrlSecUid());
                item.getDiagnosisParams().setModelId(model == null ? null : model.getModelId());

                // 设置数据诊断的模型id
                DiagnosisModelInfoVo modelData = modelDataMap.get(item.getAnchorUrlSecUid());
                item.getDataDiagnosisParams().setModelId(modelData == null ? null : modelData.getModelId());

            });

            return listR;
        }

        listR.setData(new LinkedList<>());
        return listR;
    }

    /**
     * 设置基础设置
     *
     * @param anchorList 列表
     * @param userId     用户ID
     * @param tenantId   租户ID
     */
    private void setBasicSettings(List<AnchorUrlUserVo> anchorList, Long userId, Long tenantId) {

        if (ObjectUtil.isEmpty(anchorList)) {
            return;
        }

        List<String> secUids = anchorList.stream().map(AnchorUrlUserVo::getAnchorUrlSecUid).distinct().toList();

        Map<String, BasicSettingsVo> basicSettingsMap = basicSettingsProducer.listBySourceUser(secUids, WordsEnum.basicSettingsType.ANCHOR.getCode(), userId, tenantId)
                .stream()
                .collect(Collectors.toMap(BasicSettingsVo::getSourceId, Function.identity(), (o1, o2) -> o1));

        if (ObjectUtil.isEmpty(basicSettingsMap)) {
            return;
        }

        anchorList.forEach(item -> {

            BasicSettingsVo basicSettingsVo = basicSettingsMap.get(item.getAnchorUrlSecUid());
            if (ObjectUtil.isNotEmpty(basicSettingsVo)) {
                //  赋值
                BeanUtil.copyProperties(
                        basicSettingsVo,
                        item,
                        CopyOptions.create().setIgnoreNullValue(true).setIgnoreProperties("id", "sourceId", "sourceType", "userId", "tenantId", "createDate", "updateDate")
                );
            }
        });

    }

    @Override
    public R<List<AnchorYesterdayRecordVo>> listAnchorYesterdayRecord(List<String> secUidList) {

        UserCacheVo user = GlobalObject.getLocalUser();

        return anchorUrlBll.listAnchorYesterdayRecord(secUidList, user.getId(), user.getActiveTenantId());
    }

    @Override
    public R<List<AnchorUrlInfoVo>> listByUniques(List<String> uniquesList) {

        return anchorUrlBll.listByUniques(uniquesList);
    }

    @Override
    public R<String> addOrUpdateAnchor(AddOrUpdateAnchorBo addOrUpdateAnchorBo) {

        UserCacheVo user = GlobalObject.getLocalUser();
        addOrUpdateAnchorBo.setUserId(user.getId());
        addOrUpdateAnchorBo.setTenantId(user.getActiveTenantId());
        R<String> r = distributedLock.executeWithLock("replay:anchorUrl:addOrUpdateAnchor:" + addOrUpdateAnchorBo.getSecUid(), () -> anchorUrlBll.addOrUpdateAnchor(addOrUpdateAnchorBo));
        if(r.getCode() == 0) {
            // 重新统计主播数量资产
            this.updateUserAnchorNum(user.getId(), user.getActiveTenantId());

            // 通知企业管理服务端添加主播
            if (Objects.equals(addOrUpdateAnchorBo.getIsStatisticsPerformance(), 1)) {
                try {
                    GovernanceAddAnchorBo anchorBo = new GovernanceAddAnchorBo();
                    anchorBo.setPlatform(addOrUpdateAnchorBo.getPlatform());
                    anchorBo.setAnchorNumber(addOrUpdateAnchorBo.getAnchorNumber());
                    anchorBo.setSecUid(addOrUpdateAnchorBo.getSecUid());
                    anchorBo.setHomeUrl(addOrUpdateAnchorBo.getHomeUrl());
                    anchorBo.setLiveUrl(addOrUpdateAnchorBo.getLiveUrl());
                    anchorBo.setAnchorName(addOrUpdateAnchorBo.getAnchorName());
                    anchorBo.setAnchorAvatar(addOrUpdateAnchorBo.getAnchorAvatar());
                    anchorBo.setTradeId(addOrUpdateAnchorBo.getTradeId());
                    governanceLiveRoomService.addAnchor(anchorBo);
                } catch (Exception e) {
                    log.error("[企业管理] 通知添加主播失败, secUid={}", addOrUpdateAnchorBo.getSecUid(), e);
                }
            }
        }

        // 处理诊断报告的问题选择
        if (addOrUpdateAnchorBo.getIsAutoDiagnosis() != null && addOrUpdateAnchorBo.getIsAutoDiagnosis() == 1){
            SaveDiagnosisCueBo cueBo = new SaveDiagnosisCueBo();
            cueBo.setUserId(user.getId());
            cueBo.setTenantId(user.getActiveTenantId());
            cueBo.setSourceId(addOrUpdateAnchorBo.getSecUid());
            cueBo.setSourceType(0);
            AddOrUpdateAnchorBo.DiagnosisParams diagnosisParams = addOrUpdateAnchorBo.getDiagnosisParams();
            RRException.isNotEmpty(diagnosisParams, "请输入诊断参数");
            cueBo.setModelId(diagnosisParams.getModelId());
            cueBo.setSelectCueWordsIdsList(diagnosisParams.getCueWordsIds());
            diagnosisCueBll.saveDiagnosisCue(cueBo);
        }

        // 处理数据诊断报告的问题选择
        if (addOrUpdateAnchorBo.getIsDataDiagnosis() != null && addOrUpdateAnchorBo.getIsDataDiagnosis() == 1) {
            DiagnosisModelBo cueBo = new DiagnosisModelBo();
            cueBo.setUserId(user.getId());
            cueBo.setTenantId(user.getActiveTenantId());
            cueBo.setSourceId(addOrUpdateAnchorBo.getSecUid());
            cueBo.setSourceType(0);
            AddOrUpdateAnchorBo.DiagnosisParams diagnosisParams = addOrUpdateAnchorBo.getDataDiagnosisParams();
            RRException.isNotEmpty(diagnosisParams, "请输入数据诊断参数");
            cueBo.setModelId(diagnosisParams.getModelId());
            cueBo.setDiagnosisType(AiEnums.diagnosisType.DATA_DIAGNOSIS.getCode());
            diagnosisModelBll.updateDiagnosisModel(cueBo);
        }

        return r;
    }

    @Override
    public R<List<AnchorUrlUserVo>> clientTenantAnchorList() {

        UserCacheVo user = GlobalObject.getLocalUser();

        return anchorUrlBll.listByTenantId(user.getActiveTenantId());
    }

    @Override
    public R<AnchorUrlUserVo> getCurrUserAnchorBySecUid(String secUid) {

        UserCacheVo user = GlobalObject.getLocalUser();

        return anchorUrlBll.getUserAnchorBySecUid(secUid, user.getId(), user.getActiveTenantId());
    }

    @Override
    public R<String> updateBarrageMonitoring(String secUid, Integer isBarrageMonitoring) {
        RRException.isNotEmpty(GlobalObject.getLocalUser(), "请登录");
        Long currentPropertyNum = 0L;
        if (isBarrageMonitoring == 1){
            // 检查弹幕监控位数量是否足够
            R<Long> anchorBarrageNum = userPropertyBll.getTotalUserIdPropertyByCode(GlobalObject.getLocalUser().getId(), "anchorBarrageNum");
            RRException.isNotEmpty(anchorBarrageNum, "弹幕监控位查询失败");
            currentPropertyNum = anchorBarrageNum.getData();
        }

        UserCacheVo localUser = GlobalObject.getLocalUser();
        R<Long> currentBarrageNumR = anchorUrlBll.updateBarrageMonitoring(localUser.getId(), localUser.getActiveTenantId(), secUid, isBarrageMonitoring, currentPropertyNum);
        if (currentBarrageNumR.getCode() == 0){
            // 更新弹幕监控位
            userPropertyBll.updateByPropertyNum(GlobalObject.getLocalUser().getId(), "anchorBarrageNum", currentBarrageNumR.getData());
        }
        return R.ok();
    }

    @Override
    public R<String> updateAutoUploadCloud(String secUid, Integer isAutoUploadCloud) {

        UserCacheVo user = GlobalObject.getLocalUser();

        return anchorUrlBll.updateAutoUploadCloud(secUid, isAutoUploadCloud, user.getId(), user.getActiveTenantId());
    }

    @Override
    public R<OpenMonitoringPositionVo> openMonitoringPosition(String secUid) {
        return R.ok(anchorUrlBll.openMonitoringPosition(secUid));
    }

    @Override
    public void generatedAnchorKeywords(String secUid) {
        AnchorVideoSimpleVo simpleVo = anchorCruxWordsBll.changeYesToVideoId(secUid);
        if (simpleVo == null) {
            throw new BusinessException("当前主播没有符合条件的视频，生成关键词失败");
        }

        List<String> keywords = startGetAnchorKeywords(simpleVo.getSecUid(), simpleVo.getVideoId());

        if (ObjectUtil.isNotEmpty(keywords)) {
            anchorCruxWordsBll.clearAndSaveAnchorKeywords(simpleVo.getSecUid(), keywords, null, null);
        }
    }

    /**
     * 开始获取主播关键词
     *
     * @param secUid  主播id
     * @param videoId 视频id
     */
    private static final String ANCHOR_KEYWORD_LOCK_PREFIX = "anchor:keyword:processing:";
    private static final long ANCHOR_KEYWORD_LOCK_TIMEOUT = 30;

    @Override
    public List<String> startGetAnchorKeywords(String secUid, String videoId) {
        // Redis防重：避免同时重复给相同主播提取关键词
        String lockKey = ANCHOR_KEYWORD_LOCK_PREFIX + secUid;
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", ANCHOR_KEYWORD_LOCK_TIMEOUT, TimeUnit.MINUTES);
        if (Boolean.FALSE.equals(acquired)) {
            log.info("[生成主播关键词] secUid = {} 正在处理中，跳过", secUid);
            return null;
        }

        try {
            AnchorUrlDetailsEntity entity = new AnchorUrlDetailsEntity();
            entity.setSecUid(secUid);
            // 获取视频文字
            AnalysisResultVo analysis = ResultUtil.getResult(sensitiveWordsBll.getAnalysisData(WordsEnum.sourceType.VIDEO.getCode(), videoId));
            if (analysis == null) {
                log.error("[生成主播关键词] 获取视频的音转译文字失败 secUid = {} videoId = {}", secUid, videoId);
                return null;
            }


            // 获取键值对
            SystemKvInfoVo kvPrompt = getByKey("ancho_keyword_prompt");
            SystemKvInfoVo kvPromptNum = getByKey("ancho_keyword_prompt_num");
            SystemKvInfoVo kvPromptModel = getByKey("ancho_keyword_ai_model");

            // 获取提示词
            String prompt = getAllPrompt(analysis, kvPromptNum.getKvValue(), kvPrompt.getKvValue());

            // 获取ai模型
            AiModelInfoVo aiModel = ResultUtil.getResult(aiModelBll.getByCode(kvPromptModel.getKvValue()));
            if (aiModel == null) {
                log.error("[生成主播关键词] 没有找到对应的ai模型配置, code = {}", kvPromptModel.getKvValue());
                return null;
            }

            // 获取ai模型
            AiModel aiModel1 = ModelFactoryUtils.getAiModel(aiModel.getResourceType());
            if (aiModel1 == null) {
                log.error("[生成主播关键词] 在工厂模式中获取AI模型失败，resourceType = {}", aiModel.getResourceType());
                return null;
            }

            // 提问
            AiMessageBo params = new AiMessageBo();
            params.setSystem(List.of(Map.of("text", "你是一个顶尖关键词内容提炼专家")));
            params.setUser(List.of(Map.of("text", prompt)));

            AiModelBo modelConfig = BeanUtil.copyProperties(aiModel, AiModelBo.class);
            AiReturnDataVo aiReturnDataVo = aiModel1.chatCompletion(modelConfig, params, 0L);
            if (aiReturnDataVo == null || !ObjectUtil.equals(aiReturnDataVo.getStatus(), 0)) {
                log.error("[生成主播关键词] 掉用ai问答时出错，secUid = {} videoId = {}", secUid, videoId);
                return null;
            }
            // 记录aiToken消耗量
            AiTokenUseRecordBo aiTokenUseRecordBo = AiTokenUseRecordBo.builder(aiReturnDataVo, modelConfig.getModelName());
            aiTokenUseRecordBo.setTenantId(0L);
            aiTokenUseRecordBo.setUserId(0L);
            aiTokenUseRecordBo.setModelName(modelConfig.getModelName());
            aiTokenUseRecordBo.setUseSourceType(AiEnums.useSourceType.ANCHOR_KEYWORD.getCode());
            aiTokenUseRecordBo.setUseSourceId(secUid);
            aiTokenUseRecordBo.setAssistantType(AiEnums.askType.ANCHOR_KEYWORD.getCode());
            aiTokenUseRecordBll.save(aiTokenUseRecordBo);

            String content = aiReturnDataVo.getContent();
            List<String> keywords = List.of(content.split("[,，_]"));

            return keywords;
        } catch (Exception e) {
            log.error("[生成主播关键词] 处理异常 secUid = {} videoId = {}", secUid, videoId, e);
            return null;
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    @Override
    public void getAiTrade(String secUid) {

        UserCacheVo user = GlobalObject.getLocalUser();
        RRException.isNotEmpty(user, "请登录");

        // 获取满足条件的视频,
        AnchorVideoInfoVo video = anchorVideoBll.getAiRecommendVideoBySecUid(secUid);
        if (video == null) {
            throw new BusinessException("当前主播没有符合条件的视频，无法获取AI推荐行业");
        }

        SystemKvInfoVo kvInfoVo = ResultUtil.getResult(systemKvBll.getByKey("suggest_trade_ai_model"));
        if (kvInfoVo == null) {
            throw new BusinessException("获取键值对失败，key = suggest_trade_ai_model");
        }

        AiModelInfoVo aiModelBo = aiModelBll.getAiModelByCode(kvInfoVo.getKvValue());
        if (aiModelBo == null) {
            throw new BusinessException("获取ai模型失败，code = " + kvInfoVo.getKvValue());
        }

        List<Tree<Long>> treeList = tradeBll.listTreeTrade();
        TreePrinterWithMap.TreePrintResult treePrintResult = TreePrinterWithMap.printTreeWithMap(treeList);
        // 获取对应的提示词
        String industryPrompt = anchorVideoBll.getIndustryPrompt(WordsEnum.sourceType.VIDEO.getCode(), video.getVideoId(), (type, uuid) -> ResultUtil.getResult(sensitiveWordsBll.getAnalysisData(type, uuid)), treePrintResult);

        AiModel aiModel = new DoubaoAiModelImpl();

        AiMessageBo params = new AiMessageBo();
        // 设置身份
        params.setSystem(List.of(Map.of("text", "你是一个顶尖行业分析师")));
        // 设置问题
        params.setUser(List.of(Map.of("text", industryPrompt)));

        AiReturnDataVo aiReturnDataVo = aiModel.chatCompletion(BeanUtil.copyProperties(aiModelBo, AiModelBo.class), params, 0L);

        log.info("[获取ai推荐行业] secUid = {} ,回答：{}", secUid, aiReturnDataVo.getContent());

        // 记录aiToken消耗量
        AiTokenUseRecordBo aiTokenUseRecordBo = AiTokenUseRecordBo.builder(aiReturnDataVo, aiModelBo.getModelName());
        aiTokenUseRecordBo.setTenantId(user.getActiveTenantId());
        aiTokenUseRecordBo.setUserId(user.getId());
        aiTokenUseRecordBo.setModelName(aiModelBo.getModelName());
        aiTokenUseRecordBo.setUseSourceType(AiEnums.useSourceType.VIDEO.getCode());
        aiTokenUseRecordBo.setUseSourceId(video.getVideoId());
        aiTokenUseRecordBo.setAssistantType(AiEnums.askType.TRADE_RECOMMEND.getCode());
        aiTokenUseRecordBll.save(aiTokenUseRecordBo);

        long tradeId = 1L;
        // 使用正则表达式把行业id和行业名称分开
        try {
            // 创建正则表达式模式 - 匹配数字和小数点
            Pattern pattern = Pattern.compile("\\d+(\\.\\d+)+");

            // 执行匹配
            Matcher matcher = pattern.matcher(aiReturnDataVo.getContent());

            String val = "";
            if (matcher.find()) {
                val = matcher.group();
            }

            // 从tradeList中查找匹配的行业
            if (treePrintResult.tradeList() != null && treePrintResult.tradeList().containsKey(val)) {

                TreePrinterWithMap.TradeTemp tradeTemp = treePrintResult.tradeList().get(val);
                if (tradeTemp != null) {
                    tradeId = tradeTemp.getTradeId();
                }
            }
        } catch (Exception ex) {
            log.error("[获取ai推荐行业] 匹配ai输出的行业报错: {}", ex.getMessage());
            throw new BusinessException("匹配ai输出的行业报错 ai返回的数据为: " + ex.getMessage());
        }

        anchorUrlBll.updateTrdeIdBySecUid(secUid, tradeId, null);
    }


    /**
     * 获取提示词中的内容
     *
     * @param analysis 视频文字
     * @return 内容
     */
    private String getPrompt(AnalysisResultVo analysis) {
        String res = "";

        if (analysis == null) {
            return res;
        }

        if (ObjectUtil.isEmpty(analysis.getSentenceMarkVos())) {
            return res;
        }
        // 获取视频文字
        return analysis.getSentenceMarkVos().stream()
                .map(SentenceMarkVo::getContent).collect(Collectors.joining("\n"));
    }

    /**
     * 获取全部提示词
     *
     * @param analysis 视频文字
     * @param numStr   最大字数
     * @param question 提示词内容
     * @return
     */
    private String getAllPrompt(AnalysisResultVo analysis, String numStr, String question) {
        String prompt = getPrompt(analysis);
        if (ObjectUtil.isEmpty(prompt)) {
            throw new BusinessException("获取视频文字为空");
        }
        Integer num = NumberUtil.parseInt(numStr, 0);
        String tou = question + "\n\n下文是直播间内容：\n";
        prompt = tou + prompt;
        prompt = StrUtil.sub(prompt, 0, num);

        if (ObjectUtil.isEmpty(prompt)) {
            throw new BusinessException("获取的提示词为空");
        }
        return prompt;
    }

    @Override
    public R<AuthUsageVo> getAuthUsage(Integer authType) {
        if (authType == null || authType < 1 || authType > 3) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "授权类型无效，取值范围 1=巨量 / 2=千川 / 3=来客");
        }
        UserCacheVo user = GlobalObject.getLocalUser();
        if (user == null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "当前用户未登录");
        }
        UserPropertyTypeInfoVo property = userPropertyBll.getUserPropertyByCode(user.getId(), "rpaAmountNum");
        long totalCount = property != null && property.getTotalQuantity() != null ? property.getTotalQuantity() : 0L;
        long usedCount = anchorUrlUserBll.countAuthUsed(user.getId(), user.getActiveTenantId(), authType);
        long remainingCount = Math.max(totalCount - usedCount, 0L);
        AuthUsageVo vo = new AuthUsageVo();
        vo.setTotalCount(totalCount);
        vo.setUsedCount(usedCount);
        vo.setRemainingCount(remainingCount);
        return R.ok(vo);
    }

    /**
     * 根据key获取系统配置的键值对
     *
     * @param key 建
     * @return 键值对
     */
    private SystemKvInfoVo getByKey(String key) {
        SystemKvInfoVo kv = systemKvBll.getByKeyNotR(key);
        if (kv == null || ObjectUtil.isEmpty(kv.getKvValue())) {
            throw new BusinessException("获取键值对失败，key = " + key);
        }
        return kv;
    }
}

