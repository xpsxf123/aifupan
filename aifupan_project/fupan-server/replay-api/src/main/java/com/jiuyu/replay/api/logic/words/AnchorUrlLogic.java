package com.jiuyu.replay.api.logic.words;

import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.power.vo.UserListVo;
import com.jiuyu.replay.power.vo.UserVo;
import com.jiuyu.replay.words.bo.*;
import com.jiuyu.replay.words.bo.anchor.AddOrUpdateAnchorBo;
import com.jiuyu.replay.words.bo.anchor.ClientAnchorListBo;
import com.jiuyu.replay.words.bo.anchor.TopAnchorBo;
import com.jiuyu.replay.words.vo.AnchorClientVo;
import com.jiuyu.replay.generic.vo.words.AnchorUrlInfoVo;
import com.jiuyu.replay.generic.vo.words.AnchorUrlListVo;
import com.jiuyu.replay.generic.vo.words.AnchorUrlVo;
import com.jiuyu.replay.generic.vo.words.AuthUsageVo;
import com.jiuyu.replay.words.vo.anchor.AnchorRecordListVo;
import com.jiuyu.replay.generic.vo.words.AnchorUrlUserVo;
import com.jiuyu.replay.words.vo.anchor.AnchorYesterdayRecordVo;
import com.jiuyu.replay.words.vo.anchor.OpenMonitoringPositionVo;

import java.util.List;


/**
 * 主播url
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-08-10 16:47:37
 */
public interface AnchorUrlLogic {

    /**
     * 发送上下播主播消息
     * @param anchorUrlName 主播名称
     * @param type 类型：0上播，1下播
     */
    R<String> sendSwitchAnchorMsg(final String anchorUrlName, final Integer type);

    /**
     * 主播url列表
     * @param anchorUrlListBo 主播url列表查询参数
     * @return
     */
    R<PageUtils<AnchorUrlListVo>> queryPage(AnchorUrlListBo anchorUrlListBo);

    /**
    * 主播url信息
    * @param id 主播urlid
    * @return
    */
    R<AnchorUrlInfoVo> info(Long id);

    /**
     * 新增主播url
     * @param anchorUrlBo 主播url对象
     * @return
     */
    R<String> save(AnchorUrlBo anchorUrlBo);

    /**
     * 修改主播url
     * @param anchorUrlBo 主播url对象
     * @return
     */
    R<String> update(AnchorUrlBo anchorUrlBo);

    /**
     * 删除主播url
     * @param id 主播urlid
     * @return
     */
    R<String> delete(Long id);


    /**
     * 获取主播url信息
     * @param secUid 主播唯一标识
     * @param liveUrl 主播直播地址
     * @param homeUrl 主播主页地址
     * @return
     */
    R<AnchorUrlInfoVo> infoByCondition(String secUid, String liveUrl, String homeUrl);

    /**
     * 根据主播唯一标识集合获取主播
     * @param secUidList 主播唯一标识集合
     * @return
     */
    R<List<AnchorUrlInfoVo>> listBySecUids(List<String> secUidList);

    /**
     * 批量保存
     * @param anchorUrlBos 主播集合
     * @return
     */
    R<String> saveBatch(List<AnchorUrlBo> anchorUrlBos);

    /**
     * 根据主播唯一标识集合获取主播(只返回有live地址的主播)
     * @param secUidList 主播唯一标识集合
     * @return
     */
    R<List<AnchorUrlInfoVo>> listLiveBySecUids(List<String> secUidList);

    /**
     * 根据userToken获取主播列表
     */
    R<List<AnchorClientVo>> listByUserToken();

    /**
     * 新增绑定用户与主播列表
     * @param secUidList
     * @return
     */
    R<String> savs(List<AnchorUrlUserBo> secUidList);

    /**
     * 删除用户与主播绑定关系
     * @param secUidList
     * @return
     */
    R<List<AnchorUrlInfoVo>> deletBySecUid(String secUidList);

    /**
     * 服务端根据用户id查询绑定主播列表
     * @param anchorUrlPegBo
     * @return
     */
    R<PageUtils<AnchorUrlVo>> seletByUserId(AnchorUrlPegBo anchorUrlPegBo);

    /**
     * 服务端获取主播列表
     * @param anchorUrlPegBo
     * @return
     */
    R<PageUtils<AnchorUrlVo>> seletAnchorUrl(AnchorUrlPegBo anchorUrlPegBo);

    /**
     * 客户查询用户是否有录制该主播
     * @param secUid
     * @return
     */
    R<List<String>> seletBysecUidAnchorUrlWhite( List<String> secUid);

    /**
     * 主播保存用户白名单
     * @param anchorUrlWhiteBo
     * @return
     */
    R<String> saveAnchorUrlWhite(AnchorUrlWhiteBo anchorUrlWhiteBo);

    /**
     * 服务端主播列表删除用户白名单
     * @param anchorUrlWhiteBo
     * @return
     */
    R<String> removeAnchorUrlWhite(AnchorUrlWhiteBo anchorUrlWhiteBo);

    /**
     * 服务端查询查询主播的白名单
     * @param secUid
     * @return
     */
    R<PageUtils<UserListVo>> seletUidAnchorUrlWhite(AnchorUrlWhiteListBo secUid);

    /**
     * 客户根据ssecUid查询用户是否有录制该主播
     * @param secUidS
     * @return
     */
    R<Boolean> seletBySerId(String secUidS);

    /**
     * 修改用户绑定的主播信息
     * @param anchorUrlUserBo
     * @return
     */
    R<String> updateUserAnchor(AnchorUrlUserBo anchorUrlUserBo);


    /**
     * 添加主播至用户白名单
     * @param userIdList
     * @param secUid
     * @return
     */
    R<String> saveAnchorInUserWhite(Long userId,String secUid);

    /**
     * 只根据SecUid查询主播信息
     * @param secUid
     * @return
     */
    R<AnchorUrlInfoVo> infoBySecUidOne(String secUid);

    /**
     * 查询该主播的所属用户信息（白名单）
     * @param secUid
     * @return
     */
    R<List<UserVo>> selectUserByAnchorWhite(String secUid);

    /**
     * 删除主播白名单
     * @param anchorUrlWhiteBos
     * @return
     */
    R<String> removeAnchorWhite(List<AnchorUrlWhiteBo> anchorUrlWhiteBos);

    /**
     * 根据user_id从主播用户关联表中查出用户关联的所有主播sec_uid,在拿sec_uid去查询主播信息
     * @param anchorUrlUserBo
     * @return
     */
    R<PageUtils<AnchorUrlVo>> selectAnchorByUserId(AnchorUrlUserBo anchorUrlUserBo);

    /**
     * 根据userId获取当前用户添加的主播信息
     * @param anchorUrlUserBo
     * @return
     */
    R<PageUtils<AnchorUrlVo>> userAddAnchorRecord(AnchorUrlUserBo anchorUrlUserBo);

    /**
     * 添加或修改主播信息
     * @param anchorUrlBo 主播信息
     * @return
     */
    R<String> saveOrUpdateAnchor(AnchorUrlBo anchorUrlBo);

    /**
     * 修改主播的弹幕监控位状态
     * @param secUid
     * @param isBarrageMonitoring
     * @return
     */
    R<String> updateBarrageMonitoring(String secUid, Integer isBarrageMonitoring);

    /**
     * 修改用户-主播是否自动上传云空间的状态
     * @param secUid 主播secuid
     * @param isAutoUploadCloud 自动上传云空间 0否 1是
     * @return
     */
    R<String> updateAutoUploadCloud(String secUid, Integer isAutoUploadCloud);

    /**
     * 绑定主播和用户的关系
     * @param userAnchorBo 绑定信息
     * @return
     */
    R<String> bindUserAnchor(UserAnchorBo userAnchorBo);

    /**
     * 更新主播置顶信息
     * @param secUid 主播secuid
     * @param action 动作 0：取消置顶 1：置顶
     * @param addTopTime 添加置顶的时间
     * @return
     */
    R<String> updateAnchorTop(String secUid, Integer action, String addTopTime);

    /**
     * 更新主播最后开始录制时间
     * @param secUid 主播secuid
     * @param lastRecordTime 最后开始录制时间
     * @return
     */
    R<String> updateAnchorLastRecordTime(String secUid, String lastRecordTime);

    /**
     * 客户端获取AI复盘主播列表
     * @param clientAnchorListBo 查询参数
     * @return
     */
    R<PageUtils<AnchorRecordListVo>> clientAnchorRecordList(ClientAnchorListBo clientAnchorListBo);

    /**
     * 置顶主播
     * @param topAnchorBo 置顶主播参数
     * @return
     */
    R<String> topAnchor(TopAnchorBo topAnchorBo);

    /**
     * 根据secUid获取用户主播信息（可获取同租户下的）
     * @param secUid 主播SecUid
     * @return
     */
    R<AnchorUrlUserVo> getUserAnchorBySecUid(String secUid);

    /**
     * 客户端获取主播列表
     * @return
     */
    R<List<AnchorUrlUserVo>> clientAnchorList();

    /**
     * 根据主播secuid集合获取昨日录制场次列表
     * @param secUidList 主播secuid集合
     * @return
     */
    R<List<AnchorYesterdayRecordVo>> listAnchorYesterdayRecord(List<String> secUidList);

    /**
     * 根据主播唯一标识(secUid、homeUrl、liveUrl)集合获取主播
     * @param uniquesList 主播唯一标识集合
     * @return
     */
    R<List<AnchorUrlInfoVo>> listByUniques(List<String> uniquesList);

    /**
     * 添加或修改用户的主播信息
     * @param addOrUpdateAnchorBo 添加或修改用户的主播信息参数
     * @return
     */
    R<String> addOrUpdateAnchor(AddOrUpdateAnchorBo addOrUpdateAnchorBo);

    /**
     * 客户端获取云空间主播列表
     * @return
     */
    R<List<AnchorUrlUserVo>> clientTenantAnchorList();

    /**
     * 根据secUid获取用户主播信息
     * @param secUid 主播SecUid
     * @return
     */
    R<AnchorUrlUserVo> getCurrUserAnchorBySecUid(String secUid);

    /**
     * 新添加的主播后-自动打开监控位
     *
     * @param secUid
     * @return
     */
    R<OpenMonitoringPositionVo> openMonitoringPosition(String secUid);

    /**
     * 生成关键词
     *
     * @param secUid 主播
     */
    void generatedAnchorKeywords(String secUid);

    /**
     * 生成关键词
     *
     * @param secUid  主播
     * @param videoId 视频id
     * @return 关键词
     */
    List<String> startGetAnchorKeywords(String secUid, String videoId);

    /**
     * 获取Ai推荐的行业
     *
     * @param secUid
     */
    void getAiTrade(String secUid);

    /**
     * 获取授权用量信息
     *
     * @param authType 授权类型 1=巨量 / 2=千川 / 3=来客
     * @return 授权用量信息
     */
    R<AuthUsageVo> getAuthUsage(Integer authType);
}

