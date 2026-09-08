package com.jiuyu.replay.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * SEO 分类（官网栏目）。
 *
 * <p>平台级数据，无 tenantId——与 tb_dict_data 等平台配置表一致。
 *
 * @author claude
 * @date 2026-08-12
 */
@Data
@TableName("tb_seo_category")
public class SeoCategoryEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键，雪花 ID */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /** 分类名称，业务限 1-20 字；删除时改写墓碑值 */
    private String categoryName;

    /** 别名，官网 /category/{slug}；删除时改写墓碑值 */
    private String slug;

    /** 分类描述，作为官网聚合页 SEO 描述 */
    private String description;

    /** 排序，越小越靠前 */
    private Integer sort;

    /** 软删除标记：0 正常 1 已删除 */
    private Integer isDeleted;

    /** 创建时间 */
    private LocalDateTime createDate;

    /** 最后修改时间 */
    private LocalDateTime updateDate;
}
