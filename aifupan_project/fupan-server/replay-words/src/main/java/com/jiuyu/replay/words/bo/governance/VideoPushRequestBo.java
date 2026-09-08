
package com.jiuyu.replay.words.bo.governance;

import cn.hutool.core.util.ObjUtil;
import com.jiuyu.replay.generic.enums.words.VideoPlatformEnum;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 推送视频数据到governance的请求对象
 *
 * @author jxy
 * @date 2026-03-19
 */
@Data
public class VideoPushRequestBo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 直播批次号
     */
    private String batchNumber;

    /**
     * 视频唯一标识
     */
    private String videoId;

    /**
     * 业绩数据是否存在：0-丢失业绩数据，1-完好
     */
    private Integer hasPerformance;

    /**
     * 视频文件OSS地址
     */
    private String videoOssUrl;

    /**
     * 视频开始时间
     */
    private Date startTime;

    /**
     * 视频结束时间
     */
    private Date endTime;

    /**
     * 场观
     */
    private Integer cumulativeView;

    /**
     * 开始累计销售额
     */
    private BigDecimal salesRevenue;


    /**
     * 退款
     */
    private BigDecimal cumulativeRefund;

    /**
     * 投放金额
     */
    private BigDecimal investment;

    /**
     * 主播secUid
     */
    private String secUid;

    /**
     * 平台类型：0-抖音，1-快手，2-视频号
     */
    private Integer platform;

    /**
     * 商品列表
     */
    private List<VideoProductRequestBo> products;

    /**
     * 将视频表的平台类型转换为Integer
     *
     * @param platform 平台类型
     * @return 平台类型对应的Integer值
     */
    public Integer transformedPlatform(String platform) {
        if (platform == null || platform.isEmpty()) return 0;
        if (ObjUtil.equals(VideoPlatformEnum.DOUYIN.getCode(), platform)) {
            return 0;
        } else if (ObjUtil.equals(VideoPlatformEnum.KUAISHOU.getCode(), platform)) {
            return 1;
        } else if (ObjUtil.equals(VideoPlatformEnum.SHIPINHAO.getCode(), platform)) {
            return 2;
        } else {
            return 0;
        }
    }
}
