package com.jiuyu.governance.plugins.excel;

import com.jiuyu.framework.function.BatchQuery;
import com.jiuyu.framework.util.EmptyUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.fesod.sheet.ExcelWriter;
import org.apache.fesod.sheet.FesodSheet;
import org.apache.fesod.sheet.support.ExcelTypeEnum;
import org.apache.fesod.sheet.write.builder.ExcelWriterBuilder;
import org.apache.fesod.sheet.write.metadata.WriteSheet;
import org.apache.fesod.sheet.write.metadata.style.WriteCellStyle;
import org.apache.fesod.sheet.write.metadata.style.WriteFont;
import org.apache.fesod.sheet.write.style.HorizontalCellStyleStrategy;
import org.apache.poi.ss.usermodel.*;
import org.springframework.http.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * easy-excel 工具
 *
 * @author HeHui
 * @date 2024-11-21 15:04
 */
@Slf4j
public class ExcelTemplate {

    private ExcelTemplate() {
    }


    /**
     * 使用EasyExcel库从Excel文件中读取数据
     *
     * @param inputStream Excel文件的输入流
     * @param clazz       指定Excel数据应映射到的Java对象类型
     * @param headLineNum 标题行的位置
     * @param sheetNo     需要读取的sheet编号
     * @param password    Excel文件的解密密码
     *
     * @return {@link List }<{@link T }> 返回一个包含Java对象的列表，每个对象代表Excel中的一行数据
     */
    public static <T> List<T> getRows(InputStream inputStream, Class<T> clazz, int headLineNum, int sheetNo, String password) {
        return FesodSheet.read(inputStream)
            .password(password) // 设置Excel文件的解密密码
            .headRowNumber(headLineNum) // 设置标题行的位置
            .ignoreEmptyRow(true) // 忽略空行
            .sheet(sheetNo) // 选择需要读取的sheet
            .head(clazz) // 指定Excel数据应映射到的Java对象类型
            .doReadSync(); // 执行Excel文件的读取操作
    }

    /**
     * 读取Excel内容
     *
     * @param inputStream Excel文件的输入流，用于读取Excel文件数据
     * @param clazz       数据模型类的Class对象，指定Excel数据应映射到的Java对象类型
     * @param rowRead     实现了RowRead接口的对象，用于处理每一行Excel数据
     * @param headLineNum Excel文件中标题行的位置，从0开始计数
     * @param sheetNo     需要读取的sheet编号，从0开始计数
     * @param password    如果Excel文件被加密，需要提供密码进行解密
     */
    public static <T> void readExcel(InputStream inputStream, Class<T> clazz, RowRead<T> rowRead, int headLineNum, int sheetNo, String password) {
        // 创建Excel读取监听器，用于处理读取过程中的每一行数据
        ExcelReadListener<T> readListener = new ExcelReadListener<>(rowRead);
        // 使用EasyExcelFactory读取Excel文件，并配置读取参数
        FesodSheet.read(inputStream, clazz, readListener)
            .password(password) // 设置Excel文件的解密密码
            .headRowNumber(headLineNum) // 设置标题行的位置
            .ignoreEmptyRow(true) // 忽略空行
            .sheet(sheetNo) // 选择需要读取的sheet
            .doRead(); // 执行Excel文件的读取操作
    }


    /**
     * 直接将 List 数据写入 Excel 并下载（无游标分批，适用于已加载到内存的全量数据）
     *
     * @param rows                    数据列表
     * @param fileName                下载文件名
     * @param sheetName               Sheet 名称
     * @param excludeColumnFieldNames 需要排除的列字段名集合
     * @param clazz                   数据对象的类类型
     *
     * @return 包含 Excel 文件的 HTTP 响应实体
     */
    public static <T> ResponseEntity<byte[]> downloadList(List<T> rows,
                                                          String fileName,
                                                          String sheetName,
                                                          Set<String> excludeColumnFieldNames,
                                                          Class<T> clazz) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(2048)) {
            ExcelWriter writer = buildWrite(clazz, outputStream, null, excludeColumnFieldNames, ExcelTypeEnum.XLSX, null, null);
            WriteSheet writeSheet = buildSheet(0, sheetName);
            writer.write(rows == null || rows.isEmpty() ? Collections.emptyList() : rows, writeSheet);
            writer.finish();
            HttpHeaders headers = new HttpHeaders();
            ContentDisposition contentDisposition = ContentDisposition.attachment()
                    .filename(URLEncoder.encode(fileName, StandardCharsets.UTF_8))
                    .build();
            headers.setContentDisposition(contentDisposition);
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            return new ResponseEntity<>(outputStream.toByteArray(), headers, HttpStatus.OK);
        } catch (IOException e) {
            throw new RuntimeException("Excel文件下载失败", e);
        }
    }

    /**
     * 创建一个Excel文件并下载(不推荐使用)
     *
     * @param rows                      需要写入的行数据
     * @param idMapper                  用于从对象中提取ID的函数
     * @param fileName                  下载文件时显示的文件名
     * @param sheetName                 Excel文件中的工作表名
     * @param excludeColumnFieldNames   需要排除的列字段名集合
     * @param clazz                     数据对象的类类型
     *
     * @return 响应实体，包含生成的Excel文件
     */
    public <T, ID> ResponseEntity<byte[]> download(List<T> rows,
                                                   Function<T, ID> idMapper,
                                                   String fileName,
                                                   String sheetName,
                                                   Set<String> excludeColumnFieldNames,
                                                   Class<T> clazz) {
        return download(EmptyUtil.isEmpty(rows) ? 0 : rows.size() - 1, idx -> rows, idMapper, fileName, sheetName, excludeColumnFieldNames, clazz, null);
    }


    /**
     * 下载功能方法，用于按批次下载数据
     *
     * @param batchLimit 每次下载的数据条数限制
     * @param nextRows   函数用于获取下一批数据
     * @param idMapper   函数用于从对象中提取唯一标识符
     * @param fileName   下载文件的名称
     * @param clazz      数据对象的类类型
     *
     * @return 包含文件数据的Http响应实体
     */
    public static <ID, T> ResponseEntity<byte[]> download(int batchLimit,
                                                          Function<ID, List<T>> nextRows,
                                                          Function<T, ID> idMapper, String fileName, Class<T> clazz) {
        // 调用重载的download方法，提供默认的表名"Sheet1"，空的样式集合和数据对象类类型
        return download(batchLimit, nextRows, idMapper, fileName, "Sheet1", Collections.emptySet(), clazz);
    }


    /**
     * 根据给定的参数下载一个Excel文件
     *
     * @param batchLimit              每次批量查询的限制数量
     * @param nextRows                用于获取下一批行数据的函数
     * @param idMapper                用于从对象中提取ID的函数
     * @param fileName                下载文件时显示的文件名
     * @param sheetName               Excel文件中的工作表名
     * @param excludeColumnFieldNames 需要排除的列字段名集合
     * @param clazz                   数据对象的类类型
     *
     * @return 包含Excel文件的HTTP响应实体
     *     <p>
     *     此方法通过批量查询数据并将其写入Excel文件中，然后将文件作为HTTP响应返回，以便客户端可以下载
     */
    public static <ID, T> ResponseEntity<byte[]> download(int batchLimit,
                                                          Function<ID, List<T>> nextRows,
                                                          Function<T, ID> idMapper,
                                                          String fileName,
                                                          String sheetName,
                                                          Set<String> excludeColumnFieldNames,
                                                          Class<T> clazz) {
        return download(batchLimit, nextRows, idMapper, fileName, sheetName, excludeColumnFieldNames, clazz, null);
    }

    /**
     * 根据给定的参数下载一个Excel文件
     *
     * @param batchLimit              每次批量查询的限制数量
     * @param nextRows                用于获取下一批行数据的函数
     * @param idMapper                用于从对象中提取ID的函数
     * @param fileName                下载文件时显示的文件名
     * @param sheetName               Excel文件中的工作表名
     * @param excludeColumnFieldNames 需要排除的列字段名集合
     * @param clazz                   数据对象的类类型
     * @param customized              自定义ExcelWriterBuilder的函数，用于对ExcelWriter进行自定义配置
     *
     * @return 包含Excel文件的HTTP响应实体
     *     <p>
     *     此方法通过批量查询数据并将其写入Excel文件中，然后将文件作为HTTP响应返回，以便客户端可以下载
     */
    public static <ID, T> ResponseEntity<byte[]> download(int batchLimit,
                                                          Function<ID, List<T>> nextRows,
                                                          Function<T, ID> idMapper,
                                                          String fileName,
                                                          String sheetName,
                                                          Set<String> excludeColumnFieldNames,
                                                          Class<T> clazz, Function<ExcelWriterBuilder, ExcelWriter> customized) {

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(2048)) {
            // 构建ExcelWriter对象，用于写入Excel文件
            ExcelWriter writer = buildWrite(clazz, outputStream, null, excludeColumnFieldNames, ExcelTypeEnum.XLSX, null, customized);
            WriteSheet writeSheet = buildSheet(0, sheetName);
            // 写入数据到Excel文件中
            new BatchQuery<>(batchLimit, nextRows, idMapper).consumer(rows -> {
                writer.write(rows, writeSheet);
            }).run(null);
            // 创建HTTP响应头，用于设置文件下载相关的信息
            HttpHeaders headers = new HttpHeaders();
            // 设置文件下载时的文件名和编码方式
            ContentDisposition contentDisposition = ContentDisposition.attachment().filename(URLEncoder.encode(fileName, StandardCharsets.UTF_8)).build();
            headers.setContentDisposition(contentDisposition);
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            // 完成写入操作
            writer.finish();
            // 返回包含Excel文件的响应体
            return new ResponseEntity<>(outputStream.toByteArray(), headers, HttpStatus.OK);
        } catch (IOException e) {
            // 如果发生IO异常，抛出自定义的ExcelException异常
            throw new RuntimeException("Excel文件下载失败", e);
        }
    }


    /**
     * 构建写入器
     *
     * @param clazz                   数据对象的类，用于映射到Excel表格的列
     * @param outputStream            输出流，用于写入Excel文件
     * @param password                Excel文件的密码，用于加密文件
     * @param excludeColumnFieldNames 要排除的列字段名集合，防止某些字段被写入Excel
     * @param type                    类型
     * @param templatePath            模板路径
     *
     * @return {@link ExcelWriter }
     */
    public static <T> ExcelWriter buildWrite(Class<T> clazz, OutputStream outputStream, String password, Set<String> excludeColumnFieldNames, ExcelTypeEnum type, String templatePath, Function<ExcelWriterBuilder, ExcelWriter> customized) {
        try {
            ExcelWriterBuilder builder = FesodSheet.write(outputStream, clazz)
                .autoCloseStream(false)
                // 自动适配宽度
                .registerWriteHandler(new AutoColumnWidthStrategy())
                .registerWriteHandler(new HorizontalCellStyleStrategy(ExcelTemplate.getHeadStyle(IndexedColors.WHITE, (short) 14), ExcelTemplate.getContentStyle((short) 10)))
                .password(password)
                .excelType(type)
                .excludeColumnFieldNames(excludeColumnFieldNames);
            if (EmptyUtil.isNotEmpty(templatePath)) {
                builder.withTemplate(templatePath);
            }
            return customized != null ? customized.apply(builder) : builder.build();
        } catch (Exception e) {
            // 记录日志
            log.error("Export excel occurred an error: {}. ", e.getMessage(), e);
            // 抛出自定义异常
            throw new RuntimeException("导出 Excel 出现错误");
        }
    }

    /**
     * 构建工作空间
     *
     * @param sheetNo   编号 从0开始
     * @param sheetName 工作表名称
     *
     * @return {@link WriteSheet }
     */
    public static WriteSheet buildSheet(int sheetNo, String sheetName) {
        WriteSheet writeSheet = new WriteSheet();
        writeSheet.setSheetNo(sheetNo);
        writeSheet.setSheetName(sheetName);
        return writeSheet;
    }


    /**
     * 获得头部风格
     *
     * @return {@link WriteCellStyle }
     */
    public static WriteCellStyle getHeadStyle(IndexedColors color, final short headColors) {
        WriteCellStyle headWriteCellStyle = new WriteCellStyle();
        headWriteCellStyle.setWrapped(false);
        headWriteCellStyle.setShrinkToFit(false);
        headWriteCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headWriteCellStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);
        headWriteCellStyle.setLocked(false);
        headWriteCellStyle.setFillPatternType(FillPatternType.SOLID_FOREGROUND);
        headWriteCellStyle.setFillForegroundColor(color == null ? IndexedColors.GREY_25_PERCENT.getIndex() : color.getIndex());


        headWriteCellStyle.setBorderBottom(BorderStyle.THIN);
        headWriteCellStyle.setBottomBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
        headWriteCellStyle.setBorderLeft(BorderStyle.THIN);
        headWriteCellStyle.setLeftBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
        headWriteCellStyle.setBorderRight(BorderStyle.THIN);
        headWriteCellStyle.setRightBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
        headWriteCellStyle.setBorderTop(BorderStyle.THIN);
        headWriteCellStyle.setTopBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());

        WriteFont headWriteFont = new WriteFont();
        headWriteFont.setFontName("微软雅黑");
        headWriteFont.setFontHeightInPoints(headColors);
        headWriteFont.setBold(true);
        headWriteCellStyle.setWriteFont(headWriteFont);
        return headWriteCellStyle;
    }

    /**
     * 获取内容样式
     *
     * @return {@link WriteCellStyle }
     */
    public static WriteCellStyle getContentStyle(short fontHeight) {
        WriteCellStyle contentWriteCellStyle = new WriteCellStyle();
        contentWriteCellStyle.setWrapped(true);
        contentWriteCellStyle.setShrinkToFit(false);
        contentWriteCellStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);
        contentWriteCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        contentWriteCellStyle.setBorderBottom(BorderStyle.THIN);
        contentWriteCellStyle.setBorderLeft(BorderStyle.THIN);
        contentWriteCellStyle.setBorderRight(BorderStyle.THIN);
        contentWriteCellStyle.setBorderTop(BorderStyle.THIN);
        contentWriteCellStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());

        WriteFont contentWriteFont = new WriteFont();
        contentWriteFont.setFontName("微软雅黑");
        contentWriteFont.setFontHeightInPoints(fontHeight);
        contentWriteCellStyle.setWriteFont(contentWriteFont);
        return contentWriteCellStyle;
    }

    /**
     * 单元格样式
     *
     * @param alignment  对齐
     * @param fontHeight 字体高度
     * @param bottom     底部
     * @param top        顶部
     * @param left       左边
     * @param right      右边
     *
     * @return {@link WriteCellStyle }
     */
    public static WriteCellStyle cellStyle(HorizontalAlignment alignment, int fontHeight, boolean bold, BorderStyle bottom, BorderStyle top, BorderStyle left, BorderStyle right) {
        WriteCellStyle cellStyle = new WriteCellStyle();
        cellStyle.setHorizontalAlignment(alignment);
        cellStyle.setVerticalAlignment(VerticalAlignment.BOTTOM);
        cellStyle.setBorderBottom(bottom);
        cellStyle.setBorderLeft(left);
        cellStyle.setBorderRight(right);
        cellStyle.setBorderTop(top);
        cellStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());

        WriteFont contentWriteFont = new WriteFont();
        contentWriteFont.setFontName("微软雅黑");
        contentWriteFont.setBold(bold);
        contentWriteFont.setFontHeightInPoints((short) fontHeight);
        cellStyle.setWriteFont(contentWriteFont);
        return cellStyle;
    }
}
