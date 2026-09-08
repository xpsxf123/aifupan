package com.jiuyu.replay.words.bll;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jiuyu.replay.generic.bo.words.ShareContrastCloudBo;
import com.jiuyu.replay.generic.dto.words.ShareContrastCloudDto;
import com.jiuyu.replay.generic.enums.words.SourceStarTypeEnum;
import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.utils.ResultUtil;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.power.UserCacheVo;
import com.jiuyu.replay.generic.vo.words.AnchorUrlInfoVo;
import com.jiuyu.replay.generic.vo.words.AnchorUrlUserVo;
import com.jiuyu.replay.generic.vo.words.AnchorVideoInfoVo;
import com.jiuyu.replay.generic.vo.words.SourceStarVo;
import com.jiuyu.replay.words.bo.*;
import com.jiuyu.replay.words.constant.Constant;
import com.jiuyu.replay.words.entity.SyncContrastEntity;
import com.jiuyu.replay.words.entity.VideoDataViewingConfuseEntity;
import com.jiuyu.replay.words.entity.VideoDataViewingParagraphEntity;
import com.jiuyu.replay.words.enums.DataViewingStatusEnum;
import com.jiuyu.replay.words.producer.*;
import com.jiuyu.replay.words.repository.service.VideoDataViewingConfuseService;
import com.jiuyu.replay.words.repository.service.VideoDataViewingParagraphService;
import com.jiuyu.replay.words.rse.ContrastRse;
import com.jiuyu.replay.words.rse.SourceStarRse;
import com.jiuyu.replay.words.vo.SocketCollectMessageInfoVo;
import com.jiuyu.replay.words.vo.SyncContrastInfoVo;
import com.jiuyu.replay.words.vo.SyncContrastListVo;
import com.jiuyu.replay.words.vo.VideoContrastTypeVo;
import com.jiuyu.replay.words.vo.file.UploadFileInfoVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * 客户端对比数据
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-26 14:51:59
 */
@Component
public class SyncContrastBll {

    @Resource
    private SyncContrastProducer syncContrastProducer;
    @Resource
    private AnchorUrlProducer anchorUrlProducer;
    @Resource
    private AnchorVideoProducer anchorVideoProducer;
    @Resource
    private UploadFileProducer uploadFileProducer;
    @Resource
    private ClientAiFavProducer clientAiFavProducer;
    @Resource
    private UserFeign userFeign;
    @Resource
    private ContrastRse contrastRse;
    @Resource
    private AnchorUrlUserProducer anchorUrlUserProducer;
    @Resource
    private VideoDataViewingParagraphService videoDataViewingParagraphService;
    @Resource
    private VideoDataViewingConfuseService videoDataViewingConfuseService;
    @Resource
    private SourceStarRse sourceStarRse;
    @Resource
    private SocketCollectMessageProducer socketCollectMessageProducer;


    /**
     * 客户端对比数据列表
     * @param syncContrastListBo 客户端对比数据列表查询参数
     * @return
     */
    public R<PageUtils<SyncContrastListVo>> queryPage(SyncContrastListBo syncContrastListBo) {

        return R.ok("获取成功", syncContrastProducer.queryPage(syncContrastListBo));
    }

    /**
    * 客户端对比数据信息
    * @param id 客户端对比数据id
    * @return
    */
    public R<SyncContrastInfoVo> info(Long id) {

        SyncContrastInfoVo syncContrastInfoVo = syncContrastProducer.info(id);
        return R.ok("获取成功", syncContrastInfoVo);
    }

    /**
     * 新增客户端对比数据
     * @param syncContrastBo 客户端对比数据对象
     * @return
     */
    public R<String> save(SyncContrastBo syncContrastBo) {

        if(!StringUtils.isEmpty(syncContrastBo.getVideoOneId())) {
            // 视频对比
            syncContrastBo.setContrastType(0);
            AnchorVideoInfoVo videoOne = this.anchorVideoProducer.getByVideoId(syncContrastBo.getVideoOneId());
            if(videoOne == null) {
                return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "视频数据不存在");
            }
            AnchorVideoInfoVo videoTwo = this.anchorVideoProducer.getByVideoId(syncContrastBo.getVideoTwoId());
            if(videoTwo == null) {
                return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "视频数据不存在");
            }
            syncContrastBo.setTradeOneId(videoOne.getTradeId());
            syncContrastBo.setTradeTwoId(videoTwo.getTradeId());
            syncContrastBo.setAnchorOneId(videoOne.getSecUid());
            syncContrastBo.setAnchorTwoId(videoTwo.getSecUid());
            syncContrastBo.setIsShard(0);
        }else {
            // 文件对比
            syncContrastBo.setContrastType(1);
            UploadFileInfoVo fileOne = this.uploadFileProducer.getByFileId(syncContrastBo.getFileOneId());
            if(fileOne == null) {
                return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "文件数据不存在");
            }
            UploadFileInfoVo fileTwo = this.uploadFileProducer.getByFileId(syncContrastBo.getFileTwoId());
            if(fileTwo == null) {
                return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "文件数据不存在");
            }
            syncContrastBo.setTradeOneId(fileOne.getTradeId());
            syncContrastBo.setTradeTwoId(fileTwo.getTradeId());
            syncContrastBo.setIsShard(0);
        }


        SyncContrastInfoVo syncContrastInfoVo = syncContrastProducer.save(syncContrastBo);
        return R.ok("添加成功");
    }

    /**
     * 修改客户端对比数据
     * @param syncContrastBo 客户端对比数据对象
     * @return
     */
    public R<String> update(SyncContrastBo syncContrastBo) {

        syncContrastProducer.update(syncContrastBo);
        return R.ok("修改成功");
    }

    /**
     * 删除客户端对比数据
     * @param id 客户端对比数据id
     * @return
     */
    public R<String> delete(Long id) {

        syncContrastProducer.deleteById(id);
        return R.ok("删除成功");
    }


    /**
     * 获取用户的对比数据列表
     * @return
     */
    public R<List<SyncContrastInfoVo>> listByToken(Long userId) {

        List<SyncContrastInfoVo> list = syncContrastProducer.listByUserId(userId);

        return R.ok(list);
    }

    /**
     * 获取云空间对比列表
     * @param cloudContrastListBo 请求参数
     * @return
     */
    public R<PageUtils<SyncContrastListVo>> listCloudContrast(CloudContrastListBo cloudContrastListBo) {

        PageUtils<SyncContrastListVo> pageUtils = syncContrastProducer.listCloudContrast(cloudContrastListBo);

        List<SyncContrastListVo> list = pageUtils.getList();
        if(list != null && list.size() > 0) {

            List<String> videoIds = new LinkedList<>();
            List<String> secUids = new LinkedList<>();
            for (SyncContrastListVo syncContrastListVo : list) {
                videoIds.add(syncContrastListVo.getVideoOneId());
                videoIds.add(syncContrastListVo.getVideoTwoId());
                secUids.add(syncContrastListVo.getAnchorOneId());
                secUids.add(syncContrastListVo.getAnchorTwoId());
            }
            // 封装视频数据
            List<AnchorVideoInfoVo> anchorVideoInfoVoList = this.anchorVideoProducer.listByVideoIds(videoIds);
            if(anchorVideoInfoVoList != null && anchorVideoInfoVoList.size() > 0) {
                for (SyncContrastListVo syncContrastListVo : list) {
                    for (AnchorVideoInfoVo anchorVideoInfoVo : anchorVideoInfoVoList) {
                        if(anchorVideoInfoVo.getVideoId().equals(syncContrastListVo.getVideoOneId())) {
                            syncContrastListVo.setVideoInfoOne(anchorVideoInfoVo);
                        }
                        if(anchorVideoInfoVo.getVideoId().equals(syncContrastListVo.getVideoTwoId())) {
                            syncContrastListVo.setVideoInfoTwo(anchorVideoInfoVo);
                        }
                    }
                }
            }

            // 封装主播数据
            List<AnchorUrlInfoVo> anchorUrlInfoVos = this.anchorUrlProducer.listBySecUids(secUids);
            if(anchorUrlInfoVos != null && anchorUrlInfoVos.size() > 0) {
                for (SyncContrastListVo syncContrastListVo : list) {
                    for (AnchorUrlInfoVo anchorUrlInfoVo : anchorUrlInfoVos) {
                        if(anchorUrlInfoVo.getSecUid().equals(syncContrastListVo.getAnchorOneId())) {
                            syncContrastListVo.setAnchorInfoOne(anchorUrlInfoVo);
                        }
                        if(anchorUrlInfoVo.getSecUid().equals(syncContrastListVo.getAnchorTwoId())) {
                            syncContrastListVo.setAnchorInfoTwo(anchorUrlInfoVo);
                        }
                    }
                }
            }

        }

        return R.ok(pageUtils);
    }

    /**
     * 根据对比唯一标识获取对比数据信息
     * @param contrastId 对比唯一标识
     * @return
     */
    public R<SyncContrastInfoVo> infoByContrastId(String contrastId) {

        SyncContrastInfoVo syncContrastInfoVo = this.syncContrastProducer.infoByContrastId(contrastId);
        if(syncContrastInfoVo != null) {
            return R.ok(syncContrastInfoVo);
        }

        return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "数据不存在");
    }

    /**
     * 切换对比定位（交换视频1和视频2、主播1和主播2的位置）
     * @param contrastId 对比唯一标识
     * @return
     */
    public R<String> switchContrastPosition(String contrastId) {
        SyncContrastInfoVo syncContrastInfoVo = this.syncContrastProducer.infoByContrastId(contrastId);
        if (syncContrastInfoVo == null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "对比记录不存在");
        }
        this.syncContrastProducer.switchContrastPosition(contrastId);
        return R.ok("切换成功");
    }

    /**
     * 查询所有对比分析记录
     * @return
     */
    public List<SyncContrastEntity> listAllContrast() {
        return this.syncContrastProducer.listAllContrast();
    }

    /**
     * 查询所有对比分析记录且填充用户昵称、直播间昵称、行业
     * @param syncContrastListBo 查询参数
     * @return
     */
    public R<PageUtils<SyncContrastListVo>> listAllSyncContrast(SyncContrastListBo syncContrastListBo) {
        // 这里切换到queryPage进行调用
        return R.ok("获取成功", syncContrastProducer.queryPage(syncContrastListBo));
    }

    /**
     * 客户端获取对比列表
     * @param clientContrastListBo 查询参数
     * @return
     */
    public R<PageUtils<SyncContrastInfoVo>> clientContrastList(ClientContrastListBo clientContrastListBo) {

        PageUtils<SyncContrastInfoVo> pageUtils = syncContrastProducer.clientContrastList(clientContrastListBo);

        List<SyncContrastInfoVo> list = pageUtils.getList();
        if (list != null && !list.isEmpty()) {
            // 获取所有对比id
            List<String> contrastIds = list.stream().map(SyncContrastInfoVo::getContrastId).collect(Collectors.toList());
            // 查询是否设置了星标
            List<SourceStarVo> sourceStarVos = sourceStarRse.listBySourceIds(contrastIds, SourceStarTypeEnum.CONTRAST.getCode());
            Map<String, SourceStarVo> sourceStarMap = sourceStarVos.stream().collect(Collectors.toMap(SourceStarVo::getSourceId, Function.identity(), (a, b) -> a));
            // 设置星标状态
            for (SyncContrastInfoVo item : list) {
                item.setHasStar(0);
                SourceStarVo sourceStarVo = sourceStarMap.get(item.getContrastId());
                if(sourceStarVo != null) {
                    item.setHasStar(1);
                    item.setSourceStarInfo(sourceStarVo);
                }
            }
        }

        return R.ok("获取成功", pageUtils);
    }

    /**
     * 客户端删除对比
     * @param ids 视频uuid集合
     * @param tenantId 租户id
     * @param userId 用户id
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public R<String> clientDeleteContrast(List<String> ids, Long tenantId, Long userId) {

        if(ids == null || ids.size() < 1) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "输入的数据为空");
        }

        List<String> delIds = syncContrastProducer.getAllowDeleteContrast(ids, tenantId, userId);
        if(delIds == null || delIds.size() < 1) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "没有权限");
        }

//        List<SyncContrastInfoVo> syncContrastInfoVos = this.syncContrastProducer.listByContrastIds(delIds);
//        if(syncContrastInfoVos == null || syncContrastInfoVos.size() < 1) {
//            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "数据不存在");
//        }
//
//        // 筛选出已上传云空间和未上传云空间的对比列表
//        List<SyncContrastInfoVo> notUploadList = syncContrastInfoVos.stream().filter(item -> item.getIsShard() == 0).toList();
//        List<SyncContrastInfoVo> uploadList = syncContrastInfoVos.stream().filter(item -> item.getIsShard() == 1).toList();
//
//        if(notUploadList.size() > 0) {
//            // 未上传云空间的，直接删除
//            List<String> contrastIds = notUploadList.stream().map(SyncContrastVo::getContrastId).toList();
//            syncContrastProducer.removeByContrastIds(contrastIds);
//        }
//        if(uploadList.size() > 0) {
//            // 已上传云空间，改删除状态
//            List<String> contrastIds = uploadList.stream().map(SyncContrastVo::getContrastId).toList();
//            syncContrastProducer.batchUpdateContrastDelStatus(contrastIds, 1);
//        }

        // 修改对比记录的删除状态
        syncContrastProducer.batchUpdateContrastDelStatus(ids, 1);

        // 删除关联的运营/违规助手信息
        this.clientAiFavProducer.removeByResourceIds(ids);

        return R.ok("删除成功");
    }

    /**
     * 客户端添加对比
     * @param syncContrastBo 对比数据
     * @return
     */
    public R<String> clientAddContrast(SyncContrastBo syncContrastBo) {

        if(!StringUtils.isEmpty(syncContrastBo.getVideoOneId()) && !StringUtils.isEmpty(syncContrastBo.getVideoTwoId())) {

            // 检查是否已经存在相同的对比记录，存在则更新修改时间
            SyncContrastInfoVo syncContrastInfoVo = this.syncContrastProducer.checkExistUpdate(0, syncContrastBo.getVideoOneId(), syncContrastBo.getVideoTwoId(), syncContrastBo.getUserId(), 0, syncContrastBo.getSyncScene());
            if(syncContrastInfoVo != null) {
                return R.ok("添加成功", syncContrastInfoVo.getContrastId());
            }

            // 视频对比
            AnchorVideoInfoVo videoOne = this.anchorVideoProducer.getByVideoId(syncContrastBo.getVideoOneId());
            if(videoOne == null || !videoOne.getUserId().equals(syncContrastBo.getUserId())) {
                return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "没有权限");
            }
            AnchorVideoInfoVo videoTwo = this.anchorVideoProducer.getByVideoId(syncContrastBo.getVideoTwoId());
            if(videoTwo == null || !videoTwo.getUserId().equals(syncContrastBo.getUserId())) {
                return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "没有权限");
            }
            syncContrastBo.setContrastType(0);
            syncContrastBo.setTradeOneId(videoOne.getTradeId());
            syncContrastBo.setTradeTwoId(videoTwo.getTradeId());
            syncContrastBo.setAnchorOneId(videoOne.getSecUid());
            syncContrastBo.setAnchorTwoId(videoTwo.getSecUid());
            syncContrastBo.setIsShard(0);
            // 判断是同主播还是不通主播
            if (syncContrastBo.getSyncScene() == null) {
                syncContrastBo.setSyncScene(ObjectUtil.equals(videoOne.getSecUid(), videoTwo.getSecUid()) ? 3 : 2);
            }
        }else if(!StringUtils.isEmpty(syncContrastBo.getFileOneId()) && !StringUtils.isEmpty(syncContrastBo.getFileTwoId())) {

            // 检查是否已经存在相同的对比记录，存在则更新修改时间
            SyncContrastInfoVo syncContrastInfoVo = this.syncContrastProducer.checkExistUpdate(1, syncContrastBo.getFileOneId(), syncContrastBo.getFileTwoId(), syncContrastBo.getUserId(), 0, syncContrastBo.getSyncScene());
            if(syncContrastInfoVo != null) {
                return R.ok("添加成功", syncContrastInfoVo.getContrastId());
            }

            // 文件对比
            UploadFileInfoVo fileOne = this.uploadFileProducer.getByFileId(syncContrastBo.getFileOneId());
            if(fileOne == null || !fileOne.getUserId().equals(syncContrastBo.getUserId())) {
                return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "没有权限");
            }
            UploadFileInfoVo fileTwo = this.uploadFileProducer.getByFileId(syncContrastBo.getFileTwoId());
            if(fileTwo == null || !fileTwo.getUserId().equals(syncContrastBo.getUserId())) {
                return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "没有权限");
            }
            syncContrastBo.setContrastType(1);
            syncContrastBo.setTradeOneId(fileOne.getTradeId());
            syncContrastBo.setTradeTwoId(fileTwo.getTradeId());
            syncContrastBo.setIsShard(0);
        }else {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "输入的数据异常");
        }

        SyncContrastInfoVo contrastInfoVo = syncContrastProducer.save(syncContrastBo);

        return R.ok("添加成功", contrastInfoVo.getContrastId());
    }

    /**
     * 客户端删除云空间对比
     * @param contrastId 对比uuid
     * @return
     */
    public R<String> clientDeleteCloudContrast(String contrastId) {
        SyncContrastInfoVo syncContrastInfoVo = this.syncContrastProducer.infoByContrastId(contrastId);
        if(syncContrastInfoVo == null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "数据不存在");
        }

        List<String> contrastIds = new LinkedList<>();
        contrastIds.add(contrastId);

        // 修改视频的上传状态
        this.syncContrastProducer.batchUpdateContrastIsShare(contrastIds, 0);

        if(syncContrastInfoVo.getDeleteStatus() == 1) {
            // 修改视频的删除状态
            this.syncContrastProducer.batchUpdateContrastDelStatus(contrastIds, 2);
        }

        return R.ok("删除成功");
    }

    /**
     * 客户端添加云空间对比
     * @param syncContrastBo 对比数据
     * @return
     */
    public R<String> clientAddCloudContrast(SyncContrastBo syncContrastBo) {

        if(!StringUtils.isEmpty(syncContrastBo.getVideoOneId()) && !StringUtils.isEmpty(syncContrastBo.getVideoTwoId())) {

            // 检查是否已经存在相同的对比记录，存在则更新修改时间
            SyncContrastInfoVo syncContrastInfoVo = this.syncContrastProducer.checkExistUpdate(0, syncContrastBo.getVideoOneId(), syncContrastBo.getVideoTwoId(), syncContrastBo.getUserId(), 1, syncContrastBo.getSyncScene());
            if(syncContrastInfoVo != null) {
                return R.ok("添加成功", syncContrastInfoVo.getContrastId());
            }

            // 判断视频是否是自己的
            boolean isMyself = true;

            // 视频对比
            AnchorVideoInfoVo videoOne = this.anchorVideoProducer.getByVideoId(syncContrastBo.getVideoOneId());
            if(videoOne == null) {
                return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "视频信息不存在");
            }
            if(!videoOne.getUserId().equals(syncContrastBo.getUserId())) {
                isMyself = false;
            }
            AnchorVideoInfoVo videoTwo = this.anchorVideoProducer.getByVideoId(syncContrastBo.getVideoTwoId());
            if(videoTwo == null) {
                return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "视频信息不存在");
            }
            if(!videoTwo.getUserId().equals(syncContrastBo.getUserId())) {
                isMyself = false;
            }

            if(!isMyself) {
                // 因为不是自己的视频,只在云空间列表展示
                syncContrastBo.setDeleteStatus(1);
            }

            syncContrastBo.setContrastType(0);
            syncContrastBo.setTradeOneId(videoOne.getTradeId());
            syncContrastBo.setTradeTwoId(videoTwo.getTradeId());
            syncContrastBo.setAnchorOneId(videoOne.getSecUid());
            syncContrastBo.setAnchorTwoId(videoTwo.getSecUid());
            syncContrastBo.setIsShard(1);

        }
//        else if(!StringUtils.isEmpty(syncContrastBo.getFileOneId()) && !StringUtils.isEmpty(syncContrastBo.getFileTwoId())) {
//
//            // 检查是否已经存在相同的对比记录，存在则更新修改时间
//            SyncContrastInfoVo syncContrastInfoVo = this.syncContrastProducer.checkExistUpdate(1, syncContrastBo.getFileOneId(), syncContrastBo.getFileTwoId(), syncContrastBo.getUserId(), 1);
//            if(syncContrastInfoVo != null) {
//                return R.ok("添加成功", syncContrastInfoVo.getContrastId());
//            }
//
//            // 文件对比
//            UploadFileInfoVo fileOne = this.uploadFileProducer.getByFileId(syncContrastBo.getFileOneId());
//            if(fileOne == null || !fileOne.getUserId().equals(syncContrastBo.getUserId())) {
//                return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "没有权限");
//            }
//            UploadFileInfoVo fileTwo = this.uploadFileProducer.getByFileId(syncContrastBo.getFileTwoId());
//            if(fileTwo == null || !fileTwo.getUserId().equals(syncContrastBo.getUserId())) {
//                return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "没有权限");
//            }
//            syncContrastBo.setContrastType(1);
//            syncContrastBo.setTradeOneId(fileOne.getTradeId());
//            syncContrastBo.setTradeTwoId(fileTwo.getTradeId());
//        }
        else {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "输入的数据异常");
        }

        SyncContrastInfoVo contrastInfoVo = syncContrastProducer.save(syncContrastBo);

        return R.ok("添加成功", contrastInfoVo.getContrastId());
    }

    /**
     * 分析对比记录到云空间
     * @param shareContrastCloudBo 分享信息
     * @return 分享地址
     */
    public R<String> shareContrastToCloud(ShareContrastCloudBo shareContrastCloudBo) {

        UserCacheVo userCacheVo = ResultUtil.getResult(userFeign.getLocalUser());
        if(userCacheVo == null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "用户信息不存在");
        }

        SyncContrastInfoVo contrastInfoVo = this.contrastRse.infoByContrastIdAndUserId(shareContrastCloudBo.getContrastId(), userCacheVo.getId());
        if(contrastInfoVo == null || contrastInfoVo.getDeleteStatus() != 0) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "视频信息不存在");
        }
        if(contrastInfoVo.getIsShard() == 1) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "请勿重复上传");
        }

        ShareContrastCloudDto shareContrastCloudDto = new ShareContrastCloudDto();
        shareContrastCloudDto.setId(contrastInfoVo.getId());
        shareContrastCloudDto.setContrastId(shareContrastCloudBo.getContrastId());
        shareContrastCloudDto.setCloudRemarks(shareContrastCloudBo.getCloudRemarks());

        String shareUrl = this.contrastRse.shareContrastToCloud(shareContrastCloudDto);

        return R.ok("分享成功", shareUrl);
    }

    /**
     * 分析记录页面-对比分析列表分页接口
     *
     * @param syncContrastListBo 查询参数
     * @return 分页数据
     */
    public PageUtils<SyncContrastListVo> listAllSyncContrastNew(SyncContrastListBo syncContrastListBo) {

        return syncContrastProducer.pageSyncContrastNew(syncContrastListBo);
    }

    /**
     * 判断视频对比类型（优化场次/对标场次）
     *
     * @param videoContrastTypeBo 入参，包含两个视频ID
     * @return 优化视频ID和对标视频ID
     */
    public R<VideoContrastTypeVo> determineVideoContrastType(VideoContrastTypeBo videoContrastTypeBo) {
        // 参数校验
        if (StringUtils.isEmpty(videoContrastTypeBo.getVideoOneId()) || StringUtils.isEmpty(videoContrastTypeBo.getVideoTwoId())) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "视频ID不能为空");
        }

        // 获取视频1信息
        AnchorVideoInfoVo videoOne = this.anchorVideoProducer.getByVideoId(videoContrastTypeBo.getVideoOneId());
        if (videoOne == null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "视频1不存在");
        }

        // 获取视频2信息
        AnchorVideoInfoVo videoTwo = this.anchorVideoProducer.getByVideoId(videoContrastTypeBo.getVideoTwoId());
        if (videoTwo == null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "视频2不存在");
        }

        // 获取视频1对应的主播用户信息
        AnchorUrlUserVo anchorUserOne = this.anchorUrlUserProducer.getUserAnchorBySecUid(videoOne.getSecUid(), videoOne.getUserId(), videoOne.getTenantId());
        if (anchorUserOne == null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "视频1主播不存在");
        }
        // 获取视频2对应的主播用户信息
        AnchorUrlUserVo anchorUserTwo = this.anchorUrlUserProducer.getUserAnchorBySecUid(videoTwo.getSecUid(), videoTwo.getUserId(), videoTwo.getTenantId());
        if (anchorUserTwo == null) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "视频2主播不存在");
        }

        // 获取账号归属类型，默认为0（自有账号）
        Integer accountTypeOne = anchorUserOne.getAccountType() != null ? anchorUserOne.getAccountType() : 0;
        Integer accountTypeTwo = anchorUserTwo.getAccountType() != null ? anchorUserTwo.getAccountType() : 0;

        VideoContrastTypeVo result = new VideoContrastTypeVo();

        // 情况1：一个自有账号(0)，一个同行账号(1)
        if (!accountTypeOne.equals(accountTypeTwo)) {
            // 自有账号为优化场次，同行账号为对标场次
            if (accountTypeOne == 0) {
                result.setOptimizeVideoId(videoContrastTypeBo.getVideoOneId());
                result.setBenchmarkVideoId(videoContrastTypeBo.getVideoTwoId());
            } else {
                result.setOptimizeVideoId(videoContrastTypeBo.getVideoTwoId());
                result.setBenchmarkVideoId(videoContrastTypeBo.getVideoOneId());
            }
            return R.ok(result);
        }

        // 到这里说明 accountTypeOne == accountTypeTwo
        return determineByParagraphData(videoContrastTypeBo.getVideoOneId(), videoContrastTypeBo.getVideoTwoId(), result);


    }

    /**
     * 根据本段录制数据(tb_video_data_viewing_paragraph)判断优化/对标场次
     */
    private R<VideoContrastTypeVo> determineByParagraphData(String videoOneId, String videoTwoId, VideoContrastTypeVo result) {
        // 获取视频1的数据看板数据
        VideoDataViewingParagraphEntity dataOne = videoDataViewingParagraphService.getOne(
                new LambdaQueryWrapper<VideoDataViewingParagraphEntity>()
                        .eq(VideoDataViewingParagraphEntity::getVideoId, videoOneId)
                        .in(VideoDataViewingParagraphEntity::getDataStatus, DataViewingStatusEnum.PULL_SUCCESS.getStatus(), DataViewingStatusEnum.DATA_ORGANIZE.getStatus())
                        .last("limit 1")
        );

        // 获取视频2的数据看板数据
        VideoDataViewingParagraphEntity dataTwo = videoDataViewingParagraphService.getOne(
                new LambdaQueryWrapper<VideoDataViewingParagraphEntity>()
                        .eq(VideoDataViewingParagraphEntity::getVideoId, videoTwoId)
                        .in(VideoDataViewingParagraphEntity::getDataStatus, DataViewingStatusEnum.PULL_SUCCESS.getStatus(), DataViewingStatusEnum.DATA_ORGANIZE.getStatus())
                        .last("limit 1")
        );


        if(dataOne == null || dataTwo == null) {
            // 如果任意一个视频没有看板数据，则以截止数据来判断
            return determineByConfuseData(videoOneId, videoTwoId, result);
        }else if(dataOne.getVolumeStart() == null || dataOne.getVolumeStart() <= 0 || dataTwo.getVolumeStart() == null || dataTwo.getVolumeStart() <= 0) {
            // 任意一个销售额为空，以场观人次来比
            dataOne.setTotalWatchNum(dataOne.getTotalWatchNum() == null || dataOne.getTotalWatchNum() < 0 ? 0 : dataOne.getTotalWatchNum());
            dataTwo.setTotalWatchNum(dataTwo.getTotalWatchNum() == null || dataTwo.getTotalWatchNum() < 0 ? 0 : dataTwo.getTotalWatchNum());

            return compareByWatchNum(videoOneId, videoTwoId, result, dataOne.getTotalWatchNum(), dataTwo.getTotalWatchNum());
        }

        // 两个视频都有看板数据，使用原有逻辑比较销售额和观看人次
        return compareByVolumeAndWatchNum(
                videoOneId, videoTwoId, result,
                dataOne.getVolumeStart(), dataTwo.getVolumeStart(),
                dataOne.getTotalWatchNum(), dataTwo.getTotalWatchNum()
        );
    }

    /**
     * 根据累计观看人数判断对标和优化场次
     * @param videoOneId 视频1 ID
     * @param videoTwoId 视频2 ID
     * @param result 结果对象
     * @return 判断结果
     */
    private R<VideoContrastTypeVo> determineByTotalOnlineNum(String videoOneId, String videoTwoId, VideoContrastTypeVo result) {
        // 获取视频1的socket数据
        SocketCollectMessageInfoVo socketOne = socketCollectMessageProducer.getByVideoId(videoOneId);
        // 获取视频2的socket数据
        SocketCollectMessageInfoVo socketTwo = socketCollectMessageProducer.getByVideoId(videoTwoId);

        // 只有视频2有数据，则视频2为对标场次
        if (socketOne == null && socketTwo != null) {
            result.setOptimizeVideoId(videoOneId);
            result.setBenchmarkVideoId(videoTwoId);
            return R.ok(result);
        }
        // 只有视频1有数据，则视频1为对标场次
        if (socketOne != null && socketTwo == null) {
            result.setOptimizeVideoId(videoTwoId);
            result.setBenchmarkVideoId(videoOneId);
            return R.ok(result);
        }
        // 两个都没有数据，默认视频1为优化，视频2为对标
        if (socketOne == null && socketTwo == null) {
            result.setOptimizeVideoId(videoOneId);
            result.setBenchmarkVideoId(videoTwoId);
            return R.ok(result);
        }

        // 两个视频都有数据，比较累计观看人数
        int totalOnlineNumOne = parseIntSafe(socketOne.getObservationNum());
        int totalOnlineNumTwo = parseIntSafe(socketTwo.getObservationNum());

        // 高累计观看人数为对标场次，低的为优化场次
        if (totalOnlineNumOne > totalOnlineNumTwo) {
            result.setOptimizeVideoId(videoTwoId);
            result.setBenchmarkVideoId(videoOneId);
        } else {
            result.setOptimizeVideoId(videoOneId);
            result.setBenchmarkVideoId(videoTwoId);
        }

        return R.ok(result);
    }

    /**
     * 安全解析字符串为整数
     * @param str 字符串
     * @return 整数值，解析失败返回0
     */
    private int parseIntSafe(String str) {
        if (str == null || str.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 根据混淆数据(tb_video_data_viewing_confuse)判断优化/对标场次
     */
    private R<VideoContrastTypeVo> determineByConfuseData(String videoOneId, String videoTwoId, VideoContrastTypeVo result) {
        // 获取视频1的混淆数据看板数据
        VideoDataViewingConfuseEntity dataOne = videoDataViewingConfuseService.getOne(
                new LambdaQueryWrapper<VideoDataViewingConfuseEntity>()
                        .eq(VideoDataViewingConfuseEntity::getVideoId, videoOneId)
                        .in(VideoDataViewingConfuseEntity::getDataStatus, DataViewingStatusEnum.PULL_SUCCESS.getStatus(), DataViewingStatusEnum.DATA_ORGANIZE.getStatus())
                        .last("limit 1")
        );

        // 获取视频2的混淆数据看板数据
        VideoDataViewingConfuseEntity dataTwo = videoDataViewingConfuseService.getOne(
                new LambdaQueryWrapper<VideoDataViewingConfuseEntity>()
                        .eq(VideoDataViewingConfuseEntity::getVideoId, videoTwoId)
                        .in(VideoDataViewingConfuseEntity::getDataStatus, DataViewingStatusEnum.PULL_SUCCESS.getStatus(), DataViewingStatusEnum.DATA_ORGANIZE.getStatus())
                        .last("limit 1")
        );

        if(dataOne == null || dataTwo == null) {
            // 如果任意一个视频没有看板数据，则以websocket场观数据来判断
            return determineByTotalOnlineNum(videoOneId, videoTwoId, result);
        }else if(dataOne.getVolumeStart() == null || dataOne.getVolumeStart() <= 0 || dataTwo.getVolumeStart() == null || dataTwo.getVolumeStart() <= 0) {
            // 任意一个销售额为空，以场观人次来比
            dataOne.setTotalWatchNum(dataOne.getTotalWatchNum() == null || dataOne.getTotalWatchNum() < 0 ? 0 : dataOne.getTotalWatchNum());
            dataTwo.setTotalWatchNum(dataTwo.getTotalWatchNum() == null || dataTwo.getTotalWatchNum() < 0 ? 0 : dataTwo.getTotalWatchNum());

            return compareByWatchNum(videoOneId, videoTwoId, result, dataOne.getTotalWatchNum(), dataTwo.getTotalWatchNum());
        }

        // 两个视频都有看板数据，使用原有逻辑比较销售额和观看人次
        return compareByVolumeAndWatchNum(
                videoOneId, videoTwoId, result,
                dataOne.getVolumeStart(), dataTwo.getVolumeStart(),
                dataOne.getTotalWatchNum(), dataTwo.getTotalWatchNum()
        );
    }

    /**
     * 根据销售额和观看人次比较判断优化/对标场次
     * @param videoOneId 视频1 ID
     * @param videoTwoId 视频2 ID
     * @param result 结果对象
     * @param volumeOne 视频1销售额
     * @param volumeTwo 视频2销售额
     * @param watchNumOne 视频1观看人次
     * @param watchNumTwo 视频2观看人次
     * @return 判断结果
     */
    private R<VideoContrastTypeVo> compareByVolumeAndWatchNum(
            String videoOneId, String videoTwoId, VideoContrastTypeVo result,
            Integer volumeOne, Integer volumeTwo,
            Integer watchNumOne, Integer watchNumTwo) {
        // 比较销售额（使用volumeStart作为销售额）
        int volOne = volumeOne != null ? volumeOne : 0;
        int volTwo = volumeTwo != null ? volumeTwo : 0;

        // 高销售额为对标场次，低销售额为优化场次
        if (volOne > volTwo) {
            result.setOptimizeVideoId(videoTwoId);
            result.setBenchmarkVideoId(videoOneId);
            return R.ok(result);
        } else if (volOne < volTwo) {
            result.setOptimizeVideoId(videoOneId);
            result.setBenchmarkVideoId(videoTwoId);
            return R.ok(result);
        }

        // 销售额相等或都没有销售额，比较总观看人次
        int watchOne = watchNumOne != null ? watchNumOne : 0;
        int watchTwo = watchNumTwo != null ? watchNumTwo : 0;

        // 高观看人次为对标场次，低的为优化场次
        if (watchOne > watchTwo) {
            result.setOptimizeVideoId(videoTwoId);
            result.setBenchmarkVideoId(videoOneId);
        } else {
            result.setOptimizeVideoId(videoOneId);
            result.setBenchmarkVideoId(videoTwoId);
        }

        return R.ok(result);
    }

    /**
     * 根据观看人次比较判断优化/对标场次
     * @param videoOneId 视频1 ID
     * @param videoTwoId 视频2 ID
     * @param result 结果对象
     * @param watchNumOne 视频1观看人次
     * @param watchNumTwo 视频2观看人次
     * @return 判断结果
     */
    private R<VideoContrastTypeVo> compareByWatchNum(
            String videoOneId, String videoTwoId, VideoContrastTypeVo result,
            Integer watchNumOne, Integer watchNumTwo) {

        int watchOne = watchNumOne != null ? watchNumOne : 0;
        int watchTwo = watchNumTwo != null ? watchNumTwo : 0;

        // 高观看人次为对标场次，低的为优化场次
        if (watchOne > watchTwo) {
            result.setOptimizeVideoId(videoTwoId);
            result.setBenchmarkVideoId(videoOneId);
        } else {
            result.setOptimizeVideoId(videoOneId);
            result.setBenchmarkVideoId(videoTwoId);
        }

        return R.ok(result);
    }
}

