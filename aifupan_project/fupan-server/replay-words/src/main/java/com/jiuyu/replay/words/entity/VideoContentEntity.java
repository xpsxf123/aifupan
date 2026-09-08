package com.jiuyu.replay.words.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/5/26 下午7:32
 */
@Data
@Schema(description = "ai记录")
@Document(collection = "replay_video_content")
@CompoundIndexes({
        @CompoundIndex(name = "sourceId_type_userId_tenantId_index", def = "{ 'sourceId': 1, 'type': 1, 'userId': 1, 'tenantId': 1 }")
})
public class VideoContentEntity {

    @Id
    private String id;

    /**
     * 视频id
     */
    private String sourceId;

    /**
     * 内容
     */
    private String content;

    /**
     * 提示词
     */
    private String cueWord;

    /**
     * 生成状态 0未生成，1已生成
     */
    private Integer generateStatus;

    /**
     * 段落 从0开始
     */
    private Integer paragraph;

    /**
     * 内容类型 1自然原文，2优化原文
     */
    private Integer type;

    /**
     * 类型 0视频，1文件，2对比分析
     */
    private Integer sourceType;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 租户id
     */
    private Long tenantId;

    /**
     * 删除标志 0不删除，1删除
     */
    private Integer isDeleted;

    /**
     * 创建时间
     */
    private Date createDate;

    /**
     * 更新时间
     */
    private Date updateDate;
}
