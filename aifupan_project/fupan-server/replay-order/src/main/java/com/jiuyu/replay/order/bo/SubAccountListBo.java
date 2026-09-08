package com.jiuyu.replay.order.bo;

import com.jiuyu.replay.common.bo.PageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
/**
 * 子账号查询参数
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-08 10:49:32
 */
@Data
@Schema(description = "子账号查询参数")
public class SubAccountListBo extends PageBo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户id")
    private Long parentId;

    @Schema(description = "用户名")
    private String userName;

    @Schema(description = "昵称")
    private String nickName;

    @Schema(description = "手机号")
    private String phone;
}
