package com.jiuyu.replay.words.producer;

import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.bo.video.AnalysisMarkInfoBo;
import com.jiuyu.replay.words.enums.VideoSourceType;
import com.jiuyu.replay.words.vo.AnalysisMarkVo;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * 标注生产者接口
 *
 * @author liaoxin
 * @date 2025-06-07
 * @description 提供视频段落标注的业务逻辑处理接口，包括标注的增删改查功能
 */
public interface AnalysisMarkProducer {

    /**
     * 新增标注
     *
     * @param bo 新增数据，包含标注的详细信息
     *
     * @return {@link R}<{@link Long}> 操作结果，成功时返回标注ID
     *
     * @description 创建新的视频段落标注记录
     */
    R<Long> add(AnalysisMarkInfoBo bo);

    /**
     * 查询视频标注信息
     *
     * @param videoId       视频ID，用于查询指定视频的标注
     * @param sourceType    视频来源类型，配合视频ID进行精确查询
     * @param startParaphNo 段落编号（开始编号），可选参数，用于范围查询
     * @param endParaphNo   段落编号（结束编号），可选参数，用于范围查询
     *
     * @return {@link R}<{@link List}<{@link AnalysisMarkVo}>> 标注信息列表
     *
     * @description 根据视频ID和来源类型查询标注信息，支持按段落编号范围过滤
     */
    R<List<AnalysisMarkVo>> info(String videoId, Integer sourceType, Integer startParaphNo, Integer endParaphNo);

    /**
     * 修改标注信息
     *
     * @param bo 修改数据，包含需要更新的标注信息
     *
     * @return {@link R}<{@link Long}> 操作结果，成功时返回受影响的记录数
     *
     * @description 更新已存在的标注记录，支持修改标注内容或删除标注
     */
    R<Long> edit(AnalysisMarkInfoBo bo);

    /**
     * 删除标注
     *
     * @param id 标注ID，需要删除的标注主键
     *
     * @return {@link R}<{@link Long}> 操作结果，成功时返回标注ID
     *
     * @description 逻辑删除指定的视频标注记录，会设置删除标记而非物理删除
     */
    R<Long> delete(Long id);


    /**
     * 批量查询视频标注信息并填充
     *
     * @param items         视频列表，用于批量查询指定视频的标注
     * @param sourceType    视频来源类型，配合视频ID进行精确查询
     * @param setExistsMark 批量查询结果处理函数，用于设置查询结果
     */
    <T> void fullExistsMark(List<T> items, Function<T, String> getVideoId, VideoSourceType sourceType, BiConsumer<T, Integer> setExistsMark);

}