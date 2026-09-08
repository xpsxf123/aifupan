package com.jiuyu.replay.third.tablestore.entity;

import com.alicloud.openservices.tablestore.model.ColumnType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/1/17 下午4:51
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PrimaryKeyEntity  implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;

    private Object value;
}
