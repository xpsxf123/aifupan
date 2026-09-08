package com.jiuyu.replay.common.vo;

import com.jiuyu.replay.generic.vo.common.FileShowVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "图片配置信息")
public class ImgConfigVo {

    /**
     * H5兜底销售二维码图片文件
     */
    @Schema(description = "H5兜底销售二维码图片文件")
    private FileShowVo h5ImgVo;
    /**
     * 客户端销售销售二维码图片文件
     */
    @Schema(description = "客户端销售销售二维码图片文件")
    private FileShowVo clientSaleImgVo;
    /**
     * 纯录制版本客户端销售二维码图片文件
     */
    @Schema(description = "纯录制版本客户端销售二维码图片文件")
    private FileShowVo pureRecordImgVo;
}
