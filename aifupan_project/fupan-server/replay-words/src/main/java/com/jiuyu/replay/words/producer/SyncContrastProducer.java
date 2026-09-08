package com.jiuyu.replay.words.producer;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.*;
import com.jiuyu.replay.words.entity.SyncContrastEntity;
import com.jiuyu.replay.words.vo.SyncContrastInfoVo;
import com.jiuyu.replay.words.vo.SyncContrastListVo;

import java.util.Collection;
import java.util.List;


/**
 * 客户端对比数据
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-26 14:51:59
 */
public interface SyncContrastProducer {


    /**
     * 客户端对比数据列表
     * @param syncContrastListBo 客户端对比数据列表查询参数
     * @return
     */
    PageUtils<SyncContrastListVo> queryPage(SyncContrastListBo syncContrastListBo);

    /**
    * 客户端对比数据信息
    * @param id 客户端对比数据id
    * @return
    */
    SyncContrastInfoVo info(Long id);

    /**
     * 新增客户端对比数据
     * @param syncContrastBo 客户端对比数据对象
     * @return
     */
     SyncContrastInfoVo save(SyncContrastBo syncContrastBo);

    /**
     * 修改客户端对比数据
     * @param syncContrastBo 客户端对比数据对象
     * @return
     */
    void update(SyncContrastBo syncContrastBo);

    /**
     * 删除客户端对比数据
     * @param id 客户端对比数据id
     * @return
     */
    void deleteById(Long id);


    /**
     * 根据用户id获取对比数据列表
     * @param userId 用户id
     * @return
     */
    List<SyncContrastInfoVo> listByUserId(Long userId);

    /**
     * 根据对比记录的唯一标识获取对比记录
     * @param contrastId 对比唯一标识 uuid
     * @return
     */
    SyncContrastInfoVo infoByContrastId(String contrastId);

    /**
     * 根据对比记录的唯一标识获取对比记录详情
     *
     * @param contrastId 对比唯一标识 uuid
     * @return 对比记录详情
     */
    SyncContrastInfoVo infoDetailsByContrastId(String contrastId);

    /**
     * 获取云空间对比列表
     * @param cloudContrastListBo 请求参数
     * @return
     */
    PageUtils<SyncContrastListVo> listCloudContrast(CloudContrastListBo cloudContrastListBo);

    /**
     * 查找视频相关的对比记录是否存在分享，存在则去除对比分享
     * @param videoId 视频id
     */
    void deleteCloudByVideoId(String videoId);

    /**
     * 批量查找视频相关的对比记录是否存在分享，存在则去除对比分享。
     * 一次 SELECT IN 命中所有关联记录，统一改字段后 updateBatchById 一次写回；与 N 无关。
     *
     * @param videoIds 视频id集合
     */
    void deleteCloudByVideoIds(Collection<String> videoIds);

    /**
     * 查询所有对比分析记录
     * @return
     */
    List<SyncContrastEntity> listAllContrast();

    R<PageUtils<SyncContrastListVo>> listAllSyncContrast(SyncContrastListBo syncContrastListBo);

    /**
     * 客户端获取对比列表
     * @param clientContrastListBo 查询参数
     * @return
     */
    PageUtils<SyncContrastInfoVo> clientContrastList(ClientContrastListBo clientContrastListBo);

    /**
     * 条件获取ai收藏页对比数据
     * @param clientAiFavListBo 查询条件
     * @return
     */
    List<String> listByAiFav(ClientAiFavListBo clientAiFavListBo);

    /**
     * 根据对比id集合获取对比信息
     * @param contrastIds 对比id集合
     * @param tenant 租户id
     * @return
     */
    List<SyncContrastInfoVo> listByContrastIds(List<String> contrastIds, Long tenant);

    /**
     * 获取允许删除的对比id集合
     * @param ids 对比uuid集合
     * @param tenantId 租户id
     * @param userId 用户id
     * @return
     */
    List<String> getAllowDeleteContrast(List<String> ids, Long tenantId, Long userId);

    /**
     * 根据对比id集合删除对比记录
     * @param delIds 对比id集合
     */
    void removeByContrastIds(List<String> delIds);

    /**
     * 批量修改对比记录的删除状态
     * @param contrastIds 对比记录id集合
     * @param deleteStatus 删除状态 0：未删除 1：已从对比复盘列表删除 2：已从对比复盘列表和云空间删除
     */
    void batchUpdateContrastDelStatus(List<String> contrastIds, Integer deleteStatus);

    /**
     * 批量修改对比记录的云空间分享状态
     * @param contrastIds 对比id集合
     * @param isShare 是否已上传云空间 0：否 1：是
     */
    void batchUpdateContrastIsShare(List<String> contrastIds, Integer isShare);

    /**
     * 根据视频id集合批量修改对比记录的删除状态
     * @param videoIds 视频id集合
     * @param deleteStatus 删除状态 0：未删除 1：已从对比复盘列表删除 2：已从对比复盘列表和云空间删除
     */
    void batchUpdateContrastDelStatusByVideoIds(List<String> videoIds, Integer deleteStatus);

    /**
     * 检查是否已经存在相同的对比记录，存在则更新修改时间
     * @param contrastType 对比类型 0：视频对比 1：文件对比
     * @param uuid1 对比项id1
     * @param uuid2 对比项id2
     * @param userId 用户id
     * @param isCloud 是否是云空间操作 0:否 1:是
     * @param syncScene 对比使用场景(对比分析才有) 1：对比上一次场、2：不同直播间对比、3：同直播间对比
     * @return
     */
    SyncContrastInfoVo checkExistUpdate(Integer contrastType, String uuid1, String uuid2, Long userId, Integer isCloud, Integer syncScene);


    /**
     * 切换对比定位（交换视频1和视频2、主播1和主播2的位置）
     * @param contrastId 对比唯一标识 uuid
     */
    void switchContrastPosition(String contrastId);

    /**
     * 分析记录页面-对比分析列表分页接口
     *
     * @param syncContrastListBo 查询参数
     * @return 分页数据
     */
    PageUtils<SyncContrastListVo> pageSyncContrastNew(SyncContrastListBo syncContrastListBo);

    /**
     * 租户是否存在有效的对比复盘记录（is_deleted=0）
     *
     * @param tenantId 租户id
     *
     * @return 存在则返回 true
     */
    Boolean existsByTenantId(Long tenantId);
}

