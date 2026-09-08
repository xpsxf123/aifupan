package com.jiuyu.replay.common.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "查询更新记录入参")
public class SearchClientUpdateRecordBo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "新版本号")
    private String updateVersion;

    @Schema(description = "旧版本号")
    private String oldVersion;

    @Schema(description = "客户端当前登录的用户")
    private String clientUser;

    @Schema(description = "更新类型 0手动更新  1强制更新")
    private Integer updateType;

    /**
     * 开始时间
     */
    @Schema(description = "开始时间")
    public String startDate;

    /**
     * 结束时间
     */
    @Schema(description = "结束时间")
    public String endDate;

    /**
     * 当前页码
     */
    @Schema(description = "当前页码")
    public Integer pageIndex;

    /**
     * 每页显示条数
     */
    @Schema(description = "每页显示条数")
    public Integer pageSize;


}
