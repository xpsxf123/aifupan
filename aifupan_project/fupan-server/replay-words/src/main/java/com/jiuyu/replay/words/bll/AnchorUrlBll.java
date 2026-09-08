package com.jiuyu.replay.words.bll;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.jiuyu.framework.function.BatchQuery;
import com.jiuyu.framework.json.JsonTemplate;
import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.replay.common.constant.OrderEnums;
import com.jiuyu.replay.common.constant.WordsEnum;
import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.common.utils.WindowsFileUtils;
import com.jiuyu.replay.generic.bo.governance.GovernanceAddAnchorBo;
import com.jiuyu.replay.generic.bo.words.anchor.SetAnchorTradeBo;
import com.jiuyu.replay.generic.bo.words.anchor.SubAnchorListBo;
import com.jiuyu.replay.generic.bo.words.anchor.UpdateAnchorBaseInfoBo;
import com.jiuyu.replay.generic.bo.words.anchor.UpdateAnchorUserBo;
import com.jiuyu.replay.generic.enums.words.AnchorPlatformEnum;
import com.jiuyu.replay.generic.enums.words.VideoSliceTypeEnum;
import com.jiuyu.replay.generic.feign.order.UserPropertyFeign;
import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.feign.system.DictDataFeign;
import com.jiuyu.replay.generic.feign.third.GovernanceLiveRoomService;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.utils.ResultUtil;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.common.code.StatusCode;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.generic.vo.order.MonitorPositionAuthVo;
import com.jiuyu.replay.generic.vo.order.UserPropertyTypeInfoVo;
import com.jiuyu.replay.generic.vo.power.UserCacheVo;
import com.jiuyu.replay.generic.vo.power.UserInfoExportVo;
import com.jiuyu.replay.generic.vo.power.UserVo;
import com.jiuyu.replay.generic.vo.system.DictDataListVo;
import com.jiuyu.replay.generic.vo.words.*;
import com.jiuyu.replay.words.bo.*;
import com.jiuyu.replay.words.bo.anchor.AddOrUpdateAnchorBo;
import com.jiuyu.replay.words.bo.anchor.ClientAnchorListBo;
import com.jiuyu.replay.words.bo.anchor.TopAnchorBo;
import com.jiuyu.replay.words.bo.video.UpdateAiPartialBo;
import com.jiuyu.replay.words.constant.Constant;
import com.jiuyu.replay.words.constant.WordsProperties;
import com.jiuyu.replay.words.dto.TenantAnchorQueryRequest;
import com.jiuyu.replay.words.entity.AnchorUrlDetailsEntity;
import com.jiuyu.replay.words.entity.AnchorUrlUserEntity;
import com.jiuyu.replay.words.entity.StandardScriptEntity;
import com.jiuyu.replay.words.producer.*;
import com.jiuyu.replay.words.repository.service.AnchorUrlUserService;
import com.jiuyu.replay.words.repository.service.StandardScriptService;
import com.jiuyu.replay.words.rse.AiOptimizePurposeRse;
import com.jiuyu.replay.words.rse.AnchorRse;
import com.jiuyu.replay.words.vo.AnchorClientVo;
import com.jiuyu.replay.words.vo.SyncContrastInfoVo;
import com.jiuyu.replay.words.vo.anchor.*;
import com.jiuyu.replay.words.vo.file.UploadFileInfoVo;
import com.jiuyu.replay.words.vo.viewing.VideoDataViewingConfuseInfoVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 主播url
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-08-10 16:47:37
 */
@Slf4j
@Component
public class AnchorUrlBll {

    @Resource
    private AnchorUrlProducer anchorUrlProducer;
    @Resource
    AnchorUrlUserProducer anchorUrlUserProducer;
    @Resource
    private AnchorVideoProducer anchorVideoProducer;
    @Resource
    private VideoDataViewingConfuseProducer videoDataViewingConfuseProducer;
    @Resource
    private WordsProperties wordsProperties;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private UserFeign userFeign;
    @Resource
    private UserPropertyFeign userPropertyFeign;
    @Resource
    private DictDataFeign dictDataFeign;
    @Resource
    private AnchorUrlUserService anchorUrlUserService;

    @Resource
    private StandardScriptService standardScriptService;

    @Resource
    private AnchorRse anchorRse;
    @Autowired
    private SyncContrastProducer syncContrastProducer;
    @Autowired
    private AnchorUrlDetailsBll anchorUrlDetailsBll;
    @Autowired
    private BasicSettingsProducer basicSettingsProducer;
    @Autowired
    private AiOptimizePurposeRse aiOptimizePurposeRse;
    @Autowired
    private UploadFileProducer uploadFileProducer;
    @Resource
    private GovernanceLiveRoomService governanceLiveRoomService;

    @Resource
    private RedissonClient redissonClient;

    /**
     * AnchorUrlBll save / saveOrUpdateAnchor 共用的 Redisson 锁 key 前缀（per-secUid 粒度）。
     * 完整 key = {@code LOCK_KEY_PREFIX + secUid}。
     * 用 Redisson 替代原 ReentrantLock saveLock(全局锁)的理由：
     * 1. 锁粒度从"全局"细化到"per-secUid",不同主播间互不阻塞;
     * 2. 集群部署天然安全(多实例共用 Redis 锁);
     * 3. save() 与 saveOrUpdateAnchor() 共用同一 key,实现跨方法互斥(防同 secUid 通过两条入口并发 race)。
     */
    private static final String LOCK_KEY_PREFIX = "replay:words:anchor:save:";

    /** Redisson 锁等待时间(秒):获取锁最多等 5s,超过抛业务异常返回前端。 */
    private static final long LOCK_WAIT_SECONDS = 5L;

    /** Redisson 锁持有时间(秒):兜底防应用挂掉锁不释放(正常临界区 < 1s)。 */
    private static final long LOCK_LEASE_SECONDS = 30L;

    /**
     * 主播url列表
     *
     * @param anchorUrlListBo 主播url列表查询参数
     * @return
     */
    public R<PageUtils<AnchorUrlListVo>> queryPage(AnchorUrlListBo anchorUrlListBo) {

        return R.ok("获取成功", anchorUrlProducer.queryPage(anchorUrlListBo));

    }

    /**
     * 主播url信息
     *
     * @param id 主播urlid
     * @return
     */
    public R<AnchorUrlInfoVo> info(Long id) {

        AnchorUrlInfoVo anchorUrlInfoVo = anchorUrlProducer.info(id);
        return R.ok("获取成功", anchorUrlInfoVo);

    }

    /**
     * 新增主播url
     *
     * @param anchorUrlBo 主播url对象
     * @return
     */
    public R<String> save(AnchorUrlBo anchorUrlBo) {
        // MINOR-3 防御:secUid null/empty 时 fail-fast,避免锁 key 退化为 "...null" 把所有 null 请求串行化
        if (StringUtils.isEmpty(anchorUrlBo.getSecUid())) {
            throw new BusinessException(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "secUid 不能为空");
        }
        RLock lock = redissonClient.getLock(LOCK_KEY_PREFIX + anchorUrlBo.getSecUid());
        boolean locked = false;
        try {
            locked = lock.tryLock(LOCK_WAIT_SECONDS, LOCK_LEASE_SECONDS, TimeUnit.SECONDS);
            if (!locked) {
                log.warn("[主播管理] save 获取锁超时,secUid={}", anchorUrlBo.getSecUid());
                throw new BusinessException(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "操作过于频繁,请稍后重试");
            }
            // 设置抖音号
            String anchorNumber = anchorUrlBo.getLiveUrl().substring(anchorUrlBo.getLiveUrl().lastIndexOf("/") + 1);
            anchorUrlBo.setAnchorNumber(anchorNumber);
            AnchorUrlInfoVo oldAnchorUrlInfoVo = anchorUrlProducer.infoBySecUid(anchorUrlBo.getSecUid());
            if (oldAnchorUrlInfoVo == null) {
                AnchorUrlInfoVo anchorUrlInfoVo = anchorUrlProducer.save(anchorUrlBo);
                return R.ok("添加成功");
            }

            if (!StringUtils.isEmpty(anchorUrlBo.getLiveUrl())) {
                anchorUrlBo.setId(oldAnchorUrlInfoVo.getId());
                this.anchorUrlProducer.update(anchorUrlBo);
            }

            return R.ok("添加成功");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[主播管理] save 锁等待被中断,secUid={}", anchorUrlBo.getSecUid(), e);
            throw new BusinessException(StatusCode.FAILED_TO_REQUEST.getCode(), "锁等待被中断");
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 修改主播url
     *
     * @param anchorUrlBo 主播url对象
     * @return
     */
    public R<String> update(AnchorUrlBo anchorUrlBo) {

        anchorUrlProducer.update(anchorUrlBo);
        return R.ok("修改成功");
    }

    /**
     * 删除主播url
     *
     * @param id 主播urlid
     * @return
     */
    public R<String> delete(Long id) {

        anchorUrlProducer.deleteById(id);
        return R.ok("删除成功");
    }

    /**
     * 根据任意条件获取主播url
     *
     * @param secUid  主播唯一标识
     * @param liveUrl 主播直播地址
     * @param homeUrl 主播主页地址
     * @return
     */
    public R<AnchorUrlInfoVo> infoByCondition(String secUid, String liveUrl, String homeUrl) {

        AnchorUrlInfoVo anchorUrlInfoVo = anchorUrlProducer.infoByCondition(secUid, liveUrl, homeUrl);
        return R.ok("获取成功", anchorUrlInfoVo);
    }

    /**
     * 根据主播唯一标识(secUid、homeUrl、liveUrl)集合获取主播
     *
     * @param uniquesList 主播唯一标识集合
     * @return
     */
    public R<List<AnchorUrlInfoVo>> listByUniques(List<String> uniquesList) {

        List<AnchorUrlInfoVo> anchorUrlInfoVos = anchorUrlProducer.listByConditions(uniquesList);

        return R.ok(anchorUrlInfoVos);

    }

    /**
     * 批量保存
     *
     * @param anchorUrlBos 主播集合
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public R<String> saveBatch(List<AnchorUrlBo> anchorUrlBos) {

        if (anchorUrlBos != null && anchorUrlBos.size() > 0) {
            List<AnchorUrlBo> liveAnchorList = anchorUrlBos.stream()
                    .filter(item -> !StringUtils.isEmpty(item.getLiveUrl())).toList();
            if (liveAnchorList.size() > 0) {
                List<String> secUids = liveAnchorList.stream().map(AnchorUrlBo::getSecUid).toList();

                // 删掉原主播数据
                this.anchorUrlProducer.deleteBatchBySecUids(secUids);
                // 添加新数据
                this.anchorUrlProducer.saveBatch(liveAnchorList);
            }
        }

        return R.ok("添加成功");

    }

    /**
     * 根据主播唯一标识集合获取主播(只返回有live地址的主播)
     *
     * @param secUidList 主播唯一标识集合
     * @return
     */
    public R<List<AnchorUrlInfoVo>> listLiveBySecUids(List<String> secUidList) {

        List<AnchorUrlInfoVo> anchorUrlInfoVos = anchorUrlProducer.listBySecUids(secUidList);

        if (anchorUrlInfoVos != null && anchorUrlInfoVos.size() > 0) {

            List<AnchorUrlInfoVo> result = anchorUrlInfoVos.stream()
                    .filter(item -> !StringUtils.isEmpty(item.getLiveUrl())).toList();
            return R.ok(result);
        }

        return R.ok(null);
    }

    /**
     * 根据用户idchax
     *
     * @param userId
     * @return
     */
    public R<List<AnchorUrlVo>> seletAnchorUrlList(Long userId) {
        List<AnchorUrlVo> anchorUrlVos = anchorUrlUserProducer.seleByuserId(userId);
        return R.ok("查询成功", anchorUrlVos);
    }

    /**
     * 用户绑定主播
     *
     * @param secUidList
     * @param userId
     * @return
     */
    public R<String> save(List<AnchorUrlUserBo> secUidList, Long userId) {
        return anchorUrlUserProducer.saves(secUidList, userId);
    }

    /**
     * 删除用户与绑定组播的关系
     *
     * @param secUidList
     * @param id
     */
    public void deletBySecUid(String secUidList, Long id) {
        anchorUrlUserProducer.deletBySecUid(secUidList, id);
    }

    /**
     * 服务端根据用户id分页查询绑定主播列表
     *
     * @param anchorUrlPegBo
     * @return
     */
    public R<PageUtils<AnchorUrlVo>> seletByUserId(AnchorUrlPegBo anchorUrlPegBo) {
        return anchorUrlUserProducer.seletByUserId(anchorUrlPegBo);
    }

    /**
     * 统计用户绑定了主播数量
     *
     * @param userId
     * @return
     */
    public Integer sum(Long userId) {
        return anchorUrlUserProducer.sum(userId);
    }

    /**
     * 服务端获取主播列表
     *
     * @param anchorUrlPegBo
     * @return
     */
    public R<PageUtils<AnchorUrlVo>> seletAnchorUrl(AnchorUrlPegBo anchorUrlPegBo) {
        return anchorUrlProducer.seletAnchorUrl(anchorUrlPegBo);
    }

    /**
     * 服务端根据seu_uid获取用户信息
     *
     * @param secUid
     * @return
     */
    public List<AnchorUrlUserEntity> selectBySecUid(String secUid) {
        return anchorUrlUserProducer.selectBySecUid(secUid);

    }

    /**
     * 修改用户绑定的主播信息
     *
     * @param anchorUrlUserBo 主播用户关联 Bo（含 isRemoveRecord / userId / tenantId / anchorUrlSecUid）
     * @return 操作结果
     */
    @Transactional(rollbackFor = Exception.class)
    public R<String> updateUserAnchor(AnchorUrlUserBo anchorUrlUserBo) {

        AnchorUrlUserVo anchorUrlUserVo = this.anchorUrlUserProducer.getUserAnchorBySecUid(
                anchorUrlUserBo.getAnchorUrlSecUid(), anchorUrlUserBo.getUserId(), anchorUrlUserBo.getTenantId());

        if (anchorUrlUserVo != null) {
            anchorUrlUserBo.setId(anchorUrlUserVo.getId());
            // 软删时（isRemoveRecord=1）一并关闭三个监控开关，避免主播恢复后开关复活、与资源位统计不一致
            if (Objects.equals(anchorUrlUserBo.getIsRemoveRecord(), 1)) {
                anchorUrlUserBo.setIsScriptQualityInspection(0);
                anchorUrlUserBo.setIsScriptFidelityMonitor(0);
                anchorUrlUserBo.setIsInteractionPatrol(0);
            }
            // 先写 isRemoveRecord 落库，countOpenSwitch 内置 isRemoveRecord=0 过滤，先写后查自然递减
            this.anchorUrlUserProducer.updateById(anchorUrlUserBo);

            // 软删时（isRemoveRecord=1）释放三个监控开关占用的资源位
            if (Objects.equals(anchorUrlUserBo.getIsRemoveRecord(), 1)) {
                Long userId = anchorUrlUserBo.getUserId();
                Long tenantId = anchorUrlUserBo.getTenantId();
                String secUid = anchorUrlUserBo.getAnchorUrlSecUid();

                // 构建开关字段名 → commodityTypeCode 映射（防御性总调，无条件遍历三个对）
                Map<String, String> releaseCodeMap = new LinkedHashMap<>();
                releaseCodeMap.put("isScriptQualityInspection", OrderEnums.commodityTypeCode.SCRIPT_QUALITY_NUM.getCode());
                releaseCodeMap.put("isScriptFidelityMonitor", OrderEnums.commodityTypeCode.SCRIPT_FIDELITY_NUM.getCode());
                releaseCodeMap.put("isInteractionPatrol", OrderEnums.commodityTypeCode.INTERACTION_PATROL_NUM.getCode());

                for (Map.Entry<String, String> entry : releaseCodeMap.entrySet()) {
                    String switchField = entry.getKey();
                    String code = entry.getValue();
                    // 统计当前用户该租户下 isRemoveRecord=0 且该开关开启的总行数（刚软删的行已不含）
                    Long openCount = anchorUrlUserProducer.countOpenSwitch(userId, tenantId, switchField);
                    // 覆盖写：直接传当前存量，无需 ±1
                    boolean ok = userPropertyFeign.updateByPropertyNumRetBoolean(userId, code, openCount);
                    if (!ok) {
                        log.warn("[软删释放] 更新资产失败 userId={} tenantId={} secUid={} code={} openCount={}",
                                userId, tenantId, secUid, code, openCount);
                        throw new RuntimeException("资产占用量更新失败, userId=" + userId + ", code=" + code);
                    }
                }
            }
        }

        return R.ok();
    }

    /**
     * 根据用户id获取主播列表
     *
     * @param userId 用户id
     * @return
     */
    public R<List<AnchorClientVo>> listByUserId(Long userId) {

        List<AnchorClientVo> anchorClientVos = this.anchorUrlProducer.listByUserId(userId);

        return R.ok(anchorClientVos);
    }

    /**
     * 只根据SecUid查询主播信息
     *
     * @param secUid
     * @return
     */
    public R<AnchorUrlInfoVo> infoBySecUidOne(String secUid) {
        return this.anchorUrlProducer.infoBySecUidOne(secUid);
    }

    /**
     * 根据user_id从主播用户关联表中查出用户关联的所有主播sec_uid,在拿sec_uid去查询主播信息
     *
     * @param anchorUrlUserBo
     * @return
     */
    public R<PageUtils<AnchorUrlVo>> selectAnchorByUserId(AnchorUrlUserBo anchorUrlUserBo) {
        return anchorUrlProducer.selectAnchorByUserId(anchorUrlUserBo);
    }

    public void updateIsRemoveRecord(List<String> secUids, Long userId, int isRemoveRecord) {

        anchorUrlProducer.updateIsRemoveRecord(secUids, userId, isRemoveRecord);
    }

    /**
     * 根据userId获取当前用户添加的主播信息
     *
     * @param anchorUrlUserBo
     * @return
     */
    public R<PageUtils<AnchorUrlVo>> userAddAnchorRecord(AnchorUrlUserBo anchorUrlUserBo) {
        return anchorUrlProducer.userAddAnchorRecord(anchorUrlUserBo);
    }

    /**
     * 添加或修改主播信息
     *
     * @param anchorUrlBo 主播信息
     * @return
     */
    public R<String> saveOrUpdateAnchor(AnchorUrlBo anchorUrlBo) {
        // todo 输出日志为客户端罗肖同学解惑，排查问题😊
        log.info("[主播管理] user add anchor info, client param, {}", JsonTemplate.toJson(anchorUrlBo));

        // AC-013:与 save() 共用同一把 Redisson per-secUid 锁,实现跨方法 + 集群安全的 check-then-act 互斥;
        // 修复存量并发 bug(虚拟线程上线后会被放大):同一 secUid 并发调用导致多行 INSERT。
        // MINOR-3 防御:secUid null/empty 时 fail-fast,避免锁 key 退化为 "...null" 把所有 null 请求串行化
        if (StringUtils.isEmpty(anchorUrlBo.getSecUid())) {
            throw new BusinessException(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "secUid 不能为空");
        }
        RLock lock = redissonClient.getLock(LOCK_KEY_PREFIX + anchorUrlBo.getSecUid());
        boolean locked = false;
        try {
            locked = lock.tryLock(LOCK_WAIT_SECONDS, LOCK_LEASE_SECONDS, TimeUnit.SECONDS);
            if (!locked) {
                log.warn("[主播管理] saveOrUpdateAnchor 获取锁超时,secUid={}", anchorUrlBo.getSecUid());
                throw new BusinessException(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "操作过于频繁,请稍后重试");
            }
            AnchorUrlInfoVo anchorUrlInfoVo = this.anchorUrlProducer.infoBySecUid(anchorUrlBo.getSecUid());
            if (anchorUrlInfoVo != null) {
                anchorUrlBo.setId(anchorUrlInfoVo.getId());
                this.anchorUrlProducer.update(anchorUrlBo);
            } else {
                this.anchorUrlProducer.save(anchorUrlBo);
            }
            return R.ok();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[主播管理] saveOrUpdateAnchor 锁等待被中断,secUid={}", anchorUrlBo.getSecUid(), e);
            throw new BusinessException(StatusCode.FAILED_TO_REQUEST.getCode(), "锁等待被中断");
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    public R<List<UserInfoExportVo>> exportUserInfoList(List<UserVo> userList) {
        return R.ok(anchorUrlProducer.exportUserInfoList(userList));
    }

    /**
     * 修改主播弹幕监控状态
     *
     * @param userId
     * @param tenantId
     * @param secUid              主播id
     * @param isBarrageMonitoring 开启、关闭
     * @param currentPropertyNum  当前用户拥有的弹幕监控数量
     * @return 当前用户使用的弹幕监控数量
     */
    public R<Long> updateBarrageMonitoring(Long userId, Long tenantId, String secUid, Integer isBarrageMonitoring, Long currentPropertyNum) {
        return R.ok(anchorUrlUserProducer.updateBarrageMonitoring(userId, tenantId, secUid, isBarrageMonitoring, currentPropertyNum));
    }

    /**
     * 修改用户-主播是否自动上传云空间的状态
     *
     * @param userId            用户id
     * @param secUid            主播secuid
     * @param isAutoUploadCloud 自动上传云空间 0否 1是
     * @param tenantId          租户id
     * @return
     */
    public R<String> updateAutoUploadCloud(String secUid, Integer isAutoUploadCloud, Long userId, Long tenantId) {

        anchorUrlUserProducer.updateAutoUploadCloud(userId, secUid, isAutoUploadCloud, tenantId);

        return R.ok();
    }

    /**
     * 关闭用户弹幕监控
     *
     * @param userIds
     */
    public void closeAnchorBarrageNum(List<Long> userIds) {
        anchorUrlUserProducer.closeAnchorBarrageNum(userIds);
    }

    /**
     * 关闭用户自动上传云空间
     *
     * @param userIds
     */
    public void closeAutoUploadCloud(List<Long> userIds) {
        anchorUrlUserProducer.closeAutoUploadCloud(userIds);
    }

    /**
     * 查询主播弹幕监控数量
     *
     * @param anchorUrlUserBo
     * @return
     */
    public R<Long> queryBarrageMonitoring(AnchorUrlUserBo anchorUrlUserBo) {
        return R.ok(anchorUrlUserProducer.queryBarrageMonitoring(anchorUrlUserBo));
    }

    /**
     * 绑定主播和用户的关系
     *
     * @param userAnchorBo 绑定信息
     * @return
     */
    public R<String> bindUserAnchor(UserAnchorBo userAnchorBo) {
        boolean isSuccess = anchorUrlProducer.bindUserAnchor(userAnchorBo);
        if (isSuccess) {
            return R.ok("绑定成功");
        }
        return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "绑定失败");
    }

    /**
     * 更新主播置顶信息
     *
     * @param secUid     主播secuid
     * @param action     动作 0：取消置顶 1：置顶
     * @param addTopTime 添加置顶的时间
     * @param userId     用户id
     * @param tenantId   租户id
     * @return
     */
    public R<String> updateAnchorTop(String secUid, Integer action, String addTopTime, Long userId, Long tenantId) {

        anchorUrlUserProducer.updateAnchorTop(userId, tenantId, secUid, action, addTopTime);

        return R.ok();
    }

    /**
     * 更新主播最后开始录制时间
     *
     * @param secUid         主播secuid
     * @param lastRecordTime 最后开始录制时间
     * @param userId         用户id
     * @param tenantId       租户id
     * @return
     */
    public R<String> updateAnchorLastRecordTime(String secUid, String lastRecordTime, Long userId, Long tenantId) {

        anchorUrlUserProducer.updateAnchorLastRecordTime(userId, secUid, lastRecordTime, tenantId);

        return R.ok();
    }

    /**
     * 客户端获取AI复盘主播列表
     *
     * @param clientAnchorListBo 查询参数
     * @return
     */
    public R<PageUtils<AnchorRecordListVo>> clientAnchorRecordList(ClientAnchorListBo clientAnchorListBo) {

        if (!StringUtils.isEmpty(clientAnchorListBo.getAnchorName())) {
            List<AnchorUrlInfoVo> anchorUrlInfoVos = this.anchorUrlProducer.listByLikeName(clientAnchorListBo.getAnchorName());
            if (anchorUrlInfoVos != null && anchorUrlInfoVos.size() > 0) {
                List<String> secUids = anchorUrlInfoVos.stream().map(AnchorUrlInfoVo::getSecUid).collect(Collectors.toList());
                clientAnchorListBo.setSecUidArr(secUids);
            } else {
                List<String> secUids = new LinkedList<>();
                secUids.add("0");
                clientAnchorListBo.setSecUidArr(secUids);
            }
        }

        PageUtils<AnchorRecordListVo> pageUtils = anchorUrlUserProducer.clientVideoList(clientAnchorListBo);

        List<AnchorRecordListVo> anchorInfoVoList = pageUtils.getList();
        if (anchorInfoVoList != null && anchorInfoVoList.size() > 0) {
            // 获取主播列表
            List<String> secUids = anchorInfoVoList.stream().map(AnchorUrlUserVo::getAnchorUrlSecUid).collect(Collectors.toList());
            List<AnchorUrlInfoVo> anchorUrlInfoVos = this.anchorUrlProducer.listAnchorByUserIdAndSecUids(clientAnchorListBo.getUserId(), clientAnchorListBo.getTenantId(), secUids);
            // 获取视频信息
            if(clientAnchorListBo.getVideoSliceType() == null) {
                clientAnchorListBo.setVideoSliceType(0);
            }
            List<AnchorVideoInfoVo> videoInfoVos = this.anchorVideoProducer.listByAnchorRecordList(secUids, clientAnchorListBo.getUserId(), clientAnchorListBo.getTenantId(), 0, clientAnchorListBo.getVideoSliceType());

            LocalDate today = LocalDate.now();
            for (AnchorRecordListVo anchorInfoVo : anchorInfoVoList) {
                // 设置主播信息
                if (anchorUrlInfoVos != null && anchorUrlInfoVos.size() > 0) {
                    for (AnchorUrlInfoVo anchorUrlInfoVo : anchorUrlInfoVos) {
                        if (anchorUrlInfoVo.getSecUid().equals(anchorInfoVo.getAnchorUrlSecUid())) {
                            anchorInfoVo.setAnchorInfo(anchorUrlInfoVo);
                            break;
                        }
                    }
                }
                // 统计视频数量
                anchorInfoVo.setRecordTotal(0);
                anchorInfoVo.setRecordTodayTotal(0);
                if (videoInfoVos != null && videoInfoVos.size() > 0) {
                    for (AnchorVideoInfoVo videoInfoVo : videoInfoVos) {
                        if (videoInfoVo.getSecUid().equals(anchorInfoVo.getAnchorUrlSecUid())) {
                            // 统计总数
                            anchorInfoVo.setRecordTotal(anchorInfoVo.getRecordTotal() + 1);
                            // 统计今天视频数
                            LocalDate videoDate;
                            if(videoInfoVo.getVideoSliceType().equals(VideoSliceTypeEnum.VIDEO.getCode())) {
                                videoDate = videoInfoVo.getStartTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                            }else {
                                videoDate = videoInfoVo.getCreateDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                            }
                            if (today.equals(videoDate)) {
                                anchorInfoVo.setRecordTodayTotal(anchorInfoVo.getRecordTodayTotal() + 1);
                            }
                        }
                    }
                }
            }

            // 剔除已从录制列表删除，并且视频数量为0的主播
            List<AnchorRecordListVo> listVos = anchorInfoVoList.stream().filter(item -> {
                return item.getAnchorInfo() != null && (item.getIsRemoveRecord() == 0 || (item.getIsRemoveRecord() == 1 && item.getRecordTotal() > 0));
            }).collect(Collectors.toList());

            pageUtils.setList(listVos);
            pageUtils.setTotalCount(listVos.size());
        }

        return R.ok(pageUtils);
    }

    /**
     * 置顶主播
     *
     * @param topAnchorBo 置顶主播参数
     * @return
     */
    public R<String> topAnchor(TopAnchorBo topAnchorBo) {

        String addTopTime = "";
        if (topAnchorBo.getAction() == 1) {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            addTopTime = now.format(formatter);
        }
        Boolean result = this.anchorUrlUserProducer.updateAnchorTop(topAnchorBo.getUserId(), topAnchorBo.getTenantId(), topAnchorBo.getSecUid(), topAnchorBo.getAction(), addTopTime);
        if (result) {
            return R.ok();
        }

        return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "主播不存在");

    }

    /**
     * 根据secUid获取用户主播信息
     *
     * @param secUid   主播SecUid
     * @param userId   用户id
     * @param tenantId 租户id
     * @return
     */
    public R<AnchorUrlUserVo> getUserAnchorBySecUid(String secUid, Long userId, Long tenantId) {

        AnchorUrlUserVo anchorUrlUserVo = this.anchorUrlUserProducer.getUserAnchorBySecUid(secUid, userId, tenantId);
        if (anchorUrlUserVo != null) {
            AnchorUrlInfoVo anchorUrlInfoVo = this.anchorUrlProducer.infoBySecUid(secUid);
            anchorUrlUserVo.setAnchorInfo(anchorUrlInfoVo);

            // 设置基础信息
            BasicSettingsVo basicSettingsVo = basicSettingsProducer.getBySourceUser(secUid, WordsEnum.basicSettingsType.ANCHOR.getCode(), userId, tenantId);
            if (basicSettingsVo != null) {
                //  赋值
                BeanUtil.copyProperties(
                        basicSettingsVo,
                        anchorUrlUserVo,
                        CopyOptions.create().setIgnoreNullValue(true).setIgnoreProperties("id", "sourceId", "sourceType", "userId", "tenantId", "createDate", "updateDate")
                );
            }

            return R.ok(anchorUrlUserVo);
        }

        return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "主播不存在");
    }

    /**
     * 客户端获取主播列表
     *
     * @param userId   用户id
     * @param tenantId 租户id
     * @return
     */
    public R<List<AnchorUrlUserVo>> clientAnchorList(Long userId, Long tenantId) {

        List<AnchorUrlUserVo> anchorUrlUserVos = this.anchorUrlUserProducer.listByUser(userId, tenantId);

        return R.ok(anchorUrlUserVos);

    }

    /**
     * 根据主播secuid集合获取昨日和前日录制场次列表
     *
     * @param secUidList 主播secuid集合
     * @param userId     用户id
     * @param tenantId   租户id
     * @return
     */
    public R<List<AnchorYesterdayRecordVo>> listAnchorYesterdayRecord(List<String> secUidList, Long userId, Long tenantId) {

        if (secUidList == null || secUidList.isEmpty()) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "主播数据不存在");
        }

        List<String> cacheNotExistSecUidList = new LinkedList<>(secUidList);
        List<AnchorYesterdayRecordVo> resultList = new LinkedList<>();

        // 从缓存中获取合并数据（包含昨日和前日）
        String cacheKey = wordsProperties.getYesterdayRecordRedisKey() + tenantId + ":" + userId;
        Iterator<String> iterator = cacheNotExistSecUidList.iterator();
        while (iterator.hasNext()) {
            String secUid = iterator.next();
            Object obj = this.redisTemplate.opsForHash().get(cacheKey, secUid);
            if (obj != null) {
                AnchorYesterdayRecordVo anchorRecordVo = JSONObject.parseObject(obj.toString(), AnchorYesterdayRecordVo.class);
                resultList.add(anchorRecordVo);
                // 缓存中有，从secUid集合剔除
                iterator.remove();
            }
        }

        // 封装缓存中不存在的数据（包含昨日和前日）
        if (cacheNotExistSecUidList.size() > 0) {
            // 获取主播列表
            List<AnchorUrlUserVo> anchorUrlUserVos = this.anchorUrlUserProducer.listBySecUids(cacheNotExistSecUidList, userId, tenantId);

            if (anchorUrlUserVos != null && anchorUrlUserVos.size() > 0) {
                List<String> secUids = anchorUrlUserVos.stream().map(AnchorUrlUserVo::getAnchorUrlSecUid).toList();

                // 获取昨日录制视频列表
                List<AnchorVideoInfoVo> yesterdayVideoInfoVos = this.anchorVideoProducer.listYesterdayBySecUids(secUids, userId, tenantId);
                // 获取前日录制视频列表
                List<AnchorVideoInfoVo> dayBeforeVideoInfoVos = this.anchorVideoProducer.listDayBeforeYesterdayBySecUids(secUids, userId, tenantId);

                // 获取昨日视频关联的看盘数据
                List<VideoDataViewingConfuseInfoVo> yesterdayVideoDataViewingConfuseInfoVos;
                if (yesterdayVideoInfoVos != null && yesterdayVideoInfoVos.size() > 0) {
                    List<String> videoIds = yesterdayVideoInfoVos.stream().map(AnchorVideoInfoVo::getVideoId).collect(Collectors.toList());
                    yesterdayVideoDataViewingConfuseInfoVos = this.videoDataViewingConfuseProducer.listByVideoIds(videoIds);
                } else {
                    yesterdayVideoDataViewingConfuseInfoVos = new LinkedList<>();
                }

                // 获取前日视频关联的看盘数据
                List<VideoDataViewingConfuseInfoVo> dayBeforeVideoDataViewingConfuseInfoVos;
                if (dayBeforeVideoInfoVos != null && dayBeforeVideoInfoVos.size() > 0) {
                    List<String> videoIds = dayBeforeVideoInfoVos.stream().map(AnchorVideoInfoVo::getVideoId).collect(Collectors.toList());
                    dayBeforeVideoDataViewingConfuseInfoVos = this.videoDataViewingConfuseProducer.listByVideoIds(videoIds);
                } else {
                    dayBeforeVideoDataViewingConfuseInfoVos = new LinkedList<>();
                }

                List<AnchorYesterdayRecordVo> anchorRecordVos = anchorUrlUserVos.stream().map(anchor -> {
                    AnchorYesterdayRecordVo anchorRecordVo = new AnchorYesterdayRecordVo();
                    anchorRecordVo.setSecUid(anchor.getAnchorUrlSecUid());

                    // 封装主播的昨日录制列表
                    packageYesterdayRecord(anchorRecordVo, yesterdayVideoInfoVos, anchor, yesterdayVideoDataViewingConfuseInfoVos);
                    // 封装主播的前日录制列表
                    packageDayBeforeRecord(anchorRecordVo, dayBeforeVideoInfoVos, anchor, dayBeforeVideoDataViewingConfuseInfoVos);

                    return anchorRecordVo;
                }).toList();

                // ----添加到缓存----
                // 获取今天23:59:59的时间戳
                long endOfDayTimestamp = LocalDate.now().atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                // 计算当前时间到今天23:59:59的秒数
                long differenceSecond = (endOfDayTimestamp - System.currentTimeMillis()) / 1000;
                // 添加到缓存
                for (AnchorYesterdayRecordVo anchorRecordVo : anchorRecordVos) {
                    this.redisTemplate.opsForHash().put(cacheKey, anchorRecordVo.getSecUid(), JSON.toJSONString(anchorRecordVo));
                }
                this.redisTemplate.expire(cacheKey, Duration.ofSeconds(differenceSecond));

                // 添加到结果集合
                resultList.addAll(anchorRecordVos);
            }
        }

        if (resultList.size() > 0) {
            return R.ok(resultList);
        }

        return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "主播数据不存在");
    }

    /**
     * 封装主播的昨日录制列表数据
     *
     * @param anchorYesterdayRecordVo        结果数据对象
     * @param videoInfoVos                   视频列表
     * @param anchor                         主播信息
     * @param videoDataViewingConfuseInfoVos 数据看板列表
     * @return
     */
    private List<AnchorYesterdayRecordItemVo> packageYesterdayRecord(AnchorYesterdayRecordVo anchorYesterdayRecordVo, List<AnchorVideoInfoVo> videoInfoVos, AnchorUrlUserVo anchor, List<VideoDataViewingConfuseInfoVo> videoDataViewingConfuseInfoVos) {

        List<AnchorYesterdayRecordItemVo> yesterdayRecordList = new LinkedList<>();

        if (videoInfoVos != null && videoInfoVos.size() > 0) {
            for (AnchorVideoInfoVo videoInfoVo : videoInfoVos) {
                if (videoInfoVo.getSecUid().equals(anchor.getAnchorUrlSecUid())) {
                    // 封装视频信息
                    AnchorYesterdayRecordItemVo anchorYesterdayRecordItemVo = new AnchorYesterdayRecordItemVo();
                    anchorYesterdayRecordItemVo.setRecordDate(videoInfoVo.getStartTime());
                    anchorYesterdayRecordItemVo.setVideoId(videoInfoVo.getVideoId());
                    anchorYesterdayRecordItemVo.setBatchNumber(videoInfoVo.getBatchNumber());
                    anchorYesterdayRecordItemVo.setEndTime(videoInfoVo.getEndTime());
                    // 封装看盘信息
                    if (anchor.getIsDataViewing() != null && anchor.getIsDataViewing() == 0) {
                        // 未开启数据看板
                        anchorYesterdayRecordItemVo.setObservationNum(-1);
                        anchorYesterdayRecordItemVo.setVolumeStart(-1);
                        anchorYesterdayRecordItemVo.setVolumeEnd(-1);
                    } else {
                        // 视频时长不足50分钟
                        anchorYesterdayRecordItemVo.setObservationNum(-2);
                        anchorYesterdayRecordItemVo.setVolumeStart(-2);
                        anchorYesterdayRecordItemVo.setVolumeEnd(-2);
                    }
                    if (videoDataViewingConfuseInfoVos != null && videoDataViewingConfuseInfoVos.size() > 0) {
                        for (VideoDataViewingConfuseInfoVo videoDataViewingConfuseInfoVo : videoDataViewingConfuseInfoVos) {
                            if (videoDataViewingConfuseInfoVo.getVideoId().equals(videoInfoVo.getVideoId())) {
                                if (videoDataViewingConfuseInfoVo.getTotalWatchNum() != null) {
                                    anchorYesterdayRecordItemVo.setObservationNum(videoDataViewingConfuseInfoVo.getTotalWatchNum());
                                }
                                if (videoDataViewingConfuseInfoVo.getVolumeStart() != null) {
                                    anchorYesterdayRecordItemVo.setVolumeStart(videoDataViewingConfuseInfoVo.getVolumeStart());
                                }
                                if (videoDataViewingConfuseInfoVo.getVolumeEnd() != null) {
                                    anchorYesterdayRecordItemVo.setVolumeEnd(videoDataViewingConfuseInfoVo.getVolumeEnd());
                                }

                            }
                        }
                    }

                    yesterdayRecordList.add(anchorYesterdayRecordItemVo);
                }
            }
        }
        anchorYesterdayRecordVo.setYesterdayRecordList(yesterdayRecordList);
        anchorYesterdayRecordVo.setYesterdayRecordNum(yesterdayRecordList.size());

        // 封装平均场观和销售额
        anchorYesterdayRecordVo.setYesterdayAverageObservationNum(0);
        anchorYesterdayRecordVo.setYesterdayAverageVolumeStart(0);
        anchorYesterdayRecordVo.setYesterdayAverageVolumeEnd(0);
        if (yesterdayRecordList.size() > 0) {
            // 按录制结束时间降序排序
            yesterdayRecordList.sort(Comparator.comparing(AnchorYesterdayRecordItemVo::getEndTime).reversed());
            // 根据场次号筛选出最后一场数据
            List<AnchorYesterdayRecordItemVo> resultObservationList = new LinkedList<>();
            List<AnchorYesterdayRecordItemVo> resultVolumeStartList = new LinkedList<>();
            List<AnchorYesterdayRecordItemVo> resultVolumeEndList = new LinkedList<>();

            for (AnchorYesterdayRecordItemVo anchorYesterdayRecordItemVo : yesterdayRecordList) {
                // 筛选场观数据
                filterRecordData(resultObservationList, anchorYesterdayRecordItemVo, anchorYesterdayRecordItemVo.getObservationNum());
                // 筛选销售额-起始数据
                filterRecordData(resultVolumeStartList, anchorYesterdayRecordItemVo, anchorYesterdayRecordItemVo.getVolumeStart());
                // 筛选销售额-结尾数据
                filterRecordData(resultVolumeEndList, anchorYesterdayRecordItemVo, anchorYesterdayRecordItemVo.getVolumeEnd());
            }

            int effectiveVolumeStartCount = 0;
            int effectiveVolumeEndCount = 0;
            int effectiveObservationCount = 0;

            int effectiveVolumeStartSum = 0;
            int effectiveVolumeEndSum = 0;
            int effectiveObservationSum = 0;

            for (AnchorYesterdayRecordItemVo anchorYesterdayRecordItemVo : resultObservationList) {
                effectiveObservationCount = resultObservationList.size();
                effectiveObservationSum += anchorYesterdayRecordItemVo.getObservationNum();
            }
            for (AnchorYesterdayRecordItemVo anchorYesterdayRecordItemVo : resultVolumeStartList) {
                effectiveVolumeStartCount = resultVolumeStartList.size();
                effectiveVolumeStartSum += anchorYesterdayRecordItemVo.getVolumeStart();
            }
            for (AnchorYesterdayRecordItemVo anchorYesterdayRecordItemVo : resultVolumeEndList) {
                effectiveVolumeEndCount = resultVolumeEndList.size();
                effectiveVolumeEndSum += anchorYesterdayRecordItemVo.getVolumeEnd();
            }

            if (effectiveVolumeStartCount > 0) {
                anchorYesterdayRecordVo.setYesterdayAverageVolumeStart(effectiveVolumeStartSum / effectiveVolumeStartCount);
            }
            if (effectiveVolumeEndCount > 0) {
                anchorYesterdayRecordVo.setYesterdayAverageVolumeEnd(effectiveVolumeEndSum / effectiveVolumeEndCount);
            }
            if (effectiveObservationCount > 0) {
                anchorYesterdayRecordVo.setYesterdayAverageObservationNum(effectiveObservationSum / effectiveObservationCount);
            }
        }

        return yesterdayRecordList;
    }

    /**
     * 封装主播的前日录制列表数据
     *
     * @param anchorYesterdayRecordVo        结果数据对象
     * @param videoInfoVos                   视频列表
     * @param anchor                         主播信息
     * @param videoDataViewingConfuseInfoVos 数据看板列表
     * @return
     */
    private List<AnchorYesterdayRecordItemVo> packageDayBeforeRecord(AnchorYesterdayRecordVo anchorYesterdayRecordVo, List<AnchorVideoInfoVo> videoInfoVos, AnchorUrlUserVo anchor, List<VideoDataViewingConfuseInfoVo> videoDataViewingConfuseInfoVos) {

        List<AnchorYesterdayRecordItemVo> dayBeforeRecordList = new LinkedList<>();

        if (videoInfoVos != null && videoInfoVos.size() > 0) {
            for (AnchorVideoInfoVo videoInfoVo : videoInfoVos) {
                if (videoInfoVo.getSecUid().equals(anchor.getAnchorUrlSecUid())) {
                    // 封装视频信息
                    AnchorYesterdayRecordItemVo anchorDayBeforeRecordItemVo = new AnchorYesterdayRecordItemVo();
                    anchorDayBeforeRecordItemVo.setRecordDate(videoInfoVo.getStartTime());
                    anchorDayBeforeRecordItemVo.setVideoId(videoInfoVo.getVideoId());
                    anchorDayBeforeRecordItemVo.setBatchNumber(videoInfoVo.getBatchNumber());
                    anchorDayBeforeRecordItemVo.setEndTime(videoInfoVo.getEndTime());
                    // 封装看盘信息
                    if (anchor.getIsDataViewing() != null && anchor.getIsDataViewing() == 0) {
                        // 未开启数据看板
                        anchorDayBeforeRecordItemVo.setObservationNum(-1);
                        anchorDayBeforeRecordItemVo.setVolumeStart(-1);
                        anchorDayBeforeRecordItemVo.setVolumeEnd(-1);
                    } else {
                        // 视频时长不足50分钟
                        anchorDayBeforeRecordItemVo.setObservationNum(-2);
                        anchorDayBeforeRecordItemVo.setVolumeStart(-2);
                        anchorDayBeforeRecordItemVo.setVolumeEnd(-2);
                    }
                    if (videoDataViewingConfuseInfoVos != null && videoDataViewingConfuseInfoVos.size() > 0) {
                        for (VideoDataViewingConfuseInfoVo videoDataViewingConfuseInfoVo : videoDataViewingConfuseInfoVos) {
                            if (videoDataViewingConfuseInfoVo.getVideoId().equals(videoInfoVo.getVideoId())) {
                                if (videoDataViewingConfuseInfoVo.getTotalWatchNum() != null) {
                                    anchorDayBeforeRecordItemVo.setObservationNum(videoDataViewingConfuseInfoVo.getTotalWatchNum());
                                }
                                if (videoDataViewingConfuseInfoVo.getVolumeStart() != null) {
                                    anchorDayBeforeRecordItemVo.setVolumeStart(videoDataViewingConfuseInfoVo.getVolumeStart());
                                }
                                if (videoDataViewingConfuseInfoVo.getVolumeEnd() != null) {
                                    anchorDayBeforeRecordItemVo.setVolumeEnd(videoDataViewingConfuseInfoVo.getVolumeEnd());
                                }
                            }
                        }
                    }

                    dayBeforeRecordList.add(anchorDayBeforeRecordItemVo);
                }
            }
        }
        anchorYesterdayRecordVo.setDayBeforeRecordList(dayBeforeRecordList);
        anchorYesterdayRecordVo.setDayBeforeRecordNum(dayBeforeRecordList.size());

        // 封装平均场观和销售额
        anchorYesterdayRecordVo.setDayBeforeAverageObservationNum(0);
        anchorYesterdayRecordVo.setDayBeforeAverageVolumeStart(0);
        anchorYesterdayRecordVo.setDayBeforeAverageVolumeEnd(0);
        if (dayBeforeRecordList.size() > 0) {
            // 按录制结束时间降序排序
            dayBeforeRecordList.sort(Comparator.comparing(AnchorYesterdayRecordItemVo::getEndTime).reversed());
            // 根据场次号筛选出最后一场数据
            List<AnchorYesterdayRecordItemVo> resultObservationList = new LinkedList<>();
            List<AnchorYesterdayRecordItemVo> resultVolumeStartList = new LinkedList<>();
            List<AnchorYesterdayRecordItemVo> resultVolumeEndList = new LinkedList<>();

            for (AnchorYesterdayRecordItemVo anchorDayBeforeRecordItemVo : dayBeforeRecordList) {
                // 筛选场观数据
                filterRecordData(resultObservationList, anchorDayBeforeRecordItemVo, anchorDayBeforeRecordItemVo.getObservationNum());
                // 筛选销售额-起始数据
                filterRecordData(resultVolumeStartList, anchorDayBeforeRecordItemVo, anchorDayBeforeRecordItemVo.getVolumeStart());
                // 筛选销售额-结尾数据
                filterRecordData(resultVolumeEndList, anchorDayBeforeRecordItemVo, anchorDayBeforeRecordItemVo.getVolumeEnd());
            }

            int effectiveVolumeStartCount = 0;
            int effectiveVolumeEndCount = 0;
            int effectiveObservationCount = 0;

            int effectiveVolumeStartSum = 0;
            int effectiveVolumeEndSum = 0;
            int effectiveObservationSum = 0;

            for (AnchorYesterdayRecordItemVo anchorDayBeforeRecordItemVo : resultObservationList) {
                effectiveObservationCount = resultObservationList.size();
                effectiveObservationSum += anchorDayBeforeRecordItemVo.getObservationNum();
            }
            for (AnchorYesterdayRecordItemVo anchorDayBeforeRecordItemVo : resultVolumeStartList) {
                effectiveVolumeStartCount = resultVolumeStartList.size();
                effectiveVolumeStartSum += anchorDayBeforeRecordItemVo.getVolumeStart();
            }
            for (AnchorYesterdayRecordItemVo anchorDayBeforeRecordItemVo : resultVolumeEndList) {
                effectiveVolumeEndCount = resultVolumeEndList.size();
                effectiveVolumeEndSum += anchorDayBeforeRecordItemVo.getVolumeEnd();
            }

            if (effectiveVolumeStartCount > 0) {
                anchorYesterdayRecordVo.setDayBeforeAverageVolumeStart(effectiveVolumeStartSum / effectiveVolumeStartCount);
            }
            if (effectiveVolumeEndCount > 0) {
                anchorYesterdayRecordVo.setDayBeforeAverageVolumeEnd(effectiveVolumeEndSum / effectiveVolumeEndCount);
            }
            if (effectiveObservationCount > 0) {
                anchorYesterdayRecordVo.setDayBeforeAverageObservationNum(effectiveObservationSum / effectiveObservationCount);
            }
        }

        return dayBeforeRecordList;
    }

    /**
     * 根据场次号筛选出最后一场数据
     *
     * @param resultList                  结果集合
     * @param anchorYesterdayRecordItemVo 昨日数据对象
     */
    private void filterRecordData(List<AnchorYesterdayRecordItemVo> resultList, AnchorYesterdayRecordItemVo anchorYesterdayRecordItemVo, Integer value) {
        if (value != -1 && value != -2) {
            boolean exist = false;
            for (AnchorYesterdayRecordItemVo resultItemVo : resultList) {
                if (resultItemVo.getBatchNumber().equals(anchorYesterdayRecordItemVo.getBatchNumber())) {
                    exist = true;
                    break;
                }
            }
            if (!exist) {
                resultList.add(anchorYesterdayRecordItemVo);
            }
        }
    }

    /**
     * 添加或修改用户的主播信息
     *
     * @param addOrUpdateAnchorBo 添加或修改用户的主播信息参数
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public R<String> addOrUpdateAnchor(AddOrUpdateAnchorBo addOrUpdateAnchorBo) {

        if (StringUtils.isEmpty(addOrUpdateAnchorBo.getSecUid())) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "主播secUid不能为空");
        }

        AnchorUrlInfoVo oldAnchorUrlInfoVo = anchorUrlProducer.infoBySecUid(addOrUpdateAnchorBo.getSecUid());
        if (oldAnchorUrlInfoVo == null) {
            // 保存主播信息
            AnchorUrlBo anchorUrlBo = new AnchorUrlBo();
            BeanUtils.copyProperties(addOrUpdateAnchorBo, anchorUrlBo);
            if(StringUtils.isEmpty(anchorUrlBo.getAnchorNumber())) {
                String anchorNumber = anchorUrlBo.getLiveUrl().substring(anchorUrlBo.getLiveUrl().lastIndexOf("/") + 1);
                anchorUrlBo.setAnchorNumber(anchorNumber);
            }

            oldAnchorUrlInfoVo = anchorUrlProducer.save(anchorUrlBo);

            AnchorUrlDetailsEntity entity = new AnchorUrlDetailsEntity();
            entity.setSecUid(oldAnchorUrlInfoVo.getSecUid());
            entity.setKeywordStatus(0);
            anchorUrlDetailsBll.saveOrUpdate(entity);
        } else {

            boolean change = false;
            AnchorUrlBo anchorUrlBo = new AnchorUrlBo();
            BeanUtils.copyProperties(oldAnchorUrlInfoVo, anchorUrlBo);
            // 名称已经改变，更新主播名称
            if (!StringUtils.isEmpty(addOrUpdateAnchorBo.getAnchorName()) && !oldAnchorUrlInfoVo.getAnchorNumber().equals(addOrUpdateAnchorBo.getAnchorName())) {
                anchorUrlBo.setAnchorName(addOrUpdateAnchorBo.getAnchorName());
                change = true;
            }
            // 头像已经改变，更新主播头像
            if (!StringUtils.isEmpty(addOrUpdateAnchorBo.getAnchorAvatar()) && !oldAnchorUrlInfoVo.getAnchorAvatar().equals(addOrUpdateAnchorBo.getAnchorAvatar())) {
                anchorUrlBo.setAnchorAvatar(addOrUpdateAnchorBo.getAnchorAvatar());
                change = true;
            }
            if (change) {
                this.anchorUrlProducer.update(anchorUrlBo);
            }
        }

        AnchorUrlUserVo userAnchorInfo = this.anchorUrlUserProducer.getUserAnchorBySecUid(addOrUpdateAnchorBo.getSecUid(), addOrUpdateAnchorBo.getUserId(), addOrUpdateAnchorBo.getTenantId());

        // 查询当前 anchor_url_user 记录（用于读取 accountType 和三开关现有值）
        AnchorUrlUserEntity currentAnchorUser = null;
        if (userAnchorInfo != null) {
            currentAnchorUser = anchorUrlUserService.getOne(
                    new LambdaQueryWrapper<AnchorUrlUserEntity>()
                            .eq(AnchorUrlUserEntity::getUserId, addOrUpdateAnchorBo.getUserId())
                            .eq(AnchorUrlUserEntity::getTenantId, addOrUpdateAnchorBo.getTenantId())
                            .eq(AnchorUrlUserEntity::getAnchorUrlSecUid, addOrUpdateAnchorBo.getSecUid())
                            .eq(AnchorUrlUserEntity::getIsRemoveRecord, 0)
            );
        }

        // Step 1：账号切换保护 — 自有账号（accountType=0）改为非自有，且任一 AI 监控开关已开启时拒绝
        if (currentAnchorUser != null
                && addOrUpdateAnchorBo.getAccountType() != null
                && currentAnchorUser.getAccountType() != null
                && Objects.equals(currentAnchorUser.getAccountType(), 0)
                && !Objects.equals(addOrUpdateAnchorBo.getAccountType(), 0)) {
            Integer cq = currentAnchorUser.getIsScriptQualityInspection();
            Integer cf = currentAnchorUser.getIsScriptFidelityMonitor();
            Integer ci = currentAnchorUser.getIsInteractionPatrol();
            if ((cq != null && cq == 1) || (cf != null && cf == 1) || (ci != null && ci == 1)) {
                log.warn("[B6 账号切换保护] accountType=0→非0 但 AI 监控开关已开 userId={} tenantId={} secUid={} errorCode=70007",
                        addOrUpdateAnchorBo.getUserId(), addOrUpdateAnchorBo.getTenantId(), addOrUpdateAnchorBo.getSecUid());
                throw new BusinessException(StatusCode.SCRIPT_MONITOR_ACCOUNT_TYPE_LOCKED.getCode(),
                        StatusCode.SCRIPT_MONITOR_ACCOUNT_TYPE_LOCKED.getMsg());
            }
        }

        // Step 2：还原度开关 0→1 校验（Slice A 解除暂禁，2026-06-11）
        Integer currentFidelity = (currentAnchorUser != null) ? currentAnchorUser.getIsScriptFidelityMonitor() : null;
        Integer requestFidelity = addOrUpdateAnchorBo.getIsScriptFidelityMonitor();
        if (requestFidelity != null && requestFidelity == 1
                && (currentFidelity == null || currentFidelity == 0)) {
            // 校验 accountType=0（自有账号）；新增直播间场景 currentAnchorUser 为 null，fallback 到 bo 的入参
            Integer accountType = (currentAnchorUser != null)
                    ? currentAnchorUser.getAccountType()
                    : addOrUpdateAnchorBo.getAccountType();
            if (Integer.valueOf(1).equals(accountType)) {
                log.warn("[B3 还原度校验] 竞品/同行账号拒绝开启还原度 userId={} tenantId={} secUid={} errorCode=70004",
                        addOrUpdateAnchorBo.getUserId(), addOrUpdateAnchorBo.getTenantId(), addOrUpdateAnchorBo.getSecUid());
                throw new BusinessException(StatusCode.SCRIPT_MONITOR_NOT_OWN_ACCOUNT.getCode(),
                        StatusCode.SCRIPT_MONITOR_NOT_OWN_ACCOUNT.getMsg());
            }
            // 按 (tenantId+userId+secUid) 查标准稿，无则抛 70005
            StandardScriptEntity standardScript = standardScriptService.findValid(
                    addOrUpdateAnchorBo.getTenantId(), addOrUpdateAnchorBo.getUserId(), addOrUpdateAnchorBo.getSecUid());
            if (standardScript == null) {
                log.warn("[B3 还原度校验] 无已确认标准稿，请先确认标准稿 userId={} tenantId={} secUid={} errorCode=70005",
                        addOrUpdateAnchorBo.getUserId(), addOrUpdateAnchorBo.getTenantId(), addOrUpdateAnchorBo.getSecUid());
                throw new BusinessException(StatusCode.SCRIPT_MONITOR_STANDARD_SCRIPT_UNCONFIRMED.getCode(),
                        StatusCode.SCRIPT_MONITOR_STANDARD_SCRIPT_UNCONFIRMED.getMsg());
            }
            // 回填辅助索引：standardScriptId → tb_anchor_url_user.standard_script_id
            addOrUpdateAnchorBo.setStandardScriptId(standardScript.getId());
            log.info("[B3 还原度校验] 通过，回填 standardScriptId={} userId={} tenantId={} secUid={}",
                    standardScript.getId(), addOrUpdateAnchorBo.getUserId(), addOrUpdateAnchorBo.getTenantId(), addOrUpdateAnchorBo.getSecUid());
        }

        // Step 3：监控位授权量 + Token 校验（仅质检和巡检，0→1 时校验；还原度已在 Step 2 拦截）
        Map<String, String> monitorCodeMap = new LinkedHashMap<>();
        monitorCodeMap.put("isScriptQualityInspection", OrderEnums.commodityTypeCode.SCRIPT_QUALITY_NUM.getCode());
        monitorCodeMap.put("isInteractionPatrol", OrderEnums.commodityTypeCode.INTERACTION_PATROL_NUM.getCode());

        boolean anyTurnOn = false;
        for (Map.Entry<String, String> entry : monitorCodeMap.entrySet()) {
            Integer reqVal = getMonitorSwitchFromBo(addOrUpdateAnchorBo, entry.getKey());
            Integer curVal = getMonitorSwitchFromEntity(currentAnchorUser, entry.getKey());
            if (reqVal != null && reqVal == 1 && (curVal == null || curVal == 0)) {
                // 0→1：校验监控位授权量是否有剩余
                MonitorPositionAuthVo auth = userPropertyFeign.checkMonitorPosition(
                        addOrUpdateAnchorBo.getUserId(), entry.getValue());
                if (auth == null || !Boolean.TRUE.equals(auth.getHasSurplus())) {
                    log.warn("[B6 监控位不足] 开关 0→1 但授权量耗尽 userId={} tenantId={} secUid={} switchField={} errorCode=70002",
                            addOrUpdateAnchorBo.getUserId(), addOrUpdateAnchorBo.getTenantId(), addOrUpdateAnchorBo.getSecUid(), entry.getKey());
                    throw new BusinessException(StatusCode.SCRIPT_MONITOR_QUOTA_NOT_ENOUGH.getCode(),
                            StatusCode.SCRIPT_MONITOR_QUOTA_NOT_ENOUGH.getMsg());
                }
                anyTurnOn = true;
            }
        }

        // Token 余额校验（任一开关 0→1 才校验）
        if (anyTurnOn) {
            long tokenBalance = aiTokenBalance(addOrUpdateAnchorBo.getUserId());
            if (tokenBalance < 100000L) {
                log.warn("[B6 Token 不足] 任一开关 0→1 但 aiTokenNum < 100000 userId={} tenantId={} secUid={} balance={} errorCode=70001",
                        addOrUpdateAnchorBo.getUserId(), addOrUpdateAnchorBo.getTenantId(), addOrUpdateAnchorBo.getSecUid(), tokenBalance);
                throw new BusinessException(StatusCode.SCRIPT_MONITOR_TOKEN_NOT_ENOUGH.getCode(),
                        StatusCode.SCRIPT_MONITOR_TOKEN_NOT_ENOUGH.getMsg());
            }
        }

        AnchorUrlUserBo anchorUrlUserBo = new AnchorUrlUserBo();
        BeanUtils.copyProperties(addOrUpdateAnchorBo, anchorUrlUserBo);
        // 三个 AI 监控开关字段 null-skip（前端未传时不覆盖既有值，避免误关已开开关）
        if (addOrUpdateAnchorBo.getIsScriptQualityInspection() != null) {
            anchorUrlUserBo.setIsScriptQualityInspection(addOrUpdateAnchorBo.getIsScriptQualityInspection());
        }
        if (addOrUpdateAnchorBo.getIsScriptFidelityMonitor() != null) {
            anchorUrlUserBo.setIsScriptFidelityMonitor(addOrUpdateAnchorBo.getIsScriptFidelityMonitor());
        }
        if (addOrUpdateAnchorBo.getIsInteractionPatrol() != null) {
            anchorUrlUserBo.setIsInteractionPatrol(addOrUpdateAnchorBo.getIsInteractionPatrol());
        }
        if (addOrUpdateAnchorBo.getStandardScriptId() != null) {
            anchorUrlUserBo.setStandardScriptId(addOrUpdateAnchorBo.getStandardScriptId());
        }
        anchorUrlUserBo.setAnchorUrlSecUid(addOrUpdateAnchorBo.getSecUid());

        // 当修改授权巨量百应状态时，自动更新状态修改时间
        if (addOrUpdateAnchorBo.getAuthJlbyStatus() != null) {
            anchorUrlUserBo.setAuthJlbyStatusTime(new Date());
        }

        // 当修改千川授权状态时，自动更新状态修改时间
        if (addOrUpdateAnchorBo.getAuthQcStatus() != null) {
            anchorUrlUserBo.setAuthQcStatusTime(new Date());
        }

        // 当修改授权来客状态时，自动更新状态修改时间
        if (addOrUpdateAnchorBo.getAuthLifeStatus() != null) {
            anchorUrlUserBo.setAuthLifeStatusTime(new Date());
        }

        if (userAnchorInfo == null) {
            // 保存用户主播信息
            if (StringUtils.isEmpty(anchorUrlUserBo.getFolderName())) {
                anchorUrlUserBo.setFolderName(WindowsFileUtils.sanitizeForFolderName(addOrUpdateAnchorBo.getAnchorName()));
            }
            if (StringUtils.isEmpty(anchorUrlUserBo.getRemarksName())) {
                anchorUrlUserBo.setRemarksName(addOrUpdateAnchorBo.getAnchorName());
            }
            // 视频号主播 默认已授权
            if (Objects.equals(addOrUpdateAnchorBo.getPlatform(),2)) {
                anchorUrlUserBo.setAuthChannelStatus(1);
            }
            this.anchorUrlUserProducer.save(anchorUrlUserBo);
        } else {
            // 修改用户主播信息
            anchorUrlUserBo.setId(userAnchorInfo.getId());
            if (StringUtils.isEmpty(userAnchorInfo.getFolderName())) {
                anchorUrlUserBo.setFolderName(WindowsFileUtils.sanitizeForFolderName(addOrUpdateAnchorBo.getAnchorName()));
            }
            if (StringUtils.isEmpty(userAnchorInfo.getRemarksName())) {
                anchorUrlUserBo.setRemarksName(StringUtils.isEmpty(addOrUpdateAnchorBo.getAnchorName()) ? oldAnchorUrlInfoVo.getAnchorName() : addOrUpdateAnchorBo.getAnchorName());
            }
            // 授权渠道状态 修改时如果视频号状态为空则设置为以授权
            if (Objects.equals(addOrUpdateAnchorBo.getPlatform(),2) && EmptyUtil.isEmpty(anchorUrlUserBo.getAuthChannelStatus())) {
                anchorUrlUserBo.setAuthChannelStatus(1);
            }
            this.anchorUrlUserProducer.updateById(anchorUrlUserBo);
        }

        // 修改主播的基础配置
        basicSettingsProducer.updateAiPartialNew(new BasicSettingsBo(addOrUpdateAnchorBo));

        // Step 5：统计并更新三能力监控位占用量（包含还原度；因 Step 2 拒了开启，还原度计数应为 0，但关闭路径需释放）
        Map<String, String> allCodeMap = new LinkedHashMap<>();
        allCodeMap.put("isScriptQualityInspection", OrderEnums.commodityTypeCode.SCRIPT_QUALITY_NUM.getCode());
        allCodeMap.put("isScriptFidelityMonitor", OrderEnums.commodityTypeCode.SCRIPT_FIDELITY_NUM.getCode());
        allCodeMap.put("isInteractionPatrol", OrderEnums.commodityTypeCode.INTERACTION_PATROL_NUM.getCode());

        boolean hasMonitorSwitch = addOrUpdateAnchorBo.getIsScriptQualityInspection() != null
                || addOrUpdateAnchorBo.getIsScriptFidelityMonitor() != null
                || addOrUpdateAnchorBo.getIsInteractionPatrol() != null;
        if (hasMonitorSwitch) {
            for (Map.Entry<String, String> entry : allCodeMap.entrySet()) {
                String switchField = entry.getKey();
                String code = entry.getValue();
                Integer reqVal = getMonitorSwitchFromBo(addOrUpdateAnchorBo, switchField);
                Integer curVal = getMonitorSwitchFromEntity(currentAnchorUser, switchField);
                // 仅"有传值"且"实际变化"才更新占用量
                int curInt = (curVal == null) ? 0 : curVal;
                if (reqVal == null || curInt == reqVal) {
                    continue;
                }
                Long openCount = anchorUrlUserProducer.countOpenSwitch(
                        addOrUpdateAnchorBo.getUserId(), addOrUpdateAnchorBo.getTenantId(), switchField);
                boolean ok = userPropertyFeign.updateByPropertyNumRetBoolean(
                        addOrUpdateAnchorBo.getUserId(), code, openCount);
                if (!ok) {
                    log.warn("[B6 监控位占用] 更新资产失败 userId={} tenantId={} secUid={} code={} openCount={}",
                            addOrUpdateAnchorBo.getUserId(), addOrUpdateAnchorBo.getTenantId(), addOrUpdateAnchorBo.getSecUid(), code, openCount);
                    throw new RuntimeException("资产占用量更新失败, userId=" + addOrUpdateAnchorBo.getUserId() + ", code=" + code);
                }
            }
        }

        return R.ok("添加成功");
    }

    /**
     * 计算用户 AI Token 余额（totalQuantity - useQuantity）
     *
     * @param userId 用户 ID
     * @return Token 余额，不足时返回 0
     */
    private long aiTokenBalance(Long userId) {
        List<UserPropertyTypeInfoVo> props = userPropertyFeign.getUserProperty(userId);
        if (props == null || props.isEmpty()) {
            return 0L;
        }
        String aiTokenCode = OrderEnums.commodityTypeCode.AI_TOKEN_NUM.getCode();
        return props.stream()
                .filter(p -> aiTokenCode.equals(p.getCommodityTypeCode()))
                .findAny()
                .map(p -> Math.max(0L, safeLong(p.getTotalQuantity()) - safeLong(p.getUseQuantity())))
                .orElse(0L);
    }

    /**
     * 安全转换 Long，null 时返回 0
     *
     * @param value 待转换值
     * @return long 值
     */
    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    /**
     * 从 AddOrUpdateAnchorBo 中读取指定开关字段值
     *
     * @param bo        请求 Bo
     * @param fieldName 字段名
     * @return 开关值，字段不匹配时返回 null
     */
    private Integer getMonitorSwitchFromBo(AddOrUpdateAnchorBo bo, String fieldName) {
        switch (fieldName) {
            case "isScriptQualityInspection": return bo.getIsScriptQualityInspection();
            case "isScriptFidelityMonitor": return bo.getIsScriptFidelityMonitor();
            case "isInteractionPatrol": return bo.getIsInteractionPatrol();
            default: return null;
        }
    }

    /**
     * 从 AnchorUrlUserEntity 中读取指定开关字段当前值
     *
     * @param entity    数据库实体，可为 null（新增场景）
     * @param fieldName 字段名
     * @return 当前开关值，entity 为 null 时返回 null
     */
    private Integer getMonitorSwitchFromEntity(AnchorUrlUserEntity entity, String fieldName) {
        if (entity == null) {
            return null;
        }
        switch (fieldName) {
            case "isScriptQualityInspection": return entity.getIsScriptQualityInspection();
            case "isScriptFidelityMonitor": return entity.getIsScriptFidelityMonitor();
            case "isInteractionPatrol": return entity.getIsInteractionPatrol();
            default: return null;
        }
    }

    /**
     * 根据租户id获取主播列表
     *
     * @param tenantId 租户id
     * @return
     */
    public R<List<AnchorUrlUserVo>> listByTenantId(Long tenantId) {

        List<AnchorUrlUserVo> anchorUrlUserVoList = this.anchorUrlUserProducer.listByTenantId(tenantId);

        if (anchorUrlUserVoList != null && anchorUrlUserVoList.size() > 0) {
            List<AnchorUrlUserVo> resultList = new LinkedList<>();
            for (AnchorUrlUserVo anchorUrlUserVo : anchorUrlUserVoList) {
                boolean exist = false;
                for (AnchorUrlUserVo urlUserVo : resultList) {
                    if (anchorUrlUserVo.getAnchorUrlSecUid().equals(urlUserVo.getAnchorUrlSecUid())) {
                        exist = true;
                        break;
                    }
                }

                if (!exist) {
                    resultList.add(anchorUrlUserVo);
                }
            }

            return R.ok(resultList);
        }

        return R.ok();
    }

    public List<Long> selectQuery(AnchorUrlBo anchorUrlBo) {
        return anchorUrlProducer.selectQuery(anchorUrlBo);
    }

    public Map<Long, Long> countIHave(List<Long> longList) {
        return anchorUrlProducer.countIHave(longList);
    }

    /**
     * 新添加的主播后-自动打开监控位
     *
     * @param secUid 主播secUid
     * @return 返回开启的监控位
     */
    @Transactional(rollbackFor = Exception.class)
    public OpenMonitoringPositionVo openMonitoringPosition(String secUid) {
        OpenMonitoringPositionVo result = new OpenMonitoringPositionVo();

        UserCacheVo user = ResultUtil.getResult(userFeign.getLocalUser());
        RRException.isNotEmpty(user, "用户信息获取失败");

        List<UserPropertyTypeInfoVo> userProperty = userPropertyFeign.getUserProperty(user.getId());
        if (ObjectUtil.isEmpty(userProperty)) {
            return result;
        }

        AnchorUrlUserVo userAnchorBySecUid = anchorUrlUserProducer.getUserAnchorBySecUid(secUid, user.getId(), user.getActiveTenantId());
        if (userAnchorBySecUid == null) {
            return result;
        }

        AnchorUrlInfoVo anchorUrlInfoVo = anchorUrlProducer.infoBySecUid(secUid);
        if (anchorUrlInfoVo == null) {
            return result;
        }

        if (ObjectUtil.equals(userAnchorBySecUid.getAccountType(), 0)) {
            // 自有账号处理
            // 只有抖音才有弹幕
            if (AnchorPlatformEnum.DOU_YIN.getCode().equals(anchorUrlInfoVo.getPlatform())) {
                result.setIsBarrageMonitoring(barrageMonitoringByFreedomAccountStatus(userProperty, secUid, user));
            }
        } else if (ObjectUtil.equals(userAnchorBySecUid.getAccountType(), 1)) {
            // 同行账号处理
            // 只有抖音和视频号才有数据看板
            if (AnchorPlatformEnum.DOU_YIN.getCode().equals(anchorUrlInfoVo.getPlatform()) || AnchorPlatformEnum.SHI_PING_HAO.getCode().equals(anchorUrlInfoVo.getPlatform())) {
                result.setIsDataViewing(dataViewingByPeerAccountsStatus(userProperty, secUid, user));
            }
        }
        return result;
    }

    /**
     * 是自有账号，判断是否开启弹幕监控
     *
     * @param userProperty 资产
     * @param secUid       主播
     * @return 返回是否开启弹幕监控 @{@link Constant.yesOrNoEnum}
     */
    private Integer barrageMonitoringByFreedomAccountStatus(List<UserPropertyTypeInfoVo> userProperty, String secUid, UserCacheVo user) {
        if (ObjectUtil.isEmpty(userProperty)) {
            return null;
        }
        // 获取当前资产
        UserPropertyTypeInfoVo currentUserProperty = getCurrentUserProperty(userProperty, OrderEnums.commodityTypeCode.ANCHOR_BARRAGE_NUM.getCode());
        if (currentUserProperty == null) {
            return null;
        }

        // 获取剩余资产
        long surplus = currentUserProperty.getTotalQuantity() - currentUserProperty.getUseQuantity();
        if (surplus <= 0) {
            return null;
        }
        // 更新资产
        Long quantityUsed = anchorUrlUserProducer.updateBarrageMonitoring(
                user.getId(),
                user.getActiveTenantId(),
                secUid,
                Constant.yesOrNoEnum.YES.getCode(),
                currentUserProperty.getTotalQuantity()
        );
        if (quantityUsed == null) {
            return null;
        }

        // 更新资产
        boolean updateProperty = userPropertyFeign.updateByPropertyNumRetBoolean(
                user.getId(),
                OrderEnums.commodityTypeCode.ANCHOR_BARRAGE_NUM.getCode(),
                quantityUsed);
        return updateProperty ? Constant.yesOrNoEnum.YES.getCode() : null;
    }

    /**
     * 是行业账号，判断是否开启数据看板
     *
     * @param userProperty 资产
     * @param secUid       主播
     * @ return 返回 是否开启数据看板 @{@link Constant.yesOrNoEnum}
     */
    private Integer dataViewingByPeerAccountsStatus(List<UserPropertyTypeInfoVo> userProperty, String secUid, UserCacheVo user) {
        if (ObjectUtil.isEmpty(userProperty)) {
            return null;
        }
        // 获取当前资产
        UserPropertyTypeInfoVo currentUserProperty = getCurrentUserProperty(userProperty, OrderEnums.commodityTypeCode.DATA_BOARD_NUM.getCode());
        if (currentUserProperty == null) {
            return null;
        }
        // 获取剩余资产
        long surplus = currentUserProperty.getTotalQuantity() - currentUserProperty.getUseQuantity();
        if (surplus <= 0) {
            return null;
        }
        // 更新资产
        boolean updated = anchorUrlUserProducer.updateDataViewing(user.getId(), user.getActiveTenantId(), secUid, Constant.yesOrNoEnum.YES.getCode());
        return updated ? Constant.yesOrNoEnum.YES.getCode() : null;
    }

    /**
     * 获取当前用户指定资产
     *
     * @param userProperty
     * @param code
     * @return
     */
    public UserPropertyTypeInfoVo getCurrentUserProperty(List<UserPropertyTypeInfoVo> userProperty, String code) {
        return userProperty.stream()
                .filter(item -> item.getCommodityTypeCode().equals(code))
                .findFirst()
                .orElse(null);
    }

    /**
     * 获取当前正在监控的直播位置
     *
     * @return 主播列表的监控位
     */
    public List<AnchorUrlUserVo> getCurrentMonitoringPosition() {
        // 获取当前用户-没有报错
        UserCacheVo user = ResultUtil.getUserResult(userFeign.getLocalUser());
        return anchorUrlUserProducer.monitoringPositionByUser(user.getId(), user.getActiveTenantId());
    }

    /**
     * 获取子账号主播列表
     *
     * @param subAnchorListBo 查询参数
     * @return
     */
    public R<PageUtils<AnchorUrlUserVo>> getSubUserAnchorList(SubAnchorListBo subAnchorListBo) {

        if (subAnchorListBo.getUserId() == null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "子账号id不能为空");
        }

        R<UserCacheVo> userR = this.userFeign.getLocalUser();
        if (userR.getData() == null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "没有权限");
        }
        subAnchorListBo.setTenantId(userR.getData().getActiveTenantId());

        // 分页获取子账号主播列表
        PageUtils<AnchorUrlUserVo> pageUtils = anchorRse.getSubUserAnchorList(subAnchorListBo);

        return R.ok(pageUtils);
    }

    /**
     * @param secUidList
     * @param userIds
     * @return
     */
    public List<AnchorUrlUserVo> listBySecUidsAndUserIds(List<String> secUidList, List<Long> userIds) {
        return anchorUrlUserProducer.listBySecUidsAndUserIds(secUidList, userIds);
    }

    /**
     * 更新ai页面的部分主播字段
     *
     * @param updateAiPartialBo 值
     * @return
     */
    public Boolean updateAiPartial(UpdateAiPartialBo updateAiPartialBo) {
        AnchorUrlUserVo userAnchorBySecUid = anchorUrlUserProducer.getUserAnchorBySecUid(updateAiPartialBo.getSecUid(), updateAiPartialBo.getUserId(), updateAiPartialBo.getTenantId());
        if (userAnchorBySecUid == null) {
            throw new BusinessException(StatusCode.DATA_NOT_EXIST.getCode(), "主播查询失败");
        }
        basicSettingsProducer.updateAiPartialNew(new BasicSettingsBo(updateAiPartialBo));
        return true;
    }

    /**
     * 获取主播在ai问答中的提示词
     *
     * @param sourceId   类型id
     * @param sourceType 类型
     * @return
     */
    public String getAiAnchorPrompt(String sourceId, Integer sourceType) {
        List<String> resList = new ArrayList<>();
        if (sourceType == WordsEnum.sourceType.VIDEO.getCode()) {
            AnchorVideoInfoVo video = anchorVideoProducer.getByVideoId(sourceId);
            if (video == null) {
                return "";
            }
            AnchorUrlUserVo anchor = anchorUrlUserProducer.getUserAnchorBySecUid(video.getSecUid(), video.getUserId(), video.getTenantId());
            if (anchor == null) {
                return "";
            }
            String aiAnchorPrompt = this.singleAiAnchorPrompt(BeanUtil.copyProperties(anchor, BackgroundConfigVo.class), true, 0);
            if (StrUtil.isNotEmpty(aiAnchorPrompt)) {
                resList.add(aiAnchorPrompt);
            }
        } else if (sourceType == WordsEnum.sourceType.SYNC_CONTRAST.getCode()) {
            SyncContrastInfoVo contrastInfoVo = syncContrastProducer.infoByContrastId(sourceId);

            if (contrastInfoVo == null) {
                return "";
            }
            String[] videoList = new String[2];
            if (ObjectUtil.isNotEmpty(contrastInfoVo.getVideoOneId())) {
                videoList[0] = contrastInfoVo.getVideoOneId();
            }
            if (ObjectUtil.isNotEmpty(contrastInfoVo.getVideoTwoId())) {
                videoList[1] = contrastInfoVo.getVideoTwoId();
            }
            for (int i = 0; i < videoList.length; i++) {
                String videoId = videoList[i];

                AnchorVideoInfoVo video = anchorVideoProducer.getByVideoId(videoId);
                if (video == null) {
                    continue;
                }
                AnchorUrlUserVo anchor = anchorUrlUserProducer.getUserAnchorBySecUid(video.getSecUid(), video.getUserId(), video.getTenantId());
                if (anchor == null) {
                    continue;
                }
                String aiAnchorPrompt = this.singleAiAnchorPrompt(BeanUtil.copyProperties(anchor, BackgroundConfigVo.class), false, i + 1);
                if (StrUtil.isNotEmpty(aiAnchorPrompt)) {
                    resList.add(aiAnchorPrompt);
                }
            }
        }
        return resList.isEmpty() ? "" : StrUtil.join("\n\n", resList);
    }

    /**
     * 获取主播在ai问答中的提示词
     * @param askRequestBo 问答入参
     * @return 关于主播的提示词
     */
    public String getAiAnchorPrompt(AskRequestBo askRequestBo) {

        if (askRequestBo == null || askRequestBo.getOtherObj() == null) {
            return null;
        }
        List<String> resList = new ArrayList<>();

        List<BackgroundConfigVo> dataList = new ArrayList<>();
        String sourceIdOne = null, sourceIdTwo = null;
        String sourceDateOne = "", sourceDateTwo = "";
        Integer sourceTypeOne = null, sourceTypeTwo = null;
        String anchorNameOne = null, anchorNameTwo = null;
        Integer speechRateOne = null, speechRateTwo = null;
        boolean syncScene = false;

        // 获取语速
        if (askRequestBo.getOtherObj() != null) {
            Object backgroundConfig = askRequestBo.getOtherObj().getOrDefault("backgroundConfigOne", null);
            if (backgroundConfig != null) {
                BackgroundConfigVo e = JSONObject.parseObject(JSONObject.toJSONString(backgroundConfig), BackgroundConfigVo.class);
                speechRateOne = e.getSpeechRate();
            }
            Object backgroundConfigTwo = askRequestBo.getOtherObj().getOrDefault("backgroundConfigTwo", null);
            if (backgroundConfigTwo != null) {
                BackgroundConfigVo e = JSONObject.parseObject(JSONObject.toJSONString(backgroundConfigTwo), BackgroundConfigVo.class);
                speechRateTwo = e.getSpeechRate();
            }
        }

        // 获取背景配置
        sourceIdOne = askRequestBo.getSourceId();
        if (ObjectUtil.equals(askRequestBo.getSourceType(), WordsEnum.sourceType.VIDEO.getCode())) {
            sourceTypeOne = WordsEnum.sourceType.VIDEO.getCode();
            AnchorVideoInfoVo video = anchorVideoProducer.getByVideoId(askRequestBo.getSourceId());
            if (video != null) {
                AnchorUrlInfoVo anchorUrlInfoVo = anchorUrlProducer.infoBySecUid(video.getSecUid());
                String platformLabel = getPlatformLabelByType(video.getPlatformType());
                String format = StrUtil.format("本直播间名称：{}，平台：{}，直播开始时间：{}，直播时长：{}秒",
                        anchorUrlInfoVo != null ? anchorUrlInfoVo.getAnchorName() : "无",
                        platformLabel != null ? platformLabel : "未知",
                        DateUtil.formatDateTime(video.getStartTime()), video.getDuration());
                if (speechRateOne != null) {
                    format += "，语速：" + speechRateOne + "字/分钟";
                }
                resList.add(format);
            }
        } else if (ObjectUtil.equals(askRequestBo.getSourceType(), WordsEnum.sourceType.FILE.getCode())) {
            sourceTypeOne = WordsEnum.sourceType.FILE.getCode();
            UploadFileInfoVo fileInfo = uploadFileProducer.getByFileId(askRequestBo.getSourceId());
            List<String> fileParts = new ArrayList<>();
            if (fileInfo != null) {
                String platformLabel = getPlatformLabelByType(fileInfo.getPlatformType());
                if (StrUtil.isNotEmpty(platformLabel)) {
                    fileParts.add("平台：" + platformLabel);
                }
            }
            if (speechRateOne != null) {
                fileParts.add("语速：" + speechRateOne + "字/分钟");
            }
            if (!fileParts.isEmpty()) {
                resList.add("本文件" + StrUtil.join("，", fileParts));
            }
        } else if (ObjectUtil.equals(askRequestBo.getSourceType(), WordsEnum.sourceType.SYNC_CONTRAST.getCode())) {
            // 说明两个视频的角色
            SyncContrastInfoVo syncContrastInfoVo = syncContrastProducer.infoDetailsByContrastId(askRequestBo.getSourceId());
            if (syncContrastInfoVo != null) {
                if (ObjectUtil.isNotEmpty(syncContrastInfoVo.getVideoOneId())) {
                    sourceIdOne = syncContrastInfoVo.getVideoOneId();
                    sourceTypeOne = WordsEnum.sourceType.VIDEO.getCode();
                    if (syncContrastInfoVo.getVideoOneInfo() != null && syncContrastInfoVo.getVideoOneInfo().getStartTime() != null) {
                        sourceDateOne += "(直播开始时间：" + DateUtil.formatDateTime(syncContrastInfoVo.getVideoOneInfo().getStartTime());
                        if (syncContrastInfoVo.getVideoOneInfo().getDuration() != null) {
                            sourceDateOne += "，整场直播时长：" + syncContrastInfoVo.getVideoOneInfo().getDuration() + "秒";
                        }
                        if (StrUtil.isNotEmpty(syncContrastInfoVo.getVideoOneInfo().getPlatformType())) {
                            String platformLabel = getPlatformLabelByType(syncContrastInfoVo.getVideoOneInfo().getPlatformType());
                            if (StrUtil.isNotEmpty(platformLabel)) {
                                sourceDateOne += "，平台：" + platformLabel;
                            }
                        }
                        if (speechRateOne != null) {
                            sourceDateOne += "，整场语速：" + speechRateOne + "字/分钟";
                        }
                        sourceDateOne += ")";
                    }
                    if (syncContrastInfoVo.getAnchorOneInfo() != null && syncContrastInfoVo.getAnchorOneInfo().getAnchorName() != null) {
                        anchorNameOne = syncContrastInfoVo.getAnchorOneInfo().getAnchorName();
                    }
                }
                if (ObjectUtil.isNotEmpty(syncContrastInfoVo.getVideoTwoId())) {
                    sourceIdTwo = syncContrastInfoVo.getVideoTwoId();
                    sourceTypeTwo = WordsEnum.sourceType.VIDEO.getCode();
                    if (syncContrastInfoVo.getVideoTwoInfo() != null && syncContrastInfoVo.getVideoTwoInfo().getStartTime() != null) {
                        sourceDateTwo += "(直播开始时间：" + DateUtil.formatDateTime(syncContrastInfoVo.getVideoTwoInfo().getStartTime());
                        if (syncContrastInfoVo.getVideoTwoInfo().getDuration() != null) {
                            sourceDateTwo += "，整场直播时长：" + syncContrastInfoVo.getVideoTwoInfo().getDuration() + "秒";
                        }
                        if (StrUtil.isNotEmpty(syncContrastInfoVo.getVideoTwoInfo().getPlatformType())) {
                            String platformLabel = getPlatformLabelByType(syncContrastInfoVo.getVideoTwoInfo().getPlatformType());
                            if (StrUtil.isNotEmpty(platformLabel)) {
                                sourceDateTwo += "，平台：" + platformLabel;
                            }
                        }
                        if (speechRateTwo != null) {
                            sourceDateTwo += "，整场语速：" + speechRateTwo + "字/分钟";
                        }
                        sourceDateTwo += ")";
                    }
                    if (syncContrastInfoVo.getAnchorTwoInfo() != null && syncContrastInfoVo.getAnchorTwoInfo().getAnchorName() != null) {
                        anchorNameTwo = syncContrastInfoVo.getAnchorTwoInfo().getAnchorName();
                    }
                }
                if (ObjectUtil.isNotEmpty(syncContrastInfoVo.getFileOneId())) {
                    sourceIdOne = syncContrastInfoVo.getFileOneId();
                    sourceTypeOne = WordsEnum.sourceType.FILE.getCode();
                    if (syncContrastInfoVo.getFileOneInfo() != null) {
                        String platformLabel = getPlatformLabelByType(syncContrastInfoVo.getFileOneInfo().getPlatformType());
                        if (StrUtil.isNotEmpty(platformLabel)) {
                            sourceDateOne = "(平台：" + platformLabel + ")";
                        }
                    }
                }
                if (ObjectUtil.isNotEmpty(syncContrastInfoVo.getFileTwoId())) {
                    sourceIdTwo = syncContrastInfoVo.getFileTwoId();
                    sourceTypeTwo = WordsEnum.sourceType.FILE.getCode();
                    if (syncContrastInfoVo.getFileTwoInfo() != null) {
                        String platformLabel = getPlatformLabelByType(syncContrastInfoVo.getFileTwoInfo().getPlatformType());
                        if (StrUtil.isNotEmpty(platformLabel)) {
                            sourceDateTwo = "(平台：" + platformLabel + ")";
                        }
                    }
                }
                String anchorName = "";
                if (ObjectUtil.isNotEmpty(anchorNameOne) || ObjectUtil.isNotEmpty(anchorNameTwo)) {
                    if (anchorNameOne.equals(anchorNameTwo)) {
                        anchorName = StrUtil.format("，直播间名称为：{}", anchorNameOne);
                    } else {
                        anchorName = StrUtil.format("，视频1的直播间名称为：{}，视频2的直播间名称为：{}", anchorNameOne, anchorNameTwo);
                    }
                }
                if (ObjectUtil.equals(syncContrastInfoVo.getSyncScene(), WordsEnum.syncScene.LAST_SCENE.getCode())) {
                    // 对比上一次场
                    resList.add(StrUtil.format("这两个视频都是同一个主播在播{}，视频1是本场直播{}，视频2是历史直播{}", anchorName, sourceDateOne, sourceDateTwo));
                    resList.add("说明本场总体做得好，还是上一场做得好。如果有优化动作和优化目的，请分析优化动作是否达成了优化目的，以及达成的表现和原因，或者没有达成的表现或者原因。");
                } else if (ObjectUtil.equals(syncContrastInfoVo.getSyncScene(), WordsEnum.syncScene.SAME_SCENE.getCode())) {
                    // 同直播间对比
                    resList.add(StrUtil.format("这两个视频都是同一个主播在播{}，视频1是优化的场次{}，视频2是对标的场次{}", anchorName, sourceDateOne, sourceDateTwo));
                } else if (ObjectUtil.equals(syncContrastInfoVo.getSyncScene(), WordsEnum.syncScene.DIFF_SCENE.getCode())) {
                    // 不同直播间对比
                    syncScene = true;
                    resList.add(StrUtil.format("这两个视频都是不同的主播在播{}，视频1是优化的场次{}，视频2是对标的场次{}", anchorName, sourceDateOne, sourceDateTwo));
                }
            }

        }

        List<String> keys = ObjectUtil.equals(askRequestBo.getSourceType(), WordsEnum.sourceType.SYNC_CONTRAST.getCode()) && syncScene ?
                List.of("backgroundConfigOne", "backgroundConfigTwo") : List.of("backgroundConfigOne");
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            Object backgroundConfig = askRequestBo.getOtherObj().getOrDefault(key, null);
            if (backgroundConfig != null) {
                BackgroundConfigVo e = JSONObject.parseObject(JSONObject.toJSONString(backgroundConfig), BackgroundConfigVo.class);
                if (e.getAccountType() != null) {
                    dataList.add(e);
                }
                if (i == 0) {
                    e.setSourceId(sourceIdOne);
                    e.setSourceType(sourceTypeOne);
                } else {
                    e.setSourceId(sourceIdTwo);
                    e.setSourceType(sourceTypeTwo);
                }
            }
        }
        if (dataList.isEmpty()) {
            return null;
        }

        // 输入优化动作
        for (int i = 0; i < dataList.size(); i++) {

            BackgroundConfigVo item = dataList.get(i);
            if (ObjectUtil.equals(item.getOptimizeActions(), 1)) {
                AiOptimizePurposeVo purposeVo = aiOptimizePurposeRse.getBySourceId(item.getSourceId());
                if (purposeVo != null) {
                    if (ObjectUtil.isNotEmpty(purposeVo.getOptimizeAction())) {
                        resList.add(StrUtil.format("视频{}的优化动作：{}", i + 1, purposeVo.getOptimizeAction()));
                    }
                    if (ObjectUtil.isNotEmpty(purposeVo.getOptimizePurpose())) {
                        resList.add(StrUtil.format("视频{}的优化目的：{}", i + 1, purposeVo.getOptimizePurpose()));
                    }
                }
            }
        }

        boolean single = dataList.size() == 1;
        for (int i = 0; i < dataList.size(); i++) {
            BackgroundConfigVo configVo = dataList.get(i);
            String str = singleAiAnchorPrompt(configVo, single, i + 1);
            if (ObjectUtil.isNotEmpty(str)) {
                resList.add(str);
            }
        }
        return resList.isEmpty() ? null : StrUtil.join("\n\n", resList);
    }

    /**
     * 获取单个主播在ai问答中的提示词
     *
     * @param configVo 账号信息
     * @param i        视频序号
     * @return 提示词
     */
    private String singleAiAnchorPrompt(BackgroundConfigVo configVo, boolean single, int i) {
        if (configVo == null) {
            return null;
        }
        List<String> resList = new ArrayList<>();
        String aiAnchorPrompt = this.getAiAnchorUserData(configVo);
        if (single) {
            if (ObjectUtil.equals(configVo.getAccountType(), WordsEnum.accountType.OWN.getCode())) {
                resList.add(StrUtil.format("本直播间账号属于：自有账号，是我自己的直播间"));
            } else if (ObjectUtil.equals(configVo.getAccountType(), WordsEnum.accountType.PEER.getCode())) {
                resList.add(StrUtil.format("本直播间账号属于：同行业账号，属于我的竞品直播间，属于我的对标直播间"));
            }
            if (StrUtil.isNotEmpty(aiAnchorPrompt)) {
                resList.add("这场直播中主播对应的账号信息为：\n" + aiAnchorPrompt);
            }
        } else {
            if (ObjectUtil.equals(configVo.getAccountType(), WordsEnum.accountType.OWN.getCode())) {
                resList.add(StrUtil.format("视频{}的直播间账号属于：自有账号，是我自己的直播间", i));
            } else if (ObjectUtil.equals(configVo.getAccountType(), WordsEnum.accountType.PEER.getCode())) {
                resList.add(StrUtil.format("视频{}的直播间账号属于：同行业账号，属于我的竞品直播间，属于我的对标直播间", i));
            }
            if (StrUtil.isNotEmpty(aiAnchorPrompt)) {
                resList.add(StrUtil.format("视频{}中主播对应的账号信息为：\n{}", i, aiAnchorPrompt));
            }
        }
        return resList.isEmpty() ? null : StrUtil.join("\n", resList);
    }

    /**
     * 获取主播在ai问答中的提示词
     * @param configVo 账号信息
     * @return ai问答中的提示词
     */
    private String getAiAnchorUserData(BackgroundConfigVo configVo) {
        if (configVo.getAccountType() == null) {
            return null;
        }

        List<String> resList = new ArrayList<>();

        // 语数
        if (ObjectUtil.isNotEmpty(configVo.getSpeechRate())) {
            resList.add(StrUtil.format("语速：{}字/分钟", configVo.getSpeechRate()));
        }

        // 首播日期
        if (ObjectUtil.isNotEmpty(configVo.getPremiereDate())) {
            resList.add(StrUtil.format("首播日期：{}", ObjectUtil.isNotEmpty(configVo.getPremiereDate()) ? DateUtil.formatDate(configVo.getPremiereDate()) : "无"));
        }

        // 账号问题
        if (ObjectUtil.isNotEmpty(configVo.getAnchorSituation())) {
            String name = ObjectUtil.equals(configVo.getAccountType(), WordsEnum.accountType.PEER.getCode()) ? "重点疑问" : "账号问题";
            resList.add(StrUtil.format("{}：{}", name, ObjectUtil.isNotEmpty(configVo.getAnchorSituation()) ? configVo.getAnchorSituation() : "无"));
        }


        // 账号阶段
        if (ObjectUtil.isNotEmpty(configVo.getAccountStage())) {
            resList.add(StrUtil.format("账号阶段：{}", getDictLabelByCodeAndValue("account_stage", configVo.getAccountStage(), false)));
        }

        // 直播目标
        if (ObjectUtil.isNotEmpty(configVo.getLivingTarget())) {
            resList.add(StrUtil.format("直播目标：{}", getDictLabelByCodeAndValue("living_target", configVo.getLivingTarget(), false)));
        }

        // 账号水平
        if (ObjectUtil.isNotEmpty(configVo.getAccountWaterLevel())) {
            resList.add(StrUtil.format("账号水平：{}", getDictLabelByCodeAndValue("account_water_level", configVo.getAccountWaterLevel(), false)));
        }

        // 流量结构
        if (ObjectUtil.isNotEmpty(configVo.getAccountFlow())) {
            resList.add(StrUtil.format("流量结构：{}", getDictLabelByCodeAndValue("account_flow", configVo.getAccountFlow(), false)));
        }

        // 直播形式
        if (ObjectUtil.isNotEmpty(configVo.getLivingModality())) {
            resList.add(StrUtil.format("直播形式：{}", getDictLabelByCodeAndValue("living_modality", configVo.getLivingModality(), false)));
        }

        // 营销组件
        if (ObjectUtil.isNotEmpty(configVo.getMarketing())) {
            resList.add(StrUtil.format("营销组件：{}", getDictLabelByCodeAndValue("marketing", configVo.getMarketing(), false)));
        }

        // 优化方向（可多选）
        if (ObjectUtil.isNotEmpty(configVo.getOptimizeDirection())) {
            resList.add(StrUtil.format("优化方向：{}", getDictLabelByCodeAndValue("optimize_direction", configVo.getOptimizeDirection(), true)));
        }

        // 学习方向(可多选)
        if (ObjectUtil.isNotEmpty(configVo.getLearning())) {
            resList.add(StrUtil.format("学习方向：{}", getDictLabelByCodeAndValue("learning", configVo.getLearning(), true)));
        }

        if (ObjectUtil.isEmpty(resList)) {
            return null;
        }
        return String.join("\n", resList);
    }

    /**
     * 通过字典 replay_platform_type 获取平台中文名称
     *
     * @param platformType 平台code（"1":抖音 "2":快手 "3":视频号）
     * @return 平台中文名称
     */
    private String getPlatformLabelByType(String platformType) {
        if (StrUtil.isEmpty(platformType)) return null;
        DictDataListVo dict = dictDataFeign.dictDataByValue("replay_platform_type", platformType);
        return dict != null ? dict.getLabel() : null;
    }

    /**
     * 彻底删除主播
     *
     * @param secUid 主播 secUid
     * @return 操作结果
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean thoroughlyDeleteAnchor(String secUid) {
        // 获取用户信息和租户信息
        UserCacheVo userCacheVo = userFeign.getLocalUser().getData();
        Long userId = userCacheVo.getId();
        Long tenantId = userCacheVo.getActiveTenantId();

        // 查询用户主播关联信息（排除已彻底删除的行，避免重复"彻底删"无意义触发释放循环）
        List<AnchorUrlUserEntity> anchorUrlUserEntityList = anchorUrlUserService.list(new LambdaQueryWrapper<>(AnchorUrlUserEntity.class)
                .eq(AnchorUrlUserEntity::getUserId, userId)
                .eq(AnchorUrlUserEntity::getTenantId, tenantId)
                .eq(AnchorUrlUserEntity::getAnchorUrlSecUid, secUid)
                .ne(AnchorUrlUserEntity::getIsRemoveRecord, 2)
        );
        if (CollectionUtil.isEmpty(anchorUrlUserEntityList)) {
            return false;
        }
        anchorUrlUserEntityList.forEach(item -> item.setIsRemoveRecord(2));
        // 先写 isRemoveRecord=2 落库，保存返回值，updateBatchById 失败则跳过释放循环直接返回
        boolean result = anchorUrlUserService.updateBatchById(anchorUrlUserEntityList);
        if (!result) {
            return false;
        }

        // 彻底删时（isRemoveRecord=2）释放三个监控开关占用的资源位
        Map<String, String> releaseCodeMap = new LinkedHashMap<>();
        releaseCodeMap.put("isScriptQualityInspection", OrderEnums.commodityTypeCode.SCRIPT_QUALITY_NUM.getCode());
        releaseCodeMap.put("isScriptFidelityMonitor", OrderEnums.commodityTypeCode.SCRIPT_FIDELITY_NUM.getCode());
        releaseCodeMap.put("isInteractionPatrol", OrderEnums.commodityTypeCode.INTERACTION_PATROL_NUM.getCode());

        for (Map.Entry<String, String> entry : releaseCodeMap.entrySet()) {
            String switchField = entry.getKey();
            String code = entry.getValue();
            // 统计当前用户该租户下 isRemoveRecord=0 且该开关开启的总行数（刚彻底删的行已不含）
            Long openCount = anchorUrlUserProducer.countOpenSwitch(userId, tenantId, switchField);
            // 覆盖写：直接传当前存量，无需 ±1
            boolean ok = userPropertyFeign.updateByPropertyNumRetBoolean(userId, code, openCount);
            if (!ok) {
                log.warn("[彻底删释放] 更新资产失败 userId={} tenantId={} secUid={} code={} openCount={}",
                        userId, tenantId, secUid, code, openCount);
                throw new RuntimeException("资产占用量更新失败, userId=" + userId + ", code=" + code);
            }
        }

        return result;
    }

    /**
     * 重新添加主播
     *
     * @param secUid
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean againAddAnchor(String secUid) {
        // 获取用户信息和租户信息
        UserCacheVo userCacheVo = userFeign.getLocalUser().getData();
        Long userId = userCacheVo.getId();
        Long tenantId = userFeign.getUserTenantId(userId).getData();

        // 查询用户主播关联信息
        List<AnchorUrlUserEntity> anchorUrlUserEntityList = anchorUrlUserService.list(new LambdaQueryWrapper<>(AnchorUrlUserEntity.class)
                .eq(AnchorUrlUserEntity::getUserId, userId)
                .eq(AnchorUrlUserEntity::getTenantId, tenantId)
                .eq(AnchorUrlUserEntity::getAnchorUrlSecUid, secUid)
                .eq(AnchorUrlUserEntity::getIsRemoveRecord, 1)
        );
        if (CollectionUtil.isEmpty(anchorUrlUserEntityList)) {
            return false;
        }
        anchorUrlUserEntityList.forEach(item -> item.setIsRemoveRecord(0));
        return anchorUrlUserService.updateBatchById(anchorUrlUserEntityList);
    }

    /**
     * 更新主播基础信息
     *
     * @param updateAnchorBaseInfoBo 主播基础信息
     * @return
     */
    public R<String> updateAnchorBaseInfo(UpdateAnchorBaseInfoBo updateAnchorBaseInfoBo) {
        if(!StringUtils.hasLength(updateAnchorBaseInfoBo.getSecUid())) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "secUid不能为空");
        }

        this.anchorRse.updateAnchorBaseInfo(updateAnchorBaseInfoBo);

        return R.ok();
    }

    /**
     * 更新用户主播信息
     *
     * @param updateAnchorUserBo 用户主播基础信息
     * @return
     */
    public R<String> updateUserAnchorInfo(UpdateAnchorUserBo updateAnchorUserBo) {
        // 获取用户信息和租户信息
        UserCacheVo userCacheVo = userFeign.getLocalUser().getData();
        updateAnchorUserBo.setUserId(userCacheVo.getId());
        updateAnchorUserBo.setTenantId(userCacheVo.getActiveTenantId());

        this.anchorRse.updateUserAnchorInfo(updateAnchorUserBo);

        // 通知企业管理服务端添加主播
        if (Objects.equals(updateAnchorUserBo.getIsStatisticsPerformance(), 1)) {
            try {
                // 查询主播基础信息用于构建请求参数
                AnchorUrlInfoVo anchorInfo = anchorUrlProducer.infoBySecUid(updateAnchorUserBo.getSecUid());
                if (anchorInfo != null) {
                    GovernanceAddAnchorBo anchorBo = new GovernanceAddAnchorBo();
                    anchorBo.setPlatform(anchorInfo.getPlatform());
                    anchorBo.setAnchorNumber(anchorInfo.getAnchorNumber());
                    anchorBo.setSecUid(anchorInfo.getSecUid());
                    anchorBo.setHomeUrl(anchorInfo.getHomeUrl());
                    anchorBo.setLiveUrl(anchorInfo.getLiveUrl());
                    anchorBo.setAnchorName(anchorInfo.getAnchorName());
                    anchorBo.setAnchorAvatar(anchorInfo.getAnchorAvatar());
                    anchorBo.setTradeId(updateAnchorUserBo.getTradeId());
                    governanceLiveRoomService.addAnchor(anchorBo);
                }
            } catch (Exception e) {
                log.error("[企业管理] 通知添加主播失败, secUid={}", updateAnchorUserBo.getSecUid(), e);
            }
        }

        return R.ok();
    }

    /**
     * 获取ai页面的部分主播字段
     *
     * @param videoId 视频id
     * @return ai页面的部分主播字段
     */
    public UpdateAiPartialBo getAiPartial(String videoId) {
        AnchorVideoInfoVo video = anchorVideoProducer.getByVideoId(videoId);
        if (video == null) {
            return null;
        }
        AnchorUrlUserVo userAnchor = anchorUrlUserProducer.getUserAnchorBySecUid(video.getSecUid(), video.getUserId(), video.getTenantId());
        if (userAnchor == null) {
            throw new BusinessException(StatusCode.DATA_NOT_EXIST.getCode(), "主播查询失败");
        }

        UpdateAiPartialBo updateAiPartialBo = new UpdateAiPartialBo();
        updateAiPartialBo.setSecUid(video.getSecUid());
        updateAiPartialBo.setUserId(video.getUserId());
        updateAiPartialBo.setTenantId(video.getTenantId());
        updateAiPartialBo.setAccountStage(userAnchor.getAccountStage());
        updateAiPartialBo.setAccountWaterLevel(userAnchor.getAccountWaterLevel());
        updateAiPartialBo.setAccountFlow(userAnchor.getAccountFlow());
        updateAiPartialBo.setAnchorSituation(userAnchor.getAnchorSituation());
        updateAiPartialBo.setAccountType(userAnchor.getAccountType());
        return updateAiPartialBo;
    }

    /**
     * 更新ai页面的部分主播字段
     *
     * @param updateAiPartialBo 参数
     * @return 是否更新
     */
    public Boolean updateAiPartialNew(UpdateAiPartialBo updateAiPartialBo) {
        AnchorVideoInfoVo video = anchorVideoProducer.getByVideoId(updateAiPartialBo.getVideoId());
        if (video == null || !ObjectUtil.equals(video.getUserId(), updateAiPartialBo.getUserId()) || !ObjectUtil.equals(video.getTenantId(), updateAiPartialBo.getTenantId())) {
            return false;
        }
        updateAiPartialBo.setSecUid(video.getSecUid());
        basicSettingsProducer.updateAiPartialNew(new BasicSettingsBo(updateAiPartialBo));
        return true;
    }

    /**
     * 根据主播抖音号获取主播信息
     *
     * @param anchorNumber 主播抖音号
     * @return 主播信息
     */
    public AnchorUrlInfoVo infoByAnchorNumber(String anchorNumber) {
        return anchorUrlProducer.infoByAnchorNumber(anchorNumber);
    }

    /**
     * 根据字典code和value获取对应的label值
     *
     * @param code       字典类型标识
     * @param value      字典值，如果是多选则为多个value组成的字符串，中间用逗号分隔
     * @param isMultiple 是否多选，如果是多选就把这个code的全部查询出来进行匹配，中间用逗号分隔后返回
     * @return 对应的label值，多选情况下多个label用逗号分隔
     */
    public String getDictLabelByCodeAndValue(String code, Object value, boolean isMultiple) {
        if (ObjectUtil.isEmpty(value)) {
            return "无";
        }

        if (isMultiple) {
            // 多选情况：查询该code下的所有字典项，然后匹配传入的多个value值
            List<DictDataListVo> allDictItems = dictDataFeign.dictDataListByCode(code);
            if (CollectionUtil.isEmpty(allDictItems)) {
                return "无";
            }

            // 解析传入的value，它是由逗号分隔的多个值
            Set<String> valueSet = Arrays.stream(String.valueOf(value).split(","))
                    .map(String::trim)
                    .filter(StrUtil::isNotBlank)
                    .collect(Collectors.toSet());

            // 匹配并收集对应的label
            List<String> matchedLabels = allDictItems.stream()
                    .filter(item -> item != null && item.getValue() != null && valueSet.contains(item.getValue().trim()))
                    .map(DictDataListVo::getLabel)
                    .filter(StrUtil::isNotBlank)
                    .collect(Collectors.toList());

            return CollectionUtil.isEmpty(matchedLabels) ? "无" : String.join(",", matchedLabels);
        } else {
            // 单选情况：直接根据code和value查询
            DictDataListVo dictItem = dictDataFeign.dictDataByValue(code, String.valueOf(value));
            return dictItem != null ? dictItem.getLabel() : "无";
        }
    }


    /**
     * 减少数据诊断生成次数
     *
     * @param secUid   主播secUid
     * @param videoId  视频id
     * @param userId   用户id
     * @param tenantId 租户id
     * @param num      减少次数
     */
    public void minusDataDiagnosisGenerateNum(String secUid, String videoId, Long userId, Long tenantId, int num) {
        log.info("减少数据诊断生成次数，secUid：{}，videoId：{}，userId：{}，tenantId：{}", secUid, videoId, userId, tenantId);
        anchorUrlUserProducer.minusDataDiagnosisGenerateNum(secUid, userId, tenantId, num);
    }

    public void updateTrdeIdBySecUid(String secUid, Long aiTradeId, Long systemTradeId) {
        anchorUrlProducer.updateTrdeIdBySecUid(secUid, aiTradeId, systemTradeId);

    }

    /**
     * 获取租户下的所有主播信息
     *
     * @param req  查询
     * @return 租户下的所有主播信息
     */
    public List<TenantAnchorInfoResponse> getTenantAllAnchorInfo(TenantAnchorQueryRequest req) {
        if (req.getTenantId() == null) {
            return List.of();
        }
        List<AnchorUrlUserEntity> userAnchorList = new BatchQuery<>((limit, idx) -> {
            return anchorUrlUserService.lambdaQuery()
                .gt(idx != null, AnchorUrlUserEntity::getId, idx)
                .eq(AnchorUrlUserEntity::getTenantId, req.getTenantId())
                .eq(AnchorUrlUserEntity::getIsRemoveRecord, false)
                .in(EmptyUtil.isNotEmpty(req.getTradeIds()), AnchorUrlUserEntity::getTradeId, req.getTradeIds())
                .in(EmptyUtil.isNotEmpty(req.getUserIds()), AnchorUrlUserEntity::getUserId, req.getUserIds())
                .eq(EmptyUtil.isNotEmpty(req.getAccountType()), AnchorUrlUserEntity::getAccountType, req.getAccountType())
                .eq(EmptyUtil.isNotEmpty(req.getAuthChannelStatus()), AnchorUrlUserEntity::getAuthChannelStatus, req.getAuthChannelStatus())
                .eq(EmptyUtil.isNotEmpty(req.getAuthJlbyStatus()), AnchorUrlUserEntity::getAuthJlbyStatus, req.getAuthJlbyStatus())
                .eq(EmptyUtil.isNotEmpty(req.getAuthQcStatus()), AnchorUrlUserEntity::getAuthQcStatus, req.getAuthQcStatus())
                .eq(EmptyUtil.isNotEmpty(req.getAuthLifeStatus()), AnchorUrlUserEntity::getAuthLifeStatus, req.getAuthLifeStatus())
                .eq(EmptyUtil.isNotEmpty(req.getIsScheduleRecord()), AnchorUrlUserEntity::getIsScheduleRecord, req.getIsScheduleRecord())
                .eq(EmptyUtil.isNotEmpty(req.getIsStatisticsPerformance()), AnchorUrlUserEntity::getIsStatisticsPerformance, req.getIsStatisticsPerformance())
                .last("limit " + limit).list();
        }, AnchorUrlUserEntity::getId).get();
        if (EmptyUtil.isEmpty(userAnchorList)) {
            return List.of();
        }
        Map<String, List<AnchorUrlUserEntity>> userAnchorMap = userAnchorList.stream().collect(Collectors.groupingBy(AnchorUrlUserEntity::getAnchorUrlSecUid));
        List<String> anchorSecUidList = userAnchorList.stream().map(AnchorUrlUserEntity::getAnchorUrlSecUid).distinct().toList();
        List<AnchorUrlInfoVo> anchorList = CollectionUtil.split(anchorSecUidList, 1000)
            .stream().flatMap(ids -> {
                List<AnchorUrlInfoVo> list = anchorUrlProducer.listByConditions(ids);
                if (list == null) {
                    return Stream.empty();
                }
                return list.stream();
            }).toList();
        if (EmptyUtil.isEmpty(anchorList)) {
            return List.of();
        }
        // 过滤掉非指定平台
        anchorList = anchorList.stream().filter(anchor -> EmptyUtil.isEmpty(req.getPlatformList()) || req.getPlatformList().contains(anchor.getPlatform())).toList();
        if (EmptyUtil.isEmpty(anchorList)) {
            return List.of();
        }
        return anchorList.stream()
            .map(anchorInfo -> {
                String secUid = anchorInfo.getSecUid();
                List<AnchorUrlUserEntity> entities = userAnchorMap.getOrDefault(secUid, List.of());

                TenantAnchorInfoResponse response = new TenantAnchorInfoResponse();
                // 基础信息映射
                response.setSecUid(secUid);
                response.setHomeUrl(anchorInfo.getHomeUrl());
                response.setLiveUrl(anchorInfo.getLiveUrl());
                response.setAnchorName(anchorInfo.getAnchorName());
                response.setAnchorAvatar(anchorInfo.getAnchorAvatar());
                response.setPlatform(anchorInfo.getPlatform());
                response.setPlatformResource(anchorInfo.getPlatformResource());
                response.setAnchorUserId(anchorInfo.getAnchorUserId());
                response.setWebSocketId(anchorInfo.getWebSocketId());
                response.setAnchorNumber(anchorInfo.getAnchorNumber());
                response.setChanmamaInclude(anchorInfo.getChanmamaInclude());
                response.setSystemTradeId(anchorInfo.getSystemTradeId());
                response.setAiCorrectTradeId(anchorInfo.getAiCorrectTradeId());

                // 集合字段：收集所有关联的 userId 和 tradeId
                response.setUserIds(entities.stream().map(AnchorUrlUserEntity::getUserId).filter(Objects::nonNull).distinct().toList());
                response.setTradeIds(entities.stream().map(AnchorUrlUserEntity::getTradeId).filter(Objects::nonNull).distinct().toList());

                // 优先级字段组装逻辑：

                // 1. 账户归宿：优先判断自有账户 (0)
                // 如果存在 accountType 为 0 的记录，则结果为 0；否则取第一个非空值或默认值
                response.setAccountType(entities.stream()
                    .map(AnchorUrlUserEntity::getAccountType)
                    .filter(Objects::nonNull)
                    .min((t1, t2) -> t1 == 0 ? -1 : (t2 == 0 ? 1 : 0))
                    .orElse(null));

                // 2. 是否排班录制：优先判断是 (1)
                response.setIsScheduleRecord(entities.stream()
                    .map(AnchorUrlUserEntity::getIsScheduleRecord)
                    .filter(Objects::nonNull)
                    .max(Integer::compareTo) // 1 > 0
                    .orElse(null));

                // 3. 是否统计业绩：优先判断是 (1)
                response.setIsStatisticsPerformance(entities.stream()
                    .map(AnchorUrlUserEntity::getIsStatisticsPerformance)
                    .filter(Objects::nonNull)
                    .max(Integer::compareTo) // 1 > 0
                    .orElse(null));

                // 4. 授权状态：优先判断已授权 (1)
                response.setAuthJlbyStatus(getPriorityAuthStatus(entities, AnchorUrlUserEntity::getAuthJlbyStatus));
                response.setAuthQcStatus(getPriorityAuthStatus(entities, AnchorUrlUserEntity::getAuthQcStatus));
                response.setAuthChannelStatus(getPriorityAuthStatus(entities, AnchorUrlUserEntity::getAuthChannelStatus));
                response.setAuthLifeStatus(getPriorityAuthStatus(entities, AnchorUrlUserEntity::getAuthLifeStatus));

                // 授权时间：取最近一次修改时间（或者已授权状态对应的时间，这里暂取最大值）
                response.setAuthJlbyStatusTime(entities.stream().map(AnchorUrlUserEntity::getAuthJlbyStatusTime).filter(Objects::nonNull).max(Date::compareTo).orElse(null));
                response.setAuthQcStatusTime(entities.stream().map(AnchorUrlUserEntity::getAuthQcStatusTime).filter(Objects::nonNull).max(Date::compareTo).orElse(null));
                response.setAuthLifeStatusTime(entities.stream().map(AnchorUrlUserEntity::getAuthLifeStatusTime).filter(Objects::nonNull).max(Date::compareTo).orElse(null));

                // ID：取最小的ID作为代表，或者根据业务需求调整
                response.setId(entities.stream().map(AnchorUrlUserEntity::getId).filter(Objects::nonNull).min(Long::compareTo).orElse(null));

                return response;
            })
            .toList();
    }




    /**
     * 设置主播行业
     *
     * @param setAnchorTradeBo 设置主播行业参数
     * @return 结果
     */
    public R<String> setAnchorTrade(SetAnchorTradeBo setAnchorTradeBo) {
        // 参数校验：至少传一个行业ID
        if (setAnchorTradeBo.getSystemTradeId() == null && setAnchorTradeBo.getAiCorrectTradeId() == null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "系统行业ID和AI纠正行业ID至少传一个");
        }

        this.anchorRse.setAnchorTrade(setAnchorTradeBo);

        return R.ok();
    }




    /**
     * 切换单个 AI 监控能力开关（B8）。
     *
     * <p>按「短路幂等 → 还原度暂禁 → 授权量校验 → 单字段写库 → 占用更新」4 步完成单能力切换。</p>
     *
     * @param userId      用户 ID
     * @param tenantId    租户 ID
     * @param secUid      主播唯一标识
     * @param monitorType 监控类型 0=质检 / 1=还原度 / 2=巡检
     * @param enabled     目标状态 0=关 / 1=开
     * @return R&lt;Boolean&gt;；data=true 表示切换成功（含幂等情况）
     */
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> updateMonitorSwitch(Long userId, Long tenantId, String secUid, Integer monitorType, Integer enabled) {
        // 参数边界校验：enabled 必须是 0 或 1（防 99 等合法 int 写进 db）
        if (enabled == null || !enabled.equals(0) && !enabled.equals(1)) {
            throw new BusinessException(StatusCode.SCRIPT_MONITOR_PARAM_INVALID.getCode(),
                    "enabled 非法，合法值：0=关 / 1=开");
        }
        // 参数边界校验：monitorType 必须是 0/1/2
        if (monitorType == null || (!monitorType.equals(0) && !monitorType.equals(1) && !monitorType.equals(2))) {
            throw new BusinessException(StatusCode.SCRIPT_MONITOR_PARAM_INVALID.getCode(),
                    "monitorType 非法，合法值：0=质检 / 1=还原度 / 2=巡检");
        }

        // 步骤一：查 anchor_url_user 记录（带 tenantId + userId + isRemoveRecord=0 过滤）
        AnchorUrlUserEntity current = anchorUrlUserService.getOne(
                new LambdaQueryWrapper<AnchorUrlUserEntity>()
                        .eq(AnchorUrlUserEntity::getAnchorUrlSecUid, secUid)
                        .eq(AnchorUrlUserEntity::getUserId, userId)
                        .eq(AnchorUrlUserEntity::getTenantId, tenantId)
                        .eq(AnchorUrlUserEntity::getIsRemoveRecord, 0));
        if (current == null) {
            throw new BusinessException(StatusCode.DATA_NOT_EXIST.getCode(),
                    StatusCode.DATA_NOT_EXIST.getMsg());
        }

        // 按 monitorType 路由：取当前值 + 对应 commodityTypeCode + countOpenSwitch 字段名
        Integer curValue;
        String code;
        String switchField;
        switch (monitorType) {
            case 0:
                curValue = current.getIsScriptQualityInspection();
                code = OrderEnums.commodityTypeCode.SCRIPT_QUALITY_NUM.getCode();
                switchField = "isScriptQualityInspection";
                break;
            case 1:
                curValue = current.getIsScriptFidelityMonitor();
                code = OrderEnums.commodityTypeCode.SCRIPT_FIDELITY_NUM.getCode();
                switchField = "isScriptFidelityMonitor";
                break;
            case 2:
                curValue = current.getIsInteractionPatrol();
                code = OrderEnums.commodityTypeCode.INTERACTION_PATROL_NUM.getCode();
                switchField = "isInteractionPatrol";
                break;
            default:
                throw new BusinessException(StatusCode.SCRIPT_MONITOR_PARAM_INVALID.getCode(),
                        "monitorType 非法，合法值：0/1/2");
        }
        int curInt = (curValue == null) ? 0 : curValue;

        // 步骤二：幂等短路 — 状态无变化直接返回
        if (curInt == enabled) {
            return R.ok(true);
        }

        // 步骤三：还原度 0→1 校验（Slice A 解除暂禁，2026-06-11）
        Long fidelityBackfillScriptId = null;
        if (monitorType.equals(1) && enabled.equals(1) && curInt == 0) {
            // 校验 accountType=0（自有账号）
            if (Integer.valueOf(1).equals(current.getAccountType())) {
                log.warn("[B8 还原度校验] 竞品/同行账号拒绝开启还原度 userId={} tenantId={} secUid={} errorCode=70004",
                        userId, tenantId, secUid);
                throw new BusinessException(StatusCode.SCRIPT_MONITOR_NOT_OWN_ACCOUNT.getCode(),
                        StatusCode.SCRIPT_MONITOR_NOT_OWN_ACCOUNT.getMsg());
            }
            // 按 (tenantId+userId+secUid) 查标准稿，无则抛 70005
            StandardScriptEntity standardScript = standardScriptService.findValid(tenantId, userId, current.getAnchorUrlSecUid());
            if (standardScript == null) {
                log.warn("[B8 还原度校验] 无已确认标准稿，请先确认标准稿 userId={} tenantId={} secUid={} errorCode=70005",
                        userId, tenantId, secUid);
                throw new BusinessException(StatusCode.SCRIPT_MONITOR_STANDARD_SCRIPT_UNCONFIRMED.getCode(),
                        StatusCode.SCRIPT_MONITOR_STANDARD_SCRIPT_UNCONFIRMED.getMsg());
            }
            fidelityBackfillScriptId = standardScript.getId();
            log.info("[B8 还原度校验] 通过，待回填 standardScriptId={} userId={} tenantId={} secUid={}",
                    fidelityBackfillScriptId, userId, tenantId, secUid);
        }

        // 步骤四：开启路径校验监控位（仅 0→1）
        if (enabled == 1) {
            MonitorPositionAuthVo auth = userPropertyFeign.checkMonitorPosition(userId, code);
            if (auth == null || !Boolean.TRUE.equals(auth.getHasSurplus())) {
                log.warn("[B8 setMonitorEnabled] 监控位不足 userId={} tenantId={} secUid={} code={} errorCode=70002",
                        userId, tenantId, secUid, code);
                throw new BusinessException(StatusCode.SCRIPT_MONITOR_QUOTA_NOT_ENOUGH.getCode(),
                        StatusCode.SCRIPT_MONITOR_QUOTA_NOT_ENOUGH.getMsg());
            }
        }

        // 步骤五：单字段写库（LambdaUpdateWrapper，只写对应开关字段）
        LambdaUpdateWrapper<AnchorUrlUserEntity> updateWrapper = new LambdaUpdateWrapper<AnchorUrlUserEntity>()
                        .eq(AnchorUrlUserEntity::getId, current.getId())
                        .eq(AnchorUrlUserEntity::getUserId, userId)
                        .eq(AnchorUrlUserEntity::getTenantId, tenantId)
                        .eq(AnchorUrlUserEntity::getIsRemoveRecord, 0);
        switch (monitorType) {
            case 0:
                updateWrapper.set(AnchorUrlUserEntity::getIsScriptQualityInspection, enabled);
                break;
            case 1:
                updateWrapper.set(AnchorUrlUserEntity::getIsScriptFidelityMonitor, enabled);
                break;
            case 2:
                updateWrapper.set(AnchorUrlUserEntity::getIsInteractionPatrol, enabled);
                break;
            default:
                break;
        }
        // 还原度 0→1 时同步回填辅助索引 standard_script_id
        if (fidelityBackfillScriptId != null) {
            updateWrapper.set(AnchorUrlUserEntity::getStandardScriptId, fidelityBackfillScriptId);
        }
        anchorUrlUserService.update(updateWrapper);

        // 步骤六：统计开启数量并回写资产占用量
        Long openCount = anchorUrlUserProducer.countOpenSwitch(userId, tenantId, switchField);
        boolean ok = userPropertyFeign.updateByPropertyNumRetBoolean(userId, code, openCount);
        if (!ok) {
            log.warn("[B8 setMonitorEnabled] 资产占用量更新失败 userId={} tenantId={} secUid={} code={} openCount={}",
                    userId, tenantId, secUid, code, openCount);
            throw new RuntimeException("资产占用量更新失败, userId=" + userId + ", code=" + code);
        }

        return R.ok(true);
    }

    /**
     * 获取授权状态的优先级值：如果存在状态为1（已授权）的记录，则返回1；否则返回第一个非空状态
     */
    private Integer getPriorityAuthStatus(List<AnchorUrlUserEntity> entities, java.util.function.Function<AnchorUrlUserEntity, Integer> getter) {
        List<Integer> statuses = entities.stream().map(getter).filter(Objects::nonNull).distinct().toList();
        if (statuses.contains(1)) {
            return 1;
        }
        return statuses.isEmpty() ? null : statuses.get(0);
    }
}
