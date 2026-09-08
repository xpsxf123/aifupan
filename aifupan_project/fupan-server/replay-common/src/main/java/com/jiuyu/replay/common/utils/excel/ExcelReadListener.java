package com.jiuyu.replay.common.utils.excel;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * excel读取监听
 *
 * @author HeHui
 * @date 2024-11-21 15:48
 */
public class ExcelReadListener<D> extends AnalysisEventListener<D> {

    private final RowRead<D> excelRowRead;

    private final List<D> rows;

    public ExcelReadListener(final RowRead<D> excelRowRead) {
        this.excelRowRead = excelRowRead;
        this.rows = new ArrayList<>(1024);
    }


    /**
     * 当分析一行时触发调用此方法。
     *
     * @param data    一行的值。它表示 Excel 表格中的一行数据。
     * @param context 分析上下文。它包含当前分析过程的信息。
     */
    @Override
    public void invoke(final D data, final AnalysisContext context) {
        rows.add(data);
        int batch = 1000;
        if (rows.size() >= batch) {
            excelRowRead.save(rows);
            rows.clear();
        }
    }

    /**
     * 如果在所有分析完成后有需要执行的操作。
     *
     * @param context 分析上下文。它包含整个分析过程的信息，可用于获取分析结果或其他相关信息。
     */
    @Override
    public void doAfterAllAnalysed(final AnalysisContext context) {
        if (!rows.isEmpty()) {
            excelRowRead.save(rows);
            rows.clear();
        }
        excelRowRead.finish();
    }
}
