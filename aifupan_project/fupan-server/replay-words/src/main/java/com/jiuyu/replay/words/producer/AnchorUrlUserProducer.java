package com.jiuyu.replay.words.producer;


import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.words.AnchorUrlUserVo;
import com.jiuyu.replay.generic.vo.words.AnchorUrlVo;
import com.jiuyu.replay.words.bo.AnchorUrlPegBo;
import com.jiuyu.replay.words.bo.AnchorUrlUserBo;
import com.jiuyu.replay.words.bo.anchor.ClientAnchorListBo;
import com.jiuyu.replay.words.entity.AnchorUrlUserEntity;
import com.jiuyu.replay.words.vo.anchor.AnchorRecordListVo;

import java.util.List;

public interface AnchorUrlUserProducer {

    /**
     *根据用户Id查询所用主播
     */
    List<AnchorUrlVo> seleByuserId(Long userid);

    /**
     * 用户绑定主播
     * @param secUidList
     * @param userid
     * @return
     */
    R<String> saves(List<AnchorUrlUserBo> secUidList, Long userid);

    void deletBySecUid(String secUidList, Long id);

    /**
     * 服务端根据用户id分页查询绑定主播列表
     * @param anchorUrlPegBo
     * @return
     */
    R<PageUtils<AnchorUrlVo>> seletByUserId(AnchorUrlPegBo anchorUrlPegBo);

    /**
     * 根据用户id查看用户绑定的多少主播
     * @param userId
     * @return
     */
    Integer sum(Long userId);


    /**
     * 服务端根据seu_uid获取用户信息
     * @param secUid
     * @return
     */
    List<AnchorUrlUserEntity> selectBySecUid(String secUid);

    /**
     * 根据用户id和主播secUid获取关联信息
     * @param userId 用户id
     * @param anchorUrlSecUid 主播secUid
     */
    AnchorUrlUserVo getByUserIdAndSecUid(Long userId, String anchorUrlSecUid);

    /**
     * 根据id修改信息
     * @param anchorUrlUserBo 对象
     */
    void updateById(AnchorUrlUserBo anchorUrlUserBo);

    /**
     * 修改弹幕监控状态
     *
     * @param userId
     * @param tenantId
     * @param secUid              主播id
     * @param isBarrageMonitoring 弹幕监控状态
     * @param currentPropertyNum  当前用户拥有的弹幕监控数量
     * @return 当前用户使用的弹幕监控数量
     */
    Long updateBarrageMonitoring(Long userId, Long tenantId, String secUid, Integer isBarrageMonitoring, Long currentPropertyNum);

    /**
     * 修改用户-主播是否自动上传云空间的状态
     * @param userId 用户id
     * @param secUid 主播secuid
     * @param isAutoUploadCloud 自动上传云空间 0否 1是
     * @param tenantId 租户id
     * @return
     */
    void updateAutoUploadCloud(Long userId, String secUid, Integer isAutoUploadCloud, Long tenantId);

    /**
     * 关闭主播弹幕监控
     * @param userIds
     */
    void closeAnchorBarrageNum(List<Long> userIds);

    /**
     * 关闭主播自动上传云空间
     * @param userIds
     */
    void closeAutoUploadCloud(List<Long> userIds);

    /**
     * 查询主播弹幕监控数量
     * @param anchorUrlUserBo
     * @return
     */
    Long queryBarrageMonitoring(AnchorUrlUserBo anchorUrlUserBo);

    /**
     * 根据用户id获取列表
     * @param userId 用户id
     * @return
     */
    List<AnchorUrlUserVo> listByUserId(Long userId);

    /**
     * 更新主播置顶信息
     * @param secUid 主播secuid
     * @param action 动作 0：取消置顶 1：置顶
     * @param addTopTime 添加置顶的时间
     * @param userId 用户id
     * @param tenantId 租户id
     * @return
     */
    Boolean updateAnchorTop(Long userId, Long tenantId, String secUid, Integer action, String addTopTime);

    /**
     * 更新主播最后开始录制时间
     * @param secUid 主播secuid
     * @param lastRecordTime 最后开始录制时间
     * @param userId 用户id
     * @return
     */
    void updateAnchorLastRecordTime(Long userId, String secUid, String lastRecordTime, Long tenantId);

    /**
     * 客户端获取主播列表
     * @param clientAnchorListBo 查询参数
     * @return
     */
    PageUtils<AnchorRecordListVo> clientVideoList(ClientAnchorListBo clientAnchorListBo);

    /**
     * 根据secUid获取用户主播信息
     * @param secUid 主播SecUid
     * @param userId 用户id
     * @param tenantId 租户id
     * @return
     */
    AnchorUrlUserVo getUserAnchorBySecUid(String secUid, Long userId, Long tenantId);

    /**
     * 获取用户主播列表
     * @param userId 用户id
     * @param tenantId 租户id
     * @return
     */
    List<AnchorUrlUserVo> listByUser(Long userId, Long tenantId);

    /**
     * 获取用户主播列表(精简信息)
     *
     * @param userId   用户id
     * @param tenantId 租户id
     * @return 用户主播列表
     */
    List<AnchorUrlUserVo> listByUserSimply(Long userId, Long tenantId);

    /**
     * 获取用户主播监控位置
     *
     * @param userId   用户id
     * @param tenantId 租户id
     * @return
     */
    List<AnchorUrlUserVo> monitoringPositionByUser(Long userId, Long tenantId);

    /**
     * 根据主播secuid集合获取昨日录制场次列表
     * @param secUidList 主播secuid集合
     * @param userId 用户id
     * @param tenantId 租户id
     * @return
     */
    List<AnchorUrlUserVo> listBySecUids(List<String> secUidList, Long userId, Long tenantId);

    /**
     * 保存用户主播信息
     * @param anchorUrlUserBo 用户主播信息
     */
    void save(AnchorUrlUserBo anchorUrlUserBo);

    /**
     * 根据租户id获取主播列表
     * @param tenantId 租户id
     * @return
     */
    List<AnchorUrlUserVo> listByTenantId(Long tenantId);

    /**
     * 更新主播数据看板状态
     *
     * @param userId   用户id
     * @param tenantId 租户id
     * @param secUid   主播secuid
     * @param status   状态 {@link com.jiuyu.replay.words.constant.Constant.yesOrNoEnum}
     * @return 是否成功
     */
    boolean updateDataViewing(Long userId, Long tenantId, String secUid, Integer status);

    /**
     * 根据secUids和userids查询列表
     *
     * @param secUidList secUids
     * @param userIds    用户ids
     * @return 主播列表
     */
    List<AnchorUrlUserVo> listBySecUidsAndUserIds(List<String> secUidList, List<Long> userIds);

    /**
     * 视频号取消授权处理
     *
     * @param channelId 视频号id
     * @param userIds   用户ids
     * @return 是否成功
     */
    R<Void> cancelChannel(String channelId,  List<Long> userIds);

    /**
     * 减少数据诊断生成数量
     *
     * @param secUid   主播secUid
     * @param userId   用户id
     * @param tenantId 租户id
     * @param num      减少数量
     */
    void minusDataDiagnosisGenerateNum(String secUid, Long userId, Long tenantId, int num);

    /**
     * 统计租户下添加的主播账号总数（未删除）
     *
     * @param tenantId 租户id
     *
     * @return 主播账号总数
     */
    Integer countByTenantId(Long tenantId);

    /**
     * 统计租户下指定账号归属类型的主播账号数
     *
     * @param tenantId    租户id
     * @param accountType 账号归属类型 0：自有账号 1：同行账号
     *
     * @return 主播账号数
     */
    Integer countByTenantIdAndAccountType(Long tenantId, Integer accountType);
    /**
     * 统计当前用户名下指定开关字段为 1 的有效直播间数量（按 tenantId 隔离 + isRemoveRecord=0 过滤）
     *
     * @param userId      用户 ID
     * @param tenantId    租户 ID
     * @param switchField 开关字段名（"isScriptQualityInspection" / "isScriptFidelityMonitor" / "isInteractionPatrol"）
     * @return 该开关已开启的直播间数量
     */
    Long countOpenSwitch(Long userId, Long tenantId, String switchField);

    /**
     * Data Hub：按租户批量查业务账号快照（含四类授权状态，合并 tb_anchor_url 档案）
     *
     * @param tenantIds      租户id列表
     * @param includeRemoved 是否包含已从列表移除的账号
     *
     * @return {@link java.util.List }<{@link com.jiuyu.replay.words.vo.datahub.DataHubAnchorAccountVo }>
     */
    java.util.List<com.jiuyu.replay.words.vo.datahub.DataHubAnchorAccountVo> listDataHubAccountsByTenantIds(
            java.util.Collection<Long> tenantIds, boolean includeRemoved);
}
