package com.jiuyu.replay.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * SEO slug 变更历史，供官网旧地址 301。
 *
 * <p>只记 {@code oldSlug}，不记新值——新值永远从实体表现查，
 * 这样多次改名（x→y→z）时查 x 一步跳到 z，不产生 301 跳转链。
 *
 * <p><b>本表无 is_deleted 字段</b>：历史记录不做逻辑删除，
 * 因此不受 MyBatis-Plus 全局 logic-delete 影响，可以直接用 insert/update。
 *
 * @author claude
 * @date 2026-08-18
 */
@Data
@TableName("tb_seo_slug_history")
public class SeoSlugHistoryEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键，雪花 ID */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /** 对象类型：1 文章 2 分类 3 标签，取值见 SeoConstant.SLUG_ENTITY_* */
    private Integer entityType;

    /** 对应实体主键 */
    private Long entityId;

    /** 被替换掉的旧 slug */
    private String oldSlug;

    /** 记录时间 */
    private LocalDateTime createDate;
}
