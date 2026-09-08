package com.jiuyu.replay.words.bll;

import cn.hutool.core.util.ObjectUtil;
import com.jiuyu.replay.generic.utils.PageUtils;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.viewing.VideoDataViewingConfuseBo;
import com.jiuyu.replay.words.bo.viewing.VideoDataViewingConfuseListBo;
import com.jiuyu.replay.words.enums.DataViewingStatusEnum;
import com.jiuyu.replay.words.producer.VideoDataViewingConfuseProducer;
import com.jiuyu.replay.words.vo.viewing.VideoDataViewingConfuseInfoVo;
import com.jiuyu.replay.words.vo.viewing.VideoDataViewingConfuseListVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;


/**
 * 视频看盘混淆后的数据
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-04-12 16:31:15
 */
@Component
public class VideoDataViewingConfuseBll {

    @Resource
    private VideoDataViewingConfuseProducer videoDataViewingConfuseProducer;


    /**
     * 视频看盘混淆后的数据列表
     * @param videoDataViewingConfuseListBo 视频看盘混淆后的数据列表查询参数
     * @return
     */
    public R<PageUtils<VideoDataViewingConfuseListVo>> queryPage(VideoDataViewingConfuseListBo videoDataViewingConfuseListBo) {

        return R.ok("获取成功", videoDataViewingConfuseProducer.queryPage(videoDataViewingConfuseListBo));
    }

    /**
    * 视频看盘混淆后的数据信息
    * @param id 视频看盘混淆后的数据id
    * @return
    */
    public R<VideoDataViewingConfuseInfoVo> info(Long id) {

        VideoDataViewingConfuseInfoVo videoDataViewingConfuseInfoVo = videoDataViewingConfuseProducer.info(id);
        return R.ok("获取成功", videoDataViewingConfuseInfoVo);
    }

    /**
     * 新增视频看盘混淆后的数据
     * @param videoDataViewingConfuseBo 视频看盘混淆后的数据对象
     * @return
     */
    public R<String> save(VideoDataViewingConfuseBo videoDataViewingConfuseBo) {

        VideoDataViewingConfuseInfoVo videoDataViewingConfuseInfoVo = videoDataViewingConfuseProducer.save(videoDataViewingConfuseBo);
        return R.ok("添加成功");
    }

    /**
     * 修改视频看盘混淆后的数据
     * @param videoDataViewingConfuseBo 视频看盘混淆后的数据对象
     * @return
     */
    public R<String> update(VideoDataViewingConfuseBo videoDataViewingConfuseBo) {

        videoDataViewingConfuseProducer.update(videoDataViewingConfuseBo);
        return R.ok("修改成功");
    }

    /**
     * 删除视频看盘混淆后的数据
     * @param id 视频看盘混淆后的数据id
     * @return
     */
    public R<String> delete(Long id) {

        videoDataViewingConfuseProducer.deleteById(id);
        return R.ok("删除成功");
    }

    /**
     * 根据视频id获取视频看盘混淆后的数据
     * @param videoId
     * @return
     */
    public R<VideoDataViewingConfuseInfoVo> getByVideoId(String videoId) {
        return R.ok(videoDataViewingConfuseProducer.getByVideoId(videoId));
    }

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
    public R<String> createDataViewing(String videoId, Long userId, Long tenantId, String anchorNumber, String requestId, Integer dataStatus, String batchNumber) {

        videoDataViewingConfuseProducer.createDataViewingConfuse(videoId, userId, tenantId, anchorNumber, requestId, dataStatus, batchNumber);
        return R.ok();
    }

    public R<Boolean> existVideoDataViewingConfuse(String videoId) {
        VideoDataViewingConfuseInfoVo byVideoId = videoDataViewingConfuseProducer.getByVideoId(videoId);
        return R.ok(ObjectUtil.isNotEmpty(byVideoId) && (Objects.equals(byVideoId.getDataStatus(), DataViewingStatusEnum.PULL_SUCCESS.getStatus()) || Objects.equals(byVideoId.getDataStatus(), DataViewingStatusEnum.DATA_ORGANIZE.getStatus())));
    }

    /**
     * 根据视频id集合和状态获取看盘数据集合
     *
     * @param videoIds   视频id 集合
     * @param dataStatus 数据状态 @{@link DataViewingStatusEnum}
     * @return 集合
     */
    public List<VideoDataViewingConfuseInfoVo> listByVideoIdsAndStatus(List<String> videoIds, Integer dataStatus) {
        return videoDataViewingConfuseProducer.listByVideoIdsAndStatus(videoIds, dataStatus);
    }

    /**
     * 根据请求id获取数据看盘集合
     * @param requestId 请求id
     * @return
     */
    public R<List<VideoDataViewingConfuseInfoVo>> listByRequestId(String requestId) {

        return R.ok(videoDataViewingConfuseProducer.listByRequestId(requestId));
    }

    /**
     * 根据视频id删除看板数据
     * @param videoId 视频videoId
     * @return
     */
    public R<String> deleteByVideoId(String videoId) {

        this.videoDataViewingConfuseProducer.deleteByVideoId(videoId);

        return R.ok();
    }
}

