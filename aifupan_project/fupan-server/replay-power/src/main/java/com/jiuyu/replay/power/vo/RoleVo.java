package com.jiuyu.replay.power.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@Schema(description = "角色")
public class RoleVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @Schema(description = "ID")
    private Long id;
    /**
     * 角色名
     */
    @Schema(description = "角色名")
    private String name;
    /**
     * 角色级别
     */
    @Schema(description = "角色级别")
    private String level;
    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private Date createDate;

}
