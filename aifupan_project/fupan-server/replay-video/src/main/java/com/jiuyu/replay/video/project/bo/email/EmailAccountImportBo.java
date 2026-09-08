package com.jiuyu.replay.video.project.bo.email;

import com.alibaba.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 邮箱账号导入BO（Excel导入）
 *
 * @author RayChou
 * @since 2025-11-25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "邮箱账号导入BO")
public class EmailAccountImportBo {

    /**
     * 邮箱账号
     */
    @ExcelProperty(value = "邮箱账号", index = 0)
    @Schema(description = "邮箱账号", example = "test@gmail.com")
    private String email;

    /**
     * 邮箱密码
     */
    @ExcelProperty(value = "邮箱密码", index = 1)
    @Schema(description = "邮箱密码", example = "password123")
    private String password;

    /**
     * 账号类型：1-Gmail 2-Outlook 3-QQ邮箱 4-163邮箱 5-其他
     */
    @ExcelProperty(value = "账号类型", index = 2)
    @Schema(description = "账号类型：1-Gmail 2-Outlook 3-QQ邮箱 4-163邮箱 5-其他", example = "1")
    private Integer accountType;

    /**
     * 账号归属城市
     */
    @ExcelProperty(value = "归属城市", index = 3)
    @Schema(description = "账号归属城市", example = "上海")
    private String city;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注", index = 4)
    @Schema(description = "备注", example = "测试账号")
    private String remark;
}

