package com.jiuyu.replay.common.utils.excel;

import java.util.List;

/**
 * excel 读取
 *
 * @author HeHui
 * @date 2024/11/21
 */
@FunctionalInterface
public interface RowRead<E> {


    /**
     * 保存
     *
     * @param rows 多行数据
     *
     * @return boolean
     */
    boolean save(List<E> rows);


    /**
     * 完成
     */
    default void finish() {
    }


}
