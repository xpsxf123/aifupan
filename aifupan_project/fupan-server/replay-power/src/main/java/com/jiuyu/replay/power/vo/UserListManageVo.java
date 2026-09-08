package com.jiuyu.replay.power.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;


@Data
@Schema(description = "管理用户")
public class UserListManageVo extends UserVo{

    @Schema(description = "角色列表")
    private List<RoleVo> roleList;

    @Schema(description = "员工状态 0离职  1在职")
    private Integer employeeStatus;
}
