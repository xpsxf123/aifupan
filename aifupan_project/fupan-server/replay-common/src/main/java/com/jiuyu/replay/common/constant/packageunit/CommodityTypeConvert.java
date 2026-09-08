package com.jiuyu.replay.common.constant.packageunit;

import cn.hutool.core.util.StrUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 增量包数据转换类
 *
 * @author RayChou
 * @date 2025/6/5 19:11
 */
public class CommodityTypeConvert {

    /**
     * 增量包数据转换
     *
     * @param code   商品类型code
     * @param number 商品数量
     * @return
     */
    public static Long commodityNumberConvert(String code, Long number) {
        if (StrUtil.isBlank(code)) {
            return number;
        }
        if (code.equals("aiAnalysisTime")) {
            // 奖励是分析时长，存储的值是分钟，需要转成小时
            return number / 60;
        } else if (code.equals("textExtraction")||code.equals("textExtractionNum")) {
            // 奖励是分析时长，存储的值是分钟，需要转成小时
            return number / 60;
        } else if (code.equals("storageNum")) {
            // 奖励是存储空间，存储的是KB，需要转成G
            return number / 1024 / 1024;
        }
        return number;
    }


    /**
     * 增量包数据转换
     *
     * @param code 商品类型code
     * @param number 商品数量
     * @return
     */
    public static Long commodityOnlyHour(String code, Long number) {
        if (StrUtil.isBlank(code)) {
            return number;
        }
        if (code.equals("aiAnalysisTime")) {
            // 奖励是分析时长，存储的值是分钟，需要转成小时
            return number / 60;
        } else if (code.equals("textExtraction")||code.equals("textExtractionNum")) {
            return number / 60;
        }
        return number;
    }

    /**
     * 数据单位转换
     *
     * @param code 商品类型code
     * @param number 商品数量
     * @return
     */
    public static BigDecimal commodityBigDecimalOnlyHour(String code, BigDecimal  number) {
        if (StrUtil.isBlank(code)) {
            return number;
        }
        if (number== null){
            return BigDecimal.ZERO;
        }
        if (code.equals("aiAnalysisTime")) {
            // 奖励是分析时长，存储的值是分钟，需要转成小时
            return number.divide(BigDecimal.valueOf(60), 1, RoundingMode.HALF_UP);
        } else if (code.equals("textExtraction")||code.equals("textExtractionNum")) {
            return number.divide(BigDecimal.valueOf(60), 1, RoundingMode.HALF_UP);
        }
        return number;
    }
}
