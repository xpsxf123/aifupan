package com.jiuyu.replay.generic.vo.power;

import com.jiuyu.replay.generic.vo.common.FileShowVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/10/30 10:07
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "销售级联选择器")
public class RegisterVo {

    /**
     * 销售人员id
     */
    @Schema(description = "销售人员id")
    private Long saleId;
    /**
     * 销售二维码图片
     */
    @Schema(description = "销售二维码图片")
    private FileShowVo saleQrcodeImg;

}
