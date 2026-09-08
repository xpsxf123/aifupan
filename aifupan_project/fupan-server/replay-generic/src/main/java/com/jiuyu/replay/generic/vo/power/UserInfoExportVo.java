package com.jiuyu.replay.generic.vo.power;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class UserInfoExportVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户昵称")
    private String nickName;

    @Schema(description = "用户手机号码")
    private String userPhone;

    @Schema(description = "用户注册日期")
    private Date userCreateDate;

    @Schema(description = "主播名称")
    private String anchorNickName;

    @Schema(description = "主播抖音号")
    private String anchorUrl;

    @Schema(description = "主播行业")
    private String anchorTrade;

    @Schema(description = "添加主播日期")
    private Date addAnchorDate;

}
