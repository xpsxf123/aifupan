package com.jiuyu.replay.words.producer;

import com.jiuyu.replay.generic.bo.words.GenerateVideoContentBo;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.words.AnchorVideoDetailInfoVo;
import com.jiuyu.replay.generic.vo.words.AnchorVideoDetailListVo;
import com.jiuyu.replay.generic.vo.words.AnchorVideoDetailVo;
import com.jiuyu.replay.generic.vo.words.VideoContentVo;
import com.jiuyu.replay.words.bo.video.AnchorVideoDetailBo;
import com.jiuyu.replay.words.bo.video.AnchorVideoDetailListBo;
import com.jiuyu.replay.words.vo.video.AnchorVideoFileAllVo;

import java.util.List;


/**
 * 视频的详情
 *
 * @author lyw
 * @email 1798883178@qq.com
 * @date 2025-05-27 15:30:33
 */
public interface AnchorVideoDetailProducer {


    /**
     * 视频的详情列表
     * @param anchorVideoDetailListBo 视频的详情列表查询参数
     * @return
     */
    PageUtils<AnchorVideoDetailListVo> queryPage(AnchorVideoDetailListBo anchorVideoDetailListBo);

    /**
    * 视频的详情信息
    * @param id 视频的详情id
    * @return
    */
    AnchorVideoDetailInfoVo info(Long id);

    /**
     * 修改视频的详情
     * @param anchorVideoDetailBo 视频的详情对象
     * @return
     */
    Boolean update(AnchorVideoDetailBo anchorVideoDetailBo);

    /**
     * 删除视频的详情
     * @param id 视频的详情id
     * @return
     */
    void deleteById(Long id);


    AnchorVideoDetailVo getVideoContent(String videoId, Integer type, AnchorVideoFileAllVo videoContentVo);

    List<VideoContentVo> selVideoContents(String videoId, Integer type,Integer sourceType,Long userId,Long tenantId);

    void saveVideoContents(List<VideoContentVo> voList,Long userId,Long tenantId,String videoId,Integer sourceType, Integer type);

    void inserto(GenerateVideoContentBo bo);

    /**
     * 查询视频详情，没钱就新增
     * @param anchorVideoDetailBo
     * @return
     */
    AnchorVideoDetailInfoVo getAndSave(AnchorVideoDetailBo anchorVideoDetailBo);

    AnchorVideoDetailVo updateContentStatusIng(GenerateVideoContentBo bo);

    AnchorVideoDetailVo getByVideoIdSet(GenerateVideoContentBo bo);

    AnchorVideoDetailVo getByVideo(Long userId, Long tenantId, String videoId,Integer type);

    void updateVideoDetailById(AnchorVideoDetailVo vo);

    /**
     * 根据视频id查询视频详情
     * @param videoIds
     * @return
     */
    List<AnchorVideoDetailInfoVo> listByVideoIds(List<String> videoIds);

    AnchorVideoDetailVo getEmtyByVideoId(String videoId);

    /**
     * 获取视频已分析完成的需要生成的自然/优化原文的数据
     * @param limit
     * @return
     */
    List<AnchorVideoDetailVo> selectByQuery(Integer limit);

    void toUpdateStatus(String sourceId, Integer type);

    /**
     * 获取待生成的优化、自然原文
     *
     * @return
     */
    List<AnchorVideoDetailInfoVo> contentByToGenerated(List<String> videoIds, Long userId, Long tenantId);

    /**
     * 更新
     *
     * @param detailBo
     * @return
     */
    boolean updateContentStatus(AnchorVideoDetailBo detailBo);

    /**
     * 更新原文状态
     *
     * @param id
     * @param type
     * @param contentStatus
     */
    boolean updateContentStatus(Long id, Integer type, int contentStatus);

    /**
     * 更新视频详情状态
     * @param sourceId
     * @param type
     */
    boolean updateDetailStatus(String sourceId, Integer type);

    /**
     * 根据视频id更新
     *
     * @param anchorVideoDetailBo
     * @return
     */
    Boolean updateByVideoId(AnchorVideoDetailBo anchorVideoDetailBo);

    /**
     * 初始化是否已推荐行业字段
     *
     * @param videoId 视频id
     */
    void initUpdateSuggestTrade(String videoId);

    /**
     * 根据sourceId更新内容状态（用于定时器异步生成流程）
     *
     * @param sourceId 视频id
     * @param type     内容类型 1自然/2优化
     * @param status   内容状态
     * @param resetJob 是否重置set_job为0
     */
    boolean updateContentStatusBySourceId(String sourceId, Integer type, int status, boolean resetJob);

    /**
     * 乐观锁标记拾取（set_job=0 → set_job=1），返回true表示拾取成功
     *
     * @param sourceId 视频id
     * @param type     内容类型 1自然/2优化
     */
    boolean markPickedUp(String sourceId, Integer type);
}

