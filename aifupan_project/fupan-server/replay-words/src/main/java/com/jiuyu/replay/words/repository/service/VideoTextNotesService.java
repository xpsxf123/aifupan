package com.jiuyu.replay.words.repository.service;


import cn.hutool.core.collection.CollUtil;
import com.jiuyu.framework.shandard.PageData;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.power.UserCacheVo;
import com.jiuyu.replay.words.bo.VideoNotesQueryBo;
import com.jiuyu.replay.words.enums.NotesType;
import com.jiuyu.replay.words.enums.VideoSourceType;
import com.jiuyu.replay.words.vo.NotesContentVo;
import com.jiuyu.replay.words.vo.VideoNotesInfoVo;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * 视频复盘全文笔记服务
 * 原文笔记，复盘小结，分段笔记
 *
 * @author HeHui
 * @date 2025-06-07 14:36
 */
public interface VideoTextNotesService {



    /**
     * 初始化数据租户和所属用户
     */
    void initTenantNotes();


    /**
     * 保存笔记信息
     *
     * @param sourceId   笔记来源的ID，表示笔记关联的视频ID
     * @param sourceType 笔记来源的类型，用于区分笔记所属的视频类型
     * @param notesType  笔记的类型  1: 原文笔记,2：复盘小结，3分段笔记
     * @param content    笔记的内容，即用户输入的笔记文本信息
     * @param user       用户信息，表示执行保存笔记操作的用户
     *
     * @return {@link R }<{@link Long }> 表示新生成的笔记ID
     */
    R<Long> saveNotes(String sourceId, VideoSourceType sourceType, NotesType notesType, String content, UserCacheVo user);


    /**
     * 获取笔记内容
     *
     * @param sourceId   笔记来源的ID，表示笔记关联的视频ID
     * @param sourceType 笔记来源的类型，用于区分笔记所属的视频类型
     * @param notesType  笔记的类型  1: 原文笔记,2：复盘小结，3分段笔记
     * @param user       当前用户信息，用于验证权限
     *
     * @return Optional<NotesContentVo> 返回一个Optional封装的字符串，可能包含笔记内容，如果找不到相关笔记则返回空Optional
     */
    Optional<NotesContentVo> getNotesContent(String sourceId, VideoSourceType sourceType, NotesType notesType, UserCacheVo user);


    /**
     * 获取历史记录内容
     *
     * @param sourceId   源id
     * @param sourceType 源类型
     * @param notesType  笔记类型
     * @param version    版本
     * @param user       用户
     *
     * @return {@link Optional }<{@link String }>
     */
    Optional<String> getHistoryContent(String sourceId, VideoSourceType sourceType, NotesType notesType, int version, UserCacheVo user);

    /**
     * 获取是否存在笔记
     *
     * @param sourceIds  源id集合
     * @param sourceType 源类型
     * @param notesType  笔记类型
     *
     * @return {@link Map }<{@link String }, {@link Boolean }> key:源id value:是否存在笔记
     */
    Map<String, Boolean> getExistsNotes(Collection<String> sourceIds, VideoSourceType sourceType, NotesType notesType);

    /**
     * 源是否存在笔记
     *
     * @param sourceIds  源id集合
     * @param sourceType 源类型
     *
     * @return {@link BiFunction }<{@link String }, {@link NotesType }, {@link Boolean }> param1:源id param2:笔记类型
     */
    BiFunction<String, NotesType, Boolean> sourceExists(Collection<String> sourceIds, VideoSourceType sourceType);

    /**
     * 获取源数据小结（数据截图/数据看板）
     *
     * @param sourceId    源id
     * @param sourceType  源类型
     *
     * @return {@link Optional }<{@link String }>
     */
    Optional<String> getSourceDataSummary(String sourceId, VideoSourceType sourceType);






    /**
     * 查询视频笔记信息
     *
     * @param queryBo 查询条件
     *
     * @return {@link PageData }<{@link VideoNotesInfoVo }>
     */
    PageData<VideoNotesInfoVo> queryPage(VideoNotesQueryBo queryBo);



    /**
     * 批量填充是否存在笔记
     *
     * @param items          数据项
     * @param getSourceId    获取源id
     * @param setExistsNotes 设置是否存在笔记
     * @param sourceType     源类型
     */
    default <T> void fullExistsNotes(Collection<T> items, Function<T, String> getSourceId,  VideoSourceType sourceType, BiConsumer<T, Integer> setNotesSummary,  BiConsumer<T, Integer> setExistsNotes) {
        if (CollUtil.isEmpty(items)) {
            return;
        }
        List<String> sourceIds = items.stream().map(getSourceId).filter(Objects::nonNull).distinct().toList();
        BiFunction<String, NotesType, Boolean> sourceExists = sourceExists(sourceIds, sourceType);
        items.forEach(item -> {
            setNotesSummary.accept(item, sourceExists.apply(getSourceId.apply(item), NotesType.REVIEW_NOTES) ? 1 : 0);
            setExistsNotes.accept(item, sourceExists.apply(getSourceId.apply(item), NotesType.ORIGINAL_NOTES) || sourceExists.apply(getSourceId.apply(item), NotesType.SECTION_NOTES) ? 1 : 0);
        });
    }
}
