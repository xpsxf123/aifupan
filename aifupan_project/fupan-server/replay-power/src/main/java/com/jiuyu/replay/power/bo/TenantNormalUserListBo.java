package com.jiuyu.replay.power.bo;

import com.jiuyu.replay.common.bo.PageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "普通用户租户列表查询参数")
public class TenantNormalUserListBo extends PageBo {

    @Schema(description = "手机号")
    private String phone;
}
