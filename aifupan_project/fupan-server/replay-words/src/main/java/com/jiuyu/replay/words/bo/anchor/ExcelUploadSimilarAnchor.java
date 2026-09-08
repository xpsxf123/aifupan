package com.jiuyu.replay.words.bo.anchor;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * 基于excel或手动添加的热榜主播数据
 *
 * @author HeHui
 * @date 2026-01-28 11:42
 */
@Getter
@Setter
public class ExcelUploadSimilarAnchor implements Serializable {

    @Serial
    private static final long serialVersionUID = 653110667589380927L;


    /**
     * 行业ID
     */
    @ExcelIgnore
    private Long tradeId;

    /**
     * 主播抖音账号
     */
    @NotBlank(message = "抖音号不能为空")
    @ExcelProperty(value = "抖音号", index = 0)
    private String uniqueId;

    /**
     * 主播名称
     */
    @NotBlank(message = "抖音名称不能为空")
    @ExcelProperty(value = "抖音名称", index = 1)
    private String anchorName;
    /**
     * 主播头像
     */
    @NotBlank(message = "抖音头像不能为空")
    @ExcelProperty(value = "抖音头像", index = 2)
    private String anchorAvatar;
    /**
     * 粉丝数
     */
    @NotBlank(message = "粉丝数不能为空")
    @ExcelProperty(value = "粉丝量", index = 3)
    private String followerCount;

    /**
     * 直播场次
     */
    @ExcelProperty(value = "30天直播场次", index = 4)
    private String liveCount;

    /**
     * 平均场观
     */
    @NotBlank(message = "场均观不能为空")
    @ExcelProperty(value = "30天场均观", index = 5)
    private String liveAverageUser;
    /**
     * 场均销售额
     */
    @ExcelProperty(value = "30天场均销售额", index = 6)
    private String liveAverageAmount;

    /**
     * 直播销售总额
     */
    @ExcelProperty(value = "30天直播销售额", index = 7)
    private String totalAmount;
}
