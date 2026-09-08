package com.jiuyu.replay.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 缓存降级数据表
 * </p>
 *
 * @author RayChou
 * @since 2025-07-10
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("tb_cache_fallback_data")
public class CacheFallbackDataEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 原缓存键
     */
    @TableField("cache_key")
    private String cacheKey;

    /**
     * 值内容
     */
    @TableField("cache_value")
    private String cacheValue;

    /**
     * 数据类型(0:STRING 1:LIST 2:HASH 3:SET 4:ZSET)
     */
    @TableField("data_type")
    private Byte dataType;

    /**
     * 元素类型信息（用于精确反序列化）
     */
    @TableField("element_type")
    private String elementType;

    /**
     * 过期时间
     */
    @TableField("expire_time")
    private LocalDateTime expireTime;

    /**
     * 创建时间
     */
    @TableField("create_date")
    private LocalDateTime createDate;

    /**
     * 更新时间
     */
    @TableField("update_date")
    private LocalDateTime updateDate;

    /**
     * 是否已删除
     */
    @TableField("is_deleted")
    @TableLogic
    private Byte isDeleted;
}
