package com.jiuyu.replay.words.repository.mongo;

import com.jiuyu.replay.words.entity.VideoTextNotes;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 视频复盘全文笔记存储 for mongo
 *
 * @author HeHui
 * @date 2025-06-07 14:32
 */
@Repository
public interface VideoTextNotesRepository extends MongoRepository<VideoTextNotes, Long> {

    /**
     * 查询有效的视频全文笔记
     *
     * @param sourceId   视频源ID，用于标识视频的唯一性
     * @param sourceType 视频源类型，用于区分不同类型的视频源
     * @param notesType  笔记类型，用于过滤特定类型的笔记
     *
     * @return 返回一个Optional封装的VideoTextNotes对象，如果找不到匹配的笔记则返回空Optional
     */
    @Query("{sourceId: ?0, sourceType: ?1, notesType: ?2, isDeleted: false}")
    Optional<VideoTextNotes> findVideoTextNotes(String sourceId, int sourceType, int notesType);

    /**
     * 根据来源id集合和笔记类型获取小结列表
     *
     * @param sourceIds 来源id集合
     * @param notesType 笔记类型
     * @return 返回匹配的VideoTextNotes列表
     */
    @Query("{sourceId: {$in: ?0}, notesType: ?1, isDeleted: false}")
    List<VideoTextNotes> listBySourceIdsAndNotesType(List<String> sourceIds, int notesType);

    /**
     * 查询指定源ID、源类型和笔记类型的视频笔记列表
     *
     * @param sourceIds  源ID
     * @param sourceType 源类型
     * @param notesType  笔记类型，用于过滤特定类型的笔记
     *
     * @return 匹配的笔记列表
     */
    @Query(value = "{sourceId: {$in: ?0}, sourceType: ?1, notesType: ?2, isDeleted: false}", fields = "{ sourceId: 1}")
    List<VideoTextNotes> findSourceIdBySourceTypeAndNotesTypeAndIsDeleted(Collection<String> sourceIds, int sourceType,
            int notesType);

    /**
     * 根据 sourceIds 和 sourceType 查询未删除的视频文本笔记信息，仅返回 sourceId 和 notesType 字段。
     *
     * @param sourceIds  源 ID 集合
     * @param sourceType 源类型
     * @return 符合条件的 VideoTextNotes 列表
     */
    @Query(value = "{sourceId: {$in: ?0}, sourceType: ?1, isDeleted: false}", fields = "{ sourceId: 1, notesType: 1}")
    List<VideoTextNotes> findSourcesNotes(Collection<String> sourceIds, int sourceType);


    /**
     * 获取如果id >= idx的且tenantId或userId为null的数据 返回 limit条数据
     *
     * @param idx 索引
     *
     * @return {@link List }<{@link VideoTextNotes }>
     */
    @Query(value = "{_id: {$gte: ?0}, tenantId: null, userId: null}", fields = "{ _id: 1, sourceType: 1, sourceId: 1}")
    List<VideoTextNotes> findNotTenantNotes(long idx, int limit);

    // /**
    // * 更新视频文本笔记的升级信息
    // * 此方法用于更新指定ID的笔记对象的内容哈希、最新版本号、更新用户ID以及更新时间
    // *
    // * @param id 需要更新的笔记对象的唯一标识符
    // * @param contentHash 新的内容哈希值，用于标识笔记内容的变更
    // * @param userId 更新用户的唯一标识符，记录是谁进行了修改
    // * @param lastVersion 更新后的最新版本号，表示笔记内容的版本递增
    // * @param updateTime 笔记的更新时间，记录此次更新操作的时间戳
    // */
    // @Update("{ $match: { _id: ?0 }, $set: { " +
    // "contentHash: ?1, " +
    // "lastVersion: ?2, " +
    // "updateUserId: ?3, " +
    // "updateTime: ?4 " +
    // "}}")
    // int update(long id, String contentHash, long userId, int lastVersion,
    // LocalDateTime updateTime);
}
