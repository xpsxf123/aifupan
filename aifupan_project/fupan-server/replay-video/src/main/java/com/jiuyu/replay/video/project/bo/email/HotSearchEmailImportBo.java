package com.jiuyu.replay.video.project.bo.email;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 热搜邮箱账号导入BO
 *
 * @author RayChou
 * @date 2025-11-28
 * @description 用于Excel导入邮箱账号
 */
@Data
public class HotSearchEmailImportBo {

    /**
     * 邮箱账号
     */
    @ExcelProperty("邮箱账号")
    private String email;

    /**
     * 邮箱密码
     */
    @ExcelProperty("邮箱密码")
    private String emailPassword;

    /**
     * 账号类型：1-Gmail 2-Outlook 3-QQ邮箱 4-163邮箱 5-其他
     */
    @ExcelProperty("账号类型")
    private String accountType;

    /**
     * 账号归属城市
     */
    @ExcelProperty("账号归属城市")
    private String city;

    /**
     * 最大并发用户数
     */
    @ExcelProperty("最大并发用户数")
    private Integer maxConcurrentUsers;

    /**
     * 使用超时时间（分钟）
     */
    @ExcelProperty("使用超时时间(分钟)")
    private Integer useTimeoutMinutes;

    /**
     * 备注
     */
    @ExcelProperty("备注")
    private String remark;
}

