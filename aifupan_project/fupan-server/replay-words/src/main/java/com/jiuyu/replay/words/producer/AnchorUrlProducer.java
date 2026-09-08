package com.jiuyu.replay.words.producer;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.power.UserInfoExportVo;
import com.jiuyu.replay.generic.vo.power.UserVo;
import com.jiuyu.replay.generic.vo.words.AnchorUrlInfoVo;
import com.jiuyu.replay.generic.vo.words.AnchorUrlListVo;
import com.jiuyu.replay.generic.vo.words.AnchorUrlUserVo;
import com.jiuyu.replay.generic.vo.words.AnchorUrlVo;
import com.jiuyu.replay.words.bo.*;
import com.jiuyu.replay.words.vo.AnchorClientVo;

import java.util.Collection;
import java.util.List;
import java.util.Map;


/**
 * 主播url
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-08-10 16:47:37
 */
public interface AnchorUrlProducer {


    /**
     * 主播url列表
     * @param anchorUrlListBo 主播url列表查询参数
     * @return
     */
    PageUtils<AnchorUrlListVo> queryPage(AnchorUrlListBo anchorUrlListBo);

    /**
    * 主播url信息
    * @param id 主播urlid
    * @return
    */
    AnchorUrlInfoVo info(Long id);

    /**
     * 新增主播url
     * @param anchorUrlBo 主播url对象
     * @return
     */
     AnchorUrlInfoVo save(AnchorUrlBo anchorUrlBo);

    /**
     * 修改主播url
     * @param anchorUrlBo 主播url对象
     * @return
     */
    void update(AnchorUrlBo anchorUrlBo);

    /**
     * 删除主播url
     * @param id 主播urlid
     * @return
     */
    void deleteById(Long id);


    /**
     * 获取主播url信息
     * @param secUid 主播唯一标识
     * @return
     */
    AnchorUrlInfoVo infoBySecUid(String secUid);

    /**
     * 根据主播唯一标识集合获取主播
     * @param secUidList 主播唯一标识集合
     * @return
     */
    List<AnchorUrlInfoVo> listBySecUids(Collection<String> secUidList);

    /**
     * 根据secUid集合获取主播信息
     * @param userId 用户id
     * @param tenantId 租户id
     * @param secUids 主播secUid集合
     * @return
     */
    List<AnchorUrlInfoVo> listAnchorByUserIdAndSecUids(Long userId, Long tenantId, Collection<String> secUids);

    /**
     * 根据条件集合获取主播
     * @param secUidList 标识集合
     * @return
     */
    List<AnchorUrlInfoVo> listByConditions(List<String> secUidList);

    /**
     * 根据主播唯一标识集合批量删除主播信息
     * @param secUids 主播唯一标识集合
     */
    void deleteBatchBySecUids(List<String> secUids);

    /**
     * 批量添加
     * @param liveAnchorList 主播集合
     */
    void saveBatch(List<AnchorUrlBo> liveAnchorList);

    /**
     * 获取主播url信息
     * @param secUid 主播唯一标识
     * @param liveUrl 主播直播地址
     * @param homeUrl 主播主页地址
     * @return
     */
    AnchorUrlInfoVo infoByCondition(String secUid, String liveUrl, String homeUrl);
    /**
     * 根据secUids获取所有的主播
     */
    List<AnchorUrlVo> lists(List<String> secUids);

    /**
     * 服务端获取主播列表
     * @param anchorUrlPegBo
     * @return
     */
    R<PageUtils<AnchorUrlVo>> seletAnchorUrl(AnchorUrlPegBo anchorUrlPegBo);

    /**
     * 根据用户id获取主播列表
     * @param userId 用户id
     * @return
     */
    List<AnchorClientVo> listByUserId(Long userId);

    /**
     * 只根据SecUid查询主播信息
     * @param secUid
     * @return
     */
    R<AnchorUrlInfoVo> infoBySecUidOne(String secUid);

    /**
     * 根据user_id从主播用户关联表中查出用户关联的所有主播sec_uid,在拿sec_uid去查询主播信息
     * @param anchorUrlUserBo
     * @return
     */
    R<PageUtils<AnchorUrlVo>> selectAnchorByUserId(AnchorUrlUserBo anchorUrlUserBo);

    /**
     * 更新是否删除记录
     *
     * @param secUids
     * @param userId
     * @param isRemoveRecord
     */
    void updateIsRemoveRecord(List<String> secUids, Long userId, int isRemoveRecord);

    /**
     * 根据userId获取当前用户添加的主播信息
     * @param anchorUrlUserBo
     * @return
     */
    R<PageUtils<AnchorUrlVo>> userAddAnchorRecord(AnchorUrlUserBo anchorUrlUserBo);

    List<UserInfoExportVo> exportUserInfoList(List<UserVo> userList);

    /**
     * 绑定主播和用户的关系
     * @param userAnchorBo 绑定信息
     * @return
     */
    boolean bindUserAnchor(UserAnchorBo userAnchorBo);

    /**
     * 根据主播名称查询主播列表
     * @param anchorName 主播名称
     * @return
     */
    List<AnchorUrlInfoVo> listByLikeName(String anchorName);

    /**
     * 根据secUid集合获取用户主播信息
     * @param userId 用户id
     * @param tenantId 租户id
     * @param secUids 主播secUid集合
     * @return
     */
    List<AnchorUrlUserVo> listByUserIdAndSecUids(Long userId, Long tenantId, Collection<String> secUids);

    /**
     * 根据secUid集合和用户id集合获取用户主播信息
     * @param userIds 用户id集合
     * @param tenantId 租户id
     * @param secUids 主播secUid集合
     * @return
     */
    List<AnchorUrlUserVo> listUserAnchorByUserIdsAndSecUids(Collection<Long> userIds, Long tenantId, Collection<String> secUids);

    List<Long> selectQuery(AnchorUrlBo anchorUrlBo);

    Map<Long, Long> countIHave(List<Long> longList);

    /**
     * 根据主播抖音号获取主播信息
     * @param anchorNumber 主播抖音号
     * @return 主播信息
     */
    AnchorUrlInfoVo infoByAnchorNumber(String anchorNumber);

    /**
     * 修改行业
     *
     * @param secUid        主播
     * @param aiTradeId     ai行业
     * @param systemTradeId 系统行业
     * @return 是否成功
     */
    boolean updateTrdeIdBySecUid(String secUid, Long aiTradeId, Long systemTradeId);
}

