package com.jiuyu.replay.power.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "修改用户")
public class UserUpdateClientBo {

    /**
     * id
     */
    @Schema(description = "id")
    private Long id;
    /**
     * 手机号
     */
    @Schema(description = "手机号")
    private String phone;
    /**
     * 昵称
     */
    @Schema(description = "昵称")
    private String nickName;
    /**
     * 用户头像id
     */
    @Schema(description = "用户头像id")
    private Long avatarImgId;
}
