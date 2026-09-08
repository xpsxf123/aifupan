package com.jiuyu.replay.words.producer;


import com.jiuyu.replay.generic.bo.words.video.SaveSliceCorrelationDataBo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.aiagent.VideoWatchHasVO;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.oceanEngine.OceanEngineDataBo;
import com.jiuyu.replay.words.bo.oceanEngine.OceanEngineProcessBo;
import com.jiuyu.replay.words.bo.viewing.UpdateConfuseDataByVideoIdBo;
import com.jiuyu.replay.words.bo.viewing.VideoDataViewingConfuseBo;
import com.jiuyu.replay.words.bo.viewing.VideoDataViewingConfuseListBo;
import com.jiuyu.replay.words.enums.DataViewingSourceTypeEnum;
import com.jiuyu.replay.words.enums.DataViewingStatusEnum;
import com.jiuyu.replay.words.vo.viewing.VideoDataViewingConfuseInfoVo;
import com.jiuyu.replay.words.vo.viewing.VideoDataViewingConfuseListVo;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 视频看盘混淆后的数据
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-04-12 16:31:15
 */
public interface VideoDataViewingConfuseProducer {


    /**
     * 视频看盘混淆后的数据列表
     * @param videoDataViewingConfuseListBo 视频看盘混淆后的数据列表查询参数
     * @return
     */
    PageUtils<VideoDataViewingConfuseListVo> queryPage(VideoDataViewingConfuseListBo videoDataViewingConfuseListBo);

    /**
    * 视频看盘混淆后的数据信息
    * @param id 视频看盘混淆后的数据id
    * @return
    */
    VideoDataViewingConfuseInfoVo info(Long id);

    /**
     * 新增视频看盘混淆后的数据
     * @param videoDataViewingConfuseBo 视频看盘混淆后的数据对象
     * @return
     */
     VideoDataViewingConfuseInfoVo save(VideoDataViewingConfuseBo videoDataViewingConfuseBo);

    /**
     * 修改视频看盘混淆后的数据
     * @param videoDataViewingConfuseBo 视频看盘混淆后的数据对象
     * @return
     */
    void update(VideoDataViewingConfuseBo videoDataViewingConfuseBo);

    /**
     * 删除视频看盘混淆后的数据
     * @param id 视频看盘混淆后的数据id
     * @return
     */
    void deleteById(Long id);


    /**
     * 根据视频id和用户信息获取数据看盘
     * @param videoId 视频id
     * @param userId 用户id
     * @param tenantId 租户id
     * @return
     */
    VideoDataViewingConfuseInfoVo infoByVideoIdAndUser(String videoId, Long userId, Long tenantId);

    /**
     * 根据视频id获取数据看盘
     * @param videoId
     * @return
     */
    VideoDataViewingConfuseInfoVo getByVideoId(String videoId);

    /**
     * 判断视频的看板数据是否存在
     * @param videoIds
     * @return
     */
    Map<String, Boolean> hasVideo(List<String> videoIds);

    /**
     * 根据异常回调状态修改对应请求id的记录的状态
     * @param requestId 请求id
     * @param code 回调状态
     */
    void callbackCodeErrorHandle(String requestId, Integer code);

    /**
     * 根据视频id集合获取看盘数据集合
     * @param videoIds 视频id 集合
     * @return
     */
    List<VideoDataViewingConfuseInfoVo> listByVideoIds(Collection<String> videoIds);

    /**
     * 创建数据看盘记录
     * @param videoId 视频id
     * @param userId 用户id
     * @param tenantId 租户id
     * @param anchorNumber 主播抖音号
     * @param requestId 请求id
     * @param dataStatus 数据状态 0：正在拉取 1：拉取成功 2：拉取失败 3：未收录主播 4：自动生成但视频未达到50分钟 5：资源不足 6：自动生成但主播未下播 7：已收录但直播列表为空 8：正确数据整理中
     * @return
     */
    void createDataViewingConfuse(String videoId, Long userId, Long tenantId, String anchorNumber, String requestId, Integer dataStatus, String batchNumber);

    /**
     * 根据视频id集合和状态获取看盘数据集合
     * @param videoIds 视频id 集合
     * @param dataStatus 数据状态 @{@link DataViewingStatusEnum}
     * @return
     */
    List<VideoDataViewingConfuseInfoVo> listByVideoIdsAndStatus(List<String> videoIds, Integer dataStatus);

    List<VideoDataViewingConfuseInfoVo> listByObj(VideoDataViewingConfuseBo videoDataViewingConfuseBo);

    /**
     * 保存巨量百应的数据到数据看板
     * @param oceanEngineDataBo 巨量百应的数据
     * @param oldDataViewingId 是否存在旧数据 如果不为空说明存在旧数据，存在则更新，不存在则新增
     * @return
     */
    VideoDataViewingConfuseInfoVo saveOceanEngine(OceanEngineDataBo oceanEngineDataBo, Long oldDataViewingId);

    /**
     * 根据视频id获取巨量百应数据
     * @param videoId 视频id
     * @return
     */
    VideoDataViewingConfuseInfoVo getOceanEngineByVideoId(String videoId);

    /**
     * 根据视频id获取数据看板数据（不限数据源，字段级混合补齐）。
     * <p>以「巨量百应」源为主，巨量为空的字段用「蝉妈妈」源对应字段补齐（不覆盖巨量已有非空值）；
     * 仅巨量则原样返回，仅蝉妈妈则返回蝉妈妈，均无返回 null。
     * 与 {@link #getOceanEngineByVideoId(String)} 不同，本方法不锁定单一数据源，
     * 供 AI Agent 等只关心看板指标、不关心来源的场景使用。</p>
     *
     * @param videoId 视频id
     * @return 混合补齐后的看板行（巨量百应优先），不存在返回 null
     */
    VideoDataViewingConfuseInfoVo getDashboardByVideoId(String videoId);

    /**
     * 修改数据看板的状态
     * @param videoDataViewingConfuseId 看板数据id
     * @param status 数据状态 @{@link DataViewingStatusEnum}
     */
    void updateDataStatus(Long videoDataViewingConfuseId, Integer status);

    /**
     * 根据视频id集合 和 状态 和 数据来源类型 获取看盘数据集合
     * @param videoIds 视频id 集合
     * @param dataStatus 数据状态 @{@link DataViewingStatusEnum}
     * @param sourceType 数据来源类型 @{@link DataViewingSourceTypeEnum}
     * @return
     */
    List<VideoDataViewingConfuseInfoVo> listByVideoIdsAndStatusAndSourceType(List<String> videoIds, Integer dataStatus, Integer sourceType);

    /**
     * 拷贝同租户下的巨量百应数据看板
     * @param videoDataViewingConfuseBo 拷贝源数据
     * @param userId 用户id
     * @param videoId 视频id
     */
    void copyTenantJlbyData(VideoDataViewingConfuseBo videoDataViewingConfuseBo, Long userId, String videoId);

    /**
     * 根据请求id获取数据看盘集合
     * @param requestId 请求id
     * @return
     */
    List<VideoDataViewingConfuseInfoVo> listByRequestId(String requestId);

    /**
     * 查询同租户下有没有对应的巨量百应数据
     * @param videoId 视频id
     * @return
     */
    VideoDataViewingConfuseInfoVo checkJlbyDataExist(String videoId);

    /**
     * 根据视频id删除看板数据
     * @param videoId 视频videoId
     * @return
     */
    void deleteByVideoId(String videoId);

    /**
     * 从巨量文件里获取数据
     *
     * @param videoId 视频id
     * @param ossPath oss的key
     * @return 巨量文件数据
     */
    List<OceanEngineProcessBo> getOceanEngineFileData(String videoId, String ossPath);

    /**
     * 从巨量文件里获取数据-没有缓存
     * @param videoId 视频id
     * @param ossPath oss的key
     * @return 巨量文件数据
     */
    List<OceanEngineProcessBo> getOceanEngineFileDataNotCache(String videoId, String ossPath);

    /**
     * 拷贝数据看板数据到切片视频看板数据
     * @param saveSliceCorrelationDataBo 切片信息
     */
    VideoDataViewingConfuseInfoVo copyDataViewingToSlice(SaveSliceCorrelationDataBo saveSliceCorrelationDataBo);

    /**
     * 根据数据看盘id修改数据状态
     * @param videoDataViewingId 数据看盘id
     * @param status 数据状态
     */
    void updateDataStatusByDataViewingId(Long videoDataViewingId, Integer status);

    /**
     * 根据数据看盘场次id修改数据状态
      * @param batchNumber 数据看盘场次id
     * @param status 数据状态
     */
    void updateDataStatusByBatchNumber(String batchNumber, Integer status);

    /**
     * 根据数据看盘id和数据类型获取数据看盘
     * @param videoDataViewingId 数据看盘id
     * @param type 数据类型
     * @return
     */
    List<VideoDataViewingConfuseInfoVo> listByDataViewingIdAndDataType(Long videoDataViewingId, Integer type);

    /**
     * 根据数据看盘场次id和数据类型获取数据看盘
      * @param batchNumber 数据看盘场次id
     * @param type 数据类型
     * @return
     */
    List<VideoDataViewingConfuseInfoVo> listByBatchNumberAndDataType(String batchNumber, Integer type);

    /**
     * 根据视频ID批量修改关联的混淆数据
     * <p>
     * 1. 根据videoId查询混淆表获取该视频的混淆数据
     * 2. 从查询结果中获取videoDataViewingId（关联的数据看盘ID）
     * 3. 如果videoDataViewingId有值，查询所有具有相同videoDataViewingId的混淆数据
     * 4. 批量更新所有关联的混淆数据记录（仅更新非空字段）
     * </p>
     *
     * @param bo 更新参数，包含videoId和需要修改的数据字段
     * @return 更新的记录数量
     */
    R<String> updateByVideoId(UpdateConfuseDataByVideoIdBo bo);

    /**
     * 批量查询视频是否已存在数据看盘
     * @param videoIds 视频ID集合
     * @return 视频ID和是否存在数据看盘的映射关系
     */
    Map<String, Boolean> getVideoHasDashboardMap(Collection<String> videoIds);


    /**
     * 批量查询视频观看次数
     * @param videoIds 视频ID集合
     * @return 视频ID和观看次数的映射关系
     */
    Map<String, VideoWatchHasVO> getVideoWatchNum(Collection<String> videoIds);
}

