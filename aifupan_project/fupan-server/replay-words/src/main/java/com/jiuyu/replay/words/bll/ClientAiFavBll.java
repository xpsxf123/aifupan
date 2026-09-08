package com.jiuyu.replay.words.bll;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;

import com.jiuyu.replay.generic.vo.words.AnchorUrlInfoVo;
import com.jiuyu.replay.generic.vo.words.VideoAnalysisRecordInfoVo;
import com.jiuyu.replay.generic.vo.words.VideoAnalysisRecordVo;
import com.jiuyu.replay.words.constant.Constant;
import com.jiuyu.replay.words.producer.*;
import com.jiuyu.replay.words.vo.*;
import com.jiuyu.replay.words.bo.ClientAiFavBo;
import com.jiuyu.replay.words.bo.ClientAiFavListBo;

import com.jiuyu.replay.words.vo.file.UploadFileInfoVo;
import com.jiuyu.replay.generic.vo.words.AnchorVideoInfoVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * 运营/违规收藏列表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-03-12 16:42:18
 */
@Component
public class ClientAiFavBll {

    @Resource
    private ClientAiFavProducer clientAiFavProducer;
    @Resource
    private AnchorVideoProducer anchorVideoProducer;
    @Resource
    private UploadFileProducer uploadFileProducer;
    @Resource
    private SyncContrastProducer syncContrastProducer;
    @Resource
    private AnchorUrlProducer anchorUrlProducer;
    @Resource
    private VideoAnalysisRecordProducer videoAnalysisRecordProducer;
    @Resource
    private UploadFileAnalysisRecordProducer uploadFileAnalysisRecordProducer;


    /**
     * 运营/违规收藏列表列表
     * @param clientAiFavListBo 运营/违规收藏列表列表查询参数
     * @return
     */
    public R<PageUtils<ClientAiFavListVo>> queryPage(ClientAiFavListBo clientAiFavListBo) {

        if(StringUtils.isEmpty(clientAiFavListBo.getFavType()) || StringUtils.isEmpty(clientAiFavListBo.getDataResourceType())) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "请传入类型");
        }

        // 构建来源id集合查询条件
        clientAiFavListBo.setResourceIds(buildResourceIdList(clientAiFavListBo));

        PageUtils<ClientAiFavListVo> pageUtils = clientAiFavProducer.queryPage(clientAiFavListBo);

        // 封装视频等信息
        List<ClientAiFavListVo> list = pageUtils.getList();
        if(list != null && list.size() > 0) {
            List<String> resourceIds = list.stream().map(ClientAiFavVo::getDataResourceUuid).toList();
            if(clientAiFavListBo.getDataResourceType() == 0) {
                // 录制的视频
                packageVideoInfo(list, resourceIds, clientAiFavListBo.getUserId(), clientAiFavListBo.getTenantId());

            } else if (clientAiFavListBo.getDataResourceType() == 1) {
                // 文件
                packageFileInfo(list, resourceIds);

            } else if (clientAiFavListBo.getDataResourceType() == 2) {
                // 对比
                packageContrastInfo(list, resourceIds, clientAiFavListBo.getTenantId());

            }
        }

        return R.ok("获取成功", pageUtils);
    }

    /**
     * 构建来源id集合查询条件
     * @param clientAiFavListBo 运营/违规收藏列表查询参数
     * @return
     */
    private List<String> buildResourceIdList(ClientAiFavListBo clientAiFavListBo) {
        List<String> resourceIdList = new LinkedList<>();
        if(clientAiFavListBo.getDataResourceType() == 0) {
            // 录制的视频
            if(!StringUtils.isEmpty(clientAiFavListBo.getTradeId()) || (clientAiFavListBo.getSecUidArr() != null && clientAiFavListBo.getSecUidArr().size() > 0)) {
                List<String> ids = this.anchorVideoProducer.listByAiFav(clientAiFavListBo);
                if(ids != null && ids.size() > 0) {
                    resourceIdList = ids;
                }else {
                    resourceIdList.add("0");
                }
            }

        } else if (clientAiFavListBo.getDataResourceType() == 1) {
            // 文件
            if(!StringUtils.isEmpty(clientAiFavListBo.getTradeId()) || !StringUtils.isEmpty(clientAiFavListBo.getFileName())) {
                List<String> ids = this.uploadFileProducer.listByAiFav(clientAiFavListBo);
                if(ids != null && ids.size() > 0) {
                    resourceIdList = ids;
                }else {
                    resourceIdList.add("0");
                }
            }
        } else if (clientAiFavListBo.getDataResourceType() == 2) {
            // 对比
            if(!StringUtils.isEmpty(clientAiFavListBo.getContrastType()) || !StringUtils.isEmpty(clientAiFavListBo.getTradeId()) ||
                    (clientAiFavListBo.getSecUidArr() != null && clientAiFavListBo.getSecUidArr().size() > 0) || !StringUtils.isEmpty(clientAiFavListBo.getFileName())) {
                List<String> ids = this.syncContrastProducer.listByAiFav(clientAiFavListBo);
                if(ids != null && ids.size() > 0) {
                    resourceIdList = ids;
                }else {
                    resourceIdList.add("0");
                }
            }
        }

        return resourceIdList;
    }

    /**
     * 封装助手列表的对比信息
     * @param list 助手列表
     * @param resourceIds 对比id集合
     * @param tenant 租户id
     */
    private void packageContrastInfo(List<ClientAiFavListVo> list, List<String> resourceIds, Long tenant) {
        List<SyncContrastInfoVo> syncContrastInfoVos = this.syncContrastProducer.listByContrastIds(resourceIds, tenant);
        if(syncContrastInfoVos != null && syncContrastInfoVos.size() > 0) {
            for (ClientAiFavListVo clientAiFavListVo : list) {
                for (SyncContrastInfoVo syncContrastInfoVo : syncContrastInfoVos) {
                    if(clientAiFavListVo.getDataResourceUuid().equals(syncContrastInfoVo.getContrastId())) {
                        clientAiFavListVo.setContrastInfo(syncContrastInfoVo);
                        break;
                    }
                }
            }
        }
    }

    /**
     * 封装助手列表的文件信息
     * @param list 助手列表
     * @param resourceIds 文件id集合
     */
    private void packageFileInfo(List<ClientAiFavListVo> list, List<String> resourceIds) {
        // 获取文件列表
        List<UploadFileInfoVo> uploadFileInfoVos = this.uploadFileProducer.listByFileIds(resourceIds);

        if(uploadFileInfoVos != null && uploadFileInfoVos.size() > 0) {

            // 获取分析记录
            List<String> fileIds = uploadFileInfoVos.stream().map(UploadFileInfoVo::getFileId).toList();
            List<UploadFileAnalysisRecordInfoVo> uploadFileAnalysisRecordInfoVos = this.uploadFileAnalysisRecordProducer.listByFileIds(fileIds);
            if(uploadFileAnalysisRecordInfoVos != null && uploadFileAnalysisRecordInfoVos.size() > 0) {
                uploadFileAnalysisRecordInfoVos.sort(Comparator.comparing(UploadFileAnalysisRecordInfoVo::getVersion).reversed());
            }

            for (ClientAiFavListVo clientAiFavListVo : list) {
                for (UploadFileInfoVo uploadFileInfoVo : uploadFileInfoVos) {
                    if(clientAiFavListVo.getDataResourceUuid().equals(uploadFileInfoVo.getFileId())) {

                        // 设置分析记录
                        if(uploadFileAnalysisRecordInfoVos != null && uploadFileAnalysisRecordInfoVos.size() > 0) {
                            for (UploadFileAnalysisRecordInfoVo uploadFileAnalysisRecordInfoVo : uploadFileAnalysisRecordInfoVos) {

                                if(uploadFileInfoVo.getFileId().equals(uploadFileAnalysisRecordInfoVo.getFileId())) {
                                    UploadFileAnalysisRecordInfoVo recordInfoVo = new UploadFileAnalysisRecordInfoVo();
                                    recordInfoVo.setCruxWordNum(uploadFileAnalysisRecordInfoVo.getCruxWordNum());
                                    recordInfoVo.setSensitiveWordNum(uploadFileAnalysisRecordInfoVo.getSensitiveWordNum());
                                    recordInfoVo.setContentNum(uploadFileAnalysisRecordInfoVo.getContentNum());
                                    uploadFileInfoVo.setRecordInfo(recordInfoVo);
                                    break;
                                }
                            }
                        }

                        clientAiFavListVo.setFileInfo(uploadFileInfoVo);
                        break;
                    }
                }
            }
        }
    }

    /**
     * 封装助手列表的视频信息
     * @param list 助手列表
     * @param resourceIds 视频id集合
     * @param userId 用户id
     * @param tenantId 租户id
     */
    private void packageVideoInfo(List<ClientAiFavListVo> list, List<String> resourceIds, Long userId, Long tenantId) {
        // 获取视频列表
        List<AnchorVideoInfoVo> anchorVideoInfoVoList = this.anchorVideoProducer.listByVideoIds(resourceIds);

        if(anchorVideoInfoVoList != null && anchorVideoInfoVoList.size() > 0) {

            // 获取分析记录
            List<String> videoIds = anchorVideoInfoVoList.stream().map(AnchorVideoInfoVo::getVideoId).toList();
            List<VideoAnalysisRecordInfoVo> videoAnalysisRecordInfoVos = this.videoAnalysisRecordProducer.listByVideoIds(videoIds);
            if(videoAnalysisRecordInfoVos != null && videoAnalysisRecordInfoVos.size() > 0) {
                videoAnalysisRecordInfoVos.sort(Comparator.comparing(VideoAnalysisRecordVo::getVersion).reversed());
            }

            // 获取主播信息
            Set<String> secUids = anchorVideoInfoVoList.stream().map(AnchorVideoInfoVo::getSecUid).collect(Collectors.toSet());
            List<AnchorUrlInfoVo> anchorUrlInfoVos = this.anchorUrlProducer.listAnchorByUserIdAndSecUids(userId, tenantId, secUids);

            // 设置视频信息
            for (ClientAiFavListVo clientAiFavListVo : list) {
                for (AnchorVideoInfoVo videoInfoVo : anchorVideoInfoVoList) {

                    if(clientAiFavListVo.getDataResourceUuid().equals(videoInfoVo.getVideoId())) {

                        // 设置主播信息
                        if(anchorUrlInfoVos != null && anchorUrlInfoVos.size() > 0) {
                            for (AnchorUrlInfoVo anchorUrlInfoVo : anchorUrlInfoVos) {
                                if(anchorUrlInfoVo.getSecUid().equals(videoInfoVo.getSecUid())) {
                                    videoInfoVo.setAnchorInfo(anchorUrlInfoVo);
                                    break;
                                }
                            }
                        }

                        // 设置分析记录
                        if(videoAnalysisRecordInfoVos != null && videoAnalysisRecordInfoVos.size() > 0) {
                            for (VideoAnalysisRecordInfoVo videoAnalysisRecordInfoVo : videoAnalysisRecordInfoVos) {
                                if(videoInfoVo.getVideoId().equals(videoAnalysisRecordInfoVo.getVideoId())) {
                                    VideoAnalysisRecordInfoVo recordInfoVo = new VideoAnalysisRecordInfoVo();
                                    recordInfoVo.setCruxWordNum(videoAnalysisRecordInfoVo.getCruxWordNum());
                                    recordInfoVo.setSensitiveWordNum(videoAnalysisRecordInfoVo.getSensitiveWordNum());
                                    recordInfoVo.setContentNum(videoAnalysisRecordInfoVo.getContentNum());
                                    videoInfoVo.setRecordInfo(recordInfoVo);
                                    break;
                                }
                            }
                        }

                        clientAiFavListVo.setVideoInfo(videoInfoVo);
                        break;
                    }
                }
            }
        }
    }

    /**
    * 运营/违规收藏列表信息
    * @param id 运营/违规收藏列表id
    * @return
    */
    public R<ClientAiFavInfoVo> info(Long id) {

        ClientAiFavInfoVo clientAiFavInfoVo = clientAiFavProducer.info(id);
        return R.ok("获取成功", clientAiFavInfoVo);
    }

    /**
     * 新增运营/违规收藏列表
     * @param clientAiFavBo 运营/违规收藏列表对象
     * @return
     */
    public R<String> save(ClientAiFavBo clientAiFavBo) {

        ClientAiFavInfoVo clientAiFavInfoVo = clientAiFavProducer.save(clientAiFavBo);
        return R.ok("添加成功");
    }

    /**
     * 修改运营/违规收藏列表
     * @param clientAiFavBo 运营/违规收藏列表对象
     * @return
     */
    public R<String> update(ClientAiFavBo clientAiFavBo) {

        clientAiFavProducer.update(clientAiFavBo);
        return R.ok("修改成功");
    }

    /**
     * 删除运营/违规收藏列表
     * @param id 运营/违规收藏列表id
     * @return
     */
    public R<String> delete(Long id) {

        clientAiFavProducer.deleteById(id);
        return R.ok("删除成功");
    }


    /**
     * 批量删除运营/违规收藏列表
     * @param ids 运营/违规收藏列表id集合
     * @param userId 用户id
     * @param tenantId 租户id
     * @return
     */
    public R<String> batchDelete(List<String> ids, Long userId, Long tenantId) {

        clientAiFavProducer.batchDelete(ids, userId, tenantId);
        return R.ok("删除成功");
    }

    /**
     * 新增或修改运营/违规收藏列表
     * @param clientAiFavBo 运营/违规收藏列表对象
     * @return
     */
    public R<String> saveOrUpdate(ClientAiFavBo clientAiFavBo) {

        if(StringUtils.isEmpty(clientAiFavBo.getFavType()) || StringUtils.isEmpty(clientAiFavBo.getDataResourceType()) || StringUtils.isEmpty(clientAiFavBo.getDataResourceUuid())) {
            return R.error(Constant.CodeMsgEnum.TIP_CUSTOM.getCode(), "请完善数据再提交");
        }

        ClientAiFavInfoVo clientAiFavInfoVo = this.clientAiFavProducer.infoByFavTypeAndResourceId(clientAiFavBo.getFavType(), clientAiFavBo.getDataResourceUuid());
        if(clientAiFavInfoVo != null) {
            clientAiFavBo.setId(clientAiFavInfoVo.getId());
            this.update(clientAiFavBo);
            return R.ok("修改成功");
        }else {
            this.save(clientAiFavBo);
            return R.ok("添加成功");
        }

    }

    public R<String> saveOrUpdateByNotExist(ClientAiFavBo clientAiFavBo) {
        if (this.clientAiFavProducer.hasDelete(clientAiFavBo)){
            return R.ok("操作成功");
        }

        return saveOrUpdate(clientAiFavBo);
    }
}

