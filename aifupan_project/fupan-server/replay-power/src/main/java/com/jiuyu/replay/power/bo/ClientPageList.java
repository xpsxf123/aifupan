package com.jiuyu.replay.power.bo;

import com.jiuyu.replay.common.bo.PageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/12/5 19:11
 */
@Data
public class ClientPageList extends PageBo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "昵称")
    private String nickName;

    @Schema(description = "用户类型 0：普通用户 2：子账号")
    private Integer userType;
}
