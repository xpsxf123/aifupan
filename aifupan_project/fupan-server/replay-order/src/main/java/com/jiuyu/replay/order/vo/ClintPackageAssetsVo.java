package com.jiuyu.replay.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@Schema(description = "套餐资产")
public class ClintPackageAssetsVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 套餐版本
     */
    @Schema(description = "套餐版本")
    private String packageVersion;


    @Schema(description = "有效期")
    private Date expirationDate;


    @Schema(description = "下次资源更新时间")
    private Date nextUpdateTime;

    @Schema(description = "资源数据")
    List<ClintGetPackageDataVo> dataList;

}
