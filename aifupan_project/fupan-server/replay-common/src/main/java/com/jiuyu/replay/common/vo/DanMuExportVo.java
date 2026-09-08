package com.jiuyu.replay.common.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/11/11 19:10
 */
@Data
public class DanMuExportVo {

    @ExcelProperty("抖音名称")
    private String userName;

    @ExcelProperty("抖音等级")
    private String douYinLevel;

    @ExcelProperty("粉丝灯牌等级")
    private String fansLevel;

    @ExcelProperty("新用户")
    private String isNew;

    @ExcelProperty("弹幕时间")
    private String danMuDate;

    @ExcelProperty("弹幕内容")
    private String content;
}
