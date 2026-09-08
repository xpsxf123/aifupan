package com.jiuyu.replay.generic.dto.order;

import lombok.Data;

import java.io.Serializable;

/**
 * @author RayChou
 * @date 2025/5/30 9:41
 */
@Data
public class CommodityDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    private Long id;
    /**
     * 商品名
     */
    private String name;
}
