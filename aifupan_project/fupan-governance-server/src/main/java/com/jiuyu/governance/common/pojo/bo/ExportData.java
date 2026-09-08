package com.jiuyu.governance.common.pojo.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 导出数据载体
 * <p>
 * 用于Redis存储导出文件的二进制数据和元信息
 *
 * @author lujie
 * @date 2026/3/31
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportData {

    /**
     * 文件二进制数据
     */
    private byte[] data;

    /**
     * 文件名（含扩展名）
     */
    private String filename;

    /**
     * MIME类型
     */
    private String contentType;

    /**
     * 创建时间戳（毫秒）
     */
    private Long createTime;
}
