package com.jiuyu.replay.common.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "图片配置")
public class ImgConfigBo {

    /**
     * H5兜底销售二维码图片文件id
     */
    @Schema(description = "H5兜底销售二维码图片文件id")
    private Long h5ImgId;
    /**
     * 客户端销售销售二维码图片文件id
     */
    @Schema(description = "客户端销售销售二维码图片文件id")
    private Long clientSaleImgId;
    /**
     * 纯录制版本客户端销售二维码图片文件id
     */
    @Schema(description = "纯录制版本客户端销售二维码图片文件id")
    private Long pureRecordImgId;
}
