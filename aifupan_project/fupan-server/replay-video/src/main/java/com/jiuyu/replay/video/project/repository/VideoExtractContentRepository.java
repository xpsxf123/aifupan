package com.jiuyu.replay.video.project.repository;

import com.jiuyu.replay.video.project.document.VideoContentExtract;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 视频内容提取Repository
 *
 * @author RayChou
 * @date 2025-08-11
 * @description 视频内容提取MongoDB数据访问层
 */
@Repository
public interface VideoExtractContentRepository extends MongoRepository<VideoContentExtract, String> {

    /**
     * 根据视频文件hash查询内容
     *
     * @param videoHash 视频文件hash值
     * @return 视频内容
     */
    @Query("{'video_hash': ?0, 'is_deleted': 0}")
    Optional<VideoContentExtract> findByVideoHash(String videoHash);

    /**
     * 根据视频文件 hash 集合批量查询内容
     *
     * @param videoHashes 视频文件 hash 集合
     * @return 视频内容列表
     */
    @Query("{'video_hash': {$in: ?0}, 'is_deleted': 0}")
    List<VideoContentExtract> findByVideoHashIn(Collection<String> videoHashes);

}
