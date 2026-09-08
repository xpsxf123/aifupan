package com.jiuyu.replay.words.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 商品列表项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2026-03-19
 */
@Data
@Schema(description = "商品列表项")
public class ProductDetailsListVo extends ProductDetailsVo implements Serializable {
    private static final long serialVersionUID = 1L;


}
