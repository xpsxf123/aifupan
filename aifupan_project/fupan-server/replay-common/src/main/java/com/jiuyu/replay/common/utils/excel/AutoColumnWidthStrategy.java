package com.jiuyu.replay.common.utils.excel;

import com.alibaba.excel.enums.CellDataTypeEnum;
import com.alibaba.excel.metadata.Head;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.util.MapUtils;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.style.column.AbstractColumnWidthStyleStrategy;
import org.apache.poi.ss.usermodel.Cell;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * 自动列宽策略
 * copy 于 {@link com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy}
 *
 * @author HeHui
 * @date 2024-12-20 18:16
 */
public class AutoColumnWidthStrategy extends AbstractColumnWidthStyleStrategy {

    private static final int MAX_COLUMN_WIDTH = 255;

    private static final Pattern CHINESE = Pattern.compile("[\u4E00-\u9FFF]+", Pattern.DOTALL);

    private final Map<Integer, Map<Integer, Integer>> cache = MapUtils.newHashMapWithExpectedSize(8);

    private final Map<String, Integer> stringLengMap = new HashMap<>(512);

    @Override
    protected void setColumnWidth(WriteSheetHolder writeSheetHolder, List<WriteCellData<?>> cellDataList, Cell cell, Head head, Integer relativeRowIndex, Boolean isHead) {

        Map<Integer, Integer> maxColumnWidthMap = cache.computeIfAbsent(writeSheetHolder.getSheetNo(), key -> new HashMap<>(16));
        Integer columnWidth = dataLength(cellDataList, cell, isHead);
        if (columnWidth < 0) {
            return;
        }
        if (columnWidth > MAX_COLUMN_WIDTH) {
            columnWidth = MAX_COLUMN_WIDTH;
        }
        Integer maxColumnWidth = maxColumnWidthMap.get(cell.getColumnIndex());
        if (maxColumnWidth == null || columnWidth > maxColumnWidth) {
            maxColumnWidthMap.put(cell.getColumnIndex(), columnWidth);

            // 使用位操作替代乘法
            int widthInUnits = columnWidth << 8; // 256 = 2^8
            // 最低动态扩容宽度
            boolean minWidth = columnWidth > 8;
            // 判断超大宽度的单元格 避免因为基数过大而造成的超大容量
            int difference = Math.max(96 - columnWidth, 10);
            if ((minWidth && columnWidth < 15) || isHead) {
                widthInUnits += columnWidth << 7; // 384 = 256 + 128 = 2^8 + 2^7
            } else if (minWidth) {
                // 超大宽度的动态扩容
                widthInUnits += difference * columnWidth;
            }

            writeSheetHolder.getSheet().setColumnWidth(cell.getColumnIndex(), widthInUnits);
        }
    }

    /**
     * 计算单元格数据的长度
     *
     * @param cellDataList 单元格数据列表
     * @param cell         当前单元格
     * @param isHead       是否是表头
     *
     * @return 返回单元格数据的字节长度，如果是表头则直接返回字符串字节长度，否则根据数据类型计算长度
     */
    private Integer dataLength(List<WriteCellData<?>> cellDataList, Cell cell, Boolean isHead) {
        // 如果是表头，直接返回字符串字节长度
        if (isHead) {
            return cell.getStringCellValue().getBytes().length;
        }
        // 获取第一个单元格数据
        WriteCellData<?> cellData = cellDataList.get(0);
        // 获取单元格数据类型
        CellDataTypeEnum type = cellData.getType();
        // 如果类型为空，返回-1
        if (type == null) {
            return -1;
        }
        // 根据不同的数据类型，计算长度
        switch (type) {
            case STRING:
                // 对于字符串类型，先获取字符串字节长度，然后根据是否包含中文调整长度
                return getStringLength(cellData.getStringValue(), str -> {
                    int length = cellData.getStringValue().getBytes().length;
                    // 如果字符串中包含中文，使用整数运算替代浮点运算调整长度
                    if (hasChinese(cellData.getStringValue())) {
                        return length * 7 / 10;
                    }
                    return length;
                });
            case BOOLEAN:
                // 对于布尔类型，直接返回字符串表示的字节长度
                return getStringLength(cellData.getBooleanValue().toString(), str -> str.getBytes().length);
            case NUMBER:
                // 对于数字类型，直接返回字符串表示的字节长度
                return getStringLength(cellData.getNumberValue().toString(), str -> str.getBytes().length);
            default:
                // 对于不支持的类型，返回-1
                return -1;
        }
    }


    /**
     * 获取字符串长度
     *
     * @param str         str
     * @param otherLength 其他长度
     *
     * @return int
     */
    private int getStringLength(String str, Function<String, Integer> otherLength) {
        if (str == null) {
            return 0;
        }
        return stringLengMap.computeIfAbsent(str, otherLength);
    }

    /**
     * 检查字符串中是否包含中文字符
     *
     * @param str 待检查的字符串
     *
     * @return 如果字符串中包含中文字符，则返回true；否则返回false
     */
    private boolean hasChinese(String str) {
        // 使用预定义的正则表达式匹配中文字符
        return CHINESE.matcher(str).find();
    }


}
