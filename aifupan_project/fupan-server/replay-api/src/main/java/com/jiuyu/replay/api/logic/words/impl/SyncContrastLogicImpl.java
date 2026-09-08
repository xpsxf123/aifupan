package com.jiuyu.replay.api.logic.words.impl;

import cn.hutool.core.util.ObjectUtil;
import com.jiuyu.replay.api.logic.words.SyncContrastLogic;
import com.jiuyu.replay.common.tencent.TencentVodUtils;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.utils.ResultUtil;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.power.SalesInfoVo;
import com.jiuyu.replay.generic.vo.power.SalesVo;
import com.jiuyu.replay.generic.vo.words.AnchorVideoInfoVo;
import com.jiuyu.replay.order.bll.OrderBll;
import com.jiuyu.replay.order.bll.UserPropertyBll;
import com.jiuyu.replay.order.vo.OrderFinallyVo;
import com.jiuyu.replay.power.bll.SalesBll;
import com.jiuyu.replay.power.bll.UserBll;
import com.jiuyu.replay.power.bll.UserDetailsBll;
import com.jiuyu.replay.power.utils.GlobalObject;
import com.jiuyu.replay.power.vo.UserCacheVo;
import com.jiuyu.replay.power.vo.UserInfoVo;
import com.jiuyu.replay.power.vo.UserListVo;
import com.jiuyu.replay.words.bll.*;
import com.jiuyu.replay.words.bo.ClientContrastListBo;
import com.jiuyu.replay.words.bo.SyncContrastBo;
import com.jiuyu.replay.words.bo.SyncContrastListBo;
import com.jiuyu.replay.words.constant.Constant;
import com.jiuyu.replay.words.vo.OnlineContrastAnalysisInfoVo;
import com.jiuyu.replay.words.vo.SyncContrastInfoVo;
import com.jiuyu.replay.words.vo.SyncContrastListVo;
import com.jiuyu.replay.words.vo.SyncContrastVo;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;


/**
 * 客户端对比数据
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-26 14:51:59
 */
@Service
public class SyncContrastLogicImpl implements SyncContrastLogic {

    @Resource
    private SyncContrastBll syncContrastBll;
    @Resource
    private UserBll userBll;
    @Resource
    private TradeBll tradeBll;
    @Resource
    private AnchorVideoBll anchorVideoBll;
    @Resource
    private UploadFileBll uploadFileBll;
    @Resource
    private SensitiveWordsBll sensitiveWordsBll;
    @Resource
    private SalesBll salesBll;
    @Resource
    private UserDetailsBll userDetailsBll;
    @Resource
    private TencentVodUtils tencentVodUtils;
    @Resource
    private OrderBll orderBll;
    @Autowired
    private UserPropertyBll userPropertyBll;

    @Override
    public R<PageUtils<SyncContrastListVo>> queryPage(SyncContrastListBo syncContrastListBo) {

        return syncContrastBll.queryPage(syncContrastListBo);
    }

    @Override
    public R<SyncContrastInfoVo> info(Long id) {

        return syncContrastBll.info(id);
    }

    @Override
    public R<String> save(SyncContrastBo syncContrastBo) {

        UserCacheVo user = GlobalObject.getLocalUser();
        syncContrastBo.setUserId(user.getId());
        syncContrastBo.setTenantId(user.getActiveTenantId());

        return syncContrastBll.save(syncContrastBo);
    }

    @Override
    public R<String> update(SyncContrastBo syncContrastBo) {

        UserCacheVo user = GlobalObject.getLocalUser();
        syncContrastBo.setUserId(user.getId());
        syncContrastBo.setTenantId(user.getActiveTenantId());

        return syncContrastBll.update(syncContrastBo);
    }

    @Override
    public R<String> delete(Long id) {

        return syncContrastBll.delete(id);
    }

    @Override
    public R<List<SyncContrastInfoVo>> listByToken() {

        UserCacheVo user = GlobalObject.getLocalUser();

        return syncContrastBll.listByToken(user.getId());
    }

    @Override
    public R<SyncContrastInfoVo> infoByContrastId(String contrastId) {

        return syncContrastBll.infoByContrastId(contrastId);
    }


    @Override
    public R<PageUtils<SyncContrastListVo>> listAllSyncContrastNew(SyncContrastListBo syncContrastListBo) {

        PageUtils<SyncContrastListVo> res = syncContrastBll.listAllSyncContrastNew(syncContrastListBo);

        if (res != null && ObjectUtil.isNotEmpty(res.getList())) {

            List<Long> userIds = res.getList().stream().map(SyncContrastVo::getUserId).distinct().toList();
            Map<Long, OrderFinallyVo> longOrderFinallyVoMap = orderBll.currentOrderFinallyByUserIds(userIds);

            Map<Long, String> salesMap = new HashMap<>();

            List<Long> salesIds = res.getList().stream().map(SyncContrastListVo::getSalesId).distinct().toList();
            if (ObjectUtil.isNotEmpty(salesIds)) {
                List<SalesInfoVo> salesList = ResultUtil.getResult(salesBll.listByIds(salesIds));
                if (salesList != null && !salesList.isEmpty()) {
                    salesMap = salesList.stream().collect(Collectors.toMap(SalesVo::getId, SalesVo::getSalesName, (o, n) -> o));
                }
            }

            for (SyncContrastListVo item : res.getList()) {
                OrderFinallyVo vo = longOrderFinallyVoMap.get(item.getUserId());
                item.setPackageName(vo == null ? null : vo.getCommodityName());
                item.setPackageExpiredTime(vo == null ? null : vo.getEndDate());

                item.setUserSales(salesMap.get(item.getSalesId()));
            }
        }
        return R.ok(res);
    }

    /**
     * 取联合查询出来的用户ID集合的交集
     * @param collections 所有集合
     * @return
     * @param <T>
     */
    public static <T> Collection<T> intersectionOrSingleSet(List<Collection<T>> collections) {
        if (collections == null || collections.isEmpty()) {
            return Collections.emptyList();
        }

        List<Set<T>> filteredSets = new ArrayList<>();
        for (Collection<T> collection : collections) {
            if (collection != null && !collection.isEmpty()) {
                filteredSets.add(new HashSet<>(collection)); // 确保所有的集合都是 Set 类型
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
     * PC后端获取对比分析数据
     * @param contrastId 对比分析唯一标识
     * @return
     */
    @Override
    public R<OnlineContrastAnalysisInfoVo> getContrastAnalysisInfo(String contrastId) {
        return sensitiveWordsBll.getContrastAnalysisInfo(contrastId);
    }

    @Override
    public R<PageUtils<SyncContrastInfoVo>> clientContrastList(ClientContrastListBo clientContrastListBo) {

        UserCacheVo user = GlobalObject.getLocalUser();
        clientContrastListBo.setUserId(user.getId());
        clientContrastListBo.setTenantId(user.getActiveTenantId());
        clientContrastListBo.setDeleteStatus(0);

        return syncContrastBll.clientContrastList(clientContrastListBo);
    }

    @Override
    public R<String> clientDeleteContrast(List<String> ids) {

        UserCacheVo user = GlobalObject.getLocalUser();
        // 子账号自能操作自己的
        Long userId = user.getUserType() == 2 ? user.getId() : null;

        return syncContrastBll.clientDeleteContrast(ids, user.getActiveTenantId(), userId);
    }

    @Override
    public R<String> clientAddContrast(SyncContrastBo syncContrastBo) {

        UserCacheVo user = GlobalObject.getLocalUser();
        syncContrastBo.setUserId(user.getId());
        syncContrastBo.setTenantId(user.getActiveTenantId());

        return syncContrastBll.clientAddContrast(syncContrastBo);
    }

    @Override
    public R<String> clientDeleteCloudContrast(String contrastId) {

        return syncContrastBll.clientDeleteCloudContrast(contrastId);
    }

    @Override
    public R<PageUtils<SyncContrastInfoVo>> clientListCloudContrast(ClientContrastListBo clientContrastListBo) {

        UserCacheVo user = GlobalObject.getLocalUser();
        clientContrastListBo.setTenantId(user.getActiveTenantId());
        clientContrastListBo.setIsShard(1);

        if(!StringUtils.isEmpty(clientContrastListBo.getUserKeyword())) {
            // 查用户id集合
            R<List<Long>> userListR = this.userBll.listIdsByLikePhoneOrName(clientContrastListBo.getUserKeyword());
            List<Long> userIds = userListR.getData();
            if(userListR.getCode() != 0 || userIds == null || userIds.size() < 1) {
                userIds = new LinkedList<>();
                userIds.add(0L);
            }

            clientContrastListBo.setUserIdList(userIds);
        }

        R<PageUtils<SyncContrastInfoVo>> pageUtilsR = syncContrastBll.clientContrastList(clientContrastListBo);

        // 设置用户昵称
        PageUtils<SyncContrastInfoVo> data = pageUtilsR.getData();
        if(data != null) {
            List<SyncContrastInfoVo> list = data.getList();
            if(list != null && list.size() > 0) {
                Set<Long> userIds = list.stream().map(SyncContrastVo::getUserId).collect(Collectors.toSet());
                R<List<UserListVo>> userListR = this.userBll.listByIds(userIds);
                if(userListR.getCode() == 0) {
                    List<UserListVo> userList = userListR.getData();
                    if(userList != null && userList.size() > 0) {
                        for (SyncContrastInfoVo syncContrastInfoVo : list) {
                            for (UserListVo userListVo : userList) {
                                if(syncContrastInfoVo.getUserId().equals(userListVo.getId())) {
                                    syncContrastInfoVo.setUserNickName(userListVo.getNickName());
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }

        return pageUtilsR;
    }

    @Override
    public R<SyncContrastInfoVo> clientGetContrast(String contrastId) {

        return syncContrastBll.infoByContrastId(contrastId);
    }

    @Override
    public R<String> clientAddCloudContrast(SyncContrastBo syncContrastBo) {

        UserCacheVo user = GlobalObject.getLocalUser();
        syncContrastBo.setUserId(user.getId());
        syncContrastBo.setTenantId(user.getActiveTenantId());

        return syncContrastBll.clientAddCloudContrast(syncContrastBo);
    }



    /**
     * 从云空间删除视频
     * @param videoId 视频唯一标识
     */
    private void deleteCloudVideo(String videoId) {
        // 获取视频信息
        R<AnchorVideoInfoVo> videoInfoVoR = this.anchorVideoBll.GetByVideoId(videoId);
        if(videoInfoVoR.getCode() == 0 && videoInfoVoR.getData() != null) {

            AnchorVideoInfoVo videoInfoVo = videoInfoVoR.getData();

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

                UserCacheVo user = GlobalObject.getLocalUser();
                R<Long> longR = anchorVideoBll.statisticsStoreByTenantId(user.getActiveTenantId());
                if (longR.getCode() == 0) {
                    UserInfoVo currentUser = ResultUtil.getResult(userBll.info(user.getId(), false));
                    if (currentUser != null) {
                        userPropertyBll.updateByPropertyNum(ObjectUtil.defaultIfNull(currentUser.getParentId(), 0L) > 0 ? currentUser.getParentId() : user.getId(), "storageNum", longR.getData());
                    }
                }
            }

        }
    }


}
