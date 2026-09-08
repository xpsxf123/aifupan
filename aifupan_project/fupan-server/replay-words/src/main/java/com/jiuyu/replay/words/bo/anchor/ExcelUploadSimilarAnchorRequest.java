package com.jiuyu.replay.words.bo.anchor;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

/**
 * 导入热榜主播请求, 注意请使用form-data 表单提交请求 不要用json
 *
 * @author HeHui
 * @date 2026-01-28 15:45
 */
@Getter
@Setter
public class ExcelUploadSimilarAnchorRequest {

    /**
     * 行业ID
     */
    private Long tradeId;

    /**
     * excel-文件
     */
    private MultipartFile file;
}
