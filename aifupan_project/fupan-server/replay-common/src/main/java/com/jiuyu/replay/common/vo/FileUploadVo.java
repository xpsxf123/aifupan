package com.jiuyu.replay.common.vo;

import lombok.Data;

/**
 * @author ：lujie
 * @description：文件上传后的返回值
 * @date ：2025/3/14 上午9:48
 */
@Data
public class FileUploadVo {
    /**
     * 文件key
     */
    private String key;

    /**
     * 文件url
     */
    private String url;
}
