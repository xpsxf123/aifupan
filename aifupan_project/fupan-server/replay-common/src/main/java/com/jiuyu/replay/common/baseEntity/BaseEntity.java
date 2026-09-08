package com.jiuyu.replay.common.baseEntity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

/**
 * @author lyw
 */
@Data
public class BaseEntity {

    @TableField(fill = FieldFill.INSERT)
    private Long createId;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateId;
}
